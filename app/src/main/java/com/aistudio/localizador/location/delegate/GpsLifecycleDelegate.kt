package com.aistudio.localizador.location.delegate

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegado responsable del ciclo de vida del GPS: cliente fusionado, hilo dedicado,
 * callback de ubicaciones y el WakeLock que mantiene la CPU despierta.
 *
 * Politica de WakeLock (FASE 1.3):
 *  - Grabacion activa: la CPU permanece despierta SIN limite de tiempo, para que una
 *    grabacion larga (mas de 25 horas) jamas se corte por expiracion del WakeLock.
 *  - Modo pasivo o detenido: se libera de inmediato para permitir Doze y no gastar bateria.
 */
@Singleton
class GpsLifecycleDelegate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GpsLifecycleDelegate"
        private const val WAKE_LOCK_TAG = "Localizador:TrackingWakeLock"
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationHandlerThread: HandlerThread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var locationCallback: LocationCallback? = null

    private var onLocation: ((Location) -> Unit)? = null
    private var onPermissionError: (() -> Unit)? = null

    fun initialize(
        onLocation: (Location) -> Unit,
        onPermissionError: () -> Unit
    ) {
        this.onLocation = onLocation
        this.onPermissionError = onPermissionError

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
        }
        if (locationHandlerThread == null) {
            locationHandlerThread = HandlerThread("LocationHandlerThread").apply { start() }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.forEach { loc -> onLocation?.invoke(loc) }
            }
        }
    }

    /**
     * Solicita actualizaciones de ubicacion continuas.
     * @return true si la solicitud se registro; false si no hay permisos (error fatal).
     */
    fun requestLocationUpdates(interval: Long, distance: Float): Boolean {
        val client = fusedLocationClient ?: return false
        val callback = locationCallback ?: return false
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).apply {
            setMinUpdateIntervalMillis(interval / 2)
            setMinUpdateDistanceMeters(distance)
            setMaxUpdateDelayMillis(0)
        }.build()
        return try {
            client.requestLocationUpdates(
                locationRequest,
                callback,
                locationHandlerThread?.looper ?: Looper.getMainLooper()
            )
            true
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "No se admiten permisos necesarios para iniciar updates de ubicacion GPS: $unlikely")
            onPermissionError?.invoke()
            false
        }
    }

    fun removeLocationUpdates() {
        val client = fusedLocationClient ?: return
        val callback = locationCallback ?: return
        try {
            client.removeLocationUpdates(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Error removiendo updates de ubicacion: ${e.message}")
        }
    }

    /** Activa o desactiva la retencion del WakeLock segun el estado de la grabacion. */
    fun setTrackingActive(active: Boolean) {
        if (active) acquireWakeLock() else releaseWakeLock()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val wl = wakeLock ?: return
        if (!wl.isHeld) {
            // Sin timeout: la grabacion debe permanecer activa indefinidamente.
            wl.acquire()
            Log.i(TAG, "WakeLock adquirido SIN timeout (grabacion activa).")
        }
    }

    fun releaseWakeLock() {
        val wl = wakeLock ?: return
        if (wl.isHeld) {
            try {
                wl.release()
                Log.d(TAG, "WakeLock liberado.")
            } catch (e: Exception) {
                Log.e(TAG, "Error liberando WakeLock: ${e.message}")
            }
        }
    }

    fun isWakeLockHeld(): Boolean = wakeLock?.isHeld == true

    /** Libera todos los recursos del delegado. */
    fun shutdown() {
        removeLocationUpdates()
        releaseWakeLock()
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null
        locationCallback = null
        fusedLocationClient = null
        onLocation = null
        onPermissionError = null
    }
}