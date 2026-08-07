package com.aistudio.localizador.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteColorUseCase @Inject constructor() {

    enum class RoutePalette(val hex: String, val displayName: String) {
        AZUL_BITARU("#3498DB", "Azul Primario"),
        VERDE_GPS("#2ECC71", "Verde Activo"),
        AMBER_EXPORT("#FFB74D", "Ambar Historico"),
        VIOLETA_EXPERIENCIA("#7C4DFF", "Violeta Experiencia"),
        ROJO_ALERTA("#E74C3C", "Rojo Destacado");

        companion object {
            fun fromHex(hex: String?): RoutePalette {
                return values().find { it.hex.equals(hex, ignoreCase = true) } ?: AZUL_BITARU
            }
        }
    }

    fun sanitizeColorHex(rawHex: String?): String {
        if (rawHex.isNullOrBlank()) return RoutePalette.AZUL_BITARU.hex
        val formatted = if (rawHex.startsWith("#")) rawHex else "#$rawHex"
        val regex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
        return if (regex.matches(formatted)) formatted else RoutePalette.AZUL_BITARU.hex
    }

    fun parseHexToColorInt(hexColor: String?): Int {
        val safeHex = sanitizeColorHex(hexColor)
        val cleanHex = safeHex.removePrefix("#")
        return try {
            val longVal = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                (0xFF000000 or longVal).toInt()
            } else {
                longVal.toInt()
            }
        } catch (e: Exception) {
            0xFF3498DB.toInt()
        }
    }
}
