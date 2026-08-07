package com.aistudio.localizador.ui.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BiometricSessionManagerTest {

    private lateinit var sessionManager: BiometricSessionManager

    @Before
    fun setUp() {
        sessionManager = BiometricSessionManager()
    }

    @Test
    fun `inicialmente la sesion debe estar bloqueada`() {
        assertFalse(sessionManager.isUnlocked.value)
    }

    @Test
    fun `onAuthenticationSuccess debe desbloquear la sesion`() {
        sessionManager.onAuthenticationSuccess()
        assertTrue(sessionManager.isUnlocked.value)
    }

    @Test
    fun `regreso dentro de la ventana de gracia debe mantener la sesion desbloqueada`() {
        sessionManager.onAuthenticationSuccess()
        sessionManager.onAppBackgrounded()
        sessionManager.onAppForegrounded()
        assertTrue(sessionManager.isUnlocked.value)
    }

    @Test
    fun `lockExplicitly debe forzar el bloqueo inmediato`() {
        sessionManager.onAuthenticationSuccess()
        sessionManager.lockExplicitly()
        assertFalse(sessionManager.isUnlocked.value)
    }
}
