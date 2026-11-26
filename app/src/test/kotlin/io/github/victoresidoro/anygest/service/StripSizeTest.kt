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

package io.github.victoresidoro.anygest.service

import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import org.junit.Assert.assertEquals
import org.junit.Test

class StripSizeTest {

    /** 1200 x 2600 at density 3, with a 60 px bar at the bottom only. */
    private val portrait = ScreenGeometry(1200, 2600, 3f, navLeftPx = 0, navRightPx = 0, navBottomPx = 60)

    @Test
    fun `side strips are the edge width thick and the set share of the height long`() {
        // 20 dp is 60 px; 80% of 2600 is 2080.
        val config = GestureConfiguration()
        assertEquals(StripSize(60, 2080), StripSize.of(ScreenEdge.LEFT, config, portrait))
        assertEquals(StripSize(60, 2080), StripSize.of(ScreenEdge.RIGHT, config, portrait))
    }

    @Test
    fun `the bottom strip reaches under the bar and stops short of the corners`() {
        // 60 px thick plus the 60 px bar is 120, but never less than the system zone (52 dp, 156 px); 90% of 1200 is 1080.
        assertEquals(StripSize(1080, 156), StripSize.of(ScreenEdge.BOTTOM, GestureConfiguration(), portrait))
        val thick = GestureConfiguration(edgeWidthDp = 40f)
        assertEquals(StripSize(1080, 180), StripSize.of(ScreenEdge.BOTTOM, thick, portrait))
    }

    @Test
    fun `the bottom strip keeps the system zone height when the bar inset reads zero`() {
        val noInset = ScreenGeometry(1200, 2600, 3f, navLeftPx = 0, navRightPx = 0, navBottomPx = 0)
        assertEquals(StripSize(1080, 156), StripSize.of(ScreenEdge.BOTTOM, GestureConfiguration(), noInset))
    }

    @Test
    fun `a side bar in landscape widens only the strip on its edge`() {
        val landscape = ScreenGeometry(2600, 1200, 3f, navLeftPx = 0, navRightPx = 90, navBottomPx = 0)
        val config = GestureConfiguration(sideLengthPercent = 50)
        assertEquals(StripSize(60, 600), StripSize.of(ScreenEdge.LEFT, config, landscape))
        assertEquals(StripSize(150, 600), StripSize.of(ScreenEdge.RIGHT, config, landscape))
    }

    @Test
    fun `a strip is never smaller than one pixel`() {
        val config = GestureConfiguration(edgeWidthDp = 0f, sideLengthPercent = 0)
        assertEquals(StripSize(1, 1), StripSize.of(ScreenEdge.LEFT, config, portrait))
    }
}
