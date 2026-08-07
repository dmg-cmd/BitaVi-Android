# Walkthrough CR-16 (CORREGIDO) — 20260722 _0147

## Objetivo
Eliminar barra flotante 5.3 redundante y unificar acciones en sub-barra inferior 5.2 con BackHandler.

## Archivos modificados
- ExperienceMapScreen.kt — CAPA 3 eliminada, nueva sub-barra 5.2, BackHandler añadido
- ESQUEMA_PANTALLAS.md — Sección 5 actualizada

## Archivos respaldados
- RESPALDO_ANTERIOR/20260722_ExperienceMapScreen_CR16.kt

## Verificación
### Compilación
compileDebugKotlin: BUILD SUCCESSFUL

### Tests
	estDebugUnitTest: 75 tests, 9 failures (todos pre-existentes, 0 nuevos)

### APK
ssembleDebug: BUILD SUCCESSFUL
installDebug: Instalado en 2 dispositivos

## Nueva barra 5.2
| ID | Icono | Color | Acción |
|----|-------|-------|--------|
| 5.2.0 | ← ArrowBack | onSurface | onBack |
| 5.2.1 | 📤 Share | 0xFF4DD0E1 (Cian) | shareExperience() via ShareExperienceUseCase |
| 5.2.2 | 💾 Save/Export | 0xFFFFB74D (Amber) | exportRoute() via RouteExportUseCase |
| 5.2.3 | 🟢 GPS | onSurfaceVariant | gpsPrecisionState |

## Estado final
- Barra 5.3 completamente eliminada.
- Barra 5.2 unificada con todas las acciones.
- Firma preservada.
