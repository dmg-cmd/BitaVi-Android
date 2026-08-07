package com.aistudio.localizador.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationParserTest {

    private lateinit var locationParser: LocationParser

    @Before
    fun setUp() {
        locationParser = LocationParser()
    }

    @Test
    fun `dado un formato valido de ciudad y pais debe retornar exito con ambos campos mapeados`() {
        val raw = "Buenos Aires, Argentina"
        val result = locationParser.inferCountryCity(raw)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals("Argentina", data?.country)
        assertEquals("Buenos Aires", data?.city)
    }

    @Test
    fun `dada una cadena vacia debe retornar una falla controlada sin lanzar excepcion`() {
        val result = locationParser.inferCountryCity("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `el metodo estatico parseFromAddressString debe preservar compatibilidad con el ViewModel`() {
        val (city, country) = LocationParser.parseFromAddressString("Madrid, Espana")
        assertEquals("Madrid", city)
        assertEquals("Espana", country)
    }

    @Test
    fun `parseFromAddressString con una sola parte debe devolver la parte como ciudad y Desconocido como pais`() {
        val (city, country) = LocationParser.parseFromAddressString("SoloCiudad")
        assertEquals("SoloCiudad", city)
        assertEquals("Desconocido", country)
    }
}
