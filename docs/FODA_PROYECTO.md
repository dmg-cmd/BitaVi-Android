# Análisis FODA del Proyecto BitaVi-Android (Localizador Táctico)

> **Fecha del Informe:** 14 de Octubre de 2026  
> **Proyecto:** BitaVi-Android (anteriormente BitaRu / Localizador)  
> **Espacio de Trabajo:** `e:/Sandbox/BitaVi-Android`  
> **Versión de SDK Objetivo:** Android SDK 35 (Android 15) | Min SDK 24  
> **Tecnologías Clave:** Kotlin 2.0, Jetpack Compose (MD3), Hilt DI, Room + SQLCipher, osmdroid, Groq API (Llama 3.3), Robolectric.

---

## 1. Resumen Ejecutivo y Contexto

**BitaVi-Android** es una aplicación táctica de registro de rutas, telemetría cinemática y navegación cartográfica orientada a la privacidad, el alto rendimiento y la independencia de servicios propietarios. Su propósito es capturar de forma continua y confiable trayectos a pie, en vehículo o en aeronaves comerciales, ofreciendo métricas honestas de altimetría, velocidad, detección inteligente de paradas y análisis asistido por Inteligencia Artificial (Groq Llama 3.3), todo bajo una arquitectura encriptada en reposo mediante SQLCipher y protegida por autenticación biométrica de grado hardware.

A continuación, se presenta un diagnóstico integral bajo la metodología **FODA** (Fortalezas, Oportunidades, Debilidades y Amenazas), seguido de un plan estratégico de soluciones para cada cuello de botella detectado y el desglose de los subagentes especializados involucrados en el ciclo de vida del proyecto.

---

## 2. Matriz FODA Integral

```
┌────────────────────────────────────────┬────────────────────────────────────────┐
│            FORTALEZAS (F)              │           OPORTUNIDADES (O)            │
│  - Seguridad criptográfica de hardware │  - Publicación en F-Droid / GitHub APK  │
│  - Filtrado cinemático multimodal       │  - Type-Safe Navigation Compose 2.8+   │
│  - Mapas libres offline (osmdroid)     │  - Expansión a sensores BLE e IoT      │
│  - Inyección de dependencias con Hilt   │  - Respaldo descentralizado (Nextcloud)│
│  - Suite de pruebas deterministas      │  - IA local on-device (SLM / Nano)     │
│  - Ecosistema de 7 agentes dedicados   │  - Exportación abierta (KML/GeoJSON)   │
├────────────────────────────────────────┼────────────────────────────────────────┤
│            DEBILIDADES (D)             │             AMENAZAS (A)               │
│  - MainActivity monolítica (>1100 l.)  │  - Doze Mode y cierres agresivos OEM   │
│  - Pantallas de Compose sobredimension.│  - Fiscalización estricta Google Play  │
│  - LocationService multicomponente     │  - Rate limits / caídas de Groq API    │
│  - Dispersión histórica de nombres     │  - Bloqueos de User-Agent en OSM tiles │
│  - Acumulación de respaldos y tmp/     │  - Degradación térmica y de batería    │
└────────────────────────────────────────┴────────────────────────────────────────┘
```

---

### 2.1. Fortalezas (F) — Ventajas Internas Consolidadas

1. **Seguridad y Criptografía de Grado Militar (Zero-Leakage):**
   - **Base de Datos Cifrada:** Persistencia local estructurada con [Room](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/data/db/AppDatabase.kt) sobre [SQLCipher](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/di/DatabaseModule.kt), generando una clave aleatoria única blindada por el Android Keystore del dispositivo.
   - **Almacenamiento de Credenciales:** Uso de `EncryptedSharedPreferences` y [BiometricCipherHelper.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/util/BiometricCipherHelper.kt) con validación mediante [BiometricPrompt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/security/BiometricSessionManager.kt) para proteger la API Key de Groq y datos sensibles.
