# Walkthrough — Limpieza integral de la marca "Bitaru" -> "BitaVi"

**Fecha de ocurrencia:** 2026-10-15 23:00
**Ámbito:** `app/src/main` (textos, etiquetas de log, identificadores y recursos).

---

## 1. Motivo

La app aún mostraba y usaba la marca antigua "BiTaRu/Bitaru" en varios textos y en
nombres internos. Se pidió eliminar todas las referencias restantes.

## 2. Cambios realizados

### Textos visibles al usuario
- `BrandWatermark.kt`: `"BiTaRu — $version"` -> `"BitaVi — $version"`.
- `BitaViBrandHeader.kt` (antes `BitaruBrandHeader.kt`): `contentDescription` "Logo Corporativo Bitaru" -> "Logo Corporativo BitaVi".
- `ShareExperienceUseCase.kt`: "Recorrido Bitaru" -> "Recorrido BitaVi"; "Experiencia Bitaru Localizador" -> "Experiencia BitaVi"; "Compartir Experiencia Bitaru" -> "Compartir Experiencia BitaVi".
- `ExperienceMapScreen.kt`: "Experiencia Bitaru: ..." -> "Experiencia BitaVi: ...".

### Etiquetas de log (TAG)
- `Bitaru_*` -> `BitaVi_*` (Tracking, EXIF, Purge, AutoTrack, Ble, ShortStop, Core, Contact).
- `BitaruDebug` -> `BitaViDebug`; `Log.e("Bitaru", ...)` -> `Log.e("BitaVi", ...)`.

### Nombres internos y recursos
- Función `BitaruBrandHeader` -> `BitaViBrandHeader` (+ archivo `BitaViBrandHeader.kt`).
- Constante `AZUL_BITARU` -> `AZUL_BITAVI` (`RouteColorUseCase.kt`).
- Recurso `drawable/bitaru_logo.png` -> `drawable/bitavi_logo.png`.

Total: **16 archivos** modificados + 2 renombres.

## 3. Verificación

| Comprobación | Resultado |
|---|---|
| Búsqueda `Bitaru|BiTaRu|bitaru` en `app/src/main` | **0 restos** |
| `testDebugUnitTest` | 109 tests, 0 fallos |
| `installDebug` | BUILD SUCCESSFUL (Pixel 9 Pro XL) |

## 4. Pendiente (marca "Localizador")

Quedan identificadores con la palabra *Localizador* (no es la marca, es el nombre
funcional / del paquete):
- `Theme.Localizador` (`res/values/themes.xml`, referenciado en `AndroidManifest.xml`).
- Función `LocalizadorTheme` (`ui/theme/Theme.kt`, usado en `MainActivity` y 4 tests).
- Función `TopBarLocalizador` (`ui/components/TopBarLocalizador.kt`, usado en `MapTopBar`).
- El paquete `com.aistudio.localizador` **no debe renombrarse** (identidad de la app).