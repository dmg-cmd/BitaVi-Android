package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Xml
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.export.RouteExchanger
import com.example.data.model.LocationPoint
import com.example.data.repository.RouteRepository
import com.example.util.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RouteExportUseCase(
    private val context: Context,
    private val repository: RouteRepository,
    private val exchanger: RouteExchanger
) {

    suspend fun exportCsv(routeId: Int) = withContext(Dispatchers.IO) {
        try {
            val route = repository.getRouteById(routeId) ?: return@withContext
            val points = repository.getLocationPointsSync(routeId)
            val sb = StringBuilder("Fecha,Lat,Lon\n")
            points.forEach { sb.append("${it.timestamp},${it.latitude},${it.longitude}\n") }
            saveCsvFile(sb.toString(), route.name)
        } catch (_: Exception) { }
    }

    suspend fun exportGpx(routeIds: Set<Int>) = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\">\n")
            routeIds.forEach { id ->
                val route = repository.getRouteById(id) ?: return@forEach
                val points = repository.getLocationPointsSync(id)
                sb.append("<trk><name>${route.name}</name><trkseg>\n")
                points.forEach { sb.append("<trkpt lat=\"${it.latitude}\" lon=\"${it.longitude}\"></trkpt>\n") }
                sb.append("</trkseg></trk>\n")
            }
            sb.append("</gpx>")
            saveGpxFile(sb.toString())
        } catch (_: Exception) { }
    }

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
                        val pt = LocationPoint(routeId = routeId, latitude = lat, longitude = lon, altitude = 0.0, speed = 0f, timestamp = System.currentTimeMillis())
                        repository.addLocationPoints(listOf(pt))
                    }
                }
                eventType = parser.next()
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun exportZip(routeId: Int) = withContext(Dispatchers.IO) {
        try {
            val route = repository.getRouteById(routeId)
            val zipFile = generateRouteZipFile(routeId, route?.name ?: "ruta")
            FileOutputStream(zipFile).use { fos ->
                exchanger.exportarRutaCompleta(routeId, fos)
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

    suspend fun saveZipToUri(routeId: Int, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                exchanger.exportarRutaCompleta(routeId, outputStream)
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

    suspend fun exportRouteAsZip(routeId: Int) = withContext(Dispatchers.IO) {
        try {
            val route = repository.getRouteById(routeId)
            val zipFile = generateRouteZipFile(routeId, route?.name ?: "ruta")
            FileOutputStream(zipFile).use { fos ->
                exchanger.exportRouteAsZip(routeId, fos)
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

    private fun saveCsvFile(content: String, routeName: String) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.csv"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
            val file = context.getFileStreamPath(fileName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Exportar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) { }
    }

    private fun saveGpxFile(content: String) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.gpx"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
            val file = context.getFileStreamPath(fileName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Exportar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) { }
    }
}