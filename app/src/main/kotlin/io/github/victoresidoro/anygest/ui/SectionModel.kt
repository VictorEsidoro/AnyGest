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
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.victoresidoro.anygest.R

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
    add(SearchTarget("shortcut", Section.GESTURES, "pause", R.string.pause_shortcut_title, R.string.search_kw_shortcut))
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
