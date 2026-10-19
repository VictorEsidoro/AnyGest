// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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

package io.github.victoresidoro.anygest.diagnostics

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import io.github.victoresidoro.anygest.navigation.NavigationIntegrationFactory
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.NavigationModeReader
import io.github.victoresidoro.anygest.service.AppSwitcher
import io.github.victoresidoro.anygest.service.GestureActionPerformer
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService

/** Everything here stays on the device: rendered in the UI, never transmitted. */
data class Diagnostics(
    val androidRelease: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val hyperOsVersion: String?,
    val miuiVersion: String?,
    val navigationMode: NavigationMode,
    val accessibilityServiceEnabled: Boolean,
    val accessibilityServiceRunning: Boolean,
    val navigationIntegrationId: String,
    val navigationIntegrationAvailable: Boolean,
    val lastGlobalActionFailure: String?,
    /** Usually the missing usage access, which no global action failure would explain. */
    val lastAppSwitchFailure: String?,
    /** Shown in-app because release builds cannot be read with `adb run-as`. */
    val lastRecordedFailure: String?,
    val appVersionName: String,
)

object DiagnosticsCollector {

    fun collect(context: Context): Diagnostics {
        val integration = NavigationIntegrationFactory.create(context)
        val status = integration.status()
        return Diagnostics(
            androidRelease = Build.VERSION.RELEASE ?: "unknown",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            hyperOsVersion = NavigationModeReader.hyperOsVersion(),
            miuiVersion = NavigationModeReader.miuiVersion(),
            navigationMode = status.currentMode,
            accessibilityServiceEnabled = isAccessibilityServiceEnabled(context),
            accessibilityServiceRunning = AnyGestAccessibilityService.isRunning(),
            navigationIntegrationId = status.integrationId,
            navigationIntegrationAvailable = status.available,
            lastGlobalActionFailure = GestureActionPerformer.lastFailure,
            lastAppSwitchFailure = AppSwitcher.lastFailure,
            lastRecordedFailure = FailureLog.read(context),
            appVersionName = appVersionName(context),
        )
    }

    /** Avoids parsing `ENABLED_ACCESSIBILITY_SERVICES`, whose separator format is undocumented. */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
            ?: return false
        val target = AnyGestAccessibilityService::class.java.name
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo?.serviceInfo?.name == target }
    }

    private fun appVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")
}
