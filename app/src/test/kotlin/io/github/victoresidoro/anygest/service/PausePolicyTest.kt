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

package io.github.victoresidoro.anygest.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PausePolicyTest {

    private val apps = setOf("com.example.bank")

    private fun pause(
        packageName: String = "com.example.bank",
        enabled: Boolean = true,
        available: Boolean = true,
        alreadyPaused: Boolean = false,
    ) = PausePolicy.shouldPause(packageName, enabled, apps, available, alreadyPaused)

    @Test
    fun `an app on the list pauses the gestures`() {
        assertTrue(pause())
    }

    @Test
    fun `an app off the list does not`() {
        assertFalse(pause(packageName = "com.android.chrome"))
    }

    @Test
    fun `nothing pauses without the feature, the grant or while already paused`() {
        assertFalse(pause(enabled = false))
        assertFalse(pause(available = false))
        assertFalse(pause(alreadyPaused = true))
    }

    @Test
    fun `leaving means another app is in front`() {
        assertTrue(PausePolicy.frontIsOutside("com.teslacoilsw.launcher", apps))
        assertFalse(PausePolicy.frontIsOutside("com.example.bank", apps))
    }

    @Test
    fun `an unknown front app or a system overlay is not leaving`() {
        assertFalse(PausePolicy.frontIsOutside(null, apps))
        assertFalse(PausePolicy.frontIsOutside("com.android.systemui", apps))
    }
}
