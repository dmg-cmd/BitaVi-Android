package com.aistudio.localizador.location.delegate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.location.manager.ServiceNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegado de notificaciones del servicio de ubicacion.
 * Envuelve ServiceNotificationManager y centraliza la notificacion de seguimiento
 * y las alertas de proximidad ("Cerca de mi").
 */
@Singleton
class LocationNotificationDelegate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val NOTIFICATION_ID = ServiceNotificationManager.NOTIFICATION_ID
        const val CHANNEL_ID = ServiceNotificationManager.CHANNEL_ID
        private const val PROXIMITY_CHANNEL_ID = "proximity_alerts"
        private const val PROXIMITY_NOTIFICATION_OFFSET = 2000
    }

    private val serviceNotificationManager = ServiceNotificationManager(context)

    private fun notificationManager(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** Construye la notificacion de servicio (crea el canal si hace falta). */
    fun buildNotification(title: String, content: String): Notification =
        serviceNotificationManager.buildNotification(title, content)

    /** Publica/actualiza la notificacion del servicio en primer plano. */
    fun notifyTracking(title: String, content: String) {
        notificationManager().notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    /** Alerta de proximidad a un favorito ("Cerca de tu Estrella"). */
    fun sendProximityNotification(favorite: SavedLocation) {
        val nm = notificationManager()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(PROXIMITY_CHANNEL_ID) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(
                    PROXIMITY_CHANNEL_ID,
                    "Alertas de Proximidad",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val notification = NotificationCompat.Builder(context, PROXIMITY_CHANNEL_ID)
            .setContentTitle("Cerca de tu Estrella")
            .setContentText("Estas pasando por: ${favorite.name}")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(favorite.id + PROXIMITY_NOTIFICATION_OFFSET, notification)
    }
}