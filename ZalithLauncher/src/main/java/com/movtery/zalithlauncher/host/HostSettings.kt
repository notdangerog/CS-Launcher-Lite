/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The access-control model (player-limit steps, anyone / friends-only / invited-only, the
 * "joined before" memory and per-session kick list) follows CS Launcher Plus, (C) CRAFT STUDIO,
 * used here so that a world hosted from either launcher behaves identically to its guests.
 */

package com.movtery.zalithlauncher.host

import com.movtery.zalithlauncher.friends.FriendsClient
import com.movtery.zalithlauncher.setting.launcherMMKV

/**
 * Persistent settings for hosting a world to friends, plus the per-session lists that decide who
 * the relay lets through.
 *
 * **Why the relay decides, not the game.** The integrated server has no concept of an allow-list;
 * by the time it sees a connection the player is already in. So every access rule here is enforced
 * at the tunnel door by [WorldHostManager.applyPolicy], which hands the relay a guest ceiling and
 * a name list. A guest the list keeps out is refused before any Minecraft packet is exchanged, and
 * gets [closedReason] as the explanation.
 */
object HostSettings {

    // ---- keys ----
    private const val K_MAX = "cs.host.max_players"
    private const val K_ACCESS = "cs.host.access"
    private const val K_RELAY = "cs.host.relay"
    private const val K_ICON = "cs.host.icon"
    private const val K_KNOWN = "cs.host.known_players"
    private const val K_ENABLED = "cs.host.enabled"

    /** Sentinel meaning "no ceiling at the relay"; the game is still told [JAVA_MAX_PLAYERS]. */
    const val NO_LIMIT = -1

    /** Vanilla's own hard cap on the integrated server's player list. */
    const val JAVA_MAX_PLAYERS = 512

    /** The integrated server's stock LAN limit, which the host agent lifts. */
    const val JAVA_DEFAULT_MAX = 8

    /** The stops the +/- stepper walks; [NO_LIMIT] is the last one. */
    val STEPS: IntArray = intArrayOf(2, 4, 6, 8, 10, 12, 16, 20, 24, 32, 48, 64, 100, 200, NO_LIMIT)

    /** Who may connect. */
    const val ACCESS_ANYONE = 0
    const val ACCESS_FRIENDS = 1
    const val ACCESS_INVITED = 2

    /** Which icon guests see in their server list. */
    const val ICON_CS = 0
    const val ICON_PROFILE = 1
    const val ICON_CUSTOM = 2

    /** A 64x64 PNG with this name in the instance's game folder becomes the custom server icon. */
    const val CUSTOM_ICON_FILENAME = "cs-server-icon-custom.png"

    private fun prefs() = launcherMMKV()

    // -----------------------------------------------------------------------
    // Sharing on/off
    // -----------------------------------------------------------------------

    /**
     * Whether the launcher should tunnel the world at all.
     *
     * Off means a LAN world stays LAN-only: the port line in the game log is ignored, so a player
     * who wants a private session does not have to sign out of friends to get one.
     */
    var sharingEnabled: Boolean
        get() = prefs().decodeBool(K_ENABLED, true)
        set(value) {
            prefs().encode(K_ENABLED, value)
            if (!value) WorldHostManager.stop()
        }

    // -----------------------------------------------------------------------
    // Player limit
    // -----------------------------------------------------------------------

    /** Players the world takes, host included: 2..[JAVA_MAX_PLAYERS], or [NO_LIMIT]. */
    var maxPlayers: Int
        get() {
            val n = prefs().decodeInt(K_MAX, JAVA_DEFAULT_MAX)
            if (n == NO_LIMIT) return NO_LIMIT
            return n.coerceIn(2, JAVA_MAX_PLAYERS)
        }
        set(value) {
            prefs().encode(K_MAX, if (value == NO_LIMIT) NO_LIMIT else value.coerceIn(2, JAVA_MAX_PLAYERS))
            WorldHostManager.applyPolicy()
        }

    /**
     * The ceiling actually handed to the game at start.
     *
     * "No limit" still has to pass the integrated server a concrete number, so it passes
     * [JAVA_MAX_PLAYERS]; the relay is the part that stops counting.
     */
    val javaMaxPlayers: Int
        get() = maxPlayers.let { if (it == NO_LIMIT) JAVA_MAX_PLAYERS else it }

    /** The next stop up (`+1`) or down (`-1`) from the current limit. */
    fun stepMaxPlayers(direction: Int): Int {
        val current = maxPlayers
        var index = if (current == NO_LIMIT) STEPS.lastIndex else STEPS.lastIndex - 1
        if (current != NO_LIMIT) {
            for (k in STEPS.indices) {
                if (STEPS[k] >= current) {
                    index = k
                    break
                }
            }
        }
        index = (index + direction).coerceIn(0, STEPS.lastIndex)
        return STEPS[index]
    }

    /** `"No limit"` or the number, for the settings row. */
    val maxPlayersLabel: String
        get() = maxPlayers.let { if (it == NO_LIMIT) "No limit" else it.toString() }

    // -----------------------------------------------------------------------
    // Access
    // -----------------------------------------------------------------------

    /** [ACCESS_ANYONE], [ACCESS_FRIENDS] or [ACCESS_INVITED]. */
    var access: Int
        get() = prefs().decodeInt(K_ACCESS, ACCESS_ANYONE).coerceIn(ACCESS_ANYONE, ACCESS_INVITED)
        set(value) {
            prefs().encode(K_ACCESS, value.coerceIn(ACCESS_ANYONE, ACCESS_INVITED))
            WorldHostManager.applyPolicy()
        }

