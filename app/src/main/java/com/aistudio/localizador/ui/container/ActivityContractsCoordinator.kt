package com.aistudio.localizador.ui.container

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Coordinador de Contratos y Permisos de Actividad.
 * Encapsula el registro y la solicitud escalonada de permisos sensibles
 * (Ubicación precisa, segundo plano y reconocimiento de actividad física).
 */
class ActivityContractsCoordinator(
    private val activity: FragmentActivity
) {

    private val backgroundLocationLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(activity, "Rastreo persistente activado", Toast.LENGTH_SHORT).show()
            }
        }

    private val locationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                checkAndRequestBackgroundLocation()
            }
        }

    private val activityRecognitionPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(activity, "Reconocimiento de actividad activado para auto-arranque", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "El auto-arranque no funcionará sin este permiso", Toast.LENGTH_LONG).show()
            }
        }

    /**
     * Solicita permisos de forma escalonada al iniciar la aplicación.
     */
    fun checkAndRequestInitialPermissions() {
        // 1. Ubicación precisa en primer plano
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkAndRequestBackgroundLocation()
        }

        // 2. Reconocimiento de actividad física para auto-arranque (Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    /**
     * Solicita permiso de ubicación en segundo plano mostrando el rationale
     * correspondiente para cumplir con las políticas de Google Play.
     */
    fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                AlertDialog.Builder(activity)
                    .setTitle("Ubicación en segundo plano")
                    .setMessage("Para grabar tus rutas con la pantalla apagada, selecciona 'Permitir todo el tiempo' en la siguiente pantalla.")
                    .setPositiveButton("Configurar") { _, _ ->
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    .setNegativeButton("Ahora no", null)
                    .show()
            }
        }
    }
}
