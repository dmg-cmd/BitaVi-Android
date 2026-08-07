package com.aistudio.localizador.location.manager

import com.aistudio.localizador.location.ActivityMode
import javax.inject.Inject

class ActivityModeManager @Inject constructor() {

    private var currentMode: ActivityMode = ActivityMode.STOPPED
    private var pendingMode: ActivityMode? = null
    private var pendingModeStartTimeMs: Long = 0L
    private var lastEvaluationTimestampMs: Long = 0L

    fun evaluate(
        speedKmh: Float,
        timestamp: Long,
        upgradeMs: Long,
        downgradeMs: Long,
        walkingLimitKmh: Float = 1.0f,
        bikingLimitKmh: Float = 7.0f,
        drivingLimitKmh: Float = 13.0f
    ): ActivityMode? {
        if (timestamp <= lastEvaluationTimestampMs) return null
        lastEvaluationTimestampMs = timestamp

        val instantMode = when {
            speedKmh < walkingLimitKmh -> ActivityMode.STOPPED
            speedKmh <= bikingLimitKmh -> ActivityMode.WALKING
            speedKmh <= drivingLimitKmh -> ActivityMode.BIKING
            speedKmh > drivingLimitKmh -> ActivityMode.DRIVING
            else -> ActivityMode.SIN_SENAL
        }

        if (instantMode == currentMode) {
            pendingMode = null
            pendingModeStartTimeMs = 0L
            return null
        }

        if (instantMode == pendingMode) {
            val windowDuration = timestamp - pendingModeStartTimeMs
            val requiredThresholdMs = if (instantMode.ordinal > currentMode.ordinal) upgradeMs else downgradeMs
            if (windowDuration >= requiredThresholdMs) {
                currentMode = instantMode
                pendingMode = null
                pendingModeStartTimeMs = 0L
                return currentMode
            }
        } else {
            pendingMode = instantMode
            pendingModeStartTimeMs = timestamp
        }

        return null
    }

    fun getCurrentMode(): ActivityMode = currentMode

    fun reset() {
        currentMode = ActivityMode.STOPPED
        pendingMode = null
        pendingModeStartTimeMs = 0L
        lastEvaluationTimestampMs = 0L
    }
}
