package com.example

import android.Manifest
import android.content.SharedPreferences
import android.content.ContextWrapper
import android.content.Context
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.zIndex
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Save
import com.example.ui.screens.RouteOptionsMenu
import com.example.ui.screens.RouteAnalyticsScreen
import java.text.SimpleDateFormat
import java.util.Date
import android.graphics.Bitmap
import android.content.Intent
import androidx.core.view.drawToBitmap
import java.util.Locale
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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.screens.FocusFavoriteScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.SavedLocationsScreen
import com.example.ui.dialogs.ApiKeyDialog
import com.example.ui.dialogs.DeleteAllConfirmDialog
import com.example.ui.DetailAction
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

    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Reconocimiento de actividad activado para auto-arranque", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "El auto-arranque no funcionara sin este permiso", Toast.LENGTH_LONG).show()
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
                .setUserAuthenticationRequired(false)
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
                .setUserAuthenticationRequired(false)
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
        Configuration.getInstance().userAgentValue = "Agenviaje/1.0 (Android; contact: dev@example.com)"
        
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


        // Solicitar permiso de reconocimiento de actividad fisica (API 29+) para auto-arranque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        // --- CARGA DIRECTA DE API KEY desde EncryptedSharedPreferences ---
        lifecycleScope.launch {
            lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
                val iv = encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_IV_KEY, null)
                val apiKey = if (iv == null) {
                    encryptedPrefs.getString(AppConstants.Preferences.GROQ_API_KEY_KEY, null)
                } else {
                    // Formato antiguo (cifrado biométrico): ignorar, forzar reingreso
                    encryptedPrefs.edit { remove(AppConstants.Preferences.GROQ_API_KEY_KEY) }
                    null
                }
                if (apiKey != null) {
                    viewModel.updateGroqApiKey(apiKey)
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
    var currentDetailRouteId by remember { mutableStateOf<Int?>(null) }
    var currentExperienceRouteId by remember { mutableStateOf<Int?>(null) }
    var currentRouteAnalyticsId by remember { mutableStateOf<Int?>(null) }
    var currentFocusFavoriteId by mutableIntStateOf(0)
    LaunchedEffect(currentRoute) {
        if (currentRoute !in NavRoutes.auxiliaryRoutes) {
            currentDetailRouteId = null
            currentExperienceRouteId = null
            currentRouteAnalyticsId = null
        }
    }
    
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

    // Estados para la ubicación predeterminada
    var tempLat by remember { mutableStateOf(viewModel.defaultLatitude.toString()) }
    var tempLon by remember { mutableStateOf(viewModel.defaultLongitude.toString()) }

    val mainActivity = context.findActivity()

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

    // Launcher for saving ZIP from ExperienceMap
    val experienceSaveZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null && currentExperienceRouteId != null) {
                viewModel.saveRouteZipToUri(currentExperienceRouteId!!, uri)
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
        gesturesEnabled = currentRoute in NavRoutes.baseRoutes,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                AgenviajeDrawerHeader()
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Configuración de IA (Groq)") },
                    selected = false,
                    icon = { Icon(Icons.Default.VpnKey, null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        tempKey = viewModel.groqApiKey ?: ""
                        showApiKeyDialog = true
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
                                mainActivity?.authenticateSimple { 
                                    viewModel.toggleBiometric(newState) 
                                }
                            })
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.Fingerprint, null) },
                    onClick = { 
                        val newState = !viewModel.isBiometricEnabled
                        mainActivity?.authenticateSimple {
                            viewModel.toggleBiometric(newState)
                        }
                    }
                )


                // ── Grosor del trazado ──
                Text(
                    "Grosor del trazado: ${viewModel.mapStrokeWidth.toInt()}px",
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = viewModel.mapStrokeWidth,
                    onValueChange = { viewModel.updateStrokeWidth(it) },
                    valueRange = 4f..14f,
                    steps = 5,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) },
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
                        mainActivity?.authenticateSimple(subtitle = "Acción irreversible") {
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
                keyVisible = keyVisible,
                onApiKeyChange = { tempKey = it },
                onToggleVisibility = { keyVisible = !keyVisible },
                onSaveClick = {
                    encryptedPrefs.edit {
                        putString(AppConstants.Preferences.GROQ_API_KEY_KEY, tempKey)
                        remove(AppConstants.Preferences.GROQ_API_KEY_IV_KEY)
                    }
                    viewModel.updateGroqApiKey(tempKey)
                    showApiKeyDialog = false
                    Toast.makeText(context, "API Key guardada con éxito", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showApiKeyDialog = false
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

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                // topBar eliminado — todo migrado a bottomBar unificada

        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1B1F))
            ) {
                // 🛰️ SUB-CAPA ALFA: BARRA DE SERVICIO ACTIVO
                val isTrackingByService by viewModel.isTrackingLive.collectAsState()
                if (isTrackingByService) {
                    Surface(
                        color = Color(0xFF2ECC71).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2ECC71), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Servicio activo • Grabando ruta en búnker",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2ECC71),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 🛰️ SUB-CAPA BETA: FILA FLOTANTE CONTEXTUAL (GPS + ACCIONES)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zona Izquierda: Botones contextuales de DETAIL / EXPERIENCE_MAP
                    Box(modifier = Modifier.weight(1f)) {
                        if (currentRoute == NavRoutes.DETAIL || currentRoute == NavRoutes.EXPERIENCE_MAP) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (currentRoute == NavRoutes.DETAIL) {
                                    IconButton(onClick = {
                                        currentDetailRouteId?.let { navController.navigate(NavRoutes.experienceMap(it)) }
                                    }) {
                                        Icon(Icons.Default.Explore, "Mapa de experiencia", tint = Color(0xFF7C4DFF))
                                    }
                                    IconButton(onClick = { viewModel.detailAction = DetailAction.ShowSheet }) {
                                        Icon(Icons.Default.BookmarkBorder, "Favoritos")
                                    }
                                    IconButton(onClick = {
                                        val name = viewModel.selectedDetailRoute.value?.name ?: ""
                                        viewModel.detailAction = DetailAction.ShowEditDialog(name)
                                    }) { Icon(Icons.Default.Edit, "Editar nombre") }
                                    val rid = currentDetailRouteId
                                    if (rid != null) {
                                        RouteOptionsMenu(
                                            onShare = { viewModel.exportRoute(rid) },
                                            onExportGpx = { viewModel.exportRoutesAsGPX(setOf(rid)) },
                                            onExportCsv = { viewModel.exportRoutesAsCSV(rid) },
                                            onExportZip = { viewModel.exportarRutaZip(rid) },
                                            onDelete = { viewModel.detailAction = DetailAction.ShowDeleteDialog },
                                            onHelp = { viewModel.detailAction = DetailAction.ShowHelpDialog }
                                        )
                                    }
                                }
                                if (currentRoute == NavRoutes.EXPERIENCE_MAP) {
                                    IconButton(onClick = {
                                        val a = context as? android.app.Activity ?: return@IconButton
                                        android.os.Handler(a.mainLooper).post {
                                            try {
                                                val bitmap = a.window.decorView.drawToBitmap()
                                                val cacheFile = java.io.File(context.cacheDir, "screenshot_exp.png")
                                                java.io.FileOutputStream(cacheFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                                val intent = android.content.Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                android.content.Intent.createChooser(intent, "Compartir pantalla").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).let(context::startActivity)
                                            } catch (e: Exception) { android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show() }
                                        }
                                    }) { Icon(Icons.Default.Share, "Compartir pantalla") }
                                    IconButton(onClick = {
                                        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        val routeName = viewModel.selectedDetailRoute.value?.name ?: "ruta"
                                        val sanitized = routeName.trim().replace(Regex("[\\s/\\\\?%*:|\"<>]"), "_")
                                        val exportName = "ROUTE_${currentExperienceRouteId ?: 0}_${sanitized}_$ts"
                                        experienceSaveZipLauncher.launch("$exportName.zip")
                                    }) { Icon(Icons.Default.Save, "Guardar como ZIP") }
                                }
                            }
                        }
                    }

                    // Zona Derecha: Cápsula GPS dinámica
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val gpsColor = if ((latestLocation?.accuracy ?: 100f) < 10f) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                            Box(modifier = Modifier.size(6.dp).background(gpsColor, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS: ${latestLocation?.accuracy?.let { "${it.toInt()}m" } ?: "Sin Señal"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 🛰️ SUB-CAPA GAMMA: NAVIGATION BAR MATRICIAL (4 SLOTS PUROS)
                NavigationBar(
                    containerColor = Color(0xFF1C1B1F),
                    contentColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // SLOT 1: MENÚ LATERAL / MAPA
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.TRACKING,
                        onClick = {
                            if (currentRoute != NavRoutes.TRACKING) {
                                navController.navigate(NavRoutes.TRACKING) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            } else {
                                scope.launch { drawerState.open() }
                            }
                        },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Menú / Mapa") },
                        label = { Text("Mapa") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF3498DB),
                            selectedTextColor = Color(0xFF3498DB),
                            unselectedIconColor = Color.White.copy(alpha = 0.60f),
                            indicatorColor = Color(0xFF3498DB).copy(alpha = 0.12f)
                        )
                    )
                    // SLOT 2: FAVORITOS
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.SAVED_LOCATIONS,
                        onClick = {
                            if (currentRoute != NavRoutes.SAVED_LOCATIONS) {
                                navController.navigate(NavRoutes.SAVED_LOCATIONS) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                        label = { Text("Favoritos") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2ECC71),
                            selectedTextColor = Color(0xFF2ECC71),
                            unselectedIconColor = Color.White.copy(alpha = 0.60f),
                            indicatorColor = Color(0xFF2ECC71).copy(alpha = 0.12f)
                        )
                    )
                    // SLOT 3: HISTORIAL TÁCTICO
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.HISTORY,
                        onClick = {
                            if (currentRoute != NavRoutes.HISTORY) {
                                navController.navigate(NavRoutes.HISTORY) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                        label = { Text("Historial") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF3498DB),
                            selectedTextColor = Color(0xFF3498DB),
                            unselectedIconColor = Color.White.copy(alpha = 0.60f),
                            indicatorColor = Color(0xFF3498DB).copy(alpha = 0.12f)
                        )
                    )
                    // SLOT 4: ASISTENCIA OPERATIVA
                    NavigationBarItem(
                        selected = false,
                        onClick = { showHelpDialog = true },
                        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Ayuda") },
                        label = { Text("Ayuda") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color(0xFFE74C3C),
                            unselectedTextColor = Color(0xFFE74C3C),
                            indicatorColor = Color(0xFFE74C3C).copy(alpha = 0.12f)
                        )
                    )
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
                    },
                    onFocusFavorite = { favoriteId ->
                        navController.navigate(NavRoutes.focusFavorite(favoriteId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = NavRoutes.DETAIL,
                arguments = listOf(navArgument("routeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                currentDetailRouteId = routeId
                DetailScreen(
                    viewModel = viewModel,
                    routeId = routeId,
                    onBack = { navController.popBackStack() },
                    onExperienceMap = { id -> navController.navigate(NavRoutes.experienceMap(id)) },
                    onAnalytics = { id -> navController.navigate(NavRoutes.routeAnalytics(id)) }
                )
            }
            composable(
                route = NavRoutes.EXPERIENCE_MAP,
                arguments = listOf(navArgument("routeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                currentExperienceRouteId = routeId
                ExperienceMapScreen(
                    viewModel = viewModel,
                    routeId = routeId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = NavRoutes.ROUTE_ANALYTICS,
                arguments = listOf(navArgument("routeId") { type = NavType.IntType })
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                currentRouteAnalyticsId = routeId
                RouteAnalyticsScreen(
                    viewModel = viewModel,
                    routeId = routeId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = NavRoutes.FOCUS_FAVORITE,
                arguments = listOf(navArgument("favoriteId") { type = NavType.IntType })
            ) { backStackEntry ->
                val favoriteId = backStackEntry.arguments?.getInt("favoriteId") ?: return@composable
                currentFocusFavoriteId = favoriteId
                FocusFavoriteScreen(
                    viewModel = viewModel,
                    favoriteId = favoriteId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
    }  // Cierra ModalNavigationDrawer content lambda
}  // Cierra composable principal


/**
 * Componible auxiliar para compatibilidad de pruebas de compilación y capturas.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun AgenviajeDrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = stringResource(id = R.string.drawer_title_header),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.drawer_subtitle_header),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Green.copy(alpha = 0.8f)
        )
    }
}


