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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
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

internal fun LongRange.toFloatRange(): ClosedFloatingPointRange<Float> = first.toFloat()..last.toFloat()

private fun IntRange.toFloatRange(): ClosedFloatingPointRange<Float> = first.toFloat()..last.toFloat()

/** The tuning sliders and switches, one after the other, for the card that folds them away. */
@Composable
internal fun TuningSettings(configuration: GestureConfiguration, change: ConfigChange) {
    ThresholdSlider(
        stringResource(R.string.label_edge_width),
        configuration.edgeWidthDp,
        SettingsLimits.EDGE_WIDTH_DP,
        { stringResource(R.string.value_dp, it) },
    ) { v -> change { it.copy(edgeWidthDp = v) } }

    ThresholdSlider(
        stringResource(R.string.label_side_length),
        configuration.sideLengthPercent.toFloat(),
        SettingsLimits.SIDE_LENGTH_PERCENT.toFloatRange(),
        { stringResource(R.string.value_percent, it) },
    ) { v -> change { it.copy(sideLengthPercent = v.toInt()) } }

    // A side strip that spans the whole edge has nowhere to sit.
    if (configuration.sideLengthPercent < 100) {
        ChoiceGroup(
            title = stringResource(R.string.label_side_position),
            options = SidePosition.entries,
            selected = configuration.sidePosition,
            nameOf = {
                when (it) {
                    SidePosition.Top -> R.string.side_position_top
                    SidePosition.Center -> R.string.side_position_center
                    SidePosition.Bottom -> R.string.side_position_bottom
                }
            },
        ) { chosen -> change { it.copy(sidePosition = chosen) } }
    }

    ThresholdSlider(
        stringResource(R.string.label_side_distance),
        configuration.sideMinimumSwipeDistanceDp,
        SettingsLimits.SIDE_DISTANCE_DP,
        { stringResource(R.string.value_dp, it) },
    ) { v -> change { it.copy(sideMinimumSwipeDistanceDp = v) } }

    ThresholdSlider(
        stringResource(R.string.label_bottom_distance),
        configuration.bottomMinimumSwipeDistanceDp,
        SettingsLimits.BOTTOM_DISTANCE_DP,
        { stringResource(R.string.value_dp, it) },
    ) { v -> change { it.copy(bottomMinimumSwipeDistanceDp = v) } }

    ThresholdSlider(
        stringResource(R.string.label_recents_hold),
        configuration.recentsHoldMs.toFloat(),
        SettingsLimits.RECENTS_HOLD_MS.toFloatRange(),
        { stringResource(R.string.value_ms, it) },
    ) { v -> change { it.copy(recentsHoldMs = v.toLong()) } }

    ThresholdSlider(
        stringResource(R.string.label_maximum_duration),
        configuration.maximumArmDurationMs.toFloat(),
        SettingsLimits.MAX_ARM_DURATION_MS.toFloatRange(),
        { stringResource(R.string.value_ms, it) },
    ) { v -> change { it.copy(maximumArmDurationMs = v.toLong()) } }

    ChoiceGroup(
        title = stringResource(R.string.label_edge_animation),
        options = EdgeAnimation.entries,
        selected = configuration.edgeAnimation,
        nameOf = {
            when (it) {
                EdgeAnimation.Off -> R.string.edge_animation_off
                EdgeAnimation.Lens -> R.string.edge_animation_lens
                EdgeAnimation.LensArrow -> R.string.edge_animation_lens_arrow
                EdgeAnimation.Dot -> R.string.edge_animation_dot
                EdgeAnimation.DotDark -> R.string.edge_animation_dot_dark
            }
        },
    ) { chosen -> change { it.copy(edgeAnimation = chosen) } }

    SwitchLine(stringResource(R.string.label_landscape_confirm), configuration.landscapeConfirmEnabled) { on ->
        change { it.copy(landscapeConfirmEnabled = on) }
    }
    SwitchLine(stringResource(R.string.label_left_edge_back), configuration.leftEdgeBackEnabled) { on ->
        change { it.copy(leftEdgeBackEnabled = on) }
    }
    SwitchLine(stringResource(R.string.label_right_edge_back), configuration.rightEdgeBackEnabled) { on ->
        change { it.copy(rightEdgeBackEnabled = on) }
    }
    SwitchLine(stringResource(R.string.label_haptics), configuration.hapticFeedbackEnabled) { on ->
        change { it.copy(hapticFeedbackEnabled = on) }
    }
    if (configuration.hapticFeedbackEnabled) {
        ChoiceGroup(
            title = stringResource(R.string.label_haptic_strength),
            options = HapticStrength.entries,
            selected = configuration.hapticStrength,
            nameOf = {
                when (it) {
                    HapticStrength.Light -> R.string.haptic_light
                    HapticStrength.Normal -> R.string.haptic_normal
                    HapticStrength.Strong -> R.string.haptic_strong
                }
            },
        ) { chosen -> change { it.copy(hapticStrength = chosen) } }
        SwitchLine(stringResource(R.string.label_haptic_confirm), configuration.hapticConfirmEnabled) { on ->
            change { it.copy(hapticConfirmEnabled = on) }
        }
    }

    BackupButtons(configuration) { restored -> change { restored } }

    TextButton(onClick = { change { it.withGestureSettingsOf(GestureConfiguration()) } }) {
        Text(stringResource(R.string.action_reset_defaults))
    }
}

/** Saves the settings to a file the user picks, or restores them from one. */
@Composable
private fun BackupButtons(configuration: GestureConfiguration, onRestore: (GestureConfiguration) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val save = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsBackup.MIME_TYPE),
    ) { target ->
        if (target == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = SettingsBackupIo.export(context, target, configuration)
            Toast.makeText(
                context,
                if (ok) R.string.backup_exported else R.string.backup_export_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        scope.launch {
            val restored = SettingsBackupIo.import(context, source)
            if (restored == null) {
                Toast.makeText(context, R.string.backup_import_invalid, Toast.LENGTH_LONG).show()
                return@launch
            }
            onRestore(restored.configuration)
            val message = when (restored.ignored) {
                0 -> resources.getString(R.string.backup_imported)
                else -> resources.getString(R.string.backup_imported_partial, restored.ignored)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Para(stringResource(R.string.label_backup))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { save.launch(SettingsBackup.FILE_NAME) }) {
            Text(stringResource(R.string.action_backup_export))
        }
        TextButton(onClick = { load.launch(arrayOf(SettingsBackup.MIME_TYPE, "application/octet-stream")) }) {
            Text(stringResource(R.string.action_backup_import))
        }
    }
}

/**
 * A slider that commits when the finger lifts. Writing on every frame of a drag would be a
 * settings write per frame, and each write rebuilds the strips, which stutters visibly.
 */
@Composable
internal fun ThresholdSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatter: @Composable (Int) -> String,
    onCommit: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var shown by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { if (!dragging) shown = value }

    Column {
        LabelledRow(label) {
            Text(
                text = formatter(shown.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = shown,
            valueRange = range,
            onValueChange = {
                dragging = true
                shown = it
            },
            onValueChangeFinished = {
                dragging = false
                onCommit(shown)
            },
        )
    }
}
