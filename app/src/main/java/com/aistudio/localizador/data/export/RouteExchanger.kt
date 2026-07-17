package com.aistudio.localizador.data.export

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import android.util.JsonWriter
import androidx.core.content.FileProvider
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.model.RouteFavoriteCrossRef
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.model.Waypoint
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.util.AppConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Motor de empaquetado e intercambio consolidado de rutas de telemetría.
 * Implementa la serialización secuencial (Streaming) con JsonWriter de Android
 * para prevenir excepciones OutOfMemoryError en trayectos extensos.
 */
class RouteExchanger(
    private val context: Context,
    private val repository: RouteRepository
) {

    private val fileProviderAuthority: String
        get() = "${context.packageName}.fileprovider"

    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    @Deprecated("Usa exportRouteAsZip() en su lugar", ReplaceWith("exportRouteAsZip(routeId, outputStream)"))
    suspend fun exportarRutaCompleta(routeId: Int, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val route = repository.getRouteById(routeId) ?: return@withContext
        val points = repository.getLocationPointsSync(routeId)
        val waypoints = repository.getWaypointsSync(routeId)
        val favorites = repository.getFavoritesForRoute(routeId)
        val crossRefs = favorites.map { RouteFavoriteCrossRef(routeId = routeId, favoriteId = it.id) }

        ZipOutputStream(outputStream).use { zos ->
            val buffer = ByteArray(8192)

            // 1. ruta.json
            zos.putNextEntry(ZipEntry("ruta.json"))
            val writer = BufferedWriter(OutputStreamWriter(zos, Charsets.UTF_8))
            writeRutaJsonStream(writer, route, points, waypoints)
            zos.closeEntry()

            // 2. favoritos.json
            zos.putNextEntry(ZipEntry("favoritos.json"))
            val favWriter = BufferedWriter(OutputStreamWriter(zos, Charsets.UTF_8))
            writeFavoritosJsonStream(favWriter, favorites, crossRefs)
            zos.closeEntry()

            // 3. fotos
            val photoFiles = collectPhotoFilenames(waypoints, favorites)
            for ((filename, sourcePath) in photoFiles) {
                val file = resolvePhotoFile(sourcePath) ?: continue
                if (!file.exists()) continue
                zos.putNextEntry(ZipEntry("fotos/$filename"))
                file.inputStream().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        zos.write(buffer, 0, bytesRead)
                    }
                }
                zos.closeEntry()
            }
        }
    }

    /**
     * Exporta de forma asíncrona y en streaming una ruta con todas sus entidades
     * relacionadas en un archivo comprimido ZIP.
     */
    suspend fun exportRouteAsZip(routeId: Int, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val route = repository.getRouteById(routeId) ?: throw Exception("Ruta no encontrada")
            val points = repository.getLocationPointsSync(routeId)
            val waypoints = repository.getWaypointsSync(routeId)
            val favorites = repository.getFavoritesForRoute(routeId)
            val crossRefs = favorites.map { RouteFavoriteCrossRef(routeId = routeId, favoriteId = it.id) }

            ZipOutputStream(outputStream).use { zipOut ->
                val writer = BufferedWriter(OutputStreamWriter(zipOut, Charsets.UTF_8))

                // 1. ruta.json (Persistencia relacional)
                zipOut.putNextEntry(ZipEntry("ruta.json"))
                writeRutaJsonStream(writer, route, points, waypoints)
                zipOut.closeEntry()

                // 2. favoritos.json (Lugares favoritos asociados)
                zipOut.putNextEntry(ZipEntry("favoritos.json"))
                writeFavoritosJsonStream(writer, favorites, crossRefs)
                zipOut.closeEntry()

                // 3. track.gpx (Compartido social estándar)
                zipOut.putNextEntry(ZipEntry("track.gpx"))
                writeGpxStream(writer, route.name, points)
                zipOut.closeEntry()

                // 4. telemetry.csv (Compartido CSV)
                zipOut.putNextEntry(ZipEntry("telemetry.csv"))
                writeCsvStream(writer, route, points)
                zipOut.closeEntry()

                // 5. fotos (Multimedia asociada)
                val photoFiles = collectPhotoFilenames(waypoints, favorites)
                for ((filename, sourcePath) in photoFiles) {
                    val file = resolvePhotoFile(sourcePath) ?: continue
                    if (!file.exists()) continue
                    zipOut.putNextEntry(ZipEntry("fotos/$filename"))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
    }

    /**
     * Importa una ruta a partir de un flujo ZIP de entrada.
     */
    suspend fun importarRuta(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val tempDir = File(context.cacheDir, "import_$timestamp").also { it.mkdirs() }

        try {
            // 1. Extraer ZIP al directorio temporal
            extractZip(inputStream, tempDir)

            // 2. Copiar fotos a filesDir/images/ y construir mapa filename → URI
            val fotosDir = File(tempDir, "fotos")
            val photoUriMap = mutableMapOf<String, String>()
            if (fotosDir.exists()) {
                fotosDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val destFile = File(imagesDir, file.name)
                        file.copyTo(destFile, overwrite = true)
                        val uri = FileProvider.getUriForFile(context, fileProviderAuthority, destFile)
                        photoUriMap[file.name] = uri.toString()
                    }
                }
            }

            // 3. Leer y procesar ruta.json
            val rutaFile = File(tempDir, "ruta.json")
            var newRouteId: Int? = null
            if (rutaFile.exists()) {
                val payload = JSONObject(rutaFile.readText())
                newRouteId = processRutaJson(payload, photoUriMap)
            }

            // 4. Leer y procesar favoritos.json
            val favoritosFile = File(tempDir, "favoritos.json")
            if (favoritosFile.exists() && newRouteId != null) {
                val payload = JSONObject(favoritosFile.readText())
                processFavoritosJson(payload, photoUriMap, newRouteId)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ─── ESTRUCTURA DE ESCRITURA EN STREAMING (GPX y CSV) ───

    private fun writeGpxStream(writer: java.io.Writer, name: String, points: List<LocationPoint>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<gpx version=\"1.1\" creator=\"LocalizadorPro\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        writer.write("<trk><name>${name.escapeXml()}</name><trkseg>\n")
        points.forEach { p ->
            writer.write("<trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\">\n")
            writer.write("<ele>${p.altitude}</ele>\n")
            writer.write("<speed>${p.speed}</speed>\n")
            writer.write("<time>${dateFormat.format(Date(p.timestamp))}</time>\n")
            writer.write("</trkpt>\n")
        }
        writer.write("</trkseg></trk>\n")
        writer.write("</gpx>\n")
        writer.flush()
    }

    private fun writeCsvStream(writer: java.io.Writer, route: Route, points: List<LocationPoint>) {
        writer.write("# Route: ${route.name}\n")
        writer.write("# Activity Mode: ${route.activityMode}\n")
        writer.write("# Total Points: ${points.size}\n")
        writer.write("timestamp_ms,latitude,longitude,altitude_m,speed_ms,speed_kmh,activity_mode\n")
        points.forEach { p ->
            val speedKmh = String.format(Locale.US, "%.2f", p.speed * 3.6f)
            writer.write("${p.timestamp},${p.latitude},${p.longitude},${p.altitude},${p.speed},$speedKmh,${route.activityMode}\n")
        }
        writer.flush()
    }

    private fun String.escapeXml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // ─── ESCRITURA EN STREAMING CON JSONWRITER (ruta.json y favoritos.json) ───

    private fun writeRutaJsonStream(writer: java.io.Writer, route: Route, points: List<LocationPoint>, waypoints: List<Waypoint>) {
        val jsonWriter = JsonWriter(writer)
        jsonWriter.beginObject()

        // 1. "route" object
        jsonWriter.name("route")
        jsonWriter.beginObject()
        jsonWriter.name("name").value(route.name)
        jsonWriter.name("startTime").value(route.startTime)
        jsonWriter.name("endTime").value(route.endTime)
        jsonWriter.name("distanceMeters").value(route.distanceMeters)
        jsonWriter.name("isActive").value(route.isActive)
        jsonWriter.name("maxSpeed").value(route.maxSpeed.toDouble())
        jsonWriter.name("avgSpeed").value(route.avgSpeed.toDouble())
        jsonWriter.endObject()

        // 2. "locationPoints" array
        jsonWriter.name("locationPoints")
        jsonWriter.beginArray()
        points.forEach { p ->
            jsonWriter.beginObject()
            jsonWriter.name("latitude").value(p.latitude)
            jsonWriter.name("longitude").value(p.longitude)
            jsonWriter.name("altitude").value(p.altitude)
            jsonWriter.name("speed").value(p.speed.toDouble())
            jsonWriter.name("timestamp").value(p.timestamp)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        // 3. "waypoints" array
        jsonWriter.name("waypoints")
        jsonWriter.beginArray()
        waypoints.forEach { wp ->
            jsonWriter.beginObject()
            jsonWriter.name("name").value(wp.name)
            jsonWriter.name("description").value(wp.description)
            jsonWriter.name("latitude").value(wp.latitude)
            jsonWriter.name("longitude").value(wp.longitude)
            jsonWriter.name("photoFile").value(wp.photoPath?.extractFilename() ?: "")
            jsonWriter.name("author").value(wp.author)
            jsonWriter.name("color").value(wp.color)
            jsonWriter.name("timestamp").value(wp.timestamp)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        jsonWriter.endObject()
        jsonWriter.flush()
    }

    private fun writeFavoritosJsonStream(writer: java.io.Writer, favorites: List<SavedLocation>, crossRefs: List<RouteFavoriteCrossRef>) {
        val jsonWriter = JsonWriter(writer)
        jsonWriter.beginObject()

        // 1. "favorites" array
        jsonWriter.name("favorites")
        jsonWriter.beginArray()
        favorites.forEach { fav ->
            jsonWriter.beginObject()
            jsonWriter.name("name").value(fav.name)
            jsonWriter.name("address").value(fav.address)
            jsonWriter.name("latitude").value(fav.latitude)
            jsonWriter.name("longitude").value(fav.longitude)
            jsonWriter.name("comment").value(fav.comment)
            jsonWriter.name("photoFile").value(fav.photoPath?.extractFilename() ?: "")
            jsonWriter.name("author").value(fav.author)
            jsonWriter.name("phoneNumber").value(fav.phoneNumber)
            jsonWriter.name("email").value(fav.email)
            jsonWriter.name("isContact").value(fav.isContact)
            jsonWriter.name("color").value(fav.color)
            jsonWriter.name("country").value(fav.country)
            jsonWriter.name("city").value(fav.city)
            jsonWriter.name("timestamp").value(fav.timestamp)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        // 2. "crossRefs" array
        jsonWriter.name("crossRefs")
        jsonWriter.beginArray()
        crossRefs.forEach { ref ->
            val fav = favorites.find { it.id == ref.favoriteId } ?: return@forEach
            jsonWriter.beginObject()
            jsonWriter.name("favoriteLatitude").value(fav.latitude)
            jsonWriter.name("favoriteLongitude").value(fav.longitude)
            jsonWriter.endObject()
        }
        jsonWriter.endArray()

        jsonWriter.endObject()
        jsonWriter.flush()
    }

    // ─── Procesamiento de Importación ───

    private suspend fun processRutaJson(payload: JSONObject, photoUriMap: Map<String, String>): Int {
        val routeObj = payload.getJSONObject("route")
        val newRoute = Route(
            name = routeObj.getString("name"),
            startTime = routeObj.getLong("startTime"),
            endTime = routeObj.getLong("endTime"),
            distanceMeters = routeObj.getDouble("distanceMeters"),
            isActive = routeObj.getBoolean("isActive"),
            maxSpeed = routeObj.getDouble("maxSpeed").toFloat(),
            avgSpeed = routeObj.getDouble("avgSpeed").toFloat()
        )
        val newRouteId = repository.insertRouteDirectly(newRoute)

        val pointsArray = when {
            payload.has("locationPoints") -> payload.getJSONArray("locationPoints")
            payload.has("points") -> payload.getJSONArray("points")
            else -> JSONArray()
        }

        if (pointsArray.length() > 0) {
            val points = mutableListOf<LocationPoint>()
            for (i in 0 until pointsArray.length()) {
                val p = pointsArray.getJSONObject(i)
                val lat = when {
                    p.has("latitude") -> p.getDouble("latitude")
                    p.has("lat") -> p.getDouble("lat")
                    else -> 0.0
                }
                val lon = when {
                    p.has("longitude") -> p.getDouble("longitude")
                    p.has("lon") -> p.getDouble("lon")
                    else -> 0.0
                }
                val alt = when {
                    p.has("altitude") -> p.getDouble("altitude")
                    p.has("alt") -> p.getDouble("alt")
                    else -> 0.0
                }
                val spd = when {
                    p.has("speed") -> p.getDouble("speed").toFloat()
                    p.has("spd") -> p.getDouble("spd").toFloat()
                    else -> 0.0f
                }
                val ts = when {
                    p.has("timestamp") -> p.getLong("timestamp")
                    p.has("time") -> p.getLong("time")
                    else -> System.currentTimeMillis()
                }
                points.add(LocationPoint(
                    routeId = newRouteId,
                    latitude = lat,
                    longitude = lon,
                    altitude = alt,
                    speed = spd,
                    timestamp = ts
                ))
            }
            repository.addLocationPoints(points)
        }

        val waypointsArray = payload.getJSONArray("waypoints")
        if (waypointsArray.length() > 0) {
            for (i in 0 until waypointsArray.length()) {
                val wp = waypointsArray.getJSONObject(i)
                val photoFile = wp.optString("photoFile").takeIf { it.isNotEmpty() }
                val photoPath = if (photoFile != null) photoUriMap[photoFile] else null

                repository.addWaypoint(
                    routeId = newRouteId,
                    name = wp.getString("name"),
                    description = wp.optString("description", ""),
                    latitude = wp.getDouble("latitude"),
                    longitude = wp.getDouble("longitude"),
                    photoPath = photoPath,
                    author = wp.optString("author", ""),
                    color = wp.optString("color", "#00FF00")
                )
            }
        }

        return newRouteId
    }

    private suspend fun processFavoritosJson(payload: JSONObject, photoUriMap: Map<String, String>, newRouteId: Int) {
        val favoritesArray = payload.getJSONArray("favorites")
        if (favoritesArray.length() == 0) return

        val latLonToId = mutableMapOf<String, Int>()

        for (i in 0 until favoritesArray.length()) {
            val fav = favoritesArray.getJSONObject(i)
            val photoFile = fav.optString("photoFile").takeIf { it.isNotEmpty() }
            val photoPath = if (photoFile != null) photoUriMap[photoFile] else null

            val location = SavedLocation(
                name = fav.getString("name"),
                address = fav.optString("address", ""),
                latitude = fav.getDouble("latitude"),
                longitude = fav.getDouble("longitude"),
                comment = fav.optString("comment", ""),
                photoPath = photoPath,
                author = fav.optString("author", ""),
                phoneNumber = fav.optString("phoneNumber", ""),
                email = fav.optString("email", ""),
                isContact = fav.optBoolean("isContact", false),
                color = fav.optString("color", "#FFD700"),
                country = fav.optString("country", "Desconocido"),
                city = fav.optString("city", "Desconocida"),
                timestamp = fav.optLong("timestamp", System.currentTimeMillis())
            )

            val key = "${location.latitude}_${location.longitude}"

            val result = repository.insertIfNew(location)
            val savedId = if (result > 0) {
                result.toInt()
            } else {
                repository.updatePhotoPathByLatLon(location.latitude, location.longitude, location.photoPath)
                repository.getAllSavedLocationsSync()
                    .find { it.latitude == location.latitude && it.longitude == location.longitude }
                    ?.id
            }

            if (savedId != null) {
                latLonToId[key] = savedId
            }
        }

        val crossRefsArray = payload.optJSONArray("crossRefs")
        if (crossRefsArray != null && crossRefsArray.length() > 0) {
            for (i in 0 until crossRefsArray.length()) {
                val ref = crossRefsArray.getJSONObject(i)
                val lat = ref.getDouble("favoriteLatitude")
                val lon = ref.getDouble("favoriteLongitude")
                val key = "${lat}_${lon}"
                val favoriteId = latLonToId[key]
                if (favoriteId != null) {
                    repository.addFavoriteToRoute(newRouteId, favoriteId)
                }
            }
        }
    }

    // ─── Utilidades ───

    private fun extractZip(inputStream: InputStream, targetDir: File) {
        ZipInputStream(inputStream).use { zis ->
            val buffer = ByteArray(8192)
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { fos ->
                        var bytesRead: Int
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun collectPhotoFilenames(
        waypoints: List<Waypoint>,
        favorites: List<SavedLocation>
    ): List<Pair<String, String>> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Pair<String, String>>()
        for (wp in waypoints) {
            wp.photoPath?.let { path ->
                val name = path.extractFilename() ?: return@let
                if (seen.add(name)) result.add(name to path)
            }
        }
        for (fav in favorites) {
            fav.photoPath?.let { path ->
                val name = path.extractFilename() ?: return@let
                if (seen.add(name)) result.add(name to path)
            }
        }
        return result
    }

    private fun resolvePhotoFile(sourcePath: String): File? {
        return try {
            val uri = sourcePath.toUri()
            val filename = uri.lastPathSegment ?: return null
            File(imagesDir, filename)
        } catch (_: Exception) {
            null
        }
    }

    private fun String.extractFilename(): String? {
        return try {
            this.toUri().lastPathSegment
        } catch (_: Exception) {
            null
        }
    }
}
