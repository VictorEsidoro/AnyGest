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

import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.ScreenEdge

/**
 * The size of one strip window, in pixels. Each strip also spans the navigation-bar inset on
 * its own edge: a swipe that starts on the system's bar only reaches the strip after leaving
 * it, so the strip has to reach under the bar to catch it in time.
 */
internal data class StripSize(val widthPx: Int, val heightPx: Int) {

    companion object {
        private const val MIN_BOTTOM_DP = 52f

        fun of(
            edge: ScreenEdge,
            configuration: GestureConfiguration,
            geometry: ScreenGeometry,
            layout: OverlayLayout = OverlayLayout(),
        ): StripSize {
            val thickness = (configuration.edgeWidthDp * geometry.density).toInt().coerceAtLeast(1)
            val sideLength = (geometry.heightPx * configuration.sideLengthPercent / 100).coerceAtLeast(1)
            return when (edge) {
                ScreenEdge.LEFT -> StripSize(thickness + geometry.navLeftPx, sideLength)
                ScreenEdge.RIGHT -> StripSize(thickness + geometry.navRightPx, sideLength)
                ScreenEdge.BOTTOM -> StripSize(
                    (geometry.widthPx * layout.bottomLengthPercent / 100).coerceAtLeast(1),
                    // Never under the system's own gesture zone (about 52 dp), even when the inset reads 0.
                    maxOf(thickness + geometry.navBottomPx, (MIN_BOTTOM_DP * geometry.density).toInt()),
                )
            }
        }
    }
}
