# Informe de Verificación: Corrección de Visualización de Ruta Grabada en la Primera Entrada

**Fecha y hora:** 2026-10-19 13:59 (UTC-3)  
**Versión de la aplicación:** `v3.2.6-RC1` (`versionCode = 6`)  
**Módulos intervenidos:** `ExperienceMapScreen.kt`, `RouteViewModel.kt`, sincronización de versión.

---

## 1. Diagnóstico del Problema

El usuario reportó la siguiente anomalía:
> *"Tengo problemas para ver la ruta grabada. La primera vez no entra, hay que salir y volver a entrar para que funcione, para que se muestre la ruta."*

### Causa Raíz Identificada

Se detectaron dos causas concurrentes que provocaban que la primera vez que se abría la pantalla del mapa del recorrido grabado (`ExperienceMapScreen`) la ruta no se visualizara:

1. **Premature Completion de Bandera de Centrado (`hasInitiallyCenteredRoute`):**
   - En la primera recomposición de `ExperienceMapScreen`, la lista reactiva `points` iniciaba vacía (`emptyList()`) mientras la corrutina de Room consultaba los datos.
   - El efecto `LaunchedEffect(points, waypoints, linkedFavorites, routeId)` se ejecutaba inmediatamente.
   - Al encontrar `geoPoints.isEmpty()`, no ejecutaba el bloque de zoom, pero **incondicionalmente asignaba `hasInitiallyCenteredRoute = true`**.
   - Cuando la consulta a base de datos finalizaba milisegundos después y emitía los puntos, el `LaunchedEffect` se reactivaba, pero la condición `if (!hasInitiallyCenteredRoute)` evaluaba a falso, cancelando el enfoque de cámara para siempre.
   - El mapa de OSMDroid permanecía en sus coordenadas iniciales por defecto `(0.0, 0.0)` en medio del Océano Atlántico (Golfo de Guinea) con zoom 0, fuera del campo visual del recorrido.
   - Al salir y volver a entrar, el `ViewModel` ya conservaba los puntos en memoria en el primer frame, permitiendo que la segunda entrada sí encontrara puntos y ejecutara el centrado.

2. **Destrucción Asíncrona de Overlays en Factory de AndroidView:**
   - En el bloque `factory` de `AndroidView(MapView)`, existía un llamado diferido `post { overlays.removeAll { ... } }`.
   - Dicho `post` se ejecutaba en la cola de mensajes de la interfaz de usuario justo después del primer ciclo de `update` de Jetpack Compose, **eliminando las capas de polilíneas y marcadores recién creadas**.

3. **Carencia de Medición de View en Compose (`width == 0`):**
   - Si `mapView.zoomToBoundingBox` se invoca antes de que el MapView adquiera dimensiones físicas mayores a cero dentro del layout de Jetpack Compose, el cálculo del bounding box en OSMDroid fallaba silenciosamente.

---

## 2. Cambios Implementados

### A. Corrección del Ciclo de Vida y Centrado en `ExperienceMapScreen.kt`
- Se vinculó el estado de centrado al identificador de la ruta: `var hasInitiallyCenteredRoute by remember(routeId) { mutableStateOf(false) }`.
- Se condicionó `hasInitiallyCenteredRoute = true` **exclusivamente** a cuando existan puntos (`geoPoints.isNotEmpty()`) y el proceso de centrado se ejecute con éxito.
- Se implementó un detector de layout: si el mapa aún se está midiendo (`mapView.width == 0`), se suscribe un `OnLayoutChangeListener` de disparo único que ejecuta el centrado en cuanto el mapa obtiene sus dimensiones reales.
- Se contempló el caso borde de recorridos con un único punto o dispersión infinitesimal (`< 0.0005`) utilizando `setCenter` y `setZoom(16.0)` de forma directa para evitar excepciones matemáticas con `BoundingBox`.
- Se eliminó el `post { overlays.removeAll { ... } }` destructivo del bloque `factory`.
- Se incorporó un indicador de progreso (`CircularProgressIndicator`) mientras `points.isEmpty()` para brindar retroalimentación visual inmediata.

### B. Precarga Inmediata en `RouteViewModel.kt`
- En `prepareExperience(routeId)`, se asignan inmediatamente los puntos en memoria de `selectedDetailPoints.value` a `_experiencePoints.value` si ya están disponibles, asegurando que la pantalla de experiencia disponga de la geometría desde el instante cero, al tiempo que la corrutina en segundo plano garantiza la sincronización con Room.

### C. Protocolo de Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 6`, `versionName = "3.2.6"`.
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.6-RC1"`.
- `BrandWatermark.kt`: `version: String = "v3.2.6-RC1"`.

---

## 3. Verificación de Compilación

- Compilación con Gradle: `BUILD SUCCESSFUL` en 46s sin errores.
- Tareas ejecutadas: `kspDebugKotlin`, `compileDebugKotlin`, `assembleDebug`.
