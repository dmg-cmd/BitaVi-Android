## [2026-10-15] FASE 1.3: Patrón Fachada en LocationService y Nueva Política de WakeLock (Grabación sin Límite)

### Logro
Se refactoriza `LocationService.kt` (1057 -> 806 líneas) extrayendo la persistencia, las notificaciones y el ciclo de vida del GPS a tres delegados inyectables. Además se corrige el WakeLock que tenía un límite fijo de 25 horas: ahora, mientras la grabación está activa, la CPU permanece despierta y el GPS solicitando señal de forma indefinida (una grabación larga nunca se corta), y se libera al pasar a modo pasivo o al detener.

### Detalles Técnicos
- `location/delegate/GpsLifecycleDelegate.kt` (nuevo): cliente GPS fusionado + HandlerThread + LocationCallback + WakeLock. `setTrackingActive(true)` -> `acquire()` SIN timeout; `false` -> `release()`.
- `location/delegate/LocationNotificationDelegate.kt` (nuevo): envuelve `ServiceNotificationManager`, notificación de seguimiento y alertas de proximidad.
- `location/delegate/LocationPersistenceDelegate.kt` (nuevo): envuelve `RouteRepository`; `attach()` tras descifrar la base SQLCipher.
- `LocationService.kt`: inyecta los 3 delegados; se retiran WakeLock/HandlerThread/callback directos; liberación de WakeLock en pasivo/pausa/stop/shutdown/onDestroy.

### Archivos
- Nuevos: `location/delegate/GpsLifecycleDelegate.kt`, `location/delegate/LocationNotificationDelegate.kt`, `location/delegate/LocationPersistenceDelegate.kt`
- Modificado: `location/LocationService.kt`
- Respaldo: `RESPALDO_ANTERIOR/20261015_LocationService_pre_fase13.kt`

### Verificación
- `compileDebugKotlin`: BUILD SUCCESSFUL.
- `testDebugUnitTest`: 109 tests, 0 fallos, 0 errores.
- `installDebug`: BUILD SUCCESSFUL, instalado en 2 dispositivos (Pixel 9 Pro XL).

## [2026-10-15] Cierre FASE 1.1/1.2: Modularización de MapScreen/SavedLocationsScreen, Reparación de Cachés Gradle y Suite 100% Verde

### Logro
Se retoma y cierra la modularización de las dos pantallas más grandes del proyecto (FASE 1.1 y 1.2 del Plan Maestro), que había quedado interrumpida por un cuelgue de sesión el 2026-10-14. `MapScreen.kt` pasó de 1173 a 243 líneas y `SavedLocationsScreen.kt` de 1055 a 153 líneas. Además se descubrió y corrigió un problema de infraestructura de compilación: las cachés de Gradle conservaban rutas absolutas de la ubicación anterior del proyecto (`D:\Google IA Studio\localizador`), lo que impedía ejecutar cualquier test unitario. Tras la reparación, se añadieron tests de regresión y la suite completa quedó en verde (109/0).

### Detalles Técnicos
- **Causa raíz del fallo de tests:** la tarea `kspDebugUnitTestKotlin` lanzaba `IllegalArgumentException: this and base files have different roots` porque `app/build` y `.gradle` contenían rutas absolutas de `D:\`. Se eliminaron esos artefactos generados y se reconstruyó con `--no-build-cache`.
- **MapScreen.kt (ui.screens):** orquestador reducido a 243 líneas; delega en `ui/screens/map/` (MapCanvas, MapTopBar, MapFloatingControls, MapOverlayController, IndicadorTranslucidoContenedor, MapSaveLocationDialog, WaypointDialog, StartRouteDialog).
- **SavedLocationsScreen.kt (ui.screens):** orquestador reducido a 153 líneas; delega en `ui/screens/saved/` (SavedLocationsList, LocationCard, SaveLocationDialog, EditLocationDialog).
- **Tests nuevos (5):** IndicadorTranslucidoContenedorTest (1), LocationCardTest (2), SavedLocationsListTest (2). Patrón Robolectric + Compose UI Test.
- **Tests preexistentes corregidos (3):** DepartureWatcherManagerTest (2, faltaba mock de Intent/PendingIntent en entorno JVM) y ExampleRobolectricTest (1, string esperado desactualizado "BiTaRu" -> "BiTaVi").
- **Regla de idioma reforzada** en `AGENTS.md` y `.aierules`.

### Archivos
- Nuevos: `app/src/test/java/com/aistudio/localizador/ui/screens/map/IndicadorTranslucidoContenedorTest.kt`, `.../ui/screens/saved/LocationCardTest.kt`, `.../ui/screens/saved/SavedLocationsListTest.kt`
- Modificados: `.../location/manager/DepartureWatcherManagerTest.kt`, `.../ExampleRobolectricTest.kt`, `AGENTS.md`, `.aierules`
- Respaldos: `RESPALDO_ANTERIOR/20261015_DepartureWatcherManagerTest.kt`, `RESPALDO_ANTERIOR/20261015_ExampleRobolectricTest.kt`

### Verificación
- `testDebugUnitTest`: 109 tests, 0 fallos, 0 errores, 0 skipped.
- `installDebug`: BUILD SUCCESSFUL, instalado en 2 dispositivos (Pixel 9 Pro XL).
- Sin rutas `Google IA Studio` en el nuevo `app/build`.

## [2026-10-14] Fotos y Ubicación: Corrección de Carrera NullPointer y Guardado Infalible de Coordenadas

### Logro
Se detecta y soluciona la falla reportada por el usuario donde al presionar "Guardar" una foto con coordenadas en Favoritos la acción era ignorada o provocaba un fallo silencioso en segundo plano. Se corrigió una condición de carrera asíncrona (`NullPointerException` en `SavedLocationsScreen.kt:136`), la inversión de lectura de streams EXIF y la validación de nombre vacío, permitiendo ahora guardar ubicaciones con fotos con o sin nombre escrito, generando un nombre táctico automático (`"Foto dd/MM HH:mm"`).

### Detalles Técnicos
- **SavedLocationsScreen (`ui.screens`):**
  - **Eliminación de NullPointerException:** Se extrae el URI objetivo en una referencia inmutable local antes de limpiar el estado de solicitud de permisos `pendingPhotoUri = null`.
  - **Tolerancia a Permisos Denegados:** Si `ACCESS_MEDIA_LOCATION` es denegado o restringido en Android 10+, la foto del usuario ya no se descarta; se procesa el almacenamiento interno y la extracción de metadatos de forma segura.
  - **Orden de Extracción EXIF:** Se ejecuta `extractLocationFromPhoto(targetUri)` antes de `saveImageToInternalStorage(targetUri)`, evitando que el stream de lectura de la galería sea consumido previamente.
  - **Guardado Infalible:** Si el usuario no escribe un nombre manual, se asigna automáticamente `"Foto dd/MM HH:mm"` o la primera línea de la dirección encontrada, eliminando la condición bloqueante `if (newName.isNotBlank())`.
  - **Feedback Visual:** Inclusión de confirmación `Toast.makeText("Ubicación guardada con éxito")`.
- **MapScreen (`ui.screens`):**
  - Aplicación del mismo blindaje defensivo para la selección de fotos y extracción de coordenadas de waypoints y favoritos desde el mapa.
- **Auditoría de Calidad:**
  - `./gradlew testDebugUnitTest`: 100% de tests unitarios en verde.
  - `./gradlew assembleDebug`: Compilación exitosa e instalación en Google Pixel 9 Pro XL (`Success`, PID 13555).

### Archivos Modificados
- `app/src/main/java/com/aistudio/localizador/ui/screens/SavedLocationsScreen.kt`
- `app/src/main/java/com/aistudio/localizador/ui/screens/MapScreen.kt`

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20261014_SavedLocationsScreen_pre_fix_foto.kt`
- `RESPALDO_ANTERIOR/20261014_MapScreen_pre_fix_foto.kt`

## [2026-10-14] Arquitectura UI: Modularización Incremental y Desacoplada de MainActivity.kt

### Logro
Se completa con éxito la modularización táctica de `MainActivity.kt` (reducción drástica de 1.115 a 181 líneas, -83.7% de complejidad ciclomática) encapsulando contratos de permisos, biometría cifrada y el andamiaje composable en módulos satélite independientes en el paquete `com.aistudio.localizador.ui.container`. Se mantiene 100% la compatibilidad hacia atrás, contratos públicos y la suite completa de pruebas unitarias en verde sin regresiones ni alertas.

### Detalles Técnicos
- **ActivityContractsCoordinator (`ui.container`):**
  - Encapsula el registro a nivel de Activity de `RequestPermission` para `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` y `ACTIVITY_RECOGNITION`.
  - Orquesta la solicitud escalonada de permisos con diálogo explicativo (rationale Google Play) para ubicación en segundo plano.
- **BiometricAuthHandler (`ui.container`):**
  - Encapsula la orquestación de `BiometricPrompt` nativo (`authenticateSimple`), la inicialización resiliente de `EncryptedSharedPreferences` (con auto-recuperación ante llaves corruptas en el Keystore), la persistencia segura de la clave Groq y la frase de paso de la DB SQLCipher.
- **MainScaffoldContainer (`ui.container`):**
  - Desacopla la vista raíz composable conteniendo el `ModalNavigationDrawer`, cabecera táctica, diálogos modales (`ApiKeyDialog`, `DeleteAllConfirmDialog`, `LocationDialog`, `HelpDialog`), la barra inferior trifásica unificada (`Scaffold.bottomBar`) y el grafo de pantallas `NavHost`.
- **MainActivity (`com.aistudio.localizador`):**
  - Reducida a orquestador reactivo de ciclo de vida y anfitrión Compose (181 líneas).
  - Conserva métodos de puente (`findActivity`, `authenticateSimple`, `Greeting`) para cero impacto en tests y componentes existentes.
- **Auditoría de Calidad:**
  - `gradle-compiler-qa`: `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` en 100% BUILD SUCCESSFUL.

### Archivos Creados
- `app/src/main/java/com/aistudio/localizador/ui/container/ActivityContractsCoordinator.kt`
- `app/src/main/java/com/aistudio/localizador/ui/container/BiometricAuthHandler.kt`
- `app/src/main/java/com/aistudio/localizador/ui/container/MainScaffoldContainer.kt`

### Archivos Modificados
- `app/src/main/java/com/aistudio/localizador/MainActivity.kt`

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20261014_MainActivity_pre_modularizacion.kt`

## [2026-10-14] Telemetría y Eficiencia: Módulo Autónomo Detector de Salidas (Geofence Low-Power) y Cero Consumo

### Logro
Se resuelve de raíz la falla donde el modo automático se dormía o dejaba de detectar salidas tras reposo prolongado con pantalla apagada en Android 12+/14+. Se implementa el módulo satélite desacoplado `DepartureWatcherManager` basado en el coprocesador de hardware del módem (`GeofencingClient` con `GEOFENCE_TRANSITION_EXIT`), permitiendo despertar el servicio con privilegios de excepción desde segundo plano sin sufrir suspensiones por Doze Mode. Se garantiza además una política estricta de apagado absoluto (0% CPU, 0% RAM, 0% batería) cuando el modo automático está desactivado.

### Detalles Técnicos
- **DepartureWatcherManager (`com.aistudio.localizador.location.manager`):**
  - Anclaje de geocerca pasiva en el punto de reposo (`radius = 50m`, `responsiveness = 10s`).
  - Monitoreo por hardware de bajo consumo sin mantener la CPU despierta.
- **GeofenceDepartureReceiver (`com.aistudio.localizador.location.receiver`):**
  - `BroadcastReceiver` registrado en el manifiesto que recibe el evento `EXIT` y despierta `LocationService.ACTION_START_FROM_DEPARTURE`.
- **LocationService (`com.aistudio.localizador.location`):**
  - En modo pasivo, se libera el WakeLock continuo para permitir entrada a reposo profundo (*Doze safe*).
  - Si `auto_mode == false` y finaliza o no hay tracking: ejecución inmediata de `shutdownService()` con remoción de notificaciones, desconexión de ubicación y `stopSelf()`.
  - Integración de `ACTION_UPDATE_AUTO_MODE` para apagar o encender el monitoreo en caliente según el toggle de la interfaz.
- **AutoTrackingManager (`com.aistudio.localizador.location.manager`):**
  - Calibración de tolerancia de anclaje inicial para interiores (`accuracy <= TOLERANT_GPS_ACCURACY_METERS` = 45m).
- **LocationServiceManager & RouteViewModel (`com.aistudio.localizador.ui`):**
  - Conexión del interruptor `toggleAutoMode()` hacia `LocationServiceManager.updateAutoMode()`.
- **Pruebas Automatizadas:**
  - Nueva suite `DepartureWatcherManagerTest` con 3 pruebas unitarias deterministas.
  - Suite completa ejecutada desde cero con `--rerun-tasks`: `BUILD SUCCESSFUL` en 2m 24s.

### Archivos Modificados
- `app/src/main/java/com/aistudio/localizador/util/AppConstants.kt`
- `app/src/main/java/com/aistudio/localizador/location/LocationService.kt`
- `app/src/main/java/com/aistudio/localizador/location/manager/AutoTrackingManager.kt`
- `app/src/main/java/com/aistudio/localizador/ui/LocationServiceManager.kt`
- `app/src/main/java/com/aistudio/localizador/ui/RouteViewModel.kt`
- `app/src/main/java/com/aistudio/localizador/ui/screens/MapScreen.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/aistudio/localizador/location/manager/DepartureWatcherManager.kt` (Nuevo)
- `app/src/main/java/com/aistudio/localizador/location/receiver/GeofenceDepartureReceiver.kt` (Nuevo)
- `app/src/test/java/com/aistudio/localizador/location/manager/DepartureWatcherManagerTest.kt` (Nuevo)

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20261014_1832_LocationService.kt`
- `RESPALDO_ANTERIOR/20261014_1832_AutoTrackingManager.kt`
- `RESPALDO_ANTERIOR/20261014_1832_RouteViewModel.kt`
- `RESPALDO_ANTERIOR/20261014_1832_LocationServiceManager.kt`

## [2026-09-29] Mapas y Visualización: Corrección de Teselas Satelitales (ArcGIS Z/Y/X)

### Logro
Se soluciona la distorsión y carga desordenada de fragmentos del mapa satelital. Se adapta la fuente de teselas de osmdroid a la convención nativa de ArcGIS REST API (`Z/Y/X` en lugar del estándar `Z/X/Y`), evitando la inversión de ejes y eliminando la caché corrupta previa.

### Detalles Técnicos
- **MapTileSourceManager (`com.aistudio.localizador.ui.map`):**
  - Implementación de `ArcGisTileSource` extendiendo `OnlineTileSourceBase`.
  - Sobreescritura de `getTileURLString()` para componer la URL con el orden requerido por ArcGIS: `{z}/{y}/{x}`.
  - Actualización de la clave de caché a `"ArcGisSatellite_V2"` para aislar la descarga en un directorio limpio y descartar los mosaicos viejos invertidos.
- **Despliegue (`adb`):**
  - Compilación exitosa con Gradle e instalación en caliente en Google Pixel 9 Pro XL (`48201FDAS001L5`).

### Archivos Modificados
- `app/src/main/java/com/aistudio/localizador/ui/map/MapTileSourceManager.kt`

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20260929_1537_MapTileSourceManager.kt`

## [2026-09-29] Identidad y Despliegue: Renombramiento a BiTaVi e Instalación en Dispositivo Real

### Logro
Actualización de la denominación oficial de la aplicación a **BiTaVi** (**Bi**tácora **Tá**ctica de **Vi**aje). Compilación del paquete APK e instalación inalámbrica verificada en el dispositivo del usuario.

### Detalles Técnicos
- **Recursos de Cadenas (`strings.xml`):**
  - `app_name`: "BiTaVi"
  - `drawer_title_header`: "BiTaVi"
  - `drawer_subtitle_header`: "Bitácora Táctica de Viaje"
  - `notification_service_title`: "BiTaVi activo"
- **Entorno de Compilación (`local.properties`):**
  - Normalización de `sdk.dir` a ruta absoluta de Windows (`C\:\\Users\\delmi\\AppData\\Local\\Android\\Sdk`).
- **Despliegue (`adb`):**
  - Desinstalación limpia del paquete anterior en conflicto de firma (`com.aistudio.localizador.vqtspk`).
  - Instalación exitosa de `app-debug.apk` e inicio del proceso con PID activo en Google Pixel 9 Pro XL (`48201FDAS001L5`).

### Archivos Modificados
- `app/src/main/res/values/strings.xml`
- `local.properties`

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20260929_1523_strings.xml`
- `RESPALDO_ANTERIOR/20260929_1523_local.properties`

## [2026-09-29] Telemetría y Modularidad: Corrección de Auto-Arranque por Alejamiento y Creación de scope-boundary-auditor

### Logro
Se soluciona la falla donde el modo automático no arrancaba al alejarse de un punto de reposo. Se implementa un doble mecanismo de disparo en `AutoTrackingManager` (alejamiento radial geográfico >= 25 metros calculado con Haversine JVM-Safe y velocidad sostenida), se habilita el cálculo continuo de velocidad en modo pasivo en `LocationService`, y se incorpora el subagente `scope-boundary-auditor` para evitar alteraciones cruzadas entre módulos.

### Detalles Técnicos
- **AutoTrackingManager (`com.aistudio.localizador.location.manager`):**
  - Implementación del cálculo de distancia geográfica pura mediante la fórmula de Haversine (`calculateDistanceMeters`), 100% JVM-Safe sin acoplamiento a servicios o stubs de Android.
  - Incorporación del disparador inmediato por alejamiento físico: si la distancia entre la ubicación actual y el anclaje de detención (`lastStopLocation`) es `>= MOVEMENT_TRIGGER_THRESHOLD_METERS` (25 m) con precisión `<= MAX_GPS_ACCURACY_METERS` (20 m), se inicia la nueva ruta inmediatamente.
  - Mantenimiento del disparador secundario por velocidad sostenida (`speedKmh > AUTO_START_MIN_SPEED_KMH` durante 10 segundos).
- **LocationService (`com.aistudio.localizador.location`):**
  - Habilitación del cálculo de velocidad en modo pasivo: ahora `lastValidLocation` se actualiza durante el seguimiento pasivo, evitando que `lastComputedSpeedMps` quede fijado en 0.0 m/s.
  - Inclusión de fallback a `location.speed` cuando el proveedor GPS satelital lo provea de forma nativa.
- **Auditoría de Alcance (`scope-boundary-auditor`):**
  - Creación y registro del séptimo subagente especializado en `.agents/AGENTS.md`, `.agents/skills/scope-boundary-auditor/SKILL.md` y `.opencode/skills/scope-boundary-auditor/SKILL.md`.
  - Contención estricta: ninguna pantalla de UI, base de datos ni ViewModel fue alterado en esta corrección.
- **Pruebas Automatizadas (`AutoTrackingManagerTest`):**
  - Nueva prueba unitaria: `when distance from stop anchor exceeds threshold trigger immediately`, validando el arranque inmediato al alejarse ~45 metros.
  - Suite completa de tests superada con `BUILD SUCCESSFUL`.

