// Copyright (C) 2026  Victor Eduardo (@victoresidoro)
// Copyright (C) 2026  Henrique Almeida <me@h3nc4.com>
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

package io.github.victoresidoro.anygest.gesture

/**
 * The pill HyperOS shows at the bottom of a landscape app when a first swipe is held back.
 *
 * Based on HyperGesture (f0f655f) by Henrique Almeida (h3nc4).
 */
object ConfirmPill {
    /** How long the pill waits for the swipe that confirms it. */
    const val WINDOW_MS = 2_000L

    /** As wide as the system gesture handle. */
    const val WIDTH_DP = 108f
    const val HEIGHT_DP = 4f
    const val ABOVE_EDGE_DP = 6f
}

sealed interface ConfirmVerdict {
    data object Perform : ConfirmVerdict

    /** Held back, with the pill up until [expiresAtMs] unless a bottom touch starts first. */
    data class ShowPill(val expiresAtMs: Long) : ConfirmVerdict
}

/**
 * Decides whether a bottom gesture goes ahead or waits for a second swipe. It lives outside the
 * tracker because the pill spans two touches and outlasts a rebuild of the strips.
 */
class LandscapeConfirm(private val windowMs: Long = ConfirmPill.WINDOW_MS) {

    private var asking = false
    private var landscape: Boolean? = null

    // When the pill in the air goes away, or null while there is none.
    private var upUntil: Long? = null

    // True while the bottom touch that began under the pill is still down.
    private var answering = false

    val pillShown: Boolean get() = upUntil != null

    val expiresAtMs: Long? get() = upUntil

    /** Applies the setting and the orientation. A turn drops a pill, which asked about the old one. */
    fun configure(enabled: Boolean, landscape: Boolean) {
        val turned = this.landscape != landscape
        this.landscape = landscape
        asking = enabled && landscape
        if (turned || !asking) dismiss()
    }

    fun onDown(edge: ScreenEdge, nowMs: Long) {
        if (edge != ScreenEdge.BOTTOM) return
        expire(nowMs)
        answering = pillShown
    }

    fun onFire(action: GestureAction, edge: ScreenEdge, nowMs: Long): ConfirmVerdict {
        if (edge != ScreenEdge.BOTTOM || action !in HELD_BACK) return ConfirmVerdict.Perform
        if (answering || !asking) {
            dismiss()
            return ConfirmVerdict.Perform
        }
        val until = nowMs + windowMs
        upUntil = until
        return ConfirmVerdict.ShowPill(until)
    }

    /** A bottom touch handed to the app is the user going back to it, so the pill goes. */
    fun onReplayed(edge: ScreenEdge) {
        if (edge == ScreenEdge.BOTTOM) dismiss()
    }

    fun onStreamEnd(edge: ScreenEdge, nowMs: Long) {
        if (edge != ScreenEdge.BOTTOM) return
        answering = false
        expire(nowMs)
    }

    /** Drops the pill once its time is up, unless a touch that began inside it is still down. */
    fun expire(nowMs: Long) {
        val until = upUntil ?: return
        if (!answering && nowMs >= until) dismiss()
    }

    fun dismiss() {
        upUntil = null
        answering = false
    }

    private companion object {
        // Circle to Search is a still hold rather than a swipe, so it never leaves by accident.
        val HELD_BACK = setOf(
            GestureAction.Home,
            GestureAction.Recents,
            GestureAction.PreviousApp,
            GestureAction.NextApp,
        )
    }
}
