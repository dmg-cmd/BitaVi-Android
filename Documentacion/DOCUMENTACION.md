## Modularización Táctica y Desacoplamiento de MainActivity.kt (2026-10-14)

### ¿Qué cambió para el usuario y la arquitectura?
- **Estabilidad y Mantenibilidad de la Interfaz:** Se extrajo la complejidad monolítica de `MainActivity.kt` (reduciendo su tamaño de 1.115 a 181 líneas, una reducción del 83.7%).
- **Gestión Aislada de Permisos y Criptografía:** Los permisos del sistema operativo y los contratos SAF residen ahora en `ActivityContractsCoordinator.kt`, mientras que la biometría y la bóveda de claves están encapsuladas en `BiometricAuthHandler.kt`.
- **Andamiaje Compose Independiente:** La estructura declarativa (Drawer, diálogos, BottomBar y NavHost) está contenida en `MainScaffoldContainer.kt`, facilitando futuras pruebas de UI y manteniendo intactos todos los flujos tácticos y contratos operativos.

### ¿Dónde está el cambio?
- `com.aistudio.localizador.ui.container.ActivityContractsCoordinator` (nuevo)
- `com.aistudio.localizador.ui.container.BiometricAuthHandler` (nuevo)
- `com.aistudio.localizador.ui.container.MainScaffoldContainer` (nuevo)
- `com.aistudio.localizador.MainActivity.kt` (modularizado y reducido a orquestador)

## Consolidar Paradas y Tiempo de Parada en Modo Automático (2026-09-27)

### ¿Qué cambió para el usuario?
- **No más rutas de 0 metros repetitivas:** En modo automático (`auto_mode`), cuando permaneces en un mismo lugar (casa, oficina, estacionamiento o cafetería), el sistema ya no llenará tu historial con decenas de "Auto-Ruta 0.00 km".
- **Identificación como "Parada":** Si no hubo desplazamiento significativo (< 25 metros), el registro se cataloga automáticamente como una **Parada** con icono de punto fijo (`Place`).
- **Medición del "Tiempo de parada":** Si permaneces en el mismo lugar y el sistema realiza comprobaciones periódicas, en lugar de crear rutas nuevas acumula el tiempo en la parada existente, mostrando claramente **"Tiempo de parada: HH:mm:ss"** con la duración total de permanencia.
- **Transición fluida a trayecto:** En cuanto te desplazas más de 25 metros, el sistema inicia automáticamente una nueva ruta normal de movimiento con su distancia en kilómetros y traza completa.

### ¿Dónde está el cambio?
- `RouteRepository.kt` & `RouteDao.kt`: detección de parada contigua en el mismo radio (< 40m) y fusión de tiempo en `stopActiveRoute()`.
- `LocationService.kt`: registro del punto de coordenadas inicial para fijar la ubicación de la parada.
- `HistoryScreen.kt`: visualización táctica en el historial con icono de punto de parada y métrica destacada de tiempo de permanencia.

## Infraestructura: Sistema de Agentes Especializados de Desarrollo (2026-09-26)

### ¿Qué cambió para el usuario / desarrollador?
- Se configuró e integró un ecosistema de **6 subagentes especializados** para acelerar, ordenar y proteger la calidad técnica del proyecto Localizador:
  1. `doc-custodian`: automatiza y vigila el cumplimiento de respaldos previos, bitácoras e informes walkthrough.
  2. `telemetry-gps-specialist`: cuida la estabilidad de `LocationService.kt`, filtrado GPS y prevención de fugas de memoria en osmdroid.
  3. `compose-ui-tactical`: vigila el estándar táctico de UI (nomenclatura `PANTALLA.BARRA.BOTON` y colores semánticos).
  4. `gradle-compiler-qa`: valida compilación real y cero warnings.
  5. `groq-route-analyst`: asegura la calidad de resúmenes de telemetría con Groq (Llama 3.3).
  6. `git-release-guardian`: audita la exclusión de secretos y credenciales confidenciales, prepara commits limpios y gestiona tags y Releases oficiales en GitHub.

### ¿Dónde está el cambio?
- `.agents/skills/` y `.opencode/skills/`: carpetas con directivas operativas de cada agente.
- `.agents/AGENTS.md`: catálogo y reglas operativas de los subagentes.

## CR-29: Analitica de tramos sin tirones (2026-08-07)

### Que cambio para el usuario?
- La pantalla de Analitica (Pantalla 7) ahora responde fluido al arrastrar el selector de tramo (7.3.1): las metricas se calculan en segundo plano (fuera del hilo de dibujo) y los graficos de perfil de elevacion/ritmo dejan de reconstruirse en cada fotograma.
- La tarjeta 7.4 muestra ahora el desnivel real del tramo en dos valores: Subida +X m y Bajada -Y m (antes era un solo "Desnivel" neto que podia ocultar subidas y bajadas en rutas circulares), ademas de la distancia del tramo.
- El desnivel usa un filtro de histeresis de 2.5 m: pequenas fluctuaciones del GPS/barometro (<2.5 m) se ignoran, dando totales altimetricos honestos.

### Donde esta el cambio?
- **CalculateRouteMetricsUseCase.kt** (nuevo): toda la matematica de tramo (distancia Haversine, desniveles acumulados, velocidad, ritmo) en un modulo puro e inyectable.
- **RouteMetricsResult.kt** (nuevo): modelo inmutable de resultados en `com.aistudio.localizador.model`.
- **RouteViewModel.kt**: pipeline reactivo que calcula el tramo en Dispatchers.Default y cancela recalculados obsoletos.
- **RouteAnalyticsScreen.kt**: memorizacion de series de graficos con remember(points) y nueva tarjeta 7.4.

### Detalles Tecnicos
- Altimetria: se suman solo variaciones |dH| >= 2.5 m; ganancia = suma de max(0,dH), perdida = suma de max(0,-dH).
- Velocidad promedio = distancia/tiempo del tramo; ritmo = min/km formateado.
- 7 tests nuevos; suite: 100 tests, 0 fallos.
## CR-30: Limpieza de Fotos Huerfanas (2026-08-07)

### Que cambio para el usuario?
- **Nada visible todavia**: es un modulo interno (use case) que prepara la funcion de mantenimiento "purgar fotos sin usar". Cuando borras una ruta o un favorito que tenia fotos, los archivos de imagen podian quedar acumulandose en el almacenamiento interno. Este modulo detecta y elimina esos archivos huerfanos (JPG/PNG sin referencia en la base de datos).

### Donde esta el cambio?
- **PurgeOrphanPhotosUseCase.kt** (nuevo): escanea `filesDir/images/`, compara contra las referencias de la base de datos y elimina los archivos huerfanos.
- **RouteDao.kt / RouteRepository.kt**: consulta de todas las rutas de foto referenciadas.
- **AppModule.kt**: inyeccion del nuevo use case.

### Detalles Tecnicos
- API `execute(): Result<PurgeResult>` con contadores `scanned`/`purged`; seguro ante caida de BD (no borra nada si la consulta falla).
- 7 tests deterministas (directorio temporal real + repository mockeado). Suite: 93 tests, 0 fallos.
## CR-28: Suite de Pruebas 100% Verde y Determinista (2026-08-06)

### ¿Qué cambió para el usuario?
- **Nada visible**: este hito es de infraestructura interna (testing). La app funciona igual; no se requieren cambios de uso ni se despliega APK nuevo.

### ¿Dónde está el cambio?
- Configuración de memoria de Gradle (gradle.properties y app/build.gradle.kts): el worker de tests se recicla cada 50 tests (forkEvery=50) y el daemon subió a 4 GB.
- Corrección de 3 suites de tests que estaban desincronizadas con el código real (ActivityModeManager, RouteExport, ShortStopMark) y un cambio de visibilidad mínimo en ShortStopMarkManager (el scope de corrutinas pasa de `private` a `internal var`) para poder controlarlo en tests.

