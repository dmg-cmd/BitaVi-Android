package com.example.location.manager

import android.location.Location
import android.util.Log

/**
 * Gestor de Anclaje Estacionario (Auto-Stop) v3.0
 * Mitiga el "paseo fantasma" por rebote de señal multicamino en interiores congelando
 * la telemetría si la velocidad decae y la precisión se degrada por debajo de los umbrales.
 */
class StationaryAnchorManager(
    private val speedThresholdMps: Double = 0.25,
    private val timeThresholdMillis: Long = 120_000L,
    private val accuracyAnchorThreshold: Float = 20f
) {
    private var anchorLocation: Location? = null
    private var firstLowSpeedTimeMillis: Long? = null

    /**
     * Evalúa y aplica el filtro de anclaje posicional.
     * @return Ubicación mitigada si el anclaje está activo, u original si está en tránsito legítimo.
     */
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
                    Log.i("ANCHOR_MANAGER", "Auto-Stop: Anclaje fijado en (, ) debido a rebote de señal.")
                }
            }
        } else {
            if (currentSpeed > speedThresholdMps * 2) {
                firstLowSpeedTimeMillis = null
                if (anchorLocation != null) {
                    Log.i("ANCHOR_MANAGER", "Inercia rota ( m/s). Anclaje liberado.")
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

    fun reset() {
        anchorLocation = null
        firstLowSpeedTimeMillis = null
    }
}