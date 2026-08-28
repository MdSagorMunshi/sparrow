package com.ryanshelby.spw.wallet.security

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtil {
    fun lightTap(context: Context) {
        vibrate(context, 15, 60)
    }

    fun performKeyClick(context: Context) {
        vibrate(context, 18, 90)
    }

    fun successBuzz(context: Context) {
        vibrate(context, 40, 180)
    }

    fun performSuccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            val timings = longArrayOf(0, 30, 40, 40)
            val amplitudes = intArrayOf(0, 120, 0, 180)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrate(context, 60, 180)
        }
    }

    fun errorVibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = getVibrator(context)
            val timings = longArrayOf(0, 50, 60, 50)
            val amplitudes = intArrayOf(0, 150, 0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrate(context, 100, 200)
        }
    }

    fun performError(context: Context) {
        errorVibrate(context)
    }

    fun heavyClick(context: Context) {
        vibrate(context, 35, 220)
    }

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
