package com.aistudio.localizador.ui.screens.saved

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.ui.components.FullScreenPhotoDialog
import com.aistudio.localizador.ui.model.IconCategory
import java.text.SimpleDateFormat
import java.util.Date

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
    val configuration = LocalConfiguration.current
    val formattedDate = remember(location.timestamp, configuration) {
        val locale = configuration.locales[0]
        SimpleDateFormat("dd/MM/yyyy HH:mm", locale).format(Date(location.timestamp))
    }
    var photoToZoom by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onShowOnMap() },
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

                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Fecha de captura",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

                        if (location.isContact) {
                            IconButton(onClick = {
                                val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                                    type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.NAME, location.name)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, location.phoneNumber)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, location.email)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.POSTAL, location.address)
                                    putExtra(android.provider.ContactsContract.Intents.Insert.NOTES, "BitaVi: ${location.comment}")
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
