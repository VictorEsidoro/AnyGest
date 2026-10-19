// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of HyperGesture.
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

package com.h3nc4.hypergesture.setup

/**
 * The first-run checklist, kept free of Android types so the rules can be tested on the JVM.
 *
 * Colours follow one rule: red is required and not done, green is done, amber is something
 * the app cannot check on its own (or only recommends), grey is optional, and a locked step
 * waits for the one before it. Red is never shown for a state the app cannot read, since a
 * step that stays red after the user did it teaches them to ignore the colour.
 */
enum class SetupStep {
    /** Android blocks Accessibility for sideloaded apps until this is allowed. */
    RESTRICTED_SETTINGS,
    ACCESSIBILITY,
    BATTERY,
    /** HyperOS only: no public API reports it, so the user confirms it. */
    AUTOSTART,
    /** Only meaningful while the gesture service runs: hiding the bar first strands the user. */
    HIDE_NAVIGATION_BAR,
    QUICK_SETTINGS_TILE,
    USAGE_ACCESS,
    DIGITAL_ASSISTANT,
}

enum class StepStatus { DONE, REQUIRED, UNVERIFIED, OPTIONAL, LOCKED }

/** What the app could read from the system, plus what the user confirmed by hand. */
data class SetupFacts(
    val accessibilityEnabled: Boolean,
    /** Null when the platform cannot answer. */
    val batteryUnrestricted: Boolean?,
    val isHyperOs: Boolean,
    val navigationBarCanBeHidden: Boolean,
    val navigationGestural: Boolean,
    val tileAdded: Boolean,
    val tileCanBeRequested: Boolean,
    val appSwitchEnabled: Boolean,
    val usageAccessGranted: Boolean,
    val circleToSearchEnabled: Boolean,
    /** Null when the default assistant cannot be read on this build. */
    val googleIsAssistant: Boolean?,
    val confirmed: Set<SetupStep> = emptySet(),
)

data class StepState(val step: SetupStep, val status: StepStatus, val canConfirm: Boolean)

object SetupChecklist {

    /** The steps that make gestures work at all; the rest refine or extend them. */
    private val essential = setOf(
        SetupStep.RESTRICTED_SETTINGS,
        SetupStep.ACCESSIBILITY,
        SetupStep.BATTERY,
        SetupStep.AUTOSTART,
    )

    fun evaluate(facts: SetupFacts): List<StepState> {
        val steps = mutableListOf<StepState>()
        // Each essential step waits for the previous one, so nobody tries to switch the
        // service on before Android lets them.
        var previousDone = true
        fun chain(own: StepState) {
            val state = if (previousDone) own else own.copy(status = StepStatus.LOCKED)
            steps += state
            previousDone = previousDone && own.status == StepStatus.DONE
        }

        val restrictedDone =
            facts.accessibilityEnabled || SetupStep.RESTRICTED_SETTINGS in facts.confirmed
        chain(
            // Red, unlike other unreadable steps: nothing works without it, and the service
            // switching on proves it was done.
            StepState(
                SetupStep.RESTRICTED_SETTINGS,
                if (restrictedDone) StepStatus.DONE else StepStatus.REQUIRED,
                canConfirm = !restrictedDone,
            ),
        )
        chain(
            StepState(
                SetupStep.ACCESSIBILITY,
                if (facts.accessibilityEnabled) StepStatus.DONE else StepStatus.REQUIRED,
                canConfirm = false,
            ),
        )
        chain(battery(facts))
        if (facts.isHyperOs) chain(confirmable(facts, SetupStep.AUTOSTART))

        if (facts.navigationBarCanBeHidden) {
            val own = StepState(
                SetupStep.HIDE_NAVIGATION_BAR,
                if (facts.navigationGestural) StepStatus.DONE else StepStatus.OPTIONAL,
                canConfirm = false,
            )
            // Locked behind the running service, not the whole chain: a hidden bar with no
            // gesture service is the one state the user cannot navigate out of.
            steps += if (facts.accessibilityEnabled || facts.navigationGestural) {
                own
            } else {
                own.copy(status = StepStatus.LOCKED)
            }
        }

        if (facts.tileCanBeRequested || facts.tileAdded) {
            steps += StepState(
                SetupStep.QUICK_SETTINGS_TILE,
                if (facts.tileAdded) StepStatus.DONE else StepStatus.OPTIONAL,
                canConfirm = false,
            )
        }

        if (facts.appSwitchEnabled) {
            steps += StepState(
                SetupStep.USAGE_ACCESS,
                if (facts.usageAccessGranted) StepStatus.DONE else StepStatus.REQUIRED,
                canConfirm = false,
            )
        }

        if (facts.circleToSearchEnabled) {
            steps += when (facts.googleIsAssistant) {
                true -> StepState(SetupStep.DIGITAL_ASSISTANT, StepStatus.DONE, false)
                false -> StepState(SetupStep.DIGITAL_ASSISTANT, StepStatus.REQUIRED, false)
                null -> confirmable(facts, SetupStep.DIGITAL_ASSISTANT)
            }
        }
        return steps
    }

    /** Nothing red, amber or locked: what is left is optional. */
    fun isComplete(states: List<StepState>): Boolean = states.all {
        it.status == StepStatus.DONE || it.status == StepStatus.OPTIONAL
    }

    /** True when gestures cannot work yet, which keeps the checklist open. */
    fun isBlocking(states: List<StepState>): Boolean = states.any {
        it.step in essential && it.status != StepStatus.DONE
    }

    private fun battery(facts: SetupFacts): StepState = when {
        facts.batteryUnrestricted == true ->
            StepState(SetupStep.BATTERY, StepStatus.DONE, canConfirm = false)
        // The platform's battery-optimisation answer may not track HyperOS's own
        // "No restrictions", so a "no" is not trusted enough to paint the step red.
        else -> confirmable(facts, SetupStep.BATTERY)
    }

    private fun confirmable(facts: SetupFacts, step: SetupStep): StepState =
        if (step in facts.confirmed) {
            StepState(step, StepStatus.DONE, canConfirm = false)
        } else {
            StepState(step, StepStatus.UNVERIFIED, canConfirm = true)
        }
}
