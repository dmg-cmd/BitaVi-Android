package com.example.util

import android.content.Context
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertNotifier(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    fun triggerProximityAlert() {
        playTone()
        vibrate()
    }

    private fun playTone() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(
                    AppConstants.AudioVisual.PROXIMITY_TONE,
                    ToneGenerator.MAX_VOLUME
                )
            }
            toneGenerator?.startTone(
                AppConstants.AudioVisual.PROXIMITY_TONE,
                AppConstants.AudioVisual.TONE_DURATION_MS.toInt()
            )
        } catch (_: Exception) { }
    }

    private fun vibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        AppConstants.AudioVisual.PROXIMITY_VIBRATE_PATTERN,
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(AppConstants.AudioVisual.PROXIMITY_VIBRATE_PATTERN, -1)
            }
        } catch (_: Exception) { }
    }

    fun release() {
        try { toneGenerator?.release() } catch (_: Exception) { }
        toneGenerator = null
    }
}
