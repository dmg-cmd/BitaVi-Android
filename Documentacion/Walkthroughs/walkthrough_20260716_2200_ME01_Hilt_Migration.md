# Walkthrough ME-01: Migración a Hilt DI

**Fecha:** 2026-07-16 22:00
**Rama:** main (working tree)

## Resumen
Migración completa del proyecto a Hilt 2.59 con KSP, compatible con AGP 9.2.1.

## Pasos Realizados

### 1. Configuración de Gradle
- libs.versions.toml: hilt 2.52 → 2.59, añadido hilt-android-testing
- uild.gradle.kts (app): kapt eliminado, ksp añadido, HiltTestRunner configurado

### 2. DatabaseModule
- Añadido provider para RouteRepository

### 3. AppModule (nuevo)
- Providers: PreferencesManager, GeocoderUseCase, PhotoManager, GroqAiUseCase, GeminiAiUseCase, RouteExchanger, RouteExportUseCase, BackupUseCase, ContactUseCase, ShareUseCase, LocationServiceManager

### 4. RouteViewModel
- @HiltViewModel + @Inject constructor
- RouteViewModelFactory eliminado

### 5. MainActivity
- @AndroidEntryPoint
- ViewModel via y viewModels()

### 6. LocationService
- @AndroidEntryPoint
- @Inject en repository, contactUseCase, geocoderUseCase
- Bloque de creación de DB/repositorio removido de onCreate()

### 7. Bugfix: StationaryAnchorManager
- processStationaryState() ahora congela anclaje con guardia if (!hasAnchor)
- Test de drift prolongado ahora pasa (14/14)

### 8. HiltTestRunner (nuevo)
- HiltTestRunner.kt para instrumentation tests

## Verificación
- ssembleDebug: BUILD SUCCESSFUL
- 	estDebugUnitTest: 14/14 passing
- Advertencia única: Moshi kapt deprecation (no bloqueante)
