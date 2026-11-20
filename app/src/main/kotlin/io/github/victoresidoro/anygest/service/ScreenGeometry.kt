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

package io.github.victoresidoro.anygest.service

import android.content.Context
import android.view.WindowInsets
import android.view.WindowManager

/**
 * The display as the strips need it: its size in pixels and how deep the system's navigation
 * bar is on each side. It comes from the window metrics, which span the whole display, since the
 * raw touch coordinates the tracker receives are display coordinates as well.
 */
internal data class ScreenGeometry(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val navLeftPx: Int,
    val navRightPx: Int,
    val navBottomPx: Int,
) {
    val landscape: Boolean get() = widthPx > heightPx

    companion object {
        fun current(context: Context, windowManager: WindowManager): ScreenGeometry {
            val metrics = windowManager.currentWindowMetrics
            val nav = metrics.windowInsets.getInsets(WindowInsets.Type.navigationBars())
            val bounds = metrics.bounds
            return ScreenGeometry(
                bounds.width(),
                bounds.height(),
                context.resources.displayMetrics.density,
                nav.left,
                nav.right,
                nav.bottom,
            )
        }
    }
}

/**
 * How much of the bottom edge its strip covers. The strip stops short of the corners, which stay
 * with the app; the side strips get their length from a setting.
 */
internal data class OverlayLayout(
    val bottomLengthPercent: Int = 90,
)
