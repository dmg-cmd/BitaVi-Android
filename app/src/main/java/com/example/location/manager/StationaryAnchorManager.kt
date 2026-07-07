package com.example.location.manager

import android.location.Location
import android.util.Log

class StationaryAnchorManager(
    private val speedThresholdMps: Double = 0.25,
    private val timeThresholdMillis: Long = 120_000L,
    private val accuracyAnchorThreshold: Float = 20f
) {
    private var anchorLocation: Location? = null
    private var firstLowSpeedTimeMillis: Long? = null

    // Guarda de contingencia para interiores (Bloque B)
    private var lastValidGpsTimestamp: Long = 0L
    private var isForcedStopTriggered: Boolean = false
    private val accuracyThreshold: Float = 25.0f
    private val forceStopTimeoutMs: Long = 3 * 60 * 1000L

    fun processAnchor(currentLocation: Location): Location {
        val currentSpeed = if (currentLocation.hasSpeed()) currentLocation.speed.toDouble() else 0.0

        if (currentSpeed < speedThresholdMps) {
            if (firstLowSpeedTimeMillis == null) {
                firstLowSpeedTimeMillis = System.currentTimeMillis()
            } else {
                val durationStationary = System.currentTimeMillis() - firstLowSpeedTimeMillis!!

                if (durationStationary >= timeThresholdMillis && anchorLocation == null
                    && currentLocation.accuracy > accuracyAnchorThreshold) {
                    anchorLocation = currentLocation
                    Log.i("ANCHOR_MANAGER", "Auto-Stop: Anclaje fijado por rebote de señal.")
                }
            }
        } else {
            if (currentSpeed > speedThresholdMps * 2) {
                firstLowSpeedTimeMillis = null
                if (anchorLocation != null) {
                    Log.i("ANCHOR_MANAGER", "Inercia rota. Anclaje liberado.")
                    reset()
                }
            }
        }

        return anchorLocation?.let { anchor ->
            Location(currentLocation).apply {
                latitude = anchor.latitude
                longitude = anchor.longitude
                speed = 0f
            }
        } ?: currentLocation
    }

    fun shouldForceStop(location: Location): Boolean {
        val currentTime = System.currentTimeMillis()

        if (location.accuracy <= accuracyThreshold) {
            lastValidGpsTimestamp = currentTime
            isForcedStopTriggered = false
            return false
        }

        if (lastValidGpsTimestamp == 0L) lastValidGpsTimestamp = currentTime

        val durationDegraded = currentTime - lastValidGpsTimestamp
        if (durationDegraded >= forceStopTimeoutMs && !isForcedStopTriggered) {
            isForcedStopTriggered = true
            Log.i("ANCHOR_MANAGER", "Guarda de 3min: Forzando Parada Automatica por degradacion de senhal")
            return true
        }

        return false
    }

    fun reset() {
        anchorLocation = null
        firstLowSpeedTimeMillis = null
        lastValidGpsTimestamp = 0L
        isForcedStopTriggered = false
    }
}
