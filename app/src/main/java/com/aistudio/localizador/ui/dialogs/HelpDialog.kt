package com.aistudio.localizador.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guía de Iconos y Funciones") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Barra Superior",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HelpSection(Icons.Default.Menu, "Menú lateral", "Acceso a todos los ajustes y herramientas de la app.")
                HelpSection(Icons.Default.VpnKey, "IA Groq (Llama 3)", "Analiza tus rutas automáticamente. Obtén tu clave en: https://console.groq.com/")
                HelpSection(Icons.Default.Home, "Ubicación de Inicio", "Define dónde se centra el mapa al abrir la app.")
                HelpSection(Icons.Default.AirplanemodeActive, "Modo Avión", "Permite grabar en vuelos comerciales (hasta 1200 km/h).")
                HelpSection(Icons.Default.FlashOn, "Modo Automático", "Graba solo si detecta movimiento (>25m) y para tras 15 min quieto.")
                HelpSection(Icons.Default.CloudOff, "Mapas Offline", "Usa solo mapas en memoria para no gastar datos.")
                HelpSection(Icons.Default.CloudUpload, "Respaldo en Drive", "Crea un ZIP con todo y guárdalo en tu nube. Tip: Puedes sobrescribir el archivo anterior para no llenar el Drive.")

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    "Pantalla de Detalle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HelpSection(Icons.Default.Share, "Exportar GPX", "Guarda el trayecto para usarlo en otras apps de mapas.")
                HelpSection(Icons.Default.TableChart, "Exportar Excel", "Baja un archivo CSV con todos los datos técnicos.")
                HelpSection(Icons.Default.Edit, "Editar", "Cambia el nombre de un recorrido ya guardado.")
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Entendido") } }
    )
}

@Composable
private fun HelpSection(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
