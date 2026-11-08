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
) {

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
        get() = if (held) lastActivityMs + (if (replaying) replayLimitMs else quietLimitMs) else null

    fun start(nowMs: Long): Long {
        token++
        held = true
        reheld = false
        replaying = false
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
        if (held) return false
        held = true
        reheld = true
        return true
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
        held = false
        return true
    }

    /** The strips are gone, so whatever stream they held can never complete. */
    fun reset() {
        token++
        held = false
    }
}
