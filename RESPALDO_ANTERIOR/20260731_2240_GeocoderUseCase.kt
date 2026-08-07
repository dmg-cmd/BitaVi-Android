package com.aistudio.localizador.ui

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.aistudio.localizador.data.api.RetrofitClient
import com.aistudio.localizador.util.AppConstants
import java.util.Locale

class GeocoderUseCase(private val context: Context) {

    fun formatAddress(addr: Address, fallback: String): String {
        val calle = addr.thoroughfare
        val altura = addr.subThoroughfare
        if (!calle.isNullOrBlank() && !altura.isNullOrBlank()) return "${calle} ${altura}"
        if (!calle.isNullOrBlank()) {
            val feature = addr.featureName
            if (!feature.isNullOrBlank() && feature != calle) {
                val possibleNumber = feature.removePrefix(calle).trim()
                if (possibleNumber.isNotBlank() && possibleNumber.first().isDigit()) return "${calle} ${possibleNumber}"
                if (feature.all { it.isDigit() }) return "${calle} ${feature}"
            }
            return calle
        }
        val addressLine = if (addr.maxAddressLineIndex >= 0) addr.getAddressLine(0) else null
        if (!addressLine.isNullOrBlank()) return addressLine
        val feature = addr.featureName
        if (!feature.isNullOrBlank()) return feature
        return fallback
    }

    fun isValidCoordinate(lat: Double, lon: Double): Boolean {
        val clampedLat = lat.toFloat().toDouble()
        val clampedLon = lon.toFloat().toDouble()
        return AppConstants.Geofencing.isWithinBounds(clampedLat, clampedLon)
    }

    fun fetchLocationDetails(lat: Double, lon: Double): Triple<String, String, String> {
        if (!isValidCoordinate(lat, lon)) {
            return Triple("GPS: Fuera de cobertura", "Desconocido", "Desconocida")
        }
        val coordFallback = "GPS: ${String.format(Locale.US, "%.5f, %.5f", lat, lon)}"

        // Intento A: Geocoder NATIVO del Sistema Operativo
        val native = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    val country = addr.countryName?.takeIf { it.isNotBlank() }
                    val city = (addr.locality ?: addr.subAdminArea ?: addr.adminArea)?.takeIf { it.isNotBlank() }
                    if (country != null && city != null) {
                        Triple(formatAddress(addr, coordFallback), country, city)
                    } else null
                } else null
            } else null
        }.getOrNull()

        if (native != null) return native

        // Intento B: FALLBACK REST NOMINATIM (OpenStreetMap) ante fallo o vacío del sistema
        val network = runCatching {
            val response = RetrofitClient.nominatimApi.reverseGeocode(lat, lon).execute()
            if (response.isSuccessful) {
                val body = response.body()
                val country = body?.address?.country?.takeIf { it.isNotBlank() } ?: "Desconocido"
                val city = body?.address?.getBestCityName()?.takeIf { it.isNotBlank() } ?: "Desconocida"
                val fullAddress = body?.displayName?.takeIf { it.isNotBlank() } ?: coordFallback
                Triple(fullAddress, country, city)
            } else null
        }.getOrNull()

        return network ?: Triple(coordFallback, "Desconocido", "Desconocida")
    }

    fun fetchAddress(lat: Double, lon: Double): String? {
        if (!isValidCoordinate(lat, lon)) return null

        val native = runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) formatAddress(addresses[0], "Desconocida") else null
        }.getOrNull()
        if (native != null) return native

        return runCatching {
            val response = RetrofitClient.nominatimApi.reverseGeocode(lat, lon).execute()
            if (response.isSuccessful) {
                response.body()?.displayName?.takeIf { it.isNotBlank() }
            } else null
        }.getOrNull()
    }
}