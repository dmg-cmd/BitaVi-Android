package com.aistudio.localizador.location.manager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DepartureWatcherManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val geofencingClient: GeofencingClient = mockk(relaxed = true)
    private lateinit var manager: DepartureWatcherManager

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        // Intent y PendingIntent no existen en el entorno JVM puro de los tests unitarios:
        // se simulan para que la construcción de la geocerca no lance "not mocked".
        mockkConstructor(Intent::class)
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk(relaxed = true)

        mockkStatic(LocationServices::class)
        every { LocationServices.getGeofencingClient(any<Context>()) } returns geofencingClient

        val mockAddTask = mockk<Task<Void>>(relaxed = true)
        every { geofencingClient.addGeofences(any(), any()) } returns mockAddTask
        every { mockAddTask.addOnSuccessListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>().onSuccess(null)
            mockAddTask
        }

        val mockRemoveTask = mockk<Task<Void>>(relaxed = true)
        every { geofencingClient.removeGeofences(any<android.app.PendingIntent>()) } returns mockRemoveTask
        every { mockRemoveTask.addOnSuccessListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>().onSuccess(null)
            mockRemoveTask
        }

        manager = DepartureWatcherManager(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when coordinates are zero do not start watching`() {
        manager.startWatchingDeparture(0.0, 0.0)

        assertFalse(manager.isCurrentlyWatching())
        verify(exactly = 0) { geofencingClient.addGeofences(any(), any()) }
    }

    @Test
    fun `when coordinates valid startWatchingDeparture registers geofence successfully`() {
        val lat = -34.6037
        val lon = -58.3816

        manager.startWatchingDeparture(lat, lon)

        assertTrue(manager.isCurrentlyWatching())
        val coords = manager.getAnchorCoordinates()
        assertEquals(lat, coords.first, 0.0001)
        assertEquals(lon, coords.second, 0.0001)
        verify(exactly = 1) { geofencingClient.addGeofences(any(), any()) }
    }

    @Test
    fun `stopWatching cleans active geofence`() {
        manager.startWatchingDeparture(-34.6037, -58.3816)
        assertTrue(manager.isCurrentlyWatching())

        manager.stopWatching()

        assertFalse(manager.isCurrentlyWatching())
        verify(exactly = 1) { geofencingClient.removeGeofences(any<android.app.PendingIntent>()) }
    }
}