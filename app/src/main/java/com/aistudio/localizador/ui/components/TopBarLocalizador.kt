package com.aistudio.localizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🛰️ TopBarLocalizador v3.2.3-RC1-patch15.7
 * Cabecera translúcida optimizada. Deja espacio libre para el menú flotante izquierdo
 * e implementa escala de grises de alto contraste diurno.
 */
@Composable
fun TopBarLocalizador(
    onLocalizadorClick: () -> Unit,
    modoTrafico: String,
    velocidad: String,
    modifier: Modifier = Modifier,
    colorIndicador: Color = Color(0xFFD32F2F),
    headerAlpha: Float = 0.45f,
    onAdjustAlphaClick: (() -> Unit)? = null,
    altura: String = "-- m",
    distanciaPrimerPunto: String = "--",
    tiempoFuncionamiento: String = "--"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1B1F).copy(alpha = headerAlpha))
            .border(1.dp, Color.White.copy(alpha = (headerAlpha * 0.35f).coerceIn(0.08f, 0.25f)), RoundedCornerShape(16.dp))
            .clickable { onLocalizadorClick() }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ─── 1. FILA SUPERIOR: TÍTULO, VERSIÓN Y BOTONES AL EXTREMO DERECHO ARRIBA ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bitácora Táctica de Rutas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        // 📍 CHIP INDICADOR
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF9B5DE5).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF9B5DE5).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "📍 UBICACIÓN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE3E2E6)
                            )
                        }
                    }

                    Text(
                        text = "Infraestructura Suite — v3.2.17-RC1",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                // 🔥 BOTONES EN EL EXTREMO DERECHO SUPERIOR
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (onAdjustAlphaClick != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onAdjustAlphaClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = "Ajustar atenuador de fondo",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF9B5DE5).copy(alpha = 0.25f))
                            .clickable { onLocalizadorClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = "Consultar Ubicación Actual",
                            tint = Color(0xFF9B5DE5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── 2. FILA DE MÉTRICAS PRIMARIAS: MODO, VELOCIDAD Y ALTURA ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modo de Tráfico
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "MODO DE TRÁFICO",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colorIndicador, RoundedCornerShape(50))
                                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = modoTrafico,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // Velocidad
                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = "VELOCIDAD",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colorIndicador, RoundedCornerShape(50))
                                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = velocidad,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // Altura
                Column(modifier = Modifier.weight(0.8f)) {
                    Text(
                        text = "ALTURA",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = altura,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── 3. FILA DE MÉTRICAS COMPLEMENTARIAS: DISTANCIA 1º PTO Y HORAS DE FUNCIONAMIENTO ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distancia desde el primer punto
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFF4DD0E1),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "DISTANCIA 1º PTO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = distanciaPrimerPunto,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Horas de funcionamiento
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "TIEMPO ACTIVO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tiempoFuncionamiento,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de referencia de colores del trazado según velocidad aproximada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = (headerAlpha * 0.5f).coerceIn(0.15f, 0.40f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReferenciaVelocidadItem(color = Color(0xFFD32F2F), etiqueta = "<2 km/h")
                ReferenciaVelocidadItem(color = Color(0xFF2E7D32), etiqueta = "2-7 km/h")
                ReferenciaVelocidadItem(color = Color(0xFF1565C0), etiqueta = "7-20 km/h")
                ReferenciaVelocidadItem(color = Color(0xFF7B1FA2), etiqueta = ">20 km/h")
            }
        }
    }
}

@Composable
private fun ReferenciaVelocidadItem(
    color: Color,
    etiqueta: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(50))
                .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

