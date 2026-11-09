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

package io.github.victoresidoro.anygest.shortcut

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.service.ExceptionWatchService
import io.github.victoresidoro.anygest.service.GesturePower
import io.github.victoresidoro.anygest.settings.GestureSettingsRepository
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.setup.SetupStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What a [NoGestureShortcut] opens. It has no screen: it switches the gestures off the same way
 * a pause for an exception app does (watcher first, then the button bar, then the service),
 * waits a moment for Android to unbind the service, opens the app and goes away. The watcher
 * then switches the gestures back on when the user leaves the app, as for any pause.
 */
class OpenWithoutGesturesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent?.getStringExtra(NoGestureShortcut.EXTRA_PACKAGE)
        val launch = packageName?.let { packageManager.getLaunchIntentForPackage(it) }
        if (packageName == null || launch == null) {
            Toast.makeText(this, R.string.shortcut_app_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val apps = GestureSettingsRepository(applicationContext).read().pauseApps
            // The watcher's first look comes a second after it starts, and an app other
            // than the paused one in front would end the pause: the app must be up by then.
            if (stepAside(packageName, apps)) delay(UNBIND_SETTLE_MS)
            runCatching { startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            finish()
        }
    }

    /** True when the gestures were switched off here, so the app must wait for the unbind. */
    private fun stepAside(packageName: String, apps: Set<String>): Boolean {
        val store = SetupStore(this)
        if (!SecureSettingsAccess.canWrite(this) || !SetupFactsReader.hasUsageAccess(this)) {
            store.note("shortcut: no ADB mode or usage access, opening $packageName as it is")
            Toast.makeText(this, R.string.shortcut_needs_adb, Toast.LENGTH_LONG).show()
            return false
        }
        // The watcher only waits while an app on the list is in front.
        if (packageName !in apps) {
            store.note("shortcut: $packageName is not on the exception list, opening it as it is")
            Toast.makeText(this, R.string.shortcut_not_listed, Toast.LENGTH_LONG).show()
            return false
        }
        if (!DiagnosticsCollector.isAccessibilityServiceEnabled(this)) {
            store.note("shortcut: gestures already off, opening $packageName")
            return false
        }
        store.pausedForApp = packageName
        store.note("shortcut: switching the gestures off before opening $packageName")
        if (!ExceptionWatchService.start(this)) {
            store.pausedForApp = null
            store.note("shortcut: the system refused the watcher, opening $packageName as it is")
            return false
        }
        if (GesturePower.barManaged(this)) SecureSettingsAccess.showBar(this)
        SecureSettingsAccess.setServiceEnabled(this, false)
        return true
    }

    private companion object {
        const val UNBIND_SETTLE_MS = 400L
    }
}
