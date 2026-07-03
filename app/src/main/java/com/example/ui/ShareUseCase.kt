package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.SavedLocation
import com.example.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ShareUseCase(
    private val context: Context,
    private val repository: RouteRepository
) {

    fun shareLocation(location: SavedLocation) {
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        val shareText = buildString {
            append("📍 *${location.name}*\n\n")
            if (location.address.isNotBlank()) append("🏠 Dirección: ${location.address}\n")
            if (location.comment.isNotBlank()) append("📝 Nota: ${location.comment}\n")
            append("\n🗺️ Ver en el mapa: $mapsUrl\n\n")
            append("_Enviado desde Localizador Pro_")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ubicación: ${location.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Compartir ubicación vía")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun shareCompleteRoute(routeId: Int, mapBitmap: Bitmap? = null) {
        val route = repository.getRouteById(routeId) ?: return
        val waypoints = repository.getWaypointsSync(routeId)

        val km = route.distanceMeters / 1000.0
        val durationMs = if (route.endTime > route.startTime) route.endTime - route.startTime else 0L
        val h = TimeUnit.MILLISECONDS.toHours(durationMs)
        val m = (TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60)
        val durationText = "${h}h ${m}min"

        val shareText = buildString {
            append("🚀 *Resumen de mi Trayecto: ${route.name}*\n\n")
            append("📏 Distancia: ${String.format("%.2f", km)} km\n")
            append("⏱️ Duración: $durationText\n")
            append("⚡ Vel. Máxima: ${String.format("%.1f", route.maxSpeed * 3.6f)} km/h\n")
            if (waypoints.isNotEmpty()) {
                append("\n📍 *Puntos destacados:*")
                waypoints.forEach { wp -> append("\n- ${wp.name}: ${wp.description}") }
            }
            append("\n\n🗺️ _Generado por Localizador Pro_")
        }

        val imageUris = arrayListOf<Uri>()
        mapBitmap?.let { bitmap ->
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val mapFile = File(cachePath, "map_${System.currentTimeMillis()}.png")
                val stream = FileOutputStream(mapFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mapFile)
                imageUris.add(contentUri)
            } catch (e: Exception) { e.printStackTrace() }
        }

        waypoints.forEach { wp ->
            wp.photoPath?.let { path ->
                try { imageUris.add(Uri.parse(path)) } catch (_: Exception) { }
            }
        }

        withContext(Dispatchers.Main) {
            val intent = if (imageUris.isEmpty()) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            val chooser = Intent.createChooser(intent, "Compartir trayecto en...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
