# Plan Maestro Consolidado — BitaVi-Android
**Fecha:** 15 de Octubre de 2026  
**Estado:** Plan de acción priorizado tras verificación cruzada (docs vs código real)  
**Contexto:** MainActivity ya refactorizada (181 líneas). Pendientes: 3 pantallas grandes, LocationService, branding, tests, WakeLock, limpieza.

---

## 📊 Resumen de Estado Actual (Verificado)

| Componente | Líneas | Estado | Prioridad |
|------------|--------|--------|-----------|
| `MainActivity.kt` | **181** | ✅ Hecho (era 1115) | — |
| `MapScreen.kt` | **1173** | ❌ Pendiente | **P1** |
| `SavedLocationsScreen.kt` | **1055** | ❌ Pendiente | **P1** |
| `LocationService.kt` | **1057** | ❌ Pendiente | **P1** |
| `BiometricAuthHandler.kt` | 178 | ⚠️ Revisar API keys | P2 |
| `RetrofitClient.kt` | 73 | ⚠️ No mockeable en tests | P2 |
| `AppDatabase.kt` | 124 | OK | — |
| Tests con API key hardcoded | — | ❌ Pendiente | P2 |
| WakeLock 25h | Línea 363 | ❌ Pendiente | P1 |
| Branding inconsistente | Código fuente | ❌ Pendiente | P3 |
| `tmp/` + `RESPALDO_ANTERIOR/` | ~18 + 6 archivos | ❌ Pendiente | P4 |

---

## 🎯 FASE 1 — ARQUITECTURA (P1 — Alta Prioridad, 3-4 semanas)

### 1.1 Descomposición de `MapScreen.kt` (1173 → <300 líneas)
**Objetivo:** Extraer overlays y lógica a componentes atómicos.

| Paso | Archivo/Componente | Descripción |
|------|-------------------|-------------|
| 1.1.1 | `ui/screens/map/overlays/TrackingRoutePolylineOverlay.kt` | Traza cinemática activa (polilínea + reciclaje) |
| 1.1.2 | `ui/screens/map/overlays/WaypointsMarkersOverlay.kt` | Gestión y pooling de iconos de marcadores |
| 1.1.3 | `ui/screens/map/overlays/MapControlsDock.kt` | Dock flotante inferior translúcido |
| 1.1.4 | `ui/screens/map/MapScreenCore.kt` | Orquestador MVI: `MapUiState` / `MapUiIntent` |
| 1.1.5 | `ui/screens/MapScreen.kt` | Reducido a <300 líneas, solo compose + delegación |

**Dependencias:** Ninguna (independiente).  
**Tests:** Nuevos tests para cada overlay + `MapScreenCoreTest`.  
**Verificación:** `./gradlew test` + deploy dispositivo.

---

### 1.2 Descomposición de `SavedLocationsScreen.kt` (1055 → <300 líneas)
**Objetivo:** Separar lista, detalle, importación/exportación y diálogo de foto.

| Paso | Archivo/Componente | Descripción |
|------|-------------------|-------------|
| 1.2.1 | `ui/screens/saved/SavedLocationsList.kt` | LazyColumn + agrupación por fecha |
| 1.2.2 | `ui/screens/saved/SavedLocationDetailSheet.kt` | BottomSheet con métricas y acciones |
| 1.2.3 | `ui/screens/saved/PhotoImportDialog.kt` | Diálogo SAF + extracción EXIF |
| 1.2.4 | `ui/screens/saved/ExportImportMenu.kt` | Menú ZIP/Excel/GPX |
| 1.2.5 | `ui/screens/SavedLocationsScreen.kt` | Composable raíz <300 líneas |

**Dependencias:** Ninguna.  
**Tests:** Tests por componente + integración.

---

### 1.3 Patrón Fachada en `LocationService.kt` (1057 → <400 líneas)
**Objetivo:** Delegar persistencia, notificaciones y ciclo de vida GPS.

| Paso | Archivo/Componente | Descripción |
|------|-------------------|-------------|
| 1.3.1 | `location/delegate/LocationPersistenceDelegate.kt` | Escritura async en Room (`Dispatchers.IO`) |
| 1.3.2 | `location/delegate/LocationNotificationDelegate.kt` | Wrapper sobre `ServiceNotificationManager` |
| 1.3.3 | `location/delegate/GpsLifecycleDelegate.kt` | `requestLocationUpdates` / `removeUpdates` + WakeLock |
| 1.3.4 | `location/LocationService.kt` | Orquestador puro: `onStartCommand`, `onDestroy`, `onBind` |

