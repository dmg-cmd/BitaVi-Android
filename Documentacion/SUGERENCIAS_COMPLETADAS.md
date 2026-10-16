## 2026-10-15 23:00 — Limpieza integral de la marca "Bitaru" -> "BitaVi"
- Textos visibles: BrandWatermark, contentDescription del logo, textos de compartir (ShareExperienceUseCase) y ExperienceMapScreen.
- Logs: `Bitaru_*` -> `BitaVi_*` (Tracking/EXIF/Purge/AutoTrack/Ble/ShortStop/Core/Contact), `BitaruDebug` -> `BitaViDebug`.
- Identificadores/recursos: `BitaruBrandHeader` -> `BitaViBrandHeader`, `AZUL_BITARU` -> `AZUL_BITAVI`, `bitaru_logo.png` -> `bitavi_logo.png`.
- **Resultado**: 0 restos en app/src/main; suite 109/0; deploy OK (Pixel 9 Pro XL).
- **Pendiente**: renombrar `LocalizadorTheme`/`TopBarLocalizador`/`Theme.Localizador` (no es la marca).

## 2026-10-15 22:38 — Sincronización de versión visible a v3.2.3-RC1
- **BrandWatermark.kt** y **TopBarLocalizador.kt**: etiqueta `v3.2.2-RC1` -> `v3.2.3-RC1`.
- **app/build.gradle.kts**: `versionName` 2.0 -> 3.2.3; `versionCode` 2 -> 3.
- **Resultado**: compile + suite 109/0 + installDebug OK (Pixel 9 Pro XL).
- **Nota**: la marca visible aún dice "BiTaRu" (queda para branding P3).

## 2026-10-15 — FASE 1.3: Patrón Fachada en LocationService + WakeLock sin límite
- **GpsLifecycleDelegate.kt** (nuevo): cliente GPS + HandlerThread + callback + WakeLock; acquire() SIN timeout en grabación, release en pasivo/detenido.
- **LocationNotificationDelegate.kt** (nuevo): envuelve ServiceNotificationManager + alertas de proximidad.
- **LocationPersistenceDelegate.kt** (nuevo): envuelve RouteRepository con attach() posterior a SQLCipher.
- **LocationService.kt**: 1057 -> 806 líneas; inyecta los 3 delegados; elimina acquire(25h).
- **Resultado**: compileDebugKotlin OK; suite 109 tests, 0 failures, 0 errors; installDebug OK (2 dispositivos).
- **Nota**: meta de <400 líneas no alcanzada (806); prueba prolongada en dispositivo pendiente.

## 2026-10-15 — CIERRE FASE 1.1/1.2: Modularización de pantallas + Suite 100% verde
- **MapScreen.kt**: 1173 -> 243 líneas; delega en `ui/screens/map/` (MapCanvas, MapTopBar, MapFloatingControls, MapOverlayController, IndicadorTranslucidoContenedor y diálogos).
- **SavedLocationsScreen.kt**: 1055 -> 153 líneas; delega en `ui/screens/saved/` (SavedLocationsList, LocationCard, SaveLocationDialog, EditLocationDialog).
- **Infraestructura de tests**: eliminadas cachés Gradle con rutas de `D:\Google IA Studio\localizador` que rompían KSP (`this and base files have different roots`); reconstrucción con `--no-build-cache`.
- **Tests nuevos (5)**: IndicadorTranslucidoContenedorTest (1), LocationCardTest (2), SavedLocationsListTest (2).
- **Tests corregidos (3)**: DepartureWatcherManagerTest (2, mock de Intent/PendingIntent) + ExampleRobolectricTest (1, branding "BiTaVi").
- **Resultado**: suite 109 tests, 0 failures, 0 errors, 0 skipped. installDebug OK (2 dispositivos).

