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

import android.app.Activity
import android.app.LocaleManager
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.diagnostics.Diagnostics
import io.github.victoresidoro.anygest.diagnostics.DiagnosticsCollector
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.HapticStrength
import io.github.victoresidoro.anygest.gesture.SettingsLimits
import io.github.victoresidoro.anygest.navigation.NavigationIntegrationFactory
import io.github.victoresidoro.anygest.navigation.NavigationMode
import io.github.victoresidoro.anygest.settings.GestureSettingsRepository
import io.github.victoresidoro.anygest.settings.SettingsBackup
import io.github.victoresidoro.anygest.settings.SettingsBackupIo
import io.github.victoresidoro.anygest.setup.GestureTileService
import io.github.victoresidoro.anygest.setup.NavigationModeTileService
import io.github.victoresidoro.anygest.setup.SetupChecklist
import io.github.victoresidoro.anygest.setup.SetupFactsReader
import io.github.victoresidoro.anygest.setup.SetupStep
import io.github.victoresidoro.anygest.setup.SetupStore
import io.github.victoresidoro.anygest.ui.theme.AnyGestTheme
import io.github.victoresidoro.anygest.ui.theme.onSuccessContainer
import io.github.victoresidoro.anygest.ui.theme.successContainer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android draws this app edge to edge, and without this the status bar keeps its
        // white icons on the light background: the clock and battery vanished.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = GestureSettingsRepository(applicationContext)
        setContent {
            AnyGestTheme {
                Scaffold { padding ->
                    AnyGestScreen(settings, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun AnyGestScreen(
    settings: GestureSettingsRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration by settings.configuration.collectAsState(initial = GestureConfiguration())
    val integration = remember { NavigationIntegrationFactory.create(context) }

    var diagnostics by remember { mutableStateOf(DiagnosticsCollector.collect(context)) }
    // Bumped on every resume and confirmation: the checklist reads system state that changes
    // in other apps (Settings), which never recomposes this screen on its own.
    var setupRevision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        setupRevision++
        onPauseOrDispose {}
    }
    val setupFacts = remember(configuration, setupRevision) {
        SetupFactsReader.read(context, configuration)
    }
    val setupStates = remember(setupFacts) { SetupChecklist.evaluate(setupFacts) }
    val navigationStatus = remember(setupRevision) { integration.status() }
    val tileAdded = remember(setupRevision) { SetupStore(context).tileAdded }
    val navigationTileAdded = remember(setupRevision) { SetupStore(context).navigationTileAdded }
    var settingsExpanded by remember { mutableStateOf(false) }
    var diagnosticsExpanded by remember { mutableStateOf(false) }
    var creditsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header()

        SetupCard(
            states = setupStates,
            onAction = { step ->
                when (step) {
                    // Both live on the app's own info page, in sections HyperOS names itself.
                    SetupStep.RESTRICTED_SETTINGS, SetupStep.BATTERY -> openAppInfo(context)
                    SetupStep.ACCESSIBILITY -> context.startFirst(
                        GestureTileService.accessibilityIntent(context),
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    )
                    SetupStep.AUTOSTART -> openAutostart(context)
                    SetupStep.USAGE_ACCESS ->
                        context.startFirst(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    SetupStep.DIGITAL_ASSISTANT -> openAssistantSettings(context)
                }
            },
            onConfirm = { step ->
                SetupStore(context).confirm(step)
                setupRevision++
            },
            onUndo = { step ->
                SetupStore(context).unconfirm(step)
                setupRevision++
            },
        )

        // Outside the checklist on purpose: both are choices the user comes back to, and a
        // folded checklist would hide them.
        TileCard(
            added = tileAdded,
            canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            onRequest = {
                requestTile(
                    context = context,
                    component = GestureTileService.component(context),
                    label = R.string.app_name,
                    icon = R.drawable.ic_tile,
                    onAdded = { SetupStore(context).tileAdded = true },
                    onResult = { setupRevision++ },
                )
            },
        )

        if (navigationStatus.available) {
            NavigationBarCard(
                hidden = navigationStatus.currentMode == NavigationMode.GESTURAL,
                canChange = SetupChecklist.canChangeNavigationBar(
                    accessibilityEnabled = setupFacts.accessibilityEnabled,
                    navigationGestural = navigationStatus.currentMode == NavigationMode.GESTURAL,
                ),
                onOpen = { integration.requestEnable() },
            )

            // A separate shortcut, not folded into TileCard above: it targets a different
            // vendor screen and only makes sense once HyperOS is confirmed.
            NavigationTileCard(
                added = navigationTileAdded,
                canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                onRequest = {
                    requestTile(
                        context = context,
                        component = NavigationModeTileService.component(context),
                        label = R.string.tile_navigation_label,
                        icon = R.drawable.ic_sym_bottom_navigation,
                        onAdded = { SetupStore(context).navigationTileAdded = true },
                        onResult = { setupRevision++ },
                    )
                },
            )
        }

        // Never collapsed: this is the one gesture that needs a permission before it works,
        // and hiding the toggle behind a tap is how it goes unnoticed.
        AppSwitchCard(configuration) { update ->
            scope.launch { settings.update(update) }
        }

        // Also never collapsed: it depends on a setting outside this app (the default
        // assistant), and the card is where that requirement is explained.
        CircleToSearchCard(configuration) { update ->
            scope.launch { settings.update(update) }
        }

        GestureCheatSheet()

        LanguageCard()

        // Both sections stay collapsed by default: an accidental tap while scrolling
        // must not silently change navigation behaviour.
        ExpandableCard(
            title = stringResource(R.string.section_gestures),
            icon = R.drawable.ic_sym_tune,
            summary = stringResource(R.string.section_gestures_summary),
            expanded = settingsExpanded,
            onToggle = { settingsExpanded = !settingsExpanded },
        ) {
            GestureSettings(configuration) { update ->
                scope.launch { settings.update(update) }
            }
        }

        ExpandableCard(
            title = stringResource(R.string.section_diagnostics),
            icon = R.drawable.ic_sym_bug_report,
            summary = stringResource(R.string.section_diagnostics_summary),
            expanded = diagnosticsExpanded,
            onToggle = {
                diagnosticsExpanded = !diagnosticsExpanded
                if (diagnosticsExpanded) diagnostics = DiagnosticsCollector.collect(context)
            },
        ) {
            DiagnosticsContent(diagnostics) {
                diagnostics = DiagnosticsCollector.collect(context)
            }
        }

        ExpandableCard(
            title = stringResource(R.string.section_credits),
            icon = R.drawable.ic_sym_favorite,
            summary = stringResource(R.string.section_credits_summary),
            expanded = creditsExpanded,
            onToggle = { creditsExpanded = !creditsExpanded },
        ) {
            CreditsContent()
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * VOICE_INPUT_SETTINGS is AOSP's "assist and voice input" screen, where the digital assistant
 * app is chosen; the default apps list is the fallback on builds that do not route it there.
 */
private fun openAssistantSettings(context: Context) = context.startFirst(
    Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
    Intent(Settings.ACTION_SETTINGS),
)

/**
 * Android 13's one-tap prompt to place a tile. Builds that refuse it still let the user add
 * the tile by editing the Control Center, which each card's text explains.
 */
private fun requestTile(
    context: Context,
    component: ComponentName,
    @StringRes label: Int,
    @DrawableRes icon: Int,
    onAdded: () -> Unit,
    onResult: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
    runCatching {
        statusBar.requestAddTileService(
            component,
            context.getString(label),
            Icon.createWithResource(context, icon),
            context.mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            ) {
                onAdded()
            }
            onResult()
        }
    }
}

/** Autostart decides whether HyperOS restores the service after a reboot. */
private fun openAutostart(context: Context) = context.startFirst(
    Intent(MIUI_AUTOSTART_ACTION).addCategory(Intent.CATEGORY_DEFAULT),
    appDetails(context),
)

/** Power lives on this page, which is about this app alone rather than a list of them all. */
private fun openAppInfo(context: Context) = context.startFirst(appDetails(context))

/** Only HyperOS answers the Autostart action, so that button needs somewhere to land. */
private fun appDetails(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    "package:${context.packageName}".toUri(),
)

/**
 * NEW_TASK is required from a non-Activity context and wrong from an Activity one, where it
 * would give the target its own task and stop Back returning here.
 */
private fun Context.startFirst(vararg candidates: Intent) {
    val ownTask = findActivity() == null
    for (intent in candidates) {
        if (ownTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isSuccess) return
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private const val MIUI_AUTOSTART_ACTION = "miui.intent.action.OP_AUTO_START"

@Composable
private fun TileCard(added: Boolean, canRequest: Boolean, onRequest: () -> Unit) {
    InfoCard(
        title = stringResource(R.string.tile_card_title),
        icon = R.drawable.ic_sym_dashboard_customize,
        badge = stringResource(if (added) R.string.tile_card_added else R.string.tile_card_missing),
        healthy = added,
        body = stringResource(R.string.tile_card_body),
        hint = stringResource(R.string.tile_card_manual).takeUnless { added },
        actionLabel = stringResource(R.string.setup_tile_action).takeIf { canRequest && !added },
        onAction = onRequest,
    )
}

/** Same shape as [TileCard], for the second, HyperOS-only shortcut. */
@Composable
private fun NavigationTileCard(added: Boolean, canRequest: Boolean, onRequest: () -> Unit) {
    InfoCard(
        title = stringResource(R.string.navigation_tile_card_title),
        icon = R.drawable.ic_sym_bottom_navigation,
        badge = stringResource(if (added) R.string.tile_card_added else R.string.tile_card_missing),
        healthy = added,
        body = stringResource(R.string.navigation_tile_card_body),
        hint = stringResource(R.string.navigation_tile_card_manual).takeUnless { added },
        actionLabel = stringResource(R.string.setup_tile_action).takeIf { canRequest && !added },
        onAction = onRequest,
    )
}

@Composable
private fun NavigationBarCard(hidden: Boolean, canChange: Boolean, onOpen: () -> Unit) {
    InfoCard(
        title = stringResource(R.string.navigation_card_title),
        icon = R.drawable.ic_sym_bottom_navigation,
        badge = stringResource(
            if (hidden) R.string.navigation_card_hidden else R.string.navigation_card_visible,
        ),
        healthy = hidden,
        body = stringResource(
            if (canChange) R.string.navigation_card_body else R.string.navigation_card_locked,
        ),
        hint = null,
        actionLabel = stringResource(R.string.action_open_navigation_settings).takeIf { canChange },
        onAction = onOpen,
    )
}

/** Title, a status chip, an explanation and at most one button. */
@Composable
private fun InfoCard(
    title: String,
    @DrawableRes icon: Int,
    badge: String,
    healthy: Boolean,
    body: String,
    hint: String?,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardTitle(
                    icon = icon,
                    title = title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                val chipBackground =
                    if (healthy) successContainer else MaterialTheme.colorScheme.surfaceVariant
                val chipText =
                    if (healthy) onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = chipText,
                    modifier = Modifier
                        .background(chipBackground, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            actionLabel?.let { FilledTonalButton(onClick = onAction) { Text(it) } }
        }
    }
}

@Composable
private fun GestureCheatSheet() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardTitle(
                icon = R.drawable.ic_sym_gesture,
                title = stringResource(R.string.gestures_title),
            )
            listOf(
                R.string.gesture_back_name to R.string.gesture_back_how,
                R.string.gesture_home_name to R.string.gesture_home_how,
                R.string.gesture_recents_name to R.string.gesture_recents_how,
                R.string.gesture_previous_app_name to R.string.gesture_previous_app_how,
                R.string.gesture_next_app_name to R.string.gesture_next_app_how,
                R.string.gesture_circle_to_search_name to R.string.gesture_circle_to_search_how,
            ).forEach { (name, how) ->
                Column {
                    Text(
                        text = stringResource(name),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(how),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    @DrawableRes icon: Int,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "expandArrow",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardIcon(icon, Modifier.padding(end = 12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded) R.string.action_collapse else R.string.action_expand,
                    ),
                    modifier = Modifier.rotate(arrowRotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureSettings(
    configuration: GestureConfiguration,
    onUpdate: ((GestureConfiguration) -> GestureConfiguration) -> Unit,
) {
    ThresholdSlider(
        label = stringResource(R.string.label_edge_width),
        value = configuration.edgeWidthDp,
        range = SettingsLimits.EDGE_WIDTH_DP,
        formatter = { stringResource(R.string.value_dp, it) },
    ) { updated -> onUpdate { it.copy(edgeWidthDp = updated) } }

    ThresholdSlider(
        label = stringResource(R.string.label_side_distance),
        value = configuration.sideMinimumSwipeDistanceDp,
        range = SettingsLimits.SIDE_DISTANCE_DP,
        formatter = { stringResource(R.string.value_dp, it) },
    ) { updated -> onUpdate { it.copy(sideMinimumSwipeDistanceDp = updated) } }

    ThresholdSlider(
        label = stringResource(R.string.label_bottom_distance),
        value = configuration.bottomMinimumSwipeDistanceDp,
        range = SettingsLimits.BOTTOM_DISTANCE_DP,
        formatter = { stringResource(R.string.value_dp, it) },
    ) { updated -> onUpdate { it.copy(bottomMinimumSwipeDistanceDp = updated) } }

    ThresholdSlider(
        label = stringResource(R.string.label_recents_hold),
        value = configuration.recentsHoldMs.toFloat(),
        range = SettingsLimits.RECENTS_HOLD_MS.toFloatRange(),
        formatter = { stringResource(R.string.value_ms, it) },
    ) { updated -> onUpdate { it.copy(recentsHoldMs = updated.toLong()) } }

    ThresholdSlider(
        label = stringResource(R.string.label_maximum_duration),
        value = configuration.maximumArmDurationMs.toFloat(),
        range = SettingsLimits.MAX_ARM_DURATION_MS.toFloatRange(),
        formatter = { stringResource(R.string.value_ms, it) },
    ) { updated -> onUpdate { it.copy(maximumArmDurationMs = updated.toLong()) } }

    ToggleRow(
        label = stringResource(R.string.label_left_edge_back),
        checked = configuration.leftEdgeBackEnabled,
    ) { updated -> onUpdate { it.copy(leftEdgeBackEnabled = updated) } }

    ToggleRow(
        label = stringResource(R.string.label_right_edge_back),
        checked = configuration.rightEdgeBackEnabled,
    ) { updated -> onUpdate { it.copy(rightEdgeBackEnabled = updated) } }

    ToggleRow(
        label = stringResource(R.string.label_haptics),
        checked = configuration.hapticFeedbackEnabled,
    ) { updated -> onUpdate { it.copy(hapticFeedbackEnabled = updated) } }

    if (configuration.hapticFeedbackEnabled) {
        HapticStrengthChoice(selected = configuration.hapticStrength) { chosen ->
            onUpdate { it.copy(hapticStrength = chosen) }
        }
    }

    SettingsBackupActions(configuration) { restored -> onUpdate { restored } }

    TextButton(onClick = { onUpdate { GestureConfiguration() } }) {
        Text(stringResource(R.string.action_reset_defaults))
    }
}

private fun LongRange.toFloatRange(): ClosedFloatingPointRange<Float> =
    first.toFloat()..last.toFloat()

/** Save the settings to a file of the user's choosing, or restore them from one. */
@Composable
private fun SettingsBackupActions(
    configuration: GestureConfiguration,
    onRestore: (GestureConfiguration) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsBackup.MIME_TYPE),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = SettingsBackupIo.export(context, uri, configuration)
            val message = if (saved) R.string.backup_exported else R.string.backup_export_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = SettingsBackupIo.import(context, uri)
            if (decoded == null) {
                Toast.makeText(context, R.string.backup_import_invalid, Toast.LENGTH_LONG).show()
                return@launch
            }
            onRestore(decoded.configuration)
            val message = if (decoded.ignored == 0) {
                resources.getString(R.string.backup_imported)
            } else {
                resources.getString(R.string.backup_imported_partial, decoded.ignored)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Text(
        text = stringResource(R.string.label_backup),
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { exporter.launch(SettingsBackup.FILE_NAME) }) {
            Text(stringResource(R.string.action_backup_export))
        }
        TextButton(onClick = { importer.launch(arrayOf(SettingsBackup.MIME_TYPE, "application/octet-stream")) }) {
            Text(stringResource(R.string.action_backup_import))
        }
    }
}

// Commits on release only: a write per drag frame is a DataStore read-modify-write per
// frame, and each write re-emits the config flow, rebuilding all three overlay windows -
// visibly stutters on HyperOS.
@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatter: @Composable (Int) -> String,
    onCommit: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) { if (!dragging) local = value }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            Text(
                text = formatter(local.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = local,
            valueRange = range,
            onValueChange = {
                dragging = true
                local = it
            },
            onValueChangeFinished = {
                dragging = false
                onCommit(local)
            },
        )
    }
}

/**
 * Re-checked on tap, since returning from Settings does not recompose on its own.
 */
@Composable
private fun AppSwitchCard(
    configuration: GestureConfiguration,
    onUpdate: ((GestureConfiguration) -> GestureConfiguration) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CardTitle(
                icon = R.drawable.ic_sym_swap_horiz,
                title = stringResource(R.string.section_app_switch),
            )
            Text(
                text = stringResource(R.string.section_app_switch_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            ToggleRow(
                label = stringResource(R.string.label_app_switch),
                checked = configuration.appSwitchEnabled,
            ) { updated -> onUpdate { it.copy(appSwitchEnabled = updated) } }

            if (configuration.appSwitchEnabled) UsageAccessRow()
        }
    }
}

@Composable
private fun CircleToSearchCard(
    configuration: GestureConfiguration,
    onUpdate: ((GestureConfiguration) -> GestureConfiguration) -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CardTitle(
                icon = R.drawable.ic_sym_search,
                title = stringResource(R.string.section_circle_to_search),
            )
            Text(
                text = stringResource(R.string.section_circle_to_search_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            ToggleRow(
                label = stringResource(R.string.label_circle_to_search),
                checked = configuration.circleToSearchEnabled,
            ) { updated -> onUpdate { it.copy(circleToSearchEnabled = updated) } }

            if (configuration.circleToSearchEnabled) {
                ToggleRow(
                    label = stringResource(
                        R.string.label_circle_to_search_system_timing,
                        ViewConfiguration.getLongPressTimeout(),
                    ),
                    checked = configuration.circleToSearchFollowsSystemTiming,
                ) { updated ->
                    onUpdate { it.copy(circleToSearchFollowsSystemTiming = updated) }
                }

                if (!configuration.circleToSearchFollowsSystemTiming) {
                    ThresholdSlider(
                        label = stringResource(R.string.label_circle_to_search_hold),
                        value = configuration.circleToSearchHoldMs.toFloat(),
                        // Capped below the default arm window, after which a resting finger
                        // is no longer tracked and the long press could never fire.
                        range = SettingsLimits.CIRCLE_TO_SEARCH_HOLD_MS.toFloatRange(),
                        formatter = { stringResource(R.string.value_ms, it) },
                    ) { updated -> onUpdate { it.copy(circleToSearchHoldMs = updated.toLong()) } }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_circle_to_search_assistant),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                    )
                    TextButton(onClick = { openAssistantSettings(context) }) {
                        Text(stringResource(R.string.action_open_default_apps))
                    }
                }
            }
        }
    }
}

/** An empty tag means "follow the phone"; Android then picks values-pt or the English default. */
private enum class AppLanguage(val tag: String) { Automatic(""), Portuguese("pt-BR"), English("en") }

/**
 * Per-app language needs Android 13's LocaleManager; on older versions the app simply
 * follows the phone, so the card is not shown at all.
 */
@Composable
private fun LanguageCard() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    LanguageCardContent()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LanguageCardContent() {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(LocaleManager::class.java) } ?: return
    var selected by remember { mutableStateOf(currentLanguage(manager)) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CardTitle(
                icon = R.drawable.ic_sym_language,
                title = stringResource(R.string.section_language),
            )
            Text(
                text = stringResource(R.string.section_language_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            AppLanguage.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected == option,
                            role = Role.RadioButton,
                            onClick = {
                                selected = option
                                // The system recreates the activity in the new language.
                                manager.applicationLocales = LocaleList.forLanguageTags(option.tag)
                            },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == option, onClick = null)
                    Text(
                        text = stringResource(
                            when (option) {
                                AppLanguage.Automatic -> R.string.language_automatic
                                AppLanguage.Portuguese -> R.string.language_portuguese
                                AppLanguage.English -> R.string.language_english
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun currentLanguage(manager: LocaleManager): AppLanguage {
    val chosen = manager.applicationLocales
    if (chosen.isEmpty) return AppLanguage.Automatic
    return if (chosen[0].language == "pt") AppLanguage.Portuguese else AppLanguage.English
}

/** Credit where it is due: AnyGest, the original app and the Circle to Search technique. */
@Composable
private fun CreditsContent() {
    val context = LocalContext.current
    CreditsBlock(
        title = stringResource(R.string.credits_fork_title),
        body = stringResource(R.string.credits_fork_intro),
    )
    stringArrayResource(R.array.credits_fork_items).forEach { item ->
        Text(
            text = "•  $item",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
        )
    }
    CreditsBlock(
        title = stringResource(R.string.credits_original_title),
        body = stringResource(R.string.credits_original_body),
    )
    stringArrayResource(R.array.credits_original_items).forEach { item ->
        Text(
            text = "•  $item",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
        )
    }
    CreditsBlock(
        title = stringResource(R.string.credits_cts_title),
        body = stringResource(R.string.credits_cts_body),
    )
    Text(
        text = stringResource(R.string.credits_license),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    listOf(
        R.string.credits_link_fork to R.string.url_credits_fork,
        R.string.credits_link_hypergesture to R.string.url_credits_hypergesture,
        R.string.credits_link_micts to R.string.url_credits_micts,
        R.string.credits_link_ctslauncher to R.string.url_credits_ctslauncher,
    ).forEach { (label, url) ->
        val target = stringResource(url)
        TextButton(onClick = { context.startFirst(Intent(Intent.ACTION_VIEW, target.toUri())) }) {
            Text(stringResource(label))
        }
    }
}

@Composable
private fun CreditsBlock(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(text = body, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun UsageAccessRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(SetupFactsReader.hasUsageAccess(context)) }
    // Settings grants the op in another activity, so the row reads it again on the way back.
    LifecycleResumeEffect(Unit) {
        granted = SetupFactsReader.hasUsageAccess(context)
        onPauseOrDispose {}
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { granted = SetupFactsReader.hasUsageAccess(context) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (granted) R.string.label_usage_access_granted
                else R.string.label_usage_access_missing,
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        if (!granted) {
            TextButton(onClick = {
                context.startFirst(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Text(stringResource(R.string.action_grant_usage_access))
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Weighted so a long label wraps instead of squeezing the switch.
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HapticStrengthChoice(selected: HapticStrength, onChoose: (HapticStrength) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.label_haptic_strength),
            style = MaterialTheme.typography.bodyMedium,
        )
        HapticStrength.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == option,
                        role = Role.RadioButton,
                        onClick = { onChoose(option) },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == option, onClick = null)
                Text(
                    text = stringResource(
                        when (option) {
                            HapticStrength.Light -> R.string.haptic_light
                            HapticStrength.Normal -> R.string.haptic_normal
                            HapticStrength.Strong -> R.string.haptic_strong
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsContent(diagnostics: Diagnostics, onRefresh: () -> Unit) {
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
    Text(
        text = stringResource(R.string.diag_local_only),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    val context = LocalContext.current
    val sourceUrl = stringResource(R.string.url_source)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onRefresh) {
            Text(stringResource(R.string.action_refresh_diagnostics))
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
