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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.github.victoresidoro.anygest.gesture.EdgeAnimation
import io.github.victoresidoro.anygest.gesture.EdgeIndicator
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.TrackerAction
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The shape that rises from an edge under the finger while a swipe is under way: from the
 * bottom it carries a Home icon, from a side the Back arrow. It follows the finger along the
 * edge, grows as the finger travels in, and sinks back into the edge on release while the
 * action fires alongside.
 *
 * It never touches the app underneath. The window is never touchable, is invisible (alpha 0)
 * whenever no swipe is under way, and goes invisible at once before any replay, because a
 * visible window above a replayed touch makes the system discard it.
 */
internal class EdgeIndicatorOverlay(
    context: Context,
    private val windowManager: WindowManager,
    private val edge: ScreenEdge,
    private val geometry: ScreenGeometry,
    style: EdgeAnimation,
) {

    private val view = IndicatorView(context, edge, style, geometry.density)

    private val depthPx: Int =
        ceil((EdgeIndicator.MAX_RISE_DP + MARGIN_DP) * geometry.density).toInt()

    private var attached = false

    // Whether this touch's shape is up, so a re-arm after pulling back does not restart it.
    private var shown = false

    fun install() {
        val horizontal = edge == ScreenEdge.BOTTOM
        val params = WindowManager.LayoutParams(
            if (horizontal) WindowManager.LayoutParams.MATCH_PARENT else depthPx,
            if (horizontal) depthPx else geometry.heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (edge) {
                ScreenEdge.BOTTOM -> Gravity.BOTTOM or Gravity.START
                ScreenEdge.LEFT -> Gravity.TOP or Gravity.START
                ScreenEdge.RIGHT -> Gravity.TOP or Gravity.END
            }
            // Full-display coordinates, so a touch's raw position maps straight onto it.
            fitInsetsTypes = 0
            alpha = 0f
        }
        runCatching {
            windowManager.addView(view, params)
            attached = true
        }.onFailure { Log.e(TAG, "Could not add the $edge indicator", it) }
    }

    fun remove() {
        hide()
        if (attached) runCatching { windowManager.removeView(view) }
        attached = false
    }

    /** Applies one tracker verdict, with the finger at raw [x],[y]. */
    fun follow(action: TrackerAction, x: Float, y: Float) {
        when (action) {
            is TrackerAction.Armed -> show(x, y)
            is TrackerAction.Fire, is TrackerAction.Reverted -> release()
            is TrackerAction.Unused -> hide()
            else -> if (shown) view.moveTo(along(x, y), risePx(x, y))
        }
    }

    /** The finger left without a verdict, such as a second pointer or a cancel. */
    fun release() {
        shown = false
        view.release { setWindowVisible(false) }
    }

    /** Immediate: a replay must never land under a visible window. */
    fun hide() {
        shown = false
        view.reset()
        setWindowVisible(false)
    }

    private fun show(x: Float, y: Float) {
        if (!shown) {
            shown = true
            view.start(along(x, y))
            setWindowVisible(true)
        }
        view.moveTo(along(x, y), risePx(x, y))
    }

    private fun along(x: Float, y: Float): Float = if (edge == ScreenEdge.BOTTOM) x else y

    /** How far up the shape stands for a finger at [x],[y], from its distance to the edge. */
    private fun risePx(x: Float, y: Float): Float {
        val distancePx = when (edge) {
            ScreenEdge.BOTTOM -> geometry.heightPx - y
            ScreenEdge.LEFT -> x
            ScreenEdge.RIGHT -> geometry.widthPx - x
        }
        return EdgeIndicator.riseDp(distancePx / geometry.density) * geometry.density
    }

    private fun setWindowVisible(visible: Boolean) {
        if (!attached) return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val alpha = if (visible) 1f else 0f
        if (params.alpha == alpha) return
        params.alpha = alpha
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /**
     * Draws in a local frame where x runs along the edge and y grows away from it into the
     * screen, so one drawing serves all three edges.
     */
    @SuppressLint("ViewConstructor")
    private class IndicatorView(
        context: Context,
        private val edge: ScreenEdge,
        private val style: EdgeAnimation,
        private val density: Float,
    ) : View(context) {

        private var along = 0f
        private var alongTarget = 0f
        private var rise = 0f
        private var riseTarget = 0f
        private var iconAlpha = 0f
        private var iconTarget = 0f

        private var running = false
        private var lastFrameMs = 0L
        private var onSettled: (() -> Unit)? = null

        private val frame = Matrix()
        private val body = Path()
        private val box = RectF()
        private val iconPath = Path()
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(12, 13, 16) }
        private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density
            color = Color.argb(70, 255, 255, 255)
        }
        private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ICON_STROKE_DP * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }

        fun start(at: Float) {
            onSettled = null
            along = at
            alongTarget = at
            rise = 0f
            riseTarget = 0f
            iconAlpha = 0f
            iconTarget = 1f
            runSprings()
        }

        fun moveTo(at: Float, risePx: Float) {
            if (onSettled != null) return
            alongTarget = at
            riseTarget = risePx
            runSprings()
        }

        fun release(whenSettled: () -> Unit) {
            riseTarget = 0f
            iconTarget = 0f
            onSettled = whenSettled
            runSprings()
        }

        fun reset() {
            onSettled = null
            running = false
            rise = 0f
            riseTarget = 0f
            iconAlpha = 0f
            iconTarget = 0f
            invalidate()
        }

        private fun runSprings() {
            if (running) return
            running = true
            lastFrameMs = SystemClock.uptimeMillis()
            postOnAnimation(::step)
        }

        private fun step() {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            val dt = now - lastFrameMs
            lastFrameMs = now
            along = EdgeIndicator.follow(along, alongTarget, dt, ALONG_TAU_MS)
            rise = EdgeIndicator.follow(rise, riseTarget, dt, RISE_TAU_MS)
            iconAlpha = EdgeIndicator.follow(iconAlpha, iconTarget, dt, ICON_TAU_MS)
            invalidate()
            val done = abs(along - alongTarget) < SETTLE_PX &&
                abs(rise - riseTarget) < SETTLE_PX && abs(iconAlpha - iconTarget) < 0.02f
            when {
                done && onSettled != null -> {
                    running = false
                    onSettled?.invoke()
                    onSettled = null
                }
                // A held shape keeps ticking only while it still moves.
                done -> running = false
                else -> postOnAnimation(::step)
            }
        }

        override fun onDraw(canvas: Canvas) {
            val r = rise.coerceAtLeast(0f)
            if (r < MIN_DRAWN_PX) return
            localFrame()
            canvas.save()
            canvas.concat(frame)
            val a = along
            val centreY = drawBody(canvas, a, r)
            if (r > ICON_MIN_RISE_DP * density) drawIcon(canvas, a, centreY)
            canvas.restore()
        }

        /** Local (along, away from the edge) to view coordinates. */
        private fun localFrame() {
            when (edge) {
                ScreenEdge.BOTTOM -> frame.setValues(floatArrayOf(1f, 0f, 0f, 0f, -1f, height.toFloat(), 0f, 0f, 1f))
                ScreenEdge.LEFT -> frame.setValues(floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))
                ScreenEdge.RIGHT -> frame.setValues(floatArrayOf(0f, -1f, width.toFloat(), 1f, 0f, 0f, 0f, 0f, 1f))
            }
        }

        /** Draws the shape with its top [r] away from the edge; returns where its icon sits. */
        private fun drawBody(canvas: Canvas, a: Float, r: Float): Float {
            body.rewind()
            val d = density
            return when (style) {
                EdgeAnimation.Lens -> {
                    val hw = LENS_HALF_WIDTH_DP * d
                    body.moveTo(a - hw, 0f)
                    body.cubicTo(a - hw * 0.35f, 0f, a - hw * 0.42f, r, a, r)
                    body.cubicTo(a + hw * 0.42f, r, a + hw * 0.35f, 0f, a + hw, 0f)
                    body.close()
                    paint(canvas)
                    r * 0.5f
                }
                EdgeAnimation.Capsule -> {
                    val hw = CAPSULE_HALF_WIDTH_DP * d
                    val radius = CAPSULE_RADIUS_DP * d
                    box.set(a - hw, -CAPSULE_TAIL_DP * d, a + hw, r)
                    body.addRoundRect(box, radius, radius, Path.Direction.CW)
                    paint(canvas)
                    r - maxOf(radius, ICON_MIN_RISE_DP * d)
                }
                EdgeAnimation.Bubble -> {
                    val radius = BUBBLE_RADIUS_DP * d
                    // A little taller than wide while it is still coming out of the edge.
                    val stretch = 1f + 0.18f * (1f - minOf(1f, r / (BUBBLE_STRETCH_END_DP * d)))
                    val cy = r - radius * stretch
                    val halfWidth = radius / sqrt(stretch)
                    box.set(a - halfWidth, cy - radius * stretch, a + halfWidth, cy + radius * stretch)
                    body.addOval(box, Path.Direction.CW)
                    paint(canvas)
                    cy
                }
                EdgeAnimation.Card -> {
                    val hw = CARD_HALF_SIDE_DP * d
                    val radius = CARD_RADIUS_DP * d
                    box.set(a - hw, r - CARD_SIDE_DP * d - CARD_TAIL_DP * d, a + hw, r)
                    body.addRoundRect(box, radius, radius, Path.Direction.CW)
                    paint(canvas)
                    r - hw
                }
                EdgeAnimation.Off -> r
            }
        }

        private fun paint(canvas: Canvas) {
            canvas.drawPath(body, fill)
            canvas.drawPath(body, rim)
        }

        private fun drawIcon(canvas: Canvas, cx: Float, cy: Float) {
            val radius = ICON_RADIUS_DP * density
            icon.alpha = (iconAlpha.coerceIn(0f, 1f) * 255).toInt()
            if (icon.alpha == 0) return
            iconPath.rewind()
            if (edge != ScreenEdge.BOTTOM) {
                // A chevron pointing toward the edge it came from: Back.
                iconPath.moveTo(cx - radius * 0.7f, cy + radius * 0.35f)
                iconPath.lineTo(cx, cy - radius * 0.35f)
                iconPath.lineTo(cx + radius * 0.7f, cy + radius * 0.35f)
            } else {
                // A house, roof away from the edge.
                iconPath.moveTo(cx - radius, cy + radius * 0.05f)
                iconPath.lineTo(cx, cy + radius)
                iconPath.lineTo(cx + radius, cy + radius * 0.05f)
                iconPath.moveTo(cx - radius * 0.7f, cy + radius * 0.3f)
                iconPath.lineTo(cx - radius * 0.7f, cy - radius * 0.85f)
                iconPath.lineTo(cx + radius * 0.7f, cy - radius * 0.85f)
                iconPath.lineTo(cx + radius * 0.7f, cy + radius * 0.3f)
            }
            canvas.drawPath(iconPath, icon)
        }

        private companion object {
            const val ALONG_TAU_MS = 40f
            const val RISE_TAU_MS = 55f
            const val ICON_TAU_MS = 70f
            const val SETTLE_PX = 0.3f
            const val MIN_DRAWN_PX = 0.5f
            const val ICON_MIN_RISE_DP = 14f
            const val ICON_RADIUS_DP = 9f
            const val ICON_STROKE_DP = 1.8f
            const val LENS_HALF_WIDTH_DP = 52f
            const val CAPSULE_HALF_WIDTH_DP = 38f
            const val CAPSULE_RADIUS_DP = 26f
            const val CAPSULE_TAIL_DP = 30f
            const val BUBBLE_RADIUS_DP = 24f
            const val BUBBLE_STRETCH_END_DP = 56f
            const val CARD_HALF_SIDE_DP = 26f
            const val CARD_SIDE_DP = 52f
            const val CARD_RADIUS_DP = 18f
            const val CARD_TAIL_DP = 30f
        }
    }

    private companion object {
        const val TAG = "AnyGest"

        /** Room past the tallest shape. */
        const val MARGIN_DP = 10f
    }
}
