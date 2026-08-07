package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.data.model.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GroupRoutesByDateUseCaseTest {

    private lateinit var useCase: GroupRoutesByDateUseCase

    @Before
    fun setUp() {
        useCase = GroupRoutesByDateUseCase()
    }

    @Test
    fun `invoke debe agrupar rutas por dia descendente`() {
        val day1 = Instant.parse("2025-06-01T10:00:00Z")
        val day2 = Instant.parse("2025-05-20T10:00:00Z")
        val r1 = Route(id = 1, name = "Ruta Hoy", startTime = day1.toEpochMilli(), endTime = day1.toEpochMilli() + 1000, distanceMeters = 500.0)
        val r2 = Route(id = 2, name = "Ruta Ayer", startTime = day2.toEpochMilli(), endTime = day2.toEpochMilli() + 1000, distanceMeters = 300.0)

        val result = useCase(listOf(r1, r2))

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2025, 6, 1), result[0].date)
        assertEquals(LocalDate.of(2025, 5, 20), result[1].date)
        assertEquals(1, result[0].routes.size)
        assertEquals(1, result[1].routes.size)
    }

    @Test
    fun `invoke dos rutas mismo dia deben compartir grupo y color`() {
        val day1 = Instant.parse("2025-06-01T10:00:00Z")
        val day2 = Instant.parse("2025-06-01T14:00:00Z")
        val r1 = Route(id = 1, name = "Manana", startTime = day1.toEpochMilli(), endTime = day1.toEpochMilli() + 1000, distanceMeters = 500.0)
        val r2 = Route(id = 2, name = "Tarde", startTime = day2.toEpochMilli(), endTime = day2.toEpochMilli() + 1000, distanceMeters = 300.0)

        val result = useCase(listOf(r1, r2))

        assertEquals(1, result.size)
        assertEquals(2, result[0].routes.size)
        val expectedColor = useCase(listOf(r1))[0].colorInt
        assertEquals(expectedColor, result[0].colorInt)
    }

    @Test
    fun `invoke con lista vacia debe retornar lista vacia`() {
        val result = useCase(emptyList())
        assertTrue(result.isEmpty())
    }
}
