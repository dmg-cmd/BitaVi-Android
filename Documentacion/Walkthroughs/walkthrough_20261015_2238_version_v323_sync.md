# Walkthrough — Sincronización de versión visible a v3.2.3-RC1

**Fecha de ocurrencia:** 2026-10-15 22:38
**Ámbito:** etiqueta de versión visible en la app + `app/build.gradle.kts`.

---

## 1. Motivo

El usuario veía en pantalla `v3.2.2-RC1`, mientras que `app/build.gradle.kts` declaraba
`versionName = "2.0"`. Eran dos números distintos que no estaban conectados: el visible
era texto fijo (hardcodeado) y el otro, el de instalación.

## 2. Cambios realizados

| Archivo | Antes | Después |
|---|---|---|
| `ui/components/BrandWatermark.kt` | `v3.2.2-RC1` | `v3.2.3-RC1` |
| `ui/components/TopBarLocalizador.kt` | `v3.2.2-RC1` | `v3.2.3-RC1` |
| `app/build.gradle.kts` (versionName) | `2.0` | `3.2.3` |
| `app/build.gradle.kts` (versionCode) | `2` | `3` |

Nota: `BrandWatermark()` se invoca sin parámetro de versión en `MapFloatingControls.kt`,
por lo que basta con cambiar el valor por defecto.

## 3. Verificación

| Comprobación | Resultado |
|---|---|
| `compileDebugKotlin` / `testDebugUnitTest` / `installDebug` | BUILD SUCCESSFUL |
| Suite de tests | 109 tests, 0 fallos |
| Instalación en dispositivo | Pixel 9 Pro XL (2 entradas: cable + inalámbrica) |

## 4. Pendiente relacionado (branding)

- El texto visible aún dice **"BiTaRu"** (marca antigua) en `BrandWatermark.kt`; el
  proyecto es **BitaVi**. Queda para la tarea de branding (P3) del plan.