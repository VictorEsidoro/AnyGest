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

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R
import io.github.victoresidoro.anygest.ui.theme.onSuccessContainer
import io.github.victoresidoro.anygest.ui.theme.onWarningContainer
import io.github.victoresidoro.anygest.ui.theme.successContainer
import io.github.victoresidoro.anygest.ui.theme.warningContainer

// The few building blocks every card is made of. A card is a Panel with a Heading and some
// text and rows; keeping them here is what lets the cards stay short and look alike.

/** How a chip reads at a glance. */
internal enum class Tone { Good, Neutral, Caution, Alert }

/** The chip's background and text colours for a [Tone]. */
@Composable
internal fun Tone.colours(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        Tone.Good -> successContainer to onSuccessContainer
        Tone.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
        Tone.Caution -> warningContainer to onWarningContainer
        Tone.Alert -> scheme.errorContainer to scheme.onErrorContainer
    }
}

@Composable
internal fun Chip(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val (background, ink) = tone.colours()
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ink,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A white card with its content in a padded column. */
@Composable
internal fun Panel(
    modifier: Modifier = Modifier,
    gap: Int = 10,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(gap.dp),
            content = content,
        )
    }
}

/** The card's thin accent-coloured symbol. Decorative. */
@Composable
internal fun CardIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(24.dp),
    )
}

/** A card's first line: its symbol, its name and, if there is one, a chip on the right. */
@Composable
internal fun Heading(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    chip: (@Composable () -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CardIcon(icon)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (chip != null) {
            Spacer(Modifier.width(12.dp))
            chip()
        }
    }
}

/** Running text. */
@Composable
internal fun Para(text: String, modifier: Modifier = Modifier) =
    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)

/** Smaller, muted text for hints and explanations. */
@Composable
internal fun Note(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier,
)

/** Commands and logs, which must not reflow. */
@Composable
internal fun Mono(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    fontFamily = FontFamily.Monospace,
    modifier = modifier,
)

/** A label that takes the room it needs and wraps, with something small at the end of the line. */
@Composable
internal fun LabelledRow(
    label: String,
    modifier: Modifier = Modifier,
    end: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        end()
    }
}

@Composable
internal fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    LabelledRow(label, Modifier.padding(vertical = 4.dp)) {
        Switch(checked = checked, onCheckedChange = onChange)
    }

/** One option of a radio group, the whole line tappable. */
@Composable
private fun RadioLine(label: String, selected: Boolean, onChoose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onChoose)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Radio buttons for the values of an enum, each named by a string resource. */
@Composable
internal fun <T> ChoiceGroup(
    options: List<T>,
    selected: T,
    nameOf: (T) -> Int,
    title: String? = null,
    onChoose: (T) -> Unit,
) {
    Column {
        if (title != null) Para(title)
        options.forEach { option ->
            RadioLine(stringResource(nameOf(option)), option == selected) { onChoose(option) }
        }
    }
}

@Composable
internal fun FoldArrow(expanded: Boolean) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, label = "foldArrow")
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
        modifier = Modifier.rotate(turn),
    )
}

/**
 * A card that folds to its title line. [end] sits at the right of that line (an arrow or a
 * status chip); [body] shows under a divider while it is open.
 */
@Composable
internal fun FoldPanel(
    @DrawableRes icon: Int,
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    end: @Composable () -> Unit,
    bodyGap: Int = 4,
    body: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardIcon(icon)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Note(summary)
                }
                end()
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(bodyGap.dp),
                        content = body,
                    )
                }
            }
        }
    }
}
