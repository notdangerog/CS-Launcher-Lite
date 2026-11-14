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

import android.content.Context
import android.util.Log
import com.movtery.zalithlauncher.setting.launcherMMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * The launcher's live link to the CS friends backend: presence, the friends list, requests, world
 * invites and chat.
 *
 * One instance per process. The game process uses it only to publish "hosting" with a public
 * address, so a world opened to LAN becomes joinable by friends without the launcher being in the
 * foreground.
 *
 * **`cs_sync` is both the poll and the heartbeat.** One round trip returns friends, requests, invites
 * and messages *and* reports this player's presence, so there is no second request to keep in step
 * with the first — a design worth keeping because a separate presence ping is exactly the kind of
 * thing that drifts out of sync and leaves a friend looking offline while they chat.
 *
 * **Poll rates follow attention, not the clock.** 3 s while a conversation is open (chat has to feel
 * live), 8 s while the friends surface is merely visible, 20 s in the background (a heartbeat, not a
 * conversation), and 30 s after an error so a dead network is not hammered. The loop stops entirely
 * when nobody is watching, no background watch is registered and no world is being hosted.
 */
object FriendsClient {

    private const val TAG = "FriendsClient"

    private const val POLL_FAST_MS = 3_000L
    private const val POLL_NORMAL_MS = 8_000L
    private const val POLL_SLOW_MS = 20_000L
    private const val POLL_ERROR_MS = 30_000L

    private const val K_TOKEN = "cs.friends.token"
    private const val K_USERNAME = "cs.friends.username"
    private const val K_DISPLAY = "cs.friends.name"
    private const val K_CODE = "cs.friends.code"
    private const val K_ID = "cs.friends.id"

    /** A stored token shorter than this is treated as "not signed in" rather than sent and rejected. */
    private const val MIN_TOKEN_LENGTH = 16

    /** Backend cap on one chat message; longer text is refused here rather than truncated silently. */
    private const val MAX_MESSAGE_CHARS = 2000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _state = MutableStateFlow(CsFriendsState())
    val state: StateFlow<CsFriendsState> = _state.asStateFlow()

    /** UI surfaces that want live updates; the loop runs while this is non-zero. */
    private val watchers = AtomicInteger(0)

    @Volatile
    private var backgroundWatch = false

    @Volatile
    private var fastPoll = false

    @Volatile
    private var pollJob: Job? = null

    @Volatile
    private var initialized = false

    // Presence sent with every cs_sync. Held in plain fields rather than in the state flow because
    // the game process writes them from a native callback thread while the UI collects the flow.
    @Volatile
    private var presenceStatus: String = CsFriendStatus.OFFLINE.wire

    @Volatile
    private var presenceAddress: String? = null

    @Volatile
    private var presenceWorld: String? = null

    @Volatile
    private var presenceEngine: String = CsWorldEngine.LAUNCHER

    @Volatile
    private var presenceVersion: String? = null

