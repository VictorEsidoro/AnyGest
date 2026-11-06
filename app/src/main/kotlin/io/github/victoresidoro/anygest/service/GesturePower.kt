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

import android.content.Context
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.navigation.NavigationModeReader
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Turning the gesture service on and off from code, which only works once the user has run
 * the one-time `pm grant`. Every entry point here is a no-op without it.
 */
internal object GesturePower {

    /** Pausing for an app needs the permission and the usage access that shows when it ends. */
    fun pauseAvailable(context: Context): Boolean =
        SecureSettingsAccess.canWrite(context) && SetupFactsReader.hasUsageAccess(context)

    /** True when this app is allowed to move the button bar, and the user left that on. */
    fun barManaged(context: Context): Boolean =
        SecureSettingsAccess.canWrite(context) &&
            SetupStore(context).autoHideBar &&
            NavigationModeReader.isHyperOsDevice()

    /**
     * Switches the service back on when something took it down and nobody meant it to stay
     * down. Called when the phone boots, the app opens and the tile panel is pulled down:
     * the moments the app gets to run even after the system killed it.
     */
    fun restoreIfNeeded(context: Context) {
        val store = SetupStore(context)
        if (!SecureSettingsAccess.canWrite(context) || !store.autoRestore || store.userPaused) return
        if (DiagnosticsCollector.isAccessibilityServiceEnabled(context)) return
        // A live watcher is still waiting for the user to leave the app, so this is no fault.
        if (store.pausedForApp != null && ExceptionWatchService.running) return
        store.pausedForApp = null
        SecureSettingsAccess.setServiceEnabled(context, true)
    }

    /**
     * The tile's switch. Off shows the button bar first, because with no gestures and no bar
     * the phone cannot be navigated. Returns whether the gestures are now meant to run.
     */
    fun toggleByUser(context: Context): Boolean {
        val store = SetupStore(context)
        return if (DiagnosticsCollector.isAccessibilityServiceEnabled(context)) {
            store.userPaused = true
            if (barManaged(context)) SecureSettingsAccess.setBarHidden(context, false)
            SecureSettingsAccess.setServiceEnabled(context, false)
            false
        } else {
            store.userPaused = false
            store.pausedForApp = null
            ExceptionWatchService.stop(context)
            SecureSettingsAccess.setServiceEnabled(context, true)
            true
        }
    }
}
