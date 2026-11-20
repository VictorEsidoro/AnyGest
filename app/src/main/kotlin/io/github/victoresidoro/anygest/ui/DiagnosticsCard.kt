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
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.DiagnosticReport
import io.github.victoresidoro.anygest.diagnostics.Diagnostics

private const val FAILURE_PREVIEW_CHARS = 1200

@Composable
internal fun DiagnosticsContent(diagnostics: Diagnostics, onRefresh: () -> Unit) {
    DiagnosticButtons(diagnostics, onRefresh)

    val none = stringResource(R.string.diag_none)
    listOf(
        stringResource(R.string.diag_android_version) to "${diagnostics.androidRelease} (API ${diagnostics.sdkInt})",
        stringResource(R.string.diag_device) to "${diagnostics.manufacturer} ${diagnostics.model}",
        stringResource(R.string.diag_hyperos) to (diagnostics.hyperOsVersion ?: diagnostics.miuiVersion ?: none),
        stringResource(R.string.diag_navigation_mode) to diagnostics.navigationMode.name,
        stringResource(R.string.diag_service_enabled) to "${diagnostics.accessibilityServiceEnabled}",
        stringResource(R.string.diag_service_running) to "${diagnostics.accessibilityServiceRunning}",
        stringResource(R.string.diag_adb_mode) to "${diagnostics.adbMode}",
        stringResource(R.string.diag_integration) to diagnostics.navigationIntegrationId,
        stringResource(R.string.diag_last_failure) to (diagnostics.lastGlobalActionFailure ?: none),
        stringResource(R.string.label_last_app_switch_failure) to (diagnostics.lastAppSwitchFailure ?: none),
        stringResource(R.string.diag_app_version) to diagnostics.appVersionName,
        stringResource(R.string.diag_license) to stringResource(R.string.value_license),
    ).forEach { (name, value) -> Fact(name, value) }

    diagnostics.lastRecordedFailure?.let { failure ->
        Note(stringResource(R.string.diag_recorded_failure), Modifier.padding(top = 8.dp))
        Mono(failure.take(FAILURE_PREVIEW_CHARS))
    }
    diagnostics.pauseNotes?.let { notes ->
        Note(stringResource(R.string.diag_pause_notes), Modifier.padding(top = 8.dp))
        Mono(notes)
    }
    Note(stringResource(R.string.diag_local_only), Modifier.padding(top = 8.dp))
}

/** One name and its value on a line. */
@Composable
private fun Fact(name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Note(name, Modifier.padding(end = 12.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DiagnosticButtons(diagnostics: Diagnostics, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val sourceUrl = stringResource(R.string.url_source)
    val copiedMessage = stringResource(R.string.diag_log_copied)
    Row {
        TextButton(onClick = onRefresh) { Text(stringResource(R.string.action_refresh_diagnostics)) }
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("AnyGest", DiagnosticReport.format(diagnostics)))
                // From Android 13 the system shows its own "copied" note.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            },
        ) { Text(stringResource(R.string.action_copy_log)) }
        // startFirst, since a phone with no browser throws ActivityNotFoundException.
        TextButton(onClick = { context.startFirst(Intent(Intent.ACTION_VIEW, sourceUrl.toUri())) }) {
            Text(stringResource(R.string.action_view_source))
        }
    }
}
