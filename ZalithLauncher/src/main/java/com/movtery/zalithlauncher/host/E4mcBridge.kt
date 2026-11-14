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
import com.craftstudio.cslauncher.e4mc.E4mcNative

private const val TAG = "E4mcBridge"

/** A guest the relay turned away at the door. */
data class CsGuestRefusal(val name: String, val reason: String) {
    /** One line for the hosting card's "recently refused" list. */
    override fun toString(): String = "$name — $reason"
}

/**
 * Safe Kotlin façade over the e4mc tunnel JNI in [E4mcNative].
 *
 * Two jobs, and both matter because this code runs inside the **game process**:
 *
 *  1. **Never throw.** Every entry point swallows [UnsatisfiedLinkError]. The library is optional —
 *     a build without it, or an older `.so` missing the newer symbols such as `nativeGuestNames`,
 *     must degrade to "sharing unavailable" rather than take Minecraft down with it. A crash inside
 *     the game process loses the player their world, which is a far worse outcome than a missing
 *     feature.
 *  2. **Never invent an address.** [domain] returns null until the relay has actually assigned one,
 *     so the UI cannot show a Join button that leads nowhere.
 *
 * Handles are opaque `Long`s: 0 means "no tunnel". Callers are expected to hold one handle at a
 * time; [WorldHostManager] does.
 */
object E4mcBridge {

    @Volatile
    private var loadAttempted = false

    @Volatile
    private var available = false

    /** True once the native library has been found and loaded. */
    val isAvailable: Boolean
        get() {
            load()
            return available
        }

    /**
     * Why the tunnel is unavailable, in words a player can act on.
     *
     * Null when [isAvailable]. Deliberately distinguishes "this build has no library" from "the
     * library exists but refused to load", because only the first one is fixable by installing a
     * different build.
     */
    val unavailabilityReason: String?
        get() {
            if (isAvailable) return null
            val why = E4mcNative.loadError()
            return if (why.isNullOrEmpty()) "This build has no world-hosting tunnel library"
            else "Tunnel library failed to load: $why"
        }

    /** Loads the library at most once; safe to call from any thread. */
    @Synchronized
    fun load(): Boolean {
        if (loadAttempted) return available
        loadAttempted = true
        available = try {
            E4mcNative.load()
        } catch (e: Throwable) {
            Log.w(TAG, "e4mc load failed", e)
            false
        }
        if (!available) Log.i(TAG, "world hosting unavailable: $unavailabilityReason")
        return available
    }

    /**
     * Starts publishing local port [port].
     *
     * @param relay an explicit relay `host:port`, or null to take the least loaded one
     * @return a handle, or 0 when the tunnel could not be started — see [lastError]
     */
    fun start(port: Int, relay: String? = null): Long {
        if (!isAvailable) return 0L
        if (port !in 1..65535) return 0L
        return guard(default = 0L) { E4mcNative.nativeStart(port, relay) }
    }

    /** The public address assigned to [handle], or null while still connecting. */
    fun domain(handle: Long): String? =
        if (handle == 0L) null else guard<String?>(default = null) { E4mcNative.nativeDomain(handle) }

    /** A problem reported by the tunnel for [handle], or null when healthy. */
    fun problem(handle: Long): String? =
        if (handle == 0L) null else guard<String?>(default = null) { E4mcNative.nativeProblem(handle) }

    /** How many guests are connected through [handle] right now. */
    fun guestCount(handle: Long): Int =
        if (handle == 0L) 0 else guard(default = 0) { E4mcNative.nativeGuests(handle) }

    /**
     * The names of the guests connected through [handle], in login order.
     *
     * Returns an empty list rather than failing when the loaded library predates `nativeGuestNames`:
     * the count above still works, so the hosting card shows "3 players" instead of nothing.
     */
    fun guestNames(handle: Long): List<String> {
        if (handle == 0L) return emptyList()
        val raw = guard<String?>(default = null) { E4mcNative.nativeGuestNames(handle) } ?: return emptyList()
        return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    /** Ends the tunnel for [handle]. The handle is dead afterwards. */
    fun stop(handle: Long) {
        if (handle == 0L) return
        guard(default = Unit) { E4mcNative.nativeStop(handle) }
    }

    /**
     * Hands the relay the current access policy.
     *
     * Cheap enough to call whenever a setting or the friends list changes, which is what lets
     * "friends only" follow a friend added mid-game without restarting the tunnel. Guests already
     * playing are not dropped by a policy change.
     *
     * @param maxPlayers guests at once, host excluded; 0 means no ceiling at the relay
     * @param allowedNames names to admit, or null for "anyone with the address"
     * @param closedReason shown to a guest the name list keeps out
     */
    fun setPolicy(handle: Long, maxPlayers: Int, allowedNames: List<String>?, closedReason: String?) {
        if (handle == 0L) return
        val names = allowedNames?.takeIf { it.isNotEmpty() }?.joinToString("\n")
        guard(default = Unit) {
            E4mcNative.nativeSetPolicy(handle, maxPlayers.coerceAtLeast(0), names, closedReason?.takeIf { it.isNotEmpty() })
        }
    }

    /** Names refused at the door however open the world is — this session's kick list. */
    fun setBlocked(handle: Long, blocked: List<String>) {
        if (handle == 0L) return
        guard(default = Unit) {
            E4mcNative.nativeSetBlocked(handle, blocked.takeIf { it.isNotEmpty() }?.joinToString("\n"))
        }
    }

    /**
     * Ends the connection of the guest logged in as [name].
     *
     * @return false when nothing was ended, including when the loaded library predates kick support
     */
    fun kick(handle: Long, name: String): Boolean {
        if (handle == 0L || name.isBlank()) return false
        val ended = guard(default = 0) { E4mcNative.nativeKick(handle, name) }
        return ended > 0
    }

    /** True when the loaded library can end an individual guest's connection. */
    val supportsKick: Boolean
        get() = isAvailable

    /** Refusals since the last call — drains the queue, so each one is reported exactly once. */
    fun pollRefusals(handle: Long): List<CsGuestRefusal> {
        if (handle == 0L) return emptyList()
        val raw = guard<String?>(default = null) { E4mcNative.nativePoll(handle) } ?: return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0) return@mapNotNull null
            CsGuestRefusal(
                name = line.substring(0, tab).trim(),
                reason = line.substring(tab + 1).trim()
            )
        }.toList()
    }

    /** The last error reported by the native side, or null. */
    fun lastError(): String? = guard<String?>(default = null) { E4mcNative.nativeError() }

    /**
     * Runs [block], converting a missing native symbol into [default] instead of an exception.
     *
     * [UnsatisfiedLinkError] is the expected case (optional library, or an older one missing a newer
     * symbol); anything else is logged because it means the tunnel itself is misbehaving.
     */
    private inline fun <T> guard(default: T, block: () -> T): T = try {
        block()
    } catch (e: UnsatisfiedLinkError) {
        default
    } catch (e: Throwable) {
        Log.w(TAG, "native call failed", e)
        default
    }
}
