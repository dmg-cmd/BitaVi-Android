package com.example.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.repository.RouteRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Servicio en primer plano (Foreground Service) que gestiona el seguimiento GPS en tiempo real.
 * Diseñado con persistencia interna reactiva (Room) y variables estáticas observables en Compose.
 */
class LocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: RouteRepository

    private lateinit var sharedPrefs: SharedPreferences
    private var locationHandlerThread: HandlerThread? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var activeRouteId: Int = -1
    private var totalDistanceMeters: Double = 0.0
    private var lastRecordedLocation: Location? = null
    private var lastStopLocation: Location? = null

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L
    private var lastMovementTimeMillis: Long = System.currentTimeMillis()
    private var routeStartTimeMillis: Long = 0L
    private var isWaitingForMovement: Boolean = false
    private var activeRouteName: String = "Ruta"
    
    // Buffer para no perder los primeros metros (Pre-grabación)
    private val preStartBuffer = mutableListOf<LocationPoint>()
    private val BUFFER_MAX_TIME_MS = 30 * 60 * 1000L // 30 minutos

    // Variables para refinamiento de Modo Automático
    private var consecutiveMovements = 0
    private val REQUIRED_CONFIRMATIONS = 3 // Subimos a 3 lecturas seguidas para mayor seguridad
    private var lastAutoStartTimeMillis: Long = 0L // Para evitar reinicios infinitos rápidos

    // Lógica "Cerca de mí"
    private var lastNotifiedFavoriteId: Int = -1
    private var lastNotificationTime: Long = 0L
    private val PROXIMITY_THRESHOLD_METERS = 100f
    private val NOTIFICATION_COOLDOWN_MS = 10 * 60 * 1000L // 10 minutos para repetir aviso

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1205

        // Acciones de control del servicio
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_START_PASSIVE = "ACTION_START_PASSIVE"
        const val ACTION_UPDATE_SETTINGS = "ACTION_UPDATE_SETTINGS"

        // Claves para Intent
        const val EXTRA_ROUTE_NAME = "EXTRA_ROUTE_NAME"
        const val EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID"
        const val EXTRA_UPDATE_INTERVAL = "EXTRA_UPDATE_INTERVAL"
        const val EXTRA_UPDATE_DISTANCE = "EXTRA_UPDATE_DISTANCE"

        // Estado global expuesto en tiempo real para Compose
        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _currentRouteId = MutableStateFlow<Int?>(null)
        val currentRouteId = _currentRouteId.asStateFlow()

        private val _latestLocation = MutableStateFlow<Location?>(null)
        val latestLocation = _latestLocation.asStateFlow()

        private val _elapsedTimeSeconds = MutableStateFlow(0L)
        val elapsedTimeSeconds = _elapsedTimeSeconds.asStateFlow()

        private val _currentDistanceMeters = MutableStateFlow(0.0)
        val currentDistanceMeters = _currentDistanceMeters.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext)
        repository = RouteRepository(database.routeDao())

        // Cache de SharedPreferences para evitar I/O repetitivo en el Main Thread
        sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

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

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand acción recibida: $action")

        try {
            when (action) {
                ACTION_START_PASSIVE -> {
                    startPassiveTracking()
                }
                ACTION_START -> {
                    val routeName = intent?.getStringExtra(EXTRA_ROUTE_NAME) ?: "Ruta sin nombre"
                    val routeId = intent?.getIntExtra(EXTRA_ROUTE_ID, -1) ?: -1
                    val interval = intent?.getLongExtra(EXTRA_UPDATE_INTERVAL, 5000L) ?: 5000L
                    val distance = intent?.getFloatExtra(EXTRA_UPDATE_DISTANCE, 2f) ?: 2f
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onStartCommand: ${e.message}")
            e.printStackTrace()
        }

        return START_STICKY
    }

    private fun startPassiveTracking() {
        // Al iniciar en modo pasivo con Auto-Mode, nos ponemos en primer plano para que Android no nos mate
        val isAutoModeActive = sharedPrefs.getBoolean("auto_mode", false)
        
        if (isAutoModeActive) {
            val notification = buildNotification("Localizador", "Vigilando movimiento... Modo Automático activo.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        // Solicitar actualizaciones de baja frecuencia para la UI cuando no se está grabando
        requestLocationUpdates(10000L, 5f)
    }

    private fun startTracking(routeName: String, routeId: Int, interval: Long, distance: Float) {
        if (_isTracking.value) return // Ya está en marcha

        // Limpiar buffer de pre-grabación si el usuario inicia manualmente
        preStartBuffer.clear()

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
            _currentDistanceMeters.value = totalDistanceMeters

            // Consultar las coordenadas previas para tener continuidad si es una reanudación
            val previousPoints = repository.getLocationPointsSync(routeId)
            if (previousPoints.isNotEmpty()) {
                val lastPoint = previousPoints.last()
                lastRecordedLocation = Location("gps").apply {
                    latitude = lastPoint.latitude
                    longitude = lastPoint.longitude
                    altitude = lastPoint.altitude
                }
            } else {
                lastRecordedLocation = null
            }

            requestLocationUpdates(interval, distance)
            startTimer()
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

        val isAutoModeActive = sharedPrefs.getBoolean("auto_mode", false)
        val isNearMeEnabled = sharedPrefs.getBoolean("near_me_enabled", false)

        // --- LÓGICA "CERCA DE MÍ" ---
        if (isNearMeEnabled) {
            checkProximityToFavorites(location)
        }

        // Lógica de detección de movimiento para auto-reinicio (REFINADA con Buffer)
        if (isAutoModeActive && !_isTracking.value) {
            val now = System.currentTimeMillis()
            
            // 1. Guardar punto en el buffer temporal (Pre-grabación)
            if (location.accuracy < 50f) {
                val tempPoint = LocationPoint(
                    routeId = 0, // Aún no tiene ID real
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = location.speed,
                    timestamp = now
                )
                preStartBuffer.add(tempPoint)
                
                // 2. Limpiar buffer de puntos viejos (>30 min)
                val cutoff = now - BUFFER_MAX_TIME_MS
                preStartBuffer.removeAll { p -> p.timestamp < cutoff }
            }

            // Evitar reinicios demasiado frecuentes (mínimo 30 segundos entre intentos)
            if (now - lastAutoStartTimeMillis < 30000) return

            if (lastStopLocation == null) {
                // Anclaje inicial: requerimos buena precisión para tener un punto de referencia sólido
                if (location.accuracy < 20f) {
                    lastStopLocation = location
                    consecutiveMovements = 0
                    Log.d(TAG, "Anclaje inicial establecido para Modo Automático: Acc=${location.accuracy}")
                }
            } else {
                val distFromStop = lastStopLocation!!.distanceTo(location)
                val currentSpeedKmh = location.speed * 3.6f
                
                // Si nos alejamos más de 30 metros Y tenemos velocidad real (> 1.5 km/h)
                if (distFromStop > 30f && currentSpeedKmh > 1.5f && location.accuracy < 30f) {
                    consecutiveMovements++
                    Log.d(TAG, "Movimiento real detectado: $distFromStop m, $currentSpeedKmh km/h. Confirmación $consecutiveMovements/$REQUIRED_CONFIRMATIONS")
                    
                    if (consecutiveMovements >= REQUIRED_CONFIRMATIONS) {
                        Log.d(TAG, "Movimiento confirmado. Iniciando grabación automática con buffer de ${preStartBuffer.size} puntos.")
                        lastStopLocation = null 
                        consecutiveMovements = 0
                        lastAutoStartTimeMillis = now
                        
                        val pointsToBackfill = preStartBuffer.toList()
                        preStartBuffer.clear()
                        startAutoNewRoute(pointsToBackfill)
                        return
                    }
                } else if (distFromStop < 15f && location.accuracy < 25f) {
                    // Si volvemos a estar cerca del punto de parada con buena precisión, reseteamos contador
                    if (consecutiveMovements > 0) {
                        Log.d(TAG, "Movimiento descartado (regreso a zona de calma).")
                        consecutiveMovements = 0
                    }
                }
            }
        }

        // Filtro de precisión estricto para evitar "saltos" de GPS (Drift)
        // Reducimos el umbral a 40m durante la grabación para ignorar rebotes en edificios
        val accuracyThreshold = if (lastRecordedLocation == null) 25f else 40f
        
        if (location.accuracy > accuracyThreshold) {
            Log.d(TAG, "Ubicación descartada por baja precisión: ${location.accuracy}m")
            return
        }

        serviceScope.launch {
            try {
                val routeId = activeRouteId
                if (routeId == -1) return@launch

                // --- FILTRO DE VEROSIMILITUD (SANITY CHECK) ---
                lastRecordedLocation?.let { last ->
                    val distanceStep = last.distanceTo(location)
                    val timeStepSeconds = (location.time - last.time) / 1000.0
                    
                    if (timeStepSeconds > 0) {
                        val instantSpeedKmh = (distanceStep / timeStepSeconds) * 3.6
                        
                        // Leer Modo Avión desde SharedPreferences
                        val isAirplane = sharedPrefs.getBoolean("airplane_mode", false)
                        val maxAllowedSpeed = if (isAirplane) 1250.0 else 180.0
                        
                        if (instantSpeedKmh > maxAllowedSpeed) {
                            Log.w(TAG, "SALTO DE GPS DETECTADO: Velocidad imposible de ${instantSpeedKmh}km/h. Punto descartado.")
                            return@launch
                        }
                    }

                    // Si el movimiento es significativo (> 2m) o hay velocidad real, actualizamos el tiempo de último movimiento
                    if (distanceStep > 2.0 || location.speed > 0.4f) {
                        lastMovementTimeMillis = System.currentTimeMillis()
                    }

                    // Filtrar ruidos estáticos mínimos y saltos grandes residuales
                    if (distanceStep > 2.0 && distanceStep < 500) {
                        totalDistanceMeters += distanceStep
                        _currentDistanceMeters.value = totalDistanceMeters

                        val currentRoute = repository.getRouteById(routeId)
                        if (currentRoute != null) {
                            repository.updateRoute(currentRoute.copy(distanceMeters = totalDistanceMeters))
                        }
                    } else if (distanceStep >= 500) {
                        Log.w(TAG, "Punto descartado por salto excesivo (>500m).")
                        return@launch
                    }
                }

                lastRecordedLocation = location

                // Guardar punto GPS en base de datos local
                repository.addLocationPoint(
                    routeId = routeId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = location.speed
                )

                // Actualizar notificación con estadísticas en vivo
                updateNotification(routeId)
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
            } catch (e: Exception) { }
        }
    }

    private fun sendProximityNotification(favorite: com.example.data.model.SavedLocation) {
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

    private fun updateNotification(routeId: Int) {
        serviceScope.launch {
            val route = repository.getRouteById(routeId) ?: return@launch
            val km = totalDistanceMeters / 1000.0
            val formattedDistance = String.format("%.2f km", km)
            val formattedTime = formatElapsedTime(_elapsedTimeSeconds.value)

            val contentText = "Distancia: $formattedDistance | Tiempo: $formattedTime"
            val notification = buildNotification(route.name, contentText)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        startTimeMillis = System.currentTimeMillis() - (_elapsedTimeSeconds.value * 1000L)
        timerJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = (now - startTimeMillis) / 1000L
                _elapsedTimeSeconds.value = elapsed

                // COMPROBACIÓN 1: Inactividad (15 minutos) - SOLO SI EL USUARIO QUIERE AUTO-MODE
                val isAutoModeActive = sharedPrefs.getBoolean("auto_mode", false)

                if (isAutoModeActive && _isTracking.value && (now - lastMovementTimeMillis) > 15 * 60 * 1000) {
                    Log.d(TAG, "Inactividad detectada. Auto-guardando ruta.")
                    stopTrackingWithAutoRestart()
                }

                // COMPROBACIÓN 2: Límite diario (24 horas)
                if (_isTracking.value && (now - routeStartTimeMillis) > 24 * 60 * 60 * 1000) {
                    Log.d(TAG, "Límite de 24hs alcanzado. Rotando segmento de ruta.")
                    rotateDailyRoute()
                }

                delay(5000L) // Revisar cada 5 segundos
            }
        }
    }

    private fun stopTrackingWithAutoRestart() {
        lastStopLocation = _latestLocation.value
        isWaitingForMovement = true
        stopTracking()
    }

    private fun rotateDailyRoute() {
        serviceScope.launch {
            // Guardar la actual
            if (activeRouteId != -1) {
                repository.stopActiveRoute(activeRouteId, totalDistanceMeters)
            }
            // Iniciar una nueva inmediatamente
            startAutoNewRoute()
        }
    }

    private fun startAutoNewRoute(backfillPoints: List<LocationPoint> = emptyList()) {
        serviceScope.launch {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val newName = "Auto-Ruta $dateStr"
            val newRouteId = repository.startNewRoute(newName)
            
            // Insertar los puntos del buffer (backfill) asociados a la nueva ruta
            if (backfillPoints.isNotEmpty()) {
                val preparedPoints = mutableListOf<LocationPoint>()
                for (p in backfillPoints) {
                    preparedPoints.add(p.copy(routeId = newRouteId))
                }
                repository.addLocationPoints(preparedPoints)
                
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
                _elapsedTimeSeconds.value = 0L
                _currentDistanceMeters.value = totalDistanceMeters
                lastRecordedLocation = null
                
                // Reiniciar seguimiento con el nuevo ID
                _isTracking.value = false // Forzar reinicio en startTracking
                startTracking(newName, newRouteId, 5000L, 2f)
            }
        }
    }

    private fun pauseTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        _isTracking.value = false
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()

        serviceScope.launch {
            if (activeRouteId != -1) {
                repository.stopActiveRoute(activeRouteId, totalDistanceMeters)
            }

            // Reiniciar estados una vez confirmado el guardado
            withContext(Dispatchers.Main) {
                _isTracking.value = false
                _currentRouteId.value = null
                _latestLocation.value = null
                _elapsedTimeSeconds.value = 0L
                _currentDistanceMeters.value = 0.0

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
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
            val name = "Rastreo de Rutas GPS"
            val descriptionText = "Notificación persistente para el seguimiento de rutas en segundo plano"
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
        return String.format("%02d:%02d:%02d", hr, min, sec)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        serviceScope.cancel()

        // Liberar el HandlerThread para evitar fugas de memoria
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null

        super.onDestroy()
    }
}
