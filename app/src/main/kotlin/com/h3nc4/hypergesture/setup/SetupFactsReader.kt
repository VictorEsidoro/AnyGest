// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of HyperGesture.
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

package com.h3nc4.hypergesture.setup

import android.app.AppOpsManager
import android.content.Context
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.edit
import com.h3nc4.hypergesture.diagnostics.DiagnosticsCollector
import com.h3nc4.hypergesture.gesture.GestureConfiguration
import com.h3nc4.hypergesture.navigation.NavigationModeReader

/**
 * What the user confirmed by hand, and whether the Quick Settings tile is in the panel.
 * Plain SharedPreferences: a handful of flags read on the main thread at most once per resume.
 */
internal class SetupStore(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val confirmed: Set<SetupStep>
        get() = SetupStep.entries.filterTo(mutableSetOf()) {
            preferences.getBoolean(KEY_CONFIRMED + it.name, false)
        }

    fun confirm(step: SetupStep) = preferences.edit {
        putBoolean(KEY_CONFIRMED + step.name, true)
    }

    fun unconfirm(step: SetupStep) = preferences.edit { remove(KEY_CONFIRMED + step.name) }

    var tileAdded: Boolean
        get() = preferences.getBoolean(KEY_TILE_ADDED, false)
        set(value) = preferences.edit { putBoolean(KEY_TILE_ADDED, value) }

    private companion object {
        const val FILE = "setup"
        const val KEY_CONFIRMED = "confirmed_"
        const val KEY_TILE_ADDED = "tile_added"
    }
}

internal object SetupFactsReader {

    fun read(context: Context, configuration: GestureConfiguration): SetupFacts {
        val store = SetupStore(context)
        val accessibilityEnabled = DiagnosticsCollector.isAccessibilityServiceEnabled(context)
        // A service that switched on proves restricted settings were allowed, and keeps that
        // step green after HyperOS later stops the service.
        if (accessibilityEnabled) store.confirm(SetupStep.RESTRICTED_SETTINGS)
        return SetupFacts(
            accessibilityEnabled = accessibilityEnabled,
            batteryUnrestricted = isBatteryUnrestricted(context),
            isHyperOs = NavigationModeReader.isHyperOsDevice(),
            appSwitchEnabled = configuration.appSwitchEnabled,
            usageAccessGranted = hasUsageAccess(context),
            circleToSearchEnabled = configuration.circleToSearchEnabled,
            googleIsAssistant = googleIsAssistant(context),
            confirmed = store.confirmed,
        )
    }

    private fun isBatteryUnrestricted(context: Context): Boolean? = runCatching {
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName)
    }.getOrNull()

    /**
     * The app op, not the permission: PACKAGE_USAGE_STATS reads as denied through
     * checkSelfPermission even once the user has allowed it in Settings.
     */
    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = runCatching {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }.getOrNull()
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * `assistant` is a hidden Settings.Secure key, and newer Android versions may refuse to
     * let an app read it. Null then, so the step asks the user to confirm instead of
     * guessing.
     */
    private fun googleIsAssistant(context: Context): Boolean? = runCatching {
        Settings.Secure.getString(context.contentResolver, ASSISTANT_SETTING)
    }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?.startsWith(GOOGLE_APP_PACKAGE + "/")

    private const val ASSISTANT_SETTING = "assistant"
    private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
}
