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

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAppsTest {

    private val home = "MAIN HOME -> com.teslacoilsw.launcher"
    private val poco = "MAIN HOME -> com.mi.android.globallauncher"

    @Test
    fun `equal snapshots moved nothing`() {
        assertEquals("none", DefaultApps.changes(setOf(home), setOf(home)))
    }

    @Test
    fun `a swapped default shows what left and what arrived`() {
        assertEquals("+$poco, -$home", DefaultApps.changes(setOf(home), setOf(poco)))
    }

    @Test
    fun `a default added or cleared alone is reported`() {
        assertEquals("+$home", DefaultApps.changes(emptySet(), setOf(home)))
        assertEquals("-$home", DefaultApps.changes(setOf(home), emptySet()))
    }

    @Test
    fun `a long list is cut after the shown lines with a count`() {
        val after = (1..6).map { "VIEW http -> pkg$it" }.toSet()
        assertEquals(
            "+VIEW http -> pkg1, +VIEW http -> pkg2 (+4 more)",
            DefaultApps.changes(emptySet(), after, shown = 2),
        )
    }

    @Test
    fun `action and category names lose their prefix`() {
        assertEquals("HOME", DefaultApps.short("android.intent.category.HOME"))
        assertEquals("MAIN", DefaultApps.short("android.intent.action.MAIN"))
    }
}
