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

import java.text.Normalizer
import java.util.Locale

/** One function of the app as the search box sees it: what it is called and how to find it. */
internal data class SearchItem(
    val id: String,
    val title: String,
    val keywords: String,
    val section: String,
)

/**
 * The search box behind the home screen. Plain strings in and out, so it runs in a JVM test.
 * Accents and case are ignored, every word typed must match somewhere, and a match in the
 * name counts more than one in the keywords, which counts more than one in the section name.
 */
internal object SettingsSearch {

    fun rank(query: String, items: List<SearchItem>): List<SearchItem> {
        val words = words(query)
        if (words.isEmpty()) return emptyList()
        return items
            .mapNotNull { item -> score(item, words)?.let { item to it } }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Where the typed words appear in [text], for highlighting. Indices are in [text] itself:
     * each character is folded on its own, so "Notificação" still lines up with "notificacao".
     */
    fun matches(text: String, query: String): List<IntRange> {
        val folded = text.map(::foldChar).joinToString("")
        val ranges = mutableListOf<IntRange>()
        words(query).forEach { word ->
            var at = folded.indexOf(word)
            while (at >= 0) {
                ranges += at until at + word.length
                at = folded.indexOf(word, at + 1)
            }
        }
        return merge(ranges)
    }

    fun fold(text: String): String = text.map(::foldChar).joinToString("")

    private fun words(query: String): List<String> =
        fold(query).split(' ', '\t', '\n').filter { it.isNotEmpty() }

    private fun score(item: SearchItem, words: List<String>): Int? {
        val title = fold(item.title)
        val keywords = fold(item.keywords)
        val section = fold(item.section)
        var total = 0
        for (word in words) {
            total += when {
                title.startsWith(word) || title.contains(" $word") -> START_OF_TITLE
                title.contains(word) -> IN_TITLE
                keywords.contains(word) -> IN_KEYWORDS
                section.contains(word) -> IN_SECTION
                else -> return null
            }
        }
        return total
    }

    private fun foldChar(c: Char): Char =
        Normalizer.normalize(c.toString(), Normalizer.Form.NFD).first().lowercaseChar()
            .let { if (it.isWhitespace()) ' ' else it }

    private fun merge(ranges: List<IntRange>): List<IntRange> {
        val sorted = ranges.sortedBy { it.first }
        val out = mutableListOf<IntRange>()
        for (range in sorted) {
            val last = out.lastOrNull()
            if (last != null && range.first <= last.last + 1) {
                out[out.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                out += range
            }
        }
        return out
    }

    private const val START_OF_TITLE = 4
    private const val IN_TITLE = 3
    private const val IN_KEYWORDS = 2
    private const val IN_SECTION = 1
}
