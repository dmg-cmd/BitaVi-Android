package com.aistudio.localizador.location.manager

import com.aistudio.localizador.location.manager.StationaryAnchorManager
import io.mockk.clearAllMocks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StationaryAnchorManagerTest {

    private lateinit var anchorManager: StationaryAnchorManager

    @Before
    fun setUp() {
        anchorManager = StationaryAnchorManager()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cuando la velocidad es inferior al umbral de anclaje se congela la posicion`() {
        val latFija = -34.6037
        val lonFija = -58.3816

        anchorManager.processStationaryState(latFija, lonFija, speed = 0.1f)

        val latDrift = -34.6039
        val lonDrift = -58.3818

        val coordenadaProcesada = anchorManager.getResolvedCoordinate(latDrift, lonDrift)

        assertEquals("La latitud procesada debe retornar la de anclaje para mitigar el drift", latFija, coordenadaProcesada.first, 0.0001)
        assertEquals("La longitud procesada debe retornar la de anclaje para mitigar el drift", lonFija, coordenadaProcesada.second, 0.0001)
    }

    @Test
    fun `cuando el usuario reanuda velocidad sostenida el anclaje debe liberarse`() {
        val latFija = -34.6037
        val lonFija = -58.3816
        anchorManager.processStationaryState(latFija, lonFija, speed = 0.1f)

        val latNueva = -34.6100
        val lonNueva = -58.3900
        val velocidadDeMarcha = 4.5f

        val anclajeLiberado = anchorManager.evaluateRelease(latNueva, lonNueva, velocidadDeMarcha)

        assertTrue("El anclaje debe romperse debido al movimiento sostenido", anclajeLiberado)
    }
}