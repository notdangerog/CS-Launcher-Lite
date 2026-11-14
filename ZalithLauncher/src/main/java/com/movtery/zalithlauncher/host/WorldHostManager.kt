/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The hosting flow — watch the game log for the Open-to-LAN line, tunnel that port, publish the
 * resulting address to the player's CS friends as presence — follows CS Launcher Plus,
 * (C) CRAFT STUDIO, so that a world hosted from either launcher looks the same to a guest joining it.
 */

package com.movtery.zalithlauncher.host

import android.util.Log
import com.movtery.zalithlauncher.bridge.ZLNativeInvoker
import com.movtery.zalithlauncher.friends.CsWorldEngine
import com.movtery.zalithlauncher.friends.FriendsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "WorldHostManager"

/** What the hosting card should show right now. */
enum class CsHostPhase {
    /** No world is being shared. */
    IDLE,

    /** A LAN port was seen; the tunnel is dialling out to a relay. */
    STARTING,

    /** A public address exists and friends have been told. */
    PUBLISHED,

    /** Sharing was attempted and could not complete; [CsHostState.problem] says why. */
    FAILED
}

/** Immutable view of the hosting session, for the UI to collect. */
data class CsHostState(
    val phase: CsHostPhase = CsHostPhase.IDLE,
    /** The public `name.e4mc.link` address, or null until the relay assigns one. */
    val address: String? = null,
    /** The local port the world is listening on, 0 when unknown. */
    val port: Int = 0,
    /** The world name from `Preparing level "…"`, or null when the log has not said yet. */
    val world: String? = null,
    /** Names of guests connected right now, in login order. */
    val guests: List<String> = emptyList(),
    /** Refusals, newest first, capped — for the "couldn't get in" row on the hosting card. */
    val refusals: List<CsGuestRefusal> = emptyList(),
    /** A human-readable problem; non-null only in [CsHostPhase.FAILED], or alongside a stalled dial. */
    val problem: String? = null,
    /** False when the player turned sharing off for this session. */
    val enabled: Boolean = true,
    /** Whether this build can host at all — see [E4mcBridge.unavailabilityReason]. */
    val supported: Boolean = false,
    /** [E4mcBridge.unavailabilityReason] when not [supported], else null. */
    val unsupportedReason: String? = null
) {
    /** True when a guest can actually connect right now. */
    val isLive: Boolean get() = phase == CsHostPhase.PUBLISHED && !address.isNullOrEmpty()

    /** The ceiling the game was told, host included. */
    val maxPlayers: Int get() = HostSettings.javaMaxPlayers

    /** Whether guests currently occupy every slot. */
    val isFull: Boolean
        get() {
            val max = HostSettings.maxPlayers
            if (max == HostSettings.NO_LIMIT) return false
            return guests.size + 1 >= max
        }

    /** `"3 / 8"` or `"3 / ∞"` for the hosting card. */
    val occupancyLabel: String
        get() = "${guests.size + 1} / " + if (HostSettings.maxPlayers == HostSettings.NO_LIMIT) "∞" else HostSettings.javaMaxPlayers.toString()
}

/**
 * Coordinates one Java-edition hosting session: log watch → tunnel → friends presence.
 *
 * **Lives in the game process.** `staticLauncher` is only set while a JVM launch is in flight, so
 * this object is inert in the launcher UI process; [attach] is called from the launch path and
 * [shutdownForProcessExit] from the native exit hook. That split is why nothing here touches Compose
 * state directly — the UI collects [state] and the game process owns the lifecycle.
 *
 * **Idempotent by design.** The game can print the LAN line more than once (chat echoes it, and some
 * versions log it per publish), and the player can reopen the world to LAN with a different port.
 * [onLogLine] therefore ignores a repeat of the port already tunnelled and restarts on a change,
 * which is also what makes "close to LAN, open again" work without leaving the world.
 */
object WorldHostManager {

    /** How long a refused-guest notice stays on the hosting card. */
    private const val MAX_REFUSALS = 5

    /** Poll interval while dialling, then once published. */
    private const val POLL_DIALLING_MS = 500L
    private const val POLL_PUBLISHED_MS = 2000L

    /** Give up on a dial that never produced an address; the relay may be unreachable. */
    private const val DIAL_TIMEOUT_MS = 45_000L

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val _state = MutableStateFlow(CsHostState())
    val state: StateFlow<CsHostState> = _state.asStateFlow()

