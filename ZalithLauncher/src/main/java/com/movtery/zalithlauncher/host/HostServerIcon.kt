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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import java.io.File
import java.io.FileOutputStream

private const val TAG = "HostServerIcon"

/**
 * Writes the 64x64 `server-icon.png` a hosted world shows in its guests' server list.
 *
 * **Why the launcher writes it at all.** The integrated server reads `server-icon.png` from the game
 * folder when it publishes to LAN; without one, every guest sees the same grey "LAN World" entry and
 * cannot tell one friend's world from another's. Writing it before launch costs one small file and
 * makes a world recognisable at a glance, which is the difference between a friends list of worlds
 * and a list of guesses.
 *
 * Minecraft requires exactly 64x64 — a larger or smaller icon is silently ignored — so every mode
 * here scales its source into that square rather than trusting it to already be right.
 */
object HostServerIcon {

    /** Minecraft's own name for the file; it must sit in the game folder root. */
    private const val FILE_NAME = "server-icon.png"

    private const val SIZE = 64

    /**
     * Writes the icon for the current [HostSettings.iconMode] into [gameDir].
     *
     * Never throws and never blocks a launch: an icon is decoration, and a failed decode must not be
     * the reason a player cannot open their world to friends. When nothing can be written, any
     * existing icon is left alone so a previous session's icon survives rather than the world falling
     * back to the grey default.
     *
     * @return true when an icon was written
     */
    fun write(context: Context, gameDir: File): Boolean {
        return try {
            val bitmap = when (HostSettings.iconMode) {
                HostSettings.ICON_PROFILE -> profileHead()
                HostSettings.ICON_CUSTOM -> customIcon(gameDir)
                else -> launcherIcon(context)
            } ?: return false

            val scaled = bitmap.scaleToIcon()
            val target = File(gameDir, FILE_NAME)
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            if (scaled !== bitmap) scaled.recycle()
            Log.i(TAG, "wrote ${target.name} (mode ${HostSettings.iconMode})")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "could not write the server icon", e)
            false
        }
    }

    /** Deletes a previously written icon, restoring the game's default. */
    fun clear(gameDir: File) {
        runCatching { File(gameDir, FILE_NAME).takeIf { it.isFile }?.delete() }
    }

    /** The launcher's own icon, scaled down — the default, and the one that brands a CS world. */
    private fun launcherIcon(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).let { canvas ->
            drawable.setBounds(0, 0, SIZE, SIZE)
            drawable.draw(canvas)
        }
        return bitmap
    }

    /**
     * The head of the selected account's skin.
     *
     * The skin layout is fixed by Minecraft itself: on a 64x64 skin the front of the head is the 8x8
     * region at (8, 8). Only that square is used, because a whole-skin thumbnail is an unreadable
     * smear at 64x64 while a head is instantly recognisable — the same reason player heads are a
     * thing in the first place.
     */
    private fun profileHead(): Bitmap? {
        val account = AccountsManager.currentAccountFlow.value ?: return null
        val skin = runCatching { account.getSkinFile() }.getOrNull() ?: return null
        if (!skin.isFile) return null

        val decoded = BitmapFactory.decodeFile(skin.absolutePath) ?: return null
        return try {
            // Legacy 64x32 skins have no hat layer but the same head position, so both work.
            if (decoded.width < 16 || decoded.height < 16) return null
            Bitmap.createBitmap(decoded, HEAD_X, HEAD_Y, HEAD_SIZE, HEAD_SIZE)
        } finally {
            decoded.recycle()
        }
    }

    /** The player's own PNG, if they dropped one in the game folder under the documented name. */
    private fun customIcon(gameDir: File): Bitmap? {
        val custom = File(gameDir, HostSettings.CUSTOM_ICON_FILENAME)
        if (!custom.isFile) {
            Log.i(TAG, "no ${HostSettings.CUSTOM_ICON_FILENAME} in the game folder; keeping the current icon")
            return null
        }
        return BitmapFactory.decodeFile(custom.absolutePath)
    }

    /** Scales any bitmap to exactly 64x64, returning the receiver unchanged when it already is. */
    private fun Bitmap.scaleToIcon(): Bitmap {
        if (width == SIZE && height == SIZE) return this
        return Bitmap.createScaledBitmap(this, SIZE, SIZE, true)
    }

    // Head region of a Minecraft skin.
    private const val HEAD_X = 8
    private const val HEAD_Y = 8
    private const val HEAD_SIZE = 8
}