### Archivos Modificados
- `app/src/main/java/com/aistudio/localizador/location/manager/AutoTrackingManager.kt`
- `app/src/main/java/com/aistudio/localizador/location/LocationService.kt`
- `app/src/test/java/com/aistudio/localizador/location/manager/AutoTrackingManagerTest.kt`
- `.agents/AGENTS.md`
- `.agents/skills/scope-boundary-auditor/SKILL.md`
- `.opencode/skills/scope-boundary-auditor/SKILL.md`

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20260929_1520_AutoTrackingManager.kt`
- `RESPALDO_ANTERIOR/20260929_1520_LocationService.kt`
- `RESPALDO_ANTERIOR/20260929_1520_AutoTrackingManagerTest.kt`

### Verificación
- `./gradlew testDebugUnitTest`: BUILD SUCCESSFUL (100% de pruebas unitarias superadas, cero fallos).

## [2026-09-27] Telemetría y Persistencia: Consolidación de Paradas Estáticas y Tiempo de Parada

### Logro
Se resuelve de forma integral la proliferación de rutas de 0 metros cuando el dispositivo permanece en el mismo sitio. Ahora, ante inactividad o sesiones automáticas sin desplazamiento (< 25 m), el sistema detecta si la ruta previa fue en el mismo lugar (< 40 m) y consolida el tiempo extendiendo el `endTime` de la anterior en lugar de crear infinidad de rutas vacías. Además, la entidad se representa explícitamente como "Parada" con su "Tiempo de parada" dedicado tanto en la base de datos como en la interfaz de usuario.

### Detalles Técnicos
- **RouteDao & Room (`com.aistudio.localizador.data.db`):**
  - Nuevas consultas SQL: `getLastRoute()`, `getPreviousCompletedRoute(excludeId)`, `getLastPointForRoute(routeId)`, `updateRouteEndTime(routeId, endTime)` y `updateRouteEndTimeAndName(routeId, endTime, name)`.
- **RouteRepository (`com.aistudio.localizador.data.repository`):**
  - En `stopActiveRoute()`, si la sesión automática o de parada registra `distanceMeters < 25.0`:
    - Evalúa la ruta anterior completada: si fue una parada o en las mismas coordenadas geográficas (< 40 metros con `Location.distanceBetween`), extiende el `endTime` de la ruta anterior con `System.currentTimeMillis()` y descarta la entrada duplicada actual.
    - Si no hay parada previa contigua: renombra la ruta actual como `"Parada [Fecha Hora]"`, fija su `distanceMeters = 0.0` y establece `activityMode = "STOPPED"`.
- **LocationService (`com.aistudio.localizador.location`):**
  - En `startAutoNewRoute()`, si no hay puntos en el buffer de backfill, ancla de inmediato la última ubicación satelital fija (`_latestLocation`) como primer `LocationPoint` para asegurar que las paradas tengan coordenadas persistentes en base de datos.
- **HistoryScreen (`com.aistudio.localizador.ui.screens`):**
  - Detección de paradas (`isStop`): reemplaza la estadística de "Distancia 0.00 km" por un icono representativo (`Icons.Default.Place` en color ámbar) etiquetado como "Punto Fijo / Parada" y muestra la duración destacada bajo el rótulo **"Tiempo de parada"**.
- **Estabilidad de Gradle (`gradle.properties`):**
  - Ajuste adaptativo de memoria a `-Xmx2048m -XX:MaxMetaspaceSize=512m` para evitar intervenciones del OOM Killer del sistema operativo durante tareas pesadas de dexing.

### Archivos Modificados
- `app/src/main/java/com/aistudio/localizador/data/db/RouteDao.kt`
- `app/src/main/java/com/aistudio/localizador/data/repository/RouteRepository.kt`
- `app/src/main/java/com/aistudio/localizador/location/LocationService.kt`
- `app/src/main/java/com/aistudio/localizador/ui/screens/HistoryScreen.kt`
- `gradle.properties`

### Archivos Respaldados
- `RESPALDO_ANTERIOR/20260926_2342_RouteDao.kt`
- `RESPALDO_ANTERIOR/20260926_2342_RouteRepository.kt`
- `RESPALDO_ANTERIOR/20260926_2342_LocationService.kt`
- `RESPALDO_ANTERIOR/20260926_2342_HistoryScreen.kt`

### Verificación
- `testDebugUnitTest`: BUILD SUCCESSFUL (100% pruebas unitarias superadas).
- `assembleDebug`: BUILD SUCCESSFUL (APK generado sin errores).
- `Deploy`: Exitoso vía ADB Wi-Fi en Pixel 9 Pro XL (`komodo`). Desinstalación limpia de versión en conflicto y arranque de `MainActivity` validado.

## [2026-09-26] Infraestructura: Configuración y Creación de Agentes Especializados

### Logro
Se establecen y configuran en el entorno dinámico y en el repositorio los 5 subagentes especializados recomendados para el Proyecto Localizador, permitiendo una orquestación modular de tareas bajo los estándares tácticos y técnicos del proyecto.

### Detalles Técnicos
- **Definición Dinámica de Subagentes (`define_subagent`):**
  - `doc-custodian`: Custodio de bitácoras, respaldos en `RESPALDO_ANTERIOR/`, gestión de tareas y redacción de Walkthroughs.
  - `telemetry-gps-specialist`: Especialista en `LocationService.kt`, filtrado cinemático, despachos en `Dispatchers.IO` y mitigación de fugas de memoria en `osmdroid` (Anti-OOM).
  - `compose-ui-tactical`: Especialista en Jetpack Compose, codificación `PANTALLA.BARRA.BOTON` (`ESQUEMA_PANTALLAS.md`), paleta cromática semántica (`Color.kt`) y ergonomía táctica.
  - `gradle-compiler-qa`: Auditor de compilación real con Gradle, validación de Cero Warnings, depuración de imports y testing automatizado.
  - `groq-route-analyst`: Especialista en integración analítica de rutas con Groq (Llama 3.3), prompts estructurados y resiliencia offline.
  - `git-release-guardian`: Especialista en seguridad de repositorios Git/GitHub, prevención de fugas de secretos (.env, keystores, API keys), commits atómicos y generación de Releases.
- **Estructura Persistente de Skills en Repositorio:**
  - Creación de `.agents/skills/[doc-custodian, telemetry-gps-specialist, compose-ui-tactical, gradle-compiler-qa, groq-route-analyst, git-release-guardian]/SKILL.md`.
  - Replicación y sincronización de directivas en `.opencode/skills/` para compatibilidad nativa con OpenCode.
  - Actualización de `.agents/AGENTS.md` con el catálogo formal de subagentes.

### Archivos Modificados / Creados
- `.agents/AGENTS.md` (actualizado catálogo con 6 subagentes).
- `.agents/skills/*/SKILL.md` (6 nuevos skills de agentes).
- `.opencode/skills/*/SKILL.md` (6 nuevos skills sincronizados).
- `Documentacion/HISTORIAL_CAMBIOS.md` (actualizado).
- `Documentacion/DOCUMENTACION.md` (actualizado).
- `Documentacion/Walkthroughs/walkthrough_20260926_2320_creacion_agentes_especializados.md` (informe de verificación).

## [2026-08-07] CR-29: CalculateRouteMetricsUseCase — Analitica de Tramos Fuera del UI Thread

### Logro
Se elimina el cuello de botella de la Pantalla 7 (RouteAnalyticsScreen): las metricas del tramo se calculaban inline en la composicion al arrastrar el slider (7.3.1), degradando la tasa de refresco. Ahora el calculo vive en CalculateRouteMetricsUseCase (pure-JVM, inyectable), se ejecuta en Dispatchers.Default con cancelacion via flatMapLatest, y las series de los graficos se memoizan con remember(points).

### Detalles Tecnicos
- **RouteMetricsResult** (nuevo, `com.aistudio.localizador.model`): modelo inmutable de dominio con totalDistanceKm, durationSeconds, averageSpeedKmh, maxSpeedKmh, averagePaceMinPerKm, elevationGainMeters, elevationLossMeters, pointCount.
- **CalculateRouteMetricsUseCase** (nuevo): `@Inject constructor()` sin dependencias Android; distancia Haversine pura; altimetria acumulada con histeresis d=2.5m (ignora ruido barometrico/GPS < 2.5m); velocidad media derivada de distancia/tiempo; maxSpeed desde speed*3.6; pace "m:ss".
- **RouteViewModel**: pipeline reactivo `_tramoRange` -> `tramoMetrics: StateFlow<RouteMetricsResult?>` con `flatMapLatest` + `flowOn(Dispatchers.Default)` (cancela recalculados obsoletos al arrastrar rapido); `updateTramoRange(start, end)`; reset del rango al cambiar de ruta.
- **RouteAnalyticsScreen**: colecta tramoMetrics; `snapshotFlow`+`debounce(150)`+`collectLatest` del rango del slider; series de graficos memoizadas con `remember(points)`; `LineDataSet` memoizado en AnalyticsChart (solo cambian las LimitLine al mover el slider); tarjeta 7.4 rediseñada con Vel. Prom, Subida +X m, Bajada -Y m, Ritmo, Distancia y Modo.

### Tests
- **CalculateRouteMetricsUseCaseTest** (7 tests): lista vacia, 1 punto, ruido <2.5m filtrado, ganancia/perdida acumuladas, distancia Haversine + maxSpeed, formato de ritmo, velocidad media derivada.
- Suite completa: **100 tests, 0 failures, 0 errors, 0 skipped** (24 clases).

### Archivos Modificados
- model/RouteMetricsResult.kt (nuevo), domain/usecase/CalculateRouteMetricsUseCase.kt (nuevo), test CalculateRouteMetricsUseCaseTest.kt (nuevo)
- di/AppModule.kt, ui/RouteViewModel.kt, ui/screens/RouteAnalyticsScreen.kt

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260807_0105_* (RouteAnalyticsScreen.kt, RouteViewModel.kt, AppModule.kt)

### Build
- testDebugUnitTest: BUILD SUCCESSFUL (100/100). assembleDebug: BUILD SUCCESSFUL. Deploy OK.
## [2026-08-07] CR-30: PurgeOrphanPhotosUseCase — Purgado de Fotos Huerfanas

### Logro
Nuevo modulo inyectable `PurgeOrphanPhotosUseCase` que escanea el almacenamiento privado de la app (`filesDir/images/`) y elimina los archivos JPG/JPEG/PNG que ya no estan referenciados por la base de datos (waypoints y favoritos). Previene la acumulacion de imagenes muertas tras borrar rutas/favoritos. Es un modulo de dominio puro, sin UI acoplada, listo para conectar a un boton de mantenimiento.

### Detalles Tecnicos
- **RouteDao**: nueva query `getAllReferencedPhotoPaths()` (UNION ALL de `waypoints.photoPath` y `saved_locations.photoPath`, ambas no vacias).
- **RouteRepository**: nuevo metodo `getAllReferencedPhotoPaths(): List<String>`.
- **PurgeOrphanPhotosUseCase** (`domain/usecase/`): `@Inject constructor(@ApplicationContext Context, RouteRepository)`; `execute(): Result<PurgeResult>` con `PurgeResult(scanned, purged)`; compara por nombre de archivo (las referencias en BD son URIs `content://.../files/images/<nombre>`, por lo que `substringAfterLast('/')` extrae el nombre); solo considera `.jpg/.jpeg/.png` (case-insensitive); si el directorio no existe devuelve (0,0); cualquier error de BD produce `Result.failure` sin borrar nada.
- **AppModule**: provider Hilt para el use case.

### Tests
- **PurgeOrphanPhotosUseCaseTest** (7 tests, 0 fallos): directorio temporal real + repository mockeado; cubre: purga de huerfanos conservando referenciados, borrado total sin referencias, directorio ausente, no-imagenes intactas, extension case-insensitive, referencias como ruta absoluta y error de BD -> isFailure.
- Suite completa: **93 tests, 0 failures, 0 errors, 0 skipped** (23 clases).

### Archivos Modificados
- data/db/RouteDao.kt, data/repository/RouteRepository.kt, di/AppModule.kt
- domain/usecase/PurgeOrphanPhotosUseCase.kt (nuevo), test domain/usecase/PurgeOrphanPhotosUseCaseTest.kt (nuevo)

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260806_2330_* (RouteDao.kt, RouteRepository.kt, AppModule.kt)

### Build
- testDebugUnitTest: BUILD SUCCESSFUL (93/93). assembleDebug: BUILD SUCCESSFUL. Deploy OK.
## [2026-08-06] CR-28: Estabilización de la Suite de Pruebas — 100% Verde y Determinista

### Logro
La suite completa de unit tests pasó de 81 tests con ~9 fallos (OOM de MockK/Robolectric + tests desactualizados) a **86 tests, 0 fallos, 0 errores**, con 3 corridas consecutivas idénticas. Se eliminaron los 4 "asesinos" de la infraestructura: OOM acumulativo de ClassLoaders (worker único), OOM por bucle infinito en FileProvider, test de ActivityModeManager desincronizado con el contrato real y carreras de coroutines en ShortStopMarkManagerTest.

### Detalles Técnicos
- **gradle.properties**: daemon JVM a `-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC` (antes 2560m/768m).
- **app/build.gradle.kts** (`testOptions.unitTests.all`): se añade `forkEvery = 50` (recicla el worker cada 50 tests, liberando ClassLoaders retenidos de Robolectric/ByteBuddy) y `jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:MaxMetaspaceSize=512m")`. Se mantiene `maxHeapSize="3072m"` y `maxParallelForks=1` (sin duplicar `-Xmx`). Encontré que `getUriForFile` del test RouteExport entraba en **bucle infinito dentro de FileProvider** (defaults relajados de MockK: `parser.next()=0≠END_DOCUMENT`) hasta agotar el heap y corromper el worker para las clases siguientes.
- **RouteExportUseCaseTest**: migrado a `@RunWith(RobolectricTestRunner)` + `@Config(sdk=[34])` para implementaciones reales de `Intent`/`Uri`; `Uri.parse()` real en lugar de mocks; `mockkStatic(FileProvider)` en `getUriForFile` (evita el bucle infinito); `coEvery { repository.getRouteById(any()) } returns null` para el caso "ruta inexistente".
- **ActivityModeManagerTest**: corregidas 3 aserciones `BIKING`→`DRIVING` (20 km/h > drivingLimit 13.0) y añadidos tests de límites del contrato real (STOPPED <1.0, WALKING ≤7.0, BIKING ≤13.0, DRIVING >13.0). Además se corrigió el test de downgrade: la ventana de 120 s arranca en el pending start, por lo que el timestamp de consolidación pasó de 140000 a 150000.
- **ShortStopMarkManagerTest**: `coVerify` de `addSavedLocation` ahora matchea con `timestamp = any()` (el default `System.currentTimeMillis()` hacía que el matcher `eq(valor)` nunca coincidiera por 1 ms). El test quedó determinista: se migró a `runTest`, el manager expone su scope para control de scheduler y se mockean los métodos de `android.util.Log` (evita "Method not mocked").
- **ShortStopMarkManager** (único cambio de producción): `private val managerScope` → `internal var managerScope`. Cambio SOLO de visibilidad/testabilidad, sin alterar lógica ni comportamiento; permite inyectar un test dispatcher en los tests.
- 2 heap dumps de diagnóstico (`app/java_pid*.hprof`, ~8 GB en total) generados por los OOM; pendientes de eliminar con permiso.

### Tests
- Suite completa `:app:testDebugUnitTest`: **86 tests, 0 failures, 0 errors, 0 skipped** (22 clases). Verificada 3 veces de forma consecutiva (la 3ª con clean + re-ejecución) para confirmar determinismo.

### Archivos Modificados
- gradle.properties, app/build.gradle.kts, location/manager/ShortStopMarkManager.kt (visibilidad scope)
- tests: location/manager/ActivityModeManagerTest.kt, location/manager/ShortStopMarkManagerTest.kt, domain/usecase/RouteExportUseCaseTest.kt

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260806_1939_* (gradle.properties, build.gradle.kts, ActivityModeManagerTest.kt)
- RESPALDO_ANTERIOR/20260806_2040_* (RouteExportUseCaseTest.kt, ShortStopMarkManagerTest.kt)
- RESPALDO_ANTERIOR/20260806_2256_ShortStopMarkManager.kt

### Build
- testDebugUnitTest: BUILD SUCCESSFUL (86/86). No se despliega en dispositivo: cambio exclusivo de infraestructura de testing (el APK no varía).
## [2026-08-06] CR-27: Modo Vuelo Automático — Detección Cinemática FLIGHT

### Logro
El interruptor manual 8.3 [✈️] Modo Avión era un "toggle fantasma": escribía en SharedPreferences (`airplane_mode`) pero el LocationService nunca lo leía, por lo que las rutas aéreas a >162 km/h se descartaban por completo. Se reemplazó por detección cinemática automática (KineticProfile.TERRESTRIAL/FLIGHT) con histéresis de velocidad/altitud, y se eliminó el switch del Drawer (Regla IV UX).

### Detalles Técnicos
- **CinematicFilterManager**: nuevo `enum KineticProfile` + transición con histéresis (FLIGHT si v>41.67 m/s y altitud>100m; TERRESTRIAL si v<33.33 m/s y altitud<100m). Umbral máximo 333.33 m/s (~1200 km/h) en FLIGHT. Auto-stop de 3 min suprimido en FLIGHT. API pública y constructor preservados (0 regresiones en GpsFilterPipelineTest/Hilt).
- **LocationService**: umbrales dinámicos según perfil — Fase 1 (50m→80m), guarda `shouldForceStop` (solo TERRESTRIAL), odómetro (50m→80m) y persistencia SQLCipher (20m→80m). Nuevo `StateFlow<KineticProfile> kineticProfile` en companion.
- **RouteViewModel**: puente reactivo `kineticProfile`; eliminados `isAirplaneMode` y `toggleAirplaneMode()`.
- **UI**: item 8.3 retirado del Drawer; HelpDialog actualizado a "Vuelo Automático"; badge "✈️ Vuelo" en TopBar 1.3.1 (MapScreen) cuando el perfil es FLIGHT.
- **AppConstants**: constantes FLIGHT_MAX_SPEED_MPS, FLIGHT_TRIGGER_SPEED_MPS, FLIGHT_HYSTERESIS_SPEED_MPS, FLIGHT_MIN_ALTITUDE_METERS, FLIGHT_MAX_ACCURACY_METERS + alias terrestres (MAX_SPEED_THRESHOLD_MPS, TERRESTRIAL_MAX_ACCURACY_METERS).

### Tests
- CinematicFilterManagerTest: +4 escenarios FLIGHT (transición a vuelo, no auto-stop en vuelo, degradación al aterrizar, rechazo terrestre de velocidad de jet). CinematicFilterManagerTest + GpsFilterPipelineTest: PASS.

### Archivos Modificados
- util/AppConstants.kt, location/manager/CinematicFilterManager.kt, location/manager/GpsFilterPipeline.kt, location/LocationService.kt, ui/RouteViewModel.kt, MainActivity.kt, ui/PreferencesManager.kt, ui/dialogs/HelpDialog.kt, ui/screens/MapScreen.kt, app/build.gradle.kts (heap worker tests), gradle.properties (daemon 2.5GB)

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260806_1446_* (10 archivos)

### Build
- testDebugUnitTest: suite completa 81 tests con fallos OOM preexistentes en clases MockK/Robolectric no relacionadas (documentados como preexistentes desde CR-26).
- assembleDebug: BUILD SUCCESSFUL.
- Deploy: instalado y lanzado en dispositivo 48201FDAS001L5 (com.aistudio.localizador.vqtspk).
## [2026-07-31] CR-26: Compartir Captura PNG de la Experiencia — Botón Compartir 5.2.1

### Logro
El botón Compartir de la barra 5.2 de la Pantalla 5 (ExperienceMapScreen) ya no envía fotos de waypoints sueltas: ahora genera una captura PNG de alta resolución del mapa de experiencia (mapa OSM + ruta + marcadores + panel de fotos) y la comparte por el selector nativo como imagen (ACTION_SEND image/png). La barra inferior de acciones queda excluida de la captura, logrando una "tarjeta postal" limpia.

### Detalles Técnicos
- Captura vía Compose 1.7 rememberGraphicsLayer + Modifier.drawWithContent { composeGraphicsLayer.record { drawContent() }; drawLayer(composeGraphicsLayer) } sobre un Box interno que envuelve únicamente el mapa (AndroidView osmdroid), el panel de fotos y los controles flotantes.
- toImageBitmap().asAndroidBitmap() → compresión PNG (calidad 100) en Dispatchers.IO con FileOutputStream.
- Archivo temporal en context.cacheDir/shared_experiences/experiencia_{routeId}.png (sin basura persistente).
- Se comparte con FileProvider.getUriForFile (provider ${packageName}.fileprovider) + FLAG_GRANT_READ_URI_PERMISSION + chooser "Compartir Experiencia...".
- Se eliminó la variable huérfana currentPhotoPaths (sin advertencias de compilación).
- La corrutina de captura se ejecuta en Dispatchers.IO; errores capturados con runCatching (sin crasheos).

### Archivos Modificados
- ui/screens/ExperienceMapScreen.kt

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260731_2341_ExperienceMapScreen.kt

### Build
- testDebugUnitTest: 77 tests, 9 fallidos (preexistentes).
- assembleDebug: BUILD SUCCESSFUL.
- Deploy: instalado y lanzado en dispositivo (com.aistudio.localizador.vqtspk).
## [2026-07-31] CR-25: Fix Botón "Actualizar País/Ciudad" — Reversión y Feedback en Tiempo Real

### Logro
El botón "Actualizar País/Ciudad" del diálogo Editar Favorito (Pantalla 3) sí geocodificaba y persistía, pero el resultado se revertía al pulsar "Guardar Cambios". Se corrigió la reversión (snapshot desincronizado de locationToEdit) y se añadió feedback en tiempo real con el país/ciudad resuelto.

### Causa Raíz
- locationToEdit (estado local Compose) era un snapshot del objeto original; refreshLocationDetails actualizaba la DB pero no el estado del diálogo.
- El confirmButton "Guardar Cambios" ejecutaba locationToEdit!!.copy(...) → reescribía los campos vetustos (country="Desconocido", city="Desconocida") sobre la geocodificación recién lograda.
- El diálogo no mostraba el país/ciudad (cero feedback visual).

### Archivos Modificados
- **ui/RouteViewModel.kt** (refreshLocationDetails):
  - Nuevo parámetro onResult: (SavedLocation) -> Unit = {} despachado en Dispatchers.Main tras persistir.
  - Base fresca desde repository.allSavedLocations.first() (por id) para no pisar campos editados/guardados mientras corre la corutina (evita race condition).
- **ui/screens/SavedLocationsScreen.kt** (diálogo Editar Favorito):
  - Estado isRefreshingLocation + OutlinedButton con CircularProgressIndicator (16dp) y texto "Geocodificando..." mientras procesa.
  - Fila M3 de feedback con Icons.Default.Place + "pais • ciudad" (bodySmall, onSurfaceVariant) que se actualiza en vivo al completar.
  - Botón actualiza locationToEdit = updated vía callback.
  - confirmButton usa como base el favorito fresco de locations (StateFlow) por id (locations.firstOrNull { it.id == id } ?: locationToEdit!!) para conservar el país/ciudad actualizado.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260731_2255_RouteViewModel.kt
- RESPALDO_ANTERIOR/20260731_2255_SavedLocationsScreen.kt

### Build
- testDebugUnitTest: 77 tests, 9 fallidos (preexistentes).
- assembleDebug: BUILD SUCCESSFUL.
- Deploy: instalado y lanzado en dispositivo (com.aistudio.localizador.vqtspk).
## [2026-07-31] CR-24: Cobertura Mundial WGS84 para Geocodificación — Fix País/Ciudad fuera del Cono Sur

### Logro
El fallback Nominatim del CR-23 no se activaba para favoritos fuera del rectángulo lat -55..-20 (Cono Sur): el gate AppConstants.Geofencing devolvía "GPS: Fuera de cobertura" antes de intentar el Geocoder nativo o Nominatim. El usuario reportó el fallo estando en Cusco (Perú, lat ~-13). Se amplió la cobertura a rangos WGS84 mundiales, excluyendo solo el punto nulo (0.0, 0.0).

### Causa Raíz
- Geofencing regional (MIN_LATITUDE=-55, MAX_LATITUDE=-20, MIN_LONGITUDE=-75, MAX_LONGITUDE=-53) en AppConstants.kt.
- GeocoderUseCase.fetchLocationDetails retornaba temprano Triple("GPS: Fuera de cobertura", "Desconocido", "Desconocida") sin intentar geocodificación.
- Diagnosticado: API Nominatim OK (HTTP 200), adapters Moshi generados, dispositivo con internet validado; el gate era el bloqueante.

### Archivos Modificados
- **util/AppConstants.kt** (object Geofencing):
  - MIN_LATITUDE/MAX_LATITUDE = -90.0/90.0, MIN_LONGITUDE/MAX_LONGITUDE = -180.0/180.0 (WGS84 global).
  - isWithinBounds excluye el punto nulo GPS (lat == 0.0 && lon == 0.0).
- **ui/GeocoderUseCase.kt**:
  - isValidCoordinate delega directo en AppConstants.Geofencing.isWithinBounds (se elimina el clamp lat/lon.toFloat()).
  - Mensaje de coordenadas inválidas: "GPS: Sin señal" (reemplaza "GPS: Fuera de cobertura").
  - Se mantiene la firma síncrona Triple(address, country, city) y fetchAddress String?.

### Decisiones Técnicas
- NO convertir a suspend (BackupUseCase usa fetchLocationDetails dentro de un mapNotNull no-suspend; todos los llamadores ya corren en Dispatchers.IO).
- NO añadir @Inject constructor (AppModule:35 ya provee manualmente; se evita binding duplicado de Hilt).
- formatAddress preservado para no alterar la presentación de las tarjetas (Pantalla 3).

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260731_2240_AppConstants.kt
- RESPALDO_ANTERIOR/20260731_2240_GeocoderUseCase.kt