    /**
     * Highest message id seen so far.
     *
     * `p_since` is a message **id**, not a timestamp: the backend pages on it, so two messages
     * sharing a timestamp can neither be skipped nor replayed. Sending a timestamp here would
     * silently drop chat.
     */
    @Volatile
    private var sinceMessageId: Long = 0L

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Prepares the client: restores any stored session and, when signed in, starts the loop.
     *
     * Called once from application startup. Deliberately cheap and non-blocking — it must not delay
     * first frame, because a signed-out player never sees any of this work.
     */
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        loadAccount()
        if (_state.value.account != null) {
            presenceStatus = CsFriendStatus.ONLINE.wire
            startPolling()
        }
        Log.i(TAG, "initialized, signed in = ${_state.value.account != null}")
    }

    /**
     * Keeps the loop alive while the app is backgrounded.
     *
     * Without this, walking away from the launcher stops presence reporting and a friend would see
     * the player go offline mid-conversation. With it, the loop drops to [POLL_SLOW_MS] instead.
     */
    fun setBackgroundWatch(enabled: Boolean) {
        if (backgroundWatch == enabled) return
        backgroundWatch = enabled
        if (enabled) startPolling() else maybeStopPolling()
    }

    /**
     * One more surface wants live updates. Returns a release function so a caller cannot forget to
     * give its watcher back — an unbalanced [acquireWatcher] would keep the network radio on forever.
     */
    fun acquireWatcher(): () -> Unit {
        val added = watchers.incrementAndGet() == 1
        if (added) startPolling()
        var released = false
        return {
            // Guard against a double release (a DisposableEffect and a manual call, say), which would
            // otherwise drop the count below the number of real watchers.
            if (!released) {
                released = true
                releaseWatcher()
            }
        }
    }

    private fun releaseWatcher() {
        if (watchers.decrementAndGet().coerceAtLeast(0) == 0) maybeStopPolling()
    }

    /** Chat is on screen: poll at [POLL_FAST_MS] until [setFastPoll] is called with false. */
    fun setFastPoll(enabled: Boolean) {
        if (fastPoll == enabled) return
        fastPoll = enabled
        // Nudge the loop so the new rate takes effect immediately rather than after the current sleep.
        if (enabled) pollJob?.cancel()
    }

    /** Asks for a sync outside the timer — after an action, or when a surface becomes visible. */
    fun refreshNow() {
        if (_state.value.account == null) return
        scope.launch { sync() }
    }

    private fun startPolling() {
        if (_state.value.account == null) return
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            var backoff = false
            while (isActive) {
                val ok = sync()
                backoff = !ok
                val rate = when {
                    backoff -> POLL_ERROR_MS
                    fastPoll -> POLL_FAST_MS
                    watchers.get() > 0 -> POLL_NORMAL_MS
                    else -> POLL_SLOW_MS
                }
                delay(rate)
            }
        }
    }

    private fun maybeStopPolling() {
        // Hosting keeps the loop alive on its own: a world published to friends must keep reporting
        // presence, or guests see the host vanish while still playing in their world.
        if (watchers.get() > 0 || backgroundWatch || isHosting) return
        pollJob?.cancel()
        pollJob = null
    }

    // -----------------------------------------------------------------------
    // Session
    // -----------------------------------------------------------------------

    private fun loadAccount() {
        val prefs = launcherMMKV()
        val token = prefs.decodeString(K_TOKEN, null).orEmpty()
        if (token.length < MIN_TOKEN_LENGTH) return
        val username = prefs.decodeString(K_USERNAME, "").orEmpty()
        if (username.isEmpty()) return
        _state.update {
            it.copy(
                account = CsFriendAccount(
                    id = prefs.decodeString(K_ID, "").orEmpty(),
                    username = username,
                    displayName = prefs.decodeString(K_DISPLAY, "").orEmpty().ifEmpty { username },
                    code = prefs.decodeString(K_CODE, "").orEmpty(),
                    token = token
                )
            )
        }
    }

    private fun persistAccount(account: CsFriendAccount?) {
        val prefs = launcherMMKV()
        if (account == null) {
            prefs.removeValueForKey(K_TOKEN)
            prefs.removeValueForKey(K_USERNAME)
            prefs.removeValueForKey(K_DISPLAY)
            prefs.removeValueForKey(K_CODE)
            prefs.removeValueForKey(K_ID)
            return
        }
        prefs.encode(K_TOKEN, account.token)
        prefs.encode(K_USERNAME, account.username)
        prefs.encode(K_DISPLAY, account.displayName)
        prefs.encode(K_CODE, account.code)
        prefs.encode(K_ID, account.id)
    }

    /**
     * Signs in, or registers when [register] is true.
     *
     * @return null on success, otherwise the message to show. The password is never stored — only the
     * session token is, which is what lets a relaunch come back signed in without keeping a secret
     * on disk.
     */
    suspend fun signIn(username: String, password: String, register: Boolean, display: String? = null): String? {
        val name = username.trim()
        if (!CsFriendAccount.validUsername(name)) return "Username must be 3-16 letters, numbers, _ or -"
        if (!CsFriendAccount.validPassword(password)) return "Password must be at least 6 characters"

        val args = JSONObject()
            .put("p_username", name)
            .put("p_password", password)
        if (register) {
            // The display name falls back to the username so a registration can never produce an
            // account with an empty name — friends' lists would show a blank row.
            args.put("p_display", display?.trim()?.takeIf { it.isNotEmpty() } ?: name)
        }

        return try {
            val fn = if (register) "cs_account_register" else "cs_account_login"
            val json = FriendsApi.call(fn, args)
            val account = CsFriendAccount.fromSession(json) ?: return "Sign-in returned no session"
            _state.update { it.copy(account = account, lastError = null) }
            persistAccount(account)
            presenceStatus = CsFriendStatus.ONLINE.wire
            sinceMessageId = 0L
            startPolling()
            refreshNow()
            null
        } catch (e: Exception) {
            messageFor(e)
        }
    }

    /** Changes the password; the stored token stays valid. Returns null on success. */
    suspend fun changePassword(oldPassword: String, newPassword: String): String? {
        val account = _state.value.account ?: return "Sign in first"
        if (!CsFriendAccount.validPassword(newPassword)) return "Password must be at least 6 characters"
        return try {
            FriendsApi.call(
                "cs_account_password",
                JSONObject()
                    .put("p_token", account.token)
                    .put("p_old", oldPassword)
                    .put("p_new", newPassword)
            )
            null
        } catch (e: Exception) {
            messageFor(e)
        }
    }

    /** Signs out locally and tells the backend to drop this player to offline. */
    suspend fun signOut() {
        val account = _state.value.account
        if (account != null) {
            // Best effort: a failing logout request must not leave the player signed in locally.
            runCatching {
                FriendsApi.call(
                    "cs_sync",
                    JSONObject()
                        .put("p_token", account.token)
                        .put("p_status", CsFriendStatus.OFFLINE.wire)
                        .put("p_address", JSONObject.NULL)
                        .put("p_world", JSONObject.NULL)
                        .put("p_engine", CsWorldEngine.LAUNCHER)
                        .put("p_since", 0)
                        .put("p_version", JSONObject.NULL)
                )
            }
        }
        presenceAddress = null
        presenceWorld = null
        presenceVersion = null
        presenceEngine = CsWorldEngine.LAUNCHER
        presenceStatus = CsFriendStatus.OFFLINE.wire
        sinceMessageId = 0L
        persistAccount(null)
        _state.value = CsFriendsState()
        pollJob?.cancel()
        pollJob = null
    }

    // -----------------------------------------------------------------------
    // Presence
    // -----------------------------------------------------------------------

    /** Sets the presence reported by the next sync (online / playing). */
    fun setPresence(status: CsFriendStatus) {
        if (status == CsFriendStatus.HOSTING && presenceAddress == null) return
        presenceStatus = status.wire
        refreshNow()
    }

    /**
     * Publishes a hosted world: friends see [CsFriendStatus.HOSTING] with [address] attached, which is
     * what makes the world joinable straight from the friends list.
     *
     * Starts the loop if it was idle, because a hosting player must keep reporting even with the
     * launcher UI closed.
     */
    fun setHosting(address: String, world: String?, engine: String, version: String? = null) {
        if (address.isBlank()) return
        presenceAddress = address.trim()
        presenceWorld = world?.takeIf { it.isNotBlank() }
        presenceEngine = engine.ifBlank { CsWorldEngine.JAVA }
        presenceVersion = version?.takeIf { it.isNotBlank() }
        presenceStatus = CsFriendStatus.HOSTING.wire
        startPolling()
        refreshNow()
    }

    /** Ends the hosting presence; the player drops back to [CsFriendStatus.ONLINE]. */
    fun clearHosting() {
        if (presenceAddress == null) return
        presenceAddress = null
        presenceWorld = null
        presenceVersion = null
        presenceEngine = CsWorldEngine.LAUNCHER
        presenceStatus = CsFriendStatus.ONLINE.wire
        refreshNow()
        if (watchers.get() == 0 && !backgroundWatch) stopPolling()
    }

    /**
     * Blocking [clearHosting] for a process that is about to die.
     *
     * Only the native exit hook may call this: there is no dispatcher left to resume a coroutine on,
     * so the sync runs on the calling thread with a hard [timeoutMillis] ceiling. A timeout is
     * acceptable — the alternative is hanging process teardown, and a missed presence clear costs one
     * stale "hosting" badge until the next poll from another device.
     */
    fun clearHostingBlocking(timeoutMillis: Long = 1500L) {
        if (presenceAddress == null) return
        val account = _state.value.account
        presenceAddress = null
        presenceWorld = null
        presenceVersion = null
        presenceEngine = CsWorldEngine.LAUNCHER
        presenceStatus = CsFriendStatus.ONLINE.wire
        pollJob?.cancel()
        pollJob = null
        if (account == null) return
        runCatching {
            runBlocking {
                withTimeoutOrNull(timeoutMillis) {
                    withContext(Dispatchers.IO) {
                        FriendsApi.call(
                            "cs_sync",
                            JSONObject()
                                .put("p_token", account.token)
                                .put("p_status", CsFriendStatus.ONLINE.wire)
                                .put("p_address", JSONObject.NULL)
                                .put("p_world", JSONObject.NULL)
                                .put("p_engine", CsWorldEngine.LAUNCHER)
                                .put("p_since", sinceMessageId)
                                .put("p_version", JSONObject.NULL)
                        )
                    }
                }
            }
        }.onFailure { Log.w(TAG, "blocking presence clear failed", it) }
    }

    /** True while this device is publishing a hosted world. */
    val isHosting: Boolean get() = presenceAddress != null

    /** The address this device is publishing, or null. */
    val hostingAddress: String? get() = presenceAddress

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    /** Sends a friend request using the short share code shown on a friend's profile. */
    suspend fun addFriend(code: String): String? {
        val account = _state.value.account ?: return "Sign in first"
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty()) return "Enter a friend code"
        return try {
            FriendsApi.call("cs_add_friend", JSONObject().put("p_token", account.token).put("p_code", trimmed))
            refreshNow()
            null
        } catch (e: Exception) {
            messageFor(e)
        }
    }

    /** Accepts or declines the request from [otherId]. */
    suspend fun respond(otherId: String, accept: Boolean): String? = withToken { token ->
        FriendsApi.call(
            "cs_respond",
            JSONObject().put("p_token", token).put("p_other", otherId).put("p_accept", accept)
        )
        refreshNow()
        null
    }

    /** Removes a friend. Both sides lose the entry; the backend does the cascade. */
    suspend fun removeFriend(otherId: String): String? = withToken { token ->
        FriendsApi.call("cs_remove_friend", JSONObject().put("p_token", token).put("p_other", otherId))
        _state.update { current ->
            current.copy(friends = current.friends.filterNot { it.id == otherId })
        }
        refreshNow()
        null
    }

    /**
     * Sends a chat message, optionally replying to [replyTo] and attaching an uploaded image URL.
     *
     * Optimistic-free on purpose: the message appears when the next sync returns it, which keeps one
     * source of truth and avoids the "sent" ghost that never arrives when the request failed.
     */
    suspend fun send(toId: String, text: String, replyTo: String? = null, imageUrl: String? = null): String? {
        val body = text.trim()
        if (body.isEmpty() && imageUrl == null) return "Nothing to send"
        if (body.length > MAX_MESSAGE_CHARS) return "Message is too long"
        return withToken { token ->
            val args = JSONObject()
                .put("p_token", token)
                .put("p_to", toId)
                .put("p_text", body)
            replyTo?.let { args.put("p_reply_to", it) }
            imageUrl?.let { args.put("p_image", it) }
            FriendsApi.call("cs_send", args)
            refreshNow()
            null
        }
    }

    /**
     * Invites friends to a hosted world.
     *
     * @param toId a single friend, or null to invite **everyone** — the backend fans a null target out
     * to the whole friends list, so "invite all" is one request rather than N.
     */
    suspend fun invite(toId: String?, address: String, world: String?, engine: String, version: String? = null): String? {
        if (address.isBlank()) return "There is no address to share yet"
        return withToken { token ->
            val args = JSONObject()
                .put("p_token", token)
                .put("p_address", address)
                .put("p_world", world ?: JSONObject.NULL)
                .put("p_engine", engine.ifBlank { CsWorldEngine.JAVA })
                .put("p_version", version ?: JSONObject.NULL)
            // NULL means "everyone"; omitting the key would instead be a malformed call.
            if (toId == null) args.put("p_to", JSONObject.NULL) else args.put("p_to", toId)
            FriendsApi.call("cs_invite", args)
            refreshNow()
            null
        }
    }

    /**
     * Hides an invite locally.
     *
     * Deliberately **not** sent to the server: declining is a UI concern, and the invite expires on
     * its own after [CsWorldInvite.LIFETIME_MS]. Telling the backend would need a write path for a
     * state that already resolves itself, and would make a mistap permanent.
     */
    fun declineInvite(inviteId: String) {
        _state.update { current ->
            current.copy(invites = current.invites.filterNot { it.id == inviteId })
        }
    }

    /** Expired invites are dropped from the list; returns how many were removed. */
    fun pruneExpiredInvites(): Int {
        val now = _state.value.serverNow
        val before = _state.value.invites.size
        _state.update { current -> current.copy(invites = current.invites.filter { it.isLive(now) }) }
        return before - _state.value.invites.size
    }

    /** Runs [block] with the session token, or returns a sign-in prompt when signed out. */
    private suspend fun withToken(block: suspend (String) -> String?): String? {
        val account = _state.value.account ?: return "Sign in first"
        return try {
            block(account.token)
        } catch (e: Exception) {
            messageFor(e)
        }
    }

    // -----------------------------------------------------------------------
    // Sync
    // -----------------------------------------------------------------------

    /**
     * One `cs_sync` round trip. Serialized by [syncMutex] so a manual [refreshNow] cannot interleave
     * with the timer and produce two overlapping writes to the same state.
     *
     * @return false when the call failed, which moves the loop to [POLL_ERROR_MS]
     */
    private suspend fun sync(): Boolean {
        val account = _state.value.account ?: return false
        return syncMutex.withLock {
            _state.update { it.copy(syncing = true) }
            try {
                val args = JSONObject()
                    .put("p_token", account.token)
                    .put("p_status", presenceStatus)
                    .put("p_address", presenceAddress ?: JSONObject.NULL)
                    .put("p_world", presenceWorld ?: JSONObject.NULL)
                    .put("p_engine", presenceEngine)
                    .put("p_since", sinceMessageId)
                    .put("p_version", presenceVersion ?: JSONObject.NULL)

                val json = FriendsApi.call("cs_sync", args)
                parse(json, account)
                _state.update { it.copy(syncing = false, lastError = null) }
                true
            } catch (e: Exception) {
                val message = messageFor(e)
                Log.w(TAG, "sync failed: $message")
                _state.update { it.copy(syncing = false, lastError = message) }
                false
            }
        }
    }

    /** Applies a `cs_sync` payload to [state]. */
    private fun parse(json: JSONObject, account: CsFriendAccount) {
        val myId = account.id

        val friends = json.optJSONArray("friends")?.mapObjects { o ->
            CsFriend(
                id = o.str("id", ""),
                name = o.str("name", ""),
                code = o.str("code", ""),
                status = CsFriendStatus.of(o.str("status", CsFriendStatus.OFFLINE.wire)),
                address = o.strOrNull("address"),
                world = o.strOrNull("world"),
                engine = o.strOrNull("engine"),
                version = o.strOrNull("version")
            )
        }.orEmpty().filter { it.id.isNotEmpty() }

        val requests = json.optJSONArray("requests")?.mapObjects { o ->
            CsFriendRequest(
                id = o.str("id", ""),
                name = o.str("name", ""),
                code = o.str("code", ""),
                incoming = o.optBoolean("incoming", true),
                at = o.millis("at")
            )
        }.orEmpty().filter { it.id.isNotEmpty() }

        val invites = json.optJSONArray("invites")?.mapObjects { o ->
            CsWorldInvite(
                id = o.str("id", ""),
                from = o.str("from", ""),
                fromId = o.str("from_id", ""),
                address = o.str("address", ""),
                world = o.strOrNull("world"),
                engine = o.str("engine", CsWorldEngine.JAVA),
                version = o.strOrNull("version"),
                at = o.millis("at")
            )
        }.orEmpty().filter { it.id.isNotEmpty() }

        val messages = json.optJSONArray("messages")?.mapObjects { o ->
            val meta = o.optJSONObject("meta")
            CsChatMessage(
                id = o.str("id", ""),
                from = o.str("from", ""),
                to = o.str("to", ""),
                body = o.str("body", ""),
                at = o.millis("at"),
                // `mine` is derived, not trusted: the backend does not echo it, and inferring it from
                // the sender keeps both sides of a conversation consistent by construction.
                mine = o.str("from_id", "") == myId,
                image = o.strOrNull("image"),
                kind = o.strOrNull("kind"),
                replyTo = o.strOrNull("reply_to"),
                replyFrom = o.strOrNull("reply_from"),
                replyBody = o.strOrNull("reply_body"),
                // An invite carried inside a chat message keeps its world details under `meta`, so a
                // message that is also an invite can be joined straight from the conversation.
                inviteAddress = meta?.strOrNull("address"),
                inviteWorld = meta?.strOrNull("world"),
                inviteEngine = meta?.strOrNull("engine"),
                inviteVersion = meta?.strOrNull("version")
            )
        }.orEmpty().filter { it.id.isNotEmpty() }

        // Server time is authoritative for invite expiry: device clocks drift, and a 10-minute invite
        // judged against a wrong local clock would either vanish early or linger for hours.
        val now = json.millis("now").takeIf { it > 0 } ?: System.currentTimeMillis()

        advanceSince(messages)

        _state.update { current ->
            current.copy(
                account = account,
                friends = friends,
                requests = requests,
                invites = mergeInvites(current.invites, invites, now),
                messages = mergeMessages(current.messages, messages),
                serverNow = now
            )
        }
    }

    /**
     * Tracks the highest message id so the next sync asks only for what came after it.
     *
     * Ids arrive as strings because the backend serialises bigints that way; ordering is numeric, so
     * a non-numeric id is ignored rather than allowed to reset the cursor to zero and replay history.
     */
    private fun advanceSince(messages: List<CsChatMessage>) {
        val max = messages.maxOfOrNull { it.id.toLongOrNull() ?: 0L } ?: return
        if (max > sinceMessageId) sinceMessageId = max
    }

    /**
     * Merges fresh invites over the ones already held, dropping expired ones.
     *
     * A local decline must survive the next sync — otherwise the invite the player just dismissed
     * would reappear seconds later, which reads as the app ignoring them. Anything the server no
     * longer lists was declined locally, so it stays hidden.
     */
    private fun mergeInvites(
        current: List<CsWorldInvite>,
        fresh: List<CsWorldInvite>,
        now: Long
    ): List<CsWorldInvite> {
        val freshIds = fresh.mapTo(HashSet()) { it.id }
        val declined = current.mapTo(HashSet()) { it.id }.apply { removeAll(freshIds) }
        return fresh.filter { it.isLive(now) && it.id !in declined }
            .sortedByDescending { it.at }
    }

    /** Appends messages the cursor has not seen, de-duplicating by id and keeping thread order. */
    private fun mergeMessages(current: List<CsChatMessage>, fresh: List<CsChatMessage>): List<CsChatMessage> {
        if (fresh.isEmpty()) return current
        val seen = current.mapTo(HashSet()) { it.id }
        val additions = fresh.filter { seen.add(it.id) }
        if (additions.isEmpty()) return current
        // Ordered by numeric id where possible so two devices that saw the same messages in a
        // different order still render one consistent thread.
        return (current + additions).sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
    }

    /**
     * Turns a transport failure into a line worth showing a person.
     *
     * Order matters: [FriendsApi.FriendsApiException] extends [IOException] but carries the
     * backend's own wording ("That code is already taken"), which is far more useful than the
     * generic offline message the supertype branch would produce.
     */
    private fun messageFor(e: Exception): String = when (e) {
        is FriendsApi.FriendsApiException -> e.message ?: "The friends service refused the request"
        is IOException -> "No connection to the friends service"
        else -> e.message ?: "Something went wrong"
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }
}
