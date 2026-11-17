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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsTest {

    private val all: (String) -> Boolean = { true }

    private fun RecentApps.at(timeMs: Long, packageName: String) =
        resumed(RecentApps.Resumed(packageName, timeMs))

    @Test
    fun `the newest app comes first and the app in front is counted`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(10_000, "chat")
        apps.at(20_000, "maps")

        val order = apps.order(all)

        assertEquals(listOf("maps", "chat", "mail"), order.apps)
        assertTrue(order.startsWithCurrent)
    }

    @Test
    fun `an app that flashed by is not counted`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(10_000, "intentresolver")
        apps.at(10_400, "chat")
        apps.at(30_000, "maps")

        assertEquals(listOf("maps", "chat", "mail"), apps.order(all).apps)
    }

    @Test
    fun `the app in front counts even when it only just arrived`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(10_000, "chat")

        assertEquals(listOf("chat", "mail"), apps.order(all).apps)
    }

    @Test
    fun `another activity of the same app does not restart its stay`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(5_000, "chat")
        apps.at(5_300, "chat")
        apps.at(6_200, "maps")

        assertEquals(listOf("maps", "chat", "mail"), apps.order(all).apps)
    }

    @Test
    fun `an app seen again moves to the front instead of appearing twice`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(5_000, "chat")
        apps.at(10_000, "mail")
        apps.at(15_000, "maps")

        assertEquals(listOf("maps", "mail", "chat"), apps.order(all).apps)
    }

    @Test
    fun `our own app is never in the order`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(5_000, "own")
        apps.at(9_000, "chat")

        assertEquals(listOf("chat", "mail"), apps.order(all).apps)
    }

    @Test
    fun `an app that cannot be launched is left out, and the first step then goes to the newest`() {
        val apps = RecentApps("own")
        apps.at(0, "mail")
        apps.at(5_000, "chat")
        apps.at(9_000, "home")

        val order = apps.order { it != "home" }

        assertEquals(listOf("chat", "mail"), order.apps)
        assertFalse(order.startsWithCurrent)
    }

    @Test
    fun `the time of the newest event is remembered for the next read`() {
        val apps = RecentApps("own")
        apps.at(5_000, "mail")
        apps.seen(8_000)
        apps.seen(7_000)

        assertEquals(8_000L, apps.lastEventMs)
    }

    @Test
    fun `an empty tracker has no apps and no event time`() {
        val apps = RecentApps("own")

        assertTrue(apps.order(all).apps.isEmpty())
        assertEquals(0L, apps.lastEventMs)
    }
}
