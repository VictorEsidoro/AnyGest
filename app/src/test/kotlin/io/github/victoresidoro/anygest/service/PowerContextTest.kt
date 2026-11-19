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

class PowerContextTest {

    @Test
    fun `durations read as seconds, minutes or hours`() {
        assertEquals("7s", PowerContext.duration(7_400L))
        assertEquals("14m03s", PowerContext.duration(843_000L))
        assertEquals("3h12m", PowerContext.duration(11_520_000L))
    }

    @Test
    fun `a negative span reads as zero`() {
        assertEquals("0s", PowerContext.duration(-5L))
    }

    @Test
    fun `waking reports the sleep once and only when this process saw the screen go off`() {
        assertNull(PowerContext.screenWentOn(1_000L))
        PowerContext.screenWentOff(10_000L)
        assertEquals(5_000L, PowerContext.screenWentOn(15_000L))
        assertNull(PowerContext.screenWentOn(16_000L))
    }
}