## 2026-08-07 — CR-29: CalculateRouteMetricsUseCase (analitica de tramos en segundo plano)
- **RouteMetricsResult.kt** (nuevo, com.aistudio.localizador.model): modelo inmutable de dominio.
- **CalculateRouteMetricsUseCase.kt** (nuevo): Haversine pura + histeresis altimetrica d=2.5m + velocidad/ritmo; pure-JVM, @Inject constructor().
- **RouteViewModel.kt**: _tramoRange -> tramoMetrics con flatMapLatest + flowOn(Dispatchers.Default); updateTramoRange(); reset al cambiar de ruta.
- **RouteAnalyticsScreen.kt**: snapshotFlow+debounce del slider; remember(points) para series; LineDataSet memoizado; tarjeta 7.4 con Vel. Prom, Subida +X m, Bajada -Y m, Ritmo, Distancia, Modo.
- **AppModule.kt**: provider del use case.
- **Resultado**: suite 100 tests, 0 failures, 0 errors, 0 skipped. assembleDebug OK. Deploy OK.
## 2026-08-07 — CR-30: PurgeOrphanPhotosUseCase (purgado de fotos huerfanas)
- **PurgeOrphanPhotosUseCase.kt** (nuevo): escanea filesDir/images/, compara por nombre de archivo contra las referencias de la BD (waypoints + saved_locations) y elimina JPG/JPEG/PNG huerfanos; Result<PurgeResult(scanned, purged)>; seguro ante errores de BD.
- **RouteDao.kt**: query getAllReferencedPhotoPaths() (UNION ALL de waypoints y saved_locations).
- **RouteRepository.kt**: metodo getAllReferencedPhotoPaths().
- **AppModule.kt**: provider Hilt.
- **PurgeOrphanPhotosUseCaseTest.kt** (nuevo): 7 tests deterministas con directorio temporal real + repository mockeado.
- **Resultado**: suite completa 93 tests, 0 failures, 0 errors, 0 skipped. assembleDebug OK. Deploy OK.
## 2026-08-06 — CR-28: Estabilización de la Suite de Pruebas Unitarias (100% verde)
- **gradle.properties**: daemon JVM a -Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC.
- **app/build.gradle.kts**: forkEvery=50 (reciclado de worker, libera ClassLoaders Robolectric/ByteBuddy) + HeapDumpOnOutOfMemoryError + MaxMetaspaceSize worker 512m; se mantiene maxParallelForks=1.
- **ActivityModeManagerTest**: aserciones BIKING→DRIVING (20 km/h > 13.0) + tests de límites (STOPPED<1.0, WALKING≤7.0, BIKING≤13.0, DRIVING>13.0) + fix de ventana de downgrade (t=150000).
- **RouteExportUseCaseTest**: Robolectric (Intent/Uri reales), Uri.parse real, mockkStatic(FileProvider) — elimina el bucle infinito que OOM-eaba el worker y corrompía clases siguientes.
- **ShortStopMarkManagerTest**: timestamp=any() en coVerify + runTest determinista con scheduler controlado + mock de android.util.Log.
- **ShortStopMarkManager.kt**: único cambio de producción — `managerScope` de `private val` a `internal var` (visibilidad, sin cambio de comportamiento).
- **Resultado**: 86 tests, 0 failures, 0 errors, 0 skipped; 3 corridas consecutivas idénticas (determinismo confirmado).
## 2026-08-06 — CR-27: Modo Vuelo Automático (Detección Cinemática FLIGHT)
- **CinematicFilterManager.kt**: enum KineticProfile + histéresis (FLIGHT v>41.67 m/s y alt>100m; TERRESTRIAL v<33.33 m/s y alt<100m); límite 333.33 m/s en FLIGHT; auto-stop de 3 min suprimido en vuelo; API y constructor preservados.
- **LocationService.kt**: Fase 1 (50→80m), shouldForceStop solo TERRESTRIAL, odómetro (50→80m), persistencia DB (20→80m); StateFlow kineticProfile.
- **RouteViewModel.kt**: puente kineticProfile; eliminado isAirplaneMode/toggleAirplaneMode.
- **MainActivity.kt / PreferencesManager.kt**: retirado item 8.3 y la preferencia airplane_mode.
- **MapScreen.kt**: badge "✈️ Vuelo" en TopBar 1.3.1; HelpDialog actualizado.
- **AppConstants.kt**: constantes FLIGHT_* + alias terrestres.
- **Build**: CinematicFilterManagerTest (4 escenarios nuevos) + GpsFilterPipelineTest PASS; suite completa 81 tests con OOM preexistentes (MockK/Robolectric); assembleDebug OK; Deploy OK.
## 2026-07-31 — CR-26: Compartir Captura PNG de la Experiencia
- **ExperienceMapScreen.kt**: botón Compartir (5.2.1) captura el Box interno (mapa + panel fotos + controles) vía rememberGraphicsLayer; PNG en cacheDir/shared_experiences, FileProvider + ACTION_SEND image/png; la barra inferior queda fuera de la captura. Se eliminó currentPhotoPaths.
- **Build**: testDebugUnitTest 68/9 pre-existing, assembleDebug OK. Deploy OK.
## 2026-07-31 — CR-25: Fix Botón "Actualizar País/Ciudad" — Reversión y Feedback en Tiempo Real
- **RouteViewModel.kt**: refreshLocationDetails con onResult (Main-thread) y base fresca de la DB (evita pisar campos editados).
- **SavedLocationsScreen.kt**: spinner "Geocodificando...", fila M3 de feedback (Icons.Default.Place + país • ciudad), confirmButton con entidad fresca por id.
- **Build**: testDebugUnitTest 68/9 pre-existing, assembleDebug OK. Deploy OK.
## 2026-07-31 — CR-24: Cobertura Mundial WGS84 para Geocodificación
- **AppConstants.kt**: Geofencing ampliado a rangos mundiales (-90..90, -180..180), excluyendo punto nulo (0.0, 0.0).
- **GeocoderUseCase.kt**: isValidCoordinate delega en isWithinBounds; mensaje "GPS: Sin señal".
- **Diagnóstico**: fallaba en Perú (lat ~-13) porque la cobertura regional lat -55..-20 bloqueaba antes de Nominatim.
- **Build**: testDebugUnitTest 68/9 pre-existing, assembleDebug OK. Deploy OK.
## 2026-07-31 — CR-23: Fallback de Geocodificación Nominatim (OSM)
- **NominatimApi.kt** (nuevo): DTOs Moshi + interfaz Retrofit reverseGeocode con User-Agent OSM.
- **RetrofitClient.kt**: nominatimOkHttpClient aislado (5s) + nominatimApi.
- **GeocoderUseCase.kt**: flujo híbrido Geocoder nativo → Nominatim, firma Triple(address, country, city) preservada.
- **Build**: testDebugUnitTest 68/9 pre-existing, assembleDebug OK. Deploy OK.
## 2026-07-31 — CR-22: Consolidación de Fecha EXIF en Favoritos
- **RouteViewModel.kt**: saveSpecificLocation repara el formateo del comentario con fecha EXIF (when anti-duplicado) y preserva finalTimestamp.
- **MapScreen.kt**: Nuevo estado photoTimestamp; captura metadata.timestamp en cámara/galería/reintento de permiso; paso a saveSpecificLocation y limpieza en diálogo.
- **SavedLocationsScreen.kt**: Fila de fecha (Icons.Default.Schedule + dd/MM/yyyy HH:mm) en LocationCard.
- **Build**: testDebugUnitTest 68/9 pre-existing, assembleDebug OK. Deploy OK.
## 2026-07-31 — CR-21: Fix EXIF GPS API 29+ — Posición de la Foto en Favoritos
- **ExtractPhotoLocationUseCase.kt**: setRequireOriginal(uri) en API 29+ + fallback de lectura EXIF directa desde stream.
- **SavedLocationsScreen.kt**: applyPhotoMetadata() unificado, permiso ACCESS_MEDIA_LOCATION en galería con reintento, typo de limpieza corregido.
- **MapScreen.kt**: cameraLauncher activa usePhotoGps cuando la foto de cámara contiene GPS.
- **Build**: assembleDebug OK. Deploy OK en dispositivo.
## 2026-07-22 — CR-20: AnalyzeRouteAiUseCase — Extraccion del Analisis por IA a Capa de Dominio
- **AnalyzeRouteAiUseCase.kt**: UseCase de dominio inyectable que construye prompt y llama a Groq API.
- **AnalyzeRouteAiUseCaseTest.kt**: 2 tests JVM (ruta nula, failure de red).
- **RouteViewModel.kt**: Reemplazado GroqAiUseCase, migrado aiAnalysisState a StateFlow.
- **DetailScreen.kt**: Consume StateFlow via collectAsStateWithLifecycle().
- **AppModule.kt**: Eliminado provider GroqAiUseCase.
- **ui/GroqAiUseCase.kt**: Eliminado.
- **Build**: compileDebugKotlin OK, AnalyzeRouteAiUseCaseTest OK (2/2), assembleDebug OK.
## 2026-07-22 — CR-19: Restablecimiento de Interfaz — Eliminación de Duplicación Contextual en MainActivity
- **MainActivity.kt**: Bloque EXPERIENCE_MAP contextual eliminado de sub-capa BETA. Cápsula GPS condicionada a currentRoute != NavRoutes.EXPERIENCE_MAP.
- **ExperienceMapScreen.kt**: Colores estandarizados a IconCompartir/IconExportar.
- **ShareExperienceUseCase.kt**: Sin cambios (confirmado correcto).
- **Build**: compileDebugKotlin OK, assembleDebug OK.
## [2026-07-20] CR-10: GroupRoutesByDateUseCase — Agrupamiento por Día en HistoryScreen

**Archivos creados:**
- domain/usecase/GroupRoutesByDateUseCase.kt: DTO DayGroupUiModel (LocalDate, formattedDate, colorInt ARGB, routes) + paleta de 7 colores + agrupación por LocalDate.
- test/.../GroupRoutesByDateUseCaseTest.kt: 3 tests JVM (agrupación descendente, mismo color mismo día, lista vacía).

**Archivos modificados:**
- RouteViewModel.kt: Inyectado GroupRoutesByDateUseCase. Expuesto groupedRoutesState como StateFlow<List<DayGroupUiModel>>.
- HistoryScreen.kt: Cabeceras colapsables por día con DayGroupHeader. RouteCard acepta stripeColor opcional con banda cromática izquierda de 6dp.

**Build:** compileDebugKotlin OK, testDebugUnitTest 67/9 pre-existing, assembleDebug OK.

---
## [2026-07-20] CR-11: GroupFavoritesByLocationUseCase + Headers con Emoji

**Archivos creados:**
- domain/usecase/GroupFavoritesByLocationUseCase.kt: DTOs CountryFavoritesGroup/CityFavoritesGroup + operador invoke() con agrupación ordenada.
- 	est/.../GroupFavoritesByLocationUseCaseTest.kt: 3 tests JVM.

