# Recomendaciones y Soluciones para el Proyecto Localizador

## Resumen
Este documento detalla las mejoras técnicas recomendadas para el proyecto Localizador, priorizadas por impacto y esfuerzo estimado. El objetivo es mejorar la mantenibilidad, escalabilidad y calidad del código sin perder las funcionalidades avanzadas actuales.

---

## 🔧 Soluciones Prioritarias

### 1. **Modularización de Código** (Alta prioridad)

**Problema**: `MainActivity.kt` supera las 1000 líneas, mezclando lógica de UI, navegación, diálogos, permisos y configuración de servicios.

**Solución**:
- Separar cada pantalla en su propio archivo composable:
  ```
  ui/screens/
  ├── MapScreen.kt
  ├── HistoryScreen.kt
  ├── SavedLocationsScreen.kt
  ├── DetailScreen.kt
  ├── ExperienceMapScreen.kt
  └── HelpDialog.kt
  ```
- Extraer diálogos complejos a componentes reutilizables:
  ```
  ui/dialogs/
  ├── ApiKeyDialog.kt
  ├── LocationDialog.kt
  ├── DeleteConfirmDialog.kt
  └── HelpDialog.kt
  ```
- Crear `MainContainer.kt` que solo orqueste navegación y tema

**Beneficio**: Reduce MainActivity a ~200 líneas, facilita testing y trabajo en equipo.

---

### 2. **Adoptar Navigation Compose** (Media-Alta prioridad)

**Problema**: Navegación manual mediante `viewModel.currentScreen` (sealed class) y flags booleanos en `MainContainer`.

**Solución**:
```kotlin
// Navigation graph en MainActivity
val navController = rememberNavController()
NavHost(navController, startDestination = Screen.Tracking.route) {
    composable(Screen.Tracking.route) { MapScreen(viewModel) }
    composable(Screen.History.route) { HistoryScreen(viewModel) }
    composable(Screen.SavedLocations.route) { SavedLocationsScreen(viewModel) }
    composable(
        route = "${Screen.Detail.route}/{routeId}",
        arguments = listOf(navArgument("routeId") { type = NavType.LongType })
    ) { backStackEntry ->
        val routeId = backStackEntry.getLong() 
        DetailScreen(viewModel, routeId)
    }
    // ...
}
```

**Beneficio**: Navegación tipada, back stack automático, deep linking nativo, separación de responsabilidades.

---

### 3. **Centralizar Constantes y Configuración** (Media prioridad)

**Problema**: Números mágicos dispersos en múltiples archivos.

**Solución**: Crear archivo `AppConstants.kt`:
```kotlin
object AppConstants {
    object Location {
        const val MIN_MOVEMENT_DISTANCE_METERS = 25f
        const val GPS_ACCURACY_THRESHOLD_METERS = 25f
        const val BACKGROUND_LOCATION_UPDATE_INTERVAL_MS = 30000
        const val FOREGROUND_LOCATION_UPDATE_INTERVAL_MS = 10000
    }
    
    object Map {
        const val OFFLINE_CACHE_MAX_BYTES = 500L * 1024 * 1024 // 500MB
        const val MAX_TILE_DOWNLOAD_THREADS = 5
        const val MEMORY_CACHE_TILE_COUNT = 500
    }
    
    object Backup {
        const val FILE_EXTENSION = ".zip"
        const val MIME_TYPE = "application/zip"
    }
    
    object Biometric {
        const val TITLE = "Autenticación Requerida"
        const val SUBTITLE_API = "Desbloquee la API Key para activar la IA"
        const val SUBTITLE_SETTINGS = "Confirme su identidad para acceder a ajustes protegidos"
    }
    
    object Preferences {
        const val ENCRYPTED_PREFS_FILE = "secure_api_credentials_v2"
        const val MASTER_KEY_ALIAS = "localizador_pro_master_key_alias"
        const val GROQ_API_KEY_KEY = "groq_api_key_v2"
        const val GROQ_API_KEY_IV = "groq_api_key_iv"
        const val DB_PASSPHRASE_KEY = "room_sqlcipher_key"
    }
}
```

**Beneficio**: Cambios centralizados, fácil ajuste, documentación implícita.

---

### 4. **Migrar a DataStore para Preferencias No Sensibles** (Media prioridad)

**Problema**: Uso de `SharedPreferences` estándar para configuraciones simples (modo avión, offline, biométrico, etc.) junto con `EncryptedSharedPreferences` para credenciales.

