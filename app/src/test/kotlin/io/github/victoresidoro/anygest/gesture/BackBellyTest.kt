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

package io.github.victoresidoro.anygest.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackBellyTest {

    @Test
    fun `the belly is empty at the edge, full at the end of the drag and clamped past both`() {
        assertEquals(0f, BackBelly.scale(-10f), 0f)
        assertEquals(0f, BackBelly.scale(0f), 0f)
        assertEquals(0.5f, BackBelly.scale(BackBelly.FULL_DRAG_DP / 2), 1e-6f)
        assertEquals(1f, BackBelly.scale(BackBelly.FULL_DRAG_DP), 1e-6f)
        assertEquals(1f, BackBelly.scale(BackBelly.FULL_DRAG_DP * 3), 1e-6f)
    }

    @Test
    fun `the belly never shrinks as the finger goes in`() {
        val scales = (0..BackBelly.FULL_DRAG_DP.toInt()).map { BackBelly.scale(it.toFloat()) }
        assertTrue(scales.zipWithNext().all { (before, after) -> after >= before })
    }

    @Test
    fun `the outline closes at the top and bottom and is widest in the middle, evenly`() {
        assertEquals(0f, BackBelly.profile(0f), 1e-6f)
        assertEquals(0f, BackBelly.profile(1f), 1e-6f)
        assertEquals(1f, BackBelly.profile(0.5f), 1e-6f)
        (0..49).map { it / 100f }.forEach { down ->
            assertEquals(BackBelly.profile(down), BackBelly.profile(1f - down), 1e-5f)
        }
    }

    @Test
    fun `the outline stays within 2 px of the rows measured off the HyperOS asset`() {
        // Widths in px of Xiaomi's 84 by 858 px asset, every 66 rows from the top.
        val widths = listOf(0.2f, 2.0f, 8.0f, 20.2f, 40.5f, 65.7f, 81.2f, 81.1f, 65.1f, 39.8f, 19.7f, 7.7f, 1.8f)
        widths.forEachIndexed { n, px ->
            assertEquals("row ${n * 66}", px, BackBelly.profile((n * 66 + 0.5f) / 858f) * 84f, 2f)
        }
    }

    // Runs a real left tracker at density 3 (3 px per dp) and feeds every verdict to a fresh state.
    private class Swipe {
        val tracker = GestureTracker(ScreenEdge.LEFT, GestureConfiguration(), density = 3f)
        val belly = BackBellyState()
        var clock = 0L

        init {
            tracker.onDown(TouchSample(0f, 1200f, clock))
        }

        fun moveTo(xPx: Float): TrackerAction {
            clock += 20L
            return tracker.onMove(TouchSample(xPx, 1200f, clock)).also { belly.follow(it, xPx / 3f) }
        }

        fun lift(xPx: Float): TrackerAction {
            clock += 20L
            return tracker.onUp(TouchSample(xPx, 1200f, clock)).also { belly.follow(it, xPx / 3f) }
        }
    }

    @Test
    fun `a swipe that arms shows the belly with its arrow`() {
        val swipe = Swipe()
        assertFalse(swipe.belly.visible)
        swipe.moveTo(180f)
        assertTrue(swipe.belly.visible)
        assertTrue(swipe.belly.arrowShown)
        assertEquals(BackBelly.scale(60f), swipe.belly.scale, 1e-6f)
    }

    @Test
    fun `pulling back inside the trigger drops only the arrow, and pushing out brings it back`() {
        val swipe = Swipe()
        swipe.moveTo(180f)
        swipe.moveTo(30f)
        assertFalse(swipe.belly.arrowShown)
        assertTrue(swipe.belly.visible)
        assertEquals(BackBelly.scale(10f), swipe.belly.scale, 0f)
        swipe.moveTo(180f)
        assertTrue(swipe.belly.arrowShown)
    }

    @Test
    fun `the belly shrinks step by step on the way back and is gone on a revert`() {
        val swipe = Swipe()
        val scales = listOf(180f, 120f, 60f, 3f).map { x -> swipe.moveTo(x); swipe.belly.scale }
        assertTrue(scales.zipWithNext().all { (before, after) -> after < before })
        assertEquals(TrackerAction.Reverted, swipe.lift(3f))
        assertFalse(swipe.belly.visible)
        assertFalse(swipe.belly.arrowShown)
        assertEquals(0f, swipe.belly.scale, 0f)
    }

    @Test
    fun `a fired swipe takes the belly with it`() {
        val swipe = Swipe()
        swipe.moveTo(180f)
        swipe.lift(170f)
        assertFalse(swipe.belly.visible)
    }

    @Test
    fun `a move that arrives with no belly up does not bring one back`() {
        val belly = BackBellyState()
        belly.follow(TrackerAction.None, 50f)
        assertFalse(belly.visible)
        assertEquals(0f, belly.scale, 0f)
    }
}
