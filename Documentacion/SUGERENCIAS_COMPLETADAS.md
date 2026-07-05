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
