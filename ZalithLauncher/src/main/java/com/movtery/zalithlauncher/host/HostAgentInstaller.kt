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
import android.util.Log
import com.movtery.zalithlauncher.context.copyAssetFile
import com.movtery.zalithlauncher.path.PathManager
import java.io.File
import java.util.jar.JarFile

private const val TAG = "HostAgentInstaller"

/**
 * Puts the `-javaagent` jar where the game JVM can load it, and builds the arguments for it.
 *
 * **Why an asset and not a library dependency.** A `-javaagent` jar is appended to the *system* class
 * path of the game's JVM, which is a separate JVM from the launcher's — an ordinary Gradle dependency
 * would land in the APK's dex, invisible to that class path and unusable as an agent. So the shaded
 * jar built by `:HostAgent` ships as an asset and is extracted to disk at launch, where the game JVM
 * can be pointed at a real path.
 *
 * **Why verify after extracting.** An agent jar whose manifest lost its `Premain-Class` (a bad shade,
 * a truncated copy) makes the JVM refuse to start at all, with an error that reads like a broken Java
 * runtime. That is the worst possible failure mode for a launcher — the player blames their JRE — so
 * the jar is checked before it is ever passed on, and hosting degrades to vanilla's 8-player limit
 * instead.
 */
object HostAgentInstaller {

    /** Asset path of the shaded agent jar, produced by `:HostAgent:shadowJar`. */
    private const val ASSET_PATH = "components/host_agent/cs-host-agent.jar"

    /** The Premain-Class the agent jar must declare. */
    private const val EXPECTED_PREMAIN = "com.movtery.zalithlauncher.hostagent.CsHostAgent"

    /** System property the patched constructor reads for the player ceiling. */
    const val PROPERTY_MAX_PLAYERS = "cs.host.maxPlayers"

    @Volatile
    private var verified: File? = null

    /** Where the extracted jar lives. */
    private fun targetFile(): File = File(File(PathManager.DIR_COMPONENTS, "host_agent"), "cs-host-agent.jar")

    /**
     * Extracts and verifies the agent jar.
     *
     * @return the jar to pass to `-javaagent`, or null when this build has no working agent — in
     *   which case the world still hosts, limited to the vanilla 8 players
     */
    fun install(context: Context): File? {
        verified?.let { if (it.isFile) return it }

        val target = targetFile()
        return try {
            // Re-extract when missing or when the size differs: a partial copy from an interrupted
            // launch would otherwise be cached forever and fail verification on every subsequent one.
            val assetSize = context.assets.openFd(ASSET_PATH).use { it.length }
            if (!target.isFile || target.length() != assetSize) {
                context.copyAssetFile(ASSET_PATH, target, overwrite = true)
            }
            if (!verify(target)) {
                Log.w(TAG, "agent jar failed verification; hosting without it")
                target.delete()
                return null
            }
            verified = target
            target
        } catch (e: Exception) {
            // A missing asset is the normal case for a build that skipped :HostAgent; everything else
            // is a disk or asset-manager problem. Neither may block the launch.
            Log.i(TAG, "no usable host agent (${e.javaClass.simpleName}: ${e.message})")
            null
        }
    }

    /**
     * The JVM arguments that enable hosting for this launch, or an empty list when hosting is off or
     * the agent is unavailable.
     *
     * The property is always passed alongside the agent, never alone: `-Dcs.host.maxPlayers` without
     * the agent is a no-op, and the agent without the property silently keeps the vanilla 8.
     */
    fun jvmArgs(context: Context): List<String> {
        if (!HostSettings.sharingEnabled) return emptyList()
        val jar = install(context) ?: return emptyList()
        return listOf(
            "-javaagent:${jar.absolutePath}",
            "-D$PROPERTY_MAX_PLAYERS=${HostSettings.javaMaxPlayers}"
        )
    }

    /** True when [jar] declares the expected premain class and does not leak unrelocated ASM. */
    private fun verify(jar: File): Boolean {
        if (!jar.isFile || jar.length() <= 0) return false
        return runCatching {
            JarFile(jar).use { opened ->
                val manifest = opened.manifest ?: return false
                val premain = manifest.mainAttributes.getValue("Premain-Class")
                if (premain != EXPECTED_PREMAIN) {
                    Log.w(TAG, "unexpected Premain-Class: $premain")
                    return false
                }
                // ASM must be relocated: the game ships its own, and Fabric Loader refuses to start
                // when it finds a second org.objectweb.asm.ClassReader on the class path.
                val leaked = opened.entries().asSequence()
                    .any { it.name.startsWith("org/objectweb/asm/") }
                if (leaked) {
                    Log.w(TAG, "agent jar contains unrelocated ASM classes")
                    return false
                }
                // The premain class itself has to be in the jar, or the JVM fails at agent load.
                opened.getJarEntry(EXPECTED_PREMAIN.replace('.', '/') + ".class") != null
            }
        }.getOrElse {
            Log.w(TAG, "could not read agent jar", it)
            false
        }
    }

    /** Drops the cached verification so the next launch re-checks the jar. */
    fun invalidate() {
        verified = null
    }
}
