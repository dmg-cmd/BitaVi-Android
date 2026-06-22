package com.example.data.repository

import com.example.data.db.RouteDao
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.Waypoint
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que unifica el acceso a datos para las rutas, puntos GPS y waypoints.
 * Implementa el patrón Repository para desacoplar el ViewModel y el Servicio del DAO de Room.
 */
class RouteRepository(private val routeDao: RouteDao) {

    // Flujo de todas las rutas guardadas u ordenadas de más reciente a más antigua
    val allRoutes: Flow<List<Route>> = routeDao.getAllRoutesFlow()

    // Flujo de la ruta activa actualmente
    val activeRouteFlow: Flow<Route?> = routeDao.getActiveRouteFlow()

    /**
     * Inicia una nueva ruta guardando su registro inicial y finalizando de forma preventiva
     * cualquier otra ruta que hubiera quedado en estado activo.
     */
    suspend fun startNewRoute(name: String): Int {
        // Desactivar cualquier ruta activa previamente para evitar estados inconsistentes
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

    suspend fun getActiveRouteSync(): Route? {
        return routeDao.getActiveRoute()
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

    /**
     * Finaliza una ruta activa de forma controlada actualizando estadísticas finales.
     */
    suspend fun stopActiveRoute(routeId: Int, distanceMeters: Double) {
        val route = routeDao.getRouteById(routeId)
        val points = routeDao.getLocationPointsForRoute(routeId)
        
        if (route != null) {
            val maxSpeed = if (points.isNotEmpty()) points.maxOf { it.speed } else 0f
            val avgSpeed = if (points.isNotEmpty()) points.map { it.speed }.average().toFloat() else 0f
            
            routeDao.updateRoute(
                route.copy(
                    isActive = false,
                    endTime = System.currentTimeMillis(),
                    distanceMeters = distanceMeters,
                    maxSpeed = maxSpeed,
                    avgSpeed = avgSpeed
                )
            )
        }
    }

    suspend fun deleteRoute(routeId: Int) {
        routeDao.deleteRouteById(routeId)
    }

    suspend fun deleteRoutes(routeIds: List<Int>) {
        routeDao.deleteRoutes(routeIds)
    }

    /**
     * Guarda una coordenada de rastreo vinculada a una ruta.
     */
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

    /**
     * Guarda un Punto de Interés (Waypoint) en el mapa vinculado a una ruta.
     */
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

    // --- Operaciones de Lugares Guardados (Favoritos) ---

    val allSavedLocations: Flow<List<com.example.data.model.SavedLocation>> = routeDao.getAllSavedLocationsFlow()

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
        color: String = "#FFD700"
    ) {
        val location = com.example.data.model.SavedLocation(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
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
        routeDao.insertSavedLocation(location)
    }

    suspend fun updateSavedLocation(location: com.example.data.model.SavedLocation) {
        routeDao.updateSavedLocation(location)
    }

    suspend fun deleteSavedLocation(location: com.example.data.model.SavedLocation) {
        routeDao.deleteSavedLocation(location)
    }

    suspend fun deleteAllData() {
        routeDao.deleteAllRoutes()
        routeDao.deleteAllSavedLocations()
    }
}
