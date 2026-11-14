/*
 * CS Launcher Lite — CS Friends client
 * Ported from JoooaoooDVD/CSL2 (app_cs/src/main/java/com/craftstudio/cslauncher/e4mc/HostSettings.java)
 */

package com.movtery.zalithlauncher.ui.cs.friends

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedHashSet

/**
 * World hosting settings for worlds shared through CS Friends: who may join, how many, and which
 * server icon friends see. The Java integrated server caps a LAN world at 8 players, so the Java
 * limit stays within 2..8; cheats for guests are what the game's own Open to LAN screen chose.
 */
object HostSettings {
    const val ACCESS_ANYONE = 0
    const val ACCESS_FRIENDS = 1
    const val ACCESS_INVITED = 2

    /** The game's own LAN limit (IntegratedPlayerList passes 8): what applies without the host agent. */
    const val JAVA_DEFAULT_MAX = 8
    /** The ceiling the CS host agent lifts a Java world to. */
    const val JAVA_MAX_PLAYERS = 512
    /** The system property the host agent reads at game start (-Dcs.host.maxPlayers). */
    const val JAVA_PROPERTY = "cs.host.maxPlayers"
    /** The value maxPlayers() stores for "no limit". */
    const val NO_LIMIT = 0

    private const val K_MAX = "cs.host.max_players"
    private const val K_ACCESS = "cs.host.access"
    private const val K_RELAY = "cs.host.relay"

    /** Names invited from this process while hosting ("invited only" lets them in). */
    private val sInvited = LinkedHashSet<String>()

    /** Server icon friends see in their server list: the CS Launcher mark, the profile's icon, or a custom PNG. */
    const val ICON_CS = 0
    const val ICON_PROFILE = 1
    const val ICON_CUSTOM = 2
    private const val K_ICON = "cs.host.icon"
    /** A 64x64 PNG with this name in the profile's game folder is the custom server icon (same as CS Engine). */
    const val CUSTOM_ICON = "cs-server-icon-custom.png"

    fun icon(): Int {
        val i = AllSettings.hostIconMode.state
        return if (i < ICON_CS || i > ICON_CUSTOM) ICON_CS else i
    }

    fun setIcon(mode: Int) { AllSettings.hostIconMode.save(mode) }

    fun iconLabel(mode: Int): String = when (mode) {
        ICON_PROFILE -> "Profile icon"
        ICON_CUSTOM -> "Custom ($CUSTOM_ICON)"
        else -> "CS Launcher"
    }

    /**
     * Writes server-icon.png (64x64) into the game folder before the game starts: the integrated
     * server reads it from there for the icon in friends' server lists.
     */
    fun writeServerIcon(@NonNull context: Context, @NonNull gameDir: File, @Nullable profileIcon: File?) {
        val out = File(gameDir, "server-icon.png")
        var src: Bitmap? = null
        try {
            when (icon()) {
                ICON_CUSTOM -> {
                    val custom = File(gameDir, CUSTOM_ICON)
                    if (custom.isFile()) src = BitmapFactory.decodeFile(custom.absolutePath)
                }
                ICON_PROFILE -> {
                    if (profileIcon != null && profileIcon.isFile()) src = BitmapFactory.decodeFile(profileIcon.absolutePath)
                }
            }
            if (src == null) {
                val d = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                if (d != null) {
                    src = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(src)
                    d.setBounds(0, 0, 64, 64)
                    d.draw(c)
                }
            }
            if (src == null) return
            val scaled = if (src.width == 64 && src.height == 64) src else Bitmap.createScaledBitmap(src, 64, 64, true)
            FileOutputStream(out).use { fo -> scaled.compress(Bitmap.CompressFormat.PNG, 100, fo) }
        } catch (e: IOException) {
            Log.w("HostSettings", "server icon: $e")
        } catch (e: RuntimeException) {
            Log.w("HostSettings", "server icon: $e")
        }
    }

    /** The stops the +/- buttons walk; 0 is "no limit". */
    val STEPS = intArrayOf(2, 4, 6, 8, 10, 12, 16, 20, 24, 32, 48, 64, 100, 200, NO_LIMIT)

    /** Players the world takes, host included: 2..512, or NO_LIMIT. */
    fun maxPlayers(): Int {
        var n = AllSettings.hostMaxPlayers.state
        if (n == NO_LIMIT) return NO_LIMIT
        return maxOf(2, minOf(JAVA_MAX_PLAYERS, n))
    }

    /** What the game is told at start: the limit, or the ceiling for "no limit". */
    fun javaMaxPlayers(): Int {
        val n = maxPlayers()
        return if (n == NO_LIMIT) JAVA_MAX_PLAYERS else n
    }

    /** Who may join: 0 = anyone, 1 = friends only, 2 = invited only. */
    fun access(): Int {
        val a = AllSettings.hostAccess.state
        return if (a < ACCESS_ANYONE || a > ACCESS_INVITED) ACCESS_FRIENDS else a
    }

    fun setAccess(mode: Int) { AllSettings.hostAccess.save(mode) }

    fun accessLabel(): String = when (access()) {
        ACCESS_ANYONE -> "Anyone"
        ACCESS_FRIENDS -> "Friends only"
        ACCESS_INVITED -> "Invited only"
        else -> "Friends only"
    }

    /** Names invited from this process while hosting. */
    fun invitedNames(): Set<String> = synchronized(sInvited) { LinkedHashSet(sInvited) }

    fun addInvited(name: String) { synchronized(sInvited) { sInvited.add(name) } }

    fun removeInvited(name: String) { synchronized(sInvited) { sInvited.remove(name) } }

    fun clearInvited() { synchronized(sInvited) { sInvited.clear() } }
}
