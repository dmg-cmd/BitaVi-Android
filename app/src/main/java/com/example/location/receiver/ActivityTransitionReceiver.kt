package com.example.location.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.location.LocationService
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult

class ActivityTransitionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ACTIVITY_RECEIVER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return

            for (event in result.transitionEvents) {
                Log.d(TAG, "Transicion: Tipo: ${event.activityType} | Cambio: ${event.transitionType}")

                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    evaluateActivityAndBridge(context, event.activityType)
                }
            }
        }
    }

    private fun evaluateActivityAndBridge(context: Context, activityType: Int) {
        if (activityType == com.google.android.gms.location.DetectedActivity.STILL) return

        Log.i(TAG, "Puenteando evento de movimiento detectado hacia LocationService.")
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_AUTO_START_FROM_TRANSITION
            putExtra(LocationService.EXTRA_DETECTED_ACTIVITY, activityType)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
