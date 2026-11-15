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
 * Tells a frozen process apart from a frozen system. A tick runs on the main thread every
 * couple of seconds while the screen is on; a tick that comes much later than it should means
 * AnyGest itself was not running, and when no such gap shows, a stall was elsewhere.
 */
internal class StallWatch(private val limitMs: Long = 5_000L) {

    private var lastTickMs: Long? = null

    /** The screen came on: gaps before now do not count. */
    fun start(nowMs: Long) {
        lastTickMs = nowMs
    }

    /** The screen went off: nothing runs on a schedule, so a long gap is expected. */
    fun stop() {
        lastTickMs = null
    }

    /** A note when this tick is [limitMs] or more behind the last one, null otherwise. */
    fun tick(nowMs: Long): String? {
        val last = lastTickMs
        lastTickMs = nowMs
        if (last == null) return null
        val gapMs = nowMs - last
        if (gapMs < limitMs) return null
        return "AnyGest did not run for ${gapMs / 1000}s with the screen on (process frozen or system starved)"
    }
}

/**
 * A touch that reaches a strip long after it happened says the system, or this process, was
 * busy. One note per [quietMs], so a long stall is one line and not a hundred.
 */
internal class LateTouchWatch(
    private val limitMs: Long = 300L,
    private val quietMs: Long = 10_000L,
) {
    private var lastNoteMs: Long? = null

    fun check(delayMs: Long, nowMs: Long): String? {
        if (delayMs < limitMs) return null
        val last = lastNoteMs
        if (last != null && nowMs - last < quietMs) return null
        lastNoteMs = nowMs
        return "touch reached AnyGest ${delayMs}ms after it happened (system or process busy)"
    }
}
