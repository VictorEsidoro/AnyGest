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

import android.view.HapticFeedbackConstants
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.HapticStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {

    @Test
    fun `normal keeps the original system haptics`() {
        assertEquals(
            Haptic.System(HapticFeedbackConstants.GESTURE_START),
            Haptics.tick(HapticStrength.Normal),
        )
        assertEquals(
            Haptic.System(HapticFeedbackConstants.LONG_PRESS),
            Haptics.longPress(HapticStrength.Normal),
        )
    }

    @Test
    fun `each strength plays a different tick and a different long press`() {
        val ticks = HapticStrength.entries.map(Haptics::tick)
        val longPresses = HapticStrength.entries.map(Haptics::longPress)
        assertEquals(ticks.size, ticks.toSet().size)
        assertEquals(longPresses.size, longPresses.toSet().size)
    }

    @Test
    fun `light and strong buzz with the weakest and strongest amplitude`() {
        listOf(Haptics::tick, Haptics::longPress).forEach { pick ->
            val light = pick(HapticStrength.Light) as Haptic.Buzz
            val strong = pick(HapticStrength.Strong) as Haptic.Buzz
            assertTrue(light.amplitude < strong.amplitude)
            assertTrue(light.durationMs < strong.durationMs)
            assertTrue(strong.amplitude in 1..255 && light.amplitude in 1..255)
        }
    }

    @Test
    fun `the default strength is normal`() {
        assertEquals(HapticStrength.Normal, GestureConfiguration().hapticStrength)
        assertNotEquals(HapticStrength.Light, GestureConfiguration().hapticStrength)
    }
}
