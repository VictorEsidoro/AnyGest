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

/** The decisions behind pausing for an app, apart from the framework so a JVM test can run them. */
internal object PausePolicy {

    /** Windows of these appear over any app (shade, volume), so they never say the user left. */
    private val TRANSIENT_FRONT = setOf("com.android.systemui")

    fun shouldPause(
        packageName: String,
        featureEnabled: Boolean,
        apps: Set<String>,
        available: Boolean,
        alreadyPaused: Boolean,
    ): Boolean = featureEnabled && available && !alreadyPaused && packageName in apps

    /** True once the app in front is known and is none of the paused ones. */
    fun frontIsOutside(front: String?, apps: Set<String>): Boolean =
        front != null && front !in apps && front !in TRANSIENT_FRONT
}
