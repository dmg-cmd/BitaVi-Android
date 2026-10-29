package com.aistudio.localizador.ui.screens.saved

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.aistudio.localizador.domain.usecase.ExtractPhotoLocationUseCase
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.components.FullScreenPhotoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SaveLocationDialog(
    viewModel: RouteViewModel,
    autoImportContact: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
    var inputLatitude by remember { mutableStateOf("") }
    var inputLongitude by remember { mutableStateOf("") }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    var selectedPhotoTimestamp by remember { mutableStateOf<Long?>(null) }

    fun resetState() {
        newName = ""; author = ""; comment = ""; manualAddress = ""
        phone = ""; email = ""; isContact = false; selectedColor = "#FFD700"
        capturedPhotoUri = null; photoGpsLocation = null; selectedPhotoTimestamp = null
        usePhotoGps = false; inputLatitude = ""; inputLongitude = ""
    }

    fun applyPhotoMetadata(metadata: ExtractPhotoLocationUseCase.PhotoMetadata?) {
        if (metadata != null) {
            photoGpsLocation = Pair(metadata.latitude, metadata.longitude)
            inputLatitude = String.format(Locale.US, "%.6f", metadata.latitude)
            inputLongitude = String.format(Locale.US, "%.6f", metadata.longitude)
            usePhotoGps = true
            selectedPhotoTimestamp = metadata.timestamp
            viewModel.fetchPhotoAddress(metadata.latitude, metadata.longitude)
            if (newName.isBlank()) {
                val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(metadata.timestamp ?: System.currentTimeMillis()))
                newName = "Foto $timeStr"
            }
        } else {
            photoGpsLocation = null
            usePhotoGps = false
            selectedPhotoTimestamp = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uriToProcess = capturedPhotoUri
        if (success && uriToProcess != null) {
            scope.launch(Dispatchers.IO) {
                val metadata = viewModel.extractLocationFromPhoto(uriToProcess)
                withContext(Dispatchers.Main) { applyPhotoMetadata(metadata) }
            }
        } else {
            capturedPhotoUri = null
        }
    }

    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val targetUri = pendingPhotoUri
        pendingPhotoUri = null
        if (targetUri != null) {
            scope.launch(Dispatchers.IO) {
                val metadata = viewModel.extractLocationFromPhoto(targetUri)
                val savedUri = viewModel.saveImageToInternalStorage(targetUri)
                withContext(Dispatchers.Main) {
                    capturedPhotoUri = savedUri ?: targetUri
                    applyPhotoMetadata(metadata)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { targetUri ->
            val hasMediaLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true
            if (hasMediaLocation) {
                scope.launch(Dispatchers.IO) {
                    val metadata = viewModel.extractLocationFromPhoto(targetUri)
                    val savedUri = viewModel.saveImageToInternalStorage(targetUri)
                    withContext(Dispatchers.Main) {
                        capturedPhotoUri = savedUri ?: targetUri
                        applyPhotoMetadata(metadata)
                    }
                }
            } else {
                pendingPhotoUri = targetUri
                mediaLocationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
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
            }
        }
    }

    LaunchedEffect(autoImportContact) {
        if (autoImportContact) contactPickerLauncher.launch(null)
    }

    LaunchedEffect(Unit) {
        val loc = viewModel.latestLocationLive.value
        if (loc != null) {
            inputLatitude = String.format(Locale.US, "%.6f", loc.latitude)
            inputLongitude = String.format(Locale.US, "%.6f", loc.longitude)
        }
        viewModel.fetchCurrentAddress()
    }

    LaunchedEffect(usePhotoGps) {
        manualAddress = ""
        if (!usePhotoGps) {
            viewModel.fetchCurrentAddress()
        }
    }

    val colors = listOf("#FFD700", "#4CAF50", "#2196F3", "#F44336", "#9C27B0", "#FF9800")

    AlertDialog(
        onDismissRequest = {
            viewModel.clearCurrentAddress()
            onDismiss()
        },
        title = { Text("Guardar ubicación") },
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
                OutlinedTextField(
                    value = manualAddress.ifBlank { if (usePhotoGps) { viewModel.photoExtractedAddress ?: "Obteniendo dirección..." } else { viewModel.currentAddress ?: "" } },
                    onValueChange = { manualAddress = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = if (usePhotoGps) "📷 Guardar ubicación de la foto" else "📍 Guardar ubicación actual del GPS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !usePhotoGps,
                        onClick = {
                            usePhotoGps = false
                            val loc = viewModel.latestLocationLive.value
                            if (loc != null) {
                                inputLatitude = String.format(Locale.US, "%.6f", loc.latitude)
                                inputLongitude = String.format(Locale.US, "%.6f", loc.longitude)
                            }
                        },
                        label = { Text("📍 GPS del teléfono") }
                    )
                    FilterChip(
                        selected = usePhotoGps,
                        onClick = {
                            usePhotoGps = true
                            photoGpsLocation?.let {
                                inputLatitude = String.format(Locale.US, "%.6f", it.first)
                                inputLongitude = String.format(Locale.US, "%.6f", it.second)
                            }
                        },
                        label = { Text("📷 Ubicación de la foto") },
                        enabled = photoGpsLocation != null
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = inputLatitude, onValueChange = { inputLatitude = it; usePhotoGps = false }, label = { Text("Latitud") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = inputLongitude, onValueChange = { inputLongitude = it; usePhotoGps = false }, label = { Text("Longitud") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))) {
                    AndroidView(factory = { ctx ->
                        MapView(ctx).apply {
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            setTileSource(TileSourceFactory.MAPNIK)
                        }
                    }, update = { mapView ->
                        val lat = inputLatitude.toDoubleOrNull() ?: return@AndroidView
                        val lon = inputLongitude.toDoubleOrNull() ?: return@AndroidView
                        val point = GeoPoint(lat, lon)
                        mapView.controller.animateTo(point)
                        mapView.overlays.clear()
                        mapView.overlays.add(Marker(mapView).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })
                        mapView.invalidate()
                    })
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comentario / Nota") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                Text("Color de la estrella:", style = MaterialTheme.typography.labelMedium)

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
                                .background(if (usePhotoGps) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { usePhotoGps = !usePhotoGps }
                        ) {
                            Checkbox(checked = usePhotoGps, onCheckedChange = { usePhotoGps = it })
                            Text(
                                "Foto tomada en: ${String.format(Locale.US, "%.4f", photoGpsLocation!!.first)}, ${String.format(Locale.US, "%.4f", photoGpsLocation!!.second)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                                val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasCameraPermission) {
                                    val uri = viewModel.createImageFile()
                                    if (uri != null) {
                                        capturedPhotoUri = uri
                                        cameraLauncher.launch(uri)
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Cámara", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
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
                val finalName = newName.ifBlank {
                    if (usePhotoGps) {
                        val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(selectedPhotoTimestamp ?: System.currentTimeMillis()))
                        "Foto $timeStr"
                    } else {
                        manualAddress.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "Lugar guardado"
                    }
                }

                val finalLat = inputLatitude.toDoubleOrNull() ?: 0.0
                val finalLon = inputLongitude.toDoubleOrNull() ?: 0.0
                val finalAddress = manualAddress.ifBlank {
                    if (usePhotoGps) {
                        viewModel.photoExtractedAddress ?: String.format(Locale.US, "GPS Foto: %.5f, %.5f", finalLat, finalLon)
                    } else {
                        viewModel.currentAddress ?: String.format(Locale.US, "GPS: %.5f, %.5f", finalLat, finalLon)
                    }
                }

                viewModel.saveSpecificLocation(
                    name = finalName,
                    address = finalAddress,
                    latitude = finalLat,
                    longitude = finalLon,
                    comment = comment,
                    author = author,
                    photoPath = capturedPhotoUri?.toString(),
                    phoneNumber = phone,
                    email = email,
                    isContact = isContact,
                    color = selectedColor,
                    photoTimestamp = selectedPhotoTimestamp
                )
                Toast.makeText(context, "Ubicación guardada con éxito", Toast.LENGTH_SHORT).show()
                resetState()
                viewModel.clearCurrentAddress()
                onDismiss()
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearCurrentAddress()
                resetState()
                onDismiss()
            }) { Text("Cancelar") }
        }
    )

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )
}
