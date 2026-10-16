# Análisis de Problemas Identificados - BitaVi-Android

> **Fecha:** 14 de Octubre de 2026
> **Modo:** Solo lectura (auditoría completa)
> **Versión:** 2.0

---

## Resumen Ejecutivo

Proyecto Android de tracking GPS con arquitectura basada en Kotlin, Jetpack Compose, Hilt DI, Room + SQLCipher y mapas osmdroid. Tiene buenas prácticas de seguridad, pero presenta problemas de mantenibilidad, consistencia de branding y acumulación de archivos obsoletos.

---

## 1. Arquitectura y Estructura de Código

### 1.1 MainActivity con exceso de responsabilidades
- **Archivo:** `app/src/main/java/com/aistudio/localizador/MainActivity.kt` (181 líneas)
- **Problema:** Mezcla configuración de osmdroid, inicialización de BD cifrada, biometría, permisos y renderizado Compose.
- **Nota:** La refactorización hacia `ui/container/` ya está en progreso (ActivityContractsCoordinator, BiometricAuthHandler, MainScaffoldContainer). Falta completar la migración.

### 1.2 Pantallas sobredimensionadas
- **MapScreen.kt:** Más de 1300 líneas, mezcla gestión de mapa osmdroid, overlays, lógica de interacción y composición visual.
- **SavedLocationsScreen.kt:** Más de 1000 líneas.
- **Impacto:** Dificulta testing, mantenimiento y escalabilidad. Violación del principio de responsabilidad única (SRP).

### 1.3 LocationService complejo
- **Archivo:** `app/src/main/java/com/aistudio/localizador/location/LocationService.kt` (1057 líneas)
- **Problema:** Gestión directa de WakeLock, callbacks GPS, notificaciones, persistencia en Room, geocercas y reconocimiento de actividad en una sola clase.

---

## 2. Seguridad

### 2.1 Gestión de API Keys
- **Archivo:** `app/src/main/java/com/aistudio/localizador/ui/container/BiometricAuthHandler.kt` (líneas 156-174)
- **Problema:** La API Key de Groq se carga desde EncryptedSharedPreferences con retrocompatibilidad para claves sin cifrar (línea 158: si `iv == null`, lee la clave en texto plano y la migra). Es correcto como migración, pero debería eliminarse el fallback una vez consolidado.

### 2.2 Posible exposición en respaldos
- **Ubicación:** `RESPALDO_ANTERIOR/` (30+ archivos .kt)
- **Problema:** Archivos históricos del código fuente que podrían contener fragmentos con lógica de credenciales. No están cubiertos por .gitignore.

### 2.3 .gitignore incompleto
- No excluye la carpeta `RESPALDO_ANTERIOR/`, `tmp/`, `Documentacion/` ni archivos `.aider.*`.
- No excluye `my-upload-key.jks` ni `*.jks` (referenciado en build.gradle.kts línea 35).

---

## 3. Branding y Consistencia

### 3.1 Nomenclatura inconsistente
- **Namespace:** `com.aistudio.localizador`
- **Identidad visual:** "BitaVi" (título del proyecto), "Bitaru" (BitaruBrandHeader.kt), "BiTaRu" (imágenes), "Localizador" (strings, notificaciones).
- **Archivos afectados:**
  - `app/src/main/java/com/aistudio/localizador/ui/components/BitaruBrandHeader.kt`
  - `app/src/main/java/com/aistudio/localizador/ui/components/BrandWatermark.kt`
  - `MainActivity.kt` línea 90: userAgent "Agenviaje/1.0" (nombre fantasma no relacionado)
  - `LocationService.kt` línea 648: Log con tag "Bitaru_Core"

### 3.2 Application ID no coincide
- `applicationId = "com.aistudio.localizador.vqtspk"` (app/build.gradle.kts línea 18)
- El sufijo `vqtspk` no tiene relación con la identidad BitaVi ni Localizador.

---

## 4. Archivos Obsoletos y Almacenamiento

