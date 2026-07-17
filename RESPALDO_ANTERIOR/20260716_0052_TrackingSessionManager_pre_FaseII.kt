package com.aistudio.localizador.location.manager

import android.location.Location
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.location.ActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Duration.Companion.seconds

class TrackingSessionManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null

    // ─── Compose-observable state ─────────────────────────────────
    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _currentRouteId = MutableStateFlow<Int?>(null)
    val currentRouteId = _currentRouteId.asStateFlow()

    private val _latestLocation = MutableStateFlow<Location?>(null)
    val latestLocation = _latestLocation.asStateFlow()

    private val _currentActivityMode = MutableStateFlow(ActivityMode.STOPPED)
    val currentActivityMode = _currentActivityMode.asStateFlow()

    var elapsedTimeSeconds: Long = 0L
        private set
    var currentDistanceMeters: Double = 0.0
        private set

    // ─── Session fields ───────────────────────────────────────────
    var activeRouteId: Int = -1
        private set
    var activeRouteName: String = "Ruta"
        private set
    var totalDistanceMeters: Double = 0.0
        private set
    var lastValidLocation: Location? = null
    var startTimeMillis: Long = 0L
        private set
    var routeStartTimeMillis: Long = 0L
        private set
    var lastMovementTimeMillis: Long = System.currentTimeMillis()
    var lastRealMovementTime: Long = System.currentTimeMillis()

    // ─── Pre-recording buffer ─────────────────────────────────────
    val preRecordingBuffer = ConcurrentLinkedDeque<Location>()

    fun handlePreRecording(newLocation: Location) {
        preRecordingBuffer.addLast(newLocation)
        val newestTime = preRecordingBuffer.peekLast()?.time ?: return
        while (true) {
            val firstPoint = preRecordingBuffer.peekFirst() ?: break
            if ((newestTime - firstPoint.time) > PRE_RECORDING_BUFFER_DURATION_MILLIS) {
                preRecordingBuffer.removeFirst()
            } else {
                break
            }
        }
    }

    fun Location.toBufferPoint(routeId: Int): LocationPoint = LocationPoint(
        routeId = routeId,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        speed = this.speed,
        timestamp = if (this.time > 0) this.time else System.currentTimeMillis()
    )

    // ─── Session lifecycle ────────────────────────────────────────
    fun startSession(routeId: Int, routeName: String) {
        _isTracking.value = true
        activeRouteId = routeId
        _currentRouteId.value = routeId
        activeRouteName = routeName
        routeStartTimeMillis = System.currentTimeMillis()
        lastMovementTimeMillis = System.currentTimeMillis()
        preRecordingBuffer.clear()
    }

    fun startTimer(
        onInactivityDetected: () -> Unit,
        onDailyRotationNeeded: () -> Unit,
        onGpsSilent: () -> Unit
    ) {
        timerJob?.cancel()
        startTimeMillis = System.currentTimeMillis() - (elapsedTimeSeconds * 1000L)
        timerJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = (now - startTimeMillis) / 1000L
                elapsedTimeSeconds = elapsed
                onInactivityDetected()
                onDailyRotationNeeded()
                onGpsSilent()
                delay(1.seconds)
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun endSession() {
        _isTracking.value = false
        activeRouteId = -1
        _currentRouteId.value = null
        _latestLocation.value = _latestLocation.value?.let { loc ->
            Location(loc).apply { speed = 0f }
        }
        elapsedTimeSeconds = 0L
        currentDistanceMeters = 0.0
        totalDistanceMeters = 0.0
    }

    // ─── Odometer ─────────────────────────────────────────────────
    fun addDistanceStep(distanceStep: Float, accuracy: Float, maxAccuracy: Float, tolerantAccuracy: Float): Boolean {
        if (accuracy <= tolerantAccuracy) {
            if (distanceStep > 0) {
                totalDistanceMeters += distanceStep
                currentDistanceMeters = totalDistanceMeters
                return true
            }
        }
        return false
    }

    fun setInitialDistance(distance: Double) {
        totalDistanceMeters = distance
        currentDistanceMeters = distance
    }

    // ─── Location ─────────────────────────────────────────────────
    fun updateLatestLocation(location: Location) {
        _latestLocation.value = location
        lastMovementTimeMillis = System.currentTimeMillis()
    }

    fun updateLastRealMovementTime() {
        lastRealMovementTime = System.currentTimeMillis()
    }

    // ─── Activity mode ────────────────────────────────────────────
    fun updateActivityMode(mode: ActivityMode) {
        _currentActivityMode.value = mode
    }

    // ─── Cleanup ──────────────────────────────────────────────────
    fun destroy() {
        timerJob?.cancel()
        scope.cancel()
    }

    companion object {
        private const val PRE_RECORDING_BUFFER_DURATION_MILLIS = 60_000L // 60 segundos
    }
}