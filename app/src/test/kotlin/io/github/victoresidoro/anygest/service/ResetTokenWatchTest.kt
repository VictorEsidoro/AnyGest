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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetTokenWatchTest {

    @Test
    fun `three installs refused only for the reset token read as a lost connection`() {
        val loss = ResetTokenWatch()
        assertFalse(loss.installed(0, 3, 0))
        assertFalse(loss.installed(0, 3, 0))
        assertTrue(loss.installed(0, 3, 0))
    }

    @Test
    fun `another kind of failure starts the count again`() {
        val loss = ResetTokenWatch()
        loss.installed(0, 3, 0)
        loss.installed(0, 3, 0)
        assertFalse(loss.installed(0, 2, 1))
        assertFalse(loss.installed(0, 3, 0))
    }

    @Test
    fun `an install that attached a strip starts the count again`() {
        val loss = ResetTokenWatch()
        loss.installed(0, 3, 0)
        loss.installed(0, 3, 0)
        assertFalse(loss.installed(1, 2, 0))
        assertFalse(loss.installed(0, 3, 0))
    }

    @Test
    fun `an install with no refusal at all is not a loss`() {
        val loss = ResetTokenWatch(threshold = 1)
        assertFalse(loss.installed(0, 0, 0))
    }

    @Test
    fun `reset clears the streak`() {
        val loss = ResetTokenWatch()
        loss.installed(0, 3, 0)
        loss.installed(0, 3, 0)
        loss.reset()
        assertFalse(loss.installed(0, 3, 0))
    }

    @Test
    fun `only the reset-token message counts`() {
        assertTrue(ResetTokenWatch.isResetToken(RuntimeException("token null is not valid; is your activity running?")))
        assertFalse(ResetTokenWatch.isResetToken(RuntimeException("something else")))
        assertFalse(ResetTokenWatch.isResetToken(RuntimeException()))
    }
}
