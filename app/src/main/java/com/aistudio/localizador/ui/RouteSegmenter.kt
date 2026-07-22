package com.aistudio.localizador.ui

import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.domain.usecase.RouteColorUseCase
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

/**
 * Genera segmentos de polilínea con gradiente térmico continuo basado en velocidad.
 * Cada segmento conecta un par consecutivo de puntos con el color interpolado
 * según la velocidad promedio del tramo: Azul (lento) → Rojo (rápido).
 */
fun List<LocationPoint>.toGradientRouteSegments(
    colorUseCase: RouteColorUseCase
): List<SegmentoRuta> {
    if (size < 2) return emptyList()

    val maxSpeed = maxOfOrNull { it.speed } ?: 1.0f
    val minSpeed = minOfOrNull { it.speed } ?: 0.0f

    return zipWithNext { p1, p2 ->
        val avgSpeed = (p1.speed + p2.speed) / 2.0f
        val segmentColor = colorUseCase.speedToGradientColor(
            speedMps = avgSpeed,
            minSpeedMps = minSpeed,
            maxSpeedMps = maxSpeed
        )

        SegmentoRuta(
            points = listOf(
                GeoPoint(p1.latitude, p1.longitude),
                GeoPoint(p2.latitude, p2.longitude)
            ),
            mode = avgSpeed.toActivityMode(),
            color = segmentColor
        )
    }
}
