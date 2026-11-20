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
 * What [GestureTracker] tells the caller after each event. The tracker never touches a clock
 * or a view: the caller runs the timers it asks for and reports back when they elapse.
 */
sealed interface TrackerAction {
    /** Nothing for the caller to do. */
    data object None : TrackerAction

    /** The swipe passed its distance. [scheduleHoldMs] is the Recents timer to start, if any. */
    data class Armed(val scheduleHoldMs: Long?) : TrackerAction

    /** The finger moved while armed: start the Recents timer again from now. */
    data class RearmHold(val delayMs: Long) : TrackerAction

    /** Start the long-press timer and report it through `onLongPressElapsed`. */
    data class ScheduleLongPress(val delayMs: Long) : TrackerAction

    /** Perform [action]. */
    data class Fire(val action: GestureAction, val edge: ScreenEdge) : TrackerAction

    /** A Back swipe came back inside its distance while the finger is still down. */
    data object Disarmed : TrackerAction

    /** A Back swipe was released inside its distance: nothing fires and nothing is replayed. */
    data object Reverted : TrackerAction

    /** The touch was not a gesture; the caller may hand it to the app underneath. */
    data class Unused(val reason: RejectionReason) : TrackerAction
}

/** Why a touch was not a gesture, for the log. */
enum class RejectionReason {
    NO_MOVEMENT,
    INSUFFICIENT_MOVEMENT,
    WRONG_DIRECTION,
    TOO_SLOW_TO_ARM,
    OFF_AXIS,
}