    @Volatile
    private var handle: Long = 0L

    @Volatile
    private var startedAt: Long = 0L

    private var tap: CsGameLogTap? = null
    private var pollJob: Job? = null

    /** Guests notified about, so join/leave edges fire once each. */
    private val announced = LinkedHashSet<String>()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Begins watching the current game's log for a LAN publish.
     *
     * Call once per launch, from the game process, after the launcher knows where the log file is.
     * Safe to call repeatedly: only the first call in a session starts a tail.
     *
     * The tunnel library is probed here rather than at publish time so the UI can show "hosting is
     * unavailable in this build" before the player has bothered to open a world to LAN.
     */
    fun attach(logFile: java.io.File? = null) {
        val supported = E4mcBridge.load()
        _state.update { it.copy(supported = supported, unsupportedReason = E4mcBridge.unavailabilityReason) }

        // The launch path passes the log file directly; falling back to the launcher singleton keeps
        // attach() usable from anywhere in the game process, where that reference is already set.
        val target = logFile ?: runCatching { ZLNativeInvoker.staticLauncher?.getLogFile() }.getOrNull()
        if (target == null) {
            Log.i(TAG, "no game log file known; hosting idle")
            return
        }

        synchronized(this) {
            if (tap?.isRunning == true) return
            val created = CsGameLogTap(target) { line -> onLogLine(line) }
            created.start(scope)
            tap = created
        }
        Log.i(TAG, "watching ${target.name} for a LAN publish")
    }

    /** Stops watching without tearing down a live tunnel — for a session the player wants kept. */
    fun detachLogWatch() {
        synchronized(this) { tap?.stop(); tap = null }
    }

    /**
     * Ends the tunnel and clears the hosting presence.
     *
     * Called when the world closes, when the player turns sharing off, and from
     * [shutdownForProcessExit].
     */
    fun stop() {
        scope.launch { stopInternal(clearPresence = true) }
    }

    /**
     * Best-effort synchronous shutdown for the native process-exit hook.
     *
     * Deliberately **not** a coroutine: the process is going away and there is no scheduler left to
     * resume on. Presence is cleared with a bounded blocking wait so friends do not keep seeing a
     * world that no longer exists — the one case in this class where blocking is the right call.
     */
    @JvmStatic
    fun shutdownForProcessExit() {
        val h = handle
        handle = 0L
        if (h == 0L) return
        runCatching { E4mcBridge.stop(h) }
        // One bounded blocking sync so the friends list does not keep advertising a dead world.
        // 1.5 s is far shorter than the process teardown it is racing, and the launcher is exiting
        // anyway — a dropped presence request here costs a stale "hosting" badge for one poll cycle.
        runCatching { FriendsClient.clearHostingBlocking(timeoutMillis = 1500L) }
    }

