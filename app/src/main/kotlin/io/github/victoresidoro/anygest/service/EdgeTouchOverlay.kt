// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import io.github.victoresidoro.anygest.gesture.GestureAction
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.GestureTracker
import io.github.victoresidoro.anygest.gesture.HapticStrength
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.TouchSample
import io.github.victoresidoro.anygest.gesture.TrackerAction
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Captures raw touches in thin strips along the screen edges and feeds them to a
 * per-edge [GestureTracker].
 *
 * A plain [android.accessibilityservice.AccessibilityService] receives no arbitrary
 * [MotionEvent]s, hence real windows; `TYPE_ACCESSIBILITY_OVERLAY` needs no
 * `SYSTEM_ALERT_WINDOW` permission. A strip must consume `ACTION_DOWN` to see whether a
 * swipe develops, so a touch that is not a gesture goes to [TouchReplayer] instead of
 * being swallowed.
 *
 * While a finger holds a strip, every strip goes `FLAG_NOT_TOUCHABLE` **and** alpha 0.
 * Applying it at `ACTION_DOWN` gives the asynchronous window update the whole touch to
 * take effect, since window changes only affect the targeting of *new* touches; alpha 0
 * because a visible non-touchable overlay still counts toward Android's
 * obscuring-opacity cap, and an injected touch under one is discarded as untrusted.
 */
