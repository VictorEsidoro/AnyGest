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

package io.github.victoresidoro.anygest.ui
package io.github.victoresidoro.anygest.service

import android.view.HapticFeedbackConstants
import io.github.victoresidoro.anygest.gesture.HapticStrength

/**
 * Picks the system haptic a gesture plays. View haptics rather than the Vibrator API: no
 * VIBRATE permission, and no risk of the platform dropping a buzz asked for by a process
 * that is not in the foreground. How different the three feel is up to the device's motor
 * map; [HapticStrength.Normal] is the original behaviour.
 */
internal object Haptics {

    /** Played once a gesture arms or fires. */
    fun tick(strength: HapticStrength): Int = when (strength) {
        HapticStrength.Light -> HapticFeedbackConstants.CLOCK_TICK
        HapticStrength.Normal -> HapticFeedbackConstants.GESTURE_START
        HapticStrength.Strong -> HapticFeedbackConstants.LONG_PRESS
    }

    /** Mirrors the system long-press buzz, so a held gesture reads as having landed. */
    fun longPress(strength: HapticStrength): Int = when (strength) {
        HapticStrength.Light -> HapticFeedbackConstants.GESTURE_START
        HapticStrength.Normal -> HapticFeedbackConstants.LONG_PRESS
        HapticStrength.Strong -> HapticFeedbackConstants.CONFIRM
    }
}
