package com.aistudio.localizador.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.localizador.R

@Composable
fun BitaruBrandHeader(
    modifier: Modifier = Modifier,
    maxWidthFraction: Float = 0.70f,
    showSubtitle: Boolean = true,
    subtitleText: String = "SISTEMA PROTEGIDO POR BIOMETRÍA"
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.bitaru_logo),
            contentDescription = "Logo Corporativo Bitaru",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(maxWidthFraction)
                .aspectRatio(3.5f)
        )

        if (showSubtitle) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
            )
        }
    }
}
