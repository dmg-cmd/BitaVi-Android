package com.aistudio.localizador.ui

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationParser @Inject constructor() {

    data class ParsedLocation(val country: String, val city: String)

    fun inferCountryCity(rawLocation: String?): Result<ParsedLocation> {
        if (rawLocation.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Cadena nula o vacía"))
        }
        return try {
            val parts = rawLocation.split(",").map { it.trim() }
            if (parts.size >= 2) {
                Result.success(ParsedLocation(country = parts[1], city = parts[0]))
            } else {
                Result.success(ParsedLocation(country = parts[0], city = "Desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @JvmStatic
        fun parseFromAddressString(address: String): Pair<String, String> {
            val parts = address.split(",").map { it.trim() }
            return if (parts.size >= 2) {
                Pair(parts[0], parts[1])
            } else {
                Pair(address, "Desconocido")
            }
        }
    }
}
