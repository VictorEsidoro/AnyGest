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
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.shortcut.NoGestureShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The exception list: what it does, the apps on it, and a picker for adding one. */
@Composable
internal fun PauseAppsCard(
    enabled: Boolean,
    apps: Set<String>,
    adbMode: Boolean,
    onEnabled: (Boolean) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val usageGranted = SetupFactsReader.hasUsageAccess(context)
    val working = enabled && adbMode && usageGranted
    val canMakeShortcuts = working && NoGestureShortcut.supported(context)

    Panel {
        Heading(R.drawable.ic_sym_checklist, stringResource(R.string.pause_card_title)) {
            Chip(
                stringResource(if (working) R.string.pause_card_on else R.string.pause_card_off),
                if (working) Tone.Good else Tone.Neutral,
            )
        }
        Para(stringResource(R.string.pause_card_body))
        Note(stringResource(R.string.pause_disclaimer))
        when {
            !adbMode -> Note(stringResource(R.string.pause_needs_adb))
            !usageGranted -> {
                Note(stringResource(R.string.pause_needs_usage))
                UsageAccessRow()
            }
        }
        SwitchLine(stringResource(R.string.pause_toggle), enabled, onEnabled)
        if (apps.isEmpty()) Note(stringResource(R.string.pause_empty))
        if (canMakeShortcuts && apps.isNotEmpty()) Note(stringResource(R.string.pause_shortcut_hint))
        apps.sorted().forEach { packageName ->
            val name = remember(packageName) { appLabel(context, packageName) }
            Column {
                Para(name)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (canMakeShortcuts) {
                        TextButton(onClick = { addShortcut(context, packageName) }) {
                            Text(stringResource(R.string.pause_shortcut))
                        }
                    }
                    TextButton(onClick = { onRemove(packageName) }) {
                        Text(stringResource(R.string.pause_remove, name))
                    }
                }
            }
        }
        FilledTonalButton(onClick = { picking = true }) { Text(stringResource(R.string.pause_add)) }
    }
    if (picking) {
        AppPickerDialog(
            exclude = apps,
            onPick = {
                picking = false
                onAdd(it)
            },
            onDismiss = { picking = false },
        )
    }
}

private fun addShortcut(context: Context, packageName: String) {
    if (!NoGestureShortcut.request(context, packageName)) {
        Toast.makeText(context, R.string.shortcut_unsupported, Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun AppPickerDialog(exclude: Set<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Reading every launcher label takes a moment, so it stays off the main thread.
    val choices by produceState(initialValue = emptyList<AppChoice>()) {
        value = withContext(Dispatchers.Default) { launchableApps(context) }
    }
    var typed by remember { mutableStateOf("") }
    val shown = remember(choices, exclude, typed) { filterChoices(choices, exclude, typed) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pause_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text(stringResource(R.string.pause_search)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (shown.isEmpty() && choices.isNotEmpty()) {
                    Note(stringResource(R.string.pause_search_empty), Modifier.padding(top = 12.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.packageName }) { choice ->
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

internal data class AppChoice(val packageName: String, val label: String)

/**
 * The apps not yet on the list that match what was typed, by name or by package name (a bank's
 * package is often the only thing a person knows), ignoring case.
 */
internal fun filterChoices(choices: List<AppChoice>, exclude: Set<String>, query: String): List<AppChoice> {
    val typed = query.trim()
    return choices.filter { app ->
        app.packageName !in exclude &&
            (typed.isEmpty() ||
                app.label.contains(typed, ignoreCase = true) ||
                app.packageName.contains(typed, ignoreCase = true))
    }
}

/** Apps with a launcher entry: the only ones that can be "opened", so the only ones to pause for. */
internal fun launchableApps(context: Context): List<AppChoice> {
    val packages = context.packageManager
    val launchers = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packages.queryIntentActivities(launchers, 0)
        .map { AppChoice(it.activityInfo.packageName, it.loadLabel(packages).toString()) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
