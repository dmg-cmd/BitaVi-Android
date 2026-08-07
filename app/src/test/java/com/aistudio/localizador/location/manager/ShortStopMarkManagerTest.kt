package com.aistudio.localizador.location.manager

import android.location.Location
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import com.aistudio.localizador.ui.GeocoderUseCase
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test

class ShortStopMarkManagerTest {

    private val repository: RouteRepository = mockk(relaxed = true)
    private val geocoderUseCase: GeocoderUseCase = mockk()
    private lateinit var manager: ShortStopMarkManager

    private val shortStopDelay = 120000L
    private val stopTolerance = 180000L

    @Before
    fun setUp() {
        manager = ShortStopMarkManager(repository, geocoderUseCase)
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cuando el dispositivo esta en reposo pero no supera el tiempo limite no dispara insercion`() {
        val mockLocation = mockk<Location>(relaxed = true)

        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 1000L, shortStopDelay, stopTolerance)
        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 50000L, shortStopDelay, stopTolerance)

        coVerify(exactly = 0) { repository.addSavedLocation(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `cuando supera el tiempo limite y reanuda el movimiento persiste de forma exitosa`() = runTest {
        val scheduler = coroutineContext[TestCoroutineScheduler]!!
        coEvery { geocoderUseCase.fetchLocationDetails(any(), any()) } returns Triple("Gran Via 5", "Espana", "Madrid")
        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.latitude } returns 40.4167
        every { mockLocation.longitude } returns -3.7037

        manager.managerScope = CoroutineScope(StandardTestDispatcher(scheduler))
        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 1000L, shortStopDelay, stopTolerance)
        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 130000L, shortStopDelay, stopTolerance)

        manager.processCinematicInput(mockLocation, speedKmh = 5.0f, timestamp = 140000L, shortStopDelay, stopTolerance)

        scheduler.advanceUntilIdle()
        coVerify(exactly = 1) {
            repository.addSavedLocation(
                name = any(),
                address = any(),
                latitude = 40.4167,
                longitude = -3.7037,
                comment = any(),
                author = "Automático",
                timestamp = any()
            )
        }
    }

    @Test
    fun `cuando se reanuda sin haber superado el tiempo limite no persiste nada`() = runTest {
        val scheduler = coroutineContext[TestCoroutineScheduler]!!
        coEvery { geocoderUseCase.fetchLocationDetails(any(), any()) } returns Triple("Gran Via 5", "Espana", "Madrid")
        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.latitude } returns 40.4167
        every { mockLocation.longitude } returns -3.7037

        manager.managerScope = CoroutineScope(StandardTestDispatcher(scheduler))
        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 1000L, shortStopDelay, stopTolerance)
        manager.processCinematicInput(mockLocation, speedKmh = 5.0f, timestamp = 50000L, shortStopDelay, stopTolerance)

        scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { repository.addSavedLocation(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `resetEvaluation limpia el estado interno`() = runTest {
        val scheduler = coroutineContext[TestCoroutineScheduler]!!
        coEvery { geocoderUseCase.fetchLocationDetails(any(), any()) } returns Triple("Gran Via 5", "Espana", "Madrid")
        val mockLocation = mockk<Location>(relaxed = true)
        every { mockLocation.latitude } returns 40.4167
        every { mockLocation.longitude } returns -3.7037

        manager.managerScope = CoroutineScope(StandardTestDispatcher(scheduler))
        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 1000L, shortStopDelay, stopTolerance)
        manager.processCinematicInput(mockLocation, speedKmh = 0.0f, timestamp = 130000L, shortStopDelay, stopTolerance)

        manager.resetEvaluation()

        manager.processCinematicInput(mockLocation, speedKmh = 5.0f, timestamp = 140000L, shortStopDelay, stopTolerance)

        scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { repository.addSavedLocation(any(), any(), any(), any(), any(), any()) }
    }
}