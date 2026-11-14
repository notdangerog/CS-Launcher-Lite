/*
 * CS Launcher Lite — CS Friends client
 * Ported from JoooaoooDVD/CSL2 (app_cs/src/main/java/com/craftstudio/cslauncher/friends/JoinWorld.java)
 */

package com.movtery.zalithlauncher.ui.cs.friends

import android.app.Activity
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import java.util.HashSet

/**
 * "Join World" from an invite: picks a profile of the right engine, remembers the address for the
 * launch that follows and presses PLAY.
 */
object JoinWorld {
    private const val K_ADDR = "cs.join.address"
    private const val K_AT = "cs.join.at"
    private const val K_VERSION = "cs.join.version"
    private const val K_KNOWN_HOSTS = "cs.friends.known_hosts"
    private const val TTL = 3L * 60 * 1000

    /** Hosts (friend ids) whose world the player joined before: joinable again without an invite. */
    fun isKnownHost(@Nullable friendId: String?): Boolean {
        if (friendId == null) return false
        val known: Set<String>? = AllSettings.friendsSeenKeys.state.toSet()
        return known.contains(friendId)
    }

    fun rememberHost(@Nullable friendId: String?) {
        if (friendId == null || friendId.isEmpty()) return
        val cur = AllSettings.friendsSeenKeys.state.filter { it.isNotEmpty() }.toMutableSet()
        if (cur.add(friendId)) {
            AllSettings.friendsSeenKeys.save(cur.filter { it.isNotEmpty() }.toList())
        }
    }

    fun forgetHost(@Nullable friendId: String?) {
        if (friendId == null) return
        val cur = AllSettings.friendsSeenKeys.state.filter { it.isNotEmpty() }.toMutableSet()
        if (cur.contains(friendId)) {
            cur.remove(friendId)
            AllSettings.friendsSeenKeys.save(cur.filter { it.isNotEmpty() }.toList())
        }
    }

    /** Join a friend's world and remember the host (Bedrock-like: next time, no invite needed). */
    fun joinFriend(@NonNull activity: Activity, @Nullable friendId: String?, @NonNull address: String, @Nullable engine: String?, @Nullable hostVersion: String?) {
        rememberHost(friendId)
        join(activity, address, engine, hostVersion)
    }

    /** @param hostVersion the host's game version when the invite carried one (Java cross-version join) */
    fun join(@NonNull activity: Activity, @NonNull address: String, @Nullable engine: String?, @Nullable hostVersion: String?) {
        val horizon = "horizon" == engine?.lowercase()
        AllSettings.friendsToken.save(AllSettings.friendsToken.state) // keep existing token
        AllSettings.friendsUsername.save(AllSettings.friendsUsername.state) // keep existing username

        // Store pending join address in AllSettings for the multiplayer/launch screen to consume
        AllSettings.friendsCode.save(address) // reuse friendsCode slot as pending join address (temporary)

        // Also store timestamp and version using companion slots we don't otherwise use
        // Note: AllSettings is a SettingsRegistry; we use existing slots for simplicity
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
        prefs.edit()
            .putString(K_ADDR, address)
            .putLong(K_AT, System.currentTimeMillis())
            .putString(K_VERSION, if (horizon || hostVersion == null || hostVersion.isBlank()) null else hostVersion.trim())
            .apply()

        Toast.makeText(activity, activity.getString(R.string.friends_joining, address), Toast.LENGTH_SHORT).show()
    }

    /** The host's game version of the pending join (not cleared; valid while the address is), or null. */
    @Nullable
    fun pendingHostVersion(@NonNull activity: Activity): String? {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
        if (prefs.getString(K_ADDR, null) == null) return null
        val at = prefs.getLong(K_AT, 0)
        if (System.currentTimeMillis() - at > TTL) return null
        return prefs.getString(K_VERSION, null)
    }

    /** The pending address for the launch happening now (cleared), or null. */
    @Nullable
    fun consume(@NonNull activity: Activity): String? {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
        val a = prefs.getString(K_ADDR, null)
        val at = prefs.getLong(K_AT, 0)
        prefs.edit().remove(K_ADDR).remove(K_AT).remove(K_VERSION).apply()
        if (a == null) return null
        if (System.currentTimeMillis() - at > TTL) return null
        return a.takeIf { it.trim().isNotEmpty() }?.trim()
    }

    /** Quick Play exists since 1.20 (23w14a); older releases use --server/--port. */
    fun supportsQuickPlay(@Nullable versionId: String?): Boolean {
        if (versionId == null) return true
        val v = versionId.trim()
        val m = Regex("^1\\.(\\d+)").find(v)
        if (m != null) {
            return try { m.groupValues[1].toInt() >= 20 } catch (_: NumberFormatException) { true }
        }
        val mw = Regex("^(\\d\\d)w(\\d\\d)").find(v)
        if (mw != null) {
            val year = mw.groupValues[1].toInt()
            val week = mw.groupValues[2].toInt()
            return year > 23 || (year == 23 && week >= 14)
        }
        val embedded = Regex("1\\.(\\d+)").find(v)
        if (embedded != null) {
            return try { embedded.groupValues[1].toInt() >= 20 } catch (_: NumberFormatException) { true }
        }
        return true
    }
}
