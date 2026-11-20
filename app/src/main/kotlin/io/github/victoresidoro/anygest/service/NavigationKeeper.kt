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
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.SystemNavigation
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Watches the system's navigation settings. With ADB mode it keeps the system bar hidden and
 * puts gesture navigation back when HyperOS switches to the button bar by itself (it does with
 * a third-party launcher); without it, it only tells the user the bar came back.
 *
 * Keeping gesture mode is based on HyperGesture commit 81c1ad1 (#127) by Henrique Almeida (h3nc4).
 */
internal class NavigationKeeper(
    private val context: Context,
    handler: Handler,
    /** The navigation inset may be about to change. */
    private val onModeChanging: () -> Unit,
    /** Recent app installs and updates, for the note when the system resets the mode. */
    private val appChanges: () -> String,
) {
    /** False until the service has decided whether to pause at start; nothing is put back before. */
    var active = false

    private var lastMode: NavigationMode? = null

    private val modeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = modeChanged()
    }

    private val barObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (active) keepBarHidden()
        }
    }

    fun start() {
        lastMode = SystemNavigation.currentMode(context)
        context.contentResolver.registerContentObserver(Settings.Secure.getUriFor("navigation_mode"), false, modeObserver)
        context.contentResolver.registerContentObserver(SecureSettingsAccess.barUri(), false, barObserver)
    }

    fun stop() {
        active = false
        runCatching { context.contentResolver.unregisterContentObserver(modeObserver) }
        runCatching { context.contentResolver.unregisterContentObserver(barObserver) }
    }

    /** Hides the system bar and keeps gesture mode, unless paused or the bar is not ours to manage. */
    fun keepBarHidden() {
        val store = SetupStore(context)
        if (store.pausedForApp != null || store.userPaused) return
        if (!GesturePower.barManaged(context)) return
        if (SecureSettingsAccess.barHidden(context) != true) SecureSettingsAccess.setBarHidden(context, true)
        keepGestureMode()
    }

    private fun modeChanged() {
        onModeChanging()
        val current = SystemNavigation.currentMode(context)
        // Put back at once, so there is nothing to tell the user.
        if (active && current != NavigationMode.GESTURAL && keepGestureMode()) {
            lastMode = NavigationMode.GESTURAL
            return
        }
        if (AlertNotifier.navigationBarCameBack(lastMode, current)) {
            AlertNotifier.navigationBarBack(context)
        } else if (current == NavigationMode.GESTURAL) {
            AlertNotifier.navigationBarGone(context)
        }
        lastMode = current
    }

    /** True when gesture navigation was put back just now. */
    private fun keepGestureMode(): Boolean {
        val store = SetupStore(context)
        if (store.pausedForApp != null || store.userPaused) return false
        if (!GesturePower.barManaged(context)) return false
        if (SecureSettingsAccess.gestureModeOn(context) != false) {
            // Already on gestures, put back by the user or the system: nothing of ours is left.
            if (store.buttonsByAnyGest) store.buttonsByAnyGest = false
            return false
        }
        // The button bar AnyGest brought back for a pause is not the system's doing.
        val ours = store.buttonsByAnyGest
        store.buttonsByAnyGest = false
        val restored = SecureSettingsAccess.setGestureMode(context)
        store.note(
            when {
                !restored -> "could not put gesture navigation back"
                ours -> "gesture navigation put back after AnyGest's own button bar"
                else -> "navigation mode was reset by the system, gestures put back " +
                    "(front: ${ForegroundApp.current(context, RESET_LOOKBACK_MS)}, " +
                    "home launcher: ${LauncherWatch.homeLauncher(context)}, app changes: ${appChanges()})"
            },
        )
        return restored
    }

    private companion object {
        /** How far back to look for the app in front when the system resets the navigation. */
        const val RESET_LOOKBACK_MS = 60_000L
    }
}