### 4.1 Carpetas con residuos
```
RESPALDO_ANTERIOR/    → 30+ archivos .kt con prefijos de fecha (respaldos manuales)
tmp/                  → temp_edit.js, temp_edit2.js, temp_edit3.js, temp_locationservice.txt
.aider.tags.cache.v4/ → Caché de la herramienta aider
.aider.chat.history.md, .aider.input.history → Historiales de sesión
```

### 4.2 Imágenes duplicadas
- `imagenes/Pantallas/` contiene archivos con nombres duplicados como `WhatsApp Image 2026-07-10 at 12.54.16.jpeg`, `12.54.16-.jpeg`, `12.54.16--.jpeg`, `12.54.16 (1).jpeg`.
- `imagenes/[Originals]/20260606_124211.jpg` duplica `imagenes/20260606_124211.jpg`.

---

## 5. Rendimiento

### 5.1 WakeLock de 25 horas
- **Archivo:** `LocationService.kt` línea 363: `wakeLock?.acquire(25 * 60 * 60 * 1000L)`
- **Riesgo:** Si el servicio no libera el WakeLock (crash, OOM), el CPU permanece activo hasta 25 horas agotando la batería. Considerar usar `acquire(timeout)` más corto con renovación periódica.

### 5.2 Timer de 1 segundo en LocationService
- **Archivo:** `LocationService.kt` línea 696: `delay(1.seconds)` dentro de `startTimer()`
- **Problema:** Ejecuta comprobaciones cada segundo durante toda la grabación de ruta. Podría evaluarse un intervalo de 5 segundos sin impacto funcional.

### 5.3 Consulta de favoritos sin caché
- **Archivo:** `LocationService.kt` línea 606: `repository.allSavedLocations.first()` en cada fix GPS dentro de `checkProximityToFavorites()`.
- **Problema:** Consulta a la base de datos cifrada (SQLCipher) en cada actualización de ubicación. Debería cachearse la lista de favoritos.

---

## 6. Dependencias y Build

### 6.1 URLs de API hardcodeadas
- **Archivo:** `data/api/RetrofitClient.kt` líneas 13-14
- `GROQ_BASE_URL = "https://api.groq.com/"`
- `GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"`
- **Recomendación:** Mover a `local.properties` o `BuildConfig`.

### 6.2 Sin timeouts explícitos en Retrofit/OkHttp
- No se configuran `connectTimeout`, `readTimeout` ni `writeTimeout` en el OkHttpClient.
- **Riesgo:** Peticiones a Groq/Gemini podrían colgarse indefinidamente sin cobertura de red.

### 6.3 Dependencias sin versionar en catalog
- `app/build.gradle.kts` línea 141: `"androidx.exifinterface:exifinterface:1.3.7"` (hardcodeada)
- `app/build.gradle.kts` línea 144: `"com.github.philjay:mpandroidchart:v3.1.0"` (hardcodeada)
- `gradle/libs.versions.toml` línea 100: `maps-compose` con versión inline `"6.1.1"` (no usa `version.ref`)

### 6.4 Comentario engañoso en build.gradle.kts
- Línea 122: `// Eliminamos Gemini SDK y Ktor para usar Retrofit directo` pero `GeminiApi.kt` y `GeminiAiUseCase.kt` siguen existiendo y usando Retrofit con Gemini API.

---

## 7. Testing

### 7.1 Claves de API hardcodeadas en tests
- **Archivo:** `app/src/test/java/com/aistudio/localizador/domain/usecase/AnalyzeRouteAiUseCaseTest.kt` líneas 28 y 46
- Usa `"gsk_test_key"` como API Key de prueba. Aunque es un placeholder, podría confundirse.

### 7.2 Acoplamiento a RetrofitClient estático
- `AnalyzeRouteAiUseCase` accede a `RetrofitClient.groqApi` directamente (línea 15), lo que impide mockear la capa de red en tests. El propio test lo reconoce en la línea 48.
- **Recomendación:** Inyectar la API mediante Hilt en lugar del singleton estático.

