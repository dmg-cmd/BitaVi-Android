package com.aistudio.localizador.ui.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.createBitmap

enum class IconCategory(val key: String, val icon: ImageVector, val label: String) {
    DEFAULT("DEFAULT", Icons.Default.Star, "Favorito"),
    GASTRONOMIA("GASTRONOMIA", Icons.Default.Restaurant, "Gastronomía"),
    TRANSPORTE("TRANSPORTE", Icons.Default.DirectionsCar, "Vehículo"),
    AVION("AVION", Icons.Default.Flight, "Aeropuerto"),
    PARQUE("PARQUE", Icons.Default.NaturePeople, "Descanso"),
    TRABAJO("TRABAJO", Icons.Default.Business, "Trabajo"),
    AUTOMATIC("AUTOMATIC", Icons.Default.LocationOn, "Parada Automática");

    companion object {
        fun fromKey(key: String?): IconCategory {
            return entries.firstOrNull { it.key == key } ?: DEFAULT
        }
    }

    fun toBitmap(size: Int, tintColor: Int): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, borderPaint)
        return bitmap
    }
}
