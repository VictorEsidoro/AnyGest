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

package io.github.victoresidoro.anygest.gesture

import io.github.victoresidoro.anygest.gesture.GestureAction.Back
import io.github.victoresidoro.anygest.gesture.GestureAction.Home
import io.github.victoresidoro.anygest.gesture.GestureAction.NextApp
import io.github.victoresidoro.anygest.gesture.GestureAction.PreviousApp
import io.github.victoresidoro.anygest.gesture.GestureAction.Recents
import io.github.victoresidoro.anygest.gesture.RejectionReason.INSUFFICIENT_MOVEMENT
import io.github.victoresidoro.anygest.gesture.RejectionReason.NO_MOVEMENT
import io.github.victoresidoro.anygest.gesture.RejectionReason.OFF_AXIS
import io.github.victoresidoro.anygest.gesture.RejectionReason.TOO_SLOW_TO_ARM
import io.github.victoresidoro.anygest.gesture.RejectionReason.WRONG_DIRECTION
import io.github.victoresidoro.anygest.gesture.ScreenEdge.BOTTOM
import io.github.victoresidoro.anygest.gesture.ScreenEdge.LEFT
import io.github.victoresidoro.anygest.gesture.ScreenEdge.RIGHT
import io.github.victoresidoro.anygest.gesture.TrackerAction.Disarmed
import io.github.victoresidoro.anygest.gesture.TrackerAction.None
import io.github.victoresidoro.anygest.gesture.TrackerAction.Reverted
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The swipe rules of [GestureTracker], edge by edge. At density 3, the side distance (24 dp)
 * is 72 px, the bottom distance (10 dp) is 30 px and the Recents stillness (12 dp) is 36 px.
 * The screen is 1080 x 2400; the left strip is at x 0, the right at x 1080, the bottom at y 2400.
 */
class EdgeSwipeTest {

    private class Finger(
        private val edge: ScreenEdge,
        configuration: GestureConfiguration = GestureConfiguration(),
        density: Float = 3f,
    ) {
        private val tracker = GestureTracker(edge, configuration, density)

        fun down(x: Float, y: Float, at: Long = 0L) = tracker.onDown(TouchSample(x, y, at))
        fun move(x: Float, y: Float, at: Long) = tracker.onMove(TouchSample(x, y, at))
        fun up(x: Float, y: Float, at: Long) = tracker.onUp(TouchSample(x, y, at))
        fun holdElapses() = tracker.onHoldElapsed()
        fun cancel() = tracker.onCancel()
        fun fired(action: GestureAction) = TrackerAction.Fire(action, edge)
    }

    private val armedNoHold = TrackerAction.Armed(scheduleHoldMs = null)
    private val armedForRecents = TrackerAction.Armed(scheduleHoldMs = 100L)
    private fun unused(reason: RejectionReason) = TrackerAction.Unused(reason)
    private fun switching() = GestureConfiguration(appSwitchEnabled = true)

    // Back on the side edges

    @Test
    fun `an inward swipe on the left edge is Back when released`() {
        val f = Finger(LEFT)
        assertEquals(None, f.down(4f, 1000f))
        assertEquals(armedNoHold, f.move(124f, 1000f, 50L))
        assertEquals(f.fired(Back), f.up(124f, 1000f, 70L))
    }

    @Test
    fun `an inward swipe on the right edge is Back when released`() {
        val f = Finger(RIGHT)
        f.down(1076f, 1000f)
        assertEquals(armedNoHold, f.move(956f, 1000f, 50L))
        assertEquals(f.fired(Back), f.up(956f, 1000f, 70L))
    }