### Detalles Técnicos
- La suite completa pasó de 81 tests con ~9 fallos (OOM y tests desactualizados) a **86 tests, 0 fallos**, verificado 3 veces consecutivas (determinismo).
- La causa principal de los OOM era un bucle infinito dentro de `FileProvider.getUriForFile` al usar un Context mockeado, que agotaba el heap del worker de tests y hacía fallar en cascada a otras clases.
## CR-27: Modo Vuelo Automático — ya no necesitas activar nada (2026-08-06)

### ¿Qué cambió para el usuario?
- **Antes**: el interruptor "Modo Avión" del menú lateral (8.3) era decorativo. En un vuelo comercial (>162 km/h) la app descartaba el 100% de los puntos GPS y no grababa la ruta, aunque activaras el interruptor.
- **Ahora**: la app detecta sola que estás volando. Cuando superas 150 km/h y 100 m de altitud, entra en modo vuelo automático y graba sin cortes hasta 1200 km/h. Al aterrizar vuelve a modo normal sin que hagas nada.
- El interruptor "Modo Avión" se eliminó del menú lateral (ya no es necesario). En la barra superior del mapa verás el indicador "✈️ Vuelo" mientras estés en el aire.
- En vuelo, la app tolera la menor precisión del GPS dentro de la cabina (hasta 80 m) para no perder el trazado.

### ¿Dónde está el cambio?
- **CinematicFilterManager.kt**: perfiles cinemáticos TERRESTRIAL/FLIGHT con histéresis automática.
- **LocationService.kt**: umbrales de precisión y persistencia dinámicos según el perfil; estado FLIGHT en vivo.
- **RouteViewModel.kt / MainActivity.kt / MapScreen.kt / PreferencesManager.kt / HelpDialog.kt**: limpieza del toggle fantasma y badge "✈️ Vuelo".

### Detalles Técnicos
- Detección: FLIGHT si velocidad calculada >41.67 m/s y altitud >100 m MSL; retorno a TERRESTRIAL si <33.33 m/s y <100 m (histéresis anti-flap).
- En FLIGHT: velocidad máx 333.33 m/s (~1200 km/h), precisión aceptada 80 m, auto-stop y guarda de 3 min desactivados.
## CR-26: Compartir Experiencia como captura PNG (2026-07-31)

### ¿Qué cambió para el usuario?
- El botón Compartir (cian, 5.2.1) de la Pantalla 5 (Mapa de Experiencia) ya no envía solo las fotos de los waypoints: ahora abre el selector de compartir con una imagen PNG de alta calidad que muestra el mapa con tu ruta, marcadores y las fotos del recorrido, como una "tarjeta postal".
- La barra inferior de botones no aparece en la imagen compartida.
- Al compartir se incluye el nombre de la experiencia en el texto (ej. "📍 Experiencia Bitaru: Paseo del Río").

### ¿Dónde está el cambio?
- **ExperienceMapScreen.kt**: captura de pantalla con Compose GraphicsLayer y compartición vía FileProvider.

### Detalles Técnicos
- rememberGraphicsLayer + drawWithContent en un contenedor capturable; PNG a cacheDir y ACTION_SEND image/png con chooser.
## CR-25: El botón "Actualizar País/Ciudad" ya funciona (2026-07-31)

### ¿Qué cambió para el usuario?
- **Corrección**: Antes, al pulsar "Actualizar País/Ciudad" dentro de Editar Favorito, el país y la ciudad se calculaban pero se volvían a "Desconocido" al pulsar "Guardar Cambios".
- **Ahora**: Al pulsar "Actualizar País/Ciudad" verás un indicador "Geocodificando..." y luego la ubicación resuelta (ej. "Perú • Machupicchu") justo debajo del botón. Al guardar los cambios, esa ubicación se conserva.
- **Útil para**: corregir favoritos antiguos que quedaron con "Desconocido" antes de las correcciones CR-23/CR-24.

### ¿Dónde está el cambio?
- **RouteViewModel.kt**: refreshLocationDetails notifica a la pantalla la entidad actualizada.
- **SavedLocationsScreen.kt**: feedback en tiempo real + "Guardar Cambios" conserva el país/ciudad resuelto.

### Detalles Técnicos
- El diálogo usa la entidad fresca de la base de datos como base al guardar, evitando que datos antiguos sobrescriban la geocodificación.
## CR-24: País/Ciudad ahora funciona en cualquier país del mundo (2026-07-31)

### ¿Qué cambió para el usuario?
- **Corrección importante**: El país y la ciudad de los favoritos solo se determinaban en Argentina, Chile, Uruguay y Paraguay (Cono Sur). Fuera de esa zona (ej. Perú, Ecuador, Colombia, México, Europa...) los favoritos quedaban como "Desconocido"/"Desconocida" aunque existiera el respaldo de OpenStreetMap del CR-23.
- **Ahora**: Funciona en cualquier punto del planeta. Guardar un favorito nuevo (o usar el botón "Actualizar País/Ciudad" en Editar Favorito) determina el país y la ciudad correctos aunque estés fuera del Cono Sur.
- El único caso que no se geocodifica es el de coordenadas nulas (0.0, 0.0), que indican que el GPS no tiene señal.

### ¿Dónde está el cambio?
- **AppConstants.kt**: el límite geográfico (geofencing) se amplió a todo el planeta (WGS84).
- **GeocoderUseCase.kt**: validación de coordenadas mundial; texto de error "GPS: Sin señal".

### Detalles Técnicos
- Cobertura mundial lat -90..90, lon -180..180, excluyendo (0.0, 0.0).
- Sin cambios en la interfaz; sigue usando Geocoder nativo → Nominatim (OSM) como respaldo.
## CR-23: País, Ciudad y Lugar cercano de vuelta en los Favoritos (2026-07-31)

### ¿Qué cambió para el usuario?
- **Corrección**: Al guardar un favorito, ahora vuelve a determinarse el país, la ciudad y el lugar/dirección cercano. Antes quedaban como "Desconocido"/"Desconocida" cuando el servicio de mapas del sistema no respondía.
- **Más fiable**: Si el geocodificador nativo del teléfono falla (falta de red en el servicio, límites del fabricante, etc.), la app consulta automáticamente OpenStreetMap (Nominatim) como respaldo para obtener país, ciudad y dirección.
- **Favoritos antiguos**: Los que ya quedaron con "Desconocido" se corrigen con el botón "Actualizar País/Ciudad" dentro de Editar Favorito (Pantalla 3), sin necesidad de borrarlos.

### ¿Dónde está el cambio?
- **NominatimApi.kt** (nuevo): API REST de geocodificación inversa de OpenStreetMap.
- **RetrofitClient.kt**: cliente HTTP dedicado con User-Agent "Agenviaje/1.0" (política OSM), sin tocar los clientes de IA.
- **GeocoderUseCase.kt**: flujo híbrido (Geocoder nativo → Nominatim) que calcula país/ciudad/dirección.

### Detalles Técnicos
- El cambio es interno y transparente para la interfaz; no requiere acción del usuario.
- Respeta la política de uso de Nominatim (máx. ~1 req/s): sin barridos masivos automáticos.
## CR-22: Fecha de Captura de la Foto visible en Favoritos (2026-07-31)

### ¿Qué cambió para el usuario?
- **Fecha de captura en la tarjeta**: Cada favorito ahora muestra una línea con la fecha (dd/MM/yyyy HH:mm) bajo el nombre. Si el favorito se guardó adjuntando una foto con GPS/fecha EXIF, esa línea muestra la fecha en que se tomó la foto; si no, muestra la fecha en que se guardó el favorito.
- **Comentario con la fecha**: Al guardar un favorito con foto, el comentario incluye automáticamente "📸 Capturada el dd/MM/yyyy HH:mm" (concatenado si ya escribiste una nota).
- **Cobertura en ambos flujos**: Funciona tanto al guardar desde la pantalla Favoritos como al colocar el marcador directamente en el Mapa.

