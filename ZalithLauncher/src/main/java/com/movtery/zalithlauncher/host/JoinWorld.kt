/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The join flow follows CS Launcher Plus, (C) CRAFT STUDIO: an invite carries an address and the
 * host's game version, the guest is warned before a cross-version join that cannot work, and hosts
 * joined before stay joinable without a fresh invite.
 */

package com.movtery.zalithlauncher.host

import android.util.Log
import com.movtery.zalithlauncher.friends.CsWorldEngine
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.launcherMMKV
import com.movtery.zalithlauncher.utils.network.ServerAddress

private const val TAG = "JoinWorld"

/** Why a join cannot proceed, in words the UI can show directly. */
sealed interface CsJoinFailure {
    /** The world belongs to an engine this launcher cannot run. */
    data class WrongEngine(val engine: String) : CsJoinFailure

    /** The address is empty or cannot be parsed as `host[:port]`. */
    data class BadAddress(val raw: String) : CsJoinFailure

    /** No version is selected, so there is nothing to launch into the world with. */
    data object NoVersion : CsJoinFailure
}

/** A join that is ready to launch. */
data class CsJoinPlan(
    /** Normalized `host:port` — exactly what goes in the multiplayer "direct connect" field. */
    val address: String,
    /** The version to launch, chosen by [JoinWorld.plan]. */
    val version: Version,
    /** The host's game version when the invite carried one, else null. */
    val hostVersion: String?,
    /** Non-null when the host's version and the selected one differ; the join cannot work. */
    val versionWarning: String?
)

/**
 * Turns a friend's invite or hosting presence into a launch that lands inside their world.
 *
 * **No game arguments are built here.** The launch pipeline already knows how to enter a server:
 * `LaunchArgs` emits `--quickPlayMultiplayer host:port` on 1.20+ and falls back to `--server host
 * --port N` on anything older, choosing between them from the version's own `VersionInfo`. Keeping
 * that decision in one place matters — a second implementation here is how a launcher ends up with
 * two disagreeing ideas of what "join" means, and the failure shows up as a silently ignored
 * command-line argument.
 *
 * What this class owns is everything the launch pipeline cannot know: which address is pending,
 * whether it is still fresh, whether the host is running a version this client can speak to, and
 * which hosts this player has joined before.
 */
object JoinWorld {

    private const val K_ADDRESS = "cs.join.address"
    private const val K_AT = "cs.join.at"
    private const val K_VERSION = "cs.join.version"
    private const val K_KNOWN_HOSTS = "cs.friends.known_hosts"

    /**
     * How long a pending join stays valid.
     *
     * Three minutes covers "tap join, wait through the launch, play", including a slow mod-pack
     * check, while being short enough that yesterday's address can never hijack today's launch and
     * drop the player into a world that has long since closed.
     */
    private const val TTL_MS = 3 * 60 * 1000L

    private fun prefs() = launcherMMKV()

    // -----------------------------------------------------------------------
    // Planning a join
    // -----------------------------------------------------------------------

    /**
     * Builds a launch plan for [address], or explains why there cannot be one.
     *
     * @param versionResolver supplies the version to launch; defaults to the selected one
     */
    fun plan(
        address: String,
        engine: String?,
        hostVersion: String? = null,
        versionResolver: () -> Version? = { VersionsManager.currentVersion.value }
    ): Either<CsJoinFailure, CsJoinPlan> {
        // This launcher runs Java Edition only. A Bedrock world cannot be joined however plausible
        // its address looks, and saying so up front beats a client that times out against the wrong
        // protocol with no explanation.
        if (engine != null && !CsWorldEngine.isJavaLike(engine)) {
            return Either.Left(CsJoinFailure.WrongEngine(engine))
        }

        val normalized = normalize(address) ?: return Either.Left(CsJoinFailure.BadAddress(address))
        val version = versionResolver() ?: return Either.Left(CsJoinFailure.NoVersion)

        return Either.Right(
            CsJoinPlan(
                address = normalized,
                version = version,
                hostVersion = hostVersion?.trim()?.takeIf { it.isNotEmpty() },
                versionWarning = versionWarning(hostVersion, version.getVersionName())
            )
        )
    }

