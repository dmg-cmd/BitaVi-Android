package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Route
import com.example.ui.RouteViewModel
import com.example.ui.Screen
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Pantalla Historial con soporte para selección múltiple y exportación GPX.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: RouteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routes by viewModel.allRoutes.collectAsState()
    var routeToDelete by remember { mutableStateOf<Route?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    
    // Estado para selección múltiple
    var selectedRouteIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedRouteIds.isNotEmpty()

    // Forzamos el ordenamiento por fecha (más reciente primero)
    val sortedRoutes = remember(routes) {
        routes.sortedByDescending { it.startTime }
    }

    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.importGpxFile(context, it) }
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
                    contentDescription = "Historial vacío",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Aún no hay rutas guardadas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "¡Sal al aire libre y graba tu primera aventura!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.navigateTo(Screen.Tracking) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grabar")
                    }
                    OutlinedButton(onClick = { gpxPickerLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar GPX")
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSelectionMode) "${selectedRouteIds.size} seleccionadas" else "Mis Recorridos",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (isSelectionMode) {
                            Row {
                                IconButton(onClick = { 
                                    viewModel.exportRoutesAsGPX(context, selectedRouteIds)
                                    selectedRouteIds = emptySet()
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Exportar", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showBulkDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Borrar seleccionadas", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { selectedRouteIds = emptySet() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                                }
                            }
                        } else {
                            IconButton(onClick = { gpxPickerLauncher.launch(arrayOf("*/*")) }) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Importar GPX", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = sortedRoutes, key = { it.id }) { route ->
                        val isSelected = selectedRouteIds.contains(route.id)
                        RouteCard(
                            route = route,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = { 
                                if (isSelectionMode) {
                                    selectedRouteIds = if (isSelected) selectedRouteIds - route.id else selectedRouteIds + route.id
                                } else {
                                    viewModel.selectRouteForDetail(route.id)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedRouteIds = setOf(route.id)
                                }
                            },
                            onDelete = { routeToDelete = route },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    // Modal de Confirmación de Borrado
    routeToDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar recorrido?") },
            text = { Text("Esta acción eliminará de forma irreversible el recorrido '${route.name}'.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteRoute(route.id)
                        routeToDelete = null
                    }
                ) { Text("Eliminar definitivamente") }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar ${selectedRouteIds.size} recorridos?") },
            text = { Text("Se eliminarán de forma permanente todos los recorridos seleccionados y sus datos asociados.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteRoutes(selectedRouteIds)
                        selectedRouteIds = emptySet()
                        showBulkDeleteConfirm = false
                    }
                ) { Text("Eliminar todos") }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteCard(
    route: Route,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
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
                if (!isSelectionMode) {
                    IconButton(onClick = onDelete, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(icon = Icons.Default.TrendingUp, value = String.format("%.2f km", km), label = "Distancia", tint = MaterialTheme.colorScheme.primary)
                StatItem(icon = Icons.Default.Schedule, value = formattedDuration, label = "Duración", tint = MaterialTheme.colorScheme.secondary)
            }
            if (route.maxSpeed > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(icon = Icons.Default.Speed, value = String.format("%.1f km/h", route.avgSpeed * 3.6f), label = "Promedio", tint = Color(0xFF4CAF50))
                    StatItem(icon = Icons.Default.Bolt, value = String.format("%.1f km/h", route.maxSpeed * 3.6f), label = "Máxima", tint = Color(0xFFFF9800))
                }
            }
        }
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
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp); val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("es", "ES"))
    return sdf.format(date)
}
