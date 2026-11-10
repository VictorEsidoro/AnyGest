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
import org.junit.Test

class ServiceStartTest {

    @Test
    fun `a clean stop before is just a connection`() {
        assertEquals("service connected", ServiceStart.note(false, 100, 200))
    }

    @Test
    fun `an update that killed the process is not reported as a kill`() {
        assertEquals("service started after an app update", ServiceStart.note(true, 100, 200))
    }

    @Test
    fun `the same install still up at the next start was killed`() {
        assertEquals(
            "service started after an unclean stop (process killed?)",
            ServiceStart.note(true, 200, 200),
        )
    }

    @Test
    fun `an unreadable update time does not hide a kill`() {
        assertEquals(
            "service started after an unclean stop (process killed?)",
            ServiceStart.note(true, 100, 0),
        )
    }
}