    /** Sharing on/off for this session; turning it off tears the tunnel down, on resumes if a port is known. */
    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        HostSettings.sharingEnabled = enabled
        if (!enabled) {
            stop()
        } else {
            val port = _state.value.port
            if (port > 0 && handle == 0L) start(port)
        }
    }

    // -----------------------------------------------------------------------
    // Log parsing
    // -----------------------------------------------------------------------

    /**
     * Every Minecraft version and mod loader prints its own wording, and the CS host agent adds a
     * line of its own so a 26.x client — which logs nothing on "Open to LAN" — is still detected.
     * One alternation covers all of them; the chat echo is filtered out below because it carries
     * `[CHAT]` and would otherwise be parsed twice for the same port (harmless, but noisy).
     */
    private val PORT_REGEX = Regex(
        """(?:Local game hosted on port|Started serving on|Published LAN server on port|""" +
            """open to LAN\. The port number is|\[cs-host\] published=true port=)\s*(\d{2,5})"""
    )

    private val LEVEL_REGEX = Regex("""Preparing level "(.+?)"""")

    /**
     * Handles one complete log line: remembers the world name, starts or restarts the tunnel on a
     * publish line, and stops it when the integrated server shuts down.
     *
     * Runs on the tail's IO thread, so every mutation is dispatched back through [scope].
     */
    internal fun onLogLine(text: String) {
        LEVEL_REGEX.find(text)?.let { match ->
            val world = match.groupValues[1].trim()
            if (world.isNotEmpty()) _state.update { it.copy(world = world) }
            return
        }

        PORT_REGEX.find(text)?.let { match ->
            val port = match.groupValues[1].toIntOrNull() ?: return@let
            if (port !in 1..65535) return@let
            val current = _state.value
            // Same port again (the chat echo of the same publish) — nothing to do.
            if (port == current.port && handle != 0L) return@let
            _state.update { it.copy(port = port, world = current.world) }
            if (HostSettings.sharingEnabled && current.enabled) start(port)
            return
        }

        // Closing the world to multiplayer without leaving it: the agent line covers 26.x, the
        // vanilla lines cover everything older.
        if (handle != 0L && (
                text.contains("Stopping singleplayer server") ||
                    text.contains("[cs-host] Stopping server publish") ||
                    // Bare "Stopping server" also appears when leaving a *multiplayer* server, but by
                    // then this launcher is not hosting, so the handle check above is what disambiguates.
                    text.contains("Stopping server")
                )
        ) {
            stop()
        }
    }

    // -----------------------------------------------------------------------
    // Tunnel
    // -----------------------------------------------------------------------

    /**
     * Tunnels local port [port] and publishes the address to friends.
     *
     * Restarts cleanly if already tunnelled (the player reopened the world on a new port), which is
     * why the old handle is always stopped before a new one is opened — two tunnels for one world
     * would leave a stale address on friends' lists pointing at a dead port.
     */
    fun start(port: Int) {
        scope.launch {
            mutex.withLock {
                stopInternal(clearPresence = handle != 0L)

                if (!E4mcBridge.load()) {
                    _state.update {
                        it.copy(
                            phase = CsHostPhase.FAILED,
                            problem = E4mcBridge.unavailabilityReason,
                            supported = false,
                            unsupportedReason = E4mcBridge.unavailabilityReason
                        )
                    }
                    return@withLock
                }

                val opened = E4mcBridge.start(port, HostSettings.relayOrNull)
                if (opened == 0L) {
                    val why = E4mcBridge.lastError() ?: "Could not reach a relay"
                    Log.w(TAG, "tunnel start failed: $why")
                    _state.update { it.copy(phase = CsHostPhase.FAILED, problem = why) }
                    return@withLock
                }

                handle = opened
                startedAt = System.currentTimeMillis()
                announced.clear()
                HostSettings.clearInvited()
                _state.update {
                    it.copy(
                        phase = CsHostPhase.STARTING,
                        port = port,
                        address = null,
                        problem = null,
                        guests = emptyList(),
                        refusals = emptyList()
                    )
                }
                applyPolicy()
                beginPolling()
            }
        }
    }

    /** Re-reads [HostSettings] and hands the relay the current ceiling, name list and block list. */
    fun applyPolicy() {
        val h = handle
        if (h == 0L) return
        val allowed = HostSettings.allowedNames()
        // The host occupies one of the slots the counter shows, and the relay counts guests only;
        // "no limit" is expressed as 0, which is why the two cannot share a value.
        val max = HostSettings.maxPlayers
        val guestCeiling = if (max == HostSettings.NO_LIMIT) 0 else (max - 1).coerceAtLeast(1)
        E4mcBridge.setPolicy(h, guestCeiling, allowed, HostSettings.closedReason)
        E4mcBridge.setBlocked(h, HostSettings.blockedNames())
    }

    /** Ends a guest's connection and keeps them out for the rest of this session. */
    fun kick(name: String) {
        HostSettings.kick(name)
    }

    /**
     * Invites friends to the world that is live right now.
     *
     * @param friendId one friend, or null for **everyone** — the backend fans a null target out to the
     *   whole friends list, so "invite all" is one request instead of N, and friends who are offline
     *   still find the invite waiting next time they open the launcher.
     * @return null on success, otherwise the message to show
     *
     * Owned here rather than in the UI because the address, world name and engine all live in this
     * object's state: a screen that assembled them itself would be reading three values that can
     * change between the reads, and could invite friends to an address that has already gone.
     */
    suspend fun invite(friendId: String? = null): String? {
        val current = _state.value
        val address = current.address
        if (address.isNullOrBlank()) return "There is no address to share yet"
        HostSettings.noteInvitedAll()
        return FriendsClient.invite(
            toId = friendId,
            address = address,
            world = current.world,
            engine = CsWorldEngine.JAVA,
            version = null
        )
    }

    /**
     * Drains tunnel status into [state] on a timer.
     *
     * Two rates because the two phases have different costs: while dialling, the address is the only
     * thing the player is waiting on, so 500 ms keeps the card from looking stuck; once published,
     * nothing changes but the guest list, and 2 s is plenty while costing less battery than the game
     * itself does.
     */
    private fun beginPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val h = handle
                if (h == 0L) break
                poll(h)
                delay(if (_state.value.address == null) POLL_DIALLING_MS else POLL_PUBLISHED_MS)
            }
        }
    }

    private suspend fun poll(h: Long) {
        val domain = E4mcBridge.domain(h)
        val problem = E4mcBridge.problem(h)
        val names = E4mcBridge.guestNames(h)
        val count = E4mcBridge.guestCount(h)
        val refusals = E4mcBridge.pollRefusals(h)

        var publishedNow = false

        _state.update { current ->
            var next = current

            if (!domain.isNullOrEmpty() && domain != current.address) {
                next = next.copy(address = domain, phase = CsHostPhase.PUBLISHED, problem = null)
                publishedNow = true
            }

            if (problem != null && problem != current.problem) {
                // A problem with an address already assigned is a warning, not a failure: guests who
                // are in stay in, and the card should say so rather than claim the world went dark.
                next = next.copy(
                    problem = problem,
                    phase = if (next.address == null) CsHostPhase.FAILED else next.phase
                )
            }

            // An older library has no guest names; fall back to the count so the card still shows occupancy.
            val guests = names.ifEmpty { List(count) { "Player ${it + 1}" } }
            if (guests != current.guests) next = next.copy(guests = guests)

            if (refusals.isNotEmpty()) {
                next = next.copy(refusals = (refusals + current.refusals).take(MAX_REFUSALS))
            }

            // Dialled for too long with nothing to show: say so instead of spinning forever.
            if (next.address == null &&
                next.phase == CsHostPhase.STARTING &&
                System.currentTimeMillis() - startedAt > DIAL_TIMEOUT_MS
            ) {
                next = next.copy(
                    phase = CsHostPhase.FAILED,
                    problem = next.problem ?: "Could not reach a relay — check the connection and try again"
                )
            }

            next
        }

        if (publishedNow) {
            val address = _state.value.address
            if (address != null) {
                Log.i(TAG, "published $address -> 127.0.0.1:${_state.value.port}")
                // Presence is what makes the world discoverable: friends see "hosting" with the
                // address attached and can join straight from the list.
                FriendsClient.setHosting(
                    address = address,
                    world = _state.value.world,
                    engine = CsWorldEngine.JAVA,
                    version = null
                )
            }
        }

        // Fire join/leave edges exactly once per guest.
        val now = _state.value.guests.toMutableList()
        val before = synchronized(announced) { ArrayList(announced).also { announced.clear(); announced.addAll(now) } }
        now.filter { it !in before }.forEach { name ->
            HostSettings.noteJoined(name)
            guestListeners.forEach { runCatching { it(name, true) } }
        }
        before.filter { it !in now }.forEach { name ->
            guestListeners.forEach { runCatching { it(name, false) } }
        }

        // Friends come and go mid-game; "friends only" has to follow the current list.
        if (HostSettings.access == HostSettings.ACCESS_FRIENDS) applyPolicy()
    }

    /** Stops the tunnel. [clearPresence] decides whether friends are told the world is gone. */
    private suspend fun stopInternal(clearPresence: Boolean) {
        pollJob?.cancel()
        pollJob = null
        val h = handle
        handle = 0L
        if (h != 0L) {
            E4mcBridge.stop(h)
            if (clearPresence) FriendsClient.clearHosting()
        }
        announced.clear()
        _state.update {
            it.copy(
                phase = CsHostPhase.IDLE,
                address = null,
                guests = emptyList(),
                refusals = emptyList(),
                problem = null
            )
        }
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------

    /** `(name, joined)` — the hosting card and the host's own notifications listen. */
    private val guestListeners = java.util.concurrent.CopyOnWriteArrayList<(String, Boolean) -> Unit>()

    fun addGuestListener(listener: (name: String, joined: Boolean) -> Unit) {
        guestListeners.addIfAbsent(listener)
    }

    fun removeGuestListener(listener: (String, Boolean) -> Unit) {
        guestListeners.remove(listener)
    }

    /** Convenience for Compose `collectAsState` sites that only care about the address. */
    val liveAddress: String? get() = _state.value.address

    /** True while a tunnel exists, whether or not it has an address yet. */
    val isActive: Boolean get() = handle != 0L
}

