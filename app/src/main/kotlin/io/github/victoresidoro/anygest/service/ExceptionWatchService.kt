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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.FailureLog
import io.github.victoresidoro.anygest.settings.GestureSettingsRepository
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.setup.SetupStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Alive only while the gestures are paused for an app. With AnyGest out of the enabled
 * accessibility services nothing else can notice the user leaving that app, so this watches
 * the foreground app and switches the service back on when it changes.
 *
 * A foreground service on purpose: a plain background process is exactly what the system
 * kills, and the gestures would then stay off until the app is opened.
 */
class ExceptionWatchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, throwable ->
                FailureLog.record(this, "pause watcher", throwable)
            },
    )
    private var apps: Set<String> = emptySet()
    private var startedAt = 0L
    private var stopped = false
    private var outsidePackage: String? = null
    private var outsideSince = 0L

    private val poll = object : Runnable {
        override fun run() {
            if (stopped) return
            tick()
            if (!stopped) handler.postDelayed(this, if (screenOn()) FAST_MS else SLOW_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        startedAt = System.currentTimeMillis()
        scope.launch {
            apps = GestureSettingsRepository(applicationContext).read().pauseApps
            handler.removeCallbacks(poll)
            handler.postDelayed(poll, FAST_MS)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        running = false
        super.onDestroy()
    }

    private fun tick() {
        val store = SetupStore(this)
        // Somebody else (the tile, the app) already ended the pause.
        if (store.pausedForApp == null) {
            store.note("watcher: pause ended elsewhere")
            finish(restore = false)
            return
        }
        // Without usage access this could never see the user leave, so it must not wait.
        if (!SetupFactsReader.hasUsageAccess(this)) {
            store.note("watcher: no usage access, switching the service back on")
            finish(restore = true)
            return
        }
        // Events are searched from just before the pause: an app that stays in front for an
        // hour produces no event in between, which reads as "still there".
        val window = System.currentTimeMillis() - startedAt + LOOKBACK_MS
        // Only events from the pause onwards count: the newest older one names the app the
        // user came from, and reading it as "left" would end the pause a second after it began.
        val front = ForegroundApp.current(this, window, notBeforeMs = startedAt - EVENT_GRACE_MS)
        if (!PausePolicy.frontIsOutside(front, apps)) {
            if (outsidePackage != null) {
                store.note("watcher: back inside, still paused (was on $outsidePackage)")
            }
            outsidePackage = null
            return
        }
        // Another package can sit in front of the app for a moment (the fingerprint prompt, a
        // permission dialog, the keyboard). Switching the service on then would put it next to
        // an app that checks for it, so "outside" has to last before it counts as leaving.
        val now = System.currentTimeMillis()
        if (front != outsidePackage) {
            outsidePackage = front
            outsideSince = now
            store.note("watcher: $front in front, waiting to see if it stays")
        }
        if (now - outsideSince >= OUTSIDE_HOLD_MS) {
            store.note("watcher: left for $front, switching the service back on")
            finish(restore = true)
        }
    }

    private fun finish(restore: Boolean) {
        stopped = true
        val store = SetupStore(this)
        store.pausedForApp = null
        if (restore && !store.userPaused) SecureSettingsAccess.setServiceEnabled(this, true)
        stopSelf()
    }

    private fun screenOn(): Boolean =
        getSystemService(PowerManager::class.java)?.isInteractive != false

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.pause_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.pause_notification_title))
            .setContentText(getString(R.string.pause_notification_body))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "pause"
        private const val NOTIFICATION_ID = 3
        private const val FAST_MS = 1_000L
        private const val SLOW_MS = 5_000L
        private const val LOOKBACK_MS = 30_000L

        /** How long another app must stay in front before the pause counts as over. */
        private const val OUTSIDE_HOLD_MS = 3_000L

        /** The "resumed" event of the app itself lands just before its window event. */
        private const val EVENT_GRACE_MS = 1_500L

        /** False in a fresh process, which is how a killed watcher is told from a live one. */
        @Volatile
        var running = false
            private set

        /**
         * False when the system refuses a foreground start from here; the caller then does
         * not pause, because a pause nobody is watching would never end.
         */
        fun start(context: Context): Boolean = runCatching {
            context.startForegroundService(Intent(context, ExceptionWatchService::class.java))
            true
        }.getOrDefault(false)

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, ExceptionWatchService::class.java)) }
        }
    }
}
