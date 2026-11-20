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

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import io.github.victoresidoro.anygest.diagnostics.FailureLog
import io.github.victoresidoro.anygest.diagnostics.TraceLog
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Steps the service aside for the apps on the pause list, which refuse to run next to an
 * accessibility service.
 *
 * Such an app is seen two ways. Window events are asked for only from the listed apps, and
 * only while the list can be used, so no other app ever wakes the process (the service's XML
 * asks for no events at all). And because some apps keep their windows out of what a service
 * may see, the foreground app is also read from the usage events while the screen is on: a
 * little late, but from nothing the app controls.
 */
internal class PauseController(
    private val service: AccessibilityService,
    private val handler: Handler,
    /** Called after every change of the event subscription. */
    private val onSubscribed: () -> Unit,
) {
    private var configuration = GestureConfiguration()
    private var polling = false
    private var foreground: ForegroundApp.Tracker? = null
    private val poll = Runnable { pollForeground() }
    private val narrow = Runnable { subscribe() }

    /** Only a pre-release build (`-test.N`) widens the subscription for the trace. */
    private val testBuild by lazy {
        runCatching {
            service.packageManager.getPackageInfo(service.packageName, 0).versionName.orEmpty().contains("-test")
        }.getOrDefault(false)
    }

    fun configure(updated: GestureConfiguration) {
        configuration = updated
        subscribe()
    }

    /** After the one-time grant or a change to the list, without waiting for a restart. */
    fun subscribe() {
        val info = service.serviceInfo ?: return
        val packages = if (usable()) configuration.pauseApps.toTypedArray() else emptyArray()
        if (packages.isEmpty()) {
            info.eventTypes = 0
            info.packageNames = null
        } else {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.packageNames = packages
        }
        service.serviceInfo = info
        updatePolling()
        onSubscribed()
    }

    /**
     * Test builds only: window changes from every app for a few seconds after a gesture, so
     * the trace can time the transition it starts. The narrow subscription comes back by itself.
     */
    fun widenForTrace() {
        if (!testBuild) return
        val info = service.serviceInfo ?: return
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.packageNames = null
        service.serviceInfo = info
        handler.removeCallbacks(narrow)
        handler.postDelayed(narrow, TraceLog.WINDOW_WAIT_MS + 500L)
    }

    /** A window of [packageName] came to the front. */
    fun windowShown(packageName: String) {
        if (shouldPauseFor(packageName)) pauseFor(packageName)
    }

    /** Right after the service starts: an app on the list may already be in front. */
    fun pauseIfListedAppInFront(): Boolean {
        if (!usable()) return false
        val front = ForegroundApp.current(service, STARTUP_LOOKBACK_MS) ?: return false
        if (!shouldPauseFor(front)) return false
        pauseFor(front)
        return true
    }

    fun updatePolling() {
        val screenOn = service.getSystemService(PowerManager::class.java)?.isInteractive != false
        if (usable() && screenOn && SetupStore(service).pausedForApp == null) startPolling() else stopPolling()
    }

    fun stopPolling() {
        polling = false
        foreground = null
        handler.removeCallbacks(poll)
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        foreground = ForegroundApp.Tracker(service)
        handler.postDelayed(poll, POLL_MS)
    }

    private fun pollForeground() {
        if (!polling) return
        val front = foreground?.current(POLL_WINDOW_MS)
        if (front != null && shouldPauseFor(front)) {
            stopPolling()
            pauseFor(front)
            return
        }
        handler.postDelayed(poll, POLL_MS)
    }

    private fun usable(): Boolean =
        configuration.pauseInAppsEnabled && configuration.pauseApps.isNotEmpty() && GesturePower.pauseAvailable(service)

    private fun shouldPauseFor(packageName: String): Boolean = PausePolicy.shouldPause(
        packageName = packageName,
        featureEnabled = configuration.pauseInAppsEnabled,
        apps = configuration.pauseApps,
        available = GesturePower.pauseAvailable(service),
        alreadyPaused = SetupStore(service).pausedForApp != null,
    )

    /**
     * The watcher goes first (a pause nobody ends is worse than none), then the button bar comes
     * back so the phone stays usable, then the service takes itself out of the enabled list.
     */
    private fun pauseFor(packageName: String) {
        val store = SetupStore(service)
        store.pausedForApp = packageName
        store.note("pausing for $packageName")
        if (!ExceptionWatchService.start(service)) {
            store.pausedForApp = null
            store.note("the system refused the watcher, so no pause")
            FailureLog.record(
                service,
                "pause for $packageName",
                IllegalStateException("the system refused the foreground start for the watcher"),
            )
            return
        }
        if (GesturePower.barManaged(service)) {
            val before = barState()
            val written = SecureSettingsAccess.showBar(service)
            store.note("pause: bar shown (written=$written), $before -> ${barState()}")
        } else {
            store.note("pause: the bar is not managed, left as it is (${barState()})")
        }
        service.disableSelf()
    }

    /** The two settings that decide what the bottom of the screen shows. */
    private fun barState(): String =
        "bar hidden=${SecureSettingsAccess.barHidden(service)}, gesture mode=${SecureSettingsAccess.gestureModeOn(service)}"

    private companion object {
        const val POLL_MS = 1_500L

        /** Wide enough to catch an app that opened a few seconds ago, whose event came late. */
        const val POLL_WINDOW_MS = 30_000L

        /** How far back the "which app is in front" check looks when the service starts. */
        const val STARTUP_LOOKBACK_MS = 2L * 60L * 60L * 1000L
    }
}
