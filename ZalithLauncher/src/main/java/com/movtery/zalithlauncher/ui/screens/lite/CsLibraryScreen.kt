/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsChip
import com.movtery.zalithlauncher.ui.design.CsEmptyState
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Snapshot of the content folders of one instance.
 */
@Immutable
data class CsLibraryStats(
    val mods: Int = 0,
    val resourcePacks: Int = 0,
    val shaders: Int = 0,
    val worlds: Int = 0,
    val screenshots: Int = 0
) {
    val total: Int get() = mods + resourcePacks + shaders + worlds + screenshots
}

private fun countFiles(dir: File, recursive: Boolean = false, predicate: (File) -> Boolean): Int {
    if (!dir.isDirectory) return 0
    val files = if (recursive) dir.walkTopDown().filter { it.isFile }.toList()
    else dir.listFiles()?.filter { it.isFile }.orEmpty()
    return files.count(predicate)
}

private fun countDirs(dir: File): Int {
    if (!dir.isDirectory) return 0
    return dir.listFiles()?.count { it.isDirectory } ?: 0
}

/**
 * Reads the content statistics of an instance off the main thread.
 * The scan only touches the top level of each content folder, so it stays cheap
 * even for instances with hundreds of mods.
 */
@Composable
fun rememberLibraryStats(version: Version, refreshKey: Any? = null): CsLibraryStats? {
    val stats by produceState<CsLibraryStats?>(initialValue = null, version, refreshKey) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val gameDir = version.getGameDir()
                CsLibraryStats(
                    mods = countFiles(File(gameDir, "mods")) { it.name.endsWith(".jar", true) },
                    resourcePacks = countFiles(File(gameDir, "resourcepacks")) {
                        it.isDirectory || it.name.endsWith(".zip", true)
                    },
                    shaders = countFiles(File(gameDir, "shaderpacks")) {
                        it.isDirectory || it.name.endsWith(".zip", true)
                    },
                    worlds = countDirs(File(gameDir, "saves")),
                    screenshots = countFiles(File(gameDir, "screenshots")) {
                        it.name.endsWith(".png", true) || it.name.endsWith(".jpg", true)
                    }
                )
            }.getOrNull()
        }
    }
    return stats
}

/**
 * Library screen: organizes everything that is installed inside the instances.
 *
 * Content managers are the ones already implemented by the launcher; the library
 * simply surfaces them per instance instead of hiding them behind the instance
 * settings screens.
 */
@Composable
fun CsLibraryScreen(
    versions: List<Version>,
    currentVersion: Version?,
    onManageInstance: (Version) -> Unit,
    onOpenManager: (Version, NormalNavKey.Versions) -> Unit,
    onOpenInstances: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ordered = remember(versions, currentVersion) {
        versions.sortedWith(compareByDescending<Version> { it == currentVersion }.thenBy { it.getVersionName() })
    }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CsSpace.l, vertical = CsSpace.s)
        ) {
            Text(
                text = stringResource(R.string.cs_library_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (ordered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CsEmptyState(
                    modifier = Modifier.width(360.dp),
                    iconRes = R.drawable.ic_folder_zip_outlined,
                    title = stringResource(R.string.cs_library_empty_title),
                    message = stringResource(R.string.cs_library_empty_message),
                    actionLabel = stringResource(R.string.cs_instances_create),
                    onAction = onOpenInstances
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = CsSpace.l,
                    end = CsSpace.l,
                    top = CsSpace.xs,
                    bottom = CsSpace.l
                ),
                verticalArrangement = Arrangement.spacedBy(CsSpace.s)
            ) {
                items(
                    items = ordered,
                    key = { "${it.getGameHome()}/${it.getVersionName()}" },
                    contentType = { "library" }
                ) { version ->
                    CsLibraryRow(
                        version = version,
                        onManageInstance = { onManageInstance(version) },
                        onOpenManager = { key -> onOpenManager(version, key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CsLibraryRow(
    version: Version,
    onManageInstance: () -> Unit,
    onOpenManager: (NormalNavKey.Versions) -> Unit
) {
    val stats = rememberLibraryStats(version)

    CsCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(CsSpace.m),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            VersionIconImage(
                modifier = Modifier.size(40.dp),
                version = version
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.getVersionName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (stats == null) {
                    Text(
                        text = stringResource(R.string.generic_loading),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
                    ) {
                        CsChip(
                            text = stringResource(R.string.cs_library_mods_count, stats.mods),
                            iconRes = R.drawable.ic_extension_outlined,
                            onClick = { onOpenManager(NormalNavKey.Versions.ModsManager) }
                        )
                        CsChip(
                            text = stringResource(R.string.cs_library_resource_packs_count, stats.resourcePacks),
                            iconRes = R.drawable.ic_format_paint_outlined,
                            onClick = { onOpenManager(NormalNavKey.Versions.ResourcePackManager) }
                        )
                        CsChip(
                            text = stringResource(R.string.cs_library_shaders_count, stats.shaders),
                            iconRes = R.drawable.ic_lightbulb,
                            onClick = { onOpenManager(NormalNavKey.Versions.ShadersManager) }
                        )
                        CsChip(
                            text = stringResource(R.string.cs_library_worlds_count, stats.worlds),
                            iconRes = R.drawable.ic_public,
                            onClick = { onOpenManager(NormalNavKey.Versions.SavesManager) }
                        )
                        CsChip(
                            text = stringResource(R.string.cs_library_screenshots_count, stats.screenshots),
                            iconRes = R.drawable.ic_photo_library_outlined,
                            onClick = { onOpenManager(NormalNavKey.Versions.ScreenshotsManager) }
                        )
                        CsChip(
                            text = stringResource(R.string.cs_library_servers),
                            iconRes = R.drawable.ic_dns_outlined,
                            onClick = { onOpenManager(NormalNavKey.Versions.ServerList) }
                        )
                    }
                }
            }

            CsPrimaryButton(
                text = stringResource(R.string.cs_library_open),
                onClick = onManageInstance
            )
        }
    }
}
