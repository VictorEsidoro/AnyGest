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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.util.Log
import android.view.ViewConfiguration
import io.github.victoresidoro.anygest.diagnostics.FailureLog
import io.github.victoresidoro.anygest.diagnostics.Trace
import io.github.victoresidoro.anygest.gesture.TouchSample

/**
 * Plays a touch that was no gesture back into the app underneath with `dispatchGesture`.
 *
 * The strips have been inert and transparent since the touch's DOWN, so its own length already
 * counted toward the window update and only what is left of [WINDOW_SETTLE_MS] is waited out
 * before dispatching. Whatever happens, `onDone` is called exactly once, at the latest a grace
 * period after the stroke should have ended, so the strips always come back.
 */
internal class GestureReplayer(
    private val service: AccessibilityService,
    private val handler: Handler,
) : TouchReplayer {

    override fun replay(samples: List<TouchSample>, minSettleMs: Long, onDone: () -> Unit): Boolean {
        val plan = ReplayPlan.of(samples, ViewConfiguration.getLongPressTimeout().toLong()) ?: return false
        val gesture = runCatching {
            val path = Path().apply {
                plan.points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
            }
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, plan.durationMs))
                .build()
        }.getOrElse {
            Log.w(TAG, "Could not build replay gesture", it)
            return false
        }

        val lengthMs = samples.last().timestampMs - samples.first().timestampMs
        // A rested touch had touchable strips until now, so its own length bought no time.
        val settleMs = (WINDOW_SETTLE_MS - lengthMs).coerceAtLeast(minSettleMs)
        var done = false
        val finish = Runnable {
            if (!done) {
                done = true
                onDone()
            }
        }
        handler.postDelayed(finish, plan.durationMs + settleMs + FINISH_GRACE_MS)
        handler.postDelayed({ dispatch(gesture, plan, samples.size, finish) }, settleMs)
        return true
    }

    private fun dispatch(gesture: GestureDescription, plan: ReplayPlan, sampleCount: Int, finish: Runnable) {
        val ended = {
            handler.removeCallbacks(finish)
            finish.run()
        }
        // By now the service may be unbound, and then dispatchGesture throws instead of
        // returning false; uncaught, that would kill the process.
        val dispatched = runCatching {
            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) = ended()

                    // The touch never reached the app, but the strips come back all the same.
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "REPLAY_CANCELLED after ${plan.durationMs}ms")
                        Trace.note("replay cancelled by the system after ${plan.durationMs} ms")
                        ended()
                    }
                },
                handler,
            )
        }.getOrElse {
            FailureLog.record(service, "dispatchGesture", it)
            false
        }
        if (dispatched) {
            Log.d(TAG, "REPLAY_DISPATCHED $sampleCount samples over ${plan.durationMs}ms")
        } else {
            Log.w(TAG, "REPLAY_REFUSED dispatchGesture rejected the replay")
            Trace.note("replay refused by the system")
            ended()
        }
    }

    companion object {
        private const val TAG = "AnyGest"

        /** Time for the inert, transparent strips to reach the input system before a replay. */
        const val WINDOW_SETTLE_MS = 65L

        private const val FINISH_GRACE_MS = 1_000L

        /** The longest a replay keeps the strips held after the finger is gone. */
        const val LONGEST_REPLAY_HOLD_MS = ReplayPlan.MAX_REPLAY_MS + WINDOW_SETTLE_MS + FINISH_GRACE_MS
    }
}
