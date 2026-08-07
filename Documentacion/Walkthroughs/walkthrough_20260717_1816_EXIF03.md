# Walkthrough EXIF-03: Corrección Secuencia Stream — Extraer GPS antes de Clonar

**Fecha:** 20260717_1816

## Problema
La implementación EXIF-02 extraía coordenadas GPS del archivo local clonado (internalUri), pero este ya contenía el stream anonimizado por Android (Scoped Storage), resultando en null.

## Causa Raíz
saveImageToInternalStorage() consumía el canal IPC del content:// URI original hasta EOF. Luego, al leer el archivo local clonado, este carecía de metadatos GPS porque el stream original ya venía anonimizado y la copia era byte-a-byte del stream ya mutilado.

## Solución
Invertir el orden de operaciones en los 3 pickers:
1. Extraer coordenadas GPS del URI original de la galería (con setRequireOriginal)
2. Luego clonar la imagen al almacenamiento interno

## Archivos Modificados
- ExtractPhotoLocationUseCase.kt: parámetro galleryUri, return@withContext explícito
- SavedLocationsScreen.kt: orden invertido en los 3 bloques picker
- ExtractPhotoLocationUseCaseTest.kt: mockkStatic(MediaStore::class)

## Verificación
- assembleDebug: BUILD SUCCESSFUL
- testDebugUnitTest: 16/16 passing
