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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.victoresidoro.anygest.gesture.EdgeAnimation
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.HapticStrength
import io.github.victoresidoro.anygest.gesture.SettingsLimits
import io.github.victoresidoro.anygest.gesture.SidePosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// A damaged file (a power cut in the middle of a write) reads as empty, so the defaults apply
// instead of every reader throwing until the app data is cleared. The idea comes from
// HyperGesture, by Rohith-E-S.
private val Context.gestureStore: DataStore<Preferences> by preferencesDataStore(
    name = "gesture_configuration",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Saves [GestureConfiguration]. A key that was never written reads as the class's own default. */
class GestureSettingsRepository(private val context: Context) {

    val configuration: Flow<GestureConfiguration> = context.gestureStore.data.map(::decode)

    suspend fun read(): GestureConfiguration = configuration.first()

    suspend fun update(change: (GestureConfiguration) -> GestureConfiguration) {
        val next = change(read())
        context.gestureStore.edit { it.encode(next) }
    }

    private fun MutablePreferences.encode(c: GestureConfiguration) {
        this[EDGE_WIDTH_DP] = c.edgeWidthDp
        this[SIDE_MIN_DISTANCE_DP] = c.sideMinimumSwipeDistanceDp
        this[BOTTOM_MIN_DISTANCE_DP] = c.bottomMinimumSwipeDistanceDp
        this[MAX_ARM_DURATION_MS] = c.maximumArmDurationMs
        this[RECENTS_HOLD_MS] = c.recentsHoldMs
        this[HOLD_STILLNESS_DP] = c.holdStillnessDp
        this[OFF_AXIS_TOLERANCE] = c.offAxisToleranceRatio
        this[APP_SWITCH] = c.appSwitchEnabled
        this[LEFT_EDGE_BACK] = c.leftEdgeBackEnabled
        this[RIGHT_EDGE_BACK] = c.rightEdgeBackEnabled
        this[HAPTIC_FEEDBACK] = c.hapticFeedbackEnabled
        this[HAPTIC_STRENGTH] = c.hapticStrength.name
        this[HAPTIC_CONFIRM] = c.hapticConfirmEnabled
        this[LANDSCAPE_CONFIRM] = c.landscapeConfirmEnabled
        this[EDGE_ANIMATION] = c.edgeAnimation.name
        this[SIDE_LENGTH_PERCENT] = c.sideLengthPercent
        this[SIDE_POSITION] = c.sidePosition.name
        this[CIRCLE_TO_SEARCH] = c.circleToSearchEnabled
        this[CIRCLE_TO_SEARCH_HOLD_MS] = c.circleToSearchHoldMs
        this[CIRCLE_TO_SEARCH_SYSTEM_TIMING] = c.circleToSearchFollowsSystemTiming
        this[PAUSE_IN_APPS] = c.pauseInAppsEnabled
        this[PAUSE_APPS] = c.pauseApps
        val home = c.alternateHome
        if (home == null) remove(ALTERNATE_HOME) else this[ALTERNATE_HOME] = home
    }

    private fun decode(saved: Preferences): GestureConfiguration {
        val d = GestureConfiguration()
        return GestureConfiguration(
            edgeWidthDp = saved[EDGE_WIDTH_DP] ?: d.edgeWidthDp,
            sideMinimumSwipeDistanceDp = saved[SIDE_MIN_DISTANCE_DP] ?: d.sideMinimumSwipeDistanceDp,
            bottomMinimumSwipeDistanceDp = saved[BOTTOM_MIN_DISTANCE_DP] ?: d.bottomMinimumSwipeDistanceDp,
            maximumArmDurationMs = saved[MAX_ARM_DURATION_MS] ?: d.maximumArmDurationMs,
            recentsHoldMs = testOverride(RECENTS_HOLD_OVERRIDE)?.toLongOrNull()
                ?: saved[RECENTS_HOLD_MS] ?: d.recentsHoldMs,
            holdStillnessDp = saved[HOLD_STILLNESS_DP] ?: d.holdStillnessDp,
            offAxisToleranceRatio = saved[OFF_AXIS_TOLERANCE] ?: d.offAxisToleranceRatio,
            appSwitchEnabled = testOverride(APP_SWITCH_OVERRIDE)?.let(::isTruthy)
                ?: saved[APP_SWITCH] ?: d.appSwitchEnabled,
            leftEdgeBackEnabled = saved[LEFT_EDGE_BACK] ?: d.leftEdgeBackEnabled,
            rightEdgeBackEnabled = saved[RIGHT_EDGE_BACK] ?: d.rightEdgeBackEnabled,
            hapticFeedbackEnabled = saved[HAPTIC_FEEDBACK] ?: d.hapticFeedbackEnabled,
            hapticStrength = namedIn(HapticStrength.entries, saved[HAPTIC_STRENGTH]) ?: d.hapticStrength,
            hapticConfirmEnabled = saved[HAPTIC_CONFIRM] ?: d.hapticConfirmEnabled,
            landscapeConfirmEnabled = saved[LANDSCAPE_CONFIRM] ?: d.landscapeConfirmEnabled,
            edgeAnimation = namedIn(EdgeAnimation.entries, saved[EDGE_ANIMATION]) ?: d.edgeAnimation,
            sideLengthPercent = (saved[SIDE_LENGTH_PERCENT] ?: d.sideLengthPercent)
                .coerceIn(SettingsLimits.SIDE_LENGTH_PERCENT),
            sidePosition = namedIn(SidePosition.entries, saved[SIDE_POSITION]) ?: d.sidePosition,
            circleToSearchEnabled = saved[CIRCLE_TO_SEARCH] ?: d.circleToSearchEnabled,
            circleToSearchHoldMs = saved[CIRCLE_TO_SEARCH_HOLD_MS] ?: d.circleToSearchHoldMs,
            circleToSearchFollowsSystemTiming =
                saved[CIRCLE_TO_SEARCH_SYSTEM_TIMING] ?: d.circleToSearchFollowsSystemTiming,
            pauseInAppsEnabled = saved[PAUSE_IN_APPS] ?: d.pauseInAppsEnabled,
            // A saved empty set is a list the user emptied, so it is not the same as no list.
            pauseApps = saved[PAUSE_APPS] ?: d.pauseApps,
            alternateHome = saved[ALTERNATE_HOME] ?: d.alternateHome,
        )
    }

    /** An unknown or missing name gives null, so the caller falls back instead of failing. */
    private fun <T : Enum<T>> namedIn(values: List<T>, name: String?): T? =
        values.firstOrNull { it.name == name }

    private fun isTruthy(text: String) = text == "1" || text.equals("true", ignoreCase = true)

    /**
     * A hook for the emulator suite, which cannot tap a Compose switch or deliver a touch stream
     * without gaps (a stalled input would read a swipe as a hold). It reads a global setting, and
     * only in a debuggable build, so a release APK never looks.
     */
    private fun testOverride(name: String): String? {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return null
        return runCatching { Settings.Global.getString(context.contentResolver, name) }.getOrNull()
    }

    private companion object {
        const val RECENTS_HOLD_OVERRIDE = "anygest_recents_hold_ms"
        const val APP_SWITCH_OVERRIDE = "anygest_app_switch"

        // The key names are what is on disk. They must not change.
        val EDGE_WIDTH_DP = floatPreferencesKey("edge_width_dp")
        val SIDE_MIN_DISTANCE_DP = floatPreferencesKey("side_minimum_swipe_distance_dp")
        val BOTTOM_MIN_DISTANCE_DP = floatPreferencesKey("bottom_minimum_swipe_distance_dp")
        val MAX_ARM_DURATION_MS = longPreferencesKey("maximum_arm_duration_ms")
        val RECENTS_HOLD_MS = longPreferencesKey("recents_hold_ms")
        val HOLD_STILLNESS_DP = floatPreferencesKey("hold_stillness_dp")
        val OFF_AXIS_TOLERANCE = floatPreferencesKey("off_axis_tolerance_ratio")
        val APP_SWITCH = booleanPreferencesKey("app_switch_enabled")
        val LEFT_EDGE_BACK = booleanPreferencesKey("left_edge_back_enabled")
        val RIGHT_EDGE_BACK = booleanPreferencesKey("right_edge_back_enabled")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")
        val HAPTIC_CONFIRM = booleanPreferencesKey("haptic_confirm_enabled")
        val LANDSCAPE_CONFIRM = booleanPreferencesKey("landscape_confirm_enabled")
        val EDGE_ANIMATION = stringPreferencesKey("edge_animation")
        val SIDE_LENGTH_PERCENT = intPreferencesKey("side_length_percent")
        val SIDE_POSITION = stringPreferencesKey("side_position")
        val CIRCLE_TO_SEARCH = booleanPreferencesKey("circle_to_search_enabled")
        val CIRCLE_TO_SEARCH_HOLD_MS = longPreferencesKey("circle_to_search_hold_ms")
        val CIRCLE_TO_SEARCH_SYSTEM_TIMING = booleanPreferencesKey("circle_to_search_follows_system_timing")
        val PAUSE_IN_APPS = booleanPreferencesKey("pause_in_apps_enabled")
        val PAUSE_APPS = stringSetPreferencesKey("pause_apps")
        val ALTERNATE_HOME = stringPreferencesKey("alternate_home")
    }
}
