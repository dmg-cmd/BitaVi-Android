package com.aistudio.localizador.domain.usecase

import com.aistudio.localizador.ui.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BiometricAuthUseCaseTest {

    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private lateinit var useCase: BiometricAuthUseCase

    @Before
    fun setUp() {
        useCase = BiometricAuthUseCase(preferencesManager)
    }

    @Test
    fun `isBiometricProtectionRequired debe retornar true cuando la preferencia esta activa`() {
        every { preferencesManager.isBiometricEnabled() } returns true

        val result = useCase.isBiometricProtectionRequired()

        assertTrue(result)
        verify(exactly = 1) { preferencesManager.isBiometricEnabled() }
    }

    @Test
    fun `isBiometricProtectionRequired debe retornar false cuando la preferencia esta desactivada`() {
        every { preferencesManager.isBiometricEnabled() } returns false

        val result = useCase.isBiometricProtectionRequired()

        assertFalse(result)
        verify(exactly = 1) { preferencesManager.isBiometricEnabled() }
    }
}

