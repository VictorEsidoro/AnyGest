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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Lets a section screen mark its cards. The search box can then scroll to one of them and make
 * it glow for a moment.
 */
internal class AnchorScope(
    private val focus: FocusRequest?,
    private val onPlaced: (String, Int) -> Unit,
) {
    @Composable
    fun Anchor(id: String, content: @Composable () -> Unit) {
        val glow = remember { Animatable(0f) }
        LaunchedEffect(focus) {
            if (focus?.anchor == id) {
                glow.snapTo(1f)
                delay(GLOW_HOLD_MS)
                glow.animateTo(0f, tween(GLOW_FADE_MS))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onPlaced(id, it.positionInParent().y.roundToInt()) }
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = glow.value),
                    shape = MaterialTheme.shapes.medium,
                ),
        ) {
            content()
        }
    }

    private companion object {
        const val GLOW_HOLD_MS = 500L
        const val GLOW_FADE_MS = 1_500
    }
}

/** A section's own screen: a back arrow, its title and its cards in a scrolling column. */
@Composable
internal fun SectionScreen(
    section: Section,
    focus: FocusRequest?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnchorScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val tops = remember(section) { mutableStateMapOf<String, Int>() }
    val anchors = remember(section, focus) { AnchorScope(focus) { id, y -> tops[id] = y } }
    val margin = with(LocalDensity.current) { 16.dp.toPx().roundToInt() }
    val goal = focus?.let { tops[it.anchor] }
    LaunchedEffect(focus, goal) {
        if (goal != null) scroll.animateScrollTo((goal - margin).coerceAtLeast(0))
    }
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
            }
            Text(
                text = stringResource(section.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            anchors.content()
        }
    }
}
