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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** The notification buttons that switch the gestures on or off, for ADB mode. */
class AlertActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SWITCH_ON -> {
                GesturePower.switchOn(context)
                AlertNotifier.gesturesRunning(context)
            }
            ACTION_PAUSE_TIMED -> {
                TimedPause.start(context)
                KeepAliveService.refresh(context)
            }
            ACTION_RESUME_TIMED -> {
                TimedPause.finish(context)
                KeepAliveService.refresh(context)
            }
            ACTION_SWITCH_OFF -> {
                TimedPause.cancel(context)
                GesturePower.switchOff(context)
                KeepAliveService.refresh(context)
            }
        }
    }

    companion object {
        const val ACTION_SWITCH_ON = "io.github.victoresidoro.anygest.action.SWITCH_ON"
        const val ACTION_PAUSE_TIMED = "io.github.victoresidoro.anygest.action.PAUSE_TIMED"
        const val ACTION_RESUME_TIMED = "io.github.victoresidoro.anygest.action.RESUME_TIMED"
        const val ACTION_SWITCH_OFF = "io.github.victoresidoro.anygest.action.SWITCH_OFF"
    }
}
