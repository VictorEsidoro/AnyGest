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

import android.app.AppOpsManager
import android.content.Context
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.navigation.NavigationModeReader

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

    var navigationTileAdded: Boolean
        get() = preferences.getBoolean(KEY_NAVIGATION_TILE_ADDED, false)
        set(value) = preferences.edit { putBoolean(KEY_NAVIGATION_TILE_ADDED, value) }

    /** Opt-in: the alert notifications need the Android 13+ notification permission. */
    var alertsEnabled: Boolean
        get() = preferences.getBoolean(KEY_ALERTS_ENABLED, false)
        set(value) = preferences.edit { putBoolean(KEY_ALERTS_ENABLED, value) }

    /**
     * The app AnyGest stepped aside for, so nothing treats the missing service as a fault.
     * Cleared the moment the gestures come back.
     */
    var pausedForApp: String?
        get() = preferences.getString(KEY_PAUSED_FOR_APP, null)
        set(value) = preferences.edit {
            if (value == null) remove(KEY_PAUSED_FOR_APP) else putString(KEY_PAUSED_FOR_APP, value)
        }

    /** Set by the tile's off switch: the user wants the gestures off, so nothing restores them. */
    var userPaused: Boolean
        get() = preferences.getBoolean(KEY_USER_PAUSED, false)
        set(value) = preferences.edit { putBoolean(KEY_USER_PAUSED, value) }

    /**
     * The last few pause decisions, newest first, shown in Diagnostics: what the service
     * saw and did is otherwise invisible, since the service is off for most of it.
     */
    var pauseNotes: String?
        get() = preferences.getString(KEY_PAUSE_NOTES, null)
        set(value) = preferences.edit {
            if (value == null) remove(KEY_PAUSE_NOTES) else putString(KEY_PAUSE_NOTES, value)
        }

    fun note(text: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val older = pauseNotes?.lines().orEmpty()
        pauseNotes = (listOf("$stamp $text") + older).take(MAX_NOTES).joinToString("\n")
    }

    /**
     * True from the moment the service connects until it is unbound cleanly. Still true at the
     * next connect means the process was killed without a callback.
     */
    var serviceUp: Boolean
        get() = preferences.getBoolean(KEY_SERVICE_UP, false)
        set(value) = preferences.edit { putBoolean(KEY_SERVICE_UP, value) }

    /** Whether AnyGest's own screen shows up in Recents, where a swipe would end the app. */
    /** Keeps the gestures alive with a foreground service; off unless the user asks. */
    var keepAlive: Boolean
        get() = preferences.getBoolean(KEY_KEEP_ALIVE, false)
        set(value) = preferences.edit { putBoolean(KEY_KEEP_ALIVE, value) }

    var showInRecents: Boolean
        get() = preferences.getBoolean(KEY_SHOW_IN_RECENTS, true)
        set(value) = preferences.edit { putBoolean(KEY_SHOW_IN_RECENTS, value) }

    /** ADB mode: keep the button bar hidden while the gestures run. */
    var autoHideBar: Boolean
        get() = preferences.getBoolean(KEY_AUTO_HIDE_BAR, true)
        set(value) = preferences.edit { putBoolean(KEY_AUTO_HIDE_BAR, value) }

    /** ADB mode: switch the service back on at boot, in the app and from the tile. */
    var autoRestore: Boolean
        get() = preferences.getBoolean(KEY_AUTO_RESTORE, true)
        set(value) = preferences.edit { putBoolean(KEY_AUTO_RESTORE, value) }

    private companion object {
        const val FILE = "setup"
        const val KEY_PAUSE_NOTES = "pause_notes"
        const val MAX_NOTES = 20
        const val KEY_PAUSED_FOR_APP = "paused_for_app"
        const val KEY_USER_PAUSED = "user_paused"
        const val KEY_SERVICE_UP = "service_up"
        const val KEY_SHOW_IN_RECENTS = "show_in_recents"
        const val KEY_KEEP_ALIVE = "keep_alive"
        const val KEY_AUTO_HIDE_BAR = "auto_hide_bar"
        const val KEY_AUTO_RESTORE = "auto_restore"
        const val KEY_CONFIRMED = "confirmed_"
        const val KEY_TILE_ADDED = "tile_added"
        const val KEY_NAVIGATION_TILE_ADDED = "navigation_tile_added"
        const val KEY_ALERTS_ENABLED = "alerts_enabled"
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
