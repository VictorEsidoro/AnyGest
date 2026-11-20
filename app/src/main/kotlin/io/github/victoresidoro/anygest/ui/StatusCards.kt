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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.service.KeepAliveService
import io.github.victoresidoro.anygest.setup.SetupStore

/** What a status card says and offers; [action] and [second] are the labels of up to two buttons. */
internal class StatusCardSpec(
    @DrawableRes val icon: Int,
    val title: String,
    val chip: String,
    val good: Boolean,
    val body: String,
    val hint: String? = null,
    val action: String? = null,
    val onAction: () -> Unit = {},
    val second: String? = null,
    val onSecond: () -> Unit = {},
)

/** A card with a title, a chip saying where things stand, a paragraph and at most two buttons. */
@Composable
internal fun StatusCard(spec: StatusCardSpec) = Panel {
    Heading(spec.icon, spec.title) {
        Chip(spec.chip, if (spec.good) Tone.Good else Tone.Neutral)
    }
    Para(spec.body)
    spec.hint?.let { Note(it) }
    if (spec.action != null || spec.second != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.action?.let { FilledTonalButton(onClick = spec.onAction) { Text(it) } }
            spec.second?.let { TextButton(onClick = spec.onSecond) { Text(it) } }
        }
    }
}

/**
 * Both quick-settings tiles use this. Android cannot say whether a tile is still there, so
 * "added" is only what was last seen: adding again is always offered, and the system answers
 * if it already is.
 */
@Composable
internal fun TileCard(
    @StringRes title: Int,
    @DrawableRes icon: Int,
    @StringRes body: Int,
    @StringRes manualHint: Int,
    added: Boolean,
    canRequest: Boolean,
    onRequest: () -> Unit,
) = StatusCard(
    StatusCardSpec(
        icon = icon,
        title = stringResource(title),
        chip = stringResource(if (added) R.string.tile_card_added else R.string.tile_card_missing),
        good = added,
        body = stringResource(body),
        hint = stringResource(if (added) R.string.tile_card_readd_hint else manualHint),
        action = stringResource(if (added) R.string.setup_tile_action_again else R.string.setup_tile_action)
            .takeIf { canRequest },
        onAction = onRequest,
    ),
)

@Composable
internal fun NavigationBarCard(hidden: Boolean, canChange: Boolean, onOpen: () -> Unit) = StatusCard(
    StatusCardSpec(
        icon = R.drawable.ic_sym_bottom_navigation,
        title = stringResource(R.string.navigation_card_title),
        chip = stringResource(if (hidden) R.string.navigation_card_hidden else R.string.navigation_card_visible),
        good = hidden,
        body = stringResource(if (canChange) R.string.navigation_card_body else R.string.navigation_card_locked),
        action = stringResource(R.string.action_open_navigation_settings).takeIf { canChange },
        onAction = onOpen,
    ),
)

@Composable
internal fun AlertsCard(enabled: Boolean, onToggle: () -> Unit) = StatusCard(
    StatusCardSpec(
        icon = R.drawable.ic_sym_dashboard_customize,
        title = stringResource(R.string.alerts_card_title),
        chip = stringResource(if (enabled) R.string.alerts_card_on else R.string.alerts_card_off),
        good = enabled,
        body = stringResource(R.string.alerts_card_body),
        action = stringResource(if (enabled) R.string.alerts_action_off else R.string.alerts_action_on),
        onAction = onToggle,
    ),
)

@Composable
internal fun RecentsCard() {
    val context = LocalContext.current
    var shown by remember { mutableStateOf(SetupStore(context).showInRecents) }
    StatusCard(
        StatusCardSpec(
            icon = R.drawable.ic_sym_tune,
            title = stringResource(R.string.recents_card_title),
            chip = stringResource(if (shown) R.string.recents_card_shown else R.string.recents_card_hidden),
            good = !shown,
            body = stringResource(R.string.recents_card_body),
            action = stringResource(if (shown) R.string.recents_action_hide else R.string.recents_action_show),
            onAction = {
                shown = !shown
                SetupStore(context).showInRecents = shown
                applyRecentsVisibility(context)
            },
        ),
    )
}

@Composable
internal fun KeepAliveCard() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(SetupStore(context).keepAlive) }
    StatusCard(
        StatusCardSpec(
            icon = R.drawable.ic_sym_tune,
            title = stringResource(R.string.keepalive_card_title),
            chip = stringResource(if (running) R.string.keepalive_card_on else R.string.keepalive_card_off),
            good = running,
            body = stringResource(R.string.keepalive_card_body),
            action = stringResource(if (running) R.string.keepalive_action_off else R.string.keepalive_action_on),
            onAction = {
                running = !running
                SetupStore(context).keepAlive = running
                KeepAliveService.refresh(context)
            },
            second = stringResource(R.string.keepalive_action_hide).takeIf { running },
            onSecond = { openKeepAliveChannel(context, KeepAliveService.channelSettings(context)) },
        ),
    )
}
