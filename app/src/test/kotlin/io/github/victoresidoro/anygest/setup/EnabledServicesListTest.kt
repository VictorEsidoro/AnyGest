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

package io.github.victoresidoro.anygest.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledServicesListTest {

    private val ours = "io.github.victoresidoro.anygest/io.github.victoresidoro.anygest.service.AnyGestAccessibilityService"
    private val other = "com.example.reader/com.example.reader.ReaderService"

    @Test
    fun `adding to an empty or missing list gives just our entry`() {
        assertEquals(ours, EnabledServicesList.with(null, ours))
        assertEquals(ours, EnabledServicesList.with("", ours))
    }

    @Test
    fun `adding keeps the other services and does not duplicate`() {
        assertEquals("$other:$ours", EnabledServicesList.with(other, ours))
        assertEquals("$other:$ours", EnabledServicesList.with("$other:$ours", ours))
    }

    @Test
    fun `removing keeps the other services`() {
        assertEquals(other, EnabledServicesList.without("$other:$ours", ours))
        assertEquals("", EnabledServicesList.without(ours, ours))
        assertEquals(other, EnabledServicesList.without(other, ours))
    }

    @Test
    fun `the short spelling of an entry counts as the full one`() {
        val short = "io.github.victoresidoro.anygest/.service.AnyGestAccessibilityService"

        assertTrue(EnabledServicesList.contains(short, ours))
        assertEquals(other, EnabledServicesList.without("$other:$short", ours))
        assertEquals(short, EnabledServicesList.with(short, ours))
    }

    @Test
    fun `a stray null string or blank pieces are dropped`() {
        assertFalse(EnabledServicesList.contains("null", ours))
        assertEquals(ours, EnabledServicesList.with("null: :", ours))
    }
}
