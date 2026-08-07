package com.aistudio.localizador.model

/**
 * Modelo inmutable de dominio con los resultados de telemetria y altimetria
 * calculados sobre un tramo especifico de puntos GPS.
 */
data class RouteMetricsResult(
    val totalDistanceKm: Double = 0.0,
    val durationSeconds: Long = 0L,
    val averageSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val averagePaceMinPerKm: String = "0:00",
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val pointCount: Int = 0
)