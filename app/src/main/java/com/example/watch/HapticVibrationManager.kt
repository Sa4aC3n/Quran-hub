package com.example.watch

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.HapticVibrationPattern

class HapticVibrationManager(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Trigger a specialized haptic pattern on the device / connected watch
     */
    fun triggerHaptic(pattern: HapticVibrationPattern) {
        try {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vib.hasAmplitudeControl()) {
                    val effect = VibrationEffect.createWaveform(
                        pattern.timingsMs,
                        pattern.amplitudes,
                        -1 // Do not repeat
                    )
                    vib.vibrate(effect)
                } else {
                    val effect = VibrationEffect.createWaveform(
                        pattern.timingsMs,
                        -1
                    )
                    vib.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern.timingsMs, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels any active vibration
     */
    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
