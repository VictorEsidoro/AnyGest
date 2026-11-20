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

import android.content.Context
import android.view.WindowInsets
import android.view.WindowManager

/**
 * The screen as the strips see it: the full display in pixels and the navigation-bar inset on
 * each side. Taken from the window metrics, which cover the whole display, because the touch
 * coordinates the tracker gets (`rawX`, `rawY`) are display coordinates too.
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
            val bar = metrics.windowInsets.getInsets(WindowInsets.Type.navigationBars())
            return ScreenGeometry(
                widthPx = metrics.bounds.width(),
                heightPx = metrics.bounds.height(),
                density = context.resources.displayMetrics.density,
                navLeftPx = bar.left,
                navRightPx = bar.right,
                navBottomPx = bar.bottom,
            )
        }
    }
}

/**
 * How much of the bottom edge the bottom strip covers. It stops short of the corners so the
 * corners stay with the app; the side strips' length is a setting instead.
 */
internal data class OverlayLayout(
    val bottomLengthPercent: Int = 90,
)
