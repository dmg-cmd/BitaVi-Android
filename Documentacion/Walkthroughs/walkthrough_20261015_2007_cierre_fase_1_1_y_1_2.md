# Walkthrough — Cierre FASE 1.1/1.2 (Modularización de Pantallas) y Reparación de la Suite de Pruebas

**Fecha de ocurrencia:** 2026-10-15 20:07
**Autor:** Agente de desarrollo (continuación de la sesión interrumpida del 2026-10-14)
**Ámbito:** `MapScreen.kt`, `SavedLocationsScreen.kt`, componentes extraídos, suite de tests unitarios y cachés de Gradle.

---

## 1. Contexto: por qué se retomó esta tarea

La sesión del 2026-10-14 se interrumpió (cuelgue) mientras se ejecutaba la **FASE 1.1 y 1.2 del `docs/PLAN_MAESTRO_CONSOLIDADO.md`**: la descomposición de las dos pantallas más grandes del proyecto.

Al reabrir el proyecto se verificó que:

- El refactor de código **sí estaba prácticamente completo** y compilaba (`compileDebugKotlin` → `BUILD SUCCESSFUL`).
- Pero **no se había verificado la suite de tests, ni documentado, ni desplegado**. Todo estaba sin commitear.

## 2. Diagnóstico inicial (estado real encontrado)

| Elemento | Estado encontrado |
|---|---|
| `MapScreen.kt` | Refactorizado: 1173 → **243 líneas**. Delega en `MapCanvas`, `MapTopBar`, `MapFloatingControls`, `MapOverlayController` y diálogos. |
| `SavedLocationsScreen.kt` | Refactorizado: 1055 → **153 líneas**. Delega en `SavedLocationsList`, `LocationCard`, `SaveLocationDialog`, `EditLocationDialog`. |
| Compilación (`compileDebugKotlin`) | `BUILD SUCCESSFUL`. |
| Carpeta `ui/screens/map/overlays/` | **Vacía** (artefacto del plan original; no se pobló). |
| Tests de los componentes nuevos | **No existían**. |
| Suite de tests unitarios | **No se podía ejecutar** (ver punto 3). |

## 3. Causa raíz encontrada: cachés de Gradle con rutas absolutas de otra ubicación

Al intentar correr los tests, la tarea `kspDebugUnitTestKotlin` fallaba:

```
e: [ksp] java.lang.IllegalArgumentException: this and base files have different roots:
D:\Google IA Studio\localizador\app\src\test\java\com\aistudio\localizador\location\manager\AutoTrackingManagerTest.kt
and E:\Sandbox\BitaVi-Android\app.
```

