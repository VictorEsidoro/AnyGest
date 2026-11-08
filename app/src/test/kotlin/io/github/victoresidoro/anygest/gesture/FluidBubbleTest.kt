// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of HyperGesture.
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

package io.github.victoresidoro.anygest.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FluidBubbleTest {

    @Test
    fun `the drop rises with the finger, then eases to a stop`() {
        assertEquals(0f, FluidBubble.riseDp(0f), 0f)
        // Nearly one for one at first.
        assertEquals(10f, FluidBubble.riseDp(10f), 0.3f)
        // Never past the top, however far the finger goes.
        assertTrue(FluidBubble.riseDp(1_000f) <= FluidBubble.MAX_RISE_DP)
        assertTrue(FluidBubble.riseDp(120f) > FluidBubble.riseDp(60f))
    }

    @Test
    fun `the head's top sits at the rise and its centre one radius below`() {
        val shape = FluidBubble.shape(risePx = 100f, leanPx = 0f, density = 1f, overhangPx = 1f)
        assertEquals(16f, shape.headRadius, 0f)
        assertEquals(-(100f - 16f), shape.headY, 0f)
        assertEquals(0f, shape.headX, 0f)
    }

    @Test
    fun `a drop risen clear of the edge has a neck whose feet stand on it`() {
        val neck = FluidBubble.shape(risePx = 56f, leanPx = 0f, density = 1f, overhangPx = 1f).neck
        requireNotNull(neck)
        // The feet, moved onto the edge (y 0) to within a pixel or two, sit either side.
        assertTrue(neck[3] in -2f..2f)
        assertTrue(neck[19] in -2f..2f)
        assertTrue(neck[2] > 0f && neck[18] < 0f)
    }

    @Test
    fun `a straight drop is symmetric`() {
        val neck = requireNotNull(FluidBubble.shape(56f, 0f, 1f, 1f).neck)
        // Pairs mirror about x = 0: foot to foot, handle to handle, head point to head point.
        val pairs = listOf(0 to 20, 2 to 18, 4 to 16, 6 to 14, 8 to 12)
        for ((a, b) in pairs) {
            assertEquals(-neck[a], neck[b], 0.01f)
            assertEquals(neck[a + 1], neck[b + 1], 0.01f)
        }
    }

    @Test
    fun `leaning moves the head whole and leaves the feet in place`() {
        val straight = requireNotNull(FluidBubble.shape(56f, 0f, 1f, 1f).neck)
        val leaning = FluidBubble.shape(56f, 10f, 1f, 1f)
        val neck = requireNotNull(leaning.neck)
        assertEquals(10f, leaning.headX, 0f)
        // Feet stay put; the head's own neck points move with the head.
        assertEquals(straight[2], neck[2], 0.01f)
        assertEquals(straight[10] + 10f, neck[10], 0.01f)
    }

    @Test
    fun `the lean is held to a head's width`() {
        assertEquals(16f, FluidBubble.lean(500f, 400f, density = 1f), 0f)
        assertEquals(-16f, FluidBubble.lean(300f, 400f, density = 1f), 0f)
        assertEquals(5f, FluidBubble.lean(405f, 400f, density = 1f), 0f)
    }

    @Test
    fun `surface tension wobbles the rise once, a little`() {
        val spring = BubbleSpring(BubbleSpring.TENSION, BubbleSpring.RISE_FRICTION)
        spring.snap(0f)
        spring.target = 100f
        var peak = 0f
        repeat(60) {
            spring.step(16L)
            peak = maxOf(peak, spring.value)
        }
        assertTrue("peak $peak", peak in 105f..116f)
        assertTrue("settled at ${spring.value}", spring.settled)
    }

    @Test
    fun `FNG's springs are damped just short of critical`() {
        val ratio = BubbleSpring.dampingRatio(BubbleSpring.TENSION, BubbleSpring.FRICTION)
        assertEquals(1f, ratio, 0.02f)
    }

    @Test
    fun `a spring settles on its target within half a second and never overshoots`() {
        val spring = BubbleSpring()
        spring.snap(0f)
        spring.target = 100f
        var peak = 0f
        repeat(30) {
            spring.step(16L)
            peak = maxOf(peak, spring.value)
        }
        assertTrue("settled at ${spring.value}", spring.settled)
        assertTrue("overshot to $peak", peak <= 100.5f)
    }

    @Test
    fun `a long frame gap cannot make the spring blow up`() {
        val spring = BubbleSpring()
        spring.snap(0f)
        spring.target = 100f
        spring.step(5_000L)
        assertTrue(spring.value in 0f..100.5f)
    }
}
