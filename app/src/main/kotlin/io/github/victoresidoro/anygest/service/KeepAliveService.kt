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
import android.os.IBinder
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * An optional foreground service that runs while the gestures run. HyperOS ends the app when
 * its card is swiped away in Recents, but spares a process that is in the foreground, so the
 * gestures keep working. Android requires a notification for it; it sits at the lowest
 * importance with an empty icon, so it shows nothing in the status bar.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.keepalive_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_keepalive_blank)
            .setContentTitle(getString(R.string.keepalive_title))
            .setContentText(getString(R.string.keepalive_body))
            .setOngoing(true)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            SetupStore(this).note("keepalive: running in the foreground")
        }.onFailure {
            SetupStore(this).note("keepalive: refused (${it.javaClass.simpleName})")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        SetupStore(this).note("keepalive: stopped")
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "keepalive"
        private const val NOTIFICATION_ID = 7

        /** Starts or stops the service to match the setting and whether the gestures run. */
        fun sync(context: Context, gesturesRunning: Boolean) {
            if (gesturesRunning && SetupStore(context).keepAlive) start(context) else stop(context)
        }

        private fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            }.onFailure {
                SetupStore(context).note("keepalive: could not start (${it.javaClass.simpleName})")
            }
        }

        private fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, KeepAliveService::class.java)) }
        }
    }
}
