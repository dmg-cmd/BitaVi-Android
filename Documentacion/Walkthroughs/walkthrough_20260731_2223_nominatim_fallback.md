# Walkthrough — Fallback Nominatim (OSM) en GeocoderUseCase (CR-23)

**Fecha/hora:** 20260731_2223
**Módulo:** Geocodificación inversa de favoritos (país/ciudad/dirección)
**Build:** app-debug (assembleDebug)

## Objetivo
Verificar que al guardar o actualizar un favorito se obtenga país, ciudad y lugar cercano incluso cuando el Geocoder del sistema falla, mediante fallback a Nominatim (OpenStreetMap).

## Cambios verificados en código

1. **NominatimApi.kt** (nuevo)
   - `NominatimAddressDto` con `getBestCityName()` (city ?: town ?: village ?: suburb).
   - `NominatimResponseDto` (display_name + address).
   - `NominatimApi.reverseGeocode` con format=json, addressdetails=1, accept-language=es → `Call<NominatimResponseDto>`.

2. **RetrofitClient.kt**
   - `NOMINATIM_BASE_URL = https://nominatim.openstreetmap.org/`.
   - `nominatimOkHttpClient` dedicado (timeouts 5s) con interceptor User-Agent "Agenviaje/1.0 (com.aistudio.localizador)".
   - `nominatimApi` lazy; client Groq/Gemini intactos.

3. **GeocoderUseCase.kt**
   - Firma pública preservada: `fetchLocationDetails(lat, lon): Triple<String,String,String>` en orden (address, country, city).
   - Intento A: Geocoder nativo, guarda `Geocoder.isPresent()` solo en SDK >= TIRAMISU; exige país y ciudad válidos.
   - Intento B: fallback Nominatim con `execute()` síncrono (llamadores corren en IO).
   - `fetchAddress` también con fallback (displayName).

## Nota de diseño
El orden del Triple se mantiene como (address, country, city) — el pseudocódigo propuesto invertía el orden y habría roto los 7 llamadores; se corrigió para preservar el contrato.

## Pruebas de compilación
- `gradlew :app:testDebugUnitTest` → 77 tests, 68 OK / 9 fallos preexistentes (ajenos al cambio).
- `gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

## Deploy
- `adb install -r app-debug.apk` → Success.
- Lanzamiento `com.aistudio.localizador.vqtspk/.MainActivity` → OK.

## Resultado
Intervención completa. Prueba manual recomendada: guardar un favorito (o pulsar "Actualizar País/Ciudad" en uno antiguo con "Desconocido") y verificar que se completan país, ciudad y dirección.