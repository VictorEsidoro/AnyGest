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

    fun canWrite(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun grantCommand(context: Context): String = "pm grant ${context.packageName} $PERMISSION"

    fun barUri(): Uri = Settings.Global.getUriFor(FORCE_FSG_NAV_BAR)

    /** Null when the key was never written, as on a phone that does not use it. */
    fun barHidden(context: Context): Boolean? = runCatching {
        Settings.Global.getInt(context.contentResolver, FORCE_FSG_NAV_BAR) == 1
    }.getOrNull()

    /** HyperOS only: the key means nothing to another vendor, so it is never written there. */
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
}
