# Informe de Verificación: Telemetría Avanzada en Ventana Superior y Reubicación de Botones (v3.2.17-RC1)

**Fecha:** 2026-10-29 11:19  
**Versión:** `v3.2.17-RC1` (`versionCode = 17`, `versionName = "3.2.17"`)  
**Dispositivo verificado en vivo:** Google Pixel 9 Pro XL (`komodo` - `172.16.2.37:45439`)  
**Especialistas involucrados:** `compose-ui-tactical`, `telemetry-gps-specialist`, `version-sync-guardian`, `gradle-compiler-qa`, `doc-custodian`

---

## 1. Resumen Ejecutivo
Se implementó la reorganización ergonómica y ampliación de telemetría solicitada para la ventana de control superior ([`TopBarLocalizador.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/components/TopBarLocalizador.kt)):

1. **Reubicación de Botones al Extremo Derecho Superior:**
   - Los botones interactivos de atenuador de fondo (`Icons.Default.Opacity`) y de consulta de ubicación actual (`Icons.Default.LocationSearching`) se trasladaron a la fila superior alineados al extremo derecho arriba, liberando la totalidad del ancho inferior para métricas de campo.
2. **Nuevas Métricas Tácticas Incorporadas:**
   - **Altura (Altitud):** Lectura directa de la elevación en metros (`latestLocation.altitude.toInt() m`).
   - **Distancia desde el primer punto:** Cálculo geodésico dinámico entre el primer punto registrado de la ruta y la ubicación satelital activa en metros o kilómetros.
   - **Horas de funcionamiento (Tiempo activo):** Cronómetro en vivo de la sesión/ruta activa en formato legible (*Xh Ym* o *Xm Ys*).

---

## 2. Modificaciones Técnicas por Módulo

### A. Servicio de Localización (`LocationService.kt`)
- `_elapsedTimeSeconds` y `_currentDistanceMeters` convertidos a `StateFlow` reactivos (`elapsedTimeSeconds` y `currentDistanceMeters`), notificando a la interfaz Compose en tiempo real sin requerir sondeos periódicos.

### B. Gestión de Estado en ViewModel (`RouteViewModel.kt`)
- Se expusieron `elapsedTimeSecondsLive` y `currentDistanceMetersLive`.
- Se incorporó `distanceFromFirstPointMeters`: flujo reactivo calculado mediante `combine(activeRouteIdLive, latestLocationLive)` y `Location.distanceBetween`.
- Se agregaron funciones de formateo: `formatDistanceMeters(Double?)` y `formatElapsedTimeHours(Long)`.

### C. Rediseño de la Ventana Superior (`TopBarLocalizador.kt` y `MapTopBar.kt`)
- **Fila 1 (Cabecera):** Título corporativo "Bitácora Táctica de Rutas", chip "📍 UBICACIÓN" y versión a la izquierda; botones de atenuador y búsqueda en el **extremo derecho arriba**.
- **Fila 2 (Métricas Principales):** Distribución simétrica de *MODO DE TRÁFICO*, *VELOCIDAD* y *ALTURA*.
- **Fila 3 (Métricas Complementarias):** Tarjeta tonal translúcida que presenta *DISTANCIA 1º PTO* e icono de distancia, junto a *TIEMPO ACTIVO* e icono de cronómetro.
- **Fila 4:** Barra de calibración de color según velocidad.

### D. Integración en Pantalla de Mapa (`MapScreen.kt`)
- Observa y conecta la telemetría en vivo pasando `alturaText`, `distanciaText` y `tiempoText` a `MapTopBar`.

### E. Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 17`, `versionName = "3.2.17"`
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.17-RC1"`
- `BrandWatermark.kt`: `version: String = "v3.2.17-RC1"`

---

## 3. Verificación y Despliegue
- **Pruebas Unitarias:** `.\gradlew.bat testDebugUnitTest` -> `BUILD SUCCESSFUL` (40 tareas superadas).
- **Compilación de Artefacto:** `.\gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL` en 51s.
- **Despliegue ADB Wi-Fi:** Instalado y reiniciado con éxito en Google Pixel 9 Pro XL (`172.16.2.37:45439`).
