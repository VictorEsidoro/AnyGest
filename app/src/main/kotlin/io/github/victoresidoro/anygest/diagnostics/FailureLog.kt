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

package io.github.victoresidoro.anygest.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Keeps the last failure's stack trace in app storage, since logcat cannot be trusted on the
 * target ROM: it drops debug and info lines, and a crash can be invisible from a computer.
 * A debug build can read it back with
 * `adb shell run-as io.github.victoresidoro.anygest cat files/last_failure.txt`, and the
 * diagnostics screen shows it in any build.
 */
object FailureLog {

    private const val TAG = "AnyGest"
    private const val FILE_NAME = "last_failure.txt"
    private const val LIMIT_CHARS = 16 * 1024

    /** Only the latest failure is kept; a new one replaces the one before. */
    fun record(context: Context, label: String, throwable: Throwable) {
        Log.e(TAG, "FAILURE [$label]", throwable)
        val record = listOf(
            "label: $label",
            "device: ${Build.MANUFACTURER} ${Build.MODEL}",
            "android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "---",
            Log.getStackTraceString(throwable),
        ).joinToString("\n")
        runCatching { fileIn(context).writeText(record.take(LIMIT_CHARS)) }
            .onFailure { Log.e(TAG, "Could not persist the failure record", it) }
    }

    fun read(context: Context): String? =
        runCatching { fileIn(context).takeIf(File::exists)?.readText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        runCatching { fileIn(context).delete() }
    }

    private fun fileIn(context: Context) = File(context.filesDir, FILE_NAME)
}
