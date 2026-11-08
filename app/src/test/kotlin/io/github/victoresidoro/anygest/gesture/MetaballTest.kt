// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
//
// This file is part of HyperGesture.
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class MetaballTest {

    private fun neck(topY: Float) = Metaball.neck(
        bottomX = 0f, bottomY = 40f, bottomRadius = 40f,
        topX = 0f, topY = topY, topRadius = 16f,
        edgeY = 0f, riseDistance = 12f,
        topSpread = 0.6f, bottomStartSpread = 0.9f, bottomEndSpread = 0.7f,
        handleRate = 2.4f, footDrop = 1f,
    )

    @Test
    fun `nested circles have no neck`() {
        assertNull(
            Metaball.neck(
                0f, 0f, 40f, 0f, 5f, 16f, 0f, 12f, 0.6f, 0.9f, 0.7f, 2.4f, 1f,
            ),
        )
    }

    @Test
    fun `the neck meets the top circle on its rim`() {
        val n = requireNotNull(neck(-40f))
        assertEquals(16f, hypot(n[8], n[9] + 40f), 0.01f)
        assertEquals(16f, hypot(n[12], n[13] + 40f), 0.01f)
        // And runs through the top circle's centre.
        assertEquals(0f, n[10], 0f)
        assertEquals(-40f, n[11], 0f)
    }

    @Test
    fun `the feet spread wider than the neck's top`() {
        val n = requireNotNull(neck(-40f))
        assertTrue(n[2] > n[8])
    }
}
