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

class LockWatchTest {

    private fun watch(failOpenMs: Long = 120_000L) = LockWatch(failOpenMs).also { it.begin(1_000L) }

    @Test
    fun `an unlocked phone arms at once and stops reading`() {
        assertEquals(LockWatch.Step(true, null), watch().read(locked = false, nowMs = 1_000L))
    }

    @Test
    fun `a locked phone stays disarmed and is read quickly at first`() {
        assertEquals(LockWatch.Step(false, 300L), watch().read(locked = true, nowMs = 2_000L))
    }

    @Test
    fun `reads slow down after the first twenty seconds`() {
        assertEquals(LockWatch.Step(false, 1_000L), watch().read(locked = true, nowMs = 25_000L))
    }

    @Test
    fun `unlocking arms the strips and ends the reading`() {
        val w = watch()
        w.read(locked = true, nowMs = 2_000L)
        assertEquals(LockWatch.Step(true, null), w.read(locked = false, nowMs = 5_000L))
    }

    @Test
    fun `a lock state that never clears is overridden after the limit`() {
        val w = watch(failOpenMs = 60_000L)
        assertEquals(LockWatch.Step(false, 1_000L), w.read(locked = true, nowMs = 60_000L))
        assertEquals(LockWatch.Step(true, null), w.read(locked = true, nowMs = 61_000L))
    }

    @Test
    fun `a new wake restarts the clock`() {
        val w = watch(failOpenMs = 60_000L)
        w.begin(100_000L)
        assertEquals(LockWatch.Step(false, 300L), w.read(locked = true, nowMs = 100_500L))
    }
}
