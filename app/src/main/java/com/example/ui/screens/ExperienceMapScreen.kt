package com.example.ui.screens

import com.example.ui.toRouteSegments
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.drawToBitmap
import coil.compose.AsyncImage
import coil.load
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.R
import com.example.ui.RouteViewModel
import com.example.ui.components.FullScreenPhotoDialog
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ZoomOutMap
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.io.File
import java.io.FileOutputStream

data class PhotoItem(
    val id: Int,
    val type: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceMapScreen(
    viewModel: RouteViewModel,
    routeId: Int,
) {
    android.util.Log.d("EXPERIENCE_MAP", "Screen composed, routeId=$routeId")

    val context = LocalContext.current
    val points by viewModel.selectedDetailPoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()
    val linkedFavorites by viewModel.getFavoritesForRouteFlow(routeId).collectAsState(initial = emptyList())

    val mapView = remember { MapView(context) }
    val rotationGestureOverlay = remember { RotationGestureOverlay(mapView) }.apply {
        isEnabled = true
    }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var overlayScrollOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(scrollState.value) { overlayScrollOffset = scrollState.value }
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



    val photoItems = remember(waypoints, linkedFavorites) {
        val items = mutableListOf<PhotoItem>()
        waypoints.filter { !it.photoPath.isNullOrBlank() }.forEach { wp ->
            items.add(PhotoItem(wp.id, "waypoint", wp.name, wp.latitude, wp.longitude, wp.photoPath!!))
        }
        linkedFavorites.filter { !it.photoPath.isNullOrBlank() }.forEach { fav ->
            items.add(PhotoItem(fav.id, "favorite", fav.name, fav.latitude, fav.longitude, fav.photoPath!!))
        }
        android.util.Log.d("EXPERIENCE_MAP", "photoItems: ${items.size} (wp=${waypoints.filter { !it.photoPath.isNullOrBlank() }.size}, fav=${linkedFavorites.filter { !it.photoPath.isNullOrBlank() }.size}), routeId=$routeId")
        items.take(3).forEach { android.util.Log.d("EXPERIENCE_MAP", "  photo[${it.type}]: path=${it.photoPath}") }
        items
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

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(points, waypoints, linkedFavorites, routeId) {
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
    }

    Box(modifier = Modifier.fillMaxSize()) {
            if (points.isEmpty() && linkedFavorites.isEmpty() && waypoints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                android.util.Log.d("EXPERIENCE_MAP", "RENDERING ROW, panelItems=${panelItems.size}")
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            mapView.apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                overlays.add(rotationGestureOverlay)
                            }
                        },
                         update = { view ->
                            val overlays = view.overlays
                            overlays.removeIf { it is Marker || it is GuideLinesOverlay || it is Polyline }

                            if (sortedPoints.isNotEmpty()) {
                                sortedPoints.toRouteSegments().forEach { seg ->
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
                                    density = context.resources.displayMetrics.density
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
                            } else if (view.mapOrientation != 0f) {
                                // Si se desactiva y el mapa está girado, volvemos a 0f
                                view.mapOrientation = 0f
                            }

                            view.invalidate()
                        }
                        )

                    AnimatedVisibility(
                        visible = panelItems.isNotEmpty(),
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
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
                                            model = ImageRequest.Builder(LocalContext.current)
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
                }

                // Botones flotantes abajo a la izquierda (Alineados verticalmente en columna)
                if (points.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 32.dp, start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón de Auto-Rotación/Simulación de Experiencia
                        androidx.compose.material3.FloatingActionButton(
                            onClick = {
                                isAutoRotationEnabled = !isAutoRotationEnabled
                                if (!isAutoRotationEnabled) {
                                    mapView.mapOrientation = 0f
                                }
                            },
                            containerColor = if (isAutoRotationEnabled) MaterialTheme.colorScheme.primaryContainer else Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Simular Ruta",
                                tint = if (isAutoRotationEnabled) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black
                            )
                        }

                        // Botón de Centrar vista en toda la ruta
                        androidx.compose.material3.FloatingActionButton(
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
                            containerColor = Color.White
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Ver Todo",
                                tint = Color.Black
                            )
                        }
                    }
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

