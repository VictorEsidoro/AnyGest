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

package io.github.victoresidoro.anygest.diagnostics

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.SystemNavigation
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService
import io.github.victoresidoro.anygest.service.AppSwitcher
import io.github.victoresidoro.anygest.service.GestureActionPerformer
import io.github.victoresidoro.anygest.service.LauncherWatch
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore

/** What the app can tell about the phone and itself. It stays on the device: shown, never sent. */
data class Diagnostics(
    // The phone
    val androidRelease: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val hyperOsVersion: String?,
    val miuiVersion: String?,
    val navigationMode: NavigationMode,
    val navigationIntegrationId: String,
    val navigationIntegrationAvailable: Boolean,
    /** The package that answers Home right now, to compare with what a launcher warns about. */
    val homeLauncher: String = "unknown",
    val homeCandidates: String = "unknown",
    /** The launcher Home opens in place of the system's; null when none is set. */
    val alternateHome: String? = null,
    // The app
    val appVersionName: String,
    val accessibilityServiceEnabled: Boolean,
    val accessibilityServiceRunning: Boolean,
    /** Whether the one-time `pm grant` has been done. */
    val adbMode: Boolean,
    /** The strips as installed: edge, pixel size and age, or why there are none. */
    val strips: String = "unknown",
    // What went wrong lately
    val lastGlobalActionFailure: String?,
    /** Usually the missing usage access, which no global action failure would explain. */
    val lastAppSwitchFailure: String?,
    /** Kept in-app because a release build cannot be read with `adb run-as`. */
    val lastRecordedFailure: String?,
    /** The last pause decisions, newest first; null before any. */
    val pauseNotes: String?,
    /** What the system says about itself, one "- name: value" line each; null if not gathered. */
    val systemSnapshot: String? = null,
    /** The last gestures with their timings, oldest first; null before any. */
    val trace: String? = null,
)

object DiagnosticsCollector {

    fun collect(context: Context): Diagnostics {
        val xiaomi = SystemNavigation.onXiaomiRom()
        return Diagnostics(
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            hyperOsVersion = SystemNavigation.hyperOsVersion(),
            miuiVersion = SystemNavigation.miuiVersion(),
            navigationMode = SystemNavigation.currentMode(context),
            navigationIntegrationId = if (xiaomi) "hyperos" else "generic-android",
            navigationIntegrationAvailable = xiaomi,
            homeLauncher = LauncherWatch.homeLauncher(context),
            homeCandidates = LauncherWatch.homeCandidates(context),
            alternateHome = GestureActionPerformer.inUse,
            appVersionName = versionNameOf(context),
            accessibilityServiceEnabled = isAccessibilityServiceEnabled(context),
            accessibilityServiceRunning = AnyGestAccessibilityService.isRunning(),
            adbMode = SecureSettingsAccess.canWrite(context),
            strips = AnyGestAccessibilityService.stripSummary(),
            lastGlobalActionFailure = GestureActionPerformer.lastFailure,
            lastAppSwitchFailure = AppSwitcher.lastFailure,
            lastRecordedFailure = FailureLog.read(context),
            pauseNotes = SetupStore(context).pauseNotes,
            systemSnapshot = SystemSnapshot.collect(context),
            trace = Trace.report(),
        )
    }

    /** Compares by class name instead of parsing the secure setting, whose separators are undocumented. */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val ours = AnyGestAccessibilityService::class.java.name
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any { it.resolveInfo?.serviceInfo?.name == ours }
    }

    private fun versionNameOf(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "unknown"
}
