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

import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.navigation.NavigationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationModeTileServiceTest {

    @Test
    fun `each navigation mode has its own subtitle`() {
        val subtitles = NavigationMode.entries.map(NavigationModeTileService::subtitleFor)
        assertEquals(subtitles.size, subtitles.toSet().size)
    }

    @Test
    fun `gestural reads as the active state`() {
        assertEquals(R.string.tile_navigation_gestural, NavigationModeTileService.subtitleFor(NavigationMode.GESTURAL))
    }

    @Test
    fun `three button and unknown fall back to opening the screen`() {
        assertEquals(
            R.string.tile_navigation_buttons,
            NavigationModeTileService.subtitleFor(NavigationMode.THREE_BUTTON),
        )
        assertEquals(
            R.string.tile_navigation_unknown,
            NavigationModeTileService.subtitleFor(NavigationMode.UNKNOWN),
        )
    }
}
