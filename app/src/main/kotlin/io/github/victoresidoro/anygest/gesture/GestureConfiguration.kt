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
 * Every setting the gesture code reads, with the defaults a fresh install gets. Distances
 * are in dp and converted with the display density where they are used.
 */
data class GestureConfiguration(
    val edgeWidthDp: Float = 20f,
    val sideMinimumSwipeDistanceDp: Float = 24f,
    /**
     * Shorter than on the sides: a swipe that starts on the system's own navigation area
     * reaches the bottom strip late, with part of its travel already used up.
     */
    val bottomMinimumSwipeDistanceDp: Float = 10f,
    /**
     * Time a touch has to pass its distance. Once it has, it may be held as long as the user
     * likes: swipe and hold is how Recents is reached.
     */
    val maximumArmDurationMs: Long = 1000L,
    val recentsHoldMs: Long = 100L,
    /** Movement allowed while holding for Recents before the hold starts over. */
    val holdStillnessDp: Float = 12f,
    /** Sideways movement allowed per unit of travel: 1.0 accepts up to 45 degrees off the axis. */
    val offAxisToleranceRatio: Float = 1.0f,
    /** Off until the user turns it on, since it needs usage access granted by hand. */
    val appSwitchEnabled: Boolean = false,
    val leftEdgeBackEnabled: Boolean = true,
    val rightEdgeBackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    /** Read only while [hapticFeedbackEnabled] is on. */
    val hapticStrength: HapticStrength = HapticStrength.Normal,
    /** A second, firmer click when a gesture fires. Read only while [hapticFeedbackEnabled] is on. */
    val hapticConfirmEnabled: Boolean = false,
    /** HyperOS asks for a second swipe at the bottom of a landscape app; AnyGest does the same. */
    val landscapeConfirmEnabled: Boolean = true,
    /** The shape drawn under the finger while swiping. Visual only. */
    val edgeAnimation: EdgeAnimation = EdgeAnimation.Off,
    /** Share of the screen height the side strips cover; the rest stays with the app. */
    val sideLengthPercent: Int = 80,
    val sidePosition: SidePosition = SidePosition.Center,
    /**
     * Off until the user turns it on: with it, a still finger on the bottom strip is kept from
     * the app underneath, which would otherwise see a long press.
     */
    val circleToSearchEnabled: Boolean = false,
    /** Wait as long as the system's own "touch and hold delay" (an accessibility setting). */
    val circleToSearchFollowsSystemTiming: Boolean = true,
    /**
     * The wait when [circleToSearchFollowsSystemTiming] is off. It stays under
     * [maximumArmDurationMs], after which a still touch is no longer followed.
     */
    val circleToSearchHoldMs: Long = PlatformDefaults.LONG_PRESS_TIMEOUT_MS,
    /** Does nothing until ADB mode is set up: pausing needs the secure-settings permission. */
    val pauseInAppsEnabled: Boolean = true,
    /** Apps that refuse to run next to an accessibility service. Empty until the user adds one. */
    val pauseApps: Set<String> = DEFAULT_PAUSE_APPS,
    /**
     * The launcher the Home gesture opens in place of the system's, as a package name; null
     * leaves Home to the system. It lets the system launcher stay the default, which keeps
     * Recents and its animations with the system, while another launcher is what is seen.
     */
    val alternateHome: String? = null,
    /** Read from the device each time, never saved. The defaults are AOSP's. */
    val platform: PlatformDefaults = PlatformDefaults(),
) {
    /** The long-press wait in effect. */
    val effectiveCircleToSearchHoldMs: Long
        get() = if (circleToSearchFollowsSystemTiming) platform.longPressTimeoutMs else circleToSearchHoldMs

    /**
     * This configuration with the values of the Gesture settings screen taken from [source],
     * for that screen's reset. Values that live on other screens are left alone.
     *
     * Based on HyperGesture commit d55afb4 (#123) by Henrique Almeida (h3nc4).
     */
    fun withGestureSettingsOf(source: GestureConfiguration): GestureConfiguration = copy(
        edgeWidthDp = source.edgeWidthDp,
        landscapeConfirmEnabled = source.landscapeConfirmEnabled,
        edgeAnimation = source.edgeAnimation,
        sideLengthPercent = source.sideLengthPercent,
        sidePosition = source.sidePosition,
        sideMinimumSwipeDistanceDp = source.sideMinimumSwipeDistanceDp,
        bottomMinimumSwipeDistanceDp = source.bottomMinimumSwipeDistanceDp,
        recentsHoldMs = source.recentsHoldMs,
        maximumArmDurationMs = source.maximumArmDurationMs,
        leftEdgeBackEnabled = source.leftEdgeBackEnabled,
        rightEdgeBackEnabled = source.rightEdgeBackEnabled,
        hapticFeedbackEnabled = source.hapticFeedbackEnabled,
        hapticStrength = source.hapticStrength,
        hapticConfirmEnabled = source.hapticConfirmEnabled,
    )

    companion object {
        val DEFAULT_PAUSE_APPS: Set<String> = emptySet()
    }
}

/**
 * The device values a long press on the navigation handle uses in AOSP, so Circle to Search
 * starts after the same wait, is cancelled by the same movement and only counts over the
 * handle.
 */
data class PlatformDefaults(
    /** `ViewConfiguration.getLongPressTimeout()`; 400 ms in AOSP since Android 12. */
    val longPressTimeoutMs: Long = LONG_PRESS_TIMEOUT_MS,
    /** `ViewConfiguration.getScaledTouchSlop()`, in dp. */
    val touchSlopDp: Float = 8f,
    /** SystemUI's `navigation_home_handle_width`. */
    val handleWidthDp: Float = 108f,
) {
    companion object {
        const val LONG_PRESS_TIMEOUT_MS = 400L

        /** The handle's long-press area never gets narrower than a touch target. */
        const val MIN_TOUCH_TARGET_DP = 48f
    }
}

/** How firm the gesture click is; [Normal] is the system's own. */
enum class HapticStrength { Light, Normal, Strong }
