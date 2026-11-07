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

package io.github.victoresidoro.anygest.shortcut

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import io.github.victoresidoro.anygest.R

/**
 * A home screen shortcut that opens an app with the gestures already off. The pause for an
 * exception app starts once the app is in front, which can be late for apps that look for
 * accessibility services the moment they open; this one switches the gestures off first.
 * Its icon is the app's own with a small AnyGest badge, so it is not mistaken for the app.
 */
internal object NoGestureShortcut {

    const val ACTION = "io.github.victoresidoro.anygest.action.OPEN_WITHOUT_GESTURES"
    const val EXTRA_PACKAGE = "io.github.victoresidoro.anygest.extra.PACKAGE"

    fun supported(context: Context): Boolean =
        context.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true

    /** Asks the launcher to place the shortcut; false when it cannot (no launcher support, no app). */
    fun request(context: Context, packageName: String): Boolean = runCatching {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!manager.isRequestPinShortcutSupported) return false
        val packages = context.packageManager
        val label = packages.getApplicationInfo(packageName, 0).loadLabel(packages).toString()
        val intent = Intent(context, OpenWithoutGesturesActivity::class.java)
            .setAction(ACTION)
            .putExtra(EXTRA_PACKAGE, packageName)
        val info = ShortcutInfo.Builder(context, "nogestures:$packageName")
            .setShortLabel(label)
            .setLongLabel(context.getString(R.string.shortcut_long_label, label))
            .setIcon(Icon.createWithBitmap(badgedIcon(context, packageName)))
            .setIntent(intent)
            .build()
        manager.requestPinShortcut(info, null)
    }.getOrDefault(false)

    private fun badgedIcon(context: Context, packageName: String): Bitmap {
        val size = context.getSystemService(ActivityManager::class.java)?.launcherLargeIconSize
            ?.takeIf { it > 0 } ?: DEFAULT_ICON_PX
        val icon = createBitmap(size, size)
        val canvas = Canvas(icon)
        context.packageManager.getApplicationIcon(packageName)
            .toBitmap(size, size)
            .let { canvas.drawBitmap(it, 0f, 0f, null) }
        val radius = size * BADGE_RATIO / 2f
        val centre = size - radius
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawCircle(centre, centre, radius, paint)
        paint.color = BADGE_COLOUR
        canvas.drawCircle(centre, centre, radius * BADGE_RING, paint)
        ContextCompat.getDrawable(context, R.drawable.ic_tile)?.mutate()?.let { badge ->
            badge.setTint(Color.WHITE)
            val inset = (radius * BADGE_GLYPH).toInt()
            badge.setBounds(
                (centre - inset).toInt(),
                (centre - inset).toInt(),
                (centre + inset).toInt(),
                (centre + inset).toInt(),
            )
            badge.draw(canvas)
        }
        return icon
    }

    private const val DEFAULT_ICON_PX = 192
    private const val BADGE_RATIO = 0.42f
    private const val BADGE_RING = 0.86f
    private const val BADGE_GLYPH = 0.55f

    /** The launcher icon's slate. */
    private const val BADGE_COLOUR = 0xFF1C2733.toInt()
}
