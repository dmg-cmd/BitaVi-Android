package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.api.RetrofitClient
import com.aistudio.localizador.data.repository.RouteRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class ApiKeyExpiredException : Exception("API Key ha expirado o no tiene fondos")

@Singleton
class AnalyzeRouteAiUseCase @Inject constructor(
    private val repository: RouteRepository
) {
    private val groqApi = RetrofitClient.groqApi

    suspend operator fun invoke(apiKey: String, routeId: Int): Result<String> {
        return try {
            val route = repository.getRouteById(routeId)
                ?: return Result.failure(IllegalStateException("Ruta no encontrada"))
            val waypoints = repository.getWaypointsSync(routeId)
            val durationMs = if (route.endTime > route.startTime) route.endTime - route.startTime else 0L

            val durationFormatted = String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(durationMs),
                TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60,
                TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
            )

            val distanceKm = "%.2f".format(route.distanceMeters / 1000.0)
            val maxSpeedKmh = "%.1f".format(route.maxSpeed * 3.6f)
            val avgSpeedKmh = "%.1f".format(route.avgSpeed * 3.6f)

            val promptText = buildString {
                appendLine("Actua como un Ingeniero de Telemetria GPS. Analiza tecnicamente estos datos y genera un informe tecnico BREVE:")
                appendLine("- Ruta: ${route.name}")
                appendLine("- Distancia Real: $distanceKm km")
                appendLine("- Tiempo en Movimiento: $durationFormatted")
                appendLine("- Dinamica de Velocidad: Max $maxSpeedKmh km/h, Media $avgSpeedKmh km/h")
                appendLine("- Puntos Tecnicos: ${waypoints.size} marcados")
                appendLine()
                appendLine("REGLAS CRITICAS:")
                appendLine("1. NO uses lenguaje motivador ni frases de aliento.")
                appendLine("2. Centrate en la regularidad del ritmo y el perfil del terreno.")
                appendLine("3. Identifica anomalias en la velocidad o el desplazamiento.")
                appendLine("4. Maximo 2 parrafos tecnicos.")
            }

            val request = com.aistudio.localizador.data.api.GroqRequest(
                messages = listOf(
                    com.aistudio.localizador.data.api.GroqMessage(
                        "system",
                        "Eres un sistema de analisis tecnico de telemetria y geomatica. Tu tono es seco, profesional y basado estrictamente en datos numericos y geograficos."
                    ),
                    com.aistudio.localizador.data.api.GroqMessage("user", promptText)
                )
            )

            val response = groqApi.generateContent("Bearer " + apiKey.trim(), request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: "Respuesta vacia."
                Result.success(content)
            } else if (response.code() == 401) {
                Result.failure(ApiKeyExpiredException())
            } else {
                Result.failure(Exception("Error API: " + response.code()))
            }
        } catch (e: ApiKeyExpiredException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}