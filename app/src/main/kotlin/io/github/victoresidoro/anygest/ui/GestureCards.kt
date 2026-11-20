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

import android.view.ViewConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.SettingsLimits
import io.github.victoresidoro.anygest.setup.SetupFactsReader

/** How to change the settings: a function that takes the current ones and returns the new ones. */
internal typealias ConfigChange = ((GestureConfiguration) -> GestureConfiguration) -> Unit

/** Each gesture's name next to how it is made. */
private val GESTURE_HOWTO = listOf(
    R.string.gesture_back_name to R.string.gesture_back_how,
    R.string.gesture_home_name to R.string.gesture_home_how,
    R.string.gesture_recents_name to R.string.gesture_recents_how,
    R.string.gesture_previous_app_name to R.string.gesture_previous_app_how,
    R.string.gesture_next_app_name to R.string.gesture_next_app_how,
    R.string.gesture_circle_to_search_name to R.string.gesture_circle_to_search_how,
)

@Composable
internal fun GestureCheatSheet() = Panel(gap = 12) {
    Heading(R.drawable.ic_sym_gesture, stringResource(R.string.gestures_title))
    GESTURE_HOWTO.forEach { (name, how) ->
        Column {
            Text(
                text = stringResource(name),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Note(stringResource(how))
        }
    }
}

@Composable
internal fun AppSwitchCard(configuration: GestureConfiguration, change: ConfigChange) = Panel {
    Heading(R.drawable.ic_sym_swap_horiz, stringResource(R.string.section_app_switch))
    Para(stringResource(R.string.section_app_switch_body))
    SwitchLine(stringResource(R.string.label_app_switch), configuration.appSwitchEnabled) { on ->
        change { it.copy(appSwitchEnabled = on) }
    }
    if (configuration.appSwitchEnabled) UsageAccessRow()
}

@Composable
internal fun CircleToSearchCard(configuration: GestureConfiguration, change: ConfigChange) {
    val context = LocalContext.current
    Panel {
        Heading(R.drawable.ic_sym_search, stringResource(R.string.section_circle_to_search))
        Para(stringResource(R.string.section_circle_to_search_body))
        SwitchLine(stringResource(R.string.label_circle_to_search), configuration.circleToSearchEnabled) { on ->
            change { it.copy(circleToSearchEnabled = on) }
        }
        if (!configuration.circleToSearchEnabled) return@Panel

        val followsSystem = configuration.circleToSearchFollowsSystemTiming
        SwitchLine(
            stringResource(R.string.label_circle_to_search_system_timing, ViewConfiguration.getLongPressTimeout()),
            followsSystem,
        ) { on -> change { it.copy(circleToSearchFollowsSystemTiming = on) } }
        if (!followsSystem) {
            ThresholdSlider(
                label = stringResource(R.string.label_circle_to_search_hold),
                value = configuration.circleToSearchHoldMs.toFloat(),
                // Kept under the arm window: past it a resting finger is no longer followed,
                // and the long press could never fire.
                range = SettingsLimits.CIRCLE_TO_SEARCH_HOLD_MS.toFloatRange(),
                formatter = { stringResource(R.string.value_ms, it) },
            ) { ms -> change { it.copy(circleToSearchHoldMs = ms.toLong()) } }
        }
        LabelledRow(stringResource(R.string.label_circle_to_search_assistant)) {
            TextButton(onClick = { openAssistantSettings(context) }) {
                Text(stringResource(R.string.action_open_default_apps))
            }
        }
    }
}

/**
 * Whether usage access is granted, read again each time the screen comes back, because the
 * grant is made in another app's activity.
 */
@Composable
internal fun UsageAccessRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(SetupFactsReader.hasUsageAccess(context)) }
    LifecycleResumeEffect(Unit) {
        granted = SetupFactsReader.hasUsageAccess(context)
        onPauseOrDispose {}
    }
    val text = stringResource(
        if (granted) R.string.label_usage_access_granted else R.string.label_usage_access_missing,
    )
    LabelledRow(
        label = text,
        modifier = Modifier
            .clickable { granted = SetupFactsReader.hasUsageAccess(context) }
            .padding(vertical = 4.dp),
    ) {
        if (!granted) {
            TextButton(onClick = { openUsageAccess(context) }) {
                Text(stringResource(R.string.action_grant_usage_access))
            }
        }
    }
}
