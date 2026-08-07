package com.aistudio.localizador.domain.usecase

import android.location.Location
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.ui.GeocoderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddFavoriteFromTrackingUseCase @Inject constructor(
    private val repository: RouteRepository,
    private val geocoderUseCase: GeocoderUseCase
) {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    suspend fun execute(
        currentLocation: Location?,
        activeRouteId: Int?,
        customName: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (currentLocation == null || (currentLocation.latitude == 0.0 && currentLocation.longitude == 0.0)) {
                Log.w("Bitaru_Tracking", "Ubicacion nula o coordenadas GPS invalidas (0.0, 0.0).")
                return@withContext Result.failure(IllegalArgumentException("Coordenadas GPS no disponibles."))
            }

            val lat = currentLocation.latitude
            val lon = currentLocation.longitude
            val timestamp = System.currentTimeMillis()
            val timeStr = timeFormatter.format(Date(timestamp))

            val resolvedAddress = geocoderUseCase.fetchAddress(lat, lon)
                ?: "Ubicacion en ruta ($timeStr)"

            val pointName = if (customName.isNullOrBlank()) {
                "Punto en Ruta - $timeStr"
            } else {
                customName
            }

            val comment = "Creado automaticamente durante la grabacion del recorrido."

            val newFavoriteId = repository.addSavedLocation(
                name = pointName,
                address = resolvedAddress,
                latitude = lat,
                longitude = lon,
                comment = comment,
                author = "Tracking Automatico",
                photoPath = null,
                phoneNumber = "",
                email = "",
                isContact = false,
                color = "#FFD700",
                timestamp = timestamp
            )

            if (activeRouteId != null && activeRouteId > 0 && newFavoriteId > 0L) {
                repository.addFavoriteToRoute(
                    routeId = activeRouteId,
                    favoriteId = newFavoriteId.toInt()
                )
                Log.d("Bitaru_Tracking", "Favorito $newFavoriteId vinculado a Ruta $activeRouteId")
            }

            Log.d("Bitaru_Tracking", "Favorito guardado: $pointName ($lat, $lon)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Bitaru_Tracking", "Error al crear favorito en tracking", e)
            Result.failure(e)
        }
    }
}
