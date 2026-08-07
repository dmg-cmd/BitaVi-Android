package com.aistudio.localizador.location.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aistudio.localizador.util.AlertNotifier
import com.aistudio.localizador.util.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProximityBleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    interface ProximityListener {
        fun onBleContactDetected(contactName: String, distanceMeters: Double)
    }

    private var listener: ProximityListener? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alertNotifier: AlertNotifier
    private var proximityManager: com.aistudio.localizador.location.ProximityManager? = null
    private var isBleActive = false
    private var lastBleNotificationTime: Long = 0L

    fun init(listener: ProximityListener, sharedPrefs: SharedPreferences) {
        this.listener = listener
        this.sharedPreferences = sharedPrefs
        this.alertNotifier = AlertNotifier(context)
        com.aistudio.localizador.location.ProximityManager(context) { contactName, _, distance ->
            handleContactDetected(contactName, distance)
        }.let { pm ->
            if (pm.init()) proximityManager = pm
        }
    }

    fun ensureBleProximity(
        isServiceTracking: Boolean,
        myPhoneHash: ByteArray? = null,
        contactHashes: Set<ByteArray> = emptySet()
    ) {
        val isNearMe = sharedPreferences.getBoolean("near_me_enabled", false)
        val isBleEnabled = sharedPreferences.getBoolean("ble_enabled", false)
        val pm = proximityManager ?: return

        if (isNearMe && isBleEnabled && pm.isBluetoothEnabled() && isServiceTracking) {
            if (!isBleActive) {
                isBleActive = true
                myPhoneHash?.let { pm.startAdvertising(it) }
                if (contactHashes.isNotEmpty()) {
                    pm.updateScanHashes(contactHashes)
                }
                pm.startScanning()
                Log.d("Bitaru_Ble", "BLE scanning iniciado con  hashes de contacto")
            }
        } else {
            if (isBleActive) {
                isBleActive = false
                pm.stop()
                Log.d("Bitaru_Ble", "BLE scanning detenido")
            }
        }
    }

    private fun handleContactDetected(contactName: String, distance: Double) {
        if (!isBleActive) return

        val now = System.currentTimeMillis()
        if (now - lastBleNotificationTime < AppConstants.Ble.NOTIFICATION_COOLDOWN_MILLIS) return
        lastBleNotificationTime = now

        alertNotifier.triggerProximityAlert()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "proximity_alerts", "Alertas de Proximidad",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "proximity_alerts")
            .setContentTitle("Contacto cercano")
            .setContentText(" esta a ~m")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(3001, notification)

        listener?.onBleContactDetected(contactName, distance)
    }

    fun clearAll() {
        proximityManager?.stop()
        if (::alertNotifier.isInitialized) alertNotifier.release()
        proximityManager = null
        listener = null
    }
}