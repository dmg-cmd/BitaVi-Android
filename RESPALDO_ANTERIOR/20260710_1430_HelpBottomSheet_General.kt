package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🛰️ HelpContext v3.2.2-RC1-patch12.0
 * Ámbito formal de las 5 pantallas del ecosistema de la Suite BiTaRu.
 */
enum class HelpContext {
    MAPA_RASTREO_ACTIVO,
    VISUALIZACION_FAVORITOS,
    HISTORIAL_TRAZAS,
    ANALISIS_RECORRIDO,
    GRAFICOS_RENDIMIENTO
}

/**
 * 🛰️ ItemAyudaTactico v3.2.2-RC1-patch12.0
 * Encapsulador inmutable para la renderización cromática indexada anti-reflejo.
 */
data class ItemAyudaTactico(
    val icon: ImageVector,
    val iconColor: Color,
    val tintBackground: Color,
    val label: String,
    val descripcion: String
)

private data class HelpSheetData(
    val title: String,
    val subtitle: String,
    val items: List<ItemAyudaTactico>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpBottomSheet(
    context: HelpContext,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetData = remember(context) {
        when (context) {
            HelpContext.MAPA_RASTREO_ACTIVO -> HelpSheetData(
                title = "🗺️ Pantalla Principal / Rastreo Activo",
                subtitle = "Botonera Inmersiva de Operaciones en Mapa",
                items = listOf(
                    ItemAyudaTactico(Icons.Default.Menu, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "☰ Menú Lateral (Drawer)", "Abre el panel lateral de BiTaRu, utilidades del búnker y ajustes globales."),
                    ItemAyudaTactico(Icons.Default.Layers, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Capas de Mapa", "Conmuta de forma inmediata el renderizado cartográfico entre Modo Luz y Oscuro Táctico."),
                    ItemAyudaTactico(Icons.Default.Favorite, Color(0xFF2ECC71), Color(0xFF2ECC71).copy(alpha = 0.12f), "Marcar Favorito", "Ancla la coordenada geoespacial actual directamente en la agenda local cifrada con SQLCipher."),
                    ItemAyudaTactico(Icons.Default.History, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Historial Rápido", "Salto directo sin transiciones complejas hacia el listado general de trazas persistidas."),
                    ItemAyudaTactico(Icons.Default.CompassCalibration, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Orientación / Brújula", "Aparece al rotar el plano; pulsar para restablecer la orientación del mapa al Norte absoluto."),
                    ItemAyudaTactico(Icons.Default.MyLocation, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Volver a Posición 🎯", "Centra y ancla la cámara sobre las coordenadas GPS en tiempo real de tu Pixel 9 Pro XL."),
                    ItemAyudaTactico(Icons.Default.PlayArrow, Color(0xFF2ECC71), Color(0xFF2ECC71).copy(alpha = 0.12f), "Grabar / Pausar 🔴", "Controla el LocationService: Inicia el tracking o activa el Candado Atómico de velocidad a 0.0 km/h."),
                    ItemAyudaTactico(Icons.AutoMirrored.Filled.HelpOutline, Color(0xFF9B5DE5), Color(0xFF9B5DE5).copy(alpha = 0.12f), "Barra Superior Táctica", "Pulsar la barra de estado para calcular mediante Geocoder inverso la calle y altura exacta actual.")
                )
            )
            HelpContext.VISUALIZACION_FAVORITOS -> HelpSheetData(
                title = "💚 Gestión de Lugares Favoritos",
                subtitle = "Mapeo de Hitos Seguros Guardados en Almacenamiento",
                items = listOf(
                    ItemAyudaTactico(Icons.Default.Share, Color(0xFF9B5DE5), Color(0xFF9B5DE5).copy(alpha = 0.12f), "Exportar / Compartir", "Inicia ShareUseCase para enviar el hito empaquetado bajo firmas inmutables de BiTaRu."),
                    ItemAyudaTactico(Icons.Default.Layers, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Ver en Google Maps", "Despacha un Intent nativo hacia el sistema para ruteo externo mediante coordenadas geográficas."),
                    ItemAyudaTactico(Icons.Default.Favorite, Color(0xFF2ECC71), Color(0xFF2ECC71).copy(alpha = 0.12f), "Marcador Verde con Foto", "Indica la posición física del favorito en el mapa. Pulsar para inspeccionar la evidencia visual adjunta."),
                    ItemAyudaTactico(Icons.Default.Edit, Color(0xFFF1C40F), Color(0xFFF1C40F).copy(alpha = 0.12f), "Lápiz Modificar ✏️", "Habilita la edición atómica segura del alias de identificación y metadatos del lugar."),
                    ItemAyudaTactico(Icons.Default.Delete, Color(0xFFE74C3C), Color(0xFFE74C3C).copy(alpha = 0.12f), "Tacho Eliminar 🗑️", "Purga física e irreversible de la coordenada y sus registros vinculados dentro de SQLite."),
                    ItemAyudaTactico(Icons.Default.Layers, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Controles de Barra", "Filtros rápidos de ordenamiento alfabético y proximidad geográfica sobre la agenda.")
                )
            )
            HelpContext.HISTORIAL_TRAZAS -> HelpSheetData(
                title = "📜 Historial de Rutas Registradas",
                subtitle = "Administración de Bitácoras de Navegación del Dispositivo",
                items = listOf(
                    ItemAyudaTactico(Icons.Default.Delete, Color(0xFFE74C3C), Color(0xFFE74C3C).copy(alpha = 0.12f), "Tacho Eliminar Recorrido", "Remueve por completo la traza seleccionada y todos sus nodos de ubicación en cascada."),
                    ItemAyudaTactico(Icons.AutoMirrored.Filled.InsertDriveFile, Color(0xFF9B5DE5), Color(0xFF9B5DE5).copy(alpha = 0.12f), "Importar Archivo GPX/KML", "Abre el SAF de Android para inyectar tracks externos validando su integridad matemática."),
                    ItemAyudaTactico(Icons.Default.Layers, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Filtros de Barra Global", "Búsqueda avanzada por rango cronológico, kilometraje total y etiquetas de operación."),
                    ItemAyudaTactico(Icons.Default.PlayArrow, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Presionar sobre Recorrido", "Abre de forma instantánea el módulo analítico avanzado del tramo cinemático seleccionado.")
                )
            )
            HelpContext.ANALISIS_RECORRIDO -> HelpSheetData(
                title = "📈 Análisis Detallado de Recorrido",
                subtitle = "Módulo de Telemetría Post-Marcha y Diagnóstico",
                items = listOf(
                    ItemAyudaTactico(Icons.Default.AutoAwesome, Color(0xFFF1C40F), Color(0xFFF1C40F).copy(alpha = 0.12f), "Analizar con IA 🤖", "Procesa mediante un modelo local la curva de velocidad y altimetría buscando anomalías cinemáticas."),
                    ItemAyudaTactico(Icons.Default.Analytics, Color(0xFFF1C40F), Color(0xFFF1C40F).copy(alpha = 0.12f), "Gráficos de Rendimiento", "Conmuta la visualización hacia el plano cartesiano de curvas de velocidad/tiempo acelerado."),
                    ItemAyudaTactico(Icons.Default.Layers, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Barra Inferior Táctica", "Herramientas de reproducción cinemática a velocidad variable (1x, 2x, 4x) y exportación de segmentos.")
                )
            )
            HelpContext.GRAFICOS_RENDIMIENTO -> HelpSheetData(
                title = "📉 Gráficos de Rendimiento y Curvas",
                subtitle = "Inspección Geométrica de Datos Físicos Recabados",
                items = listOf(
                    ItemAyudaTactico(Icons.Default.Edit, Color(0xFFF1C40F), Color(0xFFF1C40F).copy(alpha = 0.12f), "Selector de Tramos", "Aísla porciones discretas de tiempo o distancia para auditar picos de aceleración y velocidades medias."),
                    ItemAyudaTactico(Icons.AutoMirrored.Filled.ArrowBack, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "Botón Volver ⬅️", "Cierra el contexto analítico gráfico regresar a la pantalla de inspección del recorrido.")
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1B1F),
        contentColor = Color.White,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(text = sheetData.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = sheetData.subtitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.45f), modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sheetData.items, key = { it.label }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).background(item.tintBackground, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = item.descripcion, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
