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
    fun `cuando la velocidad alta persiste mas alla del tiempo de upgrade consolida el nuevo modo`() {
        manager.evaluate(speedKmh = 20.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val consolidated = manager.evaluate(speedKmh = 20.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("Debe consolidar la transicion a BIKING", ActivityMode.BIKING, consolidated)
        assertEquals("La maquina de estados debe actualizarse", ActivityMode.BIKING, manager.getCurrentMode())
    }

    @Test
    fun `cuando la velocidad es exactamente el limite superior de walking se mantiene en WALKING por operador menor o igual`() {
        manager.evaluate(speedKmh = 7.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        val result = manager.evaluate(speedKmh = 7.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals("7.0 km/h exacto debe consolidar en WALKING", ActivityMode.WALKING, result)
    }

    @Test
    fun `cuando la velocidad decae por debajo del umbral de forma sostenida aplica la ventana temporal de downgrade`() {
        manager.evaluate(speedKmh = 15.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        manager.evaluate(speedKmh = 15.0f, timestamp = 12000L, upgradeMs, downgradeMs)

        val shortTimeResult = manager.evaluate(speedKmh = 0.0f, timestamp = 30000L, upgradeMs, downgradeMs)
        assertNull("Debe retener DRIVING temporalmente por filtro de seguridad", shortTimeResult)

        val longTimeResult = manager.evaluate(speedKmh = 0.0f, timestamp = 140000L, upgradeMs, downgradeMs)
        assertEquals("Debe degradar el estado a STOPPED tras expirar la ventana", ActivityMode.STOPPED, longTimeResult)
    }

    @Test
    fun `reset limpia el estado interno a STOPPED`() {
        manager.evaluate(speedKmh = 20.0f, timestamp = 1000L, upgradeMs, downgradeMs)
        manager.evaluate(speedKmh = 20.0f, timestamp = 12000L, upgradeMs, downgradeMs)
        assertEquals(ActivityMode.BIKING, manager.getCurrentMode())

        manager.reset()
        assertEquals("reset() debe devolver a STOPPED", ActivityMode.STOPPED, manager.getCurrentMode())
    }
}