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

package io.github.victoresidoro.anygest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.setup.SetupChecklist
import io.github.victoresidoro.anygest.setup.SetupStep
import io.github.victoresidoro.anygest.setup.StepState
import io.github.victoresidoro.anygest.setup.StepStatus

private fun StepStatus.tone() = when (this) {
    StepStatus.DONE -> Tone.Good
    StepStatus.REQUIRED -> Tone.Alert
    StepStatus.UNVERIFIED -> Tone.Caution
    StepStatus.LOCKED -> Tone.Neutral
}

private fun StepStatus.label() = when (this) {
    StepStatus.DONE -> R.string.setup_status_done
    StepStatus.REQUIRED -> R.string.setup_status_required
    StepStatus.UNVERIFIED -> R.string.setup_status_unverified
    StepStatus.LOCKED -> R.string.setup_status_locked
}

/** The strings of a step: its name, what to do, why it is locked, and its button. */
private class StepWords(val title: Int, val body: Int, val action: Int)

private fun SetupStep.words() = when (this) {
    SetupStep.RESTRICTED_SETTINGS ->
        StepWords(R.string.setup_restricted_title, R.string.setup_restricted_body, R.string.action_open_app_info)
    SetupStep.ACCESSIBILITY ->
        StepWords(R.string.setup_accessibility_title, R.string.setup_accessibility_body, R.string.action_open_accessibility_settings)
    SetupStep.BATTERY ->
        StepWords(R.string.setup_battery_title, R.string.setup_battery_body, R.string.action_open_app_info)
    SetupStep.AUTOSTART ->
        StepWords(R.string.setup_autostart_title, R.string.setup_autostart_body, R.string.action_open_autostart)
    SetupStep.LOCK_RECENTS ->
        StepWords(R.string.setup_lock_recents_title, R.string.setup_lock_recents_body, R.string.action_open_recents)
    SetupStep.USAGE_ACCESS ->
        StepWords(R.string.setup_usage_title, R.string.setup_usage_body, R.string.action_grant_usage_access)
    SetupStep.DIGITAL_ASSISTANT ->
        StepWords(R.string.setup_assistant_title, R.string.setup_assistant_body, R.string.action_open_default_apps)
}

/**
 * The first-run checklist at the top of the home screen. Open while anything is left to do,
 * folded to one line once all is done, and open again by itself if HyperOS later stops the
 * gesture service.
 */
@Composable
internal fun SetupCard(
    states: List<StepState>,
    onAction: (SetupStep) -> Unit,
    onConfirm: (SetupStep) -> Unit,
    onUndo: (SetupStep) -> Unit,
) {
    val complete = SetupChecklist.isComplete(states)
    val blocking = SetupChecklist.isBlocking(states)
    // Keyed on completeness, so a change either way undoes what the user folded.
    var open by remember(complete) { mutableStateOf(!complete) }
    val verdict = when {
        complete -> StepStatus.DONE
        blocking -> StepStatus.REQUIRED
        else -> StepStatus.UNVERIFIED
    }
    FoldPanel(
        icon = R.drawable.ic_sym_checklist,
        title = stringResource(R.string.setup_title),
        summary = stringResource(
            when (verdict) {
                StepStatus.DONE -> R.string.setup_summary_complete
                StepStatus.REQUIRED -> R.string.setup_summary_blocking
                else -> R.string.setup_summary_almost
            },
        ),
        expanded = open,
        onToggle = { open = !open },
        end = {
            Chip(
                stringResource(
                    when (verdict) {
                        StepStatus.DONE -> R.string.setup_badge_complete
                        StepStatus.REQUIRED -> R.string.setup_badge_blocking
                        else -> R.string.setup_badge_almost
                    },
                ),
                verdict.tone(),
            )
        },
        bodyGap = 16,
    ) {
        states.forEachIndexed { index, state ->
            StepRow(index + 1, state, { onAction(state.step) }, { onConfirm(state.step) }, { onUndo(state.step) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepRow(number: Int, state: StepState, onAction: () -> Unit, onConfirm: () -> Unit, onUndo: () -> Unit) {
    val words = state.step.words()
    val tone = state.status.tone()
    val locked = state.status == StepStatus.LOCKED
    val done = state.status == StepStatus.DONE
    val (badgeBackground, badgeInk) = tone.colours()

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(badgeBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (state.status) {
                StepStatus.DONE -> Icon(Icons.Default.Check, null, Modifier.size(18.dp), badgeInk)
                StepStatus.LOCKED -> Icon(Icons.Default.Lock, null, Modifier.size(16.dp), badgeInk)
                else -> Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeInk,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(words.title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                Chip(stringResource(state.status.label()), tone)
            }
            // A finished step folds to its title; the others say what to do.
            if (!done) Note(stringResource(if (locked) R.string.setup_locked_body else words.body))
            if (!done && !locked) {
                // One under the other, never side by side: HyperOS screen names run long in Portuguese.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = onAction) { Text(stringResource(words.action)) }
                    if (state.canConfirm) {
                        OutlinedButton(onClick = onConfirm) { Text(stringResource(R.string.setup_action_confirm)) }
                    }
                }
            }
            // A done step stays reachable: the setting can change behind the app's back, and a
            // step marked done by hand may turn out not to be.
            if (done) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onAction) { Text(stringResource(words.action)) }
                    if (state.canUndo) TextButton(onClick = onUndo) { Text(stringResource(R.string.setup_action_undo)) }
                }
            }
        }
    }
}
