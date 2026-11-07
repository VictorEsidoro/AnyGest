// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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

// This accessibility privilege is used for navigation only: it never reads window
// content, text or keystrokes, and sends nothing off the device.

package io.github.victoresidoro.anygest.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import io.github.victoresidoro.anygest.diagnostics.FailureLog
import io.github.victoresidoro.anygest.gesture.GestureAction
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.PlatformDefaults
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.gesture.TouchSample
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.NavigationModeReader
import io.github.victoresidoro.anygest.settings.GestureSettingsRepository
import io.github.victoresidoro.anygest.setup.GestureTileService
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Hosts the edge strips in-process rather than in a foreground service:
 * `TYPE_ACCESSIBILITY_OVERLAY` needs no `SYSTEM_ALERT_WINDOW` permission and no ongoing
 * notification, and the framework rebinds an enabled service itself, so no watchdog.
 */
class AnyGestAccessibilityService : AccessibilityService(), TouchReplayer {

    /**
     * An uncaught throw in [scope] - realistically DataStore on an unreadable file -
     * kills the process and gets the service reported as malfunctioning.
     */
    private val coroutineFailures = CoroutineExceptionHandler { _, throwable ->
        FailureLog.record(this, "settings collector", throwable)
    }
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + coroutineFailures)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var settings: GestureSettingsRepository
    private lateinit var performer: GestureActionPerformer
    private lateinit var windowManager: WindowManager
    private var overlay: EdgeTouchOverlay? = null
    private var configuration: GestureConfiguration = GestureConfiguration()
    private var lastGeometry: ScreenGeometry? = null
    private var lastNavigationMode: NavigationMode? = null

    private var lastSubscription: String? = null

    /**
     * Second way to notice a listed app: some apps keep their windows out of what a service may
     * see, so no event ever comes. While the screen is on and the list is usable, the foreground
     * app is also read from the usage events, a little late but from nothing the app controls.
     */
    private var polling = false
    private val poll = Runnable { pollForeground() }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                stopPolling()
                return
            }
            updatePolling()
            // The moment someone is about to use the gestures: strips that went missing or
            // stuck while the screen was off are put right before the first swipe.
            val current = overlay ?: return
            if (!current.healthy()) {
                SetupStore(this@AnyGestAccessibilityService)
                    .note("strips missing when the screen came on, reinstalled")
                installOverlay()
            }
        }
    }

    /** False until the first settings emission has decided whether to pause straight away. */
    private var initialChecked = false

    /** The system flipping back to the button bar is only visible by watching the setting. */
    private val navigationObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = onNavigationModeChanged()
    }

    /** HyperOS may put the button bar back by itself; with ADB mode that is undone at once. */
    private val barObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (initialChecked) ensureBarHidden()
        }
    }

    /**
     * Rotation reaches the display *after* the configuration change lands, so
     * [onConfigurationChanged] alone would race it.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) reinstallIfGeometryChanged()
        }
    }

    /**
     * An uncaught throw in setup makes Android mark the service "malfunctioning" and
     * refuse to re-bind it until the user toggles it by hand; degraded but bound is
     * better, and the Diagnostics section shows what failed.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        SetupStore(this).let { store ->
            if (store.serviceUp) store.note("service started after an unclean stop (process killed?)")
            else store.note("service connected")
            store.serviceUp = true
        }
        RecoveryJob.schedule(this)
        KeepAliveService.sync(this, gesturesRunning = true)
        try {
            setUp()
        } catch (t: Throwable) {
            FailureLog.record(this, "onServiceConnected", t)
        }
        AlertNotifier.gesturesRunning(this)
        GestureTileService.requestRefresh(this)
    }

    private fun onNavigationModeChanged() {
        val current = NavigationModeReader.read(this)
        if (initialChecked && current != NavigationMode.GESTURAL && keepGestureMode()) {
            // Put back at once, so there is nothing to alert about.
            lastNavigationMode = NavigationMode.GESTURAL
            return
        }
        if (AlertNotifier.navigationBarCameBack(lastNavigationMode, current)) {
            AlertNotifier.navigationBarBack(this)
        } else if (current == NavigationMode.GESTURAL) {
            AlertNotifier.navigationBarGone(this)
        }
        lastNavigationMode = current
    }

    private fun setUp() {
        settings = GestureSettingsRepository(applicationContext)
        performer = GestureActionPerformer(this)
        windowManager = getSystemService(WindowManager::class.java)
        overlay = EdgeTouchOverlay(
            context = this,
            windowManager = windowManager,
            replayer = this,
            onGestureAction = ::onGestureAction,
        )

        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
            .registerDisplayListener(displayListener, handler)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        lastNavigationMode = NavigationModeReader.read(this)
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("navigation_mode"),
            false,
            navigationObserver,
        )
        contentResolver.registerContentObserver(SecureSettingsAccess.barUri(), false, barObserver)

        scope.launch {
            settings.configuration.collect { updated ->
                configuration = updated.copy(platform = platformDefaults())
                installOverlay()
                applyEventSubscription()
                if (!initialChecked) {
                    initialChecked = true
                    // Starting while a paused-for app is already in front must not flash the
                    // service at it, so that check comes before the bar is touched.
                    if (!pauseIfForegroundIsException()) ensureBarHidden()
                }
            }
        }
    }

    /**
     * Window events are asked for only while the pause list is usable, and then only from
     * the listed apps: the process is not woken by any other app, which is why the XML
     * declares no event types at all.
     */
    private fun applyEventSubscription() {
        val info = serviceInfo ?: return
        val packages =
            if (pauseUsable()) configuration.pauseApps.toTypedArray() else emptyArray<String>()
        val subscription = packages.sorted().joinToString(",").ifEmpty { "none" }
        if (subscription != lastSubscription) {
            lastSubscription = subscription
            SetupStore(this).note("window events for: $subscription")
        }
        if (packages.isEmpty()) {
            info.eventTypes = 0
            info.packageNames = null
        } else {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.packageNames = packages
        }
        serviceInfo = info
        updatePolling()
    }

    private fun updatePolling() {
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive != false
        if (pauseUsable() && interactive && SetupStore(this).pausedForApp == null) {
            startPolling()
        } else {
            stopPolling()
        }
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        handler.postDelayed(poll, POLL_MS)
    }

    private fun stopPolling() {
        polling = false
        handler.removeCallbacks(poll)
    }

    private fun pollForeground() {
        if (!polling) return
        val front = ForegroundApp.current(this, POLL_WINDOW_MS)
        if (front != null && shouldPauseFor(front)) {
            SetupStore(this).note("poll saw $front in front")
            stopPolling()
            pauseFor(front)
            return
        }
        handler.postDelayed(poll, POLL_MS)
    }

    private fun pauseUsable(): Boolean =
        configuration.pauseInAppsEnabled && configuration.pauseApps.isNotEmpty() &&
            GesturePower.pauseAvailable(this)

    private fun shouldPauseFor(packageName: String): Boolean = PausePolicy.shouldPause(
        packageName = packageName,
        featureEnabled = configuration.pauseInAppsEnabled,
        apps = configuration.pauseApps,
        available = GesturePower.pauseAvailable(this),
        alreadyPaused = SetupStore(this).pausedForApp != null,
    )

    private fun pauseIfForegroundIsException(): Boolean {
        if (!pauseUsable()) return false
        val front = ForegroundApp.current(this, FOREGROUND_LOOKBACK_MS) ?: return false
        if (!shouldPauseFor(front)) return false
        pauseFor(front)
        return true
    }

    /**
     * Steps aside for an app that refuses to run next to an accessibility service: the
     * watcher goes first (a pause nobody ends is worse than none), then the button bar comes
     * back so the phone stays navigable, then the service takes itself out of the enabled list.
     */
    private fun pauseFor(packageName: String) {
        val store = SetupStore(this)
        store.pausedForApp = packageName
        store.note("pausing for $packageName")
        if (!ExceptionWatchService.start(this)) {
            store.pausedForApp = null
            store.note("the system refused the watcher, so no pause")
            FailureLog.record(
                this,
                "pause for $packageName",
                IllegalStateException("the system refused the foreground start for the watcher"),
            )
            return
        }
        if (GesturePower.barManaged(this)) SecureSettingsAccess.setBarHidden(this, false)
        disableSelf()
    }

    private fun ensureBarHidden() {
        val store = SetupStore(this)
        if (store.pausedForApp != null || store.userPaused) return
        if (!GesturePower.barManaged(this)) return
        if (SecureSettingsAccess.barHidden(this) != true) {
            SecureSettingsAccess.setBarHidden(this, true)
        }
        keepGestureMode()
    }

    /**
     * HyperOS can switch itself back to the button bar with a third-party launcher. With ADB
     * mode and "keep the bar hidden" on, and only while this service runs, gesture navigation
     * is put back. Based on HyperGesture commit 81c1ad1 (#127) by Henrique Almeida (h3nc4).
     */
    private fun keepGestureMode(): Boolean {
        val store = SetupStore(this)
        if (store.pausedForApp != null || store.userPaused) return false
        if (!GesturePower.barManaged(this)) return false
        if (SecureSettingsAccess.gestureModeOn(this) != false) return false
        val restored = SecureSettingsAccess.setGestureMode(this)
        store.note(
            if (restored) "navigation mode was reset by the system, gestures put back"
            else "could not put gesture navigation back",
        )
        return restored
    }

    override fun onUnbind(intent: Intent?): Boolean {
        KeepAliveService.sync(this, gesturesRunning = false)
        teardown()
        val store = SetupStore(this)
        store.serviceUp = false
        store.note("service unbound (appPause=${store.pausedForApp}, userPause=${store.userPaused})")
        // A pause or the tile's off switch is a choice; anything else is a stop to report.
        if (store.pausedForApp == null && !store.userPaused) {
            AlertNotifier.gesturesStopped(this)
            // The bar was hidden for these gestures; without them nothing would be left to
            // navigate with.
            if (GesturePower.barManaged(this)) SecureSettingsAccess.setBarHidden(this, false)
        }
        GestureTileService.requestRefresh(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        SetupStore(this).note("service destroyed")
        teardown()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reinstallIfGeometryChanged()
    }

    /**
     * Only window changes of the paused-for apps are ever delivered, and only the package name
     * is read: never the content of the window.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val changed = event ?: return
        if (changed.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = changed.packageName?.toString() ?: return
        if (shouldPauseFor(packageName)) pauseFor(packageName)
    }

    override fun onInterrupt() = Unit

    /**
     * Read on every settings emission and reinstall, so a changed "touch and hold delay" in
     * accessibility settings is picked up the next time anything is adjusted; the AOSP
     * defaults stand in if the platform refuses.
     */
    private fun platformDefaults(): PlatformDefaults {
        val fallback = PlatformDefaults()
        val touchSlopDp = runCatching {
            ViewConfiguration.get(this).scaledTouchSlop / resources.displayMetrics.density
        }.getOrDefault(fallback.touchSlopDp)
        return fallback.copy(
            longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong(),
            touchSlopDp = touchSlopDp,
        )
    }

    private fun installOverlay() {
        val geometry = ScreenGeometry.current(this, windowManager)
        lastGeometry = geometry
        overlay?.install(configuration, geometry)
    }

    /**
     * Strip windows keep their pixel sizes across a rotation, so a side strip measured
     * against portrait height runs off a landscape screen.
     */
    private fun reinstallIfGeometryChanged() {
        if (overlay == null) return
        if (ScreenGeometry.current(this, windowManager) == lastGeometry) return
        installOverlay()
    }

    private fun teardown() {
        runCatching {
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .unregisterDisplayListener(displayListener)
        }
        runCatching { contentResolver.unregisterContentObserver(navigationObserver) }
        runCatching { contentResolver.unregisterContentObserver(barObserver) }
        stopPolling()
        runCatching { unregisterReceiver(screenReceiver) }
        initialChecked = false
        overlay?.remove()
        overlay = null
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        instance = null
    }

    private fun onGestureAction(action: GestureAction, edge: ScreenEdge) {
        Log.v(TAG, "Gesture from $edge -> $action")
        performer.perform(action)
    }

    /**
     * The strips have been non-touchable and alpha 0 since this touch's `ACTION_DOWN`, so
     * its own duration already counted toward the asynchronous window update and only the
     * remainder of [WINDOW_SETTLE_MS] still has to be waited out.
     */
    override fun replay(samples: List<TouchSample>, onDone: () -> Unit): Boolean {
        if (samples.isEmpty()) return false

        val first = samples.first()
        val last = samples.last()
        val movedPx = hypot(last.xPx - first.xPx, last.yPx - first.yPx)
        val isTap = movedPx < TAP_SLOP_PX
        val rawDurationMs = last.timestampMs - first.timestampMs

        val path = Path().apply {
            moveTo(first.xPx, first.yPx)
            if (isTap) {
                // dispatchGesture rejects a zero-length path. Nudge by a sub-slop pixel:
                // still a tap, not a drag.
                lineTo(first.xPx + 1f, first.yPx + 1f)
            } else {
                for (i in 1 until samples.size) lineTo(samples[i].xPx, samples[i].yPx)
            }
        }

        val durationMs = when {
            !isTap -> rawDurationMs.coerceIn(1L, MAX_REPLAY_MS)
            // A deliberate hold: mirror it so the target's long-press fires too.
            rawDurationMs >= ViewConfiguration.getLongPressTimeout() ->
                rawDurationMs.coerceAtMost(MAX_REPLAY_MS)
            // The click fires at the end of the stroke, so capping it cuts perceived
            // tap latency; the press physically happened already.
            else -> rawDurationMs.coerceIn(MIN_TAP_MS, TAP_STROKE_CAP_MS)
        }

        val gesture = runCatching {
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                .build()
        }.getOrElse {
            Log.w(TAG, "Could not build replay gesture", it)
            return false
        }

        val settleDelayMs = (WINDOW_SETTLE_MS - rawDurationMs).coerceAtLeast(0L)
        var finished = false
        val finish = Runnable {
            if (!finished) {
                finished = true
                onDone()
            }
        }
        // Safety net: release the strips even if the result callback never arrives.
        handler.postDelayed(finish, durationMs + settleDelayMs + FINISH_GRACE_MS)
        handler.postDelayed({
            // After this delay the service may be unbound, and dispatchGesture then throws
            // instead of returning false; uncaught that kills the process and gets the
            // service flagged "malfunctioning".
            val dispatched = runCatching {
                dispatchGesture(
                    gesture,
                    object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            releaseStrips()
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            // The touch never reached the app underneath, so the tap is
                            // lost; the strips still have to come back either way.
                            Log.w(TAG, "REPLAY_CANCELLED after ${durationMs}ms")
                            releaseStrips()
                        }

                        private fun releaseStrips() {
                            handler.removeCallbacks(finish)
                            finish.run()
                        }
                    },
                    handler,
                )
            }.getOrElse { throwable ->
                FailureLog.record(this, "dispatchGesture", throwable)
                false
            }
            if (dispatched) {
                Log.d(TAG, "REPLAY_DISPATCHED ${samples.size} samples over ${durationMs}ms")
            } else {
                Log.w(TAG, "REPLAY_REFUSED dispatchGesture rejected the replay")
                handler.removeCallbacks(finish)
                finish.run()
            }
        }, settleDelayMs)
        return true
    }

    companion object {
        private const val TAG = "AnyGest"

        private const val MAX_REPLAY_MS = 3_000L

        /** A 1ms stroke completes but never clicks. */
        private const val MIN_TAP_MS = 50L

        private const val TAP_STROKE_CAP_MS = 60L

        private const val TAP_SLOP_PX = 12f

        /**
         * Grace for the non-touchable + alpha-0 update to reach the input pipeline, so the
         * injected touch is not discarded as obscured.
         */
        private const val WINDOW_SETTLE_MS = 65L

        private const val FINISH_GRACE_MS = 1_000L

        /** The longest a replay keeps the strips held after the finger is gone. */
        internal const val LONGEST_REPLAY_HOLD_MS = MAX_REPLAY_MS + WINDOW_SETTLE_MS + FINISH_GRACE_MS

        /** How often the foreground app is read while the screen is on and the list is in use. */
        private const val POLL_MS = 1_500L

        /** Wide enough to catch an app that opened a few seconds ago, whose event came late. */
        private const val POLL_WINDOW_MS = 30_000L

        /** How far back the "which app is in front" check looks when the service starts. */
        private const val FOREGROUND_LOOKBACK_MS = 2L * 60L * 60L * 1000L

        @Volatile
        private var instance: AnyGestAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        /** After the one-time grant or a change to the pause list, without waiting for a restart. */
        fun refreshSubscription() {
            instance?.applyEventSubscription()
        }

        /** For the checklist's "open Recents" button; false when the service is not running. */
        fun openRecents(): Boolean =
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) == true
    }
}
