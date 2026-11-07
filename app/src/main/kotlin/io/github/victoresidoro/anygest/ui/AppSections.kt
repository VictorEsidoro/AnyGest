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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** The screens the home screen opens. Each one holds the cards that used to sit in one long list. */
internal enum class Section(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @DrawableRes val icon: Int,
) {
    GESTURES(R.string.section_title_gestures, R.string.section_summary_gestures, R.drawable.ic_sym_gesture),
    BAR(R.string.section_title_bar, R.string.section_summary_bar, R.drawable.ic_sym_bottom_navigation),
    APP(R.string.section_title_app, R.string.section_summary_app, R.drawable.ic_sym_dashboard_customize),
    ADB(R.string.section_title_adb, R.string.section_summary_adb, R.drawable.ic_sym_tune),
    ABOUT(R.string.section_title_about, R.string.section_summary_about, R.drawable.ic_sym_bug_report),
}

/** Where the search box sends the user: a section (null = the home screen) and a card in it. */
internal data class SearchTarget(
    val id: String,
    val section: Section?,
    val anchor: String,
    @StringRes val title: Int,
    @StringRes val keywords: Int,
)

/** Asks a section screen to scroll to a card and make it glow; a new tick repeats the same card. */
internal data class FocusRequest(val anchor: String, val tick: Long = System.nanoTime())

/**
 * Every function the search box can find. A new card goes here with its anchor, so it can be
 * found the day it ships.
 */
internal fun searchTargets(navigationAvailable: Boolean): List<SearchTarget> = buildList {
    add(SearchTarget("setup", null, "setup", R.string.setup_title, R.string.search_kw_setup))
    add(SearchTarget("cheatsheet", Section.GESTURES, "cheatsheet", R.string.gestures_title, R.string.search_kw_cheatsheet))
    add(SearchTarget("appswitch", Section.GESTURES, "appswitch", R.string.section_app_switch, R.string.search_kw_appswitch))
    add(SearchTarget("circle", Section.GESTURES, "circle", R.string.section_circle_to_search, R.string.search_kw_circle))
    add(SearchTarget("pause", Section.GESTURES, "pause", R.string.pause_card_title, R.string.search_kw_pause))
    add(SearchTarget("tuning", Section.GESTURES, "tuning", R.string.section_gestures, R.string.search_kw_tuning))
    if (navigationAvailable) {
        add(SearchTarget("navbar", Section.BAR, "navbar", R.string.navigation_card_title, R.string.search_kw_navbar))
    }
    add(SearchTarget("tile", Section.BAR, "tile", R.string.tile_card_title, R.string.search_kw_tile))
    if (navigationAvailable) {
        add(SearchTarget("navtile", Section.BAR, "navtile", R.string.navigation_tile_card_title, R.string.search_kw_navtile))
    }
    add(SearchTarget("alerts", Section.APP, "alerts", R.string.alerts_card_title, R.string.search_kw_alerts))
    add(SearchTarget("language", Section.APP, "language", R.string.section_language, R.string.search_kw_language))
    add(SearchTarget("recents", Section.APP, "recents", R.string.recents_card_title, R.string.search_kw_recents))
    add(SearchTarget("keepalive", Section.APP, "keepalive", R.string.keepalive_card_title, R.string.search_kw_keepalive))
    add(SearchTarget("hidenotif", Section.APP, "keepalive", R.string.keepalive_action_hide, R.string.search_kw_hidenotif))
    add(SearchTarget("adb", Section.ADB, "adb", R.string.adb_card_title, R.string.search_kw_adb))
    add(SearchTarget("restore", Section.ADB, "adb", R.string.adb_toggle_restore, R.string.search_kw_restore))
    add(SearchTarget("hidebar", Section.ADB, "adb", R.string.adb_toggle_hide_bar, R.string.search_kw_hidebar))
    add(SearchTarget("extras", Section.ADB, "extras", R.string.adb_extras_title, R.string.search_kw_extras))
    add(SearchTarget("battery", Section.ADB, "extras", R.string.adb_cmd_battery_title, R.string.search_kw_battery))
    add(SearchTarget("usage", Section.ADB, "extras", R.string.adb_cmd_usage_title, R.string.search_kw_usage))
    add(SearchTarget("restricted", Section.ADB, "extras", R.string.adb_cmd_restricted_title, R.string.search_kw_restricted))
    add(SearchTarget("diagnostics", Section.ABOUT, "diagnostics", R.string.section_diagnostics, R.string.search_kw_diagnostics))
    add(SearchTarget("copylog", Section.ABOUT, "diagnostics", R.string.action_copy_log, R.string.search_kw_copylog))
    add(SearchTarget("credits", Section.ABOUT, "credits", R.string.section_credits, R.string.search_kw_credits))
}

