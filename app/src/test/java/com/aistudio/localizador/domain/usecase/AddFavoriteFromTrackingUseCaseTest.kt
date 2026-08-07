package com.aistudio.localizador.domain.usecase

import android.location.Location
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.ui.GeocoderUseCase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AddFavoriteFromTrackingUseCaseTest {

    private val repository: RouteRepository = mockk()
    private val geocoderUseCase: GeocoderUseCase = mockk()
    private lateinit var useCase: AddFavoriteFromTrackingUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        useCase = AddFavoriteFromTrackingUseCase(repository, geocoderUseCase)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `GIVEN valid location and active route WHEN execute THEN returns success`() = runTest {
        val lat = 40.4168
        val lon = -3.7038
        val location = mockk<Location> {
            every { latitude } returns lat
            every { longitude } returns lon
        }
        coEvery { geocoderUseCase.fetchAddress(any(), any()) } returns "Madrid, Espana"
        coEvery { repository.addSavedLocation(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 42L
        coEvery { repository.addFavoriteToRoute(any(), any()) } returns Unit

        val result = useCase.execute(location, activeRouteId = 7)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.addSavedLocation(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { repository.addFavoriteToRoute(7, 42) }
    }

    @Test
    fun `GIVEN null location WHEN execute THEN returns failure`() = runTest {
        val result = useCase.execute(currentLocation = null, activeRouteId = null)
        assertTrue(result.isFailure)
        assertEquals("Coordenadas GPS no disponibles.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `GIVEN zero-zero location WHEN execute THEN returns failure`() = runTest {
        val location = mockk<Location> {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        val result = useCase.execute(location, activeRouteId = null)
        assertTrue(result.isFailure)
        assertEquals("Coordenadas GPS no disponibles.", result.exceptionOrNull()?.message)
    }
}
