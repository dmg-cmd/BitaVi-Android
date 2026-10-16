package com.aistudio.localizador.ui.screens.saved

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.ui.theme.LocalizadorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de regresión de la tarjeta individual de ubicación guardada.
 * Cubre la extracción realizada en la FASE 1.2 del Plan Maestro.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun render(location: SavedLocation) {
        composeTestRule.setContent {
            LocalizadorTheme {
                LocationCard(
                    location = location,
                    onDelete = {},
                    onEdit = {},
                    onShowOnMap = {},
                    onShare = {},
                    onFocus = {}
                )
            }
        }
    }

    @Test
    fun muestraNombreAutorYDireccion() {
        render(
            SavedLocation(
                id = 1,
                name = "Café Central",
                address = "Av. Siempreviva 742",
                latitude = 1.0,
                longitude = 2.0,
                author = "Ana"
            )
        )
        composeTestRule.onNodeWithText("Café Central").assertIsDisplayed()
        composeTestRule.onNodeWithText("Por: Ana").assertIsDisplayed()
        composeTestRule.onNodeWithText("Av. Siempreviva 742").assertIsDisplayed()
    }

    @Test
    fun muestraComentarioCuandoExiste() {
        render(
            SavedLocation(
                id = 2,
                name = "Mirador",
                latitude = 3.0,
                longitude = 4.0,
                comment = "Vista increíble"
            )
        )
        composeTestRule.onNodeWithText("Mirador").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vista increíble").assertIsDisplayed()
    }
}