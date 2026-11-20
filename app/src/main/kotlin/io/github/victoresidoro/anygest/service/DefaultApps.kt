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

import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter

/**
 * The apps the system has stored as the default for something (Home, links, calls...).
 *
 * A broadcast says only that one of them changed, never which. A launcher that warns about
 * being reset, and a default that changes while nobody touched the settings, can only be
 * explained by what moved, so each snapshot is compared with the last.
 */
internal object DefaultApps {

    /** One line per stored default: what it answers and who answers it. Empty when it cannot be read. */
    @Suppress("DEPRECATION")
    fun snapshot(context: Context): Set<String> = runCatching {
        val filters = ArrayList<IntentFilter>()
        val activities = ArrayList<ComponentName>()
        context.packageManager.getPreferredActivities(filters, activities, null)
        filters.indices.map { "${describe(filters[it])} -> ${activities[it].packageName}" }.toSet()
    }.getOrDefault(emptySet())

    private fun describe(filter: IntentFilter): String {
        val actions = filter.actionsIterator().asSequence().map(::short).joinToString("|")
        val categories = filter.categoriesIterator()?.asSequence()?.map(::short)?.joinToString("|").orEmpty()
        val schemes = (0 until filter.countDataSchemes()).joinToString("|") { filter.getDataScheme(it) }
        return listOf(actions, categories, schemes).filter { it.isNotEmpty() }.joinToString(" ")
    }

    internal fun short(name: String): String = name.substringAfterLast('.')

    /** What was added and removed between two snapshots, "none" when they match. */
    fun changes(before: Set<String>, after: Set<String>, shown: Int = 4): String {
        val lines = (after - before).sorted().map { "+$it" } + (before - after).sorted().map { "-$it" }
        if (lines.isEmpty()) return "none"
        val text = lines.take(shown).joinToString(", ")
        return if (lines.size > shown) "$text (+${lines.size - shown} more)" else text
    }
}