**Archivos modificados:**
- RouteViewModel.kt: Inyectado GroupFavoritesByLocationUseCase. groupedFavorites cambiado de Map a List<CountryFavoritesGroup>.
- SavedLocationsScreen.kt: Headers de país con 🌍, ciudad con 📍 + conteo + ExpandLess/ExpandMore.

**Build:** compileDebugKotlin OK, testDebugUnitTest 67/9 pre-existing, assembleDebug OK.

---
## [2026-07-20] CR-10: HistoryScreen Limpia + Sub-barra 2.2

**Archivos:**
- HistoryScreen.kt: Eliminado IconButton(Icons.Outlined.Delete) de RouteCard (prevención de toques accidentales). Eliminado estado outeToDelete y su diálogo de borrado individual. Eliminado import Icons.Outlined.Delete.
- DynamicHistoryBottomBar.kt: Share tint cambiado de IconExportar a IconCompartir (cian). Agregado botón Importar (FileDownload + IconExportar amber) con callback onImport.
- MainActivity.kt: Agregado gpxHistoryImportLauncher (GPX picker) conectado a onImport.

**Build:** compileDebugKotlin OK, testDebugUnitTest 64/9 pre-existing, assembleDebug OK.

---
## [2026-07-20] CR-09: Gradiente Térmico HSV + Marcador Cinemático

**Archivos:**
- RouteColorUseCase.kt: speedToGradientColor() con HSV puro Kotlin, protección delta < 0.001f.
- RouteSegmenter.kt: 	oGradientRouteSegments() segmentación por par con color por velocidad promedio.
- RouteViewModel.kt: outeColorUseCase expuesto como al público.
- MapScreen.kt: migrado a 	oGradientRouteSegments() y marcador cinemático con emember(isTracking, currentSpeedMps) + speedToGradientColor().
- ExperienceMapScreen.kt: migrado a 	oGradientRouteSegments().

**Build:** compileDebugKotlin OK, testDebugUnitTest OK, assembleDebug OK.

---
## 20/07/2026 — CR-08: Bypass Biométrico (Guardián en Ciclo de Vida)
- **BiometricAuthUseCase.kt**: Nuevo use case en domain/usecase/ que encapsula verificación de hardware (BiometricManager) y estado de preferencia persistida (PreferencesManager).
- **BiometricLockScreen.kt**: Pantalla shield en Compose MD3 que bloquea la UI con icono Lock, texto descriptivo y botón "Desbloquear con Biometría".
- **MainActivity.kt**: Inyectado BiometricAuthUseCase vía @Inject; gating reactivo isAppUnlocked en setContent; guardián triggerBiometricCheck() en onStart; manejo de errores con biometricErrorMessage para reintento.
- **Test**: 2 tests unitarios JVM con MockK (verificación de isBiometricEnabled).
- **Compilación**: testDebugUnitTest, assembleDebug BUILD SUCCESSFUL.
## 19/07/2026 — ME-06: Corrección EXIF y Comportamiento Nativo (GetContent + Semántica)
- **SavedLocationsScreen.kt**: Migración de ActivityResultContracts.OpenDocument() a GetContent() para forzar el selector multimedia local nativo, evitando que el SAF abra Google Drive como ubicación predeterminada.
- **Callback reestructurado**: saveImageToInternalStorage movido dentro del coroutine scope para respetar la semántica suspend; capturedPhotoUri asignado en withContext(Dispatchers.Main) para hidratación reactiva de la miniatura.
- **Título corregido**: "Guardar ubicación actual" → "Guardar ubicación" (eliminación del término anacrónico "actual", alineado con la inyección del timestamp EXIF original implementada en ME-02).
- **Build**: assembleDebug BUILD SUCCESSFUL (45s); installDebug desplegado en 2 dispositivos.
## 17/07/2026 — ME-02: ExtractPhotoLocationUseCase + Corrección de EXIF en Hilo Principal
- **ExtractPhotoLocationUseCase.kt**: Nuevo use case en domain/usecase/ que extrae coordenadas GPS de metadatos EXIF en Dispatchers.IO, delegando en PhotoManager.
- **RouteViewModel.kt**: Agregado extractLocationFromPhoto(uri) como suspend fun, inyectado via Hilt.
- **Bug corregido**: getPhotoLocation() se ejecutaba en hilo principal (Compose), causando SecurityException silenciosas en Android 10+ al leer EXIF desde content:// URIs de la galería.
- **SavedLocationsScreen/MapScreen**: Todas las llamadas a getPhotoLocation() migradas a corrutinas con scope.launch + extractLocationFromPhoto().
- **Tests**: 2 nuevos tests unitarios (16/16 pasando).

## 16/07/2026 — AL-01 Fase III: Pipeline de Filtrado Unificado (GpsFilterPipeline + Auto-stop 3 min)
- **GpsFilterPipeline.kt**: Nueva tubería JVM-safe que orquesta CinematicFilterManager + StationaryAnchorManager en flujo ordenado.
- **Auto-stop por ventana temporal**: Migrado de 3 ticks a 180s de inactividad sostenida, inmunizando contra frecuencia de muestreo variable.
- **Bridge eliminado**: triggerAutomaticTracking() ya no convierte BufferPoint→Location→BufferPoint. Usa GpsFilterPipeline directamente.
- **Cirugía de precisión**: LocationService.processNewLocation() modificado solo 3 líneas, preservando watchdog, BLE, paradas cortas, odómetro y persistencia.
- **Suite de pruebas**: 9 tests unitarios JVM pasando (CinematicFilterManager: 4, GpsFilterPipeline: 3, StationaryAnchorManager: 2).
## 12/07/2026 — Navegación Jerárquica MD3 (TopAppBar + onBack)
- **ExperienceMapScreen.kt**: Añadido callback onBack: () -> Unit y Scaffold con TopAppBar (ArrowBack + título "Experiencias").
- **DetailScreen.kt**: Migración a Scaffold con TopAppBar (ArrowBack + título "Detalle del Recorrido").
- **SavedLocationsScreen.kt**: Sin cambios por ser pantalla base (bottom bar).
- **Patrón de desacoplamiento**: onBack expuesto como lambda en la firma del Composable, evitando dependencia directa del NavController.

## 09/07/2026 — Watchdog de Señal y Dashboard Flotante (Watchdog de señal GPS y Refactor BC)
- **LocationService.kt**: Integración y activación del Watchdog de señal de satélite (`startGpsWatchdog()`). Ahora el watchdog se inicia con el tracking y se detiene automáticamente en pausa o finalización de ruta, evitando fugas de memoria o cambios erráticos de estado.
- **DashboardActividadFlotante.kt**: Consumo seguro del contenedor translúcido y transición animada de color (rojo 0xFFDC3545 al 75% alpha en alerta y normal 0xFF1C1B1F al 65% alpha).
- **MapScreen.kt**: Refactorización de `IndicadorTranslucidoContenedor` para inyección elástica de color de fondo y reemplazo del dashboard inline original.

