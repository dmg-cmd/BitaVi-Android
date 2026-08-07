package com.aistudio.localizador.ui.screens

import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.graphics.toColorInt
import android.graphics.Color as AndroidColor


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.localizador.ui.RouteViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteAnalyticsScreen(
    viewModel: RouteViewModel,
    modifier: Modifier = Modifier
) {
    val route by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.selectedDetailPoints.collectAsState()

    val lineColor = when (route?.activityMode) {
        "WALKING" -> "#4CAF50".toColorInt()
        "BIKING" -> "#2196F3".toColorInt()
        "DRIVING" -> "#F44336".toColorInt()
        else -> "#9E9E9E".toColorInt()
    }

    val maxIndex = (points.size - 1).coerceAtLeast(0).toFloat()
    var sliderRange by remember { mutableStateOf(0f..maxIndex) }

    LaunchedEffect(points) {
        val idx = (points.size - 1).coerceAtLeast(0).toFloat()
        sliderRange = 0f..idx
    }

    val rStart = sliderRange.start.toInt().coerceIn(0, points.lastIndex.coerceAtLeast(0))
    val rEnd = sliderRange.endInclusive.toInt().coerceIn(0, points.lastIndex.coerceAtLeast(0))
    val tramoPoints = if (points.isNotEmpty() && rStart <= rEnd) points.slice(rStart..rEnd) else emptyList()

    val startTs = if (points.isNotEmpty()) points.minOf { it.timestamp } else 0L
    val elapsedLabels = if (points.isNotEmpty()) {
        points.map { pt ->
            val sec = (pt.timestamp - startTs) / 1000
            val m = sec / 60
            val s = sec % 60
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    } else emptyList()

    Column(modifier = modifier.fillMaxSize()) {
        if (points.size < 3) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = route?.name ?: "An\u00e1lisis de Rendimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        val sdf = SimpleDateFormat("HH:mm", LocalConfiguration.current.locales[0])
                        val inicio = if (route != null) sdf.format(Date(route!!.startTime)) else "--:--"
                        append(inicio)
                        append("  \u2022  ")
                        append(elapsedLabels.lastOrNull() ?: "00:00")
                        append("  \u2022  ")
                        append(points.size)
                        append(" pts")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (route?.activityMode) {
                        "WALKING" -> "🟢 Caminata"
                        "BIKING" -> "🔵 Ciclismo"
                        "DRIVING" -> "🔴 Autom\u00f3vil"
                        else -> "\u26aa Sin modo"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terrain, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Perfil de Elevaci\u00f3n", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("Altura (m) vs Tiempo transcurrido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        AnalyticsChart(
                            data = points.map { it.altitude.toFloat() },
                            labels = elapsedLabels,
                            color = lineColor,
                            rangeStart = sliderRange.start,
                            rangeEnd = sliderRange.endInclusive,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Perfil de Ritmo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("Velocidad (km/h) vs Tiempo transcurrido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        AnalyticsChart(
                            data = points.map { it.speed * 3.6f },
                            labels = elapsedLabels,
                            color = lineColor,
                            rangeStart = sliderRange.start,
                            rangeEnd = sliderRange.endInclusive,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Selector de Tramo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = elapsedLabels.getOrElse(rStart) { "00:00" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = elapsedLabels.getOrElse(rEnd) { "00:00" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        RangeSlider(value = sliderRange,
                            onValueChange = { sliderRange = it },
                            valueRange = 0f..maxIndex,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("An\u00e1lisis del Tramo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        if (tramoPoints.size < 2) {
                            Text(
                                "Selecciona un rango con al menos 2 puntos para ver el an\u00e1lisis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val tramoAltDelta = tramoPoints.last().altitude - tramoPoints.first().altitude
                            val tramoAvgSpeedMs = tramoPoints.filter { it.speed > 0.14f }.let { pts ->
                                if (pts.isNotEmpty()) pts.map { it.speed }.average() else 0.0
                            }
                            val tramoAvgSpeedKmh = tramoAvgSpeedMs * 3.6
                            val tramoPace = if (tramoAvgSpeedKmh > 0) 60.0 / tramoAvgSpeedKmh else 0.0
                            val tramoActivity = when {
                                tramoAvgSpeedKmh < 0.5 -> "Detenido"
                                tramoAvgSpeedKmh < 6.0 -> "Caminata"
                                tramoAvgSpeedKmh < 20.0 -> "Ciclismo"
                                else -> "Autom\u00f3vil"
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                DetailStatItem(
                                    Icons.Default.Speed,
                                    "Vel. Prom",
                                    String.format(Locale.US, "%.1f km/h", tramoAvgSpeedKmh),
                                    MaterialTheme.colorScheme.primary
                                )
                                DetailStatItem(
                                    Icons.Default.Terrain,
                                    "Desnivel",
                                    String.format(Locale.US, "%+.0f m", tramoAltDelta),
                                    if (tramoAltDelta >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                val paceStr = if (tramoPace > 0) {
                                    val pMin = tramoPace.toInt()
                                    val pSec = ((tramoPace - pMin) * 60).toInt()
                                    String.format(Locale.US, "%d:%02d min/km", pMin, pSec)
                                } else "\u2014"
                                DetailStatItem(Icons.Default.Timer, "Ritmo", paceStr, MaterialTheme.colorScheme.error)
                                DetailStatItem(Icons.AutoMirrored.Filled.DirectionsWalk, "Modo", tramoActivity, MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AnalyticsChart(
    modifier: Modifier = Modifier,
    data: List<Float>,
    labels: List<String>,
    color: Int,
    rangeStart: Float = 0f,
    rangeEnd: Float = 0f,
) {
    val xLabels = labels

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = AndroidColor.GRAY
                    textSize = 10f
                    isGranularityEnabled = true
                    granularity = 60f
                    setLabelCount(8, true)
                    setAvoidFirstLastClipping(false)
                    valueFormatter = IndexAxisValueFormatter(xLabels)
                }

                axisLeft.apply {
                    textColor = AndroidColor.GRAY
                    setDrawGridLines(true)
                    gridColor = "#33FFFFFF".toColorInt()
                }

                axisRight.isEnabled = false
                legend.isEnabled = false
                setBackgroundColor("#08000000".toColorInt())
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { i, v -> Entry(i.toFloat(), v) }
            val dataSet = LineDataSet(entries, "").apply {
                this.color = color
                setDrawValues(false)
                setDrawCircles(false)
                lineWidth = 2.5f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = color
                fillAlpha = 30
            }
            chart.data = LineData(dataSet)
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = (data.size - 1).coerceAtLeast(0).toFloat()
            chart.xAxis.removeAllLimitLines()
            if (rangeEnd > rangeStart) {
                chart.xAxis.addLimitLine(LimitLine(rangeStart).apply {
                    lineColor = AndroidColor.WHITE
                    lineWidth = 1.5f
                    enableDashedLine(12f, 6f, 0f)
                })
                chart.xAxis.addLimitLine(LimitLine(rangeEnd).apply {
                    lineColor = AndroidColor.WHITE
                    lineWidth = 1.5f
                    enableDashedLine(12f, 6f, 0f)
                })
            }
            chart.invalidate()
        }
    )
}
