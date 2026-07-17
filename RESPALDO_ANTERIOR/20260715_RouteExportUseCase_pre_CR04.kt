package com.aistudio.localizador.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Xml
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aistudio.localizador.data.export.RouteExchanger
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.util.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Caso de uso responsable de la orquestación de la importación y exportación de rutas.
 * Asegura el confinamiento de hilos de persistencia en Dispatchers.IO y evita
 * la fatiga de RAM (OutOfMemoryError) escribiendo directamente a archivos en streaming.
 */
class RouteExportUseCase(
    private val context: Context,
    private val repository: RouteRepository,
    private val exchanger: RouteExchanger
) {

    /**
     * Exporta secuencialmente una ruta a formato CSV y la comparte mediante un Intent.
     */
    suspend fun exportCsv(routeId: Int) = withContext(Dispatchers.IO) {
        try {
            repository.getRouteById(routeId) ?: return@withContext
            val points = repository.getLocationPointsSync(routeId)
            val fileName = "export_${System.currentTimeMillis()}.csv"
            val file = context.getFileStreamPath(fileName)

            file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("Fecha,Lat,Lon\n")
                points.forEach { p ->
                    writer.write("${p.timestamp},${p.latitude},${p.longitude}\n")
                }
            }
            shareFile(file, "text/csv")
        } catch (_: Exception) { }
    }

    /**
     * Exporta en streaming un conjunto de rutas agrupadas en un único archivo GPX.
     */
    suspend fun exportGpx(routeIds: Set<Int>) = withContext(Dispatchers.IO) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.gpx"
            val file = context.getFileStreamPath(fileName)

            file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
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
            shareFile(file, "application/gpx+xml")
        } catch (_: Exception) { }
    }

    /**
     * Importa de forma secuencial una ruta a partir de una traza GPX externa.
     */
    suspend fun importGpx(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var routeName = "Importada"
            var routeId = -1
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "name" && routeId == -1) {
                        routeName = parser.nextText()
                        routeId = repository.startNewRoute(routeName)
                    } else if (parser.name == "trkpt") {
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
                eventType = parser.next()
            }
            true
        } catch (_: Exception) { false }
    }

    /**
     * Exporta una ruta y todas sus dependencias en formato ZIP y la comparte.
     */
    suspend fun exportZip(routeId: Int) = withContext(Dispatchers.IO) {
        try {
            val route = repository.getRouteById(routeId)
            val zipFile = generateRouteZipFile(routeId, route?.name ?: "ruta")
            FileOutputStream(zipFile).use { fos ->
                exchanger.exportRouteAsZip(routeId, fos).getOrThrow()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Exportar ruta")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Guarda la exportación consolidada de la ruta en la URI del SAF provista por el usuario.
     */
    suspend fun saveZipToUri(routeId: Int, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                exchanger.exportRouteAsZip(routeId, outputStream).getOrThrow()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ZIP guardado correctamente", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al guardar ZIP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    /**
     * Exporta la ruta consolidada utilizando el tipo de archivo MIME centralizado en constantes.
     */
    suspend fun exportRouteAsZip(routeId: Int) = withContext(Dispatchers.IO) {
        try {
            val route = repository.getRouteById(routeId)
            val zipFile = generateRouteZipFile(routeId, route?.name ?: "ruta")
            FileOutputStream(zipFile).use { fos ->
                exchanger.exportRouteAsZip(routeId, fos).getOrThrow()
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = AppConstants.Backup.MIME_TYPE
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Compartir ruta en...")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Importa de forma asíncrona una ruta relacional desde una URI de archivo ZIP.
     */
    suspend fun importZip(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                exchanger.importarRuta(inputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generateRouteZipFile(routeId: Int, routeName: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedName = routeName
            .trim()
            .replace(Regex("[\\s/\\\\?%*:|\"<>]"), "_")
            .take(20)
        val fileName = "ROUTE_${routeId}_${sanitizedName}_$timestamp.zip"
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    }

    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Exportar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) { }
    }

    private fun String.escapeXml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