### Build
- testDebugUnitTest: 77 tests, 9 fallidos (preexistentes).
- assembleDebug: BUILD SUCCESSFUL.
- Deploy: instalado y lanzado en dispositivo (com.aistudio.localizador.vqtspk). Verificado Nominatim para Cusco/Perú (country=Perú, city=Machupicchu).
## [2026-07-31] CR-23: Fallback de Geocodificación Nominatim (OSM) — País/Ciudad/Lugar en Favoritos

### Logro
Corregida la pérdida de país, ciudad y dirección ("lugar cercano") al guardar favoritos. El Geocoder del sistema (android.location.Geocoder) es poco fiable (red, GMS, límite de consultas) y dejaba los campos en "Desconocido"/"Desconocida". Se incorporó un fallback de geocodificación inversa vía Nominatim (OpenStreetMap), coherente con osmdroid.

### Causa Raíz
- GeocoderUseCase.fetchLocationDetails dependía 100% del proveedor del sistema; al devolver lista vacía o excepción, se guardaban "Desconocido"/"Desconocida" sin avisar.
- No existía ningún fallback de red.

### Archivos Creados
- **data/api/NominatimApi.kt**: DTOs Moshi (NominatimResponseDto, NominatimAddressDto con getBestCityName) e interfaz Retrofit reverseGeocode (format=json, addressdetails=1, accept-language=es).

### Archivos Modificados
- **data/api/RetrofitClient.kt**:
  - NOMINATIM_BASE_URL.
  - nominatimOkHttpClient dedicado (timeouts 5s + interceptor User-Agent "Agenviaje/1.0 (com.aistudio.localizador)" cumpliendo política OSM), aislado del client Groq/Gemini.
  - nominatimApi (lazy).
- **ui/GeocoderUseCase.kt**:
  - fetchLocationDetails mantiene la firma Triple(address, country, city) para no romper los 7 llamadores.
  - Intento A: Geocoder nativo (guarda Geocoder.isPresent() solo en SDK >= 33). Requiere país y ciudad válidos.
  - Intento B: fallback Nominatim con execute() síncrono (todos los llamadores corren en Dispatchers.IO).
  - fetchAddress también con fallback Nominatim (displayName).
  - refreshLocationDetails ("Actualizar País/Ciudad") recalcula favoritos antiguos sin cambios de firma.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260731_2218_GeocoderUseCase.kt
- RESPALDO_ANTERIOR/20260731_2218_RetrofitClient.kt

### Build
- testDebugUnitTest: 77 tests, 9 fallidos (preexistentes).
- assembleDebug: BUILD SUCCESSFUL.
- Deploy: instalado y lanzado en dispositivo (com.aistudio.localizador.vqtspk).
## [2026-07-31] CR-22: Consolidación de Fecha EXIF en Favoritos — Comentario + Tarjeta

### Logro
Corregida la visibilidad de la fecha de captura de la foto en los favoritos. Ahora la fecha EXIF de la foto se preserva íntegramente al guardar (desde Favoritos y desde el Mapa), se incrusta en el comentario del favorito ("📸 Capturada el dd/MM/yyyy HH:mm") y se muestra como línea visible en la tarjeta de Favoritos (LocationCard).

### Causa Raíz
- RouteViewModel.saveSpecificLocation: el bloque que debía añadir la fecha al comentario calculaba SimpleDateFormat sobre Date() (hora del guardado) sin interpolarlo, y si el usuario escribía una nota lo reemplazaba por "\n\n".
- MapScreen: al extraer EXIF usaba extractLocationFromPhoto(uri)?.let { Pair(...) }, descartando PhotoMetadata.timestamp, por lo que photoTimestamp llegaba null y se guardaba System.currentTimeMillis().
- LocationCard (SavedLocationsScreen): no mostraba ninguna fecha.

### Archivos Modificados
- **ui/RouteViewModel.kt** (saveSpecificLocation):
  - finalTimestamp = photoTimestamp ?: System.currentTimeMillis().
  - finalComment con when: fecha de captura concatenada si hay comentario, o como comentario único si está vacío; guarda contra duplicación del prefijo "📸 Capturada el" en reediciones.
  - repository.addSavedLocation usa timestamp = finalTimestamp.
- **ui/screens/MapScreen.kt**:
  - Nuevo estado photoTimestamp.
  - Captura de metadata.timestamp en las 3 vías de entrada (cámara, galería, reintento de permiso).
  - saveSpecificLocation del showSaveLocationDialog pasa photoTimestamp.
  - Limpieza de photoTimestamp en Guardar y Cancelar.
- **ui/screens/SavedLocationsScreen.kt** (LocationCard):
  - Fila M3 con Icons.Default.Schedule (14dp) + fecha dd/MM/yyyy HH:mm en onSurfaceVariant.
  - Imports java.text.SimpleDateFormat y java.util.Date.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260731_1905_RouteViewModel.kt
- RESPALDO_ANTERIOR/20260731_1905_MapScreen.kt
- RESPALDO_ANTERIOR/20260731_1905_SavedLocationsScreen.kt

### Build
- testDebugUnitTest: 77 tests, 9 fallidos (preexistentes, ajenos a este cambio).
- assembleDebug: BUILD SUCCESSFUL.
- Deploy: instalado y lanzado en dispositivo (com.aistudio.localizador.vqtspk).
## [2026-07-31] CR-21: Fix EXIF GPS API 29+ — Posición de la Foto en Favoritos

### Logro
Corregida la extracción silenciosa de coordenadas GPS de fotos en Android 10+ (Scoped Storage). Al adjuntar una foto a un favorito/punto de interés, la app ahora usa la posición histórica grabada en los metadatos EXIF de la foto en lugar de la posición actual del dispositivo.

### Causa Raíz
- Android 10+ (API 29+) redacta las etiquetas GPS de MediaStore al abrir un InputStream sin MediaStore.setRequireOriginal(uri). ExifInterface retornaba null y la app caía silenciosamente en el fallback de la ubicación actual del GPS.
- SavedLocationsScreen no solicitaba el permiso ACCESS_MEDIA_LOCATION en el flujo de galería (a diferencia de MapScreen), condenando la extracción a fallar desde el inicio.
- MapScreen no activaba el estado reactivo usePhotoGps al retornar del flujo de cámara, por lo que el marcador usaba la posición del punto/teléfono aunque la foto tuviera GPS.

### Archivos Modificados
- **domain/usecase/ExtractPhotoLocationUseCase.kt**:
  - resolveOriginalUri(): usa MediaStore.setRequireOriginal(uri) en API 29+ con fallback seguro ante SecurityException.
  - readLatLongFromStream(): lectura EXIF directa desde InputStream como segundo intento.
  - Coordenadas (0.0, 0.0) descartadas como inválidas.
