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

import io.github.victoresidoro.anygest.gesture.TouchSample
import kotlin.math.hypot

/**
 * How a touch that was no gesture is played back into the app underneath: the points of the
 * stroke and how long it lasts. Pure, so the timing rules are tested without a device.
 *
 * A tap is replayed as a 1 px stroke, since the system refuses a stroke of no length, and its
 * length is capped so the click lands soon: the press itself already happened. A press held
 * past the long-press timeout keeps its length, so the app's own long press fires too. A drag
 * is replayed point by point at its own speed.
 */
internal data class ReplayPlan(val points: List<Pair<Float, Float>>, val durationMs: Long, val tap: Boolean) {

    companion object {
        const val MAX_REPLAY_MS = 3_000L

        /** A shorter stroke completes but never clicks. */
        const val MIN_TAP_MS = 50L
        const val TAP_STROKE_CAP_MS = 60L
        const val TAP_SLOP_PX = 12f

        fun of(samples: List<TouchSample>, longPressTimeoutMs: Long): ReplayPlan? {
            val first = samples.firstOrNull() ?: return null
            val last = samples.last()
            val tap = hypot(last.xPx - first.xPx, last.yPx - first.yPx) < TAP_SLOP_PX
            val lengthMs = last.timestampMs - first.timestampMs
            val points = if (tap) {
                listOf(first.xPx to first.yPx, first.xPx + 1f to first.yPx + 1f)
            } else {
                samples.map { it.xPx to it.yPx }
            }
            val durationMs = when {
                !tap -> lengthMs.coerceIn(1L, MAX_REPLAY_MS)
                lengthMs >= longPressTimeoutMs -> lengthMs.coerceAtMost(MAX_REPLAY_MS)
                else -> lengthMs.coerceIn(MIN_TAP_MS, TAP_STROKE_CAP_MS)
            }
            return ReplayPlan(points, durationMs, tap)
        }
    }
}
