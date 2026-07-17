# Walkthrough: AL-01 Fase III — Pipeline de Filtrado Unificado
**Fecha**: 2026-07-16 03:00
**Tema**: Desintegración del bridge en tubería cinemática + Auto-stop por ventana temporal

## Cambios realizados

### CinematicFilterManager.kt (Refactor completo)
- **Eliminado**: filterAndProcess(Location), calculateFallbackSpeed(), evaluatePoint()
- **Eliminadas**: lastValidLocation, lowSpeedCounter, hasRef, refSpeed (variables de estado acopladas a Location)
- **Nuevo**: evaluateAndProcess(lat, lon, accuracy, timestamp, speed): Boolean — unifica filtro cinemático + máquina de auto-stop
- **Nuevo**: isAutoStopActive(): Boolean — getter para estado de auto-stop
- **Nuevo estado**: lowSpeedStartTimestamp, isAutoStopTriggered — máquina de inactividad por ventana temporal (180s)
- **Preservado**: seedReferencePoint(), reset(), haversineDistance(), constructor configurable

### GpsFilterPipeline.kt (Nuevo)
- processLocation(rawPoint: BufferPoint): BufferPoint? — tubería de 2 capas
  - Capa 1: CinematicFilterManager.evaluateAndProcess() (aberración + auto-stop)
  - Capa 2: StationaryAnchorManager.processStationaryState() + getResolvedCoordinate() (anclaje)
- shouldTriggerAutoStop(): Boolean — delega a cinematicFilter.isAutoStopActive()

### LocationService.kt (Cirugía mínima)
- **Nuevo import**: GpsFilterPipeline
- **Nueva propiedad**: filterPipeline (inicializada tras stationaryAnchorManager)
- **Líneas 448-451 reemplazadas**: filterAndProcess + processAnchor → pipeline con BufferPoint
- **triggerAutomaticTracking()**: Eliminado bridge BufferPoint→Location→CinematicFilterManager→BufferPoint. Ahora usa GpsFilterPipeline directamente.

### Tests
- **CinematicFilterManagerTest.kt**: 4 tests actualizados a evaluateAndProcess() + isAutoStopActive()
- **GpsFilterPipelineTest.kt**: 3 tests nuevos (aberración, auto-stop 180s, reanudación)

## Verificación
- ./gradlew assembleDebug: BUILD SUCCESSFUL
- ./gradlew testDebugUnitTest --tests "*.manager.*": 9/9 tests pasan