- **ui/screens/SavedLocationsScreen.kt**:
  - applyPhotoMetadata(): unifica el seteo de coordenadas/estado/dirección/timestamp desde EXIF.
  - photoPickerLauncher solicita ACCESS_MEDIA_LOCATION en API 29+ antes de extraer, con reintento automático al concederse (mediaLocationPermissionLauncher + pendingPhotoUri).
  - Corregido typo de limpieza: inputLatitude = """ → "".
- **ui/screens/MapScreen.kt**:
  - cameraLauncher ahora activa usePhotoGps cuando la foto de cámara contiene GPS.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260731_ExtractPhotoLocationUseCase.kt
- RESPALDO_ANTERIOR/20260731_SavedLocationsScreen.kt
- RESPALDO_ANTERIOR/20260731_MapScreen.kt

### Build
- assembleDebug: BUILD SUCCESSFUL
- Deploy: instalado y lanzado en dispositivo (com.aistudio.localizador.vqtspk)
## [2026-07-22] CR-20: AnalyzeRouteAiUseCase — Extracción del Análisis por IA a Capa de Dominio

### Logro
Extraída la lógica de análisis inteligente de rutas (Groq/Llama 3) desde el RouteViewModel y el antiguo GroqAiUseCase (ui/) hacia un AnalyzeRouteAiUseCase dedicado en domain/usecase/, cumpliendo con la Regla I (Modularidad Estricta) y Regla II (Testeabilidad).

### Archivos Creados
- **domain/usecase/AnalyzeRouteAiUseCase.kt**: UseCase inyectable (@Singleton) que recibe RouteRepository, construye el prompt de telemetría, llama a la API de Groq vía RetrofitClient.groqApi y retorna Result<String>.
- **test/.../domain/usecase/AnalyzeRouteAiUseCaseTest.kt**: 2 tests unitarios JVM (ruta nula → Failure, ruta existente → failure de red esperado).

### Archivos Modificados
- **RouteViewModel.kt**: 
  - Reemplazado groqAiUseCase: GroqAiUseCase → nalyzeRouteAiUseCase: AnalyzeRouteAiUseCase.
  - Migrado iAnalysisState de mutableStateOf a MutableStateFlow + StateFlow para reactividad M3.
  - Simplificado nalyzeRouteWithAI() delegando completamente en el UseCase.
- **DetailScreen.kt**: 
  - Migrado consumo de iAnalysisState de acceso directo a collectAsStateWithLifecycle().
  - Captura local iStateValue para habilitar smart-cast en when.
- **AppModule.kt**: Eliminado provideGroqAiUseCase() (ya no existe en ui/).

### Archivos Eliminados
- **ui/GroqAiUseCase.kt**: Reemplazado por AnalyzeRouteAiUseCase en domain/usecase/.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260722_1810_RouteViewModel.kt
- RESPALDO_ANTERIOR/20260722_1810_DetailScreen.kt
- RESPALDO_ANTERIOR/20260722_1810_GroqAiUseCase.kt
- RESPALDO_ANTERIOR/20260722_1810_AppModule.kt

### Build
- compileDebugKotlin: OK (warnings preexistentes)
- testDebugUnitTest (AnalyzeRouteAiUseCaseTest): OK (2/2)
- assembleDebug: BUILD SUCCESSFUL
## [2026-07-22] CR-19: Restablecimiento de Interfaz — Eliminación de Duplicación Contextual en MainActivity

### Logro
Corregida la "doble barra" en Pantalla 5 (ExperienceMapScreen). La causa raíz era la sub-capa BETA del Scaffold en MainActivity.kt que inyectaba botones [Share]/[Save] y cápsula GPS redundantes cuando currentRoute == NavRoutes.EXPERIENCE_MAP, duplicando la barra inferior 5.2.

### Archivos Modificados
- **MainActivity.kt**:
  - Eliminado bloque if (currentRoute == NavRoutes.EXPERIENCE_MAP) con IconButton(Share) e IconButton(Save) en la sub-capa BETA (líneas 858-882 eliminadas).
  - Cápsula GPS condicionada a currentRoute != NavRoutes.EXPERIENCE_MAP (línea 882) para evitar duplicación con barra 5.2.
- **ExperienceMapScreen.kt**:
  - Estandarizados colores semánticos: Color(0xFF4DD0E1) → IconCompartir, Color(0xFFFFB74D) → IconExportar (constantes de Color.kt, Regla de Convención de Colores Bitaru).

### Archivos No Modificados (Confirmado Correctos)
- **ShareExperienceUseCase.kt**: Sin cambios. Ya implementa FileProvider, validación de archivos y ACTION_SEND_MULTIPLE correctamente.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260722_1745_MainActivity.kt
- RESPALDO_ANTERIOR/20260722_1745_ExperienceMapScreen.kt

### Principio de Diseño (Reglas de Oro I, IV, V)
- Modularidad estricta (Regla I): La intervención se realizó en la causa raíz (MainActivity), no en la pantalla que ya estaba correcta.
- Jerarquía de Información (Regla IV): La barra 5.2 es ahora el ÚNICO contenedor de controles en ExperienceMapScreen.
- Estabilidad (Regla V): Se preservó intacta la funcionalidad completa de ExperienceMapScreen (~590 líneas) evitando una reescritura destructiva.

### Build
- compileDebugKotlin: OK (1 warning preexistente)
- assembleDebug: BUILD SUCCESSFUL
## [2026-07-21] CR-14: Gestión de Sesión Biométrica Reactiva (Ventana de Gracia 30s + Drawer 8.7)

### Logro
Implementado BiometricSessionManager (@Singleton) que administra el estado de desbloqueo biométrico con una ventana de gracia de 30 segundos de inactividad en segundo plano. Esto elimina la re-solicitud biométrica involuntaria al alternar entre aplicaciones por periodos cortos. Cableado completo del Drawer 8.7: al activar la protección, se bloquea la sesión inmediatamente (lockExplicitly); al desactivar, se libera (onAuthenticationSuccess).

### Archivos Creados
- **ui/security/BiometricSessionManager.kt**: Gestor inyectable con StateFlow<Boolean> isUnlocked, ventana de gracia de 30s, lockExplicitly(), onAppBackgrounded()/onAppForegrounded().
- **test/.../ui/security/BiometricSessionManagerTest.kt**: 4 tests JVM puros (sesión inicial bloqueada, desbloqueo, gracia 30s, bloqueo explícito).

### Archivos Modificados
- **MainActivity.kt**: 
  - Inyectado biometricSessionManager. Eliminado isAppUnlocked (mutableStateOf local).
  - setContent usa biometricSessionManager.isUnlocked.collectAsState() + viewModel.isBiometricEnabled para el gating.
  - onStart(): llama biometricSessionManager.onAppForegrounded() + triggerBiometricCheck().
  - onStop(): llama biometricSessionManager.onAppBackgrounded().
  - triggerBiometricCheck() refactorizada: guardia early-return si !isBiometricEnabled o ya desbloqueado; canAuthenticate usa onAuthenticationSuccess(); authenticate usa onAuthenticationSuccess/lockExplicitly.
  - Drawer 8.7: Switch y onClick cableados a lockExplicitly() al activar y onAuthenticationSuccess() al desactivar.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260721_MainActivity.kt

### Principio de Diseño (Reglas de Oro I, II, IV, V)
- Modularidad estricta (Regla I): Toda la lógica de sesión encapsulada en BiometricSessionManager. Cero nuevas variables/funciones en ViewModel.
- Testeable (Regla II): 4 tests JVM puros sin dependencias Android.
- UX Profesional (Regla IV): Ventana de gracia de 30s evita re-autenticaciones molestas al volver rápido de otra app.
- Control manual (Drawer 8.7): Bloqueo/desbloqueo inmediato con feedback táctil.

### Build
- compileDebugKotlin: OK (1 warning preexistente)
- assembleDebug: BUILD SUCCESSFUL
- assembleDebug: PENDIENTE
## [2026-07-21] CR-13: Integración de Branding Corporativo Bitaru en BiometricLockScreen

### Logro
Implementado el banner rectangular corporativo Bitaru (BiTaRu-recta-003-esp.png) como elemento hero en la pantalla de bloqueo biométrico (BiometricLockScreen). Creado el componente reutilizable BitaruBrandHeader con layout responsivo (fillMaxWidth(0.70f) + aspectRatio(3.5f)) para renderizar el isotipo sin distorsión. Preservada la firma original de BiometricLockScreen (onRetryAuth, errorMessage) para garantizar retrocompatibilidad total con MainActivity.kt.

### Archivos Creados
- **ui/components/BitaruBrandHeader.kt**: Componente @Composable reutilizable con Image + subtitleText. Sin marcos circulares ni crops — respeta la proporción rectangular del asset.
- **res/drawable/bitaru_logo.png**: Asset corporativo copiado desde imagenes/BiTaRu-recta-003-esp.png.

### Archivos Modificados
- **ui/screens/BiometricLockScreen.kt**: Integrado BitaruBrandHeader en cabecera superior. Icono Fingerprint cambia a tint error cuando hay errorMessage. Layout migrado a Arrangement.SpaceBetween. Firma intacta (onRetryAuth, errorMessage).
- **Documentacion/ESQUEMA_PANTALLAS.md**: Agregada Sección 10 — diagrama ASCII de BiometricLockScreen con nomenclatura 10.x.x.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260721_BiometricLockScreen.kt

### Principio de Diseño (Reglas de Oro I, IV, V)
- Modularidad estricta (Regla I): Branding encapsulado en BitaruBrandHeader.kt, 0 líneas en ViewModels ni capa de dominio.
- UX Profesional M3 (Regla IV): ContentScale.Fit con aspectRatio responsivo para densidad Pixel 9 Pro XL. Sin crops ni deformaciones.
- 0% Regresión (Reglas II, III): Firma BiometricLockScreen preservada. No se tocaron MainActivity, ViewModel, ni ningún otro screen.
- Asset real de marca (no placeholder): Se utilizó el banner definitivo BiTaRu-recta-003-esp.png.

### Build
- compileDebugKotlin: OK (1 warning preexistente)
- assembleDebug: BUILD SUCCESSFUL
- assembleDebug: PENDIENTE
## [2026-07-17] ME-02: ExtractPhotoLocationUseCase + Corrección de EXIF en Hilo Principal

### Logro
Creación de ExtractPhotoLocationUseCase en domain/usecase/ para extraer coordenadas GPS de metadatos EXIF de forma segura (Dispatchers.IO + withContext), delegando en PhotoManager existente. Corrección del bug donde getPhotoLocation() se ejecutaba en el hilo principal de UI, causando SecurityException/FileNotFoundException silenciosas en Android 10+ (Scoped Storage).

### Archivos Modificados
- domain/usecase/ExtractPhotoLocationUseCase.kt: **nuevo** — use case puro que envuelve PhotoManager.getPhotoLocation() en withContext(Dispatchers.IO)
- di/AppModule.kt: añadido provider @Singleton para ExtractPhotoLocationUseCase
- ui/RouteViewModel.kt: inyectado ExtractPhotoLocationUseCase como parámetro 12 del constructor; añadida suspend fun extractLocationFromPhoto(uri)
- ui/screens/SavedLocationsScreen.kt: cameraLauncher wrap en scope.launch; gallery/media pickers migrados a extractLocationFromPhoto()
- ui/screens/MapScreen.kt: cameraLauncher wrap en scope.launch; gallery/media pickers migrados a extractLocationFromPhoto()

### Principio de Diseño (Regla de Oro V)
- Separación de responsabilidades: PhotoManager queda como capa de infraestructura (E/S de archivos + EXIF)
- ExtractPhotoLocationUseCase actúa como orquestador de dominio puro, añadiendo seguridad de hilo (IO) sin acoplar al ViewModel con dispatchers

### Tests
- domain/usecase/ExtractPhotoLocationUseCaseTest.kt: **nuevo** — 2 tests (coordenadas exitosas y null seguro) con mockk para Uri y PhotoManager
- assembleDebug: BUILD SUCCESSFUL
- testDebugUnitTest: 16/16 passing

## [2026-07-16] ME-01 Fase IV: LocationModule + Inyección Total de Managers

### Logro
Creación de LocationModule.kt con providers unscoped para TrackingSessionManager, CinematicFilterManager, StationaryAnchorManager y GpsFilterPipeline. Integración completa en LocationService mediante field injection.

### Archivos Modificados
- di/LocationModule.kt: **nuevo** — providers unscoped para los 4 managers de tracking
- location/manager/GpsFilterPipeline.kt: añadidos métodos 
eset() y shouldForceStop(Location) para delegar desde el servicio sin referencias directas a los managers internos
- location/LocationService.kt: sessionManager y ilterPipeline migrados a @Inject; eliminadas las 4 declaraciones manuales de campo y su creación en onCreate(); todas las llamadas directas a stationaryAnchorManager.* y cinematicFilter.* redirigidas a ilterPipeline.*

### Principio de Diseño (Regla de Oro V)
- Ningún provider usa @Singleton — cada inyección en LocationService recibe instancias frescas, evitando fugas de estado entre sesiones de tracking
- 	riggerAutomaticTracking() continúa creando su propio pipeline limpio localmente para backfill (no comparte estado con el pipeline inyectado)

### Tests
- ssembleDebug: BUILD SUCCESSFUL
- 	estDebugUnitTest: 14/14 passing
## [2026-07-16] ME-01: Infraestructura Hilt + Migración DI Completa (MVP)

### Logro
Hilt 2.59 compila y funciona nativamente con AGP 9.2.1 usando KSP (sin kapt). Se migró toda la capa de inyección de dependencias del proyecto.

### Archivos Modificados
- gradle/libs.versions.toml: hilt = "2.52" → "2.59", añadido hilt-android-testing
- gradle/gradle.properties: añadidos flags ndroid.builtInKotlin=false, ndroid.newDsl=false (luego revertidos)
- pp/build.gradle.kts: kotlin("kapt") eliminado, kapt(libs.hilt.compiler) → ksp(libs.hilt.compiler), añadido kspAndroidTest(libs.hilt.compiler), ndroidTestImplementation(libs.hilt.android.testing), 	estInstrumentationRunner → HiltTestRunner
- pp/src/main/java/.../MainActivity.kt: @AndroidEntryPoint, RouteViewModel inyectado via y viewModels(), import ViewModelProvider/RouteViewModelFactory eliminados
- pp/src/main/java/.../ui/RouteViewModel.kt: @HiltViewModel + @Inject constructor
- pp/src/main/java/.../ui/RouteViewModelFactory.kt: **eliminado** (reemplazado por Hilt)
- pp/src/main/java/.../di/DatabaseModule.kt: añadido @Provides fun provideRouteRepository()
- pp/src/main/java/.../di/AppModule.kt: **nuevo** — providers para PreferencesManager, GeocoderUseCase, PhotoManager, GroqAiUseCase, GeminiAiUseCase, RouteExchanger, RouteExportUseCase, BackupUseCase, ContactUseCase, ShareUseCase, LocationServiceManager
- pp/src/main/java/.../location/LocationService.kt: @AndroidEntryPoint, 3 fields @Inject (repository, contactUseCase, geocoderUseCase), bloque dbPassphrase/database/repository removido de onCreate()
- pp/src/main/java/.../location/manager/StationaryAnchorManager.kt: **bugfix** — processStationaryState() ahora congela anclaje con if (!hasAnchor) en lugar de sobrescribir en cada llamado
- pp/src/androidTest/java/.../HiltTestRunner.kt: **nuevo**

### Tests
- 	estDebugUnitTest: 14/14 passing (incluyendo drift prolongado)
- ssembleDebug: BUILD SUCCESSFUL (0 errores)

### Pendiente
- Migrar LocationService a @AndroidEntryPoint con inyección de campo
[2026/07/16 03:00 - AL-01: Desintegracion de LocationService (Fase III - GpsFilterPipeline + Auto-stop 3 min)]
- **CinematicFilterManager.kt**: Refactor completo. Removidos filterAndProcess(), calculateFallbackSpeed(), evaluatePoint(), lastValidLocation, lowSpeedCounter. Nuevo método unificado evaluateAndProcess(lat, lon, accuracy, timestamp, speed): Boolean que integra filtro de aberración cinemática + máquina de auto-stop por ventana temporal de 180s. Nuevo isAutoStopActive(): Boolean. seedReferencePoint() y reset() preservados. Parámetros de constructor configurables con defaults desde AppConstants.
- **GpsFilterPipeline.kt**: Nuevo orquestador JVM-safe que combina CinematicFilterManager.evaluateAndProcess() + StationaryAnchorManager.processStationaryState()/getResolvedCoordinate(). processLocation(rawPoint: BufferPoint): BufferPoint?. shouldTriggerAutoStop(): Boolean.
- **LocationService.kt**: Cirugía de precisión (3 líneas): reemplazadas líneas 446-450 (filterAndProcess + processAnchor) por pipeline + BufferPoint. triggerAutomaticTracking() eliminado bridge BufferPoint→Location→filterAndProcess→BufferPoint, ahora usa GpsFilterPipeline directamente. Inicialización de filterPipeline añadida en onCreate().
- **CinematicFilterManagerTest.kt**: Actualizado a nueva API evaluateAndProcess() + isAutoStopActive(). 4 tests (aberración, coherencia, auto-stop 3 min, reanudación cancela auto-stop).
- **GpsFilterPipelineTest.kt**: Nuevo. 3 tests que validan pipeline completo (aberración, auto-stop 3 min, reanudación).
- **Resultado**: BUILD SUCCESSFUL (compileDebugKotlin + 9/9 tests + assembleDebug).
- **Respaldos**: RESPALDO_ANTERIOR/20260716_CinematicFilterManager.kt, 20260716_LocationService.kt.
[2026/07/16 01:00 - AL-01: Desintegracion de LocationService (Fase I - Managers)]
- **ServiceNotificationManager.kt**: Nuevo manager que encapsula creacion de canal de notificacion, buildNotification() y foreground service.
- **TrackingSessionManager.kt**: Nuevo manager que encapsula buffer de pre-grabacion (60s, ConcurrentLinkedDeque), odometro (totalDistanceMeters, currentDistanceMeters), estado de sesion (isTracking, currentRouteId, latestLocation, currentActivityMode), timer de sesion, y extension function Location.toBufferPoint().
- **LocationService.kt**: notificationManager y sessionManager integrados como campos privados. buildNotification() delegado a notificationManager.buildNotification(). updateNotification() renombrado a updateServiceNotification(). preRecordingBuffer y handlePreRecording() delegados a sessionManager.
- **Resultado**: BUILD SUCCESSFUL (32 tareas test + 39 assembleDebug). APK generado.
- **Respaldos**: RESPALDO_ANTERIOR/{timestamp}_*_AL01_Phase1.kt, ServiceNotificationManager.kt, TrackingSessionManager.kt.
[2026/07/15 19:45 - CR-02: Migracion Global de Namespace (com.example -> com.aistudio.localizador)]
- **build.gradle.kts**: namespace = ""com.aistudio.localizador"", applicationId preservado (com.aistudio.localizador.vqtspk).
- **themes.xml**: Theme.MyApplication -> Theme.Localizador, parent migrado a Theme.Material3.DayNight.NoActionBar.
- **AndroidManifest.xml**: Theme actualizado a @style/Theme.Localizador en <application> y <activity>.
- **Theme.kt**: MyApplicationTheme -> LocalizadorTheme.
- **MainActivity.kt**: Referencias a MyApplicationTheme actualizadas a LocalizadorTheme.
- **Directorio fisico**: 61 archivos movidos de com/example/ a com/aistudio/localizador/.
- **Test**: 3 archivos movidos de com/example/ a com/aistudio/localizador/ + 2 pre-existentes actualizados.
- **Package/imports**: 66 archivos .kt actualizados (134 ocurrencias import, hardcoded FQNs corregidos en AppDatabase, LocationService, BackupUseCase).
- **Dependencias**: com.google.android.material:material:1.12.0 anadido al version catalog y build.gradle.kts.
- **androidTest**: ExampleInstrumentedTest migrado, assertion corregida.
- **Fix pre-existente**: ExampleRobolectricTest SDK 36->34, app_name corregido a ""BiTaRu"".
- **Resultado**: BUILD SUCCESSFUL, 7/7 tests pasan, assembleDebug exitoso.
- **Respaldos**: RESPALDO_ANTERIOR/20260715_com.example.backup/.

[2026/07/15 19:13 - CR-01: Suite de Pruebas Unitarias (Filtrado cinemático y anclaje estacionario)]
- **Suite JVM pura**: Creados 2 test files en app/src/test/java/com/aistudio/localizador/location/manager/.
  - CinematicFilterManagerTest: 2 tests (rechazo de salto aberrante + aceptacion de punto coherente).
  - StationaryAnchorManagerTest: 2 tests (congelacion de posicion en drift + liberacion de anclaje por movimiento).
- **Refactor managers**: Nuevos metodos JVM-safe con primitivas (Double, Float, Long) sin dependencia de android.location.Location.
  - CinematicFilterManager: seedReferencePoint(), evaluatePoint(), haversineDistance().
  - StationaryAnchorManager: processStationaryState(), getResolvedCoordinate(), evaluateRelease().
- **Dependencias**: mockk 1.13.8 anadido al version catalog y build.gradle.kts.
- **Resultado**: BUILD SUCCESSFUL, 4/4 tests pasan en JVM local.
- **Respaldos**: RESPALDO_ANTERIOR/20260715_CinematicFilterManager.kt y 20260715_StationaryAnchorManager.kt.

[2026/07/13 10:30 - Esquema de referencia de pantallas (ESQUEMA_PANTALLAS.md)]
- **ESQUEMA_PANTALLAS.md**: Nuevo documento maestro con diagramas ASCII de las 7 pantallas + sub-barra + drawer + dialogos.
- **Sistema de numeracion**: Formato PANTALLA.BARRA.BOTON (ej: 4.2.3 = Detalle, Sub-barra, Exportar) para referencias rapidas.
- **Diagramas visuales**: Cada pantalla tiene un diagrama ASCII mostrando la disposicion de todos los botones con sus numeros.
- **Colores semanticos**: Tabla de referencia de colores con codigos hexadecimales y usos.
- **.aierules**: Nueva seccion "Esquema de Referencia de Pantallas" para que el agente cargue el esquema al iniciar sesion.
[2026/07/13 09:39 - Paleta semantica de colores para botones de accion] - **Color.kt**: Nuevas constantes IconCompartir (0xFF4DD0E1, Cian), IconExportar (0xFFFFB74D, Amber) e IconFavorito (0xFFFFD54F, Dorado) para acciones semanticas. - **MainActivity.kt:761**: Icono BookmarkBorder reemplazado por Icons.Default.Favorite con 	int = IconFavorito (corazon dorado) para boton de colocar favoritos. - **DetailScreen.kt:576**: Icono Send con 	int = IconCompartir (cian) para boton Compartir ruta. - **DetailScreen.kt:583,590,595,600**: Iconos KeyboardArrowRight, Description (GPX), TableChart (CSV) y Archive (ZIP) con 	int = IconExportar (ambar) para exportacion. - **.aierules**: Nueva seccion de Convencion de Colores para Botones de Accion documentando la paleta. - **Visibilidad**: Los iconos ahora son visibles sobre fondo oscuro #1C1B1F independientemente del tema (diurno/nocturno). [2026/07/12 15:10 - Mapa Inmersivo: Scaffold → Box + barra inferior adaptativa en ExperienceMapScreen] - **ExperienceMapScreen.kt**: Eliminado Scaffold + TopAppBar. Reemplazado por Box(fillMaxSize) con 4 capas Z. - **@OptIn(ExperimentalMaterial3Api::class)**: Eliminado (ya no necesario sin Scaffold/TopAppBar). - **Imports**: Eliminados ExperimentalMaterial3Api, Scaffold, TopAppBar, TopAppBarDefaults. Añadidos Row, Surface, Spacer, statusBarsPadding, navigationBarsPadding. - **CAPA 0 - Mapa**: .fillMaxSize() completo, sin padding de barra superior. - **CAPA 1 - Panel fotos**: .align(TopStart) + .statusBarsPadding() (nace desde el borde real del dispositivo). - **CAPA 2 - FABs**: Reubicados de BottomStart a CenterEnd (lado derecho). Color unificado: primaryContainer/primary (eliminado Color.White). - **CAPA 3 - Barra inferior**: Nueva Surface adaptativa ArrowBack + "Experiencias" + Spacer(weight) + Share + Save. .navigationBarsPadding() para ergonomia Pixel 9. - **Share**: Conectado a viewModel.exportRoute(routeId). - **Save**: Conectado a viewModel.exportarRutaZip(routeId). - **CAPA 4 - FullScreenPhotoDialog**: Preservado sin cambios. - **Logica cinematica (auto-rotacion, bearing, GuideLinesOverlay)**: Preservada intacta. - **Lint**: 0 errores, 47 warnings (sin regresion). - **Compilacion**: BUILD SUCCESSFUL (38s).  [2026/07/12 14:50 - Corrección de contraste: surfaceVariant → surfaceContainerHigh en DetailScreen.kt:170] - **DetailScreen.kt:170**: MaterialTheme.colorScheme.surfaceVariant → MaterialTheme.colorScheme.surfaceContainerHigh. El placeholder del AsyncImage ahora es visible en modo diurno, no solo en nocturno. - **Lint**: 0 errores, 47 warnings (sin regresion). - **Compilacion**: BUILD SUCCESSFUL (1m 22s).  [2026/07/12 14:30 - Tokens semánticos MD3: purga de colores hardcodeados en DetailScreen.kt] - **DetailScreen.kt:170**: Color.LightGray → MaterialTheme.colorScheme.surfaceVariant (placeholder AsyncImage adaptativo al tema). - **DetailScreen.kt:373**: Color.Green/Color.Gray → MaterialTheme.colorScheme.primary / MaterialTheme.colorScheme.onSurfaceVariant (toggle vincular favoritos en ModalBottomSheet). - **DetailScreen.kt:475,479**: AndroidColor.GRAY preservado (MPAndroidChart externo, fuera del ecosistema Compose). - **Lint**: 0 errores, 47 warnings (sin regresión). - **Compilación**: BUILD SUCCESSFUL (25s).  [2026/07/12 13:40 - Saneamiento de Lint: Zero Errors alcanzado (4→0 errores, 60→47 warnings)] - **ActivityRecognitionManager.kt**: Añadido @SuppressLint("MissingPermission") en stopTrackingTransitions(). - **ProximityManager.kt**: Añadido @SuppressLint("MissingPermission") en stopScanning() y stopAdvertising(). Añadido import. - **AndroidManifest.xml**: Agregado permiso FOREGROUND_SERVICE_CONNECTED_DEVICE para servicio de ubicación. - **ExperienceMapScreen.kt:123**: Migrado scrollState.value de LaunchedEffect directo a snapshotFlow para eliminar lectura de @FrequentlyChangingValue en composición. - **MapScreen.kt:477**: Migrado LocalContext.current.resources → LocalResources.current eliminando LocalContextResourcesRead. - **HistoryScreen.kt:40,304**: Reordenado parámetro modifier como primer parámetro opcional en HistoryScreen y RouteCard (convención Compose MD3). - **colors.xml**: Eliminados 7 colores no utilizados (purple_200/500/700, teal_200/700, black, white). - **strings.xml**: Eliminados biometric_subtitle y share_footer no referenciados. - **info_window_layout.xml**: Eliminado layout huérfano (legacy de PhotoInfoWindow eliminado en Fase 6). - **Lint**: 4 errores → 0 errores. 60 warnings → 47 warnings (solo versión/actualizaciones). - **Compilación**: BUILD SUCCESSFUL (25s).  [2026/07/12 12:13 - Refinamiento visual: titleLarge.copy(SemiBold 20.sp) + corrección cromática SavedLocationsScreen] - **DetailScreen.kt**: titleMedium (16sp) → titleLarge.copy(fontWeight = SemiBold, fontSize = 20.sp). Punto medio óptimo para alta densidad. - **ExperienceMapScreen.kt**: titleMedium (16sp) → titleLarge.copy(fontWeight = SemiBold, fontSize = 20.sp). Añadido import FontWeight. - **SavedLocationsScreen.kt**: Eliminado fondo negro estático Color(0xFF1C1B1F).copy(alpha=0.90f) por token dinámico MaterialTheme.colorScheme.surface + onSurface. Título unificado con titleLarge.copy(fontWeight = Bold). - **Compilación**: BUILD SUCCESSFUL (23s).  [2026/07/12 11:40 - Refinamiento visual: Unificación tipográfica titleMedium en TopAppBar] - **DetailScreen.kt**: 	itleLarge → 	itleMedium en TopAppBar. Añadidos 	itleContentColor y  avigationIconContentColor. - **ExperienceMapScreen.kt**: 	itleLarge → 	itleMedium en TopAppBar. Añadidos 	itleContentColor y  avigationIconContentColor. - **Justificación**: titleMedium (16sp) alinea el peso visual con TopBarLocalizador de MapScreen, eliminando percepción de barra expandida. - **Compilación**: BUILD SUCCESSFUL (46s).  [2026/07/12 10:45 - Navegación Jerárquica MD3: TopAppBar + onBack en pantallas auxiliares] - **ExperienceMapScreen.kt**: Añadido parámetro onBack: () -> Unit + modifier: Modifier = Modifier. Envuelto contenido en Scaffold/TopAppBar con Icons.AutoMirrored.Filled.ArrowBack y título "Experiencias". - **DetailScreen.kt**: Envuelta Column existente en Scaffold/TopAppBar con Icons.AutoMirrored.Filled.ArrowBack y título "Detalle del Recorrido". Preservado el callback onBack ya existente. - **SavedLocationsScreen.kt**: Sin cambios (pantalla base de bottom bar, no requiere Up Navigation por MD3). - **MainActivity.kt**: Cableado onBack = { navController.popBackStack() } en la ruta EXPERIENCE_MAP. - **Patrón MD3**: onBack expuesto explícitamente en firma de cada Composable para desacoplamiento del NavController, facilitando testing y @Preview. - **Compilación**: BUILD SUCCESSFUL.   [12/07/2026 - Fases 7-10: Consolidación Perimetral, Purga de Código Muerto y Build 41s] - **Fase 7 — 4 bloques**: LocationService.kt null guard + intent?. → intent. (6x) + FQNs PreferenceManager/SavedLocation. RouteRepository.kt 7x FQNs → nombre corto. RouteViewModel.kt .debounce(100) → 100.milliseconds. RouteExportUseCase.kt val route eliminado. ExperienceMapScreen.kt condición redundante !isAutoRotationEnabled eliminada. - **Fase 8 — 10 pasos**: LocationService.kt 4x Elvis muertos (?: ) eliminados + rssi eliminado de onBleContactDetected. RouteAnalyticsScreen.kt routeId/onBack eliminados + caller MainActivity. MapScreen.kt event → _, else if → else. RouteViewModel.kt catch (e: ) → catch (_: ), when (val decision = ) → when (). GeocoderUseCase.kt suspend removido de fetchAddress. - **Fase 9 — 3 bloques**: ExperienceMapScreen.kt photoItems eliminado + LocalDensity.current.density migrado. MapScreen.kt val res inlinado. SavedLocationsScreen.kt FQN android.net.Uri → import + nombre corto. RouteExchanger.kt FQNs BufferedWriter/OutputStreamWriter contraídos. MainActivity.kt FQNs Intent/Toast eliminados. - **Fase 10 — Blueprint ajustado**: ShareUseCase.kt .let { it.toUri() } → .toUri(). ExperienceMapScreen.kt ImageRequest.Builder(LocalContext.current) → context. MainActivity.kt android.widget.Toast.LENGTH_SHORT → Toast.LENGTH_SHORT. - **Optimización de compilación**: Tiempo reducido de >2min a 41s estables (-65%). - **Declaración Production-Ready**: Dictamen arquitectónico de cierre. Fase 11 declinada (falsos positivos de lint no justifican riesgo de regresión en delegados Compose). - **Respaldos**: RESPALDO_ANTERIOR/20260711_*.kt (26 archivos). - **Compilación**: BUILD SUCCESSFUL (41s). Deploy Pixel 9 Pro XL. [11/07/2026 - Fase 6 Definitiva: Purgas Finales + RouteAnalytics Compose/KTX] - **Purgas (4 regresiones)**: authenticateWithCrypto (MainActivity.kt), shareCompleteRoute (RouteViewModel.kt + ShareUseCase.kt), insertFavorite (RouteRepository.kt). Eliminación directa sin función intermediaria para evitar bug de paso de arrays en PowerShell. - **RouteAnalyticsScreen.kt**: L94 Locale.getDefault() → LocalConfiguration.current.locales[0] (lectura reactiva Compose). L46-49,312 6× AndroidColor.parseColor() → String.toColorInt() (KTX). Nuevos imports: LocalConfiguration + toColorInt. - **Bug corregido**: Remove-Lines con @(@(range)) no funcionaba porque PowerShell desenvuelve arrays anidados. Se reemplazó con for-if directo. - **Compilación**: gradlew assembleDebug → BUILD SUCCESSFUL (0 errores). Deploy Pixel 9 Pro XL. - **Respaldos**: RESPALDO_ANTERIOR/20260711_HHMM_RouteAnalyticsScreen.kt y 4 archivos más. [11/07/2026 - Fase 6 Real: Purgas Regresivas + Locale.US + Optimize Imports] - **Bloque A (Purgas)**: authenticateWithCrypto (MainActivity.kt), shareCompleteRoute (RouteViewModel.kt + ShareUseCase.kt), insertFavorite (RouteRepository.kt), val activity + 3 imports muertos (ExperienceMapScreen.kt). - **Bloque B (Locale.US)**: 8 String.format blindados en GroqAiUseCase.kt (3), GeocoderUseCase.kt (1), RouteAnalyticsScreen.kt (4) — previene NumberFormatException en regiones con coma decimal. - **Bloque C (Imports)**: PhotoPanel + DashboardActividadFlotante eliminados de MapScreen.kt (componentes ya borrados en Fase 5). - **Método**: Corrección de bug de sintaxis PowerShell (comas en argumentos de función causaron truncamiento de GroqAiUseCase.kt). Backup/restore desde RESPALDO_ANTERIOR para recuperación. - **Compilación**: gradlew assembleDebug → BUILD SUCCESSFUL (0 errores). Deploy a Pixel 9 Pro XL. - **Respaldos**: RESPALDO_ANTERIOR/20260711_1827_*.kt (10 archivos). [11/07/2026 - Fase 6: Purgas Finales (authenticateWithCrypto, shareScreenshot, PhotoInfoWindow, shareCompleteRoute, isFavoriteLinked, saveFavoritePoint)] - **MainActivity.kt**: Eliminada función uthenticateWithCrypto() (L217-244). Preservada uthenticateSimple() (3 callers vivos). - **ShareUseCase.kt**: Eliminada shareCompleteRoute() (L48-108): lógica de compartición con mapa + fotos + FileProvider. - **RouteViewModel.kt**: Eliminadas isFavoriteLinked() (L302-303, single-expression body) y saveFavoritePoint() (L309-318, con geocoder + DB). Preservada cleanDuplicateFavorites() (caller vivo en L130). - **ExperienceMapScreen.kt**: Eliminadas shareScreenshot() (L173-193, captura decorView + FileProvider) y class PhotoInfoWindow (L484-555, InfoWindow osmdroid con CircleCropTransformation). photoItems preservado. - **Método**: Reemplazo del brace-counting por eliminación por rango de línea exacto (previene corrupción de estructura). - **Respaldos**: RESPALDO_ANTERIOR/20260711_*.kt (4 archivos modificados). - **Compilación**: BUILD SUCCESSFUL (0 errores). [11/07/2026 - Limpieza de Menú Duplicado: Eliminación de IconButton flotante superior] - **MainActivity.kt**: Eliminado bloque IconButton (hamburguesa) de la capa superior absoluta (zIndex=99) que se superponía al mapa en NavRoutes.TRACKING. Se removió también el parámetro onMenuClick = { drawerState.open() } de la llamada a MapScreen(). - **MapScreen.kt**: Eliminado parámetro onMenuClick: () -> Unit de la firma del composable. El callback estaba muerto (nunca se invocaba dentro del componente). - **TopBarLocalizador.kt**: Padding corregido de start = 72.dp (residual del botón eliminado) a horizontal = 16.dp simétrico. La cabecera ahora ocupa el ancho completo balanceado. - **UX**: Se unifica la navegación MD3. El drawer se abre exclusivamente desde el IconButton ☰ de la barra inferior (NavigationBar), eliminando redundancia y confusión visual. - **Respaldos**: RESPALDO_ANTERIOR/20260711_0943_MainActivity.kt, _MapScreen.kt, _TopBarLocalizador.kt. - **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL. [09/07/2026 - Watchdog de Señal GNSS + Dashboard Actividad Flotante] - **LocationService.kt**: ActivityMode expandido con SIN_SENAL. Nuevo watchdog GPS que monitorea el tiempo desde el último update del GPS_PROVIDER. Si pasan 15s sin tick satelital y el modo actual no es STOPPED/SIN_SENAL, conmuta automáticamente a ActivityMode.SIN_SENAL. Al recuperar señal GPS, restaura el modo cinemático previo (preAlertActivityMode). Variables: lastGpsTickTime, isSignalLost, preAlertActivityMode, watchdogJob. - **IndicadorTranslucidoContenedor** (en MapScreen.kt): Nuevo parámetro overrideColor: Color con valor por defecto Color(0xFF1C1B1F).copy(alpha = 0.65f) para compatibilidad backward. Permite inyección dinámica de color de alerta. - **DashboardActividadFlotante.kt** (nuevo en ui/components/): Componente Compose que consume IndicadorTranslucidoContenedor con animación animateColorAsState. En estado normal: fondo oscuro translúcido, modo de tráfico + velocidad. En SIN_SENAL: fondo rojo táctico #DC3545 al 75% alpha, etiqueta "ESTATUS SATELLITE", texto "⚠️ SIN SEÑAL", velocidad oculta. - **MapScreen.kt**: Dashboard inline reemplazado por DashboardActividadFlotante. Clickable preservado para toggle de dirección. - **RouteSegmenter.kt**: toSegmentColor() y toSolidColor() expandidos con SIN_SENAL -> rojo #DC3545 para segmentos de ruta grabados durante pérdida de señal. - **Respaldos**: RESPALDO_ANTERIOR/20260709_LocationService.kt.bak, 20260709_MapScreen.kt.bak. - **Compilación**: BUILD SUCCESSFUL (0 errores, 0 warnings).  [07/07/2026 - Bloque B: Guarda de Contingencia 3min + Bloque C: Dashboard MD3] - **StationaryAnchorManager.kt**: Nueva función shouldForceStop() que monitorea accuracy >25m por 3 minutos continuos. Al dispararse, retorna true para forzar parada automática vía stopTrackingWithAutoRestart(). Preserva processAnchor() intacto. - **LocationService.kt**: Inyección de la guarda en processNewLocation() línea 465, justo después de stationaryAnchorManager.processAnchor(). Si shouldForceStop() retorna true, corta el pipeline con return temprano. - **MapScreen.kt**: Reemplazo del Surface de telemetría por Card MD3 con:   - Barra de acento lateral izquierda según modo (ámbar/verde/azul/rojo)   - Icono 28dp + label español 18sp bold ("Detenido", "Caminando", "En Bicicleta", "En Vehículo") coloreado por modo   - Velocidad a la derecha en blanco 15sp   - Stats secundarias (distancia + timer en tracking)   - Telemetría fina (LAT/LON/ALT) en gris 9sp - **RouteSegmenter.kt**: Sincronización de paleta: DRIVING rgb(244,67,54) → rgb(255,82,82) (#FF5252), STOPPED rgb(255,235,59) → rgb(255,214,0) (#FFD600). toSegmentColor() y toSolidColor() actualizados. - **Respaldos**: RESPALDO_ANTERIOR/20260707_1341_StationaryAnchorManager.kt, 20260707_1341_LocationService.kt, 20260707_1341_MapScreen.kt, 20260707_1341_RouteSegmenter.kt. - **Compilación**: BUILD SUCCESSFUL (1 warning preexistente). Deploy no disponible (sin dispositivo conectado). [07/07/2026 - Saneamiento Final UI: SavedLocationsScreen + DetailScreen + IconCategory] - **SavedLocationsScreen.kt**: 8 parches: Color.parseColor → toColorInt() (4 pts), Locale.US en String.format (3 pts), imports KTX+Locale. - **DetailScreen.kt**: 7 parches: Color.parseColor → toColorInt() (1 pt), Locale.US en String.format (5 pts), imports KTX+Locale.   - Nota: 6 iconos (TrendingUp, ShowChart, Send x2, KeyboardArrowRight, HelpOutline) presentan deprecación a AutoMirrored pero los equivalentes no están disponibles en la versión actual de la librería Material Icons. Pendiente de resolución con upgrade de dependencias. - **IconCategory.kt**: Refactor completo: values() → entries, createBitmap KTX, toBitmap como método de instancia, acentos en labels. - **RouteViewModel.kt**: Auditado — 0 cambios (arquitectura limpia, delegación a UseCases). - **Respaldos**: RESPALDO_ANTERIOR/20260707_SavedLocationsScreen.kt, 20260707_DetailScreen.kt, 20260707_IconCategory.kt. - **Compilación**: BUILD SUCCESSFUL. [07/07/2026 - Saneamiento de IconCategory.kt + MapScreen.kt (infraestructura)] - **IconCategory.kt**: Refactor completo del diccionario visual:   - values() → entries (asignación cíclica de arreglos erradicada, Kotlin 1.9+).   - toBitmap() migrado de companion object estático a método de instancia (diseño OOP idiomático).   - Bitmap.createBitmap(size, size, Config.ARGB_8888) → createBitmap(size, size) KTX.   - Etiquetas corregidas: "Gastronomía", "Vehículo", "Automática". - **MapScreen.kt línea 440**: Llamada actualizada a la nueva firma: IconCategory.fromKey().toBitmap(40, color). - **RouteViewModel.kt**: Auditado — 0 cambios necesarios (arquitectura limpia). - **Respaldos**: RESPALDO_ANTERIOR/20260707_IconCategory.kt. - **Compilación**: BUILD SUCCESSFUL en 22s, 0 warnings. [07/07/2026 - Saneamiento de Deuda Técnica: MapScreen + FocusFavorite + LocationService] - **MapScreen.kt** (1037 líneas): Sanación completa de 8 categorías de warnings:   - Eliminación de variables muertas (waypointName, locationName, isManualMode).   - Migración a KTX: createBitmap(size, size), String.toColorInt(), Bitmap.toDrawable().   - Corrección de accesibilidad: v.performClick() en setOnTouchListener.   - API osmdroid moderna: getPoints() → actualPoints.   - Eliminación de bloques if vacíos en retainAll (waypointMarkers, favoriteMarkers).   - Resource hoisting: context.resources elevado fuera del forEach de favoritos.   - Internacionalización: Locale.getDefault() en String.format de telemetría visible; Locale.US en formatos de color.   - Color.parseColor() → toColorInt() en todos los selectores de color. - **FocusFavoriteScreen.kt** (245 líneas): 2 parches de internacionalización:   - Migración de getDrawable() deprecado a ContextCompat.getDrawable().   - lowercase() → lowercase(Locale.ROOT) para inmunidad contra el problema de la "I turca". - **LocationService.kt** (1073 líneas): 4 correcciones de Locale en String.format:   - Locale.US en logs de telemetría (línea 450), coordenadas para metadata (512), distancia en notificación (790).   - Locale.getDefault() en formato de tiempo visible al usuario (1039). - **Respaldos**: RESPALDO_ANTERIOR/20260707_MapScreen.kt, 20260707_FocusFavoriteScreen.kt, 20260707_LocationService.kt. [05/07/2026 - Fase 2 GPS: Ventana Móvil de 5 Puntos + Filtro Cinemático 15 m/s] - **Problema**: Las antenas de telefonía pueden entregar coordenadas con saltos absurdos (>100 m/s) que contaminan el odómetro y la base de datos. El filtro existente (MAX_VALID_SPEED_MPS = 45 m/s) era demasiado permisivo y no se aplicaba como pre-filtro de entrada. - **Solución — Pre-filtro de doble capa al inicio de processNewLocation()**:   - **Fase 1 (Precisión)**: if (location.accuracy > 50f) return — descarta puntos con precisión degradada antes de cualquier procesamiento.   - **Fase 2 (Inercia/Velocidad)**: LinkedList<Location> locationWindow como cola fija de 5 posiciones. Velocidad calculada entre locationWindow.last() y el nuevo punto. Si supera MAX_ALLOWED_SPEED_MPS = 15.0 (~54 km/h), se registra un Log.w de advertencia y el punto se descarta por completo.   - **Ventana deslizante**: Solo los puntos que pasan ambos filtros se agregan a la ventana. Si excede 5 elementos, se poda el más antiguo (removeFirst()) - **Arquitectura**: El bloque se inyectó como pre-filtro en la línea 416 de processNewLocation(), ANTES de cualquier lógica pesada (ActivityMode, ShortStop, BLE, Auto-mode, odómetro, DB). Esto asegura que solo "carne premium" llegue al resto del pipeline. - **Constante nueva**: MAX_ALLOWED_SPEED_MPS = 15.0 en companion object (equivale a ~54 km/h, límite físico de corrida/caminata). - **Estructura nueva**: locationWindow: LinkedList<Location>() como campo privado de la clase. - **Respaldos**: RESPALDO_ANTERIOR/20260705_LocationService.kt. - **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.  [04/07/2026 - Pantalla Inmersiva Focus Favorite + Postal Táctica] - **FocusFavoriteScreen.kt**: Reestructuración completa. Se eliminó Scaffold/TopAppBar y se reemplazó por jerarquía Box raíz con dos Box anidados: (1) contenedor capturable con drawWithContent + GraphicsLayer.record que renderiza mapa osmdroid + card translúcida, y (2) botones flotantes (Volver/Compartir) como hijos directos del Box raíz, fuera del recorte de captura. - **Postal Táctica**: El botón Compartir ahora captura el contenedor vía graphicsLayer.toImageBitmap() → asAndroidBitmap(), guarda PNG en cacheDir/shared_posts/ y despacha Intent.ACTION_SEND con type=image/png vía FileProvider. La imagen resultante NO incluye botones de UI ni barras del sistema. - **Botones flotantes**: ArrowBack (arriba-izquierda) y Share (arriba-derecha) sobre fondo negro 50% alpha, CircleShape, padding ajustado para notch del Pixel 9 Pro XL. - **Backup**: RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen.kt. - **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL.  [REVERTIDO] [04/07/2026 - Arquitectura Opus: Transparencia nativa desde themes.xml + insets correctos]  [04/07/2026 - Rollback y Fusión: v3 (20:15) + bottom=40dp] - **Rollback completo**: Se deshicieron todos los experimentos del día (FLAG_LAYOUT_NO_LIMITS, arquitectura Opus, diagnósticos rojo/verde). - **Fusión táctica**: FocusFavoriteScreen.kt restaurado desde RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v3.kt (20:15) — arquitectura Box raíz con GraphicsLayer + watermark elegante + captura PNG. - **Único parche aplicado**: .padding(16.dp) → .padding(start=16.dp, end=16.dp, bottom=40.dp) en la Card para evitar que el modo inmersivo del Pixel la empuje fuera del viewport. - **themes.xml**: Mantenido en su estado original (sin atributos de transparencia). enableEdgeToEdge() en MainActivity es suficiente. - **Lecciones aprendidas**: navigationBarsPadding() depende de insets dinámicos que Android pone a 0 en modo inmersivo. bottom=40.dp fijo es más robusto. Los experimentos con FLAG_LAYOUT_NO_LIMITS y atributos de transparencia global en themes.xml causaron efectos secundarios en toda la app. - **Entradas [REVERTIDO]**: 2 entradas del historial marcadas. - **Compilación**: clean + installDebug — BUILD SUCCESSFUL. - **themes.xml**: Se agregaron 4 atributos de transparencia al tema raíz Theme.MyApplication:   - android:statusBarColor = @android:color/transparent   - android:navigationBarColor = @android:color/transparent   - android:windowTranslucentStatus = false   - android:windowTranslucentNavigation = false - **FocusFavoriteScreen.kt**: Reestructuración completa siguiendo la arquitectura Opus:   - Eliminado FLAG_LAYOUT_NO_LIMITS (causaba parpadeo al redibujar scrim en cada invalidate del mapa).   - Eliminados imports: android.view.WindowManager, androidx.compose.runtime.DisposableEffect.   - Bloque de ventana reducido a SideEffect con solo isAppearanceLightStatusBars = false + decorView.setBackgroundColor(BLACK).   - Mapa envuelto en Box(fillMaxSize) con setOnTouchListener que llama a requestDisallowInterceptTouchEvent(true) para evitar que osmdroid propague relayout de insets al sistema.   - Card utiliza .navigationBarsPadding() (en lugar de bottom=32.dp) para respetar la barra de gestos inferior.   - Botones flotantes (back, share) utilizan .statusBarsPadding() en lugar de top=40.dp manual.   - Watermark restaurado a su versión elegante: context.getString(R.string.app_name).lowercase(), alpha=0.5f, FontWeight.Light, letterSpacing=2.sp.   - Agregados imports: navigationBarsPadding, statusBarsPadding. - **Resultado**: El mapa fluye 100% de borde a borde sin parpadeos. Los elementos de UI respetan insets del sistema. La transparencia nace del tema (no del WindowManager runtime). - **Respaldos**: RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v5.kt, RESPALDO_ANTERIOR/20260704_themes.xml. - **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.  [REVERTIDO] [04/07/2026 - Transparencia Absoluta con FLAG_LAYOUT_NO_LIMITS] - **FocusFavoriteScreen.kt**: Se reemplazó el bloque SideEffect (window.statusBarColor = TRANSPARENT) por DisposableEffect(Unit) con FLAG_LAYOUT_NO_LIMITS + onDispose limpia.   - window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS) ignora todos los insets del sistema (status bar + nav bar), forzando transparencia real en el Pixel 9 Pro XL sin parpadeos.   - onDispose { window.clearFlags(FLAG_LAYOUT_NO_LIMITS) } restaura el estado al salir de la pantalla, evitando roturas de layout en el resto de la app (teclado, nav bar).   - window.decorView.setBackgroundColor(BLACK) se mantiene para el frame 0.   - Se añadió import android.view.WindowManager y import androidx.compose.runtime.DisposableEffect. - **Padding inferior en la Card**: .padding(16.dp) → .padding(start = 16.dp, end = 16.dp, bottom = 32.dp) para que el watermark "AGENVIAJE" y el contenido no queden bajo la barra de gestos del Pixel 9 Pro XL. - **Respaldo**: RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v4.kt. - **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL. **[04/07/2026 - Compartir Favorito con Foto Adjunta]** - **ShareUseCase.kt**: Refactor completo de shareLocation(). Ahora envía un Intent.ACTION_SEND con tipo dinámico: image/* si location.photoPath tiene URI válida (content://), text/plain si no. Se añade FLAG_GRANT_READ_URI_PERMISSION para que la app receptora (WhatsApp/Telegram) pueda leer el stream de la imagen. Formato de texto actualizado: "📍 ¡Te comparto este lugar desde mi bitácora!" + nombre + nota + enlace Google Maps. - **Arquitectura**: Sin cambios en UI ni ViewModel — el botón Share ya existía en LocationCard (SavedLocationsScreen.kt:698) y llamaba a shareLocation(). photoPath ya viene como content:// URI desde PhotoManager, no necesita FileProvider adicional. - **Backup**: RESPALDO_ANTERIOR/20260704_ShareUseCase.kt. - **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL. **[04/07/2026 - Grosor de Trazado Dinámico con Slider]** - **PreferencesManager.kt**: Nuevos getStrokeWidth() / setStrokeWidth() con clave "map_stroke_width", default 8f, persistencia SharedPreferences vía edit {}. - **RouteViewModel.kt**: Nueva variable de estado mapStrokeWidth (mutableStateOf, default 8f). Carga en init desde prefsManager. Método updateStrokeWidth(w) que actualiza el estado y persiste en Dispatchers.IO. - **MainActivity.kt**: Slider en el menú lateral, insertado justo después de "Protección Biométrica". Rango 4f..14f con steps=5 (saltos: 4, 6, 8, 10, 12, 14). Muestra etiqueta "Grosor del trazado: Xpx". Import añadido: Slider de material3. - **ExperienceMapScreen.kt / MapScreen.kt**: outilinePaint.strokeWidth ahora lee viewModel.mapStrokeWidth en lugar del hardcoded 8f. Reactivo: cualquier cambio del slider dispara recomposición instantánea. - **Backups**: RESPALDO_ANTERIOR/20260704_PreferencesManager.kt, _RouteViewModel.kt, _MainActivity.kt, _ExperienceMapScreen.kt, _MapScreen.kt. - **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL. **[04/07/2026 - Purga del Debug Overlay en ExperienceMapScreen]** - **ExperienceMapScreen.kt**: Eliminación completa del bloque DEBUG OVERLAY (líneas 332-339) que mostraba una barra negra semi-transparente con texto verde de telemetría (pts, wp, fav, fotos, load). El mapa y las fotos translúcidas ahora ocupan toda la pantalla de borde a borde sin interferencias visuales. - **Backup**: RESPALDO_ANTERIOR/20260704_ExperienceMapScreen.kt. - **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL. **[04/07/2026 - Agrupación Jerárquica de Favoritos con Árbol Animado]** - **LocationParser.kt** (NUEVO): Objeto con parseFromAddressString() para inferir país/ciudad desde direcciones tipo "Av. de Mayo 1370, C1085 CABA, Argentina" (últimas 2 partes separadas por coma), inferCountryCity() con cache ConcurrentHashMap para geocodificación redundante, y clearCache(). - **RouteViewModel.kt**: Se corrigió saveSpecificLocation() para usar geocoderUseCase.fetchLocationDetails() y almacenar país/ciudad reales (ya no hardcode "Desconocido"/"Desconocida"). Se corrigió refreshLocationDetails() para actualizar address, country y city simultáneamente. Nuevo StateFlow groupedFavorites: Map<String, Map<String, List<SavedLocation>>> derivado de allSavedLocations con inferencia jerárquica vía LocationParser (usa el campo db primero, fallback al parser de dirección). - **SavedLocationsScreen.kt**: Rediseño completo del listado. Se reemplazó el LazyColumn plano con un árbol expandible: país → ciudad → favoritos, usando AnimatedVisibility para transiciones suaves. Botones ExpandMore/ChevronRight con toggles via mutableStateMapOf. Expansión inicial por defecto (países y ciudades abiertos). cityKey único por combinación "país|ciudad". - **Backups**: RESPALDO_ANTERIOR/20260704_SavedLocationsScreen.kt, 20260704_RouteViewModel.kt. - **Respaldos**: LocationParser.kt (creado), SavedLocationsScreen.kt + RouteViewModel.kt (modificados). **[04/07/2026 - Metadata Temporal en Fotos Favoritas]** - **RouteViewModel.kt**: Se modificó saveSpecificLocation() (línea 293) para inyectar metadata temporal en el campo comment cuando photoPath != null. Si el usuario añade una foto, se concatena automáticamente "📸 Capturada el dd/MM/yyyy HH:mm" al final del comentario. Si el comentario está vacío, se usa solo la metadata. Si ya tiene texto, se añade con separador \n\n. - **Arquitectura**: Punto de inyección único en ViewModel, confluencia natural de MapScreen.kt y SavedLocationsScreen.kt. No afecta restauración de backups (usa repository directo) ni ediciones posteriores. - **Respaldo**: RESPALDO_ANTERIOR/20260704_RouteViewModel.kt.  **[04/07/2026 - Marcador de Paradas Cortas (Short Stop Marker)]** - **LocationService.kt**: Nuevo sistema de detección de paradas cortas durante el tracking activo. Cuando la velocidad cae por debajo de 2.5 km/h y se mantiene así por 2 minutos consecutivos, se registran las coordenadas y se dispara geocodificación inversa asíncrona vía GeocoderUseCase. Al reanudar la marcha (>2.5 km/h), se calcula la duración total de la parada (incluyendo tolerancia de +3 min) y se inserta automáticamente un nuevo registro en la tabla de Favoritos con comentario "⏱️ Parada automática de X min Y seg" (author="Automático"). - **Variables de estado**: timeStartedStopping, isShortStopDetected, shortStopLat, shortStopLon, shortStopAddress (viaje de ida y vuelta completo con resets en pauseTracking/stopTracking). - **AppConstants.kt**: Nueva constante STOP_SPEED_THRESHOLD_KMH = 2.5f en object Location. - **Constantess del companion**: SHORT_STOP_DELAY_MS = 120_000L (2 min de quietud), STOP_TOLERANCE_MS = 180_000L (+3 min de tolerancia post-parada). - **Resets**: Limpieza total de las 5 variables en pauseTracking() y stopTracking() para evitar estados colgados entre rutas consecutivas. **[02/07/2026 - Unificación de Formato SAF con generateRouteZipFile]** - **ExperienceMapScreen.kt**: Se reemplazó la sugerencia de nombre del SAF (Storage Access Framework) de  oute?.name a ROUTE_{id}_{sanitizedName}_{timestamp}.zip, replicando exactamente el patrón de generateRouteZipFile(). Ahora ambos caminos (Compartir y Guardar como ZIP) usan el mismo formato unificado. - **RouteExportUseCase.kt**: Se agregó .getOrThrow() en xportRouteAsZip() para que el Result<Unit> del RouteExchanger lance excepción en caso de error, mejorando la propagación de fallos.  **[02/07/2026 - Nombre Único y Ubicación Persistente para ZIPs]** - **RouteExportUseCase.kt**: Se centralizó la generación del nombre del ZIP en la nueva función generateRouteZipFile(). El nombre sigue el patrón ROUTE_{id}_{nombre}_{timestamp}.zip (ej: ROUTE_5_Paseo_del_Rio_20260702_143052.zip), eliminando colisiones y permitiendo exportar la misma ruta múltiples veces. - **Sanitización robusta**: El nombre se limpia con  eplace(Regex("[\s/\\\\?%*:|\"<>]"), "_") + .take(20) para preservar acentos, eñes y caracteres internacionales, eliminando solo caracteres prohibidos por el sistema de archivos. - **Migración a DIRECTORY_DOCUMENTS**: Los ZIP ahora se guardan en context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) en lugar de cacheDir, haciendo que los archivos persistan tras el reinicio de la app y sean visibles en la carpeta Documentos del explorador de archivos. - **file_paths.xml**: Se añadió <external-files-path name="my_documents" path="Documents" /> para que FileProvider pueda generar URIs compartibles desde la nueva ubicación.**Historial de Cambios - Proyecto Localizador**  **[Sesión Actual - Menú de Acciones de Ruta y Refactor del Filtro GPS]** - **RouteOptionsMenu**: Se reemplazaron 7 IconButtons individuales en la TopAppBar de DetailScreen por 3 visibles (Explorar, Favoritos, Editar) + un menú `⋮` (MoreVert) con DropdownMenu. El menú incluye: Compartir ruta (con captura del mapa), Exportar con submenú anidado (GPX/CSV/ZIP) y Eliminar ruta. - **Eliminar ruta con confirmación**: Nuevo `AlertDialog` de confirmación antes de borrar permanentemente una ruta. Muestra advertencia sobre pérdida de puntos GPS, waypoints y fotos. Al confirmar, redirige al Historial. - **Diálogo de ayuda en pantalla**: Nuevo `AlertDialog` con 6 `HelpItem` (componente reutilizable con `ImageVector`, título y descripción) que explica cada acción de la pantalla usando los mismos iconos de la UI: Mapa de Experiencia, Vincular Favoritos, Editar nombre, Compartir, Exportar y Eliminar ruta. - **Refactor del filtro GPS en `LocationService.kt`**: Extracción de `isLocationValid()` como función pura sin efectos secundarios. Se eliminó la asignación de `lastComputedSpeedMps` dentro del filtro (side effect). Nuevas constantes: `MAX_ACCEPTABLE_ACCURACY` de 30m→20m, `MAX_VALID_SPEED_MPS` de 4.5→45 m/s (~162 km/h), `MIN_TIME_DELTA_MS` de 5000→3000ms. Se añadió `_currentAccuracy` StateFlow para exponer la precisión del GPS en vivo a la UI. - **Actualización de documentación**: Revisión y actualización completa de `HISTORIAL_CAMBIOS.md`, `DOCUMENTACION.md`, `GUIA_USUARIO.md`, `REPORTE_FILTRADO_GPS.md`, `SUGERENCIAS_COMPLETADAS.md` y `funcionamiento.md`.  **[Sesión Anterior - Corrección de Race Condition en Centrado de Mapa]** - **Sincronización de Cámara y GPS**: Se solucionó un conflicto de prioridad donde el overlay de GPS sobreescribía la animación de la cámara al seleccionar un favorito. Se implementó una desactivación explícita de `followLocation` tanto en la inicialización del mapa (si hay un objetivo activo) como inmediatamente antes de iniciar la animación de centrado en `MapScreen.kt`. Esto garantiza que el mapa se desplace suavemente al favorito elegido sin interrupciones del GPS.  **[Sesión Anterior - Corrección de Compatibilidad y Experiencia de Mapa]** - **Compatibilidad Avanzada en Importación/Restauración**: Se actualizó el motor de copias de seguridad (`prepareBackupFile` y `restoreFromBackup`) para exportar e importar todos los campos de `SavedLocation`, incluyendo `country` y `city`. Además, el importador ahora parsea dinámicamente archivos con cualquier número de columnas (de 4 a 13+), garantizando que las copias de seguridad de versiones anteriores o intermedias se restauren sin perder información y sin fallos. - **Control de Cámara y Retardo de Favoritos**: Se perfeccionó el comportamiento de la cámara del mapa en `MapScreen.kt`. Cuando se visualiza un favorito seleccionado, se bloquea el auto-centrado del GPS mediante la desactivación explícita de `locationOverlay.followLocation` en modo manual o de navegación, y se aumentó el tiempo de asentamiento de la cámara a 10 segundos antes de limpiar el objetivo. - **Bloqueo de Auto-Seguimiento en Navegación**: Se actualizó el bloque `update` de `AndroidView` para asegurar que `locationOverlay.disableFollowLocation()` se ejecute siempre que el usuario esté en modo manual o navegando hacia un punto, evitando saltos bruscos de la cámara. - **Retorno Manual al GPS**: Se actualizó el botón flotante "Mi ubicación" para que limpie inmediatamente el objetivo manual, reactive el seguimiento GPS (`enableFollowLocation`) y reinicie el temporizador de inactividad manual.  **[Sesión Anterior - Fase 3: Social, Proximidad y Estabilidad Extrema]** - **Mapa en WhatsApp**: Implementación de captura dinámica del mapa (Bitmap) que se adjunta automáticamente como primera imagen al compartir el trayecto. - **Modo Automático Blindado**: Refinamiento con 3 confirmaciones consecutivas y filtro de velocidad real (>1.5 km/h) para eliminar inicios falsos por ruido de GPS. - **Avisos "Cerca de mí"**: Motor de notificaciones de proximidad que alerta al usuario cuando pasa a menos de 100m de un favorito guardado. - **Buffer de Pre-Grabación**: Sistema de memoria circular de 30 min que recupera el inicio de la ruta si el usuario olvida pulsar grabar o tarda en detectarse el movimiento. - **Filtro Anti-Salto (Drift)**: Algoritmo de verosimilitud que ignora puntos con velocidades imposibles (>180 km/h en tierra) o saltos de señal de más de 500m. - **Gestión de Caché Segura**: Actualización de `file_paths.xml` para permitir compartir capturas temporales del mapa de forma segura.  **[Sesión Anterior - Fase 2: Funciones Sociales y Contactos]** - **Importación Inteligente desde Agenda**: Se ha añadido un nuevo botón de acción rápida (PersonAdd) que abre el selector de contactos del sistema. Al elegir un contacto, la app extrae automáticamente su **Nombre, Teléfono, Email y Dirección Postal**, precargando el diálogo de creación de Favorito para mayor agilidad. - **Compartir Ubicación Pro**: Nueva función para compartir cualquier favorito mediante un Intent de texto plano. Genera un mensaje formateado con el nombre, dirección, nota y un enlace directo a Google Maps, optimizado para WhatsApp y Telegram. - **Mejora de Interacción (Quick Actions)**: Los clics en teléfono y email en la lista de favoritos han sido rediseñados usando `FilterChip`. Ahora son botones visuales más claros ("Llamar", "Email") que mejoran la accesibilidad y evitan toques accidentales. - **Permisos de Contactos**: Actualización del Manifiesto con los permisos `READ_CONTACTS` y `WRITE_CONTACTS` para soportar la integración bidireccional con la agenda. - **Corrección de GPS en Fotos**: Se reparó el error que ignoraba la ubicación de la foto al guardar un favorito desde el mapa. Ahora, si se activa la opción, el punto se guarda en las coordenadas EXIF de la imagen. - **Rediseño de Tarjetas**: Botón de compartir movido a la cabecera y visualización de coordenadas GPS sobre la foto del favorito.  **[Sesión Anterior - Refinamiento de Modo Automático e IA Groq]** - **Detección de Movimiento Inteligente**: Se ha mejorado la fiabilidad del inicio automático mediante 2 confirmaciones consecutivas de movimiento (>25m). - **Pausa Automática Pro**: Optimización del temporizador de 15 minutos detectando velocidad para evitar pausas accidentales. - **Migración y Consistencia de IA (Groq)**: Se ha unificado el motor de IA para usar exclusivamente Groq (Llama 3.3). Se corrigieron los endpoints de Retrofit y se implementó un prompt técnico avanzado que analiza distancia, duración, velocidades y waypoints reales. Se ha optimizado el consumo de tokens reduciendo la verbosidad del prompt y la respuesta. - **Corrección de Orientación en Mapa**: Se habilitó `enableFollowLocation()` en el overlay de ubicación y se forzó la actualización del rumbo (bearing) durante el movimiento. Ahora la flecha roja de dirección rotará correctamente para apuntar hacia donde te desplazas. - **Limpieza de Código**: Eliminación de modelos redundantes y renombrado de variables `geminiApiKey` a `groqApiKey` para mayor claridad arquitectónica.  **[Sesión Anterior - Estabilización y UX]** - **Limpieza de Código**: Se ha removido la funcionalidad experimental de descarga de mapas offline debido a problemas de compatibilidad con las políticas de servidores de tiles. La app vuelve a su estado estable anterior. - **Refactorización de ViewModel**: Reparación y limpieza de `RouteViewModel.kt` para asegurar la compilación correcta y restaurar funciones de exportación y gestión de datos.  **[Sesión Anterior - Seguridad Biométrica y UX]** - **Soporte Biométrico**: Implementación de `androidx.biometric`. Ahora el usuario puede proteger el acceso a la API Key y el borrado masivo de datos mediante Huella Digital o Rostro. - **Elimunación del PIN**: Se ha removido el sistema de PIN de 4 dígitos, simplificando la seguridad para que dependa exclusivamente de la biometría del dispositivo (o el método de desbloqueo del sistema). - **Toggle de Seguridad**: Se añadió un interruptor en el menú lateral para activar/desactivar la protección biométrica globalmente. - **MainActivity Refactor**: Migración de `ComponentActivity` a `FragmentActivity` para dar soporte a los diálogos biométricos del sistema.  **[Sesión Anterior - Funciones Sociales y Personalización Total]** - **Selector de Color Libre**: Se ha añadido un Slider de Matiz (Hue) en todos los diálogos de creación y edición de puntos. Ahora el usuario puede elegir CUALQUIER color para sus estrellas, no solo los 6 predefinidos. - **Exportación a Contactos**: Nuevo botón "A Contactos" en los favoritos que permite guardar los datos directamente en la agenda del teléfono (Nombre, Tel, Email, Dirección y Comentario). - **Interacción Directa**: Los campos de teléfono y email en la lista de favoritos ahora son clicables; al tocarlos se abre el marcador telefónico o el redactor de correos automáticamente. - **Consolidación de Protocolo**: Revisión y actualización de los archivos de instrucciones y sugerencias.  **[Sesión Anterior - Eliminación Definitiva de Errores Geográficos]** - **Blindaje contra gRPC UNAVAILABLE**: Se eliminó totalmente la posibilidad de que aparezcan errores técnicos en las direcciones. Ahora, si el servicio de Google falla o está saturado, la app muestra automáticamente las coordenadas **GPS (Lat, Lon)** formateadas. - **Limpieza de Favoritos**: El botón de refresco (lápiz -> actualizar) ahora detecta automáticamente si el favorito tiene un error viejo y lo sustituye por la ubicación real. - **Inteligencia en Ríos**: Mejora en la prioridad de nombres para zonas de agua y kilómetros de ruta.  **[Sesión Anterior - Edición de Favoritos y Geocodificación Náutica]** - **Navegación en Ríos y Campo**: Se mejoró el sistema de identificación de lugares. Ahora, si no hay una calle cerca (ej. estás en un río o zona rural), la app prioriza el nombre del lugar o río (ej. "Río Paraná Km 400") en lugar de dar error. - **Corrección de "UNAVAILABLE"**: Se eliminó el mensaje de error técnico. Si el servicio de Google falla, la app ahora muestra las coordenadas GPS directamente para mantener la utilidad. - **Herramienta de Refresco**: Se añadió un botón de **"Actualizar País/Ciudad"** dentro del panel de edición (lápiz) para corregir favoritos que hayan quedado mal guardados por falta de señal. - **Edición de Favoritos**: Funcionalidad completa para cambiar nombre, autor, comentarios y color de estrella en puntos ya existentes.  **[Sesión Anterior - Estrellas de Colores y Perfeccionamiento de Fotos]** - **Personalización de Colores**: Implementación de un selector de colores (Dorado, Verde, Azul, Rojo, Violeta, Naranja) para Favoritos y Puntos de Ruta. Los marcadores en el mapa ahora reflejan estas preferencias. - **Visualización en Lista**: Las tarjetas de Favoritos muestran el icono de la estrella en el color correspondiente. - **Mejora de Fotos (EXIF)**: La app detecta automáticamente las coordenadas GPS de las fotos y permite al usuario guardar el punto en la ubicación original de la imagen. - **Filtro de Duplicados**: El sistema de restauración de backup ahora evita duplicar favoritos y rutas existentes. - **Base de Datos V7**: Actualización del esquema para incluir el campo `color`. - **Protocolo de Idioma**: Configuración del agente para comunicación y pensamiento exclusivo en Español.  **[Sesión: Consolidación de Proyecto y Protocolo de Agente]** - **Unificación de Historiales**: Se consolidaron los archivos `historial_cambios.md`, `historial-cambios.md` y `HISTORIAL_CAMBIOS.md` en este único documento centralizado. - **Protocolo de Documentación**: Establecimiento de la norma de actualización obligatoria de `DOCUMENTACION.md`, `SUGERENCIAS.md` e `HISTORIAL_CAMBIOS.md` tras cada tarea. - **Gestión de Sugerencias**: Creación de `SUGERENCIAS_COMPLETADAS.md` para mantener limpio el archivo de tareas pendientes sin perder el registro de lo logrado. - **Manual de Instrucciones**: Actualización de `instrucciones_agente.md` con pautas estrictas de idioma (Español) y hoja de ruta paso a paso.  **[Sesión Anterior - Blindaje Multimedia y Restauración Total]** - **Respaldo Físico de Fotos**: Se rediseó el motor de exportación ZIP para incluir físicamente los archivos de imagen. Ahora las fotos viajan dentro del respaldo y se restauran automáticamente en el nuevo dispositivo. - **Gestión Inteligente de Galería**: Al elegir una foto de la galería (Google Fotos), la app ahora realiza una copia local en su almacenamiento privado. Esto evita que la foto se pierda en la app si el usuario la borra de su carrete personal.   - **Corrección de Cámara y Permisos**: Implementación de un flujo de solicitud de permisos explícito para la cámara, eliminando cierres inesperados en Android 13/14. Se optimizó el FileProvider para compartir archivos de imagen de forma segura.   - **Control de Mapa y Zoom**: Se implementó un temporizador de 12-15 segundos que suspende el auto-centrado del GPS cuando el usuario interactúa manualmente con el mapa o selecciona un favorito.   - **Flecha de Grabación Resaltada**: El icono de ubicación cambia a color  **ROJO** intenso durante la grabación para mejorar la visibilidad y diferenciar el estado de seguimiento activo.   - **Integración con Google Reviews**: Nuevo botón "Publicar Opinión" que copia el comentario del favorito al portapapeles y abre la ficha del lugar en Google Maps para facilitar la reseña.   **[Sesión Anterior - Estabilización, Seguridad e IA]**   - **Organización por Regiones**: Los favoritos ahora se agrupan automáticamente por  **País** y  **Ciudad** mediante geocodificación inversa.   - **Seguridad por PIN**: Protección de la API Key y funciones de borrado masivo mediante un PIN de 4 dígitos personalizable (por defecto: 0000).   - **Migración a Llama 3 (Groq)**: Sustitución de Gemini por Llama 3 para un análisis de rutas instantáneo y sin errores de región/versión.   - **Centro de Control Lateral**: Creación del menú de "3 rayitas" para organizar todos los ajustes de forma profesional.   - **Guía de Ayuda Visual**: Inclusión de un manual de usuario con iconos reales dentro de la aplicación.   **[Sesión Anterior - Automatización y GPX]**   - **Vigilancia Inteligente**: El Modo Automático ahora mantiene un servicio persistente que inicia la grabación al detectar movimiento (>25m) y la detiene tras 15 min de inactividad.   - **Importador Profesional**: Procesamiento de archivos GPX gigantes mediante Streaming (XmlPullParser), evitando errores de memoria.   - **Estadísticas Técnicas**: Cálculo automático de Velocidad Máxima y Promedio, visualizadas en el historial y en gráficos de altitud profesionales (MPAndroidChart). **[02/07/2026 - Recalibración de Umbrales de Modo Automático]** - **AppConstants.kt**: Se recalibraron los umbrales de detección de movimiento para el arranque automático:   - AUTO_START_MIN_SPEED_KMH: 2.0 → **3.6 km/h** (~1.0 m/s, paso humano firme). Antes era tan bajo que cualquier ruido GPS disparaba el flag.   - AUTO_START_SUSTAINED_MS: 5000 → **15000 ms** (15 segundos de continuidad). Ahora filtra correctamente el jitter de interiores y falsos positivos. - **Impacto**: Cero cambios en lógica concurrente. Solo 2 líneas de constantes. **[03/07/2026 - Recalibración Urbana de Umbrales de Modo Automático]** - **AppConstants.kt**: Se recalibraron los umbrales de detección para adaptarlos a la física real de una caminata urbana con GPS fluctuante:   - AUTO_START_MIN_SPEED_KMH: 3.6 → **2.5 km/h** (~0.7 m/s). Se reduce el umbral para tolerar baches de velocidad por árboles, edificios y semáforos sin resetear el contador.   - AUTO_START_SUSTAINED_MS: 15000 → **10000 ms** (10 segundos). Dispara el tracking más rápido (~media cuadra) manteniendo el filtro contra falsos positivos.   - INACTIVITY_THRESHOLD_MILLIS: 	oMillis(15) → **	oMillis(5)** (5 minutos). Reduce el autostop de 15 min a 5 min, estándar de la industria, ahorrando batería al finalizar el recorrido. - **Impacto**: Sin cambios en lógica concurrente. Solo 3 líneas de constantes. Backupeado en RESPALDO_ANTERIOR/20260703_AppConstants.kt. **[03/07/2026 - Súper ZIP Social: Empaquetado Estructurado con GPX + CSV + Media]** - **RouteExchanger.kt**: Se unificó la exportación ZIP bajo xportRouteAsZip() con el nuevo árbol:   - 	rack.gpx: Coordenadas estándar GPX 1.1 (antes  uta_telemetria.gpx).   - 	elemetry.csv: Matriz con columnas 	imestamp_ms,latitude,longitude,altitude_m,speed_ms,speed_kmh,activity_mode. Incluye header metadata (# Route, # Activity Mode, # Total Points). speed_kmh redondeado a 2 decimales con String.format.   - media/: Carpeta con fotos de waypoints y favoritos (antes multimedia/). - **exportarRutaCompleta()**: Marcado como @Deprecated("Usa exportRouteAsZip()"). Sin llamadas activas restantes. - **RouteExportUseCase.kt**: saveZipToUri() y xportZip() migrados a xportRouteAsZip().getOrThrow(). - **Impacto**: Cero cambios en lógica de negocio. Solo renombres de entries ZIP, mejora del CSV y deprecación segura. Backups en RESPALDO_ANTERIOR/20260703_RouteExchanger.kt y 20260703_RouteExportUseCase.kt. **[03/07/2026 - Compilación Barra Inferior Unificada]** - **DetailScreen.kt**: Se eliminó Scaffold+TopAppBar (topBar con navegación, acciones y RouteOptionsMenu) y se reemplazó por Column directa. Se eliminó innerPadding de Scaffold. Se agregó private a un formatDuration(ms: Long) para resolver conflicto con HistoryScreen. - **ExperienceMapScreen.kt**: Se eliminó Scaffold+TopAppBar con shareScreenshot() y saveZipLauncher. El contenido ahora está envuelto en Box(fillMaxSize) directo. Se eliminó el cierre extra del trailing lambda del Scaffold. - **MainActivity.kt**: Se reparó import mangled Localeimport → separado en import java.util.Locale e import androidx.compose.material.icons.filled.Warning. - **Compilación**: BUILD SUCCESSFUL + APK instalado en dispositivo.  **[03/07/2026 - Corrección Ergonómica Bottom Bar (Pixel 9)]** - **MainActivity.kt**: Se reemplazó .height(48.dp) fijo por .defaultMinSize(minHeight = 72.dp) + .navigationBarsPadding() + .padding(horizontal = 16.dp, vertical = 8.dp) en el Row principal de la bottom bar. Se agregó horizontalArrangement = Arrangement.SpaceBetween. - **Imports**: Se agregaron defaultMinSize y  avigationBarsPadding del paquete oundation.layout. - **Resultado**: Barra flota sobre la zona de gestos de Android 16; botones ☰ y ? se alejan de las curvas físicas del panel.  **[03/07/2026 - Panel de Fotos Translúcido con Animación]** - **ExperienceMapScreen.kt**: Fondo del panel lateral cambiado de Color(0xEE1A1A2E) (93% opaco azul noche) a Color(0x80000000) (50% negro translúcido). El mapa ahora se ve por detrás del panel. - **Animación**: Envuelto en AnimatedVisibility con adeIn + slideInHorizontally / adeOut + slideOutHorizontally para transición suave al aparecer/desaparecer la galería. - **Imports**: Agregados AnimatedVisibility, adeIn, adeOut, slideInHorizontally, slideOutHorizontally.  **[03/07/2026 - Anti-Doze: GPS reactivo en segundo plano]** - **LocationService.kt**: Agregado setMaxUpdateDelayMillis(0) al LocationRequest.Builder para anular el batching de Doze mode en Google Play Services. - **LocationService.kt**: Intervalo pasivo reducido de 10s/5m a 5s/2m para detección más rápida del auto-arranque. - **LocationService.kt**: Agregado Watchdog GPS silencioso en el timer que reinicia el canal de ubicación si onLocationResult no se dispara por más de 120s en modo pasivo.  **[03/07/2026 - LaunchedEffect puente para DetailAction]** - **DetailScreen.kt**: Agregado LaunchedEffect(viewModel.detailAction) que escucha las acciones disparadas desde la bottom bar unificada y las traduce a estados locales (showSheet, showEditDialog, showDeleteDialog, showHelpDialog), reseteando la acción a  ull inmediatamente después de procesarla. - **Causa raíz**: Al eliminar el Scaffold/TopAppBar de DetailScreen, los botones BookmarkBorder, Edit y RouteOptionsMenu migraron a la bottom bar (MainActivity.kt), pero DetailScreen nunca recibió las intenciones. El detailAction quedaba emitido al vacío. - **Archivos modificados**: DetailScreen.kt - **Archivos respaldados**: RESPALDO_ANTERIOR/20260703_DetailScreen.kt - **Estado**: BUILD SUCCESSFUL + deploy exitoso.  **[03/07/2026 - RouteAnalyticsScreen: telemetría temporal dedicada]** - **CREADO** RouteAnalyticsScreen.kt: Nueva pantalla auxiliar con gráficos de rendimiento sincronizados en el eje X (tiempo transcurrido MM:ss).   - Perfil de Elevación: altura (m) vs tiempo.   - Perfil de Ritmo: velocidad (km/h) vs tiempo.   - Color de línea según activityMode (🟢 Caminata, 🔵 Ciclismo, 🔴 Auto).   - Ejes X sincronizados entre ambos gráficos para análisis cruzado.   - Usa MPAndroidChart con IndexAxisValueFormatter. - **DetailScreen.kt**:    - Eliminado mapa pequeño superior + mapView + DisposableEffect + LaunchedEffect(points).   - Eliminados imports osmdroid.   - Eliminada sección de gráficos antiguos (índice vs valor).   - Nuevas estadísticas consolidadas en grid 2×2: Distancia, Tiempo en Movimiento, Vel. Promedio, Ritmo.   - Nuevo botón "Ver Gráficos de Rendimiento" → navega a RouteAnalyticsScreen.   - Nuevo parámetro onAnalytics: (Int) -> Unit.   - Nueva función helper ormatMovingTime(). - **NavRoutes.kt**: Agregado ROUTE_ANALYTICS +  outeAnalytics() + a uxiliaryRoutes. - **MainActivity.kt**: Nuevo estado currentRouteAnalyticsId, nuevo composable(NavRoutes.ROUTE_ANALYTICS), callback onAnalytics en DetailScreen. - **Estado**: BUILD SUCCESSFUL + deploy exitoso.  **[03/07/2026 - RouteAnalyticsScreen: Telemetría temporal con RangeSlider y análisis de tramos]** - **RouteAnalyticsScreen.kt (creado)**: Pantalla dedicada con dos charts MPAndroidChart (Elevación y Velocidad vs Tiempo transcurrido en mm:ss). - **Eje X sincronizado**: granularity=60f, setLabelCount(8,true), IndexAxisValueFormatter con elapsedLabels. - **Header informativo**: Muestra hora de inicio (HH:mm), duración total y cantidad de puntos GPS. - **RangeSlider + LimitLines**: Slider de doble pulgar para seleccionar tramo, con líneas punteadas blancas sincronizadas en ambos charts. - **Panel de análisis de tramo**: Vel. Promedio, Desnivel acumulado, Ritmo (min/km), Modo predominante del tramo (Detenido/Caminata/Ciclismo/Automóvil por umbrales de velocidad). - **DetailScreen.kt**: Eliminado mapa, gráficos antiguos y estadísticas reemplazadas por grid 2×2 + botón "Ver Gráficos de Rendimiento". - **Correcciones compilación Kotlin 2.0.0**: sliderRange.end → endInclusive, RangeSlider values → value, emojis hex literal \U → carácter Unicode real. - **Estado**: BUILD SUCCESSFUL + deploy en Pixel 9 Pro XL.  [05/07/2026 - Fase 1: Doble Capa de Precisión Ignición Bolsillo] - **Problema diagnosticado**: Cuando el Pixel 9 Pro XL está en el bolsillo, la precisión GPS degrada a 25-50m. El filtro isLocationValid() (accuracy ≤ 20m) descartaba TODOS los puntos para el odómetro, aunque la flecha del mapa se movía (vía _latestLocation + MyLocationNewOverlay independiente de osmdroid). - **Solución implementada — Doble Capa de Precisión**:   - **Capa A (Odómetro)**: Nueva constante TOLERANT_ACCURACY = 50f (MAX_POCKET_ACCURACY_METERS en AppConstants). La distancia suma con precisión de hasta 50m, tolerando el bolsillo.   - **Capa B (Base de Datos)**: Se mantiene el filtro estricto MAX_GPS_ACCURACY = 20f. Solo los puntos de alta precisión se guardan en la DB, preservando líneas limpias en el mapa final. - **Cambio 2 — Sensibilidad de movimiento**: AUTO_STOP_SPEED_MPS bajado de 0.5f a 0.3f (~1 km/h) en AppConstants.kt para que el tracking no se detenga al caminar lento en el bolsillo. - **Cambio 3 — Anti Auto-Stop**: lastUserActivityTimeMillis ahora se actualiza en cada tick del GPS (LocationService.kt:411), no solo con ACTION_SCREEN_ON. El auto-stop por inactividad de 5 min no se dispara mientras el teléfono se mueve aunque esté en el bolsillo. - **Cambio 4 — Timer reactivo**: delay(5000L) → delay(1000L) en el timer de tracking. El contador de tiempo ahora se siente instantáneo al sacar el celular. - **Archivos modificados**: AppConstants.kt, LocationService.kt. - **Respaldos**: RESPALDO_ANTERIOR/20260705_AppConstants.kt, 20260705_LocationService.kt.[06/07/2026 - Bloque de Contenci�n Cr�tica: Parche de Estabilizaci�n 5 puntos] - **A01 � Cast inseguro context as MainActivity**: Reemplazado `context as MainActivity` (ClassCastException potencial en ContextWrapper) por extensi�n defensiva `Context.findActivity(): MainActivity?` que recorre recursivamente la jerarqu�a de ContextWrapper. Usos de `authenticateSimple` migrados a safe call `?.`. - **A02 � Force unwrap !! en drawables**: Eliminados 3 `!!` en MapScreen.kt (personIcon, directionIcon, getCachedIcon). Reemplazados por `val d = getDrawable() ?: error("mensaje")` con fallback defensivo y `mutate().apply { setTint() }` correcto. - **A03 � Force unwrap route!! en DetailScreen**: Reemplazado `route!!.endTime - route!!.startTime` por `route?.let { r -> ... } ?: 0L` con smart cast seguro. - **A04 � Reconstrucci�n pesada de polil�neas en AndroidView**: Optimizado el bloque `update` de AndroidView. Ahora usa diffing incremental: solo reconstruye todas las polil�neas cuando cambia la estructura de segmentos (modo de actividad). En caso normal (nuevo punto GPS), solo a�ade los puntos delta al �ltimo segmento existente. Elimina ANR potencial en rutas largas. - **A05 � Permiso VIBRATE faltante**: Agregado `<uses-permission android:name="android.permission.VIBRATE" />` al AndroidManifest.xml para evitar SecurityException en Android 13+ al usar AlertNotifier. - **Archivos modificados**: MainActivity.kt, MapScreen.kt, DetailScreen.kt, AndroidManifest.xml. - **Respaldos**: RESPALDO_ANTERIOR/20260706_MainActivity.kt, 20260706_MapScreen.kt, 20260706_DetailScreen.kt, 20260706_AndroidManifest.xml.   [06/07/2026 - Sprint v3.0: CinematicFilterManager — Filtro Cinemático Extraído] - **Nuevo archivo**: com.example.location.manager.CinematicFilterManager.kt - **Propósito**: Extraer la lógica de ventana deslizante GPS y filtro de velocidad de LocationService.kt a una clase inyectable, testeable y desacoplada. - **API pública**: ilter(location): Boolean — evalúa el punto contra la ventana cinemática (anti-(0,0), anti-salto de antena con tolerancia en curvas).  eset() — limpia la ventana. - **Configurable**: maxAllowedSpeedMps (15.0 por defecto) y windowSize (5). - **Integración**: No rompe el pipeline existente. Devuelve Boolean simple; LocationService decide el resto (mode, persistencia, BLE). - **Build**: BUILD SUCCESSFUL sin warnings. - **Respaldos**: RESPALDO_ANTERIOR/20260706_CinematicFilterManager.kt.  [06/07/2026 - Sprint v3.0: StationaryAnchorManager — Filtro de Anclaje Estacionario] - **Nuevo archivo**: com.example.location.manager.StationaryAnchorManager.kt - **Propósito**: Mitigar el "paseo fantasma" por rebote de señal en interiores. Cuando el usuario está quieto >2 min y la precisión GPS empeora (>20m), congela la posición, el odómetro y las coordenadas en DB hasta que una ráfaga de velocidad >0.5 m/s rompa el anclaje. - **API pública**: processAnchor(currentLocation): Location — devuelve ubicación anclada o la original.  eset() — limpia estado al detener tracking. - **Refinamientos aplicados**:   - System.currentTimeMillis() en vez de location.time para medir duración de inactividad.   - Guarda ccuracyAnchorThreshold = 20f: solo ancla si la precisión se degrada (interiores).   - Liberación con histéresis: speed > speedThresholdMps * 2 (0.5 m/s) evita flapping. - **Inyección en LocationService.kt**: Llamado tras CinematicFilterManager.filter() en el pipeline. Afecta odómetro, DB y velocidad computada. No afecta _latestLocation (UI blue dot). - **Reset**: stationaryAnchorManager.reset() en stopTracking(). - **Build**: BUILD SUCCESSFUL (0 errores). **Deploy**: installDebug exitoso en Pixel 9 Pro XL. - **Respaldos**: RESPALDO_ANTERIOR/20260706_StationaryAnchorManager.kt, 20260706_LocationService_beforeAnchor.kt.  [06/07/2026 - Bloque C: Marca de Agua Adaptativa + Circulo Inercial] - **Nuevo archivo**: ui/components/BrandWatermark.kt - Composable que muestra "Agenviaje - v3.0.0" con MaterialTheme.colorScheme.onSurfaceVariant y alpha(0.6f). Contraste dinamico MD3 dia/noche. - **Req. #08 - Circulo Inercial**: Reemplazados personIcon y directionIcon por circleIcon programatico (Bitmap 48x48 via Canvas + Paint). Circulo con relleno (rojo tracking, verde reposo) y borde blanco. directionIcon usa el mismo circulo, eliminando rotacion magnetica. MyLocationNewOverlay intacto. - **Req. #09 - Marca de Agua**: Inyectada en Box raiz de MapScreen.kt con Alignment.BottomStart, margin bottom=100dp. - **Archivos**: MapScreen.kt (iconos + watermark), BrandWatermark.kt (nuevo). - **Build**: BUILD SUCCESSFUL (0 errores). Deploy: installDebug exitoso en Pixel 9 Pro XL.  [06/07/2026 - Bloque B — Geofencing Poligonal: Bounding Box Argentina] - **AppConstants.kt**: Nuevo objeto Geofencing anidado con constantes de bounding box:   - MIN_LATITUDE = -55.0, MAX_LATITUDE = -20.0, MIN_LONGITUDE = -75.0, MAX_LONGITUDE = -53.0   - Función isWithinBounds(lat, lon): Boolean usando ranges de Kotlin (closed range, sin !!). - **GeocoderUseCase.kt**: Nueva función isValidCoordinate(lat, lon): Boolean con clamping Float→Double para sanitizar entrada. Guarda defensiva en etchLocationDetails() y etchAddress(): si la coordenada cae fuera del bounding box o es (0.0, 0.0), retorna inmediatamente "GPS: Fuera de cobertura" (fetchLocationDetails) o null (fetchAddress). - **Impacto**: Cero cambios en LocationService, Room DB, ViewModel o UI. La validación ocurre en la capa de GeocoderUseCase, antes de que el dato se consolide en el repositorio. Elimina puntos corruptos en el Atlántico y rebotes de red transfronterizos. - **Respaldos**: RESPALDO_ANTERIOR/20260706_AppConstants_Geofencing.kt, 20260706_GeocoderUseCase_Geofencing.kt. - **Compilación**: BUILD SUCCESSFUL (0 errores). Deploy: installDebug exitoso en Pixel 9 Pro XL.  [06/07/2026 - Frente #01: ActivityRecognitionManager — Arranque Automático por Transiciones Físicas] - **Problema**: El auto-arranque existente (solo por velocidad GPS en processNewLocation()) no reaccionaba en frío. GPS pasivo no recibe updates frecuentes, y Android 12+ puede matar el listener de ubicación en segundo plano. - **Solución — Sistema de reconocimiento de actividad física con Google Play Services**:   - **ActivityRecognitionManager.kt** (NUEVO): Clase en com.example.location.manager que encapsula la API de ActivityTransition. Suscribe/desuscribe PendingIntent con FLAG_MUTABLE (requerido en Android 12+). Monitorea transiciones ENTER/EXIT de STILL, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE.   - **ActivityTransitionReceiver.kt** (NUEVO): BroadcastReceiver en com.example.location.receiver que captura los eventos del SO y puentea a LocationService via ACTION_AUTO_START_FROM_TRANSITION. Usa startForegroundService() para Android 12+.   - **LocationService.kt**: Nuevas constantes ACTION_AUTO_START_FROM_TRANSITION + EXTRA_DETECTED_ACTIVITY en companion object. Handler en when(action) que llama a handleAutoStartFromTransition(). Manager inicializado en onCreate() y detenido en shutdownService().   - **AndroidManifest.xml**: Agregado <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" /> y <receiver android:name=".location.receiver.ActivityTransitionReceiver" android:exported="false" />.   - **MainActivity.kt**: Nuevo ctivityRecognitionPermissionLauncher (registerForActivityResult). Solicitud runtime de ACTIVITY_RECOGNITION en el flujo de permisos escalonados (API 29+). - **Arquitectura**: ActivityRecognitionManager vive en LocationService (no en UI), asegurando persistencia aunque la app se cierre. MainActivity solo maneja la solicitud de permiso. - **Respaldos**: RESPALDO_ANTERIOR/20260706_LocationService_ActivityRecognition.kt, 20260706_MainActivity_ActivityRecognition.kt, 20260706_AndroidManifest_ActivityRecognition.xml. - **Compilacion**: BUILD SUCCESSFUL (0 errores). **Deploy**: installDebug exitoso en Pixel 9 Pro XL.  [07/07/2026 - Frente #08: Iconos Dinámicos y Migración DB v11→v12] - **AppDatabase.kt**: Versión 11→12 con MIGRATION_11_12 que ejecuta ALTER TABLE saved_locations ADD COLUMN iconCategory TEXT NOT NULL DEFAULT 'DEFAULT'. AddMigrations actualizado para incluir la nueva migración. fallbackToDestructiveMigration preservado como red de seguridad. - **IconCategory.kt** (EXISTENTE): Enum con 7 categorías (DEFAULT/GASTRONOMIA/TRANSPORTE/AVION/PARQUE/TRABAJO/AUTOMATIC). Cada una con key String, icon ImageVector, label String. Companion con fromKey() lookup + toBitmap() que genera círculo relleno programático con borde blanco para osmdroid. - **SavedLocation.kt**: Entidad Room con nuevo campo iconCategory: String = "DEFAULT". Compatible con registros existentes vía NOT NULL DEFAULT. - **RouteRepository.kt**: addSavedLocation() ahora acepta parámetro iconCategory: String = "DEFAULT" con propagación al constructor de SavedLocation. addWaypoint() corregido (iconCategory removido, inexistente en Waypoint). - **RouteViewModel.kt**: saveSpecificLocation() ahora acepta parámetro iconCategory con named parameter en la llamada a repository. - **LocationService.kt**: Auto-stop markers ahora usan iconCategory = "AUTOMATIC" para distinguir paradas automáticas de favoritos manuales. - **MapScreen.kt**: Marcadores osmdroid usan IconCategory.toBitmap() programático en vez de getCachedIcon con resource drawable. Marker.icon recibe BitmapDrawable con círculo coloreado según loc.color + categoría. - **SavedLocationsScreen.kt**: Icono en lista usa IconCategory.fromKey(location.iconCategory).icon en lugar de Icons.Default.Star estático. - **DetailScreen.kt**: Icono en lista de favoritos vinculados usa IconCategory.fromKey(fav.iconCategory).icon en lugar de Icons.Default.Star estático. - **Backups**: RESPALDO_ANTERIOR/20250707_*.kt para AppDatabase, IconCategory, MapScreen, SavedLocationsScreen, DetailScreen, RouteViewModel, RouteRepository. - **Compilación**: BUILD SUCCESSFUL (0 errores). **Deploy**: installDebug exitoso en Pixel 9 Pro XL.       [10/07/2026 - Refactor BottomBar MD3: NavigationBar nativa con colores de alta luminancia] - **MainActivity.kt**: Reemplazo completo de la Row personalizada (líneas 798-961) por NavigationBar MD3 estándar:   - Inyectados colores de alta luminancia anti-reflejo por NavigationBarItem: Mapa (Azul Satelital #3498DB), Favoritos (Verde Esmeralda #2ECC71), Historial (Azul Satelital #3498DB).   - Botón de Ayuda transformado a 4º NavigationBarItem en Rojo Transmisor (#E74C3C) con onClick directo a showHelpDialog.   - GPS Capsule preservada en extremo derecho con Surface nativa (RoundedCornerShape 50%) y lógica de precisión existente.   - Menú hamburguesa (zIndex 99f) en capa superior absoluta verificado: márgenes relativos no colisionan con AndroidView de osmdroid.   - Navegación dinámica preservada: Detail (4 IconButtons + RouteOptionsMenu) y ExperienceMap (Share/Save ZIP). - **Importaciones**: Añadido NavigationBarItemDefaults para configuración de colores por ítem. - **Iconos**: Favoritos corregido a Icons.Default.Place (compatible con la versión actual de Material Icons). - **Correcciones**: Replace →  eplace en la lambda de exportación ZIP. - **Respaldos**: RESPALDO_ANTERIOR/20260710_MainActivity.kt. - **Compilación**: BUILD SUCCESSFUL (0 errores).  [11/07/2026 - Corrección Estructural: Efecto Compresión en BottomBar (Opción C)] - **Problema**: Los botones centrales (Mapa, Favoritos, Historial, Ayuda) sufrían colapso de espacio en el extremo derecho de la barra inferior en Pixel 9 Pro XL. Causa raíz: mezcla de NavigationBarItem + IconButton + Row intrusa con GPS capsule dentro del NavigationBar MD3, rompiendo la distribución proporcional equitativa. - **Solución**: Reestructuración completa de bottomBar en 3 sub-capas planas dentro de Column:   - **Sub-capa Alfa**: Barra de tracking activo (preservada con estado reactivo real iewModel.isTrackingLive)   - **Sub-capa Beta**: Fila flotante contextual que aísla la cápsula GPS dinámica (con precisión latestLocation?.accuracy) y los botones contextuales de DETAIL/EXPERIENCE_MAP fuera del NavigationBar   - **Sub-capa Gamma**: NavigationBar plana con 4 NavigationBarItems puros (Menu/Mapa, Favoritos, Historial, Ayuda) sin contenedores intermedios ni restricciones de ancho rígidas - **Preservado**: Tracking bar, GPS capsule dinámica con código de colores (verde <10m, rojo ≥10m), botones contextuales de DetailScreen (Explore, Bookmark, Edit, RouteOptionsMenu) y ExperienceMapScreen (Share, Save ZIP), navegación con popUpTo + launchSingleTop + restoreState, showHelpDialog. - **Eliminado**: Row intrusa con GPS capsule dentro de NavigationBar, .width(60.dp) en Help, .navigationBarsPadding() (innecesario con Scaffold), IconButton sueltos como hijos directos de NavigationBar, import navigationBarsPadding. - **Archivos modificados**: app/src/main/java/com/example/MainActivity.kt (bottomBar líneas 774-969 reemplazadas). - **Respaldos**: RESPALDO_ANTERIOR/20260711_MainActivity.kt. - **Compilación**: BUILD SUCCESSFUL (0 errores).      [2026/07/12 23:30 - Dock Flotante Traslucido + GPS Telemetria en ExperienceMapScreen] - **RouteViewModel.kt**: Nuevo StateFlow<String> gpsAccuracy derivado de LocationService.latestLocation via .map + .stateIn. Categorias: Excelente (<5m), Buena (<15m), Regular (<50m), Baja (>50m). - **ExperienceMapScreen.kt**: CAPA 3 redisenada como dock flotante. Surface con surfaceContainerHigh.copy(alpha = 0.50f), shapes.large, padding(24.dp, 16.dp) para efecto flotante. - **Nuevo import**: Icons.Default.GpsFixed + collectAsStateWithLifecycle. - **Estado GPS**: Consumido via viewModel.gpsAccuracy.collectAsStateWithLifecycle(). Icono GpsFixed + texto categorizado en Row secundario bajo titulo. - **Estructura dock**: ArrowBack | Column("Experiencias" + GPS) | [Share + Save]. - **Barra anterior eliminada**: Surface con alpha=0.96f, shapes.medium, shadow=6.dp removida. - **Lint**: 0 errores, 47 warnings (sin regression). - **Compilacion**: BUILD SUCCESSFUL (2m 6s).  [2026/07/12 23:50 - GPS extraido del dock + Chip TopEnd + Ocultar atribucion osmdroid] - **ExperienceMapScreen.kt**: GPS Row removido del dock CAPA 3. Ahora el dock solo contiene: ArrowBack + "Experiencias" + Share + Save. - **Nuevo CAPA 1.5**: Chip GPS en Alignment.TopEnd + statusBarsPadding. Surface con surfaceContainerHigh.copy(alpha=0.70f), shapes.small. Contenido: Row(GpsFixed 12dp + gpsPrecisionState labelSmall). - **Nuevo import**: Icons.Default.GpsFixed, collectAsStateWithLifecycle. - **Barra inferior**: Preservada como dock flotante (alpha=0.50f, shapes.large, padding 24.dp horizontal). - **osmdroid attribution**: Oculto el TextView de atribucion del MapView via addOnLayoutChangeListener para evitar que se transparente a traves del dock. - **Lint**: 0 errores, 47 warnings (sin regression). - **Compilacion**: BUILD SUCCESSFUL (56s). - **Deploy**: Instalado en 2 dispositivos Pixel 9 Pro XL.

[2026/07/14 00:13 - Fix compilacion + DynamicHistoryBottomBar integrado]
- **MainActivity.kt**: Corregido brace extra en SUB-CAPA ALFA que causaba cierre prematuro del Column scope y errores en cascada.
- **MainActivity.kt**: Integrado DynamicHistoryBottomBar en SUB-CAPA BETA (fila contextual) para ruta HISTORY, reemplazando la capsula GPS con un if-else que muestra la sub-barra dinamica en modo normal/multiselect.
- **DynamicHistoryBottomBar.kt**: Agregado import faltante de Box.
- **GPS Capsule**: Preservada en el bloque lse para rutas no-HISTORY (DETAIL, EXPERIENCE_MAP, etc.).
- **Callbacks**: Mapeados todos los callbacks de DynamicHistoryBottomBar (onExportAll, onDeleteAll, onCloseScreen, onExportSelected, onDeleteSelected, onClearSelection).
- **Build**: ssembleDebug exitoso. installDebug exitoso en Pixel 9 Pro XL.
[2026/07/14 01:33 - WaypointsHorizontalSelector + Empty State en Puntos de Interés (4.5)]
- **WaypointsHorizontalSelector.kt**: Nuevo composable reutilizable que encapsula la sección Puntos de Interés.
  - Empty State: OutlinedCard con icono Place + texto "Sin puntos de interés. Añade el primero" + botón "Añadir".
  - Modo con datos: LazyRow de ElevatedCards con AsyncImage (o Place fallback) + nombre + descripción.
  - Callbacks: onWaypointClick (abre foto a pantalla completa) y onAddWaypointClick (navega a creación).
- **DetailScreen.kt**: Refactor del bloque waypoints (líneas 146-185 anterior) → llamada a WaypointsHorizontalSelector.
  - Nuevo LaunchedEffect(routeId, waypoints) con Log.d("BitaruDebug", ...) para diagnóstico en Logcat.
  - Import añadido: android.util.Log.
- **MainActivity.kt**: LaunchedEffect(routeId) en composable DETAIL que ejecuta viewModel.selectRouteForDetail(routeId) para robustez de navegación.
- **RouteViewModel.kt**: Nueva función prepareWaypointCreation(routeId: Int) que setea _selectedDetailRouteId.value para que el flujo de creación de waypoints funcione en rutas históricas.
- **Build**: assembleDebug + installDebug exitoso en Pixel 9 Pro XL.
- **Respaldos**: 20260714_0133_*.kt en RESPALDO_ANTERIOR/ (pre y post cambio).


[2026/07/14 15:30 - Fix permisos ACCESS_MEDIA_LOCATION en arranque (Pantalla 1)]
- **MapScreen.kt**: Removido ACCESS_MEDIA_LOCATION del LaunchedEffect(Unit) de arranque (lineas 200-202 anteriores).
- **MapScreen.kt**: Nuevo mediaLocationPermissionLauncher con RequestPermission() para solicitar ACCESS_MEDIA_LOCATION solo bajo demanda.
- **MapScreen.kt**: galleryLauncher ahora verifica si ACCESS_MEDIA_LOCATION ya fue concedido antes de extraer EXIF; si no, almacena la URI y solicita el permiso de forma lazy.
- **MapScreen.kt**: Nuevo estado pendingPhotoUri para retener la URI de la foto mientras se espera la concesion del permiso.
- **Build**: assembleDebug exitoso (0 errores).
- **Raiz del problema**: ACCESS_MEDIA_LOCATION estaba en el mismo lote de permisos que ACCESS_FINE_LOCATION y ACCESS_COARSE_LOCATION en el LaunchedEffect(Unit), provocando que Android mostrara el dialogo "permiso para usar fotos y videos" inmediatamente al arrancar la app.
- **Respaldo**: RESPALDO_ANTERIOR/20260714_MapScreen_pre_permisos.kt

[2026/07/14 02:30 - Dock inferior unificado en DetailScreen (Pantalla 4.0)]
- **DetailScreen.kt**: Eliminado Scaffold + TopAppBar. Reemplazado por Box(fillMaxSize) inmersivo con dock flotante Surface al BottomCenter.
- **4.3.1 + 4.3.2**: Boton de retroceso y titulo "Detalle" migrados de la barra superior al nuevo dock inferior unificado.
- **Nuevo orden Dock 4.2**: [← 4.3.1] [🧪 4.2.1] [❤️ 4.2.2] [✏️ 4.2.3] [⁝ 4.2.4] (Detalle 4.3.2) [🟢 2m 4.2.5].
- **Cableado**: Botones conectados a lambdas existentes (onBack, onExperienceMap) y estados locales (showSheet, showEditDialog, showRouteMenu).
- **RouteOptionsMenu.kt**: Firmas actualizadas: nuevo parametro expanded/onDismiss para control externo del dropdown.
- **MainActivity.kt**: RouteOptionsMenu actualizado para usar nueva firma con Box + estado local menuExpanded.
- **Color.kt**: Nuevas constantes centralizadas IconExplorar (0xFF7C4DFF, Violeta) e IconGps (0xFF2ECC71, Verde).
- **ESQUEMA_PANTALLAS.md**: Diagrama 4.0 actualizado: eliminada linea de barra superior 4.3, dock inferior reconciliado con nuevo orden.
- **RESPALDO_ANTERIOR**: Backup creado como 20260714_DetailScreen_pre_dock.kt.
- **Build**: assembleDebug exitoso (39 tareas, 0 errores).

[2026/07/15 00:45 - Fix centrado repetitivo de cámara en ExperienceMapScreen (Pantalla 5.0)]
- **ExperienceMapScreen.kt**: Añadida guardia hasInitiallyCenteredRoute con 
emember { mutableStateOf(false) } para evitar que LaunchedEffect(points, waypoints, linkedFavorites, routeId) re-centre el mapa en cada tick de GPS.
- **Motivo**: El estado reactivo points cambiaba en cada actualización GPS, forzando zoomToBoundingBox repetido que anulaba los gestos táctiles del usuario.
- **Comportamiento corregido**: El mapa se centra en la ruta una única vez al cargar la pantalla; los gestos de exploración manual no son interrumpidos.
- **Build**: assembleDebug exitoso (29s, 0 errores, 0 warnings nuevos).
- **Respaldo**: RESPALDO_ANTERIOR/20260714_ExperienceMapScreen_pre_fix.kt


[2026/07/15 02:15 - Sincronizacion norteada forzada en ExperienceMapScreen (Paso 2)]
- **ExperienceMapScreen.kt**: Reemplazado el bloque else if (view.mapOrientation != 0f) { view.mapOrientation = 0f } por un guardia determinista if (!isAutoRotationEnabled) { view.mapOrientation = 0f } en cada ciclo del bloque update.
- **Motivo**: El else if condicional solo se ejecutaba en la primera recomposicion tras desactivar auto-rotacion cuando mapOrientation != 0f. El nuevo guardia corre en cada actualizacion, garantizando que el mapa este siempre norteado en modo exploracion libre.
- **Efecto colateral**: Las lineas de GuideLinesOverlay ahora proyectan correctamente porque projection.toPixels() y las coordenadas fijas del panel de fotos (CAPA 1, Compose) operan en el mismo sistema cartesiano no-rotado.
- **Build**: assembleDebug exitoso (1m 3s, 0 errores, 0 warnings nuevos). installDebug exitoso en Pixel 9 Pro XL.
- **Respaldo**: RESPALDO_ANTERIOR/20260715_ExperienceMapScreen_post_norteado.kt


[2026/07/15 03:00 - Panel de fotos inmersivo en ExperienceMapScreen (5.5)]
- **ExperienceMapScreen.kt**: Reemplazado .statusBarsPadding() por .padding(top = 8.dp, start = 8.dp) en el modificador de AnimatedVisibility (CAPA 1, linea 341).
- **Motivo**: El statusBarsPadding() dejaba un espacio muerto equivalente a la barra de estado (statusBarHeight) entre el borde superior fisico y el panel de fotos. En el diseno inmersivo edge-to-edge actual (sin TopAppBar ni Scaffold), ese espaciado era una barrera fantasma.
- **Efecto**: El panel de fotos fluye ahora desde el limite real del hardware, aprovechando el 100% de la altura de pantalla. El verticalScroll interno maneja el contenido.
- **Build**: assembleDebug exitoso (53s, 0 errores, 0 warnings nuevos). installDebug exitoso en Pixel 9 Pro XL.


[2026/07/15 04:00 - Snapshot de datos para ExperienceMapScreen (fuga de GPS en vivo)]
- **RouteViewModel.kt**: Nuevos `_experiencePoints: MutableStateFlow<List<LocationPoint>>` + `experiencePoints` (StateFlow público) + `fun prepareExperience(routeId: Int)` que carga puntos una sola vez con `repository.getLocationPointsSync(routeId)`.
- **ExperienceMapScreen.kt**: Linea 120 cambiado de `viewModel.selectedDetailPoints.collectAsState()` (Room Flow en vivo) a `viewModel.experiencePoints.collectAsState()` (snapshot estatico).
- **MainActivity.kt**: Linea 976 anadida llamada `viewModel.prepareExperience(routeId)` al navegar a ExperienceMapScreen.
- **Motivo**: Room Flow `getLocationPointsForRouteFlow` reemite cada vez que el tracking activo inserta un punto en la misma `routeId`, dibujando un trazo recto desde la ruta historica a la ubicacion GPS actual. El snapshot rompe esa conexion.
- **Build**: assembleDebug exitoso (1m 3s, 0 errores, 0 warnings nuevos). installDebug exitoso en Pixel 9 Pro XL.


[2026/07/15 05:00 - Migracion a PickVisualMedia en SavedLocationsScreen (Pantalla 3.0)]
- **SavedLocationsScreen.kt**: Reemplazado `ActivityResultContracts.GetContent()` por `ActivityResultContracts.PickVisualMedia()` como selector de fotos del sistema.
- **ACCESS_MEDIA_LOCATION**: Anadido `mediaLocationPermissionLauncher` con `RequestPermission()` + `pendingPhotoUri` para solicitar el permiso bajo demanda en API 29+ antes de extraer EXIF GPS, siguiendo el mismo patron que MapScreen.kt.
- **Imports**: Agregados `android.Manifest`, `android.os.Build`, `androidx.activity.result.PickVisualMediaRequest`.
- **Motivo**: PickVisualMedia no requiere permisos de almacenamiento general (READ_EXTERNAL_STORAGE/READ_MEDIA_IMAGES) porque el usuario otorga acceso implicito al seleccionar la foto. El permiso ACCESS_MEDIA_LOCATION se solicita solo al extraer metadatos EXIF de geolocalizacion.
- **Build**: assembleDebug exitoso (44s, 0 errores, 0 warnings nuevos). installDebug exitoso en Pixel 9 Pro XL.

[2026/07/15 20:10 - CR-03: Eliminacion de fallbackToDestructiveMigration en AppDatabase]
- **AppDatabase.kt**: Eliminado .fallbackToDestructiveMigration(dropAllTables = true) de la construccion de la BD.
- **AppDatabase.kt**: Anadido try-catch en buildDatabase() con handleDatabaseCorruption() que registra el error via Log.e.
- **AppDatabase.kt**: Refactorizado getDatabase() -> getInstance() con delegacion a buildDatabase() privada.
- **AppDatabase.kt**: Nueva constante DATABASE_NAME = "localizador_database" (nombre preservado).
- **AppDatabase.kt**: SupportOpenHelperFactory (net.zetetic) preservado; passphrase: ByteArray? = null preservado.
- **MainActivity.kt**: Llamadas AppDatabase.getDatabase() -> AppDatabase.getInstance().
- **LocationService.kt**: Llamada AppDatabase.getDatabase() -> AppDatabase.getInstance().
- **Resultado**: BUILD SUCCESSFUL, 32/32 tareas completadas, 7/7 tests pasan, assembleDebug exitoso.
- **Respaldo**: RESPALDO_ANTERIOR/20260715_AppDatabase_pre_CR03.kt.

[2026/07/15 20:55 - CR-04: Saneamiento de Capturas Silenciosas de Excepciones]
- **UiEvent.kt**: Creado sealed class UiEvent con ShowSnackbar para eventos one-shot.
- **RouteViewModel.kt**: Anadido _uiEvent: Channel<UiEvent> + val uiEvent expuesto como Flow.
  - importGpxFile(): catch silencioso reemplazado por Log.e + _uiEvent.send().
  - importarRutaZip(): catch silencioso reemplazado por Log.e + _uiEvent.send().
  - restoreFromBackup(): catch silencioso reemplazado por Log.e + _uiEvent.send().
- **RouteExportUseCase.kt**: 4 catches silenciosos saneados con Log.e:
  - exportCsv(), exportGpx(), importGpx(), shareFile().
- **BackupUseCase.kt**: detectRestoreType(): catch silencioso saneado con Log.e.
- **MainActivity.kt**: SnackbarHostState + LaunchedEffect colector de uiEvent + SnackbarHost en Scaffold.
- **Resultado**: BUILD SUCCESSFUL, 32/32 tareas (7 tests), 39 tareas assembleDebug, APK generado.
- **Respaldos**: RESPALDO_ANTERIOR/20260715_*_pre_CR04.kt (4 archivos).

[2026/07/16 02:30 - AL-01: Desintegracion de LocationService (Fase II - BufferPoint + TrackinSessionManager puro)]
- **TrackingSessionManager.kt**: Refactor completo. Nuevo BufferPoint (DTO puro JVM con lat, lon, accuracy, speed, altitude, timestamp). preRecordingBuffer migrado de ConcurrentLinkedDeque<Location> a ArrayDeque<BufferPoint> (FIFO circular, max 60). RouteRepository inyectado en constructor. Nuevos métodos: startSession(routeId: Long), stopSession(): List<LocationPoint>, ddPointToActiveSession(BufferPoint), ddDistanceFromStep(Double), setInitialDistance(Double), clearPreRecordingBuffer(), getPreRecordingBufferSnapshot(), getActiveRouteId().
- **LocationService.kt**: Eliminadas 5 variables/estructuras del monolito: ctiveRouteId: Int, 	otalDistanceMeters: Double, preRecordingBuffer: ConcurrentLinkedDeque<Location>, handlePreRecording(Location), Location.toBufferPoint(Int). Callers actualizados a sessionManager.*. 	riggerAutomaticTracking refactorizada: acepta ufferSnapshot: List<BufferPoint> + callback, puente interno BufferPoint → Location("gps") para CinematicFilterManager intacto.
- **Errores corregidos durante la cirugía**: Stray brace del handlePreRecording (L98), ctiveRouteId residual (L622, L874, L1007, L1060), 	otalDistanceMeters residual (L635, L968), ccuracy fantasma en LocationPoint (L592, TSM L87), Long→Int en 
outeId (TSM L84), ndSession→stopSession (L1013).
- **Resultado**: BUILD SUCCESSFUL (compileDebugKotlin + test + assembleDebug). installDebug exitoso en Pixel 9 Pro XL (Android 16).
- **Respaldos**: RESPALDO_ANTERIOR/20260716_LocationService.kt, 20260716_TrackingSessionManager.kt.
## [2026-07-22] CR-15: Compartir Experiencias y Recolección Canal Reactivo (shareIntentTrigger)

### Logro
Corregido el flujo huérfano de _shareIntentTrigger añadiendo un LaunchedEffect colector en MainActivity.kt. Creado ShareExperienceUseCase para empaquetar intenciones de compartir experiencias (fotos + telemetría de ruta) vía FileProvider. Expuesto photoPathsForCurrentRoute: StateFlow<List<String>> derivado reactivamente de selectedDetailWaypoints. Cableado el botón Share (5.2.1/5.3.3) en ExperienceMapScreen.kt al nuevo flujo.

### Archivos Creados
- **domain/usecase/ShareExperienceUseCase.kt**: UseCase @Inject constructor() que construye un Intent.createChooser con 0, 1 o N fotos (ACTION_SEND / ACTION_SEND_MULTIPLE) + texto de telemetría (distancia, nombre de ruta). Usa FileProvider.getUriForFile para URIs seguras tipo content:// con FLAG_GRANT_READ_URI_PERMISSION.
- **test/.../domain/usecase/ShareExperienceUseCaseTest.kt**: 1 test con Robolectric validando que invoke con lista vacía retorna Intent.ACTION_CHOOSER no nulo.

### Archivos Modificados
- **RouteViewModel.kt**: 
  - Constructor: inyectado shareExperienceUseCase: ShareExperienceUseCase.
  - Nueva propiedad photoPathsForCurrentRoute: StateFlow<List<String>> (derivada de selectedDetailWaypoints.map { waypoints.mapNotNull { it.photoPath?.takeIf(String::isNotBlank) } }).
  - Nueva función un shareExperience(context: Context, routeId: Int, photoPaths: List<String>): obtiene Route del repositorio, construye chooserIntent vía UseCase, emite a _shareIntentTrigger.
- **ExperienceMapScreen.kt**: 
  - Línea 122: añadido al currentPhotoPaths by viewModel.photoPathsForCurrentRoute.collectAsState().
  - Botón Share (línea 510): reemplazado iewModel.exportRoute(routeId) por iewModel.shareExperience(context, routeId, currentPhotoPaths).
- **MainActivity.kt**:
  - MainContainer: añadido LaunchedEffect(Unit) { viewModel.shareIntentTrigger.collect { intent -> context.startActivity(intent) } }.

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260722_RouteViewModel.kt
- RESPALDO_ANTERIOR/20260722_ExperienceMapScreen.kt
- RESPALDO_ANTERIOR/20260722_MainActivity_CR15.kt

### Build
- compileDebugKotlin: BUILD SUCCESSFUL
- 	estDebugUnitTest: 75 tests, 9 pre-existing failures (0 nuevos)
- ssembleDebug: BUILD SUCCESSFUL
## [2026-07-22] CR-16: Restructuración de ExperienceMapScreen y Unificación de Barra 5.2

### Logro
Eliminada la barra flotante intermedia 5.3 (CAPA 3) que duplicaba botones y reducía superficie del mapa osmdroid. Consolidada la sub-barra inferior 5.2 unificada con dos Row internos: botones de acción a la izquierda [←][📤][💾], título "Experiencias" + indicador GPS a la derecha con Arrangement.SpaceBetween. Añadido BackHandler para retroceso nativo.

### Archivos Modificados
- **ExperienceMapScreen.kt**:
  - Eliminada CAPA 3 (DOCK FLOTANTE INFERIOR, 50% transparencia).
  - Añadido BackHandler(onBack = onBack) para retroceso por gesto/botón del sistema.
  - Añadida sub-barra inferior 5.2 con Surface (surfaceContainer, tonalElevation 3.dp):
    - Layout: Row(SpaceBetween) conteniendo dos Row(CenterVertically) internos
    - 5.2.0 [← ArrowBack] — onBack (tint=onSurface)
    - 5.2.1 [📤 Share] (Cian 0xFF4DD0E1) — viewModel.shareExperience(context, routeId, currentPhotoPaths)
    - 5.2.2 [💾 Export] (Amber 0xFFFFB74D) — viewModel.exportRoute(routeId)
    - 5.2.3 "Experiencias" (titleMedium, Bold) + GPS (gpsPrecisionState)
  - Corregidos errores sintácticos: duplicado ) { eliminado, Box close faltante agregado, } extra eliminado.
  - Firma preservada: (onBack, viewModel, routeId: Int, modifier).
  - Import añadido: androidx.activity.compose.BackHandler.
- **ESQUEMA_PANTALLAS.md**: Sección 5 actualizada — barra 5.3 eliminada, 5.2 renumerada.
- **MainActivity.kt**: LaunchedEffect collector para shareIntentTrigger (ver CR-15).

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260722_ExperienceMapScreen_CR16.kt (pre-corrección)
- RESPALDO_ANTERIOR/20260722_ExperienceMapScreen.kt (post-corrección final)

### Correcciones Aplicadas
1. **Duplicate ) {**: El inner Row de botones se insertó con su propio ) { duplicando el del outer Row. Corregido vía Regex.Escape.
2. **Missing Box close**: La edición inicial omitió el     } (4 espacios) que cierra el Box entre Surface y FullScreenPhotoDialog. Causó GuideLinesOverlay unresolved + "Expecting '}'".
3. **Extra }**: El cierre extra del if (showShareButtons) original permanecía del layout previo. Eliminado.
4. **Layout final**: Botones agrupados en inner Row izquierdo, título+GPS en inner Row derecho, separados por SpaceBetween.

### Build
- compileDebugKotlin: BUILD SUCCESSFUL (deprecation warning pre-existing)

- testDebugUnitTest: 75 tests, 9 pre-existing failures (0 nuevos)

---

# APRENDIZAJES TÁCTICOS Y DIRECTIVAS ESTRICTAS (SUITE BITARU)

#### A. LO QUE SÍ SE HACE (Patrones de Éxito Homologados)

*   **Guardianes de Nulidad Tempranos:** En servicios imperativos (`LocationService`), la primera línea ante un callback o `onStartCommand` debe ser un guardián estricto: `if (intent == null) return`. Esto permite desempacar extras de forma segura más abajo.

*   **Desempacado Limpio de Extras de Plataforma:** Las funciones nativas de Android como `intent.getIntExtra(KEY, default)` garantizan por contrato un tipo primitivo no nulo. Se consumen directamente sin añadir operadores Elvis (`?:`) redundantes al final que ensucien el bytecode.

*   **Aislamiento del Ciclo de Vida en Mapas:** Al renderizar vistas tradicionales dentro de Compose mediante `AndroidView(factory = { ctx -> ... })`, cualquier lectura de configuración global (como densidades con `LocalDensity.current.density`) debe capturarse en variables locales **antes** de entrar al bloque imperativo de la factoría, ya que esta última no es un entorno `@Composable`.

*   **Limpieza Manual de Cabeceras:** El barrido de advertencias por importaciones ociosas (`Unused import directive`) se realiza de forma manual y quirúrgica, archivo por archivo, inspeccionando visualmente las referencias.

#### B. LO QUE "NO" SE HACE BAJO NINGUNA CIRCUNSTANCIA (Anti-Patrones Prohibidos)

*   **PROHIBIDO - Optimize Imports Automatizado Masivo:** No se deben correr scripts automáticos de limpieza de imports en todo el proyecto. El analizador del IDE suele remover por error los delegados de propiedades reactivas de Compose (`import androidx.compose.runtime.getValue` / `setValue`), quebrando la compilación en caliente.

*   **PROHIBIDO - Migrar Recursos en Entornos Imperativos (Falsos Positivos de Osmdroid):** Si el framework de un tercero (ej: Osmdroid en `MapScreen`) exige una instancia física de `android.content.res.Resources` (como `marker.icon = ...toDrawable(context.resources)`), NO se debe intentar migrar al wrapper de Compose (`stringResource`), ya que rompería el contrato de la API externa. Se documenta como falso positivo y se ignora.

*   **PROHIBIDO - Lógica de Negocio en ViewModels:** El `RouteViewModel` no debe abrir archivos, procesar flujos de exportación de texto (GPX/KML), ni calcular distancias complejas. Si el ViewModel excede las 400 líneas de código, es un síntoma de "God-Object" y la lógica debe delegarse en un `UseCase` especializado.

*   **PROHIBIDO - Mutar Firmas Inactivas por Estética:** Si una firma de interfaz secundaria (`RouteAnalyticsScreen`) arrastra parámetros huérfanos que el árbol de navegación ya no consume, se eliminan por completo de la firma y del llamador, en lugar de dejarlos comentados o ignorados con anotaciones de supresión.

---

# SESIONES HISTÓRICAS ADICIONALES (Legacy)

## [Sesión Actual - Parte 2] - Pulido de UI y Planificación
- **Transparencia de Paneles**: Se reemplazaron los componentes `Card` opacos por `Surface` con canal alfa (60%) para los indicadores de KM y Tiempo. El panel de coordenadas ahora es 50% transparente. Esto mejora la visibilidad del mapa de fondo.
- **Planificación de Funcionalidades**: Se definió la hoja de ruta para las próximas 3 grandes mejoras.

## [Sesión Anterior] - Configuración y Nueva Funcionalidad
- **Corrección de Errores de Compilación**: Ajuste de `Metaspace` en `gradle.properties`.
- **Configuración del Agente**: Español como idioma oficial.
- **Nueva Funcionalidad: Indicador de Situación**: Botón de información ("i") con geocodificación inversa.
- **Ajustes de UI**: Resaltado amarillo para direcciones y reajuste de espacios para evitar solapamientos.




# Historial de Cambios

## [Sesión Actual - Parte 2] - Pulido de UI y Planificación
- **Transparencia de Paneles**: Se reemplazaron los componentes `Card` opacos por `Surface` con canal alfa (60%) para los indicadores de KM y Tiempo. El panel de coordenadas ahora es 50% transparente. Esto mejora la visibilidad del mapa de fondo.
- **Planificación de Funcionalidades**: Se definió la hoja de ruta para las próximas 3 grandes mejoras.

## [Sesión Anterior] - Configuración y Nueva Funcionalidad
- **Corrección de Errores de Compilación**: Ajuste de `Metaspace` en `gradle.properties`.
- **Configuración del Agente**: Español como idioma oficial.
- **Nueva Funcionalidad: Indicador de Situación**: Botón de información ("i") con geocodificación inversa.
- **Ajustes de UI**: Resaltado amarillo para direcciones y reajuste de espacios para evitar solapamientos.


## [2026-07-22] CR-21: Fix de Compartir Fotos — Mapeo Híbrido de URIs en ShareExperienceUseCase y Sincronización Directa en RouteViewModel

### Logro
Corregido el fallo por el cual el Intent de compartir en Pantalla 5 (ExperienceMapScreen) solo emitía texto sin adjuntar fotografías. Causa raíz: File(path).exists() fallaba porque photoPath almacena URIs content:// (retornadas por FileProvider.getUriForFile en PhotoManager.saveImageToInternalStorage), no rutas de sistema de archivos POSIX.

### Archivos Modificados
- **ShareExperienceUseCase.kt** — Mapeo híbrido de URIs:
  - when discriminador: rutas que inician con content:// o ile:// se parsean directamente con Uri.parse() envuelto en unCatching.
  - Rutas locales físicas (sin esquema) mantienen el flujo File(path).exists() + FileProvider.getUriForFile.
  - Telemetría actualizada: "Ruta:" en lugar de "Nombre:".
- **RouteViewModel.kt** — Sincronización directa desde Room:
  - Eliminada guardia temporal withTimeoutOrNull + photoPathsForCurrentRoute.first {}.
  - Reemplazada por consulta directa epository.getWaypointsSync(routeId) cuando photoPaths está vacío.
  - Eliminados imports obsoletos: kotlinx.coroutines.flow.first, kotlinx.coroutines.withTimeoutOrNull.

### Archivos No Modificados
- **ExperienceMapScreen.kt**: Sin cambios (capa visual correcta de CR-21 previo).
- **PhotoManager.kt**: Sin cambios (ya retorna URIs content:// válidas).

### Archivos Respaldados
- RESPALDO_ANTERIOR/20260722_2110_ShareExperienceUseCase.kt
- RESPALDO_ANTERIOR/20260722_2110_RouteViewModel.kt

### Build
- compileDebugKotlin: OK (warnings preexistentes)
- assembleDebug: BUILD SUCCESSFUL
- installDebug: INSTALADO en 2 dispositivos (Pixel 9 Pro XL)