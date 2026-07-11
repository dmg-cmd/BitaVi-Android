package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.location.ActivityMode
import com.example.ui.screens.IndicadorTranslucidoContenedor

@Composable
fun DashboardActividadFlotante(
    activityMode: ActivityMode,
    velocidadActual: String,
    modifier: Modifier = Modifier
) {
    val isSignalLost = activityMode == ActivityMode.SIN_SENAL

    val targetBackgroundColor by animateColorAsState(
        targetValue = if (isSignalLost) Color(0xFFDC3545).copy(alpha = 0.75f) else Color(0xFF1C1B1F).copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 400),
        label = "ColorTransicionAlerta"
    )

    IndicadorTranslucidoContenedor(
        modifier = modifier,
        overrideColor = targetBackgroundColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Column {
                Text(
                    text = if (isSignalLost) "ESTATUS SATELLITE" else "MODO DE TRÁFICO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSignalLost) Color.White else Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = when (activityMode) {
                        ActivityMode.STOPPED -> "🛑 Detenido"
                        ActivityMode.WALKING -> "🚶 Caminata"
                        ActivityMode.BIKING -> "🚴 Bicicleta"
                        ActivityMode.DRIVING -> "🚗 Vehículo"
                        ActivityMode.SIN_SENAL -> "⚠️ SIN SEÑAL"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            if (!isSignalLost) {
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "VELOCIDAD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = velocidadActual,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}