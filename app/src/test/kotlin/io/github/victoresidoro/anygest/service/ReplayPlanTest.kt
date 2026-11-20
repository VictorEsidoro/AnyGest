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

import io.github.victoresidoro.anygest.gesture.TouchSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayPlanTest {

    private fun plan(vararg samples: TouchSample) = ReplayPlan.of(samples.toList(), longPressTimeoutMs = 400L)!!

    @Test
    fun `nothing to replay without samples`() {
        assertNull(ReplayPlan.of(emptyList(), 400L))
    }

    @Test
    fun `a quick tap becomes a short 1 px stroke`() {
        val p = plan(TouchSample(100f, 200f, 0L), TouchSample(103f, 201f, 30L))
        assertTrue(p.tap)
        assertEquals(listOf(100f to 200f, 101f to 201f), p.points)
        assertEquals(ReplayPlan.MIN_TAP_MS, p.durationMs)
    }

    @Test
    fun `a slower tap is capped so the click lands soon`() {
        val p = plan(TouchSample(100f, 200f, 0L), TouchSample(100f, 200f, 250L))
        assertEquals(ReplayPlan.TAP_STROKE_CAP_MS, p.durationMs)
    }

    @Test
    fun `a press held past the long-press timeout keeps its length`() {
        val p = plan(TouchSample(100f, 200f, 0L), TouchSample(100f, 200f, 700L))
        assertEquals(700L, p.durationMs)
    }

    @Test
    fun `a drag is replayed point by point at its own speed, up to the limit`() {
        val drag = plan(TouchSample(10f, 500f, 0L), TouchSample(60f, 500f, 40L), TouchSample(200f, 500f, 120L))
        assertFalse(drag.tap)
        assertEquals(listOf(10f to 500f, 60f to 500f, 200f to 500f), drag.points)
        assertEquals(120L, drag.durationMs)

        val long = plan(TouchSample(10f, 500f, 0L), TouchSample(300f, 500f, 9_000L))
        assertEquals(ReplayPlan.MAX_REPLAY_MS, long.durationMs)
    }
}