    /**
     * Records a pending join so the launch that follows enters the world.
     *
     * Persisted rather than held in memory because the launch can outlive the screen that started it,
     * and because a version switch in between must not lose the address the player tapped.
     */
    fun remember(address: String, hostVersion: String? = null) {
        val normalized = normalize(address) ?: return
        prefs().apply {
            encode(K_ADDRESS, normalized)
            encode(K_AT, System.currentTimeMillis())
            encode(K_VERSION, hostVersion?.trim().orEmpty())
        }
        Log.i(TAG, "pending join -> $normalized (host ${hostVersion ?: "unknown"})")
    }

    /** Remembers the host too, so this friend's worlds stay joinable without a new invite. */
    fun rememberFriend(friendId: String?, address: String, hostVersion: String? = null) {
        rememberHost(friendId)
        remember(address, hostVersion)
    }

    /**
     * The address for the launch happening now, cleared so it cannot be consumed twice.
     *
     * Consuming on read is what keeps an ordinary launch from silently re-entering the last world the
     * player was invited to.
     */
    fun consume(): String? {
        val prefs = prefs()
        val address = prefs.decodeString(K_ADDRESS, null)
        val at = prefs.decodeLong(K_AT, 0L)
        prefs.removeValueForKey(K_ADDRESS)
        prefs.removeValueForKey(K_AT)
        prefs.removeValueForKey(K_VERSION)
        if (address.isNullOrBlank()) return null
        if (System.currentTimeMillis() - at > TTL_MS) {
            Log.i(TAG, "pending join expired")
            return null
        }
        return normalize(address)
    }

    /** True while an unexpired pending join exists — the UI uses this to relabel the PLAY button. */
    val hasPendingJoin: Boolean
        get() {
            val prefs = prefs()
            if (prefs.decodeString(K_ADDRESS, null).isNullOrBlank()) return false
            return System.currentTimeMillis() - prefs.decodeLong(K_AT, 0L) <= TTL_MS
        }

    /**
     * The host's game version for the pending join, or null when unknown or expired.
     *
     * Not cleared by reading: both the warning UI and the launch need it, and only [consume] ends the
     * pending state.
     */
    fun pendingHostVersion(): String? {
        val prefs = prefs()
        if (prefs.decodeString(K_ADDRESS, null).isNullOrBlank()) return null
        if (System.currentTimeMillis() - prefs.decodeLong(K_AT, 0L) > TTL_MS) return null
        return prefs.decodeString(K_VERSION, "").orEmpty().takeIf { it.isNotEmpty() }
    }

    /** Cancels a pending join without launching. */
    fun cancelPending() {
        prefs().apply {
            removeValueForKey(K_ADDRESS)
            removeValueForKey(K_AT)
            removeValueForKey(K_VERSION)
        }
    }

    // -----------------------------------------------------------------------
    // Addresses
    // -----------------------------------------------------------------------

    /**
     * Cleans an address into the `host:port` form the game wants.
     *
     * Handles the shapes that actually turn up: a bare e4mc domain (`abc.e4mc.link`, meaning port
     * 25565), `host:port`, a LAN IP, and the `https://abc.e4mc.link/` a messaging app produces when
     * it auto-links a pasted address. Delegates to [ServerAddress.parse] so IDN hosts and IPv6
     * literals are handled by the same code the rest of the launcher uses.
     *
     * @return the normalized address, or null when nothing usable remains
     */
    fun normalize(raw: String?): String? {
        var text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        text = text.substringAfter("://", text).substringBefore('/').trim()
        if (text.isEmpty()) return null

        return runCatching {
            val parsed = ServerAddress.parse(text)
            val port = parsed.port.takeIf { it in 1..65535 } ?: ServerAddress.DEFAULT_PORT
            "${parsed.getASCIIHost()}:$port"
        }.getOrElse {
            Log.w(TAG, "could not parse address: $raw", it)
            null
        }
    }

