package com.aistudio.localizador.location

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aistudio.localizador.MainActivity
import com.aistudio.localizador.data.db.AppDatabase
import com.aistudio.localizador.R
import com.aistudio.localizador.ui.ContactUseCase
import com.aistudio.localizador.util.AlertNotifier
import com.aistudio.localizador.util.AppConstants
import com.aistudio.localizador.util.ContactHashHelper
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.repository.RouteRepository
import androidx.preference.PreferenceManager
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.location.manager.StationaryAnchorManager
import com.aistudio.localizador.location.manager.ActivityRecognitionManager
import com.aistudio.localizador.location.manager.ServiceNotificationManager
import com.aistudio.localizador.location.manager.TrackingSessionManager
import com.aistudio.localizador.ui.GeocoderUseCase
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.LinkedList
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration.Companion.seconds

enum class ActivityMode { STOPPED, WALKING, BIKING, DRIVING, SIN_SENAL }

/**
 * Servicio en primer plano (Foreground Service) que gestiona el seguimiento GPS en tiempo real.
 * Diseñado con persistencia interna reactiva (Room) y variables estáticas observables en Compose.
 */
class LocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: RouteRepository

    private lateinit var sharedPrefs: SharedPreferences
    private var locationHandlerThread: HandlerThread? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val screenStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                lastUserActivityTimeMillis = System.currentTimeMillis()
            }
        }
    }

    private var activeRouteId: Int = -1
    private var totalDistanceMeters: Double = 0.0
    private var lastValidLocation: Location? = null
    private val notificationManager = ServiceNotificationManager(this)
    private val sessionManager = TrackingSessionManager()
    private val cinematicFilter = com.aistudio.localizador.location.manager.CinematicFilterManager(
        maxAllowedSpeedMps = AppConstants.Location.MAX_SPEED_METERS_PER_SECOND.toDouble(),
        autoStopSpeedMps = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()
    )
    private var lastStopLocation: Location? = null

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L
    private var lastMovementTimeMillis: Long = System.currentTimeMillis()
    private var lastGpsUpdateTimeMillis: Long = System.currentTimeMillis()
    private var lastGpsTickTime: Long = 0L
    private var isSignalLost: Boolean = false
    private var preAlertActivityMode: ActivityMode = ActivityMode.STOPPED
    private var watchdogJob: Job? = null
    private var routeStartTimeMillis: Long = 0L
    private var isWaitingForMovement: Boolean = false
    private var activeRouteName: String = "Ruta"
    
    // Buffer elástico de pre-grabación (Fase 3A)
    private val preRecordingBuffer = ConcurrentLinkedDeque<Location>()

    private fun handlePreRecording(newLocation: Location) {
        preRecordingBuffer.addLast(newLocation)
        val newestTime = preRecordingBuffer.peekLast()?.time ?: return
        while (true) {
            val firstPoint = preRecordingBuffer.peekFirst() ?: break
            if ((newestTime - firstPoint.time) > AppConstants.Location.PRE_RECORDING_BUFFER_DURATION_MILLIS) {
                preRecordingBuffer.removeFirst()
            } else {
                break
            }
        }
    }


    // 🛡️ Mapeo blindado con argumentos nominales explícitos para evitar corrupciones de orden
    private fun Location.toBufferPoint(routeId: Int): LocationPoint = LocationPoint(
        routeId = routeId,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude,
        speed = this.speed,
        timestamp = if (this.time > 0) this.time else System.currentTimeMillis() // Salvaguarda si el GPS da 0
    )

    // Variables para refinamiento de Modo Automático
    private var lastAutoStartTimeMillis: Long = 0L // Para evitar reinicios infinitos rápidos

    // Velocidad sostenida para auto-arranque (Módulo 3C)
    @Volatile private var movementSustainedStartTime = 0L
    @Volatile private var lastSpeedCheckTime = 0L
    @Volatile private var lastRealMovementTime = System.currentTimeMillis()

    // Activity Mode con inercia (histeresis)
    private var currentMode = ActivityMode.STOPPED
    private var lastModeChangeTime = 0L
    private var lastComputedSpeedMps = 0f

    // Lógica "Cerca de mí"
    private var lastNotifiedFavoriteId: Int = -1
    private var lastNotificationTime: Long = 0L
    private val PROXIMITY_THRESHOLD_METERS = 100f
    private val NOTIFICATION_COOLDOWN_MS = 10 * 60 * 1000L // 10 minutos para repetir aviso

    // Short Stop Marker (Paradas Cortas)
    private var timeStartedStopping: Long = 0L
    private var isShortStopDetected = false
    private var shortStopLat = 0.0
    private var shortStopLon = 0.0
    private var shortStopAddress = ""

    // BLE Proximity (Módulo 3B)
    private var alertNotifier: AlertNotifier? = null
    private var proximityManager: ProximityManager? = null
    private val stationaryAnchorManager = StationaryAnchorManager()
    private var activityRecognitionManager: ActivityRecognitionManager? = null
    private var isBleActive = false
    private var lastBleNotificationTime: Long = 0L
    private var contactUseCase: ContactUseCase? = null

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1205

        // Filtro de ruido GPS inteligente (valores centralizados en AppConstants)
        private const val MAX_GPS_ACCURACY = AppConstants.Location.MAX_GPS_ACCURACY_METERS
        private const val TOLERANT_ACCURACY = AppConstants.Location.MAX_POCKET_ACCURACY_METERS

        // Acciones de ActivityTransitionReceiver
        const val ACTION_AUTO_START_FROM_TRANSITION = "com.aistudio.localizador.location.ACTION_AUTO_START_FROM_TRANSITION"
        const val EXTRA_DETECTED_ACTIVITY = "com.aistudio.localizador.location.EXTRA_DETECTED_ACTIVITY"

        // Activity Mode: tiempos de inercia
        private const val TIME_TO_UPGRADE = 10_000L   // 10s para subir de intensidad (inicio rápido)
        private const val TIME_TO_DOWNGRADE = 120_000L // 2min para bajar de intensidad

        // Short Stop Marker
        private const val SHORT_STOP_DELAY_MS = 120_000L
        private const val STOP_TOLERANCE_MS = 180_000L

        // Acciones de control del servicio
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_START_PASSIVE = "ACTION_START_PASSIVE"
        const val ACTION_SHUTDOWN = "ACTION_SHUTDOWN"
        const val ACTION_UPDATE_SETTINGS = "ACTION_UPDATE_SETTINGS"

        // Claves para Intent
        const val EXTRA_ROUTE_NAME = "EXTRA_ROUTE_NAME"
        const val EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID"
        const val EXTRA_UPDATE_INTERVAL = "EXTRA_UPDATE_INTERVAL"
        const val EXTRA_UPDATE_DISTANCE = "EXTRA_UPDATE_DISTANCE"

        // Última actividad conocida del usuario (toques en pantalla, screen on)
        @Volatile
        var lastUserActivityTimeMillis: Long = System.currentTimeMillis()
            private set

        fun reportUserActivity() {
            lastUserActivityTimeMillis = System.currentTimeMillis()
        }

        // Estado global expuesto en tiempo real para Compose
        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _currentRouteId = MutableStateFlow<Int?>(null)
        val currentRouteId = _currentRouteId.asStateFlow()

        private val _latestLocation = MutableStateFlow<Location?>(null)
        val latestLocation = _latestLocation.asStateFlow()


        private var _elapsedTimeSeconds = 0L

        private var _currentDistanceMeters = 0.0

        private val _currentActivityMode = MutableStateFlow(ActivityMode.STOPPED)
        val currentActivityMode = _currentActivityMode.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        
        // 🛡️ Obtención simétrica de clave de descifrado para SQLCipher en segundo plano
        val dbPassphrase = try {
            val masterKey = MasterKey.Builder(this, AppConstants.Preferences.MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setUserAuthenticationRequired(false)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                this,
                AppConstants.Preferences.ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            encryptedPrefs.getString(AppConstants.Preferences.DB_PASSPHRASE_KEY, null)?.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Error crítico recuperando clave de cifrado en servicio", e)
            null
        }

        // Inicialización simétrica blindada
        val database = AppDatabase.getInstance(applicationContext, dbPassphrase)
        repository = RouteRepository(database.routeDao())

        // Inicializar WakeLock para evitar que el sistema suspenda el CPU
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Localizador:TrackingWakeLock")

        // Cache de SharedPreferences para evitar I/O repetitivo en el Main Thread
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        // HandlerThread dedicado para procesar ubicación fuera del Main Thread
        locationHandlerThread = HandlerThread("LocationHandlerThread").apply {
            start()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configuración del callback para actualizaciones GPS de FusedLocationProviderClient
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.forEach { location ->
                    processNewLocation(location)
                }
            }
        }

        // Inicializar BLE Proximity (Módulo 3B)
        alertNotifier = AlertNotifier(this)
        contactUseCase = ContactUseCase(this)
        ProximityManager(this) { contactName, _, distance ->
            onBleContactDetected(contactName, distance)
        }.let { pm ->
            if (pm.init()) proximityManager = pm
        }

        createNotificationChannel()

        // Registrar receiver para detectar encendido de pantalla (actividad de usuario)
        val screenFilter = IntentFilter(Intent.ACTION_SCREEN_ON).apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            applicationContext,
            screenStateReceiver,
            screenFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Inicializar el reconocimiento de actividad física para auto-arranque
        activityRecognitionManager = ActivityRecognitionManager(this)
        if (sharedPrefs.getBoolean("auto_mode", false)) {
            activityRecognitionManager?.startTrackingTransitions()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val action = intent.action
        Log.d(TAG, "onStartCommand acción recibida: $action")

        // Asegurar que el servicio inicie en primer plano de inmediato (Requisito Android 12+)
        if (action != ACTION_SHUTDOWN) {
            ensureForeground()
        }

        try {
            when (action) {
                ACTION_SHUTDOWN -> {
                    shutdownService()
                }
                ACTION_START_PASSIVE -> {
                    startPassiveTracking()
                }
                ACTION_START -> {
                    val routeName = intent.getStringExtra(EXTRA_ROUTE_NAME) ?: "Ruta sin nombre"
                    val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)
                    val interval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, 5000L)
                    val distance = intent.getFloatExtra(EXTRA_UPDATE_DISTANCE, 2f)
                    startTracking(routeName, routeId, interval, distance)
                }
                ACTION_PAUSE -> {
                    pauseTracking()
                }
                ACTION_STOP -> {
                    stopTracking()
                }
                ACTION_UPDATE_SETTINGS -> {
                    // No hace falta lógica especial, solo procesar la siguiente ubicación con los nuevos flags de SharedPreferences
                }
                ACTION_AUTO_START_FROM_TRANSITION -> {
                    val detectedActivity = intent.getIntExtra(EXTRA_DETECTED_ACTIVITY, -1)
                    handleAutoStartFromTransition(detectedActivity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onStartCommand: ${e.message}")
            e.printStackTrace()
        }

        return START_STICKY
    }

    private fun startPassiveTracking() {
        // Adquirir WakeLock para auto-mode con timeout de seguridad (25h)
        wakeLock?.let {
            if (!it.isHeld) it.acquire(25 * 60 * 60 * 1000L)
        }

        // Al iniciar en modo pasivo con Auto-Mode, nos ponemos en primer plano para que Android no nos mate
        val isAutoModeActive = sharedPrefs.getBoolean("auto_mode", false)
        val statusText = if (isAutoModeActive) "Vigilando movimiento... Modo Automático activo." else "Servicio activo en modo pasivo."
        
        val notification = buildNotification(getString(R.string.notification_service_title), statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Solicitar actualizaciones de baja frecuencia para la UI cuando no se está grabando
        requestLocationUpdates(5000L, 2f)
    }

    private fun startTracking(routeName: String, routeId: Int, interval: Long, distance: Float) {
        if (_isTracking.value) return // Ya está en marcha

        // Adquirir WakeLock para asegurar procesamiento de GPS continuo
        wakeLock?.let {
            if (!it.isHeld) it.acquire(25 * 60 * 60 * 1000L)
        }

        // Limpiar buffer de pre-grabación si el usuario inicia manualmente
        preRecordingBuffer.clear()

        _isTracking.value = true
        activeRouteId = routeId
        _currentRouteId.value = routeId
        activeRouteName = routeName
        routeStartTimeMillis = System.currentTimeMillis()
        lastMovementTimeMillis = System.currentTimeMillis()
        isWaitingForMovement = false

        // Iniciar en primer plano mostrando la notificación obligatoria
        val notification = buildNotification(routeName, "Calculando ubicación...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            // Si la ruta ya existía, recuperamos su distancia histórica para continuar acumulando
            val existingRoute = repository.getRouteById(routeId)
            totalDistanceMeters = existingRoute?.distanceMeters ?: 0.0
            _currentDistanceMeters = totalDistanceMeters

            // Consultar las coordenadas previas para tener continuidad si es una reanudación
            val previousPoints = repository.getLocationPointsSync(routeId)
            if (previousPoints.isNotEmpty()) {
                val lastPoint = previousPoints.last()
                lastValidLocation = Location("gps").apply {
                    latitude = lastPoint.latitude
                    longitude = lastPoint.longitude
                    altitude = lastPoint.altitude
                }
            } else {
                lastValidLocation = null
            }

            requestLocationUpdates(interval, distance)
            startTimer()
            startGpsWatchdog()
        }
    }

    private fun requestLocationUpdates(interval: Long, distance: Float) {
        // Intervalos equilibrados para ahorrar batería y dar excelente precisión real
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            interval
        ).apply {
            setMinUpdateIntervalMillis(interval / 2) // Intervalo más rápido permisible
            setMinUpdateDistanceMeters(distance)    // Desplazamiento mínimo indispensable para registrar
            setMaxUpdateDelayMillis(0)                   // Anula el batching de Doze mode
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                locationHandlerThread?.looper ?: Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "No se admiten permisos necesarios para iniciar updates de ubicación GPS: $unlikely")
            _isTracking.value = false
            stopSelf()
        }
    }

    private fun processNewLocation(location: Location) {
        // Actualizamos siempre la ubicación "Live" para la UI
        _latestLocation.value = location

        // Marcar que el GPS sigue activo (antes de cualquier filtro)
        lastGpsUpdateTimeMillis = System.currentTimeMillis()
        // Watchdog: Monitoreo de salud GNSS para deteccion de perdida de senhal
        if (location.provider == LocationManager.GPS_PROVIDER) {
            lastGpsTickTime = SystemClock.elapsedRealtime()
            if (isSignalLost) {
                isSignalLost = false
                _currentActivityMode.value = preAlertActivityMode
                Log.d(TAG, "Señal GPS recuperada. Restaurando modo: $preAlertActivityMode")
            }
        }
        lastUserActivityTimeMillis = System.currentTimeMillis()

        // Fase 1: Precisión mínima - descartar puntos con accuracy > 50m
        if (location.accuracy > 50f) {
            Log.d(TAG, "Fase 1: Punto descartado por baja precisión: ${location.accuracy}m")
            return
        }

        // Fase 2: Filtrado y procesamiento cinemático y autostop
        val filteredLocation = cinematicFilter.filterAndProcess(location) ?: return

        // Anclaje estacionario: congelar posición si el usuario está quieto en interiores
        val anchoredLocation = stationaryAnchorManager.processAnchor(filteredLocation)

        // Guarda de contingencia: forzar parada si 3 min de precision degradada (>25m)
        if (stationaryAnchorManager.shouldForceStop(location)) {
            Log.w(TAG, "Guarda de 3min activada. Forzando parada automatica por degradacion de senhal en interiores.")
            stopTrackingWithAutoRestart()
            return
        }

        val isAutoModeActive = sharedPrefs.getBoolean("auto_mode", false)
        val isNearMeEnabled = sharedPrefs.getBoolean("near_me_enabled", false)

        // Calcular velocidad entre puntos usando lastValidLocation (más fiable que location.speed)
        lastComputedSpeedMps = if (lastValidLocation != null) {
            val dist = lastValidLocation!!.distanceTo(anchoredLocation)
            val time = location.time - lastValidLocation!!.time
            if (time > 0) dist / (time / 1000f) else 0f
        } else 0f

        // Actualizar lastRealMovementTime solo cuando hay velocidad real (Módulo 3C)
        if (lastComputedSpeedMps > AppConstants.Location.AUTO_STOP_SPEED_MPS) {
            lastRealMovementTime = System.currentTimeMillis()
        }

        // Actualizar Activity Mode con la velocidad computada
        updateActivityMode(lastComputedSpeedMps * 3.6f)

        // --- MARCADOR DE PARADAS CORTAS ---
        if (_isTracking.value) {
            val speedKmh = lastComputedSpeedMps * 3.6f
            if (speedKmh < AppConstants.Location.STOP_SPEED_THRESHOLD_KMH) {
                if (timeStartedStopping == 0L) {
                    timeStartedStopping = System.currentTimeMillis()
                }
                if (!isShortStopDetected &&
                    System.currentTimeMillis() - timeStartedStopping >= SHORT_STOP_DELAY_MS
                ) {
                    isShortStopDetected = true
                    shortStopLat = location.latitude
                    shortStopLon = location.longitude
                    serviceScope.launch {
                        val geocoder = GeocoderUseCase(this@LocationService)
                        val details = geocoder.fetchLocationDetails(shortStopLat, shortStopLon)
                        shortStopAddress = details.first
                    }
                }
            } else {
                if (isShortStopDetected) {
                    val stopDurationMs = System.currentTimeMillis() -
                        timeStartedStopping + STOP_TOLERANCE_MS
                    val totalSec = stopDurationMs / 1000L
                    val minutes = totalSec / 60
                    val seconds = totalSec % 60
                    val comment = "⏱️ Parada automática de ${minutes} min ${seconds} seg"
                    val finalAddress = shortStopAddress.ifBlank {
                        "GPS: ${String.format(Locale.US, "%.5f, %.5f", shortStopLat, shortStopLon)}"
                    }
                    serviceScope.launch {
                        repository.addSavedLocation(
                            name = finalAddress,
                            address = finalAddress,
                            latitude = shortStopLat,
                            longitude = shortStopLon,
                            comment = comment,
                            author = "Automático",
                        )
                    }
                }
                timeStartedStopping = 0L
                isShortStopDetected = false
                shortStopLat = 0.0
                shortStopLon = 0.0
                shortStopAddress = ""
            }
        }

        // --- LÓGICA "CERCA DE MÍ" ---
        if (isNearMeEnabled) {
            checkProximityToFavorites(location)
        }

        // --- BLE PROXIMITY (Módulo 3B) ---
        ensureBleProximity()

        // Lógica de detección de movimiento para auto-reinicio por VELOCIDAD SOSTENIDA (Módulo 3C)
        if (isAutoModeActive && !_isTracking.value) {
            val now = System.currentTimeMillis()

            // Guardar punto estabilizado en el buffer elástico de pre-grabación (Fase 3A)
            if (anchoredLocation.accuracy < MAX_GPS_ACCURACY) {
                handlePreRecording(anchoredLocation)
            }

            // Evitar reinicios demasiado frecuentes (mínimo 30 segundos entre intentos)
            if (now - lastAutoStartTimeMillis < 30000) return

            if (lastStopLocation == null) {
                if (location.accuracy < 20f) {
                    lastStopLocation = location
                    movementSustainedStartTime = 0L
                    lastSpeedCheckTime = now
                    Log.d(TAG, "Anclaje inicial establecido para velocidad sostenida: Acc=${location.accuracy}")
                }
            } else {
                val speedKmh = lastComputedSpeedMps * 3.6f

                if (speedKmh > AppConstants.Location.AUTO_START_MIN_SPEED_KMH && location.accuracy < 30f) {
                    if (movementSustainedStartTime == 0L) {
                        movementSustainedStartTime = now
                        lastSpeedCheckTime = now
                        Log.d(TAG, "Inicio de ventana de velocidad sostenida: ${speedKmh} km/h")
                    } else {
                        val sustained = now - movementSustainedStartTime
                        if (sustained >= AppConstants.Location.AUTO_START_SUSTAINED_MS) {
                            Log.d(TAG, "Movimiento sostenido ${sustained}ms. Iniciando pipeline de arranque pasivo...")
                            lastStopLocation = null
                            movementSustainedStartTime = 0L
                            lastAutoStartTimeMillis = now
                            
                            triggerAutomaticTracking(preRecordingBuffer) { filteredLocations ->
                                val pointsToBackfill = filteredLocations.map { it.toBufferPoint(0) }
                                startAutoNewRoute(pointsToBackfill)
                            }
                            return
                        }
                    }
                } else {
                    if (movementSustainedStartTime != 0L) {
                        Log.d(TAG, "Velocidad insuficiente (${speedKmh} km/h). Reiniciando ventana.")
                    }
                    movementSustainedStartTime = 0L
                }
            }
        }

        serviceScope.launch {
            try {
                val routeId = activeRouteId
                if (routeId == -1) return@launch

                val accuracy = anchoredLocation.accuracy

                // Capa A: Odómetro tolerante (hasta 50m para el bolsillo)
                if (accuracy <= TOLERANT_ACCURACY) {
                    val distanceStep = lastValidLocation?.distanceTo(anchoredLocation) ?: 0f
                    lastValidLocation = anchoredLocation
                    lastMovementTimeMillis = System.currentTimeMillis()

                    if (distanceStep > 0) {
                        totalDistanceMeters += distanceStep
                        _currentDistanceMeters = totalDistanceMeters

                        val currentRoute = repository.getRouteById(routeId)
                        if (currentRoute != null) {
                            repository.updateRoute(currentRoute.copy(distanceMeters = totalDistanceMeters))
                        }
                    }
                } else {
                    Log.d(TAG, "Punto descartado del odómetro por baja precisión: ${accuracy}")
                    return@launch
                }

                // Capa B: Guardado en DB solo con alta precisión (<=20m)
                if (accuracy <= MAX_GPS_ACCURACY) {
                    repository.addLocationPoint(
                        routeId = routeId,
                        latitude = anchoredLocation.latitude,
                        longitude = anchoredLocation.longitude,
                        altitude = anchoredLocation.altitude,
                        speed = anchoredLocation.speed
                    )
                }

                updateServiceNotification(routeId)
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando ubicación en base de datos: ${e.message}")
            }
        }
    }


    private fun updateActivityMode(speedKmh: Float) {
        val newMode = when {
            speedKmh < 1.0f -> ActivityMode.STOPPED
            speedKmh <= 7.0f -> ActivityMode.WALKING
            speedKmh <= 13.0f -> ActivityMode.BIKING
            else -> ActivityMode.DRIVING
        }

        if (newMode != currentMode) {
            val currentTime = System.currentTimeMillis()
            val isUpgrade = newMode.ordinal > currentMode.ordinal
            val requiredTime = if (isUpgrade) TIME_TO_UPGRADE else TIME_TO_DOWNGRADE

            if (lastModeChangeTime == 0L) {
                lastModeChangeTime = currentTime
            } else if (currentTime - lastModeChangeTime >= requiredTime) {
                currentMode = newMode
                lastModeChangeTime = 0L
                _currentActivityMode.value = newMode
                Log.d(TAG, "Modo actividad: $currentMode (${speedKmh} km/h)")
            }
        } else {
            lastModeChangeTime = 0L
        }
    }

    private fun checkProximityToFavorites(currentLocation: Location) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < NOTIFICATION_COOLDOWN_MS) return

        serviceScope.launch {
            try {
                // Obtener favoritos de la base de datos de forma síncrona/segura
                val favorites = repository.allSavedLocations.first()
                for (fav in favorites) {
                    val favLoc = Location("temp").apply {
                        latitude = fav.latitude
                        longitude = fav.longitude
                    }
                    val distance = currentLocation.distanceTo(favLoc)
                    
                    if (distance < PROXIMITY_THRESHOLD_METERS) {
                        if (fav.id != lastNotifiedFavoriteId || (now - lastNotificationTime > NOTIFICATION_COOLDOWN_MS)) {
                            sendProximityNotification(fav)
                            lastNotifiedFavoriteId = fav.id
                            lastNotificationTime = now
                            break // Solo una notificación por vez
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun sendProximityNotification(favorite: SavedLocation) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Canal especial para alertas si no existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("proximity_alerts", "Alertas de Proximidad", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "proximity_alerts")
            .setContentTitle("📍 Cerca de tu Estrella")
            .setContentText("Estás pasando por: ${favorite.name}")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(favorite.id + 2000, notification)
    }

    // ─── BLE Proximity (Módulo 3B) ─────────────────────────────────

    private fun ensureBleProximity() {
        val isNearMe = sharedPrefs.getBoolean("near_me_enabled", false)
        val isBleEnabled = sharedPrefs.getBoolean("ble_enabled", false)
        val pm = proximityManager ?: return

        if (isNearMe && isBleEnabled && pm.isBluetoothEnabled()) {
            if (!isBleActive) {
                isBleActive = true
                val myPhone = sharedPrefs.getString("my_phone", "").orEmpty()
                if (myPhone.isNotBlank()) {
                    pm.startAdvertising(ContactHashHelper.hashPhoneNumber(myPhone))
                }
                serviceScope.launch(Dispatchers.IO) {
                    val contacts = contactUseCase?.getAllContactPhones().orEmpty()
                    val contactHashes = contacts.map { (_, phone) ->
                        ContactHashHelper.hashPhoneNumber(phone)
                    }.toSet()
                    pm.updateScanHashes(contactHashes)
                    pm.startScanning()
                }
            }
        } else {
            if (isBleActive) {
                isBleActive = false
                pm.stop()
            }
        }
    }

    private fun onBleContactDetected(contactName: String, distance: Double) {
        val now = System.currentTimeMillis()
        if (now - lastBleNotificationTime < AppConstants.Ble.NOTIFICATION_COOLDOWN_MILLIS) return
        lastBleNotificationTime = now

        alertNotifier?.triggerProximityAlert()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("proximity_alerts", "Alertas de Proximidad", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "proximity_alerts")
            .setContentTitle("👤 Contacto cercano")
            .setContentText("$contactName está a ~${distance.toInt()}m")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(3001, notification)
    }

    private fun updateServiceNotification(routeId: Int) {
        serviceScope.launch {
            val route = repository.getRouteById(routeId) ?: return@launch
            val km = totalDistanceMeters / 1000.0
            val formattedDistance = String.format(Locale.US, "%.2f km", km)
            val formattedTime = formatElapsedTime(_elapsedTimeSeconds)

            val contentText = "Distancia: $formattedDistance | Tiempo: $formattedTime"
            val notification = buildNotification(route.name, contentText)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        startTimeMillis = System.currentTimeMillis() - (_elapsedTimeSeconds * 1000L)
        timerJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = (now - startTimeMillis) / 1000L
                _elapsedTimeSeconds = elapsed

                // COMPROBACIÓN 1: Inactividad (15 minutos) - SOLO SI EL USUARIO QUIERE AUTO-MODE
                val isAutoModeActive = sharedPrefs.getBoolean("auto_mode", false)

                val effectiveLastActivity = maxOf(lastRealMovementTime, lastUserActivityTimeMillis)
                if (isAutoModeActive && _isTracking.value && (now - effectiveLastActivity) > AppConstants.Location.INACTIVITY_THRESHOLD_MILLIS) {
                    Log.d(TAG, "Inactividad detectada. Auto-guardando ruta.")
                    stopTrackingWithAutoRestart()
                }

                // COMPROBACIÓN 2: Límite diario (24 horas)
                if (_isTracking.value && (now - routeStartTimeMillis) > 24 * 60 * 60 * 1000) {
                    Log.d(TAG, "Límite de 24hs alcanzado. Rotando segmento de ruta.")
                    rotateDailyRoute()
                }

                // COMPROBACIÓN 3: Watchdog GPS silencioso (Doze batching)
                if (!_isTracking.value && (now - lastGpsUpdateTimeMillis) > 120_000L) {
                    Log.d(TAG, "GPS silencioso por " + ((now - lastGpsUpdateTimeMillis) / 1000L) + "s. Reiniciando canal de ubicación.")
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    requestLocationUpdates(5000L, 2f)
                }

                delay(1.seconds) // Revisar cada 1 segundo
            }
        }
    }


    /**
     * Watchdog de Seguridad GNSS. Monitorea la salud del satelite en segundo plano.
     * Evalua cada 5s si pasaron 15s sin actualizacion del GPS_PROVIDER.
     */
    private fun startGpsWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            while (isActive) {
                delay(5.seconds)
                if (lastGpsTickTime > 0 && _currentActivityMode.value != ActivityMode.STOPPED && _currentActivityMode.value != ActivityMode.SIN_SENAL) {
                    val timeSinceLastFix = SystemClock.elapsedRealtime() - lastGpsTickTime
                    if (timeSinceLastFix > 15000L && !isSignalLost) {
                        isSignalLost = true
                        preAlertActivityMode = _currentActivityMode.value
                        _currentActivityMode.value = ActivityMode.SIN_SENAL
                        Log.w(TAG, "Senhal GPS perdida por " + (timeSinceLastFix/1000) + "s. Modo conmutado a SIN_SENAL.")
                    }
                }
            }
        }
    }
    private fun stopTrackingWithAutoRestart() {
        lastStopLocation = _latestLocation.value
        isWaitingForMovement = true
        lastUserActivityTimeMillis = System.currentTimeMillis()
        stopTracking()
    }

    private fun rotateDailyRoute() {
        serviceScope.launch {
            // Guardar la actual
            if (activeRouteId != -1) {
                repository.stopActiveRoute(activeRouteId, totalDistanceMeters, currentMode.name)
            }
            // Iniciar una nueva inmediatamente
            startAutoNewRoute()
        }
    }

    private fun triggerAutomaticTracking(
        preRecordingBuffer: ConcurrentLinkedDeque<Location>,
        onHistoryProcessed: (List<Location>) -> Unit
    ) {
        serviceScope.launch(Dispatchers.IO) {
            // 1. Extraer instantánea del buffer pasivo (concurrente)
            val rawHistory = mutableListOf<Location>()
            var loc = preRecordingBuffer.pollFirst()
            while (loc != null) {
                rawHistory.add(loc)
                loc = preRecordingBuffer.pollFirst()
            }

            // 2. Aplicar filtro cinemático lineal con una ventana limpia para depurar la traza inicial
            val cleanCinematicFilter = com.aistudio.localizador.location.manager.CinematicFilterManager(
                maxAllowedSpeedMps = AppConstants.Location.MAX_SPEED_METERS_PER_SECOND.toDouble(),
                autoStopSpeedMps = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()
            )
            val filteredHistory = rawHistory.mapNotNull { location ->
                if (location.accuracy <= MAX_GPS_ACCURACY) {
                    cleanCinematicFilter.filterAndProcess(location)
                } else {
                    null
                }
            }

            // 3. Regresar al hilo principal para conmutar el hardware e iniciar la ruta
            withContext(Dispatchers.Main) {
                onHistoryProcessed(filteredHistory)
            }
        }
    }

    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {
        serviceScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val newName = "Auto-Ruta $dateStr"
            val newRouteId = repository.startNewRoute(newName)
            
            // Insertar los puntos del buffer (backfill) asociados a la nueva ruta
            if (backfillPoints.isNotEmpty()) {
                // 🛡️ Copia inmutable defensiva en un solo paso funcional para evitar fugas de memoria
                val preparedPoints = backfillPoints.map { point ->
                    point.copy(
                        routeId = newRouteId,
                        id = 0 // Forzamos a Room a autogenerar una clave primaria limpia por punto
                    )
                }.toList()

                serviceScope.launch(Dispatchers.IO) {
                    repository.addLocationPoints(preparedPoints)
                }
                
                // Recalcular distancia inicial si hay puntos en el buffer
                var initialDistance = 0.0
                for (i in 0 until preparedPoints.size - 1) {
                    val p1 = preparedPoints[i]
                    val p2 = preparedPoints[i+1]
                    val results = FloatArray(1)
                    Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
                    initialDistance += results[0].toDouble()
                }
                totalDistanceMeters = initialDistance
            } else {
                totalDistanceMeters = 0.0
            }

            withContext(Dispatchers.Main) {
                // Limpiar contadores pero mantener el servicio vivo
                _elapsedTimeSeconds = 0L
                _currentDistanceMeters = totalDistanceMeters
                lastValidLocation = null
                
                // Reiniciar seguimiento con el nuevo ID
                _isTracking.value = false // Forzar reinicio en startTracking
                startTracking(newName, newRouteId, 5000L, 2f)
            }
        }
    }

    private fun pauseTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        watchdogJob?.cancel()
        isSignalLost = false
        _isTracking.value = false
        timeStartedStopping = 0L
        isShortStopDetected = false
        shortStopLat = 0.0
        shortStopLon = 0.0
        shortStopAddress = ""
        
        // 🛡️ DOBLE CANDADO ATÓMICO: Reinicio inmediato y síncrono de velocidad y estado
        _currentActivityMode.value = ActivityMode.STOPPED
        currentMode = ActivityMode.STOPPED
        _latestLocation.value = _latestLocation.value?.let { loc ->
            Location(loc).apply { speed = 0f }
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        watchdogJob?.cancel()

        // Liberar WakeLock
        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (e: Exception) { Log.e(TAG, "Error liberando WakeLock: ${e.message}") }
        }

        serviceScope.launch {
            if (activeRouteId != -1) {
                repository.stopActiveRoute(activeRouteId, totalDistanceMeters, currentMode.name)
            }

            // Reiniciar estados una vez confirmado el guardado
            withContext(Dispatchers.Main) {
                _isTracking.value = false
                activeRouteId = -1
                _currentRouteId.value = null
                
                // 🛡️ DOBLE CANDADO ATÓMICO: Mantener última ubicación en el mapa pero con velocidad cero
                _latestLocation.value = _latestLocation.value?.let { loc ->
                    Location(loc).apply { speed = 0f }
                }
                
                timeStartedStopping = 0L
                isShortStopDetected = false
                shortStopLat = 0.0
                shortStopLon = 0.0
                shortStopAddress = ""
                stationaryAnchorManager.reset()
                cinematicFilter.reset()
                _elapsedTimeSeconds = 0L
                _currentDistanceMeters = 0.0

                // Purgar alertas y restablecer el modo síncronamente
                isSignalLost = false
                _currentActivityMode.value = ActivityMode.STOPPED
                currentMode = ActivityMode.STOPPED

                // No llamamos a stopSelf(). El servicio se mantiene en modo pasivo.
                startPassiveTracking()
            }
        }
    }

    private fun handleAutoStartFromTransition(activityType: Int) {
        if (activityType == -1) return
        val autoModeActive = sharedPrefs.getBoolean("auto_mode", false)
        if (autoModeActive && !_isTracking.value) {
            Log.i(TAG, "Arranque Automatico Instantaneo provocado por transicion tipo: $activityType")
            startAutoNewRoute()
        }
    }

    private fun shutdownService() {
        Log.d(TAG, "Iniciando apagado total del servicio.")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        activityRecognitionManager?.stopTrackingTransitions()
        
        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (_: Exception) { }
        }

        serviceScope.launch {
            if (activeRouteId != -1) {
                repository.stopActiveRoute(activeRouteId, totalDistanceMeters, currentMode.name)
            }
            withContext(Dispatchers.Main) {
                _isTracking.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun ensureForeground() {
        val notification = buildNotification(getString(R.string.notification_service_title), getString(R.string.notification_service_description))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) 
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener y Guardar",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_service_title)
            val descriptionText = getString(R.string.notification_service_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatElapsedTime(seconds: Long): String {
        val hr = TimeUnit.SECONDS.toHours(seconds)
        val min = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val sec = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hr, min, sec)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        watchdogJob?.cancel()
        serviceScope.cancel()

        // Liberar WakeLock si aún está activo
        if (wakeLock?.isHeld == true) {
            try { wakeLock?.release() } catch (_: Exception) { }
        }

        // Liberar el HandlerThread para evitar fugas de memoria
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null

        // Liberar BLE Proximity (Módulo 3B)
        proximityManager?.stop()
        alertNotifier?.release()

        // Desregistrar receiver de pantalla
        try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) { }

        super.onDestroy()
    }
}







