package com.aistudio.localizador.location.manager

import android.location.Location
import android.util.Log
import com.aistudio.localizador.util.AppConstants
import javax.inject.Inject

class AutoTrackingManager @Inject constructor() {

    interface AutoTrackingListener {
        fun onMovementSustainedTriggered(snapshot: List<TrackingSessionManager.BufferPoint>)
    }

    private var listener: AutoTrackingListener? = null
    private var lastStopLocation: Location? = null
    private var movementSustainedStartTime: Long = 0L
    private var lastAutoStartTimeMillis: Long = 0L

    fun setListener(listener: AutoTrackingListener) {
        this.listener = listener
    }

    fun processMovementSustained(
        location: Location,
        computedSpeedMps: Float,
        isAutoModeActive: Boolean,
        isTracking: Boolean,
        sessionManager: TrackingSessionManager
    ) {
        if (!isAutoModeActive || isTracking) return

        val now = System.currentTimeMillis()

        val anchoredAccuracy = location.accuracy
        if (anchoredAccuracy < AppConstants.Location.MAX_GPS_ACCURACY_METERS) {
            val bp = TrackingSessionManager.BufferPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = anchoredAccuracy,
                speed = location.speed,
                altitude = location.altitude,
                timestamp = location.time
            )
            sessionManager.handlePreRecording(bp)
        }

        if (now - lastAutoStartTimeMillis < 30000) return

        if (lastStopLocation == null) {
            if (location.accuracy <= AppConstants.Location.TOLERANT_GPS_ACCURACY_METERS) {
                lastStopLocation = location
                movementSustainedStartTime = 0L
                Log.d(TAG, "Anclaje inicial establecido para velocidad sostenida: Acc=${location.accuracy}")
            }
        } else {
            // Disparador 1: Alejamiento radial respecto al punto de detención
            val distanceFromStop = lastStopLocation?.let { stopLoc ->
                calculateDistanceMeters(stopLoc.latitude, stopLoc.longitude, location.latitude, location.longitude)
            } ?: 0.0

            val isDistanceTriggered = distanceFromStop >= AppConstants.Location.MOVEMENT_TRIGGER_THRESHOLD_METERS &&
                    location.accuracy <= AppConstants.Location.TOLERANT_GPS_ACCURACY_METERS

            if (isDistanceTriggered) {
                Log.i(TAG, "Alejamiento detectado: ${distanceFromStop.toInt()}m del anclaje de parada. Disparando arranque automático.")
                lastStopLocation = null
                movementSustainedStartTime = 0L
                lastAutoStartTimeMillis = now
                val snapshot = sessionManager.getPreRecordingBufferSnapshot()
                listener?.onMovementSustainedTriggered(snapshot)
                return
            }

            // Disparador 2: Velocidad sostenida
            val speedKmh = computedSpeedMps * 3.6f
            if (speedKmh > AppConstants.Location.AUTO_START_MIN_SPEED_KMH && location.accuracy < 30f) {
                if (movementSustainedStartTime == 0L) {
                    movementSustainedStartTime = now
                    Log.d(TAG, "Inicio de ventana de velocidad sostenida: $speedKmh km/h")
                } else {
                    val sustained = now - movementSustainedStartTime
                    if (sustained >= AppConstants.Location.AUTO_START_SUSTAINED_MS) {
                        Log.d(TAG, "Movimiento sostenido ${sustained}ms. Iniciando pipeline de arranque pasivo...")
                        lastStopLocation = null
                        movementSustainedStartTime = 0L
                        lastAutoStartTimeMillis = now
                        val snapshot = sessionManager.getPreRecordingBufferSnapshot()
                        listener?.onMovementSustainedTriggered(snapshot)
                    }
                }
            } else {
                if (movementSustainedStartTime != 0L) {
                    Log.d(TAG, "Velocidad insuficiente ($speedKmh km/h). Reiniciando ventana.")
                }
                movementSustainedStartTime = 0L
            }
        }
    }

    private fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return 6371000.0 * c
    }

    fun setAutoRestartAnchor(location: Location?) {
        lastStopLocation = location
    }

    fun resetMovementTracking() {
        movementSustainedStartTime = 0L
        lastStopLocation = null
    }

    fun clearAll() {
        resetMovementTracking()
        lastAutoStartTimeMillis = 0L
        listener = null
    }

    companion object {
        private const val TAG = "Bitaru_AutoTrack"
    }
}
