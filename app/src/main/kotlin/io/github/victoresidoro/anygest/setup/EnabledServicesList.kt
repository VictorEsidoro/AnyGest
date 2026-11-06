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

package io.github.victoresidoro.anygest.setup

/**
 * `enabled_accessibility_services` is a colon-separated list of components, and the system
 * itself may write an entry in the short form (`pkg/.Class`) or the full one. Plain strings
 * so it runs in a JVM test without the framework.
 */
internal object EnabledServicesList {

    fun contains(current: String?, component: String): Boolean {
        val wanted = normalize(component)
        return entries(current).any { normalize(it) == wanted }
    }

    /** [current] with [component] added once; every other entry is kept as it was. */
    fun with(current: String?, component: String): String {
        if (contains(current, component)) return entries(current).joinToString(SEPARATOR)
        return (entries(current) + component).joinToString(SEPARATOR)
    }

    /** [current] without [component], in either spelling; every other entry is kept. */
    fun without(current: String?, component: String): String {
        val wanted = normalize(component)
        return entries(current).filter { normalize(it) != wanted }.joinToString(SEPARATOR)
    }

    private fun entries(current: String?): List<String> = current.orEmpty()
        .split(SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "null" }

    /** Expands `pkg/.Class` to `pkg/pkg.Class` so both spellings compare equal. */
    internal fun normalize(entry: String): String {
        val slash = entry.indexOf('/')
        if (slash < 0) return entry
        val pkg = entry.substring(0, slash)
        val cls = entry.substring(slash + 1)
        return "$pkg/${if (cls.startsWith(".")) pkg + cls else cls}"
    }

    private const val SEPARATOR = ":"
}
