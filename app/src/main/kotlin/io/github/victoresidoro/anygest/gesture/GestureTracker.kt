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

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Follows one finger on one edge and decides what it means. Pure Kotlin, no Android types,
 * so every rule here is covered by plain JVM tests.
 *
 * The rules, in short:
 * - A swipe counts once it travels its distance inward, inside a cone around the axis,
 *   within [GestureConfiguration.maximumArmDurationMs]. There is no speed requirement:
 *   a slow, careful swipe is as good as a quick one.
 * - Side edges: Back fires on release, and only if the finger is still past the distance,
 *   so bringing it back to the edge cancels.
 * - Bottom edge: Home on release; holding still after arming fires Recents right away,
 *   while the finger is down, so a slow Home swipe is never taken for Recents afterwards.
 *   With app switching on, a swipe along the edge goes to the previous or next app.
 * - Bottom edge, Circle to Search on: a still press over the handle is a long press. A thumb
 *   that creeps a little while pressing still counts as a press, not as a swipe.
 */
class GestureTracker(
    private val edge: ScreenEdge,
    private val configuration: GestureConfiguration,
    density: Float,
    /** Display x of the handle's centre; null accepts the whole edge (tests). */
    private val handleCenterXPx: Float? = null,
) {

    /** Where the finger is in the gesture. Exactly one at a time. */
    private sealed interface Phase {
        /** No finger, or one that is no longer followed. */
        data object Idle : Phase

        /** Down, not armed yet. [longPress] while it may still become a long press. */
        data class Pressing(val down: TouchSample, val longPress: Boolean) : Phase

        /** Armed for the edge's own action; [still] is where the Recents hold is measured from. */
        data class Armed(val down: TouchSample, val still: TouchSample) : Phase

        /** A Back swipe that came back inside its distance; pushing out again re-arms it. */
        data class Pulled(val down: TouchSample) : Phase

        /** A swipe along the bottom edge, which fires [action] on release. */
        data class Switching(val down: TouchSample, val action: GestureAction) : Phase

        /** Something already fired mid-gesture; the release does nothing. */
        data object Done : Phase
    }

    private var phase: Phase = Phase.Idle

    /** The last DOWN, kept after the gesture ends so a late release can still be explained. */
    private var lastDown: TouchSample? = null

    private val bottom = edge == ScreenEdge.BOTTOM
    private val tapAction = if (bottom) GestureAction.Home else GestureAction.Back
    private val holdsForRecents = bottom
    private val longPressEnabled = bottom && configuration.circleToSearchEnabled
    private val switchingEnabled = bottom && configuration.appSwitchEnabled

    private val swipePx = density *
        if (bottom) configuration.bottomMinimumSwipeDistanceDp else configuration.sideMinimumSwipeDistanceDp
    private val stillPx = density * configuration.holdStillnessDp
    private val slopPx = density * configuration.platform.touchSlopDp
    private val handleHalfPx = density / 2f *
        maxOf(configuration.platform.handleWidthDp, PlatformDefaults.MIN_TOUCH_TARGET_DP)

    /** A creeping press may cover this much and still be a press. */
    private val creepPx = maxOf(slopPx, swipePx * CREEP_FACTOR)
    private val cone = configuration.offAxisToleranceRatio

    fun onDown(sample: TouchSample): TrackerAction {
        lastDown = sample
        val longPress = longPressEnabled && onHandle(sample)
        phase = Phase.Pressing(sample, longPress)
        return if (longPress) TrackerAction.ScheduleLongPress(configuration.effectiveCircleToSearchHoldMs) else TrackerAction.None
    }

    fun onMove(sample: TouchSample): TrackerAction = when (val current = phase) {
        is Phase.Pressing -> pressing(current, sample)
        is Phase.Armed -> armed(current, sample)
        is Phase.Pulled -> if (pastDistance(current.down, sample)) arm(current.down, sample) else TrackerAction.None
        Phase.Idle, Phase.Done, is Phase.Switching -> TrackerAction.None
    }

    fun onHoldElapsed(): TrackerAction {
        if (!holdsForRecents || phase !is Phase.Armed) return TrackerAction.None
        phase = Phase.Done
        return TrackerAction.Fire(GestureAction.Recents, edge)
    }

    /** Fires for a press that is still down, has not armed and has not wandered off. */
    fun onLongPressElapsed(): TrackerAction {
        val current = phase as? Phase.Pressing ?: return TrackerAction.None
        if (!current.longPress) return TrackerAction.None
        phase = Phase.Done
        return TrackerAction.Fire(GestureAction.CircleToSearch, edge)
    }

    fun onUp(sample: TouchSample): TrackerAction {
        val ended = phase
        phase = Phase.Idle
        return when (ended) {
            Phase.Done -> TrackerAction.None
            Phase.Idle -> TrackerAction.Unused(rejection(lastDown, sample))
            is Phase.Pressing -> released(ended, sample)
            is Phase.Switching -> TrackerAction.Fire(ended.action, edge)
            is Phase.Armed -> releasedArmed(ended.down, sample)
            is Phase.Pulled -> releasedArmed(ended.down, sample)
        }
    }

    fun onCancel() {
        phase = Phase.Idle
        lastDown = null
    }

    private fun pressing(current: Phase.Pressing, sample: TouchSample): TrackerAction {
        val down = current.down
        // Moving past the slop ends a long press for good, unless it is only a creep.
        val longPress = current.longPress && (!beyondSlop(down, sample) || creeping(down, sample, true))
        if (sample.timestampMs - down.timestampMs > configuration.maximumArmDurationMs) {
            phase = Phase.Idle
            return TrackerAction.Unused(RejectionReason.TOO_SLOW_TO_ARM)
        }
        phase = Phase.Pressing(down, longPress)
        if (creeping(down, sample, longPress)) return TrackerAction.None
        switchTarget(down, sample)?.let { return switchTo(down, it) }
        return if (swipe(down, sample)) arm(down, sample) else TrackerAction.None
    }

    private fun armed(current: Phase.Armed, sample: TouchSample): TrackerAction {
        val down = current.down
        if (!bottom) {
            if (pastDistance(down, sample)) return TrackerAction.None
            phase = Phase.Pulled(down)
            return TrackerAction.Disarmed
        }
        // Arming upward first and then going along the edge is still a switch.
        switchTarget(down, sample)?.let { return switchTo(down, it) }
        if (!moved(current.still, sample)) return TrackerAction.None
        // The hold may happen anywhere along the swipe: movement restarts it.
        phase = Phase.Armed(down, sample)
        return TrackerAction.RearmHold(configuration.recentsHoldMs)
    }

    private fun arm(down: TouchSample, sample: TouchSample): TrackerAction {
        phase = Phase.Armed(down, sample)
        return TrackerAction.Armed(if (holdsForRecents) configuration.recentsHoldMs else null)
    }

    private fun switchTo(down: TouchSample, action: GestureAction): TrackerAction {
        phase = Phase.Switching(down, action)
        return TrackerAction.Armed(scheduleHoldMs = null)
    }

    /** Back is judged where the finger lets go; Home fires wherever it does. */
    private fun releasedArmed(down: TouchSample, sample: TouchSample): TrackerAction =
        if (!bottom && !pastDistance(down, sample)) TrackerAction.Reverted else TrackerAction.Fire(tapAction, edge)

    /**
     * A release that never armed. Under load a whole swipe can arrive as DOWN then UP with no
     * MOVE in between, so the release point alone is judged by the same rules.
     */
    private fun released(current: Phase.Pressing, sample: TouchSample): TrackerAction {
        val down = current.down
        val inTime = sample.timestampMs - down.timestampMs <= configuration.maximumArmDurationMs
        if (inTime && !creeping(down, sample, current.longPress)) {
            switchTarget(down, sample)?.let { return TrackerAction.Fire(it, edge) }
            if (swipe(down, sample)) return TrackerAction.Fire(tapAction, edge)
        }
        return TrackerAction.Unused(rejection(down, sample))
    }

    /** Inward travel: positive away from the edge. */
    private fun travel(from: TouchSample, to: TouchSample): Float = when (edge) {
        ScreenEdge.LEFT -> to.xPx - from.xPx
        ScreenEdge.RIGHT -> from.xPx - to.xPx
        ScreenEdge.BOTTOM -> from.yPx - to.yPx
    }

    /** Movement along the edge, at right angles to [travel]. */
    private fun along(from: TouchSample, to: TouchSample): Float = when (edge) {
        ScreenEdge.LEFT, ScreenEdge.RIGHT -> to.yPx - from.yPx
        ScreenEdge.BOTTOM -> to.xPx - from.xPx
    }

    private fun pastDistance(down: TouchSample, sample: TouchSample) = travel(down, sample) >= swipePx

    private fun swipe(down: TouchSample, sample: TouchSample): Boolean {
        val inward = travel(down, sample)
        return inward >= swipePx && abs(along(down, sample)) <= inward * cone
    }

    /** A swipe along the bottom edge, held to the same cone turned sideways. */
    private fun switchTarget(down: TouchSample, sample: TouchSample): GestureAction? {
        if (!switchingEnabled) return null
        val sideways = along(down, sample)
        if (abs(sideways) < swipePx || abs(travel(down, sample)) > abs(sideways) * cone) return null
        return if (sideways > 0f) GestureAction.PreviousApp else GestureAction.NextApp
    }

    /** Moved outside the Recents stillness box, per axis. */
    private fun moved(from: TouchSample, to: TouchSample) =
        abs(to.xPx - from.xPx) > stillPx || abs(to.yPx - from.yPx) > stillPx

    /** Moved past the platform slop, as a distance, the way the handle's long press is cancelled. */
    private fun beyondSlop(from: TouchSample, to: TouchSample) =
        hypot(to.xPx - from.xPx, to.yPx - from.yPx) > slopPx

    /**
     * A pressing thumb that creeps: slow to start moving and not far. A real swipe covers its
     * first millimetres at once. Without this, a creep of the bottom distance armed Home and
     * then Recents in place of the long press.
     */
    private fun creeping(down: TouchSample, sample: TouchSample, longPress: Boolean): Boolean =
        longPress && sample.timestampMs - down.timestampMs >= QUICK_SWIPE_MS &&
            hypot(sample.xPx - down.xPx, sample.yPx - down.yPx) <= creepPx

    private fun onHandle(sample: TouchSample): Boolean {
        val center = handleCenterXPx ?: return true
        return abs(sample.xPx - center) <= handleHalfPx
    }

    private fun rejection(down: TouchSample?, sample: TouchSample): RejectionReason {
        if (down == null) return RejectionReason.NO_MOVEMENT
        val inward = travel(down, sample)
        val sideways = abs(along(down, sample))
        return when {
            inward == 0f && sideways == 0f -> RejectionReason.NO_MOVEMENT
            inward < 0f -> RejectionReason.WRONG_DIRECTION
            inward < swipePx -> RejectionReason.INSUFFICIENT_MOVEMENT
            sideways > inward * cone -> RejectionReason.OFF_AXIS
            sample.timestampMs - down.timestampMs > configuration.maximumArmDurationMs -> RejectionReason.TOO_SLOW_TO_ARM
            else -> RejectionReason.INSUFFICIENT_MOVEMENT
        }
    }

    private companion object {
        /** A press that only starts moving after this long is creeping, not swiping. */
        const val QUICK_SWIPE_MS = 250L

        /** How many swipe distances a creep may cover. */
        const val CREEP_FACTOR = 3f
    }
}
