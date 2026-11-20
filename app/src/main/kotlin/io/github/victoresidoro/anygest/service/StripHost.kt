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

import android.content.Context
import android.os.Handler
import android.view.WindowManager
import io.github.victoresidoro.anygest.gesture.GestureAction
import io.github.victoresidoro.anygest.gesture.GestureConfiguration
import io.github.victoresidoro.anygest.gesture.ScreenEdge
import io.github.victoresidoro.anygest.setup.SecureSettingsAccess
import io.github.victoresidoro.anygest.setup.SetupStore
import io.github.victoresidoro.anygest.setup.GestureTileService

/**
 * Keeps the strips on the screen and the right size: installs them for a configuration,
 * measures the screen again when it may have changed, and builds them again when one went
 * away. Leaves them alone when nothing would change, since rebuilding cuts off a touch in
 * progress, and waits a moment for a touch that is under way.
 */
internal class StripHost(
    private val context: Context,
    private val windowManager: WindowManager,
    private val handler: Handler,
    replayer: TouchReplayer,
    onGestureAction: (GestureAction, ScreenEdge) -> Unit,
) {
    private val strips = EdgeStrips(context, windowManager, replayer, onGestureAction, onBroken = { repairSoon() })

    private var configuration: GestureConfiguration? = null
    private var installedFor: GestureConfiguration? = null
    private var installedGeometry: ScreenGeometry? = null
    private var deferrals = 0
    private var failures = 0
    private val resetTokens = ResetTokenWatch()
    private val settle = GeometrySettle()

    private val installAgain = Runnable { install() }
    private val settleStep = Runnable { settleOnce() }
    private val repair = Runnable {
        installedFor = null
        install()
    }

    fun setArmed(value: Boolean, why: String) = strips.setArmed(value, why)

    fun describe(): String = strips.describe()

    /** A new configuration from the settings. */
    fun configure(updated: GestureConfiguration) {
        configuration = updated
        install()
    }

    /** The screen came on: strips that went missing or stuck while it was off are put right first. */
    fun screenOn() {
        if (strips.healthy()) return
        SetupStore(context).note("strips missing when the screen came on, reinstalled")
        install()
    }

    /** The display changed (a rotation, a fold): strips keep their pixel size, so they are measured again. */
    fun displayChanged() {
        if (ScreenGeometry.current(context, windowManager) == installedGeometry) return
        install()
        // A change usually comes in steps, the bar inset last.
        startSettle()
    }

    /** The system may be about to change the bar inset: read the screen until it holds still. */
    fun startSettle() {
        handler.removeCallbacks(settleStep)
        handler.postDelayed(settleStep, settle.begin())
    }

    fun stop() {
        handler.removeCallbacks(installAgain)
        handler.removeCallbacks(settleStep)
        handler.removeCallbacks(repair)
        installedFor = null
        deferrals = 0
        failures = 0
        strips.remove()
    }

    private fun repairSoon() {
        handler.removeCallbacks(repair)
        handler.postDelayed(repair, REPAIR_DELAY_MS)
    }

    private fun settleOnce() {
        val read = ScreenGeometry.current(context, windowManager)
        val installed = installedGeometry
        if (installed != null && read != installed) {
            installed.describeChangeTo(read)?.let {
                SetupStore(context).note("strips remeasured after the system settled ($it)")
            }
            install()
        }
        settle.next(read)?.let { handler.postDelayed(settleStep, it) }
    }

    private fun install() {
        val wanted = configuration ?: return
        handler.removeCallbacks(installAgain)
        val geometry = ScreenGeometry.current(context, windowManager)
        if (installedFor == wanted && installedGeometry == geometry && strips.intact()) return
        if (strips.streamHeld() && deferrals < MAX_DEFERRALS) {
            deferrals++
            handler.postDelayed(installAgain, DEFER_MS)
            return
        }
        deferrals = 0
        installedGeometry = geometry
        val attached = strips.install(wanted, geometry)
        if (attached > 0) {
            installedFor = wanted
            failures = 0
            resetTokens.reset()
            return
        }
        installedFor = null
        if (resetTokens.installed(attached, strips.tokenRefusals, strips.otherRefusals)) {
            connectionLost()
            return
        }
        if (failures == 0) SetupStore(context).note("no strip could be attached, trying again")
        if (failures < MAX_RETRIES) {
            handler.postDelayed(installAgain, RETRY_MS shl failures)
            failures++
        }
    }

    /**
     * The system reset the service's connection without unbinding it, so no strip can be
     * attached again. With ADB mode the service is switched off and the recovery path switches
     * it on, which gives it a new connection; without it the stop is reported like any other.
     */
    private fun connectionLost() {
        val store = SetupStore(context)
        handler.removeCallbacks(installAgain)
        store.note("connection lost: every strip was refused with a reset window token (${PowerContext.describe(context)})")
        resetTokens.reset()
        if (SecureSettingsAccess.canWrite(context) && store.autoRestore && !store.userPaused) {
            store.note("restarting the service to get a new connection")
            val app = context.applicationContext
            // The job that watches the accessibility list switches it back on; this is its backstop.
            Handler(handler.looper).postDelayed({ GesturePower.restoreIfNeeded(app) }, RESTART_BACKSTOP_MS)
            SecureSettingsAccess.setServiceEnabled(context, false)
        } else {
            store.serviceUp = false
            AlertNotifier.gesturesStopped(context)
            GestureTileService.requestRefresh(context)
        }
    }

    private companion object {
        const val DEFER_MS = 100L
        const val MAX_DEFERRALS = 10
        const val RETRY_MS = 250L
        const val MAX_RETRIES = 6
        const val RESTART_BACKSTOP_MS = 3_000L

        /** Lets a burst of window changes end before the strips are built again. */
        const val REPAIR_DELAY_MS = 500L
    }
}
