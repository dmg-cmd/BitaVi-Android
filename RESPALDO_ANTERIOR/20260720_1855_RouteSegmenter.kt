package com.aistudio.localizador.ui

import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.location.ActivityMode
import org.osmdroid.util.GeoPoint

data class SegmentoRuta(
    val points: List<GeoPoint>,
    val mode: ActivityMode,
    val color: Int
)

fun Float.toActivityMode(): ActivityMode = when {
    this * 3.6f < 1.0f -> ActivityMode.STOPPED
    this * 3.6f <= 7.0f -> ActivityMode.WALKING
    this * 3.6f <= 13.0f -> ActivityMode.BIKING
    else -> ActivityMode.DRIVING
}

fun ActivityMode.toSegmentColor(): Int = when (this) {
    ActivityMode.STOPPED -> android.graphics.Color.argb(128, 255, 214, 0)
    ActivityMode.WALKING -> android.graphics.Color.rgb(76, 175, 80)
    ActivityMode.BIKING -> android.graphics.Color.rgb(33, 150, 243)
    ActivityMode.DRIVING -> android.graphics.Color.rgb(255, 82, 82)
    ActivityMode.SIN_SENAL -> android.graphics.Color.argb(128, 220, 53, 69)
}


fun List<LocationPoint>.toRouteSegments(): List<SegmentoRuta> {
    if (isEmpty()) return emptyList()

    val segments = mutableListOf<SegmentoRuta>()
    var currentPoints = mutableListOf<GeoPoint>()
    var currentMode = first().speed.toActivityMode()

    forEach { point ->
        val mode = point.speed.toActivityMode()
        val geoPoint = GeoPoint(point.latitude, point.longitude)

        if (mode == currentMode) {
            currentPoints.add(geoPoint)
        } else {
            currentPoints.add(geoPoint)
            if (currentMode == ActivityMode.STOPPED && currentPoints.size < 3) {
                currentPoints.clear()
                currentPoints.add(geoPoint)
                currentMode = mode
            } else {
                segments.add(SegmentoRuta(currentPoints.toList(), currentMode, currentMode.toSegmentColor()))
                currentPoints = mutableListOf(geoPoint)
                currentMode = mode
            }
        }
    }

    if (currentPoints.isNotEmpty()) {
        if (currentMode == ActivityMode.STOPPED && currentPoints.size < 3) {
            segments.lastOrNull()?.let { last ->
                val mergedPoints = last.points + currentPoints
                segments[segments.lastIndex] = last.copy(points = mergedPoints)
            }
        } else {
            segments.add(SegmentoRuta(currentPoints.toList(), currentMode, currentMode.toSegmentColor()))
        }
    }

    return segments
}
