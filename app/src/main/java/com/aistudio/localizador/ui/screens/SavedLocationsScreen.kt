package com.aistudio.localizador.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.ui.RouteViewModel
import com.aistudio.localizador.ui.components.TourOverlay
import com.aistudio.localizador.ui.dialogs.DeleteConfirmationDialog
import com.aistudio.localizador.ui.screens.saved.EditLocationDialog
import com.aistudio.localizador.ui.screens.saved.SaveLocationDialog
import com.aistudio.localizador.ui.screens.saved.SavedLocationsList

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
    val collapsedCountries by viewModel.collapsedFavoriteCountries.collectAsState()
    val collapsedCities by viewModel.collapsedFavoriteCities.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var addFromContact by remember { mutableStateOf(false) }
    var locationToEdit by remember { mutableStateOf<SavedLocation?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var favoriteToDelete by remember { mutableStateOf<SavedLocation?>(null) }
    var showTourFotos by remember { mutableStateOf(!viewModel.esTourVisto("fotos")) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favoritos y Destacados",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (locations.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.collapseAllFavorites() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.UnfoldLess,
                                contentDescription = "Contraer todo",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.expandAllFavorites() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = "Expandir todo",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
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
                    onClick = {
                        addFromContact = true
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Importar contacto")
                }
                FloatingActionButton(
                    onClick = {
                        addFromContact = false
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
                SavedLocationsList(
                    groupedFavorites = groupedFavorites,
                    collapsedCountries = collapsedCountries,
                    collapsedCities = collapsedCities,
                    onToggleCountry = { viewModel.toggleFavoriteCountryCollapse(it) },
                    onToggleCity = { viewModel.toggleFavoriteCityCollapse(it) },
                    onShowOnMap = { location ->
                        android.util.Log.d("Navegacion", "Yendo a: ${location.name} en ${location.latitude}, ${location.longitude}")
                        viewModel.setMapTarget(location.latitude, location.longitude)
                        onNavigateToMap()
                    },
                    onShare = { location -> viewModel.shareLocation(location) },
                    onEdit = { location -> locationToEdit = location },
                    onDelete = { location ->
                        favoriteToDelete = location
                        showDeleteConfirmation = true
                    },
                    onFocus = { location -> onFocusFavorite(location.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        SaveLocationDialog(
            viewModel = viewModel,
            autoImportContact = addFromContact,
            onDismiss = { showAddDialog = false }
        )
    }

    if (locationToEdit != null) {
        EditLocationDialog(
            location = locationToEdit!!,
            viewModel = viewModel,
            onDismiss = { locationToEdit = null }
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
