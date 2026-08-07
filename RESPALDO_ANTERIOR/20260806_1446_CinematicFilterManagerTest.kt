package com.aistudio.localizador.location.manager

import org.junit.After
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
}