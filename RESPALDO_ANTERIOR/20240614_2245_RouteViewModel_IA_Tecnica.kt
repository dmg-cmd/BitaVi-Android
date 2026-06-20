package com.example.ui

import androidx.exifinterface.media.ExifInterface
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.data.repository.RouteRepository
import com.example.location.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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

class RouteViewModel(private val repository: RouteRepository) : ViewModel() {

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

    fun toggleBiometric(enabled: Boolean, context: Context) {
        isBiometricEnabled = enabled
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
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
    fun updateDefaultLocationPersistently(lat: Double, lon: Double, context: Context) {
        updateDefaultLocation(lat, lon)
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().putFloat("default_lat", lat.toFloat()).putFloat("default_lon", lon.toFloat()).apply()
    }

    fun toggleAirplaneMode() { isAirplaneMode = !isAirplaneMode }
    fun toggleAutoMode(context: Context) {
        isAutoMode = !isAutoMode
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().putBoolean("auto_mode", isAutoMode).apply()
    }
    fun toggleOfflineMode() { isOfflineMode = !isOfflineMode }

    private val _isNearMeEnabled = MutableStateFlow(false)
    val isNearMeEnabled: StateFlow<Boolean> = _isNearMeEnabled.asStateFlow()

    fun toggleNearMe(context: Context) {
        val newState = !_isNearMeEnabled.value
        _isNearMeEnabled.value = newState
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.edit().putBoolean("near_me_enabled", newState).apply()
        
        // Notificar al servicio que ha cambiado el ajuste
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_UPDATE_SETTINGS
        }
        context.startService(intent)
    }

    // Al inicializar, cargar el valor guardado
    init {
        // Nota: Como no tengo el context aquí de forma directa, lo haremos desde la MainActivity o inyectando sharedPrefs.
        // Por ahora, el valor se seteará desde el init de MainActivity.
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

    fun startPassiveLocationService(context: Context) {
        val intent = Intent(context, LocationService::class.java).apply { action = LocationService.ACTION_START_PASSIVE }
        context.startService(intent)
    }

    fun startTrackingRoute(context: Context, name: String) {
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

    fun stopTrackingRoute(context: Context) {
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
    fun deleteRoutes(routeIds: Set<Int>) { viewModelScope.launch { routeIds.forEach { repository.deleteRoute(it) } } }
    fun renameRoute(routeId: Int, newName: String) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId)
            if (route != null) repository.updateRoute(route.copy(name = newName))
        }
    }

