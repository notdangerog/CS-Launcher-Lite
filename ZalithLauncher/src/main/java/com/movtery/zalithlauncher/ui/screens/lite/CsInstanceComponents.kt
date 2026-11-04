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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsMetaLabel
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsStatusPill
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lightweight, immutable snapshot of an instance used by the CS UI.
 * Keeping it immutable avoids unnecessary recompositions when the version list
 * is refreshed but a single instance did not change.
 */
@Immutable
data class CsInstanceMeta(
    val name: String,
    val minecraftVersion: String?,
    val loader: String?,
    val isValid: Boolean
)

/**
 * Reads the display metadata of an instance. All values come from the already
 * parsed [Version] object, so this is cheap and safe on the main thread.
 */
@Composable
fun rememberCsInstanceMeta(version: Version): CsInstanceMeta {
    return remember(version) {
        val info = runCatching { version.getVersionInfo() }.getOrNull()
        val loader = info?.primaryLoader?.loader?.takeIf { it.isLoader }
        CsInstanceMeta(
            name = runCatching { version.getVersionName() }.getOrDefault(""),
            minecraftVersion = info?.minecraftVersion,
            loader = loader?.displayName,
            isValid = runCatching { version.isValid() }.getOrDefault(false)
        )
    }
}

/**
 * Counts files in a folder off the main thread. Returns null while loading and
 * 0 when the folder does not exist, so the UI can hide the counter entirely.
 */
@Composable
fun rememberContentCount(
    version: Version,
    folderName: String,
    refreshKey: Any? = null,
    extension: String? = null
): Int? {
    val state by produceState<Int?>(initialValue = null, version, refreshKey, folderName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(version.getGameDir(), folderName)
                if (!dir.isDirectory) return@runCatching 0
                dir.listFiles()
                    ?.count { it.isFile && (extension == null || it.name.endsWith(extension, true)) }
                    ?: 0
            }.getOrDefault(0)
        }
    }
    return state
}

/**
 * Row card for one instance, used by both Home and the Instances screen.
 */
@Composable
fun CsInstanceRowCard(
    version: Version,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onPlay: () -> Unit = {},
    onSettings: () -> Unit = {},
    onRename: () -> Unit = {},
    onCopy: () -> Unit = {},
    onExport: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSetCurrent: () -> Unit = {}
) {
    val meta = rememberCsInstanceMeta(version)
    val mods = rememberContentCount(version = version, folderName = "mods", extension = ".jar")

    CsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = CsSpace.m, vertical = CsSpace.m),
        color = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = isCurrent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            VersionIconImage(
                modifier = Modifier.size(CsSize.cardIconSmall),
                version = version
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = meta.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrent) {
                        CsStatusPill(text = stringResource(R.string.cs_instance_current))
                    }
                }
                Spacer(modifier = Modifier.height(CsSpace.xxs))
                Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.m)) {
                    Text(
                        text = buildString {
                            append(meta.minecraftVersion ?: stringResource(R.string.cs_instance_unknown_version))
                            append(" · ")
                            append(meta.loader ?: stringResource(R.string.cs_instance_vanilla))
                            if (mods != null && mods > 0) {
                                append(" · ")
                                append(stringResource(R.string.cs_instance_mods, mods))
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            CsPrimaryButton(
                text = stringResource(R.string.cs_instance_play),
                iconRes = R.drawable.ic_play_arrow_filled,
                enabled = meta.isValid,
                onClick = onPlay
            )

            CsIconButton(
                iconRes = R.drawable.ic_settings_filled,
                contentDescription = stringResource(R.string.cs_instance_settings),
                onClick = onSettings
            )

            InstanceMenu(
                isCurrent = isCurrent,
                enabled = meta.isValid,
                onSettings = onSettings,
                onRename = onRename,
                onCopy = onCopy,
                onExport = onExport,
                onDelete = onDelete,
                onSetCurrent = onSetCurrent
            )
        }
    }
}

/**
 * Overflow menu shared by instance cards.
 */
@Composable
private fun InstanceMenu(
    isCurrent: Boolean,
    enabled: Boolean,
    onSettings: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onSetCurrent: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        CsIconButton(
            iconRes = R.drawable.ic_more_horiz,
            contentDescription = stringResource(R.string.generic_more),
            onClick = { expanded = true }
        )

        DropdownMenu(
            modifier = Modifier.widthIn(min = 180.dp),
            expanded = expanded,
            shape = MaterialTheme.shapes.medium,
            onDismissRequest = { expanded = false }
        ) {
            if (!isCurrent) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.cs_instance_set_current)) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null
                        )
                    },
                    enabled = enabled,
                    onClick = {
                        expanded = false
                        onSetCurrent()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.versions_manage_settings)) },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = null
                    )
                },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onSettings()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.generic_rename)) },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.generic_copy)) },
                onClick = {
                    expanded = false
                    onCopy()
                }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.versions_export)) },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onExport()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.generic_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * Hero card of the Home screen: the recently played instance with a big play button.
 */
@Composable
fun CsInstanceHeroCard(
    version: Version,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val meta = rememberCsInstanceMeta(version)
    val mods = rememberContentCount(version = version, folderName = "mods", extension = ".jar")

    CsCard(
        modifier = modifier,
        contentPadding = PaddingValues(CsSpace.l),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.l)
        ) {
            VersionIconImage(
                modifier = Modifier.size(CsSize.cardIconLarge),
                version = version
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = meta.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrent) {
                        CsStatusPill(text = stringResource(R.string.cs_instance_current))
                    }
                }
                Spacer(modifier = Modifier.height(CsSpace.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.m)) {
                    CsMetaLabel(
                        iconRes = R.drawable.ic_sports_esports_outlined,
                        text = meta.minecraftVersion ?: stringResource(R.string.cs_instance_unknown_version)
                    )
                    CsMetaLabel(
                        iconRes = R.drawable.ic_build_outlined,
                        text = meta.loader ?: stringResource(R.string.cs_instance_vanilla)
                    )
                    if (mods != null && mods > 0) {
                        CsMetaLabel(
                            iconRes = R.drawable.ic_extension_outlined,
                            text = stringResource(R.string.cs_instance_mods, mods)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(CsSpace.l))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CsPrimaryButton(
                modifier = Modifier.widthIn(min = 140.dp),
                text = stringResource(R.string.cs_instance_play),
                iconRes = R.drawable.ic_play_arrow_filled,
                enabled = meta.isValid,
                large = true,
                onClick = onPlay
            )
            CsIconButton(
                iconRes = R.drawable.ic_settings_filled,
                contentDescription = stringResource(R.string.cs_instance_settings),
                onClick = onSettings
            )
        }
    }
}
