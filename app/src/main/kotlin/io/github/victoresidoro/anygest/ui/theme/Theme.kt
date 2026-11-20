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

package io.github.victoresidoro.anygest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The palette comes from the launcher icon: a slate ground and a light-blue swipe arc. Every
// colour role is set on purpose, since leaving some out lets them fall back to the purple
// Material 3 baseline (a lavender tonal button, pinkish-grey card surfaces).

private fun hex(rgb: Long) = Color(0xFF000000L or rgb)

// In the light scheme the cards (surfaceContainerHighest) are white on a blue-grey page, so each
// reads as an object of its own instead of a grey slab on grey.
private val LightScheme: ColorScheme = lightColorScheme(
    primary = hex(0x2C5EA8),
    onPrimary = Color.White,
    primaryContainer = hex(0xD7E3FA),
    onPrimaryContainer = hex(0x0B2A57),
    secondary = hex(0x4A5B72),
    onSecondary = Color.White,
    secondaryContainer = hex(0xD7E3FA),
    onSecondaryContainer = hex(0x16305C),
    tertiary = hex(0x2C5EA8),
    onTertiary = Color.White,
    background = hex(0xEAEFF5),
    onBackground = hex(0x10151C),
    surface = Color.White,
    onSurface = hex(0x10151C),
    surfaceVariant = hex(0xE3E8F0),
    onSurfaceVariant = hex(0x48525F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = hex(0xF2F5FA),
    surfaceContainer = hex(0xECF0F7),
    surfaceContainerHigh = hex(0xE6EBF3),
    surfaceContainerHighest = Color.White,
    outline = hex(0x79808B),
    outlineVariant = hex(0xC9D0DA),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = hex(0x8AB4F8),
    onPrimary = hex(0x0B2A57),
    primaryContainer = hex(0x21456F),
    onPrimaryContainer = hex(0xD7E3FA),
    secondary = hex(0xB9C6DA),
    onSecondary = hex(0x23303F),
    secondaryContainer = hex(0x2A3F5C),
    onSecondaryContainer = hex(0xD7E3FA),
    tertiary = hex(0x8AB4F8),
    onTertiary = hex(0x0B2A57),
    background = hex(0x0C1117),
    onBackground = hex(0xE6EAF0),
    surface = hex(0x151B23),
    onSurface = hex(0xE6EAF0),
    surfaceVariant = hex(0x2A3644),
    onSurfaceVariant = hex(0xC0C8D4),
    surfaceContainerLowest = hex(0x0B0F14),
    surfaceContainerLow = hex(0x151B23),
    surfaceContainer = hex(0x1A222C),
    surfaceContainerHigh = hex(0x232C38),
    surfaceContainerHighest = hex(0x1C2530),
    outline = hex(0x8A93A0),
    outlineVariant = hex(0x3C4654),
)

// Colours for status badges, which Material 3 has no role for.
private class StatusColours(val container: Color, val onContainer: Color)

private val SuccessLight = StatusColours(hex(0xC3EDD4), hex(0x06331A))
private val SuccessDark = StatusColours(hex(0x1E5136), hex(0xA8E5BF))
private val WarningLight = StatusColours(hex(0xFFE2A8), hex(0x3A2600))
private val WarningDark = StatusColours(hex(0x5C3D00), hex(0xFFDFA0))

val successContainer: Color
    @Composable get() = (if (isSystemInDarkTheme()) SuccessDark else SuccessLight).container

val onSuccessContainer: Color
    @Composable get() = (if (isSystemInDarkTheme()) SuccessDark else SuccessLight).onContainer

val warningContainer: Color
    @Composable get() = (if (isSystemInDarkTheme()) WarningDark else WarningLight).container

val onWarningContainer: Color
    @Composable get() = (if (isSystemInDarkTheme()) WarningDark else WarningLight).onContainer

/**
 * Dynamic colour from the wallpaper is left out on purpose: it makes the palette unpredictable,
 * and on a neutral wallpaper everything turns grey and the badges muddy.
 */
@Composable
fun AnyGestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
}
