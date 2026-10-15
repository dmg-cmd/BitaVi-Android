package com.aistudio.localizador.location.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.aistudio.localizador.location.receiver.GeofenceDepartureReceiver
import com.aistudio.localizador.util.AppConstants
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Módulo satélite ultra ligero para detección autónoma de salidas en segundo plano.
 * Ancla una geocerca pasiva en el coprocesador de hardware del módem sin despertar la CPU
 * y con 0% de consumo parásito mientras el dispositivo reposa.
 */
@Singleton
class DepartureWatcherManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "DepartureWatcher"
        private const val PENDING_INTENT_REQUEST_CODE = 4005
    }

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(context)
    }

    private var isWatching = false
    private var anchorLatitude: Double = 0.0
    private var anchorLongitude: Double = 0.0

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceDepartureReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun startWatchingDeparture(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = AppConstants.Location.GEOFENCE_DEPARTURE_RADIUS_METERS
    ) {
        if (latitude == 0.0 && longitude == 0.0) {
            Log.w(TAG, "Coordenadas inválidas para anclar geocerca de salida.")
            return
        }

        anchorLatitude = latitude
        anchorLongitude = longitude

        val geofence = Geofence.Builder()
            .setRequestId(AppConstants.Location.GEOFENCE_DEPARTURE_ID)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(10000) // 10 segundos para respuesta ágil sin consumo
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    isWatching = true
                    Log.i(TAG, "Geocerca de salida activada exitosamente en [$latitude, $longitude] con radio de ${radiusMeters}m.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al activar geocerca de salida: ${e.message}", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Faltan permisos de ubicación de segundo plano para Geofence: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al registrar geocerca de salida: ${e.message}")
        }
    }

    fun stopWatching() {
        if (!isWatching) return
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    isWatching = false
                    Log.i(TAG, "Geocerca de salida removida en limpio.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Fallo al remover geocerca de salida: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error removiendo geocerca de salida: ${e.message}")
        } finally {
            isWatching = false
        }
    }

    fun isCurrentlyWatching(): Boolean = isWatching
    fun getAnchorCoordinates(): Pair<Double, Double> = Pair(anchorLatitude, anchorLongitude)
}
