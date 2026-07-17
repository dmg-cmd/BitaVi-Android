package com.example.ui

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.util.AppConstants
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
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                Triple(formatAddress(addr, coordFallback), addr.countryName ?: "Desconocido", addr.locality ?: addr.adminArea ?: "Desconocida")
            } else Triple(coordFallback, "Desconocido", "Desconocida")
        } catch (_: Exception) { Triple(coordFallback, "Desconocido", "Desconocida") }
    }

    fun fetchAddress(lat: Double, lon: Double): String? {
        if (!isValidCoordinate(lat, lon)) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) formatAddress(addresses[0], "Desconocida") else null
        } catch (_: Exception) { null }
    }

}
