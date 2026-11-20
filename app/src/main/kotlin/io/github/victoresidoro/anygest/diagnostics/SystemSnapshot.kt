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

package io.github.victoresidoro.anygest.diagnostics

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService

/**
 * What the system reports about itself and about AnyGest's place in it, for comparing one
 * Android or HyperOS version with the next. Every line is read on its own, so a value the
 * platform refuses becomes "unavailable" instead of losing the rest.
 */
internal object SystemSnapshot {

    fun collect(context: Context): String = buildList {
        add("build" to read { "${Build.FINGERPRINT}, patch ${Build.VERSION.SECURITY_PATCH}, incremental ${Build.VERSION.INCREMENTAL}" })
        add("targetSdk" to read { context.applicationInfo.targetSdkVersion.toString() })
        add("display" to read { display(context) })
        add("refresh rate" to read { refresh(context) })
        add("insets" to read { insets(context) })
        add("service info" to read { serviceInfo(context) })
        add("battery optimization ignored" to read {
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName).toString()
        })
        add("standby bucket" to read {
            context.getSystemService(UsageStatsManager::class.java).appStandbyBucket.toString()
        })
        add("background restricted" to read {
            context.getSystemService(ActivityManager::class.java).isBackgroundRestricted.toString()
        })
        add("memory" to read { memory(context) })
        add("navigation settings" to read { navigationSettings(context) })
    }.joinToString("\n") { (name, value) -> "- $name: $value" }

    private fun read(block: () -> String): String = runCatching(block).getOrElse { "unavailable (${it.javaClass.simpleName})" }

    private fun display(context: Context): String {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
        val bounds = metrics.bounds
        return "${bounds.width()}x${bounds.height()}px, density ${context.resources.displayMetrics.density}, ${display.refreshRate.toInt()} Hz"
    }

    private fun refresh(context: Context): String {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val modes = display.supportedModes.map { "${it.physicalWidth}x${it.physicalHeight}@${it.refreshRate.toInt()}" }.distinct()
        return "now ${display.refreshRate.toInt()} Hz, mode ${display.mode.refreshRate.toInt()} Hz, modes ${modes.joinToString()}"
    }

    private fun insets(context: Context): String {
        val all = context.getSystemService(WindowManager::class.java).currentWindowMetrics.windowInsets
        fun of(type: Int) = all.getInsets(type).let { "${it.left}/${it.top}/${it.right}/${it.bottom}" }
        return "nav ${of(WindowInsets.Type.navigationBars())}, gestures ${of(WindowInsets.Type.systemGestures())}, " +
            "tappable ${of(WindowInsets.Type.tappableElement())} (left/top/right/bottom)"
    }

    private fun serviceInfo(context: Context): String {
        val target = AnyGestAccessibilityService::class.java.name
        val info = context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .firstOrNull { it.resolveInfo?.serviceInfo?.name == target }
            ?: return "not enabled"
        return "flags 0x${info.flags.toString(16)}, events 0x${info.eventTypes.toString(16)}, " +
            "capabilities 0x${info.capabilities.toString(16)}, feedback 0x${info.feedbackType.toString(16)}, " +
            "timeout ${info.notificationTimeout} ms"
    }

    private fun memory(context: Context): String {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        val mb = 1024L * 1024L
        return "app PSS ${Debug.getPss() / 1024} MB, device ${info.availMem / mb}/${info.totalMem / mb} MB free, low ${info.lowMemory}"
    }

    /** Only keys about navigation, gestures and the home screen; values cut short. */
    private fun navigationSettings(context: Context): String {
        val wanted = Regex("nav|gesture|fsg|hide|home|recent|refresh|fps|white_list", RegexOption.IGNORE_CASE)
        val found = sortedSetOf<String>()
        for ((table, uri) in listOf(
            "global" to Settings.Global.CONTENT_URI,
            "secure" to Settings.Secure.CONTENT_URI,
            "system" to Settings.System.CONTENT_URI,
        )) {
            runCatching {
                context.contentResolver.query(uri, arrayOf("name", "value"), null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0) ?: continue
                        if (wanted.containsMatchIn(name)) found += "$table.$name=${cursor.getString(1).orEmpty().take(if (name.contains("white_list", true)) 400 else 40)}"
                    }
                }
            }
        }
        return if (found.isEmpty()) "none readable" else found.take(60).joinToString("; ")
    }
}
