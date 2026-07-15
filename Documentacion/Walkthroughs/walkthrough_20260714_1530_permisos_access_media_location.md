# Walkthrough: Fix Permisos ACCESS_MEDIA_LOCATION en Arranque
## Fecha: 2026-07-14 15:30

## Problema
La aplicacion solicitaba el permiso "fotos y videos" (ACCESS_MEDIA_LOCATION) inmediatamente al arrancar en la MapScreen (Pantalla 1).

## Causa Raiz
El LaunchedEffect(Unit) del arranque agrupaba ACCESS_MEDIA_LOCATION junto con ACCESS_FINE_LOCATION y ACCESS_COARSE_LOCATION en el mismo lote, provocando que Android mostrara el dialogo al iniciar la app.

## Solucion Aplicada
1. Se removio ACCESS_MEDIA_LOCATION del LaunchedEffect(Unit) de arranque.
2. Se creo un mediaLocationPermissionLauncher independiente (RequestPermission).
3. Se agrego el estado pendingPhotoUri para retener la URI mientras se espera el permiso.
4. El galleryLauncher ahora verifica el permiso antes de extraer EXIF; si no esta concedido, lo solicita bajo demanda.

## Archivos Modificados
- app/src/main/java/com/example/ui/screens/MapScreen.kt

## Archivos de Respaldo
- RESPALDO_ANTERIOR/20260714_MapScreen_pre_permisos.kt

## Build
- assembleDebug: BUILD SUCCESSFUL (0 errores)
