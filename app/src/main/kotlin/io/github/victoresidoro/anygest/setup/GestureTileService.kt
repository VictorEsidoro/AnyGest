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

package io.github.victoresidoro.anygest.setup

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService
import io.github.victoresidoro.anygest.ui.MainActivity

/**
 * The way back when HyperOS stops the gesture service with the navigation bar hidden: the
 * Control Center still opens from the top of the screen, so this tile is always reachable.
 * It shows whether gestures are running; tapping it opens AnyGest's Accessibility page
 * when they are not, and the app when they are. It cannot switch the service on by itself:
 * only the user may enable an accessibility service.
 */
class GestureTileService : TileService() {

    override fun onTileAdded() {
        SetupStore(this).tileAdded = true
        refresh()
    }

    override fun onTileRemoved() {
        SetupStore(this).tileAdded = false
    }

    override fun onStartListening() {
        // Listening at all means the tile sits in the panel, including one added by hand
        // before this app learnt to remember it.
        SetupStore(this).tileAdded = true
        refresh()
    }

    override fun onClick() {
        val target = if (DiagnosticsCollector.isAccessibilityServiceEnabled(this)) {
            Intent(this, MainActivity::class.java)
        } else {
            accessibilityIntent(this)
        }
        if (isLocked) unlockAndRun { open(target) } else open(target)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val running = DiagnosticsCollector.isAccessibilityServiceEnabled(this)
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = getString(if (running) R.string.tile_active else R.string.tile_inactive)
        tile.updateTile()
    }

    // The Intent overload throws on Android 14+, and the PendingIntent one is missing below it.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {

        fun component(context: Context) = ComponentName(context, GestureTileService::class.java)

        /** Asks the system to refresh the tile, e.g. when the gesture service starts or stops. */
        fun requestRefresh(context: Context) {
            runCatching { requestListeningState(context, component(context)) }
        }

        /**
         * AnyGest's own Accessibility page where the platform offers it, so the switch is
         * one tap away; the full Accessibility list otherwise.
         */
        fun accessibilityIntent(context: Context): Intent {
            val details = Intent(ACCESSIBILITY_DETAILS_SETTINGS).putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                ComponentName(context, AnyGestAccessibilityService::class.java)
                    .flattenToString(),
            )
            val resolves = details.resolveActivity(context.packageManager) != null
            return if (resolves) details else Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }

        private const val ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    }
}
