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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * The phone-side events the notes need: app installs and updates (kept, not logged one by one,
 * since the Play Store updates dozens at once) and changes of the default apps. Nothing here
 * changes how the gestures work.
 */
internal class DeviceNotes(private val context: Context) {

    private val packageChanges = RecentPackageChanges()
    private var lastDefaults: Set<String> = emptySet()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            val event = when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> PackageEvent.Installed.takeUnless { replacing }
                Intent.ACTION_PACKAGE_REPLACED -> PackageEvent.Updated
                Intent.ACTION_PACKAGE_REMOVED -> PackageEvent.Removed.takeUnless { replacing }
                else -> null
            } ?: return
            val changed = intent.data?.schemeSpecificPart
            packageChanges.add(event, changed, context.packageName, SystemClock.elapsedRealtime())
            // Whether an app has a launcher entry is remembered, and that may have changed.
            changed?.let(AppSwitcher::packageChanged)
        }
    }

    private val defaultsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = defaultsMaybeChanged(BROADCAST)
    }

    fun start() {
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            context,
            defaultsReceiver,
            IntentFilter("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        lastDefaults = DefaultApps.snapshot(context)
    }

    fun stop() {
        runCatching { context.unregisterReceiver(packageReceiver) }
        runCatching { context.unregisterReceiver(defaultsReceiver) }
    }

    /** Recent installs, updates and removals, in words. */
    fun appChanges(): String = packageChanges.describe(SystemClock.elapsedRealtime())

    /**
     * Notes what moved among the default apps. The system's broadcast is always noted; the
     * checks on wake and unlock, which catch a change that sends no broadcast, only when
     * something moved.
     */
    fun defaultsMaybeChanged(what: String = WITHOUT_BROADCAST) {
        val now = DefaultApps.snapshot(context)
        val moved = DefaultApps.changes(lastDefaults, now)
        val first = lastDefaults.isEmpty()
        lastDefaults = now
        if (what != BROADCAST && (moved == "none" || first)) return
        SetupStore(context).note(
            "$what (home launcher: ${LauncherWatch.homeLauncher(context)}; moved: ${if (first) "unknown" else moved}; " +
                "${PowerContext.describe(context)})",
        )
    }

    private companion object {
        const val BROADCAST = "default apps changed"
        const val WITHOUT_BROADCAST = "default apps changed without a broadcast"
    }
}
