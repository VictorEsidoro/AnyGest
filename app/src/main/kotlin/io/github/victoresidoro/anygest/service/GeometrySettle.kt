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
 * Reads the screen geometry again for a short while after a signal that announces it, because
 * the navigation inset can land after the signal does: right after the system switches the
 * navigation mode, a read still returns the old bar. The reads stop as soon as two in a row
 * agree, so a quiet phone pays one or two reads and a restless one at most three.
 */
internal class GeometrySettle(private val delaysMs: LongArray = longArrayOf(300L, 700L, 1_500L)) {

    private var step = 0
    private var previous: ScreenGeometry? = null

    /** Starts a series and says how long to wait before its first read. */
    fun begin(): Long {
        step = 0
        previous = null
        return delaysMs[0]
    }

    /** Takes one read; returns the wait before the next one, or null when the series is over. */
    fun next(read: ScreenGeometry): Long? {
        val agrees = read == previous
        previous = read
        step++
        return if (agrees || step >= delaysMs.size) null else delaysMs[step]
    }
}

/** What differs between two geometries, in words for the notes; null when nothing does. */
internal fun ScreenGeometry.describeChangeTo(new: ScreenGeometry): String? {
    val changes = buildList {
        if (widthPx != new.widthPx) add("width $widthPx->${new.widthPx}")
        if (heightPx != new.heightPx) add("height $heightPx->${new.heightPx}")
        if (navLeftPx != new.navLeftPx) add("left inset $navLeftPx->${new.navLeftPx}")
        if (navRightPx != new.navRightPx) add("right inset $navRightPx->${new.navRightPx}")
        if (navBottomPx != new.navBottomPx) add("bottom inset $navBottomPx->${new.navBottomPx}")
    }
    return changes.takeIf { it.isNotEmpty() }?.joinToString(", ", postfix = " px")
}
