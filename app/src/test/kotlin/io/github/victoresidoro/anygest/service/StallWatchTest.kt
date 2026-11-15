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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StallWatchTest {

    @Test
    fun `ticks on time say nothing`() {
        val watch = StallWatch()
        watch.start(0)

        assertNull(watch.tick(2_000))
        assertNull(watch.tick(4_000))
    }

    @Test
    fun `a tick that comes late reports the gap in seconds`() {
        val watch = StallWatch()
        watch.start(0)
        watch.tick(2_000)

        val note = watch.tick(34_000)

        assertNotNull(note)
        assertTrue("32s" in note!!)
    }

    @Test
    fun `the gap while the screen was off is not a stall`() {
        val watch = StallWatch()
        watch.start(0)
        watch.stop()

        assertNull(watch.tick(600_000))
        assertNull(watch.tick(602_000))
    }

    @Test
    fun `a stall is reported once, not again on the next tick`() {
        val watch = StallWatch()
        watch.start(0)
        watch.tick(30_000)

        assertNull(watch.tick(32_000))
    }
}

class LateTouchWatchTest {

    @Test
    fun `a touch delivered quickly says nothing`() {
        assertNull(LateTouchWatch().check(delayMs = 40, nowMs = 1_000))
    }

    @Test
    fun `a late touch reports how late`() {
        val note = LateTouchWatch().check(delayMs = 4_200, nowMs = 1_000)

        assertTrue("4200ms" in note!!)
    }

    @Test
    fun `a long stall is one line, not one per touch`() {
        val watch = LateTouchWatch()

        assertNotNull(watch.check(900, nowMs = 1_000))
        assertNull(watch.check(800, nowMs = 3_000))
        assertNotNull(watch.check(700, nowMs = 12_000))
    }
}

class ExitReasonsTest {

    @Test
    fun `the reason is named and the description kept`() {
        val text = ExitReasons.describe(3, 9, 400, "11-15 03:10:00", "low on memory")

        assertEquals(
            "last process exit: LOW_MEMORY at 11-15 03:10:00, status 9, importance 400: low on memory",
            text,
        )
    }

    @Test
    fun `a blank description adds nothing and an unknown reason shows its number`() {
        val text = ExitReasons.describe(99, 0, 100, "11-15 03:10:00", " ")

        assertEquals("last process exit: UNKNOWN(99) at 11-15 03:10:00, status 0, importance 100", text)
    }
}