### ¿Dónde está el cambio?
- **RouteViewModel.kt**: Corrección del formateo de la fecha en el comentario y preservación del timestamp EXIF.
- **MapScreen.kt**: Captura y transmisión del timestamp EXIF de la foto al guardar desde el mapa.
- **SavedLocationsScreen.kt**: Nueva línea de fecha en la tarjeta de favoritos (LocationCard).

### Detalles Técnicos
- La fecha proviene de PhotoMetadata.timestamp (EXIF TAG_DATETIME_ORIGINAL) y se guarda en el campo timestamp de SavedLocation.
- La protección contra duplicados evita que "📸 Capturada el" se repita al re-editar un favorito.
## CR-21: Favoritos con Foto usan la Posición de la Foto (2026-07-31)

### ¿Qué cambió para el usuario?
- **Corrección de un bug importante**: Cuando agregabas una foto a un favorito (desde Favoritos o desde el mapa) y la foto tenía coordenadas GPS grabadas, el favorito se guardaba en la posición actual del teléfono en lugar de en la posición donde fue tomada la foto.
- **Ahora**: Al adjuntar una foto con GPS, la app detecta automáticamente la posición de la foto y la sugiere para guardar el favorito allí (checkbox "Usar ubicación de la foto").
- **En Android 10+**: Si la foto proviene de la galería y no se ha concedido aún el permiso de ubicación de medios (ACCESS_MEDIA_LOCATION), la app lo solicita de forma amigable y reintenta automáticamente al concederlo.
- **Nota**: Si la foto no contiene GPS (por ejemplo, fotos de algunas apps de cámara con ubicación desactivada), se mantiene el comportamiento anterior de usar la posición actual.

### ¿Dónde está el cambio?
- **ExtractPhotoLocationUseCase.kt**: Extracción de EXIF ahora compatible con Scoped Storage (API 29+) usando MediaStore.setRequireOriginal.
- **SavedLocationsScreen.kt**: Flujo de galería con permiso ACCESS_MEDIA_LOCATION y reintento automático.
- **MapScreen.kt**: La foto tomada con la cámara activa automáticamente el uso de la ubicación de la foto.

### Detalles Técnicos
- Scoped Storage (Android 10+) redacta los metadatos GPS al leer fotos de la galería a menos que se use MediaStore.setRequireOriginal(uri) y se posea ACCESS_MEDIA_LOCATION.
- El flujo de cámara en MapScreen ahora sincroniza el estado reactivo usePhotoGps = photoGpsLocation != null.
## CR-20: Extraccion del Analisis por IA a Capa de Dominio (2026-07-22)

### Que cambio para el usuario?
- **Nada visible.** El boton 4.6.1 [ Analizar con IA ] en la Pantalla 4 (DetailScreen) funciona exactamente igual.
- El cambio es puramente arquitectonico: la logica de analisis se movio a un UseCase dedicado, mejorando la testeabilidad y el desacoplamiento.

### Que cambio internamente?
- **AnalyzeRouteAiUseCase**: Nuevo caso de uso en domain/usecase/ que reemplaza al antiguo GroqAiUseCase en ui/.
- **RouteViewModel**: Ahora expone aiAnalysisState como StateFlow (reactivo) en lugar de mutableStateOf.
- **DetailScreen**: Actualizada para consumir el StateFlow con collectAsStateWithLifecycle().

### Detalles Tecnicos
- El UseCase construye el prompt de telemetria, llama a la API de Groq (Llama 3.3) y retorna Result<String>.
- ApiKeyExpiredException se maneja internamente y se propaga como Result.failure.
## CR-19: Eliminación de Barra Duplicada en Pantalla de Experiencia (2026-07-22)

### ¿Qué cambió para el usuario?
- **Una sola barra inferior**: Antes, la Pantalla 5 (ExperienceMapScreen) mostraba dos filas de botones: la barra inferior 5.2 y una fila contextual duplicada con los mismos controles (Compartir, Exportar, GPS). Ahora solo existe la barra 5.2 con todos los controles integrados.
- **Sin cambios en funcionalidad**: Compartir, Exportar y el indicador GPS siguen funcionando exactamente igual, ahora desde la barra 5.2 unificada.

### ¿Dónde está el cambio?
- **MainActivity.kt**: Se eliminó la fila contextual que se superponía en ExperienceMapScreen.
- **ExperienceMapScreen.kt**: Solo se actualizaron los colores de los botones para usar las constantes del sistema (sin cambio visual).

### Detalles Técnicos
- La causa raíz era la sub-capa BETA del Scaffold en MainActivity.kt, que inyectaba botones redundantes al detectar currentRoute == NavRoutes.EXPERIENCE_MAP.
- ShareExperienceUseCase se mantiene intacto (ya maneja correctamente fotos adjuntas vía FileProvider).

## CR-10: Eliminación de Botón Directo de Borrado en HistoryScreen (Julio 2026)

### ¿Qué cambió para el usuario?
- **Tarjetas de historial más limpias**: Se eliminó el botón de eliminar (🗑️) de cada tarjeta individual de ruta en la pantalla Historial.
- **Sub-barra inferior mejorada**: La barra inferior ahora tiene:
  - **Compartir** (cian) → exporta todas las rutas como GPX
  - **Importar** (amber) → abre selector de archivos GPX
  - **Eliminar todo** (rojo) → borra todas las rutas (con confirmación)
  - **Indicador GPS** (verde) → precisión actual de señal
- **Prevención de errores**: Al no tener botones destructivos en las tarjetas, se elimina el riesgo de borrar accidentalmente una ruta al deslizar el dedo o caminar.
- **Modo multi-selección**: Se conserva intacto (checkboxes + borrado por lote).

### ¿Dónde está el cambio?
- Los botones de la sub-barra inferior están en DynamicHistoryBottomBar.kt.
- Las tarjetas de ruta están en HistoryScreen.kt (función RouteCard).
- El lanzador de importación GPX se agregó en MainActivity.kt.

## ME-01: Inyección de Dependencias con Hilt (Julio 2026)

El proyecto ahora usa **Hilt 2.59** para inyección de dependencias, compatible con AGP 9.2.1 mediante **KSP** (no kapt).

### ¿Qué cambió para el usuario?
- **Nada visible.** La funcionalidad es idéntica.
- El código internamente es más limpio y testeable.

### Componentes inyectados
| Componente | Tipo de Inyección |
|---|---|
| RouteViewModel | @HiltViewModel con @Inject constructor |
| MainActivity | @AndroidEntryPoint, ViewModel via y viewModels() |
| LocationService | @AndroidEntryPoint, 5 campos @Inject (incl. sessionManager + filterPipeline) |
| Database/Repository | Singleton via DatabaseModule |
| Use Cases y Managers | Singleton via AppModule |

### Tests
- HiltTestRunner configurado para instrumentation tests
- Tests unitarios existentes: 16/16 pasando

## ME-02: ExtractPhotoLocationUseCase --- Extraccion Segura de EXIF (Julio 2026)

### Que cambio para el usuario?
- **Nada visible.** La funcionalidad es identica.
- **Bug corregido (EXIF-02):** Al seleccionar una foto de la galeria para crear un favorito, la app ahora extrae las coordenadas GPS del archivo local clonado en lugar del URI original de la galeria. Esto evita el error de stream IPC consumido que provocaba la perdida silenciosa de los metadatos EXIF.
- **Diagnostico mejorado:** Ahora hay logs detallados con el tag "Bitaru_EXIF" en Logcat para diagnosticar cualquier fallo en la extraccion de coordenadas GPS (permiso faltante, stream corrupto, imagen sin metadatos).

