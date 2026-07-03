package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("EXPERIENCE_MAP", "Screen composed, routeId=$routeId")

    val context = LocalContext.current
    val route by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.selectedDetailPoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()
    val linkedFavorites by viewModel.getFavoritesForRouteFlow(routeId).collectAsState(initial = emptyList())

    val mapView = remember { MapView(context) }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var overlayScrollOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(scrollState.value) { overlayScrollOffset = scrollState.value }
    val panelItems = remember { mutableStateListOf<PhotoItem>() }
    val sortedPoints by remember(points) { derivedStateOf { points.sortedBy { it.timestamp } } }
    val activity = context as? Activity

    val saveZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            viewModel.saveRouteZipToUri(routeId, uri)
        }
    }

    fun shareScreenshot() {
        val a = activity ?: return
        android.os.Handler(a.mainLooper).post {
            try {
                val bitmap = a.window.decorView.drawToBitmap()
                val cacheFile = File(context.cacheDir, "screenshot_${routeId}.png")
                FileOutputStream(cacheFile).use { fos -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) }
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Compartir pantalla")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al capturar: ${e.message}", Toast.LENGTH_SHORT).show()
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
                setTint(android.graphics.Color.parseColor(colorHex))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = route?.name ?: "Experience Map", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { shareScreenshot() }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir pantalla")
                    }
                    IconButton(onClick = { saveZipLauncher.launch("${route?.name ?: "ruta_$routeId"}.zip") }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar como ZIP")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                            }
                        },
                         update = { view ->
                            val overlays = view.overlays
                            overlays.removeIf { it is Marker || it is GuideLinesOverlay || it is Polyline }

                            if (sortedPoints.isNotEmpty()) {
                                overlays.add(Polyline().apply {
                                    setPoints(sortedPoints.map { GeoPoint(it.latitude, it.longitude) })
                                    outlinePaint.color = android.graphics.Color.RED
                                    outlinePaint.strokeWidth = 5f
                                })
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

                            view.invalidate()
                        }
                        )

                    if (panelItems.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .fillMaxHeight()
                                .background(Color(0xEE1A1A2E))
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

            }
        }

        // DEBUG OVERLAY
        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f)).padding(4.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "pts=${points.size} wp=${waypoints.size} fav=${linkedFavorites.size} fotos=${photoItems.size} load=${points.isEmpty() && linkedFavorites.isEmpty() && waypoints.isEmpty()}",
                color = Color.Green,
                fontSize = 10.sp
            )
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
            color = android.graphics.Color.parseColor("#FF8800")
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

class PhotoInfoWindow(
    mapView: MapView,
    private val onPhotoClick: ((String) -> Unit)? = null
) : InfoWindow(R.layout.info_window_layout, mapView) {

    private var pendingPhotoPath: String? = null

    fun setPhoto(photoPath: String) {
        pendingPhotoPath = photoPath
    }

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        val context = mView.context
        val density = context.resources.displayMetrics.density

        val layout = mView as LinearLayout
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(
            (12 * density).toInt(), (8 * density).toInt(),
            (12 * density).toInt(), (8 * density).toInt()
        )
        layout.setBackgroundColor(android.graphics.Color.WHITE)
        layout.removeAllViews()

        TextView(context).apply {
            text = marker.title ?: ""
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.BLACK)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layout.addView(this)
        }

        val path = pendingPhotoPath
        if (!path.isNullOrBlank()) {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    (150 * density).toInt(),
                    (150 * density).toInt()
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(path) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                setOnClickListener {
                    onPhotoClick?.invoke(path)
                }
                layout.addView(this)
            }
        } else {
            TextView(context).apply {
                text = "Sin foto"
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.GRAY)
                textSize = 12f
                layoutParams = ViewGroup.LayoutParams(
                    (150 * density).toInt(),
                    (100 * density).toInt()
                )
                layout.addView(this)
            }
        }
    }

    override fun onClose() {}
}
