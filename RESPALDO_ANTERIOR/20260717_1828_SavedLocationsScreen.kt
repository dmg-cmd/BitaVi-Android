package com.aistudio.localizador.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import com.aistudio.localizador.ui.components.FullScreenPhotoDialog

import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.aistudio.localizador.ui.components.TourOverlay
import com.aistudio.localizador.ui.dialogs.DeleteConfirmationDialog
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import java.util.Locale
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.ui.model.IconCategory
import com.aistudio.localizador.ui.RouteViewModel
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    viewModel: RouteViewModel,
    onNavigateToMap: () -> Unit,
    onFocusFavorite: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    val locations by viewModel.allSavedLocations.collectAsState()
    val groupedFavorites by viewModel.groupedFavorites.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var locationToEdit by remember { mutableStateOf<SavedLocation?>(null) }
    var newName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isContact by remember { mutableStateOf(false) }
    var manualAddress by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FFD700") }
    
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoGpsLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var usePhotoGps by remember { mutableStateOf(false) }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var favoriteToDelete by remember { mutableStateOf<SavedLocation?>(null) }
    var showTourFotos by remember { mutableStateOf(!viewModel.esTourVisto("fotos")) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            capturedPhotoUri = null
        } else {
            // Intentar extraer GPS de la foto recién sacada
            capturedPhotoUri?.let { uri ->
                scope.launch {
                    val loc = viewModel.extractLocationFromPhoto(uri)
                    photoGpsLocation = loc
                    if (loc != null) usePhotoGps = true
                }
            }
        }
    }

    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingPhotoUri != null) {
            scope.launch {
                val uri = pendingPhotoUri!!
                photoGpsLocation = viewModel.extractLocationFromPhoto(uri)
                capturedPhotoUri = viewModel.saveImageToInternalStorage(uri)
                usePhotoGps = photoGpsLocation != null
            }
            pendingPhotoUri = null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasMediaLocation = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_MEDIA_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasMediaLocation) {
                    scope.launch {
                        val uri = it
                        photoGpsLocation = viewModel.extractLocationFromPhoto(uri)
                        capturedPhotoUri = viewModel.saveImageToInternalStorage(uri)
                        usePhotoGps = photoGpsLocation != null
                    }
                } else {
                    pendingPhotoUri = it
                    mediaLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
                }
            } else {
                scope.launch {
                    val uri = it
                    photoGpsLocation = viewModel.extractLocationFromPhoto(uri)
                    capturedPhotoUri = viewModel.saveImageToInternalStorage(uri)
                    usePhotoGps = photoGpsLocation != null
                }
            }
        }
    }

    // FASE 2: IMPORTAR CONTACTO
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            viewModel.getContactDetails(it) { cName, cPhone, cEmail, cAddress ->
                newName = cName
                phone = cPhone
                email = cEmail
                manualAddress = cAddress
                isContact = true
                showAddDialog = true
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permiso concedido. Pulse de nuevo.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permiso denegado.", Toast.LENGTH_LONG).show()
        }
    }

    // Al abrir el diálogo, disparamos la búsqueda de dirección actual
    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            viewModel.fetchCurrentAddress()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Favoritos y Destacados", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { contactPickerLauncher.launch(null) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Importar contacto")
                }
                FloatingActionButton(
                    onClick = { 
                        // Limpiar antes de abrir para modo manual
                        newName = ""; phone = ""; email = ""; manualAddress = ""; comment = ""; author = ""; isContact = false; capturedPhotoUri = null
                        showAddDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = "Guardar ubicación actual")
                }
            }
        }
) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (locations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes lugares guardados aun", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val expandedCountries = remember { mutableStateMapOf<String, Boolean>() }
                val expandedCities = remember { mutableStateMapOf<String, Boolean>() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedFavorites.forEach { (country, cities) ->
                        item(key = country) {
                            val isCountryExpanded = expandedCountries[country] ?: true
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedCountries[country] = !isCountryExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCountryExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                        contentDescription = if (isCountryExpanded) "Colapsar" else "Expandir",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = country,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                AnimatedVisibility(visible = isCountryExpanded) {
                                    Column {
                                        cities.forEach { (city, locs) ->
                                            val cityKey = "${country}|${city}"
                                            val isCityExpanded = expandedCities[cityKey] ?: true
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedCities[cityKey] = !isCityExpanded }
                                                    .padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isCityExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                                    contentDescription = if (isCityExpanded) "Colapsar" else "Expandir",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = city,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            AnimatedVisibility(visible = isCityExpanded) {
                                                Column {
                                                    locs.forEach { location ->
                                                        LocationCard(
                                                            location = location,
                                                            onDelete = {
                                                                favoriteToDelete = location
                                                                showDeleteConfirmation = true
                                                            },
                                                            onEdit = { locationToEdit = location },
                                                            onShowOnMap = {
                                                                android.util.Log.d("Navegacion", "Yendo a: ${location.name} en ${location.latitude}, ${location.longitude}")
                                                                viewModel.setMapTarget(location.latitude, location.longitude)
                                                                onNavigateToMap()
                                                            },
                                                            onShare = { viewModel.shareLocation(location) },
                                                            onFocus = { onFocusFavorite(location.id) },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val currentFetchedAddress = viewModel.currentAddress
        
        // Actualizar dirección si cambia el switch de ubicación de foto
        LaunchedEffect(usePhotoGps) {
            manualAddress = "" // Limpiamos para que tome la dirección del ViewModel
            if (usePhotoGps && photoGpsLocation != null) {
                viewModel.fetchAddressForCoordinates(photoGpsLocation!!.first, photoGpsLocation!!.second)
            } else if (!usePhotoGps) {
                viewModel.fetchCurrentAddress()
            }
        }

        val colors = listOf("#FFD700", "#4CAF50", "#2196F3", "#F44336", "#9C27B0", "#FF9800")
        
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                viewModel.clearCurrentAddress()
                capturedPhotoUri = null
            },
            title = { Text("Guardar ubicación actual") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nombre (ej. Casa, Trabajo)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isContact, onCheckedChange = { isContact = it })
                        Text("Es un Contacto", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isContact) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("¿Quién lo guarda?") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = manualAddress.ifBlank { currentFetchedAddress ?: "" }, onValueChange = { manualAddress = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comentario / Nota") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Color de la estrella:", style = MaterialTheme.typography.labelMedium)
                    
                    // Colores predefinidos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorHex.toColorInt()), RoundedCornerShape(16.dp))
                                    .clickable { selectedColor = colorHex }
                                    .padding(4.dp)
                            ) {
                                if (selectedColor == colorHex) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    // Selector de color libre (Slider de Matiz)
                    var hue by remember { 
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(selectedColor.toColorInt(), hsv)
                        mutableStateOf(hsv[0]) 
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Personalizado:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            )
                        }
                        Slider(
                            value = hue,
                            onValueChange = { 
                                hue = it
                                val newColor = String.format(Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                selectedColor = newColor
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (capturedPhotoUri != null) {
                        AsyncImage(
                            model = capturedPhotoUri,
                            contentDescription = "Foto",
                            modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray, RoundedCornerShape(8.dp))
                                .clickable { photoToZoom = capturedPhotoUri?.toString() }
                        )
                        
                        if (photoGpsLocation != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(if(usePhotoGps) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp))
                                    .clickable { usePhotoGps = !usePhotoGps }
                            ) {
                                Checkbox(checked = usePhotoGps, onCheckedChange = { usePhotoGps = it })
                                Text(
                                    "Foto tomada en: ${String.format(Locale.US, "%.4f", photoGpsLocation!!.first)}, ${String.format(Locale.US, "%.4f", photoGpsLocation!!.second)}", 
                                    style = MaterialTheme.typography.labelMedium, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "La foto tiene coordenadas grabadas. Marque para guardarla allí.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "Esta foto no contiene información de ubicación (GPS).",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        TextButton(onClick = { capturedPhotoUri = null; photoGpsLocation = null; usePhotoGps = false }) { Text("Quitar foto") }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPermission) {
                                        val uri = viewModel.createImageFile()
                                        if (uri != null) {
                                            capturedPhotoUri = uri
                                            cameraLauncher.launch(uri)
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Cámara", fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch(PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Galería", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        // Decidimos qué coordenadas usar
                        val finalLat: Double
                        val finalLon: Double
                        val finalAddress: String
                        
                        if (usePhotoGps && photoGpsLocation != null) {
                            finalLat = photoGpsLocation!!.first
                            finalLon = photoGpsLocation!!.second
                            finalAddress = viewModel.currentAddress ?: manualAddress
                        } else {
                            val currentLoc = viewModel.latestLocationLive.value
                            finalLat = currentLoc?.latitude ?: 0.0
                            finalLon = currentLoc?.longitude ?: 0.0
                            finalAddress = manualAddress.ifBlank { currentFetchedAddress ?: "" }
                        }

                        viewModel.saveSpecificLocation(
                            name = newName, 
                            address = finalAddress,
                            latitude = finalLat,
                            longitude = finalLon,
                            comment = comment,
                            author = author,
                            photoPath = capturedPhotoUri?.toString(),
                            phoneNumber = phone,
                            email = email,
                            isContact = isContact,
                            color = selectedColor
                        )
                        showAddDialog = false
                        newName = ""
                        author = ""
                        comment = ""
                        manualAddress = ""
                        phone = ""
                        email = ""
                        isContact = false
                        selectedColor = "#FFD700"
                        capturedPhotoUri = null
                        photoGpsLocation = null
                        usePhotoGps = false
                        viewModel.clearCurrentAddress()
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    viewModel.clearCurrentAddress()
                    capturedPhotoUri = null
                }) { Text("Cancelar") }
            }
        )
    }

    if (locationToEdit != null) {
        var editName by remember { mutableStateOf(locationToEdit!!.name) }
        var editComment by remember { mutableStateOf(locationToEdit!!.comment) }
        var editAuthor by remember { mutableStateOf(locationToEdit!!.author) }
        var editPhone by remember { mutableStateOf(locationToEdit!!.phoneNumber) }
        var editEmail by remember { mutableStateOf(locationToEdit!!.email) }
        var editIsContact by remember { mutableStateOf(locationToEdit!!.isContact) }
        var editColor by remember { mutableStateOf(locationToEdit!!.color) }
        
        val colors = listOf("#FFD700", "#4CAF50", "#2196F3", "#F44336", "#9C27B0", "#FF9800")

        AlertDialog(
            onDismissRequest = { locationToEdit = null },
            title = { Text("Editar Favorito") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editIsContact, onCheckedChange = { editIsContact = it })
                        Text("Es un Contacto", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (editIsContact) {
                        OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(value = editAuthor, onValueChange = { editAuthor = it }, label = { Text("¿Quién lo guarda?") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editComment, onValueChange = { editComment = it }, label = { Text("Comentario / Nota") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    // BOTÓN DE REFRESCO DE UBICACIÓN
                    OutlinedButton(
                        onClick = { viewModel.refreshLocationDetails(locationToEdit!!) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Actualizar País/Ciudad")
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Color de la estrella:", style = MaterialTheme.typography.labelMedium)
                    
                    // Colores predefinidos
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(colorHex.toColorInt()), RoundedCornerShape(16.dp))
                                    .clickable { 
                                        editColor = colorHex 
                                        viewModel.updateSavedLocationColorReactively(locationToEdit!!.id, colorHex)
                                    }
                                    .padding(4.dp)
                            ) {
                                if (editColor == colorHex) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    // Selector de color libre (Slider de Matiz)
                    var hue by remember { 
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(editColor.toColorInt(), hsv)
                        mutableStateOf(hsv[0]) 
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Personalizado:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            )
                        }
                        Slider(
                            value = hue,
                            onValueChange = { 
                                hue = it
                                val newColor = String.format(Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                editColor = newColor
                                viewModel.updateSavedLocationColorReactively(locationToEdit!!.id, newColor)
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSavedLocation(
                        locationToEdit!!.copy(
                            name = editName,
                            comment = editComment,
                            author = editAuthor,
                            phoneNumber = editPhone,
                            email = editEmail,
                            isContact = editIsContact,
                            color = editColor
                        )
                    )
                    locationToEdit = null
                }) { Text("Guardar Cambios") }
            },
            dismissButton = {
                TextButton(onClick = { locationToEdit = null }) { Text("Cancelar") }
            }
        )
    }

    if (showDeleteConfirmation && favoriteToDelete != null) {
        DeleteConfirmationDialog(
            title = "¿Eliminar de favoritos?",
            message = "Esta acción eliminará el elemento seleccionado de tus favoritos de forma permanente. ¿Deseas continuar?",
            onConfirm = {
                favoriteToDelete?.let { viewModel.deleteSavedLocation(it) }
                showDeleteConfirmation = false
                favoriteToDelete = null
            },
            onDismiss = {
                showDeleteConfirmation = false
                favoriteToDelete = null
            }
        )
    }

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )

    if (showTourFotos) {
        TourOverlay(
            message = "¡Toca cualquier foto para verla a pantalla completa!",
            onDismiss = {
                showTourFotos = false
                viewModel.marcarTourVisto("fotos")
            }
        )
    }


}

@Composable
fun LocationCard(
    location: SavedLocation,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShowOnMap: () -> Unit,
    onShare: () -> Unit,
    onFocus: () -> Unit
) {
    val context = LocalContext.current
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onShowOnMap() }, // <--- Acción principal: IR AL MAPA
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (location.photoPath != null) {
                AsyncImage(
                    model = location.photoPath,
                    contentDescription = "Foto del lugar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color.LightGray)
                        .clickable { photoToZoom = location.photoPath }
                )
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = IconCategory.fromKey(location.iconCategory).icon,
                            contentDescription = null,
                            tint = Color(location.color.toColorInt()),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = location.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (location.author.isNotBlank()) {
                                Text(
                                    text = "Por: ${location.author}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    
                    // BOTÓN COMPARTIR DESTACADO
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (location.address.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = location.address, style = MaterialTheme.typography.bodyMedium)
                }

                if (location.comment.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = location.comment,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Acciones de contacto (Chips)
                if (location.isContact) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (location.phoneNumber.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = "tel:${location.phoneNumber}".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                label = { Text("Llamar") },
                                leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        if (location.email.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:${location.email}".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                label = { Text("Email") },
                                leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                
                // BARRA DE ACCIONES INFERIOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Ver en Google Maps
                        TextButton(onClick = {
                            val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.name})"
                            val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val browserUri = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, browserUri.toUri()))
                            }
                        }) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Google Maps", fontSize = 12.sp)
                        }

                        // Exportar a Contactos
                        if (location.isContact) {
                            IconButton(onClick = {
                                val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                                    type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.NAME, location.name)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, location.phoneNumber)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, location.email)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.POSTAL, location.address)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.NOTES, "BiTaRu: ${location.comment}")
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp), tint = Color(0xFF2196F3))
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onFocus) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Ver en detalle", tint = Color(0xFF4CAF50))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )
}
























