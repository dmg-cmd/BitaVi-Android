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
    this * 3.6f < 2.0f -> ActivityMode.STOPPED
    this * 3.6f <= 7.0f -> ActivityMode.WALKING
    this * 3.6f <= 20.0f -> ActivityMode.BIKING
    else -> ActivityMode.DRIVING
}

fun ActivityMode.toSegmentColor(): Int = when (this) {
    ActivityMode.STOPPED -> RouteColorUseCase.COLOR_STOPPED_SLOW
    ActivityMode.WALKING -> RouteColorUseCase.COLOR_WALKING
    ActivityMode.BIKING -> RouteColorUseCase.COLOR_BIKING
    ActivityMode.DRIVING -> RouteColorUseCase.COLOR_DRIVING
    ActivityMode.SIN_SENAL -> RouteColorUseCase.COLOR_STOPPED_SLOW
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
 * Genera segmentos de polilínea con colores tácticos de alto contraste basados en velocidad real (km/h).
 * Evita tonos claros (amarillo, celeste o verde lima) que se pierden sobre mapas claros:
 * - < 2 km/h: Rojo intenso (#D32F2F)
 * - 2-7 km/h: Verde esmeralda oscuro (#2E7D32)
 * - 7-20 km/h: Azul profundo (#1565C0)
 * - > 20 km/h: Violeta intenso (#7B1FA2)
 */
fun List<LocationPoint>.toGradientRouteSegments(
    colorUseCase: RouteColorUseCase
): List<SegmentoRuta> {
    if (size < 2) return emptyList()

    return zipWithNext { p1, p2 ->
        val avgSpeed = (p1.speed + p2.speed) / 2.0f
        val segmentColor = colorUseCase.speedToTacticalColor(avgSpeed)

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

