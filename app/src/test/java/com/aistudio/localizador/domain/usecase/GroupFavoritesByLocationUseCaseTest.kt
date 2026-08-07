package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.model.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GroupFavoritesByLocationUseCaseTest {

    private lateinit var useCase: GroupFavoritesByLocationUseCase

    @Before
    fun setUp() {
        useCase = GroupFavoritesByLocationUseCase()
    }

    @Test
    fun `invoke debe agrupar correctamente por Pais y Ciudad`() {
        val loc1 = SavedLocation(id = 1, name = "Casa", country = "Espa\u00f1a", city = "Madrid", latitude = 0.0, longitude = 0.0)
        val loc2 = SavedLocation(id = 2, name = "Caf\u00e9", country = "Espa\u00f1a", city = "Madrid", latitude = 0.0, longitude = 0.0)
        val loc3 = SavedLocation(id = 3, name = "Hotel", country = "Francia", city = "Par\u00eds", latitude = 0.0, longitude = 0.0)

        val result = useCase(listOf(loc1, loc2, loc3))

        assertEquals(2, result.size)

        val espanaGroup = result.first { it.countryName == "Espa\u00f1a" }
        assertEquals(1, espanaGroup.cities.size)
        assertEquals("Madrid", espanaGroup.cities[0].cityName)
        assertEquals(2, espanaGroup.cities[0].locations.size)

        val franciaGroup = result.first { it.countryName == "Francia" }
        assertEquals(1, franciaGroup.cities.size)
        assertEquals("Par\u00eds", franciaGroup.cities[0].cityName)
        assertEquals(1, franciaGroup.cities[0].locations.size)
    }

    @Test
    fun `invoke debe asignar Sin Pais y General a campos vacios`() {
        val loc1 = SavedLocation(id = 1, name = "Punto X", country = "", city = "", latitude = 0.0, longitude = 0.0)

        val result = useCase(listOf(loc1))

        assertEquals(1, result.size)
        assertEquals("Sin Pa\u00eds", result[0].countryName)
        assertEquals("General", result[0].cities[0].cityName)
    }

    @Test
    fun `invoke con lista vacia debe retornar lista vacia`() {
        val result = useCase(emptyList())
        assertEquals(0, result.size)
    }
}
