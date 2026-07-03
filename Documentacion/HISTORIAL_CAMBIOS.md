
**[02/07/2026 - Unificación de Formato SAF con generateRouteZipFile]**
- **ExperienceMapScreen.kt**: Se reemplazó la sugerencia de nombre del SAF (Storage Access Framework) de oute?.name a ROUTE_{id}_{sanitizedName}_{timestamp}.zip, replicando exactamente el patrón de generateRouteZipFile(). Ahora ambos caminos (Compartir y Guardar como ZIP) usan el mismo formato unificado.
- **RouteExportUseCase.kt**: Se agregó .getOrThrow() en xportRouteAsZip() para que el Result<Unit> del RouteExchanger lance excepción en caso de error, mejorando la propagación de fallos.

**[02/07/2026 - Nombre Único y Ubicación Persistente para ZIPs]**
- **RouteExportUseCase.kt**: Se centralizó la generación del nombre del ZIP en la nueva función generateRouteZipFile(). El nombre sigue el patrón ROUTE_{id}_{nombre}_{timestamp}.zip (ej: ROUTE_5_Paseo_del_Rio_20260702_143052.zip), eliminando colisiones y permitiendo exportar la misma ruta múltiples veces.
- **Sanitización robusta**: El nombre se limpia con eplace(Regex("[\s/\\\\?%*:|\"<>]"), "_") + .take(20) para preservar acentos, eñes y caracteres internacionales, eliminando solo caracteres prohibidos por el sistema de archivos.
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
  - 	rack.gpx: Coordenadas estándar GPX 1.1 (antes uta_telemetria.gpx).
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
- **NavRoutes.kt**: Agregado ROUTE_ANALYTICS + outeAnalytics() + a uxiliaryRoutes.
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
