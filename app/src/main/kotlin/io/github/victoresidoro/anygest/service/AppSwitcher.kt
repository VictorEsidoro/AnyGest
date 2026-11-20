// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import io.github.victoresidoro.anygest.setup.SetupStore

/**
 * Walks the apps the user came from, which Android exposes to nobody: there is no global
 * action for it, so the order is read from usage events and the app is launched outright.
 *
 * Starting an activity from here is a background start, allowed only because the service
 * holds a visible accessibility overlay. Measured on HyperOS as
 * `BAL_ALLOW_NON_APP_VISIBLE_WINDOW`, so the edge strips are what make this legal.
 *
 * Needs the usage access the user grants in Settings, and says so in [lastFailure] and in
 * the notes when it is missing, so the gesture does not just look broken.
 */
class AppSwitcher(private val context: Context) {

    // The order this trail started from, newest first, and where in it the last step landed.
    // Kept rather than re-read because usage events lag by seconds: right after a launch the
    // event stream still names the previous app, so re-reading would keep resetting the trail
    // to the newest entry and the gesture would bounce between two apps.
    private var trail: List<String> = emptyList()
    private var cursor = 0
    private var lastHopAt = 0L

    fun step(toPrevious: Boolean): Boolean {
        val now = SystemClock.elapsedRealtime()
        // A pause long enough means the user went somewhere by hand, so the trail restarts.
        if (trail.isEmpty() || now - lastHopAt > CHAIN_MS) {
            val order = recentOrder()
            trail = order.apps
            // The app in front is not in the list when it cannot be launched (the home
            // screen), and the first step back is then to the newest app in it.
            cursor = if (order.startsWithCurrent) 0 else -1
        }
        if (trail.isEmpty()) {
            return fail("app switch: no usage events, so usage access is probably not granted")
        }

        val next = cursor + if (toPrevious) 1 else -1
        if (next !in trail.indices) {
            return fail("app switch: nothing ${if (toPrevious) "older" else "newer"} than this")
        }

        val target = trail[next]
        val intent = context.packageManager.getLaunchIntentForPackage(target)
            ?: return fail("app switch: $target has no launcher entry")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching { context.startActivity(intent) }
            .onFailure { throwable ->
                fail("app switch: starting $target threw ${throwable.javaClass.name}", throwable)
            }
            .onSuccess {
                cursor = next
                lastHopAt = now
                lastFailure = null
                lastNoted = null
            }
            .isSuccess
    }

    /** Records why a step did nothing; the notes get each distinct reason once, not once per swipe. */
    private fun fail(message: String, cause: Throwable? = null): Boolean {
        lastFailure = message
        Log.w(TAG, message, cause)
        if (message != lastNoted) {
            lastNoted = message
            SetupStore(context).note(message)
        }
        return false
    }

    /**
     * Brings the tracker up to date with the usage events since the last step, which is a few
     * events and not the whole trail. The first read looks back [FIRST_WINDOW_MS]; if that
     * finds nothing to step back to (an hour in one app), it is redone once over [TRAIL_WINDOW_MS].
     */
    private fun recentOrder(): RecentApps.Order {
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return RecentApps.Order(emptyList(), false)
        val now = System.currentTimeMillis()
        val known = sharedTracker
        // A clock set back would leave the tracker asking for events from the future.
        val apps = if (known == null || known.lastEventMs > now) newTracker() else known
        val from = if (apps.lastEventMs == 0L) now - FIRST_WINDOW_MS else apps.lastEventMs + 1
        feed(usage, apps, from, now)
        val order = apps.order(::launchable)
        if (order.apps.size >= 2 || widened) return order
        widened = true
        val wide = newTracker()
        feed(usage, wide, now - TRAIL_WINDOW_MS, now)
        return wide.order(::launchable)
    }

    private fun newTracker(): RecentApps =
        RecentApps(context.packageName).also { sharedTracker = it }

    private fun feed(usage: UsageStatsManager, tracker: RecentApps, from: Long, to: Long) {
        val events = runCatching { usage.queryEvents(from, to) }.getOrNull() ?: return
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                tracker.resumed(RecentApps.Resumed(event.packageName ?: continue, event.timeStamp))
            } else {
                tracker.seen(event.timeStamp)
            }
        }
    }

    /** Asking the package manager is the costly part, so the answer is kept until the app changes. */
    private fun launchable(packageName: String): Boolean =
        launchableCache.getOrPut(packageName) {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }

    companion object {
        private const val TAG = "AnyGest"
        private const val FIRST_WINDOW_MS = 30L * 60L * 1000L
        private const val TRAIL_WINDOW_MS = 6L * 60L * 60L * 1000L

        /** How long a chain of swipes keeps walking one order before it restarts. */
        private const val CHAIN_MS = 5_000L

        // Kept for the life of the process: the service restarts at every pause, and the
        // events in between are read incrementally, so nothing has to be read twice.
        private var sharedTracker: RecentApps? = null
        private var widened = false
        private val launchableCache = HashMap<String, Boolean>()
        private var lastNoted: String? = null

        /** An app was installed, updated or removed: what is known about its launcher entry may be stale. */
        fun packageChanged(packageName: String) {
            launchableCache.remove(packageName)
        }

        @Volatile
        var lastFailure: String? = null
            private set
    }
}
