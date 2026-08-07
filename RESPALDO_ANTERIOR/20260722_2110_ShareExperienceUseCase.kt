package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aistudio.localizador.data.model.Route
import java.io.File
import javax.inject.Inject

/**
 * UseCase de Dominio para empaquetar y compartir experiencias perimetrales con fotos y telemetría (CR-19).
 */
class ShareExperienceUseCase @Inject constructor() {

    operator fun invoke(
        context: Context,
        route: Route?,
        photoPaths: List<String>
    ): Intent {
        val shareIntent = Intent().apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val contentUris = ArrayList<Uri>()

        // Mapeo exhaustivo de rutas locales a URIs de contenido seguro
        photoPaths.forEach { path ->
            if (path.isNotBlank()) {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    contentUris.add(uri)
                }
            }
        }

        // Construcción del resumen textual
        val routeInfo = StringBuilder().apply {
            append("📍 Experiencia Bitaru Localizador\n")
            route?.let {
                if (it.name.isNotBlank()) append("📌 Nombre: ${it.name}\n")
                val distanceKm = String.format("%.2f", it.distanceMeters / 1000.0)
                append("📏 Distancia: $distanceKm km\n")
            }
            if (contentUris.isNotEmpty()) {
                append("📸 Fotografías adjuntas: ${contentUris.size}")
            }
        }.toString()

        // Configuración del Intent según la disponibilidad de imágenes
        if (contentUris.isEmpty()) {
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, routeInfo)
        } else if (contentUris.size == 1) {
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUris[0])
            shareIntent.putExtra(Intent.EXTRA_TEXT, routeInfo)
        } else {
            shareIntent.action = Intent.ACTION_SEND_MULTIPLE
            shareIntent.type = "image/*"
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris)
            shareIntent.putExtra(Intent.EXTRA_TEXT, routeInfo)
        }

        return Intent.createChooser(shareIntent, "Compartir Experiencia")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
