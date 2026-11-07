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
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    private val items = listOf(
        SearchItem("alerts", "Alertas", "notificação aviso gestos pararam", "Avisos e app"),
        SearchItem("keepalive", "Manter os gestos vivos", "notificação persistente recentes", "Avisos e app"),
        SearchItem("hidenotif", "Esconder notificação", "canal silenciosa", "Avisos e app"),
        SearchItem("navbar", "Barra de botões", "navegação esconder", "Barra e atalhos"),
        SearchItem("tile", "Atalho no Centro de controle", "tile ligar desligar", "Barra e atalhos"),
        SearchItem("pause", "Apps em exceção", "nubank banco pausa", "Gestos"),
    )

    @Test
    fun `an empty query finds nothing`() {
        assertTrue(SettingsSearch.rank("   ", items).isEmpty())
    }

    @Test
    fun `accents and case are ignored`() {
        assertEquals("hidenotif", SettingsSearch.rank("NOTIFICACAO", items).first().id)
    }

    @Test
    fun `a match in the name ranks above one in the keywords`() {
        assertEquals(
            listOf("hidenotif", "alerts", "keepalive"),
            SettingsSearch.rank("notificação", items).map { it.id },
        )
    }

    @Test
    fun `keywords find functions whose name does not say it`() {
        assertEquals(listOf("pause"), SettingsSearch.rank("nubank", items).map { it.id })
    }

    @Test
    fun `the section name is searched last`() {
        assertEquals(listOf("navbar", "tile"), SettingsSearch.rank("barra", items).map { it.id })
    }

    @Test
    fun `every word must match`() {
        assertEquals(listOf("keepalive"), SettingsSearch.rank("gestos vivos", items).map { it.id })
        assertTrue(SettingsSearch.rank("gestos xyz", items).isEmpty())
    }

    @Test
    fun `matches line up with the original text`() {
        assertEquals(listOf(9..19), SettingsSearch.matches("Esconder notificação", "notificacao"))
    }

    @Test
    fun `overlapping matches are merged`() {
        assertEquals(listOf(0..4), SettingsSearch.matches("Barra", "bar arra"))
    }
}
