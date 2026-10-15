package com.aistudio.localizador.ui.container

import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.localizador.R
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.UiEvent
import com.aistudio.localizador.ui.dialogs.ApiKeyDialog
import com.aistudio.localizador.ui.dialogs.DeleteAllConfirmDialog
import com.aistudio.localizador.ui.dialogs.HelpDialog
import com.aistudio.localizador.ui.dialogs.LocationDialog
import com.aistudio.localizador.ui.navigation.NavRoutes
import com.aistudio.localizador.ui.screens.DetailScreen
import com.aistudio.localizador.ui.screens.DynamicHistoryBottomBar
import com.aistudio.localizador.ui.screens.ExperienceMapScreen
import com.aistudio.localizador.ui.screens.FocusFavoriteScreen
import com.aistudio.localizador.ui.screens.HistoryScreen
import com.aistudio.localizador.ui.screens.MapScreen
import com.aistudio.localizador.ui.screens.RouteAnalyticsScreen
import com.aistudio.localizador.ui.screens.SavedLocationsScreen
import com.aistudio.localizador.ui.security.BiometricSessionManager
import com.aistudio.localizador.util.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Contenedor Composable Principal (Scaffold y Navegación).
 * Aloja el Drawer lateral táctico, los diálogos modales de configuración,
 * la barra inferior trifásica unificada y el grafo de navegación reactivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldContainer(
    viewModel: RouteViewModel,
    encryptedPrefs: SharedPreferences,
    biometricSessionManager: BiometricSessionManager? = null,
    onAuthenticateSimple: ((title: String, subtitle: String, onSuccess: () -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var currentDetailRouteId by remember { mutableStateOf<Int?>(null) }
    var currentExperienceRouteId by remember { mutableStateOf<Int?>(null) }
    var currentRouteAnalyticsId by remember { mutableStateOf<Int?>(null) }
    var currentFocusFavoriteId by remember { mutableIntStateOf(0) }

    val selectedRouteIds by viewModel.selectedRouteIds.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()

    LaunchedEffect(currentRoute) {
        if (currentRoute !in NavRoutes.auxiliaryRoutes) {
            currentDetailRouteId = null
            currentExperienceRouteId = null
            currentRouteAnalyticsId = null
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareIntentTrigger.collect { intent ->
            context.startActivity(intent)
        }
    }

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

    // Selector para GUARDAR el respaldo (ZIP) en la carpeta elegida
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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

    // Selector para importar GPX desde DynamicHistoryBottomBar
    val gpxHistoryImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importGpxFile(it) }
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

    val requestAuth: (title: String, subtitle: String, onSuccess: () -> Unit) -> Unit = { title, subtitle, onSuccess ->
        if (onAuthenticateSimple != null) {
            onAuthenticateSimple(title, subtitle, onSuccess)
        } else {
            onSuccess()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Modo Automático")
                            Switch(
                                checked = viewModel.isAutoMode,
                                onCheckedChange = { viewModel.toggleAutoMode() }
                            )
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.FlashOn, null) },
                    onClick = { viewModel.toggleAutoMode() }
                )

                NavigationDrawerItem(
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Avisos \"Cerca de mí\"")
                            val isNearMeEnabled by viewModel.isNearMeEnabled.collectAsState()
                            Switch(
                                checked = isNearMeEnabled,
                                onCheckedChange = { viewModel.toggleNearMe() }
                            )
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.NotificationsActive, null) },
                    onClick = { viewModel.toggleNearMe() }
                )

                NavigationDrawerItem(
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mapas Offline")
                            Switch(
                                checked = viewModel.isOfflineMode,
                                onCheckedChange = { viewModel.toggleOfflineMode() }
                            )
                        }
                    },
                    selected = false,
                    icon = { Icon(if (viewModel.isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudQueue, null) },
                    onClick = { viewModel.toggleOfflineMode() }
                )

                NavigationDrawerItem(
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Protección Biométrica")
                            Switch(
                                checked = viewModel.isBiometricEnabled,
                                onCheckedChange = { newState ->
                                    requestAuth("Autenticación Requerida", "Confirme su identidad") {
                                        viewModel.toggleBiometric(newState)
                                        if (newState) {
                                            biometricSessionManager?.lockExplicitly()
                                        } else {
                                            biometricSessionManager?.onAuthenticationSuccess()
                                        }
                                    }
                                }
                            )
                        }
                    },
                    selected = false,
                    icon = { Icon(Icons.Default.Fingerprint, null) },
                    onClick = {
                        val newState = !viewModel.isBiometricEnabled
                        requestAuth("Autenticación Requerida", "Confirme su identidad") {
                            viewModel.toggleBiometric(newState)
                            if (newState) {
                                biometricSessionManager?.lockExplicitly()
                            } else {
                                biometricSessionManager?.onAuthenticationSuccess()
                            }
                        }
                    }
                )

                // Grosor del trazado
                Text(
                    text = "Grosor del trazado: ${viewModel.mapStrokeWidth.toInt()}px",
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
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
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
                        requestAuth("Autenticación Requerida", "Acción irreversible") {
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
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                            Box(modifier = Modifier.weight(1f))

                            if (currentRoute == NavRoutes.HISTORY) {
                                DynamicHistoryBottomBar(
                                    selectedCount = selectedRouteIds.size,
                                    gpsAccuracyText = "GPS: " + (latestLocation?.accuracy?.let { "${it.toInt()}m" } ?: "Sin Senal"),
                                    onExportAll = {
                                        val ids = allRoutes.map { it.id }.toSet()
                                        viewModel.exportRoutesAsGPX(ids)
                                    },
                                    onDeleteAll = { showDeleteAllConfirm = true },
                                    onCloseScreen = {
                                        navController.navigate(NavRoutes.TRACKING) {
                                            popUpTo(NavRoutes.TRACKING) { inclusive = true }
                                        }
                                    },
                                    onExportSelected = { viewModel.exportRoutesAsGPX(selectedRouteIds) },
                                    onDeleteSelected = {
                                        viewModel.deleteRoutes(selectedRouteIds)
                                        viewModel.clearSelection()
                                    },
                                    onClearSelection = { viewModel.clearSelection() },
                                    onCollapseAllClick = { viewModel.collapseAllGroups() },
                                    onExpandAllClick = { viewModel.expandAllGroups() },
                                    onImport = { gpxHistoryImportLauncher.launch(arrayOf("*/*")) }
                                )
                            } else if (currentRoute != NavRoutes.EXPERIENCE_MAP) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val gpsColor = if ((latestLocation?.accuracy ?: 100f) < 10f) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(gpsColor, CircleShape)
                                        )
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
                                            launchSingleTop = true
                                            restoreState = true
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
                                            launchSingleTop = true
                                            restoreState = true
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
                                            launchSingleTop = true
                                            restoreState = true
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
                            }
                        )
                    }
                    composable(
                        route = NavRoutes.DETAIL,
                        arguments = listOf(navArgument("routeId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                        LaunchedEffect(routeId) {
                            viewModel.selectRouteForDetail(routeId)
                        }
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
                        viewModel.prepareExperience(routeId)
                        ExperienceMapScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel,
                            routeId = routeId
                        )
                    }
                    composable(
                        route = NavRoutes.ROUTE_ANALYTICS,
                        arguments = listOf(navArgument("routeId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val routeId = backStackEntry.arguments?.getInt("routeId") ?: return@composable
                        currentRouteAnalyticsId = routeId
                        RouteAnalyticsScreen(
                            viewModel = viewModel
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
    }
}

/**
 * Función composable de compatibilidad directa para preservar llamadas preexistentes a MainContainer.
 */
@Composable
fun MainContainer(
    viewModel: RouteViewModel,
    encryptedPrefs: SharedPreferences
) {
    MainScaffoldContainer(
        viewModel = viewModel,
        encryptedPrefs = encryptedPrefs
    )
}

/**
 * Cabecera estilizada del cajón de navegación (Drawer).
 */
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

/**
 * Composable auxiliar para compatibilidad de pruebas de compilación y capturas.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
