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

package io.github.victoresidoro.anygest.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceLogTest {

    @Test
    fun `a gesture says how long after the touch it fired`() {
        val log = TraceLog()
        log.touchDown(1_000)
        log.gesture(1_180, "LEFT", "back")

        assertEquals(listOf("-0 s  LEFT -> back, 180 ms after touch"), log.lines(1_900))
    }

    @Test
    fun `a gesture without a recorded touch says so`() {
        val log = TraceLog()
        log.gesture(500, "BOTTOM", "home")

        assertTrue("? ms after touch" in log.lines(500).single())
    }

    @Test
    fun `the first window change after an accepted action is timed from the call`() {
        val log = TraceLog()
        log.performed(2_000, 12, "recents", ok = true)
        log.windowChanged(2_340, "com.example.app/Main")
        log.windowChanged(2_400, "other")

        val lines = log.lines(3_000)
        assertEquals(2, lines.size)
        assertTrue("took 12 ms, accepted" in lines[0])
        assertTrue("window -> com.example.app/Main, 340 ms after recents started" in lines[1])
    }

    @Test
    fun `an event from before the call, or long after it, is not attributed to it`() {
        val log = TraceLog()
        log.performed(2_000, 5, "home", ok = true)
        log.windowChanged(1_990, "stale")
        log.windowChanged(2_000 + TraceLog.WINDOW_WAIT_MS + 1, "late")
        log.windowChanged(2_100, "after the wait was dropped")

        assertEquals(1, log.lines(6_000).size)
    }

    @Test
    fun `a refused action waits for nothing`() {
        val log = TraceLog()
        log.performed(2_000, 5, "back", ok = false)
        log.windowChanged(2_050, "x")

        val lines = log.lines(2_100)
        assertEquals(1, lines.size)
        assertTrue("refused" in lines.single())
    }

    @Test
    fun `only the newest entries are kept`() {
        val log = TraceLog(capacity = 3)
        for (i in 1..5) log.gesture(i * 1_000L, "LEFT", "a$i")

        val lines = log.lines(5_000)
        assertEquals(3, lines.size)
        assertTrue("a3" in lines.first() && "a5" in lines.last())
    }
}
