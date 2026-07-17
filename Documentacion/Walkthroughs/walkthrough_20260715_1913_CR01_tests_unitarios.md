# Walkthrough CR-01 — Suite de Pruebas Unitarias Básicas
## Fecha: 2026-07-15 19:13

## Objetivo
Implementar red de seguridad (suite de tests unitarios JVM) para los algoritmos de filtrado cinemático y anclaje estacionario antes de manipular archivos monolíticos (LocationService.kt, RouteViewModel.kt).

## Archivos modificados

### Producción
- **CinematicFilterManager.kt** (`com.example.location.manager`):
  - Nuevos métodos JVM-safe: `seedReferencePoint()`, `evaluatePoint()`, `haversineDistance()`.
  - Estado interno en primitivas (`refLat`, `refLon`, `refTimestamp`, `hasRef`) desacoplado de `android.location.Location`.
  - `filterAndProcess(Location)` preservado como wrapper para producción.
  
- **StationaryAnchorManager.kt** (`com.example.location.manager`):
  - Nuevos métodos JVM-safe: `processStationaryState()`, `getResolvedCoordinate()`, `evaluateRelease()`.
  - Estado interno en primitivas (`anchorLat`, `anchorLon`, `hasAnchor`) desacoplado de `android.location.Location`.
  - `processAnchor(Location)` preservado como wrapper para producción.

### Infraestructura
- **gradle/libs.versions.toml**: Añadido `mockk = "1.13.8"` en [versions] y [libraries].
- **app/build.gradle.kts**: Añadido `testImplementation(libs.mockk)`.

### Tests (nuevos)
- **CinematicFilterManagerTest.kt** (`com.aistudio.localizador.location.manager`):
  - Test 1: Salto aberrante (~176 km en 2s) → rechazado por velocidad excesiva.
  - Test 2: Desplazamiento coherente (~10 m en 4s) → aceptado.

- **StationaryAnchorManagerTest.kt** (`com.aistudio.localizador.location.manager`):
  - Test 1: Drift en interiores (0.1 m/s) → coordenada congelada al anclaje.
  - Test 2: Reanudación de marcha (4.5 m/s) → anclaje liberado.

### Respaldos
- RESPALDO_ANTERIOR/20260715_CinematicFilterManager.kt
- RESPALDO_ANTERIOR/20260715_StationaryAnchorManager.kt

## Resultado
```
BUILD SUCCESSFUL in 32s
32 actionable tasks: 5 executed, 27 up-to-date
```
4/4 tests verdes en JVM local sin emulador.

## Lecciones aprendidas
1. PowerShell here-strings con comillas dobles (`@"..."@`) interpretan backticks como escape. Usar `@'...'@` para preservar backticks literales.
2. `android.location.Location` no está disponible en JVM pura. Para tests JVM, la lógica debe operar sobre primitivas.
3. La función `evaluatePoint()` debe rechazar cualquier velocidad sobre el límite cinemático sin importar la precisión declarada (a diferencia de `filterAndProcess` que tiene una excepción para accuracy ≤ 30m).