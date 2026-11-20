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

package io.github.victoresidoro.anygest.gesture

/**
 * What a gesture asks the system to do. [id] is what logs and the diagnostics print: it is
 * spelled out because a release build is minified and nothing else about the value is a
 * stable name.
 */
enum class GestureAction(val id: String) {
    Back("Back"),
    Home("Home"),
    Recents("Recents"),

    /** One app back in the order they were used. */
    PreviousApp("PreviousApp"),

    /** One app forward again, after a [PreviousApp]. */
    NextApp("NextApp"),

    /** Google's Circle to Search, started the way the handle's own long press starts it. */
    CircleToSearch("CircleToSearch"),
}

/** The three strips AnyGest puts on the screen. */
enum class ScreenEdge { LEFT, RIGHT, BOTTOM }

/** Where the side strips sit when they are shorter than the edge. */
enum class SidePosition { Top, Center, Bottom }

/** One point of a touch, in display pixels, with its event time. */
data class TouchSample(val xPx: Float, val yPx: Float, val timestampMs: Long)
