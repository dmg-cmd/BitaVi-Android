package com.aistudio.localizador.location.manager

import android.location.Location

import com.aistudio.localizador.location.manager.TrackingSessionManager.BufferPoint

class GpsFilterPipeline(
    private val cinematicFilter: CinematicFilterManager,
    private val stationaryAnchor: StationaryAnchorManager
) {

    fun processLocation(rawPoint: BufferPoint): BufferPoint? {
        val isPointValid = cinematicFilter.evaluateAndProcess(
            lat = rawPoint.latitude,
            lon = rawPoint.longitude,
            accuracy = rawPoint.accuracy,
            timestamp = rawPoint.timestamp,
            speed = rawPoint.speed,
            altitudeMeters = rawPoint.altitude
        )

        if (!isPointValid) return null

        stationaryAnchor.processStationaryState(
            lat = rawPoint.latitude,
            lon = rawPoint.longitude,
            speed = rawPoint.speed
        )

        val resolvedCoordinate = stationaryAnchor.getResolvedCoordinate(
            lat = rawPoint.latitude,
            lon = rawPoint.longitude
        )

        return rawPoint.copy(
            latitude = resolvedCoordinate.first,
            longitude = resolvedCoordinate.second
        )
    }

    fun shouldTriggerAutoStop(): Boolean = cinematicFilter.isAutoStopActive()

    fun getCurrentProfile(): KineticProfile = cinematicFilter.getCurrentProfile()

    fun reset() {
        cinematicFilter.reset()
        stationaryAnchor.reset()
    }

    fun shouldForceStop(location: Location): Boolean = stationaryAnchor.shouldForceStop(location)
}
