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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import io.github.victoresidoro.anygest.R

/** The pill-shaped box at the top of the home screen. */
@Composable
internal fun SearchField(query: String, onQuery: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHighest,
            unfocusedContainerColor = scheme.surfaceContainerHighest,
            unfocusedBorderColor = scheme.outlineVariant,
        ),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { CardIcon(R.drawable.ic_sym_search) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
    )
}

/** The five sections as one card of rows: the home screen's table of contents. */
@Composable
internal fun SectionList(onOpen: (Section) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Section.entries.forEachIndexed { index, section ->
            if (index > 0) HorizontalDivider()
            NavigationRow(
                icon = section.icon,
                title = AnnotatedString(stringResource(section.title)),
                subtitle = stringResource(section.summary),
                onClick = { onOpen(section) },
            )
        }
    }
}

@Composable
internal fun SearchResults(
    query: String,
    results: List<Pair<SearchTarget, SearchItem>>,
    onPick: (SearchTarget) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    if (results.isEmpty()) {
        Panel(gap = 6) {
            Text(
                text = stringResource(R.string.search_empty, query.trim()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.search_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        return
    }
    val mark = SpanStyle(background = scheme.primaryContainer, color = scheme.onPrimaryContainer)
    Card(Modifier.fillMaxWidth()) {
        results.forEachIndexed { index, (target, item) ->
            if (index > 0) HorizontalDivider()
            NavigationRow(
                icon = target.section?.icon ?: R.drawable.ic_sym_checklist,
                title = withMarks(item.title, query, mark),
                subtitle = item.section,
                onClick = { onPick(target) },
            )
        }
    }
}

/** [text] with the words of [query] painted in [mark]. */
private fun withMarks(text: String, query: String, mark: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(text)
    for (range in SettingsSearch.matches(text, query)) addStyle(mark, range.first, range.last + 1)
}

@Composable
private fun NavigationRow(@DrawableRes icon: Int, title: AnnotatedString, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(scheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = scheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Note(subtitle)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}
