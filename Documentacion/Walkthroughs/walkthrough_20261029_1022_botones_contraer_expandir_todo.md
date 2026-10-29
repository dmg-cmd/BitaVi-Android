# Informe de Verificación: Botones Contraer y Expandir Todo en Favoritos y Rutas (v3.2.14-RC1)

**Fecha:** 2026-10-29 10:22  
**Versión:** `v3.2.14-RC1` (`versionCode = 14`, `versionName = "3.2.14"`)  
**Especialistas involucrados:** `compose-ui-tactical`, `version-sync-guardian`, `gradle-compiler-qa`, `doc-custodian`, `scope-boundary-auditor`

---

## 1. Resumen Ejecutivo
Se implementó la funcionalidad solicitada por el usuario para disponer de botones de **Contraer todo** y **Expandir todo** de forma unificada e intuitiva tanto en la pantalla de **Favoritos y Destacados** (`SavedLocationsScreen`) como en la de **Mis Recorridos** (`HistoryScreen`).

Ambas pantallas cuentan ahora con controles ergonómicos en sus cabeceras mediante los iconos estándar de Material 3 (`Icons.Default.UnfoldLess` para contraer y `Icons.Default.UnfoldMore` para expandir), ofreciendo accesibilidad visual uniforme y gestión centralizada en `RouteViewModel`.

---

## 2. Componentes y Arquitectura Modificada

### A. Gestión de Estado en ViewModel (`RouteViewModel.kt`)
- Se agregaron los estados reactivos `_collapsedFavoriteCountries` y `_collapsedFavoriteCities` expuestos como `StateFlow<Set<String>>`.
- Métodos agregados:
  - `toggleFavoriteCountryCollapse(country: String)`
  - `toggleFavoriteCityCollapse(cityKey: String)`
  - `collapseAllFavorites()`: contrae simultáneamente todos los países y ciudades presentes en `groupedFavorites`.
  - `expandAllFavorites()`: limpia los conjuntos colapsados para expandir todos los países y ciudades.
- Se mantuvieron y enlazaron a la cabecera los métodos existentes para rutas:
  - `collapseAllGroups()`
  - `expandAllGroups()`

### B. Pantalla de Favoritos (`SavedLocationsScreen.kt` & `SavedLocationsList.kt`)
- **Cabecera (`TopAppBar`):** Se añadieron acciones en el `TopAppBar` de `SavedLocationsScreen` que muestran los botones de "Contraer todo" (`UnfoldLess`) y "Expandir todo" (`UnfoldMore`) condicionados a la existencia de lugares guardados (`locations.isNotEmpty()`).
- **Lista (`SavedLocationsList.kt`):** Se adaptó para recibir opcionalmente `collapsedCountries`, `collapsedCities`, `onToggleCountry` y `onToggleCity`, manteniendo retrocompatibilidad total con el estado local anterior para pruebas unitarias y componentes aislados.

### C. Pantalla de Rutas (`HistoryScreen.kt`)
- **Cabecera (`Mis Recorridos`):** En el contenedor `Surface` de la cabecera se integraron los botones de "Contraer todo" (`UnfoldLess`) y "Expandir todo" (`UnfoldMore`) alineados a la derecha del título principal `Mis Recorridos`.

### D. Versionado Automático Sincronizado
- `app/build.gradle.kts`: `versionCode = 14`, `versionName = "3.2.14"`
- `TopBarLocalizador.kt`: `"Infraestructura Suite — v3.2.14-RC1"`
- `BrandWatermark.kt`: `version: String = "v3.2.14-RC1"`

---

## 3. Pruebas y Verificación

1. **Pruebas Unitarias de Compose y UI (`SavedLocationsListTest.kt`):**
   - `muestraPaisCiudadYNombreDelLugar`: Exitosa.
   - `colapsarPaisOcultaSusLugares`: Exitosa.
   - `cuandoPaisEstaColapsadoExternamenteOcultaSusLugares`: Exitosa (verifica que al pasar el país colapsado se ocultan sus ciudades y lugares).
   - `cuandoCiudadEstaColapsadaExternamenteOcultaSusLugares`: Exitosa (verifica que al colapsar una ciudad se ocultan sus lugares).

2. **Compilación y Pruebas Unitarias Gradle:**
   - Comando: `.\gradlew.bat testDebugUnitTest`
   - Resultado: `BUILD SUCCESSFUL` (40 tareas ejecutadas/al día, 0 fallos).

3. **Compilación Completa de Artefacto Debug:**
   - Comando: `.\gradlew.bat assembleDebug`
   - Resultado: `BUILD SUCCESSFUL in 49s` (44 tareas ejecutadas sin advertencias bloqueantes).
