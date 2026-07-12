package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.TourOverlay
import com.example.ui.AiAnalysisState
import com.example.ui.DetailAction
import com.example.ui.model.IconCategory
import com.example.ui.RouteViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import android.graphics.Color as AndroidColor
import java.util.concurrent.TimeUnit
import androidx.core.graphics.toColorInt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: RouteViewModel,
    routeId: Int,
    onBack: () -> Unit,
    onExperienceMap: (Int) -> Unit,
    onAnalytics: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val route by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.selectedDetailPoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()
    val favorites by viewModel.allFavorites.collectAsState()
    val linkedFavorites by viewModel.getFavoritesForRouteFlow(routeId).collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    var showTourFotos by remember { mutableStateOf(!viewModel.esTourVisto("fotos")) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxSize()) {
            // ESTADISTICAS CONSOLIDADAS
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val km = (route?.distanceMeters ?: 0.0) / 1000.0
                    val totalMs = route?.let { r -> if ((r.endTime ?: 0) > (r.startTime ?: 0)) r.endTime - r.startTime else 0L } ?: 0L
                    val movingMs = if (points.size > 1) {
                        var acc = 0L
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val cur = points[i]
                            if (cur.speed > 0.14f) acc += cur.timestamp - prev.timestamp
                        }
                        if (acc > 0L) acc else totalMs
                    } else totalMs
                    val avgSpeedKmh = if (movingMs > 0) (route?.distanceMeters ?: 0.0) / movingMs * 3600.0 else 0.0
                    val paceMinPerKm = if (avgSpeedKmh > 0) 60.0 / avgSpeedKmh else 0.0

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DetailStatItem(Icons.AutoMirrored.Filled.TrendingUp, "Distancia", String.format(Locale.US, "%.2f km", km), MaterialTheme.colorScheme.primary)
                        DetailStatItem(Icons.Default.Schedule, "Tiempo", formatMovingTime(movingMs), MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DetailStatItem(Icons.Default.Speed, "Vel. Prom", String.format(Locale.US, "%.1f km/h", avgSpeedKmh), MaterialTheme.colorScheme.tertiary)
                        val paceStr = if (paceMinPerKm > 0) {
                            val pMin = paceMinPerKm.toInt()
                            val pSec = ((paceMinPerKm - pMin) * 60).toInt()
                            String.format(Locale.US, "%d:%02d min/km", pMin, pSec)
                        } else "—"
                        DetailStatItem(Icons.Default.Timer, "Ritmo", paceStr, MaterialTheme.colorScheme.error)
                    }
                }
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clickable { photoToZoom = wp.photoPath }
                                    ) {
                                        AsyncImage(
                                            model = wp.photoPath,
                                            contentDescription = wp.name,
                                            modifier = Modifier.fillMaxSize().background(Color.LightGray)
                                        )
                                    }
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
                    
                    val aiState = viewModel.aiAnalysisState
                    when (aiState) {
                        is AiAnalysisState.Idle -> {
                            Button(
                                onClick = { viewModel.analyzeRouteWithAI(routeId) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Analizar con IA")
                            }
                        }
                        is AiAnalysisState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Analizando...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is AiAnalysisState.Success -> {
                            Text(text = aiState.report, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { viewModel.resetAiState() }, modifier = Modifier.align(Alignment.End)) {
                                Text("Volver a analizar")
                            }
                        }
                        is AiAnalysisState.MissingKey -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "⚠️ Configure su API Key en el menú de Ajustes.",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        is AiAnalysisState.ExpiredKey -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "⚠️ Su API Key ha vencido o se quedó sin créditos.",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    var newKey by remember { mutableStateOf("") }
                                    var editing by remember { mutableStateOf(false) }
                                    if (!editing) {
                                        TextButton(onClick = { editing = true }) {
                                            Text("Colocar una API Key nueva", color = MaterialTheme.colorScheme.error)
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = newKey,
                                            onValueChange = { newKey = it },
                                            label = { Text("Nueva API Key (gsk_...)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = { editing = false }) { Text("Cancelar") }
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                enabled = newKey.startsWith("gsk_"),
                                                onClick = {
                                                    viewModel.updateApiKey(newKey)
                                                    editing = false
                                                    viewModel.analyzeRouteWithAI(routeId)
                                                }
                                            ) { Text("Guardar y Reintentar") }
                                        }
                                    }
                                }
                            }
                        }
                        is AiAnalysisState.Error -> {
                            Text(
                                text = aiState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { viewModel.analyzeRouteWithAI(routeId) },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Reintentar") }
                        }
                    }
                }
            }

            // BOTON: IR A GRAFICOS DE RENDIMIENTO
            Button(
                onClick = { onAnalytics(routeId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver Gráficos de Rendimiento", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(80.dp))
        }

    // ─── Puente: escucha acciones desde la bottom bar ────────────────
    LaunchedEffect(viewModel.detailAction) {
        when (val action = viewModel.detailAction) {
            is DetailAction.ShowSheet -> showSheet = true
            is DetailAction.ShowEditDialog -> {
                editedName = action.currentName
                showEditDialog = true
            }
            is DetailAction.ShowDeleteDialog -> showDeleteDialog = true
            is DetailAction.ShowHelpDialog -> showHelpDialog = true
            null -> {}
        }
        viewModel.detailAction = null
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Vincular Favoritos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                if (favorites.isEmpty()) {
                    Text(
                        text = "No tienes favoritos guardados.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(favorites) { fav ->
                            val isLinked = linkedFavorites.any { it.id == fav.id }
                            ListItem(
                                headlineContent = { Text(fav.name, maxLines = 1) },
                                supportingContent = { Text(fav.address, maxLines = 1) },
                                leadingContent = {
                                    Icon(
                                        IconCategory.fromKey(fav.iconCategory).icon,
                                        null,
                                        tint = Color(fav.color.toColorInt()),
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        if (isLinked) viewModel.removeFavoriteFromRoute(routeId, fav.id)
                                        else viewModel.addFavoriteToRoute(routeId, fav.id)
                                    }) {
                                        Icon(
                                            imageVector = if (isLinked) Icons.Default.Check else Icons.Default.Add,
                                            tint = if (isLinked) Color.Green else Color.Gray,
                                            contentDescription = if (isLinked) "Desvincular" else "Vincular"
                                        )
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar ruta") },
            text = { Text("Esta acción no se puede deshacer. Se eliminarán todos los puntos GPS, waypoints y fotos asociados.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRoute(routeId)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Ayuda de la pantalla") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HelpItem(Icons.Default.Explore, "Mapa de Experiencia", "Visualiza el recorrido completo con altimetría y velocidad en 3D/interactivo.")
                    HelpItem(Icons.Default.BookmarkBorder, "Vincular Favoritos", "Guarda esta ruta en tu lista de ubicaciones preferidas.")
                    HelpItem(Icons.Default.Edit, "Editar nombre", "Cambia el nombre de la ruta.")
                    HelpItem(Icons.AutoMirrored.Filled.Send, "Compartir", "Genera un ZIP con telemetría GPX+CSV y fotos para compartir por mensajería.")
                    HelpItem(Icons.Default.Archive, "Exportar", "Convierte tus datos a formatos estándar (GPX para otras apps, CSV para Excel, ZIP para respaldos completos).")
                    HelpItem(Icons.Default.Delete, "Eliminar ruta", "Borra permanentemente la ruta y todos sus datos asociados.")
                }
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Cerrar") } }
        )
    }

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )

    if (showTourFotos) {
        TourOverlay(
            message = "¡Toca cualquier foto para verla a pantalla completa!",
            onDismiss = {
                showTourFotos = false
                viewModel.marcarTourVisto("fotos")
            }
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


private fun formatMovingTime(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d h", h, m, s)
    else String.format(Locale.US, "%02d:%02d min", m, s)
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

@Composable
fun RouteOptionsMenu(
    onShare: () -> Unit,
    onExportGpx: () -> Unit,
    onExportCsv: () -> Unit,
    onExportZip: () -> Unit,
    onDelete: () -> Unit,
    onHelp: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var exportExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Compartir ruta") },
                onClick = { expanded = false; onShare() },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null) }
            )
            DropdownMenuItem(
                text = { Text("Exportar") },
                trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                onClick = { exportExpanded = true }
            )
            DropdownMenu(
                expanded = exportExpanded,
                onDismissRequest = { exportExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("GPX") },
                    onClick = { exportExpanded = false; expanded = false; onExportGpx() },
                    leadingIcon = { Icon(Icons.Default.Description, null) }
                )
                DropdownMenuItem(
                    text = { Text("CSV (Excel)") },
                    onClick = { exportExpanded = false; expanded = false; onExportCsv() },
                    leadingIcon = { Icon(Icons.Default.TableChart, null) }
                )
                DropdownMenuItem(
                    text = { Text("ZIP") },
                    onClick = { exportExpanded = false; expanded = false; onExportZip() },
                    leadingIcon = { Icon(Icons.Default.Archive, null) }
                )
            }
            DropdownMenuItem(
                text = { Text("Ayuda de la pantalla") },
                onClick = { expanded = false; onHelp() },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Eliminar ruta", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
fun HelpItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

