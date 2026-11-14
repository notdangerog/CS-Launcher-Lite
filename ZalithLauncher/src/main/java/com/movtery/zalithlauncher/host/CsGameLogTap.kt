/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.host

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

private const val TAG = "CsGameLogTap"

/**
 * Incremental tail of the running game's log file, delivering complete lines to [onLine].
 *
 * **Why a file tail and not a stream hook.** The game log is the one thing every Minecraft version,
 * mod loader and renderer already agrees on: the "open to LAN" line has been printed there since
 * long before any of them had an event bus, and it survives obfuscation, remapping and mods that
 * replace the logger. Reading it costs one small poll and works on 1.8 through 26.x alike, which is
 * more than can be said for any in-process hook.
 *
 * Mirrors the launcher's own `ProcessLogTailer`: starts at the current end of the file so a previous
 * session's leftovers are not re-read, restarts from zero when the file is truncated (the game
 * rewrites it on restart), and only ever emits lines that have terminated — a half-written port
 * number would be worse than a late one.
 */
class CsGameLogTap(
    private val logFile: File,
    private val pollMillis: Long = DEFAULT_POLL_MILLIS,
    private val onLine: (String) -> Unit
) {

    private var job: Job? = null

    /** True while a tail is running. */
    val isRunning: Boolean get() = job?.isActive == true

    /** Starts tailing within [scope]. The returned [Job] is owned by this tap; [stop] cancels it. */
    fun start(scope: CoroutineScope): Job {
        if (isRunning) return job!!
        val started = scope.launch(Dispatchers.IO) {
            // Begin at the current end: the log file persists across launches, and re-reading last
            // session's "hosted on port" line would tunnel a port nothing is listening on.
            var offset = logFile.takeIf { it.exists() }?.length() ?: 0L
            var remainder = ByteArray(0)

            while (isActive) {
                delay(pollMillis)
                try {
                    if (!logFile.exists()) continue
                    val length = logFile.length()
                    if (length < offset) {
                        // Truncated or recreated — start over from the beginning of the new content.
                        offset = 0L
                        remainder = ByteArray(0)
                    }
                    if (length <= offset) continue

                    val chunk = RandomAccessFile(logFile, "r").use { raf ->
                        raf.seek(offset)
                        ByteArray((length - offset).toInt()).also { raf.readFully(it) }
                    }
                    offset = length

                    val (lines, rest) = splitCompleteLines(remainder + chunk)
                    remainder = rest
                    // A pathological single line (a mod dumping a stack trace with no newline) must
                    // not be allowed to grow the pending buffer without bound.
                    if (remainder.size > MAX_PENDING_BYTES) remainder = ByteArray(0)
                    lines.forEach { line ->
                        try {
                            onLine(line)
                        } catch (e: Throwable) {
                            // The consumer is the host manager; a bug there must not kill the tail,
                            // or the world would silently stop being shareable for the rest of the session.
                            Log.w(TAG, "log consumer failed", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "tail failed", e)
                }
            }
        }
        job = started
        return started
    }

    /** Stops tailing. Safe to call when nothing is running. */
    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        /** 250 ms is well inside the window where a "sharing…" spinner still feels immediate. */
        const val DEFAULT_POLL_MILLIS = 250L

        private const val MAX_PENDING_BYTES = 256 * 1024

        /**
         * Splits [bytes] into complete UTF-8 lines plus whatever trailing fragment has no newline yet.
         *
         * Handles `\r\n`, bare `\n` and bare `\r`, because the game writes `\r\n` on some renderers
         * and `\n` on others, and a stray trailing `\r` would corrupt a parsed port number.
         */
        internal fun splitCompleteLines(bytes: ByteArray): Pair<List<String>, ByteArray> {
            val lines = ArrayList<String>()
            var start = 0
            var i = 0
            while (i < bytes.size) {
                val b = bytes[i]
                if (b == '\n'.code.toByte() || b == '\r'.code.toByte()) {
                    lines += String(bytes, start, i - start, Charsets.UTF_8)
                    // Consume a \r\n pair as one terminator.
                    i++
                    if (b == '\r'.code.toByte() && i < bytes.size && bytes[i] == '\n'.code.toByte()) i++
                    start = i
                } else {
                    i++
                }
            }
            val rest = if (start < bytes.size) bytes.copyOfRange(start, bytes.size) else ByteArray(0)
            return lines to rest
        }
    }
}
