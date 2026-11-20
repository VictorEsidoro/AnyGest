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

import android.app.KeyguardManager
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Turns the strips off from screen off until the lock screen is gone, and on again after.
 * The lock state is polled through [LockWatch] after each wake, quickly at first, until the
 * keyguard is gone or the wait runs out; a wait that runs out is noted, because the gestures
 * were dead for all of it.
 */
internal class LockGate(
    private val context: Context,
    private val handler: Handler,
    private val setArmed: (Boolean, String) -> Unit,
) {
    private val watch = LockWatch()
    private val check = Runnable { read() }

    /** The service starts: armed when the screen is on and unlocked. */
    fun start(screenOn: Boolean) {
        if (screenOn) screenOn() else setArmed(false, "screen off when the service started")
    }

    fun screenOff() {
        handler.removeCallbacks(check)
        setArmed(false, "screen off")
    }

    fun screenOn() {
        watch.begin(SystemClock.elapsedRealtime())
        read()
    }

    /** The phone was unlocked: no need to wait for the next poll. */
    fun unlocked() = read()

    fun stop() {
        handler.removeCallbacks(check)
    }

    private fun read() {
        handler.removeCallbacks(check)
        val locked = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        val step = watch.read(locked, SystemClock.elapsedRealtime())
        if (step.armed && locked) {
            SetupStore(context).note(
                "lock screen still reported ${LockWatch.FAIL_OPEN_MS / 1000}s after the screen came on " +
                    "(still on it, or a stuck reading); strips on anyway",
            )
        }
        setArmed(step.armed, if (locked) "lock screen still reported, armed anyway" else "unlocked")
        step.nextReadMs?.let { handler.postDelayed(check, it) }
    }
}
