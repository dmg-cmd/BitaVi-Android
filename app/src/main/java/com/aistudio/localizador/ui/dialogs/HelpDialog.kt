package com.aistudio.localizador.ui.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.localizador.ui.theme.IconCompartir
import com.aistudio.localizador.ui.theme.IconExportar
import com.aistudio.localizador.ui.theme.IconFavorito
import com.aistudio.localizador.ui.theme.IconGps

private enum class HelpCategory(val title: String) {
    SHORTCUTS("⚡ Teclas Rápidas"),
    MAP("🗺️ Mapa y GPS"),
    FAVORITES_ROUTES("⭐ Favoritos y Rutas"),
    AI_BACKUP("🤖 IA y Respaldo"),
    ALL("📋 Todos")
}

private data class HelpItemData(
    val icon: ImageVector,
    val title: String,
    val shortcutBadge: String?,
    val description: String,
    val category: HelpCategory,
    val tintColor: Color
)

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(HelpCategory.SHORTCUTS) }

    val allHelpItems = remember {
        listOf(
            // --- ATAJOS Y TECLAS RÁPIDAS ---
            HelpItemData(
                icon = Icons.Default.UnfoldLess,
                title = "Contraer Todo",
                shortcutBadge = "1 TOQUE",
                description = "En Favoritos y Rutas, pliega simultáneamente todos los países, ciudades y fechas para una vista rápida y compacta.",
                category = HelpCategory.SHORTCUTS,
                tintColor = Color(0xFF64B5F6)
            ),
            HelpItemData(
                icon = Icons.Default.UnfoldMore,
                title = "Expandir Todo",
                shortcutBadge = "1 TOQUE",
                description = "Despliega todas las secciones y tarjetas completas sin necesidad de abrirlas una por una.",
                category = HelpCategory.SHORTCUTS,
                tintColor = Color(0xFF81C784)
            ),
            HelpItemData(
                icon = Icons.Default.TouchApp,
                title = "Marcador Rápido en Mapa",
                shortcutBadge = "PULSACIÓN LARGA",
                description = "Mantén presionado cualquier punto del mapa para colocar un punto de paso (Waypoint) o guardar ese sitio como favorito.",
                category = HelpCategory.SHORTCUTS,
                tintColor = IconExportar
            ),
            HelpItemData(
                icon = Icons.Default.Checklist,
                title = "Selección Múltiple",
                shortcutBadge = "PULSACIÓN LARGA",
                description = "Mantén presionada cualquier tarjeta en 'Mis Recorridos' para activar la selección por lote y exportar o eliminar varias rutas a la vez.",
                category = HelpCategory.SHORTCUTS,
                tintColor = IconCompartir
            ),
            HelpItemData(
                icon = Icons.Default.Image,
                title = "Visor Táctico de Fotos",
                shortcutBadge = "1 TOQUE",
                description = "Toca la miniatura fotográfica de un favorito para examinar la imagen en alta definición a pantalla completa.",
                category = HelpCategory.SHORTCUTS,
                tintColor = IconFavorito
            ),
            HelpItemData(
                icon = Icons.Default.Navigation,
                title = "Telemetría Rápida",
                shortcutBadge = "CABECERA",
                description = "Toca la barra superior táctica para desplegar métricas en tiempo real de velocidad media, altitud y modo de tráfico.",
                category = HelpCategory.SHORTCUTS,
                tintColor = IconGps
            ),

            // --- MAPA Y GPS ---
            HelpItemData(
                icon = Icons.Default.Layers,
                title = "Selector de Capas de Mapa",
                shortcutBadge = "BOTÓN FLOTANTE",
                description = "Alterna al vuelo entre la cartografía estándar (OSM), fotografía satelital de alta resolución (ArcGIS) y topográfico.",
                category = HelpCategory.MAP,
                tintColor = Color(0xFF4FC3F7)
            ),
            HelpItemData(
                icon = Icons.Default.MyLocation,
                title = "Centrado GPS Inmediato",
                shortcutBadge = "BOTÓN FLOTANTE",
                description = "Reencuadra la cámara automáticamente sobre tu posición actual en vivo.",
                category = HelpCategory.MAP,
                tintColor = IconGps
            ),
            HelpItemData(
                icon = Icons.Default.FlashOn,
                title = "Modo Automático",
                shortcutBadge = "INTELIGENTE",
                description = "Inicia la grabación al detectar desplazamiento (>25 m) y pausa la ruta tras 15 minutos en reposo sin agotar batería.",
                category = HelpCategory.MAP,
                tintColor = Color(0xFFFFD54F)
            ),
            HelpItemData(
                icon = Icons.Default.AirplanemodeActive,
                title = "Modo Vuelo Continuo",
                shortcutBadge = "AERODINÁMICO",
                description = "Al superar 150 km/h y 30 m de ascenso, el sistema graba sin cortes de señal hasta 1200 km/h evitando líneas discontinuas.",
                category = HelpCategory.MAP,
                tintColor = Color(0xFFBA68C8)
            ),
            HelpItemData(
                icon = Icons.Default.CloudOff,
                title = "Mapas Fuera de Línea",
                shortcutBadge = "OFFLINE",
                description = "Opera con los mapas almacenados en memoria caché para navegar sin cobertura celular y sin consumo de datos.",
                category = HelpCategory.MAP,
                tintColor = Color(0xFF90A4AE)
            ),

            // --- FAVORITOS Y RUTAS ---
            HelpItemData(
                icon = Icons.Default.PersonAdd,
                title = "Importar desde Contactos",
                shortcutBadge = "FAVORITOS",
                description = "Crea un favorito seleccionando un contacto de tu agenda, extrayendo nombre y coordenadas automáticamente.",
                category = HelpCategory.FAVORITES_ROUTES,
                tintColor = IconCompartir
            ),
            HelpItemData(
                icon = Icons.Default.AddLocation,
                title = "Guardar Punto Actual",
                shortcutBadge = "FAVORITOS",
                description = "Registra tus coordenadas actuales con fotos, notas de campo, categoría y color personalizado.",
                category = HelpCategory.FAVORITES_ROUTES,
                tintColor = IconFavorito
            ),
            HelpItemData(
                icon = Icons.Default.Explore,
                title = "Mapa de Experiencia 3D",
                shortcutBadge = "DETALLE DE RUTA",
                description = "Reproduce el recorrido en perspectiva inmersiva con sincronización de velocidad, altitud y visor de fotos en ruta.",
                category = HelpCategory.FAVORITES_ROUTES,
                tintColor = Color(0xFF7C4DFF)
            ),
            HelpItemData(
                icon = Icons.Default.UploadFile,
                title = "Importar GPX o ZIP",
                shortcutBadge = "HISTORIAL",
                description = "Integra recorridos de otros navegadores GPX o paquetes de respaldo ZIP completos directamente a tu bitácora.",
                category = HelpCategory.FAVORITES_ROUTES,
                tintColor = IconExportar
            ),

            // --- IA Y RESPALDO ---
            HelpItemData(
                icon = Icons.Default.AutoAwesome,
                title = "Analista de Rutas IA (Groq)",
                shortcutBadge = "LLAMA 3.3",
                description = "Procesa telemetría con modelos de lenguaje para obtener diagnósticos de esfuerzo físico, velocidad y perfil de elevación.",
                category = HelpCategory.AI_BACKUP,
                tintColor = Color(0xFFFF8A65)
            ),
            HelpItemData(
                icon = Icons.Default.CloudUpload,
                title = "Respaldo en Drive o Local",
                shortcutBadge = "SEGURIDAD",
                description = "Empaqueta todas tus rutas, favoritos y fotos en un único archivo ZIP seguro y cifrado.",
                category = HelpCategory.AI_BACKUP,
                tintColor = Color(0xFF4DB6AC)
            ),
            HelpItemData(
                icon = Icons.Default.SettingsBackupRestore,
                title = "Restaurar Copia ZIP",
                shortcutBadge = "RECUPERACIÓN",
                description = "Restaura instantáneamente tu base de datos y álbum de fotografías desde cualquier respaldo generado.",
                category = HelpCategory.AI_BACKUP,
                tintColor = Color(0xFF64B5F6)
            )
        )
    }

    val filteredItems = remember(selectedCategory, allHelpItems) {
        if (selectedCategory == HelpCategory.ALL) allHelpItems
        else allHelpItems.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Guía y Teclas Rápidas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Atajos, gestos tácticos y funcionamiento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Selector horizontal de categorías
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HelpCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Lista desplazable con los elementos de ayuda
                AnimatedContent(
                    targetState = filteredItems,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "HelpItemsAnimation"
                ) { items ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEach { item ->
                            HelpItemCard(item = item)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@Composable
private fun HelpItemCard(item: HelpItemData) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(item.tintColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.shortcutBadge != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(item.tintColor.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.shortcutBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = item.tintColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
