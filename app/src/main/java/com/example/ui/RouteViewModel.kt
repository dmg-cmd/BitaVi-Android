package com.example.ui

import android.app.Application
import androidx.exifinterface.media.ExifInterface
import androidx.core.content.FileProvider
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.Waypoint
import com.example.data.model.SavedLocation
import org.osmdroid.util.GeoPoint
import com.example.data.api.GroqApi
import com.example.data.api.GroqRequest
import com.example.data.api.GroqMessage
import com.example.data.api.RetrofitClient
import com.example.data.repository.RouteRepository
import com.example.location.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import java.util.concurrent.TimeUnit

// IA (Groq) is now defined in data/api/GroqApi.kt

sealed interface Screen {
    object Tracking : Screen
    object History : Screen
    object SavedLocations : Screen
    data class Detail(val routeId: Int) : Screen
}

class RouteViewModel(application: Application, private val repository: RouteRepository) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
 
    // A13: Cache de SharedPreferences para evitar I/O repetitivo y múltiples instancias
    private val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    // Flujo compartido reactivo para actualizar color en Room evitando condiciones de carrera
    private val _savedLocationColorUpdateFlow = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 64)

    fun updateSavedLocationColorReactively(id: Int, color: String) {
        _savedLocationColorUpdateFlow.tryEmit(Pair(id, color))
    }

    var mapTargetLocation by mutableStateOf<GeoPoint?>(null)
        private set

    fun setMapTarget(lat: Double, lon: Double) {
        mapTargetLocation = GeoPoint(lat, lon)
        navigateTo(Screen.Tracking)
    }

    fun clearMapTarget() { mapTargetLocation = null }
    var currentScreen by mutableStateOf<Screen>(Screen.Tracking)
        private set
    var isOfflineMode by mutableStateOf(false)
        private set
    var isBiometricEnabled by mutableStateOf(false)
        private set

    fun toggleBiometric(enabled: Boolean) {
        isBiometricEnabled = enabled
        sharedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    var groqApiKey by mutableStateOf<String?>(null)
        private set
    var aiAnalysisResult by mutableStateOf<String?>(null)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set
    var isImporting by mutableStateOf(false)
        private set

    fun updateGroqApiKey(key: String) { groqApiKey = key }
    var currentAddress by mutableStateOf<String?>(null)
        private set
    var selectedPointAddress by mutableStateOf<String?>(null)
        private set

    var updateIntervalSeconds by mutableStateOf(5f)
    var updateDistanceMeters by mutableStateOf(2f)
    var defaultLatitude by mutableStateOf(-34.6037)
    var defaultLongitude by mutableStateOf(-58.3816)
    var isAirplaneMode by mutableStateOf(false)
        private set
    var isAutoMode by mutableStateOf(false)
        private set

    fun updateDefaultLocation(lat: Double, lon: Double) { defaultLatitude = lat; defaultLongitude = lon }
    fun updateDefaultLocationPersistently(lat: Double, lon: Double) {
        updateDefaultLocation(lat, lon)
        sharedPrefs.edit().putFloat("default_lat", lat.toFloat()).putFloat("default_lon", lon.toFloat()).apply()
    }

    fun toggleAirplaneMode() { 
        isAirplaneMode = !isAirplaneMode 
        // M08: Persistir el cambio para que el LocationService lo detecte
        sharedPrefs.edit().putBoolean("airplane_mode", isAirplaneMode).apply()
    }

    fun toggleAutoMode() {
        isAutoMode = !isAutoMode
        sharedPrefs.edit().putBoolean("auto_mode", isAutoMode).apply()
    }
    fun toggleOfflineMode() { isOfflineMode = !isOfflineMode }

    private val _isNearMeEnabled = MutableStateFlow(false)
    val isNearMeEnabled: StateFlow<Boolean> = _isNearMeEnabled.asStateFlow()

    fun toggleNearMe() {
        val newState = !_isNearMeEnabled.value
        _isNearMeEnabled.value = newState
        sharedPrefs.edit().putBoolean("near_me_enabled", newState).apply()
        
        // Notificar al servicio que ha cambiado el ajuste
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_UPDATE_SETTINGS
        }
        context.startService(intent)
    }

    // Al inicializar, cargar el valor guardado
    init {
        // Cargar preferencias iniciales
        isAutoMode = sharedPrefs.getBoolean("auto_mode", false)
        isAirplaneMode = sharedPrefs.getBoolean("airplane_mode", false)
        _isNearMeEnabled.value = sharedPrefs.getBoolean("near_me_enabled", false)
        isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)
        defaultLatitude = sharedPrefs.getFloat("default_lat", -34.6037f).toDouble()
        defaultLongitude = sharedPrefs.getFloat("default_lon", -58.3816f).toDouble()

        // Recolectar reactivamente las actualizaciones de color en Room
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch(Dispatchers.IO) {
            _savedLocationColorUpdateFlow
                .debounce(100) // Evita saturar la base de datos si el slider se mueve rápido
                .collectLatest { (id, color) ->
                    repository.updateSavedLocationColor(id, color)
                }
        }
    }

    fun navigateTo(screen: Screen) { currentScreen = screen }

    val allRoutes: StateFlow<List<Route>> = repository.allRoutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeRouteInDb: StateFlow<Route?> = repository.activeRouteFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isTrackingLive: StateFlow<Boolean> = LocationService.isTracking
    val activeRouteIdLive: StateFlow<Int?> = LocationService.currentRouteId
    val latestLocationLive: StateFlow<Location?> = LocationService.latestLocation
    val elapsedSecondsLive: StateFlow<Long> = LocationService.elapsedTimeSeconds
    val distanceMetersLive: StateFlow<Double> = LocationService.currentDistanceMeters

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeRoutePoints: StateFlow<List<LocationPoint>> = activeRouteIdLive.flatMapLatest { id ->
        if (id != null) repository.getLocationPointsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeRouteWaypoints: StateFlow<List<Waypoint>> = activeRouteIdLive.flatMapLatest { id ->
        if (id != null) repository.getWaypointsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDetailRouteId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDetailRoute: StateFlow<Route?> = _selectedDetailRouteId.flatMapLatest { id ->
        if (id != null) repository.getRouteByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDetailPoints: StateFlow<List<LocationPoint>> = _selectedDetailRouteId.flatMapLatest { id ->
        if (id != null) repository.getLocationPointsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDetailWaypoints: StateFlow<List<Waypoint>> = _selectedDetailRouteId.flatMapLatest { id ->
        if (id != null) repository.getWaypointsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectRouteForDetail(routeId: Int) { _selectedDetailRouteId.value = routeId; navigateTo(Screen.Detail(routeId)) }

    fun startPassiveLocationService() {
        val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_START_PASSIVE }
        context.startService(intent)
    }

    fun startTrackingRoute(name: String) {
        viewModelScope.launch {
            val routeId = repository.startNewRoute(name)
            val intent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_START
                putExtra(LocationService.EXTRA_ROUTE_NAME, name)
                putExtra(LocationService.EXTRA_ROUTE_ID, routeId)
                putExtra(LocationService.EXTRA_UPDATE_INTERVAL, (updateIntervalSeconds * 1000).toLong())
                putExtra(LocationService.EXTRA_UPDATE_DISTANCE, updateDistanceMeters)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }

    fun stopTrackingRoute() {
        val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_STOP }
        context.startService(intent)
    }

    fun addWaypointToActiveRoute(name: String, description: String, photoPath: String? = null, author: String = "", latitude: Double? = null, longitude: Double? = null, color: String = "#00FF00") {
        val routeId = activeRouteIdLive.value ?: return
        viewModelScope.launch {
            val finalLat = latitude ?: latestLocationLive.value?.latitude
            val finalLon = longitude ?: latestLocationLive.value?.longitude
            if (finalLat != null && finalLon != null) repository.addWaypoint(routeId, name, description, finalLat, finalLon, photoPath, author, color)
        }
    }

    fun deleteRoute(routeId: Int) { viewModelScope.launch { repository.deleteRoute(routeId) } }
    fun deleteRoutes(routeIds: Set<Int>) { 
        viewModelScope.launch { 
            repository.deleteRoutes(routeIds.toList()) 
        } 
    }
    fun renameRoute(routeId: Int, newName: String) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId)
            if (route != null) repository.updateRoute(route.copy(name = newName))
        }
    }

    fun fetchCurrentAddress() {
        val location = latestLocationLive.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) withContext(Dispatchers.Main) { currentAddress = formatAddress(addresses[0], "Calle desconocida") }
            } catch (e: Exception) { }
        }
    }

    fun clearCurrentAddress() { currentAddress = null }
    fun fetchAddressForCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = fetchLocationDetails(lat, lon)
            withContext(Dispatchers.Main) { 
                currentAddress = details.first 
                selectedPointAddress = details.first
            }
        }
    }

    fun fetchAddressForLocation(lat: Double, lon: Double) {
        selectedPointAddress = "Buscando..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) withContext(Dispatchers.Main) { selectedPointAddress = formatAddress(addresses[0], "Desconocida") }
            } catch (e: Exception) { withContext(Dispatchers.Main) { selectedPointAddress = null } }
        }
    }

    fun clearSelectedPointAddress() { selectedPointAddress = null }
    fun generateSuggestedRouteName(onResult: (String) -> Unit) {
        val datePart = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        onResult(datePart)
    }

    val allSavedLocations: StateFlow<List<SavedLocation>> = repository.allSavedLocations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSpecificLocation(name: String, address: String, latitude: Double, longitude: Double, comment: String = "", author: String = "", photoPath: String? = null, phoneNumber: String = "", email: String = "", isContact: Boolean = false, color: String = "#FFD700") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSavedLocation(name, address, latitude, longitude, comment, author, photoPath, phoneNumber, email, isContact, "Desconocido", "Desconocida", color)
        }
    }

    fun updateSavedLocation(location: SavedLocation) { viewModelScope.launch(Dispatchers.IO) { repository.updateSavedLocation(location) } }
    fun refreshLocationDetails(location: SavedLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = fetchLocationDetails(location.latitude, location.longitude)
            repository.updateSavedLocation(location.copy(address = details.first))
        }
    }

    fun deleteSavedLocation(savedLocation: SavedLocation) { viewModelScope.launch { repository.deleteSavedLocation(savedLocation) } }
    fun clearAllAppData() { viewModelScope.launch { repository.deleteAllData() } }

    /**
     * Formatea una dirección completa (calle + altura) de forma segura.
     * Estrategia de fallback:
     * 1. thoroughfare + subThoroughfare (ideal)
     * 2. Si subThoroughfare es nulo, intenta extraer el número de featureName
     * 3. Si thoroughfare es nulo, usa getAddressLine(0) completo
     * 4. Si todo falla, usa el fallback proporcionado
     */
    private fun formatAddress(addr: android.location.Address, fallback: String): String {
        val calle = addr.thoroughfare
        val altura = addr.subThoroughfare

        // Caso 1: Tenemos calle y altura → concatenar
        if (!calle.isNullOrBlank() && !altura.isNullOrBlank()) {
            return "$calle $altura"
        }

        // Caso 2: Tenemos calle pero no altura → intentar extraer de featureName
        if (!calle.isNullOrBlank()) {
            val feature = addr.featureName
            if (!feature.isNullOrBlank() && feature != calle) {
                // featureName suele ser "Calle 1234" — extraer número si empieza con la calle
                val possibleNumber = feature.removePrefix(calle).trim()
                if (possibleNumber.isNotBlank() && possibleNumber.first().isDigit()) {
                    return "$calle $possibleNumber"
                }
                // Si featureName es completamente un número suelto, usarlo como altura
                if (feature.all { it.isDigit() }) {
                    return "$calle $feature"
                }
            }
            // Solo calle, sin altura disponible
            return calle
        }

        // Caso 3: Sin thoroughfare → usar getAddressLine(0) si existe
        val addressLine = if (addr.maxAddressLineIndex >= 0) addr.getAddressLine(0) else null
        if (!addressLine.isNullOrBlank()) {
            return addressLine
        }

        // Caso 4: featureName como último recurso
        val feature = addr.featureName
        if (!feature.isNullOrBlank()) {
            return feature
        }

        // Caso 5: Fallback total
        return fallback
    }

    private fun fetchLocationDetails(lat: Double, lon: Double): Triple<String, String, String> {
        val coordFallback = "GPS: ${String.format("%.5f, %.5f", lat, lon)}"
        return try {
            val geocoder = android.location.Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                Triple(formatAddress(addr, coordFallback), addr.countryName ?: "Desconocido", addr.locality ?: addr.adminArea ?: "Desconocida")
            } else Triple(coordFallback, "Desconocido", "Desconocida")
        } catch (e: Exception) { Triple(coordFallback, "Desconocido", "Desconocida") }
    }

    fun getPhotoLocation(uri: Uri): Pair<Double, Double>? {
        return try {
            val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) android.provider.MediaStore.setRequireOriginal(uri) else uri
            context.contentResolver.openInputStream(photoUri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) Pair(latLong[0].toDouble(), latLong[1].toDouble()) else null
            }
        } catch (e: Exception) { null }
    }

    fun analyzeRouteWithAI(routeId: Int) {
        val key = groqApiKey
        if (key.isNullOrBlank()) { aiAnalysisResult = "⚠️ Configure su API Key."; return }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isAnalyzing = true; aiAnalysisResult = "Analizando..." }
            try {
                val route = repository.getRouteById(routeId)
                if (route == null) { withContext(Dispatchers.Main) { aiAnalysisResult = "Sin datos."; isAnalyzing = false }; return@launch }
                
                val waypoints = repository.getWaypointsSync(routeId)
                val durationMs = if (route.endTime > route.startTime) route.endTime - route.startTime else 0L
                val durationFormatted = String.format("%02d:%02d:%02d", 
                    TimeUnit.MILLISECONDS.toHours(durationMs),
                    TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
                )

                val promptText = """
                    Actúa como un Ingeniero de Telemetría GPS. Analiza sínicamente estos datos y genera un informe técnico BREVE:
                    - Ruta: ${route.name}
                    - Distancia Real: ${String.format("%.2f", route.distanceMeters / 1000.0)} km
                    - Tiempo en Movimiento: $durationFormatted
                    - Dinámica de Velocidad: Máx ${String.format("%.1f", route.maxSpeed * 3.6f)} km/h, Media ${String.format("%.1f", route.avgSpeed * 3.6f)} km/h
                    - Puntos Técnicos: ${waypoints.size} marcados (Analiza su distribución)
                    
                    REGLAS CRÍTICAS:
                    1. NO uses lenguaje motivador ni frases de aliento.
                    2. Céntrate en la regularidad del ritmo y el perfil del terreno.
                    3. Identifica anomalías en la velocidad o el desplazamiento.
                    4. Máximo 2 párrafos técnicos.
                """.trimIndent()

                val api = RetrofitClient.groqApi
                val request = GroqRequest(
                    messages = listOf(
                        GroqMessage("system", "Eres un sistema de análisis técnico de telemetría y geomática. Tu tono es seco, profesional y basado estrictamente en datos numéricos y geográficos."),
                        GroqMessage("user", promptText)
                    )
                )
                val response = api.generateContent("Bearer ${key.trim()}", request)
                withContext(Dispatchers.Main) { 
                    aiAnalysisResult = if (response.isSuccessful) {
                        response.body()?.choices?.firstOrNull()?.message?.content ?: "Respuesta vacía."
                    } else {
                        "Error API: ${response.code()}"
                    }
                }
            } catch (t: Throwable) { 
                t.printStackTrace()
                withContext(Dispatchers.Main) { aiAnalysisResult = "Error de conexión." } 
            } finally { withContext(Dispatchers.Main) { isAnalyzing = false } }
        }
    }

    fun exportRoutesAsCSV(routeId: Int) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId) ?: return@launch
            val points = repository.getLocationPointsSync(routeId)
            val sb = StringBuilder("Fecha,Lat,Lon\n")
            points.forEach { sb.append("${it.timestamp},${it.latitude},${it.longitude}\n") }
            saveCsvFile(sb.toString(), route.name)
        }
    }

    private fun saveCsvFile(content: String, routeName: String) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.csv"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
            val file = context.getFileStreamPath(fileName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val chooser = Intent.createChooser(intent, "Exportar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) { }
    }

    fun exportRoutesAsGPX(routeIds: Set<Int>) {
        viewModelScope.launch {
            val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\">\n")
            routeIds.forEach { id ->
                val route = repository.getRouteById(id) ?: return@forEach
                val points = repository.getLocationPointsSync(id)
                sb.append("<trk><name>${route.name}</name><trkseg>\n")
                points.forEach { sb.append("<trkpt lat=\"${it.latitude}\" lon=\"${it.longitude}\"></trkpt>\n") }
                sb.append("</trkseg></trk>\n")
            }
            sb.append("</gpx>")
            saveGpxFile(sb.toString())
        }
    }

    private fun saveGpxFile(content: String) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.gpx"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
            val file = context.getFileStreamPath(fileName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/gpx+xml"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val chooser = Intent.createChooser(intent, "Exportar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) { }
    }

    fun importGpxFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isImporting = true }
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)
                var eventType = parser.eventType
                var routeName = "Importada"
                var routeId = -1
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.name == "name" && routeId == -1) {
                            routeName = parser.nextText()
                            routeId = repository.startNewRoute(routeName)
                        } else if (parser.name == "trkpt") {
                            if (routeId == -1) routeId = repository.startNewRoute(routeName)
                            val lat = parser.getAttributeValue(null, "lat")?.toDouble() ?: 0.0
                            val lon = parser.getAttributeValue(null, "lon")?.toDouble() ?: 0.0
                            val pt = LocationPoint(routeId = routeId, latitude = lat, longitude = lon, altitude = 0.0, speed = 0f, timestamp = System.currentTimeMillis())
                            repository.addLocationPoints(listOf(pt))
                        }
                    }
                    eventType = parser.next()
                }
                withContext(Dispatchers.Main) { navigateTo(Screen.History) }
            } catch (e: Exception) { } finally { withContext(Dispatchers.Main) { isImporting = false } }
        }
    }

    fun createImageFile(): Uri? {
        return try {
            val storageDir = File(context.filesDir, "images")
            if (!storageDir.exists()) storageDir.mkdirs()
            val file = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", storageDir)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }
    suspend fun saveImageToInternalStorage(uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val storageDir = File(context.filesDir, "images")
                if (!storageDir.exists()) storageDir.mkdirs()
                val file = File(storageDir, "IMG_GALLERY_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) { null }
        }
    }
    suspend fun prepareBackupFile(): File? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                val backupFile = File(context.cacheDir, "backup_$timestamp.zip")
                val buffer = ByteArray(4096)
                ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                    // 1. Exportar FAVORITOS (Completo)
                    val favs: List<SavedLocation> = repository.allSavedLocations.first()
                    if (favs.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("favoritos.txt"))
                        val content = favs.joinToString("\n") { f ->
                            "${f.name}|${f.address}|${f.latitude}|${f.longitude}|${f.comment}|${f.author}|${f.photoPath ?: ""}|${f.phoneNumber}|${f.email}|${f.isContact}|${f.color}|${f.country}|${f.city}|${f.timestamp}" 
                        }
                        zos.write(content.toByteArray())
                        zos.closeEntry()
                    }

                    // 2. Exportar RUTAS
                    val routes: List<Route> = repository.allRoutes.first()
                    if (routes.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("rutas.txt"))
                        val routesContent = routes.joinToString("\n") { r ->
                            "${r.id}|${r.name}|${r.startTime}|${r.endTime}|${r.distanceMeters}|${r.maxSpeed}|${r.avgSpeed}"
                        }
                        zos.write(routesContent.toByteArray())
                        zos.closeEntry()

                        // 3. Exportar PUNTOS y WAYPOINTS
                        val allPoints = mutableListOf<String>()
                        val allWaypoints = mutableListOf<String>()
                        
                        for (route in routes) {
                            val pts = repository.getLocationPointsSync(route.id)
                            for (p in pts) {
                                allPoints.add("${p.routeId}|${p.latitude}|${p.longitude}|${p.altitude}|${p.speed}|${p.timestamp}")
                            }
                            
                            val wps = repository.getWaypointsSync(route.id)
                            for (w in wps) {
                                allWaypoints.add("${w.routeId}|${w.name}|${w.description}|${w.latitude}|${w.longitude}|${w.photoPath ?: ""}|${w.author}|${w.color}")
                            }
                        }

                        if (allPoints.isNotEmpty()) {
                            zos.putNextEntry(ZipEntry("puntos.txt"))
                            zos.write(allPoints.joinToString("\n").toByteArray())
                            zos.closeEntry()
                        }
                        if (allWaypoints.isNotEmpty()) {
                            zos.putNextEntry(ZipEntry("marcadores.txt"))
                            zos.write(allWaypoints.joinToString("\n").toByteArray())
                            zos.closeEntry()
                        }
                    }

                    // 4. Exportar FOTOS físicas
                    val imageDir = File(context.filesDir, "images")
                    if (imageDir.exists()) {
                        imageDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                zos.putNextEntry(ZipEntry("images/${file.name}"))
                                file.inputStream().use { inputStream ->
                                    var bytesRead: Int
                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        zos.write(buffer, 0, bytesRead)
                                    }
                                }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                backupFile
            } catch (e: Exception) { 
                e.printStackTrace()
                null 
            }
        }
    }

    fun restoreFromBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                isImporting = true 
                Toast.makeText(context, "Restaurando datos completos...", Toast.LENGTH_SHORT).show()
            }
            try {
                val headerBytes = ByteArray(4)
                context.contentResolver.openInputStream(uri)?.use { it.read(headerBytes) }
                
                if (headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte()) {
                    context.contentResolver.openInputStream(uri)?.use { fis ->
                        ZipInputStream(fis).use { zis ->
                            var entry = zis.nextEntry
                            val imageDir = File(context.filesDir, "images")
                            if (!imageDir.exists()) imageDir.mkdirs()
                            val buffer = ByteArray(4096)
                            
                            val pendingRouteLines = mutableListOf<String>()
                            val pendingPointLines = mutableListOf<String>()
                            val pendingWaypointLines = mutableListOf<String>()

                            while (entry != null) {
                                val name = entry.name
                                when {
                                    name.endsWith(".gpx", ignoreCase = true) -> {
                                        val routeName = name.substringAfterLast("/").substringBeforeLast(".")
                                        val content = zis.readBytes()
                                        processGpxInputStream(java.io.ByteArrayInputStream(content), routeName)
                                    }
                                    name.contains("localizador_database") -> {
                                        val dbFile = File(context.getDatabasePath("localizador_database").parent, name.substringAfterLast("/"))
                                        dbFile.outputStream().use { os ->
                                            var bytesRead: Int
                                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                                os.write(buffer, 0, bytesRead)
                                            }
                                        }
                                    }
                                    name == "mis_favoritos.txt" || name == "favoritos.txt" -> {
                                        val lines = zis.readBytes().decodeToString().split("\n")
                                        lines.filter { it.isNotBlank() }.forEach { line ->
                                            val p = line.split("|")
                                            if (p.size >= 4) {
                                                val nameVal = p[0]
                                                val addressVal = p[1]
                                                val lat = p[2].toDoubleOrNull() ?: 0.0
                                                val lon = p[3].toDoubleOrNull() ?: 0.0
                                                val commentVal = if (p.size > 4) p[4] else ""
                                                val authorVal = if (p.size > 5) p[5] else ""
                                                val originalPhotoPath = if (p.size > 6 && p[6].isNotBlank()) p[6] else null
                                                var photoPathVal = originalPhotoPath

                                                // Validar y regenerar integridad referencial del URI
                                                if (originalPhotoPath != null) {
                                                    val fileName = originalPhotoPath.substringAfterLast("/")
                                                    val file = File(context.filesDir, "images/$fileName")
                                                    // Si la imagen importada existe físicamente en el almacenamiento, generamos un URI válido
                                                    if (file.exists()) {
                                                        photoPathVal = FileProvider.getUriForFile(
                                                            context, 
                                                            "${context.packageName}.fileprovider", 
                                                            file
                                                        ).toString()
                                                    }
                                                }
                                                val phoneNumberVal = if (p.size > 7) p[7] else ""
                                                val emailVal = if (p.size > 8) p[8] else ""
                                                val isContactVal = if (p.size > 9) p[9].toBoolean() else false
                                                val colorVal = if (p.size > 10) p[10] else "#FFD700"
                                                
                                                val details = fetchLocationDetails(lat, lon)
                                                val countryVal = if (p.size > 11) p[11] else details.second
                                                val cityVal = if (p.size > 12) p[12] else details.third
                                                val timestampVal = if (p.size > 13) p[13].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()

                                                repository.addSavedLocation(
                                                    name = nameVal,
                                                    address = addressVal,
                                                    latitude = lat,
                                                    longitude = lon,
                                                    comment = commentVal,
                                                    author = authorVal,
                                                    photoPath = photoPathVal,
                                                    phoneNumber = phoneNumberVal,
                                                    email = emailVal,
                                                    isContact = isContactVal,
                                                    country = countryVal,
                                                    city = cityVal,
                                                    color = colorVal,
                                                    timestamp = timestampVal
                                                )
                                            }
                                        }
                                    }
                                    name == "rutas.txt" -> {
                                        pendingRouteLines.addAll(
                                            zis.readBytes().decodeToString().split("\n").filter { it.isNotBlank() }
                                        )
                                    }
                                    name == "puntos.txt" -> {
                                        pendingPointLines.addAll(
                                            zis.readBytes().decodeToString().split("\n").filter { it.isNotBlank() }
                                        )
                                    }
                                    name == "marcadores.txt" -> {
                                        pendingWaypointLines.addAll(
                                            zis.readBytes().decodeToString().split("\n").filter { it.isNotBlank() }
                                        )
                                    }
                                    (name.startsWith("fotos/") || name.startsWith("images/")) -> {
                                        val fileName = name.substringAfterLast("/")
                                        if (fileName.isNotEmpty()) {
                                            File(imageDir, fileName).outputStream().use { os ->
                                                var bytesRead: Int
                                                while (zis.read(buffer).also { bytesRead = it } != -1) {
                                                    os.write(buffer, 0, bytesRead)
                                                }
                                            }
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }

                            // --- Procesar rutas, puntos y waypoints pendientes ---
                            if (pendingRouteLines.isNotEmpty()) {
                                // Mapa de routeId original → routeId nuevo
                                val routeIdMap = mutableMapOf<Int, Int>()

                                // 1. Crear las rutas
                                for (line in pendingRouteLines) {
                                    val p = line.split("|")
                                    if (p.size >= 7) {
                                        val originalId = p[0].toIntOrNull() ?: continue
                                        val routeName = p[1]
                                        val startTime = p[2].toLongOrNull() ?: System.currentTimeMillis()
                                        val endTime = p[3].toLongOrNull() ?: 0L
                                        val distance = p[4].toDoubleOrNull() ?: 0.0
                                        val maxSpd = p[5].toFloatOrNull() ?: 0f
                                        val avgSpd = p[6].toFloatOrNull() ?: 0f

                                        val newRoute = Route(
                                            name = routeName,
                                            startTime = startTime,
                                            endTime = endTime,
                                            distanceMeters = distance,
                                            isActive = false,
                                            maxSpeed = maxSpd,
                                            avgSpeed = avgSpd
                                        )
                                        val newId = repository.insertRouteDirectly(newRoute)
                                        routeIdMap[originalId] = newId
                                    }
                                }

                                // 2. Insertar los puntos GPS con speed y timestamp originales
                                if (pendingPointLines.isNotEmpty()) {
                                    val pointsBatch = mutableListOf<LocationPoint>()
                                    for (line in pendingPointLines) {
                                        val p = line.split("|")
                                        if (p.size >= 6) {
                                            val originalRouteId = p[0].toIntOrNull() ?: continue
                                            val newRouteId = routeIdMap[originalRouteId] ?: continue
                                            val lat = p[1].toDoubleOrNull() ?: continue
                                            val lon = p[2].toDoubleOrNull() ?: continue
                                            val alt = p[3].toDoubleOrNull() ?: 0.0
                                            val spd = p[4].toFloatOrNull() ?: 0f
                                            val ts = p[5].toLongOrNull() ?: System.currentTimeMillis()

                                            pointsBatch.add(LocationPoint(
                                                routeId = newRouteId,
                                                latitude = lat,
                                                longitude = lon,
                                                altitude = alt,
                                                speed = spd,
                                                timestamp = ts
                                            ))
                                        }
                                    }
                                    if (pointsBatch.isNotEmpty()) {
                                        repository.addLocationPoints(pointsBatch)
                                    }
                                }

                                // 3. Insertar los waypoints/marcadores
                                if (pendingWaypointLines.isNotEmpty()) {
                                    for (line in pendingWaypointLines) {
                                        val p = line.split("|")
                                        if (p.size >= 5) {
                                            val originalRouteId = p[0].toIntOrNull() ?: continue
                                            val newRouteId = routeIdMap[originalRouteId] ?: continue
                                            val wpName = p[1]
                                            val wpDesc = if (p.size > 2) p[2] else ""
                                            val wpLat = p[3].toDoubleOrNull() ?: continue
                                            val wpLon = p[4].toDoubleOrNull() ?: continue
                                            val wpPhoto = if (p.size > 5 && p[5].isNotBlank()) p[5] else null
                                            val wpAuthor = if (p.size > 6) p[6] else ""
                                            val wpColor = if (p.size > 7) p[7] else "#00FF00"

                                            repository.addWaypoint(
                                                routeId = newRouteId,
                                                name = wpName,
                                                description = wpDesc,
                                                latitude = wpLat,
                                                longitude = wpLon,
                                                photoPath = wpPhoto,
                                                author = wpAuthor,
                                                color = wpColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (headerBytes[0] == 0x3C.toByte()) {
                    importGpxFile(uri)
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "✅ Restauración completa finalizada.", Toast.LENGTH_SHORT).show()
                    navigateTo(Screen.History)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            } finally { withContext(Dispatchers.Main) { isImporting = false } }
        }
    }

    /**
     * Procesa GPX extrayendo ALTITUD, TIEMPO y calculando VELOCIDAD.
     */
    private suspend fun processGpxInputStream(inputStream: java.io.InputStream, defaultName: String) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var routeName = defaultName
            var routeId = -1
            val points = mutableListOf<LocationPoint>()
            
            var currentLat = 0.0
            var currentLon = 0.0
            var currentEle = 0.0
            var currentTime = 0L

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                if (eventType == XmlPullParser.START_TAG) {
                    when (tag) {
                        "name" -> if (routeId == -1) routeName = parser.nextText()
                        "trkpt" -> {
                            currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                            currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        }
                        "ele" -> currentEle = parser.nextText().toDoubleOrNull() ?: 0.0
                        "time" -> {
                            val timeStr = parser.nextText()
                            currentTime = try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                sdf.parse(timeStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) { System.currentTimeMillis() }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && tag == "trkpt") {
                    if (routeId == -1) routeId = repository.startNewRoute(routeName)
                    var speed = 0f
                    if (points.isNotEmpty()) {
                        val last = points.last()
                        val dist = FloatArray(1)
                        Location.distanceBetween(last.latitude, last.longitude, currentLat, currentLon, dist)
                        val timeDiffSec = (currentTime - last.timestamp) / 1000.0
                        if (timeDiffSec > 0.0) speed = (dist[0] / timeDiffSec).toFloat()
                    }
                    points.add(LocationPoint(
                        routeId = routeId, latitude = currentLat, longitude = currentLon,
                        altitude = currentEle, speed = speed, timestamp = currentTime
                    ))
                }
                eventType = parser.next()
            }
            if (points.isNotEmpty()) {
                repository.addLocationPoints(points)
                var d = 0.0
                for (i in 0 until points.size - 1) {
                    val res = FloatArray(1)
                    Location.distanceBetween(points[i].latitude, points[i].longitude, points[i+1].latitude, points[i+1].longitude, res)
                    d += res[0].toDouble()
                }
                repository.stopActiveRoute(routeId, d)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- FUNCIONES FASE 2: SOCIAL Y CONTACTOS ---

    /**
     * Extrae los detalles de un contacto de la agenda usando su URI.
     */
    fun getContactDetails(contactUri: Uri, onResult: (name: String, phone: String, email: String, address: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var name = ""
            var phone = ""
            var email = ""
            var address = ""

            val contentResolver = context.contentResolver
            
            // 1. Obtener Nombre e ID
            contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts._ID))
                    name = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME))

                    // 2. Obtener Teléfono
                    if (cursor.getInt(cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                phone = pCursor.getString(pCursor.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                        }
                    }

                    // 3. Obtener Email
                    contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { eCursor ->
                        if (eCursor.moveToFirst()) {
                            email = eCursor.getString(eCursor.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS))
                        }
                    }

                    // 4. Obtener Dirección Postal
                    contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        "${android.provider.ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )?.use { aCursor ->
                        if (aCursor.moveToFirst()) {
                            address = aCursor.getString(aCursor.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onResult(name, phone, email, address)
            }
        }
    }

    /**
     * Comparte los detalles de una ubicación guardada con otras aplicaciones.
     */
    fun shareLocation(location: SavedLocation) {
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        val shareText = """
            📍 *${location.name}*
            
            ${if (location.address.isNotBlank()) "🏠 Dirección: ${location.address}" else ""}
            ${if (location.comment.isNotBlank()) "📝 Nota: ${location.comment}" else ""}
            
            🗺️ Ver en el mapa: $mapsUrl
            
            _Enviado desde Localizador Pro_
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Ubicación: ${location.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Compartir ubicación vía")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // --- FUNCIONES FASE 3: COMPARTIR TRAYECTO COMPLETO ---

    /**
     * Genera un resumen de la ruta y permite compartirlo junto con las fotos de sus waypoints y una captura del mapa.
     */
    fun shareCompleteRoute(routeId: Int, mapBitmap: android.graphics.Bitmap? = null) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId) ?: return@launch
            val waypoints = repository.getWaypointsSync(routeId)
            
            val km = route.distanceMeters / 1000.0
            val durationMs = if (route.endTime > route.startTime) route.endTime - route.startTime else 0L
            val h = TimeUnit.MILLISECONDS.toHours(durationMs)
            val m = (TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60)
            val durationText = "${h}h ${m}min"

            val shareText = StringBuilder().apply {
                append("🚀 *Resumen de mi Trayecto: ${route.name}*\n\n")
                append("📏 Distancia: ${String.format("%.2f", km)} km\n")
                append("⏱️ Duración: $durationText\n")
                append("⚡ Vel. Máxima: ${String.format("%.1f", route.maxSpeed * 3.6f)} km/h\n")
                
                if (waypoints.isNotEmpty()) {
                    append("\n📍 *Puntos destacados:*")
                    waypoints.forEach { wp ->
                        append("\n- ${wp.name}: ${wp.description}")
                    }
                }
                
                append("\n\n🗺️ _Generado por Localizador Pro_")
            }.toString()

            val imageUris = arrayListOf<Uri>()

            // 1. Añadir Captura del Mapa (si existe) como primera imagen
            mapBitmap?.let { bitmap ->
                try {
                    val cachePath = File(context.cacheDir, "images")
                    cachePath.mkdirs()
                    val mapFile = File(cachePath, "map_${System.currentTimeMillis()}.png")
                    val stream = FileOutputStream(mapFile)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    stream.close()
                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mapFile)
                    imageUris.add(contentUri)
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 2. Añadir fotos de Waypoints
            waypoints.forEach { wp ->
                wp.photoPath?.let { path ->
                    try {
                        val uri = Uri.parse(path)
                        imageUris.add(uri)
                    } catch (e: Exception) { }
                }
            }

            val intent = if (imageUris.isEmpty()) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(intent, "Compartir trayecto en...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}

class RouteViewModelFactory(private val application: Application, private val repository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) return RouteViewModel(application, repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
