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

/**
 * Whether the strips are held inert for a touch, and for which one. Each touch gets a token;
 * an event or release carrying an older token is ignored, so a late release of one touch can
 * never give the strips back in the middle of the next.
 *
 * A held touch is let go on its own when it goes quiet too long, with a limit that depends on
 * what it is: a touch that never armed is a resting finger or palm and gets only the rest
 * limit (plus the long-press wait while that is pending), an armed touch the quiet limit, and
 * a replay, which sends the strips no events at all, the replay limit.
 *
 * Based on HyperGesture commit 209e504 (#124) by Henrique Almeida (h3nc4).
 */
internal class StreamHold(
    private val quietLimitMs: Long,
    private val replayLimitMs: Long = quietLimitMs,
    private val restLimitMs: Long = quietLimitMs,
) {

    /** One touch, from its DOWN until the next touch replaces it. */
    private class Stream(val token: Long, val startMs: Long) {
        var lastEventMs = startMs

        /** Armed a gesture or fired one: no resting touch. */
        var engaged = false

        /** Being replayed into the app underneath. */
        var replaying = false

        /** Let go while resting; its later events do not take the strips again. */
        var rested = false

        /** Taken again after an expiry, because the finger was only still. Never replayed. */
        var heldAgain = false

        /** How long it may wait to become a long press, while it still may. */
        var longPressWaitMs: Long? = null
    }

    private var stream: Stream? = null
    private var lastToken = 0L

    var held = false
        private set

    val longPressWaitMs: Long? get() = stream?.longPressWaitMs
    val rested: Boolean get() = stream?.rested == true
    val replaying: Boolean get() = stream?.replaying == true

    /** When the held touch would be let go, or null when nothing is held. */
    val deadlineMs: Long?
        get() {
            val s = stream?.takeIf { held } ?: return null
            val limit = when {
                s.replaying -> replayLimitMs
                s.engaged -> quietLimitMs
                else -> restLimitMs + (s.longPressWaitMs ?: 0L)
            }
            return s.lastEventMs + limit
        }

    /** A new touch holds the strips. Returns its token. */
    fun start(nowMs: Long): Long {
        val s = Stream(++lastToken, nowMs)
        stream = s
        held = true
        return s.token
    }

    fun isCurrent(token: Long): Boolean = stream?.token == token

    private fun current(token: Long): Stream? = stream?.takeIf { it.token == token }

    fun quietMs(nowMs: Long): Long = stream?.let { nowMs - it.lastEventMs } ?: 0L

    /**
     * An event of the touch [token]. True when it came after an expiry let the strips go,
     * because the finger was only still: the caller must make the strips inert again.
     */
    fun activity(token: Long, nowMs: Long): Boolean {
        val s = current(token) ?: return false
        s.lastEventMs = nowMs
        if (held || s.rested) return false
        held = true
        s.heldAgain = true
        return true
    }

    fun engage(token: Long) {
        current(token)?.engaged = true
    }

    /** The touch is over the handle and fires a long press after [waitMs] if it stays put. */
    fun awaitLongPress(token: Long, waitMs: Long) {
        current(token)?.longPressWaitMs = waitMs
    }

    /**
     * The release of [token] is about to be replayed. Returns how long the replay must wait
     * for the strips to turn inert, or null when it must not run.
     *
     * A touch still held waits nothing: its strips have been inert since its DOWN. A rested
     * one is held again here and waits the whole [settleMs], since its strips were touchable
     * until now; and a rested touch longer than [maxRestedTouchMs] was a grip, not a press for
     * the app, so it is not replayed.
     */
    fun resumeForReplay(token: Long, nowMs: Long, settleMs: Long, maxRestedTouchMs: Long): Long? {
        val s = current(token) ?: return null
        if (s.heldAgain) return null
        if (held) {
            s.replaying = true
            s.lastEventMs = nowMs
            return 0L
        }
        if (!s.rested || nowMs - s.startMs > maxRestedTouchMs) return null
        s.rested = false
        s.replaying = true
        s.lastEventMs = nowMs
        held = true
        return settleMs
    }

    /** False for a touch held again after an expiry: replaying it could replay twice. */
    fun mayReplay(token: Long): Boolean = current(token)?.heldAgain == false

    /** A replay in flight keeps the touch alive without events. */
    fun touch(token: Long, nowMs: Long) {
        val s = current(token) ?: return
        if (!held) return
        s.replaying = true
        s.lastEventMs = nowMs
    }

    fun release(token: Long): Boolean {
        if (current(token) == null || !held) return false
        held = false
        return true
    }

    /** True when the held touch went quiet past its limit and the strips are let go now. */
    fun expire(nowMs: Long): Boolean {
        val deadline = deadlineMs ?: return false
        if (nowMs < deadline) return false
        val s = stream ?: return false
        if (!s.engaged && !s.replaying) s.rested = true
        held = false
        return true
    }

    /** The strips were taken down: no touch they held can finish. */
    fun reset() {
        stream = null
        held = false
    }
}
