package com.aistudio.localizador.ui.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Contenedor genérico translúcido de alta visibilidad diurna.
 * 🛡️ Respeta la regla de evitar elevación tonal para bloquear grises indeseados.
 */
@Composable
fun IndicadorTranslucidoContenedor(
    modifier: Modifier = Modifier,
    overrideColor: Color = Color(0xFF1C1B1F).copy(alpha = 0.65f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = overrideColor,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
