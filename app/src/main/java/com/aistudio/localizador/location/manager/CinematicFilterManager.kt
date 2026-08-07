package com.aistudio.localizador.location.manager

import com.aistudio.localizador.util.AppConstants
import kotlin.math.*

enum class KineticProfile {
    TERRESTRIAL,
    FLIGHT
}

class CinematicFilterManager(
    private val maxAllowedSpeedMps: Double = AppConstants.Location.MAX_SPEED_THRESHOLD_MPS.toDouble(),
    private val autoStopSpeedMps: Double = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()
) {

    companion object {
        private const val AUTO_STOP_TIME_THRESHOLD_MS = 180000L
    }

    private var currentProfile = KineticProfile.TERRESTRIAL
    private var refLatitude: Double? = null
    private var refLongitude: Double? = null
    private var refTimestamp: Long? = null

    private var lowSpeedStartTimestamp: Long? = null
    private var isAutoStopTriggered: Boolean = false

    fun reset() {
        currentProfile = KineticProfile.TERRESTRIAL
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
        speed: Float,
        altitudeMeters: Double = 0.0
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

        // Transición de perfil cinemático con histéresis (Modo Vuelo Automático)
        if (calculatedSpeed > AppConstants.Location.FLIGHT_TRIGGER_SPEED_MPS &&
            altitudeMeters > AppConstants.Location.FLIGHT_MIN_ALTITUDE_METERS) {
            currentProfile = KineticProfile.FLIGHT
        } else if (calculatedSpeed < AppConstants.Location.FLIGHT_HYSTERESIS_SPEED_MPS &&
                   altitudeMeters < AppConstants.Location.FLIGHT_MIN_ALTITUDE_METERS) {
            currentProfile = KineticProfile.TERRESTRIAL
        }

        val limitSpeed = if (currentProfile == KineticProfile.FLIGHT) {
            AppConstants.Location.FLIGHT_MAX_SPEED_MPS.toDouble()
        } else {
            maxAllowedSpeedMps
        }

        if (calculatedSpeed > limitSpeed) return false

        // Auto-stop terrestre (3 min): se suprime por completo en perfil FLIGHT
        if (currentProfile == KineticProfile.TERRESTRIAL) {
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
        }

        refLatitude = lat
        refLongitude = lon
        refTimestamp = timestamp

        return true
    }

    fun isAutoStopActive(): Boolean = isAutoStopTriggered

    fun getCurrentProfile(): KineticProfile = currentProfile

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}