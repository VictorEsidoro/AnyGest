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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import io.github.victoresidoro.anygest.diagnostics.Trace
import io.github.victoresidoro.anygest.gesture.GestureAction
import io.github.victoresidoro.anygest.gesture.GestureTracker
import io.github.victoresidoro.anygest.gesture.HapticStrength
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.TouchSample
import io.github.victoresidoro.anygest.gesture.TrackerAction

/** The clicks a strip gives, from the settings. */
internal data class StripFeel(val enabled: Boolean, val strength: HapticStrength, val confirm: Boolean)

/**
 * The view inside one strip window. It turns the strip's touch events into [GestureTracker]
 * calls, runs the timers the tracker asks for, clicks, moves the feedback shapes, and tells
 * [Listener] what happened. It reads coordinates and times only: never window content, text
 * or keys.
 */
@SuppressLint("ViewConstructor")
internal class StripTouchView(
    context: Context,
    private val edge: ScreenEdge,
    private val tracker: GestureTracker,
    private val feel: StripFeel,
    private val belly: BackBellyOverlay?,
    private val drop: EdgeIndicatorOverlay?,
    private val listener: Listener,
) : View(context) {

    interface Listener {
        /** A DOWN: the strips go inert for this touch. Returns the touch's token. */
        fun touchStarted(edge: ScreenEdge): Long

        /** Any later event of the touch. */
        fun touchActive(token: Long)

        /** The touch armed or fired: it is no resting finger. */
        fun touchEngaged(token: Long)

        fun longPressPending(token: Long, waitMs: Long)

        /** False when the landscape pill holds the gesture back. */
        fun admit(action: GestureAction, edge: ScreenEdge): Boolean

        fun fire(token: Long, action: GestureAction, edge: ScreenEdge)

        /** A release that was no gesture, to replay into the app. The release is then the replay's. */
        fun replay(token: Long, edge: ScreenEdge, samples: List<TouchSample>)

        /** The touch is over and was not handed to a replay. */
        fun touchEnded(token: Long, edge: ScreenEdge)

        /** One line per event, kept only to explain a stuck touch. */
        fun lastEvent(description: String)

        /** The event's own time, to spot touches that arrive late. */
        fun delivered(eventTimeMs: Long)

        /** The window went away; whether that was expected is the listener's call. */
        fun detached(view: StripTouchView, edge: ScreenEdge)
    }

    private var token = 0L
    private val samples = ArrayList<TouchSample>(64)
    private var twoFingers = false
    private var moves = 0

    /** One click per gesture: a side swipe that clicked on arming stays quiet when it fires. */
    private var clicked = false

    /** What the touch came to, for the trace line written when it ends. */
    private var outcome = "nothing"

    private val holdTimer = Runnable { react(tracker.onHoldElapsed(), atRelease = false) }
    private val longPressTimer = Runnable { react(tracker.onLongPressElapsed(), atRelease = false) }

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    // Nothing to click here: a real tap is replayed to the app underneath.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        listener.delivered(event.eventTime)
        // Display coordinates, which is what the tracker and the replay use.
        val sample = TouchSample(event.rawX, event.rawY, event.eventTime)
        val action = event.actionMasked
        if (action != MotionEvent.ACTION_DOWN) listener.touchActive(token)
        if (action == MotionEvent.ACTION_DOWN) moves = 0 else if (action == MotionEvent.ACTION_MOVE) moves++
        listener.lastEvent(
            "$edge ${MotionEvent.actionToString(action)} at ${sample.xPx.toInt()},${sample.yPx.toInt()} " +
                "after ${event.eventTime - event.downTime}ms, moves=$moves, pointers=${event.pointerCount}, " +
                "abandoned=$twoFingers",
        )
        when (action) {
            MotionEvent.ACTION_DOWN -> down(sample)
            MotionEvent.ACTION_MOVE -> {
                keep(sample)
                follow(tracker.onMove(sample), sample, atRelease = false)
            }
            // Never a navigation gesture; replaying half a pinch would be worse than dropping it.
            MotionEvent.ACTION_POINTER_DOWN -> {
                twoFingers = true
                outcome = "second finger, dropped"
                settleShapes()
                stopTimers()
                tracker.onCancel()
            }
            MotionEvent.ACTION_UP -> up(sample, event.eventTime - event.downTime)
            // Usually the system taking a touch that began on its own bar, above the strips.
            MotionEvent.ACTION_CANCEL -> {
                outcome = "taken by the system"
                settleShapes()
                stopTimers()
                tracker.onCancel()
                samples.clear()
                summarize(event.eventTime - event.downTime)
                listener.touchEnded(token, edge)
            }
            else -> return false
        }
        return true
    }

    private fun down(sample: TouchSample) {
        Trace.touchDown()
        samples.clear()
        twoFingers = false
        clicked = false
        outcome = "nothing"
        samples += sample
        token = listener.touchStarted(edge)
        follow(tracker.onDown(sample), sample, atRelease = false)
    }

    /** Every UP ends the touch, or the strips would stay inert; only a replay takes the release over. */
    private fun up(sample: TouchSample, durationMs: Long) {
        stopTimers()
        val replayed = if (twoFingers) {
            false
        } else {
            keep(sample)
            follow(tracker.onUp(sample), sample, atRelease = true).also { drop?.release() }
        }
        samples.clear()
        summarize(durationMs)
        if (!replayed) listener.touchEnded(token, edge)
    }

    private fun keep(sample: TouchSample) {
        if (samples.size < MAX_SAMPLES) samples += sample
    }

    /** The shapes move first: a replay must never land under a visible window. */
    private fun follow(action: TrackerAction, sample: TouchSample, atRelease: Boolean): Boolean {
        belly?.follow(action, samples.firstOrNull()?.yPx ?: sample.yPx, inwardPx(sample))
        drop?.follow(action, sample.xPx, sample.yPx)
        return react(action, atRelease)
    }

    /** Returns true when the release was handed to a replay. */
    private fun react(action: TrackerAction, atRelease: Boolean): Boolean {
        when (action) {
            TrackerAction.None, TrackerAction.Disarmed -> Unit
            is TrackerAction.ScheduleLongPress -> {
                listener.longPressPending(token, action.delayMs)
                removeCallbacks(longPressTimer)
                postDelayed(longPressTimer, action.delayMs)
            }
            is TrackerAction.Armed -> {
                listener.touchEngaged(token)
                // A swipe is under way: this touch is no long press any more.
                removeCallbacks(longPressTimer)
                // The bottom edge clicks when it fires, so a plain swipe up does not buzz on the way.
                action.scheduleHoldMs?.let { postDelayed(holdTimer, it) } ?: click()
            }
            is TrackerAction.RearmHold -> {
                removeCallbacks(holdTimer)
                postDelayed(holdTimer, action.delayMs)
            }
            is TrackerAction.Fire -> fire(action.action)
            // Kept, not replayed: the app never asked for this swipe.
            TrackerAction.Reverted -> outcome = "pulled back, nothing"
            is TrackerAction.Unused -> {
                stopTimers()
                outcome = "unused (${action.reason})"
                if (atRelease && samples.isNotEmpty()) {
                    outcome += ", replayed"
                    listener.replay(token, edge, samples.toList())
                    return true
                }
            }
        }
        return false
    }

    private fun fire(action: GestureAction) {
        listener.touchEngaged(token)
        stopTimers()
        // Held behind the landscape pill: a lighter click, since nothing happened yet.
        if (!listener.admit(action, edge)) {
            outcome = "${action.id} held for the landscape pill"
            if (feel.enabled) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            return
        }
        outcome = action.id
        val clickedOnArming = clicked
        if (action == GestureAction.CircleToSearch) {
            if (feel.enabled) Haptics.play(this, Haptics.longPress(feel.strength))
        } else {
            click()
        }
        if (clickedOnArming && feel.enabled && feel.confirm) Haptics.play(this, Haptics.confirm(feel.strength))
        Log.i(TAG, "GESTURE_FIRED $edge ${action.id}")
        listener.fire(token, action, edge)
    }

    private fun click() {
        if (!feel.enabled || clicked) return
        clicked = true
        Haptics.play(this, Haptics.tick(feel.strength))
    }

    private fun summarize(durationMs: Long) {
        Trace.note("$edge touch ${durationMs} ms, $moves moves: $outcome")
    }

    private fun settleShapes() {
        belly?.collapse()
        drop?.release()
    }

    private fun stopTimers() {
        removeCallbacks(holdTimer)
        removeCallbacks(longPressTimer)
    }

    private fun inwardPx(sample: TouchSample): Float {
        val down = samples.firstOrNull() ?: return 0f
        return if (edge == ScreenEdge.RIGHT) down.xPx - sample.xPx else sample.xPx - down.xPx
    }

    // A strip taken off the screen mid-touch never sees its UP; its timers must not fire after.
    override fun onDetachedFromWindow() {
        stopTimers()
        super.onDetachedFromWindow()
        listener.detached(this, edge)
    }

    private companion object {
        const val TAG = "AnyGest"

        /** A few seconds of a 240 Hz panel, so a long touch is replayed whole. */
        const val MAX_SAMPLES = 4000
    }
}
