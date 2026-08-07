package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.model.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateRouteMetricsUseCaseTest {

    private lateinit var useCase: CalculateRouteMetricsUseCase

    @Before
    fun setUp() {
        useCase = CalculateRouteMetricsUseCase()
    }

    private fun point(
        id: Long,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        speed: Float = 0f,
        timestamp: Long = 0L
    ): LocationPoint = LocationPoint(
        id = id,
        routeId = 1,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        speed = speed,
        timestamp = timestamp
    )

    @Test
    fun execute_withEmptyList_returnsDefaultMetrics() {
        val result = useCase.execute(emptyList())
        assertEquals(0, result.pointCount)
        assertEquals(0.0, result.totalDistanceKm, 0.001)
        assertEquals(0.0, result.elevationGainMeters, 0.001)
        assertEquals("0:00", result.averagePaceMinPerKm)
    }

    @Test
    fun execute_withSinglePoint_returnsDefaultMetricsWithCount() {
        val result = useCase.execute(
            listOf(point(id = 1, latitude = -13.16, longitude = -72.54, altitude = 2400.0))
        )
        assertEquals(1, result.pointCount)
        assertEquals(0.0, result.totalDistanceKm, 0.001)
        assertEquals(0L, result.durationSeconds)
        assertEquals(0.0, result.maxSpeedKmh, 0.001)
    }

    @Test
    fun execute_withNoiseBelowThreshold_ignoresSmallElevationChanges() {
        // Puntos con ruido altimetrico < 2.5m (2400 -> 2405 [OK] -> 2403 [Ignorado])
        val points = listOf(
            point(id = 1, latitude = -13.16, longitude = -72.54, altitude = 2400.0, speed = 1.0f, timestamp = 1000000L),
            point(id = 2, latitude = -13.17, longitude = -72.54, altitude = 2405.0, speed = 2.0f, timestamp = 1060000L),
            point(id = 3, latitude = -13.18, longitude = -72.54, altitude = 2403.0, speed = 1.5f, timestamp = 1120000L)
        )

        val result = useCase.execute(points)

        assertEquals(3, result.pointCount)
        assertEquals(5.0, result.elevationGainMeters, 0.001)
        assertEquals(0.0, result.elevationLossMeters, 0.001)
    }

    @Test
    fun execute_withValidAscentAndDescent_accumulatesGainAndLoss() {
        val points = listOf(
            point(id = 1, latitude = -13.16, longitude = -72.54, altitude = 2000.0, speed = 1.0f, timestamp = 1000000L),
            point(id = 2, latitude = -13.17, longitude = -72.54, altitude = 2010.0, speed = 2.0f, timestamp = 1060000L),
            point(id = 3, latitude = -13.18, longitude = -72.54, altitude = 2003.0, speed = 1.5f, timestamp = 1120000L)
        )

        val result = useCase.execute(points)

        assertEquals(10.0, result.elevationGainMeters, 0.001)
        assertEquals(7.0, result.elevationLossMeters, 0.001)
    }

    @Test
    fun execute_withTwoPoints_calculatesHaversineDistanceAndSpeed() {
        val points = listOf(
            point(id = 1, latitude = -13.16, longitude = -72.54, altitude = 2400.0, speed = 2.0f, timestamp = 1000000L),
            point(id = 2, latitude = -13.17, longitude = -72.54, altitude = 2410.0, speed = 5.0f, timestamp = 2000000L)
        )

        val result = useCase.execute(points)

        assertEquals(2, result.pointCount)
        assertEquals(1000L, result.durationSeconds)
        assertEquals(1112.0, result.totalDistanceKm * 1000.0, 2.0)
        assertEquals(18.0, result.maxSpeedKmh, 0.01)
        assertEquals(0.0, result.elevationLossMeters, 0.001)
        assertEquals(10.0, result.elevationGainMeters, 0.001)
    }

    @Test
    fun execute_withKnownDurationAndDistance_formatsPace() {
        // 2 segmentos de 0.01 grados de latitud (~1.112 km c/u) en 120s totales
        val points = listOf(
            point(id = 1, latitude = -13.16, longitude = -72.54, altitude = 2400.0, timestamp = 1000000L),
            point(id = 2, latitude = -13.17, longitude = -72.54, altitude = 2400.0, timestamp = 1060000L),
            point(id = 3, latitude = -13.18, longitude = -72.54, altitude = 2400.0, timestamp = 1120000L)
        )

        val result = useCase.execute(points)

        assertEquals(120L, result.durationSeconds)
        assertEquals("0:53", result.averagePaceMinPerKm)
        assertEquals(0.0, result.elevationGainMeters, 0.001)
    }

    @Test
    fun execute_averageSpeedDerivedFromDistanceAndTime() {
        val points = listOf(
            point(id = 1, latitude = -13.16, longitude = -72.54, altitude = 2400.0, timestamp = 1000000L),
            point(id = 2, latitude = -13.17, longitude = -72.54, altitude = 2400.0, timestamp = 2000000L)
        )

        val result = useCase.execute(points)

        val expectedKmh = (result.totalDistanceKm / (result.durationSeconds / 3600.0))
        assertEquals(expectedKmh, result.averageSpeedKmh, 0.001)
    }
}