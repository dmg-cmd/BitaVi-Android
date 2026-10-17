package com.aistudio.localizador.ui.screens.map

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.model.Waypoint
import com.aistudio.localizador.location.ActivityMode
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.model.IconCategory
import com.aistudio.localizador.ui.toGradientRouteSegments
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Controlador de renderizado de overlays de osmdroid para MapScreen.
 * Encapsula las cachés de marcadores/polilíneas y la lógica de diffing incremental,
 * manteniendo el rendimiento (anti-flicker) fuera del composable principal.
 */
class MapOverlayController(private val context: Context) {

    private val markerIconCache = mutableMapOf<String, Drawable>()
    private val waypointMarkers = mutableMapOf<Int, Marker>()
    private val favoriteMarkers = mutableMapOf<Int, Marker>()
    private val routePolylines = mutableListOf<Polyline>()
    private val prevSegmentModes = mutableListOf<ActivityMode>()
    private var prevTotalPoints = 0

    private fun getCachedIcon(resId: Int, colorHex: String): Drawable {
        val key = "$resId-$colorHex"
        return markerIconCache.getOrPut(key) {
            val d = ContextCompat.getDrawable(context, resId) ?: error("resId")
            d.mutate().apply { setTint(colorHex.toColorInt()) }
        }
    }

    fun renderRoute(view: MapView, routePoints: List<LocationPoint>, viewModel: RouteViewModel): Boolean {
        if (routePoints.isEmpty()) return false
        var needsInvalidate = false

        val newSegments = routePoints.toGradientRouteSegments(viewModel.routeColorUseCase)
        val currentTotalPoints = routePoints.size
        val newModes = newSegments.map { it.mode }
        val structureChanged = newSegments.size != prevSegmentModes.size || newModes != prevSegmentModes

        if (structureChanged || currentTotalPoints <= prevTotalPoints) {
            routePolylines.forEach { view.overlays.remove(it) }
            routePolylines.clear()
            newSegments.forEach { seg ->
                Polyline().apply {
                    setPoints(seg.points)
                    outlinePaint.color = seg.color
                    outlinePaint.strokeWidth = viewModel.mapStrokeWidth
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                }.also {
                    routePolylines.add(it)
                    view.overlays.add(it)
                    needsInvalidate = true
                }
            }
        } else {
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

        prevSegmentModes.clear()
        prevSegmentModes.addAll(newModes)
        prevTotalPoints = currentTotalPoints
        return needsInvalidate
    }

    fun renderWaypoints(
        view: MapView,
        waypoints: List<Waypoint>,
        onMarkerClick: (Marker, MapView) -> Boolean
    ): Boolean {
        var needsInvalidate = false
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
                    setOnMarkerClickListener { m, v -> onMarkerClick(m, v) }
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
        return needsInvalidate
    }

    fun renderFavorites(
        view: MapView,
        savedLocations: List<SavedLocation>,
        resources: Resources,
        onMarkerClick: (Marker, MapView) -> Boolean
    ): Boolean {
        var needsInvalidate = false
        val currentFavIds = savedLocations.map { it.id }.toSet()
        favoriteMarkers.keys.retainAll { id ->
            if (id !in currentFavIds) {
                view.overlays.remove(favoriteMarkers[id])
                needsInvalidate = true
                false
            } else true
        }

        savedLocations.forEach { loc ->
            var isNew = false
            val marker = favoriteMarkers.getOrPut(loc.id) {
                isNew = true
                Marker(view).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { m, v -> onMarkerClick(m, v) }
                    view.overlays.add(this)
                }
            }
            val newPos = GeoPoint(loc.latitude, loc.longitude)
            if (isNew || marker.position != newPos || marker.title != loc.name || marker.snippet != loc.address) {
                val isStop = loc.iconCategory == "AUTOMATIC" ||
                        loc.author.equals("Automático", ignoreCase = true) ||
                        loc.author.contains("Parada", ignoreCase = true) ||
                        loc.comment.contains("Parada", ignoreCase = true)
                val effectiveColorHex = if (isStop && loc.color.equals("#FFD700", ignoreCase = true)) {
                    "#FF6D00"
                } else {
                    loc.color
                }
                val effectiveCategory = if (isStop) {
                    IconCategory.AUTOMATIC
                } else {
                    IconCategory.fromKey(loc.iconCategory)
                }
                marker.position = newPos
                marker.title = if (isStop && !loc.name.startsWith("🛑")) "🛑 ${loc.name}" else loc.name
                marker.snippet = loc.address
                marker.icon = effectiveCategory.toBitmap(44, effectiveColorHex.toColorInt()).toDrawable(resources)
                needsInvalidate = true
            }
        }
        return needsInvalidate
    }
}

