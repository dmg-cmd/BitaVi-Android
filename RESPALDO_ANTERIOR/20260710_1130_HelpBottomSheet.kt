package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🛰️ Contextos de Asistencia Táctica
 */
enum class HelpContext {
    MAPA_RASTREO_ACTIVO,
    VISUALIZACION_EXPERIENCIAS
}

/**
 * 🛰️ HelpBottomSheet v3.2.2-RC1-patch9.2
 * Hoja inferior táctica de asistencia para exteriores.
 * 🛡️ Cumple con las Reglas de Oro: Contraste Accesible (WCAG > 4.5:1), Cero Advertencias y MD3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpBottomSheet(
    context: HelpContext,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1C1B1F), // Fondo búnker premium MD3 contra reflejo solar
        contentColor = Color.White,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (context) {
                HelpContext.MAPA_RASTREO_ACTIVO -> {
                    Text(
                        text = "🗺️ Panel de Rastreo Activo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "El mapa de rastreo muestra tu ubicación y el progreso de la ruta en tiempo real.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    HelpItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Iniciar bitácora",
                        description = "Comienza la grabación activa del recorrido GNSS y la telemetría cinemática."
                    )
                    HelpItem(
                        icon = Icons.Default.Pause,
                        title = "Pausar trayecto",
                        description = "Suspende temporalmente el odómetro y detiene el reloj sin cerrar la ruta."
                    )
                    HelpItem(
                        icon = Icons.Default.Stop,
                        title = "Finalizar y guardar",
                        description = "Finaliza el recorrido y vuelca los puntos filtrados en la base de datos cifrada SQLite."
                    )
                    HelpItem(
                        icon = Icons.Default.AddLocation,
                        title = "Destacar ubicación",
                        description = "Añade un punto de interés personalizado con foto, comentarios y geocodificación inversa."
                    )
                }
                HelpContext.VISUALIZACION_EXPERIENCIAS -> {
                    Text(
                        text = "🛰️ Reproducción de Experiencias",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Visualiza y administra tus ubicaciones favoritas, marcas y experiencias de campo guardadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    HelpItem(
                        icon = Icons.Default.PersonAdd,
                        title = "Importar contacto",
                        description = "Importa la dirección de un contacto de tu agenda y guárdalo como punto de interés."
                    )
                    HelpItem(
                        icon = Icons.Default.Share,
                        title = "Exportar datos",
                        description = "Permite enviar y respaldar las ubicaciones guardadas en formato de contacto táctico."
                    )
                    HelpItem(
                        icon = Icons.Default.Edit,
                        title = "Editar metadatos",
                        description = "Modifica el título, el color táctico del marcador, o el comentario descriptivo del punto."
                    )
                    HelpItem(
                        icon = Icons.Default.Delete,
                        title = "Remover marcador",
                        description = "Elimina de forma irreversible el punto de interés de tu base de datos local."
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
