package com.aistudio.localizador.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationDialog(
    lat: String,
    lon: String,
    onLatChange: (String) -> Unit,
    onLonChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubicación de Inicio") },
        text = {
            Column {
                Text(
                    "Coordenadas para centrar el mapa al abrir la app:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = lat,
                    onValueChange = onLatChange,
                    label = { Text("Latitud") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lon,
                    onValueChange = onLonChange,
                    label = { Text("Longitud") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onUseCurrentLocation) {
                    Text("Usar ubicación actual")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
