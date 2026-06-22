# 🔬 Auditoría Estática Completa — Localizador Pro Android

> **Fecha:** 2026-06-22 | **Auditor:** Ingeniero Principal de QA & Rendimiento Android  
> **Alcance:** 20 archivos fuente Kotlin, configuración Gradle, Manifest  
> **Regla:** Solo lectura — ningún archivo fue modificado

---

## Índice de Hallazgos por Severidad

| Severidad | Cantidad | Impacto |
|-----------|----------|---------|
| 🔴 **Alta** | 14 | Crashes, pérdida de datos, fugas de memoria, seguridad |
| 🟡 **Media** | 14 | Rendimiento, mantenibilidad, UX degradada |
| 🟢 **Baja** | 10 | Código muerto, convenciones, mejoras incrementales |

---

## 🔴 Severidad ALTA (14 hallazgos)

---

### A01 — Minificación y ProGuard DESACTIVADOS en Release

**Archivo:** [build.gradle.kts](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/build.gradle.kts#L46)

```kotlin
isMinifyEnabled = false // Línea 46
```

**Impacto:** El APK de producción incluye todo el código sin ofuscar, incluyendo clases de Retrofit/Moshi/OkHttp. Esto resulta en:
- APK significativamente más grande (>2x estimado)
- Código fuente completamente legible en descompilación (ingeniería inversa trivial)
- Sin tree-shaking de dependencias Material Icons Extended (~2400 iconos, >5MB)
- Sin optimización R8 de clases no utilizadas

**Sugerencia:** Activar `isMinifyEnabled = true` y configurar reglas ProGuard para Retrofit, Moshi, Room y OSMDroid.

---

### A02 — `fallbackToDestructiveMigration()` destruye datos de usuario silenciosamente

**Archivo:** [AppDatabase.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/data/db/AppDatabase.kt#L35)

```kotlin
.fallbackToDestructiveMigration() // Línea 35
```

**Impacto:** Cada vez que se incrementa la versión de la base de datos (actualmente `version = 7`), todas las rutas, waypoints y favoritos del usuario se eliminan silenciosamente sin aviso. En una app de tracking donde los datos son el valor central, esto es devastador.

**Sugerencia:** Implementar migraciones explícitas con `addMigrations(MIGRATION_7_8)` para preservar datos entre versiones. Mantener `fallbackToDestructiveMigration()` solo en builds de desarrollo.

---

### A03 — ViewModel God Object: RouteViewModel con 885 líneas

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt) — 885 líneas, 44KB

**Impacto:** Este ViewModel viola el principio de responsabilidad única al manejar simultáneamente:
- Navegación de pantallas
- Estado de tracking GPS
- Análisis con IA (Groq)
- Backup/Restore ZIP
- Importación/Exportación GPX y CSV
- Gestión de contactos
- Geocodificación
- Manejo de fotos/cámara
- Preferencias de usuario
- Compartir rutas

**Consecuencias:**
- Recomposiciones innecesarias en Compose: cualquier cambio de estado dispara recomposiciones en todas las pantallas
- Dificultad extrema para testing unitario
- `mutableStateOf` expuestos directamente crean acoplamiento fuerte

**Sugerencia:** Dividir en ViewModels especializados: `TrackingViewModel`, `HistoryViewModel`, `FavoritesViewModel`, `SettingsViewModel`, `ExportViewModel`.

---

### A04 — Fuga de memoria potencial: `CoroutineScope` manual en LocationService

**Archivo:** [LocationService.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/location/LocationService.kt#L33)

```kotlin
private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Línea 33
```

**Impacto:** Aunque `serviceScope.cancel()` se invoca en `onDestroy()` (L645), existe un riesgo si Android mata el proceso sin invocar `onDestroy()` (edge case conocido en Foreground Services con bajo nivel de memoria). Las coroutinas lanzadas en `serviceScope` acceden a `repository`, que a su vez mantiene una referencia a la base de datos Room.

Además, `stopTracking()` (L555-576) lanza una nueva coroutina en `serviceScope` para guardar datos, pero llama `stopSelf()` dentro de `withContext(Dispatchers.Main)`, lo que puede provocar que el servicio se destruya antes de que la coroutina de guardado complete su ejecución.

**Sugerencia:** Usar `lifecycleScope` del servicio (con `LifecycleService`) o garantizar que `stopSelf()` solo se invoque después de que el Job de guardado se complete usando `.join()`.

---

### A05 — Thread Safety: Acceso concurrente a variables mutables sin sincronización

**Archivo:** [LocationService.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/location/LocationService.kt)

Variables afectadas:
- `totalDistanceMeters` (L43) — leída y escrita desde múltiples coroutinas en `serviceScope`
- `lastRecordedLocation` (L44) — accedida desde `processNewLocation()` en el `HandlerThread` y desde coroutinas IO
- `preStartBuffer` (L55) — `mutableListOf` accedido desde el `HandlerThread` del callback GPS y desde coroutinas
- `consecutiveMovements` (L59) — incrementado desde el callback GPS, leído desde la misma

**Impacto:** Race conditions que pueden causar:
- Cálculos de distancia incorrectos (doble conteo o puntos perdidos)
- `ConcurrentModificationException` en `preStartBuffer`
- Arranques automáticos espurios en modo auto

**Sugerencia:** Usar `AtomicInteger`/`AtomicReference` para contadores, y `CopyOnWriteArrayList` o `Mutex` de coroutinas para `preStartBuffer`. Serializar todo el procesamiento de ubicación en un solo `Dispatcher.IO.limitedParallelism(1)`.

---

### A06 — `MutableStateFlow` estáticos en companion object causan leaks entre configuraciones

**Archivo:** [LocationService.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/location/LocationService.kt#L88-L101)

```kotlin
companion object {
    private val _isTracking = MutableStateFlow(false)        // L88
    private val _currentRouteId = MutableStateFlow<Int?>(null) // L91
    private val _latestLocation = MutableStateFlow<Location?>(null) // L94
    // ...
}
```

**Impacto:** Estos `StateFlow` estáticos sobreviven a la destrucción del servicio y mantienen referencias a objetos `Location` (que contienen `Bundle` con extras). Si el servicio se destruye sin limpiar correctamente, los valores obsoletos persisten en la siguiente instancia.

Aunque `stopTracking()` resetea los valores, el código **no los resetea** en `onDestroy()` si el servicio es matado por el sistema sin pasar por `stopTracking()`.

**Sugerencia:** Añadir reseteo explícito de todos los `StateFlow` estáticos en `onDestroy()`.

---

### A07 — `context as MainActivity` casting inseguro en Composable

**Archivo:** [MainActivity.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/MainActivity.kt#L233)

```kotlin
val mainActivity = context as MainActivity // Línea 233
```

**Impacto:** Este cast forzado fallará con `ClassCastException` si el composable es utilizado en un contexto diferente (tests instrumentados, Preview, multi-window). Es una violación directa de la separación entre UI y Activity en Jetpack Compose.

**Sugerencia:** Pasar las funciones necesarias como lambdas al composable, o usar un `LocalProvider` para exponer la funcionalidad de biometría.

---

### A08 — `Toast.makeText` desde contexto de Composable sin verificar lifecycle

**Archivo:** [MainActivity.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/MainActivity.kt#L294) y múltiples otros

```kotlin
Toast.makeText(context, "Restaurando datos y fotos...", Toast.LENGTH_SHORT).show() // L294
```

**Impacto:** Las llamadas a `Toast` desde callbacks de `ActivityResultContracts` no verifican que la actividad siga activa. Si el usuario rota la pantalla durante un backup/restore, el `context` puede apuntar a una Activity destruida, causando un leak del Window token o crash.

**Sugerencia:** Usar `Snackbar` de Material 3 integrado con el `SnackbarHostState` de Compose, que respeta el ciclo de vida.

---

### A09 — Excepciones silenciosas en operaciones críticas de I/O

**Archivos múltiples:**
- [RouteViewModel.kt L282](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L282): `catch (e: Exception) { }` en backup
- [RouteViewModel.kt L388](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L388): `catch (e: Exception) { }` en CSV export
- [RouteViewModel.kt L416](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L416): `catch (e: Exception) { }` en GPX export
- [RouteViewModel.kt L445](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L445): `catch (e: Exception) { }` en GPX import
- [LocationService.kt L424](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/location/LocationService.kt#L424): `catch (e: Exception) { }` en proximidad

**Impacto:** Fallos silenciosos en operaciones de exportación, importación y backup. El usuario cree que la operación fue exitosa cuando puede haber fallado completamente. Particularmente grave en backup (`prepareBackupFile`) y restore (`restoreFromBackup`), donde la pérdida de datos pasa desapercibida.

**Sugerencia:** Implementar un sistema de feedback al usuario para errores (Snackbar con acción de reintentar), y al menos loggear todas las excepciones.

---

### A10 — Vulnerabilidad de Zip Slip en `restoreFromBackup`

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L564-L617)

```kotlin
val name = entry.name
// ...
(name.startsWith("fotos/") || name.startsWith("images/")) -> {
    val fileName = name.substringAfterLast("/")
    if (fileName.isNotEmpty()) File(imageDir, fileName).outputStream().use { zis.copyTo(it) }
}
```

**Impacto:** Aunque se extrae solo el nombre del archivo (mitigando parcialmente el ataque), no se valida que `entry.name` no contenga secuencias de path traversal como `images/../../` antes del `substringAfterLast`. En el caso de restaurar archivos de base de datos (L572-574), el path se construye directamente desde `entry.name`:

```kotlin
name.contains("localizador_database") -> {
    val dbFile = File(context.getDatabasePath("localizador_database").parent, name.substringAfterLast("/"))
    dbFile.outputStream().use { zis.copyTo(it) }
}
```

Un ZIP malicioso podría sobreescribir archivos arbitrarios en el directorio de la base de datos.

**Sugerencia:** Validar que el archivo descomprimido se resuelve dentro del directorio esperado usando `canonicalPath.startsWith(targetDir.canonicalPath)`.

---

### A11 — User-Agent falsificado para OSMDroid

**Archivo:** [MainActivity.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/MainActivity.kt#L137)

```kotlin
Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."
```

**Impacto:** Violación de los [Términos de Uso de OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/) que requieren un User-Agent identificable. Suplantar un navegador de escritorio puede resultar en:
- Bloqueo de IP por parte de los servidores de tiles OSM
- Tráfico identificado como scraping automatizado e inhibido

**Sugerencia:** Usar un User-Agent descriptivo: `"LocaizadorPro/1.0 (Android; contact: dev@example.com)"`.

---

### A12 — Permisos excesivos declarados sin uso evidente

**Archivo:** [AndroidManifest.xml](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/AndroidManifest.xml#L16-L19)

```xml
<uses-permission android:name="android.permission.CAMERA" />           <!-- L16 -->
<uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION" />  <!-- L17 -->
<uses-permission android:name="android.permission.READ_CONTACTS" />    <!-- L18 -->
<uses-permission android:name="android.permission.WRITE_CONTACTS" />   <!-- L19 -->
```

**Impacto:**
- `WRITE_CONTACTS`: Nunca se utiliza en el código. La app solo lee contactos y lanza un Intent de inserción (que no requiere este permiso).
- `CAMERA`: Se solicita correctamente en runtime, pero el permiso en Manifest hace que Google Play muestre la app como requirente de cámara incluso si el feature está marcado `required=false`.

Los permisos innecesarios degradan la confianza del usuario y aumentan la superficie de ataque.

**Sugerencia:** Eliminar `WRITE_CONTACTS` del Manifest.

---

### A13 — `SharedPreferences` accedidas desde el hilo principal sin caching consistente

**Archivos:**
- [RouteViewModel.kt L77](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L77): `PreferenceManager.getDefaultSharedPreferences(context)` en cada toggle
- [RouteViewModel.kt L108](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L108): Misma instancia recreada en `updateDefaultLocationPersistently`
- [RouteViewModel.kt L115-116](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L115-L116): Y en `toggleAutoMode`

**Impacto:** Cada llamada a `PreferenceManager.getDefaultSharedPreferences()` implica una lectura de disco potencial (la primera vez) y un cache por instancia. Crear múltiples instancias del mismo `SharedPreferences` en diferentes métodos desperdicia memoria y puede crear inconsistencias de estado durante lecturas/escrituras concurrentes.

**Sugerencia:** Cachear una única instancia de `SharedPreferences` como propiedad del ViewModel durante `init {}`, como ya se hace en `LocationService`.

---

### A14 — `BiometricCipherHelper.getEncryptCipher()` no usa `CryptoObject` para cifrado

**Archivo:** [MainActivity.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/MainActivity.kt#L547-L548)

```kotlin
val encryptCipher = BiometricCipherHelper.getEncryptCipher() // L547
val (iv, encrypted) = BiometricCipherHelper.encryptText(tempKey, encryptCipher) // L548
```

**Impacto:** El cifrado de la API Key se realiza **sin** pasar el Cipher por un `BiometricPrompt.CryptoObject`. Dependiendo del nivel de API y la implementación del Keystore del dispositivo, el `Cipher` podría funcionar sin autenticación biométrica real, convirtiendo el modelo de seguridad `setUserAuthenticationRequired(true)` en ineficaz. Comparar con el descifrado (L170-172) que SÍ usa `authenticateWithCrypto()` correctamente.

**Sugerencia:** Envolver el `encryptCipher` en un flujo biométrico con `authenticateWithCrypto()` antes de llamar a `encryptText()`.

---

## 🟡 Severidad MEDIA (14 hallazgos)

---

### M01 — Recomposición excesiva: `derivedStateOf` con `System.currentTimeMillis()` no funciona

**Archivo:** [MapScreen.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/MapScreen.kt#L78-L80)

```kotlin
val isManualMode by remember { 
    derivedStateOf { System.currentTimeMillis() - lastInteractionTime < 10000L }
}
```

**Impacto:** `derivedStateOf` solo recalcula cuando sus inputs de estado Compose cambian. `System.currentTimeMillis()` **no es un estado Compose observable**, por lo que `isManualMode` nunca se actualiza en tiempo real — solo se recalcula cuando `lastInteractionTime` cambia. El nombre sugiere una intención de temporizador, pero la implementación no cumple esa semántica.

> [!NOTE]
> La variable `isManualMode` ni siquiera se utiliza directamente en el código de renderizado; se usa `lastInteractionTime != 0L` en su lugar (L302), lo que sugiere código vestigial.

---

### M02 — MapView recreada en recomposición de DetailScreen

**Archivo:** [DetailScreen.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/DetailScreen.kt#L139)

```kotlin
update = { view ->
    view.overlays.clear() // Línea 139 — DESTRUYE Y RECREA todos los overlays
    val poly = Polyline().apply { ... }
    view.overlays.add(poly)
    waypoints.forEach { wp ->
        val marker = Marker(view).apply { ... } // NUEVO marcador por recomposición
        view.overlays.add(marker)
    }
}
```

**Impacto:** A diferencia de `MapScreen.kt` que implementa diffing selectivo con caches de marcadores (L131-134), `DetailScreen` recrea TODOS los overlays (Polyline + Markers) en cada invocación del bloque `update`. Esto causa:
- Parpadeo visual del mapa
- Garbage collection innecesario
- Ineficiencia de CPU proporcional al número de waypoints

**Sugerencia:** Aplicar el mismo patrón de cache/diff que ya existe en `MapScreen`.

---

### M03 — `Geocoder` síncrono en Dispatchers.IO sin timeout ni fallback

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L236-L241)

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val geocoder = android.location.Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
    // ...
}
```

**Impacto:** `Geocoder.getFromLocation()` realiza una llamada de red al servicio de Google Play Services que puede bloquear indefinidamente sin timeout configurado. En zonas con mala conectividad, múltiples llamadas concurrentes (pulsaciones rápidas del usuario) pueden saturar los hilos del pool IO de coroutinas.

Además, se crean nuevas instancias de `Geocoder` en cada llamada (4 métodos diferentes: `fetchCurrentAddress`, `fetchAddressForCoordinates`, `fetchAddressForLocation`, `fetchLocationDetails`).

**Sugerencia:** Cachear la instancia de `Geocoder`, usar `withTimeoutOrNull` de coroutinas (e.g., 5 segundos), e implementar una cola serial para evitar llamadas concurrentes.

---

### M04 — Archivos temporales de exportación nunca se limpian

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L378-L389)

```kotlin
private fun saveCsvFile(content: String, routeName: String) {
    val fileName = "export_${System.currentTimeMillis()}.csv"
    context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
    // Archivo nunca se elimina después del share
}
```

**Impacto:** Cada exportación CSV/GPX crea un archivo temporal en `filesDir` con un nombre único por timestamp. Estos archivos nunca se eliminan, acumulándose indefinidamente. Lo mismo ocurre con capturas de mapa en `cacheDir/images/` (L837-843) durante compartición de rutas.

**Sugerencia:** Eliminar archivos temporales en un `finally` block o usar `File.deleteOnExit()`. Implementar limpieza periódica de archivos en `filesDir/export_*.csv` y `filesDir/export_*.gpx`.

---

### M05 — Navegación manual con `mutableStateOf` sin back-stack

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L68)

```kotlin
var currentScreen by mutableStateOf<Screen>(Screen.Tracking) // L68
fun navigateTo(screen: Screen) { currentScreen = screen }   // L147
```

**Impacto:** La navegación reimplementa manualmente lo que `NavController` de Jetpack Navigation Compose ya proporciona. Consecuencias:
- Sin soporte para botón "Atrás" del sistema — el usuario sale de la app en vez de volver a la pantalla anterior
- Sin deep-linking
- Sin animaciones de transición entre pantallas
- Sin preservación de estado al navegar (scroll position, etc.)
- Sin type-safe arguments para rutas

**Sugerencia:** Migrar a `NavHost` + `NavController` de `navigation-compose` (ya incluida como dependencia en L94 del build.gradle).

---

### M06 — `collectAsState()` en múltiples StateFlows causa recomposiciones en cascada

**Archivo:** [MapScreen.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/MapScreen.kt#L58-L64)

```kotlin
val isTracking by viewModel.isTrackingLive.collectAsState()     // L58
val latestLocation by viewModel.latestLocationLive.collectAsState() // L59
val elapsedSeconds by viewModel.elapsedSecondsLive.collectAsState() // L60
val distanceMeters by viewModel.distanceMetersLive.collectAsState() // L61
val routePoints by viewModel.activeRoutePoints.collectAsState()     // L62
val waypoints by viewModel.activeRouteWaypoints.collectAsState()    // L63
val savedLocations by viewModel.allSavedLocations.collectAsState()  // L64
```

**Impacto:** 7 `collectAsState()` en el nivel raíz del composable `MapScreen`. Cada emisión de CUALQUIERA de estos flows provoca la recomposición COMPLETA del composable, incluyendo el pesado bloque `AndroidView.update`. El `elapsedSecondsLive` emite cada 5 segundos (timer en `LocationService.startTimer()`), forzando actualizaciones de la `MapView` y todos los overlays incluso cuando solo cambia el cronómetro.

**Sugerencia:** Elevar los `collectAsState()` solo al composable hijo que los necesita. Usar `collectAsStateWithLifecycle()` de `lifecycle-runtime-compose`.

---

### M07 — Caché de tiles OSMDroid excesivo: 500MB

**Archivo:** [MainActivity.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/MainActivity.kt#L149)

```kotlin
Configuration.getInstance().tileFileSystemCacheMaxBytes = 500L * 1024 * 1024 // 500MB
```

**Impacto:** En dispositivos con almacenamiento limitado (entry-level Android con 16-32GB), 500MB de caché de tiles puede representar un porcentaje significativo del almacenamiento libre, causando que el sistema marque la app como consumidora excesiva de almacenamiento.

Además, `tileDownloadThreads = 5` (L146) puede crear contención de red excesiva en conexiones lentas.

**Sugerencia:** Reducir a 100-200MB o hacerlo configurable. Reducir `tileDownloadThreads` a 2-3.

---

### M08 — Manejo inconsistente de `isAirplaneMode` entre ViewModel y Service

**Archivo:** 
- [RouteViewModel.kt L112](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L112): `fun toggleAirplaneMode() { isAirplaneMode = !isAirplaneMode }`
- [LocationService.kt L352](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/location/LocationService.kt#L352): `val isAirplane = sharedPrefs.getBoolean("airplane_mode", false)`

**Impacto:** `isAirplaneMode` en el ViewModel se modifica solo en memoria (`mutableStateOf`), pero el `LocationService` lee el valor desde `SharedPreferences`. Como `toggleAirplaneMode()` nunca persiste el cambio en `SharedPreferences`, el servicio siempre leerá `false` para `"airplane_mode"`, haciendo que el filtro de velocidad máxima de 1250 km/h nunca se active.

**Sugerencia:** Persistir el toggle en SharedPreferences como se hace con `auto_mode` y `near_me_enabled`.

---

### M09 — `deleteAllData()` no limpia waypoints ni location_points explícitamente

**Archivo:** [RouteRepository.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/data/repository/RouteRepository.kt#L206-L209)

```kotlin
suspend fun deleteAllData() {
    routeDao.deleteAllRoutes()
    routeDao.deleteAllSavedLocations()
}
```

**Impacto:** Los `LocationPoint` y `Waypoint` están vinculados con `ForeignKey(onDelete = CASCADE)`, por lo que se eliminan al borrar las rutas. Sin embargo:
- Los archivos de fotos en `filesDir/images/` NO se eliminan
- Los archivos temporales de exportación NO se eliminan
- El caché de tiles de OSMDroid NO se limpia

El usuario cree que "Limpiar Todo" elimina todo, pero las fotos y archivos huérfanos persisten ocupando almacenamiento.

**Sugerencia:** Añadir limpieza de `filesDir/images/` y archivos temporales en `deleteAllData()`.

---

### M10 — Duplicación masiva de código UI: Selector de color repetido 4 veces

**Archivos:**
- [MapScreen.kt L558-606](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/MapScreen.kt#L558-L606) (Waypoint dialog)
- [MapScreen.kt L757-806](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/MapScreen.kt#L757-L806) (Save location dialog)
- [SavedLocationsScreen.kt L250-299](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/SavedLocationsScreen.kt#L250-L299) (Add dialog)
- [SavedLocationsScreen.kt L477-526](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/SavedLocationsScreen.kt#L477-L526) (Edit dialog)

**Impacto:** ~50 líneas de código idénticas copiadas 4 veces. Cada cambio futuro en la lógica del selector de color debe propagarse manualmente a 4 ubicaciones, aumentando exponencialmente el riesgo de inconsistencias.

**Sugerencia:** Extraer un composable `ColorPickerSection(selectedColor, onColorChange)`.

---

### M11 — `ElevationChart` (Canvas) nunca se utiliza

**Archivo:** [DetailScreen.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/screens/DetailScreen.kt#L324-L344)

```kotlin
@Composable
fun ElevationChart(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
```

**Impacto:** Código muerto. Se implementó un chart con Canvas puro, pero luego se reemplazó por `ProfessionalChart` que usa MPAndroidChart. El composable `ElevationChart` nunca se invoca, añadiendo peso muerto al APK.

**Sugerencia:** Eliminar `ElevationChart` o reemplazar la dependencia de MPAndroidChart por esta implementación nativa más ligera.

---

### M12 — `Greeting` composable es código de template no eliminado

**Archivo:** [MainActivity.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/MainActivity.kt#L864-L867)

```kotlin
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
```

**Impacto:** Código residual del template de nuevo proyecto de Android Studio. Indica una falta de limpieza del proyecto.

---

### M13 — Firebase BOM declarada como dependencia sin uso

**Archivo:** [build.gradle.kts](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/build.gradle.kts#L76)

```kotlin
implementation(platform(libs.firebase.bom)) // L76
```

**Impacto:** El proyecto incluye el BOM de Firebase pero no usa ninguna librería Firebase (firebase-ai está en el catálogo pero comentada). Esto añade resolución de dependencias innecesaria y puede causar conflictos de versiones transitorias.

**Sugerencia:** Eliminar la dependencia `firebase-bom` si no se usa ningún servicio Firebase.

---

### M14 — `gradle.properties` tiene `org.gradle.parallel` definido con valores contradictorios

**Archivo:** [gradle.properties](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/gradle.properties#L11-L16)

```properties
org.gradle.parallel=false    # Línea 11
# ...
org.gradle.parallel=true     # Línea 16
```

**Impacto:** La segunda definición (L16) sobreescribe la primera (L11). Aunque el resultado final es correcto (`true`), las propiedades contradictorias indican confusión en la configuración y pueden llevar a errores de razonamiento futuro.

**Sugerencia:** Eliminar la línea duplicada (L11).

---

## 🟢 Severidad BAJA (10 hallazgos)

---

### B01 — `@Suppress("UNCHECKED_CAST")` implícito en ViewModelFactory

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L879-L884)

```kotlin
class RouteViewModelFactory(...) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RouteViewModel(application, repository) as T // Unchecked cast
    }
}
```

**Impacto:** Patrón obsoleto. Desde `lifecycle-viewmodel 2.5.0+`, se puede usar `CreationExtras` o `viewModels { }` con factory lambda.

**Sugerencia:** Migrar a `viewModels { RouteViewModel(application, repository) }` o usar un framework de DI como Hilt.

---

### B02 — Uso de `import android.app.*` y `com.google.android.gms.location.*` (wildcards)

**Archivo:** [LocationService.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/location/LocationService.kt#L3-L21)

**Impacto:** Los imports wildcard dificultan la comprensión de qué clases se usan realmente y pueden causar colisiones de nombres inesperadas.

**Sugerencia:** Usar imports explícitos.

---

### B03 — Comentarios en inglés del template mezclados con español del proyecto

**Archivos múltiples:** El proyecto alterna entre español (comentarios del desarrollador) e inglés (comentarios de templates y constantes). Esto dificulta la lectura cohesiva.

**Sugerencia:** Unificar idioma de comentarios (español dado el contexto del proyecto).

---

### B04 — `namespace = "com.example"` es un placeholder genérico

**Archivo:** [build.gradle.kts](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/build.gradle.kts#L14)

```kotlin
namespace = "com.example"
```

**Impacto:** El `applicationId` es correcto (`com.aistudio.localizador.vqtspk`), pero el namespace para las clases R es `com.example`, lo cual es confuso y puede causar conflictos con otros proyectos en el mismo workspace.

**Sugerencia:** Alinear namespace con applicationId: `com.aistudio.localizador`.

---

### B05 — Hardcoded strings en la UI

**Archivos múltiples:** Todos los textos de la interfaz están hardcodeados directamente en el código Kotlin en lugar de usar `strings.xml`. Esto imposibilita:
- Internacionalización (i18n)
- Cambio de textos sin recompilar
- Consistencia de textos entre pantallas

**Sugerencia:** Extraer strings a `res/values/strings.xml` progresivamente, empezando por los textos visibles al usuario.

---

### B06 — `exportSchema = false` en Room

**Archivo:** [AppDatabase.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/data/db/AppDatabase.kt#L18)

```kotlin
exportSchema = false
```

**Impacto:** Sin exportación de esquema, no se pueden generar migraciones automáticas ni verificar la integridad del esquema en tests. Combinado con `fallbackToDestructiveMigration()`, el proyecto no tiene ninguna red de seguridad para la evolución del modelo de datos.

**Sugerencia:** Activar `exportSchema = true` y configurar `room.schemaLocation` en el build.gradle para exportar JSON de esquema.

---

### B07 — Shell scripts en el directorio `app/`

**Archivo:** [app/](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app)

```
new.sh, new2.sh, new3.sh, new4.sh
```

**Impacto:** Scripts de desarrollo/debugging dejados en el directorio del módulo. Pueden ser incluidos en el APK dependiendo de la configuración de packaging.

**Sugerencia:** Mover a un directorio `scripts/` fuera del módulo `app` o añadir al `.gitignore`.

---

### B08 — Dependencias alpha en producción

**Archivo:** [libs.versions.toml](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/gradle/libs.versions.toml#L43-L44)

```toml
biometric = "1.2.0-alpha05"     # L43
securityCrypto = "1.1.0-alpha06" # L44
```

**Impacto:** Las versiones alpha no tienen garantía de estabilidad API ni de comportamiento. Pueden contener bugs conocidos sin fix. Para un feature de seguridad (cifrado biométrico), esto es particularmente arriesgado.

**Sugerencia:** Evaluar migrar a versiones estables o al menos RC. `biometric:1.1.0` es estable; `security-crypto:1.0.0` es estable.

---

### B09 — GPX export incompleto: falta `<ele>`, `<time>`, y atributos de namespace

**Archivo:** [RouteViewModel.kt](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/src/main/java/com/example/ui/RouteViewModel.kt#L391-L402)

```kotlin
sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\">\n")
// ...
sb.append("<trkpt lat=\"${it.latitude}\" lon=\"${it.longitude}\"></trkpt>\n")
```

**Impacto:** El GPX exportado:
- No incluye namespace (`xmlns`) requerido por el estándar GPX 1.1
- No exporta elevación (`<ele>`) ni timestamp (`<time>`) aunque los datos existen en `LocationPoint`
- No incluye metadatos del track (`<desc>`, estadísticas)

Muchas apps de terceros no importarán este archivo correctamente.

**Sugerencia:** Generar GPX compliant: incluir `xmlns`, `<ele>`, `<time>`, y `<metadata>`.

---

### B10 — `material-icons-extended` completa incluida como dependencia

**Archivo:** [build.gradle.kts](file:///home/delm/Documentos/Google%20IA%20Studio/localizador/app/build.gradle.kts#L84)

```kotlin
implementation(libs.androidx.compose.material.icons.extended) // L84
```

**Impacto:** Esta librería contiene ~2400 iconos y añade varios MB al APK. Solo se usan ~30 iconos diferentes en el proyecto. Sin R8/minificación (hallazgo A01), TODOS los iconos se incluyen.

**Sugerencia:** Con minificación activada, R8 eliminará los no usados. Alternativamente, copiar solo los iconos SVG necesarios como drawables.

---

## 📊 Resumen Ejecutivo

### Estado General de Salud del Proyecto

| Área | Evaluación | Críticos |
|------|-----------|----------|
| Arquitectura | ⚠️ Concentración excesiva | God ViewModel, sin DI, sin Navigation |
| Seguridad | ⚠️ Riesgos en cifrado y Zip | Zip Slip, BiometricCipher inconsistente |
| Rendimiento UI | ⚠️ Recomposiciones excesivas | 7 StateFlows en raíz de MapScreen |
| Memory Management | ⚠️ Riesgos de leak | Service CoroutineScope, StateFlow estáticos |
| Batería/Red | ✅ Razonable | Filtros de GPS bien implementados |
| Datos de Usuario | 🔴 Riesgo crítico | DestructiveMigration borra datos |
| Build Configuration | ⚠️ No optimizada | Sin minificación, dependencias alpha |
| Calidad de Código | ⚠️ Duplicación significativa | 4x selector de color, excepciones silenciosas |

### Top 5 Prioridades de Remediación

1. **Activar R8/minificación** (A01) — Impacto inmediato en tamaño APK y seguridad
2. **Eliminar `fallbackToDestructiveMigration`** (A02) — Proteger datos del usuario
3. **Corregir bug de `isAirplaneMode`** (M08) — Filtro de velocidad roto, bug funcional
4. **Asegurar thread safety en LocationService** (A05) — Prevenir crashes intermitentes
5. **Dividir RouteViewModel** (A03) — Fundamento para todas las mejoras futuras

> [!CAUTION]
> Este reporte documenta hallazgos sin modificar ningún archivo. Todas las sugerencias deben implementarse con testing unitario e instrumentado previo.
