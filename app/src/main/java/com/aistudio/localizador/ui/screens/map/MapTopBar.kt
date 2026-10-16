package com.aistudio.localizador.ui.screens.map

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aistudio.localizador.location.ActivityMode
import com.aistudio.localizador.location.manager.KineticProfile
import com.aistudio.localizador.ui.components.TopBarLocalizador
import java.util.Locale

@Composable
fun BoxScope.MapTopBar(
    activityMode: ActivityMode,
    kineticProfile: KineticProfile,
    latestLocation: Location?,
    showAddressBox: Boolean,
    currentAddress: String?,
    onLocalizadorClick: () -> Unit,
    onAddressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .zIndex(1f)
    ) {
        val modoTraficoText = if (kineticProfile == KineticProfile.FLIGHT) {
            "✈️ Vuelo"
        } else {
            when (activityMode) {
                ActivityMode.STOPPED -> "🛑 Detenido"
                ActivityMode.WALKING -> "🚶 Caminata"
                ActivityMode.BIKING -> "🚴 Bicicleta"
                ActivityMode.DRIVING -> "🚗 Vehículo"
                ActivityMode.SIN_SENAL -> "⚠️ SIN SEÑAL"
            }
        }

        TopBarLocalizador(
            onLocalizadorClick = onLocalizadorClick,
            modoTrafico = modoTraficoText,
            velocidad = "${String.format(LocalConfiguration.current.locales[0], "%.1f", (latestLocation?.speed ?: 0f) * 3.6f)} km/h"
        )

        if (showAddressBox && currentAddress != null) {
            Spacer(modifier = Modifier.height(4.dp))
            IndicadorTranslucidoContenedor(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onAddressClick() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = currentAddress,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