    fun accessLabel(mode: Int): String = when (mode) {
        ACCESS_FRIENDS -> "CS friends only"
        ACCESS_INVITED -> "Invited players only"
        else -> "Anyone with the address"
    }

    /** What a guest the allow-list keeps out is told, so a refusal is not a mystery. */
    val closedReason: String
        get() = when (access) {
            ACCESS_FRIENDS -> "This world is open to the host's CS friends only."
            ACCESS_INVITED -> "This world is open to invited players only."
            else -> ""
        }

    /**
     * The names the relay admits under the current access setting; null means "anyone".
     *
     * "Invited only" also admits everyone in [knownPlayers], which is what makes an invite a
     * one-time act rather than a per-session ritual — a friend who joined last week can come back
     * without being re-invited, exactly like a Bedrock Realm.
     */
    fun allowedNames(): List<String>? = when (access) {
        ACCESS_FRIENDS -> FriendsClient.state.value.friends.mapNotNull { it.name.takeIf(String::isNotEmpty) }
        ACCESS_INVITED -> buildList {
            invited.forEach { add(it) }
            knownPlayers().forEach { if (it !in this) add(it) }
        }
        else -> null
    }

    // -----------------------------------------------------------------------
    // Relay
    // -----------------------------------------------------------------------

    /** A relay `host:port` the player chose; empty means "whichever relay is least loaded". */
    var relay: String
        get() = prefs().decodeString(K_RELAY, "").orEmpty().trim()
        set(value) {
            prefs().encode(K_RELAY, value.trim())
        }

    /**
     * The relay value to hand the native side, or null when unset or malformed.
     *
     * Validated here rather than in the JNI layer so a typo in the settings field cannot reach the
     * tunnel as a half-parsed string.
     */
    val relayOrNull: String?
        get() {
            val value = relay
            if (value.isEmpty()) return null
            val split = value.lastIndexOf(':')
            if (split <= 0 || split == value.length - 1) return null
            val port = value.substring(split + 1).toIntOrNull() ?: return null
            if (port !in 1..65535) return null
            return value
        }

    // -----------------------------------------------------------------------
    // Server icon
    // -----------------------------------------------------------------------

    var iconMode: Int
        get() = prefs().decodeInt(K_ICON, ICON_CS).coerceIn(ICON_CS, ICON_CUSTOM)
        set(value) {
            prefs().encode(K_ICON, value.coerceIn(ICON_CS, ICON_CUSTOM))
        }

    fun iconLabel(mode: Int): String = when (mode) {
        ICON_PROFILE -> "Profile icon"
        ICON_CUSTOM -> "Custom ($CUSTOM_ICON_FILENAME)"
        else -> "CS Launcher"
    }

    // -----------------------------------------------------------------------
    // Invites (this hosting session)
    // -----------------------------------------------------------------------

    private val invited = LinkedHashSet<String>()
    private val blocked = LinkedHashSet<String>()

    /** Records that [name] was invited to the running world. */
    @Synchronized
    fun noteInvited(name: String?) {
        val trimmed = name?.trim()
        if (trimmed.isNullOrEmpty()) return
        invited += trimmed
        WorldHostManager.applyPolicy()
    }

    /**
     * Records that every friend was invited.
     *
     * Offline friends are included: the invite is stored server-side, so they see it the next time
     * they open the launcher rather than never.
     */
    @Synchronized
    fun noteInvitedAll() {
        FriendsClient.state.value.friends
            .mapNotNull { it.name.takeIf(String::isNotEmpty) }
            .forEach { invited += it }
        WorldHostManager.applyPolicy()
    }

    @Synchronized
    fun invitedCount(): Int = invited.size

    /** Clears both the invite list and the session's kick list — a new world is a new session. */
    @Synchronized
    fun clearInvited() {
        invited.clear()
        blocked.clear()
    }

    // -----------------------------------------------------------------------
    // Players who joined before
    // -----------------------------------------------------------------------

    /**
     * Names that completed a login to one of this player's worlds.
     *
     * Persisted across sessions, so "invited only" keeps working next week without the host having
     * to remember who was ever let in.
     */
    fun knownPlayers(): Set<String> =
        prefs().decodeString(K_KNOWN, "").orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    @Synchronized
    fun noteJoined(name: String?) {
        val trimmed = name?.trim()
        if (trimmed.isNullOrEmpty()) return
        val known = knownPlayers().toMutableSet()
        if (known.add(trimmed)) prefs().encode(K_KNOWN, known.joinToString("\n"))
    }

    @Synchronized
    fun forgetPlayer(name: String?) {
        val trimmed = name?.trim()
        if (trimmed.isNullOrEmpty()) return
        val known = knownPlayers().toMutableSet()
        if (known.remove(trimmed)) {
            prefs().encode(K_KNOWN, known.joinToString("\n"))
            WorldHostManager.applyPolicy()
        }
    }

    @Synchronized
    fun clearKnownPlayers() {
        prefs().removeValueForKey(K_KNOWN)
        WorldHostManager.applyPolicy()
    }

    // -----------------------------------------------------------------------
    // Kicks (this hosting session)
    // -----------------------------------------------------------------------

    /**
     * Removes [name] from the world: their stream ends and they cannot reconnect this session.
     *
     * Also forgets them, so a kicked player is not silently readmitted by the "joined before"
     * memory when access is set to invited-only.
     */
    @Synchronized
    fun kick(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        blocked += trimmed
        invited -= trimmed
        forgetPlayer(trimmed)
        WorldHostManager.kick(trimmed)
    }

    @Synchronized
    fun unblock(name: String) {
        blocked -= name.trim()
        WorldHostManager.applyPolicy()
    }

    @Synchronized
    fun blockedNames(): List<String> = blocked.toList()
}
