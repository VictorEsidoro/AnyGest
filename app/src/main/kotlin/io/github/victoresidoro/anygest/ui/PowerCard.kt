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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.service.GesturePower
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * One button that switches the gestures off until the user switches them on again. It does
 * what the tile does: shows the button bar, and no recovery brings the service back. Only
 * shown where the app can write the setting (ADB mode), since that is what it needs.
 */
@Composable
internal fun PowerCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!SecureSettingsAccess.canWrite(context)) return
    var on by remember {
        mutableStateOf(
            DiagnosticsCollector.isAccessibilityServiceEnabled(context) && !SetupStore(context).userPaused,
        )
    }
    Panel(modifier) {
        Heading(R.drawable.ic_sym_gesture, stringResource(R.string.power_title))
        Para(stringResource(if (on) R.string.power_body_on else R.string.power_body_off))
        val click = { on = GesturePower.toggleByUser(context) }
        if (on) {
            FilledTonalButton(onClick = click, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.power_turn_off))
            }
        } else {
            Button(onClick = click, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.power_turn_on))
            }
        }
    }
}
