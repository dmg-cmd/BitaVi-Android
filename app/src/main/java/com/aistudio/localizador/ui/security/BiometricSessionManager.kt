package com.aistudio.localizador.ui.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricSessionManager @Inject constructor() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastPausedTimestampMs: Long = 0L
    private val gracePeriodMs: Long = 30_000L

    fun onAuthenticationSuccess() {
        _isUnlocked.value = true
        lastPausedTimestampMs = 0L
    }

    fun onAppBackgrounded() {
        if (_isUnlocked.value) {
            lastPausedTimestampMs = System.currentTimeMillis()
        }
    }

    fun onAppForegrounded() {
        if (_isUnlocked.value && lastPausedTimestampMs > 0L) {
            val elapsedTimeMs = System.currentTimeMillis() - lastPausedTimestampMs
            if (elapsedTimeMs > gracePeriodMs) {
                _isUnlocked.value = false
            }
            lastPausedTimestampMs = 0L
        }
    }

    fun lockExplicitly() {
        _isUnlocked.value = false
        lastPausedTimestampMs = 0L
    }
}
