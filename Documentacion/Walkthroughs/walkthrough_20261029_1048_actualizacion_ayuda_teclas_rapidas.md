# Informe de Verificación: Actualización Integral de Ayuda y Teclas Rápidas (v3.2.15-RC1)

**Fecha:** 2026-10-29 10:48  
**Versión:** `v3.2.15-RC1` (`versionCode = 15`, `versionName = "3.2.15"`)  
**Dispositivo verificado en vivo:** Google Pixel 9 Pro XL (`komodo` - `172.16.2.37:45439`)  
**Especialistas involucrados:** `compose-ui-tactical`, `version-sync-guardian`, `gradle-compiler-qa`, `doc-custodian`

---

## 1. Resumen Ejecutivo
Se rediseñó y actualizó completamente el centro de **Ayuda y Funcionamiento** ([`HelpDialog.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/dialogs/HelpDialog.kt)) de la aplicación BitaVi. El diálogo anterior se encontraba desactualizado respecto a las capacidades actuales de la arquitectura y carecía de una sección de atajos y gestos.

El nuevo diálogo incorpora una barra horizontal interactiva de categorías mediante chips (`FilterChip`), situando como categoría predeterminada y destacada **⚡ Teclas Rápidas** con etiquetas tácticas (*1 TOQUE*, *PULSACIÓN LARGA*, *CABECERA*, *BOTÓN FLOTANTE*), además de secciones especializadas para *Mapa y GPS*, *Favoritos y Rutas*, *IA y Respaldo*, y una vista consolidada de *Todos*.

---

## 2. Características del Nuevo Diálogo de Ayuda

### A. Categoría "⚡ Teclas Rápidas" (Predeterminada)
- **Contraer Todo (`Icons.Default.UnfoldLess` | Badge: 1 TOQUE):** Plegado instantáneo de todos los países, ciudades y fechas tanto en Favoritos como en Rutas.
- **Expandir Todo (`Icons.Default.UnfoldMore` | Badge: 1 TOQUE):** Despliegue simultáneo de todas las carpetas y tarjetas completas sin abrirlas una a una.
- **Marcador Rápido en Mapa (`Icons.Default.TouchApp` | Badge: PULSACIÓN LARGA):** Creación directa de Waypoints o guardado de un favorito manteniendo presionado cualquier punto del mapa.
- **Selección Múltiple (`Icons.Default.Checklist` | Badge: PULSACIÓN LARGA):** Selección en lote en 'Mis Recorridos' para exportar o eliminar varias rutas a la vez.
- **Visor Táctico de Fotos (`Icons.Default.Image` | Badge: 1 TOQUE):** Apertura en alta definición a pantalla completa al tocar la foto de cualquier favorito.
- **Telemetría Rápida (`Icons.Default.Navigation` | Badge: CABECERA):** Toque en la barra superior para desplegar métricas en tiempo real de velocidad media, altitud y modo de tráfico.

### B. Categoría "🗺️ Mapa y GPS"
- Selector de capas (OSM, Satelital ArcGIS, Topográfico).
- Centrado GPS inmediato sobre posición en vivo.
- Modo Automático cinemático (>25 m de movimiento, parada tras 15 min de reposo).
- Modo Vuelo continuo (>150 km/h y ascenso, traza ininterrumpida hasta 1200 km/h).
- Mapas fuera de línea (modo offline sin gasto de datos móviles).

### C. Categoría "⭐ Favoritos y Rutas"
- Importación directa desde la libreta de contactos.
- Guardado de posición actual con notas, fotos, categorías y colores.
- Visualización interactiva 3D en Mapa de Experiencia.
- Importación de rutas GPX y paquetes ZIP de BitaVi.

### D. Categoría "🤖 IA y Respaldo"
- Analista de rutas con IA Groq (Llama 3.3).
- Respaldo completo en archivo ZIP para Google Drive o almacenamiento local.
- Restauración de datos y fotos desde paquetes de respaldo.

---

## 3. Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 15`, `versionName = "3.2.15"`
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.15-RC1"`
- `BrandWatermark.kt`: `version: String = "v3.2.15-RC1"`

---

## 4. Verificación y Despliegue en Vivo
- **Pruebas Unitarias Gradle:** `.\gradlew.bat testDebugUnitTest` -> `BUILD SUCCESSFUL` (40 tareas superadas).
- **Compilación de Artefacto:** `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL` en 48 segundos.
- **Despliegue ADB Wi-Fi:** Instalado en Google Pixel 9 Pro XL (`172.16.2.37:45439`) con resultado `Success` y reinicio validado del proceso.
