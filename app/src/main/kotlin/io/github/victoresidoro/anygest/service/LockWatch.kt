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
 * Decides when the strips may take touches again after the screen wakes on the lock screen.
 *
 * Accessibility overlays draw above the keyguard, where Back, Home and Recents have nothing to
 * act on, and a swipe up to unlock would reach a strip first. The strips therefore stay inert
 * from screen off until the keyguard is gone. The lock state is only read, never trusted
 * forever: if it still says locked after [failOpenMs] the strips are armed anyway, because
 * gestures that stay dead behind a stuck "locked" reading are worse than a gesture on the
 * lock screen.
 */
internal class LockWatch(private val failOpenMs: Long = FAIL_OPEN_MS) {

    /** [armed] says whether the strips may take touches; [nextReadMs] null means stop reading. */
    data class Step(val armed: Boolean, val nextReadMs: Long?)

    private var startedAtMs = 0L

    fun begin(nowMs: Long) {
        startedAtMs = nowMs
    }

    fun read(locked: Boolean, nowMs: Long): Step {
        if (!locked) return Step(armed = true, nextReadMs = null)
        val waitedMs = nowMs - startedAtMs
        if (waitedMs >= failOpenMs) return Step(armed = true, nextReadMs = null)
        return Step(armed = false, nextReadMs = if (waitedMs < FAST_PHASE_MS) FAST_READ_MS else SLOW_READ_MS)
    }

    companion object {
        const val FAST_READ_MS = 300L
        const val SLOW_READ_MS = 1_000L
        const val FAST_PHASE_MS = 20_000L
        const val FAIL_OPEN_MS = 120_000L
    }
}
