package com.aistudio.localizador.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import com.aistudio.localizador.location.LocationService

class LocationServiceManager(private val context: Context) {

    fun startPassiveService() {
        val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_START_PASSIVE }
        context.startService(intent)
    }

    fun startTracking(routeId: Int, name: String, intervalMillis: Long, distanceMeters: Float) {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra(LocationService.EXTRA_ROUTE_NAME, name)
            putExtra(LocationService.EXTRA_ROUTE_ID, routeId)
            putExtra(LocationService.EXTRA_UPDATE_INTERVAL, intervalMillis)
            putExtra(LocationService.EXTRA_UPDATE_DISTANCE, distanceMeters)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }

    fun stopTracking() {
        val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_STOP }
        context.startService(intent)
    }

    fun updateSettings() {
        val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_UPDATE_SETTINGS }
        context.startService(intent)
    }
}
