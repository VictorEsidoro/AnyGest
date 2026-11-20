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

package io.github.victoresidoro.anygest.ui

import android.app.Activity
import android.app.ActivityManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import io.github.victoresidoro.anygest.setup.SetupStore

// The doors out of the app: other apps' screens, the tile prompt, the Recents setting.

private const val MIUI_AUTOSTART_ACTION = "miui.intent.action.OP_AUTO_START"

/**
 * Opens the first of [candidates] the system accepts. A context that is not an Activity needs
 * NEW_TASK; from an Activity it is wrong, since it would give the target its own task and Back
 * would no longer return here.
 */
internal fun Context.startFirst(vararg candidates: Intent) {
    val needsTask = hostActivity() == null
    for (intent in candidates) {
        if (needsTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isSuccess) return
    }
}

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

/** This app's own page in Settings, where the battery and restricted-settings switches are. */
private fun appDetailsIntent(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    "package:${context.packageName}".toUri(),
)

internal fun openAppInfo(context: Context) = context.startFirst(appDetailsIntent(context))

/** Autostart decides whether HyperOS restores the service after a reboot. Only HyperOS has it. */
internal fun openAutostart(context: Context) = context.startFirst(
    Intent(MIUI_AUTOSTART_ACTION).addCategory(Intent.CATEGORY_DEFAULT),
    appDetailsIntent(context),
)

/** The notification channel's page, for hiding the keep-alive notice. */
internal fun openKeepAliveChannel(context: Context, channel: Intent) =
    context.startFirst(channel, appDetailsIntent(context))

/**
 * Where the digital assistant app is chosen. VOICE_INPUT_SETTINGS is AOSP's page for it; the
 * default-apps list is the fallback for builds that do not route it there.
 */
internal fun openAssistantSettings(context: Context) = context.startFirst(
    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
    Intent(Settings.ACTION_SETTINGS),
)

internal fun openUsageAccess(context: Context) =
    context.startFirst(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

/**
 * Android 13's one-tap prompt to place a quick-settings tile. A build that refuses it still
 * lets the user add the tile by hand, which each card's text explains.
 */
internal fun askToAddTile(
    context: Context,
    component: ComponentName,
    @StringRes label: Int,
    @DrawableRes icon: Int,
    onAdded: () -> Unit,
    onAnswered: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
    runCatching {
        statusBar.requestAddTileService(
            component,
            context.getString(label),
            Icon.createWithResource(context, icon),
            context.mainExecutor,
        ) { answer ->
            val there = answer == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                answer == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            if (there) onAdded()
            onAnswered()
        }
    }
}

/**
 * Swiping the app's card away in Recents ends it, and the gestures with it, so the card can be
 * left out of Recents. It is applied on every resume as well as on the switch, because the
 * setting belongs to the task, which a fresh launch creates anew.
 */
internal fun applyRecentsVisibility(context: Context) {
    val hidden = !SetupStore(context).showInRecents
    runCatching {
        context.getSystemService(ActivityManager::class.java)?.appTasks?.forEach {
            it.setExcludeFromRecents(hidden)
        }
    }
}
