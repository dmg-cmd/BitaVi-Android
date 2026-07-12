[2026/07/12 10:45 - Navegación Jerárquica MD3: TopAppBar + onBack en pantallas auxiliares]
- **ExperienceMapScreen.kt**: Añadido parámetro onBack: () -> Unit + modifier: Modifier = Modifier. Envuelto contenido en Scaffold/TopAppBar con Icons.AutoMirrored.Filled.ArrowBack y título "Experiencias".
- **DetailScreen.kt**: Envuelta Column existente en Scaffold/TopAppBar con Icons.AutoMirrored.Filled.ArrowBack y título "Detalle del Recorrido". Preservado el callback onBack ya existente.
- **SavedLocationsScreen.kt**: Sin cambios (pantalla base de bottom bar, no requiere Up Navigation por MD3).
- **MainActivity.kt**: Cableado onBack = { navController.popBackStack() } en la ruta EXPERIENCE_MAP.
- **Patrón MD3**: onBack expuesto explícitamente en firma de cada Composable para desacoplamiento del NavController, facilitando testing y @Preview.
- **Compilación**: BUILD SUCCESSFUL.


[12/07/2026 - Fases 7-10: Consolidación Perimetral, Purga de Código Muerto y Build 41s]
- **Fase 7 — 4 bloques**: LocationService.kt null guard + intent?. → intent. (6x) + FQNs PreferenceManager/SavedLocation. RouteRepository.kt 7x FQNs → nombre corto. RouteViewModel.kt .debounce(100) → 100.milliseconds. RouteExportUseCase.kt val route eliminado. ExperienceMapScreen.kt condición redundante !isAutoRotationEnabled eliminada.
- **Fase 8 — 10 pasos**: LocationService.kt 4x Elvis muertos (?: ) eliminados + rssi eliminado de onBleContactDetected. RouteAnalyticsScreen.kt routeId/onBack eliminados + caller MainActivity. MapScreen.kt event → _, else if → else. RouteViewModel.kt catch (e: ) → catch (_: ), when (val decision = ) → when (). GeocoderUseCase.kt suspend removido de fetchAddress.
- **Fase 9 — 3 bloques**: ExperienceMapScreen.kt photoItems eliminado + LocalDensity.current.density migrado. MapScreen.kt val res inlinado. SavedLocationsScreen.kt FQN android.net.Uri → import + nombre corto. RouteExchanger.kt FQNs BufferedWriter/OutputStreamWriter contraídos. MainActivity.kt FQNs Intent/Toast eliminados.
- **Fase 10 — Blueprint ajustado**: ShareUseCase.kt .let { it.toUri() } → .toUri(). ExperienceMapScreen.kt ImageRequest.Builder(LocalContext.current) → context. MainActivity.kt android.widget.Toast.LENGTH_SHORT → Toast.LENGTH_SHORT.
- **Optimización de compilación**: Tiempo reducido de >2min a 41s estables (-65%).
- **Declaración Production-Ready**: Dictamen arquitectónico de cierre. Fase 11 declinada (falsos positivos de lint no justifican riesgo de regresión en delegados Compose).
- **Respaldos**: RESPALDO_ANTERIOR/20260711_*.kt (26 archivos).
- **Compilación**: BUILD SUCCESSFUL (41s). Deploy Pixel 9 Pro XL.
[11/07/2026 - Fase 6 Definitiva: Purgas Finales + RouteAnalytics Compose/KTX]
- **Purgas (4 regresiones)**: authenticateWithCrypto (MainActivity.kt), shareCompleteRoute (RouteViewModel.kt + ShareUseCase.kt), insertFavorite (RouteRepository.kt). Eliminación directa sin función intermediaria para evitar bug de paso de arrays en PowerShell.
- **RouteAnalyticsScreen.kt**: L94 Locale.getDefault() → LocalConfiguration.current.locales[0] (lectura reactiva Compose). L46-49,312 6× AndroidColor.parseColor() → String.toColorInt() (KTX). Nuevos imports: LocalConfiguration + toColorInt.
- **Bug corregido**: Remove-Lines con @(@(range)) no funcionaba porque PowerShell desenvuelve arrays anidados. Se reemplazó con for-if directo.
- **Compilación**: gradlew assembleDebug → BUILD SUCCESSFUL (0 errores). Deploy Pixel 9 Pro XL.
- **Respaldos**: RESPALDO_ANTERIOR/20260711_HHMM_RouteAnalyticsScreen.kt y 4 archivos más.
[11/07/2026 - Fase 6 Real: Purgas Regresivas + Locale.US + Optimize Imports]
- **Bloque A (Purgas)**: authenticateWithCrypto (MainActivity.kt), shareCompleteRoute (RouteViewModel.kt + ShareUseCase.kt), insertFavorite (RouteRepository.kt), val activity + 3 imports muertos (ExperienceMapScreen.kt).
- **Bloque B (Locale.US)**: 8 String.format blindados en GroqAiUseCase.kt (3), GeocoderUseCase.kt (1), RouteAnalyticsScreen.kt (4) — previene NumberFormatException en regiones con coma decimal.
- **Bloque C (Imports)**: PhotoPanel + DashboardActividadFlotante eliminados de MapScreen.kt (componentes ya borrados en Fase 5).
- **Método**: Corrección de bug de sintaxis PowerShell (comas en argumentos de función causaron truncamiento de GroqAiUseCase.kt). Backup/restore desde RESPALDO_ANTERIOR para recuperación.
- **Compilación**: gradlew assembleDebug → BUILD SUCCESSFUL (0 errores). Deploy a Pixel 9 Pro XL.
- **Respaldos**: RESPALDO_ANTERIOR/20260711_1827_*.kt (10 archivos).
[11/07/2026 - Fase 6: Purgas Finales (authenticateWithCrypto, shareScreenshot, PhotoInfoWindow, shareCompleteRoute, isFavoriteLinked, saveFavoritePoint)]
- **MainActivity.kt**: Eliminada función uthenticateWithCrypto() (L217-244). Preservada uthenticateSimple() (3 callers vivos).
- **ShareUseCase.kt**: Eliminada shareCompleteRoute() (L48-108): lógica de compartición con mapa + fotos + FileProvider.
- **RouteViewModel.kt**: Eliminadas isFavoriteLinked() (L302-303, single-expression body) y saveFavoritePoint() (L309-318, con geocoder + DB). Preservada cleanDuplicateFavorites() (caller vivo en L130).
- **ExperienceMapScreen.kt**: Eliminadas shareScreenshot() (L173-193, captura decorView + FileProvider) y class PhotoInfoWindow (L484-555, InfoWindow osmdroid con CircleCropTransformation). photoItems preservado.
- **Método**: Reemplazo del brace-counting por eliminación por rango de línea exacto (previene corrupción de estructura).
- **Respaldos**: RESPALDO_ANTERIOR/20260711_*.kt (4 archivos modificados).
- **Compilación**: BUILD SUCCESSFUL (0 errores).
[11/07/2026 - Limpieza de Menú Duplicado: Eliminación de IconButton flotante superior]
- **MainActivity.kt**: Eliminado bloque IconButton (hamburguesa) de la capa superior absoluta (zIndex=99) que se superponía al mapa en NavRoutes.TRACKING. Se removió también el parámetro onMenuClick = { drawerState.open() } de la llamada a MapScreen().
- **MapScreen.kt**: Eliminado parámetro onMenuClick: () -> Unit de la firma del composable. El callback estaba muerto (nunca se invocaba dentro del componente).
- **TopBarLocalizador.kt**: Padding corregido de start = 72.dp (residual del botón eliminado) a horizontal = 16.dp simétrico. La cabecera ahora ocupa el ancho completo balanceado.
- **UX**: Se unifica la navegación MD3. El drawer se abre exclusivamente desde el IconButton ☰ de la barra inferior (NavigationBar), eliminando redundancia y confusión visual.
- **Respaldos**: RESPALDO_ANTERIOR/20260711_0943_MainActivity.kt, _MapScreen.kt, _TopBarLocalizador.kt.
- **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.
[09/07/2026 - Watchdog de Señal GNSS + Dashboard Actividad Flotante]
- **LocationService.kt**: ActivityMode expandido con SIN_SENAL. Nuevo watchdog GPS que monitorea el tiempo desde el último update del GPS_PROVIDER. Si pasan 15s sin tick satelital y el modo actual no es STOPPED/SIN_SENAL, conmuta automáticamente a ActivityMode.SIN_SENAL. Al recuperar señal GPS, restaura el modo cinemático previo (preAlertActivityMode). Variables: lastGpsTickTime, isSignalLost, preAlertActivityMode, watchdogJob.
- **IndicadorTranslucidoContenedor** (en MapScreen.kt): Nuevo parámetro overrideColor: Color con valor por defecto Color(0xFF1C1B1F).copy(alpha = 0.65f) para compatibilidad backward. Permite inyección dinámica de color de alerta.
- **DashboardActividadFlotante.kt** (nuevo en ui/components/): Componente Compose que consume IndicadorTranslucidoContenedor con animación animateColorAsState. En estado normal: fondo oscuro translúcido, modo de tráfico + velocidad. En SIN_SENAL: fondo rojo táctico #DC3545 al 75% alpha, etiqueta "ESTATUS SATELLITE", texto "⚠️ SIN SEÑAL", velocidad oculta.
- **MapScreen.kt**: Dashboard inline reemplazado por DashboardActividadFlotante. Clickable preservado para toggle de dirección.
- **RouteSegmenter.kt**: toSegmentColor() y toSolidColor() expandidos con SIN_SENAL -> rojo #DC3545 para segmentos de ruta grabados durante pérdida de señal.
- **Respaldos**: RESPALDO_ANTERIOR/20260709_LocationService.kt.bak, 20260709_MapScreen.kt.bak.
- **Compilación**: BUILD SUCCESSFUL (0 errores, 0 warnings).

