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
            if (location.accuracy < 20f) {
                lastStopLocation = location
                movementSustainedStartTime = 0L
                Log.d(TAG, "Anclaje inicial establecido para velocidad sostenida: Acc=${location.accuracy}")
            }
        } else {
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
