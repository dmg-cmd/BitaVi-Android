package com.aistudio.localizador.data.repository

import com.aistudio.localizador.data.db.RouteDao
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.model.RouteFavoriteCrossRef
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.model.Waypoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import android.location.Location
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class RouteRepository @Inject constructor(private val routeDao: RouteDao) {

    val allRoutes: Flow<List<Route>> = routeDao.getAllRoutesFlow()

    suspend fun startNewRoute(name: String): Int {
        val currentActive = routeDao.getActiveRoute()
        if (currentActive != null) {
            routeDao.updateRoute(
                currentActive.copy(
                    isActive = false,
                    endTime = System.currentTimeMillis()
                )
            )
        }

        val newRoute = Route(
            name = name,
            isActive = true,
            startTime = System.currentTimeMillis(),
            distanceMeters = 0.0
        )
        return routeDao.insertRoute(newRoute).toInt()
    }

    suspend fun insertRouteDirectly(route: Route): Int {
        return routeDao.insertRoute(route).toInt()
    }

    suspend fun getRouteById(id: Int): Route? {
        return routeDao.getRouteById(id)
    }

    fun getRouteByIdFlow(id: Int): Flow<Route?> {
        return routeDao.getRouteByIdFlow(id)
    }

    suspend fun updateRoute(route: Route) {
        routeDao.updateRoute(route)
    }

    suspend fun updateRouteColor(routeId: Int, color: String) {
        routeDao.updateRouteColor(routeId, color)
    }

    suspend fun getLastRoute(): Route? = withContext(Dispatchers.IO) {
        routeDao.getLastRoute()
    }

    suspend fun getLastPointForRoute(routeId: Int): LocationPoint? = withContext(Dispatchers.IO) {
        routeDao.getLastPointForRoute(routeId)
    }

    suspend fun extendRouteEndTime(routeId: Int, endTime: Long) = withContext(Dispatchers.IO) {
        routeDao.updateRouteEndTime(routeId, endTime)
    }

    suspend fun stopActiveRoute(
        routeId: Int,
        distanceMeters: Double,
        activityMode: String = "WALKING"
    ): Boolean = withContext(NonCancellable + Dispatchers.IO) {
        try {
            val route = routeDao.getRouteById(routeId)
            val points = routeDao.getLocationPointsForRoute(routeId)

            if (route != null) {
                val now = System.currentTimeMillis()
                val isAutoOrStop = route.name.startsWith("Auto-Ruta") || route.name.startsWith("Parada")
                val isZeroMovement = distanceMeters < 25.0

                if (isAutoOrStop && isZeroMovement) {
                    val currentPoint = points.lastOrNull() ?: points.firstOrNull()
                    if (currentPoint != null) {
                        val durationMs = maxOf(0L, now - route.startTime)
                        recordOrAccumulateStopFavorite(
                            latitude = currentPoint.latitude,
                            longitude = currentPoint.longitude,
                            durationMs = durationMs,
                            timestamp = now
                        )
                    }
                    // Purgar la ruta efímera para que NO figure como una ruta en la lista de rutas
                    routeDao.deleteRouteById(routeId)
                    routeDao.deleteLocationPointsForRoute(routeId)
                    Log.i("RouteRepository", "Punto fijo de parada registrado en Favoritos. Ruta efímera $routeId descartada del historial.")
                    return@withContext true
                }

                val maxSpeed = if (points.isNotEmpty()) points.maxOf { it.speed } else 0f
                val avgSpeed = if (points.isNotEmpty()) points.map { it.speed }.average().toFloat() else 0f

                val dominantMode = if (activityMode == "STOPPED" && points.isNotEmpty()) {
                    val avgKmh = points.map { it.speed }.average() * 3.6
                    when {
                        avgKmh > 20.0 -> "DRIVING"
                        avgKmh > 7.0 -> "BIKING"
                        avgKmh >= 1.5 -> "WALKING"
                        else -> activityMode
                    }
                } else {
                    activityMode
                }

                routeDao.updateRoute(
                    route.copy(
                        isActive = false,
                        endTime = now,
                        distanceMeters = distanceMeters,
                        maxSpeed = maxSpeed,
                        avgSpeed = avgSpeed,
                        activityMode = dominantMode
                    )
                )
                true
            } else {
                Log.w("RouteRepository", "Se intento cerrar una ruta inexistente: ID $routeId")
                false
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error catastrofico en la persistencia final de ruta $routeId", e)
            false
        }
    }

    suspend fun deleteRoute(routeId: Int) {
        routeDao.deleteRouteById(routeId)
    }

    suspend fun deleteRoutes(routeIds: List<Int>) {
        routeDao.deleteRoutes(routeIds)
    }

    suspend fun addLocationPoint(
        routeId: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        speed: Float
    ) {
        val point = LocationPoint(
            routeId = routeId,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speed = speed,
            timestamp = System.currentTimeMillis()
        )
        routeDao.insertLocationPoint(point)
    }

    suspend fun addLocationPoints(points: List<LocationPoint>) {
        routeDao.insertLocationPoints(points)
    }

    fun getLocationPointsFlow(routeId: Int): Flow<List<LocationPoint>> {
        return routeDao.getLocationPointsForRouteFlow(routeId)
    }

    suspend fun getLocationPointsSync(routeId: Int): List<LocationPoint> {
        return routeDao.getLocationPointsForRoute(routeId)
    }

    suspend fun addWaypoint(
        routeId: Int,
        name: String,
        description: String,
        latitude: Double,
        longitude: Double,
        photoPath: String? = null,
        author: String = "",
        color: String = "#00FF00"
    ) {
        val waypoint = Waypoint(
            routeId = routeId,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            photoPath = photoPath,
            author = author,
            color = color
        )
        routeDao.insertWaypoint(waypoint)
    }

    fun getWaypointsFlow(routeId: Int): Flow<List<Waypoint>> {
        return routeDao.getWaypointsForRouteFlow(routeId)
    }

    suspend fun getWaypointsSync(routeId: Int): List<Waypoint> {
        return routeDao.getWaypointsForRoute(routeId)
    }

    val allSavedLocations: Flow<List<SavedLocation>> = routeDao.getAllSavedLocationsFlow()

    suspend fun getAllSavedLocationsSync(): List<SavedLocation> = routeDao.getAllSavedLocations()

    suspend fun addSavedLocation(
        name: String, 
        address: String, 
        latitude: Double, 
        longitude: Double,
        comment: String = "",
        author: String = "",
        photoPath: String? = null,
        phoneNumber: String = "",
        email: String = "",
        isContact: Boolean = false,
        country: String = "Desconocido",
        city: String = "Desconocida",
        color: String = "#FFD700",
        timestamp: Long = System.currentTimeMillis(),
    ): Long {
        val isStopPoint = author.equals("Automático", ignoreCase = true) ||
                author.contains("Parada", ignoreCase = true) ||
                comment.contains("Parada", ignoreCase = true)
        val effectiveColor = if (isStopPoint && color.equals("#FFD700", ignoreCase = true)) {
            "#FF6D00"
        } else {
            color
        }
        val effectiveCategory = if (isStopPoint || effectiveColor.equals("#FF6D00", ignoreCase = true)) {
            "AUTOMATIC"
        } else {
            "DEFAULT"
        }
        val location = SavedLocation(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            comment = comment,
            author = author,
            photoPath = photoPath,
            phoneNumber = phoneNumber,
            email = email,
            isContact = isContact,
            country = country,
            city = city,
            color = effectiveColor,
            iconCategory = effectiveCategory
        )
        return routeDao.insertSavedLocation(location)
    }

    suspend fun updateSavedLocation(location: SavedLocation) {
        routeDao.updateSavedLocation(location)
    }

    suspend fun updateSavedLocationColor(id: Int, color: String) {
        routeDao.updateSavedLocationColor(id, color)
    }

    suspend fun deleteSavedLocation(location: SavedLocation) {
        routeDao.deleteSavedLocation(location)
    }

    suspend fun insertIfNew(location: SavedLocation): Long {
        return routeDao.insertIfNew(location)
    }

    suspend fun updatePhotoPathByLatLon(lat: Double, lon: Double, photoPath: String?) {
        routeDao.updatePhotoPathByLatLon(lat, lon, photoPath)
    }

    suspend fun deleteDuplicateFavorites() {
        routeDao.deleteDuplicateFavorites()
    }

    suspend fun cleanOrphanedRelations() {
        routeDao.cleanOrphanedRelations()
    }

    suspend fun getAllReferencedPhotoPaths(): List<String> {
        return routeDao.getAllReferencedPhotoPaths()
    }

    suspend fun deleteAllData() {
        routeDao.deleteAllRoutes()
        routeDao.deleteAllSavedLocations()
    }

    suspend fun addFavoriteToRoute(routeId: Int, favoriteId: Int) {
        routeDao.addRouteFavoriteCrossRef(RouteFavoriteCrossRef(routeId = routeId, favoriteId = favoriteId))
    }

    suspend fun removeFavoriteFromRoute(routeId: Int, favoriteId: Int) {
        routeDao.removeRouteFavoriteCrossRef(RouteFavoriteCrossRef(routeId = routeId, favoriteId = favoriteId))
    }

    fun getFavoritesForRouteFlow(routeId: Int): Flow<List<SavedLocation>> {
        return routeDao.getFavoritesForRouteFlow(routeId)
    }

    suspend fun getFavoritesForRoute(routeId: Int): List<SavedLocation> {
        return routeDao.getFavoritesForRoute(routeId)
    }

    fun getSavedLocationByIdFlow(id: Int): Flow<SavedLocation?> = routeDao.getSavedLocationByIdFlow(id)

    suspend fun recordOrAccumulateStopFavorite(
        latitude: Double,
        longitude: Double,
        durationMs: Long,
        timestamp: Long = System.currentTimeMillis(),
        addressHint: String = ""
    ): SavedLocation {
        val allFavs = routeDao.getAllSavedLocations()
        val proximityThresholdMeters = 50f

        var matchedFav: SavedLocation? = null
        var minDistance = Float.MAX_VALUE

        for (fav in allFavs) {
            val results = FloatArray(1)
            Location.distanceBetween(latitude, longitude, fav.latitude, fav.longitude, results)
            if (results[0] <= proximityThresholdMeters && results[0] < minDistance) {
                minDistance = results[0]
                matchedFav = fav
            }
        }

        if (matchedFav != null) {
            val previousDurationMs = extractDurationMsFromComment(matchedFav.comment)
            val newTotalMs = previousDurationMs + durationMs
            val formattedDuration = formatStopDuration(newTotalMs)

            val cleanComment = matchedFav.comment
                .replace(Regex("""⏱️[^\n\r]*"""), "")
                .trim()

            val updatedComment = if (cleanComment.isNotBlank()) {
                "$cleanComment\n⏱️ Tiempo acumulado de parada: $formattedDuration"
            } else {
                "⏱️ Tiempo acumulado de parada: $formattedDuration"
            }

            val updatedFav = matchedFav.copy(
                comment = updatedComment,
                timestamp = timestamp
            )
            routeDao.updateSavedLocation(updatedFav)
            Log.i("RouteRepository", "Favorito existente #${matchedFav.id} actualizado con tiempo de parada acumulado: $formattedDuration")
            return updatedFav
        } else {
            val formattedDuration = formatStopDuration(durationMs)
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            val defaultName = if (addressHint.isNotBlank()) addressHint else "Parada $timeFormat"
            val defaultAddress = if (addressHint.isNotBlank()) addressHint else String.format(Locale.US, "GPS: %.5f, %.5f", latitude, longitude)

            val newFav = SavedLocation(
                name = defaultName,
                address = defaultAddress,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
                comment = "⏱️ Tiempo de parada: $formattedDuration",
                author = "Automático",
                color = "#FF6D00",
                iconCategory = "AUTOMATIC"
            )
            val newId = routeDao.insertSavedLocation(newFav)
            Log.i("RouteRepository", "Nuevo punto de parada registrado en Favoritos #$newId con duración $formattedDuration")
            return newFav.copy(id = newId.toInt())
        }
    }

    companion object {
        fun formatStopDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return when {
                hours > 0 -> String.format(Locale.getDefault(), "%d h %02d min", hours, minutes)
                minutes > 0 -> String.format(Locale.getDefault(), "%d min %02d seg", minutes, seconds)
                else -> String.format(Locale.getDefault(), "%d seg", maxOf(1L, seconds))
            }
        }

        fun extractDurationMsFromComment(comment: String): Long {
            var totalMs = 0L
            val hMatch = Regex("""(\d+)\s*h""").find(comment)
            val mMatch = Regex("""(\d+)\s*min""").find(comment)
            val sMatch = Regex("""(\d+)\s*seg""").find(comment)

            hMatch?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { totalMs += it * 3600 * 1000L }
            mMatch?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { totalMs += it * 60 * 1000L }
            sMatch?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { totalMs += it * 1000L }

            return totalMs
        }
    }
}
