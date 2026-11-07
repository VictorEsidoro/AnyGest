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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdbCommandsTest {

    @Test
    fun `battery covers every package once in a single line`() {
        val line = AdbCommands.battery(listOf("a.b", "c.d", "a.b"))
        assertEquals(
            "dumpsys deviceidle whitelist +a.b; cmd appops set a.b RUN_ANY_IN_BACKGROUND allow; " +
                "dumpsys deviceidle whitelist +c.d; cmd appops set c.d RUN_ANY_IN_BACKGROUND allow",
            line,
        )
    }

    @Test
    fun `usage access and restricted settings name the app`() {
        assertEquals("cmd appops set a.b GET_USAGE_STATS allow", AdbCommands.usageAccess("a.b"))
        assertEquals(
            "cmd appops set a.b ACCESS_RESTRICTED_SETTINGS allow",
            AdbCommands.restrictedSettings("a.b"),
        )
    }

    @Test
    fun `only plain package names are accepted`() {
        assertEquals("com.teslacoilsw.launcher", AdbCommands.safePackage("com.teslacoilsw.launcher"))
        assertNull(AdbCommands.safePackage("android"))
        assertNull(AdbCommands.safePackage("a.b; rm -rf /"))
        assertNull(AdbCommands.safePackage(null))
    }
}