internal class EdgeTouchOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val replayer: TouchReplayer,
    private val onGestureAction: (GestureAction, ScreenEdge) -> Unit,
    private val layout: OverlayLayout = OverlayLayout(),
) {

    private val strips = mutableMapOf<ScreenEdge, View>()

    private val bellies = mutableMapOf<ScreenEdge, BackBellyOverlay>()

    /**
     * While a stream is held the strips are inert (not touchable, alpha 0). If the UP or CANCEL
     * that ends it never reaches a strip, they would stay inert and the gestures would be dead
     * while the service still looks healthy, so a stream that goes quiet for too long is
     * released. Tokens keep a late release of an old stream from touching a new one.
     */
    private val hold = StreamHold(STUCK_STREAM_MS)

    private val handler = Handler(Looper.getMainLooper())

    private val stuckCheck = Runnable(::onStuckCheck)

    fun install(configuration: GestureConfiguration, geometry: ScreenGeometry) {
        remove()

        val edges = buildList {
            if (configuration.leftEdgeBackEnabled) add(ScreenEdge.LEFT)
            if (configuration.rightEdgeBackEnabled) add(ScreenEdge.RIGHT)
            add(ScreenEdge.BOTTOM)
        }

        for (edge in edges) {
            if (edge != ScreenEdge.BOTTOM) {
                bellies[edge] = BackBellyOverlay(context, windowManager, edge, geometry).also {
                    it.install()
                }
            }
            addStrip(edge, configuration, geometry)
        }
        applyInteractivity()
    }

    fun remove() {
        handler.removeCallbacks(stuckCheck)
        strips.values.forEach { view ->
            runCatching { windowManager.removeView(view) }
        }
        strips.clear()
        bellies.values.forEach(BackBellyOverlay::remove)
        bellies.clear()
        // Detaching kills any in-flight stream, so its UP never arrives to release the
        // hold; without this reset, freshly installed strips start out inert.
        hold.reset()
    }

    /**
     * Called when the screen comes on, the moment someone is about to use the gestures. False
     * when the strips are missing or detached and must be installed again; a stream that
     * outlived its deadline while the screen was off is released here too.
     */
    fun healthy(): Boolean {
        if (hold.expire(SystemClock.uptimeMillis())) {
            report("strips were stuck inert, released when the screen came on")
            applyInteractivity()
        }
        return strips.isNotEmpty() && strips.values.all { it.isAttachedToWindow }
    }

    private fun addStrip(
        edge: ScreenEdge,
        configuration: GestureConfiguration,
        geometry: ScreenGeometry,
    ) {
        val view = TouchCaptureView(
            context = context,
            edge = edge,
            tracker = GestureTracker(
                edge,
                configuration,
                geometry.density,
                // Display coordinates, like the rawX the tracker is fed.
                handleCenterXPx = if (edge == ScreenEdge.BOTTOM) geometry.widthPx / 2f else null,
            ),
            hapticsEnabled = configuration.hapticFeedbackEnabled,
            hapticStrength = configuration.hapticStrength,
            belly = bellies[edge],
            onStreamStart = ::onStreamStart,
            onActivity = ::onActivity,
            onFire = { stream, action -> if (hold.isCurrent(stream)) onFire(action, edge) },
            onUnused = ::onUnused,
            onStreamEnd = { stream -> releaseStream(stream) },
        )
        val params = paramsFor(edge, configuration, geometry)
        runCatching {
            windowManager.addView(view, params)
            strips[edge] = view
            Log.d(TAG, "strip $edge installed ${params.width}x${params.height} geometry=$geometry")
            // Keep the platform's own back gesture off these pixels.
            view.post {
                if (view.width > 0 && view.height > 0) {
                    view.systemGestureExclusionRects =
                        listOf(Rect(0, 0, view.width, view.height))
                }
            }
        }.onFailure { Log.e(TAG, "Could not add $edge strip", it) }
    }

    private fun paramsFor(
        edge: ScreenEdge,
        configuration: GestureConfiguration,
        geometry: ScreenGeometry,
    ): WindowManager.LayoutParams {
        val thicknessPx = (configuration.edgeWidthDp * geometry.density).toInt().coerceAtLeast(1)

        // Each strip is extended across its own edge's navigation-bar inset: the bar is
        // slippery, so a swipe starting on it lands on us only after leaving the bar.
        val (widthPx, heightPx, gravity) = when (edge) {
            ScreenEdge.LEFT -> Triple(
                thicknessPx + geometry.navLeftPx,
                (geometry.heightPx * layout.sideLengthPercent / 100).coerceAtLeast(1),
                Gravity.START or Gravity.CENTER_VERTICAL,
            )
            ScreenEdge.RIGHT -> Triple(
                thicknessPx + geometry.navRightPx,
                (geometry.heightPx * layout.sideLengthPercent / 100).coerceAtLeast(1),
                Gravity.END or Gravity.CENTER_VERTICAL,
            )
            ScreenEdge.BOTTOM -> Triple(
                (geometry.widthPx * layout.bottomLengthPercent / 100).coerceAtLeast(1),
                thicknessPx + geometry.navBottomPx,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            )
        }

        return WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            // Without this the strip is pushed inside the inset it was just extended across.
            fitInsetsTypes = 0
        }
    }

    private fun onStreamStart(): Long {
        bellies.values.forEach(BackBellyOverlay::hide)
        val stream = hold.start(SystemClock.uptimeMillis())
        applyInteractivity()
        scheduleStuckCheck()
        return stream
    }

    private fun onActivity(stream: Long) {
        if (!hold.activity(stream, SystemClock.uptimeMillis())) return
        // The finger was only still: the stream is alive after all.
        Log.w(TAG, "stream held again, a touch event followed the stuck release")
        applyInteractivity()
        scheduleStuckCheck()
    }

    private fun onFire(action: GestureAction, edge: ScreenEdge) {
        onGestureAction(action, edge)
    }

    private fun onUnused(stream: Long, samples: List<TouchSample>) {
        if (!hold.isCurrent(stream)) return
        if (!hold.mayReplay(stream)) {
            releaseStream(stream)
            return
        }
        // A replay sends no touch events to the strips, so it counts as activity itself.
        hold.touch(stream, SystemClock.uptimeMillis())
        scheduleStuckCheck()
        val dispatched = replayer.replay(samples) { releaseStream(stream) }
        if (!dispatched) releaseStream(stream)
    }

    private fun releaseStream(stream: Long) {
        if (!hold.release(stream)) return
        handler.removeCallbacks(stuckCheck)
        applyInteractivity()
        Log.d(TAG, "stream released; strips interactive again")
    }

    private fun scheduleStuckCheck() {
        handler.removeCallbacks(stuckCheck)
        hold.deadlineMs?.let { handler.postAtTime(stuckCheck, it) }
    }

    private fun onStuckCheck() {
        if (!hold.expire(SystemClock.uptimeMillis())) {
            scheduleStuckCheck()
            return
        }
        bellies.values.forEach(BackBellyOverlay::hide)
        report("strips were stuck inert, released after ${STUCK_STREAM_MS}ms without a touch")
        applyInteractivity()
    }

    private fun report(message: String) {
        Log.w(TAG, message)
        SetupStore(context).note(message)
    }

    /** See the class comment for why this is alpha 0 as well as non-touchable. */
    private fun applyInteractivity() {
        val interactive = !hold.held
        strips.values.forEach { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return@forEach
            val flags = if (interactive) {
                params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            val alpha = if (interactive) 1f else 0f
            if (flags == params.flags && params.alpha == alpha) return@forEach
            params.flags = flags
            params.alpha = alpha
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    /** Reads coordinates and timestamps only — never window content, text or keystrokes. */
    @SuppressLint("ViewConstructor")
    private class TouchCaptureView(
        context: Context,
        private val edge: ScreenEdge,
        private val tracker: GestureTracker,
        private val hapticsEnabled: Boolean,
        private val hapticStrength: HapticStrength,
        private val belly: BackBellyOverlay?,
        private val onStreamStart: () -> Long,
        private val onActivity: (Long) -> Unit,
        private val onFire: (Long, GestureAction) -> Unit,
        private val onUnused: (Long, List<TouchSample>) -> Unit,
        private val onStreamEnd: (Long) -> Unit,
    ) : View(context) {

        private var stream = 0L

        private val samples = ArrayList<TouchSample>(MAX_SAMPLES)
        private var abandoned = false

        // One gesture, one buzz: an edge that ticked when its swipe armed stays quiet when
        // the gesture fires on release, like the system Back gesture.
        private var tickedThisTouch = false

        private val holdRunnable = Runnable { handle(tracker.onHoldElapsed(), replayable = false) }

        private val longPressRunnable =
            Runnable { handle(tracker.onLongPressElapsed(), replayable = false) }

        init {
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Nothing here to click: a real tap is replayed to the app underneath instead.
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            // rawX/rawY are display coordinates, which is what the replay path expects.
            val sample = TouchSample(event.rawX, event.rawY, event.eventTime)
            if (event.actionMasked != MotionEvent.ACTION_DOWN) onActivity(stream)
            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    samples.clear()
                    abandoned = false
                    tickedThisTouch = false
                    samples += sample
                    Log.d(TAG, "$edge DOWN at ${sample.xPx.toInt()},${sample.yPx.toInt()}")
                    stream = onStreamStart()
                    track(tracker.onDown(sample), sample, replayable = false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (samples.size < MAX_SAMPLES) samples += sample
                    track(tracker.onMove(sample), sample, replayable = false)
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Never a navigation gesture, and replaying a partial pinch would be
                    // worse than dropping it.
                    Log.d(TAG, "$edge ABANDONED second pointer went down")
                    abandoned = true
                    collapseBelly()
                    cancelHold()
                    tracker.onCancel()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    cancelHold()
                    // Every UP must end the stream, or the strips stay inert forever after
                    // the first gesture. Only a started replay defers the release.
                    val releaseDeferred = if (abandoned) {
                        false
                    } else {
                        if (samples.size < MAX_SAMPLES) samples += sample
                        track(tracker.onUp(sample), sample, replayable = true)
                    }
                    samples.clear()
                    if (!releaseDeferred) onStreamEnd(stream)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    // Commonly SystemUI when the touch began on the navigation bar, which
                    // sits above us in Z order.
                    Log.d(TAG, "$edge CANCELLED stream taken over by another window")
                    collapseBelly()
                    cancelHold()
                    tracker.onCancel()
                    samples.clear()
                    onStreamEnd(stream)
                    true
                }
                else -> false
            }
        }

        /**
         * [replayable] only on release: a mid-gesture rejection must not inject anything
         * while the finger is still down. Returns true when the release was deferred.
         */
        private fun handle(action: TrackerAction, replayable: Boolean): Boolean {
            when (action) {
                is TrackerAction.None -> Unit
                is TrackerAction.ScheduleLongPress -> {
                    removeCallbacks(longPressRunnable)
                    postDelayed(longPressRunnable, action.delayMs)
                }
                is TrackerAction.Armed -> {
                    // A swipe is under way, so this touch can no longer be a long press.
                    removeCallbacks(longPressRunnable)
                    Log.d(TAG, "$edge armed (hold=${action.scheduleHoldMs})")
                    // The bottom edge ticks on fire instead of on arming, so a plain
                    // upward swipe does not buzz on the way up.
                    action.scheduleHoldMs?.let { postDelayed(holdRunnable, it) } ?: tick()
                }
                is TrackerAction.RearmHold -> {
                    removeCallbacks(holdRunnable)
                    postDelayed(holdRunnable, action.delayMs)
                }
                is TrackerAction.Fire -> {
                    cancelHold()
                    if (action.action == GestureAction.CircleToSearch) longPressTick() else tick()
                    // action.id, not ::class.simpleName - R8 obfuscates class names.
                    Log.i(TAG, "GESTURE_FIRED $edge ${action.action.id}")
                    onFire(stream, action.action)
                }
                is TrackerAction.Disarmed -> Log.d(TAG, "$edge disarmed, pulled back inside the trigger")
                // Consumed rather than replayed: the app never asked for this swipe.
                is TrackerAction.Reverted -> Log.i(TAG, "GESTURE_REVERTED $edge")
                is TrackerAction.Unused -> {
                    cancelHold()
                    Log.d(TAG, "$edge unused (${action.reason})")
                    if (replayable && samples.isNotEmpty()) {
                        onUnused(stream, samples.toList())
                        return true
                    }
                }
            }
            return false
        }

        // A strip taken off the screen mid-touch never sees its UP; its timers must not fire
        // on a stream that is already over.
        override fun onDetachedFromWindow() {
            cancelHold()
            super.onDetachedFromWindow()
        }

        // Clears both timers: every caller is ending or deciding the touch, and a long press
        // left scheduled past that point would fire on a finished stream.
        private fun cancelHold() {
            removeCallbacks(holdRunnable)
            removeCallbacks(longPressRunnable)
        }

        // The belly moves first. A replay must never land under a visible window.
        private fun track(action: TrackerAction, sample: TouchSample, replayable: Boolean): Boolean {
            belly?.follow(action, samples.firstOrNull()?.yPx ?: sample.yPx, inwardPx(sample))
            return handle(action, replayable)
        }

        private fun collapseBelly() {
            belly?.collapse()
        }

        private fun inwardPx(sample: TouchSample): Float {
            val down = samples.firstOrNull() ?: return 0f
            return if (edge == ScreenEdge.RIGHT) down.xPx - sample.xPx else sample.xPx - down.xPx
        }

                private fun tick() {
            if (!hapticsEnabled || tickedThisTouch) return
            tickedThisTouch = true
            Haptics.play(this, Haptics.tick(hapticStrength))
        }

        /** Mirrors the system long-press buzz, so the hold reads as having landed. */
        private fun longPressTick() {
            if (!hapticsEnabled) return
            Haptics.play(this, Haptics.longPress(hapticStrength))
        }

        private companion object {
            const val MAX_SAMPLES = 400
        }
    }

    private companion object {
        const val TAG = "AnyGest"

        /**
         * Past the longest stretch a live stream goes without a touch event (a replay after its
         * UP), with a second to spare for a busy main thread.
         */
        const val STUCK_STREAM_MS = AnyGestAccessibilityService.LONGEST_REPLAY_HOLD_MS + 1_000L
    }
}
