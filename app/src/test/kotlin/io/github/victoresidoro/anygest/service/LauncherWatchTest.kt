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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherWatchTest {

    private val own = "io.github.victoresidoro.anygest"

    @Test
    fun `an installed app is noted with the launcher that answers Home`() {
        assertEquals(
            "app installed: com.example.app (home launcher: com.teslacoilsw.launcher)",
            LauncherWatch.packageNote(PackageEvent.Installed, "com.example.app", own, "com.teslacoilsw.launcher"),
        )
    }

    @Test
    fun `updates and removals are told apart`() {
        assertEquals(
            "app updated: a.b (home launcher: x)",
            LauncherWatch.packageNote(PackageEvent.Updated, "a.b", own, "x"),
        )
        assertEquals(
            "app removed: a.b (home launcher: x)",
            LauncherWatch.packageNote(PackageEvent.Removed, "a.b", own, "x"),
        )
    }

    @Test
    fun `AnyGest itself and nameless events say nothing`() {
        assertNull(LauncherWatch.packageNote(PackageEvent.Updated, own, own, "x"))
        assertNull(LauncherWatch.packageNote(PackageEvent.Installed, null, own, "x"))
        assertNull(LauncherWatch.packageNote(PackageEvent.Installed, " ", own, "x"))
    }

    @Test
    fun `a change of default apps names the launcher`() {
        assertEquals("default apps changed (home launcher: chooser)", LauncherWatch.defaultsNote("chooser"))
    }
}
