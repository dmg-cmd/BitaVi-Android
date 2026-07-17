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

