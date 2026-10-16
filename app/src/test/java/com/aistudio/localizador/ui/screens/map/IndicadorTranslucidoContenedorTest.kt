package com.aistudio.localizador.ui.screens.map

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.aistudio.localizador.ui.theme.LocalizadorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de regresión del contenedor translúcido reutilizable extraído en la FASE 1.1.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IndicadorTranslucidoContenedorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun renderizaElContenidoProvisto() {
        composeTestRule.setContent {
            LocalizadorTheme {
                IndicadorTranslucidoContenedor {
                    Text("Indicador de prueba")
                }
            }
        }
        composeTestRule.onNodeWithText("Indicador de prueba").assertIsDisplayed()
    }
}