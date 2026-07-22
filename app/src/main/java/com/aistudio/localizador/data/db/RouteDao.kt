package com.aistudio.localizador.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.model.RouteFavoriteCrossRef
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.model.Waypoint
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    // ─── Routes ──────────────────────────────────────────
    @Query("SELECT * FROM routes ORDER BY startTime DESC")
    fun getAllRoutesFlow(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveRoute(): Route?

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: Int): Route?

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getRouteByIdFlow(id: Int): Flow<Route?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route): Long

    @Update
    suspend fun updateRoute(route: Route)

    @Query("UPDATE routes SET color = :color WHERE id = :routeId")
    suspend fun updateRouteColor(routeId: Int, color: String)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRouteById(id: Int)

    @Query("DELETE FROM routes WHERE id IN (:ids)")
    suspend fun deleteRoutes(ids: List<Int>)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()

    // ─── Location Points ─────────────────────────────────
    @Query("SELECT * FROM location_points WHERE route_id = :routeId ORDER BY timestamp ASC")
    fun getLocationPointsForRouteFlow(routeId: Int): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE route_id = :routeId ORDER BY timestamp ASC")
    suspend fun getLocationPointsForRoute(routeId: Int): List<LocationPoint>

    @Insert
    suspend fun insertLocationPoint(point: LocationPoint)

    @Insert
    suspend fun insertLocationPoints(points: List<LocationPoint>)

    @Query("DELETE FROM location_points")
    suspend fun deleteAllLocationPoints()

    @Query("DELETE FROM location_points WHERE route_id = :routeId")
    suspend fun deleteLocationPointsForRoute(routeId: Int)

    // ─── Waypoints ───────────────────────────────────────
    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun getWaypointsForRouteFlow(routeId: Int): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY timestamp ASC")
    suspend fun getWaypointsForRoute(routeId: Int): List<Waypoint>

    @Insert
    suspend fun insertWaypoint(waypoint: Waypoint)

    @Query("DELETE FROM waypoints")
    suspend fun deleteAllWaypoints()

    // ─── Saved Locations (Favorites) ─────────────────────
    @Query("SELECT * FROM saved_locations ORDER BY timestamp DESC")
    fun getAllSavedLocationsFlow(): Flow<List<SavedLocation>>

    @Query("SELECT * FROM saved_locations ORDER BY timestamp DESC")
    suspend fun getAllSavedLocations(): List<SavedLocation>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    fun getSavedLocationByIdFlow(id: Int): Flow<SavedLocation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLocation(location: SavedLocation): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(location: SavedLocation): Long

    @Update
    suspend fun updateSavedLocation(location: SavedLocation)

    @Query("UPDATE saved_locations SET color = :color WHERE id = :id")
    suspend fun updateSavedLocationColor(id: Int, color: String)

    @Query("UPDATE saved_locations SET photoPath = :photoPath WHERE latitude = :lat AND longitude = :lon")
    suspend fun updatePhotoPathByLatLon(lat: Double, lon: Double, photoPath: String?)

    @Delete
    suspend fun deleteSavedLocation(location: SavedLocation)

    @Query("DELETE FROM saved_locations WHERE id IN (SELECT id FROM saved_locations GROUP BY latitude, longitude HAVING COUNT(*) > 1)")
    suspend fun deleteDuplicateFavorites()

    @Query("DELETE FROM route_favorite_cross_ref WHERE routeId NOT IN (SELECT id FROM routes) OR favoriteId NOT IN (SELECT id FROM saved_locations)")
    suspend fun cleanOrphanedRelations()

    @Query("DELETE FROM saved_locations")
    suspend fun deleteAllSavedLocations()

    // ─── Route-Favorite Cross Reference ──────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRouteFavoriteCrossRef(crossRef: RouteFavoriteCrossRef)

    @Delete
    suspend fun removeRouteFavoriteCrossRef(crossRef: RouteFavoriteCrossRef)

    @Query("SELECT s.* FROM saved_locations s INNER JOIN route_favorite_cross_ref r ON s.id = r.favoriteId WHERE r.routeId = :routeId")
    fun getFavoritesForRouteFlow(routeId: Int): Flow<List<SavedLocation>>

    @Query("SELECT s.* FROM saved_locations s INNER JOIN route_favorite_cross_ref r ON s.id = r.favoriteId WHERE r.routeId = :routeId")
    suspend fun getFavoritesForRoute(routeId: Int): List<SavedLocation>
}
