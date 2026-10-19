package com.aistudio.localizador.location.manager

import android.location.Location
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.ui.GeocoderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
class ShortStopMarkManager @Inject constructor(
    private val repository: RouteRepository,
    private val geocoderUseCase: GeocoderUseCase
) {

    private var timeStartedStopping: Long = 0L
    private var isShortStopDetected: Boolean = false
    private var shortStopLat: Double = 0.0
    private var shortStopLon: Double = 0.0
    private var shortStopAddress: String = ""

    internal var managerScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    fun processCinematicInput(
        location: Location,
        speedKmh: Float,
        timestamp: Long,
        shortStopDelayMs: Long = 120000L,
        stopToleranceMs: Long = 180000L,
        stopSpeedThresholdKmh: Float = 1.0f
    ) {
        if (speedKmh <= stopSpeedThresholdKmh) {
            if (!isShortStopDetected) {
                if (timeStartedStopping == 0L) {
                    timeStartedStopping = timestamp
                } else if (timestamp - timeStartedStopping >= shortStopDelayMs) {
                    isShortStopDetected = true
                    shortStopLat = location.latitude
                    shortStopLon = location.longitude
                    shortStopAddress = ""

                    triggerAsyncGeocoding(shortStopLat, shortStopLon)
                }
            }
        } else {
            if (isShortStopDetected && timeStartedStopping > 0L) {
                val durationMs = maxOf(0L, timestamp - timeStartedStopping)
                persistShortStopAsFavorite(durationMs)
            }
            resetEvaluation()
        }
    }

    private fun triggerAsyncGeocoding(lat: Double, lon: Double) {
        managerScope.launch {
            try {
                val details = geocoderUseCase.fetchLocationDetails(lat, lon)
                shortStopAddress = details.first ?: ""
                Log.d("BitaVi_ShortStop", """Fase I: Direccion pre-resuelta: $shortStopAddress""")
            } catch (e: Exception) {
                Log.e("BitaVi_ShortStop", "Error al geocodificar la parada", e)
                shortStopAddress = ""
            }
        }
    }

    private fun persistShortStopAsFavorite(durationMs: Long) {
        val lat = shortStopLat
        val lon = shortStopLon
        val addressHint = shortStopAddress

        managerScope.launch {
            try {
                repository.recordOrAccumulateStopFavorite(
                    latitude = lat,
                    longitude = lon,
                    durationMs = durationMs,
                    addressHint = addressHint
                )
            } catch (e: Exception) {
                Log.e("BitaVi_ShortStop", "Fallo al persistir o acumular parada en favoritos: $e")
            }
        }
    }

    fun resetEvaluation() {
        timeStartedStopping = 0L
        isShortStopDetected = false
        shortStopLat = 0.0
        shortStopLon = 0.0
        shortStopAddress = ""
    }

    fun clearAll() {
        resetEvaluation()
    }
}
