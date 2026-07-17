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
| LocationService | @AndroidEntryPoint, 3 campos @Inject |
| Database/Repository | Singleton via DatabaseModule |
| Use Cases y Managers | Singleton via AppModule |

### Tests
- HiltTestRunner configurado para instrumentation tests
- Tests unitarios existentes: 16/16 pasando
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


