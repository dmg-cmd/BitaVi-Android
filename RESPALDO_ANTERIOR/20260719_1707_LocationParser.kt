package com.aistudio.localizador.ui

import android.location.Geocoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object LocationParser {

    private val geoCache = ConcurrentHashMap<String, Pair<String, String>>()

    fun inferCountryCity(
        address: String,
        lat: Double,
        lon: Double,
        geocoder: Geocoder
    ): Pair<String, String> {
        val parsed = parseFromAddressString(address)
        if (parsed.first != "Desconocido") return parsed

        val cacheKey = String.format(Locale.US, "%.3f,%.3f", lat, lon)
        geoCache[cacheKey]?.let { return it }

        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val country = addr.countryName ?: "Desconocido"
                val city = addr.locality ?: addr.adminArea ?: "Desconocida"
                Pair(country, city).also { geoCache[cacheKey] = it }
            } else {
                Pair("Desconocido", "Desconocida")
            }
        } catch (_: Exception) {
            Pair("Desconocido", "Desconocida")
        }
    }

    fun parseFromAddressString(address: String): Pair<String, String> {
        if (address.isBlank() || address.startsWith("GPS:")) {
            return Pair("Desconocido", "Desconocida")
        }
        val parts = address.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val last = parts.last()
            val secondLast = parts[parts.size - 2]
            if (last.any { it.isLetter() } && secondLast.any { it.isLetter() }) {
                return Pair(last, secondLast)
            }
        }
        return Pair("Desconocido", "Desconocida")
    }

    fun clearCache() {
        geoCache.clear()
    }
}
