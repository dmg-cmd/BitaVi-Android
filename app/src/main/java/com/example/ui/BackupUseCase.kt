package com.example.ui

import android.content.Context
import android.location.Location
import android.net.Uri
import android.util.Xml
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupUseCase(
    private val context: Context,
    private val repository: RouteRepository,
    private val geocoderUseCase: GeocoderUseCase
) {

    sealed class RestoreDecision {
        data class ProcessZip(val uri: Uri) : RestoreDecision()
        data class ProcessGpx(val uri: Uri) : RestoreDecision()
        object Invalid : RestoreDecision()
    }

    suspend fun detectRestoreType(uri: Uri): RestoreDecision = withContext(Dispatchers.IO) {
        try {
            val headerBytes = ByteArray(4)
            context.contentResolver.openInputStream(uri)?.use { it.read(headerBytes) }
            if (headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte()) {
                RestoreDecision.ProcessZip(uri)
            } else if (headerBytes[0] == 0x3C.toByte()) {
                RestoreDecision.ProcessGpx(uri)
            } else {
                RestoreDecision.Invalid
            }
        } catch (_: Exception) { RestoreDecision.Invalid }
    }

    suspend fun prepareBackup(): File? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val backupFile = File(context.cacheDir, "backup_$timestamp.zip")
            val buffer = ByteArray(4096)
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                val favs = repository.allSavedLocations.first()
                if (favs.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("favoritos.txt"))
                    val content = favs.joinToString("\n") { f ->
                        "${f.name}|${f.address}|${f.latitude}|${f.longitude}|${f.comment}|${f.author}|${f.photoPath ?: ""}|${f.phoneNumber}|${f.email}|${f.isContact}|${f.color}|${f.country}|${f.city}|${f.timestamp}"
                    }
                    zos.write(content.toByteArray())
                    zos.closeEntry()
                }

                val routes = repository.allRoutes.first()
                if (routes.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("rutas.txt"))
                    val routesContent = routes.joinToString("\n") { r ->
                        "${r.id}|${r.name}|${r.startTime}|${r.endTime}|${r.distanceMeters}|${r.maxSpeed}|${r.avgSpeed}"
                    }
                    zos.write(routesContent.toByteArray())
                    zos.closeEntry()

                    val allPoints = mutableListOf<String>()
                    val allWaypoints = mutableListOf<String>()

                    for (route in routes) {
                        val pts = repository.getLocationPointsSync(route.id)
                        for (p in pts) {
                            allPoints.add("${p.routeId}|${p.latitude}|${p.longitude}|${p.altitude}|${p.speed}|${p.timestamp}")
                        }
                        val wps = repository.getWaypointsSync(route.id)
                        for (w in wps) {
                            allWaypoints.add("${w.routeId}|${w.name}|${w.description}|${w.latitude}|${w.longitude}|${w.photoPath ?: ""}|${w.author}|${w.color}")
                        }
                    }

                    if (allPoints.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("puntos.txt"))
                        zos.write(allPoints.joinToString("\n").toByteArray())
                        zos.closeEntry()
                    }
                    if (allWaypoints.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("marcadores.txt"))
                        zos.write(allWaypoints.joinToString("\n").toByteArray())
                        zos.closeEntry()
                    }
                }

                val imageDir = File(context.filesDir, "images")
                if (imageDir.exists()) {
                    imageDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            zos.putNextEntry(ZipEntry("images/${file.name}"))
                            file.inputStream().use { inputStream ->
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    zos.write(buffer, 0, bytesRead)
                                }
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreFromZip(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Restaurando datos completos...", Toast.LENGTH_SHORT).show()
            }

            context.contentResolver.openInputStream(uri)?.use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    val imageDir = File(context.filesDir, "images")
                    if (!imageDir.exists()) imageDir.mkdirs()
                    val buffer = ByteArray(4096)

                    val pendingRouteLines = mutableListOf<String>()
                    val pendingPointLines = mutableListOf<String>()
                    val pendingWaypointLines = mutableListOf<String>()

                    while (entry != null) {
                        val name = entry.name
                        when {
                            name.endsWith(".gpx", ignoreCase = true) -> {
                                val routeName = name.substringAfterLast("/").substringBeforeLast(".")
                                val content = zis.readBytes()
                                processGpxInputStream(java.io.ByteArrayInputStream(content), routeName)
                            }
                            name.contains("localizador_database") -> {
                                val dbFile = File(context.getDatabasePath("localizador_database").parent, name.substringAfterLast("/"))
                                dbFile.outputStream().use { os ->
                                    var bytesRead: Int
                                    while (zis.read(buffer).also { bytesRead = it } != -1) {
                                        os.write(buffer, 0, bytesRead)
                                    }
                                }
                            }
                            name == "mis_favoritos.txt" || name == "favoritos.txt" -> {
                                val lines = zis.readBytes().decodeToString().split("\n")
                                val parsed = lines.filter { it.isNotBlank() }.mapNotNull { line ->
                                    val p = line.split("|")
                                    if (p.size < 4) return@mapNotNull null
                                    val nameVal = p[0]
                                    val addressVal = p[1]
                                    val lat = p[2].toDoubleOrNull() ?: return@mapNotNull null
                                    val lon = p[3].toDoubleOrNull() ?: return@mapNotNull null
                                    val commentVal = if (p.size > 4) p[4] else ""
                                    val authorVal = if (p.size > 5) p[5] else ""
                                    val originalPhotoPath = if (p.size > 6 && p[6].isNotBlank()) p[6] else null
                                    var photoPathVal = originalPhotoPath

                                    if (originalPhotoPath != null) {
                                        val fileName = originalPhotoPath.substringAfterLast("/")
                                        val file = File(context.filesDir, "images/$fileName")
                                        if (file.exists()) {
                                            photoPathVal = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            ).toString()
                                        }
                                    }
                                    val phoneNumberVal = if (p.size > 7) p[7] else ""
                                    val emailVal = if (p.size > 8) p[8] else ""
                                    val isContactVal = if (p.size > 9) p[9].toBoolean() else false
                                    val colorVal = if (p.size > 10) p[10] else "#FFD700"

                                    val details = geocoderUseCase.fetchLocationDetails(lat, lon)
                                    val countryVal = if (p.size > 11) p[11] else details.second
                                    val cityVal = if (p.size > 12) p[12] else details.third
                                    val timestampVal = if (p.size > 13) p[13].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()

                                    com.example.data.model.SavedLocation(
                                        name = nameVal,
                                        address = addressVal,
                                        latitude = lat,
                                        longitude = lon,
                                        comment = commentVal,
                                        author = authorVal,
                                        photoPath = photoPathVal,
                                        phoneNumber = phoneNumberVal,
                                        email = emailVal,
                                        isContact = isContactVal,
                                        country = countryVal,
                                        city = cityVal,
                                        color = colorVal,
                                        timestamp = timestampVal
                                    )
                                }

                                parsed
                                    .distinctBy { "${it.name}_${it.latitude}_${it.longitude}" }
                                    .forEach { repository.insertIfNew(it) }

                                repository.deleteDuplicateFavorites()
                                repository.cleanOrphanedRelations()
                            }
                            name == "rutas.txt" -> {
                                pendingRouteLines.addAll(
                                    zis.readBytes().decodeToString().split("\n").filter { it.isNotBlank() }
                                )
                            }
                            name == "puntos.txt" -> {
                                pendingPointLines.addAll(
                                    zis.readBytes().decodeToString().split("\n").filter { it.isNotBlank() }
                                )
                            }
                            name == "marcadores.txt" -> {
                                pendingWaypointLines.addAll(
                                    zis.readBytes().decodeToString().split("\n").filter { it.isNotBlank() }
                                )
                            }
                            (name.startsWith("fotos/") || name.startsWith("images/")) -> {
                                val fileName = name.substringAfterLast("/")
                                if (fileName.isNotEmpty()) {
                                    File(imageDir, fileName).outputStream().use { os ->
                                        var bytesRead: Int
                                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                                            os.write(buffer, 0, bytesRead)
                                        }
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }

                    if (pendingRouteLines.isNotEmpty()) {
                        val routeIdMap = mutableMapOf<Int, Int>()

                        for (line in pendingRouteLines) {
                            val p = line.split("|")
                            if (p.size >= 7) {
                                val originalId = p[0].toIntOrNull() ?: continue
                                val routeName = p[1]
                                val startTime = p[2].toLongOrNull() ?: System.currentTimeMillis()
                                val endTime = p[3].toLongOrNull() ?: 0L
                                val distance = p[4].toDoubleOrNull() ?: 0.0
                                val maxSpd = p[5].toFloatOrNull() ?: 0f
                                val avgSpd = p[6].toFloatOrNull() ?: 0f

                                val newRoute = Route(
                                    name = routeName,
                                    startTime = startTime,
                                    endTime = endTime,
                                    distanceMeters = distance,
                                    isActive = false,
                                    maxSpeed = maxSpd,
                                    avgSpeed = avgSpd
                                )
                                val newId = repository.insertRouteDirectly(newRoute)
                                routeIdMap[originalId] = newId
                            }
                        }

                        if (pendingPointLines.isNotEmpty()) {
                            val pointsBatch = mutableListOf<LocationPoint>()
                            for (line in pendingPointLines) {
                                val p = line.split("|")
                                if (p.size >= 6) {
                                    val originalRouteId = p[0].toIntOrNull() ?: continue
                                    val newRouteId = routeIdMap[originalRouteId] ?: continue
                                    val lat = p[1].toDoubleOrNull() ?: continue
                                    val lon = p[2].toDoubleOrNull() ?: continue
                                    val alt = p[3].toDoubleOrNull() ?: 0.0
                                    val spd = p[4].toFloatOrNull() ?: 0f
                                    val ts = p[5].toLongOrNull() ?: System.currentTimeMillis()

                                    pointsBatch.add(LocationPoint(
                                        routeId = newRouteId,
                                        latitude = lat,
                                        longitude = lon,
                                        altitude = alt,
                                        speed = spd,
                                        timestamp = ts
                                    ))
                                }
                            }
                            if (pointsBatch.isNotEmpty()) {
                                repository.addLocationPoints(pointsBatch)
                            }
                        }

                        if (pendingWaypointLines.isNotEmpty()) {
                            for (line in pendingWaypointLines) {
                                val p = line.split("|")
                                if (p.size >= 5) {
                                    val originalRouteId = p[0].toIntOrNull() ?: continue
                                    val newRouteId = routeIdMap[originalRouteId] ?: continue
                                    val wpName = p[1]
                                    val wpDesc = if (p.size > 2) p[2] else ""
                                    val wpLat = p[3].toDoubleOrNull() ?: continue
                                    val wpLon = p[4].toDoubleOrNull() ?: continue
                                    val wpPhoto = if (p.size > 5 && p[5].isNotBlank()) p[5] else null
                                    val wpAuthor = if (p.size > 6) p[6] else ""
                                    val wpColor = if (p.size > 7) p[7] else "#00FF00"

                                    repository.addWaypoint(
                                        routeId = newRouteId,
                                        name = wpName,
                                        description = wpDesc,
                                        latitude = wpLat,
                                        longitude = wpLon,
                                        photoPath = wpPhoto,
                                        author = wpAuthor,
                                        color = wpColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            false
        }
    }

    private suspend fun processGpxInputStream(inputStream: java.io.InputStream, defaultName: String) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var routeName = defaultName
            var routeId = -1
            val points = mutableListOf<LocationPoint>()

            var currentLat = 0.0
            var currentLon = 0.0
            var currentEle = 0.0
            var currentTime = 0L

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                if (eventType == XmlPullParser.START_TAG) {
                    when (tag) {
                        "name" -> if (routeId == -1) routeName = parser.nextText()
                        "trkpt" -> {
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        }
                        "ele" -> currentEle = parser.nextText().toDoubleOrNull() ?: 0.0
                        "time" -> {
                            val timeStr = parser.nextText()
                            currentTime = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                sdf.parse(timeStr)?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) { System.currentTimeMillis() }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && tag == "trkpt") {
                    if (routeId == -1) routeId = repository.startNewRoute(routeName)
                    var speed = 0f
                    if (points.isNotEmpty()) {
                        val last = points.last()
                        val dist = FloatArray(1)
                        Location.distanceBetween(last.latitude, last.longitude, currentLat, currentLon, dist)
                        val timeDiffSec = (currentTime - last.timestamp) / 1000.0
                        if (timeDiffSec > 0.0) speed = (dist[0] / timeDiffSec).toFloat()
                    }
                    points.add(LocationPoint(
                        routeId = routeId, latitude = currentLat, longitude = currentLon,
                        altitude = currentEle, speed = speed, timestamp = currentTime
                    ))
                }
                eventType = parser.next()
            }
            if (points.isNotEmpty()) {
                repository.addLocationPoints(points)
                var d = 0.0
                for (i in 0 until points.size - 1) {
                    val res = FloatArray(1)
                    Location.distanceBetween(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude, res)
                    d += res[0].toDouble()
                }
                repository.stopActiveRoute(routeId, d)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
