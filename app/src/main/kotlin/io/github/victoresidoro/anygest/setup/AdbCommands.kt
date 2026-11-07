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

/**
 * The optional adb commands the app shows for the user to copy. The app never runs them: the
 * permissions behind them belong to the adb shell, not to `pm grant`. Each has an equivalent
 * on the Android screens, so nobody has to use adb.
 */
internal object AdbCommands {

    /** Out of battery optimisation and allowed to run in the background, for each package. */
    fun battery(packages: List<String>): String = packages.distinct().joinToString("; ") {
        "dumpsys deviceidle whitelist +$it; cmd appops set $it RUN_ANY_IN_BACKGROUND allow"
    }

    /** The same switch as "Usage access" in Settings. */
    fun usageAccess(packageName: String): String =
        "cmd appops set $packageName GET_USAGE_STATS allow"

    /** The same as "Allow restricted settings" in the app's info screen. */
    fun restrictedSettings(packageName: String): String =
        "cmd appops set $packageName ACCESS_RESTRICTED_SETTINGS allow"

    /** A package name the shell would take as one word, or null so nothing odd is pasted. */
    fun safePackage(name: String?): String? =
        name?.takeIf { Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(it) }
}
