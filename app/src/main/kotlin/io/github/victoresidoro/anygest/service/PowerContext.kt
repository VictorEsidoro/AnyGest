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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock

/**
 * What the phone was doing around a service fault: screen, charger, doze and how long this
 * process has been alive. A fault right after a long screen-off on the charger points at the
 * system freezing or stopping the app, which the fault's own line cannot say.
 */
internal object PowerContext {

    /** [SystemClock.elapsedRealtime] of the last screen-off this process saw, or null. */
    @Volatile
    private var screenOffAtMs: Long? = null

    fun screenWentOff(nowMs: Long) {
        screenOffAtMs = nowMs
    }

    /** How long the screen had been off, or null when this process did not see it go off. */
    fun screenWentOn(nowMs: Long): Long? {
        val off = screenOffAtMs ?: return null
        screenOffAtMs = null
        return nowMs - off
    }

    fun describe(context: Context, nowMs: Long = SystemClock.elapsedRealtime()): String {
        val power = context.getSystemService(PowerManager::class.java)
        val parts = mutableListOf<String>()
        parts += if (power?.isInteractive == false) "screen off" else "screen on"
        screenOffAtMs?.let { parts += "off for ${duration(nowMs - it)}" }
        parts += "charger ${charger(context)}"
        parts += "idle=${power?.isDeviceIdleMode}"
        parts += "saver=${power?.isPowerSaveMode}"
        parts += "process up ${duration(nowMs - Process.getStartElapsedRealtime())}"
        return parts.joinToString(", ")
    }

    private fun charger(context: Context): String {
        val battery = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return "unknown"
        return when (battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            0 -> "no"
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "yes"
        }
    }

    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0L)) / 1000L
        val h = total / 3600L
        val m = (total % 3600L) / 60L
        val s = total % 60L
        return when {
            h > 0 -> "${h}h${m.toString().padStart(2, '0')}m"
            m > 0 -> "${m}m${s.toString().padStart(2, '0')}s"
            else -> "${s}s"
        }
    }
}
