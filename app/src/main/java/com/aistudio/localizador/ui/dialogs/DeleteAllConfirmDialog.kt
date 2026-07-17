package com.aistudio.localizador.ui.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DeleteAllConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
        title = { Text("¿Eliminar absolutamente todo?") },
        text = {
            Text("Esta acción borrará todas las rutas y favoritos del teléfono. Asegúrese de haber hecho un respaldo antes.")
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) { Text("Eliminar definitivamente") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
