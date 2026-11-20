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
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import io.github.victoresidoro.anygest.gesture.ConfirmPill
import kotlin.math.ceil

/**
 * The handle-like pill a held-back landscape swipe shows at the bottom centre, based on
 * HyperGesture (f0f655f) by Henrique Almeida (h3nc4). Its window is only as big as the pill, so
 * it covers as little as it can of a touch replayed below it.
 */
internal class ConfirmPillOverlay(
    private val context: Context,
    windowManager: WindowManager,
    geometry: ScreenGeometry,
) {

    private val view = PillView(context, geometry.density)

    private val window = run {
        val ring = RING_DP * geometry.density
        GhostWindow(
            windowManager,
            view,
            "confirm pill",
            ceil(ConfirmPill.WIDTH_DP * geometry.density + 2 * ring).toInt(),
            ceil(ConfirmPill.HEIGHT_DP * geometry.density + 2 * ring).toInt(),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            (ConfirmPill.ABOVE_EDGE_DP * geometry.density).toInt(),
        )
    }

    private val fade = Tween { view.opacity = it }

    fun install() = window.install()

    fun remove() {
        hide()
        window.remove()
    }

    fun show() {
        view.nightTheme = inNightMode()
        window.light(true)
        fadeTo(1f, FADE_IN_MS)
    }

    fun fadeOut() {
        if (view.opacity == 0f) return
        fadeTo(0f, FADE_OUT_MS) { window.light(false) }
    }

    /** Immediate, for a touch about to be replayed: it must never land under a visible window. */
    fun hide() {
        fade.stop()
        view.opacity = 0f
        window.light(false)
    }

    private fun fadeTo(target: Float, ms: Long, onDone: (() -> Unit)? = null) =
        fade.play(view.opacity, target, ms, DecelerateInterpolator(), onDone)

    // Asked again on each show, since a theme switch does not rebuild the overlay.
    private fun inNightMode(): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    @SuppressLint("ViewConstructor")
    private class PillView(context: Context, density: Float) : View(context) {

        var nightTheme = false

        var opacity = 0f
            set(value) {
                field = value
                invalidate()
            }

        private val ringPx = RING_DP * density
        private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ringPx
        }
        private val area = RectF()

        // Light on a dark theme and dark on a light one, each ringed in the other for any game.
        override fun onDraw(canvas: Canvas) {
            if (opacity <= 0f) return
            val ink = if (nightTheme) Color.WHITE else Color.BLACK
            val ring = if (nightTheme) Color.BLACK else Color.WHITE
            inkPaint.color = faded(ink, INK_ALPHA)
            ringPaint.color = faded(ring, RING_ALPHA)
            area.set(ringPx, ringPx, width - ringPx, height - ringPx)
            val round = area.height() / 2
            canvas.drawRoundRect(area, round, round, ringPaint)
            canvas.drawRoundRect(area, round, round, inkPaint)
        }

        private fun faded(colour: Int, alpha: Int): Int =
            Color.argb((alpha * opacity).toInt(), Color.red(colour), Color.green(colour), Color.blue(colour))
    }

    private companion object {
        const val RING_DP = 1f
        const val INK_ALPHA = 230
        const val RING_ALPHA = 90
        const val FADE_IN_MS = 120L
        const val FADE_OUT_MS = 250L
    }
}
