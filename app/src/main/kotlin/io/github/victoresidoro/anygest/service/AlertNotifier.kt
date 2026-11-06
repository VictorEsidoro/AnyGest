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

package io.github.victoresidoro.anygest.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.navigation.HyperOSNavigationIntegration
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.setup.GestureTileService
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Two quiet notifications, both opt-in: the gestures stopped, and the system put the button
 * bar back. Neither can be fixed from here (that needs WRITE_SECURE_SETTINGS), so each one
 * only shortens the trip to the screen that fixes it.
 */
internal object AlertNotifier {

    private const val CHANNEL_ID = "alerts"
    private const val ID_STOPPED = 1
    private const val ID_BUTTONS = 2

    /** The bar only counts as "back" when gestures were what it left; a fresh read says nothing. */
    fun navigationBarCameBack(previous: NavigationMode?, current: NavigationMode): Boolean =
        previous == NavigationMode.GESTURAL && current == NavigationMode.THREE_BUTTON

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun gesturesStopped(context: Context) {
        if (!enabled(context)) return
        post(
            context,
            ID_STOPPED,
            R.string.alert_stopped_title,
            R.string.alert_stopped_body,
            GestureTileService.accessibilityIntent(context),
        )
    }

    fun gesturesRunning(context: Context) = cancel(context, ID_STOPPED)

    fun navigationBarBack(context: Context) {
        if (!enabled(context)) return
        post(
            context,
            ID_BUTTONS,
            R.string.alert_buttons_title,
            R.string.alert_buttons_body,
            Intent(HyperOSNavigationIntegration.NAVIGATION_MODE_SETTINGS_ACTION),
        )
    }

    fun navigationBarGone(context: Context) = cancel(context, ID_BUTTONS)

    private fun enabled(context: Context): Boolean =
        SetupStore(context).alertsEnabled && canPost(context)

    private fun post(context: Context, id: Int, title: Int, body: Int, target: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val opens = PendingIntent.getActivity(
            context,
            id,
            target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(body))
            .setContentIntent(opens)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(id, notification) }
    }

    private fun cancel(context: Context, id: Int) {
        runCatching { context.getSystemService(NotificationManager::class.java)?.cancel(id) }
    }
}
