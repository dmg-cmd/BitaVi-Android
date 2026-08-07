# Walkthrough CR-29 — CalculateRouteMetricsUseCase / Analitica de tramos

- **Fecha/Hora**: 2026-08-07 01:56
- **Dispositivo**: 48201FDAS001L5
- **Estado**: COMPLETADO

## Verificaciones
1. **Suite de tests** (`:app:testDebugUnitTest`): BUILD SUCCESSFUL — 24 clases, 100 tests, 0 failures, 0 errors, 0 skipped.
2. **Clase nueva** CalculateRouteMetricsUseCaseTest: 7/7 PASS.
3. **Compilacion** (`:app:assembleDebug`): BUILD SUCCESSFUL (sin warnings de FlowPreview tras @OptIn).
4. **Deploy**: `adb install -r app-debug.apk` -> Success en 48201FDAS001L5.
5. **Documentacion**: HISTORIAL_CAMBIOS.md, DOCUMENTACION.md, SUGERENCIAS.md, SUGERENCIAS_COMPLETADAS.md, ESQUEMA_PANTALLAS.md (item 7.4) actualizados; respaldos 20260807_0105_* creados.
6. **Validacion fisica pendiente de usuario**: fluidez del slider de tramos (7.3.1) en Pantalla 7 con rutas reales de cientos de puntos.

## Notas tecnicas
- El pipeline `tramoMetrics` usa `flatMapLatest` + `flowOn(Dispatchers.Default)`: al arrastrar rapido el slider, el recalculado previo se cancela (Regla II).
- La tarjeta 7.4 ahora muestra Subida +X m / Bajada -Y m con histeresis altimetrica d=2.5m (Regla V).
- Los graficos (7.2.1 y 7.2.2) memoizan series y LineDataSet con remember(points); solo las LimitLine reaccionan al slider.
- Inconvenientes resueltos en la ejecucion: import `flow` faltante en RouteViewModel y corrupcion de la linea del comentario ExperienceMap (comentario+declaracion pegados) tras la insercion.