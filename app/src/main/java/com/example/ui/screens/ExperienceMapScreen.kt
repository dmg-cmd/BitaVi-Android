package com.example.ui.screens

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.RouteViewModel
import com.example.R
import com.example.ui.Screen
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperienceMapScreen(
    viewModel: RouteViewModel,
    routeId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val route by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.selectedDetailPoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()
    val linkedFavorites by viewModel.getFavoritesForRouteFlow(routeId).collectAsState(initial = emptyList())

    val mapView = remember { MapView(context) }
    var photoToZoom by remember { mutableStateOf<String?>(null) }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(points, waypoints, linkedFavorites) {
        val geoPoints = buildList {
            points.sortedBy { it.timestamp }.forEach { add(GeoPoint(it.latitude, it.longitude)) }
            waypoints.forEach { add(GeoPoint(it.latitude, it.longitude)) }
            linkedFavorites.forEach { add(GeoPoint(it.latitude, it.longitude)) }
        }
        if (geoPoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
            mapView.post { mapView.zoomToBoundingBox(boundingBox, true, 100) }
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = route?.name ?: "Experience Map",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.History) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (points.isEmpty() && linkedFavorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
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
                        view.overlays.removeAll { it is Marker }

                        if (points.isNotEmpty()) {
                            val poly = Polyline().apply {
                                val sortedPoints = points.sortedBy { it.timestamp }
                                setPoints(sortedPoints.map { GeoPoint(it.latitude, it.longitude) })
                                outlinePaint.color = android.graphics.Color.RED
                                outlinePaint.strokeWidth = 5f
                            }
                            view.overlays.add(poly)
                        }

                        waypoints.forEach { wp ->
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(wp.latitude, wp.longitude)
                                title = wp.name
                                snippet = wp.description
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = getCachedIcon(android.R.drawable.star_on, wp.color)
                                setOnMarkerClickListener { m, _ ->
                                    m.showInfoWindow()
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                        }

                        linkedFavorites.forEach { fav ->
                            android.util.Log.d("DEBUG_FOTOS", "ID: ${fav.id}, Path: '${fav.photoPath}'")
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(fav.latitude, fav.longitude)
                            marker.title = fav.name
                            marker.icon = getCachedIcon(android.R.drawable.btn_star_big_on, fav.color)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.infoWindow = PhotoInfoWindow(
                                mapView,
                                onPhotoClick = { path -> photoToZoom = path }
                            ).apply {
                                if (!fav.photoPath.isNullOrEmpty()) {
                                    setPhoto(fav.photoPath)
                                }
                            }
                            mapView.overlays.add(marker)
                        }

                        view.invalidate()
                    }
                )
            }
        }
    }

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )
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
