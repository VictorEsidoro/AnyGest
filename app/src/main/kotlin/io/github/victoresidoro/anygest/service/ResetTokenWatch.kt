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

/**
 * Tells a lost service connection from a one-off refusal.
 *
 * Android can reset an accessibility connection without calling `onUnbind`, and the live
 * service is then left with a window token that is null: every strip is refused with the same
 * "token null is not valid" error, however often it is retried. A few installs in a row that
 * attach nothing and fail only for that reason are a lost connection; any other failure, or
 * an install that attached something, starts the count again.
 */
internal class ResetTokenWatch(private val threshold: Int = REFUSED_INSTALLS) {

    private var streak = 0

    /** True once [threshold] installs in a row failed only because of the reset token. */
    fun installed(attached: Int, tokenRefusals: Int, otherFailures: Int): Boolean {
        streak = if (attached == 0 && tokenRefusals > 0 && otherFailures == 0) streak + 1 else 0
        return streak >= threshold
    }

    fun reset() {
        streak = 0
    }

    companion object {
        const val REFUSED_INSTALLS = 3

        fun isResetToken(failure: Throwable): Boolean =
            failure.message?.contains("token null is not valid") == true
    }
}
