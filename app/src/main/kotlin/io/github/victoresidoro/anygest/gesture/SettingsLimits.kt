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
 * The range each adjustable setting may take. The sliders and the settings importer both read
 * these, so a value a slider cannot reach is never accepted from a file either.
 */
object SettingsLimits {
    val EDGE_WIDTH_DP = 8f..64f
    val SIDE_DISTANCE_DP = 8f..80f
    val BOTTOM_DISTANCE_DP = 4f..60f
    val RECENTS_HOLD_MS = 50L..500L
    val MAX_ARM_DURATION_MS = 300L..3000L
    val CIRCLE_TO_SEARCH_HOLD_MS = 250L..900L
    val HOLD_STILLNESS_DP = 1f..100f
    val OFF_AXIS_TOLERANCE = 0.1f..5f
}
