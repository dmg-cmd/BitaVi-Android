# Walkthrough: WaypointsHorizontalSelector + Empty State (4.5)
**Fecha:** 2026/07/14 01:33  
**Build:** BUILD SUCCESSFUL (assembleDebug)  
**Deploy:** Pixel 9 Pro XL (installDebug)

## Cambios realizados

### 1. WaypointsHorizontalSelector.kt (NUEVO)
- pp/src/main/java/com/example/ui/screens/WaypointsHorizontalSelector.kt
- Composable reutilizable con dos estados:
  - **Empty**: OutlinedCard con Place icon + "Sin puntos de interés. Añade el primero" + botón "Añadir"
  - **Con datos**: LazyRow horizontal de ElevatedCards (200.dp width, 12.dp spacing)
    - AsyncImage para foto (120.dp height) o Place icon si no hay foto
    - Nombre (bold, 1 línea) + descripción (2 líneas)
- Callbacks: onWaypointClick(Waypoint), onAddWaypointClick()

### 2. DetailScreen.kt (MODIFICADO)
- Reemplazadas líneas 146-185 (bloque if (waypoints.isNotEmpty())) con:
  `kotlin
  WaypointsHorizontalSelector(
      waypoints = waypoints,
      onWaypointClick = { wp -> if (wp.photoPath != null) { photoToZoom = wp.photoPath } },
      onAddWaypointClick = { viewModel.prepareWaypointCreation(routeId) }
  )
  `
- Añadido LaunchedEffect(routeId, waypoints) { Log.d("BitaruDebug", ...) }
- Añadido import android.util.Log
- Restaurado desde git (brace balanceado: 166=166)

### 3. MainActivity.kt (MODIFICADO)
- LaunchedEffect(routeId) { viewModel.selectRouteForDetail(routeId) } en composable DETAIL

### 4. RouteViewModel.kt (MODIFICADO)
- un prepareWaypointCreation(routeId: Int) { _selectedDetailRouteId.value = routeId }

## Verificación
- .\gradlew assembleDebug: BUILD SUCCESSFUL
- .\gradlew installDebug: Installed on 1 device (Pixel 9 Pro XL)
- Archivo compila sin errores (0 errores reportados)
- Logcat tag BitaruDebug mostrará "Puntos de interés: N" al navegar a detalle

## Archivos tocados
| Archivo | Cambio |
|---------|--------|
| WaypointsHorizontalSelector.kt | +NUEVO (113 líneas) |
| DetailScreen.kt | -39 líneas viejas, +15 líneas nuevas |
| MainActivity.kt | +3 líneas (LaunchedEffect) |
| RouteViewModel.kt | +5 líneas (prepareWaypointCreation) |

## Respaldos
- RESPALDO_ANTERIOR/20260714_0133_DetailScreen.kt (pre-cambio)
- RESPALDO_ANTERIOR/20260714_0133_MainActivity.kt (pre-cambio)
- RESPALDO_ANTERIOR/20260714_0133_RouteViewModel.kt (pre-cambio)
- RESPALDO_ANTERIOR/20260714_0133_WaypointsHorizontalSelector.kt (nuevo)
- RESPALDO_ANTERIOR/20260714_0133_*_post.kt (post-cambio)
