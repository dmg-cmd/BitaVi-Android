package com.aistudio.localizador.location.delegate

import android.util.Log
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.repository.RouteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegado de persistencia del servicio de ubicacion.
 *
 * La base de datos Room esta cifrada con SQLCipher y requiere la clave derivada del
 * almacen de claves, por lo que el repositorio real se inyecta con [attach] desde el
 * servicio (que es quien inicializa la base con la passphrase). Hasta entonces opera
 * en modo seguro (no-op) para no romper el arranque.
 */
@Singleton
class LocationPersistenceDelegate @Inject constructor() {

    companion object {
        private const val TAG = "LocationPersistenceDelegate"
    }

    @Volatile
    private var repository: RouteRepository? = null

    /** Asocia el repositorio ya inicializado con la passphrase de SQLCipher. */
    fun attach(repository: RouteRepository) {
        this.repository = repository
    }

    private fun repo(): RouteRepository? {
        val r = repository
        if (r == null) Log.w(TAG, "Persistencia usada antes de attach(); operacion ignorada.")
        return r
    }

    suspend fun getRouteById(routeId: Int): Route? = repo()?.getRouteById(routeId)

    suspend fun getPreviousPoints(routeId: Int): List<LocationPoint> =
        repo()?.getLocationPointsSync(routeId) ?: emptyList()

    suspend fun updateRoute(route: Route) {
        repo()?.updateRoute(route)
    }

    suspend fun addLocationPoint(
        routeId: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        speed: Float
    ) {
        repo()?.addLocationPoint(routeId, latitude, longitude, altitude, speed)
    }

    suspend fun addLocationPoints(points: List<LocationPoint>) {
        repo()?.addLocationPoints(points)
    }

    suspend fun startNewRoute(name: String): Int = repo()?.startNewRoute(name) ?: -1

    suspend fun stopActiveRoute(routeId: Int, distanceMeters: Double, activityMode: String) {
        repo()?.stopActiveRoute(routeId, distanceMeters, activityMode)
    }

    suspend fun allSavedLocationsOnce(): List<SavedLocation> =
        repo()?.allSavedLocations?.first() ?: emptyList()
}