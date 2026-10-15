# Informe Walkthrough: Reparación del Auto-Arranque por Alejamiento y Creación de Scope Auditor

- **Fecha y Hora:** 2026-09-29 15:22
- **Tema:** Corrección del Sistema Automático para Iniciar Rutas al Alejarse y Blindaje Modular de Alcance
- **Estado:** Implementado, Verificado y Compilado Exitosamente (BUILD SUCCESSFUL)

---

## 1. Problema Abordado
Tras la implementación previa de la consolidación de paradas de 0 metros, se detectó que el modo automático de seguimiento (`auto_mode`) no arrancaba cuando el usuario se alejaba físicamente de un punto de reposo. Asimismo, el usuario manifestó la inquietud recurrente de que modificaciones puntuales parecían alterar múltiples módulos ajenos a la solicitud inicial.

## 2. Diagnóstico de Causas Raíz
1. **Velocidad nula en modo pasivo (`LocationService.kt`):**
   - Al detener una ruta previa, `lastValidLocation` se reiniciaba a `null`.
   - Durante el modo pasivo (cuando `_isTracking.value == false`), la actualización de `lastValidLocation` estaba bloqueada tras la comprobación de `routeId != -1`.
   - Por consiguiente, `lastComputedSpeedMps` calculaba indefinidamente `0.0 m/s`, imposibilitando la activación del disparador por velocidad sostenida.
2. **Inexistencia de disparador por desplazamiento radial (`AutoTrackingManager.kt`):**
   - El sistema guardaba el anclaje de detención en `lastStopLocation`, pero únicamente evaluaba la velocidad, sin medir la distancia física euclidiana/geográfica recorrida para determinar si el usuario se había alejado del punto de detención.
3. **Acoplamiento arquitectónico en un único módulo Gradle (`:app`):**
   - No existían fronteras modulares rígidas a nivel de compilación, lo que facilitaba efectos colaterales involuntarios en cascada si no se delimitaba estrictamente el alcance.

---

## 3. Solución Aplicada

### A. Subagente de Alcance (`scope-boundary-auditor`)
- Se definió y documentó el séptimo subagente del proyecto en `.agents/AGENTS.md`, `.agents/skills/scope-boundary-auditor/SKILL.md` y `.opencode/skills/scope-boundary-auditor/SKILL.md`.
- Su directiva obligatoria exige una *allowlist* previa de archivos permitidos y el bloqueo estricto de cualquier archivo fuera del área bajo análisis.

### B. Doble Disparador en `AutoTrackingManager.kt`
- Se incorporó la función de cálculo geográfico `calculateDistanceMeters()` mediante la fórmula de Haversine pura (JVM-Safe, sin dependencias de Android).
- **Disparador 1 (Alejamiento Radial Inmediato):** Si la distancia entre la ubicación actual y `lastStopLocation` es `>= 25.0 metros` (`AppConstants.Location.MOVEMENT_TRIGGER_THRESHOLD_METERS`) con precisión satelital `<= 20.0 metros`, se dispara de forma inmediata el inicio de la nueva ruta.
- **Disparador 2 (Velocidad Sostenida):** Se mantiene la verificación secundaria para arranques por velocidad continua (`speedKmh > 2.5 km/h` durante 10 segundos).

### C. Mantenimiento de Estado en Modo Pasivo (`LocationService.kt`)
- Se habilitó la actualización continua de `lastValidLocation` durante el modo pasivo ante lecturas de precisión admisible (`<= TOLERANT_ACCURACY`).
- Se añadió lectura directa de `location.speed` como respaldo cuando el GPS satelital lo provea nativamente.

---

## 4. Archivos Respaldados
- `RESPALDO_ANTERIOR/20260929_1520_AutoTrackingManager.kt`
- `RESPALDO_ANTERIOR/20260929_1520_LocationService.kt`
- `RESPALDO_ANTERIOR/20260929_1520_AutoTrackingManagerTest.kt`

## 5. Resultados de Verificación
- **Prueba Unitaria Agregada:** `when distance from stop anchor exceeds threshold trigger immediately` en `AutoTrackingManagerTest.kt`.
- **Ejecución de Gradle (`./gradlew testDebugUnitTest`):**
  - **Resultado:** `BUILD SUCCESSFUL in 1m 59s` (40 tareas accionables, 100% pruebas unitarias superadas, cero fallos).