## 07/07/2026 — Saneamiento de Deuda Técnica (Fase 0: Zero Warnings)
- **MapScreen.kt**: Eliminación de 8+ warnings de linter: variables muertas, KTX, performClick, actualPoints, retainAll, resources hoisting, Locale, toColorInt.
- **FocusFavoriteScreen.kt**: Corrección de getDrawable deprecado + lowercase(Locale.ROOT).
- **LocationService.kt**: Locale.US/Locale.getDefault en 4 String.format para inmunidad regional.
- **Resultado**: Los 3 archivos principales quedan con 0 warnings de linter.
## 04/07/2026 — Pantalla Inmersiva Focus Favorite + Postal Táctica
- **FocusFavoriteScreen**: Rediseño completo sin Scaffold. Box raíz + contenedor drawWithContent + botones flotantes CircleShape.
- **Captura de Postal**: GraphicsLayer.record → toImageBitmap() → PNG en caché → FileProvider → Intent ACTION_SEND image/png.
- **Sin UI en la postal**: Botones de volver/compartir fuera del contenedor capturable; el destinatario recibe solo mapa + card.

## 04/07/2026 — Agrupación Jerárquica de Favoritos (Árbol Animado)
- **LocationParser**: Nuevo objeto utilitario para inferir país y ciudad desde direcciones de geocoder o directamente desde el AddressField.
- **Grouped Favorites en ViewModel**: Nuevo StateFlow groupedFavorites derivado de allSavedLocations que organiza favoritos en Map<País, Map<Ciudad, List<SavedLocation>>>.
- **Interfaz de Árbol**: SavedLocationsScreen reemplaza el listado plano por secciones expandibles país→ciudad con animaciones AnimatedVisibility y botones ExpandMore/ChevronRight.
- **Geodatos reales**: saveSpecificLocation() ahora almacena país y ciudad reales desde geocoder; refreshLocationDetails() actualiza los 3 campos (address, country, city).
## 04/07/2026 — Metadata Temporal en Fotos Favoritas
- **Metadata automática**: Al crear un favorito con foto (cámara o galería), se inyecta "📸 Capturada el dd/MM/yyyy HH:mm" en el campo de comentario.
- **Sin pérdida de datos**: Si el usuario ya escribió un comentario, la metadata se concatena con separador \n\n sin sobrescribir.
- **Punto único**: Modificación centralizada en saveSpecificLocation() del ViewModel, beneficiando ambas pantallas de creación (Mapa y Favoritos).

## 04/07/2026 — Marcador de Paradas Cortas (Short Stop Marker)
- **Detección de quietud**: Cuando velocidad < 2.5 km/h con tracking activo, se inicia contador de 2 minutos.
- **Registro automático**: Si pasan 2 min quieto, captura coordenadas y geocodifica en segundo plano.
- **Inserción en Favoritos**: Al reanudar (>2.5 km/h), calcula duración + 3 min de tolerancia y guarda "⏱️ Parada automática de X min Y seg" como favorito (author="Automático").
- **Resets de ciclo de vida**: Limpieza en pauseTracking() y stopTracking() para evitar estados fantasma.# Sugerencias Completadas

## Sesión Actual — Menú de Acciones de Ruta y Refactor del Filtro GPS
- **RouteOptionsMenu**: Implementación de menú `⋮` con DropdownMenu y submenú de exportación anidado (GPX/CSV/ZIP). Reemplazo de 7 IconButtons individuales por 3 visibles + menú.
- **Eliminar ruta con confirmación**: Nuevo `AlertDialog` de confirmación antes de borrar rutas permanentemente, con navegación automática al Historial.
- **Diálogo de ayuda en pantalla**: Componente `HelpItem` reutilizable con iconos Material Design, título y descripción. AlertDialog informativo con las 6 acciones de la pantalla de detalle.
- **Refactor del filtro GPS**: Extracción de `isLocationValid()` como función pura. Eliminación del side effect de `lastComputedSpeedMps`. Nuevas constantes: accuracy 30→20m, speed 4.5→45 m/s, time delta 5000→3000ms. Nuevo `_currentAccuracy` StateFlow para UI.
- **Actualización de documentación**: Revisión completa de `HISTORIAL_CAMBIOS.md`, `DOCUMENTACION.md`, `GUIA_USUARIO.md`, `REPORTE_FILTRADO_GPS.md`, `SUGERENCIAS_COMPLETADAS.md` y `funcionamiento.md`.

## Fase 3: Funciones Sociales Avanzadas y Estabilidad (Sesión Anterior)
- **Mapa en WhatsApp (Tarea 3.1)**: Captura automática del mapa del recorrido para reportes visuales en WhatsApp.
- **Avisos "Cerca de mí" (Tarea 3.3)**: Notificaciones automáticas al pasar por favoritos guardados.
- **Estabilidad Automática**: Blindaje del modo automático con 3 confirmaciones y filtro de velocidad mínima.
- **Filtro GPS Drift**: Eliminación de saltos de señal imposibles.
- **Buffer de 30 min**: Recuperación de puntos previos al inicio de la grabación.

## Fase 2: Funciones Sociales y Contactos (Sesión Anterior)
- **Importación desde Agenda (Tarea 2.1)**: Botón "Importar Contacto" que usa el Picker de sistema y extrae automáticamente Nombre, Teléfono, Email y Dirección Postal.
- **Compartir Ubicación (Tarea 2.2)**: Generación de mensaje formateado con enlace a Google Maps para WhatsApp/Telegram.
- **Mejora de Interacción Directa**: Uso de `FilterChips` ("Llamar", "Email") en las tarjetas de favoritos para una acción más rápida y profesional.
- **Permisos de Contactos**: Integración total de los permisos `READ_CONTACTS` y `WRITE_CONTACTS`.

## Sesión Anterior
- **Refinamiento del Modo Automático (Tarea 1.1)**: Mejora de la fiabilidad del inicio mediante confirmaciones consecutivas de movimiento y optimización de la pausa por inactividad detectando velocidad. Sincronización de persistencia en ajustes.
- **Descarga de Áreas Offline (Sin éxito)**: Se intentó implementar la descarga masiva de tiles mediante `CacheManager`, pero fue descartada debido a bloqueos de política de los servidores de OpenStreetMap y cierres de la librería osmdroid.
- **Exportar a Contactos**: Botón para crear un contacto real en Android desde un favorito (Uso de Intents de Sistema).
- **Interacción Directa**: Clic en teléfono para llamar o email para redactar correo (Implementado en Favoritos).
- **Edición de Favoritos**: Implementación de la funcionalidad para modificar datos y color de puntos ya existentes.
- **Soporte Biométrico**: Uso de Huella/Cara para proteger API Keys y borrado masivo de datos (Integración con `FragmentActivity`).
- **Estrellas de Colores (Personalización)**: Implementación de selector de colores predefinidos y **selector libre (Hue Slider)** con guardado en base de datos.
- **Interacción Directa**: Llamada telefónica y redacción de email con un solo toque desde la lista de favoritos.
- **Exportar a Contactos**: Botón para crear un contacto real en Android desde un favorito (Uso de Intents de Sistema).
- **Detección GPS en Fotos**: Uso de metadatos EXIF para ubicar favoritos y puntos de ruta.
- **Protección contra Duplicados**: Filtro inteligente durante la restauración de backups.
- **Idioma del Agente**: Pensamiento y comunicación estrictamente en Español.

- **Arreglo del Arranque Automático**: Implementación de vigilancia proactiva y sensibilidad de 25m.
- **Estrellas de Colores (Básico)**: Diferenciación entre favoritos fijos (Dorado) y puntos de ruta (Verde).
- **Diario de Viaje Multimedia**: 
    - Captura de fotos desde Cámara.
    - Importación de fotos desde Galería/Google Fotos.
    - Extracción de coordenadas GPS de fotos (Metadatos EXIF).
    - Campos de Comentario y Autor.
