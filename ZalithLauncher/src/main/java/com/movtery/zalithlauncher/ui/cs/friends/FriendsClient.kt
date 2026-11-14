/*
 * CS Launcher Lite — CS Friends client
 * Ported from JoooaoooDVD/CSL2 (app_cs/src/main/java/com/craftstudio/cslauncher/friends/FriendsClient.java)
 */

package com.movtery.zalithlauncher.ui.cs.friends

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * The launcher's live link to the CS friends backend: periodic {@code cs_sync} (friends, requests,
 * invites, messages), presence (online / hosting with address), and the actions of the Friends
 * page. One instance per process; the game process uses it only to publish "hosting".
 * Listeners are called on the main thread.
 */
object FriendsClient {
    private const val TAG = "FriendsClient"
    private const val POLL_FAST = 3000L
    private const val POLL_NORMAL = 8000L
    private const val POLL_SLOW = 20000L
    private const val K_SEEN = "cs.friends.seen"
    private const val K_INBOX = "cs.friends.inbox"

    data class Friend(
        val id: String = "",
        val name: String = "",
        val code: String = "",
        val status: String = "offline",
        val address: String? = null,
        val world: String? = null,
        val engine: String? = null,
        val version: String? = null
    ) {
        fun online(): Boolean = status != "offline" && status.isNotEmpty()
        fun hosting(): Boolean = address != null && address.isNotEmpty()
    }

    data class Request(
        val id: String = "",
        val name: String = "",
        val code: String = "",
        val incoming: Boolean = false,
        val at: Long = 0
    )

    data class Invite(
        val id: String = "",
        val from: String = "",
        val fromId: String = "",
        val address: String = "",
        val world: String? = null,
        val engine: String = "java",
        val version: String? = null,
        val at: Long = 0
    )

    data class Message(
        val id: String = "",
        val from: String = "",
        val to: String = "",
        val body: String = "",
        val at: Long = 0,
        val mine: Boolean = false,
        val image: String? = null,
        val replyTo: String? = null,
        val replyFrom: String? = null,
        val replyBody: String? = null,
        val kind: String? = null,
        val inviteAddress: String? = null,
        val inviteWorld: String? = null,
        val inviteEngine: String? = null,
        val inviteVersion: String? = null
    ) {
        fun hasText(): Boolean = body.isNotEmpty()
        fun isInvite(): Boolean = "invite" == kind && inviteAddress != null && inviteAddress.isNotEmpty()
        fun inviteLive(now: Long): Boolean = isInvite() && now - at < 10L * 60_000L
    }

    /** Immutable snapshot of the last successful sync. */
    data class Snapshot(
        val friends: List<Friend> = emptyList(),
        val requests: List<Request> = emptyList(),
        val invites: List<Invite> = emptyList(),
        val messages: List<Message> = emptyList(),
        val now: Long = 0
    ) {
        fun friend(id: String): Friend? = friends.find { it.id == id }
        fun incomingRequests(): Int = requests.count { it.incoming }
    }

    /** One line of the bell inbox (persisted, newest first). */
    data class Note(
        val kind: Int,
        val key: String,
        val title: String,
        val body: String,
        val fromId: String? = null,
        val address: String? = null,
        val engine: String? = null,
        val world: String? = null,
        val version: String? = null,
        val at: Long = 0,
        var read: Boolean = false
    ) {
        companion object {
            const val INVITE = 0
            const val REQUEST = 1
            const val MESSAGE = 2
            const val INFO = 3
            const val HOSTING = 4
        }
    }

    interface Listener {
        fun onFriendsChanged(snapshot: Snapshot)
        fun onNote(note: Note) {}
        fun onFriendsError(message: String) {}
    }

    interface Result {
        fun done(@Nullable error: String?)
    }

