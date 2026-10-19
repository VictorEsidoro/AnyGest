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

package io.github.victoresidoro.anygest.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.setup.SetupChecklist
import io.github.victoresidoro.anygest.setup.SetupStep
import io.github.victoresidoro.anygest.setup.StepState
import io.github.victoresidoro.anygest.setup.StepStatus
import io.github.victoresidoro.anygest.ui.theme.onSuccessContainer
import io.github.victoresidoro.anygest.ui.theme.onWarningContainer
import io.github.victoresidoro.anygest.ui.theme.successContainer
import io.github.victoresidoro.anygest.ui.theme.warningContainer

/**
 * First-run checklist at the top of the screen. Open while anything is left to do, folded to
 * one line once everything is done, and open again on its own if HyperOS later stops the
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
    // Keyed on completeness, so a change in either direction resets what the user folded.
    var expanded by remember(complete) { mutableStateOf(!complete) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardIcon(R.drawable.ic_sym_checklist, Modifier.padding(end = 12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            when {
                                complete -> R.string.setup_summary_complete
                                blocking -> R.string.setup_summary_blocking
                                else -> R.string.setup_summary_almost
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Chip(
                    label = stringResource(
                        when {
                            complete -> R.string.setup_badge_complete
                            blocking -> R.string.setup_badge_blocking
                            else -> R.string.setup_badge_almost
                        },
                    ),
                    colors = when {
                        complete -> statusColors(StepStatus.DONE)
                        blocking -> statusColors(StepStatus.REQUIRED)
                        else -> statusColors(StepStatus.UNVERIFIED)
                    },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        states.forEachIndexed { index, state ->
                            StepRow(
                                number = index + 1,
                                state = state,
                                onAction = { onAction(state.step) },
                                onConfirm = { onConfirm(state.step) },
                                onUndo = { onUndo(state.step) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepRow(
    number: Int,
    state: StepState,
    onAction: () -> Unit,
    onConfirm: () -> Unit,
    onUndo: () -> Unit,
) {
    val text = stepText(state.step)
    val colors = statusColors(state.status)
    val locked = state.status == StepStatus.LOCKED
    val done = state.status == StepStatus.DONE

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (state.status) {
                StepStatus.DONE -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.size(18.dp),
                )
                StepStatus.LOCKED -> Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.size(16.dp),
                )
                else -> Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.foreground,
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
                    text = stringResource(text.title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (locked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                Chip(label = stringResource(statusLabel(state.status)), colors = colors)
            }
            // A finished step folds to its title; the rest explain what to do.
            if (!done) {
                Text(
                    text = stringResource(if (locked) text.lockedBody else text.body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!done && !locked) {
                // Stacked, never side by side: HyperOS screen names run long in Portuguese.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = onAction) { Text(stringResource(text.action)) }
                    if (state.canConfirm) {
                        OutlinedButton(onClick = onConfirm) {
                            Text(stringResource(R.string.setup_action_confirm))
                        }
                    }
                }
            }
            // A finished step stays reachable: the setting can change behind the app's back,
            // and a step marked done by hand may turn out not to be.
            if (done) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onAction) { Text(stringResource(text.action)) }
                    if (state.canUndo) {
                        TextButton(onClick = onUndo) {
                            Text(stringResource(R.string.setup_action_undo))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, colors: StatusColors) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = colors.foreground,
        modifier = Modifier
            .background(colors.background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private data class StatusColors(val background: Color, val foreground: Color)

@Composable
private fun statusColors(status: StepStatus): StatusColors = when (status) {
    StepStatus.DONE -> StatusColors(successContainer, onSuccessContainer)
    StepStatus.REQUIRED -> StatusColors(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
    )
    StepStatus.UNVERIFIED -> StatusColors(warningContainer, onWarningContainer)
    StepStatus.LOCKED -> StatusColors(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun statusLabel(status: StepStatus): Int = when (status) {
    StepStatus.DONE -> R.string.setup_status_done
    StepStatus.REQUIRED -> R.string.setup_status_required
    StepStatus.UNVERIFIED -> R.string.setup_status_unverified
    StepStatus.LOCKED -> R.string.setup_status_locked
}

private data class StepText(val title: Int, val body: Int, val lockedBody: Int, val action: Int)

private fun stepText(step: SetupStep): StepText = when (step) {
    SetupStep.RESTRICTED_SETTINGS -> StepText(
        R.string.setup_restricted_title,
        R.string.setup_restricted_body,
        R.string.setup_locked_body,
        R.string.action_open_app_info,
    )
    SetupStep.ACCESSIBILITY -> StepText(
        R.string.setup_accessibility_title,
        R.string.setup_accessibility_body,
        R.string.setup_locked_body,
        R.string.action_open_accessibility_settings,
    )
    SetupStep.BATTERY -> StepText(
        R.string.setup_battery_title,
        R.string.setup_battery_body,
        R.string.setup_locked_body,
        R.string.action_open_app_info,
    )
    SetupStep.AUTOSTART -> StepText(
        R.string.setup_autostart_title,
        R.string.setup_autostart_body,
        R.string.setup_locked_body,
        R.string.action_open_autostart,
    )
    SetupStep.USAGE_ACCESS -> StepText(
        R.string.setup_usage_title,
        R.string.setup_usage_body,
        R.string.setup_locked_body,
        R.string.action_grant_usage_access,
    )
    SetupStep.DIGITAL_ASSISTANT -> StepText(
        R.string.setup_assistant_title,
        R.string.setup_assistant_body,
        R.string.setup_locked_body,
        R.string.action_open_default_apps,
    )
}