    // -----------------------------------------------------------------------
    // Version compatibility
    // -----------------------------------------------------------------------

    /**
     * Warns when the host's version and the selected version differ.
     *
     * Java Edition has no cross-version play: the server refuses the handshake. The launcher cannot
     * fix that, but it can say so *before* the player waits through a launch to discover it in a
     * disconnect screen. Comparison is on the release number, which is what the protocol follows —
     * `1.21.4` and `1.21.5` are genuinely incompatible, while a mod-loader id such as
     * `fabric-loader-0.15-1.20.4` still carries the release it was built from.
     *
     * @return a message to show, or null when the versions are compatible or either is unknown
     */
    fun versionWarning(hostVersion: String?, myVersionName: String?): String? {
        val host = releaseOf(hostVersion) ?: return null
        val mine = releaseOf(myVersionName) ?: return null
        if (host == mine) return null
        return "That world runs Minecraft $host and this version is $mine. Java Edition cannot join " +
            "across versions — switch to $host first."
    }

    /** True when [hostVersion] and [myVersionName] name the same release, or either is unknown. */
    fun isCompatible(hostVersion: String?, myVersionName: String?): Boolean =
        versionWarning(hostVersion, myVersionName) == null

    /** Extracts a comparable `1.x[.y]` release from a version id, or null when there is none. */
    internal fun releaseOf(versionId: String?): String? {
        val text = versionId?.trim().orEmpty()
        if (text.isEmpty()) return null
        // A plain release or snapshot id starts with the release number.
        Regex("""^1\.(\d+)(?:\.(\d+))?""").find(text)?.let { return formatRelease(it) }
        // Otherwise look for an embedded release, which is how modded ids carry theirs.
        Regex("""1\.(\d+)(?:\.(\d+))?""").find(text)?.let { return formatRelease(it) }
        return null
    }

    private fun formatRelease(match: MatchResult): String {
        val minor = match.groupValues[1]
        val patch = match.groupValues[2]
        return if (patch.isEmpty()) "1.$minor" else "1.$minor.$patch"
    }

    // -----------------------------------------------------------------------
    // Hosts joined before
    // -----------------------------------------------------------------------

    /**
     * Friend ids whose world this player has joined before.
     *
     * Persisted across launches, which is what lets the friends list offer "join" on a familiar host
     * without a fresh invite — the Bedrock Realm behaviour players expect, and the mirror of
     * [HostSettings.knownPlayers] on the hosting side.
     */
    fun knownHosts(): Set<String> =
        prefs().decodeString(K_KNOWN_HOSTS, "").orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun isKnownHost(friendId: String?): Boolean {
        val id = friendId?.trim()
        return !id.isNullOrEmpty() && id in knownHosts()
    }

    fun rememberHost(friendId: String?) {
        val id = friendId?.trim()
        if (id.isNullOrEmpty()) return
        val known = knownHosts().toMutableSet()
        if (known.add(id)) prefs().encode(K_KNOWN_HOSTS, known.joinToString("\n"))
    }

    fun forgetHost(friendId: String?) {
        val id = friendId?.trim()
        if (id.isNullOrEmpty()) return
        val known = knownHosts().toMutableSet()
        if (known.remove(id)) prefs().encode(K_KNOWN_HOSTS, known.joinToString("\n"))
    }

    fun clearKnownHosts() {
        prefs().removeValueForKey(K_KNOWN_HOSTS)
    }
}

/**
 * Minimal either/or, because a join either has a plan or has a reason it cannot.
 *
 * Kotlin's `Result` was considered and rejected: it can only carry a `Throwable`, and a wrong-engine
 * invite is not an exception — it is an expected outcome the UI must present as a sentence.
 */
sealed interface Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>
    data class Right<out R>(val value: R) : Either<Nothing, R>

    /** The plan, or null when the join cannot proceed. */
    val rightOrNull: R? get() = (this as? Right)?.value

    /** The reason the join cannot proceed, or null when it can. */
    val leftOrNull: L? get() = (this as? Left)?.value
}