    fun fetchCurrentAddress(context: Context) {
        val location = latestLocationLive.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) withContext(Dispatchers.Main) { currentAddress = addresses[0].thoroughfare ?: "Calle desconocida" }
            } catch (e: Exception) { }
        }
    }

    fun clearCurrentAddress() { currentAddress = null }
    fun fetchAddressForCoordinates(context: Context, lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = fetchLocationDetails(context, lat, lon)
            withContext(Dispatchers.Main) { 
                currentAddress = details.first 
                selectedPointAddress = details.first
            }
        }
    }

    fun fetchAddressForLocation(context: Context, lat: Double, lon: Double) {
        selectedPointAddress = "Buscando..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) withContext(Dispatchers.Main) { selectedPointAddress = addresses[0].thoroughfare ?: "Desconocida" }
            } catch (e: Exception) { withContext(Dispatchers.Main) { selectedPointAddress = null } }
        }
    }

    fun clearSelectedPointAddress() { selectedPointAddress = null }
    fun generateSuggestedRouteName(context: Context, onResult: (String) -> Unit) {
        val datePart = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        onResult(datePart)
    }

    val allSavedLocations: StateFlow<List<SavedLocation>> = repository.allSavedLocations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSpecificLocation(name: String, address: String, latitude: Double, longitude: Double, comment: String = "", author: String = "", photoPath: String? = null, phoneNumber: String = "", email: String = "", isContact: Boolean = false, context: Context, color: String = "#FFD700") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSavedLocation(name, address, latitude, longitude, comment, author, photoPath, phoneNumber, email, isContact, "Desconocido", "Desconocida", color)
        }
    }

    fun updateSavedLocation(location: SavedLocation) { viewModelScope.launch(Dispatchers.IO) { repository.updateSavedLocation(location) } }
    fun refreshLocationDetails(context: Context, location: SavedLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = fetchLocationDetails(context, location.latitude, location.longitude)
            repository.updateSavedLocation(location.copy(address = details.first))
        }
    }

    fun deleteSavedLocation(savedLocation: SavedLocation) { viewModelScope.launch { repository.deleteSavedLocation(savedLocation) } }
    fun clearAllAppData() { viewModelScope.launch { repository.deleteAllData() } }

    private fun fetchLocationDetails(context: Context, lat: Double, lon: Double): Triple<String, String, String> {
        val coordFallback = "GPS: ${String.format("%.5f, %.5f", lat, lon)}"
        return try {
            val geocoder = android.location.Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                Triple(addr.thoroughfare ?: coordFallback, addr.countryName ?: "Desconocido", addr.locality ?: addr.adminArea ?: "Desconocida")
            } else Triple(coordFallback, "Desconocido", "Desconocida")
        } catch (e: Exception) { Triple(coordFallback, "Desconocido", "Desconocida") }
    }

    fun getPhotoLocation(context: Context, uri: Uri): Pair<Double, Double>? {
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
                    Analiza técnicamente esta ruta de GPS y genera un comentario profesional MUY BREVE (máximo 2 párrafos):
                    - Nombre: ${route.name}
                    - Distancia Total: ${String.format("%.2f", route.distanceMeters / 1000.0)} km
                    - Duración: $durationFormatted
                    - Velocidad Máxima: ${String.format("%.1f", route.maxSpeed * 3.6f)} km/h
                    - Velocidad Promedio: ${String.format("%.1f", route.avgSpeed * 3.6f)} km/h
                    - Puntos de Interés: ${waypoints.size} marcados
                    
                    Instrucciones: Sé muy conciso. No gastes palabras innecesarias. Destaca el ritmo y el tipo de recorrido.
                """.trimIndent()

                val okHttpClient = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).build()
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.groq.com/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
                val api = retrofit.create(GroqApi::class.java)
                val request = GroqRequest(
                    messages = listOf(
                        GroqMessage("system", "Eres un asistente experto en análisis de rutas GPS y deportes al aire libre."),
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

    fun exportRoutesAsCSV(context: Context, routeId: Int) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId) ?: return@launch
            val points = repository.getLocationPointsSync(routeId)
            val sb = StringBuilder("Fecha,Lat,Lon\n")
            points.forEach { sb.append("${it.timestamp},${it.latitude},${it.longitude}\n") }
            saveCsvFile(context, sb.toString(), route.name)
        }
    }

    private fun saveCsvFile(context: Context, content: String, routeName: String) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.csv"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
            val file = context.getFileStreamPath(fileName)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(Intent.createChooser(intent, "Exportar"))
        } catch (e: Exception) { }
    }

    fun exportRoutesAsGPX(context: Context, routeIds: Set<Int>) {
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
            saveGpxFile(context, sb.toString())
        }
    }

    private fun saveGpxFile(context: Context, content: String) {
        try {
            val fileName = "export_${System.currentTimeMillis()}.gpx"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
            val file = context.getFileStreamPath(fileName)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "application/gpx+xml"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(Intent.createChooser(intent, "Exportar"))
        } catch (e: Exception) { }
    }

    fun importGpxFile(context: Context, uri: Uri) {
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

    fun createImageFile(context: Context): Uri? {
        return try {
            val storageDir = File(context.filesDir, "images")
            if (!storageDir.exists()) storageDir.mkdirs()
            val file = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", storageDir)
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }
    suspend fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val storageDir = File(context.filesDir, "images")
                if (!storageDir.exists()) storageDir.mkdirs()
                val file = File(storageDir, "IMG_GALLERY_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) { null }
        }
    }
    suspend fun prepareBackupFile(context: Context): File? {
        return try {
            val backupFile = File(context.cacheDir, "respaldo.zip")
            val zos = ZipOutputStream(FileOutputStream(backupFile))
            val favs = repository.allSavedLocations.first()
            if (favs.isNotEmpty()) {
                zos.putNextEntry(ZipEntry("mis_favoritos.txt"))
                zos.write(favs.joinToString("\n") { "${it.name}|${it.address}|${it.latitude}|${it.longitude}" }.toByteArray())
                zos.closeEntry()
            }
            zos.close()
            backupFile
        } catch (e: Exception) { null }
    }
    fun restoreFromBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isImporting = true }
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val zis = ZipInputStream(input)
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "mis_favoritos.txt") {
                            zis.bufferedReader().lineSequence().forEach { line ->
                                val parts = line.split("|")
                                if (parts.size >= 4) repository.addSavedLocation(parts[0], parts[1], parts[2].toDouble(), parts[3].toDouble(), "", "", null, "", "", false, "", "", "#FFD700")
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            } catch (e: Exception) { } finally { withContext(Dispatchers.Main) { isImporting = false } }
        }
    }

    // --- FUNCIONES FASE 2: SOCIAL Y CONTACTOS ---

    /**
     * Extrae los detalles de un contacto de la agenda usando su URI.
     */
    fun getContactDetails(context: Context, contactUri: Uri, onResult: (name: String, phone: String, email: String, address: String) -> Unit) {
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
    fun shareLocation(context: Context, location: SavedLocation) {
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
        context.startActivity(Intent.createChooser(intent, "Compartir ubicación vía"))
    }

    // --- FUNCIONES FASE 3: COMPARTIR TRAYECTO COMPLETO ---

    /**
     * Genera un resumen de la ruta y permite compartirlo junto con las fotos de sus waypoints y una captura del mapa.
     */
    fun shareCompleteRoute(context: Context, routeId: Int, mapBitmap: android.graphics.Bitmap? = null) {
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
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mapFile)
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

            context.startActivity(Intent.createChooser(intent, "Compartir trayecto en..."))
        }
    }
}

class RouteViewModelFactory(private val repository: RouteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) return RouteViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
