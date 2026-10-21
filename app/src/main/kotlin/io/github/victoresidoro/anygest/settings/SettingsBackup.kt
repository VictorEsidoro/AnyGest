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

package io.github.victoresidoro.anygest.settings

import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.HapticStrength
import io.github.victoresidoro.anygest.gesture.SettingsLimits
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

/**
 * Settings as a small `key=value` text file the user can keep or move to another phone.
 * Plain `java.util.Properties`, so there is no extra dependency and it runs in a JVM test.
 * Nothing but the gesture settings goes in: no device data, no identifiers.
 */
object SettingsBackup {

    const val FILE_NAME = "anygest-settings.txt"
    const val MIME_TYPE = "text/plain"

    private const val FORMAT_KEY = "format"
    private const val FORMAT = "1"

    /** [ignored] counts lines present in the file that were unknown or out of range. */
    data class Decoded(val configuration: GestureConfiguration, val ignored: Int)

    fun encode(configuration: GestureConfiguration): String {
        val properties = Properties()
        properties[FORMAT_KEY] = FORMAT
        properties["edge_width_dp"] = configuration.edgeWidthDp.toString()
        properties["side_minimum_swipe_distance_dp"] = configuration.sideMinimumSwipeDistanceDp.toString()
        properties["bottom_minimum_swipe_distance_dp"] = configuration.bottomMinimumSwipeDistanceDp.toString()
        properties["maximum_arm_duration_ms"] = configuration.maximumArmDurationMs.toString()
        properties["recents_hold_ms"] = configuration.recentsHoldMs.toString()
        properties["hold_stillness_dp"] = configuration.holdStillnessDp.toString()
        properties["off_axis_tolerance_ratio"] = configuration.offAxisToleranceRatio.toString()
        properties["app_switch_enabled"] = configuration.appSwitchEnabled.toString()
        properties["left_edge_back_enabled"] = configuration.leftEdgeBackEnabled.toString()
        properties["right_edge_back_enabled"] = configuration.rightEdgeBackEnabled.toString()
        properties["haptic_feedback_enabled"] = configuration.hapticFeedbackEnabled.toString()
        properties["haptic_strength"] = configuration.hapticStrength.name
        properties["circle_to_search_enabled"] = configuration.circleToSearchEnabled.toString()
        properties["circle_to_search_hold_ms"] = configuration.circleToSearchHoldMs.toString()
        properties["circle_to_search_follows_system_timing"] =
            configuration.circleToSearchFollowsSystemTiming.toString()
        val writer = StringWriter()
        properties.store(writer, "AnyGest settings")
        return writer.toString()
    }

    /**
     * Null when the text is not an AnyGest settings file at all. Otherwise every valid line is
     * applied over the defaults, and a bad one is skipped and counted instead of failing the lot.
     */
    fun decode(text: String): Decoded? {
        val properties = Properties()
        val loaded = runCatching { properties.load(StringReader(text)) }.isSuccess
        if (!loaded || properties.getProperty(FORMAT_KEY) != FORMAT) return null

        var ignored = 0
        var result = GestureConfiguration()
        fun <T> read(key: String, parse: (String) -> T?, apply: (GestureConfiguration, T) -> GestureConfiguration) {
            val raw = properties.getProperty(key) ?: return
            val value = parse(raw)
            if (value == null) ignored++ else result = apply(result, value)
        }
        fun float(range: ClosedFloatingPointRange<Float>) = { raw: String ->
            raw.toFloatOrNull()?.takeIf { it.isFinite() && it in range }
        }
        fun long(range: LongRange) = { raw: String -> raw.toLongOrNull()?.takeIf { it in range } }
        val boolean = { raw: String ->
            when (raw) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }

        read("edge_width_dp", float(SettingsLimits.EDGE_WIDTH_DP)) { c, v -> c.copy(edgeWidthDp = v) }
        read("side_minimum_swipe_distance_dp", float(SettingsLimits.SIDE_DISTANCE_DP)) { c, v ->
            c.copy(sideMinimumSwipeDistanceDp = v)
        }
        read("bottom_minimum_swipe_distance_dp", float(SettingsLimits.BOTTOM_DISTANCE_DP)) { c, v ->
            c.copy(bottomMinimumSwipeDistanceDp = v)
        }
        read("maximum_arm_duration_ms", long(SettingsLimits.MAX_ARM_DURATION_MS)) { c, v ->
            c.copy(maximumArmDurationMs = v)
        }
        read("recents_hold_ms", long(SettingsLimits.RECENTS_HOLD_MS)) { c, v -> c.copy(recentsHoldMs = v) }
        read("hold_stillness_dp", float(SettingsLimits.HOLD_STILLNESS_DP)) { c, v -> c.copy(holdStillnessDp = v) }
        read("off_axis_tolerance_ratio", float(SettingsLimits.OFF_AXIS_TOLERANCE)) { c, v ->
            c.copy(offAxisToleranceRatio = v)
        }
        read("app_switch_enabled", boolean) { c, v -> c.copy(appSwitchEnabled = v) }
        read("left_edge_back_enabled", boolean) { c, v -> c.copy(leftEdgeBackEnabled = v) }
        read("right_edge_back_enabled", boolean) { c, v -> c.copy(rightEdgeBackEnabled = v) }
        read("haptic_feedback_enabled", boolean) { c, v -> c.copy(hapticFeedbackEnabled = v) }
        read("haptic_strength", { raw -> HapticStrength.entries.firstOrNull { it.name == raw } }) { c, v ->
            c.copy(hapticStrength = v)
        }
        read("circle_to_search_enabled", boolean) { c, v -> c.copy(circleToSearchEnabled = v) }
        read("circle_to_search_hold_ms", long(SettingsLimits.CIRCLE_TO_SEARCH_HOLD_MS)) { c, v ->
            c.copy(circleToSearchHoldMs = v)
        }
        read("circle_to_search_follows_system_timing", boolean) { c, v ->
            c.copy(circleToSearchFollowsSystemTiming = v)
        }
        return Decoded(result, ignored)
    }
}