    private val mMain = Handler(Looper.getMainLooper())
    private val mIo = Executors.newSingleThreadExecutor { r ->
        Thread(r, "cs-friends").apply { isDaemon = true }
    }
    private val mListeners = CopyOnWriteArrayList<Listener>()
    private val mSeen = HashSet<String>()
    private val mInbox = ArrayList<Note>()
    private val mHistory = ArrayList<Message>()
    @Volatile private var mSnapshot = Snapshot()
    @Volatile private var mStatus = "online"
    @Volatile private var mAddress: String? = null
    @Volatile private var mWorld: String? = null
    @Volatile private var mEngine: String? = null
    @Volatile private var mVersion: String? = null
    @Volatile private var mRunning = false
    @Volatile private var mFast = false
    @Volatile private var mLoaded = false
    @Volatile private var mBackground = false
    private var mWatchers = 0
    private var mSince: Long = 0
    @Volatile private var mLastError: String? = null
    private val mDeclined = HashSet<String>()
    private val mTick = Runnable { tick() }

    fun addListener(@NonNull l: Listener) {
        load()
        if (mListeners.add(l)) {
            mWatchers++
            ensureRunning()
        }
    }

    fun removeListener(@NonNull l: Listener) {
        if (mListeners.remove(l)) {
            mWatchers = maxOf(0, mWatchers - 1)
            if (mWatchers == 0 && mAddress == null && !mBackground) stopPolling()
        }
    }

    fun addBackgroundListener(@NonNull l: Listener) {
        load()
        mListeners.add(l)
        mBackground = true
        ensureRunning()
    }

    fun removeBackgroundListener(@NonNull l: Listener) {
        mListeners.remove(l)
        mBackground = false
        if (mWatchers == 0 && mAddress == null) stopPolling()
    }

    fun setFast(fast: Boolean) {
        mFast = fast
        if (fast) {
            mMain.removeCallbacks(mTick)
            mMain.post(mTick)
        }
    }

    fun refreshNow() {
        if (!CsAccount.isSignedIn()) return
        mMain.removeCallbacks(mTick)
        mMain.post(mTick)
    }

    private fun ensureRunning() {
        if (mRunning || !CsAccount.isSignedIn()) return
        mRunning = true
        mMain.removeCallbacks(mTick)
        mMain.post(mTick)
    }

    private fun stopPolling() {
        mRunning = false
        mMain.removeCallbacks(mTick)
    }

    fun onAccountChanged() {
        mSince = 0
        mSnapshot = Snapshot()
        synchronized(mHistory) { mHistory.clear() }
        mLastError = null
        if (CsAccount.isSignedIn()) {
            mRunning = false
            ensureRunning()
        } else {
            stopPolling()
        }
        notifySnapshot()
    }

    fun setHosting(@NonNull address: String, @Nullable world: String?, @NonNull engine: String) {
        setHosting(address, world, engine, null)
    }

    fun setHosting(@NonNull address: String, @Nullable world: String?, @NonNull engine: String, @Nullable version: String?) {
        mVersion = version
        mStatus = "hosting"
        mAddress = address
        mWorld = world
        mEngine = engine
        ensureRunning()
        refreshNow()
    }

    fun clearHosting() {
        val was = mAddress != null
        mStatus = "online"
        mAddress = null
        mWorld = null
        mEngine = null
        mVersion = null
        if (was) {
            refreshNow()
            if (mWatchers == 0 && !mBackground) {
                mMain.postDelayed({ stopPolling() }, 4000L)
            }
        }
    }

    fun clearHostingBlocking() {
        if (mAddress == null) return
        mAddress = null
        mWorld = null
        mStatus = "online"
        val token = CsAccount.token()
        if (token.isEmpty()) return
        Thread {
            try { FriendsApi.call("cs_sync", syncArgs(token)) } catch (_: IOException) {}
        }.apply { start(); join(3000L) }
    }

    private fun syncArgs(token: String): JSONObject {
        val a = JSONObject()
        try {
            a.put("p_token", token)
            a.put("p_status", mStatus)
            a.put("p_address", if (mAddress == null) JSONObject.NULL else mAddress)
            a.put("p_world", if (mWorld == null) JSONObject.NULL else mWorld)
            a.put("p_engine", if (mEngine == null) "launcher" else mEngine)
            a.put("p_since", mSince)
            a.put("p_version", if (mVersion == null) JSONObject.NULL else mVersion)
        } catch (_: JSONException) {}
        return a
    }

