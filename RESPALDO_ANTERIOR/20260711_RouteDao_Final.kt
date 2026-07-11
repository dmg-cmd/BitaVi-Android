package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.RouteFavoriteCrossRef
import com.example.data.model.SavedLocation
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

    @Query("SELECT * FROM location_points WHERE route_id = :routeId ORDER BY timestamp ASC")
    fun getLocationPointsForRouteFlow(routeId: Int): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE route_id = :routeId ORDER BY timestamp ASC")
    suspend fun getLocationPointsForRoute(routeId: Int): List<LocationPoint>


    // --- Operaciones de Puntos de Interés (Waypoints) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: Waypoint): Long

    @Query("UPDATE waypoints SET photoPath = NULL WHERE id = :waypointId")
    suspend fun clearWaypointPhoto(waypointId: Int)

    @Query("UPDATE saved_locations SET photoPath = NULL WHERE id = :favoriteId")
    suspend fun clearFavoritePhoto(favoriteId: Int)

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun getWaypointsForRouteFlow(routeId: Int): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY timestamp ASC")
    suspend fun getWaypointsForRoute(routeId: Int): List<Waypoint>

    // --- Operaciones de Lugares Guardados (Favoritos) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLocation(location: com.example.data.model.SavedLocation): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(location: com.example.data.model.SavedLocation): Long

    @Query("UPDATE saved_locations SET photoPath = :photoPath WHERE latitude = :lat AND longitude = :lon")
    suspend fun updatePhotoPathByLatLon(lat: Double, lon: Double, photoPath: String?)

    @Query("""
        DELETE FROM saved_locations
        WHERE id NOT IN (
            SELECT MIN(id) FROM saved_locations GROUP BY latitude, longitude, name
        )
    """)
    suspend fun deleteDuplicateFavorites()

    @Query("""
        DELETE FROM route_favorite_cross_ref
        WHERE favoriteId NOT IN (SELECT id FROM saved_locations)
    """)
    suspend fun cleanOrphanedRelations()

    @Update
    suspend fun updateSavedLocation(location: com.example.data.model.SavedLocation)

    @Query("UPDATE saved_locations SET color = :color WHERE id = :id")
    suspend fun updateSavedLocationColor(id: Int, color: String)

    @Query("SELECT * FROM saved_locations ORDER BY timestamp DESC")
    fun getAllSavedLocationsFlow(): Flow<List<com.example.data.model.SavedLocation>>

    @Query("SELECT * FROM saved_locations")
    suspend fun getAllSavedLocations(): List<com.example.data.model.SavedLocation>


    @Query("SELECT * FROM saved_locations WHERE id = :id")
    fun getSavedLocationByIdFlow(id: Int): Flow<com.example.data.model.SavedLocation?>
    @Delete
    suspend fun deleteSavedLocation(location: com.example.data.model.SavedLocation)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAllSavedLocations()


    // --- Operaciones de relación muchos-a-muchos (Ruta ↔ Favorito) ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRouteFavoriteCrossRef(crossRef: RouteFavoriteCrossRef)

    @Delete
    suspend fun removeRouteFavoriteCrossRef(crossRef: RouteFavoriteCrossRef)

    @Query("""
        SELECT sl.* FROM saved_locations sl
        INNER JOIN route_favorite_cross_ref rf ON sl.id = rf.favoriteId
        WHERE rf.routeId = :routeId
    """)
    fun getFavoritesForRouteFlow(routeId: Int): Flow<List<SavedLocation>>

    @Query("""
        SELECT sl.* FROM saved_locations sl
        INNER JOIN route_favorite_cross_ref rf ON sl.id = rf.favoriteId
        WHERE rf.routeId = :routeId
    """)
    suspend fun getFavoritesForRoute(routeId: Int): List<SavedLocation>

    @Query("""
        SELECT r.* FROM routes r
        INNER JOIN route_favorite_cross_ref rf ON r.id = rf.routeId
        WHERE rf.favoriteId = :favoriteId
    """)
    fun getRoutesForFavoriteFlow(favoriteId: Int): Flow<List<Route>>

    @Query("""
        SELECT r.* FROM routes r
        INNER JOIN route_favorite_cross_ref rf ON r.id = rf.routeId
        WHERE rf.favoriteId = :favoriteId
    """)
    suspend fun getRoutesForFavorite(favoriteId: Int): List<Route>
}
