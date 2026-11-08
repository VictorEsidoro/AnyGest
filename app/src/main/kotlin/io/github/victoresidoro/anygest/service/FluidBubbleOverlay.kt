// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of HyperGesture.
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
import android.graphics.PixelFormat
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.github.victoresidoro.anygest.gesture.BubbleSpring
import io.github.victoresidoro.anygest.gesture.FluidBubble
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.TrackerAction
import kotlin.math.ceil

/**
 * The bubble that rises from an edge under the finger, in the manner of Fluid Navigation
 * Gestures: from the bottom it carries the Home icon; from a side it carries the Back arrow.
 * It follows the finger along the edge, grows as the finger travels in, and springs back into
 * the edge on release while the action fires alongside.
 *
 * Ported from NavGesture by vijayyaso, a fork of HyperGesture.
 *
 * It never touches the app underneath. The window is never touchable, and goes invisible
 * at once before any replay.
 */
internal class FluidBubbleOverlay(
    context: Context,
    private val windowManager: WindowManager,
    private val edge: ScreenEdge,
    private val geometry: ScreenGeometry,
) {

    private val view = BubbleView(context, edge, geometry.density)

    private val depthPx: Int = ceil((FluidBubble.MAX_RISE_DP + MARGIN_DP) * geometry.density).toInt()

    private var attached = false

    // Whether this touch's bubble is up, so a re-arm after pulling back does not restart it.
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
        }.onFailure { Log.e(TAG, "Could not add the $edge bubble", it) }
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

    /** How far up the bubble stands for a finger at [x],[y], from its distance to the edge. */
    private fun risePx(x: Float, y: Float): Float {
        val distancePx = when (edge) {
            ScreenEdge.BOTTOM -> geometry.heightPx - y
            ScreenEdge.LEFT -> x
            ScreenEdge.RIGHT -> geometry.widthPx - x
        }
        return FluidBubble.riseDp(distancePx / geometry.density) * geometry.density
    }

    private fun setWindowVisible(visible: Boolean) {
        if (!attached) return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val alpha = if (visible) 1f else 0f
        if (params.alpha == alpha) return
        params.alpha = alpha
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    @SuppressLint("ViewConstructor")
    private class BubbleView(
        context: Context,
        private val edge: ScreenEdge,
        private val density: Float,
    ) : View(context) {

        private val overhangPx = density

        // The tip follows the finger; the base drags behind it, so a sliding drop leans.
        private val along = BubbleSpring()
        private val baseAlong = BubbleSpring(BubbleSpring.BASE_TENSION, BubbleSpring.BASE_FRICTION)
        private val rise = BubbleSpring(BubbleSpring.TENSION, BubbleSpring.RISE_FRICTION)
        private val iconAlpha = BubbleSpring(BubbleSpring.ICON_TENSION, BubbleSpring.ICON_FRICTION)

        private var running = false
        private var lastFrameMs = 0L
        private var onSettled: (() -> Unit)? = null

        private val shape = Path()
        private val head = Path()
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FILL }
        private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ICON_STROKE_DP * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        private val iconPath = Path()

        fun start(at: Float) {
            onSettled = null
            along.snap(at)
            baseAlong.snap(at)
            rise.snap(0f)
            iconAlpha.snap(0f)
            iconAlpha.target = 1f
            runSprings()
        }

        fun moveTo(at: Float, risePx: Float) {
            if (onSettled != null) return
            along.target = at
            baseAlong.target = at
            rise.target = risePx
            runSprings()
        }

        fun release(whenSettled: () -> Unit) {
            rise.target = 0f
            iconAlpha.target = 0f
            onSettled = whenSettled
            runSprings()
        }

        fun reset() {
            onSettled = null
            running = false
            rise.snap(0f)
            iconAlpha.snap(0f)
            invalidate()
        }

        private fun runSprings() {
            if (running) return
            running = true
            lastFrameMs = SystemClock.uptimeMillis()
            postOnAnimation(::frame)
        }

        private fun frame() {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            val dt = now - lastFrameMs
            lastFrameMs = now
            along.step(dt)
            baseAlong.step(dt)
            rise.step(dt)
            iconAlpha.step(dt)
            invalidate()
            val done = along.settled && baseAlong.settled && rise.settled && iconAlpha.settled
            when {
                done && onSettled != null -> {
                    running = false
                    onSettled?.invoke()
                    onSettled = null
                }
                // A held bubble keeps ticking only while it still moves.
                done -> running = false
                else -> postOnAnimation(::frame)
            }
        }

        override fun onDraw(canvas: Canvas) {
            val r = rise.value.coerceAtLeast(0f)
            if (r < MIN_DRAWN_PX) return
            val base = baseAlong.value
            val drop = FluidBubble.shape(r, FluidBubble.lean(along.value, base, density), density, overhangPx)

            // Local (x along, y into the edge) to view coordinates.
            fun vx(lx: Float, ly: Float) = x(base + lx, -ly)
            fun vy(lx: Float, ly: Float) = y(base + lx, -ly)

            shape.rewind()
            drop.neck?.let { n ->
                shape.moveTo(vx(n[0], n[1]), vy(n[0], n[1]))
                shape.lineTo(vx(n[2], n[3]), vy(n[2], n[3]))
                shape.cubicTo(vx(n[4], n[5]), vy(n[4], n[5]), vx(n[6], n[7]), vy(n[6], n[7]), vx(n[8], n[9]), vy(n[8], n[9]))
                shape.lineTo(vx(n[10], n[11]), vy(n[10], n[11]))
                shape.lineTo(vx(n[12], n[13]), vy(n[12], n[13]))
                shape.cubicTo(vx(n[14], n[15]), vy(n[14], n[15]), vx(n[16], n[17]), vy(n[16], n[17]), vx(n[18], n[19]), vy(n[18], n[19]))
                shape.lineTo(vx(n[20], n[21]), vy(n[20], n[21]))
                shape.close()
            }
            head.rewind()
            val headX = vx(drop.headX, drop.headY)
            val headY = vy(drop.headX, drop.headY)
            head.addCircle(headX, headY, drop.headRadius, Path.Direction.CW)
            // Filled separately, never merged: Path.op(UNION) came back empty on some frames on
            // the phone, so the drop vanished for a frame mid-hold. Both are solid black, so
            // the overlap shows no seam.
            canvas.drawPath(shape, fill)
            canvas.drawPath(head, fill)
            drawIcon(canvas, headX, headY, drop.headRadius * (r / (2 * drop.headRadius)).coerceIn(0f, 1f))
        }

        // Edge-relative (along, rise) to view x and y.
        private fun x(a: Float, r: Float): Float = when (edge) {
            ScreenEdge.BOTTOM -> a
            ScreenEdge.LEFT -> r
            ScreenEdge.RIGHT -> width - r
        }

        private fun y(a: Float, r: Float): Float = if (edge == ScreenEdge.BOTTOM) height - r else a

        private fun drawIcon(canvas: Canvas, cx: Float, cy: Float, headRadius: Float) {
            val radius = headRadius * ICON_FILL
            val alpha = iconAlpha.value.coerceIn(0f, 1f)
            if (radius < density * 2 || alpha <= 0f) return
            icon.alpha = (alpha * 255).toInt()
            iconPath.rewind()
            when {
                edge != ScreenEdge.BOTTOM -> {
                    // A chevron pointing in from the edge.
                    val dir = if (edge == ScreenEdge.LEFT) 1f else -1f
                    iconPath.moveTo(cx + dir * radius * 0.35f, cy - radius * 0.7f)
                    iconPath.lineTo(cx - dir * radius * 0.35f, cy)
                    iconPath.lineTo(cx + dir * radius * 0.35f, cy + radius * 0.7f)
                }
                else -> {
                    // A house: roof and walls.
                    iconPath.moveTo(cx - radius, cy - radius * 0.05f)
                    iconPath.lineTo(cx, cy - radius)
                    iconPath.lineTo(cx + radius, cy - radius * 0.05f)
                    iconPath.moveTo(cx - radius * 0.7f, cy - radius * 0.3f)
                    iconPath.lineTo(cx - radius * 0.7f, cy + radius * 0.85f)
                    iconPath.lineTo(cx + radius * 0.7f, cy + radius * 0.85f)
                    iconPath.lineTo(cx + radius * 0.7f, cy - radius * 0.3f)
                }
            }
            canvas.drawPath(iconPath, icon)
        }

        private companion object {
            /** Solid black, like FNG's default fill. */
            val FILL: Int = Color.BLACK

            const val ICON_STROKE_DP = 1.8f

            /** The drawn icon fills most, not all, of the head. */
            const val ICON_FILL = 0.6f

            const val MIN_DRAWN_PX = 0.5f
        }
    }

    private companion object {
        const val TAG = "AnyGest"

        /** Room past the tallest drop for its wobble (about 13% over). */
        const val MARGIN_DP = 10f
    }
}
