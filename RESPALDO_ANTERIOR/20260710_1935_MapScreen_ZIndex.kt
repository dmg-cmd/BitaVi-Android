package com.example.ui.screens

import com.example.ui.SegmentoRuta
import com.example.ui.toRouteSegments
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable
import java.util.Locale
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.location.ActivityMode
import com.example.location.LocationService
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.TourOverlay
import com.example.ui.components.BrandWatermark
import com.example.ui.components.TopBarLocalizador
import com.example.ui.components.DashboardActividadFlotante
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.ui.model.IconCategory
import com.example.ui.RouteViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

@Composable
fun MapScreen(viewModel: RouteViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isTracking by viewModel.isTrackingLive.collectAsState()
    val latestLocation by viewModel.latestLocationLive.collectAsState()
    val elapsedSeconds by viewModel.elapsedSecondsLive.collectAsState()
    val distanceMeters by viewModel.distanceMetersLive.collectAsState()
    val activityMode by viewModel.currentActivityMode.collectAsState()
    val routePoints by viewModel.activeRoutePoints.collectAsState()
    val waypoints by viewModel.activeRouteWaypoints.collectAsState()
    val savedLocations by viewModel.allSavedLocations.collectAsState()
    val scope = rememberCoroutineScope()


    var showStartDialog by remember { mutableStateOf(false) }
    var showWaypointDialog by remember { mutableStateOf(false) }
    var showSaveLocationDialog by remember { mutableStateOf(false) }
    var showAddressBox by remember { mutableStateOf(false) }
    var newRouteName by remember { mutableStateOf("") }
    var clickedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Control de Mapa: Seguimiento vs Manual
    var lastInteractionTime by remember { mutableStateOf(0L) }
    var isNavigationModeEnabled by remember { mutableStateOf(false) }
    var mapOrientation by remember { mutableFloatStateOf(0f) }

    var capturedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var photoGpsLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var usePhotoGps by remember { mutableStateOf(false) }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    var showTourMapa by remember { mutableStateOf(!viewModel.esTourVisto("actividad")) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            capturedPhotoUri = null
        } else {
            capturedPhotoUri?.let { photoGpsLocation = viewModel.getPhotoLocation(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val internalUri = viewModel.saveImageToInternalStorage(it)
                capturedPhotoUri = internalUri
                photoGpsLocation = viewModel.getPhotoLocation(it)
                usePhotoGps = photoGpsLocation != null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.startPassiveLocationService()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, el usuario tendrá que pulsar el botón de nuevo
            Toast.makeText(context, "Permiso de cámara concedido. Pulse de nuevo para tomar la foto.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara para sacar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    val mapView = remember { MapView(context) }
    val locationOverlay = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapView) }
    val rotationGestureOverlay = remember { RotationGestureOverlay(mapView) }.apply {
        isEnabled = true
    }

    // --- CACHES PARA OPTIMIZACIÓN DE RENDERIZADO (Hallazgo A11) ---
    val markerIconCache = remember { mutableMapOf<String, android.graphics.drawable.Drawable>() }
    val waypointMarkers = remember { mutableMapOf<Int, Marker>() }
    val favoriteMarkers = remember { mutableMapOf<Int, Marker>() }
    val routePolylines = remember { mutableListOf<Polyline>() }
    
    val prevSegmentModes = remember { mutableListOf<ActivityMode>() }
    var prevTotalPoints by remember { mutableStateOf(0) }
    val circleIcon = remember(isTracking) {
        val size = 48
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val fillColor = if (isTracking) android.graphics.Color.RED else "#1B5E20".toColorInt()
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }.run { canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, this) }
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = android.graphics.Color.WHITE
        }.run { canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, this) }
        bitmap
    }
    val getCachedIcon = { resId: Int, colorHex: String ->
        val key = "$resId-$colorHex"
        markerIconCache.getOrPut(key) {
            val d = ContextCompat.getDrawable(context, resId)
                ?: error("resId")
            d.mutate().apply {
                setTint(colorHex.toColorInt())
            }
        }
    }
    // --------------------------------------------------------------

    // Centrar en la ubicación predeterminada y pedir permisos
    LaunchedEffect(Unit) {
        if (viewModel.mapTargetLocation == null) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(GeoPoint(viewModel.defaultLatitude, viewModel.defaultLongitude))
        }
        
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
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
            // BLOQUEO DE AUTO-CENTRADO
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

    // Efecto para centrar en un favorito seleccionado desde la lista
    LaunchedEffect(viewModel.mapTargetLocation) {
        viewModel.mapTargetLocation?.let { target ->
            // BLOQUEO ULTRA-FUERTE: Impedimos que el GPS tome el control
            lastInteractionTime = System.currentTimeMillis()
            
            // Desactivar explícitamente el auto-seguimiento antes de iniciar la animación de cámara
            locationOverlay.disableFollowLocation()
            
            // Animación al favorito con zoom cercano y mayor duración
            mapView.controller.animateTo(target, 18.0, 1500L)
            
            // NO limpiamos el target inmediatamente. Dejamos que el mapa se asiente.
            kotlinx.coroutines.delay(10000L) // 10 segundos
            viewModel.clearMapTarget()
        }
    }

    // Timer para resetear el modo manual después de 15 segundos de inactividad
    LaunchedEffect(lastInteractionTime) {
        if (lastInteractionTime > 0) {
            kotlinx.coroutines.delay(15000L) // 15 segundos de espera
            lastInteractionTime = 0L // Volver a modo seguimiento automático
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                lastInteractionTime = System.currentTimeMillis()
                LocationService.reportUserActivity()
            })
        }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    
                    // Configuración visual robusta
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(15.0)
                    
                    // Configuración de gestos
                    rotationGestureOverlay.isEnabled = true
                    overlays.add(rotationGestureOverlay)
                    
                    // Asegurar que el overlay de ubicación dibuje el radio de precisión (Hallazgo A11)
                    locationOverlay.isDrawAccuracyEnabled = true
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)

                    // Evento de toque prolongado para agregar hito
                    overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            return false
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            lastInteractionTime = System.currentTimeMillis()
                            p?.let {
                                clickedGeoPoint = it
                                showSaveLocationDialog = true
                            }
                            return true
                        }
                    }))

                    // Escuchar cambios de zoom del usuario
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            mapOrientation = mapView.mapOrientation
                            return false
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            lastInteractionTime = System.currentTimeMillis()
                            mapOrientation = mapView.mapOrientation
                            return false
                        }
                    })
                }
            },
            update = { view ->
                try {
                    view.setUseDataConnection(!viewModel.isOfflineMode)
                    
                    if (mapOrientation != view.mapOrientation) {
                        mapOrientation = view.mapOrientation
                    }
                    
                    val isManual = lastInteractionTime != 0L
                    val isNavigating = viewModel.mapTargetLocation != null
                    if (isManual || isNavigating) {
                        locationOverlay.disableFollowLocation()
                    } else {
                        locationOverlay.enableFollowLocation()
                    }

                    // --- MIGRACIÓN DE CONEXIÓN DE TRAZADO (Hallazgo A11) ---
                    // Actualizar trazado de la ruta activa utilizando la caché optimizada
                    val totalPoints = routePoints.size
                    if (totalPoints > 0) {
                        val segmentModes = routePoints.toRouteSegments()
                        
                        // Si cambió la cantidad de puntos o los modos de los segmentos, redibujamos
                        if (totalPoints != prevTotalPoints || segmentModes.map { it.mode } != prevSegmentModes) {
                            // Limpiar polilíneas anteriores del mapa
                            routePolylines.forEach { view.overlays.remove(it) }
                            routePolylines.clear()

                            // Dibujar las nuevas polilíneas por tramos coloreados
                            segmentModes.forEach { segment ->
                                val polyline = Polyline(view).apply {
                                    setPoints(segment.points)
                                    outlinePaint.color = segment.mode.toColorInt()
                                    outlinePaint.strokeWidth = viewModel.mapStrokeWidth
                                    // Configurar un estilo de unión redondeado y tapas circulares
                                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                                }
                                view.overlays.add(polyline)
                                routePolylines.add(polyline)
                            }
                            
                            prevSegmentModes.clear()
                            prevSegmentModes.addAll(segmentModes.map { it.mode })
                            prevTotalPoints = totalPoints
                            needsInvalidate = true
                        }
                    } else {
                        if (routePolylines.isNotEmpty()) {
                            routePolylines.forEach { view.overlays.remove(it) }
                            routePolylines.clear()
                            prevSegmentModes.clear()
                            prevTotalPoints = 0
                            needsInvalidate = true
                        }
                    }

                    // Renderizar los waypoints (hitos de parada durante el viaje)
                    var needsInvalidate = false
                    
                    // Remover del mapa los marcadores que ya no están en la lista
                    val currentWaypointIds = waypoints.map { it.id }.toSet()
                    val removedWaypointIds = waypointMarkers.keys.filter { it !in currentWaypointIds }
                    removedWaypointIds.forEach { id ->
                        view.overlays.remove(waypointMarkers[id])
                        waypointMarkers.remove(id)
                        needsInvalidate = true
                    }

                    waypoints.forEach { wp ->
                        var isNew = false
                        val marker = waypointMarkers.getOrPut(wp.id) {
                            isNew = true
                            Marker(view).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { m, v ->
                                    lastInteractionTime = System.currentTimeMillis()
                                    v.controller.animateTo(m.position)
                                    m.showInfoWindow()
                                    true
                                }
                                view.overlays.add(this)
                            }
                        }
                        val newPos = GeoPoint(wp.latitude, wp.longitude)
                        if (isNew || marker.position != newPos || marker.title != wp.name) {
                            marker.position = newPos
                            marker.title = wp.name
                            marker.snippet = wp.description
                            marker.icon = getCachedIcon(com.example.R.drawable.ic_waypoint, wp.color)
                            needsInvalidate = true
                        }
                    }

                    // Renderizar los favoritos en el mapa
                    val currentSavedIds = savedLocations.map { it.id }.toSet()
                    val removedFavIds = favoriteMarkers.keys.filter { it !in currentSavedIds }
                    removedFavIds.forEach { id ->
                        view.overlays.remove(favoriteMarkers[id])
                        needsInvalidate = true
                        false
                    } else true
                    }

                    val res = context.resources

                    savedLocations.forEach { loc ->
                        var isNew = false
                        val marker = favoriteMarkers.getOrPut(loc.id) {
                            isNew = true
                            Marker(view).apply {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { m, v ->
                                    lastInteractionTime = System.currentTimeMillis()
                                    v.controller.animateTo(m.position)
                                    m.showInfoWindow()
                                    true
                                }
                                view.overlays.add(this)
                            }
                        }
                        val newPos = GeoPoint(loc.latitude, loc.longitude)
                        if (isNew || marker.position != newPos || marker.title != loc.name) {
                            marker.position = newPos
                            marker.title = loc.name
                            marker.snippet = loc.address
                            marker.icon = IconCategory.fromKey(loc.iconCategory).toBitmap(40, loc.color.toColorInt()).toDrawable(res)
                            needsInvalidate = true
                        }
                    }

                    if (needsInvalidate) view.invalidate()
                } catch (_: Exception) { }
            }
        )

        // 🛰️ CAPA NIVEL 1: Pila de control unificada superior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            // 1. Barra de identidad e interactividad táctica superior
            val modoTraficoText = when (activityMode) {
                ActivityMode.STOPPED -> "🛑 Detenido"
                ActivityMode.WALKING -> "🚶 Caminata"
                ActivityMode.BIKING -> "🚴 Bicicleta"
                ActivityMode.DRIVING -> "🚗 Vehículo"
                ActivityMode.SIN_SENAL -> "⚠️ SIN SEÑAL"
            }

            TopBarLocalizador(
                onLocalizadorClick = {
                    if (!showAddressBox) {
                        viewModel.fetchCurrentAddress()
                        showAddressBox = true
                    } else {
                        showAddressBox = false
                        viewModel.clearCurrentAddress()
                    }
                },
                modoTrafico = modoTraficoText,
                velocidad = "${String.format(Locale.getDefault(), "%.1f", (latestLocation?.speed ?: 0f) * 3.6f)} km/h"
            )

            if (showAddressBox && viewModel.currentAddress != null) {
                // 2. Separación sutil e indicador de calle
                Spacer(modifier = Modifier.height(4.dp))
                IndicadorTranslucidoContenedor(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            showAddressBox = false
                            viewModel.clearCurrentAddress()
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = viewModel.currentAddress!!,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Botones flotantes abajo a la izquierda (Alineados verticalmente en columna)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isMapRotated = Math.abs(mapOrientation) > 0.1f
            if (isMapRotated) {
                FloatingActionButton(
                    onClick = {
                        mapView.mapOrientation = 0f
                        mapOrientation = 0f
                        isNavigationModeEnabled = false
                    },
                    containerColor = Color(0xFF3498DB), // 🔥 Azul Satelital Homologado
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.CompassCalibration, contentDescription = "Fijar Norte")
                }
            }

            FloatingActionButton(
                onClick = { 
                    lastInteractionTime = 0L
                    viewModel.clearMapTarget()
                    locationOverlay.enableFollowLocation()
                    latestLocation?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude)) } 
                },
                containerColor = Color(0xFF3498DB), // 🔥 Azul Satelital Homologado
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) { Icon(Icons.Default.MyLocation, contentDescription = "Volver a posición") }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp, end = 16.dp)) {
            if (!isTracking) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.generateSuggestedRouteName { newRouteName = it; showStartDialog = true } },
                    containerColor = Color(0xFF2ECC71), // 🔥 Verde Esmeralda de Grabación Activa
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grabar", fontWeight = FontWeight.Bold)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FloatingActionButton(onClick = { showWaypointDialog = true }) { Icon(Icons.Default.AddLocationAlt, null) }
                    FloatingActionButton(onClick = { viewModel.stopTrackingRoute() }, containerColor = MaterialTheme.colorScheme.error) { Icon(Icons.Default.Stop, null) }
                }
            }
        }
    }
}