@Composable
internal fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { CardIcon(R.drawable.ic_sym_search) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
    )
}

/** The five sections as one card of rows, the home screen's table of contents. */
@Composable
internal fun SectionList(onOpen: (Section) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Section.entries.forEachIndexed { index, section ->
            if (index > 0) HorizontalDivider()
            NavigationRow(
                icon = section.icon,
                title = AnnotatedString(stringResource(section.title)),
                subtitle = stringResource(section.summary),
                onClick = { onOpen(section) },
            )
        }
    }
}

@Composable
internal fun SearchResults(
    query: String,
    results: List<Pair<SearchTarget, SearchItem>>,
    onPick: (SearchTarget) -> Unit,
) {
    if (results.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_empty, query.trim()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.search_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    val mark = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        results.forEachIndexed { index, (target, item) ->
            if (index > 0) HorizontalDivider()
            NavigationRow(
                icon = target.section?.icon ?: R.drawable.ic_sym_checklist,
                title = highlighted(item.title, query, mark),
                subtitle = item.section,
                onClick = { onPick(target) },
            )
        }
    }
}

private fun highlighted(text: String, query: String, mark: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        append(text)
        SettingsSearch.matches(text, query).forEach { addStyle(mark, it.first, it.last + 1) }
    }

@Composable
private fun NavigationRow(
    @DrawableRes icon: Int,
    title: AnnotatedString,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Lets a section screen mark its cards, so the search box can scroll to one and make it glow. */
internal class AnchorScope(
    private val focus: FocusRequest?,
    private val onPlaced: (String, Int) -> Unit,
) {
    @Composable
    fun Anchor(id: String, content: @Composable () -> Unit) {
        val glow = remember { Animatable(0f) }
        LaunchedEffect(focus) {
            if (focus?.anchor == id) {
                glow.snapTo(1f)
                delay(GLOW_HOLD_MS)
                glow.animateTo(0f, tween(GLOW_FADE_MS))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onPlaced(id, it.positionInParent().y.roundToInt()) }
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = glow.value),
                    shape = MaterialTheme.shapes.medium,
                ),
        ) {
            content()
        }
    }

    private companion object {
        const val GLOW_HOLD_MS = 500L
        const val GLOW_FADE_MS = 1_500
    }
}

/** A section's own screen: a back arrow, its title, and its cards in a scrolling column. */
@Composable
internal fun SectionScreen(
    section: Section,
    focus: FocusRequest?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnchorScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val positions = remember(section) { mutableStateMapOf<String, Int>() }
    val anchors = remember(section, focus) { AnchorScope(focus) { id, y -> positions[id] = y } }
    val margin = with(LocalDensity.current) { 16.dp.toPx().roundToInt() }
    val target = focus?.let { positions[it.anchor] }
    LaunchedEffect(focus, target) {
        if (target != null) scroll.animateScrollTo((target - margin).coerceAtLeast(0))
    }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(section.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            anchors.content()
        }
    }
}

/** Builds the searchable items once per language, from the targets' string resources. */
@Composable
internal fun rememberSearchItems(targets: List<SearchTarget>): List<Pair<SearchTarget, SearchItem>> {
    val home = stringResource(R.string.section_title_home)
    return targets.map { target ->
        val section = target.section?.let { stringResource(it.title) } ?: home
        target to SearchItem(
            id = target.id,
            title = stringResource(target.title),
            keywords = stringResource(target.keywords),
            section = section,
        )
    }
}
