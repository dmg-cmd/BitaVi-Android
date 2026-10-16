package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aistudio.localizador.data.model.Route
import java.io.File
import java.util.Locale
import javax.inject.Inject

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

        photoPaths.filter { it.isNotBlank() }.forEach { path ->
            when {
                path.startsWith("content://") || path.startsWith("file://") -> {
                    runCatching { Uri.parse(path) }.getOrNull()?.let { contentUris.add(it) }
                }
                else -> {
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        runCatching {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        }.getOrNull()?.let { contentUris.add(it) }
                    }
                }
            }
        }

        val routeName = route?.name?.takeIf { it.isNotBlank() } ?: "Recorrido Bitaru"
        val distanceMeters = route?.distanceMeters ?: 0.0
        val distanceKmFormatted = String.format(Locale.getDefault(), "%.2f", distanceMeters / 1000.0)

        val routeInfo = StringBuilder().apply {
            append("📍 Experiencia Bitaru Localizador\n")
            append("📌 Ruta: $routeName\n")
            append("📏 Distancia: $distanceKmFormatted km\n")
            if (contentUris.isNotEmpty()) {
                append("📸 Fotografías adjuntas: ${contentUris.size}\n")
            }
        }.toString()

        when {
            contentUris.isEmpty() -> {
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, routeInfo)
            }
            contentUris.size == 1 -> {
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUris[0])
                shareIntent.putExtra(Intent.EXTRA_TEXT, routeInfo)
            }
            else -> {
                shareIntent.action = Intent.ACTION_SEND_MULTIPLE
                shareIntent.type = "image/*"
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, contentUris)
                shareIntent.putExtra(Intent.EXTRA_TEXT, routeInfo)
            }
        }

        return Intent.createChooser(shareIntent, "Compartir Experiencia Bitaru")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}