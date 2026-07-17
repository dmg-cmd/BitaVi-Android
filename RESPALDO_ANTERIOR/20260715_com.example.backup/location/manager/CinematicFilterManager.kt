package com.example.location.manager

import android.location.Location
import kotlin.math.*

class CinematicFilterManager(
    private val maxAllowedSpeedMps: Double = 15.0,
    private val autoStopSpeedMps: Double = 0.3
) {

    companion object {
        const val SUSTAINED_STOP_TICKS = 3
    }

    private var lastValidLocation: Location? = null
    private var lowSpeedCounter = 0

    private var refLat: Double = 0.0
    private var refLon: Double = 0.0
    private var refTimestamp: Long = 0L
    private var refSpeed: Float = 0f
    private var hasRef: Boolean = false

    fun filterAndProcess(currentLocation: Location): Location? {
        if (currentLocation.latitude == 0.0 && currentLocation.longitude == 0.0) return null

        val last = lastValidLocation ?: run {
            lastValidLocation = Location(currentLocation)
            return currentLocation
        }

        val dist = last.distanceTo(currentLocation)
        val timeDeltaSeconds = (currentLocation.time - last.time) / 1000.0f
        if (timeDeltaSeconds > 0f) {
            val calculatedSpeed = dist / timeDeltaSeconds
            if (calculatedSpeed > maxAllowedSpeedMps) {
                if (last.speed == 0f && currentLocation.accuracy > 30f) {
                    return null
                }
            }
        }

        val speed = if (currentLocation.hasSpeed()) {
            currentLocation.speed
        } else {
            calculateFallbackSpeed(currentLocation, last)
        }

        if (speed < autoStopSpeedMps) {
            lowSpeedCounter++
            if (lowSpeedCounter >= SUSTAINED_STOP_TICKS) {
                val staticLocation = Location(last).apply {
                    time = currentLocation.time
                    if (currentLocation.hasAccuracy()) {
                        accuracy = currentLocation.accuracy
                    }
                    this.speed = 0.0f
                }
                lastValidLocation = staticLocation
                return staticLocation
            }
        } else {
            lowSpeedCounter = 0
        }

        lastValidLocation = Location(currentLocation)
        return currentLocation
    }

    private fun calculateFallbackSpeed(current: Location, last: Location): Float {
        val timeDeltaSeconds = (current.time - last.time) / 1000.0f
        if (timeDeltaSeconds <= 0f) return 0f

        val distance = current.distanceTo(last)
        return distance / timeDeltaSeconds
    }

    fun reset() {
        lastValidLocation = null
        lowSpeedCounter = 0
        hasRef = false
    }

    fun seedReferencePoint(lat: Double, lon: Double, timestamp: Long) {
        refLat = lat
        refLon = lon
        refTimestamp = timestamp
        refSpeed = 0f
        hasRef = true
        lowSpeedCounter = 0
    }

    fun evaluatePoint(lat: Double, lon: Double, accuracy: Float, timestamp: Long): Boolean {
        if (!hasRef) return true

        val dist = haversineDistance(refLat, refLon, lat, lon)
        val timeDeltaSeconds = (timestamp - refTimestamp) / 1000.0f

        if (timeDeltaSeconds > 0f) {
            val calculatedSpeed = dist / timeDeltaSeconds
            if (calculatedSpeed > maxAllowedSpeedMps) {
                return false
            }
        }
        return true
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}