# Walkthrough: Mapa Inmersivo — ExperienceMapScreen
**Fecha:** 2026/07/12 15:10

## Resumen
Migracion del layout de ExperienceMapScreen de Scaffold+TopAppBar a un sistema de capas Box(fillMaxSize) para mapa fullscreen inmersivo.

## Cambios Estructurales
- **Antes**: Scaffold con TopAppBar (barra superior opaca) + paddingValues que recortaban el mapa
- **Despues**: Box(fillMaxSize) con 5 capas en eje Z

## Arbol de Capas
`
Box(fillMaxSize)
├── [Capa 0] AndroidView(MapView) — fullscreen
├── [Capa 1] AnimatedVisibility(panel fotos) — TopStart + statusBarsPadding
├── [Capa 2] Column(FABs) — CenterEnd, primaryContainer/primary
├── [Capa 3] Surface(barra inferior) — BottomCenter + navigationBarsPadding
│   ├── IconButton(ArrowBack)
│   ├── Text("Experiencias")
│   ├── Spacer(weight)
│   ├── IconButton(Share) → viewModel.exportRoute()
│   └── IconButton(Save) → viewModel.exportarRutaZip()
└── [Capa 4] FullScreenPhotoDialog — preservado
`

## Cambios de Tokens
- FABs: Color.White → MaterialTheme.colorScheme.primaryContainer / primary
- Iconos barra inferior: tint = MaterialTheme.colorScheme.primary (violeta/magenta adaptativo)

## Logica Preservada
- Auto-rotacion/simulacion (Explore FAB + bearing + LaunchedEffects)
- ZoomToFit (ZoomOutMap FAB)
- Panel de fotos translucido (AnimatedVisibility)
- Guia de lineas (GuideLinesOverlay)
- Visor fullscreen (FullScreenPhotoDialog)
- Cache de iconos de marcadores

## Verificacion
- Build: BUILD SUCCESSFUL (38s)
- Lint: 0 errores, 47 warnings, 12 hints