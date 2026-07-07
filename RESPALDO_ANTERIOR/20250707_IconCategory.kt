package com.example.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconCategory(val key: String, val icon: ImageVector, val label: String) {
    DEFAULT("DEFAULT", Icons.Default.Star, "Favorito"),
    GASTRONOMIA("GASTRONOMIA", Icons.Default.Restaurant, "Gastronomia"),
    TRANSPORTE("TRANSPORTE", Icons.Default.DirectionsCar, "Vehiculo"),
    AVION("AVION", Icons.Default.Flight, "Aeropuerto"),
    PARQUE("PARQUE", Icons.Default.NaturePeople, "Descanso"),
    TRABAJO("TRABAJO", Icons.Default.Business, "Trabajo"),
    AUTOMATIC("AUTOMATIC", Icons.Default.LocationOn, "Parada Automatica");

    companion object {
        fun fromKey(key: String?): IconCategory {
            return values().firstOrNull { it.key == key } ?: DEFAULT
        }

        fun toBitmap(iconCategory: IconCategory, color: Int, sizePx: Int): android.graphics.Bitmap {
            val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = android.graphics.Paint.Style.FILL
            }
            val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, paint)
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, borderPaint)
            return bitmap
        }
    }
}


