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
package io.github.victoresidoro.anygest.settings

import android.content.Context
import android.net.Uri
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads and writes a [SettingsBackup] through the system file picker's `content://` URI. */
object SettingsBackupIo {

    /** A settings file is well under a kilobyte; anything bigger is not one. */
    private const val MAX_CHARS = 16 * 1024

    suspend fun export(context: Context, uri: Uri, configuration: GestureConfiguration): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                // "wt" truncates: a shorter file must not keep the tail of an older one.
                context.contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(SettingsBackup.encode(configuration).toByteArray(Charsets.UTF_8))
                } != null
            }.getOrDefault(false)
        }

    /** Null when the file cannot be read or is not an AnyGest settings file. */
    suspend fun import(context: Context, uri: Uri): SettingsBackup.Decoded? =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { reader ->
                        val buffer = CharArray(MAX_CHARS)
                        val read = reader.read(buffer)
                        if (read > 0) String(buffer, 0, read) else ""
                    }
                }
                text?.let(SettingsBackup::decode)
            }.getOrNull()
        }
}