    private fun tick() {
        if (!mRunning) return
        val token = CsAccount.token()
        if (token.isEmpty()) { stopPolling(); return }
        mIo.execute {
            var snap: Snapshot? = null
            var error: String? = null
            try {
                val r = FriendsApi.call("cs_sync", syncArgs(token))
                snap = parse(r)
            } catch (e: IOException) {
                error = e.message
            }
            val fs = snap
            val fe = error
            mMain.post {
                if (fs != null) {
                    mLastError = null
                    apply(fs)
                } else {
                    mLastError = fe
                    for (l in mListeners) l.onFriendsError(fe ?: "Offline")
                }
                if (mRunning) {
                    val delay = if (fs == null) POLL_SLOW else if (mFast) POLL_FAST else if (mWatchers > 0) POLL_NORMAL else POLL_SLOW
                    mMain.removeCallbacks(mTick)
                    mMain.postDelayed(mTick, delay)
                }
            }
        }
    }

    private fun parse(r: JSONObject): Snapshot {
        val me = r.optJSONObject("me")
        if (me != null) CsAccount.updateMe(me)
        val myId = CsAccount.id()

        val friends = ArrayList<Friend>()
        val a = r.optJSONArray("friends")
        if (a != null) {
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                friends.add(Friend(
                    id = o.optString("id", ""),
                    name = o.optString("name", ""),
                    code = o.optString("code", ""),
                    status = o.optString("status", "offline"),
                    address = if (o.isNull("address")) null else o.optString("address", null),
                    world = if (o.isNull("world")) null else o.optString("world", null),
                    engine = if (o.isNull("engine")) null else o.optString("engine", null),
                    version = if (o.isNull("version")) null else o.optString("version", null)
                ))
            }
        }

        val requests = ArrayList<Request>()
        val ra = r.optJSONArray("requests")
        if (ra != null) {
            for (i in 0 until ra.length()) {
                val o = ra.optJSONObject(i) ?: continue
                requests.add(Request(
                    id = o.optString("id", ""),
                    name = o.optString("name", ""),
                    code = o.optString("code", ""),
                    incoming = o.optBoolean("incoming", false),
                    at = when(o.opt("at")) is Number -> (o.opt("at") as Number).toLong() else 0
                ))
            }
        }

        val invites = ArrayList<Invite>()
        val ia = r.optJSONArray("invites")
        if (ia != null) {
            for (i in 0 until ia.length()) {
                val o = ia.optJSONObject(i) ?: continue
                invites.add(Invite(
                    id = o.optString("id", ""),
                    from = o.optString("from", ""),
                    fromId = o.optString("from_id", ""),
                    address = o.optString("address", ""),
                    world = if (o.isNull("world")) null else o.optString("world", null),
                    engine = o.optString("engine", "java"),
                    version = if (o.isNull("version")) null else o.optString("version", null),
                    at = when(o.opt("at")) is Number -> (o.opt("at") as Number).toLong() else 0
                ))
            }
        }

        val messages = ArrayList<Message>()
        val ma = r.optJSONArray("messages")
        if (ma != null) {
            for (i in 0 until ma.length()) {
                val o = ma.optJSONObject(i) ?: continue
                val mine = o.optString("id", "") == myId
                val meta = o.optJSONObject("meta")
                messages.add(Message(
                    id = o.optString("id", ""),
                    from = o.optString("from", ""),
                    to = o.optString("to", ""),
                    body = o.optString("body", ""),
                    at = when(o.opt("at")) is Number -> (o.opt("at") as Number).toLong() else 0,
                    mine = mine,
                    image = if (o.isNull("image")) null else o.optString("image", null),
                    replyTo = if (o.isNull("reply_to")) null else o.optLong("reply_to").toString(),
                    replyFrom = if (o.isNull("reply_from")) null else o.optString("reply_from", null),
                    replyBody = if (o.isNull("reply_body")) null else o.optString("reply_body", null),
                    kind = if (o.isNull("kind")) null else o.optString("kind", null),
                    inviteAddress = if (meta != null && !meta.isNull("address")) meta.optString("address", null) else null,
                    inviteWorld = if (meta != null && !meta.isNull("world")) meta.optString("world", null) else null,
                    inviteEngine = if (meta != null && !meta.isNull("engine")) meta.optString("engine", "java") else null,
                    inviteVersion = if (meta != null && !meta.isNull("version")) meta.optString("version", null) else null
                ))
            }
        }

