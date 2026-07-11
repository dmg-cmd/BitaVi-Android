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
import androidx.compose.material.icons.automirrored.filled.AirplaneTicket
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Thunderstorm
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
 * 🛰️ HelpContext v3.2.2-RC1-patch14.0
 * Ámbito formal ampliado para albergar la ayuda general del Menú Lateral.
 */
enum class HelpContext {
    MENU_LATERAL_GENERAL, // 🔥 NUEVO: Contexto general de Guía de Iconos y Funciones
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
            HelpContext.MENU_LATERAL_GENERAL -> HelpSheetData(
                title = "❓ Guía de Iconos y Funciones Globales",
                subtitle = "Manual del Menú Lateral y Pantalla de Detalle",
                items = listOf(
                    // --- SUBSECCIÓN: BARRA SUPERIOR Y AJUSTES ---
                    ItemAyudaTactico(Icons.Default.Menu, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "☰ Menú Lateral", "Acceso centralizado a todos los ajustes, configuraciones y herramientas avanzadas de la app."),
                    ItemAyudaTactico(Icons.Default.Key, Color(0xFFF1C40F), Color(0xFFF1C40F).copy(alpha = 0.12f), "🔑 IA Groq (Llama 3)", "Analiza tus rutas automáticamente mediante IA local. Configura tu API Key desde la consola segura en https://console.groq.com/"),
                    ItemAyudaTactico(Icons.Default.Home, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "🏠 Ubicación de Inicio", "Define y ancla de forma persistente las coordenadas geográficas donde se centrará el mapa al iniciar la suite."),
                    ItemAyudaTactico(Icons.AutoMirrored.Filled.AirplaneTicket, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "✈️ Modo Avión Táctico", "Filtro dinámico de alta velocidad. Permite grabar y trackear telemetría de forma segura en vuelos comerciales (hasta 1200 km/h)."),
                    ItemAyudaTactico(Icons.Default.Thunderstorm, Color(0xFF2ECC71), Color(0xFF2ECC71).copy(alpha = 0.12f), "⚡ Modo Automático (Autostop)", "Graba con inteligencia de movimiento: inicia el guardado si detecta desplazamientos mayores a 25 metros y se pausa tras 15 minutos en reposo."),
                    ItemAyudaTactico(Icons.Default.OfflinePin, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "📦 Mapas Offline", "Fuerza el consumo estricto de la caché cartográfica descargada en la memoria para anular el gasto de datos móviles."),
                    ItemAyudaTactico(Icons.Default.CloudUpload, Color(0xFF9B5DE5), Color(0xFF9B5DE5).copy(alpha = 0.12f), "☁️ Respaldo en Drive", "Genera un empaquetado ZIP cifrado con todo el historial local y lo resguarda en tu nube. Tip: Puedes sobrescribir el archivo previo para optimizar el almacenamiento."),
                    
                    // --- SUBSECCIÓN: PANTALLA DE DETALLE ---
                    ItemAyudaTactico(Icons.Default.Share, Color(0xFF9B5DE5), Color(0xFF9B5DE5).copy(alpha = 0.12f), "🔀 Exportar GPX", "Empaqueta y guarda el trayecto en formato estándar de geolocalización para reutilizarlo en otros navegadores."),
                    ItemAyudaTactico(Icons.Default.TableChart, Color(0xFF9B5DE5), Color(0xFF9B5DE5).copy(alpha = 0.12f), "📊 Exportar Excel (CSV)", "Baja un reporte técnico estructurado en columnas con todas las métricas de velocidad, tiempo y coordenadas de la ruta."),
                    ItemAyudaTactico(Icons.Default.Edit, Color(0xFFF1C40F), Color(0xFFF1C40F).copy(alpha = 0.12f), "✏️ Editar Registro", "Permite renombrar o modificar los metadatos descriptivos de un recorrido histórico que ya fue guardado en la base de datos.")
                )
            )
            HelpContext.MAPA_RASTREO_ACTIVO -> HelpSheetData(
                title = "🗺️ Controles de Pantalla: Mapa Activo",
                subtitle = "Manual Exclusivo de la Botonera Física de la Vista",
                items = listOf(
                    ItemAyudaTactico(Icons.Default.Menu, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "☰ Menú Lateral (Drawer)", "Ubicado arriba a la izquierda en la barra base. Abre las configuraciones del búnker, llaves de API Groq y utilidades de la suite."),
                    ItemAyudaTactico(Icons.Default.CompassCalibration, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "🧭 Botón de Orientación / Brújula", "Control circular flotante izquierdo. Tocar para congelar la lona de OsmDroid apuntando al Norte absoluto."),
                    ItemAyudaTactico(Icons.Default.MyLocation, Color(0xFF3498DB), Color(0xFF3498DB).copy(alpha = 0.12f), "🎯 Botón Volver a Posición", "Control circular flotante izquierdo. Centra y fija de inmediato la cámara sobre el pin GPS del operador."),
                    ItemAyudaTactico(Icons.Default.PlayArrow, Color(0xFF2ECC71), Color(0xFF2ECC71).copy(alpha = 0.12f), "► Botón Grabar / Pausar Recorrido", "Óvalo extendido Verde Esmeralda (esquina inferior derecha). Inicia la traza o congela la velocidad residual a 0.0 km/h.")
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

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sheetData.items, key = { it.label }) { item ->
                    // Separador estético sutil si detectamos el cambio de contexto interno en la lista
                    if (item.label == "🔀 Exportar GPX") {
                        Text(
                            text = "Pantalla de Detalle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

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