[07/07/2026 - Bloque B: Guarda de Contingencia 3min + Bloque C: Dashboard MD3]
- **StationaryAnchorManager.kt**: Nueva función shouldForceStop() que monitorea accuracy >25m por 3 minutos continuos. Al dispararse, retorna true para forzar parada automática vía stopTrackingWithAutoRestart(). Preserva processAnchor() intacto.
- **LocationService.kt**: Inyección de la guarda en processNewLocation() línea 465, justo después de stationaryAnchorManager.processAnchor(). Si shouldForceStop() retorna true, corta el pipeline con return temprano.
- **MapScreen.kt**: Reemplazo del Surface de telemetría por Card MD3 con:
  - Barra de acento lateral izquierda según modo (ámbar/verde/azul/rojo)
  - Icono 28dp + label español 18sp bold ("Detenido", "Caminando", "En Bicicleta", "En Vehículo") coloreado por modo
  - Velocidad a la derecha en blanco 15sp
  - Stats secundarias (distancia + timer en tracking)
  - Telemetría fina (LAT/LON/ALT) en gris 9sp
- **RouteSegmenter.kt**: Sincronización de paleta: DRIVING rgb(244,67,54) → rgb(255,82,82) (#FF5252), STOPPED rgb(255,235,59) → rgb(255,214,0) (#FFD600). toSegmentColor() y toSolidColor() actualizados.
- **Respaldos**: RESPALDO_ANTERIOR/20260707_1341_StationaryAnchorManager.kt, 20260707_1341_LocationService.kt, 20260707_1341_MapScreen.kt, 20260707_1341_RouteSegmenter.kt.
- **Compilación**: BUILD SUCCESSFUL (1 warning preexistente). Deploy no disponible (sin dispositivo conectado).
[07/07/2026 - Saneamiento Final UI: SavedLocationsScreen + DetailScreen + IconCategory]
- **SavedLocationsScreen.kt**: 8 parches: Color.parseColor → toColorInt() (4 pts), Locale.US en String.format (3 pts), imports KTX+Locale.
- **DetailScreen.kt**: 7 parches: Color.parseColor → toColorInt() (1 pt), Locale.US en String.format (5 pts), imports KTX+Locale.
  - Nota: 6 iconos (TrendingUp, ShowChart, Send x2, KeyboardArrowRight, HelpOutline) presentan deprecación a AutoMirrored pero los equivalentes no están disponibles en la versión actual de la librería Material Icons. Pendiente de resolución con upgrade de dependencias.
- **IconCategory.kt**: Refactor completo: values() → entries, createBitmap KTX, toBitmap como método de instancia, acentos en labels.
- **RouteViewModel.kt**: Auditado — 0 cambios (arquitectura limpia, delegación a UseCases).
- **Respaldos**: RESPALDO_ANTERIOR/20260707_SavedLocationsScreen.kt, 20260707_DetailScreen.kt, 20260707_IconCategory.kt.
- **Compilación**: BUILD SUCCESSFUL.
[07/07/2026 - Saneamiento de IconCategory.kt + MapScreen.kt (infraestructura)]
- **IconCategory.kt**: Refactor completo del diccionario visual:
  - values() → entries (asignación cíclica de arreglos erradicada, Kotlin 1.9+).
  - toBitmap() migrado de companion object estático a método de instancia (diseño OOP idiomático).
  - Bitmap.createBitmap(size, size, Config.ARGB_8888) → createBitmap(size, size) KTX.
  - Etiquetas corregidas: "Gastronomía", "Vehículo", "Automática".
- **MapScreen.kt línea 440**: Llamada actualizada a la nueva firma: IconCategory.fromKey().toBitmap(40, color).
- **RouteViewModel.kt**: Auditado — 0 cambios necesarios (arquitectura limpia).
- **Respaldos**: RESPALDO_ANTERIOR/20260707_IconCategory.kt.
- **Compilación**: BUILD SUCCESSFUL en 22s, 0 warnings.
[07/07/2026 - Saneamiento de Deuda Técnica: MapScreen + FocusFavorite + LocationService]
- **MapScreen.kt** (1037 líneas): Sanación completa de 8 categorías de warnings:
  - Eliminación de variables muertas (waypointName, locationName, isManualMode).
  - Migración a KTX: createBitmap(size, size), String.toColorInt(), Bitmap.toDrawable().
  - Corrección de accesibilidad: v.performClick() en setOnTouchListener.
  - API osmdroid moderna: getPoints() → actualPoints.
  - Eliminación de bloques if vacíos en retainAll (waypointMarkers, favoriteMarkers).
  - Resource hoisting: context.resources elevado fuera del forEach de favoritos.
  - Internacionalización: Locale.getDefault() en String.format de telemetría visible; Locale.US en formatos de color.
  - Color.parseColor() → toColorInt() en todos los selectores de color.
- **FocusFavoriteScreen.kt** (245 líneas): 2 parches de internacionalización:
  - Migración de getDrawable() deprecado a ContextCompat.getDrawable().
  - lowercase() → lowercase(Locale.ROOT) para inmunidad contra el problema de la "I turca".
- **LocationService.kt** (1073 líneas): 4 correcciones de Locale en String.format:
  - Locale.US en logs de telemetría (línea 450), coordenadas para metadata (512), distancia en notificación (790).
  - Locale.getDefault() en formato de tiempo visible al usuario (1039).
- **Respaldos**: RESPALDO_ANTERIOR/20260707_MapScreen.kt, 20260707_FocusFavoriteScreen.kt, 20260707_LocationService.kt.
[05/07/2026 - Fase 2 GPS: Ventana Móvil de 5 Puntos + Filtro Cinemático 15 m/s]
- **Problema**: Las antenas de telefonía pueden entregar coordenadas con saltos absurdos (>100 m/s) que contaminan el odómetro y la base de datos. El filtro existente (MAX_VALID_SPEED_MPS = 45 m/s) era demasiado permisivo y no se aplicaba como pre-filtro de entrada.
- **Solución — Pre-filtro de doble capa al inicio de processNewLocation()**:
  - **Fase 1 (Precisión)**: if (location.accuracy > 50f) return — descarta puntos con precisión degradada antes de cualquier procesamiento.
  - **Fase 2 (Inercia/Velocidad)**: LinkedList<Location> locationWindow como cola fija de 5 posiciones. Velocidad calculada entre locationWindow.last() y el nuevo punto. Si supera MAX_ALLOWED_SPEED_MPS = 15.0 (~54 km/h), se registra un Log.w de advertencia y el punto se descarta por completo.
  - **Ventana deslizante**: Solo los puntos que pasan ambos filtros se agregan a la ventana. Si excede 5 elementos, se poda el más antiguo (removeFirst())
- **Arquitectura**: El bloque se inyectó como pre-filtro en la línea 416 de processNewLocation(), ANTES de cualquier lógica pesada (ActivityMode, ShortStop, BLE, Auto-mode, odómetro, DB). Esto asegura que solo "carne premium" llegue al resto del pipeline.
- **Constante nueva**: MAX_ALLOWED_SPEED_MPS = 15.0 en companion object (equivale a ~54 km/h, límite físico de corrida/caminata).
- **Estructura nueva**: locationWindow: LinkedList<Location>() como campo privado de la clase.
- **Respaldos**: RESPALDO_ANTERIOR/20260705_LocationService.kt.
- **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.

[04/07/2026 - Pantalla Inmersiva Focus Favorite + Postal Táctica]
- **FocusFavoriteScreen.kt**: Reestructuración completa. Se eliminó Scaffold/TopAppBar y se reemplazó por jerarquía Box raíz con dos Box anidados: (1) contenedor capturable con drawWithContent + GraphicsLayer.record que renderiza mapa osmdroid + card translúcida, y (2) botones flotantes (Volver/Compartir) como hijos directos del Box raíz, fuera del recorte de captura.
- **Postal Táctica**: El botón Compartir ahora captura el contenedor vía graphicsLayer.toImageBitmap() → asAndroidBitmap(), guarda PNG en cacheDir/shared_posts/ y despacha Intent.ACTION_SEND con type=image/png vía FileProvider. La imagen resultante NO incluye botones de UI ni barras del sistema.
- **Botones flotantes**: ArrowBack (arriba-izquierda) y Share (arriba-derecha) sobre fondo negro 50% alpha, CircleShape, padding ajustado para notch del Pixel 9 Pro XL.
- **Backup**: RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen.kt.
- **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL.

[REVERTIDO] [04/07/2026 - Arquitectura Opus: Transparencia nativa desde themes.xml + insets correctos]

[04/07/2026 - Rollback y Fusión: v3 (20:15) + bottom=40dp]
- **Rollback completo**: Se deshicieron todos los experimentos del día (FLAG_LAYOUT_NO_LIMITS, arquitectura Opus, diagnósticos rojo/verde).
- **Fusión táctica**: FocusFavoriteScreen.kt restaurado desde RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v3.kt (20:15) — arquitectura Box raíz con GraphicsLayer + watermark elegante + captura PNG.
- **Único parche aplicado**: .padding(16.dp) → .padding(start=16.dp, end=16.dp, bottom=40.dp) en la Card para evitar que el modo inmersivo del Pixel la empuje fuera del viewport.
- **themes.xml**: Mantenido en su estado original (sin atributos de transparencia). enableEdgeToEdge() en MainActivity es suficiente.
- **Lecciones aprendidas**: navigationBarsPadding() depende de insets dinámicos que Android pone a 0 en modo inmersivo. bottom=40.dp fijo es más robusto. Los experimentos con FLAG_LAYOUT_NO_LIMITS y atributos de transparencia global en themes.xml causaron efectos secundarios en toda la app.
- **Entradas [REVERTIDO]**: 2 entradas del historial marcadas.
- **Compilación**: clean + installDebug — BUILD SUCCESSFUL.
- **themes.xml**: Se agregaron 4 atributos de transparencia al tema raíz Theme.MyApplication:
  - android:statusBarColor = @android:color/transparent
  - android:navigationBarColor = @android:color/transparent
  - android:windowTranslucentStatus = false
  - android:windowTranslucentNavigation = false
- **FocusFavoriteScreen.kt**: Reestructuración completa siguiendo la arquitectura Opus:
  - Eliminado FLAG_LAYOUT_NO_LIMITS (causaba parpadeo al redibujar scrim en cada invalidate del mapa).
  - Eliminados imports: android.view.WindowManager, androidx.compose.runtime.DisposableEffect.
  - Bloque de ventana reducido a SideEffect con solo isAppearanceLightStatusBars = false + decorView.setBackgroundColor(BLACK).
  - Mapa envuelto en Box(fillMaxSize) con setOnTouchListener que llama a requestDisallowInterceptTouchEvent(true) para evitar que osmdroid propague relayout de insets al sistema.
  - Card utiliza .navigationBarsPadding() (en lugar de bottom=32.dp) para respetar la barra de gestos inferior.
  - Botones flotantes (back, share) utilizan .statusBarsPadding() en lugar de top=40.dp manual.
  - Watermark restaurado a su versión elegante: context.getString(R.string.app_name).lowercase(), alpha=0.5f, FontWeight.Light, letterSpacing=2.sp.
  - Agregados imports: navigationBarsPadding, statusBarsPadding.
- **Resultado**: El mapa fluye 100% de borde a borde sin parpadeos. Los elementos de UI respetan insets del sistema. La transparencia nace del tema (no del WindowManager runtime).
- **Respaldos**: RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v5.kt, RESPALDO_ANTERIOR/20260704_themes.xml.
- **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.

[REVERTIDO] [04/07/2026 - Transparencia Absoluta con FLAG_LAYOUT_NO_LIMITS]
- **FocusFavoriteScreen.kt**: Se reemplazó el bloque SideEffect (window.statusBarColor = TRANSPARENT) por DisposableEffect(Unit) con FLAG_LAYOUT_NO_LIMITS + onDispose limpia.
  - window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS) ignora todos los insets del sistema (status bar + nav bar), forzando transparencia real en el Pixel 9 Pro XL sin parpadeos.
  - onDispose { window.clearFlags(FLAG_LAYOUT_NO_LIMITS) } restaura el estado al salir de la pantalla, evitando roturas de layout en el resto de la app (teclado, nav bar).
  - window.decorView.setBackgroundColor(BLACK) se mantiene para el frame 0.
  - Se añadió import android.view.WindowManager y import androidx.compose.runtime.DisposableEffect.
