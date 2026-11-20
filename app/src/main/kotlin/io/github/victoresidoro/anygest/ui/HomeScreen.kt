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

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService
import io.github.victoresidoro.anygest.setup.GestureTileService
import io.github.victoresidoro.anygest.setup.SetupStep
import io.github.victoresidoro.anygest.setup.SetupStore
import io.github.victoresidoro.anygest.setup.StepState

/** The first screen: the name, the search box, the checklist and the five sections. */
@Composable
internal fun HomeScreen(
    states: List<StepState>,
    onXiaomiRom: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (Section) -> Unit,
    onJump: (SearchTarget) -> Unit,
    onChecked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val items = rememberSearchItems(remember(onXiaomiRom) { searchTargets(onXiaomiRom) })
    val matches = remember(query, items) {
        SettingsSearch.rank(query, items.map { it.second }).map { hit -> items.first { it.second.id == hit.id } }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Note(stringResource(R.string.app_tagline))
        }
        SearchField(query, onQuery)
        if (query.isNotBlank()) {
            SearchResults(query, matches, onJump)
            return@Column
        }
        SetupCard(
            states = states,
            onAction = { step -> openStepScreen(context, step) },
            onConfirm = { step ->
                SetupStore(context).confirm(step)
                onChecked()
            },
            onUndo = { step ->
                SetupStore(context).unconfirm(step)
                onChecked()
            },
        )
        SectionList(onOpen)
    }
}

/** Takes the user to the screen where [step] is done. */
private fun openStepScreen(context: Context, step: SetupStep) {
    when (step) {
        // Both live on the app's own info page, in sections HyperOS names itself.
        SetupStep.RESTRICTED_SETTINGS, SetupStep.BATTERY -> openAppInfo(context)
        SetupStep.ACCESSIBILITY -> context.startFirst(
            GestureTileService.accessibilityIntent(context),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        )
        SetupStep.AUTOSTART -> openAutostart(context)
        SetupStep.LOCK_RECENTS -> AnyGestAccessibilityService.openRecents()
        SetupStep.USAGE_ACCESS -> openUsageAccess(context)
        SetupStep.DIGITAL_ASSISTANT -> openAssistantSettings(context)
    }
}
