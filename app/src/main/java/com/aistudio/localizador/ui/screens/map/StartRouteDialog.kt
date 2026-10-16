package com.aistudio.localizador.ui.screens.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StartRouteDialog(
    newRouteName: String,
    onNameChange: (String) -> Unit,
    updateIntervalSeconds: Float,
    onIntervalChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Ruta") },
        text = {
            Column {
                OutlinedTextField(value = newRouteName, onValueChange = onNameChange, label = { Text("Nombre") })
                Text("Precisión: ${updateIntervalSeconds.toInt()}s", modifier = Modifier.padding(top = 8.dp))
                Slider(value = updateIntervalSeconds, onValueChange = onIntervalChange, valueRange = 1f..60f)
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Iniciar") } }
    )
}
