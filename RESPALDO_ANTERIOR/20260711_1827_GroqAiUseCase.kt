package com.example.ui

import com.example.data.api.GroqMessage
import com.example.data.api.GroqRequest
import com.example.data.api.RetrofitClient
import java.util.concurrent.TimeUnit

class ApiKeyExpiredException : Exception("API Key ha expirado o no tiene fondos")

class GroqAiUseCase {

    suspend fun analyze(
        apiKey: String,
        routeName: String,
        distanceMeters: Double,
        maxSpeed: Float,
        avgSpeed: Float,
        durationMs: Long,
        waypointCount: Int
    ): String {
        val durationFormatted = String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(durationMs),
            TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60,
            TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        )
        val promptText = """
            Actúa como un Ingeniero de Telemetría GPS. Analiza técnicamente estos datos y genera un informe técnico BREVE:
            - Ruta: $routeName
            - Distancia Real: ${String.format("%.2f", distanceMeters / 1000.0)} km
            - Tiempo en Movimiento: $durationFormatted
            - Dinámica de Velocidad: Máx ${String.format("%.1f", maxSpeed * 3.6f)} km/h, Media ${String.format("%.1f", avgSpeed * 3.6f)} km/h
            - Puntos Técnicos: $waypointCount marcados (Analiza su distribución)
            
            REGLAS CRÍTICAS:
            1. NO uses lenguaje motivador ni frases de aliento.
            2. Céntrate en la regularidad del ritmo y el perfil del terreno.
            3. Identifica anomalías en la velocidad o el desplazamiento.
            4. Máximo 2 párrafos técnicos.
        """.trimIndent()

        val api = RetrofitClient.groqApi
        val request = GroqRequest(
            messages = listOf(
                GroqMessage("system", "Eres un sistema de análisis técnico de telemetría y geomática. Tu tono es seco, profesional y basado estrictamente en datos numéricos y geográficos."),
                GroqMessage("user", promptText)
            )
        )
        val response = api.generateContent("Bearer ${apiKey.trim()}", request)
        if (response.isSuccessful) {
            return response.body()?.choices?.firstOrNull()?.message?.content ?: "Respuesta vacía."
        } else if (response.code() == 401) {
            throw ApiKeyExpiredException()
        } else {
            return "Error API: ${response.code()}"
        }
    }
}
