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

package io.github.victoresidoro.anygest.gesture

/**
 * The tracker's answer to one touch event. The tracker owns no clock and no view: when an answer
 * asks for a timer, the caller runs it and tells the tracker once it has elapsed.
 */
sealed interface TrackerAction {
    /** Nothing to do. */
    data object None : TrackerAction

    /** The swipe went far enough. Start the Recents timer if [scheduleHoldMs] is set. */
    data class Committed(val scheduleHoldMs: Long?) : TrackerAction

    /** The finger moved while armed, so the Recents timer starts over with [delayMs]. */
    data class RestartHold(val delayMs: Long) : TrackerAction

    /** Start the long-press timer; its end is reported through the long-press callback. */
    data class StartLongPress(val delayMs: Long) : TrackerAction

    /** Carry out [action] for the swipe that came from [edge]. */
    data class Perform(val action: GestureAction, val edge: ScreenEdge) : TrackerAction

    /** A Back swipe moved back inside its distance with the finger still down. */
    data object Uncommitted : TrackerAction

    /** A Back swipe lifted inside its distance: nothing fires and nothing is replayed. */
    data object Cancelled : TrackerAction

    /** Not a gesture. The caller may hand the touch to the app underneath. */
    data class Passed(val reason: RejectionReason) : TrackerAction
}

/** The reason a touch did not become a gesture, as the log spells it. */
enum class RejectionReason {
    NO_MOVEMENT,
    INSUFFICIENT_MOVEMENT,
    WRONG_DIRECTION,
    TOO_SLOW_TO_ARM,
    OFF_AXIS,
}
