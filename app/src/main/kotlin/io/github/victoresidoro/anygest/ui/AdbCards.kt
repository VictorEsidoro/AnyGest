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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.setup.AdbCommands
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.ui.theme.onSuccessContainer
import io.github.victoresidoro.anygest.ui.theme.successContainer

/**
 * Where the one-time `pm grant` is explained and its result shown. Without the grant the card
 * shows the command to copy; with it, the two switches that say how much the app may do alone.
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
    Panel {
        Heading(R.drawable.ic_sym_tune, stringResource(R.string.adb_card_title)) {
            Chip(
                stringResource(if (granted) R.string.adb_card_on else R.string.adb_card_off),
                if (granted) Tone.Good else Tone.Neutral,
            )
        }
        Para(stringResource(if (granted) R.string.adb_card_body_on else R.string.adb_card_body_off))
        if (granted) {
            SwitchLine(stringResource(R.string.adb_toggle_hide_bar), autoHideBar, onAutoHideBar)
            SwitchLine(stringResource(R.string.adb_toggle_restore), autoRestore, onAutoRestore)
            return@Panel
        }
        Note(stringResource(R.string.adb_card_command_label))
        SelectionContainer { Mono(command) }
        Note(stringResource(R.string.adb_card_hint_off))
        FilledTonalButton(onClick = { copyToClipboard(context, "pm grant", command) }) {
            Text(stringResource(R.string.adb_action_copy))
        }
    }
}

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, R.string.adb_copied, Toast.LENGTH_SHORT).show()
}

/**
 * The optional adb commands to copy, with what each does on the Android screens and a live
 * check of what already took. The app only shows and checks: it cannot run them.
 */
@Composable
internal fun AdbExtrasCard() {
    val context = LocalContext.current
    val launcher = remember { defaultLauncherPackage(context) }
    val launcherName = remember(launcher) { launcher?.let { appLabel(context, it) } }
    var looks by remember { mutableIntStateOf(0) }
    // The commands run elsewhere, so everything is read again on the way back.
    LifecycleResumeEffect(Unit) {
        looks++
        onPauseOrDispose {}
    }
    val granted = remember(looks) { SecureSettingsAccess.canWrite(context) }
    val selfExempt = remember(looks) { ignoresBatteryLimits(context, context.packageName) }
    val launcherExempt = remember(looks) { launcher?.let { ignoresBatteryLimits(context, it) } }
    val usage = remember(looks) { SetupFactsReader.hasUsageAccess(context) }
    val allDone = granted && selfExempt && launcherExempt != false && usage

    Panel {
        Heading(R.drawable.ic_sym_tune, stringResource(R.string.adb_extras_title))
        Para(stringResource(R.string.adb_extras_body))
        Check(stringResource(R.string.adb_check_grant), granted)
        Check(stringResource(R.string.adb_check_battery_self), selfExempt)
        if (launcherExempt != null && launcherName != null) {
            Check(stringResource(R.string.adb_check_battery_launcher, launcherName), launcherExempt)
        }
        Check(stringResource(R.string.adb_check_usage), usage)
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
        Command(
            R.string.adb_cmd_battery_title,
            AdbCommands.battery(listOfNotNull(context.packageName, launcher)),
            R.string.adb_cmd_battery_alt,
        )
        Command(R.string.adb_cmd_usage_title, AdbCommands.usageAccess(context.packageName), R.string.adb_cmd_usage_alt)
        Command(
            R.string.adb_cmd_restricted_title,
            AdbCommands.restrictedSettings(context.packageName),
            R.string.adb_cmd_restricted_alt,
        )
    }
}

@Composable
private fun Check(label: String, ok: Boolean) = LabelledRow(label) {
    Chip(
        stringResource(if (ok) R.string.adb_check_ok else R.string.adb_check_pending),
        if (ok) Tone.Good else Tone.Neutral,
    )
}

@Composable
private fun Command(title: Int, command: String, alternative: Int) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
        SelectionContainer { Mono(command) }
        Note(stringResource(alternative))
        FilledTonalButton(onClick = { copyToClipboard(context, "adb", command) }) {
            Text(stringResource(R.string.adb_action_copy))
        }
    }
}

/** The app's name, or its package name when it is not installed or this app cannot see it. */
internal fun appLabel(context: Context, packageName: String): String = runCatching {
    val packages = context.packageManager
    packages.getApplicationInfo(packageName, 0).loadLabel(packages).toString()
}.getOrDefault(packageName)

/** The default launcher's package, or null when none is set (a chooser would answer "android"). */
private fun defaultLauncherPackage(context: Context): String? {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val chosen = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName
    return AdbCommands.safePackage(chosen)?.takeIf { it != context.packageName }
}

private fun ignoresBatteryLimits(context: Context, packageName: String): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true
