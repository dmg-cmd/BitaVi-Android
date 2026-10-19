# Informe de Verificación: Desacoplamiento de Puntos Fijos de Parada a Favoritos con Acumulación de Tiempo

**Fecha y hora:** 2026-10-19 15:01 (UTC-3)  
**Versión de la aplicación:** `v3.2.8-RC1` (`versionCode = 8`)  
**Módulos intervenidos:** `RouteRepository.kt`, `ShortStopMarkManager.kt`, `LocationService.kt`, `RouteViewModel.kt`, sincronización de versión.

---

## 1. Diagnóstico del Problema

El usuario reportó el siguiente requerimiento arquitectónico:
> *"En la ruta sigue colocando puntos fijos de parada. Los puntos fijos de parada tendrían que estar en favoritos, no en la ruta en sí. Y en los favoritos, acumular el tiempo de parada también. Lo tendría que editar y colocar el máximo tiempo una vez que me empiece a mover nuevamente."*

### Causas Identificadas en el Código Anterior:
1. **Creación de Rutas Espurias de Parada en la Tabla `routes`:**
   - En `RouteRepository.stopActiveRoute`, si la sesión finalizaba con desplazamiento nulo o inferior a 25 metros (`isZeroMovement`), se renombraba y persistía como una ruta completa denominada `"Parada yyyy-MM-dd HH:mm"` con `activityMode = "STOPPED"`.
   - En la pantalla de historial (`HistoryScreen.kt`), estas entradas aparecían listadas con la etiqueta `"Punto Fijo" / "Parada"`, contaminando la lista de recorridos genuinos.
2. **Duplicación Incesante de Puntos Estacionarios en el Trazado:**
   - Durante una sesión activa de grabación en `LocationService.kt`, mientras el usuario permanecía detenido en un lugar fijo, el servicio continuaba insertando puntos idénticos de latitud y longitud en `location_points` a intervalos de 1 a 3 segundos.
3. **Falta de Acumulación de Tiempo en Favoritos:**
   - `ShortStopMarkManager.kt` creaba un registro independiente cada vez que se detectaba una parada, sin verificar si ya existía un favorito previo en ese sitio ni acumular el tiempo de permanencia histórico de ese punto.

---

## 2. Solución Implementada

### A. Canalización Exclusiva a Favoritos y Purga de Rutas de Parada (`RouteRepository.kt`)
- Se implementaron los métodos `recordOrAccumulateStopFavorite`, `formatStopDuration` y `extractDurationMsFromComment`.
- En `stopActiveRoute`:
  - Si una sesión registra movimiento nulo (`distanceMeters < 25.0`), se extrae la coordenada del punto detenido y la duración exacta (`now - route.startTime`).
  - Se invoca `recordOrAccumulateStopFavorite`: si ya existe un favorito en un radio de 50 metros, **acumula el tiempo de parada** sobre el comentario y actualiza la fecha/hora. Si es un lugar nuevo, crea el favorito con categoría `AUTOMATIC`, color `#FF6D00` y tiempo formateado.
  - **Purga de la ruta:** Se eliminan de forma atómica la ruta efímera de la tabla `routes` y sus puntos asociados de `location_points`, impidiendo que las paradas aparezcan en el historial de rutas.

### B. Actualización al Reanudar el Movimiento (`ShortStopMarkManager.kt`)
- Al romper la inercia y comenzar a moverse nuevamente (`speedKmh > stopSpeedThresholdKmh`), se calcula el tiempo real y definitivo de detención (`timestamp - timeStartedStopping`).
- Se invoca `repository.recordOrAccumulateStopFavorite` con las coordenadas fijadas, la dirección geocodificada y la duración máxima real, actualizando el favorito en base de datos.

### C. Supresión de Puntos Fijos Repetidos en Trazado Activo (`LocationService.kt`)
- Se introdujo `lastPersistedLocation`. Mientras la velocidad sea inferior a 1 km/h (`isStationary`) y la distancia al último punto guardado sea menor a 4 metros, se omite el guardado redundante en base de datos, evitando que el trazado de la ruta se sature de puntos fijos duplicados.

### D. Filtrado Estricto de Rutas en `RouteViewModel.kt`
- En `allRoutes` y `allRoutesForGrouping`, se filtraron explícitamente las rutas para excluir cualquier residuo previo de parada (`!it.name.startsWith("Parada") && it.activityMode != "STOPPED" && (it.distanceMeters >= 25.0 || it.isActive)`), asegurando que "Mis Recorridos" solo presente trayectos de desplazamiento real.

### E. Protocolo de Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 8`, `versionName = "3.2.8"`.
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.8-RC1"`.
- `BrandWatermark.kt`: `version: String = "v3.2.8-RC1"`.

---

## 3. Verificación de Compilación

- Compilación con Gradle: `BUILD SUCCESSFUL` en 1m 41s.
- Verificación limpia en `assembleDebug`.
