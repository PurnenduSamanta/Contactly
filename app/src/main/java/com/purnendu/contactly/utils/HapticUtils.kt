package com.purnendu.contactly.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Utility object for triggering haptic feedback on Android.
 *
 * Provides both Compose-level and Context-level haptic feedback methods.
 */
object HapticUtils {

    /**
     * Performs a toggle-style haptic feedback using Compose's HapticFeedback API.
     * Best for switch toggles, chip selections, etc.
     */
    fun performToggleFeedback(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
