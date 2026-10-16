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
 * 🛰️ TopBarLocalizador v3.2.2-RC1-patch15.7
 * Cabecera translúcida optimizada. Deja espacio libre para el menú flotante izquierdo
 * e implementa escala de grises de alto contraste diurno.
 */
@Composable
fun TopBarLocalizador(
    onLocalizadorClick: () -> Unit,
    modoTrafico: String,
    velocidad: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // 🔥 CUMPLIMIENTO UX: Cabecera simétrica sin espacio para botón lateral
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1B1F).copy(alpha = 0.50f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable { onLocalizadorClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
                    
                    // 📍 CHIP INDICADOR RECALIBRADO (Gris clarito de alta legibilidad)
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
                            color = Color(0xFFE3E2E6) // 🔥 Gris clarito superior al tono de la versión
                        )
                    }
                }
                
                Text(
                    text = "Infraestructura Suite — v3.2.2-RC1",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f), // 🔥 Contraste aumentado (Gris más nítido)
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text(
                            text = "MODO DE TRÁFICO", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.White.copy(alpha = 0.85f) // 🔥 Línea siguiente contrastada a fondo
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Red, RoundedCornerShape(50)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(modoTrafico, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Column {
                        Text(
                            text = "VELOCIDAD", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.White.copy(alpha = 0.85f) // 🔥 Línea siguiente contrastada a fondo
                        )
                        Text(velocidad, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF9B5DE5).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationSearching,
                    contentDescription = "Consultar Ubicación Actual",
                    tint = Color(0xFF9B5DE5)
                )
            }
        }
    }
}
