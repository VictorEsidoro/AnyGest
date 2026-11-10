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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamHoldTest {

    @Test
    fun `a release with an older token is ignored`() {
        val hold = StreamHold(5_000)
        val first = hold.start(0)
        val second = hold.start(10)
        assertFalse(hold.release(first))
        assertTrue(hold.held)
        assertTrue(hold.release(second))
        assertFalse(hold.held)
    }

    @Test
    fun `the deadline counts from the last touch event`() {
        val hold = StreamHold(5_000)
        val stream = hold.start(0)
        hold.activity(stream, 4_000)
        assertFalse(hold.expire(5_000))
        assertTrue(hold.expire(9_000))
        assertNull(hold.deadlineMs)
    }

    @Test
    fun `a replay in flight pushes the deadline`() {
        val hold = StreamHold(5_000)
        val stream = hold.start(0)
        hold.touch(stream, 3_000)
        assertEquals(8_000L, hold.deadlineMs)
    }

    @Test
    fun `an armed touch after an expiry holds again and forbids a replay`() {
        val hold = StreamHold(5_000)
        val stream = hold.start(0)
        hold.engage(stream)
        assertTrue(hold.expire(5_000))
        assertTrue(hold.activity(stream, 6_000))
        assertTrue(hold.held)
        assertFalse(hold.mayReplay(stream))
    }

    @Test
    fun `a reset ends the stream for good`() {
        val hold = StreamHold(5_000)
        val stream = hold.start(0)
        hold.reset()
        assertFalse(hold.held)
        assertFalse(hold.isCurrent(stream))
        assertFalse(hold.activity(stream, 1))
    }

    @Test
    fun `a stream with no replay gives up sooner than one with a replay`() {
        val hold = StreamHold(2_000, 5_000)
        val quiet = hold.start(0)
        assertEquals(2_000L, hold.deadlineMs)
        hold.touch(quiet, 100)
        assertEquals(5_100L, hold.deadlineMs)
        hold.start(200)
        assertEquals(2_200L, hold.deadlineMs)
    }

    private fun resting() = StreamHold(quietLimitMs = 2_000, replayLimitMs = 5_000, restLimitMs = 500)

    @Test
    fun `a touch that never armed gives the strips back after the rest limit`() {
        val hold = resting()
        hold.start(0)
        assertEquals(500L, hold.deadlineMs)
        assertFalse(hold.expire(499))
        assertTrue(hold.expire(500))
        assertTrue(hold.rested)
        assertFalse(hold.held)
    }

    @Test
    fun `an armed touch keeps the longer limit`() {
        val hold = resting()
        val stream = hold.start(0)
        hold.engage(stream)
        assertEquals(2_000L, hold.deadlineMs)
        assertFalse(hold.expire(1_999))
        assertTrue(hold.expire(2_000))
        assertFalse(hold.rested)
    }

    @Test
    fun `a resting grip that twitches does not take the strips again`() {
        val hold = resting()
        val stream = hold.start(0)
        hold.expire(500)
        assertFalse(hold.activity(stream, 800))
        assertFalse(hold.held)
    }

    @Test
    fun `a press released soon after resting is replayed once the strips settle`() {
        val hold = resting()
        val stream = hold.start(0)
        hold.expire(500)
        assertEquals(65L, hold.resumeForReplay(stream, 900, settleMs = 65, maxRestedTouchMs = 2_000))
        assertTrue(hold.held)
        assertEquals(5_900L, hold.deadlineMs)
        assertTrue(hold.release(stream))
    }

    @Test
    fun `a long grip is not replayed into the app`() {
        val hold = resting()
        val stream = hold.start(0)
        hold.expire(500)
        assertNull(hold.resumeForReplay(stream, 8_000, settleMs = 65, maxRestedTouchMs = 2_000))
        assertFalse(hold.held)
    }

    @Test
    fun `a stream still held replays without waiting`() {
        val hold = resting()
        val stream = hold.start(0)
        assertEquals(0L, hold.resumeForReplay(stream, 120, settleMs = 65, maxRestedTouchMs = 2_000))
        assertEquals(5_120L, hold.deadlineMs)
    }

    @Test
    fun `a stream held again after a stuck release is still not replayed`() {
        val hold = resting()
        val stream = hold.start(0)
        hold.engage(stream)
        hold.expire(2_000)
        assertTrue(hold.activity(stream, 2_100))
        assertNull(hold.resumeForReplay(stream, 2_200, settleMs = 65, maxRestedTouchMs = 2_000))
    }

    @Test
    fun `a new touch clears what the resting one left behind`() {
        val hold = resting()
        val old = hold.start(0)
        hold.expire(500)
        val fresh = hold.start(700)
        assertFalse(hold.rested)
        assertNull(hold.resumeForReplay(old, 800, settleMs = 65, maxRestedTouchMs = 2_000))
        assertEquals(0L, hold.resumeForReplay(fresh, 800, settleMs = 65, maxRestedTouchMs = 2_000))
    }
}
