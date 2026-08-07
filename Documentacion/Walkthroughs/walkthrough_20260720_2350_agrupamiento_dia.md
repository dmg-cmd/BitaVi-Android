# Walkthrough: CR-10 — Agrupamiento por Día en HistoryScreen
**Fecha:** 2026-07-20 23:50  
**Tema:** GroupRoutesByDateUseCase + DayGroupHeader + stripeColor en RouteCard

## Cambios Realizados

### 1. GroupRoutesByDateUseCase.kt (NUEVO)
- DTO `DayGroupUiModel(LocalDate, formattedDate, colorInt: Int, routes: List<Route>)`
- `colorInt` es `Int` ARGB (no `Color` de Compose) para testabilidad JVM pura
- Paleta de 7 colores: #1E88E5, #43A047, #FB8C00, #8E24AA, #00ACC1, #D81B60, #3949AB
- Agrupación: `routes.groupBy { startTime → LocalDate }` + `toSortedMap(Comparator.reverseOrder())`
- Ruteo intra-día: `dayRoutes.sortedByDescending { it.startTime }`
- Sin dependencias Android, JVM puro

### 2. RouteViewModel.kt
- Inyectado `GroupRoutesByDateUseCase` como parámetro del constructor (vía Hilt)
- Nuevo `allRoutesForGrouping` derivado de `repository.allRoutes`
- Nuevo `groupedRoutesState: StateFlow<List<DayGroupUiModel>>` con `stateIn(viewModelScope, WhileSubscribed(5000), emptyList())`

### 3. HistoryScreen.kt
- `val groupedRoutes by viewModel.groupedRoutesState.collectAsState()` agregado
- `var collapsedDates by remember { mutableStateOf(setOf<LocalDate>()) }` para estado colapsable
- LazyColumn itera sobre `groupedRoutes` en lugar de `sortedRoutes`
- `DayGroupHeader`: Row con círculo de color (12dp, CircleShape), fecha formateada, ExpandLess/ExpandMore
- `AnimatedVisibility` envuelve el grupo de rutas (expandVertically/shrinkVertically)
- `RouteCard`: nuevo parámetro `stripeColor: Color = Color.Transparent`
- Stripe: `Box(6dp width, fillMaxHeight, clip RoundedCornerShape(topStart/bottomStart 16dp), background stripeColor)`

### 4. GroupRoutesByDateUseCaseTest.kt (NUEVO)
- 3 tests JVM:
  - `invoke debe agrupar rutas por dia descendente` (2 días, orden correcto)
  - `invoke dos rutas mismo dia deben compartir grupo y color` (determinismo)
  - `invoke con lista vacia debe retornar lista vacia`

## Resultado de Compilación
- `compileDebugKotlin`: OK
- `testDebugUnitTest`: 67 tests, 9 fallos pre-existentes (RouteExportUseCase, ActivityModeManager, ShortStopMarkManager, ContactUseCase)
- `assembleDebug`: BUILD SUCCESSFUL

## Archivos Afectados
- **NUEVOS:** domain/usecase/GroupRoutesByDateUseCase.kt, test/.../GroupRoutesByDateUseCaseTest.kt
- **MODIFICADOS:** RouteViewModel.kt, HistoryScreen.kt
- **BACKUPS:** RESPALDO_ANTERIOR/20260720_*.kt

## Principios Aplicados
- Regla I (Modularidad): UseCase dedicado sin acoplar al ViewModel
- Regla II (Testabilidad): 3 tests JVM puros
- Regla IV (UX): Cabeceras colapsables con animación y banda cromática por día
- Paleta ARGB primitiva: Int en lugar de Color Compose para evitar dependencia Android