**Solución**:
```kotlin
// DataStore para preferencias de UI/Configuración
data class AppPreferences(
    val isAirplaneMode: Boolean = false,
    val isAutoMode: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isNearMeEnabled: Boolean = true,
    val defaultLatitude: Double = -34.6037,
    val defaultLongitude: Double = -58.3816
)

object AppPreferencesSerializer : Serializer<AppPreferences> {
    override val defaultValue: AppPreferences = AppPreferences()
    override suspend fun readFrom(input: InputStream): AppPreferences { /* ... */ }
    override suspend fun writeTo(output: OutputStream, obj: AppPreferences) { /* ... */ }
}

// Uso
val Context.dataStore by preferencesDataStore(
    name = "app_preferences",
    serializer = AppPreferencesSerializer
)
```

**Beneficio**: Tipo seguro, corrutinas nativas, migración automática, mejor rendimiento.

---

### 5. **Optimizar Tareas en Segundo Plano con WorkManager** (Media prioridad)

**Problema**: Servicio foreground permanente para tracking + tareas periódicas manuales (sync, cleanup).

**Solución**:
```kotlin
// Solo para tracking activo real
class LocationTrackingService : ForegroundService() { /* ... */ }

// Para tareas periódicas no críticas
class PeriodicSyncWorker(context: Context, params: WorkerParameters) : 
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        // Sync backup, cleanup old tiles, etc.
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

// Programación
PeriodicWorkRequestBuilder<PeriodicSyncWorker>(24, TimeUnit.HOURS)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresCharging(false)
        .build())
    .build()
    .also { WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "periodic_sync", ExistingPeriodicWorkPolicy.KEEP, it) }
```

**Beneficio**: Uso óptimo de batería, respeta Doze mode, ejecución garantizada.

---

### 6. **Mejorar Manejo de Errores y Observabilidad** (Media prioridad)

**Problema**: `catch (_: Exception)` genéricos, falta de logging estructurado, mensajes de usuario genéricos.

**Solución**:
```kotlin
// Logger centralizado
object AppLogger {
    private const val TAG = "LocalizadorPro"
    
    fun d(message: String, throwable: Throwable? = null) = 
        Log.d(TAG, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = 
        Log.e(TAG, message, throwable)
    // ...
}

// Uso en repositorio
suspend fun getAllRoutes(): Result<List<Route>> = try {
    Result.success(routeDao.getAll())
} catch (e: SQLiteException) {
    AppLogger.e("Error leyendo rutas", e)
    Result.failure(e)
}

// Retry exponencial para red
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    baseDelayMs: Long = 1000,
    block: suspend () -> Result<T>
): Result<T> {
    var attempt = 0
    while (true) {
        val result = block()
        if (result.isSuccess || attempt >= maxRetries) return result
        delay(baseDelayMs * (2.0.pow(attempt)).toLong())
        attempt++
    }
}
```

**Beneficio**: Depuración más fácil, recuperaciones automáticas, mejor UX en errores.

---

## 📈 Matriz de Priorización

| Mejora | Impacto | Esfuerzo | ROI | Sprint Sugerido |
|--------|---------|----------|-----|-----------------|
| Modularización | ⭐⭐⭐⭐⭐ | Alto | Alto | 1-2 |
| Navigation Compose | ⭐⭐⭐⭐ | Medio | Alto | 2-3 |
| Constants | ⭐⭐⭐ | Bajo | Alto | 1 |
| DataStore | ⭐⭐⭐ | Bajo-Medio | Medio | 3 |
| WorkManager | ⭐⭐⭐ | Medio | Medio | 3-4 |
| Error Handling | ⭐⭐⭐⭐ | Bajo-Medio | Alto | 1-2 |

---

## 🗓️ Plan de Implementación Sugerido

### Sprint 1 (Semanas 1-2): Fundamentos
- [ ] Crear `AppConstants.kt`
- [ ] Extraer constantes mágicas existentes
- [ ] Crear estructura de paquetes `ui/screens`, `ui/dialogs`, `util`
- [ ] Mover `MapScreen`, `HistoryScreen` a archivos propios
- [ ] Configurar logger centralizado (`AppLogger`)

### Sprint 2 (Semanas 3-4): Navegación y UI
- [ ] Migrar a Navigation Compose
- [ ] Extraer diálogos: `ApiKeyDialog`, `LocationDialog`, `DeleteConfirmDialog`, `HelpDialog`
- [ ] Simplificar `MainContainer` a orquestador puro
- [ ] Añadir testing de navegación

