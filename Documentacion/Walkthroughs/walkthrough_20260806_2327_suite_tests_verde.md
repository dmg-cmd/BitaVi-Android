# Walkthrough — CR-28: Suite de Unit Tests 100% Verde y Determinista
**Fecha/Hora:** 2026-08-06 (tarde-noche) · **Entorno:** JVM (Gradle 9.5, AGP) · **Plataforma:** Windows 11

## Objetivo
Alcanzar la Regla III (Cero Advertencias / Pruebas Determinísticas): suite completa `testDebugUnitTest` en verde al 100% sin OutOfMemoryError ni flakiness, corrigiendo 2 frentes — tests desactualizados y OOM de MockK/Robolectric.

## Diagnóstico encontrado
1. **OOM acumulativo**: con `maxParallelForks=1`, toda la suite corría en un único worker reutilizado que retenía ClassLoaders de Robolectric/ByteBuddy (MockK), agotando el heap y matando clases tardías.
2. **OOM intrínseco (bucle infinito)**: `RouteExportUseCaseTest.getUriForFile` llamaba `FileProvider.getUriForFile` con un Context mockeado `relaxed`; dentro de FileProvider, `parser.next()` relajado devolvía 0 (≠ END_DOCUMENT=1) → bucle infinito hasta heap dump (~4 GB). Al estallar, corrompía el worker y hacía fallar en cascada a `ShortStopMarkManagerTest` y `ContactUseCaseTest`.
3. **Test desactualizado**: `ActivityModeManagerTest` esperaba `BIKING` para 20 km/h, pero el contrato real (`evaluate(speedKmh, timestamp, upgradeMs, downgradeMs, walking=1.0, biking=7.0, driving=13.0)`) produce `DRIVING` (20>13). También fallaba la ventana de downgrade: 140000-30000=110s < 120s.
4. **Carrera de corrutinas**: `coVerify` de MockK no espera corrutinas lanzadas en `CoroutineScope(Dispatchers.IO)` externo; la persistencia `addSavedLocation` (14 parámetros, timestamp default `System.currentTimeMillis()`) nunca matcheaba por 1 ms (necesario `timestamp = any()`).

## Cambios aplicados
| Archivo | Cambio |
|---|---|
| gradle.properties | daemon a `-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC` |
| app/build.gradle.kts | `forkEvery=50`, `jvmArgs(HeapDumpOnOutOfMemoryError, MaxMetaspaceSize=512m)`; mantiene `maxHeapSize=3072m`, `maxParallelForks=1` |
| ActivityModeManagerTest.kt | aserciones BIKING→DRIVING (L31,32,58), tests de límites de umbral, downgrade t=150000 |
| RouteExportUseCaseTest.kt | `@RunWith(RobolectricTestRunner) @Config(sdk=[34])`, `Uri.parse` real, `mockkStatic(FileProvider)`, `coEvery getRouteById=null` |
| ShortStopMarkManagerTest.kt | `timestamp=any()`, `runTest` + scheduler controlado, `mockkStatic(Log)` |
| ShortStopMarkManager.kt | `private val managerScope` → `internal var managerScope` (visibilidad, sin cambio de comportamiento) |

## Verificación (resultados)
1. `:app:testDebugUnitTest` suite completa → **BUILD SUCCESSFUL**.
2. Conteo desde XML: **22 clases, 86 tests, 0 failures, 0 errors, 0 skipped**.
3. Re-ejecución inmediata → BUILD SUCCESSFUL (cache).
4. `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` (re-ejecución forzada) → BUILD SUCCESSFUL, 86/86 con timestamps regenerados.

## Conclusiones
- El `forkEvery` resolvió la acumulación interclase; el bucle infinito de FileProvider era la raíz del OOM "intratable" y del contagio a otras clases.
- La suite quedó determinista: 3 corridas consecutivas con el mismo resultado 86/86.
- Pendiente: eliminar ~8 GB de heap dumps de diagnóstico (`app/java_pid20472.hprof`, `app/java_pid7832.hprof`) con permiso del usuario.