---
name: version-sync-guardian
description: Agente especializado en incrementar y mantener sincronizada automáticamente la versión de BitaVi en cada modificación de código desplegada (TopBarLocalizador.kt, BrandWatermark.kt y app/build.gradle.kts).
---

# 🏷️ Agente y Skill: Guardián de Versionado y Sincronización (version-sync-guardian)

Este agente garantiza que **toda modificación funcional o visual en el código de la aplicación** incremente automáticamente el número de versión antes de compilar y desplegar en el dispositivo, manteniendo sincronizados los tres puntos de verdad del proyecto.

## 🎯 Regla de Disparo Automático (Obligatoria)
Cada vez que se apliquen modificaciones en el código fuente (`app/src/main/...`) para ser compiladas o desplegadas en el dispositivo físico, se DEBE incrementar la versión sin necesidad de que el usuario lo solicite manualmente.

## 🔗 Los 3 Puntos de Sincronización Obligatoria

1. **`app/build.gradle.kts`**:
   - Incrementar `versionCode` en `+1` (ej.: `4` → `5`).
   - Incrementar `versionName` en versión semántica `"X.Y.Z"` (ej.: `"3.2.4"` → `"3.2.5"`).
2. **`app/src/main/java/com/aistudio/localizador/ui/components/TopBarLocalizador.kt`**:
   - Actualizar la etiqueta visible en el cartel superior:
     `"Infraestructura Suite — vX.Y.Z-RC1"` (ej.: `"Infraestructura Suite — v3.2.5-RC1"`).
3. **`app/src/main/java/com/aistudio/localizador/ui/components/BrandWatermark.kt`**:
   - Actualizar el valor por defecto del parámetro `version`:
     `version: String = "vX.Y.Z-RC1"` (ej.: `"v3.2.5-RC1"`).

## 📋 Criterio de Incremento Semántico
- **Parche (`Z + 1`)**: Correcciones de errores, ajustes visuales, mejoras incrementales o calibración de parámetros (ej. `3.2.4` → `3.2.5`).
- **Menor (`Y + 1, Z = 0`)**: Nuevas pantallas completas, nuevos módulos arquitectónicos o fases mayores del Plan Maestro (ej. `3.2.5` → `3.3.0`).
- **Mayor (`X + 1, Y = 0, Z = 0`)**: Reestructuración total o cambio generacional de la aplicación.

## ✅ Verificación Pre-Deploy
Antes de ejecutar `assembleDebug` o `installDebug`, verificar que los tres archivos coincidan exactamente en el número `X.Y.Z` y mencionar la nueva versión desplegada en el resumen al usuario.
