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

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import io.github.victoresidoro.anygest.diagnostics.Trace
import io.github.victoresidoro.anygest.gesture.ConfirmVerdict
import io.github.victoresidoro.anygest.gesture.EdgeAnimation
import io.github.victoresidoro.anygest.gesture.GestureAction
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.GestureTracker
import io.github.victoresidoro.anygest.gesture.LandscapeConfirm
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.SidePosition
import io.github.victoresidoro.anygest.gesture.TouchSample
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * The three edge strips: their windows, whether they take touches, and what happens to a
 * touch that is no gesture.
 *
 * An accessibility service gets no raw touch events, so each strip is a real window of type
 * `TYPE_ACCESSIBILITY_OVERLAY` (no overlay permission needed). A strip has to take the DOWN
 * to see whether a swipe follows, so a touch that turns out to be no gesture is replayed into
 * the app underneath through [TouchReplayer].
 *
 * During a touch every strip is made non-touchable and fully transparent. It happens at the
 * DOWN because a window change only affects touches that start after it, so the whole touch
 * is time for it to land; transparent too, because a visible overlay counts toward Android's
 * obscuring-opacity limit and a replayed touch under it would be thrown away as untrusted.
 *
 * [onBroken] is called when a strip can no longer be trusted (the system took its window
 * away, or refused to change it) and the strips have to be installed again.
 */
