package com.aistudio.localizador.ui.screens.map

import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.model.Waypoint
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.map.MapLayerType
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapCanvas(
    viewModel: RouteViewModel,
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
    rotationGestureOverlay: RotationGestureOverlay,
    isTracking: Boolean,
    latestLocation: Location?,
    routePoints: List<LocationPoint>,
    waypoints: List<Waypoint>,
    savedLocations: List<SavedLocation>,
    currentMapLayer: MapLayerType,
    mapOrientation: Float,
    isNavigationModeEnabled: Boolean,
    lastInteractionTime: Long,
    onMapOrientationChange: (Float) -> Unit,
    onUserInteraction: () -> Unit,
    onLongPress: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val overlayController = remember { MapOverlayController(context) }

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

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                viewModel.mapTileSourceManager.applyLayerToMapView(this, currentMapLayer)
                setMultiTouchControls(true)
                setOnTouchListener { v, _ ->
                    v.performClick()
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    false
                }
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                viewModel.mapTargetLocation?.let { target ->
                    controller.setZoom(18.0)
                    controller.setCenter(target)
                    onUserInteraction()
                } ?: run {
                    controller.setZoom(15.0)
                }

                locationOverlay.enableMyLocation()
                locationOverlay.setDrawAccuracyEnabled(true)

                if (viewModel.mapTargetLocation == null) {
                    locationOverlay.enableFollowLocation()
                } else {
                    locationOverlay.disableFollowLocation()
                }

                val arrowIcon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.next)
                arrowIcon?.setTint(android.graphics.Color.RED)

                overlays.add(locationOverlay)
                overlays.add(rotationGestureOverlay)

                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        onMapOrientationChange(mapView.mapOrientation)
                        return false
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        onUserInteraction()
                        onMapOrientationChange(mapView.mapOrientation)
                        return false
                    }
                })

                overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let { onLongPress(it) }
                        return true
                    }
                }))
            }
        },
        update = { view ->
            try {
                view.setUseDataConnection(!viewModel.isOfflineMode)

                if (mapOrientation != view.mapOrientation) {
                    onMapOrientationChange(view.mapOrientation)
                }

                val isManual = lastInteractionTime != 0L
                val isNavigating = viewModel.mapTargetLocation != null
                if (isManual || isNavigating) {
                    locationOverlay.disableFollowLocation()
                } else {
                    locationOverlay.enableFollowLocation()

                    if (isNavigationModeEnabled && latestLocation != null && latestLocation.hasBearing()) {
                        val targetOrientation = -latestLocation.bearing
                        val currentOrientation = view.mapOrientation
                        val deltaAngle = targetOrientation - currentOrientation
                        val normalizedDiff = ((deltaAngle + 180f) % 360f - 360f) % 360f + 180f
                        if (Math.abs(normalizedDiff) > 1.0f) {
                            view.mapOrientation = currentOrientation + (normalizedDiff * 0.15f)
                        }
                    } else if (!isNavigationModeEnabled && view.mapOrientation != 0f) {
                        view.mapOrientation = 0f
                    }
                }

                locationOverlay.setPersonIcon(circleIcon)
                locationOverlay.setDirectionIcon(circleIcon)
                var needsInvalidate = false

                if (viewModel.mapTileSourceManager.applyLayerToMapView(view, currentMapLayer)) {
                    needsInvalidate = true
                }

                if (overlayController.renderRoute(view, routePoints, viewModel)) {
                    needsInvalidate = true
                }

                if (overlayController.renderWaypoints(view, waypoints) { m, v ->
                        onUserInteraction()
                        v.controller.animateTo(m.position)
                        m.showInfoWindow()
                        true
                    }) {
                    needsInvalidate = true
                }

                if (overlayController.renderFavorites(view, savedLocations, resources) { m, v ->
                        onUserInteraction()
                        v.controller.animateTo(m.position)
                        m.showInfoWindow()
                        true
                    }) {
                    needsInvalidate = true
                }

                if (needsInvalidate) view.invalidate()
            } catch (_: Exception) { }
        }
    )
}
