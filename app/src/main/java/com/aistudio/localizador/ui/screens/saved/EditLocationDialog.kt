package com.aistudio.localizador.ui.screens.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.ui.RouteViewModel
import java.util.Locale

@Composable
fun EditLocationDialog(
    location: SavedLocation,
    viewModel: RouteViewModel,
    onDismiss: () -> Unit
) {
    val initialIsStop = location.iconCategory == "AUTOMATIC" ||
            location.author.equals("Automático", ignoreCase = true) ||
            location.author.contains("Parada", ignoreCase = true) ||
            location.comment.contains("Parada", ignoreCase = true) ||
            location.color.equals("#FF6D00", ignoreCase = true)

    var current by remember(location.id) { mutableStateOf(location) }
    var editName by remember(current.id) { mutableStateOf(current.name) }
    var editAddress by remember(current.id) { mutableStateOf(current.address) }
    var editComment by remember(current.id) { mutableStateOf(current.comment) }
    var editAuthor by remember(current.id) { mutableStateOf(current.author) }
    var editPhone by remember(current.id) { mutableStateOf(current.phoneNumber) }
    var editEmail by remember(current.id) { mutableStateOf(current.email) }
    var editIsContact by remember(current.id) { mutableStateOf(current.isContact) }
    var editIsStopPoint by remember(current.id) { mutableStateOf(initialIsStop) }
    var editColor by remember(current.id) {
        mutableStateOf(
            if (initialIsStop && current.color.equals("#FFD700", ignoreCase = true)) "#FF6D00" else current.color
        )
    }
    var isRefreshingLocation by remember { mutableStateOf(false) }

    val colors = listOf("#FFD700", "#FF6D00", "#4CAF50", "#2196F3", "#F44336", "#9C27B0")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editIsStopPoint) "Editar Punto de Parada" else "Editar Favorito") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Nombre del lugar o parada") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editAddress,
                    onValueChange = { editAddress = it },
                    label = { Text(if (editIsStopPoint) "Dirección donde se paró" else "Dirección") },
                    placeholder = { Text("Ej: Av. Mitre 1250, José Mármol") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = editIsStopPoint,
                        onCheckedChange = { checked ->
                            editIsStopPoint = checked
                            if (checked) {
                                editColor = "#FF6D00"
                                viewModel.updateSavedLocationColorReactively(current.id, "#FF6D00")
                            } else if (editColor.equals("#FF6D00", ignoreCase = true)) {
                                editColor = "#FFD700"
                                viewModel.updateSavedLocationColorReactively(current.id, "#FFD700")
                            }
                        }
                    )
                    Text("🛑 Es un Punto de Parada (Color Naranja)", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = editIsContact, onCheckedChange = { editIsContact = it })
                    Text("Es un Contacto", style = MaterialTheme.typography.bodyMedium)
                }

                if (editIsContact) {
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(value = editAuthor, onValueChange = { editAuthor = it }, label = { Text("¿Quién lo guarda?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editComment, onValueChange = { editComment = it }, label = { Text("Comentario / Nota") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        isRefreshingLocation = true
                        viewModel.refreshLocationDetails(current) { updated ->
                            current = updated
                            if (updated.address.isNotBlank()) {
                                editAddress = updated.address
                                if (editName.startsWith("GPS:", ignoreCase = true)) {
                                    editName = updated.address
                                }
                            }
                            isRefreshingLocation = false
                        }
                    },
                    enabled = !isRefreshingLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRefreshingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buscando dirección por GPS...")
                    } else {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Autocompletar Dirección por GPS")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Ubicación resuelta",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${current.country} • ${current.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Color del marcador (#FF6D00 = Parada):", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(colorHex.toColorInt()), RoundedCornerShape(16.dp))
                                .clickable {
                                    editColor = colorHex
                                    editIsStopPoint = colorHex.equals("#FF6D00", ignoreCase = true)
                                    viewModel.updateSavedLocationColorReactively(current.id, colorHex)
                                }
                                .padding(4.dp)
                        ) {
                            if (editColor.equals(colorHex, ignoreCase = true)) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                var hue by remember {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(editColor.toColorInt(), hsv)
                    mutableStateOf(hsv[0])
                }

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Personalizado:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        )
                    }
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            val newColor = String.format(Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                            editColor = newColor
                            viewModel.updateSavedLocationColorReactively(current.id, newColor)
                        },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.updateSavedLocation(
                    current.copy(
                        name = editName.ifBlank { editAddress.ifBlank { current.name } },
                        address = editAddress,
                        comment = editComment,
                        author = editAuthor,
                        phoneNumber = editPhone,
                        email = editEmail,
                        isContact = editIsContact,
                        color = editColor,
                        iconCategory = if (editIsStopPoint) "AUTOMATIC" else if (current.iconCategory == "AUTOMATIC") "DEFAULT" else current.iconCategory
                    )
                )
                onDismiss()
            }) { Text("Guardar Cambios") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

