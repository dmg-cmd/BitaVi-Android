package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun BrandWatermark(
    modifier: Modifier = Modifier,
    version: String = "v3.2.2-RC1"
) {
    Text(
        text = "Agenviaje — $version",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .alpha(0.6f)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}