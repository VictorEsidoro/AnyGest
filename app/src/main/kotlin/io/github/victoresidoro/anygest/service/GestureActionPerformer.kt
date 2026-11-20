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

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.Trace
import io.github.victoresidoro.anygest.gesture.GestureAction

/**
 * Carries out a gesture's action. HyperOS sometimes answers a global action with false for no
 * visible reason, so each refusal is written down for the diagnostics screen instead of looking
 * like a recognizer bug.
 */
class GestureActionPerformer(private val service: AccessibilityService) {

    private val switcher = AppSwitcher(service)

    fun perform(action: GestureAction): Boolean {
        val startedMs = SystemClock.uptimeMillis()
        val done = dispatch(action)
        Trace.performed(startedMs, action.id, done)
        return done
    }

    private fun dispatch(action: GestureAction): Boolean = when (action) {
        GestureAction.Back -> global(action, AccessibilityService.GLOBAL_ACTION_BACK)
        GestureAction.Home -> global(action, AccessibilityService.GLOBAL_ACTION_HOME)
        GestureAction.Recents -> global(action, AccessibilityService.GLOBAL_ACTION_RECENTS)
        // Android has no global action for these two, so they launch an app instead.
        GestureAction.PreviousApp -> switcher.step(toPrevious = true)
        GestureAction.NextApp -> switcher.step(toPrevious = false)
        GestureAction.CircleToSearch -> circleToSearch()
    }

    // A service that was unbound throws here instead of returning false, and an uncaught throw
    // would take the process down and mark the service as malfunctioning.
    private fun global(action: GestureAction, code: Int): Boolean {
        val done = try {
            service.performGlobalAction(code)
        } catch (t: Throwable) {
            fail(action, "performGlobalAction threw ${t.javaClass.name}", t)
            return false
        }
        // No retry: repeating a global action can navigate twice.
        if (!done) fail(action, "performGlobalAction returned false")
        return done
    }

    /**
     * The usual cause of a refusal, Google not being the default assistant, cannot be seen from
     * a swipe, so it is also shown once on screen and not only under Diagnostics.
     */
    private fun circleToSearch(): Boolean {
        val problem = CircleToSearchLauncher.launch(service) ?: return true
        fail(GestureAction.CircleToSearch, problem)
        runCatching {
            Toast.makeText(service, R.string.toast_circle_to_search_failed, Toast.LENGTH_LONG).show()
        }
        return false
    }

    private fun fail(action: GestureAction, why: String, cause: Throwable? = null) {
        lastFailure = "${action.id}: $why"
        if (cause != null) Log.w(TAG, "${action.id}: $why", cause) else Log.w(TAG, "${action.id}: $why")
    }

    companion object {
        private const val TAG = "AnyGest"

        @Volatile
        var lastFailure: String? = null
            private set
    }
}
