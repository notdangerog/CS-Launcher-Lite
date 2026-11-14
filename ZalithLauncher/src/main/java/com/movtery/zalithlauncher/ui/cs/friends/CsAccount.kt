/*
 * CS Launcher Lite — CS Friends client
 * Ported from JoooaoooDVD/CSL2 (app_cs/src/main/java/com/craftstudio/cslauncher/friends/CsAccount.java)
 */

package com.movtery.zalithlauncher.ui.cs.friends

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import org.json.JSONObject
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * The launcher's CS account (username + password, Supabase). Separate from the engine's own
 * friend identity: this one unlocks the launcher Friends page, notifications and world sharing.
 * Only the stable friend token is kept on the device, never the password.
 */
object CsAccount {
    private fun put(key: String, value: String) {
        when (key) {
            "cs.friends.token" -> AllSettings.friendsToken.save(value)
            "cs.friends.username" -> AllSettings.friendsUsername.save(value)
            "cs.friends.name" -> AllSettings.friendsDisplayName.save(value)
            "cs.friends.code" -> AllSettings.friendsCode.save(value)
            "cs.friends.id" -> AllSettings.friendsId.save(value)
            else -> throw IllegalArgumentException("Unknown friends key: $key")
        }
    }

    private fun get(key: String): String {
        return when (key) {
            "cs.friends.token" -> AllSettings.friendsToken.state
            "cs.friends.username" -> AllSettings.friendsUsername.state
            "cs.friends.name" -> AllSettings.friendsDisplayName.state
            "cs.friends.code" -> AllSettings.friendsCode.state
            "cs.friends.id" -> AllSettings.friendsId.state
            else -> throw IllegalArgumentException("Unknown friends key: $key")
        }
    }

    fun isSignedIn(): Boolean {
        val t = token()
        return t.isNotEmpty() && t.length >= 16
    }

    @JvmStatic
    fun token(): String = get("cs.friends.token")

    @JvmStatic
    fun username(): String = get("cs.friends.username")

    @JvmStatic
    fun displayName(): String = get("cs.friends.name")

    @JvmStatic
    fun code(): String = get("cs.friends.code")

    @JvmStatic
    fun id(): String = get("cs.friends.id")

    /** Stores the session returned by cs_account_login / cs_account_register. */
    fun store(@NonNull session: JSONObject) {
        put("cs.friends.token", session.optString("token", ""))
        put("cs.friends.username", session.optString("username", ""))
        put("cs.friends.name", session.optString("name", ""))
        put("cs.friends.code", session.optString("code", ""))
        put("cs.friends.id", session.optString("id", ""))
    }

    fun updateMe(@Nullable me: JSONObject?) {
        if (me == null) return
        put("cs.friends.name", me.optString("name", displayName()))
        put("cs.friends.code", me.optString("code", code()))
        put("cs.friends.id", me.optString("id", id()))
    }

    fun signOut() {
        put("cs.friends.token", "")
        put("cs.friends.username", "")
        put("cs.friends.name", "")
        put("cs.friends.code", "")
        put("cs.friends.id", "")
    }

    /** Username rule shared with the backend. */
    fun validUsername(@Nullable u: String?): Boolean {
        return u != null && u.matches(Regex("^[A-Za-z0-9_]{3,16}$"))
    }

    /** Password rule shared with the backend. */
    fun validPassword(@Nullable p: String?): Boolean {
        return p != null && p.length in 6..72
    }
}
