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
package io.github.victoresidoro.anygest.settings

import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.HapticStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsBackupTest {

    @Test
    fun `a round trip keeps every setting`() {
        val original = GestureConfiguration(
            edgeWidthDp = 30f,
            sideMinimumSwipeDistanceDp = 40f,
            bottomMinimumSwipeDistanceDp = 15f,
            maximumArmDurationMs = 1500L,
            recentsHoldMs = 200L,
            holdStillnessDp = 20f,
            offAxisToleranceRatio = 0.8f,
            appSwitchEnabled = true,
            leftEdgeBackEnabled = false,
            rightEdgeBackEnabled = true,
            hapticFeedbackEnabled = false,
            hapticStrength = HapticStrength.Strong,
            circleToSearchEnabled = true,
            circleToSearchHoldMs = 500L,
            circleToSearchFollowsSystemTiming = false,
        )

        val decoded = SettingsBackup.decode(SettingsBackup.encode(original))

        assertNotNull(decoded)
        assertEquals(original, decoded!!.configuration)
        assertEquals(0, decoded.ignored)
    }

    @Test
    fun `text that is not a settings file is refused`() {
        assertNull(SettingsBackup.decode("hello world"))
        assertNull(SettingsBackup.decode(""))
        assertNull(SettingsBackup.decode("edge_width_dp=30.0"))
    }

    @Test
    fun `a value out of range is skipped and counted, the rest still applies`() {
        val decoded = SettingsBackup.decode(
            "format=1\nedge_width_dp=9999\nrecents_hold_ms=200\nhaptic_strength=Strong\n",
        )!!

        assertEquals(GestureConfiguration().edgeWidthDp, decoded.configuration.edgeWidthDp, 0f)
        assertEquals(200L, decoded.configuration.recentsHoldMs)
        assertEquals(HapticStrength.Strong, decoded.configuration.hapticStrength)
        assertEquals(1, decoded.ignored)
    }

    @Test
    fun `garbage values and non finite numbers are skipped`() {
        val decoded = SettingsBackup.decode(
            "format=1\nedge_width_dp=NaN\nside_minimum_swipe_distance_dp=abc\n" +
                "haptic_feedback_enabled=maybe\nhaptic_strength=Huge\n",
        )!!

        assertEquals(GestureConfiguration(), decoded.configuration)
        assertEquals(4, decoded.ignored)
    }

    @Test
    fun `unknown keys are ignored without being counted as errors`() {
        val decoded = SettingsBackup.decode("format=1\nfuture_setting=1\n")!!

        assertEquals(GestureConfiguration(), decoded.configuration)
        assertEquals(0, decoded.ignored)
    }
}
