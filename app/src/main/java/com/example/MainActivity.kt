package com.example

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.db.AppDatabase
import com.example.data.repository.RouteRepository
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.RouteViewModel
import com.example.ui.RouteViewModelFactory
import com.example.ui.navigation.NavRoutes
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.ExperienceMapScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.SavedLocationsScreen
import com.example.ui.dialogs.ApiKeyDialog
import com.example.ui.dialogs.DeleteAllConfirmDialog
import com.example.ui.dialogs.HelpDialog
import com.example.ui.dialogs.LocationDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppConstants
import com.example.util.BiometricCipherHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import java.util.UUID
import android.security.keystore.KeyPermanentlyInvalidatedException
import javax.crypto.Cipher

/**
 * Actividad Principal que inicializa la capa de base de datos local (Room) y sirve de contenedor
 * de navegación reactiva bajo Compose.
 */
class MainActivity : FragmentActivity() {

    private lateinit var encryptedPrefs: SharedPreferences

    // --- LÓGICA DE PERMISOS ESCALONADA (Hallazgo B03) ---
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Toast.makeText(this, "Rastreo persistente activado", Toast.LENGTH_SHORT).show()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndRequestBackgroundLocation()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                // Rationale para cumplir con las políticas de Google Play
                androidx.appcompat.app.AlertDialog.Builder(this)
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

    // --- GENERACIÓN DE CLAVE PARA CIFRADO (Hallazgo A01) ---
    private fun getOrCreateDbPassphrase(): ByteArray {
        val key = AppConstants.Preferences.DB_PASSPHRASE_KEY
        var pass = encryptedPrefs.getString(key, null)
        if (pass == null) {
            pass = UUID.randomUUID().toString()
            encryptedPrefs.edit { putString(key, pass) }
        }
        return pass.toByteArray()
    }

