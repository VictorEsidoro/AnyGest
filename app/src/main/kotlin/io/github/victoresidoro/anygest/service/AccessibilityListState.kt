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

package io.github.victoresidoro.anygest.service

/** The enabled accessibility services in a few words, for the notes that follow a change of the list. */
internal object AccessibilityListState {

    /**
     * [raw] is the `enabled_accessibility_services` setting and [enabledFlag] the global
     * `accessibility_enabled` switch. Who else is listed shows what else may have rewritten it.
     */
    fun describe(raw: String?, enabledFlag: Int?, ownPackage: String): String {
        val packages = raw.orEmpty().split(':')
            .map { it.trim().substringBefore('/') }
            .filter { it.isNotEmpty() && it != "null" }
        val others = packages.filter { it != ownPackage }.distinct()
        return "accessibility_enabled=${enabledFlag ?: "?"}, AnyGest listed=${ownPackage in packages}, " +
            "others=${if (others.isEmpty()) "none" else others.joinToString(",")}"
    }
}
