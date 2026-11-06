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

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFilterTest {

    private val choices = listOf(
        AppChoice("com.example.bank", "Example Bank"),
        AppChoice("br.com.other.bank", "Other Bank"),
        AppChoice("com.android.chrome", "Chrome"),
    )

    @Test
    fun `an empty search keeps everything not already listed`() {
        val shown = filterChoices(choices, exclude = setOf("com.example.bank"), query = "  ")

        assertEquals(listOf("br.com.other.bank", "com.android.chrome"), shown.map { it.packageName })
    }

    @Test
    fun `the search matches the name or the package, ignoring case`() {
        assertEquals(listOf("Other Bank"), filterChoices(choices, emptySet(), "other").map { it.label })
        assertEquals(listOf("Example Bank"), filterChoices(choices, emptySet(), "EXAMPLE.BANK").map { it.label })
    }

    @Test
    fun `an app already on the list never shows up`() {
        assertEquals(emptyList<AppChoice>(), filterChoices(choices, setOf("com.example.bank"), "nubank"))
    }
}
