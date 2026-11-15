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

import android.app.ActivityManager
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Why Android says the last AnyGest process ended. A process killed without a callback leaves
 * only "unclean stop" in the notes; the system keeps the real reason (low memory, a signal,
 * an ANR, the freezer) and this reads it back at the next start.
 */
internal object ExitReasons {

    fun name(reason: Int): String = when (reason) {
        1 -> "EXIT_SELF"
        2 -> "SIGNALED"
        3 -> "LOW_MEMORY"
        4 -> "CRASH"
        5 -> "CRASH_NATIVE"
        6 -> "ANR"
        7 -> "INITIALIZATION_FAILURE"
        8 -> "PERMISSION_CHANGE"
        9 -> "EXCESSIVE_RESOURCE_USAGE"
        10 -> "USER_REQUESTED"
        11 -> "USER_STOPPED"
        12 -> "DEPENDENCY_DIED"
        13 -> "OTHER"
        14 -> "FREEZER"
        15 -> "PACKAGE_STATE_CHANGE"
        16 -> "PACKAGE_UPDATED"
        else -> "UNKNOWN($reason)"
    }

    fun describe(reason: Int, status: Int, importance: Int, at: String, description: String?): String =
        "last process exit: ${name(reason)} at $at, status $status, importance $importance" +
            (description?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")

    fun last(context: Context): String? = runCatching {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val info = manager.getHistoricalProcessExitReasons(context.packageName, 0, 1)
            .firstOrNull() ?: return null
        val at = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(info.timestamp))
        describe(info.reason, info.status, info.importance, at, info.description)
    }.getOrNull()
}
