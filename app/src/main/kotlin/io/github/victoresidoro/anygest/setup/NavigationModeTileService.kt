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
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.navigation.HyperOSNavigationIntegration
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.NavigationModeReader

/**
 * A second, independent Control Center tile: when HyperOS reverts to the three-button bar
 * on its own (see docs/hyperos-navigation.md), this opens the vendor's navigation-mode
 * screen in one tap instead of hunting through Settings. It cannot switch the mode itself -
 * writing it needs WRITE_SECURE_SETTINGS, signature-level - so the user still taps the
 * option there; this only saves the trip to find that screen.
 */
class NavigationModeTileService : TileService() {

    override fun onTileAdded() {
        SetupStore(this).navigationTileAdded = true
        refresh()
    }

    override fun onTileRemoved() {
        SetupStore(this).navigationTileAdded = false
    }

    override fun onStartListening() {
        SetupStore(this).navigationTileAdded = true
        refresh()
    }

    override fun onClick() {
        val intent = Intent(HyperOSNavigationIntegration.NAVIGATION_MODE_SETTINGS_ACTION)
        if (isLocked) unlockAndRun { open(intent) } else open(intent)
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val mode = NavigationModeReader.read(this)
        tile.state = if (mode == NavigationMode.GESTURAL) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_navigation_label)
        tile.subtitle = getString(subtitleFor(mode))
        tile.updateTile()
    }

    // The same background-launch handling GestureTileService needs: a plain startActivity
    // from a TileService can be silently dropped on Android 14+ (the 3.1.1 fix), so this
    // goes through startActivityAndCollapse too, with a toast if the vendor screen is gone.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
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
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Navigation mode settings activity not found", e)
        }
    }

    companion object {
        private const val TAG = "AnyGest"

        fun component(context: Context) =
            ComponentName(context, NavigationModeTileService::class.java)

        internal fun subtitleFor(mode: NavigationMode): Int = when (mode) {
            NavigationMode.GESTURAL -> R.string.tile_navigation_gestural
            NavigationMode.THREE_BUTTON -> R.string.tile_navigation_buttons
            NavigationMode.UNKNOWN -> R.string.tile_navigation_unknown
        }
    }
}
