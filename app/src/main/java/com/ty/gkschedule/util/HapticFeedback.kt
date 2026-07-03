package com.ty.gkschedule.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedback {
    /** Light tap — button press, switch toggle */
    fun light(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** Medium tap — course click, navigation */
    fun medium(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Heavy — long press, delete */
    fun heavy(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Success — save, export */
    fun success(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // Context-based fallback
    fun light(context: Context) {
        vibrate(context, 30, 80)
    }

    fun medium(context: Context) {
        vibrate(context, 50, 150)
    }

    fun heavy(context: Context) {
        vibrate(context, 80, 255)
    }

    fun success(context: Context) {
        vibrate(context, 40, 120)
    }

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        try {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            v ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
