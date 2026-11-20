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

// This accessibility privilege is used for navigation only: it never reads window content,
// text or keystrokes, and sends nothing off the device.

package io.github.victoresidoro.anygest.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import io.github.victoresidoro.anygest.diagnostics.FailureLog
import io.github.victoresidoro.anygest.diagnostics.Trace
import io.github.victoresidoro.anygest.gesture.GestureAction
import io.github.victoresidoro.anygest.gesture.PlatformDefaults
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.settings.GestureSettingsRepository
import io.github.victoresidoro.anygest.setup.GestureTileService
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The accessibility service: it owns the parts and passes the system's events to them.
 *
 * - [StripHost]: the edge strips, their size and their upkeep.
 * - [LockGate]: strips off from screen off until the lock screen is gone.
 * - [PauseController]: stepping aside for the apps on the pause list.
 * - [NavigationKeeper]: the system bar and gesture mode, with ADB mode.
 * - [DeviceNotes]: app installs and default apps, for the notes.
 * - [GestureReplayer]: touches that were no gesture, played back into the app.
 *
 * The strips live in this process, as accessibility overlays: no overlay permission and no
 * ongoing notification are needed, and the system binds an enabled service again by itself.
 */
class AnyGestAccessibilityService : AccessibilityService() {

    /** An uncaught throw in [scope] (DataStore on an unreadable file) would kill the process. */
    private val coroutineFailures = CoroutineExceptionHandler { _, throwable ->
        FailureLog.record(this, "settings collector", throwable)
    }

    // Rebuilt in setUp: teardown cancels it, and a connection made again on the same instance
    // would otherwise collect its settings in a dead scope.
    private var scope = newScope()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var performer: GestureActionPerformer
    private var host: StripHost? = null
    private var lockGate: LockGate? = null
    private var pause: PauseController? = null
    private var keeper: NavigationKeeper? = null
    private var notes: DeviceNotes? = null

    /** False until the first settings have decided whether to pause straight away. */
    private var started = false

    private val stallWatch = StallWatch()
    private val stallTick = object : Runnable {
        override fun run() {
            stallWatch.tick(SystemClock.elapsedRealtime())?.let { SetupStore(this@AnyGestAccessibilityService).note(it) }
            handler.postDelayed(this, STALL_TICK_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> screenOff()
                Intent.ACTION_USER_PRESENT -> {
                    lockGate?.unlocked()
                    notes?.defaultsMaybeChanged()
                }
                else -> screenOn()
            }
        }
    }

