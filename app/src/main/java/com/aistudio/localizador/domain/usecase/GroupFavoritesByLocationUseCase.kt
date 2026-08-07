package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.model.SavedLocation
import javax.inject.Inject

data class CityFavoritesGroup(
    val cityName: String,
    val locations: List<SavedLocation>
)

data class CountryFavoritesGroup(
    val countryName: String,
    val cities: List<CityFavoritesGroup>
)

class GroupFavoritesByLocationUseCase @Inject constructor() {

    operator fun invoke(locations: List<SavedLocation>): List<CountryFavoritesGroup> {
        if (locations.isEmpty()) return emptyList()

        return locations
            .groupBy { it.country.ifBlank { "Sin Pa\u00eds" } }
            .map { (country, countryLocations) ->
                val cityGroups = countryLocations
                    .groupBy { it.city.ifBlank { "General" } }
                    .map { (city, cityLocations) ->
                        CityFavoritesGroup(
                            cityName = city,
                            locations = cityLocations.sortedBy { it.name }
                        )
                    }
                    .sortedBy { it.cityName }

                CountryFavoritesGroup(
                    countryName = country,
                    cities = cityGroups
                )
            }
            .sortedBy { it.countryName }
    }
}
