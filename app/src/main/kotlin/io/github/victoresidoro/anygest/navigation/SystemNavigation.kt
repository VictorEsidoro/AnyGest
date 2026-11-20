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

package io.github.victoresidoro.anygest.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

enum class NavigationMode { GESTURAL, THREE_BUTTON, UNKNOWN }

/**
 * What the app can know and do about the system's navigation mode. Reading it needs no
 * permission; changing it needs WRITE_SECURE_SETTINGS, which only the ADB mode grants. Without
 * that, the most an app can do on HyperOS is open the screen where the user picks the mode.
 */
object SystemNavigation {

    /** A vendor action, not one of the public Settings.ACTION_* constants. */
    const val MODE_SCREEN_ACTION = "com.android.settings.NAVIGATION_MODE_SETTINGS"

    private const val TAG = "AnyGest"
    private const val MODE_KEY = "navigation_mode"
    private val XIAOMI_BRANDS = setOf("xiaomi", "redmi", "poco")

    fun currentMode(context: Context): NavigationMode {
        val raw = runCatching { Settings.Secure.getInt(context.contentResolver, MODE_KEY) }.getOrNull()
        return when (raw) {
            2 -> NavigationMode.GESTURAL
            0, 1 -> NavigationMode.THREE_BUTTON
            else -> NavigationMode.UNKNOWN
        }
    }

    /** A Xiaomi, Redmi or POCO phone that reports a HyperOS or MIUI version. */
    fun onXiaomiRom(): Boolean =
        Build.MANUFACTURER.lowercase() in XIAOMI_BRANDS && (hyperOsVersion() != null || miuiVersion() != null)

    fun hyperOsVersion(): String? = property("ro.mi.os.version.name")

    /** For builds older than the HyperOS property. */
    fun miuiVersion(): String? = property("ro.miui.ui.version.name")

    /** Opens the navigation-mode screen. It does not pick a mode: that stays with the user. */
    fun openModeScreen(context: Context): Boolean {
        if (!onXiaomiRom()) return false
        val intent = Intent(MODE_SCREEN_ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No navigation-mode screen on this build", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "The navigation-mode screen refused the launch", e)
            false
        }
    }

    // A hidden API: any failure reads as "not there" instead of crashing on other brands.
    private fun property(key: String): String? = runCatching {
        @Suppress("PrivateApi")
        val method = Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
        (method.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
