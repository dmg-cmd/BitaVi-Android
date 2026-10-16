package com.aistudio.localizador.ui.screens.map

import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.components.BrandWatermark

@Composable
fun BoxScope.MapFloatingControls(
    viewModel: RouteViewModel,
    isTracking: Boolean,
    mapOrientation: Float,
    onFixNorth: () -> Unit,
    onShowLayers: () -> Unit,
    onRecenter: () -> Unit,
    onOpenStartDialog: () -> Unit,
    onOpenWaypointDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 32.dp, start = 16.dp)
            .zIndex(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val isMapRotated = Math.abs(mapOrientation) > 0.1f
        if (isMapRotated) {
            FloatingActionButton(
                onClick = onFixNorth,
                containerColor = Color(0xFF3498DB),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.CompassCalibration, contentDescription = "Fijar Norte")
            }
        }

        FloatingActionButton(
            onClick = onShowLayers,
            containerColor = Color(0xFF3498DB),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(Icons.Default.Layers, contentDescription = "Capas de Mapa")
        }

        FloatingActionButton(
            onClick = onRecenter,
            containerColor = Color(0xFF3498DB),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) { Icon(Icons.Default.MyLocation, contentDescription = "Volver a posición") }
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 32.dp, end = 16.dp)
            .zIndex(1f)
    ) {
        if (!isTracking) {
            ExtendedFloatingActionButton(
                onClick = onOpenStartDialog,
                containerColor = Color(0xFF2ECC71),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grabar", fontWeight = FontWeight.Bold)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FloatingActionButton(onClick = onOpenWaypointDialog) { Icon(Icons.Default.AddLocationAlt, null) }
                FloatingActionButton(
                    onClick = { viewModel.addQuickFavoriteFromTracking() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Guardar punto en ruta",
                        tint = Color(0xFFFFD54F)
                    )
                }
                FloatingActionButton(onClick = { viewModel.stopTrackingRoute() }, containerColor = MaterialTheme.colorScheme.error) { Icon(Icons.Default.Stop, null) }
            }
        }
    }

    BrandWatermark(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 168.dp, start = 8.dp)
    )
}
