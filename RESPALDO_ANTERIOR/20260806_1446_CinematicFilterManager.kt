package com.aistudio.localizador.location.manager

import com.aistudio.localizador.util.AppConstants
import kotlin.math.*

class CinematicFilterManager(
    private val maxAllowedSpeedMps: Double = AppConstants.Location.MAX_SPEED_METERS_PER_SECOND.toDouble(),
    private val autoStopSpeedMps: Double = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()
) {

    companion object {
        private const val AUTO_STOP_TIME_THRESHOLD_MS = 180000L
    }

    private var refLatitude: Double? = null
    private var refLongitude: Double? = null
    private var refTimestamp: Long? = null

    private var lowSpeedStartTimestamp: Long? = null
    private var isAutoStopTriggered: Boolean = false

    fun reset() {
        refLatitude = null
        refLongitude = null
        refTimestamp = null
        lowSpeedStartTimestamp = null
        isAutoStopTriggered = false
    }

    fun seedReferencePoint(lat: Double, lon: Double, timestamp: Long) {
        refLatitude = lat
        refLongitude = lon
        refTimestamp = timestamp
        lowSpeedStartTimestamp = null
        isAutoStopTriggered = false
    }

    fun evaluateAndProcess(
        lat: Double,
        lon: Double,
        accuracy: Float,
        timestamp: Long,
        speed: Float
    ): Boolean {
        val lastLat = refLatitude ?: return run {
            seedReferencePoint(lat, lon, timestamp)
            true
        }
        val lastLon = refLongitude ?: return true
        val lastTime = refTimestamp ?: return true

        val timeDeltaSeconds = (timestamp - lastTime) / 1000.0
        if (timeDeltaSeconds <= 0) return false

        val distanceMeters = haversineDistance(lastLat, lastLon, lat, lon)
        val calculatedSpeed = distanceMeters / timeDeltaSeconds

        if (calculatedSpeed > maxAllowedSpeedMps) return false

        if (speed < autoStopSpeedMps) {
            val startTime = lowSpeedStartTimestamp
            if (startTime == null) {
                lowSpeedStartTimestamp = timestamp
            } else if (timestamp - startTime >= AUTO_STOP_TIME_THRESHOLD_MS) {
                isAutoStopTriggered = true
            }
        } else {
            lowSpeedStartTimestamp = null
            isAutoStopTriggered = false
        }

        refLatitude = lat
        refLongitude = lon
        refTimestamp = timestamp

        return true
    }

    fun isAutoStopActive(): Boolean = isAutoStopTriggered

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
