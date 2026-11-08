// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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
//
// The metaball geometry below is adapted from Ramotion's fluid-slider-android
// (FluidSlider.drawMetaball), https://github.com/Ramotion/fluid-slider-android,
// under the MIT License:
//
// Copyright (c) 2017 Ramotion
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package io.github.victoresidoro.anygest.gesture

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The gooey neck between two circles: the classic metaball join, as Ramotion's fluid slider
 * draws the bubble rising out of its bar. Android-free, so it is unit tested.
 *
 * Coordinates are any flat frame with y growing *down*; [edgeY] is the line the bottom
 * circle sits under, and the neck's feet are pulled up onto it as the top circle rises.
 */
object Metaball {

    /**
     * The neck outline, or null when the circles are too nested to join. As 22 floats:
     * `moveTo(0,1)`, `lineTo(2,3)`, `cubicTo(4..9)`, `lineTo(10,11)` (top centre),
     * `lineTo(12,13)`, `cubicTo(14..19)`, `lineTo(20,21)`, then close.
     */
    fun neck(
        bottomX: Float, bottomY: Float, bottomRadius: Float,
        topX: Float, topY: Float, topRadius: Float,
        edgeY: Float,
        riseDistance: Float,
        topSpread: Float,
        bottomStartSpread: Float,
        bottomEndSpread: Float,
        handleRate: Float,
        footDrop: Float,
    ): FloatArray? {
        val r1 = bottomRadius
        val r2 = topRadius
        if (r1 <= 0f || r2 <= 0f) return null
        val d = hypot(bottomX - topX, bottomY - topY)
        if (d <= abs(r1 - r2)) return null

        val riseRatio = (max(0f, edgeY - (topY - r2)) / riseDistance).coerceAtMost(1f)

        val u1: Float
        val u2: Float
        if (d < r1 + r2) {
            u1 = acos(((r1 * r1 + d * d - r2 * r2) / (2 * r1 * d)).coerceIn(-1f, 1f))
            u2 = acos(((r2 * r2 + d * d - r1 * r1) / (2 * r2 * d)).coerceIn(-1f, 1f))
        } else {
            u1 = 0f
            u2 = 0f
        }

        val bottomSpread = bottomStartSpread - (bottomStartSpread - bottomEndSpread) * riseRatio
        val pi = PI.toFloat()
        val angle1 = atan2(topY - bottomY, topX - bottomX)
        val angle2 = acos(((r1 - r2) / d).coerceIn(-1f, 1f))
        val angle1a = angle1 + u1 + (angle2 - u1) * bottomSpread
        val angle1b = angle1 - u1 - (angle2 - u1) * bottomSpread
        val angle2a = angle1 + pi - u2 - (pi - u2 - angle2) * topSpread
        val angle2b = angle1 - pi + u2 + (pi - u2 - angle2) * topSpread

        val p1aX = bottomX + cos(angle1a) * r1
        val p1aY = bottomY + sin(angle1a) * r1
        val p1bX = bottomX + cos(angle1b) * r1
        val p1bY = bottomY + sin(angle1b) * r1
        val p2aX = topX + cos(angle2a) * r2
        val p2aY = topY + sin(angle2a) * r2
        val p2bX = topX + cos(angle2b) * r2
        val p2bY = topY + sin(angle2b) * r2

        val handleBase = min(max(topSpread, bottomSpread) * handleRate, hypot(p1aX - p2aX, p1aY - p2aY) / (r1 + r2))
        val handle = handleBase * min(1f, d * 2 / (r1 + r2))
        val h1 = r1 * handle
        val h2 = r2 * handle
        val half = pi / 2

        // The feet are pulled up onto the edge as the top circle rises.
        val lift = abs(edgeY - p1aY) * riseRatio - 1f
        val f1aY = p1aY - lift
        val f1bY = p1bY - lift

        return floatArrayOf(
            p1aX, f1aY + footDrop,
            p1aX, f1aY,
            p1aX + cos(angle1a - half) * h1, f1aY + sin(angle1a - half) * h1,
            p2aX + cos(angle2a + half) * h2, p2aY + sin(angle2a + half) * h2,
            p2aX, p2aY,
            topX, topY,
            p2bX, p2bY,
            p2bX + cos(angle2b - half) * h2, p2bY + sin(angle2b - half) * h2,
            p1bX + cos(angle1b + half) * h1, f1bY + sin(angle1b + half) * h1,
            p1bX, f1bY,
            p1bX, f1bY + footDrop,
        )
    }
}
