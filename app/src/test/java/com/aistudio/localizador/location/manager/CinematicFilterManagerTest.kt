package com.aistudio.localizador.location.manager

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CinematicFilterManagerTest {

    private lateinit var filterManager: CinematicFilterManager

    @Before
    fun setUp() {
        filterManager = CinematicFilterManager()
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `cuando la velocidad calculada supera el limite de inercia el punto debe ser rechazado`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        filterManager.seedReferencePoint(latBase, lonBase, timestamp = 1000L)

        val latSaltoAberrante = -35.9000
        val lonSaltoAberrante = -59.5000

        val esValido = filterManager.evaluateAndProcess(
            lat = latSaltoAberrante,
            lon = lonSaltoAberrante,
            accuracy = 10f,
            timestamp = 3000L,
            speed = 10f
        )

        assertFalse("El punto aberrante deberia haber sido filtrado por velocidad excesiva", esValido)
    }

    @Test
    fun `cuando la distancia y velocidad son coherentes el punto debe ser aceptado`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        filterManager.seedReferencePoint(latBase, lonBase, timestamp = 1000L)

        val latCoherente = -34.6038
        val lonCoherente = -58.3817

        val esValido = filterManager.evaluateAndProcess(
            lat = latCoherente,
            lon = lonCoherente,
            accuracy = 5f,
            timestamp = 5000L,
            speed = 2f
        )

        assertTrue("El punto coherente dentro de limites cinematicos deberia ser aceptado", esValido)
    }

    @Test
    fun `cuando la velocidad decae sostenidamente por 3 minutos se activa el auto stop`() {
        var currentTimestamp = 10000L

        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 5f)
        assertFalse("Auto-stop no debe estar activo al inicio", filterManager.isAutoStopActive())

        currentTimestamp += 1000L
        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 0.1f)
        assertFalse("Auto-stop no debe activarse instantaneamente", filterManager.isAutoStopActive())

        currentTimestamp += 120000L
        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 0.1f)
        assertFalse("Auto-stop no debe activarse a los 2 minutos", filterManager.isAutoStopActive())

        currentTimestamp += 60000L
        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 0.1f)
        assertTrue("Auto-stop debe activarse a los 3 minutos exactos", filterManager.isAutoStopActive())
    }

    @Test
    fun `cuando el usuario reanuda marcha se cancela el auto stop`() {
        var currentTimestamp = 10000L

        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 0.1f)
        currentTimestamp += 180000L
        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 0.1f)
        currentTimestamp += 180000L
        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 0.1f)
        assertTrue(filterManager.isAutoStopActive())

        currentTimestamp += 5000L
        filterManager.evaluateAndProcess(-34.6037, -58.3816, 5f, currentTimestamp, 2.5f)
        assertFalse("Auto-stop debe cancelarse al reanudar marcha", filterManager.isAutoStopActive())
    }

    @Test
    fun `superando 150 kmh con altitud mayor a 100m debe activar perfil FLIGHT y aceptar puntos de jet`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        filterManager.seedReferencePoint(latBase, lonBase, timestamp = 1000L)

        // ~500m en 2s = 250 m/s (~900 km/h) con altitud de crucero 3000m
        val latJet = latBase + (500.0 / 111195.0)
        val esValido = filterManager.evaluateAndProcess(
            lat = latJet,
            lon = lonBase,
            accuracy = 60f,
            timestamp = 3000L,
            speed = 100f,
            altitudeMeters = 3000.0
        )

        assertTrue("Un punto de jet comercial a 250 m/s debe ser aceptado en FLIGHT", esValido)
        assertEquals("El perfil debe conmutar a FLIGHT", KineticProfile.FLIGHT, filterManager.getCurrentProfile())
    }

    @Test
    fun `en perfil FLIGHT no se dispara auto stop por velocidad baja con altitud alta`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        filterManager.seedReferencePoint(latBase, lonBase, timestamp = 1000L)

        val latJet = latBase + (500.0 / 111195.0)
        filterManager.evaluateAndProcess(latJet, lonBase, 60f, 3000L, 100f, altitudeMeters = 3000.0)
        assertEquals("El perfil debe conmutar a FLIGHT", KineticProfile.FLIGHT, filterManager.getCurrentProfile())

        // Turbulencia: velocidad instantanea baja pero a altitud de crucero -> no auto-stop
        var currentTimestamp = 5000L
        repeat(4) {
            currentTimestamp += 60000L
            filterManager.evaluateAndProcess(latJet, lonBase, 60f, currentTimestamp, 0.1f, altitudeMeters = 3000.0)
        }

        assertFalse("El auto-stop debe permanecer suprimido en perfil FLIGHT", filterManager.isAutoStopActive())
    }

    @Test
    fun `al descender bajo 120 kmh y 100m el perfil vuelve a TERRESTRIAL y vuelve a filtrar velocidades de jet`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        filterManager.seedReferencePoint(latBase, lonBase, timestamp = 1000L)

        val latJet = latBase + (500.0 / 111195.0)
        filterManager.evaluateAndProcess(latJet, lonBase, 60f, 3000L, 100f, altitudeMeters = 3000.0)
        assertEquals("El perfil debe conmutar a FLIGHT", KineticProfile.FLIGHT, filterManager.getCurrentProfile())

        // Descenso a nivel de tierra: velocidad y altitud bajas -> regresa a TERRESTRIAL
        filterManager.evaluateAndProcess(latJet, lonBase, 5f, 6000L, 0.5f, altitudeMeters = 0.0)
        assertEquals("El perfil debe regresar a TERRESTRIAL tras aterrizar", KineticProfile.TERRESTRIAL, filterManager.getCurrentProfile())

        // Un nuevo salto de jet a nivel de tierra debe ser rechazado
        // (desde latJet, ~300m en 3s = 100 m/s > 45 -> debe purgarse)
        val latSalto = latBase + (200.0 / 111195.0)
        val esValido = filterManager.evaluateAndProcess(
            lat = latSalto,
            lon = lonBase,
            accuracy = 60f,
            timestamp = 9000L,
            speed = 100f,
            altitudeMeters = 0.0
        )

        assertFalse("Un salto de 200 m/s en TERRESTRIAL debe seguir siendo filtrado", esValido)
    }

    @Test
    fun `a nivel de tierra una velocidad de jet debe ser rechazada sin activar FLIGHT`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        filterManager.seedReferencePoint(latBase, lonBase, timestamp = 1000L)

        val latSalto = latBase + (400.0 / 111195.0)
        val esValido = filterManager.evaluateAndProcess(
            lat = latSalto,
            lon = lonBase,
            accuracy = 60f,
            timestamp = 3000L,
            speed = 100f,
            altitudeMeters = 0.0
        )

        assertFalse("Un punto a 200 m/s sin altitud no debe activar FLIGHT y debe ser filtrado", esValido)
        assertEquals("El perfil debe permanecer TERRESTRIAL", KineticProfile.TERRESTRIAL, filterManager.getCurrentProfile())
    }
}