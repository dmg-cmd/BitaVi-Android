package com.example.ui

import android.location.Location
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.LocationPoint
import com.example.data.model.Route
import com.example.data.model.SavedLocation
import com.example.data.model.Waypoint
import com.example.data.repository.RouteRepository
import com.example.location.ActivityMode
import com.example.location.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.location.Geocoder
import kotlinx.coroutines.flow.combine
sealed class AiAnalysisState {
    object Idle : AiAnalysisState()
    object Loading : AiAnalysisState()
    data class Success(val report: String) : AiAnalysisState()
    object MissingKey : AiAnalysisState()
    object ExpiredKey : AiAnalysisState()
    data class Error(val message: String) : AiAnalysisState()
}

sealed class DetailAction {
    data object ShowSheet : DetailAction()
    data class ShowEditDialog(val currentName: String) : DetailAction()
    data object ShowDeleteDialog : DetailAction()
    data object ShowHelpDialog : DetailAction()
}

class RouteViewModel(
    val repository: RouteRepository,
    private val prefsManager: PreferencesManager,
    private val geocoderUseCase: GeocoderUseCase,
    private val photoManager: PhotoManager,
    private val groqAiUseCase: GroqAiUseCase,
    private val routeExportUseCase: RouteExportUseCase,
    private val backupUseCase: BackupUseCase,
    private val contactUseCase: ContactUseCase,
    private val shareUseCase: ShareUseCase,
    private val locationServiceManager: LocationServiceManager
) : ViewModel() {

    // ─── UI State ─────────────────────────────────────────────────

    var detailAction by mutableStateOf<DetailAction?>(null)

    var mapTargetLocation by mutableStateOf<GeoPoint?>(null)
        private set
    var isOfflineMode by mutableStateOf(false)
        private set
    var isBiometricEnabled by mutableStateOf(false)
        private set
    var groqApiKey by mutableStateOf<String?>(null)
        private set
    var aiAnalysisState by mutableStateOf<AiAnalysisState>(AiAnalysisState.Idle)
        private set
    var isImporting by mutableStateOf(false)
        private set
    var currentAddress by mutableStateOf<String?>(null)
        private set
    var selectedPointAddress by mutableStateOf<String?>(null)
        private set
    var updateIntervalSeconds by mutableStateOf(5f)
    var updateDistanceMeters by mutableStateOf(2f)
    var defaultLatitude by mutableStateOf(-34.6037)
    var mapStrokeWidth by mutableStateOf(8f)
        private set
    var defaultLongitude by mutableStateOf(-58.3816)
    var isAirplaneMode by mutableStateOf(false)
        private set
    var isAutoMode by mutableStateOf(false)
        private set

    var isBleEnabled by mutableStateOf(false)
        private set

    private val _isNearMeEnabled = MutableStateFlow(false)
    val isNearMeEnabled: StateFlow<Boolean> = _isNearMeEnabled.asStateFlow()

    private val _savedLocationColorUpdateFlow = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 64)
    private val _selectedDetailRouteId = MutableStateFlow<Int?>(null)

    private val _selectedFavoriteId = MutableStateFlow<Int?>(null)
    // ─── Init ─────────────────────────────────────────────────────

    init {
        isAutoMode = prefsManager.isAutoMode()
        isAirplaneMode = prefsManager.isAirplaneMode()
        isBleEnabled = prefsManager.isBleEnabled()
        _isNearMeEnabled.value = prefsManager.isNearMeEnabled()
        isBiometricEnabled = prefsManager.isBiometricEnabled()
        defaultLatitude = prefsManager.getDefaultLatitude()
        defaultLongitude = prefsManager.getDefaultLongitude()

        mapStrokeWidth = prefsManager.getStrokeWidth()
        @OptIn(FlowPreview::class)
        viewModelScope.launch(Dispatchers.IO) {
            _savedLocationColorUpdateFlow
                .debounce(100)
                .collectLatest { (id, color) -> repository.updateSavedLocationColor(id, color) }
        }

        viewModelScope.launch(Dispatchers.IO) { cleanDuplicateFavorites() }
    }

    fun setMapTarget(lat: Double, lon: Double) {
        mapTargetLocation = GeoPoint(lat, lon)
    }

    fun clearMapTarget() { mapTargetLocation = null }

    // ─── Toggles & Preferences ────────────────────────────────────

    fun toggleOfflineMode() { isOfflineMode = !isOfflineMode }

    fun toggleBiometric(enabled: Boolean) {
        isBiometricEnabled = enabled
        prefsManager.setBiometricEnabled(enabled)
    }

    fun toggleAirplaneMode() {
        isAirplaneMode = !isAirplaneMode
        prefsManager.setAirplaneMode(isAirplaneMode)
    }

    fun toggleAutoMode() {
        isAutoMode = !isAutoMode
        prefsManager.setAutoMode(isAutoMode)
    }

    fun toggleBle() {
        isBleEnabled = !isBleEnabled
        prefsManager.setBleEnabled(isBleEnabled)
        locationServiceManager.updateSettings()
    }

    fun toggleNearMe() {
        val newState = !_isNearMeEnabled.value
        _isNearMeEnabled.value = newState
        prefsManager.setNearMeEnabled(newState)
        locationServiceManager.updateSettings()
    }

    fun updateDefaultLocation(lat: Double, lon: Double) {
        defaultLatitude = lat; defaultLongitude = lon
    }

    fun updateDefaultLocationPersistently(lat: Double, lon: Double) {
        updateDefaultLocation(lat, lon)
        prefsManager.setDefaultLocation(lat, lon)
    }

    fun updateGroqApiKey(key: String) { groqApiKey = key }


    fun updateStrokeWidth(w: Float) {
        mapStrokeWidth = w
        viewModelScope.launch(Dispatchers.IO) { prefsManager.setStrokeWidth(w) }
    }
    fun esTourVisto(key: String): Boolean = prefsManager.isTourSeen(key)
    fun marcarTourVisto(key: String) { prefsManager.markTourSeen(key) }

    fun generateSuggestedRouteName(onResult: (String) -> Unit) {
        val datePart = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        onResult(datePart)
    }

    // ─── Location Service ─────────────────────────────────────────

    fun startPassiveLocationService() = locationServiceManager.startPassiveService()

    fun startTrackingRoute(name: String) {
        viewModelScope.launch {
            val routeId = repository.startNewRoute(name)
            locationServiceManager.startTracking(
                routeId, name,
                (updateIntervalSeconds * 1000).toLong(),
                updateDistanceMeters
            )
        }
    }

    fun stopTrackingRoute() = locationServiceManager.stopTracking()

    // ─── Waypoints ────────────────────────────────────────────────

    fun addWaypointToActiveRoute(name: String, description: String, photoPath: String? = null, author: String = "", latitude: Double? = null, longitude: Double? = null, color: String = "#00FF00") {
        val routeId = LocationService.currentRouteId.value ?: return
        viewModelScope.launch {
            val finalLat = latitude ?: LocationService.latestLocation.value?.latitude
            val finalLon = longitude ?: LocationService.latestLocation.value?.longitude
            if (finalLat != null && finalLon != null) repository.addWaypoint(routeId, name, description, finalLat, finalLon, photoPath, author, color)
        }
    }

    fun removeWaypointPhoto(waypointId: Int, routeId: Int) {
        viewModelScope.launch(Dispatchers.IO) { repository.removeWaypointPhoto(waypointId) }
    }

    fun removeFavoritePhoto(favoriteId: Int) {
        viewModelScope.launch(Dispatchers.IO) { repository.removeFavoritePhoto(favoriteId) }
    }

    // ─── Routes CRUD ──────────────────────────────────────────────

    fun deleteRoute(routeId: Int) { viewModelScope.launch { repository.deleteRoute(routeId) } }

    fun deleteRoutes(routeIds: Set<Int>) { viewModelScope.launch { repository.deleteRoutes(routeIds.toList()) } }

    fun renameRoute(routeId: Int, newName: String) {
        viewModelScope.launch {
            val route = repository.getRouteById(routeId)
            if (route != null) repository.updateRoute(route.copy(name = newName))
        }
    }

    fun clearAllAppData() { viewModelScope.launch { repository.deleteAllData() } }

    fun selectRouteForDetail(routeId: Int) {
        _selectedDetailRouteId.value = routeId
    }

    fun selectFavoriteForFocus(id: Int) { _selectedFavoriteId.value = id }

    fun clearFavoriteFocus() { _selectedFavoriteId.value = null }

    // ─── Flows: LocationService companion ─────────────────────────

    val isTrackingLive: StateFlow<Boolean> = LocationService.isTracking
    val activeRouteIdLive: StateFlow<Int?> = LocationService.currentRouteId
    val latestLocationLive: StateFlow<Location?> = LocationService.latestLocation
    val currentActivityMode: StateFlow<ActivityMode> = LocationService.currentActivityMode
    val elapsedSecondsLive: StateFlow<Long> = LocationService.elapsedTimeSeconds
    val distanceMetersLive: StateFlow<Double> = LocationService.currentDistanceMeters

    // ─── Flows: Repository-based ──────────────────────────────────

    val allRoutes: StateFlow<List<Route>> = repository.allRoutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeRouteInDb: StateFlow<Route?> = repository.activeRouteFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeRoutePoints: StateFlow<List<LocationPoint>> = activeRouteIdLive.flatMapLatest { id ->
        if (id != null) repository.getLocationPointsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeRouteWaypoints: StateFlow<List<Waypoint>> = activeRouteIdLive.flatMapLatest { id ->
        if (id != null) repository.getWaypointsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // ─── Saved Locations ──────────────────────────────────────────

    val allSavedLocations: StateFlow<List<SavedLocation>> = repository.allSavedLocations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFavorites: StateFlow<List<SavedLocation>> = allSavedLocations
    val groupedFavorites: StateFlow<Map<String, Map<String, List<SavedLocation>>>> = allSavedLocations.map { list ->
        list.groupBy { loc ->
            if (loc.country != "Desconocido") loc.country
            else LocationParser.parseFromAddressString(loc.address).first
        }.mapValues { entry ->
            entry.value.groupBy { loc ->
                if (loc.city != "Desconocida") loc.city
                else LocationParser.parseFromAddressString(loc.address).second
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedFavorite: StateFlow<SavedLocation?> = _selectedFavoriteId.flatMapLatest { id ->
        if (id != null) repository.getSavedLocationByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    fun getFavoritesForRouteFlow(routeId: Int): Flow<List<SavedLocation>> = repository.getFavoritesForRouteFlow(routeId)

    fun isFavoriteLinked(routeId: Int, favoriteId: Int): Flow<Boolean> =
        repository.getFavoritesForRouteFlow(routeId).map { favorites -> favorites.any { it.id == favoriteId } }.distinctUntilChanged()

    fun addFavoriteToRoute(routeId: Int, favoriteId: Int) { viewModelScope.launch { repository.addFavoriteToRoute(routeId, favoriteId) } }

    fun removeFavoriteFromRoute(routeId: Int, favoriteId: Int) { viewModelScope.launch { repository.removeFavoriteFromRoute(routeId, favoriteId) } }

    fun saveSpecificLocation(name: String, address: String, latitude: Double, longitude: Double, comment: String = "", author: String = "", photoPath: String? = null, phoneNumber: String = "", email: String = "", isContact: Boolean = false, color: String = "#FFD700") {
        viewModelScope.launch(Dispatchers.IO) {
            val finalComment = if (photoPath != null) {
                val datePart = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                val meta = "📸 Capturada el ${datePart}"
                if (comment.isBlank()) meta else "${comment}\n\n${meta}"
            } else comment
            val details = geocoderUseCase.fetchLocationDetails(latitude, longitude)
            val country = details.second.ifBlank { "Desconocido" }
            val city = details.third.ifBlank { "Desconocida" }
            repository.addSavedLocation(name, address, latitude, longitude, finalComment, author, photoPath, phoneNumber, email, isContact, country, city, color)
        }
    }
    fun updateSavedLocation(location: SavedLocation) { viewModelScope.launch(Dispatchers.IO) { repository.updateSavedLocation(location) } }

    fun deleteSavedLocation(savedLocation: SavedLocation) { viewModelScope.launch { repository.deleteSavedLocation(savedLocation) } }

    fun updateSavedLocationColorReactively(id: Int, color: String) {
        _savedLocationColorUpdateFlow.tryEmit(Pair(id, color))
    }

    fun refreshLocationDetails(location: SavedLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = geocoderUseCase.fetchLocationDetails(location.latitude, location.longitude)
            val country = details.second.ifBlank { "Desconocido" }
            val city = details.third.ifBlank { "Desconocida" }
            repository.updateSavedLocation(location.copy(address = details.first, country = country, city = city))
        }
    }

    suspend fun cleanDuplicateFavorites() {
        val allFavorites = repository.getAllSavedLocationsSync()
        val seen = mutableSetOf<String>()
        allFavorites.forEach { fav ->
            val fingerprint = "${fav.name}_${fav.latitude}_${fav.longitude}"
            if (seen.contains(fingerprint)) repository.deleteSavedLocation(fav) else seen.add(fingerprint)
        }
    }

    // ─── Geocoding ────────────────────────────────────────────────

    fun fetchCurrentAddress() {
        val location = latestLocationLive.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            currentAddress = geocoderUseCase.fetchAddress(location.latitude, location.longitude)
        }
    }

    fun clearCurrentAddress() { currentAddress = null }

    fun fetchAddressForCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = geocoderUseCase.fetchLocationDetails(lat, lon)
            currentAddress = details.first
            selectedPointAddress = details.first
        }
    }

    fun fetchAddressForLocation(lat: Double, lon: Double) {
        selectedPointAddress = "Buscando..."
        viewModelScope.launch(Dispatchers.IO) {
            selectedPointAddress = geocoderUseCase.fetchAddress(lat, lon)
        }
    }

    fun clearSelectedPointAddress() { selectedPointAddress = null }

    // ─── Photo ────────────────────────────────────────────────────

    fun getPhotoLocation(uri: Uri): Pair<Double, Double>? = photoManager.getPhotoLocation(uri)

    fun createImageFile(): Uri? = photoManager.createImageFile()

    suspend fun saveImageToInternalStorage(uri: Uri): Uri? = photoManager.saveImageToInternalStorage(uri)

    // ─── AI Analysis ──────────────────────────────────────────────

    fun analyzeRouteWithAI(routeId: Int) {
        val key = groqApiKey
        if (key.isNullOrBlank()) { aiAnalysisState = AiAnalysisState.MissingKey; return }
        viewModelScope.launch(Dispatchers.IO) {
            aiAnalysisState = AiAnalysisState.Loading
            try {
                val route = repository.getRouteById(routeId)
                if (route == null) { aiAnalysisState = AiAnalysisState.Error("Sin datos."); return@launch }
                val waypoints = repository.getWaypointsSync(routeId)
                val durationMs = if (route.endTime > route.startTime) route.endTime - route.startTime else 0L
                val result = groqAiUseCase.analyze(
                    apiKey = key, routeName = route.name,
                    distanceMeters = route.distanceMeters, maxSpeed = route.maxSpeed,
                    avgSpeed = route.avgSpeed, durationMs = durationMs, waypointCount = waypoints.size
                )
                aiAnalysisState = AiAnalysisState.Success(result)
            } catch (e: ApiKeyExpiredException) {
                aiAnalysisState = AiAnalysisState.ExpiredKey
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    aiAnalysisState = AiAnalysisState.ExpiredKey
                } else {
                    aiAnalysisState = AiAnalysisState.Error("Error de servidor: ${e.code()}")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                aiAnalysisState = AiAnalysisState.Error(t.message ?: "Error de conexión.")
            }
        }
    }

    fun updateApiKey(newKey: String) {
        groqApiKey = newKey
        aiAnalysisState = AiAnalysisState.Idle
    }

    fun resetAiState() {
        aiAnalysisState = AiAnalysisState.Idle
    }

    // ─── Export / Import ──────────────────────────────────────────

    fun exportRoutesAsCSV(routeId: Int) { viewModelScope.launch { routeExportUseCase.exportCsv(routeId) } }
    fun exportRoutesAsGPX(routeIds: Set<Int>) { viewModelScope.launch { routeExportUseCase.exportGpx(routeIds) } }

    fun importGpxFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isImporting = true
            try {
                routeExportUseCase.importGpx(uri)
            } catch (_: Exception) { } finally { isImporting = false }
        }
    }

    fun exportarRutaZip(routeId: Int) { viewModelScope.launch(Dispatchers.IO) { routeExportUseCase.exportZip(routeId) } }
    fun exportRoute(routeId: Int) { viewModelScope.launch(Dispatchers.IO) { routeExportUseCase.exportRouteAsZip(routeId) } }
    fun saveRouteZipToUri(routeId: Int, uri: Uri) { viewModelScope.launch(Dispatchers.IO) { routeExportUseCase.saveZipToUri(routeId, uri) } }

    fun importarRutaZip(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isImporting = true
            try {
                routeExportUseCase.importZip(uri)
            } catch (_: Exception) { } finally { isImporting = false }
        }
    }

    // ─── Backup ───────────────────────────────────────────────────

    suspend fun prepareBackupFile(): File? = backupUseCase.prepareBackup()

    fun restoreFromBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isImporting = true
            try {
                when (val decision = backupUseCase.detectRestoreType(uri)) {
                    is BackupUseCase.RestoreDecision.ProcessZip -> {
                        backupUseCase.restoreFromZip(uri)
                    }
                    is BackupUseCase.RestoreDecision.ProcessGpx -> {
                        routeExportUseCase.importGpx(uri)
                    }
                    is BackupUseCase.RestoreDecision.Invalid -> { }
                }
            } catch (_: Exception) { } finally { isImporting = false }
        }
    }

    // ─── Contacts ─────────────────────────────────────────────────

    fun getContactDetails(contactUri: Uri, onResult: (name: String, phone: String, email: String, address: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = contactUseCase.getContactDetails(contactUri)
            withContext(Dispatchers.Main) { onResult(details.name, details.phone, details.email, details.address) }
        }
    }

    // ─── Sharing ──────────────────────────────────────────────────

    fun shareLocation(location: SavedLocation) = shareUseCase.shareLocation(location)

    fun shareCompleteRoute(routeId: Int, mapBitmap: android.graphics.Bitmap? = null) {
        viewModelScope.launch { shareUseCase.shareCompleteRoute(routeId, mapBitmap) }
    }
}