internal class EdgeStrips(
    private val context: Context,
    private val windowManager: WindowManager,
    private val replayer: TouchReplayer,
    private val onGestureAction: (GestureAction, ScreenEdge) -> Unit,
    private val onBroken: (String) -> Unit,
    private val layout: OverlayLayout = OverlayLayout(),
) : StripTouchView.Listener {

    private class Strip(val view: StripTouchView, val params: WindowManager.LayoutParams)

    private val strips = linkedMapOf<ScreenEdge, Strip>()
    private val bellyByEdge = mutableMapOf<ScreenEdge, BackBellyOverlay>()
    private val drops = mutableMapOf<ScreenEdge, EdgeIndicatorOverlay>()

    private val hold = StreamHold(STUCK_STREAM_MS, REPLAY_STUCK_STREAM_MS, RESTING_TOUCH_MS)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val stuckWatch = Runnable(::onStuckCheck)

    // Kept here rather than in a strip, so a pill that is showing survives a reinstall.
    private val confirmer = LandscapeConfirm()
    private var pill: ConfirmPillOverlay? = null
    private var pillUp = false
    private val pillTimeout = Runnable(::onPillTimeout)

    private var installedAtMs = 0L
    private var landscape = false

    /** False from screen off until the lock screen is gone: no touches then. */
    private var armed = true

    /** Whether the windows were last made touchable, and since when they are not. */
    private var touchable = true
    private var inertSinceMs = 0L

    /** The newest event any strip saw, kept only to explain a stuck touch. */
    private var lastTouch = "none"
    private val lateTouches = LateTouchWatch()
    private var lastRestingNoteMs = -RESTING_NOTE_GAP_MS

    /** Strips the window manager refused in the last [install], by cause. */
    var tokenRefusals = 0
        private set
    var otherRefusals = 0
        private set

    /** Returns how many strips the window manager accepted. */
    fun install(configuration: GestureConfiguration, geometry: ScreenGeometry): Int {
        remove()
        tokenRefusals = 0
        otherRefusals = 0
        landscape = geometry.landscape
        confirmer.configure(configuration.landscapeConfirmEnabled, geometry.landscape)
        if (configuration.landscapeConfirmEnabled && geometry.landscape) {
            pill = ConfirmPillOverlay(context, windowManager, geometry).also { it.install() }
        }
        syncPill()

        val edges = listOfNotNull(
            ScreenEdge.LEFT.takeIf { configuration.leftEdgeBackEnabled },
            ScreenEdge.RIGHT.takeIf { configuration.rightEdgeBackEnabled },
            ScreenEdge.BOTTOM,
        )
        for (edge in edges) {
            addFeedback(edge, configuration, geometry)
            addStrip(edge, configuration, geometry)
        }
        touchable = true
        syncTouchable()
        installedAtMs = SystemClock.elapsedRealtime()
        Trace.note("strips installed: ${strips.keys.joinToString { it.name.lowercase() }}")
        return strips.size
    }

    fun remove() {
        mainHandler.removeCallbacks(stuckWatch)
        mainHandler.removeCallbacks(pillTimeout)
        pill?.remove()
        pill = null
        pillUp = false
        // Out of the map first, so their detaching is not taken for the system's doing.
        val gone = strips.values.toList()
        strips.clear()
        gone.forEach { runCatching { windowManager.removeView(it.view) } }
        bellyByEdge.values.forEach(BackBellyOverlay::remove)
        bellyByEdge.clear()
        drops.values.forEach(EdgeIndicatorOverlay::remove)
        drops.clear()
        // A touch in progress dies with its window and its UP never comes to let the strips go.
        hold.reset()
    }

    fun setArmed(value: Boolean, why: String) {
        if (armed == value) return
        armed = value
        Trace.note(if (value) "strips on ($why)" else "strips off ($why)")
        syncTouchable()
    }

    /** True while a touch holds the strips: installing again now would cut it off. */
    fun streamHeld(): Boolean = hold.held

    /** Strips present and attached, without the side effects of [healthy]. */
    fun intact(): Boolean = strips.isNotEmpty() && strips.values.all { it.view.isAttachedToWindow }

    /**
     * Called when the screen comes on, the moment the gestures are about to be used. A touch
     * that outlived its limit while the screen was off is let go. False when the strips have to
     * be installed again.
     */
    fun healthy(): Boolean {
        if (hold.expire(SystemClock.uptimeMillis())) {
            report("strips were stuck inert, released when the screen came on")
            syncTouchable()
        }
        return intact()
    }

    /** For the copied log: each strip's size, the install's age and whether touches get through. */
    fun describe(nowMs: Long = SystemClock.elapsedRealtime()): String {
        if (strips.isEmpty()) return "none attached"
        val sizes = strips.entries.sortedBy { it.key.ordinal }.joinToString(", ") { (edge, strip) ->
            "${edge.name.lowercase()} ${strip.params.width}x${strip.params.height}px"
        }
        val state = when {
            !armed -> ", off over the lock screen"
            !touchable -> ", inert for a touch for ${(SystemClock.uptimeMillis() - inertSinceMs) / 1000}s"
            else -> ""
        }
        val detached = strips.filterValues { !it.view.isAttachedToWindow }.keys
        val missing = if (detached.isEmpty()) "" else ", detached: ${detached.joinToString { it.name.lowercase() }}"
        return "$sizes (installed ${(nowMs - installedAtMs) / 1000}s ago$state$missing)"
    }

    private fun addFeedback(edge: ScreenEdge, configuration: GestureConfiguration, geometry: ScreenGeometry) {
        if (configuration.edgeAnimation != EdgeAnimation.Off) {
            drops[edge] = EdgeIndicatorOverlay(context, windowManager, edge, geometry, configuration.edgeAnimation)
                .also { it.install() }
        } else if (edge != ScreenEdge.BOTTOM) {
            bellyByEdge[edge] = BackBellyOverlay(context, windowManager, edge, geometry).also { it.install() }
        }
    }

    private fun addStrip(edge: ScreenEdge, configuration: GestureConfiguration, geometry: ScreenGeometry) {
        val tracker = GestureTracker(
            edge,
            configuration,
            geometry.density,
            // Display coordinates, like the rawX the tracker gets.
            handleCenterXPx = if (edge == ScreenEdge.BOTTOM) geometry.widthPx / 2f else null,
        )
        val feel = StripFeel(configuration.hapticFeedbackEnabled, configuration.hapticStrength, configuration.hapticConfirmEnabled)
        val view = StripTouchView(context, edge, tracker, feel, bellyByEdge[edge], drops[edge], this)
        val params = windowParams(edge, configuration, geometry)
        runCatching {
            windowManager.addView(view, params)
            strips[edge] = Strip(view, params)
            Log.d(TAG, "strip $edge installed ${params.width}x${params.height} geometry=$geometry")
            // Keeps the system's own Back gesture off these pixels.
            view.post {
                if (view.width > 0 && view.height > 0) view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
            }
        }.onFailure {
            if (ResetTokenWatch.isResetToken(it)) tokenRefusals++ else otherRefusals++
            Log.e(TAG, "Could not add $edge strip", it)
        }
    }

    private fun windowParams(edge: ScreenEdge, configuration: GestureConfiguration, geometry: ScreenGeometry): WindowManager.LayoutParams {
        val size = StripSize.of(edge, configuration, geometry, layout)
        return WindowManager.LayoutParams(
            size.widthPx,
            size.heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (edge) {
                ScreenEdge.LEFT -> Gravity.START or alongGravity(configuration.sidePosition)
                ScreenEdge.RIGHT -> Gravity.END or alongGravity(configuration.sidePosition)
                ScreenEdge.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            // Otherwise the strip is pushed back inside the inset it was made to cover.
            fitInsetsTypes = 0
        }
    }

    private fun alongGravity(position: SidePosition): Int = when (position) {
        SidePosition.Top -> Gravity.TOP
        SidePosition.Center -> Gravity.CENTER_VERTICAL
        SidePosition.Bottom -> Gravity.BOTTOM
    }

    /**
     * Brings every window in line with whether touches should reach the strips. A change the
     * window manager refuses is undone in the strip's own copy of its parameters, so the next
     * call tries again instead of believing it already happened, and the strips are rebuilt.
     */
    private fun syncTouchable() {
        val want = !hold.held && armed
        if (want != touchable) {
            touchable = want
            if (!want) inertSinceMs = SystemClock.uptimeMillis()
        }
        val refused = strips.filter { (_, strip) -> !setTouchable(strip, want) }.keys
        if (refused.isEmpty()) return
        val what = "the system refused to make the ${refused.joinToString { it.name.lowercase() }} strip " +
            if (want) "touchable" else "inert"
        report(what)
        onBroken(what)
    }

    private fun setTouchable(strip: Strip, on: Boolean): Boolean {
        val params = strip.params
        val flags = if (on) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        val alpha = if (on) 1f else 0f
        if (flags == params.flags && alpha == params.alpha) return true
        val oldFlags = params.flags
        val oldAlpha = params.alpha
        params.flags = flags
        params.alpha = alpha
        return runCatching { windowManager.updateViewLayout(strip.view, params) }
            .onFailure {
                params.flags = oldFlags
                params.alpha = oldAlpha
                Log.w(TAG, "updateViewLayout refused", it)
            }
            .isSuccess
    }

    // StripTouchView.Listener

    override fun touchStarted(edge: ScreenEdge): Long {
        bellyByEdge.values.forEach(BackBellyOverlay::hide)
        drops.values.forEach(EdgeIndicatorOverlay::hide)
        confirmer.onDown(edge, SystemClock.uptimeMillis())
        syncPill()
        val token = hold.start(SystemClock.uptimeMillis())
        syncTouchable()
        scheduleStuckCheck()
        return token
    }

    override fun touchActive(token: Long) {
        if (!hold.activity(token, SystemClock.uptimeMillis())) return
        // The finger was only still: the touch is alive after all.
        Log.w(TAG, "stream held again, a touch event followed the stuck release")
        syncTouchable()
        scheduleStuckCheck()
    }

    override fun touchEngaged(token: Long) = hold.engage(token)

    override fun longPressPending(token: Long, waitMs: Long) = hold.awaitLongPress(token, waitMs)

    override fun admit(action: GestureAction, edge: ScreenEdge): Boolean {
        val verdict = confirmer.onFire(action, edge, SystemClock.uptimeMillis())
        syncPill()
        if (verdict is ConfirmVerdict.ShowPill) Log.i(TAG, "GESTURE_CONFIRM_SHOWN $edge ${action.id}")
        return verdict is ConfirmVerdict.Perform
    }

    override fun fire(token: Long, action: GestureAction, edge: ScreenEdge) {
        if (hold.isCurrent(token)) {
            onGestureAction(action, edge)
        } else {
            Trace.note("$edge ${action.id} dropped: the strips were rebuilt during the touch")
        }
    }

    override fun replay(token: Long, edge: ScreenEdge, samples: List<TouchSample>) {
        if (!hold.isCurrent(token)) return
        confirmer.onReplayed(edge)
        syncPill(immediate = true)
        // A replay sends the strips no events, so starting one counts as activity.
        val settleMs = hold.resumeForReplay(token, SystemClock.uptimeMillis(), GestureReplayer.WINDOW_SETTLE_MS, MAX_RESTED_REPLAY_MS)
        if (settleMs == null) {
            letGo(token)
            return
        }
        // A rested touch was just held again, so the strips turn inert now.
        if (settleMs > 0L) syncTouchable()
        scheduleStuckCheck()
        if (!replayer.replay(samples, settleMs) { letGo(token) }) letGo(token)
    }

    override fun touchEnded(token: Long, edge: ScreenEdge) {
        confirmer.onStreamEnd(edge, SystemClock.uptimeMillis())
        syncPill()
        letGo(token)
    }

    override fun lastEvent(description: String) {
        lastTouch = description
    }

    override fun delivered(eventTimeMs: Long) {
        val now = SystemClock.uptimeMillis()
        lateTouches.check(now - eventTimeMs, now)?.let(::report)
    }

    /**
     * A removed window detaches later, on its own, so only the view still installed for its
     * edge counts: that one going away was not our doing.
     */
    override fun detached(view: StripTouchView, edge: ScreenEdge) {
        if (strips[edge]?.view !== view) return
        val what = "the system took the ${edge.name.lowercase()} strip off the screen"
        report(what)
        onBroken(what)
    }

    private fun letGo(token: Long) {
        if (!hold.release(token)) return
        mainHandler.removeCallbacks(stuckWatch)
        syncTouchable()
    }

    private fun scheduleStuckCheck() {
        mainHandler.removeCallbacks(stuckWatch)
        hold.deadlineMs?.let { mainHandler.postAtTime(stuckWatch, it) }
    }

    private fun onStuckCheck() {
        val now = SystemClock.uptimeMillis()
        val quietMs = hold.quietMs(now)
        val replaying = hold.replaying
        if (!hold.expire(now)) {
            scheduleStuckCheck()
            return
        }
        if (hold.rested) {
            // A touch that never armed showed nothing, and nothing is wrong.
            val longPress = hold.longPressWaitMs?.let { " (long press waited ${it}ms, nothing fired)" } ?: ""
            Trace.note("resting touch, strips given back after ${quietMs}ms")
            if (now - lastRestingNoteMs >= RESTING_NOTE_GAP_MS) {
                lastRestingNoteMs = now
                SetupStore(context).note("resting touch, strips given back after ${quietMs}ms$longPress: $lastTouch")
            }
            syncTouchable()
            return
        }
        bellyByEdge.values.forEach(BackBellyOverlay::hide)
        drops.values.forEach(EdgeIndicatorOverlay::hide)
        report("strips were stuck inert, released after ${quietMs}ms without a touch (replay: $replaying)")
        // What the strip saw last and what was in front: a lost UP or a touch taken over.
        report("stuck: last touch $lastTouch; landscape=$landscape; front=${ForegroundApp.current(context, FOREGROUND_LOOKBACK_MS)}")
        syncTouchable()
    }

    private fun onPillTimeout() {
        confirmer.expire(SystemClock.uptimeMillis())
        if (!confirmer.pillShown) Log.i(TAG, "GESTURE_CONFIRM_EXPIRED")
        syncPill()
    }

    /** Brings the pill window and its timer in line with [confirmer]. */
    private fun syncPill(immediate: Boolean = false) {
        val expiresAt = confirmer.expiresAtMs
        val overlay = pill
        if (expiresAt != null && overlay != null) {
            if (!pillUp) overlay.show()
            pillUp = true
            mainHandler.removeCallbacks(pillTimeout)
            mainHandler.postAtTime(pillTimeout, expiresAt)
            return
        }
        mainHandler.removeCallbacks(pillTimeout)
        if (!pillUp) return
        pillUp = false
        if (immediate) overlay?.hide() else overlay?.fadeOut()
    }

    private fun report(message: String) {
        Log.w(TAG, message)
        Trace.note(message)
        SetupStore(context).note(message)
    }

    private companion object {
        const val TAG = "AnyGest"

        /**
         * How long an armed touch may send nothing before the strips are given back. A finger
         * holding still sends events often enough, so a touch silent this long lost its UP.
         */
        const val STUCK_STREAM_MS = 2_000L

        /** A touch that never armed and sends nothing this long is a resting finger or palm. */
        const val RESTING_TOUCH_MS = 500L

        /** A resting touch released within this long was a press for the app, and is replayed. */
        const val MAX_RESTED_REPLAY_MS = 2_000L

        /** At most one note about resting touches per this long; they can be frequent. */
        const val RESTING_NOTE_GAP_MS = 5 * 60_000L

        const val FOREGROUND_LOOKBACK_MS = 30_000L

        /** A replay sends no events and may last this long. */
        const val REPLAY_STUCK_STREAM_MS = GestureReplayer.LONGEST_REPLAY_HOLD_MS + 1_000L
    }
}
