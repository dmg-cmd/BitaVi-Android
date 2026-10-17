package com.aistudio.localizador.ui.screens.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import com.aistudio.localizador.data.model.LocationPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Overlay táctico ligero (Anti-OOM) que dibuja flechas direccionales sobre el trazado
 * de la ruta cada N puntos (con control de separación en pantalla según el zoom),
 * permitiendo distinguir de un vistazo el sentido de avance (ida vs. vuelta).
 */
class RouteDirectionArrowsOverlay(
    private var routePoints: List<LocationPoint> = emptyList(),
    private val stepPoints: Int = 12
) : Overlay() {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 15, 15, 20)
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val arrowPath = Path()
    private val pt1 = Point()
    private val pt2 = Point()

    fun updatePoints(newPoints: List<LocationPoint>) {
        routePoints = newPoints
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow) return
        val c = canvas ?: return
        val mv = mapView ?: return
        if (routePoints.size < 2 || mv.width <= 0 || mv.height <= 0) return

        val projection = mv.projection
        val density = mv.resources.displayMetrics.density
        val wingLength = 9f * density
        val minScreenSpacingPx = 42f * density
        val headAngle = Math.toRadians(32.0)

        borderPaint.strokeWidth = 5.5f * density
        fillPaint.strokeWidth = 3.0f * density

        var lastDrawnX = Float.NaN
        var lastDrawnY = Float.NaN
        var arrowsDrawn = 0

        val total = routePoints.size
        val effectiveStep = if (total < stepPoints + 2) {
            maxOf(1, total / 2)
        } else {
            stepPoints
        }

        var i = effectiveStep
        while (i < total) {
            val prevIdx = maxOf(0, i - 2)
            val pStart = routePoints[prevIdx]
            val pEnd = routePoints[i]

            projection.toPixels(GeoPoint(pStart.latitude, pStart.longitude), pt1)
            projection.toPixels(GeoPoint(pEnd.latitude, pEnd.longitude), pt2)

            val dx = (pt2.x - pt1.x).toFloat()
            val dy = (pt2.y - pt1.y).toFloat()
            val segmentPx = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            // Ignorar si los dos puntos están prácticamente en el mismo píxel (detenido)
            if (segmentPx >= 4f * density) {
                val midX = (pt1.x + pt2.x) * 0.5f
                val midY = (pt1.y + pt2.y) * 0.5f

                // Solo dibujar si está dentro o cerca del área visible de la pantalla
                val margin = 40f * density
                val inViewport = midX >= -margin && midX <= mv.width + margin &&
                        midY >= -margin && midY <= mv.height + margin

                val farEnoughFromPrev = lastDrawnX.isNaN() ||
                        hypot((midX - lastDrawnX).toDouble(), (midY - lastDrawnY).toDouble()) >= minScreenSpacingPx

                if (inViewport && farEnoughFromPrev) {
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    drawChevron(c, midX, midY, angle, wingLength, headAngle)
                    lastDrawnX = midX
                    lastDrawnY = midY
                    arrowsDrawn++
                }
            }

            i += effectiveStep
        }

        // Si por baja cantidad de puntos no se llegó a dibujar ninguna flecha pero hay desplazamiento visible,
        // dibujar al menos una flecha entre el primer y último punto para indicar el sentido general.
        if (arrowsDrawn == 0 && total >= 2) {
            val first = routePoints.first()
            val last = routePoints.last()
            projection.toPixels(GeoPoint(first.latitude, first.longitude), pt1)
            projection.toPixels(GeoPoint(last.latitude, last.longitude), pt2)
            val dx = (pt2.x - pt1.x).toFloat()
            val dy = (pt2.y - pt1.y).toFloat()
            if (hypot(dx.toDouble(), dy.toDouble()) >= 8f * density) {
                val midX = (pt1.x + pt2.x) * 0.5f
                val midY = (pt1.y + pt2.y) * 0.5f
                val angle = atan2(dy.toDouble(), dx.toDouble())
                drawChevron(c, midX, midY, angle, wingLength, headAngle)
            }
        }
    }

    private fun drawChevron(
        canvas: Canvas,
        tipX: Float,
        tipY: Float,
        angleRad: Double,
        wingLength: Float,
        headAngleRad: Double
    ) {
        val leftX = (tipX - wingLength * cos(angleRad - headAngleRad)).toFloat()
        val leftY = (tipY - wingLength * sin(angleRad - headAngleRad)).toFloat()
        val rightX = (tipX - wingLength * cos(angleRad + headAngleRad)).toFloat()
        val rightY = (tipY - wingLength * sin(angleRad + headAngleRad)).toFloat()

        arrowPath.reset()
        arrowPath.moveTo(leftX, leftY)
        arrowPath.lineTo(tipX, tipY)
        arrowPath.lineTo(rightX, rightY)

        canvas.drawPath(arrowPath, borderPaint)
        canvas.drawPath(arrowPath, fillPaint)
    }
}
