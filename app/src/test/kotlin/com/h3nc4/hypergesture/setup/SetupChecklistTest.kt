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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupChecklistTest {

    /** A fresh install on HyperOS: nothing done, no optional feature switched on. */
    private val fresh = SetupFacts(
        accessibilityEnabled = false,
        batteryUnrestricted = false,
        isHyperOs = true,
        appSwitchEnabled = false,
        usageAccessGranted = false,
        circleToSearchEnabled = false,
        googleIsAssistant = null,
    )

    /** Everything essential done. */
    private val ready = fresh.copy(
        accessibilityEnabled = true,
        batteryUnrestricted = true,
        confirmed = setOf(SetupStep.AUTOSTART),
    )

    private fun status(facts: SetupFacts, step: SetupStep): StepStatus? =
        SetupChecklist.evaluate(facts).firstOrNull { it.step == step }?.status

    private fun state(facts: SetupFacts, step: SetupStep): StepState =
        SetupChecklist.evaluate(facts).first { it.step == step }

    @Test
    fun `a fresh install asks for restricted settings first and locks the rest`() {
        assertEquals(StepStatus.REQUIRED, status(fresh, SetupStep.RESTRICTED_SETTINGS))
        assertEquals(StepStatus.LOCKED, status(fresh, SetupStep.ACCESSIBILITY))
        assertEquals(StepStatus.LOCKED, status(fresh, SetupStep.BATTERY))
        assertEquals(StepStatus.LOCKED, status(fresh, SetupStep.AUTOSTART))
        assertTrue(state(fresh, SetupStep.RESTRICTED_SETTINGS).canConfirm)
    }

    @Test
    fun `confirming restricted settings unlocks accessibility`() {
        val facts = fresh.copy(confirmed = setOf(SetupStep.RESTRICTED_SETTINGS))
        assertEquals(StepStatus.DONE, status(facts, SetupStep.RESTRICTED_SETTINGS))
        assertEquals(StepStatus.REQUIRED, status(facts, SetupStep.ACCESSIBILITY))
        assertEquals(StepStatus.LOCKED, status(facts, SetupStep.BATTERY))
    }

    @Test
    fun `a running service proves restricted settings were allowed`() {
        val facts = fresh.copy(accessibilityEnabled = true)
        assertEquals(StepStatus.DONE, status(facts, SetupStep.RESTRICTED_SETTINGS))
        assertFalse(state(facts, SetupStep.RESTRICTED_SETTINGS).canConfirm)
        assertEquals(StepStatus.DONE, status(facts, SetupStep.ACCESSIBILITY))
    }

    @Test
    fun `battery is never red because the platform answer may not match HyperOS`() {
        val facts = fresh.copy(accessibilityEnabled = true, batteryUnrestricted = false)
        assertEquals(StepStatus.UNVERIFIED, status(facts, SetupStep.BATTERY))
        assertTrue(state(facts, SetupStep.BATTERY).canConfirm)
        val unknown = facts.copy(batteryUnrestricted = null)
        assertEquals(StepStatus.UNVERIFIED, status(unknown, SetupStep.BATTERY))
    }

    @Test
    fun `battery turns green on its own or once confirmed`() {
        val read = fresh.copy(accessibilityEnabled = true, batteryUnrestricted = true)
        assertEquals(StepStatus.DONE, status(read, SetupStep.BATTERY))
        val confirmed = fresh.copy(
            accessibilityEnabled = true,
            confirmed = setOf(SetupStep.BATTERY),
        )
        assertEquals(StepStatus.DONE, status(confirmed, SetupStep.BATTERY))
    }

    @Test
    fun `autostart waits for battery and then asks to be confirmed`() {
        val facts = fresh.copy(accessibilityEnabled = true)
        assertEquals(StepStatus.LOCKED, status(facts, SetupStep.AUTOSTART))
        val batteryDone = facts.copy(batteryUnrestricted = true)
        assertEquals(StepStatus.UNVERIFIED, status(batteryDone, SetupStep.AUTOSTART))
        assertTrue(state(batteryDone, SetupStep.AUTOSTART).canConfirm)
    }

    @Test
    fun `autostart only exists on HyperOS`() {
        assertNull(status(ready.copy(isHyperOs = false), SetupStep.AUTOSTART))
    }

    @Test
    fun `the button bar can only be hidden while the service runs`() {
        assertFalse(SetupChecklist.canChangeNavigationBar(false, navigationGestural = false))
        assertTrue(SetupChecklist.canChangeNavigationBar(true, navigationGestural = false))
        // A bar already hidden can always be brought back.
        assertTrue(SetupChecklist.canChangeNavigationBar(false, navigationGestural = true))
    }

    @Test
    fun `a hand-made confirmation can be taken back`() {
        assertTrue(state(ready, SetupStep.AUTOSTART).canUndo)
        // Battery read from the system, restricted settings proven by the running service.
        assertFalse(state(ready, SetupStep.BATTERY).canUndo)
        assertFalse(state(ready, SetupStep.RESTRICTED_SETTINGS).canUndo)
        val byHand = fresh.copy(confirmed = setOf(SetupStep.RESTRICTED_SETTINGS))
        assertTrue(state(byHand, SetupStep.RESTRICTED_SETTINGS).canUndo)
        assertFalse(state(fresh, SetupStep.AUTOSTART).canUndo)
    }

    @Test
    fun `feature steps appear only with their feature`() {
        assertNull(status(ready, SetupStep.USAGE_ACCESS))
        assertNull(status(ready, SetupStep.DIGITAL_ASSISTANT))
        val on = ready.copy(appSwitchEnabled = true, circleToSearchEnabled = true)
        assertEquals(StepStatus.REQUIRED, status(on, SetupStep.USAGE_ACCESS))
        assertEquals(StepStatus.UNVERIFIED, status(on, SetupStep.DIGITAL_ASSISTANT))
    }

    @Test
    fun `the assistant step uses the system answer when there is one`() {
        val on = ready.copy(circleToSearchEnabled = true)
        assertEquals(
            StepStatus.DONE,
            status(on.copy(googleIsAssistant = true), SetupStep.DIGITAL_ASSISTANT),
        )
        assertEquals(
            StepStatus.REQUIRED,
            status(on.copy(googleIsAssistant = false), SetupStep.DIGITAL_ASSISTANT),
        )
    }

    @Test
    fun `complete and blocking`() {
        assertTrue(SetupChecklist.isBlocking(SetupChecklist.evaluate(fresh)))
        assertFalse(SetupChecklist.isComplete(SetupChecklist.evaluate(fresh)))

        assertFalse(SetupChecklist.isBlocking(SetupChecklist.evaluate(ready)))
        assertTrue(SetupChecklist.isComplete(SetupChecklist.evaluate(ready)))

        // A feature missing its permission is not complete, but gestures still work.
        val missing = ready.copy(appSwitchEnabled = true)
        assertFalse(SetupChecklist.isComplete(SetupChecklist.evaluate(missing)))
        assertFalse(SetupChecklist.isBlocking(SetupChecklist.evaluate(missing)))

        // HyperOS stopping the service reopens it, at the service step: the app remembers
        // that restricted settings were already allowed.
        val dropped = ready.copy(
            accessibilityEnabled = false,
            confirmed = ready.confirmed + SetupStep.RESTRICTED_SETTINGS,
        )
        assertTrue(SetupChecklist.isBlocking(SetupChecklist.evaluate(dropped)))
        assertEquals(StepStatus.DONE, status(dropped, SetupStep.RESTRICTED_SETTINGS))
        assertEquals(StepStatus.REQUIRED, status(dropped, SetupStep.ACCESSIBILITY))
    }

    @Test
    fun `steps come in a fixed order`() {
        val all = ready.copy(appSwitchEnabled = true, circleToSearchEnabled = true)
        assertEquals(
            SetupStep.entries.toList(),
            SetupChecklist.evaluate(all).map { it.step },
        )
    }
}
