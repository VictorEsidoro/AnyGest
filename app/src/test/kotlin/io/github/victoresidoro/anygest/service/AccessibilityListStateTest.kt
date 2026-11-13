// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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
import org.junit.Test

class AccessibilityListStateTest {

    private val own = "io.github.victoresidoro.anygest"

    @Test
    fun `lists whether AnyGest is in the list and who else is`() {
        assertEquals(
            "accessibility_enabled=1, AnyGest listed=true, others=com.a,com.b",
            AccessibilityListState.describe("com.a/.S:$own/.service.X:com.b/.T", 1, own),
        )
    }

    @Test
    fun `an empty or missing list says AnyGest is gone`() {
        val gone = "accessibility_enabled=0, AnyGest listed=false, others=none"
        assertEquals(gone, AccessibilityListState.describe("", 0, own))
        assertEquals(gone, AccessibilityListState.describe(null, 0, own))
        assertEquals(gone, AccessibilityListState.describe("null", 0, own))
    }

    @Test
    fun `an unreadable switch is a question mark and repeats are listed once`() {
        assertEquals(
            "accessibility_enabled=?, AnyGest listed=false, others=com.a",
            AccessibilityListState.describe("com.a/.S:com.a/.T", null, own),
        )
    }
}
