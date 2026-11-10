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

/**
 * Whether the strips are held for a touch stream, and which stream that is. Every start and
 * every reinstall hands out a new token, and a release with an older token is ignored.
 *
 * Based on HyperGesture commit 209e504 (#124) by Henrique Almeida (h3nc4).
 */
internal class StreamHold(
    private val quietLimitMs: Long,
    private val replayLimitMs: Long = quietLimitMs,
    private val restLimitMs: Long = quietLimitMs,
) {

    /**
     * Set once the touch armed a gesture or may still become a long press. Until then a touch
     * that sends nothing is a finger or palm resting on the edge (a still finger produces no
     * events at all), and it gets only [restLimitMs] before the strips are given back.
     */
    private var engaged = false

    /**
     * The stream was given back while resting. It is not held again by its later events:
     * holding only serves a replay, so a resting grip that twitches must not turn every
     * strip inert again. [resumeForReplay] takes it back if its release is to be replayed.
     */
    var rested = false
        private set

    private var startMs = 0L

    /** Set once a replay runs: it sends the strips no touch events, so it may stay quiet longer. */
    var replaying = false
        private set

    private var token = 0L

    private var lastActivityMs = 0L

    private var reheld = false

    var held = false
        private set

    /** When [expire] would release the stream, or null while nothing is held. */
    val deadlineMs: Long?
        get() {
            if (!held) return null
            val limit = when {
                replaying -> replayLimitMs
                engaged -> quietLimitMs
                else -> restLimitMs
            }
            return lastActivityMs + limit
        }

    fun start(nowMs: Long): Long {
        token++
        held = true
        reheld = false
        replaying = false
        engaged = false
        rested = false
        startMs = nowMs
        lastActivityMs = nowMs
        return token
    }

    /** How long the held stream has gone without a touch event. */
    fun quietMs(nowMs: Long): Long = nowMs - lastActivityMs

    fun isCurrent(streamToken: Long): Boolean = streamToken == token

    /**
     * A touch event of [streamToken]. True when it arrived after [expire] gave the strips back,
     * because the finger was only still, so the caller must hold them again.
     */
    fun activity(streamToken: Long, nowMs: Long): Boolean {
        if (!isCurrent(streamToken)) return false
        lastActivityMs = nowMs
        if (held || rested) return false
        held = true
        reheld = true
        return true
    }

    /** The touch of [streamToken] armed a gesture or is waiting to become a long press. */
    fun engage(streamToken: Long) {
        if (isCurrent(streamToken)) engaged = true
    }

    /**
     * Called when the release of [streamToken] is to be replayed. Returns how long the replay
     * must wait for the strips to turn inert first, or null when it must not run at all.
     *
     * A stream still held waits nothing: its strips have been inert since the touch began. A
     * rested one is held again here and needs the full [settleMs], since its strips were
     * touchable until this moment and the injected touch would land on them; and it is not
     * replayed at all once the touch lasted longer than [maxRestedTouchMs], because that was
     * a grip and not a press meant for the app.
     */
    fun resumeForReplay(streamToken: Long, nowMs: Long, settleMs: Long, maxRestedTouchMs: Long): Long? {
        if (!isCurrent(streamToken) || reheld) return null
        if (held) {
            replaying = true
            lastActivityMs = nowMs
            return 0L
        }
        if (!rested || nowMs - startMs > maxRestedTouchMs) return null
        rested = false
        held = true
        replaying = true
        lastActivityMs = nowMs
        return settleMs
    }

    /** A replay started after the strips were held again could start another replay. */
    fun mayReplay(streamToken: Long): Boolean = isCurrent(streamToken) && !reheld

    /** A replay keeps the stream alive without touch events; it counts as activity. */
    fun touch(streamToken: Long, nowMs: Long) {
        if (!isCurrent(streamToken) || !held) return
        replaying = true
        lastActivityMs = nowMs
    }

    fun release(streamToken: Long): Boolean {
        if (!isCurrent(streamToken) || !held) return false
        held = false
        return true
    }

    /** True when the stream went [quietLimitMs] without a touch event and is now released. */
    fun expire(nowMs: Long): Boolean {
        val deadline = deadlineMs ?: return false
        if (nowMs < deadline) return false
        if (!engaged && !replaying) rested = true
        held = false
        return true
    }

    /** The strips are gone, so whatever stream they held can never complete. */
    fun reset() {
        token++
        held = false
    }
}
