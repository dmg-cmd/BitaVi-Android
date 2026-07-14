# Walkthrough: Dock Flotante Traslucido + GPS Telemetria
**Fecha:** 2026/07/12 23:30
**Tema:** Implementacion dock flotante 50% transparencia con indicador GPS integrado

## Archivos Modificados
1. **RouteViewModel.kt** (RESPALDO_ANTERIOR/20260712_RouteViewModel.kt)
   - Nuevo: val gpsAccuracy: StateFlow<String> derivado de LocationService.latestLocation
   - Mapa: .map { location -> categorias por rango de accuracy }
   - StateIn: SharingStarted.WhileSubscribed(5000)

2. **ExperienceMapScreen.kt** (RESPALDO_ANTERIOR/20260712_ExperienceMapScreen.kt)
   - CAPA 3 reemplazada: Surface opaca (alpha=0.96, shapes.medium) -> Surface traslucida (alpha=0.50, shapes.large)
   - Nuevo: Icono GpsFixed + texto gpsPrecisionState en dock
   - Nuevo import: GpsFixed, collectAsStateWithLifecycle

## Arbol de Capas Post-Implementacion
`
Box(fillMaxSize)
+-- [0] AndroidView(MapView) - fullscreen
+-- [1] AnimatedVisibility(panel fotos) - TopStart + statusBarsPadding
+-- [2] Column(FABs) - CenterEnd, primaryContainer/primary
+-- [3] Surface(dock flotante) - BottomCenter + navigationBarsPadding
|   +-- IconButton(ArrowBack)
|   +-- Column(weight=1f)
|   |   +-- Text("Experiencias", titleMedium, Bold)
|   |   +-- Row(GpsFixed 14dp + labelSmall gpsPrecisionState)
|   +-- Row(Share + Save)
+-- [4] FullScreenPhotoDialog - preservado
`

## Verificacion
- assembleDebug: BUILD SUCCESSFUL (2m 6s)
- lintDebug: 0 errors, 47 warnings (sin regresion)