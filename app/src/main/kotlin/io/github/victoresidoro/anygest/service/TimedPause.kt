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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * "Pause for 10 minutes": the gestures go off like with the plain pause, and a single inexact
 * alarm (no exact-alarm permission, no wake lock) switches them back on. Switching on by hand
 * first cancels it. Swiping AnyGest away in Recents cancels the alarm with the process, which
 * is why the pause button only appears next to the keep-alive notification.
 */
internal object TimedPause {

    const val DURATION_MS = 10L * 60L * 1000L

    fun start(context: Context) {
        GesturePower.switchOff(context)
        SetupStore(context).timedPauseEndsAt = System.currentTimeMillis() + DURATION_MS
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            alarms.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + DURATION_MS,
                pending(context),
            )
        }
    }

    /** Ends the countdown without switching anything on: the caller does that. */
    fun cancel(context: Context) {
        val store = SetupStore(context)
        if (store.timedPauseEndsAt == 0L) return
        store.timedPauseEndsAt = 0L
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }

    /** The alarm went off: back on, unless the countdown was cancelled in the meantime. */
    fun finish(context: Context) {
        if (SetupStore(context).timedPauseEndsAt == 0L) return
        GesturePower.switchOn(context)
        AlertNotifier.gesturesRunning(context)
    }

    /** An alarm does not survive a reboot, and a pause that outlived one should not stay on. */
    fun settleAfterBoot(context: Context) {
        val store = SetupStore(context)
        if (store.timedPauseEndsAt == 0L) return
        store.timedPauseEndsAt = 0L
        store.userPaused = false
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(AlertActionReceiver.ACTION_RESUME_TIMED).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
