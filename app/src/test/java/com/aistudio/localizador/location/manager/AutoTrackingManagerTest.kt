package com.aistudio.localizador.location.manager

import android.location.Location
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class AutoTrackingManagerTest {

    private lateinit var manager: AutoTrackingManager
    private val listener: AutoTrackingManager.AutoTrackingListener = mockk(relaxed = true)
    private val sessionManager: TrackingSessionManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        manager = AutoTrackingManager()
        manager.setListener(listener)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when auto mode is off do not trigger`() {
        val loc = mockk<Location>(relaxed = true)
        every { loc.accuracy } returns 10f
        every { loc.time } returns System.currentTimeMillis()

        manager.processMovementSustained(loc, 5f, false, false, sessionManager)

        verify(exactly = 0) { listener.onMovementSustainedTriggered(any()) }
    }

    @Test
    fun `when already tracking do not trigger`() {
        val loc = mockk<Location>(relaxed = true)
        every { loc.accuracy } returns 10f
        every { loc.time } returns System.currentTimeMillis()

        manager.processMovementSustained(loc, 5f, true, true, sessionManager)

        verify(exactly = 0) { listener.onMovementSustainedTriggered(any()) }
    }

    @Test
    fun `when speed is below threshold do not trigger`() {
        val loc = mockk<Location>(relaxed = true)
        every { loc.accuracy } returns 10f
        every { loc.time } returns System.currentTimeMillis()

        manager.processMovementSustained(loc, 0.5f, true, false, sessionManager)

        verify(exactly = 0) { listener.onMovementSustainedTriggered(any()) }
    }

    @Test
    fun `when cooldown active do not trigger`() {
        val loc = mockk<Location>(relaxed = true)
        every { loc.accuracy } returns 10f

        val t1 = System.currentTimeMillis() - 100000L
        every { loc.time } returns t1
        manager.processMovementSustained(loc, 3f, true, false, sessionManager)

        val anchorLoc = mockk<Location>(relaxed = true)
        every { anchorLoc.accuracy } returns 10f
        every { anchorLoc.time } returns (t1 - 60000L)
        manager.setAutoRestartAnchor(anchorLoc)

        val t2 = t1 + 5000L
        every { loc.time } returns t2
        every { loc.accuracy } returns 10f
        manager.processMovementSustained(loc, 3f, true, false, sessionManager)

        verify(exactly = 0) { listener.onMovementSustainedTriggered(any()) }
    }

    @Test
    fun `clearAll resets state`() {
        val loc = mockk<Location>(relaxed = true)
        every { loc.accuracy } returns 10f
        every { loc.time } returns System.currentTimeMillis()

        manager.setAutoRestartAnchor(loc)
        manager.clearAll()

        manager.processMovementSustained(loc, 3f, true, false, sessionManager)
        verify(exactly = 0) { listener.onMovementSustainedTriggered(any()) }
    }

    @Test
    fun `when distance from stop anchor exceeds threshold trigger immediately`() {
        val anchorLoc = mockk<Location>(relaxed = true)
        every { anchorLoc.latitude } returns -34.6037
        every { anchorLoc.longitude } returns -58.3816
        every { anchorLoc.accuracy } returns 10f
        manager.setAutoRestartAnchor(anchorLoc)

        // Punto desplazado ~45 metros
        val newLoc = mockk<Location>(relaxed = true)
        every { newLoc.latitude } returns -34.6033
        every { newLoc.longitude } returns -58.3816
        every { newLoc.accuracy } returns 12f
        every { newLoc.time } returns System.currentTimeMillis()

        manager.processMovementSustained(newLoc, 0f, true, false, sessionManager)

        verify(exactly = 1) { listener.onMovementSustainedTriggered(any()) }
    }
}
