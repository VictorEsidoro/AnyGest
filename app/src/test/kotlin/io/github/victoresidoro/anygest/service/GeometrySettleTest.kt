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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeometrySettleTest {

    private fun geometry(navBottom: Int) = ScreenGeometry(
        widthPx = 1_220, heightPx = 2_576, density = 2.75f,
        navLeftPx = 0, navRightPx = 0, navBottomPx = navBottom,
    )

    @Test
    fun `a series starts with the first wait`() {
        assertEquals(300L, GeometrySettle().begin())
    }

    @Test
    fun `two equal reads in a row end the series`() {
        val settle = GeometrySettle()
        settle.begin()

        assertEquals(700L, settle.next(geometry(63)))
        assertNull(settle.next(geometry(63)))
    }

    @Test
    fun `a read that still changes keeps the series going until the last wait`() {
        val settle = GeometrySettle()
        settle.begin()

        assertEquals(700L, settle.next(geometry(137)))
        assertEquals(1_500L, settle.next(geometry(63)))
        assertNull(settle.next(geometry(64)))
    }

    @Test
    fun `a new series forgets the reads of the last one`() {
        val settle = GeometrySettle()
        settle.begin()
        settle.next(geometry(63))
        settle.begin()

        assertEquals(700L, settle.next(geometry(63)))
    }

    @Test
    fun `the change is described field by field, and none is null`() {
        val before = geometry(137)

        assertEquals("bottom inset 137->63 px", before.describeChangeTo(geometry(63)))
        assertNull(before.describeChangeTo(geometry(137)))
        assertEquals(
            "width 1220->2576, height 2576->1220 px",
            before.describeChangeTo(before.copy(widthPx = 2_576, heightPx = 1_220)),
        )
    }
}
