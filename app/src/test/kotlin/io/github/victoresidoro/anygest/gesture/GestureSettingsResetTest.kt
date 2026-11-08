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

package io.github.victoresidoro.anygest.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureSettingsResetTest {

    @Test
    fun `the reset puts back what the screen shows and keeps the rest`() {
        val changed = GestureConfiguration(
            edgeWidthDp = 40f,
            sideLengthPercent = 50,
            sidePosition = SidePosition.Top,
            hapticStrength = HapticStrength.Strong,
            hapticConfirmEnabled = true,
            appSwitchEnabled = true,
            circleToSearchEnabled = true,
            pauseApps = setOf("com.example.bank"),
        )

        val reset = changed.withGestureSettingsOf(GestureConfiguration())

        val defaults = GestureConfiguration()
        assertEquals(defaults.edgeWidthDp, reset.edgeWidthDp, 0f)
        assertEquals(defaults.sideLengthPercent, reset.sideLengthPercent)
        assertEquals(defaults.sidePosition, reset.sidePosition)
        assertEquals(defaults.hapticStrength, reset.hapticStrength)
        assertEquals(defaults.hapticConfirmEnabled, reset.hapticConfirmEnabled)
        assertEquals(true, reset.appSwitchEnabled)
        assertEquals(true, reset.circleToSearchEnabled)
        assertEquals(setOf("com.example.bank"), reset.pauseApps)
    }
}