**WakeLock 25h (Línea 363):** Mover a `GpsLifecycleDelegate` con política:
- Tracking activo: `acquire()` sin timeout (o 30 min renovable)
- Modo pasivo: `release()` inmediato
- `auto_mode == false` + sin tracking: `stopSelf()` total

**Dependencias:** 1.1 y 1.2 (para que MapScreen use nuevos delegates).  
**Tests:** `LocationServiceTest` con mocks de delegates.

---

## 🎯 FASE 2 — SEGURIDAD Y TESTING (P2 — Media-Alta, 1-2 semanas)

### 2.1 Limpieza de API Keys en Tests
| Archivo | Línea | Acción |
|---------|-------|--------|
| `AnalyzeRouteAiUseCaseTest.kt` | 28, 46 | Reemplazar `"gsk_test_key"` por `TestConstants.TEST_GROQ_KEY` |
| `RetrofitClient.kt` | 41-47 | Hacer `groqApi` y `geminiApi` `internal` + factory para tests |

**Nuevo:** `app/src/test/java/com/aistudio/localizador/TestConstants.kt`

---

### 2.2 Hacer `RetrofitClient` Mockeable
```kotlin
// RetrofitClient.kt — añadir:
internal fun createGroqApi(client: OkHttpClient = okHttpClient): GroqApi = ...
internal fun createGeminiApi(client: OkHttpClient = okHttpClient): GeminiApi = ...
```
Tests usan `MockWebServer` + `createGroqApi(mockClient)`.

---

### 2.3 Auditar `BiometricAuthHandler.kt` (líneas 156-174)
- Verificar que `loadGroqApiKey` / `saveGroqApiKey` no loguean la key
- Añadir `kotlinx.coroutines.withTimeout` en operaciones Keystore

---

### 2.4 Mejorar `.gitignore`
Añadir patrones:
```
# Secrets
*.keystore
*.jks
*.p12
.env.*
local.properties
/secrets/
/keystore/

# Build artifacts extra
/app/release/
*.apk
*.aab

# IDE
.idea/inspectionProfiles/
*.iml
```

---

## 🎯 FASE 3 — BRANDING UNIFICADO (P3 — Media, 3-5 días)

**Identidad definitiva: `BitaVi`** (package `com.aistudio.localizador` se mantiene).

| Archivo | Cambio |
|---------|--------|
| `ui/components/BitaruBrandHeader.kt` | Renombrar → `BitaViBrandHeader.kt`, actualizar `contentDescription` |
| `ui/components/TopBarLocalizador.kt` | Renombrar → `TopBarBitaVi.kt` |
| `ui/theme/Theme.kt` | `LocalizadorTheme` → `BitaViTheme` |
| `domain/usecase/ShareExperienceUseCase.kt` | `"Recorrido Bitaru"` → `"Recorrido BitaVi"` |
| `ui/screens/ExperienceMapScreen.kt` | `"Experiencia Bitaru"` → `"Experiencia BitaVi"` |
| `location/manager/AutoTrackingManager.kt` | TAG `"Bitaru_AutoTrack"` → `"BitaVi_AutoTrack"` |
| `location/LocationService.kt` | TAG `"Bitaru_Core"` → `"BitaVi_Core"` |
| `location/manager/ProximityBleManager.kt` | TAG `"Bitaru_Ble"` → `"BitaVi_Ble"` |
| `location/manager/ShortStopMarkManager.kt` | TAG `"Bitaru_ShortStop"` → `"BitaVi_ShortStop"` |
| `domain/usecase/*` | Logs `"Bitaru_*"` → `"BitaVi_*"` |
| `ui/RouteViewModel.kt` | Logs `"Bitaru"` → `"BitaVi"` |
| `ui/ContactUseCase.kt` | Log `"Bitaru_Contact"` → `"BitaVi_Contact"` |
| `ui/screens/DetailScreen.kt` | Log `"BitaruDebug"` → `"BitaViDebug"` |
| Tests | Actualizar assertions y mocks |

**Nota:** `strings.xml` y `colors.xml` ya usan "BitaVi" — solo código fuente.

---

## 🎯 FASE 4 — LIMPIEZA REPOSITORIO (P4 — Baja-Media, 1-2 días)

### 4.1 Purgar `tmp/`
```bash
rm -rf tmp/
# (contiene: temp_edit*.js, historial-cambios.md duplicado, temp_locationservice.txt)
```

