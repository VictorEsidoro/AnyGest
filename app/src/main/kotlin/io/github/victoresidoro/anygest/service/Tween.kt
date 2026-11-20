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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator

/**
 * One animation at a time for one value. Starting another stops the first without calling its
 * [onDone], so an interrupted fade never runs the step meant for a finished one.
 */
internal class Tween(private val apply: (Float) -> Unit) {

    private var running: ValueAnimator? = null

    fun play(from: Float, to: Float, ms: Long, easing: TimeInterpolator, onDone: (() -> Unit)? = null) {
        stop()
        running = ValueAnimator.ofFloat(from, to).apply {
            duration = ms
            interpolator = easing
            addUpdateListener { apply(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                private var interrupted = false

                override fun onAnimationCancel(animation: Animator) {
                    interrupted = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!interrupted) onDone?.invoke()
                }
            })
            start()
        }
    }

    fun stop() {
        running?.cancel()
        running = null
    }
}
