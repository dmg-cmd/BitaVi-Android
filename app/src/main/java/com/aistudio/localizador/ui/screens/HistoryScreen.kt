package com.aistudio.localizador.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.domain.usecase.DayGroupUiModel
import com.aistudio.localizador.ui.RouteViewModel
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HistoryScreen(
    viewModel: RouteViewModel,
    onRouteClick: (Int) -> Unit,
    onNavigateToTracking: () -> Unit,
    onImportRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val routes by viewModel.allRoutes.collectAsState()
    val groupedRoutes by viewModel.groupedRoutesState.collectAsState()
    val selectedRouteIds by viewModel.selectedRouteIds.collectAsState()
    val isSelectionMode = selectedRouteIds.isNotEmpty()
    val collapsedDates by viewModel.collapsedDates.collectAsState()
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val sortedRoutes = remember(routes) {
        routes.sortedByDescending { it.startTime }
    }

    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.importGpxFile(it) }
        }
    )

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.importarRutaZip(it) }
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (viewModel.isImporting) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Importando recorrido grande...", fontWeight = FontWeight.Bold)
                        Text("Esto puede tardar unos segundos", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (routes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryToggleOff,
                    contentDescription = "Historial vacio",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Aun no hay rutas guardadas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "!Sal al aire libre y graba tu primera aventura!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNavigateToTracking) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grabar")
                    }
                    OutlinedButton(onClick = { gpxPickerLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar GPX")
                    }
                    OutlinedButton(onClick = { zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }) {
                        Icon(Icons.Default.Unarchive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar ZIP")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mis Recorridos",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = groupedRoutes, key = { it.date.toString() }) { dayGroup ->
                        val isCollapsed = collapsedDates.contains(dayGroup.date)
                        DayGroupHeader(
                            formattedDate = dayGroup.formattedDate,
                            colorInt = dayGroup.colorInt,
                            isCollapsed = isCollapsed,
                            onToggle = {
                                viewModel.toggleDateCollapse(dayGroup.date)
                            }
                        )
                        AnimatedVisibility(
                            visible = !isCollapsed,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                dayGroup.routes.forEach { route ->
                                    val isSelected = selectedRouteIds.contains(route.id)
                                    RouteCard(
                                        route = route,
                                        stripeColor = Color(dayGroup.colorInt),
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            if (isSelectionMode) {
                                                viewModel.toggleSelection(route.id)
                                            } else {
                                                viewModel.selectRouteForDetail(route.id)
                                                onRouteClick(route.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                viewModel.toggleSelection(route.id)
                                            }
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showImportDialog = true },
            icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
            text = { Text("Importar") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar " + selectedRouteIds.size + " recorridos?") },
            text = { Text("Se eliminaran de forma permanente todos los recorridos seleccionados y sus datos asociados.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteRoutes(selectedRouteIds)
                        viewModel.clearSelection()
                        showBulkDeleteConfirm = false
                    }
                ) { Text("Eliminar todos") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Recorrido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona el formato del archivo que deseas integrar al historial:")
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showImportDialog = false
                        gpxPickerLauncher.launch(arrayOf("*/*"))
                    }) {
                        Text("Archivo GPX")
                    }
                    TextButton(onClick = {
                        showImportDialog = false
                        zipPickerLauncher.launch(arrayOf("application/zip"))
                    }) {
                        Text("Paquete ZIP")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RouteCard(
    route: Route,
    stripeColor: Color = Color.Transparent,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val durationMillis = if (route.endTime > route.startTime) route.endTime - route.startTime else 0L
    val formattedDuration = formatDuration(durationMillis)
    val formattedDate = formatDate(route.startTime)
    val km = route.distanceMeters / 1000.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                route.isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxHeight()) {
            if (stripeColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(stripeColor)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (isSelectionMode) {
                            Checkbox(checked = isSelected, onCheckedChange = { onClick() }, modifier = Modifier.padding(end = 8.dp))
                        }
                        Column {
                            Text(text = route.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(text = formattedDate, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                val isStop = route.name.startsWith("Parada") || route.activityMode == "STOPPED" || (route.distanceMeters < 25.0 && !route.isActive)
                if (isStop) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem(icon = Icons.Default.Place, value = "Punto Fijo", label = "Parada", tint = Color(0xFFFF9800))
                        StatItem(icon = Icons.Default.AccessTime, value = formattedDuration, label = "Tiempo de parada", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem(icon = Icons.AutoMirrored.Filled.TrendingUp, value = String.format(Locale.ROOT, "%.2f km", km), label = "Distancia", tint = MaterialTheme.colorScheme.primary)
                        StatItem(icon = Icons.Default.Schedule, value = formattedDuration, label = "Duracion", tint = MaterialTheme.colorScheme.secondary)
                    }
                    if (route.maxSpeed > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem(icon = Icons.Default.Speed, value = String.format(Locale.ROOT, "%.1f km/h", route.avgSpeed * 3.6f), label = "Promedio", tint = Color(0xFF4CAF50))
                            StatItem(icon = Icons.Default.Bolt, value = String.format(Locale.ROOT, "%.1f km/h", route.maxSpeed * 3.6f), label = "Maxima", tint = Color(0xFFFF9800))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayGroupHeader(
    formattedDate: String,
    colorInt: Int,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = { }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(colorInt))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (isCollapsed) "Expandir" else "Colapsar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatDuration(millis: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(millis); val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60; val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp); val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("es", "ES"))
    return sdf.format(date)
}
