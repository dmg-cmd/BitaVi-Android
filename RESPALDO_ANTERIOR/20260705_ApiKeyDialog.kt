package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
    isKeyUnlocked: Boolean,
    keyVisible: Boolean,
    onApiKeyChange: (String) -> Unit,
    onUnlockClick: () -> Unit,
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
                    "La clave está protegida. Pulse el candado para desbloquear y editar.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { if (isKeyUnlocked) onApiKeyChange(it) },
                    label = { Text("Groq API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isKeyUnlocked,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (isKeyUnlocked) {
                                onToggleVisibility()
                            } else {
                                onUnlockClick()
                            }
                        }) {
                            Icon(
                                imageVector = if (!isKeyUnlocked) Icons.Default.Lock
                                else if (keyVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                if (!isKeyUnlocked) {
                    Text(
                        "🔒 Desbloquee con Biometría para ver o editar",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isKeyUnlocked,
                onClick = onSaveClick
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
