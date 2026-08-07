# PLAN MAESTRO ME-02: Descomposicion de LocationService
**Fecha:** 2026-07-17 | **Estado:** Borrador | **Version:** 1.0

---

## 1. Objetivo

Reducir LocationService.kt de ~1139 lineas a ~700 lineas mediante la extraccion de 4 subsistemas
en managers inyectables, eliminando el acoplamiento de estado global y permitiendo tests
unitarios JVM aislados para cada subsistema.

---

## 2. Mapa de Subsistemas Extraibles

| # | Manager | Lineas en LocationService | Variables de Estado | Dependencias |
|---|---------|--------------------------|---------------------|--------------|
| 1 | AutoTrackingManager | 532-615, 804-838, 840-860 | lastStopLocation, movementSustainedStartTime, lastAutoStartTimeMillis, lastSpeedCheckTime, lastRealMovementTime, lastGpsTickTime, isSignalLost, preAlertActivityMode, watchdogJob, timerJob | AppConstants, TrackingSessionManager, CoroutineScope |
| 2 | ProximityBleManager | 249-255, 736-763, 765-787, 1128-1130 | isBleActive, lastBleNotificationTime | Context, ProximityManager, AlertNotifier, ContactUseCase, ContactHashHelper, AppConstants.Ble |
| 3 | ActivityModeManager | 662-686 | currentMode, lastModeChangeTime, lastComputedSpeedMps | AppConstants, StateFlow<ActivityMode> |
| 4 | ShortStopMarkManager | 476-522 | timeStartedStopping, isShortStopDetected, shortStopLat, shortStopLon, shortStopAddress | AppConstants, GeocoderUseCase, RouteRepository, CoroutineScope |

---

## 3. Especificacion de Cada Subsistema

### 3.1 AutoTrackingManager

**Responsabilidad:** Encapsular la maquina de estados del tracking automatico:
deteccion de movimiento sostenido (L532-615), watchdog GNSS (L840-860),
timer de inactividad (L804-838) y limite diario (L822-826).

#### Estado encapsulado (desde LocationService)

| Variable | Tipo | Linea | Uso |
|----------|------|-------|-----|
| lastStopLocation | Location? | 84 | Anclaje de arranque automatico |
| movementSustainedStartTime | Long | 103 | Inicio de ventana de velocidad sostenida |
| lastSpeedCheckTime | Long | 104 | Control de frecuencia de evaluacion |
| lastRealMovementTime | Long | 105 | Timestamp de ultimo movimiento real |
| lastAutoStartTimeMillis | Long | 100 | Cooldown entre reinicios (30s) |
| lastGpsTickTime | Long | 90 | Ultimo tick del GPS_PROVIDER |
| isSignalLost | Boolean | 91 | Flag de perdida de senhal |
| preAlertActivityMode | ActivityMode | 92 | Modo previo a la perdida de senhal |
| watchdogJob | Job? | 93 | Coroutine del watchdog GNSS |
| timerJob | Job? | 86 | Coroutine del timer de inactividad |
| startTimeMillis | Long | 87 | Inicio del timer de sesion |
| routeStartTimeMillis | Long | 94 | Inicio de la ruta para limite diario |
| lastMovementTimeMillis | Long | 88 | Ultimo movimiento (odometro) |
| lastGpsUpdateTimeMillis | Long | 89 | Ultima actualizacion GPS (Doze) |

#### Interfaz Callback

interface AutoTrackingCallback {
    fun onTriggerAutoTracking(bufferSnapshot: List<BufferPoint>)
    fun onInactivityTimeout()
    fun onDailyRotationNeeded()
    fun onSignalLost(preAlertMode: ActivityMode)
    fun onSignalRecovered()
}

#### Dependencias inyectadas por constructor

- AppConstants.Location (constantes de timing)
- TrackingSessionManager (para preRecordingBuffer)

#### Logica extraida (referencias exactas)

1. Bloque de movimiento sostenido: L532-615 completo
2. Watchdog GNSS: L840-860 (startGpsWatchdog())
3. Timer de inactividad: L804-838 (startTimer() - Check 1 y Check 3)
4. Campos de senhal GPS: L429-437 (actualizacion de lastGpsTickTime y manejo isSignalLost)

#### Logica que PERMANECE en LocationService

- triggerAutomaticTracking() (L879-902) - depende de GpsFilterPipeline y startAutoNewRoute
- stopTrackingWithAutoRestart() (L861-866) - depende de stopTracking()
- rotateDailyRoute() - depende del ciclo de vida del servicio
- startTimer() Check 2 (L822-826) - limite diario, opcionalmente migrable

---

### 3.2 ProximityBleManager

**Responsabilidad:** Encapsular todo el ecosistema BLE: inicializacion de ProximityManager,
advertising, scanning, filtrado RSSI, alertas tactiles/visuales y notificaciones.

