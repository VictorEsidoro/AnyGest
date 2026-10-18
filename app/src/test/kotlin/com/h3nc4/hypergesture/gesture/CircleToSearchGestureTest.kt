// Copyright (C) 2026  Victor Esidoro
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

package com.h3nc4.hypergesture.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircleToSearchGestureTest {

    /** Same phone density as GestureTrackerTest: 12dp stillness = 33px. */
    private val density = 2.75f

    private fun sample(x: Float, y: Float, t: Long) = TouchSample(x, y, t)

    private fun tracker(edge: ScreenEdge = ScreenEdge.BOTTOM, enabled: Boolean = true) =
        GestureTracker(
            edge,
            GestureConfiguration(circleToSearchEnabled = enabled, circleToSearchHoldMs = 500L),
            density,
        )

    private val fired = TrackerAction.Fire(GestureAction.CircleToSearch, ScreenEdge.BOTTOM)

    @Test
    fun `a bottom DOWN schedules the long press when the feature is on`() {
        assertEquals(
            TrackerAction.ScheduleLongPress(500L),
            tracker().onDown(sample(540f, 2395f, 0L)),
        )
    }

    @Test
    fun `nothing is scheduled when the feature is off`() {
        val t = tracker(enabled = false)
        assertEquals(TrackerAction.None, t.onDown(sample(540f, 2395f, 0L)))
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }

    @Test
    fun `side edges never schedule a long press`() {
        val t = tracker(edge = ScreenEdge.LEFT)
        assertEquals(TrackerAction.None, t.onDown(sample(5f, 1200f, 0L)))
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }

    @Test
    fun `a still finger fires Circle to Search when the timer elapses`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        // Jitter well inside the 33px stillness radius.
        assertEquals(TrackerAction.None, t.onMove(sample(545f, 2390f, 200L)))
        assertEquals(fired, t.onLongPressElapsed())
    }

    @Test
    fun `the release after a long press neither fires Home nor asks for a replay`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        t.onLongPressElapsed()
        assertEquals(TrackerAction.None, t.onUp(sample(540f, 2395f, 900L)))
    }

    @Test
    fun `the long press fires only once`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        assertEquals(fired, t.onLongPressElapsed())
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }

    @Test
    fun `a quick tap is still released as unused so it reaches the app`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        assertEquals(
            TrackerAction.Unused(RejectionReason.NO_MOVEMENT),
            t.onUp(sample(540f, 2395f, 80L)),
        )
        // A timer that was not cancelled in time must not fire on the finished touch.
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }

    @Test
    fun `an upward swipe still fires Home and blocks the long press`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        assertTrue(t.onMove(sample(540f, 2295f, 50L)) is TrackerAction.Armed)
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
        assertEquals(
            TrackerAction.Fire(GestureAction.Home, ScreenEdge.BOTTOM),
            t.onUp(sample(540f, 2295f, 70L)),
        )
    }

    @Test
    fun `swipe and hold still means Recents, not Circle to Search`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        t.onMove(sample(540f, 2295f, 50L))
        assertEquals(
            TrackerAction.Fire(GestureAction.Recents, ScreenEdge.BOTTOM),
            t.onHoldElapsed(),
        )
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }

    @Test
    fun `a finger that strayed and came back does not fire`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        // 40px sideways is past the stillness radius but short of any swipe.
        assertEquals(TrackerAction.None, t.onMove(sample(580f, 2395f, 100L)))
        assertEquals(TrackerAction.None, t.onMove(sample(540f, 2395f, 200L)))
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }

    @Test
    fun `a cancelled stream does not fire`() {
        val t = tracker()
        t.onDown(sample(540f, 2395f, 0L))
        t.onCancel()
        assertEquals(TrackerAction.None, t.onLongPressElapsed())
    }
}
