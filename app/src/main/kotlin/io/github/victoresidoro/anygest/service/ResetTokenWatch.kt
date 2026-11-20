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

package io.github.victoresidoro.anygest.service

/**
 * Tells a lost service connection from a refusal that happens once.
 *
 * Android can reset the accessibility connection without calling onUnbind. The service that is
 * still running then holds a null window token, and every strip it installs is refused with the
 * same "token null is not valid" error however often it tries. Several installs in a row that
 * attach nothing and fail only for that reason mean the connection is gone. Any other failure,
 * or an install that attached a strip, starts the count over.
 */
internal class ResetTokenWatch(private val threshold: Int = REFUSED_INSTALLS) {

    private var inARow = 0

    /** True once [threshold] installs in a row were refused for the reset token alone. */
    fun installed(attached: Int, tokenRefusals: Int, otherFailures: Int): Boolean {
        val onlyTheToken = attached == 0 && tokenRefusals > 0 && otherFailures == 0
        inARow = if (onlyTheToken) inARow + 1 else 0
        return inARow >= threshold
    }

    fun reset() {
        inARow = 0
    }

    companion object {
        const val REFUSED_INSTALLS = 3

        private const val TOKEN_MESSAGE = "token null is not valid"

        fun isResetToken(failure: Throwable): Boolean =
            failure.message.orEmpty().contains(TOKEN_MESSAGE)
    }
}