#### Estado encapsulado (desde LocationService)

| Variable | Tipo | Linea | Uso |
|----------|------|-------|-----|
| proximityManager | ProximityManager? | 127 | Nucleo BLE nativo |
| alertNotifier | AlertNotifier? | 126 | Alertas tactiles (tono + vibracion) |
| isBleActive | Boolean | 129 | Flag de estado BLE |
| lastBleNotificationTime | Long | 130 | Cooldown de notificaciones |

#### Callbacks

interface BleCallback {
    fun onContactDetected(contactName: String, distance: Double)
}

#### Dependencias inyectadas por constructor

| Dependencia | Paquete real | Nota |
|-------------|--------------|------|
| @ApplicationContext Context | android | Para sistemas y notificaciones |
| ContactUseCase | ui/ContactUseCase.kt | Ubicacion real, no domain |
| AppConstants.Ble | util/AppConstants.kt | NOTIFICATION_COOLDOWN_MILLIS, HASH_BYTES |

#### Dependencias internas (creadas dentro del manager)

| Dependencia | Nota |
|-------------|------|
| ProximityManager(context) { ... } | Instanciada en init, callback despacha a BleCallback |
| AlertNotifier(context) | Instanciada internamente para triggerProximityAlert() |
| ContactHashHelper | Llamada estatica para hashPhoneNumber() |
| CoroutineScope(Dispatchers.IO) | Para llamada a contactUseCase.getAllContactPhones() |

#### Metodos publicos

| Metodo | Visibilidad | Origen |
|--------|-------------|--------|
| ensureProximity(isNearMe, isBleEnabled, bluetoothEnabled, myPhone, isTracking) | fun | L736-763 |
| handleIncomingBeacon(mac, rssi) | fun | Callback desde ProximityManager |
| stop() | fun | L1128-1130 (onDestroy) |
| setCallback(callback) | fun | Nuevo |

---

### 3.3 ActivityModeManager

**Responsabilidad:** Encapsular la maquina de histeresis del modo de actividad
(STOPPED/WALKING/BIKING/DRIVING) basada en velocidad cinematica (L662-686).

#### Estado encapsulado

| Variable | Tipo | Linea | Uso |
|----------|------|-------|-----|
| currentMode | ActivityMode | 108 | Modo actual con inercia |
| lastModeChangeTime | Long | 109 | Timestamp del ultimo cambio |
| lastComputedSpeedMps | Float | 110 | Ultima velocidad computada |

#### Constantes migradas del companion

| Constante | Valor |
|-----------|-------|
| TIME_TO_UPGRADE | 10_000L |
| TIME_TO_DOWNGRADE | 120_000L |

#### Interfaz

class ActivityModeManager @Inject constructor() {
    private var currentMode = ActivityMode.STOPPED
    private var lastModeChangeTime = 0L

    fun evaluate(speedMps: Float, timestamp: Long): ActivityMode? {
        // Retorna nuevo modo si cambio, null si se mantiene
    }

    fun reset()
}

Sin callback - retorna ActivityMode? que LocationService asigna a _currentActivityMode.value

---

### 3.4 ShortStopMarkManager

**Responsabilidad:** Encapsular la deteccion de paradas cortas y creacion
automatica de favoritos (L476-522).

#### Estado encapsulado

| Variable | Tipo | Linea | Uso |
|----------|------|-------|-----|
| timeStartedStopping | Long | 119 | Inicio de detencion |
| isShortStopDetected | Boolean | 120 | Flag de parada confirmada |
| shortStopLat | Double | 121 | Latitud de la parada |
| shortStopLon | Double | 122 | Longitud de la parada |
| shortStopAddress | String | 123 | Direccion geocodificada |

#### Constantes migradas

| Constante | Valor |
|-----------|-------|
| SHORT_STOP_DELAY_MS | 120_000L |
| STOP_TOLERANCE_MS | 180_000L |

#### Callback

interface ShortStopCallback {
    fun onShortStopDetected(lat: Double, lon: Double, address: String, durationMs: Long)
}

#### Dependencias

- AppConstants.Location (STOP_SPEED_THRESHOLD_KMH)
- GeocoderUseCase (para geocodificar direccion)
- CoroutineScope (para lanzar geocoder)

---

## 4. Orden de Migracion Propuesto

| Paso | Manager | Impacto en LocationService | Lineas extraidas | Riesgo |
|------|---------|---------------------------|------------------|--------|
| 1 | AutoTrackingManager | Eliminar L532-615, L840-860, L804-838 | ~160 | Medio |
| 2 | ActivityModeManager | Eliminar L662-686 + L462-466 | ~25 | Bajo |
| 3 | ShortStopMarkManager | Eliminar L476-522 | ~45 | Bajo |
| 4 | ProximityBleManager | Eliminar L249-255, L736-763, L765-787, L1128-1130 | ~60 | Medio |

