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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.DiagnosticReport
import io.github.victoresidoro.anygest.diagnostics.Diagnostics

@Composable
internal fun DiagnosticsContent(diagnostics: Diagnostics, onRefresh: () -> Unit) {
    DiagnosticActions(diagnostics, onRefresh)
    val none = stringResource(R.string.diag_none)
    val rows = listOf(
        stringResource(R.string.diag_android_version) to
            "${diagnostics.androidRelease} (API ${diagnostics.sdkInt})",
        stringResource(R.string.diag_device) to
            "${diagnostics.manufacturer} ${diagnostics.model}",
        stringResource(R.string.diag_hyperos) to
            (diagnostics.hyperOsVersion ?: diagnostics.miuiVersion ?: none),
        stringResource(R.string.diag_navigation_mode) to diagnostics.navigationMode.name,
        stringResource(R.string.diag_service_enabled) to
            diagnostics.accessibilityServiceEnabled.toString(),
        stringResource(R.string.diag_service_running) to
            diagnostics.accessibilityServiceRunning.toString(),
        stringResource(R.string.diag_adb_mode) to diagnostics.adbMode.toString(),
        stringResource(R.string.diag_integration) to diagnostics.navigationIntegrationId,
        stringResource(R.string.diag_last_failure) to
            (diagnostics.lastGlobalActionFailure ?: none),
        stringResource(R.string.label_last_app_switch_failure) to
            (diagnostics.lastAppSwitchFailure ?: none),
        stringResource(R.string.diag_app_version) to diagnostics.appVersionName,
        stringResource(R.string.diag_license) to stringResource(R.string.value_license),
    )
    rows.forEach { (label, value) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(text = value, style = MaterialTheme.typography.bodySmall)
        }
    }
    diagnostics.lastRecordedFailure?.let { failure ->
        Text(
            text = stringResource(R.string.diag_recorded_failure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = failure.take(MAX_FAILURE_CHARS),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
    diagnostics.pauseNotes?.let { notes ->
        Text(
            text = stringResource(R.string.diag_pause_notes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
    Text(
        text = stringResource(R.string.diag_local_only),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun DiagnosticActions(diagnostics: Diagnostics, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val sourceUrl = stringResource(R.string.url_source)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onRefresh) {
            Text(stringResource(R.string.action_refresh_diagnostics))
        }
        val copiedMessage = stringResource(R.string.diag_log_copied)
        TextButton(
            onClick = {
                val text = DiagnosticReport.format(diagnostics)
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("AnyGest", text))
                // Android 13 and later show their own "copied" confirmation.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            },
        ) {
            Text(stringResource(R.string.action_copy_log))
        }
        TextButton(
            onClick = {
                // A device with no browser throws ActivityNotFoundException.
                context.startFirst(Intent(Intent.ACTION_VIEW, sourceUrl.toUri()))
            },
        ) {
            Text(stringResource(R.string.action_view_source))
        }
    }
}

private const val MAX_FAILURE_CHARS = 1200
