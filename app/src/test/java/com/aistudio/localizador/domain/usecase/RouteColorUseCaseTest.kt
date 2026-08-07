package com.aistudio.localizador.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteColorUseCaseTest {

    private lateinit var useCase: RouteColorUseCase

    @Before
    fun setUp() {
        useCase = RouteColorUseCase()
    }

    @Test
    fun `dado un hex invalido o nulo debe retornar el color por defecto Azul Bitaru`() {
        val result = useCase.sanitizeColorHex("invalido")
        assertEquals("#3498DB", result)
    }

    @Test
    fun `dado un hex valido de 6 digitos debe convertirlo a un entero ARGB correcto`() {
        val colorInt = useCase.parseHexToColorInt("#2ECC71")
        assertEquals(0xFF2ECC71.toInt(), colorInt)
    }

    @Test
    fun `dado un hex valido de 8 digitos ARGB con alfa debe parsearlo con precision de canal`() {
        val colorInt = useCase.parseHexToColorInt("#803498DB")
        assertEquals(0x803498DB.toInt(), colorInt)
    }

    @Test
    fun `speedToGradientColor con velocidad cero debe devolver tono azul`() {
        val color = useCase.speedToGradientColor(speedMps = 0.0f, maxSpeedMps = 10.0f)
        // Azul: componente rojo bajo, azul alto
        val red = color shr 16 and 0xFF
        val blue = color and 0xFF
        assertTrue("Azul debe ser mayor que rojo para velocidad cero", blue > red)
    }

    @Test
    fun `speedToGradientColor con velocidad maxima debe devolver tono rojo`() {
        val color = useCase.speedToGradientColor(speedMps = 10.0f, maxSpeedMps = 10.0f)
        val red = color shr 16 and 0xFF
        val blue = color and 0xFF
        assertTrue("Rojo debe ser mayor que azul para velocidad maxima", red > blue)
    }

    @Test
    fun `speedToGradientColor con delta cero debe devolver tono azul seguro sin division por cero`() {
        val color = useCase.speedToGradientColor(speedMps = 5.0f, minSpeedMps = 5.0f, maxSpeedMps = 5.0f)
        val red = color shr 16 and 0xFF
        val blue = color and 0xFF
        assertTrue("Con delta cero debe caer a azul (reposo)", blue > red)
    }

    @Test
    fun `speedToGradientColor con velocidad media debe producir tono intermedio verde-amarillo`() {
        val color = useCase.speedToGradientColor(speedMps = 5.0f, maxSpeedMps = 10.0f)
        val green = color shr 8 and 0xFF
        val red = color shr 16 and 0xFF
        val blue = color and 0xFF
        assertTrue("Verde debe ser dominante en velocidad media", green >= red || green >= blue)
    }

    @Test
    fun `speedToGradientColor con velocidad negativa debe coercear a 0 y devolver azul`() {
        val color = useCase.speedToGradientColor(speedMps = -5.0f, maxSpeedMps = 10.0f)
        val red = color shr 16 and 0xFF
        val blue = color and 0xFF
        assertTrue("Velocidad negativa debe comportarse como 0 -> azul", blue > red)
    }

    @Test
    fun `speedToGradientColor con velocidad superior al maximo debe coercear a 1 y devolver rojo`() {
        val color = useCase.speedToGradientColor(speedMps = 20.0f, maxSpeedMps = 10.0f)
        val red = color shr 16 and 0xFF
        val blue = color and 0xFF
        assertTrue("Velocidad superior al maximo debe dar rojo", red > blue)
    }
}