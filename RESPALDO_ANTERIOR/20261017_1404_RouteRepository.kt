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
                    val prevRoute = routeDao.getPreviousCompletedRoute(routeId)
                    val currentPoint = points.lastOrNull() ?: points.firstOrNull()

                    var shouldMergeWithPrevious = false
                    if (prevRoute != null) {
                        val prevIsStop = prevRoute.name.startsWith("Parada") || prevRoute.activityMode == "STOPPED" || prevRoute.distanceMeters < 25.0
                        if (prevIsStop) {
                            val prevPoint = routeDao.getLastPointForRoute(prevRoute.id)
                            if (currentPoint != null && prevPoint != null) {
                                val results = FloatArray(1)
                                Location.distanceBetween(
                                    currentPoint.latitude, currentPoint.longitude,
                                    prevPoint.latitude, prevPoint.longitude,
                                    results
                                )
                                if (results[0] < 40f) {
                                    shouldMergeWithPrevious = true
                                }
                            } else {
                                shouldMergeWithPrevious = true
                            }
                        }
                    }

                    if (shouldMergeWithPrevious && prevRoute != null) {
                        // Consolidar: extender el tiempo de parada de la ruta anterior
                        val prevTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(prevRoute.startTime))
                        val consolidatedName = if (prevRoute.name.startsWith("Parada")) prevRoute.name else "Parada $prevTimeFormat"
                        routeDao.updateRoute(
                            prevRoute.copy(
                                name = consolidatedName,
                                endTime = now,
                                activityMode = "STOPPED",
                                distanceMeters = 0.0,
                                maxSpeed = 0f,
                                avgSpeed = 0f
                            )
                        )
                        // Purgar la ruta duplicada actual
                        routeDao.deleteRouteById(routeId)
                        routeDao.deleteLocationPointsForRoute(routeId)
                        Log.i("RouteRepository", "Parada en el mismo lugar consolidada con ruta previa ${prevRoute.id}. Duplicado $routeId descartado.")
                        return@withContext true
                    } else {
                        // Es una parada nueva (primer registro sin movimiento en esta ubicación)
                        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(route.startTime))
                        val stopName = "Parada $timeFormat"
                        routeDao.updateRoute(
                            route.copy(
                                name = stopName,
                                isActive = false,
                                endTime = now,
                                distanceMeters = 0.0,
                                maxSpeed = 0f,
                                avgSpeed = 0f,
                                activityMode = "STOPPED"
                            )
                        )
                        return@withContext true
                    }
                }

                val maxSpeed = if (points.isNotEmpty()) points.maxOf { it.speed } else 0f
                val avgSpeed = if (points.isNotEmpty()) points.map { it.speed }.average().toFloat() else 0f

                routeDao.updateRoute(
                    route.copy(
                        isActive = false,
                        endTime = now,
                        distanceMeters = distanceMeters,
                        maxSpeed = maxSpeed,
                        avgSpeed = avgSpeed,
                        activityMode = activityMode
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
            color = color
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

}
