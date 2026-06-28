package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.RouteFavoriteCrossRef
import com.example.data.model.SavedLocation
import com.example.data.model.Waypoint
import com.example.data.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class RouteExchanger(
    private val context: Context,
    private val repository: RouteRepository
) {

    private val fileProviderAuthority: String
        get() = "${context.packageName}.fileprovider"

    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

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
            zos.write(buildRutaJson(route, points, waypoints).toString().toByteArray())
            zos.closeEntry()

            // 2. favoritos.json
            zos.putNextEntry(ZipEntry("favoritos.json"))
            zos.write(buildFavoritosJson(favorites, crossRefs).toString().toByteArray())
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

    // ─── Construcción de JSON ───

    private fun buildRutaJson(route: Route, points: List<LocationPoint>, waypoints: List<Waypoint>): JSONObject {
        return JSONObject().apply {
            put("route", JSONObject().apply {
                put("name", route.name)
                put("startTime", route.startTime)
                put("endTime", route.endTime)
                put("distanceMeters", route.distanceMeters)
                put("isActive", route.isActive)
                put("maxSpeed", route.maxSpeed.toDouble())
                put("avgSpeed", route.avgSpeed.toDouble())
            })
            put("locationPoints", JSONArray().apply {
                points.forEach { p ->
                    put(JSONObject().apply {
                        put("latitude", p.latitude)
                        put("longitude", p.longitude)
                        put("altitude", p.altitude)
                        put("speed", p.speed.toDouble())
                        put("timestamp", p.timestamp)
                    })
                }
            })
            put("waypoints", JSONArray().apply {
                waypoints.forEach { wp ->
                    put(JSONObject().apply {
                        put("name", wp.name)
                        put("description", wp.description)
                        put("latitude", wp.latitude)
                        put("longitude", wp.longitude)
                        put("photoFile", wp.photoPath?.extractFilename())
                        put("author", wp.author)
                        put("color", wp.color)
                        put("timestamp", wp.timestamp)
                    })
                }
            })
        }
    }

    private fun buildFavoritosJson(favorites: List<SavedLocation>, crossRefs: List<RouteFavoriteCrossRef>): JSONObject {
        return JSONObject().apply {
            put("favorites", JSONArray().apply {
                favorites.forEach { fav ->
                    put(JSONObject().apply {
                        put("name", fav.name)
                        put("address", fav.address)
                        put("latitude", fav.latitude)
                        put("longitude", fav.longitude)
                        put("comment", fav.comment)
                        put("photoFile", fav.photoPath?.extractFilename())
                        put("author", fav.author)
                        put("phoneNumber", fav.phoneNumber)
                        put("email", fav.email)
                        put("isContact", fav.isContact)
                        put("color", fav.color)
                        put("country", fav.country)
                        put("city", fav.city)
                        put("timestamp", fav.timestamp)
                    })
                }
            })
            put("crossRefs", JSONArray().apply {
                crossRefs.forEach { ref ->
                    val fav = favorites.find { it.id == ref.favoriteId } ?: return@forEach
                    put(JSONObject().apply {
                        put("favoriteLatitude", fav.latitude)
                        put("favoriteLongitude", fav.longitude)
                    })
                }
            })
        }
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

        val pointsArray = payload.getJSONArray("locationPoints")
        if (pointsArray.length() > 0) {
            val points = mutableListOf<LocationPoint>()
            for (i in 0 until pointsArray.length()) {
                val p = pointsArray.getJSONObject(i)
                points.add(LocationPoint(
                    routeId = newRouteId,
                    latitude = p.getDouble("latitude"),
                    longitude = p.getDouble("longitude"),
                    altitude = p.getDouble("altitude"),
                    speed = p.getDouble("speed").toFloat(),
                    timestamp = p.getLong("timestamp")
                ))
            }
            repository.addLocationPoints(points)
        }

        val waypointsArray = payload.getJSONArray("waypoints")
        if (waypointsArray.length() > 0) {
            for (i in 0 until waypointsArray.length()) {
                val wp = waypointsArray.getJSONObject(i)
                val photoFile = wp.optString("photoFile", null)
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
            val photoFile = fav.optString("photoFile", null)
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
            val uri = Uri.parse(sourcePath)
            val filename = uri.lastPathSegment ?: return null
            File(imagesDir, filename)
        } catch (_: Exception) {
            null
        }
    }

    private fun String.extractFilename(): String? {
        return try {
            Uri.parse(this).lastPathSegment
        } catch (_: Exception) {
            null
        }
    }
}
