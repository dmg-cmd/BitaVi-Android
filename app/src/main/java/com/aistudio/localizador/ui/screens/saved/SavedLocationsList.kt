package com.aistudio.localizador.ui.screens.saved

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.domain.usecase.CountryFavoritesGroup

@Composable
fun SavedLocationsList(
    groupedFavorites: List<CountryFavoritesGroup>,
    onShowOnMap: (SavedLocation) -> Unit,
    onShare: (SavedLocation) -> Unit,
    onEdit: (SavedLocation) -> Unit,
    onDelete: (SavedLocation) -> Unit,
    onFocus: (SavedLocation) -> Unit,
    modifier: Modifier = Modifier,
    collapsedCountries: Set<String> = emptySet(),
    collapsedCities: Set<String> = emptySet(),
    onToggleCountry: ((String) -> Unit)? = null,
    onToggleCity: ((String) -> Unit)? = null
) {
    val localExpandedCountries = remember { mutableStateMapOf<String, Boolean>() }
    val localExpandedCities = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedFavorites.forEach { group ->
            val country = group.countryName
            item(key = country) {
                val isCountryExpanded = if (onToggleCountry != null) {
                    !collapsedCountries.contains(country)
                } else {
                    localExpandedCountries[country] ?: true
                }
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (onToggleCountry != null) {
                                    onToggleCountry(country)
                                } else {
                                    localExpandedCountries[country] = !isCountryExpanded
                                }
                            }
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
                            group.cities.forEach { cityGroup ->
                                val city = cityGroup.cityName
                                val cityKey = "${country}|${city}"
                                val isCityExpanded = if (onToggleCity != null) {
                                    !collapsedCities.contains(cityKey)
                                } else {
                                    localExpandedCities[cityKey] ?: true
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (onToggleCity != null) {
                                                onToggleCity(cityKey)
                                            } else {
                                                localExpandedCities[cityKey] = !isCityExpanded
                                            }
                                        }
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
                                        cityGroup.locations.forEach { location ->
                                            LocationCard(
                                                location = location,
                                                onDelete = { onDelete(location) },
                                                onEdit = { onEdit(location) },
                                                onShowOnMap = { onShowOnMap(location) },
                                                onShare = { onShare(location) },
                                                onFocus = { onFocus(location) },
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
