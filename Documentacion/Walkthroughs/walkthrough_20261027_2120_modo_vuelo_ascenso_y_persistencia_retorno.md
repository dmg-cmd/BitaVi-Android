# Informe de Verificación: Transición Continua a Modo Vuelo y Visibilidad de Rutas Completadas

**Fecha y hora:** 2026-10-27 21:20 (UTC-3)  
**Versión de la aplicación:** `v3.2.13-RC1` (`versionCode = 13`)  
**Módulos intervenidos:** `RouteViewModel.kt`, `RouteRepository.kt`, `CinematicFilterManager.kt`, `LocationService.kt`, `CinematicFilterManagerTest.kt`, `app/build.gradle.kts`, `TopBarLocalizador.kt`, `BrandWatermark.kt`.

---

## 1. Diagnóstico del Problema

El usuario reportó dos situaciones críticas:
1. > *"mira que la ultima vez mire si la estaba armando y si pero no llego al final nola encontre o sea no la grabo"*
2. > *"otra cosa que me olvide cuando viaje en avion la marcación no marco cuando estaba subiendo hasta cierto momento que continuo grabando haciendo una linea desde cuando se paro la marcación pero despues andubo bien caundo alcanzo me parece cierta v3locidad y empezo a marca vuelo y continuo marcando"*

### Causas Raíz Técnicas

1. **Ocultamiento de Rutas Finalizadas en Reposo (`RouteViewModel.kt` y `RouteRepository.kt`):**
   - Cuando el usuario finalizaba su viaje de regreso y se detenía en casa, la velocidad caía a 0 km/h.
   - Al cerrarse la ruta por inactividad o detección de parada, `LocationService.stopTracking` enviaba `activityMode = "STOPPED"`.
   - En `RouteViewModel.allRoutes` y `allRoutesForGrouping`, existía la condición `it.activityMode != "STOPPED"`.
   - Por este motivo, aunque la ruta de regreso se hubiera armado con kilómetros de distancia y cientos de puntos en la base de datos, el ViewModel la descartaba del flujo observable, provocando que no apareciera en la pantalla "Mis Recorridos".

2. **Corte y Salto Discontinuo en Despegue de Avión (`CinematicFilterManager.kt`):**
   - Para conmutar de `TERRESTRIAL` a `FLIGHT`, la condición exigía estrictamente `altitudeMeters > 100.0`.
   - Al momento de la rotación y despegue (velocidad entre 180 km/h y 300 km/h), el avión superaba el límite terrestre de 162 km/h (`limitSpeed = 45 m/s`) pero aún no alcanzaba los 100 metros de altitud o la altitud del GPS dentro de la cabina se estabilizaba lentamente.
   - Cuando un punto superaba `limitSpeed`, el filtro retornaba `false` sin actualizar la coordenada de referencia (`refLatitude`, `refTimestamp`).
   - Esto creaba un *deadlock cinemático*: los puntos sucesivos se calculaban contra la posición de la pista de aterrizaje, arrojando distancias crecientes y velocidades exorbitantes que eran descartadas una tras otra.
   - Solo cuando el avión alcanzaba gran velocidad y altitud en crucero, un punto lograba romper la condición, activando `FLIGHT` y trazando una línea recta gigante desde la pista hasta ese punto en el aire.

---

## 2. Solución Implementada

### A. Visibilidad Incondicional de Rutas Genuinas (`RouteViewModel.kt`)
- Se removió `&& it.activityMode != "STOPPED"`. Toda ruta con `distanceMeters >= 25.0` o `isActive` se presenta en 'Mis Recorridos'.
- En `RouteRepository.stopActiveRoute`, se añade inferencia de modo cinemático dominante (`dominantMode`): si una ruta con desplazamiento termina con el usuario detenido, se asigna `DRIVING`, `BIKING` o `WALKING` según la velocidad media real del recorrido.

### B. Transición Inmediata y Fluida a Modo Vuelo (`CinematicFilterManager.kt`)
- **Detección temprana de despegue:** Se conmuta a `FLIGHT` si la velocidad supera 150 km/h y la altitud supera 30 metros:
  ```kotlin
  val isAscendingFlight = calculatedSpeed > AppConstants.Location.FLIGHT_TRIGGER_SPEED_MPS &&
          (altitudeMeters > AppConstants.Location.FLIGHT_MIN_ALTITUDE_METERS || altitudeMeters >= 30.0)
  ```
- **Desbloqueo cinemático (Anti-Deadlock):** Ante 2 lecturas consecutivas de velocidad aeronáutica con altitud positiva, el filtro re-ancla la semilla de referencia hacia la coordenada actual y activa `FLIGHT`, permitiendo registrar la curva de ascenso sin saltos ni líneas rectas artificiales.

### C. Admisión de Precisión en Ascenso (`LocationService.kt`)
- Durante el despegue/ascenso (velocidad > 150 km/h o altitud > 100m), se admite una precisión de hasta 80m (`FLIGHT_MAX_ACCURACY_METERS`), tolerando la atenuación de satélites dentro del fuselaje del avión.

### D. Pruebas Unitarias Automatizadas (`CinematicFilterManagerTest.kt`)
- Se añadieron tests específicos de ascenso en despegue y desbloqueo cinemático ante aceleración continua, superando los 119 tests unitarios sin fallos.

### E. Protocolo de Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 13`, `versionName = "3.2.13"`.
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.13-RC1"`.
- `BrandWatermark.kt`: `version: String = "v3.2.13-RC1"`.

---

## 3. Verificación de Compilación

- Compilación con Gradle: `BUILD SUCCESSFUL` en 1m 55s (40 tareas ejecutadas/actualizadas, 0 fallos).
- Verificación limpia en `testDebugUnitTest`.
