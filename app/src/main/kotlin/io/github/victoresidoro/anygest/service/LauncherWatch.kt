// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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
import android.content.pm.PackageManager

/** What happened to an app, as far as the pause notes care. */
internal enum class PackageEvent { Installed, Updated, Removed }

/**
 * Which launcher answers Home, and which apps changed around a reset. HyperOS can send Home
 * to its own launcher while a third-party one is the default, and a launcher may warn about it;
 * the notes show whether that follows an install, an update or a change of default.
 */
internal object LauncherWatch {

    /** The package that answers Home, "chooser" when none is the default, "unknown" when it cannot be told. */
    fun homeLauncher(context: Context): String = runCatching {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }.getOrNull()?.let { if (it == "android") "chooser" else it } ?: "unknown"

    fun defaultsNote(home: String): String = "default apps changed (home launcher: $home)"
}

/**
 * The apps installed, updated or removed lately, kept in memory and told only when a reset
 * needs them: the Play Store updates dozens of apps in a minute, which as notes of their own
 * would push everything else out of the log.
 */
internal class RecentPackageChanges(
    private val maxKept: Int = 50,
    private val windowMs: Long = 30 * 60_000L,
) {
    private class Change(val event: PackageEvent, val packageName: String, val atMs: Long)

    private val changes = ArrayDeque<Change>()

    @Synchronized
    fun add(event: PackageEvent, packageName: String?, ownPackage: String, nowMs: Long) {
        if (packageName.isNullOrBlank() || packageName == ownPackage) return
        changes.addLast(Change(event, packageName, nowMs))
        while (changes.size > maxKept) changes.removeFirst()
    }

    /** The newest three within the window, then how many more there were; "none" when quiet. */
    @Synchronized
    fun describe(nowMs: Long): String {
        val recent = changes.filter { nowMs - it.atMs <= windowMs }
        if (recent.isEmpty()) return "none"
        val shown = recent.takeLast(SHOWN).reversed()
        val text = shown.joinToString(", ") {
            val verb = when (it.event) {
                PackageEvent.Installed -> "installed"
                PackageEvent.Updated -> "updated"
                PackageEvent.Removed -> "removed"
            }
            "${it.packageName} $verb ${(nowMs - it.atMs) / 60_000}m ago"
        }
        val more = recent.size - shown.size
        return if (more > 0) "$text (+$more more)" else text
    }

    private companion object {
        const val SHOWN = 3
    }
}
