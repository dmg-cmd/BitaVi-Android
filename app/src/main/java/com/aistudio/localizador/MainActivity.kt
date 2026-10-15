package com.aistudio.localizador

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.preference.PreferenceManager
import com.aistudio.localizador.data.db.AppDatabase
import com.aistudio.localizador.domain.usecase.BiometricAuthUseCase
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.container.ActivityContractsCoordinator
import com.aistudio.localizador.ui.container.BiometricAuthHandler
import com.aistudio.localizador.ui.container.MainScaffoldContainer
import com.aistudio.localizador.ui.screens.BiometricLockScreen
import com.aistudio.localizador.ui.security.BiometricSessionManager
import com.aistudio.localizador.ui.theme.LocalizadorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import javax.inject.Inject

/**
 * Busca de forma recursiva y segura una instancia de MainActivity en la jerarquía de Context.
 */
fun Context.findActivity(): MainActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Actividad Principal delegada y modularizada.
 * Actúa como orquestador del ciclo de vida y anfitrión de Compose,
 * delegando la gestión de permisos en [ActivityContractsCoordinator],
 * la autenticación y cifrado en [BiometricAuthHandler] y la interfaz en [MainScaffoldContainer].
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: RouteViewModel by viewModels()

    @Inject
    lateinit var biometricAuthUseCase: BiometricAuthUseCase

    @Inject
    lateinit var biometricSessionManager: BiometricSessionManager

    private lateinit var contractsCoordinator: ActivityContractsCoordinator
    private lateinit var biometricAuthHandler: BiometricAuthHandler
    private lateinit var encryptedPrefs: SharedPreferences

    private var biometricErrorMessage by mutableStateOf<String?>(null)

    /**
     * Puente de autenticación biométrica simple para confirmaciones críticas en la UI.
     */
    fun authenticateSimple(
        title: String = "Autenticación Requerida",
        subtitle: String = "Confirme su identidad",
        onSuccess: () -> Unit
    ) {
        biometricAuthHandler.authenticateSimple(
            activity = this,
            title = title,
            subtitle = subtitle,
            onSuccess = onSuccess
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Configuración previa de osmdroid
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        Configuration.getInstance().load(this, sharedPrefs)
        Configuration.getInstance().userAgentValue = "Agenviaje/1.0 (Android; contact: dev@example.com)"
        Configuration.getInstance().osmdroidTileCache = getExternalFilesDir(null) ?: filesDir

        super.onCreate(savedInstanceState)

        // 2. Optimización de Caché Offline para mapas
        Configuration.getInstance().cacheMapTileCount = 500
        Configuration.getInstance().tileDownloadThreads = 5
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 500L * 1024 * 1024

        // 3. Diseño Edge-to-Edge
        enableEdgeToEdge()

        // 4. Inicialización de coordinadores satélites
        biometricAuthHandler = BiometricAuthHandler(this)
        contractsCoordinator = ActivityContractsCoordinator(this)
        encryptedPrefs = biometricAuthHandler.createEncryptedPreferences()

        // 5. Inicialización resiliente de base de datos con cifrado SQLCipher
        val dbPassphrase = biometricAuthHandler.getOrCreateDbPassphrase(encryptedPrefs)
        try {
            val db = AppDatabase.getInstance(applicationContext, dbPassphrase)
            db.openHelper.writableDatabase
        } catch (_: Exception) {
            AppDatabase.destroyInstance()
            applicationContext.deleteDatabase("localizador_database")
            AppDatabase.getInstance(applicationContext, dbPassphrase)
        }

        // 6. Solicitud escalonada de permisos
        contractsCoordinator.checkAndRequestInitialPermissions()

        // 7. Carga reactiva de API Key Groq desde almacenamiento cifrado
        lifecycleScope.launch {
            lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
                biometricAuthHandler.loadGroqApiKey(encryptedPrefs, viewModel)
            }
        }

        // 8. Renderizado del árbol Compose
        setContent {
            LocalizadorTheme {
                val isUnlocked by biometricSessionManager.isUnlocked.collectAsState()

                if (viewModel.isBiometricEnabled && !isUnlocked) {
                    BiometricLockScreen(
                        onRetryAuth = { triggerBiometricCheck() },
                        errorMessage = biometricErrorMessage
                    )
                } else {
                    MainScaffoldContainer(
                        viewModel = viewModel,
                        encryptedPrefs = encryptedPrefs,
                        biometricSessionManager = biometricSessionManager,
                        onAuthenticateSimple = { title, subtitle, onSuccess ->
                            authenticateSimple(title, subtitle, onSuccess)
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        biometricSessionManager.onAppForegrounded()
        triggerBiometricCheck()
    }

    override fun onStop() {
        super.onStop()
        biometricSessionManager.onAppBackgrounded()
    }

    private fun triggerBiometricCheck() {
        biometricAuthHandler.triggerBiometricCheck(
            activity = this,
            viewModel = viewModel,
            sessionManager = biometricSessionManager,
            authUseCase = biometricAuthUseCase,
            onErrorChanged = { biometricErrorMessage = it }
        )
    }
}

/**
 * Componible auxiliar para compatibilidad de pruebas de compilación y capturas.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