**Total lineas extraibles:** ~290 (25% del archivo)
**Tamano resultante estimado:** ~850 lineas

---

## 5. Analisis de Riesgos por Paso

### Paso 1 (AutoTrackingManager) - Riesgo medio

| Riesgo | Mitigacion |
|--------|-----------|
| Corrutinas watchdog/timer se cancelan al rotar el manager | LocationService pasa su serviceScope al manager o los Jobs son gestionados externamente |
| _currentActivityMode.value es StateFlow del companion | LocationService asigna _currentActivityMode.value = ActivityMode.SIN_SENAL en el callback |
| triggerAutomaticTracking() y startAutoNewRoute() son privados de LocationService | Callback onTriggerAutoTracking(snapshot), LocationService ejecuta la cadena completa |

### Paso 2 (ActivityModeManager) - Riesgo bajo

| Riesgo | Mitigacion |
|--------|-----------|
| lastComputedSpeedMps usado en L462-466 y L560 | Mantener variable publica hasta migracion completa del Paso 1 |

### Paso 3 (ShortStopMarkManager) - Riesgo bajo

| Riesgo | Mitigacion |
|--------|-----------|
| repository.addSavedLocation() requiere RouteRepository inyectado | Inyectar en constructor o callback |
| serviceScope.launch para geocoder | Pasar CoroutineScope como dependencia |

### Paso 4 (ProximityBleManager) - Riesgo medio

| Riesgo | Mitigacion |
|--------|-----------|
| sharedPrefs leido sincronamente en ensureBleProximity() | Flags pasados como parametros desde LocationService |
| Notificaciones requieren Context y NotificationManager | Inyectar @ApplicationContext |
| onBleContactDetected() mezcla alerta tactil + notificacion | AlertNotifier interno; NotificationManager inline en el manager |

---

## 6. Mapa de Referencias Cruzadas

### LocationService: metodos que dejaran de existir

| Metodo | Manager destino | Lineas |
|--------|----------------|--------|
| startGpsWatchdog() | AutoTrackingManager | 840-860 |
| startTimer() | AutoTrackingManager (parcial) | 804-838 |
| updateActivityMode() | ActivityModeManager | 662-686 |
| ensureBleProximity() | ProximityBleManager | 736-763 |
| onBleContactDetected() | ProximityBleManager (callback) | 765-787 |
| (bloque inline) Short-stop | ShortStopMarkManager | 476-522 |
| (bloque inline) Movimiento sostenido | AutoTrackingManager | 532-615 |

### LocationService: metodos que permanecen

| Metodo | Razon |
|--------|-------|
| processNewLocation() | Orquestador principal |
| triggerAutomaticTracking() | Crea GpsFilterPipeline + puente a startAutoNewRoute |
| startAutoNewRoute() | Ciclo de vida del servicio |
| stopTrackingWithAutoRestart() | Ciclo de vida del servicio |
| rotateDailyRoute() | Ciclo de vida del servicio |
| checkProximityToFavorites() | No forma parte de ningun subsistema actual |
| onStartCommand(), onDestroy(), onBind() | Ciclo de vida del Service Android |

---

## 7. Dependencias Inyectables (LocationModule)

Despues de la migracion completa, LocationModule.kt contendra 8 providers:
4 existentes + 4 nuevos (todos unscoped):

- provideAutoTrackingManager(): AutoTrackingManager
- provideProximityBleManager(): ProximityBleManager
- provideActivityModeManager(): ActivityModeManager
- provideShortStopMarkManager(): ShortStopMarkManager

---

## 8. Verificacion Post-Migracion

Despues de cada paso:

1. ./gradlew assembleDebug -> BUILD SUCCESSFUL
2. ./gradlew testDebugUnitTest -> Suite completa pasando
3. Verificar en Logcat tags: Bitaru_AutoTrack, Bitaru_Ble, Bitaru_ActivityMode, Bitaru_ShortStop
4. Deploy a dispositivo fisico y verificar tracking manual + automatico + alertas BLE

---

*Documento generado como parte del Plan Maestro ME-02.*
*Proxima accion: implementar Paso 1 - AutoTrackingManager.*

---
## 9. Estado de Ejecucion

| Paso | Manager | Estado | Fecha | Lineas extraidas | Tests |
|------|---------|--------|-------|-----------------|-------|
| **1** | **AutoTrackingManager** | **COMPLETADO** | **2026-07-17** | **~60 (mas 75 nuevas)** | **5 tests nuevos, 21/21 suite** |
| 2 | ActivityModeManager | PENDIENTE | — | ~25 | — |
| 3 | ShortStopMarkManager | PENDIENTE | — | ~45 | — |
| 4 | ProximityBleManager | PENDIENTE | — | ~60 | — |