    @Test
    fun `a side edge has nothing to hold for`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        f.move(124f, 1000f, 50L)
        assertEquals(None, f.holdElapses())
        assertEquals(None, f.move(260f, 1000f, 90L))
        assertEquals(f.fired(Back), f.up(260f, 1000f, 110L))
    }

    @Test
    fun `bringing a Back swipe inside its distance disarms it, and pushing out again re-arms it`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        f.move(204f, 1000f, 50L)
        // 60 px of travel is under the 72 px distance.
        assertEquals(Disarmed, f.move(64f, 1000f, 120L))
        assertEquals(None, f.move(10f, 1000f, 150L))
        assertEquals(armedNoHold, f.move(204f, 1000f, 250L))
        assertEquals(f.fired(Back), f.up(204f, 1000f, 270L))
    }

    @Test
    fun `a Back swipe let go at the edge is reverted, with or without a move to disarm it first`() {
        val disarmedFirst = Finger(RIGHT)
        disarmedFirst.down(1076f, 1000f)
        disarmedFirst.move(876f, 1000f, 50L)
        assertEquals(Disarmed, disarmedFirst.move(1070f, 1000f, 150L))
        assertEquals(Reverted, disarmedFirst.up(1070f, 1000f, 170L))

        val batched = Finger(LEFT)
        batched.down(4f, 1000f)
        batched.move(204f, 1000f, 50L)
        assertEquals(Reverted, batched.up(4f, 1000f, 150L))
    }

    @Test
    fun `a small step back toward the edge keeps Back`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        f.move(204f, 1000f, 50L)
        assertEquals(None, f.move(164f, 1000f, 80L))
        assertEquals(f.fired(Back), f.up(164f, 1000f, 90L))
    }

    @Test
    fun `reverting uses the configured distance`() {
        // 50 dp is 150 px: armed at 200, let go at 120.
        val f = Finger(LEFT, GestureConfiguration(sideMinimumSwipeDistanceDp = 50f))
        f.down(4f, 1000f)
        assertEquals(armedNoHold, f.move(204f, 1000f, 50L))
        assertEquals(Reverted, f.up(124f, 1000f, 100L))
    }

    // Home and Recents on the bottom edge

    @Test
    fun `an upward swipe is Home when released`() {
        val f = Finger(BOTTOM)
        f.down(540f, 2394f)
        assertEquals(armedForRecents, f.move(540f, 2304f, 40L))
        assertEquals(f.fired(Home), f.up(540f, 2304f, 60L))
    }

    @Test
    fun `holding after arming fires Recents at once, and only once`() {
        val f = Finger(BOTTOM)
        f.down(540f, 2394f)
        f.move(540f, 2304f, 40L)
        assertEquals(f.fired(Recents), f.holdElapses())
        assertEquals(None, f.holdElapses())
        assertEquals(None, f.move(540f, 2100f, 300L))
        assertEquals(None, f.up(540f, 2100f, 800L))
    }

    @Test
    fun `moving while armed starts the hold over, a jitter does not`() {
        val f = Finger(BOTTOM)
        f.down(540f, 2394f)
        f.move(540f, 2304f, 40L)
        assertEquals(None, f.move(550f, 2296f, 60L))
        assertEquals(TrackerAction.RearmHold(100L), f.move(540f, 2260f, 90L))
        assertEquals(TrackerAction.RearmHold(100L), f.move(620f, 2260f, 120L))
        assertEquals(f.fired(Recents), f.holdElapses())
    }

    @Test
    fun `an armed swipe may be held for as long as the user likes`() {
        val f = Finger(BOTTOM)
        f.down(540f, 2394f)
        f.move(540f, 2304f, 40L)
        assertEquals(TrackerAction.RearmHold(100L), f.move(540f, 2200f, 6_000L))
        assertEquals(f.fired(Recents), f.holdElapses())
    }

    @Test
    fun `the Recents hold is the configured one`() {
        val f = Finger(BOTTOM, GestureConfiguration(recentsHoldMs = 250L))
        f.down(540f, 2394f)
        assertEquals(TrackerAction.Armed(250L), f.move(540f, 2304f, 40L))
    }

    @Test
    fun `a Home swipe brought back down is still Home`() {
        val f = Finger(BOTTOM)
        f.down(540f, 2394f)
        f.move(540f, 2304f, 40L)
        f.move(540f, 2390f, 80L)
        assertEquals(f.fired(Home), f.up(540f, 2390f, 90L))
    }

    @Test
    fun `no hold fires before arming or after a cancel`() {
        val early = Finger(BOTTOM)
        early.down(540f, 2394f)
        assertEquals(None, early.holdElapses())

        val cancelled = Finger(BOTTOM)
        cancelled.down(540f, 2394f)
        cancelled.move(540f, 2304f, 40L)
        cancelled.cancel()
        assertEquals(None, cancelled.holdElapses())
    }

    // Touches that are not gestures

    @Test
    fun `a tap is unused with no movement`() {
        val f = Finger(BOTTOM)
        f.down(540f, 2394f)
        assertEquals(unused(NO_MOVEMENT), f.up(540f, 2394f, 50L))
    }

    @Test
    fun `a swipe short of the distance is unused`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        assertEquals(None, f.move(34f, 1000f, 40L))
        assertEquals(unused(INSUFFICIENT_MOVEMENT), f.up(34f, 1000f, 60L))
    }

    @Test
    fun `a swipe toward the edge is unused as the wrong direction`() {
        val side = Finger(LEFT)
        side.down(60f, 1000f)
        side.move(10f, 1000f, 40L)
        assertEquals(unused(WRONG_DIRECTION), side.up(10f, 1000f, 60L))

        val bottom = Finger(BOTTOM)
        bottom.down(540f, 2300f)
        assertEquals(unused(WRONG_DIRECTION), bottom.up(540f, 2350f, 60L))
    }

    @Test
    fun `the cone around the axis decides what arms`() {
        // 100 px in with 150 px along is outside 45 degrees; 100 in with 90 along is inside.
        val outside = Finger(LEFT)
        outside.down(4f, 1000f)
        assertEquals(None, outside.move(104f, 1150f, 50L))
        assertEquals(unused(OFF_AXIS), outside.up(104f, 1150f, 70L))

        val inside = Finger(LEFT)
        inside.down(4f, 1000f)
        assertEquals(armedNoHold, inside.move(104f, 1090f, 50L))
    }

    @Test
    fun `a touch that takes too long to arm is dropped`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        assertEquals(unused(TOO_SLOW_TO_ARM), f.move(20f, 1000f, 1_200L))
        assertEquals(None, f.move(254f, 1000f, 1_300L))
        assertEquals(unused(TOO_SLOW_TO_ARM), f.up(254f, 1000f, 1_400L))
    }

    @Test
    fun `a move along a side edge alone is too short`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        assertEquals(unused(INSUFFICIENT_MOVEMENT), f.up(4f, 1100f, 60L))
    }

    @Test
    fun `events with no DOWN before them do nothing`() {
        val f = Finger(LEFT)
        assertEquals(None, f.move(124f, 1000f, 50L))
        assertEquals(unused(NO_MOVEMENT), f.up(124f, 1000f, 60L))
    }

    @Test
    fun `a second release after the gesture ended fires nothing`() {
        val f = Finger(LEFT)
        f.down(4f, 1000f)
        f.move(124f, 1000f, 50L)
        assertEquals(f.fired(Back), f.up(124f, 1000f, 70L))
        assertEquals(unused(INSUFFICIENT_MOVEMENT), f.up(124f, 1000f, 80L))
    }

    // Distances

    @Test
    fun `distances are in dp, so a denser screen needs more pixels`() {
        // 80 px is 26.7 dp at density 3 but 20 dp at density 4.
        val phone = Finger(LEFT)
        phone.down(4f, 1000f)
        assertEquals(armedNoHold, phone.move(84f, 1000f, 50L))

        val dense = Finger(LEFT, density = 4f)
        dense.down(4f, 1000f)
        assertEquals(None, dense.move(84f, 1000f, 50L))
    }

    @Test
    fun `the bottom edge arms after a shorter swipe than the sides`() {
        // 45 px is 15 dp: past the bottom's 10, short of the sides' 24.
        val bottom = Finger(BOTTOM)
        bottom.down(540f, 2394f)
        assertEquals(armedForRecents, bottom.move(540f, 2349f, 40L))

        val side = Finger(LEFT)
        side.down(4f, 1000f)
        assertEquals(None, side.move(49f, 1000f, 40L))
    }

    @Test
    fun `a longer configured distance arms later`() {
        // 60 dp is 180 px.
        val f = Finger(LEFT, GestureConfiguration(sideMinimumSwipeDistanceDp = 60f))
        f.down(4f, 1000f)
        assertEquals(None, f.move(124f, 1000f, 50L))
        assertEquals(armedNoHold, f.move(204f, 1000f, 80L))
    }

    // A whole swipe delivered as DOWN and UP only

    @Test
    fun `a DOWN and UP pair is judged by the release point alone`() {
        val swipe = Finger(LEFT)
        assertEquals(None, swipe.down(2f, 1000f))
        assertEquals(swipe.fired(Back), swipe.up(302f, 1000f, 100L))

        val slow = Finger(LEFT)
        slow.down(2f, 1000f)
        assertEquals(unused(TOO_SLOW_TO_ARM), slow.up(302f, 1000f, 4_000L))

        val short = Finger(LEFT)
        short.down(2f, 1000f)
        assertEquals(unused(INSUFFICIENT_MOVEMENT), short.up(22f, 1000f, 60L))

        val backwards = Finger(RIGHT)
        backwards.down(1072f, 1000f)
        assertEquals(unused(WRONG_DIRECTION), backwards.up(1079f, 1000f, 60L))
    }

    // Switching apps along the bottom edge

    @Test
    fun `along the bottom edge, right is the previous app and left the next`() {
        val right = Finger(BOTTOM, switching())
        right.down(300f, 2394f)
        assertEquals(armedNoHold, right.move(500f, 2394f, 50L))
        assertEquals(None, right.holdElapses())
        assertEquals(right.fired(PreviousApp), right.up(500f, 2394f, 70L))

        val left = Finger(BOTTOM, switching())
        left.down(500f, 2394f)
        assertEquals(armedNoHold, left.move(300f, 2394f, 50L))
        assertEquals(left.fired(NextApp), left.up(300f, 2394f, 70L))
    }

    @Test
    fun `a switch delivered as DOWN and UP still switches`() {
        val f = Finger(BOTTOM, switching())
        f.down(300f, 2394f)
        assertEquals(f.fired(PreviousApp), f.up(500f, 2394f, 50L))
    }

    @Test
    fun `a steep diagonal is Home, not a switch`() {
        // 200 px along with 300 px up is outside the sideways cone and inside the upward one.
        val f = Finger(BOTTOM, switching())
        f.down(300f, 2394f)
        assertEquals(armedForRecents, f.move(500f, 2094f, 50L))
        assertEquals(f.fired(Home), f.up(500f, 2094f, 70L))
    }

    @Test
    fun `a sideways nudge is too short to switch`() {
        val f = Finger(BOTTOM, switching())
        f.down(300f, 2394f)
        assertEquals(None, f.move(320f, 2394f, 40L))
        assertEquals(unused(INSUFFICIENT_MOVEMENT), f.up(320f, 2394f, 60L))
    }

    @Test
    fun `switching never applies to a side edge`() {
        val f = Finger(LEFT, switching())
        f.down(4f, 1000f)
        assertEquals(armedNoHold, f.move(124f, 1000f, 50L))
        assertEquals(f.fired(Back), f.up(124f, 1000f, 70L))
    }

    @Test
    fun `a swipe that armed upward first can still turn into a switch`() {
        val f = Finger(BOTTOM, switching())
        f.down(300f, 2394f)
        // 35 px up arms Home first.
        assertEquals(armedForRecents, f.move(305f, 2359f, 30L))
        assertEquals(armedNoHold, f.move(620f, 2364f, 80L))
        assertEquals(None, f.holdElapses())
        assertEquals(f.fired(PreviousApp), f.up(620f, 2364f, 100L))
    }

    @Test
    fun `a swipe that keeps going up is never read as a switch`() {
        val f = Finger(BOTTOM, switching())
        f.down(540f, 2394f)
        f.move(540f, 2294f, 50L)
        assertEquals(TrackerAction.RearmHold(100L), f.move(580f, 2194f, 90L))
        assertEquals(f.fired(Recents), f.holdElapses())
    }

    @Test
    fun `with switching off a sideways swipe is unused`() {
        val f = Finger(BOTTOM)
        f.down(300f, 2394f)
        assertEquals(None, f.move(500f, 2394f, 50L))
        assertEquals(unused(INSUFFICIENT_MOVEMENT), f.up(500f, 2394f, 70L))
    }
}
