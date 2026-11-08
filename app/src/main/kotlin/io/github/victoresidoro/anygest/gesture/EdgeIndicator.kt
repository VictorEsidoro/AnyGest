// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of AnyGest, based on HyperGesture by Henrique Almeida (h3nc4).
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

import kotlin.math.exp
import kotlin.math.tanh

/** The shape that follows the finger when a swipe starts at an edge. Purely visual. */
enum class EdgeAnimation { Off, Lens, LensArrow, Dot, DotDark }

/** The numbers behind the edge indicator, kept free of Android so they can be tested. */
object EdgeIndicator {

    /** The tallest the shape stands from the edge, however far the finger goes. */
    const val MAX_RISE_DP = 56f

    /**
     * How far the shape rises after [travelDp] of finger travel in from the edge: close to one
     * for one at first, easing to a stop at [MAX_RISE_DP] so it never runs off with the finger.
     */
    fun riseDp(travelDp: Float): Float = MAX_RISE_DP * tanh(travelDp.coerceAtLeast(0f) / MAX_RISE_DP)

    /**
     * One step of a value chasing its target without overshoot: the larger [timeConstantMs],
     * the lazier it follows. Frame-rate independent, so 60 and 120 Hz screens move alike.
     */
    fun follow(value: Float, target: Float, dtMs: Long, timeConstantMs: Float): Float {
        val k = 1f - exp(-dtMs.coerceAtLeast(0L) / timeConstantMs)
        return value + (target - value) * k
    }
}
