package com.example.ui
import com.example.data.model.SavedLocation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

class ShareUseCase(
    private val context: Context
) {

    fun shareLocation(location: SavedLocation) {
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        val shareText = buildString {
            appendLine("📍 ¡Te comparto este lugar desde mi bitácora!")
            appendLine("📌 Nombre: ${location.name}")
            if (location.comment.isNotBlank()) appendLine("📝 Nota: ${location.comment}")
            appendLine("🗺️ Ver en Google Maps: $mapsUrl")
        }

        Intent(Intent.ACTION_SEND).apply {
            val photoUri = location.photoPath?.takeIf { it.isNotBlank() }?.toUri()
            if (photoUri != null) {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, shareText)
        }.also { intent ->
            val chooser = Intent.createChooser(intent, "Compartir ubicación vía")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

}
