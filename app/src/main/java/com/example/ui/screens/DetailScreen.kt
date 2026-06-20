package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.RouteViewModel
import com.example.ui.Screen
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import android.graphics.Color as AndroidColor
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: RouteViewModel,
    routeId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val route by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.selectedDetailPoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    val mapView = remember { MapView(context) }

    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
            mapView.post { mapView.zoomToBoundingBox(boundingBox, true, 120) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Detalle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.History) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Captura del mapa optimizada
                        try {
                            if (mapView.width > 0 && mapView.height > 0) {
                                val bitmap = android.graphics.Bitmap.createBitmap(mapView.width, mapView.height, android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                mapView.draw(canvas)
                                viewModel.shareCompleteRoute(context, routeId, bitmap)
                            } else {
                                viewModel.shareCompleteRoute(context, routeId, null)
                            }
                        } catch (e: Exception) {
                            viewModel.shareCompleteRoute(context, routeId, null)
                        }
                    }) {
                        Icon(Icons.Default.Send, "Enviar Resumen", tint = Color(0xFF25D366)) // Verde WhatsApp
                    }
                    IconButton(onClick = { viewModel.exportRoutesAsGPX(context, setOf(routeId)) }) {
                        Icon(Icons.Default.Description, "Archivo GPX", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.exportRoutesAsCSV(context, routeId) }) {
                        Icon(Icons.Default.TableChart, "Exportar Excel", tint = Color(0xFF2E7D32))
                    }
                    IconButton(onClick = { 
                        editedName = route?.name ?: ""
                        showEditDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, "Editar nombre")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
            // MAPA
            Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                if (points.isEmpty()) {
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
                            view.overlays.clear()
                            val poly = Polyline().apply {
                                val sortedPoints = points.sortedBy { it.timestamp }
                                setPoints(sortedPoints.map { GeoPoint(it.latitude, it.longitude) })
                                outlinePaint.color = android.graphics.Color.RED
                                outlinePaint.strokeWidth = 5f
                            }
                            view.overlays.add(poly)
                            
                            // Marcadores de Waypoints
                            waypoints.forEach { wp ->
                                val marker = Marker(view).apply {
                                    position = GeoPoint(wp.latitude, wp.longitude)
                                    title = wp.name
                                    snippet = wp.description
                                }
                                view.overlays.add(marker)
                            }
                            
                            view.invalidate()
                        }
                    )
                }
            }

            // ESTADISTICAS
            Card(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    val km = (route?.distanceMeters ?: 0.0) / 1000.0
                    val duration = if ((route?.endTime ?: 0) > (route?.startTime ?: 0)) route!!.endTime - route!!.startTime else 0L
                    DetailStatItem(Icons.Default.TrendingUp, "Distancia", String.format("%.2f km", km), MaterialTheme.colorScheme.primary)
                    DetailStatItem(Icons.Default.Schedule, "Duración", formatDuration(duration), MaterialTheme.colorScheme.secondary)
                }
            }

            // GRÁFICOS PROFESIONALES
            if (points.size > 2) {
                Text("Perfil de Altura (m)", modifier = Modifier.padding(start = 16.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ProfessionalChart(
                    data = points.map { it.altitude.toFloat() }, 
                    color = AndroidColor.parseColor("#4CAF50"), 
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(16.dp)
                )
                
                Text("Velocidad (km/h)", modifier = Modifier.padding(start = 16.dp, top = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ProfessionalChart(
                    data = points.map { it.speed * 3.6f }, 
                    color = AndroidColor.parseColor("#2196F3"), 
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(16.dp)
                )
            }

            // PUNTOS DE INTERÉS Y FOTOS
            if (waypoints.isNotEmpty()) {
                Text("Puntos de Interés", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(waypoints) { wp ->
                        Card(
                            modifier = Modifier.width(200.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                if (wp.photoPath != null) {
                                    AsyncImage(
                                        model = wp.photoPath,
                                        contentDescription = wp.name,
                                        modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.LightGray)
                                    )
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = wp.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                    if (wp.author.isNotBlank()) {
                                        Text(text = "Por: ${wp.author}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(text = wp.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // ANÁLISIS CON IA
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Resumen Inteligente (Llama 3)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    if (viewModel.aiAnalysisResult != null) {
                        Text(text = viewModel.aiAnalysisResult!!, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Button(
                            onClick = { viewModel.analyzeRouteWithAI(routeId) },
                            enabled = !viewModel.isAnalyzing,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (viewModel.isAnalyzing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("Analizar con IA")
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar nombre") },
            text = {
                OutlinedTextField(value = editedName, onValueChange = { editedName = it }, label = { Text("Nuevo nombre") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    if (editedName.isNotBlank()) {
                        viewModel.renameRoute(routeId, editedName)
                        showEditDialog = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun ProfessionalChart(data: List<Float>, color: Int, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
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
                }
                
                axisLeft.apply {
                    textColor = AndroidColor.GRAY
                    setDrawGridLines(true)
                }
                
                axisRight.isEnabled = false
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { index, value -> Entry(index.toFloat(), value) }
            val dataSet = LineDataSet(entries, "Datos").apply {
                this.color = color
                setDrawValues(false)
                setDrawCircles(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = color
                fillAlpha = 50
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun ElevationChart(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    val max = data.maxOrNull() ?: 1f
    val min = data.minOrNull() ?: 0f
    val range = if (max - min == 0f) 1f else max - min

    Canvas(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))) {
        val width = size.width
        val height = size.height
        val stepX = if (data.size > 1) width / (data.size - 1) else width
        
        val path = Path()
        data.forEachIndexed { i, d ->
            val x = i * stepX
            val y = height - ((d - min) / range * height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(path, color = color, style = Stroke(width = 4f))
    }
}

@Composable
fun DetailStatItem(icon: ImageVector, title: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
