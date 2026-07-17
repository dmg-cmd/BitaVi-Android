package com.aistudio.localizador.location.manager

import com.aistudio.localizador.location.manager.TrackingSessionManager.BufferPoint
import io.mockk.clearAllMocks
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GpsFilterPipelineTest {

    private lateinit var pipeline: GpsFilterPipeline
    private lateinit var cinematicFilter: CinematicFilterManager
    private lateinit var stationaryAnchor: StationaryAnchorManager

    @Before
    fun setUp() {
        cinematicFilter = CinematicFilterManager(
            maxAllowedSpeedMps = 50.0,
            autoStopSpeedMps = 0.5
        )
        stationaryAnchor = StationaryAnchorManager()
        pipeline = GpsFilterPipeline(cinematicFilter, stationaryAnchor)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cuando ingresa un punto con velocidad aberrante el pipeline lo descarta`() {
        val t0 = 1000L
        val latBase = -34.6037
        val lonBase = -58.3816

        val p0 = BufferPoint(latBase, lonBase, 5f, 10f, 0.0, t0)
        val res0 = pipeline.processLocation(p0)
        assertNotNull("El primer punto debe establecer la semilla y ser procesado", res0)

        val t1 = t0 + 1000L
        val latAberrante = latBase + 0.1
        val pAberrante = BufferPoint(latAberrante, lonBase, 5f, 10f, 0.0, t1)

        val resAberrante = pipeline.processLocation(pAberrante)
        assertNull("El pipeline debe purgar coordenadas que violen los limites cinematicos", resAberrante)
    }

    @Test
    fun `cuando la velocidad decae sostenidamente por 3 minutos se activa el auto stop`() {
        val lat = -34.6037
        val lon = -58.3816
        var currentTimestamp = 10000L

        pipeline.processLocation(BufferPoint(lat, lon, 3f, 5.0f, 0.0, currentTimestamp))
        assertFalse("El auto-stop no debe estar activo al inicio", pipeline.shouldTriggerAutoStop())

        currentTimestamp += 1000L
        pipeline.processLocation(BufferPoint(lat, lon, 3f, 0.1f, 0.0, currentTimestamp))
        assertFalse("El auto-stop no debe activarse instantaneamente", pipeline.shouldTriggerAutoStop())

        currentTimestamp += 120000L
        pipeline.processLocation(BufferPoint(lat, lon, 3f, 0.1f, 0.0, currentTimestamp))
        assertFalse("El auto-stop no debe activarse a los 2 minutos", pipeline.shouldTriggerAutoStop())

        currentTimestamp += 60000L
        pipeline.processLocation(BufferPoint(lat, lon, 3f, 0.1f, 0.0, currentTimestamp))
        assertTrue("El auto-stop debe activarse exactamente a los 3 minutos", pipeline.shouldTriggerAutoStop())
    }

    @Test
    fun `cuando el usuario reanuda marcha por encima del umbral se cancela el auto stop`() {
        val lat = -34.6037
        val lon = -58.3816
        var currentTimestamp = 10000L

        pipeline.processLocation(BufferPoint(lat, lon, 3f, 0.1f, 0.0, currentTimestamp))
        currentTimestamp += 180000L
        pipeline.processLocation(BufferPoint(lat, lon, 3f, 0.1f, 0.0, currentTimestamp))
        currentTimestamp += 180000L
        pipeline.processLocation(BufferPoint(lat, lon, 3f, 0.1f, 0.0, currentTimestamp))
        assertTrue(pipeline.shouldTriggerAutoStop())

        currentTimestamp += 5000L
        pipeline.processLocation(BufferPoint(lat, lon, 3f, 2.5f, 0.0, currentTimestamp))
        assertFalse("La maquina de estados debe desactivar el auto-stop si el usuario acelera", pipeline.shouldTriggerAutoStop())
    }


    @Test
    fun `drift prolongado en interiores debe congelar coordenadas`() {
        val latBase = -34.6037
        val lonBase = -58.3816
        var currentTimestamp = 1000L

        val p0 = BufferPoint(latBase, lonBase, 5f, 0.1f, 0.0, currentTimestamp)
        pipeline.processLocation(p0)

        repeat(10) { i ->
            currentTimestamp += 1000L
            val driftLat = latBase + (0.00001 * i)
            val driftLon = lonBase + (0.00001 * i)

            val driftPoint = BufferPoint(
                latitude = driftLat,
                longitude = driftLon,
                accuracy = 25f,
                speed = 0.05f,
                altitude = 0.0,
                timestamp = currentTimestamp
            )

            val result = pipeline.processLocation(driftPoint)

            assertNotNull("El pipeline debe procesar la coordenada del drift", result)

            assertEquals("La latitud debe mantenerse anclada para anular la deriva", latBase, result!!.latitude, 0.00001)
            assertEquals("La longitud debe mantenerse anclada para anular la deriva", lonBase, result.longitude, 0.00001)
        }
    }

    @Test
    fun `reconexion tras larga perdida de senhal no debe descartar punto valido`() {
        val latBase = -34.6037
        val lonBase = -58.3816

        val p0 = BufferPoint(latBase, lonBase, 5f, 10f, 0.0, 1000L)
        pipeline.processLocation(p0)

        val tReconexion = 1000L + 301000L
        val latReconexion = -34.6130
        val lonReconexion = -58.3900

        val pReconexion = BufferPoint(
            latitude = latReconexion,
            longitude = lonReconexion,
            accuracy = 5f,
            speed = 10f,
            altitude = 15.0,
            timestamp = tReconexion
        )

        val result = pipeline.processLocation(pReconexion)

        assertNotNull("El punto tras cold-start prolongado debe ser procesado exitosamente", result)
        assertEquals("La latitud debe actualizarse a la nueva referencia real", latReconexion, result!!.latitude, 0.0001)
        assertEquals("La longitud debe actualizarse a la nueva referencia real", lonReconexion, result.longitude, 0.0001)
    }

}