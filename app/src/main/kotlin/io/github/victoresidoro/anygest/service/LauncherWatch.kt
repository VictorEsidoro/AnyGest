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
 * Which launcher answers Home, and what app changes happened around it. HyperOS can send Home
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

    /** The pause note for an app change, or null when it is AnyGest itself or says nothing. */
    fun packageNote(event: PackageEvent, packageName: String?, ownPackage: String, home: String): String? {
        if (packageName.isNullOrBlank() || packageName == ownPackage) return null
        val what = when (event) {
            PackageEvent.Installed -> "app installed"
            PackageEvent.Updated -> "app updated"
            PackageEvent.Removed -> "app removed"
        }
        return "$what: $packageName (home launcher: $home)"
    }

    fun defaultsNote(home: String): String = "default apps changed (home launcher: $home)"
}
