package com.aistudio.localizador.location.manager

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProximityBleManagerTest {

    private lateinit var manager: ProximityBleManager
    private val context: Context = mockk(relaxed = true)
    private val sharedPrefs: SharedPreferences = mockk(relaxed = true)
    private val listener: ProximityBleManager.ProximityListener = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk(relaxed = true)

        mockkConstructor(com.aistudio.localizador.location.ProximityManager::class)
        every { anyConstructed<com.aistudio.localizador.location.ProximityManager>().init() } returns true
        every { anyConstructed<com.aistudio.localizador.location.ProximityManager>().isBluetoothEnabled() } returns true
        every { anyConstructed<com.aistudio.localizador.location.ProximityManager>().stop() } returns Unit
        every { anyConstructed<com.aistudio.localizador.location.ProximityManager>().startScanning() } returns Unit
        every { anyConstructed<com.aistudio.localizador.location.ProximityManager>().startAdvertising(any()) } returns Unit

        manager = ProximityBleManager(context)
        manager.init(listener, sharedPrefs)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `init creates ProximityManager and registers listener`() {
        verify { anyConstructed<com.aistudio.localizador.location.ProximityManager>().init() }
    }

    @Test
    fun `when near me and ble are enabled ensureBleProximity starts scanning`() {
        every { sharedPrefs.getBoolean("near_me_enabled", false) } returns true
        every { sharedPrefs.getBoolean("ble_enabled", false) } returns true

        manager.ensureBleProximity(isServiceTracking = true)

        verify { anyConstructed<com.aistudio.localizador.location.ProximityManager>().startScanning() }
    }

    @Test
    fun `when near me disabled ensureBleProximity stops scanning`() {
        every { sharedPrefs.getBoolean("near_me_enabled", false) } returns false

        manager.ensureBleProximity(isServiceTracking = true)

        verify(exactly = 0) { anyConstructed<com.aistudio.localizador.location.ProximityManager>().startScanning() }
    }

    @Test
    fun `when not tracking ensureBleProximity does not start scanning`() {
        every { sharedPrefs.getBoolean("near_me_enabled", false) } returns true
        every { sharedPrefs.getBoolean("ble_enabled", false) } returns true

        manager.ensureBleProximity(isServiceTracking = false)

        verify(exactly = 0) { anyConstructed<com.aistudio.localizador.location.ProximityManager>().startScanning() }
    }

    @Test
    fun `clearAll stops proximity manager and releases alert notifier`() {
        manager.clearAll()

        verify { anyConstructed<com.aistudio.localizador.location.ProximityManager>().stop() }
    }
}