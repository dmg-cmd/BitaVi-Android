# Reporte de Implementación: Filtro de Ruido GPS Inteligente

Se ha refactorizado `LocationService.kt` para mejorar la calidad de los datos recolectados mediante un sistema de validación multicapa, eliminando efectos secundarios y exponiendo la precisión en tiempo real a la UI.

## Cambios Implementados

### 1. Función `isLocationValid()` — Validación Pura sin Efectos Secundarios

Se extrajo la lógica de filtrado a una función independiente y pura (sin side effects):

```kotlin
private fun isLocationValid(location: Location): Boolean {
    if (location.accuracy > ABSOLUTE_MAX_ACCURACY) return false
    val last = lastValidLocation ?: return true
    val timeDelta = location.time - last.time
    val distanceStep = last.distanceTo(location)
    if (timeDelta < MIN_TIME_DELTA_MS && distanceStep < MIN_DISTANCE_DELTA) return false
    if (distanceStep < MIN_DISTANCE_THRESHOLD) return false
    if (timeDelta > 0) {
        val speedMps = distanceStep / (timeDelta / 1000f)
        if (speedMps > MAX_VALID_SPEED_MPS) return false
    }
    if (location.accuracy > MAX_ACCEPTABLE_ACCURACY) return false
    return true
}
```

### 2. Constantes de Configuración Actualizadas

| Constante | Valor Anterior | Valor Actual | Propósito |
|---|---|---|---|
| `ABSOLUTE_MAX_ACCURACY` | 50.0m | 50.0m (sin cambio) | Pre-filtro: error máximo absoluto |
| `MAX_ACCEPTABLE_ACCURACY` | 30.0m | **20.0m** | Precisión mínima aceptable para guardar |
| `MIN_TIME_DELTA_MS` | 5000ms | **3000ms** | Delta temporal mínimo para evitar ruido HF |
| `MIN_DISTANCE_DELTA` | 15.0m | 15.0m (sin cambio) | Distancia mínima combinada con time delta |
| `MIN_DISTANCE_THRESHOLD` | 10.0m | 10.0m (sin cambio) | Distancia mínima absoluta entre puntos |
| `MAX_VALID_SPEED_MPS` | 4.5 m/s (~16 km/h) | **45.0 m/s (~162 km/h)** | Velocidad máxima creíble |

### 3. Eliminación de Side Effect

Anteriormente, `lastComputedSpeedMps` se actualizaba dentro del bloque de validación. Esto causaba que incluso puntos descartados por el filtro modificaran el estado de velocidad usado por la UI y el detector de modo actividad. Ahora `lastComputedSpeedMps` solo se calcula al inicio de `processNewLocation()` (línea 314), antes del filtro, manteniendo la velocidad del último punto válido como referencia para el auto-mode.

### 4. Nuevo StateFlow: `_currentAccuracy`

Se añadió un nuevo `StateFlow<Float>` en el companion object de `LocationService`:

```kotlin
private val _currentAccuracy = MutableStateFlow(Float.MAX_VALUE)
val currentAccuracy = _currentAccuracy.asStateFlow()
```

Se actualiza en cada `processNewLocation()` justo después de `_latestLocation`, permitiendo que la UI (futuro) muestre indicadores visuales de la calidad de la señal GPS.

### 5. Flujo Actual de `processNewLocation()`

```
1. _latestLocation.value = location
2. _currentAccuracy.value = location.accuracy
3. lastComputedSpeedMps = cálculo desde lastValidLocation
4. updateActivityMode()
5. Auto-mode / Near-me logic
6. serviceScope.launch {
     if (!isLocationValid(location)) return@launch
     lastValidLocation = location
     acumular distancia + guardar en Room
   }
```

## Beneficios Esperados

1. **Rutas más limpias:** El filtro de 20m de precisión elimina puntos borrosos en zonas de señal débil (túneles, cañones urbanos).
2. **Sin corrupción de estado:** La función `isLocationValid()` es pura y no modifica variables externas.
3.**Precisión en distancia:** Al no acumular micro-movimientos ni saltosGPS, la distancia total es más fiel a la realidad.
4. **Exposición de métrica:** `_currentAccuracy` permite en el futuro mostrar un indicador visual de calidad de señal en la UI.
