package com.example.location.manager

import android.location.Location
import java.util.LinkedList

/**
 * Filtro Cinemático de Inercia y Autostop v3.5
 *
 * Gestiona una ventana deslizante de ubicaciones para descartar saltos aberrantes
 * de antena y congelar la posición en detenciones para erradicar la distancia fantasma.
 */
class CinematicFilterManager(
    private val maxAllowedSpeedMps: Double = 15.0,
    private val autoStopSpeedMps: Double = 0.3,
    private val windowSize: Int = 5
) {

    companion object {
        const val SUSTAINED_STOP_TICKS = 3     // Ventana de tolerancia temporal
    }

    private var lastValidLocation: Location? = null
    private var lowSpeedCounter = 0

    /**
     * Procesa la ubicación aplicando el umbral de supervivencia cinemática adaptativo.
     */
    fun filterAndProcess(currentLocation: Location): Location? {
        if (currentLocation.latitude == 0.0 && currentLocation.longitude == 0.0) return null

        val last = lastValidLocation ?: run {
            lastValidLocation = Location(currentLocation)
            return currentLocation
        }

        // Detección de salto de antena (velocidad excesiva)
        val dist = last.distanceTo(currentLocation)
        val timeDeltaSeconds = (currentLocation.time - last.time) / 1000.0f
        if (timeDeltaSeconds > 0f) {
            val calculatedSpeed = dist / timeDeltaSeconds
            if (calculatedSpeed > maxAllowedSpeedMps) {
                if (last.speed == 0f && currentLocation.accuracy > 30f) {
                    return null // Descartar salto de antena
                }
            }
        }

        // Resiliencia de cómputo: cálculo manual si el hardware no reporta velocidad
        val speed = if (currentLocation.hasSpeed()) {
            currentLocation.speed
        } else {
            calculateFallbackSpeed(currentLocation, last)
        }

        if (speed < autoStopSpeedMps) {
            lowSpeedCounter++
            if (lowSpeedCounter >= SUSTAINED_STOP_TICKS) {
                // Fricción Estática Virtual Confirmada
                val staticLocation = Location(last).apply {
                    time = currentLocation.time // Preservar cronología
                    if (currentLocation.hasAccuracy()) {
                        accuracy = currentLocation.accuracy
                    }
                    this.speed = 0.0f
                }
                lastValidLocation = staticLocation
                return staticLocation
            }
        } else {
            lowSpeedCounter = 0 // Movimiento legítimo detectado, reseteo inmediato
        }

        lastValidLocation = Location(currentLocation)
        return currentLocation
    }

    private fun calculateFallbackSpeed(current: Location, last: Location): Float {
        val timeDeltaSeconds = (current.time - last.time) / 1000.0f
        if (timeDeltaSeconds <= 0f) return 0f
        
        val distance = current.distanceTo(last)
        return distance / timeDeltaSeconds
    }

    fun reset() {
        lastValidLocation = null
        lowSpeedCounter = 0
    }
}