**Explicación:** el proyecto se desarrolló originalmente en `D:\Google IA Studio\localizador` y luego se copió a `E:\Sandbox\BitaVi-Android`. La carpeta `app/build/` y la carpeta `.gradle/` (caché de configuración y de ejecución) se arrastraron con rutas absolutas de `D:\`. KSP intentaba calcular una ruta relativa entre la raíz nueva (`E:\`) y un archivo registrado en la caché vieja (`D:\`), lo que lanzaba la excepción.

**Evidencia recogida:** las cadenas `Google IA Studio` aparecían en `app/build/intermediates/...` (lint models, manifest-merger-blame) y en `.gradle/configuration-cache/.../entry.bin` y `.gradle/9.5.0/executionHistory/executionHistory.bin`.

**Solución aplicada:** se detuvieron los daemons de Gradle y se eliminaron únicamente los **artefactos generados** `app\build` y `.gradle` (no es código fuente). La reconstrucción posterior se ejecutó con `--no-build-cache` para impedir que se restauraran salidas viejas. Tras esto, KSP y toda la suite funcionaron con normalidad.

> Verificación final: un barrido posterior confirmó que **no queda ninguna ruta `Google IA Studio`** dentro del nuevo `app\build`.

## 4. Tests de regresión añadidos (FASE 1.1/1.2)

Se crearon 5 tests (Robolectric + Compose UI Test, siguiendo el patrón ya existente en `GreetingScreenshotTest`):

| Archivo nuevo | Test | Qué valida |
|---|---|---|
| `ui/screens/map/IndicadorTranslucidoContenedorTest.kt` | `renderizaElContenidoProvisto` | El contenedor translúcido reutilizable renderiza su contenido. |
| `ui/screens/saved/LocationCardTest.kt` | `muestraNombreAutorYDireccion` | La tarjeta de favorito muestra nombre, autor y dirección. |
| `ui/screens/saved/LocationCardTest.kt` | `muestraComentarioCuandoExiste` | La tarjeta muestra el comentario cuando está presente. |
| `ui/screens/saved/SavedLocationsListTest.kt` | `muestraPaisCiudadYNombreDelLugar` | La lista agrupada renderiza país, ciudad y lugar. |
| `ui/screens/saved/SavedLocationsListTest.kt` | `colapsarPaisOcultaSusLugares` | Al tocar el encabezado del país, sus lugares se ocultan. |

**Resultado:** 5/5 en verde.

## 5. Corrección de 3 tests preexistentes que estaban en rojo

Al correr la suite completa (109 tests), aparecieron 3 fallos que **no pertenecen a esta tarea** pero que se repararon para dejar la suite 100% verde:

1. y 2. **`DepartureWatcherManagerTest`** (`when coordinates valid...` y `stopWatching...`): el test es JVM puro y no simulaba `Intent` ni `PendingIntent`. En el JAR de pruebas de Android, `PendingIntent.getBroadcast()` lanza `Method not mocked`, el `try/catch` del manager lo absorbía y `isCurrentlyWatching()` quedaba en `false`. **Fix:** se añadieron `mockkConstructor(Intent::class)` y `mockkStatic(PendingIntent::class)` en `setUp()`.
3. **`ExampleRobolectricTest.read string from context`**: esperaba el string antiguo `"BiTaRu"`, pero el branding ya es `"BiTaVi"` (ver `res/values/strings.xml`). **Fix:** actualizado el valor esperado a `"BiTaVi"`.

Los tres archivos de test se respaldaron previamente en `RESPALDO_ANTERIOR/` con prefijo `20261015_`.

## 6. Verificación final

| Comprobación | Resultado |
|---|---|
| `compileDebugKotlin` | `BUILD SUCCESSFUL` |
| Suite completa `testDebugUnitTest` | **109 tests, 0 fallos, 0 errores, 0 skipped** |
| Tests nuevos de pantallas | 5/5 en verde |
| Rutas `D:\Google IA Studio` en `app/build` | Ninguna |
| `installDebug` (deploy) | `BUILD SUCCESSFUL` — instalado en 2 dispositivos (Pixel 9 Pro XL) |

## 7. Archivos tocados

**Test (nuevos):**
- `app/src/test/java/com/aistudio/localizador/ui/screens/map/IndicadorTranslucidoContenedorTest.kt`
- `app/src/test/java/com/aistudio/localizador/ui/screens/saved/LocationCardTest.kt`
- `app/src/test/java/com/aistudio/localizador/ui/screens/saved/SavedLocationsListTest.kt`

**Test (corregidos):**
- `app/src/test/java/com/aistudio/localizador/location/manager/DepartureWatcherManagerTest.kt`
- `app/src/test/java/com/aistudio/localizador/ExampleRobolectricTest.kt`

**Configuración de agentes (regla de idioma reforzada):**
- `AGENTS.md`
- `.aierules`

**Artefactos generados eliminados (no son código):**
- `app/build/` y `.gradle/` (se regeneran solos).

## 8. Pendientes y recomendaciones

- La carpeta `ui/screens/map/overlays/` quedó **vacía**. Por la regla del proyecto de no borrar archivos sin permiso explícito, **no se eliminó**; se sugiere borrarla con autorización o poblarla si se retoma el diseño original del plan.
- Los cambios siguen **sin commitear**. Se recomienda un commit con, por ejemplo: `test: cierre FASE 1.1/1.2 pantallas + reparacion suite y caches`.
- Aún queda la **FASE 1.3** del Plan Maestro (`LocationService` + WakeLock 25h), de riesgo **crítico**, pendiente de autorización.
- Los mensajes `w: ... createComposeRule ... is deprecated` son advertencias de la librería Compose Test, consistentes con el resto del proyecto (no son errores).