### Sprint 3 (Semanas 5-6): Almacenamiento y Background
- [ ] Migrar preferencias UI a DataStore
- [ ] Implementar WorkManager para tareas periódicas
- [ ] Revisar y optimizar Foreground Service
- [ ] Añadir retry exponencial en llamadas de red (Groq API)

### Sprint 4 (Semanas 7-8): Pulido y Calidad
- [ ] Cobertura de tests unitarios >70% (ViewModels, Repository)
- [ ] Tests de UI para flujos críticos
- [ ] Documentación técnica actualizada
- [ ] Performance profiling (LeakCanary, Perfetto)
- [ ] Revisión de seguridad (OWASP MASVS)

---

## 📋 Checklist de Calidad Post-Implementación

- [ ] **MainActivity < 300 líneas**
- [ ] **Navegación 100% con Navigation Compose**
- [ ] **0 números mágicos en código** (todos en AppConstants)
- [ ] **SharedPreferences estándar = 0** (todo DataStore o EncryptedSharedPreferences)
- [ ] **WorkManager para todas las tareas periódicas no críticas**
- [ ] **Logging estructurado en todos los puntos críticos**
- [ ] **Tests unitarios >70% cobertura en capa data/domain**
- [ ] **Tests de UI para 5 flujos principales**
- [ ] **Documentación técnica actualizada**

---

## 🔗 Referencias Técnicas

- [Navigation Compose Guide](https://developer.android.com/jetpack/compose/navigation)
- [DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore)
- [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Foreground Service Best Practices](https://developer.android.com/guide/components/foreground-services)
- [Kotlin Result Type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)

---

*Documento generado el $(date) - Versión 1.0*


## 🛠️ Lecciones Aprendidas de Hardware (Julio 2026)

### A. Persistencia Criptográfica en Android TEE (Biometría de Arranque y Guardado)
**Regla Contractual Absoluta:** Cuando una clave del `AndroidKeystore` se genera con la directiva `setUserAuthenticationRequired(true)`, el chip de seguridad físico bloquea el descriptor del hardware en frío.
* **Error histórico:** Intentar instanciar `BiometricCipherHelper.getEncryptCipher()` o `getDecryptCipher()` antes o de forma desacoplada al diálogo nativo (`authenticateSimple`) provocaba rebotes inmediatos de hardware (`UserNotAuthenticatedException` o `KeyPermanentlyInvalidatedException`), rompiendo el hilo de ejecución.
* **Solución Implementada (Patrón en Dos Fases con CryptoObject):**
  Tanto el flujo de arranque en frío (`withStateAtLeast(Lifecycle.State.RESUMED)`) como el de guardado de credenciales (`onSaveClick`) deben encapsular síncronamente el `Cipher` dentro del `BiometricPrompt.CryptoObject(cipher)` a través del método `authenticateWithCrypto()`.
* **Ventana de Gracia:** Se inyectó `.setUserAuthenticationValidityDurationSeconds(5)` en la llave maestra para permitir operaciones de lectura/escritura seguidas sin exigir múltiples lecturas del sensor.
* **Resiliencia ante Cambios de OS:** Se implementó un catch explícito de `KeyPermanentlyInvalidatedException` conectado a `regenerateMasterKey()`. Si el usuario altera la configuración biométrica de Android (agrega/borra huellas o cambia el PIN general), la app limpia el alias y fuerza un reingreso controlado de la API Key en lugar de sufrir un Crash.

### B. Estabilización de Localización en Fondo (Filtro de Velocidad Sostenida)
Para neutralizar por completo el "baile de la flecha" provocado por el rebote de la señal GPS/GNSS en interiores, se modificó el paradigma de detección del motor de fondo en `LocationService.kt`:
* **Mecánica:** Se abandonó el anclaje por distancia estática y se reemplazó por un acumulador temporal de **Velocidad Sostenida** ($> 2.0\text{ km/h}$ ininterrumpidos durante un umbral de $5000\text{ ms}$).
* **Concurrencia Segura:** Las variables de control de tiempo (`movementSustainedStartTime`, `lastSpeedCheckTime`, `lastRealMovementTime`) fueron marcadas con la anotación `@Volatile` para asegurar visibilidad inmediata entre el hilo del callback de localización (`HandlerThread`) y el bucle del temporizador del servicio (`MainThread/Coroutines`). Esto blinda al Autostop de falsos reseteos en interiores.
