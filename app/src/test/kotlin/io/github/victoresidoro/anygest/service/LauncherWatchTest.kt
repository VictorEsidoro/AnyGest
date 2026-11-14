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
import org.junit.Test

class LauncherWatchTest {

    private val own = "io.github.victoresidoro.anygest"
    private val minute = 60_000L

    @Test
    fun `a quiet phone has no recent app changes`() {
        assertEquals("none", RecentPackageChanges().describe(10 * minute))
    }

    @Test
    fun `the newest change comes first with its age and kind`() {
        val recent = RecentPackageChanges()
        recent.add(PackageEvent.Installed, "com.a", own, 0)
        recent.add(PackageEvent.Updated, "com.b", own, 2 * minute)
        assertEquals("com.b updated 3m ago, com.a installed 5m ago", recent.describe(5 * minute))
    }

    @Test
    fun `a batch of updates is summed up instead of listed`() {
        val recent = RecentPackageChanges()
        repeat(25) { recent.add(PackageEvent.Updated, "com.app$it", own, it * 1_000L) }
        assertEquals(
            "com.app24 updated 0m ago, com.app23 updated 0m ago, com.app22 updated 0m ago (+22 more)",
            recent.describe(25_000L),
        )
    }

    @Test
    fun `changes older than the window are left out`() {
        val recent = RecentPackageChanges()
        recent.add(PackageEvent.Removed, "com.old", own, 0)
        recent.add(PackageEvent.Removed, "com.new", own, 40 * minute)
        assertEquals("com.new removed 0m ago", recent.describe(40 * minute))
        assertEquals("none", recent.describe(80 * minute))
    }

    @Test
    fun `AnyGest itself and nameless events are not kept`() {
        val recent = RecentPackageChanges()
        recent.add(PackageEvent.Updated, own, own, 0)
        recent.add(PackageEvent.Installed, null, own, 0)
        recent.add(PackageEvent.Installed, " ", own, 0)
        assertEquals("none", recent.describe(0))
    }

    @Test
    fun `a change of default apps names the launcher`() {
        assertEquals("default apps changed (home launcher: chooser)", LauncherWatch.defaultsNote("chooser"))
    }
}
