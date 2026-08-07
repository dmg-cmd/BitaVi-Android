# PLAN: Dock Flotante Traslucido para ExperienceMapScreen
**Fecha:** 2026/07/12
**Estado:** COMPLETADO

## Decisiones Finales
1. GPS -> Chip TopEnd (independiente del dock) ✅
2. Dock -> Transparente alpha=0.50f, SOLO botones + titulo ✅
3. osmdroid attribution -> Oculto (evita doble barra) ✅

## Arbol de Capas Final
`
Box(fillMaxSize)
+-- [0] AndroidView(MapView) - fullscreen (attribution oculta)
+-- [1] AnimatedVisibility(panel fotos) - TopStart
+-- [1.5] Surface(chip GPS) - TopEnd + statusBarsPadding
|   +-- Row(GpsFixed 12dp + gpsPrecisionState labelSmall)
+-- [2] Column(FABs) - CenterEnd
+-- [3] Surface(dock flotante) - BottomCenter
|   +-- Row(ArrowBack | "Experiencias" | Share + Save)
+-- [4] FullScreenPhotoDialog
`

## Verificacion
- assembleDebug: BUILD SUCCESSFUL
- lintDebug: 0 errors, 47 warnings
- deploy: 2 dispositivos Pixel 9 Pro XL