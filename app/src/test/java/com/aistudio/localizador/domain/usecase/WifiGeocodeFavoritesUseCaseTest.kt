package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.aistudio.localizador.data.model.SavedLocation
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.ui.GeocoderUseCase
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WifiGeocodeFavoritesUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val repository: RouteRepository = mockk(relaxed = true)
    private val geocoderUseCase: GeocoderUseCase = mockk()
    private val connectivityManager: ConnectivityManager = mockk(relaxed = true)
    private val networkCapabilities: NetworkCapabilities = mockk(relaxed = true)

    private lateinit var useCase: WifiGeocodeFavoritesUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        useCase = WifiGeocodeFavoritesUseCase(context, repository, geocoderUseCase)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cuando no hay wifi no intenta geocodificar ni actualiza favoritos`() = runTest {
        every { connectivityManager.activeNetwork } returns null

        val result = useCase.execute()

        assertEquals(0, result)
        coVerify(exactly = 0) { repository.updateSavedLocation(any()) }
    }

    @Test
    fun `cuando hay wifi actualiza el titulo solo con la direccion sin prefijo Parada`() = runTest {
        val mockNetwork = mockk<android.net.Network>()
        every { connectivityManager.activeNetwork } returns mockNetwork
        every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val pendingStop = SavedLocation(
            id = 1,
            name = "Parada 2026-10-21 12:30",
            address = "GPS: -34.60370, -58.38160",
            latitude = -34.6037,
            longitude = -58.3816,
            iconCategory = "AUTOMATIC"
        )
        coEvery { repository.getAllSavedLocationsSync() } returns listOf(pendingStop)
        coEvery { geocoderUseCase.fetchLocationDetails(-34.6037, -58.3816) } returns Triple(
            "Av. Corrientes 1234, CABA",
            "Argentina",
            "Buenos Aires"
        )

        val updatedCount = useCase.execute()

        assertEquals(1, updatedCount)
        coVerify(exactly = 1) {
            repository.updateSavedLocation(match {
                // Título directo con la dirección, sin 'Parada'
                it.name == "Av. Corrientes 1234" &&
                it.address == "Av. Corrientes 1234, CABA" &&
                it.city == "Buenos Aires" &&
                it.country == "Argentina"
            })
        }
    }

    @Test
    fun `cuando esta fuera de ciudad y no hay calle coloca la localidad sin prefijo Parada`() = runTest {
        val mockNetwork = mockk<android.net.Network>()
        every { connectivityManager.activeNetwork } returns mockNetwork
        every { connectivityManager.getNetworkCapabilities(mockNetwork) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val ruralStop = SavedLocation(
            id = 2,
            name = "Parada 2026-10-21 14:00",
            address = "GPS: -31.40000, -64.18000",
            latitude = -31.4,
            longitude = -64.18,
            iconCategory = "AUTOMATIC"
        )
        coEvery { repository.getAllSavedLocationsSync() } returns listOf(ruralStop)
        coEvery { geocoderUseCase.fetchLocationDetails(-31.4, -64.18) } returns Triple(
            "GPS: -31.40000, -64.18000", // No hay calle
            "Argentina",
            "Capilla del Monte"
        )

        val updatedCount = useCase.execute()

        assertEquals(1, updatedCount)
        coVerify(exactly = 1) {
            repository.updateSavedLocation(match {
                // Debe colocar la localidad directamente
                it.name == "Capilla del Monte" &&
                it.city == "Capilla del Monte" &&
                it.country == "Argentina"
            })
        }
    }
}
