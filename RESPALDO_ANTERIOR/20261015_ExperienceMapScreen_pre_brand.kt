package com.aistudio.localizador.ui.screens

import com.aistudio.localizador.ui.theme.IconCompartir
import com.aistudio.localizador.ui.theme.IconExportar
import com.aistudio.localizador.ui.toGradientRouteSegments
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.drawToBitmap
import coil.compose.AsyncImage
import coil.load
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.aistudio.localizador.R
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.components.FullScreenPhotoDialog
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class PhotoItem(
    val id: Int,
    val type: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String
)

@Composable
fun ExperienceMapScreen(
    onBack: () -> Unit,
    viewModel: RouteViewModel,
    routeId: Int,
    modifier: Modifier = Modifier,
) {
    android.util.Log.d("EXPERIENCE_MAP", "Screen composed, routeId=$routeId")

    val context = LocalContext.current
    val currentRoute by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.experiencePoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()

    val linkedFavorites by viewModel.getFavoritesForRouteFlow(routeId).collectAsState(initial = emptyList())
    val gpsPrecisionState by viewModel.gpsAccuracy.collectAsStateWithLifecycle()
    val currentMapLayer by viewModel.currentMapLayer.collectAsState()
    var hasInitiallyCenteredRoute by remember { mutableStateOf(false) }

    // Carga orquestada de waypoints e imágenes de la experiencia (Uso de prepareExperience)
    LaunchedEffect(routeId) {
        viewModel.prepareExperience(routeId)
    }

    BackHandler(onBack = onBack)

    val mapView = remember { MapView(context) }
    val rotationGestureOverlay = remember { RotationGestureOverlay(mapView) }.apply {
        isEnabled = true
    }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    val composeGraphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var overlayScrollOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value }.collect { overlayScrollOffset = it }
    }
    val panelItems = remember { mutableStateListOf<PhotoItem>() }
    val sortedPoints by remember(points) { derivedStateOf { points.sortedBy { it.timestamp } } }
    
    // 🧭 Controles de simulación y motor de orientación para visualización de experiencias
    var isAutoRotationEnabled by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var experienceBearing by remember { mutableStateOf(0f) }

    // Cálculo del rumbo secuencial óptimo mediante derivedStateOf
    val targetBearing by remember(sortedPoints, currentIndex) {
        derivedStateOf {
            val currentPoint = sortedPoints.getOrNull(currentIndex)
            val nextPoint = sortedPoints.getOrNull(currentIndex + 1)
            if (currentPoint != null && nextPoint != null) {
                val p1 = GeoPoint(currentPoint.latitude, currentPoint.longitude)
                val p2 = GeoPoint(nextPoint.latitude, nextPoint.longitude)
                p1.bearingTo(p2).toFloat()
            } else {
                experienceBearing
            }
        }
    }

    LaunchedEffect(targetBearing) {
        experienceBearing = targetBearing
    }

    // Timer para avance automático de telemetría si está activo el recorrido (panning cada 2.5s)
    LaunchedEffect(isAutoRotationEnabled) {
        if (isAutoRotationEnabled) {
            if (currentIndex >= sortedPoints.size - 1) {
                currentIndex = 0
            }
            while (isAutoRotationEnabled && currentIndex < sortedPoints.size - 1) {
                delay(2.5.seconds)
                currentIndex++
            }
            if (currentIndex >= sortedPoints.size - 1) {
                isAutoRotationEnabled = false
            }
        }
    }

    // Centrado suave de cámara sobre el nodo enfocado de la simulación
    LaunchedEffect(currentIndex) {
        if (isAutoRotationEnabled && sortedPoints.isNotEmpty()) {
            val currentPoint = sortedPoints.getOrNull(currentIndex)
            if (currentPoint != null) {
                mapView.controller.animateTo(GeoPoint(currentPoint.latitude, currentPoint.longitude))
            }
        }
    }

    val markerIconCache = remember { mutableMapOf<String, Drawable>() }
    val getCachedIcon: (Int, String) -> Drawable = { resId, colorHex ->
        val key = "$resId-$colorHex"
        markerIconCache.getOrPut(key) {
            ContextCompat.getDrawable(context, resId)!!.mutate().apply {
                setTint(colorHex.toColorInt())
            }
        }
    }

    val density = LocalDensity.current.density

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(points, waypoints, linkedFavorites, routeId) {
        if (!hasInitiallyCenteredRoute) {
            val geoPoints = buildList {
                points.sortedBy { it.timestamp }.forEach { add(GeoPoint(it.latitude, it.longitude)) }
                waypoints.forEach { add(GeoPoint(it.latitude, it.longitude)) }
                linkedFavorites.forEach { add(GeoPoint(it.latitude, it.longitude)) }
            }
            if (geoPoints.isNotEmpty()) {
                try {
                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                    mapView.post { mapView.zoomToBoundingBox(boundingBox, true, 100) }
                } catch (_: Exception) {}
            }
            hasInitiallyCenteredRoute = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    composeGraphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(composeGraphicsLayer)
                }
        ) {

        // ====================================================================
        // CAPA 0: MAPA EN FULLSCREEN
        // ====================================================================
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    setTileSource(viewModel.mapTileSourceManager.getTileSource(currentMapLayer))
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setBuiltInZoomControls(false)
                    post {
                        // Explicitly remove any non-tile overlay that could render UI buttons
                        overlays.removeAll { it !is org.osmdroid.views.overlay.gestures.RotationGestureOverlay && it !is org.osmdroid.views.overlay.TilesOverlay }
                    }
                    overlays.add(rotationGestureOverlay)
                    // Hide OSM attribution bar (visible through transparent dock)
                    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                        for (i in 0 until childCount) {
                            (getChildAt(i) as? android.widget.TextView)?.visibility = android.view.View.GONE
                        // Also hide potential OSM logo ImageView
                        (getChildAt(i) as? android.widget.ImageView)?.visibility = android.view.View.GONE
                        }
                    }
                }
            },
            update = { view ->
                val targetTileSource = viewModel.mapTileSourceManager.getTileSource(currentMapLayer)
                if (view.tileProvider.tileSource != targetTileSource) {
                    view.setTileSource(targetTileSource)
                }

                val overlays = view.overlays
                overlays.removeIf { it is Marker || it is GuideLinesOverlay || it is Polyline }

                if (sortedPoints.isNotEmpty()) {
                    sortedPoints.toGradientRouteSegments(viewModel.routeColorUseCase).forEach { seg ->
                        overlays.add(Polyline().apply {
                            setPoints(seg.points)
                            outlinePaint.color = seg.color
                            outlinePaint.strokeWidth = viewModel.mapStrokeWidth
                        })
                    }
                }

                waypoints.forEach { wp ->
                    overlays.add(Marker(view).apply {
                        position = GeoPoint(wp.latitude, wp.longitude)
                        title = wp.name
                        snippet = wp.description
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = getCachedIcon(android.R.drawable.star_on, wp.color)
                        setOnMarkerClickListener { _, _ ->
                            val item = PhotoItem(wp.id, "waypoint", wp.name, wp.latitude, wp.longitude, wp.photoPath ?: "")
                            if (panelItems.none { it.id == wp.id && it.type == "waypoint" }) {
                                panelItems.add(item)
                            }
                            true
                        }
                    })
                }

                linkedFavorites.forEach { fav ->
                    overlays.add(Marker(view).apply {
                        position = GeoPoint(fav.latitude, fav.longitude)
                        title = fav.name
                        icon = getCachedIcon(android.R.drawable.btn_star_big_on, fav.color)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            val item = PhotoItem(fav.id, "favorite", fav.name, fav.latitude, fav.longitude, fav.photoPath ?: "")
                            if (panelItems.none { it.id == fav.id && it.type == "favorite" }) {
                                panelItems.add(item)
                            }
                            true
                        }
                    })
                }

                if (panelItems.isNotEmpty()) {
                    overlays.add(GuideLinesOverlay(
                        scrollOffset = { overlayScrollOffset },
                        photoItems = panelItems,
                        density = density,
                    ))
                }

                // 🧭 Modo Navegación de Experiencia (Rotación Automática y Suavizado)
                if (isAutoRotationEnabled) {
                    val currentOrientation = view.mapOrientation
                    val deltaAngle = experienceBearing - currentOrientation
                    
                    // Normalización estricta del delta de ángulo
                    val normalizedDiff = ((deltaAngle + 180f) % 360f - 360f) % 360f + 180f
                    
                    if (Math.abs(normalizedDiff) > 1.0f) {
                        // Filtro Lerp de baja frecuencia (alpha = 0.15f)
                        view.mapOrientation = currentOrientation + (normalizedDiff * 0.15f)
                    }
                }
                
                // PASO 1: Sincronización norteada para exploración libre
                if (!isAutoRotationEnabled) {
                    view.mapOrientation = 0f
                }

                view.invalidate()
            }
        )

        // ====================================================================
        // CAPA 1: PANEL DE FOTOS TRANSLÚCIDO (LATERAL IZQUIERDO)
        // ====================================================================
        AnimatedVisibility(
            visible = panelItems.isNotEmpty(),
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .background(Color(0x80000000))
                    .verticalScroll(scrollState)
            ) {
                panelItems.forEach { item ->
                    Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                        if (!item.photoPath.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.photoPath)
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = item.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { photoToZoom = item.photoPath },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().clickable { photoToZoom = item.photoPath }, contentAlignment = Alignment.Center) {
                                Text(item.name, color = Color.White, fontSize = 12.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .clickable {
                                    panelItems.remove(item)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar del panel",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // CAPA 1.5: INDICADOR GPS (TOP END)
        // ====================================================================


        // CAPA 2: CONTROLES FLOTANTES DE NAVEGACIÓN (LATERAL DERECHO)
        // ====================================================================
        if (points.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón de Auto-Rotación/Simulación de Experiencia
                FloatingActionButton(
                    onClick = {
                        isAutoRotationEnabled = !isAutoRotationEnabled
                        if (!isAutoRotationEnabled) {
                            mapView.mapOrientation = 0f
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Simular Ruta",
                    )
                }

                // Botón de Centrar vista en toda la ruta
                FloatingActionButton(
                    onClick = {
                        isAutoRotationEnabled = false
                        mapView.mapOrientation = 0f
                        val geoPoints = sortedPoints.map { GeoPoint(it.latitude, it.longitude) }
                        if (geoPoints.isNotEmpty()) {
                            try {
                                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                mapView.zoomToBoundingBox(boundingBox, true, 100)
                            } catch (_: Exception) {}
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Ver Todo",
                    )
                }
            }
        }
        }

        // ====================================================================
        // ÚNICA SUB-BARRA INFERIOR UNIFICADA 5.2 (CR-19)
        // ====================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color(0xFF121212),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sección Izquierda: Botón Volver + Nombre de la Experiencia
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = currentRoute?.name?.takeIf { it.isNotBlank() } ?: "Experiencia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    }
                }

                // Sección Central: Acciones de Compartir y Exportar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 5.2.1 [📤 Share Experiencia] (Cian 0xFF4DD0E1)
                    IconButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                runCatching {
                                    val imageBitmap = composeGraphicsLayer.toImageBitmap()
                                    val bitmap = imageBitmap.asAndroidBitmap()
                                    val cacheDir = File(context.cacheDir, "shared_experiences").apply { mkdirs() }
                                    val file = File(cacheDir, "experiencia_${routeId}.png")
                                    FileOutputStream(file).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val routeName = currentRoute?.name?.takeIf { it.isNotBlank() } ?: "Experiencia"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, "📍 Experiencia Bitaru: $routeName")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartir Experiencia..."))
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir Experiencia",
                            tint = IconCompartir
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // 5.2.2 [💾 Exportar ZIP/GPX] (Amber 0xFFFFB74D)
                    IconButton(
                        onClick = {
                            viewModel.exportRoute(routeId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Exportar Ruta",
                            tint = IconExportar
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sección Derecha: Telemetría GPS Satelital
                Text(
                    text = gpsPrecisionState,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB0BEC5)
                )
            }
        }
    }
    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )
}

class GuideLinesOverlay(
    private val scrollOffset: () -> Int,
    private val photoItems: List<PhotoItem>,
    private val density: Float
) : Overlay() {
    override fun draw(canvas: android.graphics.Canvas?, mapView: MapView?, shadow: Boolean) {
        val c = canvas ?: return
        val mv = mapView ?: return
        if (mv.width <= 0) return

        val paint = android.graphics.Paint().apply {
            color = "#FF8800".toColorInt()
            alpha = 180
            strokeWidth = 4f
            style = android.graphics.Paint.Style.STROKE
        }

        val thumbSizePx = 90f * density
        val scrollOff = scrollOffset().toFloat()

        photoItems.forEachIndexed { i, item ->
            try {
                val screenPt = android.graphics.Point()
                mv.projection.toPixels(GeoPoint(item.latitude, item.longitude), screenPt)

                val panelWidthPx = 160f * density
                val startX = panelWidthPx
                val startY = thumbSizePx * i + thumbSizePx / 2 - scrollOff
                val endX = screenPt.x.toFloat()
                val endY = screenPt.y.toFloat()

                c.drawLine(startX, startY, endX, endY, paint)
            } catch (_: Exception) {}
        }
    }
}




