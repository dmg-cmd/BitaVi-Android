# Walkthrough — Cobertura Mundial WGS84 para Geocodificación (CR-24)

**Fecha/hora:** 20260731_2250
**Módulo:** Geofencing / Geocodificación inversa de favoritos (país/ciudad/dirección)
**Build:** app-debug (assembleDebug)

## Objetivo
Verificar que el país, la ciudad y la dirección de los favoritos se determinen en cualquier parte del mundo (no solo en el Cono Sur), permitiendo que el fallback Nominatim del CR-23 se active fuera del rectángulo regional.

## Síntoma reportado
El usuario en Cusco (Perú, lat ~-13, lon ~-72) reportó que ni los favoritos nuevos ni el botón "Actualizar País/Ciudad" completaban el país/ciudad ("Desconocido"/"Desconocida").

## Diagnóstico
1. **AppConstants.Geofencing**: lat -55..-20, lon -75..-53 (Cono Sur). Cusco queda fuera → `GeocoderUseCase.isValidCoordinate` devolvía false y `fetchLocationDetails` retornaba temprano `Triple("GPS: Fuera de cobertura", "Desconocido", "Desconocida")` sin intentar el Geocoder nativo ni Nominatim.
2. **Infraestructura OK**: API Nominatim responde HTTP 200 (probado con User-Agent "Agenviaje/1.0"): `lat=-34.6037&lon=-58.3816` → country=Argentina, city=Buenos Aires; `lat=-13.1631&lon=-72.5450` (Machu Picchu) → country=Perú, city=Machupicchu.
3. **Adapters Moshi generados**: `NominatimResponseDtoJsonAdapter` y `NominatimAddressDtoJsonAdapter` presentes en `app/build/generated/ksp/debug/kotlin/`.
4. **Dispositivo con red**: WIFI "Tierra Viva Machu Picchu" (Perú) validada con INTERNET. App v2.0 instalada (lastUpdateTime 2026-07-31).

## Cambios verificados en código
1. **AppConstants.kt** (object Geofencing)
   - MIN_LATITUDE/MAX_LATITUDE = -90.0/90.0; MIN_LONGITUDE/MAX_LONGITUDE = -180.0/180.0 (WGS84 global).
   - `isWithinBounds` ahora excluye el punto nulo GPS `(lat == 0.0 && lon == 0.0)`.
2. **GeocoderUseCase.kt**
   - `isValidCoordinate` delega directo en `AppConstants.Geofencing.isWithinBounds(lat, lon)` (se elimina el clamp `lat/lon.toFloat()`).
   - Coordenadas inválidas → `Triple("GPS: Sin señal", "Desconocido", "Desconocida")` (reemplaza "GPS: Fuera de cobertura").
   - Se preservan: firma síncrona `Triple(address, country, city)`, `fetchAddress: String?`, `formatAddress` (presentación de tarjetas de la Pantalla 3).

## Notas de diseño
- **NO suspend**: `BackupUseCase.restoreFromZip` llama `fetchLocationDetails` dentro de un `mapNotNull` (lambda no-suspend) → una firma suspend no compila. Todos los llamadores ya corren en `Dispatchers.IO` (RouteViewModel L432/458/479/487/495, ShortStopMarkManager L58, AddFavoriteFromTrackingUseCase L27, BackupUseCase L130), por lo que `execute()` síncrono es seguro.
- **NO @Inject constructor**: AppModule:35 ya provee `GeocoderUseCase(context)` manualmente; añadirlo crearía binding duplicado de Hilt.
- El contrato de tupla se mantiene (address, country, city); el pseudocódigo del solicitante invertía el orden.

## Pruebas de compilación
- `gradlew :app:testDebugUnitTest` → 77 tests, 68 OK / 9 fallos preexistentes (ajenos al cambio).
- `gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

## Deploy
- `adb -s 48201FDAS001L5 install -r app-debug.apk` → Success.
- Lanzamiento `com.aistudio.localizador.vqtspk/.MainActivity` → OK (sin FATAL en logcat).

## Verificación funcional remota
- Nominatim para Machu Picchu (Cusco): HTTP 200, country=Perú, city=Machupicchu, display_name="Templo de las Tres Ventanas, ..., Machupicchu, Urubamba, Cusco, 08680, Perú". El fallback ya no queda bloqueado por el geofencing.

## Resultado
Intervención completa. Prueba manual recomendada: con GPS fijo en Cusco, guardar un favorito nuevo y pulsar "Actualizar País/Ciudad" sobre uno antiguo con "Desconocido" → debe mostrarse Perú / Machupicchu.