package com.aistudio.localizador.ui.screens.map

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.components.FullScreenPhotoDialog
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WaypointDialog(
    viewModel: RouteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wpName by remember { mutableStateOf("") }
    var wpAuthor by remember { mutableStateOf("") }
    var wpComment by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    val colors = listOf("#4CAF50", "#FFD700", "#2196F3", "#F44336", "#9C27B0", "#FF9800")

    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoGpsLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var usePhotoGps by remember { mutableStateOf(false) }
    var photoTimestamp by remember { mutableStateOf<Long?>(null) }
    var photoToZoom by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            capturedPhotoUri = null
        } else {
            capturedPhotoUri?.let { uri ->
                scope.launch {
                    val metadata = viewModel.extractLocationFromPhoto(uri)
                    photoGpsLocation = metadata?.let { Pair(it.latitude, it.longitude) }
                    photoTimestamp = metadata?.timestamp
                    usePhotoGps = photoGpsLocation != null
                }
            }
        }
    }
    val mediaLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val targetUri = pendingPhotoUri
        pendingPhotoUri = null
        if (targetUri != null) {
            scope.launch {
                val metadata = viewModel.extractLocationFromPhoto(targetUri)
                val internalUri = viewModel.saveImageToInternalStorage(targetUri)
                capturedPhotoUri = internalUri ?: targetUri
                photoGpsLocation = metadata?.let { Pair(it.latitude, it.longitude) }
                photoTimestamp = metadata?.timestamp
                usePhotoGps = photoGpsLocation != null
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { targetUri ->
            val hasMediaLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true
            if (hasMediaLocation) {
                scope.launch {
                    val metadata = viewModel.extractLocationFromPhoto(targetUri)
                    val internalUri = viewModel.saveImageToInternalStorage(targetUri)
                    capturedPhotoUri = internalUri ?: targetUri
                    photoGpsLocation = metadata?.let { Pair(it.latitude, it.longitude) }
                    photoTimestamp = metadata?.timestamp
                    usePhotoGps = photoGpsLocation != null
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
            Toast.makeText(context, "Permiso de cámara concedido. Pulse de nuevo para tomar la foto.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara para sacar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(usePhotoGps) {
        if (usePhotoGps && photoGpsLocation != null) {
            viewModel.fetchAddressForCoordinates(photoGpsLocation!!.first, photoGpsLocation!!.second)
        } else if (!usePhotoGps) {
            viewModel.fetchCurrentAddress()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.currentAddress == null) {
            viewModel.fetchCurrentAddress()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Punto de Interés") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = wpName, onValueChange = { wpName = it }, label = { Text("Nombre (ej. Gasolinera)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = wpAuthor, onValueChange = { wpAuthor = it }, label = { Text("Autor (quién marca)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = wpComment,
                    onValueChange = { wpComment = it },
                    label = { Text("Comentario / Nota") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(viewModel.currentAddress ?: "") }
                )
                Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(12.dp))

                if (capturedPhotoUri != null) {
                    AsyncImage(
                        model = capturedPhotoUri,
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { photoToZoom = capturedPhotoUri?.toString() }
                    )

                    if (photoGpsLocation != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp).clickable { usePhotoGps = !usePhotoGps }
                        ) {
                            Checkbox(checked = usePhotoGps, onCheckedChange = { usePhotoGps = it })
                            Text("Usar ubicación de la foto", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
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
                            onClick = { galleryLauncher.launch("image/*") },
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
                val finalLat: Double
                val finalLon: Double
                if (usePhotoGps && photoGpsLocation != null) {
                    finalLat = photoGpsLocation!!.first
                    finalLon = photoGpsLocation!!.second
                } else {
                    val currentLoc = viewModel.latestLocationLive.value
                    finalLat = currentLoc?.latitude ?: 0.0
                    finalLon = currentLoc?.longitude ?: 0.0
                }

                viewModel.addWaypointToActiveRoute(
                    name = wpName.ifBlank { "Punto" },
                    description = wpComment.ifBlank { viewModel.currentAddress ?: "" },
                    author = wpAuthor,
                    photoPath = capturedPhotoUri?.toString(),
                    latitude = finalLat,
                    longitude = finalLon,
                    color = selectedColor
                )
                onDismiss()
            }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    FullScreenPhotoDialog(
        photoUri = photoToZoom,
        onDismiss = { photoToZoom = null }
    )
}
