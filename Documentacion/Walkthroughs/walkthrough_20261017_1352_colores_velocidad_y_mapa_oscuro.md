# Informe Walkthrough: Escala de Colores por Velocidad de Alto Contraste, Leyenda en Cabecera y Reparación de Mapa Oscuro

- **Fecha y Hora:** 2026-10-17 13:52
- **Tema:** Visibilidad del Trazado por Velocidad en Mapa Claro, Referencia Visual en Cartel Superior y Reparación del Modo Táctico Oscuro
- **Estado:** Implementado y Verificado (`assembleDebug` + `testDebugUnitTest` 100% exitoso en nueva unidad `G:\`)

---

## 1. Problemas Abordados
1. **Migración de Unidad (`E:\` → `G:\`)**: Las cachés previas de compilación incremental (`app/build`, `.gradle`, `.kotlin`) conservaban rutas absolutas hacia `E:\Sandbox\BitaVi-Android`.
2. **Trazado de Ruta Poco Visible y Cambio de Color Confuso**:
   - El gradiente anterior (`speedToGradientColor`) interpolaba tonos HSV con `value = 0.95f` (pasando por amarillo claro, verde lima y celeste pálido) que se confundían con el fondo crema y verde claro de OpenStreetMap.
   - Al normalizar entre la velocidad mínima y máxima relativa de los puntos actuales, el significado de cada color cambiaba dinámicamente y carecía de una leyenda de referencia en pantalla.
3. **Mapa Oscuro ("Táctico Oscuro") Bloqueado**:
   - El proveedor `CARTO_DARK` (`basemaps.cartocdn.com/dark_all/`) restringió el acceso sin clave comercial, devolviendo teselas grises con el mensaje `"API KEY REQUIRED — carto.com/basemaps/apikey"`.

---

## 2. Soluciones Implementadas

### 2.1. Escala Fija de Alto Contraste por Velocidad Real (`km/h`)
En [`RouteColorUseCase.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/domain/usecase/RouteColorUseCase.kt) y [`RouteSegmenter.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/RouteSegmenter.kt):
- Se incorporó `speedToTacticalColor(speedMps: Float)` con 4 colores intensos y oscuros que contrastan tanto sobre mapas claros como oscuros:
  - **Rojo intenso (`#D32F2F`)**: `< 2 km/h` (Inicio / Detenido / Paso muy lento)
  - **Verde esmeralda oscuro (`#2E7D32`)**: `2 – 7 km/h` (Caminata)
  - **Azul profundo (`#1565C0`)**: `7 – 20 km/h` (Bicicleta / Urbano)
  - **Violeta intenso (`#7B1FA2`)**: `> 20 km/h` (Vehículo)
- En [`MapOverlayController.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/map/MapOverlayController.kt) y [`ExperienceMapScreen.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/ExperienceMapScreen.kt) se configuró `Paint.Cap.ROUND`, `Paint.Join.ROUND` y `isAntiAlias = true` para un trazo continuo y nítido.

### 2.2. Indicador Dinámico y Leyenda de Velocidades en el Cartel Superior
En [`TopBarLocalizador.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/components/TopBarLocalizador.kt) y [`MapTopBar.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/map/MapTopBar.kt):
- Los indicadores circulares junto a **MODO DE TRÁFICO** y **VELOCIDAD** ahora cambian en tiempo real al color exacto que corresponde a la velocidad actual.
- Se añadió en la parte inferior del cartel superior una barra compacta de referencia con los 4 colores y sus rangos de velocidad aproximada (`<2 km/h`, `2-7 km/h`, `7-20 km/h`, `>20 km/h`).

### 2.3. Reparación Definitiva del Mapa "Táctico Oscuro"
En [`MapTileSourceManager.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/map/MapTileSourceManager.kt), [`MapCanvas.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/map/MapCanvas.kt) y [`ExperienceMapScreen.kt`](file:///g:/Sandbox/BitaVi-Android/app/src/main/java/com/aistudio/localizador/ui/screens/ExperienceMapScreen.kt):
- Se eliminó la dependencia externa de `CARTO_DARK`.
- Se implementó `applyLayerToMapView(mapView, type)` utilizando las teselas oficiales de OpenStreetMap (`TileSourceFactory.MAPNIK`) con el filtro nativo nocturno `TilesOverlay.INVERT_COLORS` y fondo de carga oscuro (`rgb(18, 18, 24)`), funcionando sin API Key y compartiendo caché offline con el mapa estándar.

---

## 3. Archivos Respaldados en `RESPALDO_ANTERIOR/`
- `20261017_1344_RouteColorUseCase.kt`
- `20261017_1344_RouteSegmenter.kt`
- `20261017_1344_MapOverlayController.kt`
- `20261017_1344_TopBarLocalizador.kt`
- `20261017_1344_MapTopBar.kt`
- `20261017_1344_MapTileSourceManager.kt`
- `20261017_1344_MapCanvas.kt`
- `20261017_1344_ExperienceMapScreen.kt`
- `20261017_1344_RouteColorUseCaseTest.kt`

---

## 4. Verificación
- **Limpieza de Caché de Unidad Anterior**: Se eliminaron `app/build`, `build`, `.gradle` y `.kotlin` con rutas de `E:\`.
- **Compilación y Pruebas Unitarias**: `.\gradlew.bat assembleDebug testDebugUnitTest --tests "com.aistudio.localizador.domain.usecase.RouteColorUseCaseTest" --no-build-cache` → `BUILD SUCCESSFUL in 2m 34s` (13 pruebas en `RouteColorUseCaseTest`, 0 fallos).
