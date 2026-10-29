package com.aistudio.localizador.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.aistudio.localizador.location.LocationService
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.components.MapLayersSelectorDialog
import com.aistudio.localizador.ui.components.TourOverlay
import com.aistudio.localizador.ui.screens.map.MapCanvas
import com.aistudio.localizador.ui.screens.map.MapFloatingControls
import com.aistudio.localizador.ui.screens.map.MapSaveLocationDialog
import com.aistudio.localizador.ui.screens.map.MapTopBar
import com.aistudio.localizador.ui.screens.map.StartRouteDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.localizador.ui.screens.map.WaypointDialog
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.time.Duration.Companion.seconds

@Composable
fun MapScreen(
    viewModel: RouteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTrackingLive.collectAsState()
    val latestLocation by viewModel.latestLocationLive.collectAsState()
    val activityMode by viewModel.currentActivityMode.collectAsState()
    val kineticProfile by viewModel.kineticProfile.collectAsState()
    val routePoints by viewModel.activeRoutePoints.collectAsState()
    val waypoints by viewModel.activeRouteWaypoints.collectAsState()
    val savedLocations by viewModel.allSavedLocations.collectAsState()
    val currentMapLayer by viewModel.currentMapLayer.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }
    var showWaypointDialog by remember { mutableStateOf(false) }
    var showSaveLocationDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showAddressBox by remember { mutableStateOf(false) }
    var showAlphaDialog by remember { mutableStateOf(false) }
    var newRouteName by remember { mutableStateOf("") }
    var clickedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showTourMapa by remember { mutableStateOf(!viewModel.esTourVisto("actividad")) }

    var lastInteractionTime by remember { mutableStateOf(0L) }
    var isNavigationModeEnabled by remember { mutableStateOf(false) }
    var mapOrientation by remember { mutableFloatStateOf(0f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (viewModel.isAutoMode) {
                viewModel.startPassiveLocationService()
            }
        }
    }

    val mapView = remember { MapView(context) }
    val locationOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }
    val rotationGestureOverlay = remember { RotationGestureOverlay(mapView) }.apply {
        isEnabled = true
    }

    LaunchedEffect(Unit) {
        if (viewModel.mapTargetLocation == null) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(GeoPoint(viewModel.defaultLatitude, viewModel.defaultLongitude))
        }

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(latestLocation) {
        latestLocation?.let { loc ->
            val isManual = lastInteractionTime != 0L
            val isNavigating = viewModel.mapTargetLocation != null

            if (isManual || isNavigating) {
                locationOverlay.disableFollowLocation()
            } else {
                locationOverlay.enableFollowLocation()
                val currentZoom = if (mapView.zoomLevelDouble < 5.0) 17.0 else mapView.zoomLevelDouble
                mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude), currentZoom, 800L)
            }
        }
    }

    LaunchedEffect(viewModel.mapTargetLocation) {
        viewModel.mapTargetLocation?.let { target ->
            lastInteractionTime = System.currentTimeMillis()
            locationOverlay.disableFollowLocation()
            mapView.controller.animateTo(target, 18.0, 1500L)
            delay(10.seconds)
            viewModel.clearMapTarget()
        }
    }

    LaunchedEffect(lastInteractionTime) {
        if (lastInteractionTime > 0) {
            delay(15.seconds)
            lastInteractionTime = 0L
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    lastInteractionTime = System.currentTimeMillis()
                    LocationService.reportUserActivity()
                })
            }
    ) {
        MapCanvas(
            viewModel = viewModel,
            mapView = mapView,
            locationOverlay = locationOverlay,
            rotationGestureOverlay = rotationGestureOverlay,
            isTracking = isTracking,
            latestLocation = latestLocation,
            routePoints = routePoints,
            waypoints = waypoints,
            savedLocations = savedLocations,
            currentMapLayer = currentMapLayer,
            mapOrientation = mapOrientation,
            isNavigationModeEnabled = isNavigationModeEnabled,
            lastInteractionTime = lastInteractionTime,
            onMapOrientationChange = { mapOrientation = it },
            onUserInteraction = { lastInteractionTime = System.currentTimeMillis() },
            onLongPress = { p ->
                clickedGeoPoint = p
                viewModel.fetchAddressForLocation(p.latitude, p.longitude)
                showSaveLocationDialog = true
            }
        )

        val elapsedTimeSeconds by viewModel.elapsedTimeSecondsLive.collectAsState()
        val distanceFromFirstPoint by viewModel.distanceFromFirstPointMeters.collectAsState()
        val alturaText = if (latestLocation != null && latestLocation!!.hasAltitude()) "${latestLocation!!.altitude.toInt()} m" else "-- m"
        val distanciaText = viewModel.formatDistanceMeters(distanceFromFirstPoint)
        val tiempoText = viewModel.formatElapsedTimeHours(elapsedTimeSeconds)

        MapTopBar(
            activityMode = activityMode,
            kineticProfile = kineticProfile,
            latestLocation = latestLocation,
            showAddressBox = showAddressBox,
            currentAddress = viewModel.currentAddress,
            headerAlpha = viewModel.headerAlpha,
            onAdjustAlphaClick = { showAlphaDialog = true },
            altura = alturaText,
            distanciaPrimerPunto = distanciaText,
            tiempoFuncionamiento = tiempoText,
            onLocalizadorClick = {
                if (!showAddressBox) {
                    viewModel.fetchCurrentAddress()
                    showAddressBox = true
                } else {
                    showAddressBox = false
                    viewModel.clearCurrentAddress()
                }
            },
            onAddressClick = {
                showAddressBox = false
                viewModel.clearCurrentAddress()
            }
        )

        MapFloatingControls(
            viewModel = viewModel,
            isTracking = isTracking,
            mapOrientation = mapOrientation,
            onFixNorth = {
                mapView.mapOrientation = 0f
                mapOrientation = 0f
                isNavigationModeEnabled = false
            },
            onShowLayers = { showLayersDialog = true },
            onRecenter = {
                lastInteractionTime = 0L
                viewModel.clearMapTarget()
                locationOverlay.enableFollowLocation()
                latestLocation?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude)) }
            },
            onOpenStartDialog = {
                viewModel.generateSuggestedRouteName { newRouteName = it; showStartDialog = true }
            },
            onOpenWaypointDialog = { showWaypointDialog = true }
        )
    }

    if (showStartDialog) {
        StartRouteDialog(
            newRouteName = newRouteName,
            onNameChange = { newRouteName = it },
            updateIntervalSeconds = viewModel.updateIntervalSeconds,
            onIntervalChange = { viewModel.updateIntervalSeconds = it },
            onConfirm = {
                viewModel.startTrackingRoute(newRouteName)
                showStartDialog = false
            },
            onDismiss = { showStartDialog = false }
        )
    }

    if (showWaypointDialog) {
        WaypointDialog(
            viewModel = viewModel,
            onDismiss = { showWaypointDialog = false }
        )
    }

    if (showSaveLocationDialog) {
        MapSaveLocationDialog(
            viewModel = viewModel,
            clickedGeoPoint = clickedGeoPoint,
            onDismiss = { showSaveLocationDialog = false }
        )
    }

    if (showTourMapa) {
        TourOverlay(
            message = "¡Ahora detecto si caminas, vas en bici o en coche automáticamente!",
            onDismiss = {
                showTourMapa = false
                viewModel.marcarTourVisto("actividad")
            }
        )
    }

    if (showLayersDialog) {
        MapLayersSelectorDialog(
            currentLayer = currentMapLayer,
            availableLayers = viewModel.getAvailableMapLayers(),
            onLayerSelected = { viewModel.setMapLayer(it) },
            onDismiss = { showLayersDialog = false }
        )
    }

    if (showAlphaDialog) {
        AlertDialog(
            onDismissRequest = { showAlphaDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Atenuador de Ventana Superior",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Regula la transparencia del fondo de la ventana de control para visualizar el mapa subyacente con la atenuación deseada:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Opacidad actual:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(viewModel.headerAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = viewModel.headerAlpha,
                        onValueChange = { viewModel.updateHeaderAlpha(it) },
                        valueRange = 0.15f..0.85f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = viewModel.headerAlpha <= 0.28f,
                            onClick = { viewModel.updateHeaderAlpha(0.25f) },
                            label = { Text("Sutil (25%)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = viewModel.headerAlpha in 0.29f..0.55f,
                            onClick = { viewModel.updateHeaderAlpha(0.45f) },
                            label = { Text("Equilibrado (45%)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = viewModel.headerAlpha > 0.55f,
                            onClick = { viewModel.updateHeaderAlpha(0.70f) },
                            label = { Text("Contraste (70%)", fontSize = 11.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAlphaDialog = false }) {
                    Text("Listo")
                }
            }
        )
    }
}
