package com.aistudio.localizador.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteColorUseCase @Inject constructor() {

    enum class RoutePalette(val hex: String, val displayName: String) {
        AZUL_BITAVI("#3498DB", "Azul Primario"),
        VERDE_GPS("#2ECC71", "Verde Activo"),
        AMBER_EXPORT("#FFB74D", "Ambar Historico"),
        VIOLETA_EXPERIENCIA("#7C4DFF", "Violeta Experiencia"),
        ROJO_ALERTA("#E74C3C", "Rojo Destacado");

        companion object {
            fun fromHex(hex: String?): RoutePalette {
                return values().find { it.hex.equals(hex, ignoreCase = true) } ?: AZUL_BITAVI
            }
        }
    }

    companion object {
        /** Rojo intenso (< 2 km/h: Inicio / Detenido / Paso muy lento) */
        const val COLOR_STOPPED_SLOW: Int = 0xFFD32F2F.toInt()
        /** Verde esmeralda oscuro (2 – 7 km/h: Caminata) */
        const val COLOR_WALKING: Int = 0xFF2E7D32.toInt()
        /** Azul profundo (7 – 20 km/h: Bicicleta / Urbano) */
        const val COLOR_BIKING: Int = 0xFF1565C0.toInt()
        /** Violeta intenso (> 20 km/h: Vehículo) */
        const val COLOR_DRIVING: Int = 0xFF7B1FA2.toInt()
    }

    /**
     * Convierte la velocidad instantánea (en m/s) en un color táctico de alto contraste
     * sobre mapas claros y oscuros, usando rangos fijos en km/h:
     * - < 2 km/h   -> Rojo intenso (#D32F2F)
     * - 2 - 7 km/h -> Verde esmeralda oscuro (#2E7D32)
     * - 7 - 20 km/h -> Azul profundo (#1565C0)
     * - > 20 km/h  -> Violeta intenso (#7B1FA2)
     */
    fun speedToTacticalColor(speedMps: Float): Int {
        val speedKmh = (speedMps * 3.6f).coerceAtLeast(0f)
        return when {
            speedKmh < 2.0f -> COLOR_STOPPED_SLOW
            speedKmh <= 7.0f -> COLOR_WALKING
            speedKmh <= 20.0f -> COLOR_BIKING
            else -> COLOR_DRIVING
        }
    }

    /**
     * Convierte velocidad instantánea en un Color Int (ARGB) usando gradiente térmico HSV continuo.
     * Azul/Cian (200°) para velocidad baja → Rojo (0°) para velocidad máxima.
     * Implementación pura Kotlin sin dependencias Android framework.
     */
    fun speedToGradientColor(
        speedMps: Float,
        minSpeedMps: Float = 0.0f,
        maxSpeedMps: Float = 15.0f
    ): Int {
        val delta = maxSpeedMps - minSpeedMps

        val normalized = if (delta < 0.001f) {
            0.0f
        } else {
            ((speedMps - minSpeedMps) / delta).coerceIn(0.0f, 1.0f)
        }

        // Mapeo continuo HSV: 200° (Azul/Cian) → 0° (Rojo)
        val hueDeg = (1.0f - normalized) * 200.0f
        return hsvToArgb(hueDeg, 0.85f, 0.95f)
    }

    /**
     * Convierte HSV a ARGB Int sin dependencias Android.
     * Algoritmo estándar HSV → RGB.
     */
    private fun hsvToArgb(hueDeg: Float, saturation: Float, value: Float): Int {
        val h = hueDeg / 60.0f
        val s = saturation.coerceIn(0.0f, 1.0f)
        val v = value.coerceIn(0.0f, 1.0f)

        val hi = h.toInt() % 6
        val f = h - hi
        val p = (v * (1.0f - s) * 255.0f).toInt().coerceIn(0, 255)
        val q = (v * (1.0f - s * f) * 255.0f).toInt().coerceIn(0, 255)
        val t = (v * (1.0f - s * (1.0f - f)) * 255.0f).toInt().coerceIn(0, 255)
        val vi = (v * 255.0f).toInt().coerceIn(0, 255)

        val (r, g, b) = when (hi) {
            0 -> Triple(vi, t, p)
            1 -> Triple(q, vi, p)
            2 -> Triple(p, vi, t)
            3 -> Triple(p, q, vi)
            4 -> Triple(t, p, vi)
            else -> Triple(vi, p, q)
        }

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun sanitizeColorHex(rawHex: String?): String {
        if (rawHex.isNullOrBlank()) return RoutePalette.AZUL_BITAVI.hex
        val formatted = if (rawHex.startsWith("#")) rawHex else "#"
        val regex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
        return if (regex.matches(formatted)) formatted else RoutePalette.AZUL_BITAVI.hex
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
