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

package io.github.victoresidoro.anygest.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.SystemNavigation
import io.github.victoresidoro.anygest.service.GesturePower
import io.github.victoresidoro.anygest.service.KeepAliveService
import io.github.victoresidoro.anygest.service.RecoveryJob
import io.github.victoresidoro.anygest.service.TimedPause

/**
 * Android rebinds the accessibility service after a boot by itself. This receiver picks up what
 * does not survive one: a pause that was running, the ADB-mode power state, the content-triggered
 * recovery job and the keep-alive notification. It also notes when HyperOS came back from the
 * boot with its gestures off, which an app cannot fix.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        TimedPause.settleAfterBoot(context)
        GesturePower.restoreIfNeeded(context) // does nothing until ADB mode is set up
        RecoveryJob.schedule(context)
        KeepAliveService.refresh(context)
        noteNavigationAfterBoot(context)
    }

    private fun noteNavigationAfterBoot(context: Context) {
        if (!SystemNavigation.onXiaomiRom()) return
        val mode = SystemNavigation.currentMode(context)
        if (mode != NavigationMode.GESTURAL) {
            Log.i(TAG, "Navigation mode after boot is $mode; the system did not bring gestures back.")
        }
    }

    private companion object {
        const val TAG = "AnyGest"
    }
}