### Arquitectura
| Capa | Componente | Responsabilidad |
|---|---|---|
| Dominio | domain/usecase/ExtractPhotoLocationUseCase.kt | Orquesta la extraccion en Dispatchers.IO, devuelve Pair<Double,Double>? |
| Infraestructura | ui/PhotoManager.kt | Lee EXIF via MediaStore.setRequireOriginal() + ExifInterface (sin cambios) |
| Presentacion | ui/RouteViewModel.kt | Expone suspend fun extractLocationFromPhoto(uri): Pair<Double,Double>? |
| UI | SavedLocationsScreen, MapScreen | Llaman a extractLocationFromPhoto() desde scope.launch (corrutina) |

### Causa Raiz del Bug
getPhotoLocation() se ejecutaba en el hilo principal de Compose. En Android 10+, ContentResolver.openInputStream() desde un content:// URI (galeria) puede lanzar SecurityException por Scoped Storage. El catch (_: Exception) lo tragaba -> devolvia null. Solucion: envolver la llamada en withContext(Dispatchers.IO).

### Tests
- ExtractPhotoLocationUseCaseTest.kt: 2 tests (coordenadas exitosas, null seguro) con mockk
- Suite total: 16/16 pasando

# Guía de Uso y Documentación Técnica: Localizador Pro

## Descripción General
Localizador Pro es una herramienta avanzada de seguimiento GPS para Android, diseñada para usuarios que requieren precisión técnica, automatización inteligente y un diario multimedia de sus recorridos. Funciona de forma autónoma, sin depender de servicios en la nube para el almacenamiento de rutas.

### 5. Postal Táctica (Focus Favorite)
### 5. Postal Táctica (Focus Favorite)
- **Pantalla Inmersiva**: Al pulsar CenterFocusStrong, se abre vista full-screen Box raíz con drawWithContent + GraphicsLayer. Mapa 100% con pin central y card translúcida flotante abajo.
- **Card con protección bottom=40dp**: La card inferior usa padding fijo de 40dp desde el borde inferior para resistir el modo inmersivo del Pixel (que oculta la status bar al tocar el mapa). NO usa navigationBarsPadding() porque los insets dinámicos se ponen a 0 en modo inmersivo.
- **Compartir como Postal**: Botón verde captura el contenedor mapa+card via GraphicsLayer → toImageBitmap() → PNG → FileProvider → Intent ACTION_SEND image/png. Los botones de UI quedan fuera del capturable.
## Guía de Uso (Manual del Usuario)

### 1. Gestión de Rutas
- **Grabación Manual:** Pulsa el botón "Grabar" en el mapa. Puedes ajustar la frecuencia del GPS antes de iniciar.
- **Grabación Automática (Modo Automático):** Actívalo en el menú lateral. La app vigila el movimiento en segundo plano y grabará sola al detectar un desplazamiento real (>25m confirmado). El sistema ignora ruidos estáticos del GPS y se detiene automáticamente tras 5 minutos de inactividad total (estándar de la industria).
- **Modo Avión:** Actívalo para registrar vuelos comerciales (hasta 1200 km/h). Si está apagado, filtra ruidos por encima de 162 km/h.
- **Menú de Acciones `⋮` (Detalle de Ruta):** Las acciones de compartir, exportar y eliminar están agrupadas en un menú de tres puntos. Exportar ofrece submenú con GPX, CSV (Excel) y ZIP. Eliminar ruta muestra un diálogo de confirmación antes de borrar permanentemente los datos.
- **Exportación ZIP Persistente:** Los ZIP exportados se guardan en la carpeta **Documentos** del dispositivo con nombres únicos (ej: `ROUTE_5_Paseo_del_Rio_20260702_143052.zip`) y contienen un árbol estructurado: `track.gpx` (coordenadas estándar GPX 1.1), `telemetry.csv` (matriz con timestamp, lat, lon, altitud, speed_ms, speed_kmh, activity_mode) y `media/` (fotos de la ruta). Al guardar desde el mapa de experiencia, el nombre también sigue el formato ROUTE_{id}_{nombre}_{timestamp}.zip y se conservan incluso tras reiniciar la app.
- **Ayuda en Pantalla:** El menú `⋮` incluye un botón de ayuda que muestra un diálogo con la descripción de cada acción disponible en la pantalla de detalle.

### 2. Puntos de Interés y Multimedia
- **Marcar Punto:** Durante una ruta, usa el botón del marcador para guardar un lugar. Puedes añadir un comentario, el nombre del autor y capturar una foto (Cámara o Galería).
- **Favoritos:** Mantén pulsado cualquier punto del mapa para guardarlo. Se organizan en un árbol jerárquico expandible por País → Ciudad con animaciones suaves. Pulsa el icono ExpandMore/ChevronRight para colapsar o expandir secciones. Los datos geográficos se obtienen automáticamente del geocodificador al guardar. Ahora puedes elegir el **color de la estrella** y editar cualquier dato de un favorito existente (nombre, contacto, color) tocando el icono de lápiz.
- **Importar Contactos (Agenda):** (Novedad Fase 2) Puedes importar datos directamente de la agenda de tu teléfono. El botón de "Importar contacto" lee automáticamente el nombre, dirección, teléfono y correo electrónico para crear un favorito al instante.
- **Modo Cerca de mí:** (Novedad Fase 3) Si activas este ajuste en el menú lateral, la app te notificará cuando estés a menos de 100 metros de una de tus estrellas guardadas.
- **Compartir Trayecto con Mapa:** Envía un resumen visual a WhatsApp que incluye una captura de tu mapa de ruta y todas las fotos de los puntos marcados. Accesible desde el menú `⋮` en el detalle de la ruta.
- **Inteligencia Anti-Error:** La app filtra automáticamente saltos de GPS mediante un sistema multicapa de 3 niveles: (1) **Fase 1 — Precisión**: descarta puntos con accuracy >50m al instante; (2) **Fase 2 — Inercia**: ventana móvil de 5 posiciones que calcula velocidad cinemática entre el último punto válido y el nuevo; si supera 15 m/s (~54 km/h, límite físico de carrera), el punto se descarta como salto de antena con registro en Log.w; (3) **Fase 3 — Precisión fina**: accuracy >20m para DB y velocidad >45 m/s como respaldo. Solo arranca el modo automático si detectas un paso firme y continuo (>2.5 km/h) durante al menos 10 segundos, eliminando falsos positivos por ruido GPS.
- **GPS en Fotos:** Al elegir una foto de la galería, la app puede detectar su ubicación original (GPS EXIF) y ofrecerte guardar el favorito allí.
- **Contactos:** Marca la casilla "Es un Contacto" para vincular una dirección con un teléfono y email.

### 3. Seguridad y Respaldo
- **Protección Biométrica:** Actívala en el menú lateral para proteger el acceso a configuraciones críticas y el borrado de datos usando tu huella o rostro. Sustituye al antiguo sistema de PIN para una experiencia más fluida y moderna.
- **Copia de Seguridad Pro:** Sistema de respaldo en ZIP con fotos integradas. Totalmente compatible con versiones anteriores; el motor de restauración detecta automáticamente el formato del archivo (de 4 a 13+ columnas de datos) y rellena la información faltante (`country`, `city`, etc.) de forma inteligente.
- **Control Inteligente de Centrado del Mapa:** Al navegar a un favorito desde la lista, el mapa bloqueará el seguimiento automático del GPS por 10 segundos para permitirte ver el lugar sin saltos de cámara. Se ha implementado un bloqueo de hardware sincronizado para evitar que el GPS interrumpa la animación de aproximación. El botón "Mi ubicación" restablece inmediatamente el seguimiento en tiempo real.

### 4. Análisis de Datos
- **IA Narrativa (Groq)**: En el detalle de la ruta, pulsa "Analizar con IA". El sistema enviará tus estadísticas técnicas (Km, Tiempo, Velocidades) y tus marcadores a Llama 3 para generar un resumen profesional y motivador de tu viaje. Requiere una Groq API Key configurada en los ajustes.
- **Exportación (Menú `⋮` > Exportar):** 
    - **GPX:** Para usar en Google Earth o Strava.
    - **CSV (Excel):** Para analizar velocidades y altitudes en tu PC.
    - **ZIP:** Respaldo completo con datos y fotos.
