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

        MapTopBar(
            activityMode = activityMode,
            kineticProfile = kineticProfile,
            latestLocation = latestLocation,
            showAddressBox = showAddressBox,
            currentAddress = viewModel.currentAddress,
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
}