        val now = when(r.opt("now")) is Number -> (r.opt("now") as Number).toLong() else System.currentTimeMillis()
        return Snapshot(friends = friends, requests = requests, invites = invites, messages = messages, now = now)
    }

    private fun apply(s: Snapshot) {
        val merged = Snapshot(
            friends = s.friends.toMutableList(),
            requests = s.requests.toMutableList(),
            invites = s.invites.toMutableList(),
            messages = s.messages.toMutableList(),
            now = s.now
        )

        synchronized(mHistory) {
            for (m in s.messages) {
                if (mHistory.none { it.id == m.id }) mHistory.add(m)
                try {
                    val id = m.id.toLong()
                    if (id > mSince) mSince = id
                } catch (_: NumberFormatException) {}
            }
            while (mHistory.size > 600) mHistory.removeAt(0)
            merged.messages = mHistory.toList()
        }

        val before = mSnapshot
        mSnapshot = merged

        val inboxChanged = mutableBoolean(false)

        for (f in s.friends) {
            if (!f.hosting() || !JoinWorld.isKnownHost(f.id)) continue
            val was = before.friend(f.id)
            if (was != null && f.address == was.address) continue
            val key = "host:${f.id}:${f.address}"
            if (mSeen.add(key)) {
                val n = Note(
                    kind = Note.HOSTING,
                    key = key,
                    title = f.name,
                    body = if (f.world.isNullOrBlank()) "is hosting a world you played in before — you can join without an invite"
                    else "is hosting \"${f.world}\" again — you can join without an invite",
                    fromId = f.id,
                    address = f.address,
                    engine = f.engine,
                    world = f.world,
                    version = f.version,
                    at = System.currentTimeMillis()
                )
                push(n); inboxChanged.value = true
            }
        }

        for (v in s.invites) {
            val key = "inv:${v.id}"
            if (mSeen.add(key)) {
                val n = Note(
                    kind = Note.INVITE,
                    key = key,
                    title = v.from,
                    body = if (v.world.isNullOrBlank()) "invited you to play" else "invited you to \"${v.world}\"",
                    fromId = v.fromId,
                    address = v.address,
                    engine = v.engine,
                    world = v.world,
                    version = v.version,
                    at = v.at
                )
                push(n); inboxChanged.value = true
            }
        }

        for (q in s.requests) {
            if (!q.incoming) continue
            val key = "req:${q.id}"
            if (mSeen.add(key)) {
                val n = Note(
                    kind = Note.REQUEST,
                    key = key,
                    title = q.name,
                    body = "sent you a friend request",
                    fromId = q.id,
                    at = q.at
                )
                push(n); inboxChanged.value = true
            }
        }

        for (m in s.messages) {
            if (m.mine || m.isInvite()) continue
            val key = "msg:${m.id}"
            if (mSeen.add(key)) {
                val f = merged.friend(m.from)
                val n = Note(
                    kind = Note.MESSAGE,
                    key = key,
                    title = f?.name ?: "Friend",
                    body = if (m.hasText()) m.body else if (m.image != null) "sent a photo" else "",
                    fromId = m.from,
                    at = m.at
                )
                push(n); inboxChanged.value = true
            }
        }

        val live = HashSet<String>()
        for (v in s.invites) live.add("inv:${v.id}")
        for (q in s.requests) if (q.incoming) live.add("req:${q.id}")
        for (f in s.friends) if (f.hosting()) live.add("host:${f.id}:${f.address}")

        synchronized(mInbox) {
            for (i in mInbox.size - 1 downTo 0) {
                val n = mInbox[i]
                if ((n.kind == Note.INVITE || n.kind == Note.REQUEST || n.kind == Note.HOSTING) && !live.contains(n.key)) {
                    mInbox.removeAt(i)
                    inboxChanged.value = true
                }
            }
        }

        if (inboxChanged.value) save()
        notifySnapshot()
    }

    private class MutableBoolean(var value: Boolean)

    private fun push(n: Note) {
        synchronized(mInbox) {
            mInbox.add(0, n)
            while (mInbox.size > 80) mInbox.removeAt(mInbox.size - 1)
        }
        for (l in mListeners) l.onNote(n)
    }

    private fun notifySnapshot() {
        val s = mSnapshot
        for (l in mListeners) {
            try { l.onFriendsChanged(s) } catch (e: RuntimeException) { Log.w(TAG, "listener", e) }
        }
    }

    fun inbox(): List<Note> = synchronized(mInbox) { mInbox.toList() }

    fun unread(): Int = synchronized(mInbox) { mInbox.count { !it.read } }

    fun markAllRead() {
        synchronized(mInbox) { for (x in mInbox) x.read = true }
        save()
        notifySnapshot()
    }

    fun markRead(key: String) {
        synchronized(mInbox) { for (x in mInbox) if (x.key == key) x.read = true }
        save()
    }

    fun dismiss(key: String) {
        synchronized(mInbox) {
            for (i in mInbox.size - 1 downTo 0) if (mInbox[i].key == key) mInbox.removeAt(i)
        }
        save()
        notifySnapshot()
    }

    fun conversation(@NonNull friendId: String): List<Message> {
        val out = ArrayList<Message>()
        synchronized(mHistory) {
            for (m in mHistory) if (m.from == friendId || m.to == friendId) out.add(m)
        }
        out.sortBy { it.at }
        return out
    }

    fun unreadMessagesFrom(@NonNull friendId: String): Int {
        return synchronized(mInbox) {
            mInbox.count { x -> x.kind == Note.MESSAGE && !x.read && friendId == x.fromId }
        }
    }

    fun markConversationRead(@NonNull friendId: String) {
        synchronized(mInbox) { for (x in mInbox) if (x.kind == Note.MESSAGE && friendId == x.fromId) x.read = true }
        save()
        notifySnapshot()
    }

    private fun load() {
        if (mLoaded) return
        mLoaded = true
        try {
            val seenJson = AllSettings.friendsSeenKeys.state
            if (seenJson.isNotEmpty()) {
                try {
                    val arr = JSONArray(seenJson)
                    for (i in 0 until arr.length()) mSeen.add(arr.getString(i))
                } catch (_: JSONException) {}
            }
            val inboxJson = AllSettings.friendsInbox.state
            if (inboxJson.isNotEmpty()) {
                try {
                    val arr = JSONArray(inboxJson)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val n = Note(
                            kind = o.optInt("kind", 0),
                            key = o.optString("key", ""),
                            title = o.optString("title", ""),
                            body = o.optString("body", ""),
                            fromId = o.optString("fromId", null)?.takeIf { it.isNotEmpty() },
                            address = o.optString("address", null)?.takeIf { it.isNotEmpty() },
                            engine = o.optString("engine", null)?.takeIf { it.isNotEmpty() },
                            world = o.optString("world", null)?.takeIf { it.isNotEmpty() },
                            at = o.optLong("at", 0),
                            read = o.optBoolean("read", false)
                        )
                        mInbox.add(n)
                    }
                } catch (_: JSONException) {}
            }
        } catch (_: RuntimeException) {}
    }

    private fun save() {
        try {
            val keys = synchronized(mSeen) { mSeen.toList() }
            val from = maxOf(0, keys.size - 400)
            val seenArr = JSONArray()
            for (i in from until keys.size) seenArr.put(keys[i])

            val inboxArr = JSONArray()
            synchronized(mInbox) {
                for (n in mInbox) {
                    val o = JSONObject()
                    o.put("kind", n.kind)
                    o.put("key", n.key)
                    o.put("title", n.title)
                    o.put("body", n.body)
                    o.put("at", n.at)
                    o.put("read", n.read)
                    n.fromId?.let { o.put("fromId", it) }
                    n.address?.let { o.put("address", it) }
                    n.engine?.let { o.put("engine", it) }
                    n.world?.let { o.put("world", it) }
                    inboxArr.put(o)
                }
            }
            AllSettings.friendsSeenKeys.save(keys.subList(from, keys.size))
            val inboxList = ArrayList<String>()
            synchronized(mInbox) {
                for (n in mInbox) {
                    val o = JSONObject()
                    o.put("kind", n.kind)
                    o.put("key", n.key)
                    o.put("title", n.title)
                    o.put("body", n.body)
                    o.put("at", n.at)
                    o.put("read", n.read)
                    n.fromId?.let { o.put("fromId", it) }
                    n.address?.let { o.put("address", it) }
                    n.engine?.let { o.put("engine", it) }
                    n.world?.let { o.put("world", it) }
                    inboxList.add(o.toString())
                }
            }
            AllSettings.friendsInbox.save(inboxList)
        } catch (_: JSONException) {}
    }

    fun addFriend(@NonNull code: String, @Nullable cb: Result? = null) {
        rpc("cs_add_friend", JSONObject().put("p_code", code.trim().uppercase()), cb)
    }

    fun respond(@NonNull otherId: String, accept: Boolean, @Nullable cb: Result? = null) {
        val a = JSONObject().put("p_other", otherId).put("p_accept", accept)
        rpc("cs_respond", a, cb)
    }

    fun removeFriend(@NonNull otherId: String, @Nullable cb: Result? = null) {
        rpc("cs_remove_friend", JSONObject().put("p_other", otherId), cb)
    }

    fun invite(@Nullable toId: String?, @NonNull address: String, @Nullable world: String?, @NonNull engine: String, @Nullable cb: Result? = null) {
        val a = JSONObject().apply {
            put("p_to", if (toId == null) JSONObject.NULL else toId)
            put("p_address", address)
            put("p_world", if (world == null) JSONObject.NULL else world)
            put("p_engine", engine)
            put("p_version", if (mVersion == null) JSONObject.NULL else mVersion)
        }
        rpc("cs_invite", a, cb)
    }

    fun declineInvite(@NonNull v: Invite) {
        dismiss("inv:${v.id}")
        mDeclined.add(v.id)
        notifySnapshot()
    }

    fun openInvites(): List<Invite> = mSnapshot.invites.filter { !mDeclined.contains(it.id) }

    fun send(@NonNull toId: String, @NonNull text: String, @Nullable cb: Result? = null) {
        send(toId, text, null, null, cb)
    }

    fun send(@NonNull toId: String, @NonNull text: String, @Nullable replyToId: String?, @Nullable imageUrl: String?, @Nullable cb: Result? = null) {
        val a = JSONObject().put("p_to", toId).put("p_text", text)
        try {
            if (replyToId != null) a.put("p_reply_to", replyToId.toLong())
            if (imageUrl != null) a.put("p_image", imageUrl)
        } catch (_: JSONException) {}
        rpc("cs_send", a, cb)
    }

    private fun rpc(@NonNull fn: String, @NonNull args: JSONObject, @Nullable cb: Result?) {
        val token = CsAccount.token()
        if (token.isEmpty()) { cb?.done("Sign in to your CS account first"); return }
        try { args.put("p_token", token) } catch (_: JSONException) {}
        mIo.execute {
            var err: String? = null
            try { FriendsApi.call(fn, args) } catch (e: IOException) { err = e.message }
            val fe = err
            mMain.post {
                cb?.done(fe)
                if (fe == null) refreshNow()
            }
        }
    }

    fun account(register: Boolean, @NonNull username: String, @NonNull password, @Nullable display: String?, @NonNull cb: Result) {
        mIo.execute {
            var err: String? = null
            try {
                val a = JSONObject().apply {
                    put("p_username", username.trim())
                    put("p_password", password)
                    if (register) a.put("p_display", if (display.isNullOrBlank()) username.trim() else display.trim())
                }
                val r = FriendsApi.call(if (register) "cs_account_register" else "cs_account_login", a)
                if (!r.has("token")) throw IOException("Unexpected reply from the account service")
                CsAccount.store(r)
            } catch (e: IOException | JSONException) { err = e.message }
            val fe = err
            mMain.post { cb?.done(fe) }
        }
    }
}
