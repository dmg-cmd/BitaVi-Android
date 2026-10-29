package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.ui.GeocoderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class WifiGeocodeFavoritesUseCase(
    private val context: Context,
    private val repository: RouteRepository,
    private val geocoderUseCase: GeocoderUseCase
) {

    companion object {
        private const val TAG = "WifiGeocodeFavs"
        private val STOP_NAME_REGEX = Regex("""^Parada\s+\d{4}.*""")
    }

    fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            isWifi && hasInternet
        } catch (e: Exception) {
            Log.e(TAG, "Error al comprobar estado de red Wi-Fi: ${e.message}")
            false
        }
    }

    suspend fun execute(): Int = withContext(Dispatchers.IO) {
        if (!isWifiConnected()) {
            Log.d(TAG, "Omitiendo resolución de direcciones: no hay conexión Wi-Fi activa con Internet.")
            return@withContext 0
        }

        val allFavorites = repository.getAllSavedLocationsSync()
        val candidates = allFavorites.filter { fav ->
            val isGenericStopName = fav.name.matches(STOP_NAME_REGEX) || fav.name.startsWith("Parada 20")
            val isUnresolvedAddress = fav.address.isBlank() || fav.address.startsWith("GPS:") || fav.address.contains("Desconocid")
            val isAutoStopCategory = fav.iconCategory == "AUTOMATIC" && (fav.name.startsWith("Parada") || isUnresolvedAddress)
            isGenericStopName || isAutoStopCategory
        }

        if (candidates.isEmpty()) {
            Log.d(TAG, "No hay puntos de parada pendientes de geolocalización.")
            return@withContext 0
        }

        Log.i(TAG, "Iniciando geocodificación Wi-Fi de ${candidates.size} puntos de parada.")
        var updatedCount = 0

        for (fav in candidates) {
            if (!isWifiConnected()) {
                Log.w(TAG, "Interrupción de Wi-Fi durante la geocodificación. Deteniendo proceso.")
                break
            }

            try {
                val details = geocoderUseCase.fetchLocationDetails(fav.latitude, fav.longitude)
                val fullAddress = details.first
                val country = details.second
                val city = details.third

                val isResolvedAddress = fullAddress.isNotBlank() && !fullAddress.startsWith("GPS:")
                val isResolvedCity = city.isNotBlank() && city != "Desconocida"
                val isResolvedCountry = country.isNotBlank() && country != "Desconocido"

                val resolvedTitle: String? = when {
                    isResolvedAddress -> {
                        // Título limpio con solo la dirección/calle/ruta (sin el prefijo 'Parada')
                        fullAddress.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: fullAddress
                    }
                    isResolvedCity -> {
                        // Zona fuera de ciudad / rural: colocar la localidad o paraje más cercano
                        city
                    }
                    isResolvedCountry -> {
                        country
                    }
                    else -> null
                }

                if (resolvedTitle != null) {
                    val finalAddress = if (isResolvedAddress) fullAddress else fav.address
                    val finalCountry = if (isResolvedCountry) country else fav.country
                    val finalCity = if (isResolvedCity) city else fav.city

                    val updatedFav = fav.copy(
                        name = resolvedTitle,
                        address = finalAddress,
                        country = finalCountry,
                        city = finalCity
                    )
                    repository.updateSavedLocation(updatedFav)
                    updatedCount++
                    Log.i(TAG, "Punto #${fav.id} actualizado a título: '$resolvedTitle', dirección: '$finalAddress'")
                }

                // Pausa prudente para no saturar los servicios de geocodificación
                delay(300L)
            } catch (e: Exception) {
                Log.e(TAG, "Error geocodificando favorito #${fav.id}: ${e.message}")
            }
        }

        Log.i(TAG, "Geocodificación Wi-Fi completada: $updatedCount paradas actualizadas.")
        return@withContext updatedCount
    }

    fun registerWifiCallback(onWifiAvailable: () -> Unit): ConnectivityManager.NetworkCallback? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onWifiAvailable()
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        onWifiAvailable()
                    }
                }
            }

            connectivityManager.registerNetworkCallback(request, callback)
            callback
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo registrar callback de Wi-Fi: ${e.message}")
            null
        }
    }

    fun unregisterWifiCallback(callback: ConnectivityManager.NetworkCallback?) {
        if (callback == null) return
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desregistrar callback de Wi-Fi: ${e.message}")
        }
    }
}
