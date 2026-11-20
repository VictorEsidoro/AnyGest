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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.navigation.SystemNavigation
import io.github.victoresidoro.anygest.service.AnyGestAccessibilityService
import io.github.victoresidoro.anygest.service.GesturePower
import io.github.victoresidoro.anygest.service.RecoveryJob
import io.github.victoresidoro.anygest.settings.GestureSettingsRepository
import io.github.victoresidoro.anygest.setup.SetupChecklist
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.ui.theme.AnyGestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app draws edge to edge. Without this the status bar keeps its white icons on the
        // light background, and the clock and battery vanish.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = GestureSettingsRepository(applicationContext)
        setContent {
            AnyGestTheme {
                Scaffold { padding -> AnyGestApp(settings, Modifier.padding(padding)) }
            }
        }
    }
}

/** Holds what the screens share, and shows the home screen or the section that was opened. */
@Composable
private fun AnyGestApp(settings: GestureSettingsRepository, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration by settings.configuration.collectAsState(initial = GestureConfiguration())
    val onXiaomiRom = remember { SystemNavigation.onXiaomiRom() }
    var diagnostics by remember { mutableStateOf(DiagnosticsCollector.collect(context)) }

    // Goes up on every resume and confirmation. The checklist reads state that changes in other
    // apps (Settings), which never recomposes this screen on its own.
    var revision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        // Opening the app is a moment a killed service can be brought back, and the first one
        // after the one-time grant when the pause list can start listening and the recovery
        // job can be set up.
        GesturePower.restoreIfNeeded(context)
        AnyGestAccessibilityService.refreshSubscription()
        applyRecentsVisibility(context)
        RecoveryJob.schedule(context)
        revision++
        onPauseOrDispose {}
    }
    val facts = remember(configuration, revision) { SetupFactsReader.read(context, configuration) }

    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var focus by remember { mutableStateOf<FocusRequest?>(null) }
    val section = Section.entries.firstOrNull { it.name == openName }
    fun leaveSection() {
        openName = null
        focus = null
    }
    BackHandler(enabled = section != null, onBack = ::leaveSection)
    BackHandler(enabled = section == null && query.isNotEmpty()) { query = "" }

    LaunchedEffect(section) { if (section == Section.ABOUT) diagnostics = DiagnosticsCollector.collect(context) }

    if (section == null) {
        HomeScreen(
            states = remember(facts) { SetupChecklist.evaluate(facts) },
            onXiaomiRom = onXiaomiRom,
            query = query,
            onQuery = { query = it },
            onOpen = { openName = it.name },
            onJump = { target ->
                if (target.section == null) {
                    query = "" // the checklist is on the home screen itself
                } else {
                    focus = FocusRequest(target.anchor)
                    openName = target.section.name
                }
            },
            onChecked = { revision++ },
            modifier = modifier,
        )
        return
    }

    val env = SectionEnv(
        configuration = configuration,
        change = { change -> scope.launch { settings.update(change) } },
        onXiaomiRom = onXiaomiRom,
        setupFacts = facts,
        revision = revision,
        refresh = { revision++ },
        focus = focus,
    )
    SectionScreen(section, focus, ::leaveSection, modifier) {
        when (section) {
            Section.GESTURES -> GesturesSection(env)
            Section.BAR -> BarSection(env)
            Section.APP -> AppSection(env)
            Section.ADB -> AdbSection(env)
            Section.ABOUT -> AboutSection(diagnostics) { diagnostics = DiagnosticsCollector.collect(context) }
        }
    }
}
