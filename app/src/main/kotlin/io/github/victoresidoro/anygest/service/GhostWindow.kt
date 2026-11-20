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

import android.graphics.PixelFormat
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * A window that draws but never takes a touch, which the belly, the pill and the edge shape
 * all live in. It stays invisible (alpha 0) until something is drawn, because a visible window
 * above a replayed touch makes the system discard that touch.
 *
 * Positions are in full-display coordinates, so a touch's raw position maps straight onto it.
 */
internal class GhostWindow(
    private val windowManager: WindowManager,
    val view: View,
    private val label: String,
    private val width: Int,
    private val height: Int,
    private val gravity: Int,
    private val offsetY: Int = 0,
) {

    private var attached = false

    fun install() {
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = gravity
        params.y = offsetY
        params.fitInsetsTypes = 0
        params.alpha = 0f
        runCatching { windowManager.addView(view, params) }
            .onSuccess { attached = true }
            .onFailure { Log.e(TAG, "Could not add the $label window", it) }
    }

    fun remove() {
        if (attached) runCatching { windowManager.removeView(view) }
        attached = false
    }

    /** Shows or hides the whole window; asking for the state it is already in does nothing. */
    fun light(on: Boolean) {
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (!attached) return
        val wanted = if (on) 1f else 0f
        if (params.alpha == wanted) return
        params.alpha = wanted
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private companion object {
        const val TAG = "AnyGest"
    }
}
