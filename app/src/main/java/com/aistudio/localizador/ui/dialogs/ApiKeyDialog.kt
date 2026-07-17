package com.aistudio.localizador.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyDialog(
    apiKey: String,
    keyVisible: Boolean,
    onApiKeyChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSaveClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración de IA (Groq)") },
        text = {
            Column {
                Text(
                    "Ingrese o pegue su API Key de Groq. Se cifrará al guardar.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("Groq API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (keyVisible) "Ocultar clave" else "Mostrar clave"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onSaveClick) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}