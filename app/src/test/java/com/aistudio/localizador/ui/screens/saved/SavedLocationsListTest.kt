package com.aistudio.localizador.ui.screens.saved

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.domain.usecase.CityFavoritesGroup
import com.aistudio.localizador.domain.usecase.CountryFavoritesGroup
import com.aistudio.localizador.ui.theme.LocalizadorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de regresión de la lista de favoritos agrupada por país/ciudad.
 * Cubre la extracción realizada en la FASE 1.2 del Plan Maestro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedLocationsListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun location(id: Int, name: String, city: String, country: String) = SavedLocation(
        id = id,
        name = name,
        latitude = 1.0,
        longitude = 2.0,
        city = city,
        country = country
    )

    private val sampleGroup = CountryFavoritesGroup(
        countryName = "Argentina",
        cities = listOf(
            CityFavoritesGroup(
                cityName = "Buenos Aires",
                locations = listOf(location(1, "Obelisco", "Buenos Aires", "Argentina"))
            )
        )
    )

    private fun render() {
        composeTestRule.setContent {
            LocalizadorTheme {
                SavedLocationsList(
                    groupedFavorites = listOf(sampleGroup),
                    onShowOnMap = {},
                    onShare = {},
                    onEdit = {},
                    onDelete = {},
                    onFocus = {}
                )
            }
        }
    }

    @Test
    fun muestraPaisCiudadYNombreDelLugar() {
        render()
        composeTestRule.onNodeWithText("Argentina").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buenos Aires").assertIsDisplayed()
        composeTestRule.onNodeWithText("Obelisco").assertIsDisplayed()
    }

    @Test
    fun colapsarPaisOcultaSusLugares() {
        render()
        composeTestRule.onNodeWithText("Obelisco").assertIsDisplayed()
        composeTestRule.onNodeWithText("Argentina").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Obelisco").assertDoesNotExist()
    }
}