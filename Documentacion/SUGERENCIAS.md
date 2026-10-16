- [x] **FASE 1.3 LocationService + WakeLock (2026-10-15)**: Patrón Fachada con 3 delegados inyectables por Hilt (GpsLifecycleDelegate, LocationNotificationDelegate, LocationPersistenceDelegate); LocationService reducido 1057 -> 806 líneas. WakeLock corregido: acquire() SIN timeout mientras la grabación está activa (antes expiraba a las 25h y podía cortar el GPS) y release inmediato en modo pasivo/pausa/detenido. Compila, suite 109/0 y deploy OK en 2 dispositivos.
- [x] **CIERRE FASE 1.1/1.2 (2026-10-15)**: Modularización de MapScreen (1173->243) y SavedLocationsScreen (1055->153) + 5 tests nuevos de pantallas + reparación de cachés Gradle con rutas obsoletas (`D:\Google IA Studio`) que rompían KSP + 3 tests preexistentes corregidos. Suite 109/0 verde y deploy OK.
- [x] **CR-29 CalculateRouteMetricsUseCase**: Analitica de tramos desacoplada del UI thread (Dispatchers.Default + flatMapLatest), desnivel acumulado Subida/Bajada con histeresis de 2.5m, charts memoizados con remember(points). 7 tests nuevos; suite 100/0.
- [x] **CR-30 PurgeOrphanPhotosUseCase**: Nuevo modulo inyectable que escanea y purga imagenes JPG/PNG huerfanas (sin referencia en waypoints/favoritos) del almacenamiento privado de la app. 7 tests nuevos; suite 93/0.
## Completadas (Julio 2026)

- [x] **CR-28 Suite de pruebas 100% verde y determinista**: resueltos OOM de MockK/Robolectric (forkEvery=50 + heap daemon 4g + fix del bucle infinito en FileProvider), corregidos ActivityModeManagerTest (contrato real, DRIVING a 20km/h + límites de umbral), ShortStopMarkManagerTest (timestamp=any + runTest determinista) y RouteExportUseCaseTest (Robolectric + mockkStatic FileProvider). Resultado: 86 tests, 0 fallos, 3 corridas consecutivas.

- [x] **CR-27 Modo Vuelo Automatico**: Se elimino el interruptor 8.3 (era un toggle fantasma) y se implemento deteccion cinematica automatica FLIGHT (v>150 km/h y altitud>100m, hasta 1200 km/h) con umbrales de precision dinamicos en LocationService y badge "✈️ Vuelo" en la TopBar

- [x] **CR-26 Compartir captura PNG de Experiencia**: El boton Compartir (cian, 5.2.1) de la Pantalla 5 ahora envia una captura PNG HD (mapa + ruta + fotos) via rememberGraphicsLayer, sin la barra inferior de botones

- [x] **CR-25 Fix Boton Actualizar Pais/Ciudad**: El boton ya funciona: muestra feedback en tiempo real (pais • ciudad) y al guardar ya no se revierte a "Desconocido"

- [x] **CR-24 Cobertura Mundial WGS84**: País/ciudad de favoritos ahora se determinan en cualquier país del mundo (se eliminó la restricción al Cono Sur que bloqueaba el fallback Nominatim)

- [x] **CR-23 Fallback Nominatim (OSM)**: País/ciudad/lugar de los favoritos ya no quedan en "Desconocido" — fallback a OpenStreetMap cuando el geocoder del sistema falla

- [x] **CR-22 Fecha EXIF en favoritos**: Fecha de captura de la foto visible en la tarjeta de favoritos y en el comentario (📸 Capturada el dd/MM/yyyy HH:mm), también al guardar desde el mapa

- [x] **CR-21 Fix EXIF GPS API 29+**: Favoritos con foto ahora usan la posición GPS de la foto (Scoped Storage + ACCESS_MEDIA_LOCATION) en lugar de la posición actual del dispositivo

- [x] **CR-07 AddFavoriteFromTrackingUseCase**: Guardar favorito rápido durante tracking activo con geocodificación inversa, validación GPS (0.0, 0.0) y vinculación a ruta

- [x] **ME-01 Foundation**: Migrar a Hilt con KSP en lugar de kapt para compatibilidad AGP 9
- [x] **ME-01 MainActivity**: Agregar @AndroidEntryPoint y migrar ViewModel a y viewModels()
- [x] **ME-01 AppModule**: Crear módulo con providers para todos los use cases
- [x] **ME-06**: Migrar OpenDocument() → GetContent() para forzar selector local de imágenes; corregir título diálogo 9.3
- [x] **ME-01 LocationService**: Migrar a @AndroidEntryPoint con field injection
- [x] **ME-01 Tests**: Configurar HiltTestRunner + dependencias de testing
- [x] **AL-01 Bugfix**: StationaryAnchorManager.processStationaryState() no congelaba anclaje (causaba fallo en test de drift)
- [x] **ME-04**: Migrar RouteExportUseCase de ui/ a domain/usecase/ con API Result<T> y SharedFlow para Intents
# Lista de Sugerencias y Mejoras Pendientes

## Prioridad 1: IA y Multimedia Avanzada (Fase 4)
- [ ] **Análisis Multimodal**: Hacer que la IA (Llama 3) reciba descripciones de las fotos tomadas para generar un diario de viaje mucho más humano y descriptivo.
- [ ] **Mejora de Gráficos**: Añadir comparativa visual de ritmos entre diferentes rutas del mismo trayecto.
- [ ] **Video-Resumen**: Investigar la posibilidad de crear un video corto animado (o GIF) con el movimiento del punto sobre el mapa para redes sociales.

## Prioridad 2: Infraestructura (Fase 5)
- [ ] **Sincronización Cloud**: Investigar integración con Google Drive para sincronizar favoritos entre dispositivos sin depender solo del archivo ZIP.

---
*Nota: Este archivo se irá actualizando a medida que se completen las tareas. Las tareas terminadas se mueven a SUGERENCIAS_COMPLETADAS.md.*

## Prioridad 3: Investigación (04/07/2026)
- [ ] **Modo Inmersivo del Pixel**: Investigar si se puede personalizar el comportamiento nativo (ocultación de status bar al tocar el mapa) sin romper el renderizado de Compose. Alternativa: aceptar el comportamiento por defecto del SO.
- [ ] **Watermark definitivo**: Una vez confirmado el layout, decidir estilo final del texto "agenviaje" en la card (verde neón debug vs elegante alpha=0.4).






- [x] **CR-15 Compartir Experiencias**: ShareExperienceUseCase + shareExperience() + botón Share cian en barra 5.2
- [x] **CR-16 Barra Inferior Unificada**: CAPA 3 eliminada, barra 5.2 con layout de dos Row + BackHandler
