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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.Diagnostics
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.navigation.SystemNavigation
import io.github.victoresidoro.anygest.service.AlertNotifier
import io.github.victoresidoro.anygest.setup.GestureTileService
import io.github.victoresidoro.anygest.setup.NavigationModeTileService
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupChecklist
import io.github.victoresidoro.anygest.setup.SetupFacts
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * What a section screen needs from the app: the settings and how to change them, a number that
 * goes up whenever something outside the app may have changed, and where the search wants to land.
 */
internal class SectionEnv(
    val configuration: GestureConfiguration,
    val change: ConfigChange,
    val onXiaomiRom: Boolean,
    val setupFacts: SetupFacts,
    val revision: Int,
    val refresh: () -> Unit,
    val focus: FocusRequest?,
)

private val tilesAskable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
internal fun AnchorScope.GesturesSection(env: SectionEnv) {
    val context = LocalContext.current
    val config = env.configuration
    Anchor("cheatsheet") { GestureCheatSheet() }
    // Never folded: this is the one gesture that needs a permission before it works, and behind
    // a tap its switch goes unnoticed.
    Anchor("appswitch") { AppSwitchCard(config, env.change) }
    // Never folded either: it depends on a setting outside the app (the default assistant),
    // and this card is where that is explained.
    Anchor("circle") { CircleToSearchCard(config, env.change) }
    Anchor("pause") {
        PauseAppsCard(
            enabled = config.pauseInAppsEnabled,
            apps = config.pauseApps,
            adbMode = remember(env.revision) { SecureSettingsAccess.canWrite(context) },
            onEnabled = { on -> env.change { it.copy(pauseInAppsEnabled = on) } },
            onAdd = { app -> env.change { it.copy(pauseApps = it.pauseApps + app) } },
            onRemove = { app -> env.change { it.copy(pauseApps = it.pauseApps - app) } },
        )
    }
    // Folded unless the search asked for it: a stray tap while scrolling must not quietly change
    // how navigation behaves.
    var tuningOpen by remember(env.focus) { mutableStateOf(env.focus?.anchor == "tuning") }
    Anchor("tuning") {
        FoldPanel(
            icon = R.drawable.ic_sym_tune,
            title = stringResource(R.string.section_gestures),
            summary = stringResource(R.string.section_gestures_summary),
            expanded = tuningOpen,
            onToggle = { tuningOpen = !tuningOpen },
            end = { FoldArrow(tuningOpen) },
        ) {
            TuningSettings(config, env.change)
        }
    }
}

@Composable
internal fun AnchorScope.BarSection(env: SectionEnv) {
    val context = LocalContext.current
    val store = remember(env.revision) { SetupStore(context) }
    val gestural = remember(env.revision) { SystemNavigation.currentMode(context) == NavigationMode.GESTURAL }
    if (env.onXiaomiRom) {
        Anchor("navbar") {
            NavigationBarCard(
                hidden = gestural,
                canChange = SetupChecklist.canChangeNavigationBar(env.setupFacts.accessibilityEnabled, gestural),
                onOpen = { SystemNavigation.openModeScreen(context) },
            )
        }
    }
    Anchor("tile") {
        TileCard(
            title = R.string.tile_card_title,
            icon = R.drawable.ic_sym_dashboard_customize,
            body = R.string.tile_card_body,
            manualHint = R.string.tile_card_manual,
            added = store.tileAdded,
            canRequest = tilesAskable,
        ) {
            askToAddTile(
                context,
                GestureTileService.component(context),
                R.string.app_name,
                R.drawable.ic_tile,
                onAdded = { SetupStore(context).tileAdded = true },
                onAnswered = env.refresh,
            )
        }
    }
    // A separate shortcut: it opens another vendor screen, so it only makes sense on HyperOS.
    if (env.onXiaomiRom) {
        Anchor("navtile") {
            TileCard(
                title = R.string.navigation_tile_card_title,
                icon = R.drawable.ic_navigation_tile,
                body = R.string.navigation_tile_card_body,
                manualHint = R.string.navigation_tile_card_manual,
                added = store.navigationTileAdded,
                canRequest = tilesAskable,
            ) {
                askToAddTile(
                    context,
                    NavigationModeTileService.component(context),
                    R.string.tile_navigation_label,
                    R.drawable.ic_navigation_tile,
                    onAdded = { SetupStore(context).navigationTileAdded = true },
                    onAnswered = env.refresh,
                )
            }
        }
    }
}

@Composable
internal fun AnchorScope.AppSection(env: SectionEnv) {
    val context = LocalContext.current
    val alertsOn = remember(env.revision) { SetupStore(context).alertsEnabled && AlertNotifier.canPost(context) }
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        SetupStore(context).alertsEnabled = ok
        env.refresh()
    }
    Anchor("alerts") {
        AlertsCard(alertsOn) {
            val store = SetupStore(context)
            when {
                alertsOn -> store.alertsEnabled = false
                AlertNotifier.canPost(context) -> store.alertsEnabled = true
                else -> askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            env.refresh()
        }
    }
    Anchor("language") { LanguageCard() }
    Anchor("recents") { RecentsCard() }
    Anchor("keepalive") { KeepAliveCard() }
}

@Composable
internal fun AnchorScope.AdbSection(env: SectionEnv) {
    val context = LocalContext.current
    // The ADB mode and its extras sit together. The exception list also needs the grant, and
    // says so on its own card under Gestures.
    Anchor("adb") {
        val store = remember(env.revision) { SetupStore(context) }
        AdbModeCard(
            granted = remember(env.revision) { SecureSettingsAccess.canWrite(context) },
            command = SecureSettingsAccess.grantCommand(context),
            autoHideBar = store.autoHideBar,
            autoRestore = store.autoRestore,
            onAutoHideBar = {
                SetupStore(context).autoHideBar = it
                env.refresh()
            },
            onAutoRestore = {
                SetupStore(context).autoRestore = it
                env.refresh()
            },
        )
    }
    Anchor("extras") { AdbExtrasCard() }
}

@Composable
internal fun AnchorScope.AboutSection(diagnostics: Diagnostics, onCollect: () -> Unit) {
    // Both start open here: showing them is what this screen is for.
    var diagnosticsOpen by remember { mutableStateOf(true) }
    var creditsOpen by remember { mutableStateOf(true) }
    Anchor("diagnostics") {
        FoldPanel(
            icon = R.drawable.ic_sym_bug_report,
            title = stringResource(R.string.section_diagnostics),
            summary = stringResource(R.string.section_diagnostics_summary),
            expanded = diagnosticsOpen,
            onToggle = {
                diagnosticsOpen = !diagnosticsOpen
                if (diagnosticsOpen) onCollect()
            },
            end = { FoldArrow(diagnosticsOpen) },
        ) {
            DiagnosticsContent(diagnostics, onCollect)
        }
    }
    Anchor("credits") {
        FoldPanel(
            icon = R.drawable.ic_sym_favorite,
            title = stringResource(R.string.section_credits),
            summary = stringResource(R.string.section_credits_summary),
            expanded = creditsOpen,
            onToggle = { creditsOpen = !creditsOpen },
            end = { FoldArrow(creditsOpen) },
        ) {
            CreditsContent()
        }
    }
}
