// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp

/**
 * The bulge HyperOS grows out of the edge during a back swipe. The sizes are Xiaomi's 84 by 858
 * px xxhdpi asset at 3 px per dp, measured by Henrique Almeida (h3nc4) for HyperGesture.
 */
object BackBelly {
    const val WIDTH_DP = 28f
    const val HEIGHT_DP = 286f

    /** The belly is centred a little above the finger, as on HyperOS. */
    const val ABOVE_FINGER_DP = 6.7f

    /** The drag after which the belly is full grown. */
    const val FULL_DRAG_DP = 120f

    /** Below this scale the belly is too thin for its arrow. */
    const val ARROW_MINIMUM_SCALE = 0.1f

    const val ARROW_WIDTH_DP = 6.7f
    const val ARROW_HEIGHT_DP = 13f
    const val ARROW_STROKE_DP = 2.1f

    private const val SPREAD = 0.16f
    private const val MIDDLE = 0.5f

    // What the bell curve is worth at the top and bottom rows, taken off so those close to a point.
    private val floor = bell(MIDDLE)

    /** How grown the belly is, 0 to 1, after [dragDp] of travel in from the edge. */
    fun scale(dragDp: Float): Float {
        val travelled = dragDp.coerceIn(0f, FULL_DRAG_DP) / FULL_DRAG_DP
        return 0.5f - 0.5f * cos(PI.toFloat() * travelled)
    }

    /** How wide the belly is [down] of the way from its top row to its bottom, 0 to 1. */
    fun profile(down: Float): Float {
        val raw = bell(down.coerceIn(0f, 1f) - MIDDLE)
        return ((raw - floor) / (1f - floor)).coerceIn(0f, 1f)
    }

    private fun bell(offset: Float): Float = exp(-0.5f * (offset / SPREAD) * (offset / SPREAD))
}

/** What the belly shows for one swipe, kept off Android so the tests can walk it. */
class BackBellyState {

    // Null while no belly is up, otherwise how far in the finger is.
    private var pulledDp: Float? = null

    var arrowShown = false
        private set

    val visible: Boolean get() = pulledDp != null

    val scale: Float get() = pulledDp?.let(BackBelly::scale) ?: 0f

    /** Applies one tracker verdict, so the overlay and the tests share one reading of it. */
    fun follow(action: TrackerAction, dragDp: Float) {
        when (action) {
            is TrackerAction.Armed -> {
                pulledDp = dragDp
                arrowShown = true
            }
            is TrackerAction.Disarmed -> {
                // Pulled back inside the trigger: the arrow goes and the belly keeps following.
                arrowShown = false
                trail(dragDp)
            }
            is TrackerAction.Fire, is TrackerAction.Reverted, is TrackerAction.Unused -> hide()
            else -> trail(dragDp)
        }
    }

    fun hide() {
        pulledDp = null
        arrowShown = false
    }

    private fun trail(dragDp: Float) {
        if (visible) pulledDp = dragDp
    }
}
