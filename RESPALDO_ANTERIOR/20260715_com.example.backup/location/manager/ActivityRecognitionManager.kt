package com.example.location.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.location.receiver.ActivityTransitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionManager(private val context: Context) {

    companion object {
        private const val REQUEST_CODE = 4004
        private const val TAG = "ACTIVITY_REC_MANAGER"
    }

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun buildTransitionRequest(): ActivityTransitionRequest {
        val transitions = mutableListOf<ActivityTransition>()

        val targetActivities = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
            DetectedActivity.STILL
        )

        for (activity in targetActivities) {
            transitions.add(ActivityTransition.Builder()
                .setActivityType(activity)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build())
            transitions.add(ActivityTransition.Builder()
                .setActivityType(activity)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build())
        }

        return ActivityTransitionRequest(transitions)
    }

    @SuppressLint("MissingPermission")
    fun startTrackingTransitions() {
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(buildTransitionRequest(), pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "Suscripcion exitosa al arranque automatico por transiciones.")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al suscribirse al reconocimiento de actividad: ${e.message}")
            }
    }

    @SuppressLint("MissingPermission")
    fun stopTrackingTransitions() {
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "Remocion de actualizaciones automaticas completada en limpio.")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fallo al remover el monitoreo de transiciones: ${e.message}")
            }
    }
}
