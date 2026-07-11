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
                    setMultiTouchControls(true)
                    setOnTouchListener { v, event ->
                        v.performClick()
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    
                    // Si venimos con un objetivo, centrar inmediatamente en el factory
                    viewModel.mapTargetLocation?.let { target ->
                        controller.setZoom(18.0)
                        controller.setCenter(target)
                        lastInteractionTime = System.currentTimeMillis()
                    } ?: run {
                        controller.setZoom(15.0)
                    }
                    
                    // Personalizar el icono de ubicación (Flecha Roja para mejor visibilidad)
                    locationOverlay.enableMyLocation()
                    locationOverlay.setDrawAccuracyEnabled(true)
                    
                    // CORRECCIÓN DE ORIENTACIÓN: Habilitamos el seguimiento de orientación (brújula)
                    // para que el icono gire según la dirección del movimiento o del dispositivo.
                    // Nota: Si el usuario toca el mapa, el auto-centrado se gestiona con lastInteractionTime.
                    if (viewModel.mapTargetLocation == null) {
                        locationOverlay.enableFollowLocation()
                    } else {
                        locationOverlay.disableFollowLocation()
                    }
                    
                    // Cambiamos el color de la flecha de dirección a Rojo para que se vea bien
                    val arrowIcon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.next)
                    arrowIcon?.setTint(android.graphics.Color.RED)
                    // Nota: setDirectionArrow requiere Bitmaps, lo haremos de forma simple con el icono estándar pero resaltado
                    
                    overlays.add(locationOverlay)
                    overlays.add(rotationGestureOverlay)
                    
                    // Escuchar cambios de zoom del usuario
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            // Detectamos el scroll pero no bloqueamos el hilo
                            return false
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            lastInteractionTime = System.currentTimeMillis()
                            return false
                        }
                    })

                    overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            p?.let { 
                                clickedGeoPoint = it
                                viewModel.fetchAddressForLocation(it.latitude, it.longitude)
                                showSaveLocationDialog = true 
                            }
                            return true
                        }
                    }))
                }
            },
            update = { view ->
                try {
                    view.setUseDataConnection(!viewModel.isOfflineMode)
                    
                    val isManual = lastInteractionTime != 0L
                    val isNavigating = viewModel.mapTargetLocation != null
                    if (isManual || isNavigating) {
                        locationOverlay.disableFollowLocation()
                    } else {
                        locationOverlay.enableFollowLocation()
                        
                        // 🧭 Modo Navegación Táctica: Control de rotación automática
                        if (isNavigationModeEnabled && latestLocation != null && latestLocation!!.hasBearing()) {
                            val targetOrientation = -latestLocation!!.bearing
                            val currentOrientation = view.mapOrientation
                            val deltaAngle = targetOrientation - currentOrientation
                            
                            // Normalizar diferencia de ángulo a [-180, 180] para evitar rotaciones de 360 grados innecesarias
                            val normalizedDiff = ((deltaAngle + 180f) % 360f - 360f) % 360f + 180f
                            
                            // Suavizado por Lerp (Factor de amortiguación alpha = 0.15f)
                            if (Math.abs(normalizedDiff) > 1.0f) {
                                view.mapOrientation = currentOrientation + (normalizedDiff * 0.15f)
                            }
                        } else if (!isNavigationModeEnabled && view.mapOrientation != 0f) {
                            view.mapOrientation = 0f
                        }
                    }
                    
                    // Sincronizar iconos de ubicación (solo si cambiaron por isTracking)
                    locationOverlay.setPersonIcon(circleIcon)
                    locationOverlay.setDirectionIcon(circleIcon)

                    var needsInvalidate = false

                    // 1. Polyline segmentada por modo de actividad (OPTIMIZADA: diffing incremental)
                    val newSegments = routePoints.toRouteSegments()
                    val currentTotalPoints = routePoints.size
                    val newModes = newSegments.map { it.mode }
                    val structureChanged = newSegments.size != prevSegmentModes.size || newModes != prevSegmentModes
                    if (structureChanged || currentTotalPoints <= prevTotalPoints) {
                        // Reconstrucción completa solo cuando cambia estructura o hay reset
                        routePolylines.forEach { view.overlays.remove(it) }
                        routePolylines.clear()
                        newSegments.forEach { seg ->
                            Polyline().apply {
                                setPoints(seg.points)
                                outlinePaint.color = seg.color
                                outlinePaint.strokeWidth = viewModel.mapStrokeWidth
                            }.also {
                                routePolylines.add(it)
                                view.overlays.add(it)
                                needsInvalidate = true
                            }
                        }
                    } else if (currentTotalPoints > prevTotalPoints) {
                        // Solo añadir puntos nuevos al último segmento
                        val lastPolyline = routePolylines.lastOrNull()
                        val lastSegment = newSegments.lastOrNull()
                        if (lastPolyline != null && lastSegment != null) {
                            val existingCount = lastPolyline.actualPoints.size
                            if (existingCount < lastSegment.points.size) {
                                val newGeoPoints = lastSegment.points.drop(existingCount)
                                newGeoPoints.forEach { lastPolyline.addPoint(it) }
                                needsInvalidate = true
                            }
                        }
                    }
                    // Actualizar tracking
                    prevSegmentModes.clear()
                    prevSegmentModes.addAll(newModes)
                    prevTotalPoints = currentTotalPoints
                    // 2. Optimización de Waypoints (Diffing selectivo)
                    val currentWpIds = waypoints.map { it.id }.toSet()
                    waypointMarkers.keys.retainAll { id ->
                        if (id !in currentWpIds) {
                            view.overlays.remove(waypointMarkers[id])
                            needsInvalidate = true
                            false
                        } else true
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
                            marker.icon = getCachedIcon(android.R.drawable.star_on, wp.color)
                            needsInvalidate = true
                        }
                    }

                    // 3. Optimización de Favoritos (Diffing selectivo)
                    val currentFavIds = savedLocations.map { it.id }.toSet()
                    favoriteMarkers.keys.retainAll { id ->
                        if (id !in currentFavIds) {
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
            // 1. Barra de identidad de marca 100% transparente
            TopBarLocalizador(isDarkMap = false) // Mantenemos compatible por defecto en false

            // 2. Separación matemática elástica anti-colisiones
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Distribución horizontal libre de superposiciones en hilos visuales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Cartel A: Dashboard de métricas cinemáticas (modular)
                DashboardActividadFlotante(
                    activityMode = activityMode,
                    velocidadActual = "${String.format(Locale.getDefault(), "%.1f", (latestLocation?.speed ?: 0f) * 3.6f)} km/h",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!showAddressBox) {
                                viewModel.fetchCurrentAddress()
                                showAddressBox = true
                            } else {
                                showAddressBox = false
                                viewModel.clearCurrentAddress()
                            }
                        }
                )

                if (showAddressBox && viewModel.currentAddress != null) {
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                    // Cartel B: Cuadro de Calle actual por geocodificación
                    IndicadorTranslucidoContenedor(
                        modifier = Modifier
                            .weight(1f)
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
        }

        // Botones flotantes abajo a la izquierda (Alineados verticalmente en columna)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    isNavigationModeEnabled = !isNavigationModeEnabled
                    if (!isNavigationModeEnabled) {
                        mapView.mapOrientation = 0f
                    } else {
                        lastInteractionTime = 0L
                        locationOverlay.enableFollowLocation()
                    }
                },
                containerColor = if (isNavigationModeEnabled) MaterialTheme.colorScheme.primaryContainer else Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Modo Navegación",
                    tint = if (isNavigationModeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black
                )
            }

            FloatingActionButton(
                onClick = { 
                    lastInteractionTime = 0L
                    viewModel.clearMapTarget()
                    locationOverlay.enableFollowLocation()
                    latestLocation?.let { mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude)) } 
                },
                containerColor = Color.White
            ) { Icon(Icons.Default.MyLocation, null) }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp, end = 16.dp)) {
            if (!isTracking) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.generateSuggestedRouteName { newRouteName = it; showStartDialog = true } },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("Grabar") }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FloatingActionButton(onClick = { showWaypointDialog = true }) { Icon(Icons.Default.AddLocationAlt, null) }
                    FloatingActionButton(onClick = { viewModel.stopTrackingRoute() }, containerColor = MaterialTheme.colorScheme.error) { Icon(Icons.Default.Stop, null) }
                }
            }
        }
        // Marca de agua adaptativa con contraste dinámico MD3
        BrandWatermark(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 168.dp, start = 8.dp)
        )
    }

    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("Nueva Ruta") },
            text = {
                Column {
                    OutlinedTextField(value = newRouteName, onValueChange = { newRouteName = it }, label = { Text("Nombre") })
                    Text("Precisión: ${viewModel.updateIntervalSeconds.toInt()}s", modifier = Modifier.padding(top = 8.dp))
                    Slider(value = viewModel.updateIntervalSeconds, onValueChange = { viewModel.updateIntervalSeconds = it }, valueRange = 1f..60f)
                }
            },
            confirmButton = { Button(onClick = { viewModel.startTrackingRoute(newRouteName); showStartDialog = false }) { Text("Iniciar") } }
        )
    }

    if (showWaypointDialog) {
        var wpName by remember { mutableStateOf("") }
        var wpAuthor by remember { mutableStateOf("") }
        var wpComment by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf("#4CAF50") } // Verde por defecto para rutas
        val colors = listOf("#4CAF50", "#FFD700", "#2196F3", "#F44336", "#9C27B0", "#FF9800")
        
        // Actualizar dirección si cambia el switch de ubicación de foto
        LaunchedEffect(usePhotoGps) {
            if (usePhotoGps && photoGpsLocation != null) {
                viewModel.fetchAddressForCoordinates(photoGpsLocation!!.first, photoGpsLocation!!.second)
            } else if (!usePhotoGps) {
                viewModel.fetchCurrentAddress()
            }
        }

        // Intentar obtener la dirección actual para la descripción si está vacía
        LaunchedEffect(Unit) {
            if (viewModel.currentAddress == null) {
                viewModel.fetchCurrentAddress()
            }
        }

        AlertDialog(
            onDismissRequest = { 
                showWaypointDialog = false
                capturedPhotoUri = null
            },
            title = { Text("Añadir Punto de Interés") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = wpName, onValueChange = { wpName = it }, label = { Text("Nombre (ej. Gasolinera)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = wpAuthor, onValueChange = { wpAuthor = it }, label = { Text("Autor (quién marca)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wpComment, 
                        onValueChange = { wpComment = it }, 
                        label = { Text("Comentario / Nota") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(viewModel.currentAddress ?: "") }
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("Color de la estrella:", style = MaterialTheme.typography.labelMedium)
                    
                    // Colores predefinidos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorHex.toColorInt()), RoundedCornerShape(16.dp))
                                    .clickable { selectedColor = colorHex }
                                    .padding(4.dp)
                            ) {
                                if (selectedColor == colorHex) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    // Selector de color libre (Slider de Matiz)
                    var hue by remember { 
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(selectedColor.toColorInt(), hsv)
                        mutableStateOf(hsv[0]) 
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Personalizado:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            )
                        }
                        Slider(
                            value = hue,
                            onValueChange = { 
                                hue = it
                                val newColor = String.format(Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                selectedColor = newColor
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    
                    if (capturedPhotoUri != null) {
                        AsyncImage(
                            model = capturedPhotoUri,
                            contentDescription = "Foto capturada",
                            modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray, RoundedCornerShape(8.dp))
                                .clickable { photoToZoom = capturedPhotoUri?.toString() }
                        )

                        if (photoGpsLocation != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp).clickable { usePhotoGps = !usePhotoGps }
                            ) {
                                Checkbox(checked = usePhotoGps, onCheckedChange = { usePhotoGps = it })
                                Text("Usar ubicación de la foto", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { capturedPhotoUri = null; photoGpsLocation = null; usePhotoGps = false }) { Text("Quitar foto") }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPermission) {
                                        val uri = viewModel.createImageFile()
                                        if (uri != null) {
                                            capturedPhotoUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Cámara", fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Galería", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    val finalLat: Double
                    val finalLon: Double
                    
                    if (usePhotoGps && photoGpsLocation != null) {
                        finalLat = photoGpsLocation!!.first
                        finalLon = photoGpsLocation!!.second
                    } else {
                        val currentLoc = viewModel.latestLocationLive.value
                        finalLat = currentLoc?.latitude ?: 0.0
                        finalLon = currentLoc?.longitude ?: 0.0
                    }

                    viewModel.addWaypointToActiveRoute(
                        name = wpName.ifBlank { "Punto" }, 
                        description = wpComment.ifBlank { viewModel.currentAddress ?: "" },
                        author = wpAuthor,
                        photoPath = capturedPhotoUri?.toString(),
                        latitude = finalLat,
                        longitude = finalLon,
                        color = selectedColor
                    )
                    showWaypointDialog = false 
                    wpName = ""
                    wpAuthor = ""
                    wpComment = ""
                    capturedPhotoUri = null
                    photoGpsLocation = null
                    usePhotoGps = false
                }) { Text("Añadir") } 
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showWaypointDialog = false
                    capturedPhotoUri = null
                }) { Text("Cancelar") } 
            }
        )
    }

    if (showSaveLocationDialog) {
        val selectedAddress = viewModel.selectedPointAddress // Asegurarnos que esto sea reactivo
        var manualName by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        var author by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var isContact by remember { mutableStateOf(false) }
        var selectedColor by remember { mutableStateOf("#FFD700") } // Dorado por defecto para favoritos
        val colors = listOf("#FFD700", "#4CAF50", "#2196F3", "#F44336", "#9C27B0", "#FF9800")

        // Actualizar dirección si cambia el switch de ubicación de foto
        LaunchedEffect(usePhotoGps) {
            if (usePhotoGps && photoGpsLocation != null) {
                viewModel.fetchAddressForLocation(photoGpsLocation!!.first, photoGpsLocation!!.second)
            } else if (!usePhotoGps && clickedGeoPoint != null) {
                viewModel.fetchAddressForLocation(clickedGeoPoint!!.latitude, clickedGeoPoint!!.longitude)
            }
        }

        AlertDialog(
            onDismissRequest = { 
                showSaveLocationDialog = false
                viewModel.clearSelectedPointAddress()
                capturedPhotoUri = null
            },
            title = { Text("Guardar Lugar") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = manualName, 
                        onValueChange = { manualName = it }, 
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isContact, onCheckedChange = { isContact = it })
                        Text("Es un Contacto", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isContact) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("¿Quién lo guarda?") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comentario / Nota") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Color de la estrella:", style = MaterialTheme.typography.labelMedium)
                    
                    // Colores predefinidos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorHex.toColorInt()), RoundedCornerShape(16.dp))
                                    .clickable { selectedColor = colorHex }
                                    .padding(4.dp)
                            ) {
                                if (selectedColor == colorHex) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    // Selector de color libre (Slider de Matiz)
                    var hue by remember { 
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(selectedColor.toColorInt(), hsv)
                        mutableStateOf(hsv[0]) 
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Personalizado:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            )
                        }
                        Slider(
                            value = hue,
                            onValueChange = { 
                                hue = it
                                val newColor = String.format(Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                selectedColor = newColor
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Dirección detectada:", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedAddress ?: "Buscando dirección...", 
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    if (capturedPhotoUri != null) {
                        AsyncImage(
                            model = capturedPhotoUri,
                            contentDescription = "Foto",
                            modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray, RoundedCornerShape(8.dp))
                                .clickable { photoToZoom = capturedPhotoUri?.toString() }
                        )

                        if (photoGpsLocation != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp).clickable { usePhotoGps = !usePhotoGps }
                            ) {
                                Checkbox(checked = usePhotoGps, onCheckedChange = { usePhotoGps = it })
                                Text("Usar ubicación de la foto", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { capturedPhotoUri = null; photoGpsLocation = null; usePhotoGps = false }) { Text("Quitar foto") }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPermission) {
                                        val uri = viewModel.createImageFile()
                                        if (uri != null) {
                                            capturedPhotoUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Cámara", fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Galería", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    val finalLat: Double
                    val finalLon: Double
                    
                    if (usePhotoGps && photoGpsLocation != null) {
                        finalLat = photoGpsLocation!!.first
                        finalLon = photoGpsLocation!!.second
                    } else {
                        finalLat = clickedGeoPoint?.latitude ?: 0.0
                        finalLon = clickedGeoPoint?.longitude ?: 0.0
                    }

                    viewModel.saveSpecificLocation(
                        name = manualName.ifBlank { "Lugar guardado" }, 
                        address = selectedAddress ?: "", 
                        latitude = finalLat, 
                        longitude = finalLon,
                        comment = comment,
                        author = author,
                        photoPath = capturedPhotoUri?.toString(),
                        phoneNumber = phone,
                        email = email,
                        isContact = isContact,
                        color = selectedColor
                    )
                    showSaveLocationDialog = false 
                    viewModel.clearSelectedPointAddress()
                    manualName = ""
                    author = ""
                    comment = ""
                    phone = ""
                    email = ""
                    isContact = false
                    capturedPhotoUri = null
                    photoGpsLocation = null
                    usePhotoGps = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSaveLocationDialog = false
                    viewModel.clearSelectedPointAddress()
                    capturedPhotoUri = null
                }) { Text("Cancelar") }
            }
        )
    }

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )

    if (showTourMapa) {
        TourOverlay(
            message = "¡Ahora detecto si caminas, vas en bici o en coche automáticamente!",
            onDismiss = {
                showTourMapa = false
                viewModel.marcarTourVisto("actividad")
            }
        )
    }


}

/**
 * Contenedor genérico translúcido de alta visibilidad diurna.
 * 🛡️ Respeta la regla de evitar elevación tonal para bloquear grises indeseados.
 */
@Composable
fun IndicadorTranslucidoContenedor(
    modifier: Modifier = Modifier,
    overrideColor: Color = Color(0xFF1C1B1F).copy(alpha = 0.65f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = overrideColor, // Inyeccion elastica de color (alerta o default)
        tonalElevation = 0.dp // Fuerza la anulación de capas grises sólidas automáticas de MD3
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
