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

import io.github.victoresidoro.anygest.navigation.NavigationMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertNotifierTest {

    @Test
    fun `only gestures turning into buttons counts as the bar coming back`() {
        assertTrue(
            AlertNotifier.navigationBarCameBack(NavigationMode.GESTURAL, NavigationMode.THREE_BUTTON),
        )
    }

    @Test
    fun `no alert without a known starting mode or when nothing changed`() {
        assertFalse(AlertNotifier.navigationBarCameBack(null, NavigationMode.THREE_BUTTON))
        assertFalse(
            AlertNotifier.navigationBarCameBack(NavigationMode.THREE_BUTTON, NavigationMode.THREE_BUTTON),
        )
        assertFalse(
            AlertNotifier.navigationBarCameBack(NavigationMode.GESTURAL, NavigationMode.GESTURAL),
        )
        assertFalse(
            AlertNotifier.navigationBarCameBack(NavigationMode.THREE_BUTTON, NavigationMode.GESTURAL),
        )
    }
}