### 4.2 Gestionar `RESPALDO_ANTERIOR/`
1. Crear tag git: `git tag backup-pre-limpieza-20261015`
2. Comprimir respaldos >30 días: `tar -czf RESPALDO_ANTERIOR_legacy.tar.gz RESPALDO_ANTERIOR/2024* RESPALDO_ANTERIOR/202607* RESPALDO_ANTERIOR/202608* RESPALDO_ANTERIOR/202609*`
3. Mantener solo últimos 30 días en carpeta
4. Documentar política en `Documentacion/SUGERENCIAS.md`

---

## 🎯 FASE 5 — DOCUMENTACIÓN (Continuo)

| Archivo | Acción |
|---------|--------|
| `Documentacion/HISTORIAL_CAMBIOS.md` | Registrar cada fase al completar |
| `Documentacion/DOCUMENTACION.md` | Actualizar funcionalidad de usuario |
| `Documentacion/SUGERENCIAS.md` | Marcar completadas, mover a `SUGERENCIAS_COMPLETADAS.md` |
| `Documentacion/Walkthroughs/walkthrough_YYYYMMDD_HHMM_[tema].md` | Informe por fase completada |

---

## 📋 ORDEN DE EJECUCIÓN RECOMENDADO

```
Semana 1-2:  Fase 1.1 (MapScreen)  →  Deploy + tests
Semana 2-3:  Fase 1.2 (SavedLocationsScreen)  →  Deploy + tests
Semana 3-4:  Fase 1.3 (LocationService + WakeLock)  →  Deploy + tests
Semana 5:    Fase 2 (Seguridad + Tests)  →  Deploy
Semana 5:    Fase 3 (Branding)  →  Deploy
Semana 5:    Fase 4 (Limpieza)  →  Tag git
Continuo:    Fase 5 (Documentación)
```

---

## ⚠️ RIESGOS Y MITIGACIONES

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Romper MapScreen en refactor | Alto | Tests de regresión + deploy incremental por overlay |
| LocationService deja de grabar | Crítico | Feature flag `useNewDelegates` + fallback a código actual |
| Branding rompe strings en UI | Medio | Buscar/Reemplazar global + verificación visual |
| Tests fallan por RetrofitClient | Medio | MockWebServer + factory methods antes de refactor |
| WakeLock mal gestionado → battery drain | Alto | Tests de integración en dispositivo real (4h+ pantalla apagada) |

---

## ✅ CRITERIOS DE ACEPTACIÓN GLOBAL

- [ ] `MapScreen.kt` < 300 líneas, overlays en `ui/screens/map/overlays/`
- [ ] `SavedLocationsScreen.kt` < 300 líneas, componentes en `ui/screens/saved/`
- [ ] `LocationService.kt` < 400 líneas, 3 delegates inyectados por Hilt
- [ ] WakeLock: 0% CPU/batería en `auto_mode=false` + sin tracking
- [ ] 0 API keys hardcoded en tests; `RetrofitClient` mockeable
- [ ] 0 referencias "Bitaru"/"LocalizadorTheme"/"TopBarLocalizador" en `app/src/main`
- [ ] `tmp/` eliminado; `RESPALDO_ANTERIOR/` ≤ 30 días
- [ ] Suite tests 100% verde (`./gradlew testDebugUnitTest`)
- [ ] Compilación 0 warnings (`./gradlew assembleDebug`)
- [ ] Deploy en dispositivo real funcionando (tracking, mapa, favoritos, IA)

---

## 🤔 PREGUNTAS PARA CONFIRMAR ANTES DE EJECUTAR

1. **¿Orden estricto 1.1 → 1.2 → 1.3 o se puede paralelizar 1.1 + 1.2?**
2. **¿WakeLock: política de 30 min renovable o `acquire()` indefinido solo en tracking activo?**
3. **¿Branding: renombrar archivos (`BitaruBrandHeader.kt` → `BitaViBrandHeader.kt`) o solo contenido?**
4. **¿`RESPALDO_ANTERIOR/`: comprimir todo y borrar carpeta, o mantener últimos 30 días?**
5. **¿Ejecutar todo en una sesión `implementa` o por fases con deploy intermedio?**

---

*Generado automáticamente tras verificación cruzada de `docs/ANALISIS_PROBLEMAS_PROYECTO.md`, `docs/FODA_PROYECTO.md`, `docs/PLAN_MODULARIZACION_Y_DETECTOR_SALIDAS.md` contra código real.*
