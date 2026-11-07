// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.shortcut.NoGestureShortcut
import io.github.victoresidoro.anygest.setup.AdbCommands
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.ui.theme.onSuccessContainer
import io.github.victoresidoro.anygest.ui.theme.successContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The same status chip as the other cards, for the two cards that hold more than a button. */
@Composable
private fun StatusChip(text: String, healthy: Boolean) {
    val background = if (healthy) successContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (healthy) onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Where the one-time `pm grant` is explained and its result shown. Without the grant it shows
 * the command to copy; with it, the two switches that decide how much AnyGest may do by itself.
 */
@Composable
internal fun AdbModeCard(
    granted: Boolean,
    command: String,
    autoHideBar: Boolean,
    autoRestore: Boolean,
    onAutoHideBar: (Boolean) -> Unit,
    onAutoRestore: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardTitle(
                    icon = R.drawable.ic_sym_tune,
                    title = stringResource(R.string.adb_card_title),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                StatusChip(
                    text = stringResource(
                        if (granted) R.string.adb_card_on else R.string.adb_card_off,
                    ),
                    healthy = granted,
                )
            }
            Text(
                text = stringResource(
                    if (granted) R.string.adb_card_body_on else R.string.adb_card_body_off,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (granted) {
                ToggleRow(stringResource(R.string.adb_toggle_hide_bar), autoHideBar, onAutoHideBar)
                ToggleRow(stringResource(R.string.adb_toggle_restore), autoRestore, onAutoRestore)
            } else {
                Text(
                    text = stringResource(R.string.adb_card_command_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(
                        text = command,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    text = stringResource(R.string.adb_card_hint_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = { copyCommand(context, command) }) {
                    Text(stringResource(R.string.adb_action_copy))
                }
            }
        }
    }
}

private fun copyCommand(context: Context, command: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("pm grant", command))
    Toast.makeText(context, R.string.adb_copied, Toast.LENGTH_SHORT).show()
}

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
    val active = enabled && adbMode && usageGranted
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardTitle(
                    icon = R.drawable.ic_sym_checklist,
                    title = stringResource(R.string.pause_card_title),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                StatusChip(
                    text = stringResource(
                        if (active) R.string.pause_card_on else R.string.pause_card_off,
                    ),
                    healthy = active,
                )
            }
            Text(
                text = stringResource(R.string.pause_card_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.pause_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!adbMode) {
                Text(
                    text = stringResource(R.string.pause_needs_adb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (!usageGranted) {
                Text(
                    text = stringResource(R.string.pause_needs_usage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UsageAccessRow()
            }
            ToggleRow(stringResource(R.string.pause_toggle), enabled, onEnabled)
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.pause_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val shortcuts = active && NoGestureShortcut.supported(context)
            if (shortcuts && apps.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.pause_shortcut_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            apps.sorted().forEach { packageName ->
                val label = remember(packageName) { labelOf(context, packageName) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (shortcuts) {
                            TextButton(
                                onClick = {
                                    if (!NoGestureShortcut.request(context, packageName)) {
                                        Toast.makeText(
                                            context,
                                            R.string.shortcut_unsupported,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.pause_shortcut))
                            }
                        }
                        TextButton(onClick = { onRemove(packageName) }) {
                            Text(stringResource(R.string.pause_remove, label))
                        }
                    }
                }
            }
            FilledTonalButton(onClick = { picking = true }) {
                Text(stringResource(R.string.pause_add))
            }
        }
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

@Composable
private fun AppPickerDialog(
    exclude: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Reading every launcher label takes a moment, so it stays off the main thread.
    val choices by produceState(initialValue = emptyList<AppChoice>()) {
        value = withContext(Dispatchers.Default) { launchableApps(context) }
    }
    var query by remember { mutableStateOf("") }
    val shown = remember(choices, exclude, query) { filterChoices(choices, exclude, query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pause_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.pause_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (shown.isEmpty() && choices.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.pause_search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.packageName }) {
                        Text(
                            text = it.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(it.packageName) }
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
 * The apps not already on the list that match what was typed, by name or by package name
 * (a bank's package is often the only thing a user knows), ignoring case.
 */
internal fun filterChoices(
    choices: List<AppChoice>,
    exclude: Set<String>,
    query: String,
): List<AppChoice> {
    val needle = query.trim()
    return choices.filter { choice ->
        choice.packageName !in exclude &&
            (needle.isEmpty() ||
                choice.label.contains(needle, ignoreCase = true) ||
                choice.packageName.contains(needle, ignoreCase = true))
    }
}

/** Apps with a launcher entry, the only ones that can be "opened" and so be paused for. */
internal fun launchableApps(context: Context): List<AppChoice> {
    val packages = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packages.queryIntentActivities(launcher, 0)
        .map { AppChoice(it.activityInfo.packageName, it.loadLabel(packages).toString()) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

/** The app's name, or its package name when it is not installed or not visible to this app. */
private fun labelOf(context: Context, packageName: String): String = runCatching {
    val packages = context.packageManager
    packages.getApplicationInfo(packageName, 0).loadLabel(packages).toString()
}.getOrDefault(packageName)

/** The default launcher's package, or null when none is set (a chooser would answer "android"). */
private fun defaultLauncher(context: Context): String? {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val name = context.packageManager
        .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    return AdbCommands.safePackage(name)?.takeIf { it != context.packageName }
}

private fun exemptFromBattery(context: Context, packageName: String): Boolean =
    context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(packageName) == true

/**
 * Optional adb commands to copy, next to what each one does on the Android screens, plus a
 * live check of what already took. The app only shows and checks: it cannot run them.
 */
@Composable
internal fun AdbExtrasCard() {
    val context = LocalContext.current
    val launcher = remember { defaultLauncher(context) }
    val launcherLabel = remember(launcher) { launcher?.let { labelOf(context, it) } }
    var revision by remember { mutableStateOf(0) }
    // The commands run in another app, so everything is read again on the way back.
    LifecycleResumeEffect(Unit) {
        revision++
        onPauseOrDispose {}
    }
    val granted = remember(revision) { SecureSettingsAccess.canWrite(context) }
    val selfExempt = remember(revision) { exemptFromBattery(context, context.packageName) }
    val launcherExempt = remember(revision) { launcher?.let { exemptFromBattery(context, it) } }
    val usage = remember(revision) { SetupFactsReader.hasUsageAccess(context) }
    val allDone = granted && selfExempt && launcherExempt != false && usage

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CardTitle(
                icon = R.drawable.ic_sym_tune,
                title = stringResource(R.string.adb_extras_title),
            )
            Text(
                text = stringResource(R.string.adb_extras_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            CheckRow(stringResource(R.string.adb_check_grant), granted)
            CheckRow(stringResource(R.string.adb_check_battery_self), selfExempt)
            if (launcherExempt != null && launcherLabel != null) {
                CheckRow(
                    stringResource(R.string.adb_check_battery_launcher, launcherLabel),
                    launcherExempt,
                )
            }
            CheckRow(stringResource(R.string.adb_check_usage), usage)
            if (allDone) {
                Text(
                    text = stringResource(R.string.adb_check_all_done),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSuccessContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(successContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }
            CommandBlock(
                title = stringResource(R.string.adb_cmd_battery_title),
                command = AdbCommands.battery(listOfNotNull(context.packageName, launcher)),
                alternative = stringResource(R.string.adb_cmd_battery_alt),
            )
            CommandBlock(
                title = stringResource(R.string.adb_cmd_usage_title),
                command = AdbCommands.usageAccess(context.packageName),
                alternative = stringResource(R.string.adb_cmd_usage_alt),
            )
            CommandBlock(
                title = stringResource(R.string.adb_cmd_restricted_title),
                command = AdbCommands.restrictedSettings(context.packageName),
                alternative = stringResource(R.string.adb_cmd_restricted_alt),
            )
        }
    }
}

@Composable
private fun CheckRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        StatusChip(
            text = stringResource(if (ok) R.string.adb_check_ok else R.string.adb_check_pending),
            healthy = ok,
        )
    }
}

@Composable
private fun CommandBlock(title: String, command: String, alternative: String) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        SelectionContainer {
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            text = alternative,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = { copyCommand(context, command) }) {
            Text(stringResource(R.string.adb_action_copy))
        }
    }
}
