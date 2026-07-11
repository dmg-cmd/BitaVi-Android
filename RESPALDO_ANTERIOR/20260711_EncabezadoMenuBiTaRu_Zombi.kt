package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🛰️ EncabezadoMenuBiTaRu v3.2.2-RC1-patch9.0
 * Corrige la identidad visual obsoleta y blinda el contraste tipográfico diurno.
 * 🛡️ Cumple con las Reglas de Oro: UX Profesional, Cero Advertencias y MD3.
 */
@Composable
fun EncabezadoMenuBiTaRu(
    versionSuite: String = "v3.2.2-RC1",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // 🔥 IDENTIDAD CORREGIDA: Acrónimo de Alta Visibilidad
        Text(
            text = "BiTaRu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White, // 🛡️ Corrección de contraste: Blanco absoluto anti-reflejo
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        
        // Subtítulo descriptivo de la Suite Táctica
        Text(
            text = "Bitácora Táctica de Rutas",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f), // Blanco de alta opacidad
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Indicador de compilación e infraestructura
        Text(
            text = "ESTADO: BASELINE / TEMPORIZADOR SATELLITE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f) // Texto secundario controlado
        )
        
        Text(
            text = versionSuite,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Línea divisoria de Material Design 3 con opacidad integrada
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.15f),
            thickness = 1.dp
        )
    }
}
