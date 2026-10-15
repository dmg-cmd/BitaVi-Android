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
import com.aistudio.localizador.ui.GeocoderUseCase
import com.aistudio.localizador.util.AppConstants
import com.aistudio.localizador.data.model.LocationPoint
import com.aistudio.localizador.data.model.Route
import com.aistudio.localizador.data.repository.RouteRepository
import androidx.preference.PreferenceManager
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.location.manager.StationaryAnchorManager
import com.aistudio.localizador.location.manager.CinematicFilterManager
import com.aistudio.localizador.location.manager.KineticProfile
import com.aistudio.localizador.location.manager.GpsFilterPipeline
import com.aistudio.localizador.location.manager.ActivityRecognitionManager
import com.aistudio.localizador.location.manager.ServiceNotificationManager
import com.aistudio.localizador.location.manager.TrackingSessionManager
import com.aistudio.localizador.location.manager.ActivityModeManager
import com.aistudio.localizador.location.manager.AutoTrackingManager
import com.aistudio.localizador.location.manager.ShortStopMarkManager
import com.aistudio.localizador.location.manager.ProximityBleManager
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
@AndroidEntryPoint
class LocationService : Service(), AutoTrackingManager.AutoTrackingListener, ProximityBleManager.ProximityListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Inject lateinit var repository: RouteRepository
    @Inject lateinit var activityModeManager: ActivityModeManager


    @Inject
    lateinit var autoTrackingManager: AutoTrackingManager
    @Inject
    lateinit var proximityBleManager: ProximityBleManager
    @Inject lateinit var shortStopMarkManager: ShortStopMarkManager
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

    private var lastValidLocation: Location? = null
    private val notificationManager = ServiceNotificationManager(this)

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


    @Volatile private var lastRealMovementTime = System.currentTimeMillis()

    private var lastComputedSpeedMps = 0f

    // Lógica "Cerca de mí"
    private var lastNotifiedFavoriteId: Int = -1
    private var lastNotificationTime: Long = 0L
    private val PROXIMITY_THRESHOLD_METERS = 100f
    private val NOTIFICATION_COOLDOWN_MS = 10 * 60 * 1000L // 10 minutos para repetir aviso

    private var activityRecognitionManager: ActivityRecognitionManager? = null
    @Inject lateinit var contactUseCase: ContactUseCase
    @Inject lateinit var geocoderUseCase: GeocoderUseCase
    @Inject lateinit var sessionManager: TrackingSessionManager
    @Inject lateinit var filterPipeline: GpsFilterPipeline

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

        private val _kineticProfile = MutableStateFlow(KineticProfile.TERRESTRIAL)
        val kineticProfile = _kineticProfile.asStateFlow()
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

        proximityBleManager.init(this, sharedPrefs, contactUseCase)

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

        autoTrackingManager.setListener(this)
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
        sessionManager.clearPreRecordingBuffer()

        _isTracking.value = true
        sessionManager.startSession(routeId.toLong())
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
            sessionManager.setInitialDistance(existingRoute?.distanceMeters ?: 0.0)
            _currentDistanceMeters = sessionManager.getTotalDistance()

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

        // Fase 1: Precisión mínima - umbral dinámico según perfil cinemático (Vuelo: 80m)
        val kineticProfile = filterPipeline.getCurrentProfile()
        _kineticProfile.value = kineticProfile
        val maxInitialAccuracy = if (kineticProfile == KineticProfile.FLIGHT) {
            AppConstants.Location.FLIGHT_MAX_ACCURACY_METERS
        } else {
            AppConstants.Location.MAX_POCKET_ACCURACY_METERS
        }
        if (location.accuracy > maxInitialAccuracy) {
            Log.d(TAG, "Fase 1: Punto descartado por baja precisión: ${location.accuracy}m (perfil: $kineticProfile)")
            return
        }

        // Fase 2: Pipeline unificado de filtrado cinemático y anclaje estacionario (JVM-Safe)
        val rawBp = TrackingSessionManager.BufferPoint(location.latitude, location.longitude, location.accuracy, location.speed, location.altitude, location.time)
        val processedBp = filterPipeline.processLocation(rawBp) ?: return
        val anchoredLocation = location.apply { latitude = processedBp.latitude; longitude = processedBp.longitude }

        // Guarda de contingencia: forzar parada si 3 min de precision degradada (>25m).
        // Suprimida en perfil FLIGHT para no cortar rutas aéreas por atenuación de la fuselaje.
        if (kineticProfile != KineticProfile.FLIGHT && filterPipeline.shouldForceStop(location)) {
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

        // Actualizar Activity Mode con la velocidad computada (delegado a ActivityModeManager)
        val newMode = activityModeManager.evaluate(
            speedKmh = lastComputedSpeedMps * 3.6f,
            timestamp = System.currentTimeMillis(),
            upgradeMs = TIME_TO_UPGRADE,
            downgradeMs = TIME_TO_DOWNGRADE
        )
        if (newMode != null) { _currentActivityMode.value = newMode }

        // --- MARCADOR DE PARADAS CORTAS ---
        if (_isTracking.value) {
            shortStopMarkManager.processCinematicInput(
                location = location,
                speedKmh = lastComputedSpeedMps * 3.6f,
                timestamp = System.currentTimeMillis(),
                shortStopDelayMs = SHORT_STOP_DELAY_MS,
                stopToleranceMs = STOP_TOLERANCE_MS
            )
        }

        // --- LÓGICA "CERCA DE MÍ" ---
        if (isNearMeEnabled) {
            checkProximityToFavorites(location)
        }

        // --- BLE PROXIMITY (delegado a ProximityBleManager) ---
        proximityBleManager.ensureBleProximity(isServiceTracking = _isTracking.value)
        // LOGICA DE MOVIMIENTO SOSTENIDO (delegada a AutoTrackingManager)
        if (isAutoModeActive && !_isTracking.value) {
            autoTrackingManager.processMovementSustained(
                location = anchoredLocation,
                computedSpeedMps = lastComputedSpeedMps,
                isAutoModeActive = isAutoModeActive,
                isTracking = _isTracking.value,
                sessionManager = sessionManager
            )
        }

        serviceScope.launch {
            try {
                val routeId = (sessionManager.getActiveRouteId() ?: -1L).toInt()
                if (routeId == -1) return@launch

                val accuracy = anchoredLocation.accuracy

                // Capa A: Odómetro tolerante (hasta 50m bolsillo / 80m vuelo)
                val odometerAccuracyLimit = if (kineticProfile == KineticProfile.FLIGHT) {
                    AppConstants.Location.FLIGHT_MAX_ACCURACY_METERS
                } else {
                    TOLERANT_ACCURACY
                }
                if (accuracy <= odometerAccuracyLimit) {
                    val distanceStep = lastValidLocation?.distanceTo(anchoredLocation) ?: 0f
                    lastValidLocation = anchoredLocation
                    lastMovementTimeMillis = System.currentTimeMillis()

                    if (distanceStep > 0) {
                        sessionManager.addDistanceFromStep(distanceStep.toDouble())
                        _currentDistanceMeters = sessionManager.getTotalDistance()

                        val currentRoute = repository.getRouteById(routeId)
                        if (currentRoute != null) {
                            repository.updateRoute(currentRoute.copy(distanceMeters = sessionManager.getTotalDistance()))
                        }
                    }
                } else {
                    Log.d(TAG, "Punto descartado del odómetro por baja precisión: ${accuracy}")
                    return@launch
                }

                // Capa B: Guardado en DB con precisión dinámica (<=20m terrestre / <=80m vuelo)
                val dbAccuracyLimit = if (kineticProfile == KineticProfile.FLIGHT) {
                    AppConstants.Location.FLIGHT_MAX_ACCURACY_METERS
                } else {
                    MAX_GPS_ACCURACY
                }
                if (accuracy <= dbAccuracyLimit) {
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

    override fun onBleContactDetected(contactName: String, distanceMeters: Double) {
        Log.d("Bitaru_Core", "Contacto BLE detectado: $contactName a ${distanceMeters.toInt()}m")
    }
    private fun updateServiceNotification(routeId: Int) {
        serviceScope.launch {
            val route = repository.getRouteById(routeId) ?: return@launch
            val km = sessionManager.getTotalDistance() / 1000.0
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
        autoTrackingManager.setAutoRestartAnchor(_latestLocation.value)
        isWaitingForMovement = true
        lastUserActivityTimeMillis = System.currentTimeMillis()
        stopTracking()
    }


    override fun onMovementSustainedTriggered(snapshot: List<TrackingSessionManager.BufferPoint>) {
        Log.i(TAG, "AutoTrackingManager: Movimiento sostenido detectado. Iniciando pipeline...")
        triggerAutomaticTracking(snapshot) { filteredBpList ->
            val pointsToBackfill = filteredBpList.map { bp ->
                LocationPoint(id = 0, routeId = 0,
                    latitude = bp.latitude, longitude = bp.longitude,
                    speed = bp.speed, altitude = bp.altitude,
                    timestamp = bp.timestamp
                )
            }
            startAutoNewRoute(pointsToBackfill)
        }
    }

    private fun rotateDailyRoute() {
        serviceScope.launch {
            // Guardar la actual
            if (sessionManager.getActiveRouteId() != null) {
                repository.stopActiveRoute(sessionManager.getActiveRouteId()!!.toInt(), sessionManager.getTotalDistance(), activityModeManager.getCurrentMode().name)
            }
            // Iniciar una nueva inmediatamente
            startAutoNewRoute()
        }
    }

    private fun triggerAutomaticTracking(
        bufferSnapshot: List<TrackingSessionManager.BufferPoint>,
        onHistoryProcessed: (List<TrackingSessionManager.BufferPoint>) -> Unit
    ) {
        if (bufferSnapshot.isEmpty()) return

        serviceScope.launch(Dispatchers.IO) {
            val cleanPipeline = GpsFilterPipeline(
                CinematicFilterManager(
                    maxAllowedSpeedMps = AppConstants.Location.MAX_SPEED_METERS_PER_SECOND.toDouble(),
                    autoStopSpeedMps = AppConstants.Location.AUTO_STOP_SPEED_MPS.toDouble()
                ),
                StationaryAnchorManager()
            )

            val filteredHistory = bufferSnapshot.mapNotNull { bp ->
                cleanPipeline.processLocation(bp)
            }

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
            // Insertar los puntos del buffer (backfill) o la última ubicación actual asociada a la nueva ruta
            val pointsToInsert = if (backfillPoints.isNotEmpty()) {
                backfillPoints.map { point ->
                    point.copy(
                        routeId = newRouteId,
                        id = 0
                    )
                }
            } else {
                _latestLocation.value?.let { loc ->
                    listOf(
                        LocationPoint(
                            id = 0,
                            routeId = newRouteId,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            speed = loc.speed,
                            altitude = loc.altitude,
                            timestamp = loc.time
                        )
                    )
                } ?: emptyList()
            }

            if (pointsToInsert.isNotEmpty()) {
                serviceScope.launch(Dispatchers.IO) {
                    repository.addLocationPoints(pointsToInsert)
                }
                var initialDistance = 0.0
                for (i in 0 until pointsToInsert.size - 1) {
                    val p1 = pointsToInsert[i]
                    val p2 = pointsToInsert[i+1]
                    val results = FloatArray(1)
                    Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
                    initialDistance += results[0].toDouble()
                }
                sessionManager.setInitialDistance(initialDistance)
            } else {
                sessionManager.setInitialDistance(0.0)
            }

            withContext(Dispatchers.Main) {
                // Limpiar contadores pero mantener el servicio vivo
                _elapsedTimeSeconds = 0L
                        _currentDistanceMeters = sessionManager.getTotalDistance()
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
        shortStopMarkManager.resetEvaluation()
        // 🛡️ DOBLE CANDADO ATÓMICO: Reinicio inmediato y síncrono de velocidad y estado
        _currentActivityMode.value = ActivityMode.STOPPED
        activityModeManager.reset()
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
            if (sessionManager.getActiveRouteId() != null) {
                repository.stopActiveRoute(sessionManager.getActiveRouteId()!!.toInt(), sessionManager.getTotalDistance(), activityModeManager.getCurrentMode().name)
            }

            // Reiniciar estados una vez confirmado el guardado
            withContext(Dispatchers.Main) {
                _isTracking.value = false
                sessionManager.stopSession()
                _currentRouteId.value = null
                // 🛡️ DOBLE CANDADO ATÓMICO: Mantener última ubicación en el mapa pero con velocidad cero
                _latestLocation.value = _latestLocation.value?.let { loc ->
                    Location(loc).apply { speed = 0f }
                }
                filterPipeline.reset()
                _elapsedTimeSeconds = 0L
                _currentDistanceMeters = 0.0

                // Purgar alertas y restablecer el modo síncronamente
                isSignalLost = false
                _currentActivityMode.value = ActivityMode.STOPPED
                activityModeManager.reset()

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
            if (sessionManager.getActiveRouteId() != null) {
                repository.stopActiveRoute(sessionManager.getActiveRouteId()!!.toInt(), sessionManager.getTotalDistance(), activityModeManager.getCurrentMode().name)
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
        activityModeManager.reset()
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

        // Liberar BLE Proximity (delegado a ProximityBleManager)
        proximityBleManager.clearAll()

        // Desregistrar receiver de pantalla
        try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) { }

        shortStopMarkManager.clearAll()
        autoTrackingManager.clearAll()

        super.onDestroy()
    }
}




