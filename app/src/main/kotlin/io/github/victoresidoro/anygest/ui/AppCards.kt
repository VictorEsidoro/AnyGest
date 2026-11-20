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

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.victoresidoro.anygest.R

/** An empty tag means "follow the phone": Android then picks values-pt or the English default. */
private enum class AppLanguage(val tag: String, @StringRes val label: Int) {
    Automatic("", R.string.language_automatic),
    Portuguese("pt-BR", R.string.language_portuguese),
    English("en", R.string.language_english),
}

/** Per-app language needs Android 13's LocaleManager; before that the card is not shown at all. */
@Composable
internal fun LanguageCard() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) LanguageChoices()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LanguageChoices() {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(LocaleManager::class.java) } ?: return
    var current by remember { mutableStateOf(languageOf(manager)) }
    Panel(gap = 4) {
        Heading(R.drawable.ic_sym_language, stringResource(R.string.section_language))
        Note(stringResource(R.string.section_language_body), Modifier.padding(bottom = 4.dp))
        ChoiceGroup(
            options = AppLanguage.entries,
            selected = current,
            nameOf = AppLanguage::label,
        ) { chosen ->
            current = chosen
            // Android recreates the activity in the new language.
            manager.applicationLocales = LocaleList.forLanguageTags(chosen.tag)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun languageOf(manager: LocaleManager): AppLanguage {
    val locales = manager.applicationLocales
    return when {
        locales.isEmpty -> AppLanguage.Automatic
        locales[0].language == "pt" -> AppLanguage.Portuguese
        else -> AppLanguage.English
    }
}

/** The credits: this app, the original it is based on, and the Circle to Search technique. */
@Composable
internal fun CreditsContent() {
    val context = LocalContext.current
    CreditBlock(R.string.credits_fork_title, R.string.credits_fork_intro, R.array.credits_fork_items)
    CreditBlock(R.string.credits_original_title, R.string.credits_original_body, R.array.credits_original_items)
    CreditBlock(R.string.credits_cts_title, R.string.credits_cts_body, null)
    Note(stringResource(R.string.credits_license), Modifier.padding(top = 12.dp))
    listOf(
        R.string.credits_link_fork to R.string.url_credits_fork,
        R.string.credits_link_hypergesture to R.string.url_credits_hypergesture,
        R.string.credits_link_micts to R.string.url_credits_micts,
        R.string.credits_link_ctslauncher to R.string.url_credits_ctslauncher,
    ).forEach { (label, url) ->
        val address = stringResource(url)
        TextButton(onClick = { context.startFirst(Intent(Intent.ACTION_VIEW, address.toUri())) }) {
            Text(stringResource(label))
        }
    }
}

/** A credits paragraph with its heading and, when there is one, a bulleted list. */
@Composable
private fun CreditBlock(@StringRes title: Int, @StringRes body: Int, items: Int?) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(text = stringResource(body), style = MaterialTheme.typography.bodySmall)
    if (items != null) {
        stringArrayResource(items).forEach { item ->
            Text(
                text = "•  $item",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
        }
    }
}
