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

package io.github.victoresidoro.anygest.diagnostics

import io.github.victoresidoro.anygest.navigation.NavigationMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportTest {

    private fun diagnostics(failure: String? = null, notes: String? = null) = Diagnostics(
        androidRelease = "16",
        sdkInt = 36,
        manufacturer = "Xiaomi",
        model = "Poco F8 Ultra",
        hyperOsVersion = "OS3.0",
        miuiVersion = null,
        navigationMode = NavigationMode.GESTURAL,
        accessibilityServiceEnabled = true,
        accessibilityServiceRunning = true,
        adbMode = true,
        navigationIntegrationId = "hyperos",
        navigationIntegrationAvailable = true,
        lastGlobalActionFailure = null,
        lastAppSwitchFailure = null,
        lastRecordedFailure = failure,
        pauseNotes = notes,
        appVersionName = "4.3.0",
    )

    @Test
    fun `the report names the version, the device and the service state`() {
        val report = DiagnosticReport.format(diagnostics())

        assertTrue(report.startsWith("### AnyGest 4.3.0"))
        assertTrue(report.contains("Xiaomi Poco F8 Ultra"))
        assertTrue(report.contains("HyperOS / MIUI: OS3.0"))
        assertTrue(report.contains("running true"))
    }

    @Test
    fun `failures and notes appear only when there are some`() {
        val empty = DiagnosticReport.format(diagnostics())
        assertFalse(empty.contains("####"))

        val full = DiagnosticReport.format(diagnostics(failure = "boom", notes = "paused for x"))
        assertTrue(full.contains("#### Last recorded failure\nboom"))
        assertTrue(full.contains("#### Pause notes\npaused for x"))
    }
}
