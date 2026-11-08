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

/**
 * The text "Copy log" puts on the clipboard: the device and the state of the service first,
 * then the failures and pause notes, so one paste into a message says what the phone is.
 * Never holds more than the screen already shows, and goes nowhere on its own.
 *
 * The layout follows the diagnostic report of Rohith-E-S's HyperGesture fork.
 */
internal object DiagnosticReport {

    fun format(diagnostics: Diagnostics): String = buildString {
        appendLine("### AnyGest ${diagnostics.appVersionName}")
        appendLine("- Device: ${diagnostics.manufacturer} ${diagnostics.model}")
        appendLine("- Android: ${diagnostics.androidRelease} (API ${diagnostics.sdkInt})")
        appendLine("- HyperOS / MIUI: ${diagnostics.hyperOsVersion ?: diagnostics.miuiVersion ?: "none"}")
        appendLine("- Navigation: ${diagnostics.navigationMode.name} (${diagnostics.navigationIntegrationId}, available: ${diagnostics.navigationIntegrationAvailable})")
        appendLine("- Accessibility service: enabled ${diagnostics.accessibilityServiceEnabled}, running ${diagnostics.accessibilityServiceRunning}")
        appendLine("- ADB mode: ${diagnostics.adbMode}")
        section("Last global action failure", diagnostics.lastGlobalActionFailure)
        section("Last app switch failure", diagnostics.lastAppSwitchFailure)
        section("Last recorded failure", diagnostics.lastRecordedFailure)
        section("Pause notes", diagnostics.pauseNotes)
    }.trimEnd()

    private fun StringBuilder.section(title: String, text: String?) {
        if (text.isNullOrBlank()) return
        appendLine()
        appendLine("#### $title")
        appendLine(text.trim())
    }
}