- **Sistema de Contactos**: 
    - Opción "Es un Contacto" en favoritos.
    - Almacenamiento manual de Teléfono y Email vinculado a la dirección.
- **Backup y Restauración Avanzada**:
    - Respaldo ZIP con archivos físicos de imagen.
    - Restauración inteligente que evita duplicados.
    - Reconstrucción de fechas reales en el historial desde el backup.
- **Organización Geográfica**: Clasificación automática de favoritos por País y Ciudad mediante Geocoder.
- **Integración con Google Reviews**: Botón para copiar comentario y abrir sección de opinión en Maps.

## 03/07/2026 — Recalibración Urbana de Modo Automático
- **Umbral de velocidad de arranque**: Reducido de 3.6 km/h a 2.5 km/h para tolerar baches de GPS urbano (árboles, edificios, semáforos) sin resetear el contador de inicio.
- **Tiempo de sostenimiento de arranque**: Reducido de 15 s a 10 s para disparar el tracking en menos de media cuadra.
- **Autostop por inactividad**: Reducido de 15 min a 5 min usando TimeUnit.MINUTES.toMillis(5) para legibilidad. Ahorro de batería del 66% en recorridos finalizados.
## 03/07/2026 — Súper ZIP Social (Empaquetado Estructurado)
- **Árbol unificado**: ZIP contiene 	rack.gpx, 	elemetry.csv y media/ (fotos).
- **CSV enriquecido**: Header metadata (# Route, # Activity Mode, # Total Points) + columnas speed_kmh (redondeado a 2 decimales) y ctivity_mode por fila.
- **Deprecación segura**: xportarRutaCompleta() marcado como @Deprecated, sin llamadas activas.
- **Unificación total**: saveZipToUri() y xportZip() ahora usan xportRouteAsZip(). Ambos caminos de guardado producen el mismo formato estructurado.
**[03/07/2026 - Compilación Barra Inferior Unificada]**
- [x] Eliminar Scaffolds anidados de DetailScreen.kt y ExperienceMapScreen.kt
- [x] Mover saveZipLauncher de ExperienceMapScreen a MainActivity.kt
- [x] Reparar imports de MainActivity.kt (Localeimport, RouteOptionsMenu)
- [x] Resolver conflicto formatDuration entre DetailScreen y HistoryScreen
- [x] Compilación exitosa y APK instalado

**[03/07/2026 - Corrección Ergonómica Bottom Bar]**
- [x] .height(48.dp) → .defaultMinSize(minHeight = 72.dp)
- [x] Agregar .navigationBarsPadding() para colchón de gestos
- [x] Agregar padding(horizontal = 16.dp, vertical = 8.dp) contra curvas del panel
- [x] Agregar horizontalArrangement = Arrangement.SpaceBetween

## 03/07/2026 — RouteAnalyticsScreen con telemetría temporal
- **RouteAnalyticsScreen**: Nueva pantalla dedicada con Elevation y Speed charts sincronizados (mm:ss), RangeSlider, LimitLines y panel de análisis de tramo.
- **DetailScreen simplificado**: Grid 2×2 de métricas + botón "Ver Gráficos de Rendimiento" que navega a la nueva pantalla.

## 04/07/2026 — Rollback y Fusión Táctica (v4 + bottom=40dp)
- **Problema diagnosticado**: La card inferior de FocusFavoriteScreen desaparecía al tocar el mapa porque el modo inmersivo del Pixel pone los insets a 0 y navigationBarsPadding() pierde efecto.
- **Solución confirmada**: bottom=40dp fijo en lugar de navigationBarsPadding(). La card se mantiene visible pase lo que pase con los insets.
- **Experimentos revertidos**:
  - FLAG_LAYOUT_NO_LIMITS: Causaba parpadeo al redibujar el scrim en cada invalidate() del mapa.
  - Attributos de transparencia en themes.xml (statusBarColor, navigationBarColor, windowTranslucentStatus): Causaban efectos secundarios globales en todas las pantallas con mapa.
  - pointerInput + detectVerticalDragGestures: Innecesario (no hay scroll heredado).
  - DisposableEffect/WindowManager: Innecesario, SideEffect simple es suficiente.
- **Lección aprendida**: El modo inmersivo del Pixel es nativo e inevitable al tocar un MapView. Pelear contra él rompe el renderizado. La solución es adaptar el layout (bottom fijo) en lugar de force tear la transparencia.
- **Rollback base**: Se restauró RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v4.kt (20:53, 258 líneas, Box + GraphicsLayer + fitsSystemWindows=false). Único parche: bottom=40dp en la Card.

## 07/07/2026 — Migración DB v11→v12 + Iconos Dinámicos (Frente #08)
- **AppDatabase.kt**: Versión 12 con MIGRATION_11_12 (ALTER TABLE saved_locations ADD COLUMN iconCategory TEXT NOT NULL DEFAULT 'DEFAULT').
- **IconCategory.kt**: Enum con 7 categorías, fromKey() y toBitmap() programático para osmdroid.
- **SavedLocation.kt**: Nuevo campo iconCategory: String = "DEFAULT".
- **RouteRepository.kt + RouteViewModel.kt**: Parámetro iconCategory en addSavedLocation/saveSpecificLocation.
- **LocationService.kt**: Auto-stop markers usan iconCategory = "AUTOMATIC".
- **MapScreen.kt**: Markers osmdroid con círculo programático vía IconCategory.toBitmap().
- **SavedLocationsScreen.kt + DetailScreen.kt**: Iconos dinámicos vía IconCategory.fromKey().icon.
- **Deploy**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.



## 09/07/2026 — Watchdog de Señal GNSS + Dashboard Actividad Flotante
- **Watchdog GNSS**: Nuevo sistema de monitoreo de salud satelital en LocationService. Detecta pérdida de señal GPS después de 15s sin updates del GPS_PROVIDER y conmuta automáticamente a ActivityMode.SIN_SENAL. Al recuperar señal, restaura el modo cinemático previo atómicamente.
- **DashboardActividadFlotante**: Nuevo componente UI que reemplaza el dashboard inline. Fondo oscuro translúcido en estado normal, rojo táctico #DC3545 al 75% alpha en pérdida de señal. Velocidad oculta automáticamente durante SIN_SENAL. Animación suave de 400ms en transiciones.
- **IndicadorTranslucidoContenedor**: Refactor backward-compatible con nuevo parámetro overrideColor.
- **RouteSegmenter**: Segmentos de ruta SIN_SENAL se renderizan en rojo #DC3545 en el mapa.
- **Compilación**: BUILD SUCCESSFUL (0 errores, 0 warnings).

## 11/07/2026 — Fase 6: Eliminación de 6 Funciones Muertas
- **authenticateWithCrypto**: Eliminada de MainActivity.kt (biométrica con cipher, 28 líneas). authenticateSimple preservada.
- **shareCompleteRoute**: Eliminada de ShareUseCase.kt (61 líneas, generaba resumen vía Intent ACTION_SEND/SEND_MULTIPLE).
- **shareScreenshot**: Eliminada de ExperienceMapScreen.kt (21 líneas, captura decorView + FileProvider).
- **PhotoInfoWindow**: Eliminada de ExperienceMapScreen.kt (72 líneas, InfoWindow osmdroid con CircleCropTransformation, sin instanciaciones).
- **isFavoriteLinked**: Eliminada de RouteViewModel.kt (2 líneas, single-expression Flow<Boolean>).
- **saveFavoritePoint**: Eliminada de RouteViewModel.kt (10 líneas, insertaba favorito con geocoder).
- **Método**: Eliminación por rango de línea exacto (sin brace-counting) para evitar corrupción sintáctica.


## 11/07/2026 — Fase 6 Real: 10 Pasos de Cierre
- **Purgas (Pasos 1-5)**: 6 funciones muertas extirpadas (authenticateWithCrypto, 2× shareCompleteRoute, insertFavorite, val activity, imports Activity/rememberLauncherForActivityResult/ActivityResultContracts).
- **Locale.US (Pasos 6-8)**: 8 String.format blindados contra NumberFormatException en GroqAiUseCase (L21,29,31), GeocoderUseCase (L46), RouteAnalyticsScreen (L69,239,245,254).
- **Optimize Imports (Paso 9)**: 2 imports muertos eliminados de MapScreen.kt (PhotoPanel, DashboardActividadFlotante — componentes ya borrados en Fase 5).
- **Build**: gradlew assembleDebug → BUILD SUCCESSFUL. Deploy a Pixel 9 Pro XL.
- **Lección**: PowerShell requiere espacio (no coma) como separador de argumentos. Coma crea array y corrompe regex.


## 11/07/2026 — Fase 6 Definitiva: Cierre Completo
- **Regresiones extirpadas**: authenticateWithCrypto (MainActivity), 2× shareCompleteRoute (RouteViewModel + ShareUseCase), insertFavorite (RouteRepository).
- **Error de Compose eliminado**: L94 RouteAnalyticsScreen — Locale.getDefault() reemplazado por LocalConfiguration.current.locales[0].
- **Migración KTX**: 6× parseColor → toColorInt() en RouteAnalyticsScreen.
- **Bug de PowerShell corregido**: Remove-Lines con arrays anidados no funcionaba; reemplazado por filtro directo.
- **Build**: BUILD SUCCESSFUL. Deploy Pixel 9 Pro XL.


## 12/07/2026 — Fases 7-10: Consolidación Production-Ready
- **Fase 7 (4 bloques)**: LocationService null guard + intent?. a intent. (6x) + FQNs cortos. RouteRepository FQNs contraídos. RouteViewModel debounce KTX. RouteExportUseCase val route muerto eliminado. ExperienceMapScreen redundancia !isAutoRotationEnabled eliminada.
- **Fase 8 (10 pasos)**: LocationService 4 Elvis muertos extirpados + rssi eliminado de lambda BLE. RouteAnalyticsScreen routeId/onBack extirpados + caller. MapScreen event a _ + else if simplificado. RouteViewModel catch/when saneados. GeocoderUseCase suspend removido.
- **Fase 9 (3 bloques)**: ExperienceMapScreen photoItems eliminado + LocalDensity migrado. MapScreen val res inlinado. SavedLocationsScreen FQN android.net.Uri contraído. RouteExchanger FQNs contraídos. MainActivity FQNs Intent/Toast eliminados.
- **Fase 10 (Blueprint)**: ShareUseCase .let { it.toUri() } a .toUri(). ImageRequest.Builder corregido a context. Toast.LENGTH_SHORT sin FQN.
- **Build 41s**: Tiempo de compilación reducido de >2min a 41s estables (-65%).
- **Declaración Production-Ready**: Dictamen arquitectónico de cierre. Fase 11 declinada.
- **Commit sellado**: chore(arch): consolidacion perimetral fases 6-10, purga de codigo muerto y optimizacion de tiempos de build (41s)

## 13/07/2026 — Paleta semantica de colores para botones de accion
- **Color.kt**: Nuevas constantes IconCompartir, IconExportar e IconFavorito para acciones semanticas.
- **MainActivity.kt**: Icono de favoritos cambiado a corazon dorado IconFavorito.
- **DetailScreen.kt**: Compartir en cian IconCompartir, exportacion en ambar IconExportar.
- **.aierules**: Nueva regla de convencion de colores para botones de accion.
## 13/07/2026 — Esquema de referencia de pantallas (ESQUEMA_PANTALLAS.md)
- **ESQUEMA_PANTALLAS.md**: Documento maestro con diagramas ASCII de las 7 pantallas.
- **Sistema de numeracion**: PANTALLA.BARRA.BOTON para comunicacion precisa.
- **Colores semanticos**: Tabla de referencia con codigos y usos.
- **.aierules**: Regla para cargar esquema al iniciar sesion.

## 14/07/2026 — Sub-barra dinámica de historial (DynamicHistoryBottomBar)
- [x] Crear DynamicHistoryBottomBar.kt con estados Normal/Multiselect
- [x] Integrar en MainActivity.kt Capa 2 BETA para ruta HISTORY
- [x] Agregar _selectedRouteIds a RouteViewModel.kt
- [x] Refactor HistoryScreen.kt para usar estado del ViewModel
- [x] Agregar IconEliminar a Color.kt
- [x] Compilacion exitosa y deploy en Pixel 9 Pro XL

## 14/07/2026 — WaypointsHorizontalSelector + Empty State en Puntos de Interés
- [x] Crear WaypointsHorizontalSelector.kt con Empty State y LazyRow de ElevatedCards
- [x] Integrar en DetailScreen.kt reemplazando el bloque if (waypoints.isNotEmpty())
- [x] Agregar LaunchedEffect diagnóstico (Log.d con tag "BitaruDebug")
- [x] Agregar LaunchedEffect de navegación robusta en MainActivity.kt
- [x] Agregar prepareWaypointCreation() en RouteViewModel.kt
- [x] Compilación exitosa (assembleDebug) y deploy en Pixel 9 Pro XL


## 16/07/2026 — AL-01 Fase I: Desintegracion de LocationService en managers
- [x] Crear ServiceNotificationManager.kt con canal de notificacion, buildNotification(), foreground service
- [x] Crear TrackingSessionManager.kt con buffer de pre-grabacion (60s), odometro, estado de sesion, timer
- [x] Integrar notificationManager en LocationService (buildNotification delegado, updateNotification renombrado)
- [x] Integrar sessionManager en LocationService (field + preRecordingBuffer/handlePreRecording delegados)
- [x] BUILD SUCCESSFUL, assembleDebug OK, APK generado
- [x] Respaldos creados en RESPALDO_ANTERIOR/

## 16/07/2026 — AL-01 Fase II: BufferPoint + cirugia completa de LocationService
- [x] Refactor TrackingSessionManager.kt con BufferPoint, ArrayDeque FIFO, RouteRepository en constructor
- [x] Eliminar activeRouteId, totalDistanceMeters, preRecordingBuffer, handlePreRecording, toBufferPoint de LocationService.kt
- [x] Refactor triggerAutomaticTracking con bufferSnapshot: List<BufferPoint> y callback
- [x] Puente BufferPoint→Location("gps") preservando CinematicFilterManager intacto
- [x] BUILD SUCCESSFUL, assembleDebug OK, installDebug en Pixel 9 Pro XL
- [x] Respaldos creados en RESPALDO_ANTERIOR/


## 18/07/2026 - ME-02 Paso 2: ActivityModeManager - Extraccion de Histeresis de Actividad
- **ActivityModeManager.kt**: Nuevo manager con evaluate() encapsulando la maquina de estados de modo de actividad (L587-611 original). Histeresis temporal pura con ventanas de upgrade (10s) y downgrade (120s). Thresholds exactos con operadores <=.
- **LocationModule.kt**: Provider unscoped provideActivityModeManager() anadido.
- **LocationService.kt**: 2 variables globales eliminadas (currentMode, lastModeChangeTime). Metodo updateActivityMode() eliminado (25 lineas). L473 reemplazado por inline evaluate(). 3 lecturas externas migradas a getCurrentMode().name. 2 escrituras migradas a reset().
- **ActivityModeManagerTest.kt**: 5 tests unitarios (borde 7.0 km/h, ventana de upgrade/downgrade, consolidacion, reset).
- **AssembleDebug**: BUILD SUCCESSFUL.

## 18/07/2026 - ME-02 Paso 3: ShortStopMarkManager - Extraccion de Paradas Cortas
- **ShortStopMarkManager.kt**: Nuevo manager con algoritmo de 2 fases (deteccion 120s + geocoding background, persistencia al reanudar con tolerancia 180s). Inyecta RouteRepository y GeocoderUseCase.
- **LocationModule.kt**: Provider unscoped con dependencias.
- **LocationService.kt**: 5 variables eliminadas, bloque L476-522 delegado, resets migrados a resetEvaluation().
- **ShortStopMarkManagerTest.kt**: 4 tests unitarios.
- **AssembleDebug**: BUILD SUCCESSFUL.

## 18/07/2026 — ME-03: Inyección Directa EXIF + Micro-Mapa en Diálogo de Favoritos
- **Bug corregido (Sincronización EXIF-03)**: Carrera asíncrona en SavedLocationsScreen.kt corregida mediante inyección directa de coordenadas EXIF en inputLatitude/inputLongitude al seleccionar foto.
- **Campos Lat/Lon editables**: Nuevos OutlinedTextField con desactivación automática de usePhotoGps al editar manualmente.
- **Micro-mapa osmdroid**: AndroidView(MapView) de 180dp con marcador reactivo centrado en las coordenadas del formulario.
- **Guardado simplificado**: El botón Guardar ahora lee directamente de inputLatitude/inputLongitude como fuente de verdad única.
- **Build**: assembleDebug BUILD SUCCESSFUL.
## 18/07/2026 — ME-03b: Refinamiento Multihilo en Launchers de Favoritos
- **Dispatchers explícitos**: cameraLauncher, mediaLocationPermissionLauncher y photoPickerLauncher migrados a scope.launch(Dispatchers.IO) + withContext(Dispatchers.Main).
- **LaunchedEffect eliminado**: fetchPhotoAddress() ahora se llama directamente desde cada launcher, eliminando la dependencia reactiva de LaunchedEffect(photoGpsLocation).
- **Cancelación preservada**: cameraLauncher mantiene el bloque else { capturedPhotoUri = null } contra fotos fantasma.
- **Build**: assembleDebug BUILD SUCCESSFUL.
## 18/07/2026 — UI-05b: Selector de Origen (FilterChips) + Blindaje de Dirección
- **FilterChips M3**: Selector de origen GPS/Foto en el diálogo de guardar favorito, conmutando usePhotoGps y rellenando inputs numéricos.
- **Blindaje finalAddress**: Fallback formateado "GPS Foto: lat, lon" / "GPS: lat, lon" cuando la geocodificación es lenta o falla.
- **0 flags nuevos**: Reutiliza usePhotoGps como única fuente de verdad. Launchers intactos.
- **Build**: assembleDebug BUILD SUCCESSFUL (987 líneas finales).
## 18/07/2026 — UI-05c: Permiso ACCESS_MEDIA_LOCATION preventivo en Galería
- **Permiso preventivo**: El botón Galería ahora verifica ACCESS_MEDIA_LOCATION antes de abrir el picker, evitando la censura silenciosa de EXIF en Android 10+.
- **photoPickerLauncher simplificado**: Eliminada la bifurcación Q+/<Q; flujo lineal único.
- **pendingPhotoUri eliminada**: Variable huérfana y referencias removidas (-1 estado mutable).
- **Build**: assembleDebug BUILD SUCCESSFUL (959 líneas finales).
## 19/07/2026 — ME-02 Paso 4: ProximityBleManager - Extraccion de Logica BLE de Proximidad
- **ProximityBleManager.kt**: Nuevo manager que encapsula el ciclo de vida de ProximityManager (advertising + scanning), notificaciones de contacto cercano y notificaciones push. Constructor solo con @ApplicationContext (inyeccion Hilt simplificada, sin providers adicionales en LocationModule).
- **LocationService.kt**: 5 variables BLE eliminadas (alertNotifier, proximityManager, isBleActive, lastBleNotificationTime). Metodos ensureBleProximity() y onBleContactDetected() removidos (~55 lineas). Implementa ProximityBleManager.ProximityListener. onCreate/onDestroy delegados.
- **ProximityBleManagerTest.kt**: 5 tests unitarios (init, scanning activo, NearMe desactivado, tracking desactivado, clearAll).
- **Reduccion**: LocationService ~1014 → ~972 lineas (-42, -167 acumulado desde ME-02 Paso 1).
- **AssembleDebug**: BUILD SUCCESSFUL. Test suite: 31/31 passing.
- **Respaldos**: RESPALDO_ANTERIOR/20260719_*.kt (4 archivos).

## 19/07/2026 — HOTFIX: SecurityException al abrir app (ME-02 Paso 4a)
- **Bug corregido**: ContactUseCase.getAllContactPhones() se ejecutaba en cada primer tick de GPS sin verificar READ_CONTACTS, lanzando SecurityException y cerrando la app.
- **Solucion**: El hashing de contactos se movio dentro de ProximityBleManager.ensureBleProximity(), ejecutandose solo cuando BLE esta configurado para activarse, con try-catch SecurityException como salvaguarda.
- **Archivos**: ProximityBleManager.kt, LocationService.kt, ProximityBleManagerTest.kt.
- **Build**: assembleDebug + testDebugUnitTest + installDebug exitoso.

## 19/07/2026 — ME-04: Migracion de RouteExportUseCase a domain/usecase
- **RouteExportUseCase.kt**: Migrado de ui/ a domain/usecase/ con constructor @Inject (Context, RouteRepository, RouteExchanger). API Result<T> para todos los metodos. Sin Toasts ni startActivity(). Metodos: exportCsv, exportGpx, exportZip/exportRouteAsZip (unificados), importGpx, importZip, saveZipToUri, buildShareIntent, buildChooserIntent, getUriForFile.
- **RouteViewModel.kt**: Nuevo _shareIntentTrigger (SharedFlow), metodos de exportacion/importacion (L540-569) reescritos con onSuccess/onFailure, manteniendo nombres publicos intactos. Snackbars via _uiEvent.
- **MainActivity.kt**: Nuevo LaunchedEffect que colecta shareIntentTrigger y ejecuta startActivity().
- **ui/RouteExportUseCase.kt**: Eliminado.
- **Pruebas**: 9 tests unitarios nuevos (RouteExportUseCaseTest).
- **Build**: assembleDebug + installDebug exitoso en 2 dispositivos.

## 19/07/2026 — ME-06: Correccion EXIF - Transcodificacion robusta + OpenDocument
- **ExtractPhotoLocationUseCase.kt**: Nuevo metodo executeRobust(uri) con transcodificacion a cacheDir para soporte seek fisico. Extrae GPS + fecha de creacion (TAG_DATETIME_ORIGINAL). Retorna PhotoExifResult.
- **RouteViewModel.kt**: Nuevo extractPhotoExifRobust(uri) como puente.
- **SavedLocationsScreen.kt**: Galeria migrada de PickVisualMedia a OpenDocument. La extraccion usa executeRobust en lugar del metodo anterior.
- **Causa raiz**: PickVisualMedia retorna URIs proxy (content://media/picker/...) que Android filtra eliminando EXIF GPS. MediaStore.setRequireOriginal() no opera sobre estos URIs efimeros.


## 19/07/2026 — CR-05: Saneamiento ContactUseCase (Result<T>) + LocationParser (@Singleton Facade)
- **ContactUseCase.kt**: Migrado a @Singleton con @Inject constructor. getContactDetails() ahora retorna Result<ContactDetails> con column indexing defensivo y catch general. Se eliminaron NPEs silenciosos por contactos corruptos.
- **LocationParser.kt**: Migrado de object estático a @Singleton class con companion object puente (parseFromAddressString) para compatibilidad backward con RouteViewModel L362/L366. Nuevo inferCountryCity(String?): Result<ParsedLocation>. Código muerto (Geocoder, ConcurrentHashMap) eliminado.
- **RouteViewModel.kt**: L675-679 actualizado con onFailure + getOrDefault para mapear Result<ContactDetails>.
- **Tests**: ContactUseCaseTest (2 tests) + LocationParserTest (4 tests) — todos pasando en JVM.
- **Build**: assembleDebug BUILD SUCCESSFUL (55s).

## 19/07/2026 — CR-06: RouteColorUseCase — Gestión de Colores de Rutas Históricas
- **RouteColorUseCase**: Nuevo use case en domain/usecase/ con sanitizeColorHex(), parseHexToColorInt() y paleta RoutePalette (5 colores semánticos). colorInt eliminado del enum — derivación dinámica desde hex.
- **Persistencia SQLCipher**: MIGRATION_12_13 en AppDatabase con ALTER TABLE routes ADD COLUMN color TEXT DEFAULT '#3498DB'. Version bump 12→13.
- **Route.kt**: Nuevo campo color: String con fallback Azul Bitaru. Todos los campos existentes preservados.
- **RouteDao/RouteRepository**: updateRouteColor(routeId, color) agregado en ambas capas.
- **RouteColorPicker.kt**: Componente Compose reutilizable con 5 círculos de color y borde de selección primario.
- **RouteViewModel**: Inyectado RouteColorUseCase; expone updateRouteColor() y getRouteColorInt().
- **Tests**: 3 tests JVM (hex inválido, 6 dígitos, 8 dígitos ARGB).
- **Build**: assembleDebug BUILD SUCCESSFUL (37s).
## 19/07/2026 — CR-07: AddFavoriteFromTrackingUseCase + Favorito Rápido en Tracking
- **RouteRepository.kt**: addSavedLocation() retorna Long (rowId) en lugar de Unit, permitiendo obtener el ID de la ubicación insertada para vincularla a una ruta.
- **AddFavoriteFromTrackingUseCase.kt**: Nuevo @Singleton que valida Location no nula ni (0.0, 0.0), resuelve dirección con GeocoderUseCase.fetchAddress(), inserta SavedLocation con color #FFD700, vincula a ruta activa via addFavoriteToRoute().
- **RouteViewModel.kt**: Inyectado addFavoriteFromTrackingUseCase; nuevo método addQuickFavoriteFromTracking() con Snackbar de éxito/fracaso.
- **MapScreen.kt (FAB 1.5.2)**: Nuevo FloatingActionButton con LocationOn y tint dorado (0xFFFFD54F) en columna de tracking activo (entre waypoint y stop).
- **Tests**: 3 tests unitarios JVM pasando (válido con ruta 7, location nulo, location 0.0/0.0).
- **Build**: testDebugUnitTest (3/3 nuevos passing); assembleDebug BUILD SUCCESSFUL (43s).


## 20/07/2026 — CR-09: Restauración de Diferenciación Cromática de Trayectos (Mapa Térmico)
- **RouteColorUseCase.kt**: Agregado speedToGradientColor() con interpolación HSV continua Azul→Rojo y protección contra división por cero en delta < 0.001f. Implementación pura Kotlin sin android.graphics.Color.
- **RouteSegmenter.kt**: Nueva extension 	oGradientRouteSegments() que genera un segmento por par de puntos consecutivos con color interpolado por velocidad promedio.
- **RouteViewModel.kt**: outeColorUseCase expuesto como público para acceso desde UI.
- **MapScreen.kt**: Migrado a 	oGradientRouteSegments() para renderizado térmico.
- **ExperienceMapScreen.kt**: Migrado a 	oGradientRouteSegments().
- **Test**: 7 nuevos tests unitarios (9 total), todos pasando. BUILD SUCCESSFUL.





## 22/07/2026 — CR-15: Compartir Experiencias con Fotos
- **ShareExperienceUseCase.kt**: UseCase que construye Intent con chooser, adjunta fotos vía FileProvider.
- **ShareExperienceUseCaseTest.kt**: 1 test con Robolectric (intención no nula con lista vacía).
- **RouteViewModel.kt**: Inyectado ShareExperienceUseCase, expuesto photoPathsForCurrentRoute + shareExperience().
- **ExperienceMapScreen.kt**: Botón Share (cian 0xFF4DD0E1) cableado a shareExperience().
- **MainActivity.kt**: LaunchedEffect collector para shareIntentTrigger.
- **Build**: compileDebugKotlin OK, testDebugUnitTest 75/9, assembleDebug OK.

## 22/07/2026 — CR-16: Barra Inferior Unificada y Restructuración de ExperienceMapScreen
- **CAPA 3 eliminada**: Floating dock 50% transparencia removido, liberando 70dp vertical de mapa.
- **Barra 5.2 unificada**: Surface sólida con layout Row(SpaceBetween) conteniendo dos Row(CenterVertically).
- **Izquierda**: ArrowBack (onSurface) + Share (cian) + Export (amber).
- **Derecha**: "Experiencias" (titleMedium Bold) + indicador GPS en tiempo real.
- **BackHandler** agregado para gesto/botón de retroceso del sistema.
- **Correcciones**: Duplicado ) { eliminado, Box close faltante restaurado, } extra eliminado.
- **Build**: compileDebugKotlin OK, testDebugUnitTest 75/9 pre-existing.

## 2026-07-22 — CR-21: Fix de Compartir Fotos — Mapeo Híbrido de URIs y Sincronización Directa
- **ShareExperienceUseCase.kt**: Mapeo híbrido content:// / ile:// / rutas locales. URIs nativas se parsean con Uri.parse() + unCatching; rutas físicas mantienen FileProvider.getUriForFile.
- **RouteViewModel.kt**: Reemplazada guardia withTimeoutOrNull por consulta directa epository.getWaypointsSync(routeId).
- **Causa raíz eliminada**: File("content://...").exists() siempre retornaba false; las fotos se filtraban y solo se enviaba texto plano.
- **Build**: compileDebugKotlin OK, assembleDebug OK, installDebug OK (2 dispositivos Pixel 9 Pro XL).