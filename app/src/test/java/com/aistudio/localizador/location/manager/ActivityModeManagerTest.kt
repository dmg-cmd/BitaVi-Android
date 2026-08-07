package com.aistudio.localizador.location.manager

import com.aistudio.localizador.location.ActivityMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ActivityModeManagerTest {

    private lateinit var manager: ActivityModeManager
    private val upgradeMs = 10000L
    private val downgradeMs = 120000L

    @Before
    fun setUp() {
        manager = ActivityModeManager()
    }

    @Test
    fun `cuando la velocidad escala pero no supera la ventana temporal de upgrade retorna null`() {
        val result = manager.evaluate(speedKmh = 20.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        assertNull("Debe retornar null porque no se ha cumplido el tiempo de transicion", result)
        assertEquals("El estado base debe continuar siendo STOPPED", ActivityMode.STOPPED, manager.getCurrentMode())
    }

    @Test
    fun `cuando la velocidad alta persiste mas alla del tiempo de upgrade consolida el nuevo modo DRIVING`() {
        manager.evaluate(speedKmh = 20.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val consolidated = manager.evaluate(speedKmh = 20.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("20 km/h supera drivingLimitKmh (13.0) y debe consolidar en DRIVING", ActivityMode.DRIVING, consolidated)
        assertEquals("La maquina de estados debe actualizarse", ActivityMode.DRIVING, manager.getCurrentMode())
    }

    @Test
    fun `cuando la velocidad es exactamente el limite superior de walking se mantiene en WALKING por operador menor o igual`() {
        manager.evaluate(speedKmh = 7.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val result = manager.evaluate(speedKmh = 7.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("7.0 km/h exacto debe consolidar en WALKING", ActivityMode.WALKING, result)
    }

    @Test
    fun `cuando la velocidad supera el limite inferior del rango STOPPED y persiste consolida WALKING`() {
        manager.evaluate(speedKmh = 1.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val result = manager.evaluate(speedKmh = 1.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("1.0 km/h entra al rango WALKING (1.0 < speed <= 7.0)", ActivityMode.WALKING, result)
    }

    @Test
    fun `cuando la velocidad supera el limite superior del rango WALKING y persiste consolida BIKING`() {
        manager.evaluate(speedKmh = 10.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val result = manager.evaluate(speedKmh = 10.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("10.0 km/h entra al rango BIKING (7.0 < speed <= 13.0)", ActivityMode.BIKING, result)
    }

    @Test
    fun `cuando la velocidad se encuentra en el limite exacto de BIKING consolida BIKING por operador menor o igual`() {
        manager.evaluate(speedKmh = 13.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val result = manager.evaluate(speedKmh = 13.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("13.0 km/h exacto debe consolidar en BIKING", ActivityMode.BIKING, result)
    }

    @Test
    fun `cuando la velocidad supera el limite superior del rango BIKING y persiste consolida DRIVING`() {
        manager.evaluate(speedKmh = 13.1f, timestamp = 1000L, upgradeMs, downgradeMs)
        val result = manager.evaluate(speedKmh = 13.1f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("13.1 km/h supera drivingLimitKmh (13.0) y debe consolidar en DRIVING", ActivityMode.DRIVING, result)
    }

    @Test
    fun `cuando la velocidad esta por debajo del limite de walking se mantiene en STOPPED`() {
        manager.evaluate(speedKmh = 0.9f, timestamp = 1000L, upgradeMs, downgradeMs)
        assertEquals("0.9 km/h (speed < 1.0) debe permanecer en STOPPED", ActivityMode.STOPPED, manager.getCurrentMode())
    }

    @Test
    fun `cuando la velocidad decae por debajo del umbral de forma sostenida aplica la ventana temporal de downgrade`() {
        manager.evaluate(speedKmh = 15.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        manager.evaluate(speedKmh = 15.0f, timestamp = 12000L, upgradeMs, downgradeMs)

        val shortTimeResult = manager.evaluate(speedKmh = 0.0f, timestamp = 30000L, upgradeMs, downgradeMs)
        assertNull("Debe retener DRIVING temporalmente por filtro de seguridad", shortTimeResult)

        val longTimeResult = manager.evaluate(speedKmh = 0.0f, timestamp = 150000L, upgradeMs, downgradeMs)
        assertEquals("Debe degradar el estado a STOPPED tras expirar la ventana", ActivityMode.STOPPED, longTimeResult)
    }

    @Test
    fun `reset limpia el estado interno a STOPPED`() {
        manager.evaluate(speedKmh = 20.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        manager.evaluate(speedKmh = 20.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals(ActivityMode.DRIVING, manager.getCurrentMode())

        manager.reset()
        assertEquals("reset() debe devolver a STOPPED", ActivityMode.STOPPED, manager.getCurrentMode())
    }
}