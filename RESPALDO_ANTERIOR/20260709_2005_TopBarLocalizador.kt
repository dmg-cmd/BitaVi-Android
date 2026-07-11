package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🛰️ TopBarLocalizador v3.2.2-RC1-patch9.2
 * Encabezado flotante 100% transparente de alta jerarquía con soporte de ayuda contextual.
 * 🛡️ Cumple con las Reglas de Oro: Diseño Inmersivo Total, Contraste Adaptativo y Cero Advertencias.
 */
@Composable
fun TopBarLocalizador(
    modifier: Modifier = Modifier,
    isDarkMap: Boolean = false, // Permite invertir el color de la letra según el tipo de mapa activo
    onHelpClick: (() -> Unit)? = null
) {
    // ☀️ Selección cromática adaptiva para legibilidad extrema sin bloques de fondo
    val textColor = if (isDarkMap) Color.White else Color(0xFF1C1B1F) // Negro profundo MD3
    val subTextColor = if (isDarkMap) Color.White.copy(alpha = 0.7f) else Color(0xFF49454F) // Variante MD3

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding() // Respeta la barra de estado del sistema operativo
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Bitácora Táctica de Rutas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Text(
                text = "Infraestructura Suite — v3.2.2-RC1",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = subTextColor
            )
        }

        if (onHelpClick != null) {
            IconButton(onClick = onHelpClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Asistencia táctica",
                    tint = textColor
                )
            }
        }
    }
}
