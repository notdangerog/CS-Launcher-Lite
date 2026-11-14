/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.friends

import org.json.JSONObject

/**
 * Immutable value types for the CS friends service.
 *
 * Every type here is a data class with defaults, so a partially-populated reply from the backend
 * still produces a usable object instead of throwing — a friends list that renders with one missing
 * field beats a screen that crashes because a column was added server-side.
 */

/** Presence reported by a friend's launcher. */
enum class CsFriendStatus(val wire: String) {
    /** Launcher closed, or the player explicitly went offline. */
    OFFLINE("offline"),

    /** Launcher open, not in a game. */
    ONLINE("online"),

    /** In a game that is not shared with friends. */
    PLAYING("playing"),

    /** Hosting a world that friends can join — [CsFriend.address] is the public address. */
    HOSTING("hosting");

    /** True when the friend's launcher is reachable at all. */
    val isOnline: Boolean get() = this != OFFLINE

    /** True when [CsFriend.address] can be joined right now. */
    val isHosting: Boolean get() = this == HOSTING

    companion object {
        /** Maps the wire value, treating anything unrecognised as offline rather than inventing a state. */
        fun of(wire: String?): CsFriendStatus =
            entries.firstOrNull { it.wire.equals(wire, ignoreCase = true) } ?: OFFLINE
    }
}

/** Which engine a hosted world runs, so a guest knows whether a cross-version join is needed. */
object CsWorldEngine {
    const val LAUNCHER = "launcher"
    const val JAVA = "java"
    const val BEDROCK = "bedrock"

    /** True when [engine] names a Minecraft world rather than "just has the launcher open". */
    fun isPlayable(engine: String?): Boolean =
        engine != null && engine.isNotEmpty() && engine != LAUNCHER

    /**
     * True when [engine] is a world this launcher can actually join.
     *
     * The friends backend is shared with CS Launcher Plus, which can host Bedrock worlds too; this
     * launcher runs Java Edition only, so a Bedrock invite has to be refused with an explanation
     * instead of being handed to the launch pipeline, which would fail at the protocol layer with a
     * message nobody can act on.
     */
    fun isJavaLike(engine: String?): Boolean =
        engine == null || engine.isEmpty() || engine == JAVA || engine == LAUNCHER
}

/**
 * One entry in the friends list.
 *
 * @property address public `host:port` when this friend is hosting; null otherwise. This is exactly
 *   what a guest types into the multiplayer "direct connect" field, so it is stored verbatim.
 * @property version game version of the hosted world, used to warn before a cross-version join.
 */
data class CsFriend(
    val id: String,
    val name: String,
    val code: String = "",
    val status: CsFriendStatus = CsFriendStatus.OFFLINE,
    val address: String? = null,
    val world: String? = null,
    val engine: String? = null,
    val version: String? = null
) {
    /** True when this friend can be joined right now. */
    val isJoinable: Boolean get() = !address.isNullOrEmpty()

    /** True when the friend's launcher is open. */
    val isOnline: Boolean get() = status.isOnline

    /** A short human label for the hosted world, or null when there is nothing to show. */
    val worldLabel: String? get() = world?.takeIf { it.isNotBlank() }

    /** True when this friend's world runs a different game version than [mine]. */
    fun isCrossVersion(mine: String?): Boolean =
        isJoinable && !mine.isNullOrBlank() && !version.isNullOrBlank() && version != mine
}

/** A pending friend request, incoming or outgoing. */
data class CsFriendRequest(
    val id: String,
    val name: String,
    val code: String = "",
    val incoming: Boolean = true,
    val at: Long = 0L
)

/**
 * An invitation to join a running world.
 *
 * Invites expire server-side after ten minutes; [isLive] applies the same window locally so the UI
 * never offers a "Join" button for an address that has already stopped resolving.
 */
data class CsWorldInvite(
    val id: String,
    val from: String,
    val fromId: String = "",
    val address: String,
    val world: String? = null,
    val engine: String = CsWorldEngine.JAVA,
    val version: String? = null,
    val at: Long = 0L
) {
    companion object {
        /** The server's invite lifetime: ten minutes. */
        const val LIFETIME_MS = 10 * 60_000L
    }

    /** True while the invite is still within its lifetime relative to [now]. */
    fun isLive(now: Long): Boolean = address.isNotEmpty() && now - at < LIFETIME_MS
}

