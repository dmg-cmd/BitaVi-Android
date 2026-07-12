# Walkthrough: Saneamiento Global de Lint — Zero Errors Alcanzado
**Fecha:** 2026/07/12 13:40

## Diagnóstico Inicial
- Lint report: **4 errores, 60 warnings, 12 hints**
- Errores bloqueaban lintDebug (BUILD FAILED)

## Bloque 1: 4 Errores de Permisos
### MissingPermission ×3
- **ActivityRecognitionManager.kt:70**: stopTrackingTransitions() sin @SuppressLint
  - Solución: Añadida anotación @SuppressLint("MissingPermission") (startTrackingTransitions ya lo tenía)
- **ProximityManager.kt:131,139**: stopScan() y stopAdvertising() sin @SuppressLint
  - Solución: Añadidas anotaciones + import android.annotation.SuppressLint

### ForegroundServicePermission ×1
- **AndroidManifest.xml:47**: foregroundServiceType="location|connectedDevice" sin permiso FOREGROUND_SERVICE_CONNECTED_DEVICE
  - Solución: Añadido <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

## Bloque 2: Rendimiento Compose
### ExperienceMapScreen.kt:123 — @FrequentlyChangingValue
- scrollState.value leído directamente en LaunchedEffect → recomposiciones en cada píxel de scroll
- Solución: Migrado a snapshotFlow { scrollState.value }.collect { overlayScrollOffset = it }
- Nuevo import: import androidx.compose.runtime.snapshotFlow

### MapScreen.kt:477 — LocalContextResourcesRead
- context.resources rompe invalidación reactiva de Configuration
- Solución: Capturado al resources = LocalResources.current y usado en lugar de context.resources
- Nuevo import: import androidx.compose.ui.platform.LocalResources

## Bloque 3: Convenciones Compose
### HistoryScreen.kt:40,304 — ModifierParameter
- modifier no era el primer parámetro opcional en HistoryScreen ni RouteCard
- Solución: Reordenados parámetros según convención oficial Jetpack Compose

## Bloque 4: Recursos No Utilizados
- colors.xml: 7 colores eliminados (purple_200/500/700, teal_200/700, black, white)
- strings.xml: 2 strings eliminados (biometric_subtitle, share_footer)
- info_window_layout.xml: Eliminado (legacy de PhotoInfoWindow)

## Verificación Final
- Lint: **0 errores, 47 warnings** (solo versiones/actualizaciones), **12 hints** (autoboxing)
- Compilación: BUILD SUCCESSFUL (25s)