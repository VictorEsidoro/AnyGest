// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of AnyGest, based on HyperGesture.
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

package io.github.victoresidoro.anygest.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureTileServiceTest {

    @Test
    fun `unreachable when nothing resolves the details page`() {
        assertFalse(GestureTileService.detailsReachable(resolves = false, requiredPermission = null) { true })
    }

    @Test
    fun `reachable when the details page asks for no permission`() {
        assertTrue(GestureTileService.detailsReachable(resolves = true, requiredPermission = null) { false })
    }

    @Test
    fun `unreachable when the required permission is not granted`() {
        // The real-world case: AOSP guards ACTION_ACCESSIBILITY_DETAILS_SETTINGS with a
        // signature permission a sideloaded app never holds, so a tap on the tile silently
        // did nothing instead of falling back to the general Accessibility list.
        assertFalse(
            GestureTileService.detailsReachable(resolves = true, requiredPermission = "signature.perm") { false },
        )
    }

    @Test
    fun `reachable when the required permission is granted`() {
        assertTrue(
            GestureTileService.detailsReachable(resolves = true, requiredPermission = "some.perm") { true },
        )
    }
}
