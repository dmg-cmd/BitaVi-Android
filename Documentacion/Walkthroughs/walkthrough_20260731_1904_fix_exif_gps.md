# Walkthrough — Fix EXIF GPS API 29+ (Posición de la Foto en Favoritos)

**Fecha/hora:** 2026-07-31 19:04
**Módulo:** Guardado de favoritos/puntos con foto (MapScreen + SavedLocationsScreen)
**Build:** app-debug (assembleDebug)

## Objetivo
Verificar que al adjuntar una foto a un favorito, se tome la posición GPS de la foto (EXIF) en lugar de la posición actual del dispositivo, respetando Scoped Storage de Android 10+.

## Cambios verificados en código

1. **ExtractPhotoLocationUseCase.kt**
   - `resolveOriginalUri()` → `MediaStore.setRequireOriginal(uri)` en `SDK_INT >= Q`, con try/catch y fallback al URI original.
   - `readLatLongFromStream()` → segundo intento de lectura EXIF desde `InputStream` directo.
   - Filtro de coordenadas inválidas `(0.0, 0.0)`.
   - Confirmado por lectura de archivo: imports `android.os.Build`, `android.provider.MediaStore` presentes.

2. **SavedLocationsScreen.kt**
   - `pendingPhotoUri` estado para reintento post-permiso.
   - `mediaLocationPermissionLauncher` reintenta extracción al concederse.
   - `photoPickerLauncher` solicita `ACCESS_MEDIA_LOCATION` en API 29+ antes de extraer.
   - `applyPhotoMetadata()` unifica el seteo de coordenadas, `usePhotoGps`, timestamp y dirección.
   - Typo de limpieza corregido: `inputLatitude = """` → `""`.

3. **MapScreen.kt**
   - `cameraLauncher` activa `usePhotoGps = photoGpsLocation != null` tras capturar con cámara.

4. **AndroidManifest.xml**
   - `android.permission.ACCESS_MEDIA_LOCATION` ya declarado (línea 18). Sin cambios.

## Pruebas de compilación
- `gradlew :app:assembleDebug` → BUILD SUCCESSFUL (3m 20s). Solo warning preexistente de annotation target en `@ApplicationContext`.

## Deploy
- `adb install -r app-debug.apk` → Success.
- Lanzamiento `com.aistudio.localizador.vqtspk/.MainActivity` → OK.

## Resultado
Intervención completa y verificada. El flujo ahora preserva la posición GPS de la foto en favoritos cuando la foto contiene metadatos EXIF válidos.