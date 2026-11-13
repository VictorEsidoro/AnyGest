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

package io.github.victoresidoro.anygest.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Lets Android wake AnyGest when the list of enabled accessibility services changes, even
 * with its process dead. Nothing polls: the system watches the setting and runs this job.
 * It then does what opening the app would do ([GesturePower.restoreIfNeeded]), which honours
 * the tile's off switch, a pause for an exception app and "Switch the gestures back on by
 * itself". Only scheduled in ADB mode, the one case where it can act.
 */
class RecoveryJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        val store = SetupStore(this)
        if (!DiagnosticsCollector.isAccessibilityServiceEnabled(this)) {
            val raw = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val flag = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, -1)
                .takeIf { it >= 0 }
            store.note(
                "job: the accessibility list changed with the service off (" +
                    AccessibilityListState.describe(raw, flag, packageName) + ")",
            )
        }
        GesturePower.restoreIfNeeded(this)
        // A content trigger fires once; the next change needs a fresh schedule.
        schedule(this)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    companion object {
        private const val JOB_ID = 4_201
        private const val UPDATE_DELAY_MS = 300L
        private const val MAX_DELAY_MS = 2_000L

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (!SecureSettingsAccess.canWrite(context)) {
                runCatching { scheduler.cancel(JOB_ID) }
                return
            }
            val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, RecoveryJob::class.java))
                .addTriggerContentUri(JobInfo.TriggerContentUri(uri, 0))
                .setTriggerContentUpdateDelay(UPDATE_DELAY_MS)
                .setTriggerContentMaxDelay(MAX_DELAY_MS)
                .build()
            runCatching { scheduler.schedule(job) }
        }
    }
}
