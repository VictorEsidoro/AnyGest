// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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

package io.github.victoresidoro.anygest.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.gesture.EdgeAnimation
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.HapticStrength
import io.github.victoresidoro.anygest.gesture.SettingsLimits
import io.github.victoresidoro.anygest.gesture.SidePosition
import io.github.victoresidoro.anygest.settings.SettingsBackup
import io.github.victoresidoro.anygest.settings.SettingsBackupIo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun GestureSettings(
    configuration: GestureConfiguration,
    onUpdate: ((GestureConfiguration) -> GestureConfiguration) -> Unit,
) {
    ThresholdSlider(
        label = stringResource(R.string.label_edge_width),
        value = configuration.edgeWidthDp,
        range = SettingsLimits.EDGE_WIDTH_DP,
        formatter = { stringResource(R.string.value_dp, it) },
    ) { updated -> onUpdate { it.copy(edgeWidthDp = updated) } }

    ThresholdSlider(
        label = stringResource(R.string.label_side_length),
        value = configuration.sideLengthPercent.toFloat(),
        range = SettingsLimits.SIDE_LENGTH_PERCENT.first.toFloat()..
            SettingsLimits.SIDE_LENGTH_PERCENT.last.toFloat(),
        formatter = { stringResource(R.string.value_percent, it.toInt()) },
    ) { updated -> onUpdate { it.copy(sideLengthPercent = updated.toInt()) } }

    if (configuration.sideLengthPercent < 100) {
        SidePositionChoice(selected = configuration.sidePosition) { chosen ->
            onUpdate { it.copy(sidePosition = chosen) }
        }
    }

    ThresholdSlider(
        label = stringResource(R.string.label_side_distance),
        value = configuration.sideMinimumSwipeDistanceDp,
        range = SettingsLimits.SIDE_DISTANCE_DP,
        formatter = { stringResource(R.string.value_dp, it) },
    ) { updated -> onUpdate { it.copy(sideMinimumSwipeDistanceDp = updated) } }

    ThresholdSlider(
        label = stringResource(R.string.label_bottom_distance),
        value = configuration.bottomMinimumSwipeDistanceDp,
        range = SettingsLimits.BOTTOM_DISTANCE_DP,
        formatter = { stringResource(R.string.value_dp, it) },
    ) { updated -> onUpdate { it.copy(bottomMinimumSwipeDistanceDp = updated) } }

    ThresholdSlider(
        label = stringResource(R.string.label_recents_hold),
        value = configuration.recentsHoldMs.toFloat(),
        range = SettingsLimits.RECENTS_HOLD_MS.toFloatRange(),
        formatter = { stringResource(R.string.value_ms, it) },
    ) { updated -> onUpdate { it.copy(recentsHoldMs = updated.toLong()) } }

    ThresholdSlider(
        label = stringResource(R.string.label_maximum_duration),
        value = configuration.maximumArmDurationMs.toFloat(),
        range = SettingsLimits.MAX_ARM_DURATION_MS.toFloatRange(),
        formatter = { stringResource(R.string.value_ms, it) },
    ) { updated -> onUpdate { it.copy(maximumArmDurationMs = updated.toLong()) } }

    EdgeAnimationChoice(selected = configuration.edgeAnimation) { chosen ->
        onUpdate { it.copy(edgeAnimation = chosen) }
    }

    ToggleRow(
        label = stringResource(R.string.label_landscape_confirm),
        checked = configuration.landscapeConfirmEnabled,
    ) { updated -> onUpdate { it.copy(landscapeConfirmEnabled = updated) } }

    ToggleRow(
        label = stringResource(R.string.label_left_edge_back),
        checked = configuration.leftEdgeBackEnabled,
    ) { updated -> onUpdate { it.copy(leftEdgeBackEnabled = updated) } }

    ToggleRow(
        label = stringResource(R.string.label_right_edge_back),
        checked = configuration.rightEdgeBackEnabled,
    ) { updated -> onUpdate { it.copy(rightEdgeBackEnabled = updated) } }

    ToggleRow(
        label = stringResource(R.string.label_haptics),
        checked = configuration.hapticFeedbackEnabled,
    ) { updated -> onUpdate { it.copy(hapticFeedbackEnabled = updated) } }

    if (configuration.hapticFeedbackEnabled) {
        HapticStrengthChoice(selected = configuration.hapticStrength) { chosen ->
            onUpdate { it.copy(hapticStrength = chosen) }
        }
        ToggleRow(
            label = stringResource(R.string.label_haptic_confirm),
            checked = configuration.hapticConfirmEnabled,
        ) { updated -> onUpdate { it.copy(hapticConfirmEnabled = updated) } }
    }

    SettingsBackupActions(configuration) { restored -> onUpdate { restored } }

    TextButton(onClick = { onUpdate { it.withGestureSettingsOf(GestureConfiguration()) } }) {
        Text(stringResource(R.string.action_reset_defaults))
    }
}