### 7.3 Cobertura limitada
- 27 archivos de test para ~80 archivos principales. No hay tests para pantallas Compose, el servicio de ubicación ni los managers de filtrado GPS (excepto los unitarios puros).

---

## 8. Concurrencia y Ciclo de Vida

### 8.1 serviceScope no cancelado limpiamente
- **Archivo:** `LocationService.kt` línea 69: `CoroutineScope(Dispatchers.IO + SupervisorJob())`
- Se cancela en `onDestroy()` (línea 1031), pero `stopTracking()` lanza corrutinas dentro de `serviceScope` (línea 862) que podrían ejecutarse después de `shutdownService()`.

### 8.2 Estado estático en companion object
- **Archivo:** `LocationService.kt` líneas 174-191: Los StateFlow (`_isTracking`, `_currentRouteId`, `_latestLocation`, etc.) son estáticos en el companion object.
- **Riesgo:** Estado global compartido que persiste entre instancias del servicio. Difícil de testear y puede causar estados inconsistentes si el servicio se reinicia.

### 8.3 Mutación directa de Location
- **Archivo:** `LocationService.kt` línea 471: `location.apply { latitude = processedBp.latitude; longitude = processedBp.longitude }`
- Modifica el objeto `Location` recibido del callback. Debería crear una copia con `Location(location)`.

---

## 9. Base de Datos

### 9.1 Migraciones vacías
- **Archivo:** `data/db/AppDatabase.kt`
- `MIGRATION_7_8` (línea 30-33): Vacía, no hace nada.
- `MIGRATION_9_10` (línea 51-54): Vacía, no hace nada.
- **Riesgo:** Si Room validó cambios de esquema en esas versiones, la app podría crashear al migrar.

### 9.2 exportSchema = false
- **Archivo:** `AppDatabase.kt` línea 19
- Sin esquemas exportados, no se pueden validar migraciones automáticamente ni usar `MigrationTestHelper`.

### 9.3 Recreación silenciosa de BD
- **Archivo:** `MainActivity.kt` líneas 110-117: Si la BD falla al abrir, se destruye y recrea sin informar al usuario. **Pérdida total de datos silenciosa.**

---

## 10. Permisos y Manifest

### 10.1 allowBackup = true con datos cifrados
- **Archivo:** `AndroidManifest.xml` línea 38
- Con `allowBackup="true"`, los datos (incluyendo EncryptedSharedPreferences) pueden extraerse vía adb backup. Para una app de privacidad como BitaVi, debería ser `false`.

### 10.2 Permisos de Contactos sin uso evidente
- `READ_CONTACTS` y `WRITE_CONTACTS` declarados en el manifest. Se usan en `ContactUseCase.kt` y `ProximityBleManager`, pero debería verificarse si son realmente necesarios para publicarse en Google Play.

---

## Resumen de Prioridades

| Prioridad | Área | Acción |
|-----------|------|--------|
| **P1 Alta** | Datos | Evitar recreación silenciosa de BD (§9.3) |
| **P1 Alta** | Seguridad | Agregar `RESPALDO_ANTERIOR/`, `*.jks`, `tmp/` al .gitignore (§2.3) |
| **P1 Alta** | Seguridad | Evaluar `allowBackup="false"` (§10.1) |
| **P2 Media** | Arquitectura | Completar modularización de MainActivity y LocationService (§1.1, §1.3) |
| **P2 Media** | Concurrencia | Eliminar estado estático del companion object (§8.2) |
| **P2 Media** | Rendimiento | Cachear favoritos en checkProximityToFavorites (§5.3) |
| **P3 Baja** | Consistencia | Unificar branding a "BitaVi" (§3.1) |
| **P3 Baja** | Build | Configurar timeouts en OkHttp (§6.2) |
| **P3 Baja** | Testing | Inyectar GroqApi vía Hilt en lugar de singleton estático (§7.2) |
| **P3 Baja** | Higiene | Limpiar tmp/, imágenes duplicadas (§4) |