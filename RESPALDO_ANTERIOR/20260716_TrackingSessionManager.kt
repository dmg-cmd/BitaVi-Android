package com.aistudio.localizador.location.manager

import android.util.Log
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.repository.RouteRepository
import java.util.ArrayDeque

class TrackingSessionManager(
    private val repository: RouteRepository
) {
    companion object {
        private const val MAX_PRE_RECORDING_SIZE = 60
        private const val TAG = "TrackingSessionManager"
    }

    private val preRecordingBuffer = ArrayDeque<BufferPoint>(MAX_PRE_RECORDING_SIZE)

    private var activeRouteId: Long? = null
    private var totalDistanceMeters: Double = 0.0
    private val activePoints = mutableListOf<LocationPoint>()
    private var lastProcessedPoint: BufferPoint? = null

    data class BufferPoint(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val speed: Float,
        val altitude: Double,
        val timestamp: Long
    )

    fun handlePreRecording(point: BufferPoint) {
        if (activeRouteId != null) return

        synchronized(preRecordingBuffer) {
            if (preRecordingBuffer.size >= MAX_PRE_RECORDING_SIZE) {
                preRecordingBuffer.pollFirst()
            }
            preRecordingBuffer.addLast(point)
        }
    }

    fun startSession(routeId: Long) {
        activeRouteId = routeId
        totalDistanceMeters = 0.0
        activePoints.clear()
        lastProcessedPoint = null

        synchronized(preRecordingBuffer) {
            Log.d(TAG, "Iniciando sesion de tracking para ruta $routeId. Volcando ${preRecordingBuffer.size} puntos.")
            preRecordingBuffer.forEach { bufferPoint ->
                addPointToActiveSession(bufferPoint)
            }
            preRecordingBuffer.clear()
        }
    }

    fun stopSession(): List<LocationPoint> {
        val pointsToSave = activePoints.toList()
        activeRouteId = null
        totalDistanceMeters = 0.0
        activePoints.clear()
        lastProcessedPoint = null
        return pointsToSave
    }

    fun addPointToActiveSession(point: BufferPoint) {
        val currentRouteId = activeRouteId ?: return

        lastProcessedPoint?.let { last ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                last.latitude, last.longitude,
                point.latitude, point.longitude,
                results
            )
            totalDistanceMeters += results[0].toDouble()
        }

        lastProcessedPoint = point

        val dbPoint = LocationPoint(
            id = 0,
            routeId = currentRouteId.toInt(),
            latitude = point.latitude,
            longitude = point.longitude,

            speed = point.speed,
            altitude = point.altitude,
            timestamp = point.timestamp
        )
        activePoints.add(dbPoint)
    }

        fun addDistanceFromStep(stepMeters: Double) {
        totalDistanceMeters += stepMeters
    }

    fun setInitialDistance(distance: Double) {
        totalDistanceMeters = distance
    }

    fun clearPreRecordingBuffer() {
        synchronized(preRecordingBuffer) {
            preRecordingBuffer.clear()
        }
    }

    fun getPreRecordingBufferSnapshot(): List<BufferPoint> {
        synchronized(preRecordingBuffer) {
            return preRecordingBuffer.toList()
        }
    }

    fun getActiveRouteId(): Long? = activeRouteId
    fun getTotalDistance(): Double = totalDistanceMeters
    fun getActivePointsCount(): Int = activePoints.size
}
