package com.aistudio.localizador.ui.container

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aistudio.localizador.domain.usecase.BiometricAuthUseCase
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.security.BiometricSessionManager
import com.aistudio.localizador.util.AppConstants
import java.security.KeyStore
import java.util.UUID

/**
 * Gestor de Autenticación Biométrica y Criptografía de Preferencias.
 * Encapsula la orquestación de BiometricPrompt, el ciclo de vida de la sesión biométrica,
 * la inicialización tolerante a fallos de EncryptedSharedPreferences y el almacenamiento de claves.
 */
class BiometricAuthHandler(
    private val context: Context
) {

    /**
     * Inicializa EncryptedSharedPreferences de manera robusta,
     * recuperándose de posibles corrupciones de alias en el Keystore del dispositivo.
     */
    fun createEncryptedPreferences(): SharedPreferences {
        val fileName = AppConstants.Preferences.ENCRYPTED_PREFS_FILE
        return try {
            val masterKey = MasterKey.Builder(context, AppConstants.Preferences.MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(false)
                .build()

            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.deleteSharedPreferences(fileName)
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                keyStore.deleteEntry(AppConstants.Preferences.MASTER_KEY_ALIAS)
                keyStore.deleteEntry("_androidx_security_master_key_")
            } catch (keystoreEx: Exception) {
                keystoreEx.printStackTrace()
            }

            val masterKey = MasterKey.Builder(context, AppConstants.Preferences.MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(false)
                .build()

            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * Obtiene o genera la frase de paso aleatoria para la base de datos SQLCipher.
     */
    fun getOrCreateDbPassphrase(encryptedPrefs: SharedPreferences): ByteArray {
        val key = AppConstants.Preferences.DB_PASSPHRASE_KEY
        var pass = encryptedPrefs.getString(key, null)
        if (pass == null) {
            pass = UUID.randomUUID().toString()
            encryptedPrefs.edit { putString(key, pass) }
        }
        return pass.toByteArray()
    }

    /**
     * Lanza un diálogo de autenticación biométrica simple para confirmar operaciones sensibles.
     */
    fun authenticateSimple(
        activity: FragmentActivity,
        title: String = "Autenticación Requerida",
        subtitle: String = "Confirme su identidad",
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val message = "Error: $errString"
                    onError?.invoke(message) ?: Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Evalúa y ejecuta la verificación biométrica al volver la aplicación al primer plano.
     */
    fun triggerBiometricCheck(
        activity: FragmentActivity,
        viewModel: RouteViewModel,
        sessionManager: BiometricSessionManager,
        authUseCase: BiometricAuthUseCase,
        onErrorChanged: (String?) -> Unit
    ) {
        if (!viewModel.isBiometricEnabled || sessionManager.isUnlocked.value) return

        if (!authUseCase.canAuthenticate(activity)) {
            sessionManager.onAuthenticationSuccess()
            return
        }

        authUseCase.authenticate(
            activity = activity,
            onSuccess = {
                onErrorChanged(null)
                sessionManager.onAuthenticationSuccess()
            },
            onError = { error ->
                onErrorChanged(error)
                sessionManager.lockExplicitly()
            }
        )
    }

    /**
     * Carga de forma segura la clave de Groq almacenada en EncryptedSharedPreferences.
     */
    fun loadGroqApiKey(encryptedPrefs: SharedPreferences, viewModel: RouteViewModel) {
        val iv = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_IV_KEY, null)
        val apiKey = if (iv == null) {
            encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_KEY, null)
        } else {
            encryptedPrefs.edit { remove(AppConstants.Preferences.GROQ_API_KEY_KEY) }
            null
        }
        if (apiKey != null) {
            viewModel.updateGroqApiKey(apiKey)
        }
    }

    /**
     * Guarda la clave de Groq de forma segura en EncryptedSharedPreferences.
     */
    fun saveGroqApiKey(encryptedPrefs: SharedPreferences, viewModel: RouteViewModel, newKey: String) {
        encryptedPrefs.edit {
            putString(AppConstants.Preferences.GROQ_API_KEY_KEY, newKey)
            remove(AppConstants.Preferences.GROQ_API_KEY_IV_KEY)
        }
        viewModel.updateGroqApiKey(newKey)
    }
}
