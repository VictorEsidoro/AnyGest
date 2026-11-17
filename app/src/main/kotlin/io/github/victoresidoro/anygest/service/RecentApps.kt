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

/**
 * The apps the user came from, newest first, built from usage events as they arrive. It is
 * fed only what happened since the last read, so a step costs a handful of events and not
 * six hours of them.
 *
 * An app counts once it stayed in front for [minStayMs]: the share sheet, a permission dialog
 * or an app that only redirects flashes by in less, and stepping back must not land on one.
 * The app in front now is always counted, however short its stay so far.
 */
internal class RecentApps(
    private val ownPackage: String,
    private val minStayMs: Long = 1_000L,
) {

    /** One resume of an activity, as the usage events report it. */
    class Resumed(val packageName: String, val timeMs: Long)

    /** The apps in order, and whether the first one is the app in front right now. */
    class Order(val apps: List<String>, val startsWithCurrent: Boolean)

    private var current: String? = null
    private var since = 0L
    private val promoted = ArrayList<String>()

    /** The time of the newest event fed, so the next read asks only for later ones; 0 when none. */
    var lastEventMs = 0L
        private set

    /** Notes that something happened at [timeMs], resume or not, so the next read starts after it. */
    fun seen(timeMs: Long) {
        if (timeMs > lastEventMs) lastEventMs = timeMs
    }

    fun resumed(event: Resumed) {
        seen(event.timeMs)
        if (event.packageName == ownPackage) return
        val before = current
        if (before == event.packageName) return
        if (before != null && event.timeMs - since >= minStayMs) promote(before)
        current = event.packageName
        since = event.timeMs
    }

    /**
     * The order, keeping only apps [launchable] says can be started: the home screen resumes
     * between every pair of apps and has no launcher entry, so it is left out. When the app in
     * front is one of those left out, the first step back is to the newest app, not the next.
     */
    fun order(launchable: (String) -> Boolean): Order {
        val front = current?.takeIf(launchable)
        val apps = buildList {
            if (front != null) add(front)
            promoted.filterTo(this) { it != front && launchable(it) }
        }
        return Order(apps, startsWithCurrent = front != null)
    }

    private fun promote(app: String) {
        promoted.remove(app)
        promoted.add(0, app)
        if (promoted.size > MAX_KEPT) promoted.removeAt(promoted.lastIndex)
    }

    private companion object {
        const val MAX_KEPT = 60
    }
}
