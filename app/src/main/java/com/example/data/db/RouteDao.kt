package com.example.data.db

import androidx.room.*
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.Waypoint
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    // --- Operaciones de Rutas ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route): Long

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRouteById(routeId: Int)

    @Transaction
    @Query("DELETE FROM routes WHERE id IN (:ids)")
    suspend fun deleteRoutes(ids: List<Int>)

    @Query("SELECT * FROM routes ORDER BY startTime DESC")
    fun getAllRoutesFlow(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :routeId LIMIT 1")
    suspend fun getRouteById(routeId: Int): Route?

    @Query("SELECT * FROM routes WHERE id = :routeId LIMIT 1")
    fun getRouteByIdFlow(routeId: Int): Flow<Route?>

    @Query("SELECT * FROM routes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveRoute(): Route?

    @Query("SELECT * FROM routes WHERE isActive = 1 LIMIT 1")
    fun getActiveRouteFlow(): Flow<Route?>


    // --- Operaciones de Puntos de Ubicación ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(point: LocationPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoints(points: List<LocationPoint>)

    @Query("SELECT * FROM location_points WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun getLocationPointsForRouteFlow(routeId: Int): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE routeId = :routeId ORDER BY timestamp ASC")
    suspend fun getLocationPointsForRoute(routeId: Int): List<LocationPoint>


    // --- Operaciones de Puntos de Interés (Waypoints) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: Waypoint): Long

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun getWaypointsForRouteFlow(routeId: Int): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY timestamp ASC")
    suspend fun getWaypointsForRoute(routeId: Int): List<Waypoint>

    // --- Operaciones de Lugares Guardados (Favoritos) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLocation(location: com.example.data.model.SavedLocation): Long

    @Update
    suspend fun updateSavedLocation(location: com.example.data.model.SavedLocation)

    @Query("SELECT * FROM saved_locations ORDER BY timestamp DESC")
    fun getAllSavedLocationsFlow(): Flow<List<com.example.data.model.SavedLocation>>

    @Delete
    suspend fun deleteSavedLocation(location: com.example.data.model.SavedLocation)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAllSavedLocations()
}
