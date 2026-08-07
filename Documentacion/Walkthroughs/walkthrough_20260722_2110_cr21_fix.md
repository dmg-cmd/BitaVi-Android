# Walkthrough - Implementacion CR-21 Fix (Mapeo Hibrido de URIs y Sincronizacion Directa)

## Fecha: 2026-07-22 21:10

## Objetivo
Corregir el fallo en el flujo de compartir de Pantalla 5 (ExperienceMapScreen) donde las fotos nunca se adjuntaban al Intent, enviando solo texto plano.

## Diagnostico de Causa Raiz
1. PhotoManager.saveImageToInternalStorage() retorna FileProvider.getUriForFile() → URI content://com.aistudio.localizador.fileprovider/images/IMG_GALLERY_*.jpg
2. capturedPhotoUri?.toString() persiste esa URI como Waypoint.photoPath en Room
3. ShareExperienceUseCase ejecuta File(path).exists() donde path = "content://com.aistudio.localizador.fileprovider/images/..."
4. File("content://...").exists() siempre retorna alse porque content:// no es una ruta POSIX valida
5. contentUris queda vacio → cae en 	ext/plain → solo se envia nombre y distancia

## Cambios Realizados

### 1. ShareExperienceUseCase.kt — Mapeo Hibrido
- Nuevo when discriminador de 2 casos:
  - **Caso A** (content:// o ile://): Uri.parse(path) directo con unCatching
  - **Caso B** (ruta local fisica): File(path).exists() + FileProvider.getUriForFile (flujo original)
- Telemetria actualizada: "Ruta:" en lugar de "Nombre:"

### 2. RouteViewModel.kt — Sincronizacion Directa
- Eliminada guardia temporal withTimeoutOrNull(3000L) + photoPathsForCurrentRoute.first {}
- Reemplazada por consulta directa: epository.getWaypointsSync(routeId)
- Eliminados imports obsoletos: kotlinx.coroutines.flow.first, kotlinx.coroutines.withTimeoutOrNull

## Verificacion
- compileDebugKotlin: OK
- assembleDebug: BUILD SUCCESSFUL
- installDebug: INSTALADO en 2 dispositivos (Pixel 9 Pro XL)

## Archivos Respaldados
- RESPALDO_ANTERIOR/20260722_2110_ShareExperienceUseCase.kt
- RESPALDO_ANTERIOR/20260722_2110_RouteViewModel.kt