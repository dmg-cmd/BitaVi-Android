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
    private val reusableGeoPoint = GeoPoint(0.0, 0.0)
    private val ptPrev = Point()
    private val ptCurr = Point()

    fun updatePoints(newPoints: List<LocationPoint>) {
        routePoints = newPoints
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (shadow) return
        val c = canvas ?: return
        val mv = mapView ?: return
        val total = routePoints.size
        if (total < 2 || mv.width <= 0 || mv.height <= 0) return

        val projection = mv.projection
        val density = mv.resources.displayMetrics.density
        val wingLength = 8.5f * density
        val targetSpacingPx = 65f * density
        val headAngle = Math.toRadians(32.0)

        borderPaint.strokeWidth = 5.0f * density
        fillPaint.strokeWidth = 2.8f * density

        val margin = 40f * density
        val viewWidth = mv.width.toFloat()
        val viewHeight = mv.height.toFloat()

        var arrowsDrawn = 0
        var accumulatedPx = 0f

        // Inicializamos con el primer punto proyectado
        val firstPoint = routePoints[0]
        reusableGeoPoint.setCoords(firstPoint.latitude, firstPoint.longitude)
        projection.toPixels(reusableGeoPoint, ptPrev)

        var anchorX = ptPrev.x.toFloat()
        var anchorY = ptPrev.y.toFloat()

        for (k in 1 until total) {
            val p = routePoints[k]
            reusableGeoPoint.setCoords(p.latitude, p.longitude)
            projection.toPixels(reusableGeoPoint, ptCurr)

            val currX = ptCurr.x.toFloat()
            val currY = ptCurr.y.toFloat()
            val prevX = ptPrev.x.toFloat()
            val prevY = ptPrev.y.toFloat()

            val dx = currX - prevX
            val dy = currY - prevY
            val stepDist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            // Descartar micro-jitter prácticamente en el mismo píxel
            if (stepDist >= 0.5f) {
                accumulatedPx += stepDist

                if (accumulatedPx >= targetSpacingPx) {
                    val dirX = currX - anchorX
                    val dirY = currY - anchorY
                    val dirDist = hypot(dirX.toDouble(), dirY.toDouble()).toFloat()

                    val effectiveAngle = if (dirDist >= 4f * density) {
                        atan2(dirY.toDouble(), dirX.toDouble())
                    } else if (stepDist >= 2f * density) {
                        atan2(dy.toDouble(), dx.toDouble())
                    } else {
                        Double.NaN
                    }

                    if (!effectiveAngle.isNaN()) {
                        val midX = (prevX + currX) * 0.5f
                        val midY = (prevY + currY) * 0.5f

                        val inViewport = midX >= -margin && midX <= viewWidth + margin &&
                                midY >= -margin && midY <= viewHeight + margin

                        if (inViewport) {
                            drawChevron(c, midX, midY, effectiveAngle, wingLength, headAngle)
                            arrowsDrawn++
                        }
                    }

                    accumulatedPx = 0f
                    anchorX = currX
                    anchorY = currY
                }
            }

            ptPrev.x = ptCurr.x
            ptPrev.y = ptCurr.y
        }

        // Si la ruta es corta o no alcanzó a acumular targetSpacingPx:
        // Garantizamos al menos una flecha direccional en el tramo central
        if (arrowsDrawn == 0 && total >= 2) {
            val midIdx = total / 2
            val startIdx = maxOf(0, midIdx - 1)
            val endIdx = minOf(total - 1, midIdx + 1)

            val pStart = routePoints[startIdx]
            val pEnd = routePoints[endIdx]

            reusableGeoPoint.setCoords(pStart.latitude, pStart.longitude)
            projection.toPixels(reusableGeoPoint, ptPrev)

            reusableGeoPoint.setCoords(pEnd.latitude, pEnd.longitude)
            projection.toPixels(reusableGeoPoint, ptCurr)

            val dx = (ptCurr.x - ptPrev.x).toFloat()
            val dy = (ptCurr.y - ptPrev.y).toFloat()
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (dist >= 4f * density) {
                val midX = (ptPrev.x + ptCurr.x) * 0.5f
                val midY = (ptPrev.y + ptCurr.y) * 0.5f
                val angle = atan2(dy.toDouble(), dx.toDouble())
                drawChevron(c, midX, midY, angle, wingLength, headAngle)
            } else {
                val first = routePoints.first()
                val last = routePoints.last()
                reusableGeoPoint.setCoords(first.latitude, first.longitude)
                projection.toPixels(reusableGeoPoint, ptPrev)
                reusableGeoPoint.setCoords(last.latitude, last.longitude)
                projection.toPixels(reusableGeoPoint, ptCurr)

                val altDx = (ptCurr.x - ptPrev.x).toFloat()
                val altDy = (ptCurr.y - ptPrev.y).toFloat()
                if (hypot(altDx.toDouble(), altDy.toDouble()) >= 4f * density) {
                    val midX = (ptPrev.x + ptCurr.x) * 0.5f
                    val midY = (ptPrev.y + ptCurr.y) * 0.5f
                    val angle = atan2(altDy.toDouble(), altDx.toDouble())
                    drawChevron(c, midX, midY, angle, wingLength, headAngle)
                }
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
