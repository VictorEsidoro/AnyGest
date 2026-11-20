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

package io.github.victoresidoro.anygest.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeConfirmTest {

    private fun asking(landscape: Boolean = true) =
        LandscapeConfirm().apply { configure(enabled = true, landscape = landscape) }

    private fun LandscapeConfirm.swipe(action: GestureAction, at: Long, edge: ScreenEdge = ScreenEdge.BOTTOM): ConfirmVerdict {
        onDown(edge, at)
        val verdict = onFire(action, edge, at + 50L)
        onStreamEnd(edge, at + 100L)
        return verdict
    }

    @Test
    fun `the first guarded swipe is held back and the second one inside the window goes through`() {
        val confirm = asking()
        assertEquals(ConfirmVerdict.ShowPill(50L + ConfirmPill.WINDOW_MS), confirm.swipe(GestureAction.Home, 0L))
        assertTrue(confirm.pillShown)
        assertEquals(ConfirmVerdict.Perform, confirm.swipe(GestureAction.Home, 1_000L))
        assertFalse(confirm.pillShown)
    }

    @Test
    fun `every bottom action except Circle to Search is guarded`() {
        listOf(GestureAction.Home, GestureAction.Recents, GestureAction.PreviousApp, GestureAction.NextApp).forEach {
            assertTrue("$it", asking().swipe(it, 0L) is ConfirmVerdict.ShowPill)
        }
        assertEquals(ConfirmVerdict.Perform, asking().swipe(GestureAction.CircleToSearch, 0L))
    }

    @Test
    fun `a different action can answer the pill`() {
        val confirm = asking()
        confirm.swipe(GestureAction.Home, 0L)
        assertEquals(ConfirmVerdict.Perform, confirm.swipe(GestureAction.Recents, 500L))
    }

    @Test
    fun `Back from the sides is never held back`() {
        val confirm = asking()
        assertEquals(ConfirmVerdict.Perform, confirm.swipe(GestureAction.Back, 0L, ScreenEdge.LEFT))
        assertEquals(ConfirmVerdict.Perform, confirm.swipe(GestureAction.Back, 0L, ScreenEdge.RIGHT))
        assertFalse(confirm.pillShown)
    }

    @Test
    fun `the pill times out after exactly the window`() {
        val confirm = asking()
        confirm.onFire(GestureAction.Home, ScreenEdge.BOTTOM, 1_000L)
        confirm.expire(1_000L + ConfirmPill.WINDOW_MS - 1L)
        assertTrue(confirm.pillShown)
        confirm.expire(1_000L + ConfirmPill.WINDOW_MS)
        assertFalse(confirm.pillShown)
        assertTrue(confirm.swipe(GestureAction.Home, 4_000L) is ConfirmVerdict.ShowPill)
    }

    @Test
    fun `a touch that began inside the window is still the answer after the window ends`() {
        val confirm = asking()
        confirm.onFire(GestureAction.Home, ScreenEdge.BOTTOM, 0L)
        confirm.onDown(ScreenEdge.BOTTOM, ConfirmPill.WINDOW_MS - 10L)
        confirm.expire(ConfirmPill.WINDOW_MS + 500L)
        assertTrue(confirm.pillShown)
        assertEquals(ConfirmVerdict.Perform, confirm.onFire(GestureAction.Home, ScreenEdge.BOTTOM, ConfirmPill.WINDOW_MS + 600L))
    }

    @Test
    fun `a touch on a side edge does not answer a bottom pill`() {
        val confirm = asking()
        confirm.onFire(GestureAction.Home, ScreenEdge.BOTTOM, 0L)
        confirm.swipe(GestureAction.Back, 100L, ScreenEdge.LEFT)
        assertTrue(confirm.pillShown)
        assertEquals(ConfirmVerdict.Perform, confirm.swipe(GestureAction.Home, 300L))
    }

    @Test
    fun `portrait and a disabled setting go straight through`() {
        assertEquals(ConfirmVerdict.Perform, asking(landscape = false).swipe(GestureAction.Home, 0L))
        val off = LandscapeConfirm().apply { configure(enabled = false, landscape = true) }
        assertEquals(ConfirmVerdict.Perform, off.swipe(GestureAction.Home, 0L))
    }

    @Test
    fun `turning the setting off or rotating drops a pill that is up`() {
        val off = asking().also { it.swipe(GestureAction.Home, 0L) }
        off.configure(enabled = false, landscape = true)
        assertFalse(off.pillShown)

        val turned = asking().also { it.swipe(GestureAction.Home, 0L) }
        turned.configure(enabled = true, landscape = false)
        assertFalse(turned.pillShown)
        assertEquals(ConfirmVerdict.Perform, turned.swipe(GestureAction.Home, 500L))
    }

    @Test
    fun `rotating into landscape starts asking, and a rebuild in the same orientation keeps the pill`() {
        val confirm = asking(landscape = false)
        assertEquals(ConfirmVerdict.Perform, confirm.swipe(GestureAction.Home, 0L))
        confirm.configure(enabled = true, landscape = true)
        assertTrue(confirm.swipe(GestureAction.Home, 100L) is ConfirmVerdict.ShowPill)
        confirm.configure(enabled = true, landscape = true)
        assertTrue(confirm.pillShown)
    }

    @Test
    fun `only a bottom touch handed back to the app drops the pill`() {
        val confirm = asking().also { it.swipe(GestureAction.Home, 0L) }
        confirm.onReplayed(ScreenEdge.LEFT)
        assertTrue(confirm.pillShown)
        confirm.onReplayed(ScreenEdge.BOTTOM)
        assertFalse(confirm.pillShown)
    }

    @Test
    fun `the pill reports when it goes away`() {
        val confirm = asking()
        assertEquals(null, confirm.expiresAtMs)
        confirm.onFire(GestureAction.Home, ScreenEdge.BOTTOM, 700L)
        assertEquals(700L + ConfirmPill.WINDOW_MS, confirm.expiresAtMs)
        confirm.dismiss()
        assertEquals(null, confirm.expiresAtMs)
    }
}
