package com.aistudio.localizador.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.localizador.ui.components.FullScreenPhotoDialog
import com.aistudio.localizador.ui.components.TourOverlay
import com.aistudio.localizador.ui.AiAnalysisState
import com.aistudio.localizador.ui.DetailAction
import com.aistudio.localizador.ui.model.IconCategory
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.theme.IconExplorar
import com.aistudio.localizador.ui.theme.IconFavorito
import com.aistudio.localizador.ui.theme.IconExportar
import com.aistudio.localizador.ui.theme.IconCompartir
import com.aistudio.localizador.ui.theme.IconGps
import java.util.concurrent.TimeUnit
import androidx.core.graphics.toColorInt
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.util.Log

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
    val route by viewModel.selectedDetailRoute.collectAsState()
    val points by viewModel.selectedDetailPoints.collectAsState()
    val waypoints by viewModel.selectedDetailWaypoints.collectAsState()
    val favorites by viewModel.allFavorites.collectAsState()
    val linkedFavorites by viewModel.getFavoritesForRouteFlow(routeId).collectAsState(initial = emptyList())
    val gpsPrecisionState by viewModel.gpsAccuracy.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    var showTourFotos by remember { mutableStateOf(!viewModel.esTourVisto("fotos")) }
    var showRouteMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
        ) {
            // ESTADISTICAS CONSOLIDADAS (4.4)
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
                        } else "\u2014"
                        DetailStatItem(Icons.Default.Timer, "Ritmo", paceStr, MaterialTheme.colorScheme.error)
                    }
                }
            }

            // PUNTOS DE INTERES Y FOTOS (4.5)
            WaypointsHorizontalSelector(
                waypoints = waypoints,
                onWaypointClick = { wp ->
                    if (wp.photoPath != null) { photoToZoom = wp.photoPath }
                },
                onAddWaypointClick = {
                    viewModel.prepareWaypointCreation(routeId)
                }
            )

            LaunchedEffect(routeId, waypoints) {
                Log.d("BitaViDebug", "Puntos de inter\u00e9s: ${waypoints.size}")
            }

            // ANALISIS CON IA (4.6)
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

                    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
                    val aiStateValue = aiState
                    when (aiStateValue) {
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
                            Text(text = aiStateValue.report, style = MaterialTheme.typography.bodyMedium)
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
                                        "\u26a0\ufe0f Configure su API Key en el men\u00fa de Ajustes.",
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
                                            "\u26a0\ufe0f Su API Key ha vencido o se qued\u00f3 sin cr\u00e9ditos.",
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
                                text = aiStateValue.message,
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

            // BOTON: IR A GRAFICOS DE RENDIMIENTO (4.7)
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
                Text("Ver Gr\u00e1ficos de Rendimiento", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(80.dp))
        }

        // DOCK INFERIOR UNIFICADO (4.2 + 4.3 reconcilidas)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.70f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 4.3.1 - Retroceso
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver atr\u00e1s",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 4.2.1 - Explorar experiencia (Violeta)
                IconButton(onClick = { onExperienceMap(routeId) }) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Explorar recorrido",
                        tint = IconExplorar
                    )
                }

                // 4.2.2 - Vincular favoritos (Dorado)
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Vincular a favoritos",
                        tint = IconFavorito
                    )
                }

                // 4.2.3 - Editar nombre (Amber)
                IconButton(onClick = {
                    editedName = route?.name ?: ""
                    showEditDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ruta",
                        tint = IconExportar
                    )
                }

                // 4.2.4 - Menu contextual (Cian)
                Box {
                    IconButton(onClick = { showRouteMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "M\u00e1s opciones",
                            tint = IconCompartir
                        )
                    }
                    RouteOptionsMenu(
                        expanded = showRouteMenu,
                        onDismiss = { showRouteMenu = false },
                        onShare = { showRouteMenu = false; viewModel.exportRoute(routeId) },
                        onExportGpx = { showRouteMenu = false; viewModel.exportRoutesAsGPX(setOf(routeId)) },
                        onExportCsv = { showRouteMenu = false; viewModel.exportRoutesAsCSV(routeId) },
                        onExportZip = { showRouteMenu = false; viewModel.exportarRutaZip(routeId) },
                        onDelete = { showRouteMenu = false; showDeleteDialog = true },
                        onHelp = { showRouteMenu = false; showHelpDialog = true }
                    )
                }


                // 4.3.2 - Titulo unificado (con peso elastico)
                Text(
                    text = "Detalle de la Ruta",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                // 4.2.5 - Indicador GPS (Verde)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Se\u00f1al de precisi\u00f3n",
                        tint = IconGps,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = gpsPrecisionState,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ─── Puente: escucha acciones desde vistas externas ────────────
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
                                            tint = if (isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = { Text("Esta acci\u00f3n no se puede deshacer. Se eliminar\u00e1n todos los puntos GPS, waypoints y fotos asociados.") },
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
                    HelpItem(Icons.Default.Explore, "Mapa de Experiencia", "Visualiza el recorrido completo con altimetr\u00eda y velocidad en 3D/interactivo.")
                    HelpItem(Icons.Default.BookmarkBorder, "Vincular Favoritos", "Guarda esta ruta en tu lista de ubicaciones preferidas.")
                    HelpItem(Icons.Default.Edit, "Editar nombre", "Cambia el nombre de la ruta.")
                    HelpItem(Icons.AutoMirrored.Filled.Send, "Compartir", "Genera un ZIP con telemetr\u00eda GPX+CSV y fotos para compartir por mensajer\u00eda.")
                    HelpItem(Icons.Default.Archive, "Exportar", "Convierte tus datos a formatos est\u00e1ndar (GPX para otras apps, CSV para Excel, ZIP para respaldos completos).")
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
            message = "\u00a1Toca cualquier foto para verla a pantalla completa!",
            onDismiss = {
                showTourFotos = false
                viewModel.marcarTourVisto("fotos")
            }
        )
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

@Composable
fun RouteOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExportGpx: () -> Unit,
    onExportCsv: () -> Unit,
    onExportZip: () -> Unit,
    onDelete: () -> Unit,
    onHelp: () -> Unit
) {
    var exportExpanded by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Compartir ruta") },
            onClick = { onShare() },
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
                onClick = { exportExpanded = false; onDismiss(); onExportGpx() },
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
            DropdownMenuItem(
                text = { Text("CSV (Excel)") },
                onClick = { exportExpanded = false; onDismiss(); onExportCsv() },
                leadingIcon = { Icon(Icons.Default.TableChart, null) }
            )
            DropdownMenuItem(
                text = { Text("ZIP") },
                onClick = { exportExpanded = false; onDismiss(); onExportZip() },
                leadingIcon = { Icon(Icons.Default.Archive, null) }
            )
        }
        DropdownMenuItem(
            text = { Text("Ayuda de la pantalla") },
            onClick = { onHelp() },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Eliminar ruta", color = MaterialTheme.colorScheme.error) },
            onClick = { onDelete() },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
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

