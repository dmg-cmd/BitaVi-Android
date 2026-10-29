# Informe de Verificación: Reparación del Arranque de Vuelta en Modo Automático

**Fecha y hora:** 2026-10-27 20:05 (UTC-3)  
**Versión de la aplicación:** `v3.2.12-RC1` (`versionCode = 12`)  
**Módulos intervenidos:** `LocationService.kt`, `AutoTrackingManager.kt`, `AutoTrackingManagerTest.kt`, `app/build.gradle.kts`, `TopBarLocalizador.kt`, `BrandWatermark.kt`.

---

## 1. Diagnóstico del Problema

El usuario reportó la siguiente falla crítica:
> *"tengo un problema me di cuenta que cuando hago el mismo camino de ida y vuelta me borra el de vuelta ya va la tercera vez que me lo hace"*
>
> **Aclaración del usuario:** *"Uso el Modo Automático (auto_mode): la app inició la ida, pero al quedarme quieto en el destino o regresar a casa, la ruta de vuelta no se grabó o desapareció. En la lista de rutas ('Mis Recorridos') la ruta de vuelta no aparece guardada (o solo está la de ida)."*

### Causa Raíz Técnica

1. **Aborto Silencioso por Búfer Vacío en `LocationService.kt` (`triggerAutomaticTracking`):**
   - Cuando el usuario llegaba a su destino (Punto B) y permanecía quieto, la ruta de ida finalizaba correctamente (`stopTrackingWithAutoRestart`).
   - El servicio pasaba a modo pasivo (`startPassiveTracking`).
   - Al emprender el regreso a casa (Punto A), `AutoTrackingManager` detectaba el alejamiento radial (`isDistanceTriggered = true`) e invocaba `listener.onMovementSustainedTriggered(snapshot)`.
   - Sin embargo, en `LocationService.kt`, la función `triggerAutomaticTracking` contenía la guarda:
     ```kotlin
     if (bufferSnapshot.isEmpty()) return
     ```
   - Al salir de un edificio o lugar de reposo, el búfer de pre-grabación (`preRecordingBuffer`) solía estar vacío debido a que los puntos en reposo no superaban el filtro de precisión estricto (`< 20m`).
   - Como resultado, `triggerAutomaticTracking` abortaba con `return` **sin invocar la devolución de llamada `onHistoryProcessed`**, impidiendo que `startAutoNewRoute()` fuera llamado.
   - El arranque automático quedaba bloqueado por la ventana de enfriamiento de 30 segundos (`lastAutoStartTimeMillis`) y el punto de anclaje quedaba en `null`, provocando que **el viaje de regreso se realizara completamente sin iniciar la grabación**.

2. **Umbral de Admisión Excesivamente Rígido en `AutoTrackingManager.kt`:**
   - La condición `if (anchoredAccuracy < AppConstants.Location.MAX_GPS_ACCURACY_METERS)` descartaba cualquier punto con precisión superior a 20 metros, impidiendo que lecturas válidas de reposo en interiores moderados (20 a 40 metros) poblaran el búfer previo al movimiento.

3. **Anclaje Espurio de Geocercas en Movimiento:**
   - En `processNewLocation`, ante una lectura de modo pasivo, se rearmaba la geocerca de salida (`departureWatcherManager.startWatchingDeparture`) sin verificar si el usuario ya estaba en movimiento, interfiriendo con la detección del alejamiento.

---

## 2. Solución Implementada

### A. Desbloqueo del Arranque Automático con Búfer Vacío (`LocationService.kt`)
- Se modificó `triggerAutomaticTracking` para que, en caso de que `bufferSnapshot.isEmpty()`, invoque inmediatamente `onHistoryProcessed(emptyList())`:
  ```kotlin
  if (bufferSnapshot.isEmpty()) {
      Log.i(TAG, "Buffer de pre-grabación vacío. Iniciando ruta automática de inmediato con coordenada actual.")
      onHistoryProcessed(emptyList())
      return
  }
  ```
- Esto garantiza que `startAutoNewRoute(pointsToBackfill = emptyList())` se ejecute indefectiblemente, creando la nueva ruta con `_latestLocation.value` e iniciando el seguimiento de vuelta.

### B. Admisión Tolerante al Búfer de Pre-Grabación (`AutoTrackingManager.kt`)
- Se elevó el umbral de admisión de puntos al búfer de pre-grabación a `TOLERANT_GPS_ACCURACY_METERS` (40 metros), garantizando que al comenzar la marcha el búfer contenga telemetría reciente:
  ```kotlin
  val anchoredAccuracy = location.accuracy
  if (anchoredAccuracy <= AppConstants.Location.TOLERANT_GPS_ACCURACY_METERS) {
      val bp = TrackingSessionManager.BufferPoint(...)
      sessionManager.handlePreRecording(bp)
  }
  ```

### C. Condicionamiento de Rearme de Geocerca Pasiva (`LocationService.kt`)
- Se condicionó el inicio de la vigilancia de salida pasiva al estado de reposo efectivo (`isStationary`):
  ```kotlin
  val isStationary = lastComputedSpeedMps < 0.28f || _currentActivityMode.value == ActivityMode.STOPPED
  if (!departureWatcherManager.isCurrentlyWatching() && isStationary && anchoredLocation.accuracy <= AppConstants.Location.TOLERANT_GPS_ACCURACY_METERS) {
      departureWatcherManager.startWatchingDeparture(anchoredLocation.latitude, anchoredLocation.longitude)
  }
  ```

### D. Pruebas Unitarias Automatizadas (`AutoTrackingManagerTest.kt`)
- Se añadió un test unitario que comprueba que lecturas dentro del rango tolerante (35m) sean correctamente agregadas a `sessionManager.handlePreRecording`.

### E. Protocolo de Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 12`, `versionName = "3.2.12"`.
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.12-RC1"`.
- `BrandWatermark.kt`: `version: String = "v3.2.12-RC1"`.

---

## 3. Verificación de Compilación

- Compilación con Gradle: `BUILD SUCCESSFUL` en 2m 38s (40 tareas ejecutadas/actualizadas, 0 fallos).
- Pruebas unitarias de telemetría y auto-tracking ejecutadas y aprobadas al 100%.
