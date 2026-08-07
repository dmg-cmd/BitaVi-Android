package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Xml
import androidx.core.content.FileProvider
import com.aistudio.localizador.data.export.RouteExchanger
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.repository.RouteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class RouteExportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RouteRepository,
    private val exchanger: RouteExchanger
) {

    suspend fun exportCsv(routeId: Int): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val points = repository.getLocationPointsSync(routeId)
            val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.csv")
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("Fecha,Lat,Lon\n")
                points.forEach { p ->
                    writer.write("${p.timestamp},${p.latitude},${p.longitude}\n")
                }
            }
            file
        }
    }

    suspend fun exportGpx(routeIds: Set<Int>): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.gpx")
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\">\n")
                routeIds.forEach { id ->
                    val route = repository.getRouteById(id) ?: return@forEach
                    val points = repository.getLocationPointsSync(id)
                    writer.write("<trk><name>${route.name.escapeXml()}</name><trkseg>\n")
                    points.forEach { p ->
                        writer.write("<trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\"></trkpt>\n")
                    }
                    writer.write("</trkseg></trk>\n")
                }
                writer.write("</gpx>")
            }
            file
        }
    }

    suspend fun importGpx(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("No se pudo abrir el archivo")
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var routeName = "Importada"
            var routeId = -1
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "name" -> if (routeId == -1) {
                            routeName = parser.nextText()
                            routeId = repository.startNewRoute(routeName)
                        }
                        "trkpt" -> {
                            if (routeId == -1) routeId = repository.startNewRoute(routeName)
                            val lat = parser.getAttributeValue(null, "lat")?.toDouble() ?: 0.0
                            val lon = parser.getAttributeValue(null, "lon")?.toDouble() ?: 0.0
                            val pt = LocationPoint(
                                routeId = routeId,
                                latitude = lat,
                                longitude = lon,
                                altitude = 0.0,
                                speed = 0f,
                                timestamp = System.currentTimeMillis()
                            )
                            repository.addLocationPoints(listOf(pt))
                        }
                    }
                }
                eventType = parser.next()
            }
        }
    }

    suspend fun exportZip(routeId: Int): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val route = repository.getRouteById(routeId)
                ?: throw IllegalStateException("Ruta no encontrada: $routeId")
            val zipFile = generateRouteZipFile(routeId, route.name)
            FileOutputStream(zipFile).use { fos ->
                exchanger.exportRouteAsZip(routeId, fos).getOrThrow()
            }
            zipFile
        }
    }

    suspend fun exportRouteAsZip(routeId: Int): Result<File> = exportZip(routeId)

    suspend fun saveZipToUri(routeId: Int, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                exchanger.exportRouteAsZip(routeId, outputStream).getOrThrow()
            } ?: throw IllegalStateException("No se pudo abrir el URI de destino")
        }
    }

    suspend fun importZip(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                exchanger.importarRuta(inputStream)
            } ?: throw IllegalStateException("No se pudo abrir el archivo ZIP")
        }
    }

    fun buildShareIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun buildChooserIntent(intent: Intent, title: String): Intent {
        return Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun generateRouteZipFile(routeId: Int, routeName: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedName = routeName
            .trim()
            .replace(Regex("[\\s/\\\\?%*:|\"<>]"), "_")
            .take(20)
        val fileName = "ROUTE_${routeId}_${sanitizedName}_${timestamp}.zip"
        return File(context.cacheDir, fileName)
    }

    private fun String.escapeXml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
