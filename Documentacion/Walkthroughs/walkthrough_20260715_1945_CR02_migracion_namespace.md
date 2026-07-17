# Walkthrough CR-02 — Migración Global de Namespace
## Fecha: 2026-07-15 19:45

## Objetivo
Migrar el namespace de compilación de `com.example` a `com.aistudio.localizador`, preservando el applicationId (`com.aistudio.localizador.vqtspk`) para mantener la continuidad de la base de datos SQLCipher en dispositivos de prueba. Renombrar el tema de `Theme.MyApplication` a `Theme.Localizador` con parent Material3.

## Archivos modificados

### Configuración
- `app/build.gradle.kts`: `namespace = "com.aistudio.localizador"` (applicationId preservado)
- `res/values/themes.xml`: `Theme.Localizador` con parent `Theme.Material3.DayNight.NoActionBar`
- `AndroidManifest.xml`: `@style/Theme.Localizador` en `<application>` y `<activity>`
- `gradle/libs.versions.toml`: Añadido `material = "1.12.0"` y library correspondiente

### Renombrado de tema
- `ui/theme/Theme.kt`: `fun MyApplicationTheme(...)` → `fun LocalizadorTheme(...)`
- `MainActivity.kt`: Import y uso actualizados
- `GreetingScreenshotTest.kt`: Import y uso actualizados

### Migración física
- 61 archivos .kt movidos de `com/example/` a `com/aistudio/localizador/`
- 3 tests movidos de `com/example/` a `com/aistudio/localizador/`
- 1 androidTest movido de `com/example/` a `com/aistudio/localizador/`

### Correcciones de FQN inline (no imports)
- `AppDatabase.kt`: `com.example.data.model.SavedLocation::class` → `SavedLocation::class`
- `LocationService.kt`: Intent actions actualizadas a `com.aistudio.localizador.*`
- `BackupUseCase.kt`: `com.example.data.model.SavedLocation` → `SavedLocation` (con import añadido)

### Fixes pre-existentes
- `ExampleRobolectricTest`: SDK 36→34 (Robolectric 4.16.1 no soporta API 36), app_name corregido a "BiTaRu"
- `GreetingScreenshotTest`: SDK 36→34
- `ExampleInstrumentedTest`: package actualizado, assertion corregida a `vqtspk`

## Resultado
```
BUILD SUCCESSFUL in 2m 55s
49 actionable tasks: 20 executed, 29 up-to-date
7/7 tests pass
```

## Lecciones aprendidas
1. Los FQN inline (usados en anotaciones Room, intent strings) NO son detectados por reemplazos de `import` — requieren escaneo manual post-migración.
2. `Theme.Material3.DayNight.NoActionBar` requiere `com.google.android.material:material` como dependencia explícita.
3. AGP 9.2.1 no necesita `package` en AndroidManifest.xml — el namespace se define en build.gradle.kts.
4. PowerShell `@"..."@` inserta `` `n `` literal como texto — usar `@'...'@` o escapar correctamente los saltos de línea.