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

package io.github.victoresidoro.anygest.diagnostics

import android.os.SystemClock

/**
 * The last gestures with their timings, kept in memory only (gone when the process ends)
 * and shown in "Copy log". It separates what AnyGest spends from what the system spends:
 * touch to action, how long `performGlobalAction` itself took, and how long until the
 * first window change followed.
 */
internal class TraceLog(private val capacity: Int = 80) {

    private class Entry(val atMs: Long, val text: String)
    private class Waiting(val startedMs: Long, val action: String)

    private val entries = ArrayDeque<Entry>()
    private var downAtMs = NONE
    private var waiting: Waiting? = null

    @Synchronized
    fun touchDown(atMs: Long) {
        downAtMs = atMs
    }

    @Synchronized
    fun gesture(atMs: Long, edge: String, action: String) {
        val sinceDown = if (downAtMs == NONE) "?" else "${atMs - downAtMs}"
        add(atMs, "$edge -> $action, $sinceDown ms after touch")
    }

    /** [startedMs] is when the call began; the first window change after it is awaited. */
    @Synchronized
    fun performed(startedMs: Long, durationMs: Long, action: String, ok: Boolean) {
        add(startedMs + durationMs, "$action call took $durationMs ms, ${if (ok) "accepted" else "refused"}")
        waiting = if (ok) Waiting(startedMs, action) else null
    }

    /** Only the first window change after an action counts; anything older is ignored. */
    @Synchronized
    fun windowChanged(atMs: Long, label: String) {
        val pending = waiting ?: return
        if (atMs < pending.startedMs) return
        waiting = null
        if (atMs - pending.startedMs > WINDOW_WAIT_MS) return
        add(atMs, "window -> $label, ${atMs - pending.startedMs} ms after ${pending.action} started")
    }

    /** Oldest first, each with its age at [nowMs]. */
    @Synchronized
    fun lines(nowMs: Long): List<String> = entries.map { "-${(nowMs - it.atMs) / 1000} s  ${it.text}" }

    private fun add(atMs: Long, text: String) {
        entries.addLast(Entry(atMs, text))
        while (entries.size > capacity) entries.removeFirst()
    }

    companion object {
        const val WINDOW_WAIT_MS = 3_000L
        private const val NONE = Long.MIN_VALUE
    }
}

/** The process-wide log; times are `uptimeMillis`, the clock accessibility events carry. */
internal object Trace {
    private val log = TraceLog()

    fun touchDown() = log.touchDown(SystemClock.uptimeMillis())

    fun gesture(edge: String, action: String) = log.gesture(SystemClock.uptimeMillis(), edge, action)

    fun performed(startedMs: Long, action: String, ok: Boolean) =
        log.performed(startedMs, SystemClock.uptimeMillis() - startedMs, action, ok)

    fun windowChanged(eventTimeMs: Long, label: String) = log.windowChanged(eventTimeMs, label)

    fun report(): String? =
        log.lines(SystemClock.uptimeMillis()).takeIf { it.isNotEmpty() }?.joinToString("\n")
}
