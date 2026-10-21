// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of AnyGest, based on HyperGesture by Henrique Almeida (h3nc4).
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.victoresidoro.anygest.service

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import io.github.victoresidoro.anygest.gesture.HapticStrength

/** What a gesture plays: the system's own haptic, or a buzz with a fixed length and power. */
internal sealed interface Haptic {

    /** A [HapticFeedbackConstants] value, played through the view. */
    data class System(val constant: Int) : Haptic

    /** [amplitude] is 1..255; phones without amplitude control only honour [durationMs]. */
    data class Buzz(val durationMs: Long, val amplitude: Int) : Haptic
}

/**
 * Picks what a gesture plays. Normal is the system haptic AnyGest always used. The system
 * haptic constants cannot tell light from strong apart on HyperOS - they map to the same
 * motor effect - so Light and Strong drive the vibrator directly with their own length and
 * amplitude. That needs the (automatically granted) VIBRATE permission.
 */
internal object Haptics {

    /** Played once a gesture arms or fires. */
    fun tick(strength: HapticStrength): Haptic = when (strength) {
        HapticStrength.Light -> Haptic.Buzz(durationMs = 8, amplitude = 40)
        HapticStrength.Normal -> Haptic.System(HapticFeedbackConstants.GESTURE_START)
        HapticStrength.Strong -> Haptic.Buzz(durationMs = 30, amplitude = 255)
    }

    /** Mirrors the system long-press buzz, so a held gesture reads as having landed. */
    fun longPress(strength: HapticStrength): Haptic = when (strength) {
        HapticStrength.Light -> Haptic.Buzz(durationMs = 20, amplitude = 50)
        HapticStrength.Normal -> Haptic.System(HapticFeedbackConstants.LONG_PRESS)
        HapticStrength.Strong -> Haptic.Buzz(durationMs = 60, amplitude = 255)
    }

    fun play(view: View, haptic: Haptic) {
        when (haptic) {
            is Haptic.System -> view.performHapticFeedback(haptic.constant)
            is Haptic.Buzz -> buzz(view.context, haptic)
        }
    }

    /**
     * Tagged as touch feedback (accessibility on Android 11 and 12, where touch has no
     * attributes of its own) so the platform does not drop it for a process that is not
     * in the foreground.
     */
    @Suppress("DEPRECATION")
    private fun buzz(context: Context, haptic: Haptic.Buzz) {
        runCatching {
            val vibrator = context.getSystemService(Vibrator::class.java) ?: return
            if (!vibrator.hasVibrator()) return
            val amplitude =
                if (vibrator.hasAmplitudeControl()) haptic.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
            val effect = VibrationEffect.createOneShot(haptic.durationMs, amplitude)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH),
                )
            } else {
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build(),
                )
            }
        }
    }
}
