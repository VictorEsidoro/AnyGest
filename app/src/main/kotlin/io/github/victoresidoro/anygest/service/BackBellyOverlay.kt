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

package io.github.victoresidoro.anygest.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import io.github.victoresidoro.anygest.gesture.BackBelly
import io.github.victoresidoro.anygest.gesture.BackBellyState
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.TrackerAction
import kotlin.math.ceil

/**
 * The back-gesture belly beside one side strip. It shows only once a back swipe arms, since an
 * armed swipe fires or reverts, and neither replays a touch underneath this window.
 */
internal class BackBellyOverlay(
    context: Context,
    windowManager: WindowManager,
    private val edge: ScreenEdge,
    private val geometry: ScreenGeometry,
) {

    private val view = BellyView(context, edge, geometry.density)

    private val window = GhostWindow(
        windowManager,
        view,
        "$edge belly",
        ceil(BackBelly.WIDTH_DP * geometry.density).toInt(),
        geometry.heightPx,
        Gravity.TOP or if (edge == ScreenEdge.LEFT) Gravity.START else Gravity.END,
    )

    private val state = BackBellyState()

    private val shrink = Tween { view.scale = it }

    private val arrowFade = Tween { view.arrowAlpha = it }

    fun install() = window.install()

    fun remove() {
        hide()
        window.remove()
    }

    /** Moves the belly along with one tracker verdict for a finger at [fingerY], [dragPx] in. */
    fun follow(action: TrackerAction, fingerY: Float, dragPx: Float) {
        val wasUp = state.visible
        val hadArrow = state.arrowShown
        state.follow(action, dragPx / geometry.density)
        when {
            action is TrackerAction.Passed -> hide()
            wasUp && !state.visible -> shrinkAway()
            !wasUp && state.visible -> grow(fingerY)
            hadArrow != state.arrowShown -> fadeArrow(state.arrowShown)
        }
        if (state.visible) view.scale = state.scale
    }

    /** The belly folds back the same way whether the swipe fired or was cancelled. */
    fun collapse() {
        if (!state.visible) return
        state.hide()
        shrinkAway()
    }

    /** Immediate, for a new touch: a replay must never land under a visible window. */
    fun hide() {
        state.hide()
        shrink.stop()
        arrowFade.stop()
        view.arrowAlpha = 0f
        view.scale = 0f
        window.light(false)
    }

    private fun grow(fingerY: Float) {
        shrink.stop()
        view.centreY = fingerY - BackBelly.ABOVE_FINGER_DP * geometry.density
        window.light(true)
        fadeArrow(true)
    }

    private fun shrinkAway() {
        fadeArrow(false)
        shrink.play(view.scale, 0f, SHRINK_MS, DecelerateInterpolator()) { window.light(false) }
    }

    private fun fadeArrow(visible: Boolean) {
        val ms = if (visible) ARROW_IN_MS else ARROW_OUT_MS
        arrowFade.play(view.arrowAlpha, if (visible) 1f else 0f, ms, DecelerateInterpolator(ARROW_EASING))
    }

    @SuppressLint("ViewConstructor")
    private class BellyView(
        context: Context,
        private val edge: ScreenEdge,
        private val density: Float,
    ) : View(context) {

        var centreY = 0f

        var scale = 0f
            set(value) {
                field = value
                invalidate()
            }

        var arrowAlpha = 0f
            set(value) {
                field = value
                invalidate()
            }

        private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(BODY_ALPHA, 0, 0, 0) }

        private val chevron = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = BackBelly.ARROW_STROKE_DP * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val outline = Path()
        private val chevronPath = Path()

        override fun onDraw(canvas: Canvas) {
            if (scale <= 0f) return
            val reach = BackBelly.WIDTH_DP * density * scale
            traceOutline(reach)
            canvas.drawPath(outline, body)
            if (scale > BackBelly.ARROW_MINIMUM_SCALE && arrowAlpha > 0f) drawChevron(canvas, reach / 2)
        }

        private fun traceOutline(reach: Float) {
            val span = BackBelly.HEIGHT_DP * density
            val top = centreY - span / 2
            outline.rewind()
            outline.moveTo(inward(0f), top)
            for (row in 0..ROWS) {
                val down = row.toFloat() / ROWS
                outline.lineTo(inward(BackBelly.profile(down) * reach), top + down * span)
            }
            outline.close()
        }

        // The same "<" on both edges, never mirrored.
        private fun drawChevron(canvas: Canvas, depth: Float) {
            val half = (BackBelly.ARROW_WIDTH_DP * density - chevron.strokeWidth) / 2
            val rise = (BackBelly.ARROW_HEIGHT_DP * density - chevron.strokeWidth) / 2
            val x = inward(depth)
            chevronPath.rewind()
            chevronPath.moveTo(x + half, centreY - rise)
            chevronPath.lineTo(x - half, centreY)
            chevronPath.lineTo(x + half, centreY + rise)
            chevron.alpha = (CHEVRON_ALPHA * arrowAlpha).toInt()
            canvas.drawPath(chevronPath, chevron)
        }

        private fun inward(depth: Float): Float = if (edge == ScreenEdge.LEFT) depth else width - depth

        private companion object {
            const val BODY_ALPHA = 204
            const val CHEVRON_ALPHA = 230
            const val ROWS = 64
        }
    }

    private companion object {
        const val SHRINK_MS = 100L
        const val ARROW_IN_MS = 100L
        const val ARROW_OUT_MS = 50L
        const val ARROW_EASING = 1.5f
    }
}
