# Walkthrough — Modo Vuelo Automático (CR-27)
**Fecha/Hora:** 2026-08-06 19:15 · **Dispositivo:** 48201FDAS001L5 · **Variante:** debug (2.0)

## Objetivo
Verificar que la app graba rutas aéreas (>162 km/h) sin requerir el interruptor manual 8.3, mediante detección cinemática automática.

## Cambios aplicados
1. **AppConstants.kt**: constantes `FLIGHT_*` (max 333.33 m/s, trigger 41.67 m/s, histéresis 33.33 m/s, altitud 100 m, precisión 80 m) + alias terrestres.
2. **CinematicFilterManager.kt**: `enum KineticProfile { TERRESTRIAL, FLIGHT }`; transición con histéresis dentro de `evaluateAndProcess`; umbral dinámico; auto-stop suprimido en FLIGHT; `getCurrentProfile()`.
3. **GpsFilterPipeline.kt**: pasa `altitudeMeters` al filtro; delega `getCurrentProfile()`.
4. **LocationService.kt**: Fase 1 (50→80 m), guarda `shouldForceStop` solo TERRESTRIAL, odómetro (50→80 m), persistencia SQLCipher (20→80 m); `StateFlow<KineticProfile> kineticProfile`.
5. **RouteViewModel.kt**: puente `kineticProfile`; eliminado `isAirplaneMode`/`toggleAirplaneMode()`.
6. **UI**: item 8.3 retirado del Drawer (MainActivity + PreferencesManager); HelpDialog "Vuelo Automático"; badge "✈️ Vuelo" en TopBar 1.3.1 (MapScreen).

## Verificación automatizada
- `:app:testDebugUnitTest --tests CinematicFilterManagerTest/GpsFilterPipelineTest/StationaryAnchorManagerTest` → **29 tests, PASS** (incluye 4 escenarios FLIGHT nuevos: transición, no auto-stop en vuelo, degradación al aterrizar, rechazo terrestre de jet).
- `:app:assembleDebug` → **BUILD SUCCESSFUL**.
- Suite completa (81 tests): fallos OOM preexistentes en clases MockK/Robolectric no relacionadas (RouteExport, Contact, ProximityBle, ActivityMode, ShortStop) — ya documentados como preexistentes desde CR-26; ActivityModeManagerTest espera BIKING para 20 km/h (test desactualizado de archivo sin commitear).
- `adb install -r app-debug.apk` → Success. Launch OK (`am start`).

## Resultado
- Toggle fantasma eliminado: `airplane_mode` ya no se escribe ni se lee.
- Detección: con v calculada >41.67 m/s y altitud >100 m el perfil conmuta a FLIGHT; los puntos se persisten con tolerancia 80 m y velocidad hasta 333.33 m/s; al aterrizar vuelve a TERRESTRIAL.

## Pendientes / Notas
- Los OOM de la suite completa dependen de la RAM libre del equipo; el worker de tests quedó configurado con heap 3 GB y 1 fork en `app/build.gradle.kts`.