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
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore
import io.github.victoresidoro.anygest.ui.MainActivity

/**
 * An optional foreground service that keeps the app's process alive. HyperOS ends the app when
 * its card is swiped away in Recents, but spares a process that is in the foreground, so the
 * gestures keep working. Android requires a notification for it; it sits at the lowest
 * importance with an empty icon, so it shows nothing in the status bar. In ADB mode the
 * notification carries a Pause / Switch on button, like the tile.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        runCatching {
            val notification = build(this)
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
        running = false
        SetupStore(this).note("keepalive: stopped")
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "keepalive"
        private const val NOTIFICATION_ID = 7

        @Volatile
        private var running = false

        /**
         * Makes the service and its notification match the setting and the state of the
         * gestures: started or refreshed while the setting is on, stopped when it is off.
         */
        fun refresh(context: Context) {
            if (!SetupStore(context).keepAlive) {
                runCatching { context.stopService(Intent(context, KeepAliveService::class.java)) }
                return
            }
            if (running) {
                runCatching {
                    context.getSystemService(NotificationManager::class.java)
                        ?.notify(NOTIFICATION_ID, build(context))
                }
                return
            }
            runCatching {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            }.onFailure {
                SetupStore(context).note("keepalive: could not start (${it.javaClass.simpleName})")
            }
        }

        /** The system page where this one notification can be hidden, leaving the others on. */
        fun channelSettings(context: Context): Intent =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        private fun build(context: Context): Notification {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.keepalive_channel_name),
                    NotificationManager.IMPORTANCE_MIN,
                ),
            )
            val up = AnyGestAccessibilityService.isRunning() || GesturePower.starting()
            val opens = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_keepalive_blank)
                .setContentTitle(context.getString(R.string.keepalive_title))
                .setContentText(
                    context.getString(if (up) R.string.keepalive_body_on else R.string.keepalive_body_off),
                )
                .setContentIntent(opens)
                .setOngoing(true)
            if (SecureSettingsAccess.canWrite(context)) {
                val action = if (up) AlertActionReceiver.ACTION_SWITCH_OFF else AlertActionReceiver.ACTION_SWITCH_ON
                builder.addAction(
                    Notification.Action.Builder(
                        null,
                        context.getString(if (up) R.string.keepalive_action_pause else R.string.keepalive_action_resume),
                        PendingIntent.getBroadcast(
                            context,
                            0,
                            Intent(action).setPackage(context.packageName),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        ),
                    ).build(),
                )
            }
            return builder.build()
        }
    }
}