/**
 * One chat message.
 *
 * A world invite can also travel as a chat message ([kind] == `"invite"`), so a conversation keeps
 * the context of who was invited where — the invite row alone would lose that.
 */
data class CsChatMessage(
    val id: String,
    val from: String,
    val to: String = "",
    val body: String = "",
    val at: Long = 0L,
    /** True when this device's account sent it. */
    val mine: Boolean = false,
    /** Public URL of an attached photo, or null for a text-only message. */
    val image: String? = null,
    /** `"invite"` for a world invite carried as a message; null for an ordinary message. */
    val kind: String? = null,
    val replyTo: String? = null,
    val replyFrom: String? = null,
    val replyBody: String? = null,
    val inviteAddress: String? = null,
    val inviteWorld: String? = null,
    val inviteEngine: String? = null,
    val inviteVersion: String? = null
) {
    val hasText: Boolean get() = body.isNotEmpty()

    /** True when this message is a world invite with a usable address. */
    val isInvite: Boolean get() = kind == "invite" && !inviteAddress.isNullOrEmpty()

    /** True while the embedded invite is still joinable relative to [now]. */
    fun inviteLive(now: Long): Boolean = isInvite && now - at < CsWorldInvite.LIFETIME_MS
}

/** The signed-in CS identity, as returned by `cs_account_login` / `cs_account_register`. */
data class CsFriendAccount(
    val id: String,
    val username: String,
    val displayName: String,
    /** The short friend code other players use to add this account. */
    val code: String,
    /**
     * The session token. Passed as `p_token` on every call and never displayed.
     *
     * Only this token is kept on the device — the password is never stored, so a lost phone leaks a
     * revocable session rather than a credential that also works on the website.
     */
    val token: String
) {
    companion object {
        /** Username rule shared with the backend: 3-16 of letters, digits and underscore. */
        private val USERNAME = Regex("^[A-Za-z0-9_]{3,16}$")

        fun validUsername(value: String?): Boolean = value != null && USERNAME.matches(value)

        /** Bcrypt's 72-byte input limit is why the backend caps the password length here. */
        fun validPassword(value: String?): Boolean =
            value != null && value.length in 6..72

        /** Parses a session object from `cs_account_login` / `cs_account_register`. */
        fun fromSession(json: JSONObject): CsFriendAccount? {
            val token = json.strOrNull("token") ?: return null
            return CsFriendAccount(
                id = json.str("id"),
                username = json.str("username"),
                displayName = json.str("name").ifEmpty { json.str("username") },
                code = json.str("code"),
                token = token
            )
        }
    }
}

/**
 * Everything the friends UI renders, as one immutable snapshot.
 *
 * Publishing a single snapshot (rather than five separate flows) means the list, the badge counts
 * and the chat can never disagree with each other for a frame — which is what causes a badge to
 * show "2" above a list that has one row.
 */
data class CsFriendsState(
    val account: CsFriendAccount? = null,
    val friends: List<CsFriend> = emptyList(),
    val requests: List<CsFriendRequest> = emptyList(),
    val invites: List<CsWorldInvite> = emptyList(),
    val messages: List<CsChatMessage> = emptyList(),
    /** Server clock at the last successful sync, used for every expiry comparison. */
    val serverNow: Long = 0L,
    /** True while a sync or action is in flight. */
    val syncing: Boolean = false,
    /** Human-readable reason the last call failed, or null when healthy. */
    val lastError: String? = null
) {
    val signedIn: Boolean get() = account != null

    val onlineCount: Int get() = friends.count { it.isOnline }

    /** Friends currently hosting a joinable world — the number the rail badge shows. */
    val hostingCount: Int get() = friends.count { it.isJoinable }

    val incomingRequests: Int get() = requests.count { it.incoming }

    /** Invites still within their ten-minute window. */
    fun liveInvites(): List<CsWorldInvite> = invites.filter { it.isLive(serverNow) }

    /** Anything worth a badge: friend requests plus live world invites. */
    val attentionCount: Int get() = incomingRequests + liveInvites().size

    fun friendById(id: String?): CsFriend? =
        if (id == null) null else friends.firstOrNull { it.id == id }

    /** Messages exchanged with [otherId], oldest first, for one conversation thread. */
    fun threadWith(otherId: String): List<CsChatMessage> =
        messages.filter { it.from == otherId || it.to == otherId }.sortedBy { it.at }

    /** A copy with [error] recorded and the syncing flag cleared. */
    fun withError(error: String?): CsFriendsState = copy(lastError = error, syncing = false)
}
