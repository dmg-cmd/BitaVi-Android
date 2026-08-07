# Walkthrough EXIF-02: Refactor ExtractPhotoLocationUseCase + Corrección Stream IPC

**Fecha:** 20260717_1756

## Problema Detectado
ExtractPhotoLocationUseCase devolvía 
ull incluso cuando las fotos tenían coordenadas GPS reales.

## Causa Raíz
Colisión por consumo destructivo del stream IPC. saveImageToInternalStorage(uri) consumía el InputStream del content:// URI original hasta EOF, cerrando el canal. Luego xtractLocationFromPhoto(uri) intentaba leer el mismo URI ya agotado, obteniendo 0 bytes y ExifInterface retornaba 
ull silenciosamente.

## Cambios Realizados

### 1. ExtractPhotoLocationUseCase.kt (reescrito)
- Inyecta @ApplicationContext en lugar de PhotoManager
- Usa ContentResolver.openInputStream() + ExifInterface directamente
- Guardia de API Build.VERSION.SDK_INT >= Build.VERSION_CODES.R para MediaStore.setRequireOriginal() (API 30+, no 29)
- Logging granular con tag Bitaru_EXIF para cada tipo de fallo
- Captura específica: SecurityException, IllegalArgumentException, IOException, Exception

### 2. AppModule.kt
- Provider de ExtractPhotoLocationUseCase ahora inyecta @ApplicationContext context: Context

### 3. RouteViewModel.kt
- xtractLocationFromPhoto() adaptado para convertir LocationResult? → Pair<Double, Double>?

### 4. SavedLocationsScreen.kt (bugfix)
- Las 3 llamadas a xtractLocationFromPhoto() ahora usan internalUri (URI local clonado) en lugar del URI original del picker
- Se agregó null-safety check if (internalUri != null) antes de extraer coordenadas

## Archivos Modificados
- pp/src/main/java/.../domain/usecase/ExtractPhotoLocationUseCase.kt
- pp/src/main/java/.../di/AppModule.kt
- pp/src/main/java/.../ui/RouteViewModel.kt
- pp/src/main/java/.../ui/screens/SavedLocationsScreen.kt
- pp/src/test/java/.../domain/usecase/ExtractPhotoLocationUseCaseTest.kt

## Verificación
- ssembleDebug: BUILD SUCCESSFUL
- 	estDebugUnitTest: 16/16 passing
