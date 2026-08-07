package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.model.RouteMetricsResult
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * UseCase encargado de calcular de forma deterministica las metricas cinematicas y altimetricas
 * de un conjunto de puntos GPS. Ejecutable 100% en JVM sin dependencias de Android SDK.
 */
class CalculateRouteMetricsUseCase @Inject constructor() {

    companion object {
        private const val ELEVATION_THRESHOLD_METERS = 2.5
        private const val EARTH_RADIUS_METERS = 6371000.0
    }

    fun execute(points: List<LocationPoint>): RouteMetricsResult {
        if (points.size < 2) {
            return RouteMetricsResult(pointCount = points.size)
        }

        var totalDistanceMeters = 0.0
        var elevationGain = 0.0
        var elevationLoss = 0.0
        var maxSpeedMps = 0.0

        val startTimeMs = points.first().timestamp
        val endTimeMs = points.last().timestamp
        val durationSeconds = max(0L, (endTimeMs - startTimeMs) / 1000L)

        var previousPoint = points.first()

        for (i in 1 until points.size) {
            val currentPoint = points[i]

            // 1. Distancia por Haversine Puro
            val dist = calculateHaversineDistance(
                previousPoint.latitude, previousPoint.longitude,
                currentPoint.latitude, currentPoint.longitude
            )
            totalDistanceMeters += dist

            // 2. Velocidad Maxima Registrada
            if (currentPoint.speed > maxSpeedMps) {
                maxSpeedMps = currentPoint.speed.toDouble()
            }

            // 3. Altimetria Acumulada con Filtro de Umbral Vertical (2.5m)
            val deltaAlt = currentPoint.altitude - previousPoint.altitude
            if (abs(deltaAlt) >= ELEVATION_THRESHOLD_METERS) {
                if (deltaAlt > 0) {
                    elevationGain += deltaAlt
                } else {
                    elevationLoss += abs(deltaAlt)
                }
            }

            previousPoint = currentPoint
        }

        val totalDistanceKm = totalDistanceMeters / 1000.0
        val averageSpeedKmh = if (durationSeconds > 0) {
            (totalDistanceKm / (durationSeconds / 3600.0))
        } else 0.0

        val paceMinPerKm = calculatePace(totalDistanceKm, durationSeconds)

        return RouteMetricsResult(
            totalDistanceKm = totalDistanceKm,
            durationSeconds = durationSeconds,
            averageSpeedKmh = averageSpeedKmh,
            maxSpeedKmh = maxSpeedMps * 3.6,
            averagePaceMinPerKm = paceMinPerKm,
            elevationGainMeters = elevationGain,
            elevationLossMeters = elevationLoss,
            pointCount = points.size
        )
    }

    private fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun calculatePace(distanceKm: Double, durationSeconds: Long): String {
        if (distanceKm <= 0.0 || durationSeconds <= 0) return "0:00"
        val totalPaceSecondsPerKm = (durationSeconds / distanceKm).toLong()
        val minutes = totalPaceSecondsPerKm / 60
        val seconds = totalPaceSecondsPerKm % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}