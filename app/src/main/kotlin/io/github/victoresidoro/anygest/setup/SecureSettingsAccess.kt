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

package io.github.victoresidoro.anygest.setup

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import io.github.victoresidoro.anygest.navigation.NavigationModeReader
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService

/**
 * Everything that needs `WRITE_SECURE_SETTINGS`. That permission is signature-level, but
 * `pm grant` over adb can hand it to a normal app once, and it then stays. Without it every
 * call here returns false and the app behaves exactly as it did before "ADB mode".
 */
internal object SecureSettingsAccess {

    private const val PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"

    /**
     * The key that hides the HyperOS button bar. Found on a Poco F8 Ultra: `navigation_mode`
     * stayed at 0 (buttons) while this one at 1 removed the bar.
     */
    private const val FORCE_FSG_NAV_BAR = "force_fsg_nav_bar"
    private const val NAVIGATION_MODE = "navigation_mode"
    private const val GESTURE_MODE = 2
    private const val BUTTON_MODE = 0

    fun canWrite(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun grantCommand(context: Context): String = "pm grant ${context.packageName} $PERMISSION"

    fun barUri(): Uri = Settings.Global.getUriFor(FORCE_FSG_NAV_BAR)

    /** Null when the key was never written, as on a phone that does not use it. */
    fun barHidden(context: Context): Boolean? = runCatching {
        Settings.Global.getInt(context.contentResolver, FORCE_FSG_NAV_BAR) == 1
    }.getOrNull()

    /** HyperOS only: the key means nothing to another vendor, so it is never written there. */
    /** Null when the key cannot be read. */
    fun gestureModeOn(context: Context): Boolean? = runCatching {
        Settings.Secure.getInt(context.contentResolver, NAVIGATION_MODE) == GESTURE_MODE
    }.getOrNull()

    /** Puts gesture navigation back; true when the system kept the value. */
    fun setGestureMode(context: Context): Boolean = runCatching {
        Settings.Secure.putInt(context.contentResolver, NAVIGATION_MODE, GESTURE_MODE) &&
            gestureModeOn(context) == true
    }.getOrDefault(false)

    /**
     * Brings the button bar back for real. With gesture navigation on, showing the bar
     * (clearing the hide key) is not enough: the system keeps the gestures and no buttons
     * appear, which left the phone with neither AnyGest's gestures nor a bar. Switching to
     * the button bar mode as well fixes that; when the gestures start again they put
     * gesture navigation back by themselves. Returns whether the key was written.
     */
    fun showBar(context: Context): Boolean {
        val written = setBarHidden(context, false)
        if (written && gestureModeOn(context) == true) {
            runCatching { Settings.Secure.putInt(context.contentResolver, NAVIGATION_MODE, BUTTON_MODE) }
        }
        return written
    }

    fun setBarHidden(context: Context, hidden: Boolean): Boolean {
        if (!canWrite(context) || !NavigationModeReader.isHyperOsDevice()) return false
        return runCatching {
            Settings.Global.putInt(context.contentResolver, FORCE_FSG_NAV_BAR, if (hidden) 1 else 0)
        }.getOrDefault(false)
    }

    /**
     * Adds or removes AnyGest in the enabled accessibility services, leaving every other
     * entry alone. The system binds or unbinds the service by itself afterwards.
     */
    fun setServiceEnabled(context: Context, enabled: Boolean): Boolean {
        if (!canWrite(context)) return false
        val component =
            ComponentName(context, AnyGestAccessibilityService::class.java).flattenToString()
        val resolver = context.contentResolver
        return runCatching {
            val current =
                Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (enabled && EnabledServicesList.contains(current, component)) {
                // Listed but not running: the process died and Android marked the service
                // as crashed. Writing the same list changes nothing, so the system never
                // binds it again. Take it out and put it back after a short gap.
                Settings.Secure.putString(
                    resolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    EnabledServicesList.without(current, component),
                )
                val app = context.applicationContext
                Handler(Looper.getMainLooper()).postDelayed({ putBack(app, component) }, REBIND_GAP_MS)
                return@runCatching true
            }
            val updated = if (enabled) {
                EnabledServicesList.with(current, component)
            } else {
                EnabledServicesList.without(current, component)
            }
            Settings.Secure.putString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                updated,
            )
            if (enabled) {
                Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            }
            true
        }.getOrDefault(false)
    }

    private const val REBIND_GAP_MS = 500L

    private fun putBack(context: Context, component: String) {
        runCatching {
            val resolver = context.contentResolver
            val current =
                Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            Settings.Secure.putString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                EnabledServicesList.with(current, component),
            )
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        }
    }
}