- **Padding inferior en la Card**: .padding(16.dp) → .padding(start = 16.dp, end = 16.dp, bottom = 32.dp) para que el watermark "AGENVIAJE" y el contenido no queden bajo la barra de gestos del Pixel 9 Pro XL.
- **Respaldo**: RESPALDO_ANTERIOR/20260704_FocusFavoriteScreen_v4.kt.
- **Compilación**: BUILD SUCCESSFUL + installDebug en Pixel 9 Pro XL.
**[04/07/2026 - Compartir Favorito con Foto Adjunta]**
- **ShareUseCase.kt**: Refactor completo de shareLocation(). Ahora envía un Intent.ACTION_SEND con tipo dinámico: image/* si location.photoPath tiene URI válida (content://), text/plain si no. Se añade FLAG_GRANT_READ_URI_PERMISSION para que la app receptora (WhatsApp/Telegram) pueda leer el stream de la imagen. Formato de texto actualizado: "📍 ¡Te comparto este lugar desde mi bitácora!" + nombre + nota + enlace Google Maps.
- **Arquitectura**: Sin cambios en UI ni ViewModel — el botón Share ya existía en LocationCard (SavedLocationsScreen.kt:698) y llamaba a shareLocation(). photoPath ya viene como content:// URI desde PhotoManager, no necesita FileProvider adicional.
- **Backup**: RESPALDO_ANTERIOR/20260704_ShareUseCase.kt.
- **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL.
**[04/07/2026 - Grosor de Trazado Dinámico con Slider]**
- **PreferencesManager.kt**: Nuevos getStrokeWidth() / setStrokeWidth() con clave "map_stroke_width", default 8f, persistencia SharedPreferences vía edit {}.
- **RouteViewModel.kt**: Nueva variable de estado mapStrokeWidth (mutableStateOf, default 8f). Carga en init desde prefsManager. Método updateStrokeWidth(w) que actualiza el estado y persiste en Dispatchers.IO.
- **MainActivity.kt**: Slider en el menú lateral, insertado justo después de "Protección Biométrica". Rango 4f..14f con steps=5 (saltos: 4, 6, 8, 10, 12, 14). Muestra etiqueta "Grosor del trazado: Xpx". Import añadido: Slider de material3.
- **ExperienceMapScreen.kt / MapScreen.kt**: outilinePaint.strokeWidth ahora lee viewModel.mapStrokeWidth en lugar del hardcoded 8f. Reactivo: cualquier cambio del slider dispara recomposición instantánea.
- **Backups**: RESPALDO_ANTERIOR/20260704_PreferencesManager.kt, _RouteViewModel.kt, _MainActivity.kt, _ExperienceMapScreen.kt, _MapScreen.kt.
- **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL.
**[04/07/2026 - Purga del Debug Overlay en ExperienceMapScreen]**
- **ExperienceMapScreen.kt**: Eliminación completa del bloque DEBUG OVERLAY (líneas 332-339) que mostraba una barra negra semi-transparente con texto verde de telemetría (pts, wp, fav, fotos, load). El mapa y las fotos translúcidas ahora ocupan toda la pantalla de borde a borde sin interferencias visuales.
- **Backup**: RESPALDO_ANTERIOR/20260704_ExperienceMapScreen.kt.
- **Compilación**: BUILD SUCCESSFUL, deploy exitoso en Pixel 9 Pro XL.
**[04/07/2026 - Agrupación Jerárquica de Favoritos con Árbol Animado]**
- **LocationParser.kt** (NUEVO): Objeto con parseFromAddressString() para inferir país/ciudad desde direcciones tipo "Av. de Mayo 1370, C1085 CABA, Argentina" (últimas 2 partes separadas por coma), inferCountryCity() con cache ConcurrentHashMap para geocodificación redundante, y clearCache().
- **RouteViewModel.kt**: Se corrigió saveSpecificLocation() para usar geocoderUseCase.fetchLocationDetails() y almacenar país/ciudad reales (ya no hardcode "Desconocido"/"Desconocida"). Se corrigió refreshLocationDetails() para actualizar address, country y city simultáneamente. Nuevo StateFlow groupedFavorites: Map<String, Map<String, List<SavedLocation>>> derivado de allSavedLocations con inferencia jerárquica vía LocationParser (usa el campo db primero, fallback al parser de dirección).
- **SavedLocationsScreen.kt**: Rediseño completo del listado. Se reemplazó el LazyColumn plano con un árbol expandible: país → ciudad → favoritos, usando AnimatedVisibility para transiciones suaves. Botones ExpandMore/ChevronRight con toggles via mutableStateMapOf. Expansión inicial por defecto (países y ciudades abiertos). cityKey único por combinación "país|ciudad".
- **Backups**: RESPALDO_ANTERIOR/20260704_SavedLocationsScreen.kt, 20260704_RouteViewModel.kt.
- **Respaldos**: LocationParser.kt (creado), SavedLocationsScreen.kt + RouteViewModel.kt (modificados).
**[04/07/2026 - Metadata Temporal en Fotos Favoritas]**
- **RouteViewModel.kt**: Se modificó saveSpecificLocation() (línea 293) para inyectar metadata temporal en el campo comment cuando photoPath != null. Si el usuario añade una foto, se concatena automáticamente "📸 Capturada el dd/MM/yyyy HH:mm" al final del comentario. Si el comentario está vacío, se usa solo la metadata. Si ya tiene texto, se añade con separador \n\n.
- **Arquitectura**: Punto de inyección único en ViewModel, confluencia natural de MapScreen.kt y SavedLocationsScreen.kt. No afecta restauración de backups (usa repository directo) ni ediciones posteriores.
- **Respaldo**: RESPALDO_ANTERIOR/20260704_RouteViewModel.kt.

**[04/07/2026 - Marcador de Paradas Cortas (Short Stop Marker)]**
- **LocationService.kt**: Nuevo sistema de detección de paradas cortas durante el tracking activo. Cuando la velocidad cae por debajo de 2.5 km/h y se mantiene así por 2 minutos consecutivos, se registran las coordenadas y se dispara geocodificación inversa asíncrona vía GeocoderUseCase. Al reanudar la marcha (>2.5 km/h), se calcula la duración total de la parada (incluyendo tolerancia de +3 min) y se inserta automáticamente un nuevo registro en la tabla de Favoritos con comentario "⏱️ Parada automática de X min Y seg" (author="Automático").
- **Variables de estado**: timeStartedStopping, isShortStopDetected, shortStopLat, shortStopLon, shortStopAddress (viaje de ida y vuelta completo con resets en pauseTracking/stopTracking).
- **AppConstants.kt**: Nueva constante STOP_SPEED_THRESHOLD_KMH = 2.5f en object Location.
- **Constantess del companion**: SHORT_STOP_DELAY_MS = 120_000L (2 min de quietud), STOP_TOLERANCE_MS = 180_000L (+3 min de tolerancia post-parada).
- **Resets**: Limpieza total de las 5 variables en pauseTracking() y stopTracking() para evitar estados colgados entre rutas consecutivas.
**[02/07/2026 - Unificación de Formato SAF con generateRouteZipFile]**
- **ExperienceMapScreen.kt**: Se reemplazó la sugerencia de nombre del SAF (Storage Access Framework) de 
oute?.name a ROUTE_{id}_{sanitizedName}_{timestamp}.zip, replicando exactamente el patrón de generateRouteZipFile(). Ahora ambos caminos (Compartir y Guardar como ZIP) usan el mismo formato unificado.
- **RouteExportUseCase.kt**: Se agregó .getOrThrow() en xportRouteAsZip() para que el Result<Unit> del RouteExchanger lance excepción en caso de error, mejorando la propagación de fallos.

**[02/07/2026 - Nombre Único y Ubicación Persistente para ZIPs]**
- **RouteExportUseCase.kt**: Se centralizó la generación del nombre del ZIP en la nueva función generateRouteZipFile(). El nombre sigue el patrón ROUTE_{id}_{nombre}_{timestamp}.zip (ej: ROUTE_5_Paseo_del_Rio_20260702_143052.zip), eliminando colisiones y permitiendo exportar la misma ruta múltiples veces.
- **Sanitización robusta**: El nombre se limpia con 
eplace(Regex("[\s/\\\\?%*:|\"<>]"), "_") + .take(20) para preservar acentos, eñes y caracteres internacionales, eliminando solo caracteres prohibidos por el sistema de archivos.
- **Migración a DIRECTORY_DOCUMENTS**: Los ZIP ahora se guardan en context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) en lugar de cacheDir, haciendo que los archivos persistan tras el reinicio de la app y sean visibles en la carpeta Documentos del explorador de archivos.
- **file_paths.xml**: Se añadió <external-files-path name="my_documents" path="Documents" /> para que FileProvider pueda generar URIs compartibles desde la nueva ubicación.**Historial de Cambios - Proyecto Localizador**

**[Sesión Actual - Menú de Acciones de Ruta y Refactor del Filtro GPS]**
- **RouteOptionsMenu**: Se reemplazaron 7 IconButtons individuales en la TopAppBar de DetailScreen por 3 visibles (Explorar, Favoritos, Editar) + un menú `⋮` (MoreVert) con DropdownMenu. El menú incluye: Compartir ruta (con captura del mapa), Exportar con submenú anidado (GPX/CSV/ZIP) y Eliminar ruta.
- **Eliminar ruta con confirmación**: Nuevo `AlertDialog` de confirmación antes de borrar permanentemente una ruta. Muestra advertencia sobre pérdida de puntos GPS, waypoints y fotos. Al confirmar, redirige al Historial.
- **Diálogo de ayuda en pantalla**: Nuevo `AlertDialog` con 6 `HelpItem` (componente reutilizable con `ImageVector`, título y descripción) que explica cada acción de la pantalla usando los mismos iconos de la UI: Mapa de Experiencia, Vincular Favoritos, Editar nombre, Compartir, Exportar y Eliminar ruta.
- **Refactor del filtro GPS en `LocationService.kt`**: Extracción de `isLocationValid()` como función pura sin efectos secundarios. Se eliminó la asignación de `lastComputedSpeedMps` dentro del filtro (side effect). Nuevas constantes: `MAX_ACCEPTABLE_ACCURACY` de 30m→20m, `MAX_VALID_SPEED_MPS` de 4.5→45 m/s (~162 km/h), `MIN_TIME_DELTA_MS` de 5000→3000ms. Se añadió `_currentAccuracy` StateFlow para exponer la precisión del GPS en vivo a la UI.
- **Actualización de documentación**: Revisión y actualización completa de `HISTORIAL_CAMBIOS.md`, `DOCUMENTACION.md`, `GUIA_USUARIO.md`, `REPORTE_FILTRADO_GPS.md`, `SUGERENCIAS_COMPLETADAS.md` y `funcionamiento.md`.

**[Sesión Anterior - Corrección de Race Condition en Centrado de Mapa]**
- **Sincronización de Cámara y GPS**: Se solucionó un conflicto de prioridad donde el overlay de GPS sobreescribía la animación de la cámara al seleccionar un favorito. Se implementó una desactivación explícita de `followLocation` tanto en la inicialización del mapa (si hay un objetivo activo) como inmediatamente antes de iniciar la animación de centrado en `MapScreen.kt`. Esto garantiza que el mapa se desplace suavemente al favorito elegido sin interrupciones del GPS.

**[Sesión Anterior - Corrección de Compatibilidad y Experiencia de Mapa]**
- **Compatibilidad Avanzada en Importación/Restauración**: Se actualizó el motor de copias de seguridad (`prepareBackupFile` y `restoreFromBackup`) para exportar e importar todos los campos de `SavedLocation`, incluyendo `country` y `city`. Además, el importador ahora parsea dinámicamente archivos con cualquier número de columnas (de 4 a 13+), garantizando que las copias de seguridad de versiones anteriores o intermedias se restauren sin perder información y sin fallos.
- **Control de Cámara y Retardo de Favoritos**: Se perfeccionó el comportamiento de la cámara del mapa en `MapScreen.kt`. Cuando se visualiza un favorito seleccionado, se bloquea el auto-centrado del GPS mediante la desactivación explícita de `locationOverlay.followLocation` en modo manual o de navegación, y se aumentó el tiempo de asentamiento de la cámara a 10 segundos antes de limpiar el objetivo.
- **Bloqueo de Auto-Seguimiento en Navegación**: Se actualizó el bloque `update` de `AndroidView` para asegurar que `locationOverlay.disableFollowLocation()` se ejecute siempre que el usuario esté en modo manual o navegando hacia un punto, evitando saltos bruscos de la cámara.
- **Retorno Manual al GPS**: Se actualizó el botón flotante "Mi ubicación" para que limpie inmediatamente el objetivo manual, reactive el seguimiento GPS (`enableFollowLocation`) y reinicie el temporizador de inactividad manual.

**[Sesión Anterior - Fase 3: Social, Proximidad y Estabilidad Extrema]**
- **Mapa en WhatsApp**: Implementación de captura dinámica del mapa (Bitmap) que se adjunta automáticamente como primera imagen al compartir el trayecto.
- **Modo Automático Blindado**: Refinamiento con 3 confirmaciones consecutivas y filtro de velocidad real (>1.5 km/h) para eliminar inicios falsos por ruido de GPS.
- **Avisos "Cerca de mí"**: Motor de notificaciones de proximidad que alerta al usuario cuando pasa a menos de 100m de un favorito guardado.
- **Buffer de Pre-Grabación**: Sistema de memoria circular de 30 min que recupera el inicio de la ruta si el usuario olvida pulsar grabar o tarda en detectarse el movimiento.
- **Filtro Anti-Salto (Drift)**: Algoritmo de verosimilitud que ignora puntos con velocidades imposibles (>180 km/h en tierra) o saltos de señal de más de 500m.
- **Gestión de Caché Segura**: Actualización de `file_paths.xml` para permitir compartir capturas temporales del mapa de forma segura.

**[Sesión Anterior - Fase 2: Funciones Sociales y Contactos]**
- **Importación Inteligente desde Agenda**: Se ha añadido un nuevo botón de acción rápida (PersonAdd) que abre el selector de contactos del sistema. Al elegir un contacto, la app extrae automáticamente su **Nombre, Teléfono, Email y Dirección Postal**, precargando el diálogo de creación de Favorito para mayor agilidad.
- **Compartir Ubicación Pro**: Nueva función para compartir cualquier favorito mediante un Intent de texto plano. Genera un mensaje formateado con el nombre, dirección, nota y un enlace directo a Google Maps, optimizado para WhatsApp y Telegram.
- **Mejora de Interacción (Quick Actions)**: Los clics en teléfono y email en la lista de favoritos han sido rediseñados usando `FilterChip`. Ahora son botones visuales más claros ("Llamar", "Email") que mejoran la accesibilidad y evitan toques accidentales.
- **Permisos de Contactos**: Actualización del Manifiesto con los permisos `READ_CONTACTS` y `WRITE_CONTACTS` para soportar la integración bidireccional con la agenda.
- **Corrección de GPS en Fotos**: Se reparó el error que ignoraba la ubicación de la foto al guardar un favorito desde el mapa. Ahora, si se activa la opción, el punto se guarda en las coordenadas EXIF de la imagen.
- **Rediseño de Tarjetas**: Botón de compartir movido a la cabecera y visualización de coordenadas GPS sobre la foto del favorito.

**[Sesión Anterior - Refinamiento de Modo Automático e IA Groq]**
- **Detección de Movimiento Inteligente**: Se ha mejorado la fiabilidad del inicio automático mediante 2 confirmaciones consecutivas de movimiento (>25m).
- **Pausa Automática Pro**: Optimización del temporizador de 15 minutos detectando velocidad para evitar pausas accidentales.
- **Migración y Consistencia de IA (Groq)**: Se ha unificado el motor de IA para usar exclusivamente Groq (Llama 3.3). Se corrigieron los endpoints de Retrofit y se implementó un prompt técnico avanzado que analiza distancia, duración, velocidades y waypoints reales. Se ha optimizado el consumo de tokens reduciendo la verbosidad del prompt y la respuesta.
- **Corrección de Orientación en Mapa**: Se habilitó `enableFollowLocation()` en el overlay de ubicación y se forzó la actualización del rumbo (bearing) durante el movimiento. Ahora la flecha roja de dirección rotará correctamente para apuntar hacia donde te desplazas.
- **Limpieza de Código**: Eliminación de modelos redundantes y renombrado de variables `geminiApiKey` a `groqApiKey` para mayor claridad arquitectónica.

**[Sesión Anterior - Estabilización y UX]**
- **Limpieza de Código**: Se ha removido la funcionalidad experimental de descarga de mapas offline debido a problemas de compatibilidad con las políticas de servidores de tiles. La app vuelve a su estado estable anterior.
- **Refactorización de ViewModel**: Reparación y limpieza de `RouteViewModel.kt` para asegurar la compilación correcta y restaurar funciones de exportación y gestión de datos.

**[Sesión Anterior - Seguridad Biométrica y UX]**
- **Soporte Biométrico**: Implementación de `androidx.biometric`. Ahora el usuario puede proteger el acceso a la API Key y el borrado masivo de datos mediante Huella Digital o Rostro.
- **Elimunación del PIN**: Se ha removido el sistema de PIN de 4 dígitos, simplificando la seguridad para que dependa exclusivamente de la biometría del dispositivo (o el método de desbloqueo del sistema).
- **Toggle de Seguridad**: Se añadió un interruptor en el menú lateral para activar/desactivar la protección biométrica globalmente.
- **MainActivity Refactor**: Migración de `ComponentActivity` a `FragmentActivity` para dar soporte a los diálogos biométricos del sistema.

**[Sesión Anterior - Funciones Sociales y Personalización Total]**
- **Selector de Color Libre**: Se ha añadido un Slider de Matiz (Hue) en todos los diálogos de creación y edición de puntos. Ahora el usuario puede elegir CUALQUIER color para sus estrellas, no solo los 6 predefinidos.
- **Exportación a Contactos**: Nuevo botón "A Contactos" en los favoritos que permite guardar los datos directamente en la agenda del teléfono (Nombre, Tel, Email, Dirección y Comentario).
- **Interacción Directa**: Los campos de teléfono y email en la lista de favoritos ahora son clicables; al tocarlos se abre el marcador telefónico o el redactor de correos automáticamente.
- **Consolidación de Protocolo**: Revisión y actualización de los archivos de instrucciones y sugerencias.

**[Sesión Anterior - Eliminación Definitiva de Errores Geográficos]**
- **Blindaje contra gRPC UNAVAILABLE**: Se eliminó totalmente la posibilidad de que aparezcan errores técnicos en las direcciones. Ahora, si el servicio de Google falla o está saturado, la app muestra automáticamente las coordenadas **GPS (Lat, Lon)** formateadas.
- **Limpieza de Favoritos**: El botón de refresco (lápiz -> actualizar) ahora detecta automáticamente si el favorito tiene un error viejo y lo sustituye por la ubicación real.
- **Inteligencia en Ríos**: Mejora en la prioridad de nombres para zonas de agua y kilómetros de ruta.

**[Sesión Anterior - Edición de Favoritos y Geocodificación Náutica]**
- **Navegación en Ríos y Campo**: Se mejoró el sistema de identificación de lugares. Ahora, si no hay una calle cerca (ej. estás en un río o zona rural), la app prioriza el nombre del lugar o río (ej. "Río Paraná Km 400") en lugar de dar error.
- **Corrección de "UNAVAILABLE"**: Se eliminó el mensaje de error técnico. Si el servicio de Google falla, la app ahora muestra las coordenadas GPS directamente para mantener la utilidad.
- **Herramienta de Refresco**: Se añadió un botón de **"Actualizar País/Ciudad"** dentro del panel de edición (lápiz) para corregir favoritos que hayan quedado mal guardados por falta de señal.
- **Edición de Favoritos**: Funcionalidad completa para cambiar nombre, autor, comentarios y color de estrella en puntos ya existentes.

**[Sesión Anterior - Estrellas de Colores y Perfeccionamiento de Fotos]**
- **Personalización de Colores**: Implementación de un selector de colores (Dorado, Verde, Azul, Rojo, Violeta, Naranja) para Favoritos y Puntos de Ruta. Los marcadores en el mapa ahora reflejan estas preferencias.
- **Visualización en Lista**: Las tarjetas de Favoritos muestran el icono de la estrella en el color correspondiente.
- **Mejora de Fotos (EXIF)**: La app detecta automáticamente las coordenadas GPS de las fotos y permite al usuario guardar el punto en la ubicación original de la imagen.
- **Filtro de Duplicados**: El sistema de restauración de backup ahora evita duplicar favoritos y rutas existentes.
- **Base de Datos V7**: Actualización del esquema para incluir el campo `color`.
- **Protocolo de Idioma**: Configuración del agente para comunicación y pensamiento exclusivo en Español.

**[Sesión: Consolidación de Proyecto y Protocolo de Agente]**
- **Unificación de Historiales**: Se consolidaron los archivos `historial_cambios.md`, `historial-cambios.md` y `HISTORIAL_CAMBIOS.md` en este único documento centralizado.
- **Protocolo de Documentación**: Establecimiento de la norma de actualización obligatoria de `DOCUMENTACION.md`, `SUGERENCIAS.md` e `HISTORIAL_CAMBIOS.md` tras cada tarea.
- **Gestión de Sugerencias**: Creación de `SUGERENCIAS_COMPLETADAS.md` para mantener limpio el archivo de tareas pendientes sin perder el registro de lo logrado.
- **Manual de Instrucciones**: Actualización de `instrucciones_agente.md` con pautas estrictas de idioma (Español) y hoja de ruta paso a paso.

**[Sesión Anterior - Blindaje Multimedia y Restauración Total]**
- **Respaldo Físico de Fotos**: Se rediseó el motor de exportación ZIP para incluir físicamente los archivos de imagen. Ahora las fotos viajan dentro del respaldo y se restauran automáticamente en el nuevo dispositivo.
- **Gestión Inteligente de Galería**: Al elegir una foto de la galería (Google Fotos), la app ahora realiza una copia local en su almacenamiento privado. Esto evita que la foto se pierda en la app si el usuario la borra de su carrete personal.  
- **Corrección de Cámara y Permisos**: Implementación de un flujo de solicitud de permisos explícito para la cámara, eliminando cierres inesperados en Android 13/14. Se optimizó el FileProvider para compartir archivos de imagen de forma segura.  
- **Control de Mapa y Zoom**: Se implementó un temporizador de 12-15 segundos que suspende el auto-centrado del GPS cuando el usuario interactúa manualmente con el mapa o selecciona un favorito.  
- **Flecha de Grabación Resaltada**: El icono de ubicación cambia a color  **ROJO** intenso durante la grabación para mejorar la visibilidad y diferenciar el estado de seguimiento activo.  
- **Integración con Google Reviews**: Nuevo botón "Publicar Opinión" que copia el comentario del favorito al portapapeles y abre la ficha del lugar en Google Maps para facilitar la reseña.  
**[Sesión Anterior - Estabilización, Seguridad e IA]**  
- **Organización por Regiones**: Los favoritos ahora se agrupan automáticamente por  **País** y  **Ciudad** mediante geocodificación inversa.  
- **Seguridad por PIN**: Protección de la API Key y funciones de borrado masivo mediante un PIN de 4 dígitos personalizable (por defecto: 0000).  
- **Migración a Llama 3 (Groq)**: Sustitución de Gemini por Llama 3 para un análisis de rutas instantáneo y sin errores de región/versión.  
- **Centro de Control Lateral**: Creación del menú de "3 rayitas" para organizar todos los ajustes de forma profesional.  
- **Guía de Ayuda Visual**: Inclusión de un manual de usuario con iconos reales dentro de la aplicación.  
**[Sesión Anterior - Automatización y GPX]**  
- **Vigilancia Inteligente**: El Modo Automático ahora mantiene un servicio persistente que inicia la grabación al detectar movimiento (>25m) y la detiene tras 15 min de inactividad.  
- **Importador Profesional**: Procesamiento de archivos GPX gigantes mediante Streaming (XmlPullParser), evitando errores de memoria.  
- **Estadísticas Técnicas**: Cálculo automático de Velocidad Máxima y Promedio, visualizadas en el historial y en gráficos de altitud profesionales (MPAndroidChart).
**[02/07/2026 - Recalibración de Umbrales de Modo Automático]**
- **AppConstants.kt**: Se recalibraron los umbrales de detección de movimiento para el arranque automático:
  - AUTO_START_MIN_SPEED_KMH: 2.0 → **3.6 km/h** (~1.0 m/s, paso humano firme). Antes era tan bajo que cualquier ruido GPS disparaba el flag.
  - AUTO_START_SUSTAINED_MS: 5000 → **15000 ms** (15 segundos de continuidad). Ahora filtra correctamente el jitter de interiores y falsos positivos.
- **Impacto**: Cero cambios en lógica concurrente. Solo 2 líneas de constantes.
**[03/07/2026 - Recalibración Urbana de Umbrales de Modo Automático]**
- **AppConstants.kt**: Se recalibraron los umbrales de detección para adaptarlos a la física real de una caminata urbana con GPS fluctuante:
  - AUTO_START_MIN_SPEED_KMH: 3.6 → **2.5 km/h** (~0.7 m/s). Se reduce el umbral para tolerar baches de velocidad por árboles, edificios y semáforos sin resetear el contador.
  - AUTO_START_SUSTAINED_MS: 15000 → **10000 ms** (10 segundos). Dispara el tracking más rápido (~media cuadra) manteniendo el filtro contra falsos positivos.
  - INACTIVITY_THRESHOLD_MILLIS: 	oMillis(15) → **	oMillis(5)** (5 minutos). Reduce el autostop de 15 min a 5 min, estándar de la industria, ahorrando batería al finalizar el recorrido.
- **Impacto**: Sin cambios en lógica concurrente. Solo 3 líneas de constantes. Backupeado en RESPALDO_ANTERIOR/20260703_AppConstants.kt.
**[03/07/2026 - Súper ZIP Social: Empaquetado Estructurado con GPX + CSV + Media]**
- **RouteExchanger.kt**: Se unificó la exportación ZIP bajo xportRouteAsZip() con el nuevo árbol:
  - 	rack.gpx: Coordenadas estándar GPX 1.1 (antes 
uta_telemetria.gpx).
  - 	elemetry.csv: Matriz con columnas 	imestamp_ms,latitude,longitude,altitude_m,speed_ms,speed_kmh,activity_mode. Incluye header metadata (# Route, # Activity Mode, # Total Points). speed_kmh redondeado a 2 decimales con String.format.
  - media/: Carpeta con fotos de waypoints y favoritos (antes multimedia/).
- **exportarRutaCompleta()**: Marcado como @Deprecated("Usa exportRouteAsZip()"). Sin llamadas activas restantes.
- **RouteExportUseCase.kt**: saveZipToUri() y xportZip() migrados a xportRouteAsZip().getOrThrow().
- **Impacto**: Cero cambios en lógica de negocio. Solo renombres de entries ZIP, mejora del CSV y deprecación segura. Backups en RESPALDO_ANTERIOR/20260703_RouteExchanger.kt y 20260703_RouteExportUseCase.kt.
**[03/07/2026 - Compilación Barra Inferior Unificada]**
- **DetailScreen.kt**: Se eliminó Scaffold+TopAppBar (topBar con navegación, acciones y RouteOptionsMenu) y se reemplazó por Column directa. Se eliminó innerPadding de Scaffold. Se agregó private a un formatDuration(ms: Long) para resolver conflicto con HistoryScreen.
- **ExperienceMapScreen.kt**: Se eliminó Scaffold+TopAppBar con shareScreenshot() y saveZipLauncher. El contenido ahora está envuelto en Box(fillMaxSize) directo. Se eliminó el cierre extra del trailing lambda del Scaffold.
- **MainActivity.kt**: Se reparó import mangled Localeimport → separado en import java.util.Locale e import androidx.compose.material.icons.filled.Warning.
- **Compilación**: BUILD SUCCESSFUL + APK instalado en dispositivo.

**[03/07/2026 - Corrección Ergonómica Bottom Bar (Pixel 9)]**
- **MainActivity.kt**: Se reemplazó .height(48.dp) fijo por .defaultMinSize(minHeight = 72.dp) + .navigationBarsPadding() + .padding(horizontal = 16.dp, vertical = 8.dp) en el Row principal de la bottom bar. Se agregó horizontalArrangement = Arrangement.SpaceBetween.
- **Imports**: Se agregaron defaultMinSize y 
avigationBarsPadding del paquete oundation.layout.
- **Resultado**: Barra flota sobre la zona de gestos de Android 16; botones ☰ y ? se alejan de las curvas físicas del panel.

**[03/07/2026 - Panel de Fotos Translúcido con Animación]**
- **ExperienceMapScreen.kt**: Fondo del panel lateral cambiado de Color(0xEE1A1A2E) (93% opaco azul noche) a Color(0x80000000) (50% negro translúcido). El mapa ahora se ve por detrás del panel.
- **Animación**: Envuelto en AnimatedVisibility con adeIn + slideInHorizontally / adeOut + slideOutHorizontally para transición suave al aparecer/desaparecer la galería.
- **Imports**: Agregados AnimatedVisibility, adeIn, adeOut, slideInHorizontally, slideOutHorizontally.

**[03/07/2026 - Anti-Doze: GPS reactivo en segundo plano]**
- **LocationService.kt**: Agregado setMaxUpdateDelayMillis(0) al LocationRequest.Builder para anular el batching de Doze mode en Google Play Services.
- **LocationService.kt**: Intervalo pasivo reducido de 10s/5m a 5s/2m para detección más rápida del auto-arranque.
- **LocationService.kt**: Agregado Watchdog GPS silencioso en el timer que reinicia el canal de ubicación si onLocationResult no se dispara por más de 120s en modo pasivo.

**[03/07/2026 - LaunchedEffect puente para DetailAction]**
- **DetailScreen.kt**: Agregado LaunchedEffect(viewModel.detailAction) que escucha las acciones disparadas desde la bottom bar unificada y las traduce a estados locales (showSheet, showEditDialog, showDeleteDialog, showHelpDialog), reseteando la acción a 
ull inmediatamente después de procesarla.
- **Causa raíz**: Al eliminar el Scaffold/TopAppBar de DetailScreen, los botones BookmarkBorder, Edit y RouteOptionsMenu migraron a la bottom bar (MainActivity.kt), pero DetailScreen nunca recibió las intenciones. El detailAction quedaba emitido al vacío.
- **Archivos modificados**: DetailScreen.kt
- **Archivos respaldados**: RESPALDO_ANTERIOR/20260703_DetailScreen.kt
- **Estado**: BUILD SUCCESSFUL + deploy exitoso.

**[03/07/2026 - RouteAnalyticsScreen: telemetría temporal dedicada]**
- **CREADO** RouteAnalyticsScreen.kt: Nueva pantalla auxiliar con gráficos de rendimiento sincronizados en el eje X (tiempo transcurrido MM:ss).
  - Perfil de Elevación: altura (m) vs tiempo.
  - Perfil de Ritmo: velocidad (km/h) vs tiempo.
  - Color de línea según activityMode (🟢 Caminata, 🔵 Ciclismo, 🔴 Auto).
  - Ejes X sincronizados entre ambos gráficos para análisis cruzado.
  - Usa MPAndroidChart con IndexAxisValueFormatter.
- **DetailScreen.kt**: 
  - Eliminado mapa pequeño superior + mapView + DisposableEffect + LaunchedEffect(points).
  - Eliminados imports osmdroid.
  - Eliminada sección de gráficos antiguos (índice vs valor).
  - Nuevas estadísticas consolidadas en grid 2×2: Distancia, Tiempo en Movimiento, Vel. Promedio, Ritmo.
  - Nuevo botón "Ver Gráficos de Rendimiento" → navega a RouteAnalyticsScreen.
  - Nuevo parámetro onAnalytics: (Int) -> Unit.
  - Nueva función helper ormatMovingTime().
- **NavRoutes.kt**: Agregado ROUTE_ANALYTICS + 
outeAnalytics() + a uxiliaryRoutes.
- **MainActivity.kt**: Nuevo estado currentRouteAnalyticsId, nuevo composable(NavRoutes.ROUTE_ANALYTICS), callback onAnalytics en DetailScreen.
- **Estado**: BUILD SUCCESSFUL + deploy exitoso.

**[03/07/2026 - RouteAnalyticsScreen: Telemetría temporal con RangeSlider y análisis de tramos]**
- **RouteAnalyticsScreen.kt (creado)**: Pantalla dedicada con dos charts MPAndroidChart (Elevación y Velocidad vs Tiempo transcurrido en mm:ss).
- **Eje X sincronizado**: granularity=60f, setLabelCount(8,true), IndexAxisValueFormatter con elapsedLabels.
- **Header informativo**: Muestra hora de inicio (HH:mm), duración total y cantidad de puntos GPS.
- **RangeSlider + LimitLines**: Slider de doble pulgar para seleccionar tramo, con líneas punteadas blancas sincronizadas en ambos charts.
- **Panel de análisis de tramo**: Vel. Promedio, Desnivel acumulado, Ritmo (min/km), Modo predominante del tramo (Detenido/Caminata/Ciclismo/Automóvil por umbrales de velocidad).
- **DetailScreen.kt**: Eliminado mapa, gráficos antiguos y estadísticas reemplazadas por grid 2×2 + botón "Ver Gráficos de Rendimiento".
- **Correcciones compilación Kotlin 2.0.0**: sliderRange.end → endInclusive, RangeSlider values → value, emojis hex literal \U → carácter Unicode real.
- **Estado**: BUILD SUCCESSFUL + deploy en Pixel 9 Pro XL.

[05/07/2026 - Fase 1: Doble Capa de Precisión Ignición Bolsillo]
- **Problema diagnosticado**: Cuando el Pixel 9 Pro XL está en el bolsillo, la precisión GPS degrada a 25-50m. El filtro isLocationValid() (accuracy ≤ 20m) descartaba TODOS los puntos para el odómetro, aunque la flecha del mapa se movía (vía _latestLocation + MyLocationNewOverlay independiente de osmdroid).
- **Solución implementada — Doble Capa de Precisión**:
  - **Capa A (Odómetro)**: Nueva constante TOLERANT_ACCURACY = 50f (MAX_POCKET_ACCURACY_METERS en AppConstants). La distancia suma con precisión de hasta 50m, tolerando el bolsillo.
  - **Capa B (Base de Datos)**: Se mantiene el filtro estricto MAX_GPS_ACCURACY = 20f. Solo los puntos de alta precisión se guardan en la DB, preservando líneas limpias en el mapa final.
- **Cambio 2 — Sensibilidad de movimiento**: AUTO_STOP_SPEED_MPS bajado de 0.5f a 0.3f (~1 km/h) en AppConstants.kt para que el tracking no se detenga al caminar lento en el bolsillo.
- **Cambio 3 — Anti Auto-Stop**: lastUserActivityTimeMillis ahora se actualiza en cada tick del GPS (LocationService.kt:411), no solo con ACTION_SCREEN_ON. El auto-stop por inactividad de 5 min no se dispara mientras el teléfono se mueve aunque esté en el bolsillo.
- **Cambio 4 — Timer reactivo**: delay(5000L) → delay(1000L) en el timer de tracking. El contador de tiempo ahora se siente instantáneo al sacar el celular.
- **Archivos modificados**: AppConstants.kt, LocationService.kt.
- **Respaldos**: RESPALDO_ANTERIOR/20260705_AppConstants.kt, 20260705_LocationService.kt.[06/07/2026 - Bloque de Contenci�n Cr�tica: Parche de Estabilizaci�n 5 puntos]
- **A01 � Cast inseguro context as MainActivity**: Reemplazado `context as MainActivity` (ClassCastException potencial en ContextWrapper) por extensi�n defensiva `Context.findActivity(): MainActivity?` que recorre recursivamente la jerarqu�a de ContextWrapper. Usos de `authenticateSimple` migrados a safe call `?.`.
- **A02 � Force unwrap !! en drawables**: Eliminados 3 `!!` en MapScreen.kt (personIcon, directionIcon, getCachedIcon). Reemplazados por `val d = getDrawable() ?: error("mensaje")` con fallback defensivo y `mutate().apply { setTint() }` correcto.
- **A03 � Force unwrap route!! en DetailScreen**: Reemplazado `route!!.endTime - route!!.startTime` por `route?.let { r -> ... } ?: 0L` con smart cast seguro.
- **A04 � Reconstrucci�n pesada de polil�neas en AndroidView**: Optimizado el bloque `update` de AndroidView. Ahora usa diffing incremental: solo reconstruye todas las polil�neas cuando cambia la estructura de segmentos (modo de actividad). En caso normal (nuevo punto GPS), solo a�ade los puntos delta al �ltimo segmento existente. Elimina ANR potencial en rutas largas.
- **A05 � Permiso VIBRATE faltante**: Agregado `<uses-permission android:name="android.permission.VIBRATE" />` al AndroidManifest.xml para evitar SecurityException en Android 13+ al usar AlertNotifier.
- **Archivos modificados**: MainActivity.kt, MapScreen.kt, DetailScreen.kt, AndroidManifest.xml.
- **Respaldos**: RESPALDO_ANTERIOR/20260706_MainActivity.kt, 20260706_MapScreen.kt, 20260706_DetailScreen.kt, 20260706_AndroidManifest.xml.


[06/07/2026 - Sprint v3.0: CinematicFilterManager — Filtro Cinemático Extraído]
- **Nuevo archivo**: com.example.location.manager.CinematicFilterManager.kt
- **Propósito**: Extraer la lógica de ventana deslizante GPS y filtro de velocidad de LocationService.kt a una clase inyectable, testeable y desacoplada.
- **API pública**: ilter(location): Boolean — evalúa el punto contra la ventana cinemática (anti-(0,0), anti-salto de antena con tolerancia en curvas). 
eset() — limpia la ventana.
- **Configurable**: maxAllowedSpeedMps (15.0 por defecto) y windowSize (5).
- **Integración**: No rompe el pipeline existente. Devuelve Boolean simple; LocationService decide el resto (mode, persistencia, BLE).
- **Build**: BUILD SUCCESSFUL sin warnings.
- **Respaldos**: RESPALDO_ANTERIOR/20260706_CinematicFilterManager.kt.

[06/07/2026 - Sprint v3.0: StationaryAnchorManager — Filtro de Anclaje Estacionario]
- **Nuevo archivo**: com.example.location.manager.StationaryAnchorManager.kt
- **Propósito**: Mitigar el "paseo fantasma" por rebote de señal en interiores. Cuando el usuario está quieto >2 min y la precisión GPS empeora (>20m), congela la posición, el odómetro y las coordenadas en DB hasta que una ráfaga de velocidad >0.5 m/s rompa el anclaje.
- **API pública**: processAnchor(currentLocation): Location — devuelve ubicación anclada o la original. 
eset() — limpia estado al detener tracking.
- **Refinamientos aplicados**:
  - System.currentTimeMillis() en vez de location.time para medir duración de inactividad.
  - Guarda ccuracyAnchorThreshold = 20f: solo ancla si la precisión se degrada (interiores).
  - Liberación con histéresis: speed > speedThresholdMps * 2 (0.5 m/s) evita flapping.
- **Inyección en LocationService.kt**: Llamado tras CinematicFilterManager.filter() en el pipeline. Afecta odómetro, DB y velocidad computada. No afecta _latestLocation (UI blue dot).
- **Reset**: stationaryAnchorManager.reset() en stopTracking().
- **Build**: BUILD SUCCESSFUL (0 errores). **Deploy**: installDebug exitoso en Pixel 9 Pro XL.
- **Respaldos**: RESPALDO_ANTERIOR/20260706_StationaryAnchorManager.kt, 20260706_LocationService_beforeAnchor.kt.

[06/07/2026 - Bloque C: Marca de Agua Adaptativa + Circulo Inercial]
- **Nuevo archivo**: ui/components/BrandWatermark.kt - Composable que muestra "Agenviaje - v3.0.0" con MaterialTheme.colorScheme.onSurfaceVariant y alpha(0.6f). Contraste dinamico MD3 dia/noche.
- **Req. #08 - Circulo Inercial**: Reemplazados personIcon y directionIcon por circleIcon programatico (Bitmap 48x48 via Canvas + Paint). Circulo con relleno (rojo tracking, verde reposo) y borde blanco. directionIcon usa el mismo circulo, eliminando rotacion magnetica. MyLocationNewOverlay intacto.
- **Req. #09 - Marca de Agua**: Inyectada en Box raiz de MapScreen.kt con Alignment.BottomStart, margin bottom=100dp.
- **Archivos**: MapScreen.kt (iconos + watermark), BrandWatermark.kt (nuevo).
- **Build**: BUILD SUCCESSFUL (0 errores). Deploy: installDebug exitoso en Pixel 9 Pro XL.

[06/07/2026 - Bloque B — Geofencing Poligonal: Bounding Box Argentina]
- **AppConstants.kt**: Nuevo objeto Geofencing anidado con constantes de bounding box:
  - MIN_LATITUDE = -55.0, MAX_LATITUDE = -20.0, MIN_LONGITUDE = -75.0, MAX_LONGITUDE = -53.0
  - Función isWithinBounds(lat, lon): Boolean usando ranges de Kotlin (closed range, sin !!).
- **GeocoderUseCase.kt**: Nueva función isValidCoordinate(lat, lon): Boolean con clamping Float→Double para sanitizar entrada. Guarda defensiva en etchLocationDetails() y etchAddress(): si la coordenada cae fuera del bounding box o es (0.0, 0.0), retorna inmediatamente "GPS: Fuera de cobertura" (fetchLocationDetails) o null (fetchAddress).
- **Impacto**: Cero cambios en LocationService, Room DB, ViewModel o UI. La validación ocurre en la capa de GeocoderUseCase, antes de que el dato se consolide en el repositorio. Elimina puntos corruptos en el Atlántico y rebotes de red transfronterizos.
- **Respaldos**: RESPALDO_ANTERIOR/20260706_AppConstants_Geofencing.kt, 20260706_GeocoderUseCase_Geofencing.kt.
- **Compilación**: BUILD SUCCESSFUL (0 errores). Deploy: installDebug exitoso en Pixel 9 Pro XL.

[06/07/2026 - Frente #01: ActivityRecognitionManager — Arranque Automático por Transiciones Físicas]
- **Problema**: El auto-arranque existente (solo por velocidad GPS en processNewLocation()) no reaccionaba en frío. GPS pasivo no recibe updates frecuentes, y Android 12+ puede matar el listener de ubicación en segundo plano.
- **Solución — Sistema de reconocimiento de actividad física con Google Play Services**:
  - **ActivityRecognitionManager.kt** (NUEVO): Clase en com.example.location.manager que encapsula la API de ActivityTransition. Suscribe/desuscribe PendingIntent con FLAG_MUTABLE (requerido en Android 12+). Monitorea transiciones ENTER/EXIT de STILL, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE.
  - **ActivityTransitionReceiver.kt** (NUEVO): BroadcastReceiver en com.example.location.receiver que captura los eventos del SO y puentea a LocationService via ACTION_AUTO_START_FROM_TRANSITION. Usa startForegroundService() para Android 12+.
  - **LocationService.kt**: Nuevas constantes ACTION_AUTO_START_FROM_TRANSITION + EXTRA_DETECTED_ACTIVITY en companion object. Handler en when(action) que llama a handleAutoStartFromTransition(). Manager inicializado en onCreate() y detenido en shutdownService().
  - **AndroidManifest.xml**: Agregado <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" /> y <receiver android:name=".location.receiver.ActivityTransitionReceiver" android:exported="false" />.
  - **MainActivity.kt**: Nuevo ctivityRecognitionPermissionLauncher (registerForActivityResult). Solicitud runtime de ACTIVITY_RECOGNITION en el flujo de permisos escalonados (API 29+).
- **Arquitectura**: ActivityRecognitionManager vive en LocationService (no en UI), asegurando persistencia aunque la app se cierre. MainActivity solo maneja la solicitud de permiso.
- **Respaldos**: RESPALDO_ANTERIOR/20260706_LocationService_ActivityRecognition.kt, 20260706_MainActivity_ActivityRecognition.kt, 20260706_AndroidManifest_ActivityRecognition.xml.
- **Compilacion**: BUILD SUCCESSFUL (0 errores). **Deploy**: installDebug exitoso en Pixel 9 Pro XL.

[07/07/2026 - Frente #08: Iconos Dinámicos y Migración DB v11→v12]
- **AppDatabase.kt**: Versión 11→12 con MIGRATION_11_12 que ejecuta ALTER TABLE saved_locations ADD COLUMN iconCategory TEXT NOT NULL DEFAULT 'DEFAULT'. AddMigrations actualizado para incluir la nueva migración. fallbackToDestructiveMigration preservado como red de seguridad.
- **IconCategory.kt** (EXISTENTE): Enum con 7 categorías (DEFAULT/GASTRONOMIA/TRANSPORTE/AVION/PARQUE/TRABAJO/AUTOMATIC). Cada una con key String, icon ImageVector, label String. Companion con fromKey() lookup + toBitmap() que genera círculo relleno programático con borde blanco para osmdroid.
- **SavedLocation.kt**: Entidad Room con nuevo campo iconCategory: String = "DEFAULT". Compatible con registros existentes vía NOT NULL DEFAULT.
- **RouteRepository.kt**: addSavedLocation() ahora acepta parámetro iconCategory: String = "DEFAULT" con propagación al constructor de SavedLocation. addWaypoint() corregido (iconCategory removido, inexistente en Waypoint).
- **RouteViewModel.kt**: saveSpecificLocation() ahora acepta parámetro iconCategory con named parameter en la llamada a repository.
- **LocationService.kt**: Auto-stop markers ahora usan iconCategory = "AUTOMATIC" para distinguir paradas automáticas de favoritos manuales.
- **MapScreen.kt**: Marcadores osmdroid usan IconCategory.toBitmap() programático en vez de getCachedIcon con resource drawable. Marker.icon recibe BitmapDrawable con círculo coloreado según loc.color + categoría.
- **SavedLocationsScreen.kt**: Icono en lista usa IconCategory.fromKey(location.iconCategory).icon en lugar de Icons.Default.Star estático.
- **DetailScreen.kt**: Icono en lista de favoritos vinculados usa IconCategory.fromKey(fav.iconCategory).icon en lugar de Icons.Default.Star estático.
- **Backups**: RESPALDO_ANTERIOR/20250707_*.kt para AppDatabase, IconCategory, MapScreen, SavedLocationsScreen, DetailScreen, RouteViewModel, RouteRepository.
- **Compilación**: BUILD SUCCESSFUL (0 errores). **Deploy**: installDebug exitoso en Pixel 9 Pro XL.






[10/07/2026 - Refactor BottomBar MD3: NavigationBar nativa con colores de alta luminancia]
- **MainActivity.kt**: Reemplazo completo de la Row personalizada (líneas 798-961) por NavigationBar MD3 estándar:
  - Inyectados colores de alta luminancia anti-reflejo por NavigationBarItem: Mapa (Azul Satelital #3498DB), Favoritos (Verde Esmeralda #2ECC71), Historial (Azul Satelital #3498DB).
  - Botón de Ayuda transformado a 4º NavigationBarItem en Rojo Transmisor (#E74C3C) con onClick directo a showHelpDialog.
  - GPS Capsule preservada en extremo derecho con Surface nativa (RoundedCornerShape 50%) y lógica de precisión existente.
  - Menú hamburguesa (zIndex 99f) en capa superior absoluta verificado: márgenes relativos no colisionan con AndroidView de osmdroid.
  - Navegación dinámica preservada: Detail (4 IconButtons + RouteOptionsMenu) y ExperienceMap (Share/Save ZIP).
- **Importaciones**: Añadido NavigationBarItemDefaults para configuración de colores por ítem.
- **Iconos**: Favoritos corregido a Icons.Default.Place (compatible con la versión actual de Material Icons).
- **Correcciones**: Replace → 
eplace en la lambda de exportación ZIP.
- **Respaldos**: RESPALDO_ANTERIOR/20260710_MainActivity.kt.
- **Compilación**: BUILD SUCCESSFUL (0 errores).

[11/07/2026 - Corrección Estructural: Efecto Compresión en BottomBar (Opción C)]
- **Problema**: Los botones centrales (Mapa, Favoritos, Historial, Ayuda) sufrían colapso de espacio en el extremo derecho de la barra inferior en Pixel 9 Pro XL. Causa raíz: mezcla de NavigationBarItem + IconButton + Row intrusa con GPS capsule dentro del NavigationBar MD3, rompiendo la distribución proporcional equitativa.
- **Solución**: Reestructuración completa de bottomBar en 3 sub-capas planas dentro de Column:
  - **Sub-capa Alfa**: Barra de tracking activo (preservada con estado reactivo real iewModel.isTrackingLive)
  - **Sub-capa Beta**: Fila flotante contextual que aísla la cápsula GPS dinámica (con precisión latestLocation?.accuracy) y los botones contextuales de DETAIL/EXPERIENCE_MAP fuera del NavigationBar
  - **Sub-capa Gamma**: NavigationBar plana con 4 NavigationBarItems puros (Menu/Mapa, Favoritos, Historial, Ayuda) sin contenedores intermedios ni restricciones de ancho rígidas
- **Preservado**: Tracking bar, GPS capsule dinámica con código de colores (verde <10m, rojo ≥10m), botones contextuales de DetailScreen (Explore, Bookmark, Edit, RouteOptionsMenu) y ExperienceMapScreen (Share, Save ZIP), navegación con popUpTo + launchSingleTop + restoreState, showHelpDialog.
- **Eliminado**: Row intrusa con GPS capsule dentro de NavigationBar, .width(60.dp) en Help, .navigationBarsPadding() (innecesario con Scaffold), IconButton sueltos como hijos directos de NavigationBar, import navigationBarsPadding.
- **Archivos modificados**: app/src/main/java/com/example/MainActivity.kt (bottomBar líneas 774-969 reemplazadas).
- **Respaldos**: RESPALDO_ANTERIOR/20260711_MainActivity.kt.
- **Compilación**: BUILD SUCCESSFUL (0 errores).




