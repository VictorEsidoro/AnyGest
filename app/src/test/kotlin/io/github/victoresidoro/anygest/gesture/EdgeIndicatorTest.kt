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
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeIndicatorTest {

    @Test
    fun `the rise starts with the finger and never passes the ceiling`() {
        assertEquals(0f, EdgeIndicator.riseDp(0f), 0f)
        assertEquals(0f, EdgeIndicator.riseDp(-10f), 0f)
        assertEquals(5f, EdgeIndicator.riseDp(5f), 0.2f)
        assertTrue(EdgeIndicator.riseDp(10_000f) <= EdgeIndicator.MAX_RISE_DP)
    }

    @Test
    fun `the rise only ever grows with the travel`() {
        var last = -1f
        for (travel in 0..300 step 10) {
            val rise = EdgeIndicator.riseDp(travel.toFloat())
            assertTrue(rise >= last)
            last = rise
        }
    }

    @Test
    fun `following moves toward the target without overshooting`() {
        var value = 0f
        repeat(200) { value = EdgeIndicator.follow(value, 40f, 8, 60f) }
        assertTrue(value <= 40f)
        assertEquals(40f, value, 0.1f)
    }

    @Test
    fun `the same time takes the same path at any frame rate`() {
        var coarse = 0f
        repeat(10) { coarse = EdgeIndicator.follow(coarse, 40f, 16, 60f) }
        var fine = 0f
        repeat(20) { fine = EdgeIndicator.follow(fine, 40f, 8, 60f) }
        assertEquals(coarse, fine, 0.01f)
    }
}
