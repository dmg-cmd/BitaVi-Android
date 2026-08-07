# Walkthrough — Consolidación de Fecha EXIF en Favoritos

**Fecha/hora:** 20260731_2114
**Módulo:** Guardado de favoritos con foto (RouteViewModel + MapScreen + SavedLocationsScreen)
**Build:** app-debug (assembleDebug)

## Objetivo
Verificar que la fecha de captura de la foto (EXIF) se preserve y muestre en los favoritos: en el comentario ("📸 Capturada el dd/MM/yyyy HH:mm") y como línea visible en la tarjeta.

## Cambios verificados en código

1. **RouteViewModel.kt — saveSpecificLocation**
   - `finalTimestamp = photoTimestamp ?: System.currentTimeMillis()`.
   - `finalComment` con `when`: añade fecha de captura sin duplicar el prefijo en reediciones.
   - `timestamp = finalTimestamp` en `repository.addSavedLocation`.
   - Confirmado por lectura de archivo (líneas 431-448).

2. **MapScreen.kt**
   - Estado `photoTimestamp` declarado (L104).
   - Captura de `metadata?.timestamp` en cámara (L117), reintento de permiso (L132) y galería (L153).
   - Paso `photoTimestamp = photoTimestamp` en `saveSpecificLocation` del diálogo de guardar (L1069).
   - Limpieza de `photoTimestamp = null` al Guardar (L1082) y Cancelar (L1090).

3. **SavedLocationsScreen.kt — LocationCard**
   - Fila M3 con `Icons.Default.Schedule` (14dp) y fecha `dd/MM/yyyy HH:mm` en `onSurfaceVariant` (L842-858).
   - Imports `java.text.SimpleDateFormat` y `java.util.Date` (L50-51).

## Pruebas de compilación
- `gradlew :app:testDebugUnitTest` → 77 tests, 68 OK / 9 fallos preexistentes (RouteExportUseCaseTest, ActivityModeManagerTest, ShortStopMarkManagerTest, ContactUseCaseTest) ajenos al cambio.
- `gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

## Deploy
- `adb install -r app-debug.apk` → Success.
- Lanzamiento `com.aistudio.localizador.vqtspk/.MainActivity` → OK.

## Resultado
Intervención completa. La fecha de captura EXIF ahora viaja íntegra al almacenamiento y es visible en la tarjeta de favoritos y en el comentario.