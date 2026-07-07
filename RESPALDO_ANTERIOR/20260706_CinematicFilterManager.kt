package com.example.location.manager

import android.location.Location
import java.util.LinkedList

/**
 * Filtro Cinemático de Inercia v3.0
 *
 * Gestiona una ventana deslizante de ubicaciones GPS para descartar
 * saltos de antena aberrantes preservando la continuidad en curvas.
 *
 * Uso típico:
 *   val filter = CinematicFilterManager()
 *   if (filter.filter(location)) {
 *       // punto válido, procesar
 *   }
 */
class CinematicFilterManager(
    private val maxAllowedSpeedMps: Double = 15.0,
    private val windowSize: Int = 5
) {
    private val locationWindow = LinkedList<Location>()

    /**
     * Evalúa una nueva ubicación contra la ventana cinemática.
     * @return true si el punto pasa el filtro y se incorporó a la ventana
     */
    fun filter(location: Location): Boolean {
        if (location.latitude == 0.0 && location.longitude == 0.0) return false

        if (locationWindow.isNotEmpty()) {
            val last = locationWindow.last
            val dist = last.distanceTo(location)
            val timeDelta = (location.time - last.time) / 1000.0
            if (timeDelta > 0) {
                val calculatedSpeed = dist / timeDelta
                if (calculatedSpeed > maxAllowedSpeedMps) {
                    if (last.speed == 0f && location.accuracy > 30f) {
                        return false
                    }
                }
            }
        }

        locationWindow.addLast(location)
        if (locationWindow.size > windowSize) {
            locationWindow.removeFirst()
        }
        return true
    }

    fun reset() {
        locationWindow.clear()
    }
}