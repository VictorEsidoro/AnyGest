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
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Chooses the launcher the Home gesture opens. The system launcher stays the default, so
 * Recents and its animations stay with the system, and this launcher is what is seen.
 */
@Composable
internal fun AlternateHomeCard(configuration: GestureConfiguration, change: ConfigChange) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val chosen = configuration.alternateHome
    val name = remember(chosen) { chosen?.let { appLabel(context, it) } }

    Panel {
        Heading(R.drawable.ic_sym_bottom_navigation, stringResource(R.string.alt_home_title)) {
            Chip(
                stringResource(if (chosen != null) R.string.alt_home_on else R.string.alt_home_off),
                if (chosen != null) Tone.Good else Tone.Neutral,
            )
        }
        Para(stringResource(R.string.alt_home_body))
        Note(stringResource(R.string.alt_home_setup))
        if (name != null) Para(stringResource(R.string.alt_home_current, name))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(onClick = { picking = true }) {
                Text(stringResource(if (chosen == null) R.string.alt_home_choose else R.string.alt_home_change))
            }
            if (chosen != null) {
                TextButton(onClick = { change { it.copy(alternateHome = null) } }) {
                    Text(stringResource(R.string.alt_home_clear))
                }
            }
        }
    }
    if (picking) {
        HomePickerDialog(
            onPick = { picked ->
                picking = false
                change { it.copy(alternateHome = picked) }
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun HomePickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val choices by produceState(initialValue = emptyList<AppChoice>()) {
        value = withContext(Dispatchers.Default) { homeLaunchers(context) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alt_home_dialog_title)) },
        text = {
            if (choices.isEmpty()) {
                Note(stringResource(R.string.alt_home_none))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(choices, key = { it.packageName }) { choice ->
                        Text(
                            text = choice.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(choice.packageName) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pause_dialog_close)) }
        },
    )
}

/** Every launcher that could answer Home, this app excluded, by name. */
internal fun homeLaunchers(context: Context): List<AppChoice> {
    val packages = context.packageManager
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return packages.queryIntentActivities(home, PackageManager.MATCH_ALL)
        .map { AppChoice(it.activityInfo.packageName, it.loadLabel(packages).toString()) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