2. **Filtrado Cinemático y Motor de Telemetría Robusto:**
   - [CinematicFilterManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/CinematicFilterManager.kt): Detección automática multimodal que conmuta dinámicamente entre modo terrestre y **Modo Vuelo Automático** (tolerando hasta 1200 km/h y 80 m de dispersión GPS con histéresis anti-flap).
   - [StationaryAnchorManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/StationaryAnchorManager.kt) y [ShortStopMarkManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/ShortStopMarkManager.kt): Supresión de dispersión por efecto túnel urbano o deriva GPS estática, consolidando automáticamente paradas continuas y midiendo el tiempo de permanencia sin saturar el historial con rutas de 0 metros.
3. **Independencia de Servicios Propietarios de Mapas:**
   - Implementación de [osmdroid](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/MapScreen.kt) y [MapTileSourceManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/map/MapTileSourceManager.kt): Múltiples fuentes cartográficas (OpenStreetMap estándar, vista satelital ArcGIS, topográfico) con memoria caché offline de hasta 500 MB, permitiendo operaciones en zonas sin cobertura celular.
4. **Arquitectura Limpia e Inyección de Dependencias Moderna:**
   - Adopción de Clean Architecture estricta con división en `data`, `domain` (casos de uso especializados como [CalculateRouteMetricsUseCase.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/domain/usecase/CalculateRouteMetricsUseCase.kt) y [PurgeOrphanPhotosUseCase.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/domain/usecase/PurgeOrphanPhotosUseCase.kt)) y presentación con Jetpack Compose.
   - Configuración de [Hilt con KSP](file:///e:/Sandbox/BitaVi-Android/app/build.gradle.kts) y Java 17 toolchain con Desugaring habilitado.
5. **Suite de Pruebas Automatizadas Determinista:**
   - Batería de más de 85 tests unitarios y de integración con Robolectric, MockK y Coroutines Test en [app/src/test/java](file:///e:/Sandbox/BitaVi-Android/app/src/test/java/com/aistudio/localizador/), optimizada con reciclado de JVM (`forkEvery = 50`) para prevenir `OutOfMemoryError` en entornos con recursos ajustados.
6. **Ecosistema Gobernado por Agentes de Software Especializados:**
   - Marco de gobernanza técnica formalizado en [.agents/AGENTS.md](file:///e:/Sandbox/BitaVi-Android/.agents/AGENTS.md) con 7 subagentes que vigilan compilación limpia, límites modulares, protección de secretos y ergonomía visual.

---

### 2.2. Oportunidades (O) — Factores Externos y Evolución

1. **Distribución en Tiendas Orientadas a Privacidad y FOSS:**
   - La arquitectura basada en Room, SQLCipher y osmdroid prescinde de Google Maps API, permitiendo empaquetar una variante 100% libre para **F-Droid**, distribuciones AOSP puras o despliegues corporativos cerrados vía GitHub Releases.
2. **Modernización a Type-Safe Navigation Compose (Compose 2.8+):**
   - Posibilidad de reemplazar la navegación actual orquestada en `MainActivity.kt` por rutas tipadas nativas mediante `@Serializable`, eliminando estados manuales y reduciendo el acoplamiento global.
3. **Expansión hacia IoT, Sensores Deportivos y Balizas BLE:**
   - La presencia embrionaria de [ProximityBleManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/ProximityBleManager.kt) abre la puerta a vincular pulsómetros Bluetooth (BLE Heart Rate), sensores de cadencia de ciclismo, balizas de emergencia o rastreadores de activos de proximidad.
4. **Interoperabilidad de Datos Abiertos y Nubes Soberanas:**
   - Expandir el soporte actual de exportación (GPX y Excel) hacia formatos geoespaciales estándar como KML, GeoJSON y sincronización automática bidireccional contra instancias autoalojadas de Nextcloud o servidores WebDAV personales.
5. **Inteligencia Artificial Híbrida / On-Device (SLM Local):**
   - Incorporar modelos de lenguaje ligeros en el dispositivo (como Llama 3.2 1B o Gemma 2B mediante MediaPipe LLM Inference) para redactar bitácoras de viaje y diagnósticos de ruta incluso en alta montaña o en vuelo sin conexión a internet ni consumo de saldo en Groq.

---

### 2.3. Debilidades (D) — Áreas Críticas Internas

1. **`MainActivity.kt` Hipertrofiada y Multirrol (>1100 líneas):**
   - [MainActivity.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/MainActivity.kt) acumula responsabilidades directas de verificación de permisos, callbacks de biometría, lanzadores de actividad para importación/exportación de archivos ZIP, instanciación de diálogos y control del Drawer lateral.
2. **Composables de Pantalla Monolíticos:**
   - [MapScreen.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/MapScreen.kt) cuenta con más de 1300 líneas y un tamaño superior a 57 KB; igualmente [SavedLocationsScreen.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/SavedLocationsScreen.kt) supera las 1000 líneas. Mezclan gestión de ciclo de vida del mapa, lógica de interacción táctil, observabilidad de estados y composición visual sin granularidad en subcomponentes atómicos.
3. **Complejidad y Acoplamiento en `LocationService.kt` (>1000 líneas):**
   - Pese a contar con módulos de soporte, [LocationService.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/LocationService.kt) mantiene una alta densidad de código al entrelazar el ciclo de vida del servicio en primer plano, persistencia directa mediante repositorios, notificaciones push, registro de puntos y radiodifusión de broadcasts.
4. **Dispersión Histórica de Identidad y Nomenclatura:**
   - Coexisten vestigios de nombres en capas diferentes: namespace `com.aistudio.localizador`, strings que exhiben `BitaVi`, cabeceras gráficas con `BitaruBrandHeader.kt` y documentación antigua que refiere a "Localizador". Esto confunde la trazabilidad y la incorporación de nuevos desarrolladores.
5. **Acumulación de Artefactos Obsoletos y Carpetas Temporales:**
   - Existen directorios sueltos como [tmp/](file:///e:/Sandbox/BitaVi-Android/tmp/) (con scripts `.js` de edición previa) y decenas de archivos históricos en [RESPALDO_ANTERIOR/](file:///e:/Sandbox/BitaVi-Android/RESPALDO_ANTERIOR/) que consumen espacio en el repositorio y ensucian las búsquedas globales de código.

---

### 2.4. Amenazas (A) — Factores de Riesgo Externos

1. **Matanza Agresiva de Procesos en Segundo Plano (OEM Battery Killers):**
   - Fabricantes como Samsung (OneUI), Xiaomi (MIUI/HyperOS), Huawei y OnePlus implementan políticas de ahorro de energía muy restrictivas que suspenden servicios en primer plano de tracking prolongado a menos que el usuario desactive manualmente la optimización de batería.
2. **Restricciones y Auditorías de Permisos en Google Play Store:**
   - Los permisos `ACCESS_BACKGROUND_LOCATION` y `ACCESS_MEDIA_LOCATION` exigen justificación en video y revisión manual exhaustiva por parte del equipo de Google Play, con riesgo constante de rechazo o suspensión si se cambian los flujos de solicitud.
3. **Riesgos de Disponibilidad y Cuotas en Servicios de Terceros:**
   - Dependencia de la API de Groq para resúmenes de IA: caídas de servicio o cambios en los precios y cuotas de tokens de Llama 3.3 impactan directamente la experiencia de usuario.
   - Servidores de Tiles de OpenStreetMap: aplican baneos automáticos de IP o User-Agent si las peticiones exceden su política de uso justo (*Tile Usage Policy*).
4. **Consumo Eléctrico y Estrés Térmico del Dispositivo:**
   - El uso continuo del receptor GNSS en modo alta precisión, combinado con la pantalla OLED activa en trayectos al sol y el cálculo de métricas en segundo plano, puede provocar estrangulamiento térmico (*thermal throttling*) y rápido agotamiento de la batería en caminatas de más de 4 horas.

---

## 3. Plan Estratégico y Sugerencias de Arreglo

A continuación se detalla la propuesta técnica de mitigación para cada una de las debilidades y amenazas identificadas, priorizada por impacto arquitectónico:

```
┌────────────────────────────────────────────────────────────────────────┐
│               HOJA DE RUTA DE OPTIMIZACIÓN TÉCNICA                    │
├────────────┬───────────────────────────────┬───────────────────────────┤
│ Prioridad  │ Iniciativa                    │ Módulos Involucrados      │
├────────────┼───────────────────────────────┼───────────────────────────┤
│ P1 (Alta)  │ Modularización de MainActivity│ MainActivity, NavRoutes   │
│ P1 (Alta)  │ Descomposición de MapScreen   │ MapScreen, ui/map/        │
│ P2 (Media) │ Fachada en LocationService    │ LocationService, managers │
│ P2 (Media) │ Saneamiento de Proyecto & tmp │ tmp/, RESPALDO_ANTERIOR/  │
│ P3 (Media) │ Resiliencia ante Doze/OEM     │ BatteryOptimizationDialog │
│ P3 (Baja)  │ Motor IA Híbrido Offline      │ GeminiAiUseCase, OnDevice │
└────────────┴───────────────────────────────┴───────────────────────────┘
```

### Sugerencia 1: Refactorización de `MainActivity.kt` hacia Navegación Declarativa
* **Diagnóstico:** `MainActivity.kt` centraliza navegación, permisos, diálogos y biometría en un solo bloque de más de 1100 líneas.
* **Solución Propuesta:**
  1. Extraer el contenedor de navegación a una clase composable dedicada: `BitaViAppScaffold.kt`.
  2. Implementar `Navigation Compose 2.8+` con rutas tipadas mediante clases `@Serializable` (por ejemplo `sealed interface RouteDestination`).
  3. Mover los lanzadores de permisos y selección de archivos a un manejador delegado: `ActivityContractManager.kt`.
  4. Reducir `MainActivity` a una estructura delgada (< 150 líneas) cuya única labor sea configurar `enableEdgeToEdge()`, inicializar el tema y alojar el Scaffold principal.

### Sugerencia 2: Descomposición Modular de Pantallas Masivas (`MapScreen` y `SavedLocationsScreen`)
* **Diagnóstico:** `MapScreen.kt` (>1300 líneas) y `SavedLocationsScreen.kt` concentran la vista completa, capas de overlays, botones flotantes y hojas de ruta.
* **Solución Propuesta:**
  1. Extraer los overlays de `osmdroid` en componentes aislados bajo `ui/screens/map/overlays/`:
     - `TrackingRoutePolylineOverlay.kt`: traza cinemática activa.
     - `WaypointsMarkersOverlay.kt`: gestión y reciclaje de iconos de marcadores.
     - `MapControlsDock.kt`: dock flotante inferior translúcido.
  2. Separar los eventos de intención de usuario usando un patrón MVI claro (`MapUiIntent` y `MapUiState`).

### Sugerencia 3: Patrón Fachada y Delegación de Ciclo de Vida en `LocationService`
* **Diagnóstico:** `LocationService.kt` es responsable de crear canales de notificación, registrar puntos en base de datos, escuchar proveedores de hardware y difundir estados a la UI.
* **Solución Propuesta:**
  1. Crear un `LocationPersistenceDelegate.kt` que absorba la escritura asíncrona en base de datos (`Dispatchers.IO`).
  2. Delegar la orquestación de notificaciones al ya existente [ServiceNotificationManager.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/location/manager/ServiceNotificationManager.kt).
  3. Mantener `LocationService` estrictamente como un orquestador del ciclo de vida del servicio (`onStartCommand`, `onDestroy`, `onBind`).

### Sugerencia 4: Higiene del Repositorio y Política de Retención de Respaldos
* **Diagnóstico:** Existen scripts sueltos en `tmp/` y más de 30 archivos en `RESPALDO_ANTERIOR/` sin orden de depuración.
* **Solución Propuesta:**
  1. Purgar la carpeta `tmp/` de scripts obsoletos de Node/JavaScript (`temp_edit*.js`).
  2. Establecer una política gestionada por el agente `doc-custodian` para mover respaldos con más de 30 días a un archivo comprimido o eliminarlos si los cambios ya están consolidados en Git con tags.
  3. Unificar el branding en cadenas de texto, layouts y documentación bajo la identidad definitiva: **BitaVi**.

### Sugerencia 5: Mitigación de Suspensión por Batería (OEM / Doze Mode)
* **Diagnóstico:** En dispositivos Samsung, Xiaomi y Huawei el servicio puede cerrarse silenciosamente tras 30 minutos de pantalla apagada.
* **Solución Propuesta:**
  1. Implementar un diálogo contextual amigable que verifique `PowerManager.isIgnoringBatteryOptimizations()`.
  2. Ofrecer un botón que redirija directamente a la pantalla de configuración de batería del sistema mediante el Intent `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  3. Implementar un *Watchdog Alarm* con `AlarmManager.setExactAndAllowWhileIdle()` que despierte el servicio de forma periódica en caso de desconexión anómala del GPS.

### Sugerencia 6: Estrategia de IA Híbrida y Respaldo Local
* **Diagnóstico:** Sin red o sin clave de Groq, la analítica inteligente de rutas queda desactivada.
* **Solución Propuesta:**
  1. Diseñar un fallback analítico basado en reglas cinemáticas deterministas (ya implementado parcialmente en [CalculateRouteMetricsUseCase.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/domain/usecase/CalculateRouteMetricsUseCase.kt)), permitiendo generar reportes de rendimiento físico en Markdown sin tocar la red.
  2. En fases posteriores, incorporar la inferencia local con SLMs on-device para sintetizar la bitácora de viaje 100% offline.

---

## 4. Catálogo de Subagentes Especializados Involucrados

El desarrollo, mantenimiento y control de calidad de BitaVi-Android está estructurado bajo una plantilla de **7 subagentes especializados** (formalizados en [.agents/AGENTS.md](file:///e:/Sandbox/BitaVi-Android/.agents/AGENTS.md) y [.agents/skills/](file:///e:/Sandbox/BitaVi-Android/.agents/skills/)), cada uno con directivas operativas estrictas:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                     MATRIZ DE SUBAGENTES DEL PROYECTO BITAVI                          │
├──────────────────────────┬─────────────────────────────┬───────────────────────────────┤
│ Subagente                │ Especialidad Primaria       │ Archivos y Dominios Clave     │
├──────────────────────────┼─────────────────────────────┼───────────────────────────────┤
│ doc-custodian            │ Bitácoras, Walkthroughs     │ HISTORIAL_CAMBIOS, Walkthroughs│
│ telemetry-gps-specialist │ GPS, Cinemática, Anti-OOM   │ LocationService, managers/    │
│ compose-ui-tactical      │ Jetpack Compose & Ergonomía │ ui/screens/, ui/theme/Color.kt│
│ gradle-compiler-qa       │ Compilación Cero Warnings   │ build.gradle.kts, tests/      │
│ groq-route-analyst       │ IA de Rutas (Groq LLaMA)    │ GeminiAiUseCase, Retrofit API │
│ git-release-guardian     │ Seguridad Git & Releases    │ .gitignore, Git Tags, Secs.   │
│ scope-boundary-auditor   │ Contención de Alcance       │ Allowlist de archivos, PRs    │
└──────────────────────────┴─────────────────────────────┴───────────────────────────────┘
```

### 1. `doc-custodian` (Custodio de Memoria Histórica y Documentación)
* **Rol:** Guardián de bitácoras, respaldos preventivos e informes de entrega.
* **Responsabilidades:**
  - Garantizar que cualquier clase crítica (`LocationService.kt`, `RouteViewModel.kt`, `MainActivity.kt`) sea respaldada en `RESPALDO_ANTERIOR/` antes de una modificación profunda.
  - Mantener actualizados [HISTORIAL_CAMBIOS.md](file:///e:/Sandbox/BitaVi-Android/Documentacion/HISTORIAL_CAMBIOS.md) y [DOCUMENTACION.md](file:///e:/Sandbox/BitaVi-Android/Documentacion/DOCUMENTACION.md).
  - Generar el informe de cierre en `Documentacion/Walkthroughs/` con el estándar de nombres `walkthrough_AAAAMMDD_HHMM_[tema].md`.

### 2. `telemetry-gps-specialist` (Especialista en Telemetría y Filtrado GNSS)
* **Rol:** Custodio del motor cinemático, captura en segundo plano y rendimiento cartográfico.
* **Responsabilidades:**
  - Garantizar que todo cálculo cinemático, filtrado de coordenadas y persistencia en Room se ejecute en `withContext(Dispatchers.IO)` sin bloquear la UI.
  - Asegurar la estabilidad de `LocationService.kt` y la prevención de fugas de memoria en osmdroid mediante reciclaje de pools de marcadores y limpieza de capas (`overlays.clear()`).
  - Calibrar perfiles dinámicos terrestres y de vuelo, suprimiendo rebotes de precisión (HDOP/accuracy).

### 3. `compose-ui-tactical` (Especialista en UI Táctica Jetpack Compose)
* **Rol:** Vigilante de la interfaz táctica, ergonomía en campo y coherencia visual Material Design 3.
* **Responsabilidades:**
  - Asegurar la adhesión al esquema taxonómico de interfaz (`PANTALLA.BARRA.BOTON`).
  - Utilizar estrictamente las constantes semánticas de color de [Color.kt](file:///e:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/theme/Color.kt) (`IconCompartir`, `IconExportar`, `IconFavorito`, etc.).
  - Preservar tipografía táctica de alto contraste (mínimo `32.sp` en métricas críticas de velocidad y distancia para legibilidad en intemperie o con vibración).

### 4. `gradle-compiler-qa` (Auditor de Compilación y QA de Pruebas)
* **Rol:** Custodio de la integridad de compilación y suite de pruebas unitarias.
* **Responsabilidades:**
  - Cumplir la **Directiva de Cero Advertencias**: verificar que el código compile sin warnings en Kotlin ni Java, eliminando imports muertos y código deprecado.
  - Ejecutar tareas reales de `./gradlew` diagnosticando las salidas de compilador y reportes de lint.
  - Verificar que la suite de pruebas automatizadas permanezca 100% verde antes de liberar cambios.

### 5. `groq-route-analyst` (Especialista en Inteligencia Artificial y Rutas)
* **Rol:** Supervisor de la integración analítica con Groq (Llama 3.3).
* **Responsabilidades:**
  - Garantizar que los prompts hacia Groq devuelvan payloads JSON estrictos y deserializables por los DTOs de Moshi/Kotlin.
  - Diseñar resiliencia ante cortes de red en áreas rurales o sin cobertura, gestionando colas locales.
  - Optimizar la carga de datos (muestreo y compresión de coordenadas) antes del envío para reducir latencia y costo de tokens.

### 6. `git-release-guardian` (Guardián de Git, Publicación y Prevención de Fugas)
* **Rol:** Auditor de seguridad en control de versiones y gestor de entregas formales.
* **Responsabilidades:**
  - **Política de Fuga Cero:** Auditar que ningún archivo de credenciales (`local.properties`, `.env`, `*.keystore`, `*.jks`, volcados de bases de datos o llaves privadas) ingrese a Git.
  - Asegurar commits atómicos según la convención estándar (`feat:`, `fix:`, `docs:`, `refactor:`).
  - Gestionar el versionado semántico formal (`vX.Y.Z`), etiquetas anotadas en Git y redacción de Release Notes oficiales.

### 7. `scope-boundary-auditor` (Auditor de Contención de Alcance y Fronteras)
* **Rol:** Verificador de impacto transversal y contención del radio de afectación (*blast radius*).
* **Responsabilidades:**
  - Delimitar la lista blanca de archivos autorizados antes de cada tarea, bloqueando ediciones involuntarias fuera de alcance.
  - Prevenir efectos secundarios en la persistencia o el ciclo de vida del servicio al modificar pantallas de UI o viceversa.
  - Asegurar que la reactivación automática por alejamiento permanezca funcional tras cualquier ajuste en la lógica de paradas.

---

## 5. Conclusión y Recomendación Final

BitaVi-Android posee una base técnica e ingenieril sumamente avanzada, destacándose por su enfoque en la privacidad del usuario, su motor de telemetría cinemática autocalibrable y una protección criptográfica superior a la media de aplicaciones comerciales.

El principal desafío actual no radica en incorporar nuevas funciones, sino en ejecutar una **refactorización estructural orientada a la mantenibilidad**:
1. Desarticular el monolito de `MainActivity.kt` mediante Navigation Compose.
2. Dividir `MapScreen.kt` en subcomponentes atómicos.
3. Saneamiento definitivo de la identidad y limpieza de residuos en el repositorio.

La coordinación entre los 7 subagentes especializados garantiza que estas optimizaciones puedan llevarse a cabo con precisión quirúrgica, contención de alcance estricta y riesgo cero de regresiones.
