package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.RouteViewModel
import com.example.ui.components.FullScreenPhotoDialog
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusFavoriteScreen(
    viewModel: RouteViewModel,
    favoriteId: Int,
    onBack: () -> Unit
) {
    LaunchedEffect(favoriteId) { viewModel.selectFavoriteForFocus(favoriteId) }

    val favorite by viewModel.selectedFavorite.collectAsState()
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val composeGraphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        SideEffect {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // ��� RAIZ PURA: Sin insets. Android controla su barra nativa; nosotros solo íconos blancos.
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ─── CONTENEDOR CAPTURABLE: Mapa + Card ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    composeGraphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(composeGraphicsLayer)
                }
        ) {
            favorite?.let { fav ->
                // 🗺️ Mapa con clipToBounds + candado táctil anti-flicker
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                controller.setZoom(16.5)
                                controller.setCenter(GeoPoint(fav.latitude, fav.longitude))
                                fitsSystemWindows = false
                                setOnTouchListener { v, event ->
                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                    false
                                }
                                overlays.add(Marker(this).apply {
                                    position = GeoPoint(fav.latitude, fav.longitude)
                                    title = fav.name
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    icon = context.getDrawable(android.R.drawable.btn_star_big_on)
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 🏷️ Card premium protegida por navigationBarsPadding
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)

                        .padding(start = 16.dp, end = 16.dp, bottom = 40.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC121212)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column {
                        if (!fav.photoPath.isNullOrBlank()) {
                            AsyncImage(
                                model = fav.photoPath,
                                contentDescription = "Foto del lugar",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .clickable { photoToZoom = fav.photoPath },
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            // ✨ Watermark premium
                            Text(
                                text = context.getString(R.string.app_name).lowercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "\uD83D\uDCCC ${fav.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (fav.comment.isNotBlank()) {
                                Text(
                                    text = "\uD83D\uDCDD ${fav.comment}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "\uD83D\uDDFA\uFE0F ${String.format(Locale.US, "%.4f, %.4f", fav.latitude, fav.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Green
                                )
                                val dateStr = remember(fav.timestamp) {
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fav.timestamp))
                                }
                                Text(
                                    text = "\u23F1\uFE0F $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // ─── BOTONES FLOTANTES protegidos por statusBarsPadding ───
        IconButton(
            onClick = { onBack(); viewModel.clearFavoriteFocus() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        favorite?.let { fav ->
            IconButton(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val imageBitmap = composeGraphicsLayer.toImageBitmap()
                        val bitmap = imageBitmap.asAndroidBitmap()
                        val cacheDir = File(context.cacheDir, "shared_posts").apply { mkdirs() }
                        val file = File(cacheDir, "postal_${fav.id}.png")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Enviar Postal T\u00E1ctica..."))
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir Postal", tint = Color.Green)
            }
        }

        FullScreenPhotoDialog(
            photoUri = photoToZoom,
            onDismiss = { photoToZoom = null }
        )
    }
}