- **Eliminar Ruta (Menú `⋮`):** Borra permanentemente la ruta, sus puntos GPS, waypoints y fotos asociadas tras confirmación del usuario.

## Especificaciones Técnicas
- **Barra de Navegación Inferior MD3**: Implementada con NavigationBar nativa de Material 3. Colores diferenciados por pestaña: Mapa (Azul Satelital #3498DB), Favoritos (Verde Esmeralda #2ECC71), Historial (Azul Satelital), Ayuda (Rojo Transmisor #E74C3C). Incluye GPS Capsule de precisión en el extremo derecho.
- **Arquitectura:** MVVM (Model-View-ViewModel) con Jetpack Compose.
- **Base de Datos:** Room (Versión 10), con SQLCipher y 5 tablas (routes, location_points, waypoints, saved_locations, route_favorite_cross_ref).
- **Motor de IA:** Groq API (Llama 3.3 70B) vía Retrofit.
- **Mapas:** OpenStreetMap (osmdroid) con caché persistente de 500MB.
- **Marcador de Paradas Cortas:** Durante el tracking activo, si permaneces quieto (>2.5 km/h) por más de 2 minutos, la app registra automáticamente las coordenadas y las guarda como un Favorito con la etiqueta ⏱️ Parada automática al reanudar la marcha. Incluye +3 min de tolerancia para contar tiempos de semáforos o tráfico lento.
- **Filtro GPS:** Sistema multicapa con precisión máxima de 20m, velocidad máxima de 45 m/s (~162 km/h) y delta temporal mínimo de 3s.
- **Seguridad:** Gestión de permisos dinámicos y almacenamiento privado para archivos multimedia cifrados mediante hardware Keystore (API Key).

---
*Documentación actualizada al 29 de Junio de 2026.*
## Barra Inferior Unificada (03/07/2026)
- DetailScreen y ExperienceMapScreen ya no tienen Scaffold/TopAppBar propios.
- La barra inferior en MainActivity.kt maneja toda la navegación con 3 zonas:
  - **Izquierda**: ☰ (ru¬tas base) o ← (rutas auxiliares)
  - **Centro**: Navegación entre pestañas (base) o botones contextuales (auxiliares)
  - **Derecha**: Cápsula GPS + ? global
- Las acciones de ZIP y compartir pantalla para ExperienceMapScreen ahora se manejan desde la barra inferior.

## Ergonomía Bottom Bar (03/07/2026)
- Altura mínima táctil de 72dp para evitar activación accidental de gestos del sistema.
- 
avigationBarsPadding(): colchón dinámico que empuja la barra por encima de la línea de gestos de Android 16.
- Padding horizontal de 16dp aparta los botones ☰ y ? de las esquinas curvas del Pixel 9 Pro XL.

## Panel de Fotos Translúcido (03/07/2026)
- Fondo 50% negro ( x80000000) que deja ver el mapa satelical por detrás.
- Transición animada slideInHorizontally al aparecer la primera foto.
- Transición fadeOut + slideOutHorizontally al retirar todas las fotos.

## RouteAnalyticsScreen — Telemetría Temporal (03/07/2026)
- **Dos charts sincronizados**: Elevación (m) y Velocidad (km/h) vs Tiempo transcurrido (mm:ss). Eje X con etiquetas cada ~1 minuto.
- **RangeSlider**: Selecciona un tramo moviendo los dos pulgares. Los charts muestran líneas punteadas blancas delimitando el rango.
- **Panel de análisis**: Al seleccionar un tramo muestra Vel. Promedio, Desnivel acumulado, Ritmo (min/km) y Modo predominante (Detenido/Caminata/Ciclismo/Automóvil).
- **Acceso**: Botón "Ver Gráficos de Rendimiento" en el detalle de la ruta.
- **Eliminación de mapa en DetailScreen**: Las estadísticas ahora son grid 2×2 (Distancia, Tiempo en Movimiento, Vel. Promedio, Ritmo) sin mapa para carga más rápida.

## Pixel 9 Pro XL — Modo Inmersivo Nativo (04/07/2026)
- La barra de estado (hora/batería) se oculta automáticamente al tocar el mapa y reaparece al soltar. Es comportamiento estándar del SO, no un bug.
- Pelear contra esto con FLAG_LAYOUT_NO_LIMITS o android:statusBarColor en themes.xml causa parpadeos y efectos secundarios en toda la app.
- La solución estable es: themes.xml limpio, enableEdgeToEdge() en MainActivity, y bottom=40dp fijo en la Card en lugar de depender de insets dinámicos.

## Geofencing Poligonal — Bounding Box Argentina (06/07/2026)
- **Problema**: Coordenadas (0.0, 0.0) del Golfo de Guinea y rebotes de red transfronterizos corrompen la telemetría y las métricas de IA.
- **Solución**: Validación geográfica en GeocoderUseCase con bounding box continental para Argentina:
  - Latitud: -55.0 (Tierra del Fuego) a -20.0 (límite norte)
  - Longitud: -75.0 (Cordillera) a -53.0 (límite este)
- **Comportamiento**: Si una coordenada cae fuera de estos límites, etchLocationDetails() retorna "GPS: Fuera de cobertura" y etchAddress() retorna null, impidiendo que datos corruptos lleguen a la base de datos.
- **Arquitectura**: Guarda defensiva en la capa de GeocoderUseCase, sin tocar LocationService, ViewModel ni UI.

## ActivityRecognitionManager — Arranque Automático por Transiciones (06/07/2026)
- **Problema**: El auto-arranque por velocidad GPS no reaccionaba porque el servicio pasivo no recibe updates frecuentes en Android 12+.
- **Solución**: Sistema de reconocimiento de actividad física basado en Google Play Services ActivityTransition API.
- **Componentes**:
  - ActivityRecognitionManager: Suscribe transiciones de actividad (STILL, WALKING, RUNNING, BICYCLE, VEHICLE) con PendingIntent FLAG_MUTABLE.
  - ActivityTransitionReceiver: BroadcastReceiver que captura eventos y puentea a LocationService via startForegroundService().
  - handleAutoStartFromTransition(): Verifica auto-mode activo y tracking no iniciado, luego llama a startAutoNewRoute().
- **Permiso requerido**: ACTIVITY_RECOGNITION (runtime en API 29+). Solicitado en MainActivity con el flujo escalonado de permisos.
- **Arquitectura**: El manager vive en LocationService, no en UI, para persistencia en background.
## Iconos Dinámicos por Categoría (07/07/2026)
- **Categorías de Favoritos**: Ahora los favoritos se pueden etiquetar con 7 categorías: Favorito (⭐), Gastronomía (🍽), Transporte (🚗), Aeropuerto (✈), Descanso (🌳), Trabajo (🏢) y Parada Automática (📍).
- **Icono en el Mapa**: Los marcadores en el mapa OpenStreetMap se dibujan como círculos programáticos (Canvas/Paint) con el color personalizado del favorito y borde blanco, reemplazando el antiguo resource drawable. Los iconos se diferencian por color y categoría.
- **Icono en Listas**: En las pantallas de lista (SavedLocationsScreen) y detalle de ruta (DetailScreen), cada favorito muestra el icono Vector Material Design correspondiente a su categoría, tintado con el color personalizado.
- **Paradas Automáticas**: Los marcadores generados por el sistema (Short Stop) usan la categoría AUTOMATIC con icono LocationOn y se distinguen visualmente de los favoritos manuales.
- **Migración Automática**: Al actualizar la app, Room ejecuta automáticamente MIGRATION_11_12 (ALTER TABLE ADD COLUMN iconCategory TEXT NOT NULL DEFAULT 'DEFAULT'). Todos los favoritos existentes reciben el valor DEFAULT sin pérdida de datos.
- **Selector en Diálogo de Guardado**: Al guardar un favorito (desde el mapa o desde la lista), un selector de categoría permite elegir entre las 7 opciones disponibles.

## Watchdog de Señal GNSS y Alertas Visuales (09/07/2026)
- **Monitoreo GNSS Activo**: `LocationService.kt` implementa un watchdog de salud satelital (`startGpsWatchdog`) que evalúa cada 5 segundos si el dispositivo ha dejado de recibir actualizaciones del proveedor satelital (`GPS_PROVIDER`) por más de 15 segundos estando en movimiento.
- **Conmutación a SIN_SENAL**: Si el watchdog detecta la pérdida de ticks satelitales, almacena el modo previo (`preAlertActivityMode`), conmutando el estado a `ActivityMode.SIN_SENAL` y actualizando el StateFlow público de Compose para alertar a la interfaz gráfica. Al recuperar la cobertura del GPS, el sistema reestablece automáticamente el modo cinemático previo.
- **Dashboard Flotante de Alerta**: `DashboardActividadFlotante.kt` reacciona de forma dinámica ante el estado `SIN_SENAL` aplicando un color rojo de alerta táctica (`#DC3545` con 75% alpha) con una transición animada suave de 400ms (`animateColorAsState`).
- **Ocultamiento de Datos Fuera de Cobertura**: Mientras el dispositivo permanece en estado de alerta (`SIN_SENAL`), la interfaz oculta automáticamente el valor de velocidad actual (que podría ser errático o nulo) y muestra la etiqueta "ESTATUS SATELLITE" con la advertencia "⚠️ SIN SEÑAL".


## 11/07/2026 — Corrección Estructural BottomBar (Opción C)
- **Efecto Compresión Eliminado**: La barra inferior se reestructuró en 3 sub-capas planas dentro de una Column para eliminar el colapso de botones en el extremo derecho.
- **Capa 1 — Tracking Activo**: Barra superior verde translúcida "Servicio activo • Grabando ruta en búnker" cuando el LocationService reporta grabación activa.
- **Capa 2 — Fila Flotante Contextual**: Aísla la cápsula GPS dinámica (precisión satelital con código de colores: verde <10m, rojo ≥10m) y los botones contextuales de las pantallas DetailScreen (Mapa de Experiencia, Favoritos, Editar nombre, Menú de opciones) y ExperienceMapScreen (Compartir pantalla, Guardar ZIP).
- **Capa 3 — NavigationBar MD3 Plana**: 4 NavigationBarItems puros sin contenedores intermedios:
  - Mapa (Azul #3498DB): Abre drawer si ya está en tracking, navega si no.
  - Favoritos (Verde #2ECC71): Navega a SavedLocations.
  - Historial (Azul #3498DB): Navega a History.
  - Ayuda (Rojo #E74C3C): Abre diálogo de ayuda global.
- **Beneficio Técnico**: Al eliminar la Row intrusa y los IconButton sueltos como hijos directos del NavigationBar, el motor de layout MD3 recupera la distribución equitativa del ancho, eliminando el amontonamiento en el Pixel 9 Pro XL.

## 12/07/2026 — Fases 7-10: Consolidación Production-Ready (Build 41s)
- **Purga de código muerto**: 12+ funciones eliminadas (authenticateWithCrypto, shareCompleteRoute, shareScreenshot, PhotoInfoWindow, isFavoriteLinked, saveFavoritePoint, insertFavorite, val activity, photoItems, rssi, routeId/onBack, val route, val decision, val res).
- **Migración KTX/Compose**: .debounce(100) → 100.milliseconds, FQNs de 7 archivos contraídos a imports, 4 Elvis muertos eliminados, Color.parseColor → toColorInt (completado).
- **AndroidView diffing optimizado**: MapScreen.kt ya no reconstruye polilíneas completas en cada update — solo añade puntos delta al último segmento.
- **Falso positivo protegido**: context.resources en AndroidView factory de MapScreen.kt L477 no migrable a Compose API.
- **Lint residual**: ~49 warnings reales + 5 falsos positivos Markdown. Fase 11 declinada por ROI negativo (riesgo de regresión en delegados Compose no justifica barrer imports cosméticos).
- **Tiempo de compilación**: 41s (vs >2min al inicio de la sesión, mejora del 65%).
- **Declaración**: Suite BiTaRu declarada Production-Ready bajo el amparo del protocolo de Código Seguro y Modularidad Estricta.

## 10. Mapa de Experiencia Inmersivo (ExperienceMapScreen)
- **Arquitectura**: Box(fillMaxSize) con 4 capas Z: [0] MapView fullscreen, [1] Panel fotos translucido lateral, [2] FABs navegacion, [3] Dock flotante inferior, [4] Dialogos.
- **Dock Flotante**: Barra inferior con 50% de transparencia (surfaceContainerHigh alpha=0.50), bordes grandes (shapes.large) y margen flotante (padding 24.dp). Contiene: boton retroceso, titulo + indicador de precision GPS, botones Compartir y Guardar.
- **GPS Telemetria**: Prevision categorizada en 4 niveles (Excelente/Buena/Regular/Baja) segun accuracy del LocationService. Consumido reactivamente via StateFlow en RouteViewModel + collectAsStateWithLifecycle.

## 13/07/2026 - Paleta semantica de colores para botones de accion
- **IconCompartir** (Cian #4DD0E1): Botones de compartir/enviar. Ej: icono Send en Compartir ruta.
- **IconExportar** (Amber #FFB74D): Botones de exportar/guardar/descargar. Ej: iconos GPX, CSV, ZIP.
- **IconFavorito** (Dorado #FFD54F): Botones de marcar/agregar a favoritos. Ej: icono corazon en Vincular Favoritos.
- **Definiciones**: Centralizadas en Color.kt (com.example.ui.theme). Visibles sobre fondo oscuro #1C1B1F en cualquier tema.
## 13/07/2026 — Esquema de referencia de pantallas (ESQUEMA_PANTALLAS.md)
- Se creo Documentacion/ESQUEMA_PANTALLAS.md con diagramas ASCII de todas las pantallas del proyecto.
- Cada pantalla tiene un sistema de numeracion PANTALLA.BARRA.BOTON para referenciar elementos UI sin ambiguedad.
- Incluye colores semanticos para botones de accion (Compartir=Cian, Exportar=Amber, Favorito=Dorado).
- El agente ahora carga este esquema al iniciar sesion para mejorar la comunicacion.
- Los botones de la sub-barra de detalle ahora usan colores semanticos visibles sobre fondo oscuro.

## 14/07/2026 — Sub-barra dinámica de historial (DynamicHistoryBottomBar)
- **DynamicHistoryBottomBar.kt**: Nueva sub-barra contextual con dos estados:
  - **Normal**: Export All (Amber IconExportar), Delete All (Rojo IconEliminar), Close (←), GPS Accuracy (Verde/rojo segun precision).
  - **Multiselect**: Cancelar + contador + Export Selected (Amber) + Delete Selected (Rojo).
- **MainActivity.kt (Capa 2 BETA)**: Integrado DynamicHistoryBottomBar para ruta HISTORY via if-else. La capsula GPS se preserva en el bloque else para rutas DETAIL, EXPERIENCE_MAP, etc.
- **RouteViewModel.kt**: Nuevo estado _selectedRouteIds: MutableStateFlow<Set<Int>> con metodos toggleSelection(), clearSelection(), deleteRoutes().
- **HistoryScreen.kt**: Refactor para consumir iewModel.selectedRouteIds en lugar de estado local. Botones exportar/eliminar ahora operan sobre seleccion del ViewModel.
- **Color.kt**: Nuevo IconEliminar = Color(0xFFE74C3C) (Rojo) para acciones de eliminacion.
## 14/07/2026 — WaypointsHorizontalSelector (Puntos de Interés con Empty State)
- **Sección 4.5 (Puntos de Interés)**: Refactorizada en un composable independiente WaypointsHorizontalSelector.
- **Empty State integrado**: Cuando la ruta no tiene waypoints, se muestra un OutlinedCard con icono Place + mensaje "Sin puntos de interés. Añade el primero" + botón "Añadir" que navega a la creación.
- **Modo con datos**: LazyRow horizontal de ElevatedCards, cada una con foto (AsyncImage o icono Place por defecto), nombre en negrita y descripción en dos líneas.
- **Diagnóstico en Logcat**: LaunchedEffect registra el conteo de waypoints con tag "BitaruDebug" para depuración.
- **Navegación robusta**: LaunchedEffect en MainActivity.kt asegura que selectRouteForDetail() se ejecute al navegar a DetailScreen.
- **Creación desde detalle**: prepareWaypointCreation(routeId) en RouteViewModel sincroniza el estado para que el flujo de creación de waypoints funcione en rutas del historial.


## Dock inferior unificado en Pantalla de Detalle (v2.0)
La DetailScreen ahora usa un diseño inmersivo sin barra superior. El boton de retroceso y el titulo "Detalle" se integraron en el dock inferior flotante, que contiene (de izquierda a derecha): [←] [Explorar] [Favoritos] [Editar] [Menú] "Detalle" [GPS]. Esto mejora la ergonomia a una mano y libera espacio vertical para el contenido.

## Control Inteligente de Centrado del Mapa de Experiencia (15/07/2026)
- **Pantalla 5.0 (ExperienceMap)**: El mapa centra la ruta automáticamente al cargar la pantalla mediante zoomToBoundingBox. El centrado se ejecuta **una sola vez** gracias a la bandera hasInitiallyCenteredRoute.
- **Navegación libre**: Una vez centrada la ruta, el usuario puede desplazar y hacer zoom manualmente sin que el GPS o la actualización de puntos interfiera con la cámara.
- **Gestos preservados**: Zooms táctiles, arrastres y rotaciones manuales no son anulados por el motor de geolocalización.

## Sincronizacion Norteada Forzada en Experiencias (15/07/2026)
- **Pantalla 5.0 (ExperienceMap)**: El mapa fuerza orientacion 0 grados (norte verdadero) cuando la auto-rotacion de navegacion esta desactivada.
- **Guidelines corregidas**: Las lineas de guia entre el panel de fotos y los puntos geograficos ahora proyectan correctamente porque `toPixels()` y las coordenadas del layout de Compose operan en el mismo sistema cartesiano.
- **Exploracion libre**: El usuario puede hacer pan y zoom manual sin interferencias de rotacion residual.


## Panel de Fotos Inmersivo en Experiencias (15/07/2026)
- **Pantalla 5.0 (ExperienceMap)**: El panel de fotos translucido (CAPA 1, lateral izquierdo) ahora ocupa el 100% de la altura de pantalla desde el borde superior fisico, eliminando el espacio muerto que dejaba statusBarsPadding().
- **Padding minimo**: 8dp de separacion superior y lateral para mantener distancia visual con los bordes del dispositivo.
- **Scroll autonomo**: El contenido del panel sigue desplazandose con verticalScroll sin perdida de frames ni clipping.

## 16/07/2026 — LocationService desintegrado en managers (AL-01 Fase I)
- **ServiceNotificationManager**: Encapsula la creación de canal de notificación, construcción de notificación de servicio en primer plano y obtención del pendingIntent de la app. Aislado de la lógica de tracking para facilitar tests unitarios y reemplazo de canales.
- **TrackingSessionManager**: Encapsula todo el estado de sesión activa de tracking: buffer de pre-grabación (60s elástico en ConcurrentLinkedDeque), odómetro (totalDistanceMeters/currentDistanceMeters), timer de sesión con callbacks de inactividad y rotación diaria, y estado reactivo via StateFlow (isTracking, currentRouteId, latestLocation, currentActivityMode).
- **Arquitectura**: LocationService ahora delega responsabilidades a estos dos managers mediante composición simple (field + delegación de llamadas), manteniendo los companion Flows como bridge para callers externos.
- **Próximo paso (AL-01 Fase II)**: Mover handlePreRecording(), preRecordingBuffer y Location.toBufferPoint() del LocationService al TrackingSessionManager.

## 16/07/2026 — AL-01 Fase II: BufferPoint + TrackingSessionManager puro
- **BufferPoint**: Nuevo DTO JVM (sin dependencias Android) con campos latitude, longitude, accuracy, speed, altitude, timestamp. Reemplaza el almacenamiento crudo de Location en el buffer de pre-grabación.
- **TrackingSessionManager**: Ahora usa ArrayDeque<BufferPoint> (FIFO circular, máx 60 puntos) en lugar de ConcurrentLinkedDeque<Location>. Inyecta RouteRepository en constructor para persistencia atómica de puntos. Odómetro delegado completamente (ddDistanceFromStep, setInitialDistance, getTotalDistance). Ciclo de sesión con startSession(routeId: Long) y stopSession(): List<LocationPoint>.
- **LocationService**: Saneado de todas las variables y funciones del monolito que ahora viven en sessionManager. El bridge 	riggerAutomaticTracking acepta List<BufferPoint> y convierte internamente a Location("gps") para preservar el CinematicFilterManager sin modificaciones.
- **Próximo paso (AL-01 Fase III)**: Suite de tests unitarios JVM para TrackingSessionManager (buffer FIFO, odómetro, ciclo de sesión).



## 17/07/2026 — UI-05: Geocodificación Aislada para Fotos EXIF
- **Estado separado**: La dirección de la foto (extraída de metadatos EXIF) ahora se almacena en photoExtractedAddress, independiente de currentAddress (GPS del dispositivo).
- **Comportamiento corregido**: Al marcar "Foto tomada en..." en el diálogo de guardar favorito, el campo de dirección muestra exclusivamente la ubicación de la foto, sin riesgo de contaminación por la posición actual del GPS.
- **Confirmación**: El botón Guardar también utiliza photoExtractedAddress cuando la opción de foto está activa.

## 17/07/2026 — ME-02 Paso 1: AutoTrackingManager (Nuevo Manager)
- **AutoTrackingManager.kt**: Nuevo gestor que encapsula la deteccion de movimiento sostenido para el arranque automatico de rutas.
- **Reduccion de LocationService**: 59 lineas eliminadas del servicio principal (4 variables de estado + bloque de logica de 84 lineas).
- **Arquitectura**: El manager se comunica con LocationService via la interfaz AutoTrackingListener, manteniendo el control del ciclo de vida del Service en el orquestador.
- **Inyeccion**: Provider unscoped en LocationModule para evitar buffers fantasmas entre sesiones.
- **Tests**: 5 tests unitarios JVM con mockk (21/21 suite completa).

## 18/07/2026 — ME-03: Inyección Directa EXIF + Micro-Mapa en Favoritos
- **Bug corregido (Sincronización EXIF-03)**: Al seleccionar una foto de la galería en el diálogo de guardar favorito, las coordenadas GPS del EXIF se inyectan directamente en los campos de latitud/longitud del formulario, eliminando la carrera asíncrona entre la corrutina de extracción y el clic de guardar.
- **Nuevos campos Lat/Lon**: El diálogo ahora muestra dos OutlinedTextField editables con las coordenadas numéricas, actualizándose al instante cuando se selecciona una foto con GPS.
- **Micro-mapa reflectivo**: Se agregó un mapa osmdroid (180dp) dentro del diálogo que se centra automáticamente en las coordenadas del formulario y muestra un marcador, dando retroalimentación visual inmediata al usuario.
- **Edición manual segura**: Si el usuario edita manualmente latitud o longitud, el sistema desactiva automáticamente la sobrescritura de la foto (usePhotoGps = false), preservando la intención del operador.
- **Inicialización al abrir**: Al abrir el diálogo, los campos se pre-rellenan con la última ubicación conocida del GPS del dispositivo.

## ME-06: Extraccion de Metadatos de Fotos (Julio 2026)

### Que cambio?
La app ahora extrae correctamente las coordenadas GPS y la fecha de creacion original de las fotos seleccionadas de la galeria o tomadas con la camara, incluso en Android 10+.

### Por que fallaba antes?
Android entrega las fotos a traves de un stream secuencial (Binder pipe) que no permite movimientos aleatorios dentro del archivo. El lector de metadatos EXIF necesita saltar hacia adelante y atras en el archivo para encontrar los datos GPS, y fallaba silenciosamente al no poder hacerlo.

### Como se soluciono?
La app copia temporalmente el archivo de la foto a su propia area de almacenamiento antes de leer los metadatos. Esto le da al lector acceso completo al archivo fisico, permitiendole saltar libremente y extraer todos los datos (GPS + fecha de la foto).

### Beneficio para el usuario
- Las coordenadas GPS de las fotos ahora se detectan correctamente (el boton "Ubicacion de la foto" se habilita)
- La fecha original de la foto se conserva al guardar un favorito desde una imagen
- No requiere permisos especiales de ubicacion en medios
- Compatible con fotos de galeria y de camara

## CR-09: Mapa Térmico por Velocidad (Julio 2026)

### ¿Qué cambió para el usuario?
- **Las rutas en el mapa ahora se ven con un gradiente térmico de colores**: Azul/Cian para tramos lentos, Verde para ritmo medio, Amarillo para ciclismo/carrera, Rojo para alta velocidad.
- El gradiente es continuo (HSV interpolado), no por saltos discretos de modo de actividad.
- Cada segmento entre dos puntos consecutivos recibe su propio color según la velocidad promedio del tramo.

### Arquitectura
| Capa | Componente | Responsabilidad |
|---|---|---|
| Dominio | RouteColorUseCase.speedToGradientColor() | Normaliza velocidad y genera color ARGB vía HSV puro Kotlin |
| UI | RouteSegmenter.toGradientRouteSegments() | Genera segmentos por par de puntos con color por velocidad |
| Presentación | MapScreen / ExperienceMapScreen | Consume segmentos y renderiza polilíneas individuales en osmdroid |

### Protecciones
- División por cero: si maxSpeed == minSpeed (delta < 0.001f), el normalized factor es 0.0f (color azul de reposo).
- Valores fuera de rango: coerceIn(0.0f, 1.0f) en la normalización.
- Sin dependencias Android framework en la lógica de dominio.



## CR-13: Branding Corporativo Bitaru en Pantalla de Bloqueo (Julio 2026)

### ¿Qué cambió para el usuario?
- **Nuevo banner corporativo Bitaru**: Al abrir la app con protección biométrica activada, ahora verás el logo rectangular de BiTaRu en la cabecera de la pantalla de bloqueo, con el subtítulo "SISTEMA PROTEGIDO POR BIOMETRÍA".
- **Icono de huella reactivo**: El icono del sensor biométrico cambia a color rojo cuando hay un error de autenticación, proporcionando retroalimentación visual inmediata.
- **Sin cambios funcionales**: La autenticación biométrica sigue funcionando exactamente igual. Es una mejora puramente visual de identidad corporativa.

### Arquitectura
| Componente | Archivo | Responsabilidad |
|---|---|---|
| BitaruBrandHeader | ui/components/BitaruBrandHeader.kt | Renderiza el banner rectangular corporativo con subtitle |
| BiometricLockScreen | ui/screens/BiometricLockScreen.kt | Pantalla de gating que consume BitaruBrandHeader |
| Asset | res/drawable/bitaru_logo.png | Logo corporativo BiTaRu-recta-003-esp |

### Detalles Técnicos
- Layout responsivo: illMaxWidth(0.70f) + spectRatio(3.5f) para escalar en cualquier densidad.
- ContentScale.Fit preserva la proporción sin crops ni distorsión.
- Sin dependencias de ViewModel ni capa de dominio (Regla de Oro I).


## CR-15: Compartir Experiencias con Fotos (Julio 2026)

### ¿Qué cambió para el usuario?
- **Nuevo botón Compartir** (📤 cian) en la barra inferior de la pantalla de Experiencias.
- Al presionarlo, abre el selector nativo de Android (Chooser) para compartir la experiencia actual (ruta + fotos) por cualquier app compatible (WhatsApp, Gmail, Telegram, etc.).
- Si la ruta tiene fotos asociadas (waypoints con foto), se adjuntan automáticamente al mensaje.
- El texto incluye el nombre de la ruta y la distancia total recorrida.

### Arquitectura
| Capa | Componente | Responsabilidad |
|---|---|---|
| Dominio | domain/usecase/ShareExperienceUseCase | Construye Intent (ACTION_SEND / ACTION_SEND_MULTIPLE) con fotos vía FileProvider |
| Presentación | RouteViewModel.shareExperience() | Obtiene ruta del repositorio, llama al UseCase, emite Intent a _shareIntentTrigger |
| UI | ExperienceMapScreen (botón Share) | Consume photoPathsForCurrentRoute, llama a viewModel.shareExperience() |
| Infraestructura | MainActivity (LaunchedEffect) | Colecta shareIntentTrigger y lanza context.startActivity(intent) |

### Canal Reactivo
- _shareIntentTrigger: MutableSharedFlow<Intent> permite que el ViewModel emita Intents sin depender de Context.
- MainActivity colecta en un LaunchedEffect(Unit) dentro de MainContainer.

## CR-16: Barra Inferior Unificada en Experiencias (Julio 2026)

### ¿Qué cambió para el usuario?
- **Barra inferior más compacta y funcional**: Se eliminó la barra flotante semitransparente (50%) que ocupaba espacio innecesario.
- **Nuevo layout**: Todos los controles están en una sola barra sólida en la parte inferior:
  - **Izquierda**: ← Volver, 📤 Compartir (cian), 💾 Exportar (amber)
  - **Derecha**: "Experiencias" + indicador de precisión GPS (🟢)
- **BackHandler**: Ahora el botón de retroceso del sistema y el gesto de deslizar también funcionan para salir de la pantalla de Experiencias.
- **Mapa más visible**: Al eliminar la capa flotante intermedia, el mapa de osmdroid tiene más área de visualización.

### Comportamiento
- **Compartir 📤**: Abre el selector de apps con la ruta actual y sus fotos adjuntas.
- **Exportar 💾**: Guarda la ruta completa como archivo ZIP/GPX en el almacenamiento local.
- **GPS 🟢**: Muestra la precisión actual de la señal satelital en tiempo real.

## CR-21: Pulido Visual de Barra 5.2 y Sincronización de Fotos al Compartir (2026-07-22)

### ¿Qué cambió para el usuario?
- **Barra inferior más elegante**: La sub-barra 5.2 ahora tiene fondo oscuro (0xFF121212) con íconos y texto en blanco, mejorando el contraste y la legibilidad sobre el mapa OpenStreetMap.
- **Indicador GPS satelital**: Cambió a color gris claro (0xFFB0BEC5) para integrarse mejor con el nuevo fondo oscuro.
- **Fotos siempre adjuntas al compartir**: Si la galería de fotos tarda en cargar, el sistema espera hasta 3 segundos para que las imágenes estén disponibles antes de lanzar el Intent de compartir.

### ¿Dónde está el cambio?
- **ExperienceMapScreen.kt**: Fondo oscuro de barra, íconos y textos blancos, GPS gris.
- **RouteViewModel.kt**: Guardia temporal con withTimeoutOrNull(3000L) en shareExperience().

### Detalles Técnicos
- El fondo oscuro se aplica via color = Color(0xFF121212) y 	onalElevation = 6.dp en el Surface de la barra 5.2.
- La guardia usa photoPathsForCurrentRoute.first { it.isNotEmpty() } dentro de withTimeoutOrNull(3000L) para garantizar la disponibilidad de fotos.
- ShareExperienceUseCase no fue modificado; ya maneja correctamente los adjuntos vía FileProvider.