    // Función para lanzar Biometría con Cifrado Real (CryptoObject)
    fun authenticateWithCrypto(
        cipher: Cipher,
        title: String = "Autenticación Requerida",
        onSuccess: (Cipher) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    result.cryptoObject?.cipher?.let { authorizedCipher ->
                        onSuccess(authorizedCipher)
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Desbloquee la API Key para activar la IA")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun authenticateSimple(
        title: String = "Autenticación Requerida",
        subtitle: String = "Confirme su identidad",
        onSuccess: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Configuración necesaria para osmdroid ANTES de super.onCreate
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // --- INICIALIZACIÓN ROBUSTA DE ENCRYPTED SHARED PREFERENCES ---
        // 1. Cambiamos el nombre del archivo para forzar un contenedor limpio en disco
        val fileName = AppConstants.Preferences.ENCRYPTED_PREFS_FILE 
        encryptedPrefs = try {
            // Usamos un alias personalizado único para evadir el alias corrupto del hardware
            val masterKey = MasterKey.Builder(this, AppConstants.Preferences.MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                this,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Si el hardware falla, limpiamos de raíz tanto el archivo como el alias del chip
            deleteSharedPreferences(fileName)
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                keyStore.deleteEntry(AppConstants.Preferences.MASTER_KEY_ALIAS)
                keyStore.deleteEntry("_androidx_security_master_key_") // Limpieza del alias por defecto
            } catch (keystoreEx: Exception) {
                keystoreEx.printStackTrace()
            }
            
            // Reintento final limpio
            val masterKey = MasterKey.Builder(this, AppConstants.Preferences.MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                this,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        Configuration.getInstance().load(this, sharedPrefs)
        // User-Agent legítimo e identificable siguiendo las directrices de la política de OSM (Hallazgo A11)
        Configuration.getInstance().userAgentValue = "LocalizadorPro/1.0 (Android; contact: dev@example.com)"
        
        // Asegurar que osmdroid use la carpeta interna de la app para el caché (evita problemas de permisos)
        Configuration.getInstance().osmdroidTileCache = getExternalFilesDir(null) ?: filesDir
        
        super.onCreate(savedInstanceState)
        
        // Optimización de Caché Offline
        Configuration.getInstance().cacheMapTileCount = 500 // Más cuadros en memoria
        Configuration.getInstance().tileDownloadThreads = 5 // Descargas más rápidas
        // El caché en disco es automático en la carpeta de la app, 
        // pero podemos asegurar que sea grande (ej. 500MB)
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 500L * 1024 * 1024

        // Habilitar diseño de dibujo de borde a borde (Edge-to-Edge)
        enableEdgeToEdge()

        // Inicialización de Base de Datos con Cifrado SQLCipher (Hallazgo A01)
        val dbPassphrase = getOrCreateDbPassphrase()
        
        // --- MANEJO ROBUSTO DE APERTURA DE DB (Hallazgo A01 / Corregir Crash Code 26) ---
        val database = try {
            val db = AppDatabase.getDatabase(applicationContext, dbPassphrase)
            // Forzamos una operación de lectura simple para validar la clave inmediatamente
            db.openHelper.writableDatabase
            db
        } catch (_: Exception) {
            // Si la clave no coincide (ej. tras reset de Keystore) o el archivo está corrupto (Code 26)
            AppDatabase.destroyInstance()
            applicationContext.deleteDatabase("localizador_database")
            // Reintento con base de datos nueva
            AppDatabase.getDatabase(applicationContext, dbPassphrase)
        }

        val repository = RouteRepository(database.routeDao())

        // Inicializar ViewModel utilizando el factory personalizado
        val viewModel = ViewModelProvider(
            this,
            RouteViewModelFactory(application, repository)
        )[RouteViewModel::class.java]

        // Solicitar permisos de forma escalonada (Hallazgo B03)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            checkAndRequestBackgroundLocation()
        }

        // --- CARGA SEGURA DE API KEY (HARDWARE BACKED) con blindaje de ciclo de vida ---
        lifecycleScope.launch {
            lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
                val encryptedKey = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_KEY, null)
                val iv = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_IV_KEY, null)

                if (encryptedKey != null && iv != null) {
                    try {
                        val rawIv = Base64.decode(iv, Base64.DEFAULT)
                        val decryptCipher = try {
                            BiometricCipherHelper.getDecryptCipher(rawIv)
                        } catch (_: KeyPermanentlyInvalidatedException) {
                            Toast.makeText(this@MainActivity, "Seguridad: llave del hardware renovada. Reingrese su API Key.", Toast.LENGTH_LONG).show()
                            return@withStateAtLeast
                        }
                        authenticateWithCrypto(decryptCipher, "Inicio Seguro") { authorizedCipher ->
                            val decryptedKey = BiometricCipherHelper.decryptText(authorizedCipher, encryptedKey!!)
                            viewModel.updateGroqApiKey(decryptedKey)
                            Toast.makeText(this@MainActivity, "IA y Aplicación Desbloqueadas", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Error de seguridad: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else if (viewModel.isBiometricEnabled) {
                    authenticateSimple(
                        title = "Acceso Seguro",
                        subtitle = "Coloque su huella para abrir Localizador Pro"
                    ) {
                        // Éxito: la app sigue su curso normal
                    }
                }
            }
        }

        setContent {
            MyApplicationTheme {
                MainContainer(viewModel = viewModel, encryptedPrefs = encryptedPrefs)
            }
        }
    }
}

/**
 * Contenedor Maestro de la Interfaz.
 * Ofrece la barra de navegación integrada Material 3 con active-pills y transiciona orgánicamente las pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(viewModel: RouteViewModel, encryptedPrefs: SharedPreferences) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isBottomBarVisible = currentRoute in NavRoutes.bottomBarRoutes
    
    // Obtener versión de la app de forma segura
    val appVersion = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Diálogos de ajustes
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    
    var tempKey by remember { mutableStateOf(viewModel.groqApiKey ?: "") }
    var keyVisible by remember { mutableStateOf(false) }
    var isKeyUnlocked by remember { mutableStateOf(false) }

    // Estados para la ubicación predeterminada
    var tempLat by remember { mutableStateOf(viewModel.defaultLatitude.toString()) }
    var tempLon by remember { mutableStateOf(viewModel.defaultLongitude.toString()) }

    val mainActivity = context as MainActivity

    // Selector para GUARDAR el respaldo (ZIP) en la carpeta elegida (Google Drive, etc)
    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let { destinationUri ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val tempFile = viewModel.prepareBackupFile()
                        if (tempFile != null) {
                            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                                tempFile.inputStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Respaldo completado con éxito (incluye fotos)", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    )

    // Selector para RESTAURAR el respaldo (ZIP)
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { 
                viewModel.restoreFromBackup(it)
                Toast.makeText(context, "Restaurando datos y fotos...", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Auto-actualizar ubicación inicial si el GPS es preciso
    val latestLocation by viewModel.latestLocationLive.collectAsState()
    LaunchedEffect(latestLocation) {
        latestLocation?.let { loc ->
            if (loc.accuracy < 25f) {
                viewModel.updateDefaultLocationPersistently(loc.latitude, loc.longitude)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isBottomBarVisible,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Ajustes de Localizador", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Configuración de IA (Groq)") },
                    selected = false,
                    icon = { Icon(Icons.Default.VpnKey, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        
                        // Obtenemos los datos cifrados guardados
                        val encryptedKey = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_KEY, null)
                        val iv = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_IV_KEY, null)

                        if (encryptedKey != null && iv != null) {
                            try {
                                val decryptCipher = BiometricCipherHelper.getDecryptCipher(Base64.decode(iv, Base64.DEFAULT))
                                mainActivity.authenticateWithCrypto(decryptCipher, "Desbloquear API Key") { authorizedCipher ->
                                    tempKey = BiometricCipherHelper.decryptText(authorizedCipher, encryptedKey)
                                    isKeyUnlocked = true
                                    showApiKeyDialog = true
                                }
                            } catch (_: Exception) {
                                // Si la clave del Keystore fue invalidada (nuevas huellas) o error
                                Toast.makeText(context, "Error de hardware seguro. Reintroduzca la clave.", Toast.LENGTH_LONG).show()
                                tempKey = ""
                                isKeyUnlocked = true
                                showApiKeyDialog = true
                            }
                        } else {
                            // No hay clave guardada aún con este método
                            tempKey = ""
                            isKeyUnlocked = true
                            showApiKeyDialog = true
                        }
                    }
                )
                
                NavigationDrawerItem(
                    label = { Text("Ubicación de Inicio") },
                    selected = false,
                    icon = { Icon(Icons.Default.Home, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        showLocationDialog = true 
                    }
                )

                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Modo Avión")
                            Switch(checked = viewModel.isAirplaneMode, onCheckedChange = { viewModel.toggleAirplaneMode() })
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.AirplanemodeActive, null) },
                    onClick = { viewModel.toggleAirplaneMode() }
                )

                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Modo Automático")
                            Switch(checked = viewModel.isAutoMode, onCheckedChange = { 
                                viewModel.toggleAutoMode()
                            })
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.FlashOn, null) },
                    onClick = { 
                        viewModel.toggleAutoMode()
                    }
                )

                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Avisos \"Cerca de mí\"")
                            val isNearMeEnabled by viewModel.isNearMeEnabled.collectAsState()
                            Switch(checked = isNearMeEnabled, onCheckedChange = { 
                                viewModel.toggleNearMe()
                            })
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.NotificationsActive, null) },
                    onClick = { 
                        viewModel.toggleNearMe()
                    }
                )

                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Mapas Offline")
                            Switch(checked = viewModel.isOfflineMode, onCheckedChange = { viewModel.toggleOfflineMode() })
                        }
                    },
                    selected = false,
                    icon = { Icon(if (viewModel.isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudQueue, null) },
                    onClick = { viewModel.toggleOfflineMode() }
                )

                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Protección Biométrica")
                            Switch(checked = viewModel.isBiometricEnabled, onCheckedChange = { 
                                val newState = it
                                mainActivity.authenticateSimple { 
                                    viewModel.toggleBiometric(newState) 
                                }
                            })
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.Fingerprint, null) },
                    onClick = { 
                        val newState = !viewModel.isBiometricEnabled
                        mainActivity.authenticateSimple {
                            viewModel.toggleBiometric(newState)
                        }
                    }
                )

                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Respaldo en Drive / Carpeta") },
                    selected = false,
                    icon = { Icon(Icons.Default.CloudUpload, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                        saveBackupLauncher.launch("backup_$timestamp.zip")
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Restaurar Respaldo (ZIP)") },
                    selected = false,
                    icon = { Icon(Icons.Default.SettingsBackupRestore, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        restoreBackupLauncher.launch(arrayOf("application/zip"))
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Ayuda y Funcionamiento") },
                    selected = false,
                    icon = { Icon(Icons.Default.HelpOutline, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        showHelpDialog = true 
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Limpiar Todo el Historial") },
                    selected = false,
                    icon = { Icon(Icons.Default.DeleteSweep, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        mainActivity.authenticateSimple(subtitle = "Acción irreversible") {
                            showDeleteAllConfirm = true
                        }
                    }
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        // --- DIÁLOGOS DE AJUSTES ---
        
        if (showApiKeyDialog) {
            ApiKeyDialog(
                apiKey = tempKey,
                isKeyUnlocked = isKeyUnlocked,
                keyVisible = keyVisible,
                onApiKeyChange = { tempKey = it },
                onUnlockClick = {
                    val encryptedKey = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_KEY, null)
                    val iv = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_IV_KEY, null)
                    if (encryptedKey != null && iv != null) {
                        try {
                            val decryptCipher = BiometricCipherHelper.getDecryptCipher(Base64.decode(iv, Base64.DEFAULT))
                            mainActivity.authenticateWithCrypto(decryptCipher, "Desbloquear API Key") { authorizedCipher ->
                                tempKey = BiometricCipherHelper.decryptText(authorizedCipher, encryptedKey)
                                isKeyUnlocked = true
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "Error de hardware seguro. Reintroduzca la clave.", Toast.LENGTH_LONG).show()
                            tempKey = ""
                            isKeyUnlocked = true
                        }
                    } else {
                        tempKey = ""
                        isKeyUnlocked = true
                    }
                },
                onToggleVisibility = { keyVisible = !keyVisible },
                onSaveClick = {
                    try {
                        val encryptCipher = try {
                            BiometricCipherHelper.getEncryptCipher()
                        } catch (_: KeyPermanentlyInvalidatedException) {
                            BiometricCipherHelper.regenerateMasterKey()
                            BiometricCipherHelper.getEncryptCipher()
                        }
                        mainActivity.authenticateWithCrypto(encryptCipher, "Cifrar API Key") { authorizedCipher ->
                            val (iv, encrypted) = BiometricCipherHelper.encryptText(tempKey, authorizedCipher)
                            encryptedPrefs.edit {
                                putString(AppConstants.Preferences.GROQ_API_KEY_KEY, encrypted)
                                putString(AppConstants.Preferences.GROQ_API_KEY_IV_KEY, iv)
                            }
                            viewModel.updateGroqApiKey(tempKey)
                            showApiKeyDialog = false
                            isKeyUnlocked = false
                            Toast.makeText(context, "API Key guardada con éxito", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error fatal del Keystore: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = {
                    showApiKeyDialog = false
                    isKeyUnlocked = false
                    keyVisible = false
                }
            )
        }

        if (showDeleteAllConfirm) {
            DeleteAllConfirmDialog(
                onConfirm = { viewModel.clearAllAppData() },
                onDismiss = { showDeleteAllConfirm = false }
            )
        }

        if (showLocationDialog) {
            LocationDialog(
                lat = tempLat,
                lon = tempLon,
                onLatChange = { tempLat = it },
                onLonChange = { tempLon = it },
                onUseCurrentLocation = {
                    viewModel.latestLocationLive.value?.let {
                        tempLat = it.latitude.toString()
                        tempLon = it.longitude.toString()
                    }
                },
                onSave = {
                    val lat = tempLat.toDoubleOrNull() ?: -34.6037
                    val lon = tempLon.toDoubleOrNull() ?: -58.3816
                    viewModel.updateDefaultLocationPersistently(lat, lon)
                    showLocationDialog = false
                },
                onDismiss = { showLocationDialog = false }
            )
        }

        if (showHelpDialog) {
            HelpDialog(onDismiss = { showHelpDialog = false })
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (isBottomBarVisible) {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Localizador PRO (v $appVersion)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menú",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                // GPS Quality Tracker status capsule
                                val isTrackingActive by viewModel.isTrackingLive.collectAsState()
                                val hasSignal = latestLocation != null

                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = if (hasSignal) Color(0xFF1B5E20) else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hasSignal) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                                            contentDescription = null,
                                            tint = if (hasSignal) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (hasSignal) {
                                                "PRECISIÓN: ${latestLocation?.accuracy?.toInt() ?: 0}M"
                                            } else if (isTrackingActive) {
                                                "BUSCANDO..."
                                            } else {
                                                "GPS LISTO"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasSignal) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                    }
                }
            },
        bottomBar = {
            // Ocultamos la barra de navegación si el usuario se encuentra visualizando el detalle de un mapa estático
            if (isBottomBarVisible) {
                Column {
                    val isTrackingActive by viewModel.isTrackingLive.collectAsState()
                    if (isTrackingActive) {
                        // Foreground Service Active Notification Simulation bar from Design HTML
                        Surface(
                            color = Color(0xFF21005D),
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationSearching,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Servicio activo • Grabando recorrido...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "0.3 MB",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    NavigationBar {
                        // Item 1: Mapa / Localizador en tiempo real
                        NavigationBarItem(
                            selected = currentRoute == NavRoutes.TRACKING,
                            onClick = {
                                navController.navigate(NavRoutes.TRACKING) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == NavRoutes.TRACKING) Icons.Filled.Map else Icons.Outlined.Map,
                                    contentDescription = "Localizador en Vivo"
                                )
                            },
                            label = { Text("Mapa", fontWeight = FontWeight.SemiBold) }
                        )

                        // Item 2: Lugares Guardados (Favoritos)
                        NavigationBarItem(
                            selected = currentRoute == NavRoutes.SAVED_LOCATIONS,
                            onClick = {
                                navController.navigate(NavRoutes.SAVED_LOCATIONS) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == NavRoutes.SAVED_LOCATIONS) Icons.Filled.Place else Icons.Default.Place,
                                    contentDescription = "Lugares Guardados"
                                )
                            },
                            label = { Text("Favoritos", fontWeight = FontWeight.SemiBold) }
                        )

                        // Item 3: Historial de rutas
                        NavigationBarItem(
                            selected = currentRoute == NavRoutes.HISTORY,
                            onClick = {
                                navController.navigate(NavRoutes.HISTORY) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == NavRoutes.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "Historial de Rutas"
                                )
                            },
                            label = { Text("Historial", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.TRACKING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.TRACKING) {
                MapScreen(viewModel = viewModel)
            }
            composable(NavRoutes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onRouteClick = { routeId ->
                        navController.navigate(NavRoutes.detail(routeId))
                    },
                    onNavigateToTracking = {
                        navController.navigate(NavRoutes.TRACKING) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onImportRequested = { }
                )
            }
            composable(NavRoutes.SAVED_LOCATIONS) {
                SavedLocationsScreen(
                    viewModel = viewModel,
                    onNavigateToMap = {
                        navController.navigate(NavRoutes.TRACKING) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = NavRoutes.DETAIL,
                arguments = listOf(navArgument("routeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                DetailScreen(
                    viewModel = viewModel,
                    routeId = routeId,
                    onBack = { navController.popBackStack() },
                    onExperienceMap = { id -> navController.navigate(NavRoutes.experienceMap(id)) }
                )
            }
            composable(
                route = NavRoutes.EXPERIENCE_MAP,
                arguments = listOf(navArgument("routeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                ExperienceMapScreen(
                    viewModel = viewModel,
                    routeId = routeId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

/**
 * Componible auxiliar para compatibilidad de pruebas de compilación y capturas.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