    // A rotation reaches the display after the configuration change, so that alone would race it.
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) host?.displayChanged()
        }
    }

    /**
     * An uncaught throw here gets the service marked "malfunctioning" and not bound again
     * until the user switches it by hand; degraded but bound is better, and Diagnostics says
     * what failed.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        SetupStore(this).let { store ->
            val updatedAt = runCatching { packageManager.getPackageInfo(packageName, 0).lastUpdateTime }.getOrDefault(0L)
            // A plain start is routine (every pause ends in one); an update or a stop without a
            // callback is worth a line.
            if (store.serviceUp) {
                store.note(ServiceStart.note(true, store.lastUpdateSeen, updatedAt))
                ExitReasons.last(this)?.let(store::note)
            }
            store.lastUpdateSeen = updatedAt
            store.serviceUp = true
        }
        RecoveryJob.schedule(this)
        KeepAliveService.refresh(this)
        try {
            setUp()
        } catch (t: Throwable) {
            FailureLog.record(this, "onServiceConnected", t)
        }
        AlertNotifier.gesturesRunning(this)
        GestureTileService.requestRefresh(this)
    }

    private fun setUp() {
        if (!scope.isActive) scope = newScope()
        performer = GestureActionPerformer(this)
        val stripHost = StripHost(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            handler = handler,
            replayer = GestureReplayer(this, handler),
            onGestureAction = ::onGestureAction,
        )
        host = stripHost
        lockGate = LockGate(this, handler, stripHost::setArmed).also { it.start(screenIsOn()) }
        val deviceNotes = DeviceNotes(this)
        notes = deviceNotes
        pause = PauseController(this, handler, onSubscribed = { if (screenIsOn()) startStallWatch() })
        keeper = NavigationKeeper(this, handler, onModeChanging = stripHost::startSettle, appChanges = deviceNotes::appChanges)

        getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, handler)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        deviceNotes.start()
        keeper?.start()

        val settings = GestureSettingsRepository(applicationContext)
        scope.launch {
            settings.configuration.collect { updated ->
                val configuration = updated.copy(platform = platformDefaults())
                stripHost.configure(configuration)
                if (!started) stripHost.startSettle()
                pause?.configure(configuration)
                if (!started) {
                    started = true
                    keeper?.active = true
                    // Starting while a listed app is in front must not flash the service at it,
                    // so that comes before the bar is touched.
                    if (pause?.pauseIfListedAppInFront() != true) keeper?.keepBarHidden()
                }
            }
        }
    }

    private fun screenOff() {
        PowerContext.screenWentOff(SystemClock.elapsedRealtime())
        pause?.stopPolling()
        stopStallWatch()
        lockGate?.screenOff()
    }

    private fun screenOn() {
        lockGate?.screenOn()
        notes?.defaultsMaybeChanged()
        // A long sleep is when the freezes on waking were seen, so it is worth a line.
        PowerContext.screenWentOn(SystemClock.elapsedRealtime())?.takeIf { it >= LONG_SLEEP_MS }?.let {
            SetupStore(this).note("screen on after ${PowerContext.duration(it)} off (${PowerContext.describe(this)})")
        }
        startStallWatch()
        pause?.updatePolling()
        // The moment the gestures are about to be used: what went wrong while the screen was off
        // is put right before the first swipe.
        host?.screenOn()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        KeepAliveService.refresh(this)
        val store = SetupStore(this)
        store.serviceUp = false
        // A pause wrote its own line; an unbind nobody chose is the one worth a note.
        if (store.pausedForApp == null) store.note("service unbound (userPause=${store.userPaused}; ${PowerContext.describe(this)})")
        // A pause or the tile's off switch is a choice; anything else is a stop to report.
        if (store.pausedForApp == null && !store.userPaused) {
            AlertNotifier.gesturesStopped(this)
            // The bar was hidden for these gestures; without them nothing would be left to navigate with.
            if (GesturePower.barManaged(this)) SecureSettingsAccess.showBar(this)
        }
        GestureTileService.requestRefresh(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        host?.displayChanged()
    }

    /**
     * Only window changes are delivered (of the listed apps, or of every app for a few seconds
     * after a gesture in a test build), and only the package and class names are read.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val changed = event ?: return
        if (changed.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = changed.packageName?.toString() ?: return
        Trace.windowChanged("$packageName/${changed.className?.toString().orEmpty()}", changed.eventTime)
        pause?.windowShown(packageName)
    }

    override fun onInterrupt() = Unit

    private fun onGestureAction(action: GestureAction, edge: ScreenEdge) {
        Log.v(TAG, "Gesture from $edge -> $action")
        pause?.widenForTrace()
        Trace.gesture(edge.name, action.id, refreshRate())
        performer.perform(action)
    }

    /**
     * Read again on every settings change, so a changed "touch and hold delay" is picked up the
     * next time anything is adjusted; the AOSP values stand in if the platform refuses.
     */
    private fun platformDefaults(): PlatformDefaults {
        val fallback = PlatformDefaults()
        val touchSlopDp = runCatching { ViewConfiguration.get(this).scaledTouchSlop / resources.displayMetrics.density }
            .getOrDefault(fallback.touchSlopDp)
        return fallback.copy(longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong(), touchSlopDp = touchSlopDp)
    }

    /** The panel's refresh rate right now, since the system lowers it on its own. */
    private fun refreshRate(): String = runCatching {
        "${getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).refreshRate.toInt()} Hz"
    }.getOrDefault("")

    private fun screenIsOn(): Boolean = getSystemService(PowerManager::class.java)?.isInteractive != false

    private fun startStallWatch() {
        handler.removeCallbacks(stallTick)
        stallWatch.start(SystemClock.elapsedRealtime())
        handler.postDelayed(stallTick, STALL_TICK_MS)
    }

    private fun stopStallWatch() {
        handler.removeCallbacks(stallTick)
        stallWatch.stop()
    }

    private fun teardown() {
        runCatching { getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener) }
        keeper?.stop()
        pause?.stopPolling()
        stopStallWatch()
        lockGate?.stop()
        runCatching { unregisterReceiver(screenReceiver) }
        notes?.stop()
        started = false
        host?.stop()
        host = null
        lockGate = null
        pause = null
        keeper = null
        notes = null
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        instance = null
    }

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + coroutineFailures)

    companion object {
        private const val TAG = "AnyGest"
        private const val STALL_TICK_MS = 2_000L
        private const val LONG_SLEEP_MS = 5 * 60_000L

        @Volatile
        private var instance: AnyGestAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        /** The strips as installed, for the copied log; says so when the service is not running. */
        fun stripSummary(): String = instance?.host?.describe() ?: "service not running"

        /** After the one-time grant or a change to the pause list, without waiting for a restart. */
        fun refreshSubscription() {
            instance?.pause?.subscribe()
        }

        /** For the checklist's "open Recents" button; false when the service is not running. */
        fun openRecents(): Boolean = instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) == true
    }
}