internal fun LongRange.toFloatRange(): ClosedFloatingPointRange<Float> =
    first.toFloat()..last.toFloat()

/** Save the settings to a file of the user's choosing, or restore them from one. */
@Composable
private fun SettingsBackupActions(
    configuration: GestureConfiguration,
    onRestore: (GestureConfiguration) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsBackup.MIME_TYPE),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = SettingsBackupIo.export(context, uri, configuration)
            val message = if (saved) R.string.backup_exported else R.string.backup_export_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = SettingsBackupIo.import(context, uri)
            if (decoded == null) {
                Toast.makeText(context, R.string.backup_import_invalid, Toast.LENGTH_LONG).show()
                return@launch
            }
            onRestore(decoded.configuration)
            val message = if (decoded.ignored == 0) {
                resources.getString(R.string.backup_imported)
            } else {
                resources.getString(R.string.backup_imported_partial, decoded.ignored)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Text(
        text = stringResource(R.string.label_backup),
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { exporter.launch(SettingsBackup.FILE_NAME) }) {
            Text(stringResource(R.string.action_backup_export))
        }
        TextButton(onClick = { importer.launch(arrayOf(SettingsBackup.MIME_TYPE, "application/octet-stream")) }) {
            Text(stringResource(R.string.action_backup_import))
        }
    }
}

// Commits on release only: a write per drag frame is a DataStore read-modify-write per
// frame, and each write re-emits the config flow, rebuilding all three overlay windows -
// visibly stutters on HyperOS.
@Composable
internal fun ThresholdSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatter: @Composable (Int) -> String,
    onCommit: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { if (!dragging) local = value }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            Text(
                text = formatter(local.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = local,
            valueRange = range,
            onValueChange = {
                dragging = true
                local = it
            },
            onValueChangeFinished = {
                dragging = false
                onCommit(local)
            },
        )
    }
}

@Composable
private fun EdgeAnimationChoice(selected: EdgeAnimation, onChoose: (EdgeAnimation) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.label_edge_animation),
            style = MaterialTheme.typography.bodyMedium,
        )
        EdgeAnimation.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == option,
                        role = Role.RadioButton,
                        onClick = { onChoose(option) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option, onClick = null)
                Text(
                    text = stringResource(
                        when (option) {
                            EdgeAnimation.Off -> R.string.edge_animation_off
                            EdgeAnimation.Lens -> R.string.edge_animation_lens
                            EdgeAnimation.LensArrow -> R.string.edge_animation_lens_arrow
                            EdgeAnimation.Dot -> R.string.edge_animation_dot
                            EdgeAnimation.DotDark -> R.string.edge_animation_dot_dark
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SidePositionChoice(selected: SidePosition, onChoose: (SidePosition) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.label_side_position),
            style = MaterialTheme.typography.bodyMedium,
        )
        SidePosition.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == option,
                        role = Role.RadioButton,
                        onClick = { onChoose(option) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option, onClick = null)
                Text(
                    text = stringResource(
                        when (option) {
                            SidePosition.Top -> R.string.side_position_top
                            SidePosition.Center -> R.string.side_position_center
                            SidePosition.Bottom -> R.string.side_position_bottom
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun HapticStrengthChoice(selected: HapticStrength, onChoose: (HapticStrength) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.label_haptic_strength),
            style = MaterialTheme.typography.bodyMedium,
        )
        HapticStrength.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == option,
                        role = Role.RadioButton,
                        onClick = { onChoose(option) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option, onClick = null)
                Text(
                    text = stringResource(
                        when (option) {
                            HapticStrength.Light -> R.string.haptic_light
                            HapticStrength.Normal -> R.string.haptic_normal
                            HapticStrength.Strong -> R.string.haptic_strong
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
