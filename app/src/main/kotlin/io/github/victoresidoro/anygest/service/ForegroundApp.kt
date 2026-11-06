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

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** Which app is in front, from the usage events the app-switch gesture already relies on. */
internal object ForegroundApp {

    /**
     * The package of the newest "resumed" event in the last [windowMs], or null when there was
     * none, which is also what a missing usage access looks like: callers check that first.
     */
    fun current(context: Context, windowMs: Long): String? {
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val events = runCatching { usage.queryEvents(now - windowMs, now) }.getOrNull()
            ?: return null
        var latest: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latest = event.packageName ?: latest
            }
        }
        return latest
    }
}
