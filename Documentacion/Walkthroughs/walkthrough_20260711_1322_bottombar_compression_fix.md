# Walkthrough 20260711_1322 — Corrección Estructural BottomBar (Efecto Compresión)

## Objetivo
Eliminar el colapso horizontal de los botones de navegación en la barra inferior (Pixel 9 Pro XL).

## Causa Raíz
Mezcla de NavigationBarItem + IconButton + Row (con GPS capsule) como hijos directos de NavigationBar MD3. El motor de layout no puede distribuir proporcionalmente el ancho cuando hay tipos de hijos heterogéneos.

## Cambio Realizado
Reemplazo completo del bloque bottomBar (MainActivity.kt:774-969) por arquitectura de 3 sub-capas:

### Sub-capa Alfa (Tracking)
- Variable reactiva: iewModel.isTrackingLive.collectAsState()
- Surface verde translúcido con texto "Servicio activo"
- Solo visible durante grabación activa

### Sub-capa Beta (Fila Flotante Contextual)
- **Izquierda**: Botones contextuales para DETAIL (Explore, Bookmark, Edit, RouteOptionsMenu) y EXPERIENCE_MAP (Share, Save ZIP)
- **Derecha**: Cápsula GPS con código de colores (verde accuracy <10m, rojo ≥10m)
- Aislada del NavigationBar para no interferir con el layout MD3

### Sub-capa Gamma (NavigationBar MD3)
- 4 NavigationBarItems puros (sin Row, sin IconButton sueltos)
- Navegación con popUpTo(graph.startDestinationId) + launchSingleTop + restoreState
- showHelpDialog (variable correcta, no showHelpBottomSheet)
- Sin restricciones de ancho fijo (eliminado .width(60.dp))

## Archivos Modificados
- pp/src/main/java/com/example/MainActivity.kt (bottomBar reemplazado)
- Limpieza de import: 
avigationBarsPadding (no utilizado)

## Archivos No Tocados
- NavHost / NavController
- ViewModel / RouteRepository
- Room / SQLCipher
- Biometría / FragmentActivity
- Drawer / MapScreen / Demás screens

## Resultado Compilación
BUILD SUCCESSFUL (0 errores, 0 warnings nuevos)

## Verificación Visual
- [ ] Mapa: botón centrado, menú lateral abre al hacer clic
- [ ] Favoritos: navega correctamente
- [ ] Historial: navega correctamente
- [ ] Ayuda: diálogo global aparece
- [ ] GPS capsule: muestra precisión dinámica en fila flotante
- [ ] Tracking bar: visible durante grabación
- [ ] DETAIL: botones contextuales visibles (Explore, Bookmark, Edit, ⋮)
- [ ] EXPERIENCE_MAP: botones Share y Save ZIP visibles
- [ ] Sin amontonamiento: 4 botones equitativos en el NavigationBar
