package com.example.ui

import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.RetrofitClient
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiAiUseCase {

    suspend fun analyzeRoute(
        apiKey: String,
        routeName: String,
        distanceMeters: Double,
        maxSpeed: Float,
        avgSpeed: Float,
        durationMs: Long,
        waypointCount: Int
    ): String {
        val durationFormatted = String.format(Locale.US, "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(durationMs),
            TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60,
            TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        )
        val promptText = """
            Actúa como un Ingeniero de Telemetría GPS. Analiza técnicamente estos datos y genera un informe técnico BREVE:
            - Ruta: $routeName
            - Distancia Real: ${String.format(Locale.US, "%.2f", distanceMeters / 1000.0)} km
            - Tiempo en Movimiento: $durationFormatted
            - Dinámica de Velocidad: Máx ${String.format(Locale.US, "%.1f", maxSpeed * 3.6f)} km/h, Media ${String.format(Locale.US, "%.1f", avgSpeed * 3.6f)} km/h
            - Puntos Técnicos: $waypointCount marcados (Analiza su distribución)
            
            REGLAS CRÍTICAS:
            1. NO uses lenguaje motivador ni frases de aliento.
            2. Céntrate en la regularidad del ritmo y el perfil del terreno.
            3. Identifica anomalías en la velocidad o el desplazamiento.
            4. Máximo 2 párrafos técnicos.
        """.trimIndent()

        val api = RetrofitClient.geminiApi
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "Eres un sistema de análisis técnico de telemetría y geomática. Tu tono es seco, profesional y basado estrictamente en datos numéricos y geográficos."),
                        GeminiPart(text = promptText)
                    )
                )
            )
        )
        
        return try {
            val response = api.generateContent(apiKey.trim(), request)
            if (response.isSuccessful) {
                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Respuesta vacía."
            } else {
                "Error API Gemini: ${response.code()}"
            }
        } catch (e: Exception) {
            "Error de conexión Gemini: ${e.message}"
        }
    }

    suspend fun analyzeLocation(
        apiKey: String,
        name: String,
        lat: Double,
        lon: Double,
        comment: String
    ): String {
        val promptText = """
            Analiza esta ubicación estratégica y genera una ficha técnica BREVE:
            - Nombre: $name
            - Coordenadas: $lat, $lon
            - Comentario: $comment
            
            Proporciona una descripción técnica del lugar, posibles usos tácticos o logísticos basados en su ubicación geográfica y cualquier observación relevante. 
            Sé objetivo y profesional. Máximo 1 párrafo.
        """.trimIndent()

        val api = RetrofitClient.geminiApi
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "Eres un analista de inteligencia geoespacial."),
                        GeminiPart(text = promptText)
                    )
                )
            )
        )

        return try {
            val response = api.generateContent(apiKey.trim(), request)
            if (response.isSuccessful) {
                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Respuesta vacía."
            } else {
                "Error API Gemini: ${response.code()}"
            }
        } catch (e: Exception) {
            "Error de conexión Gemini: ${e.message}"
        }
    }
}
