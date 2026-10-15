package com.aistudio.localizador.location.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.aistudio.localizador.location.LocationService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

/**
 * Receptor de eventos de geocercas pasivas.
 * Despierta legalmente el servicio de seguimiento con privilegios de excepción
 * otorgados por el subsistema de geofencing cuando el usuario abandona el punto de reposo.
 */
class GeofenceDepartureReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceDeparture"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Error en GeofencingEvent: $errorMessage (código: ${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.i(TAG, "¡Transición EXIT detectada! El usuario abandonó el perímetro de parada. Iniciando rastreo automático...")

            val serviceIntent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START_FROM_DEPARTURE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al iniciar LocationService desde Geofence exit: ${e.message}", e)
            }
        }
    }
}
