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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionType
import com.movtery.zalithlauncher.ui.design.CsChip
import com.movtery.zalithlauncher.ui.design.CsEmptyState
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSearchField
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsTonalButton
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionsOperation
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

private enum class InstanceFilter {
    ALL,
    MODDED,
    VANILLA
}

/**
 * CS Launcher Lite instance manager.
 *
 * The list itself is a plain [LazyColumn] with stable keys; filtering and sorting
 * are derived states, so typing in the search field never rebuilds the whole list
 * and scrolling stays allocation free.
 */
@Composable
fun CsInstancesScreen(
    versions: List<Version>,
    currentVersion: Version?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPlay: (Version) -> Unit,
    onOpenInstance: (Version) -> Unit,
    onSetCurrent: (Version) -> Unit,
    onCreate: () -> Unit,
    onManageFolders: () -> Unit,
    onExport: (Version) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    // Rename / copy / delete reuse the dialogs that already exist in the launcher.
    var operation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }
    VersionsOperation(
        versionsOperation = operation,
        updateVersionsOperation = { operation = it },
        submitError = submitError
    )

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(InstanceFilter.ALL) }

    val visibleVersions by remember(versions, currentVersion, filter) {
        derivedStateOf {
            val filtered = versions.filter { version ->
                when (filter) {
                    InstanceFilter.ALL -> true
                    InstanceFilter.MODDED -> version.versionType == VersionType.MODLOADERS
                    InstanceFilter.VANILLA -> version.versionType == VersionType.VANILLA
                }
            }
            filtered.sortedWith(
                compareByDescending<Version> { it == currentVersion }
                    .thenBy { it.getVersionName().lowercase() }
            )
        }
    }

    val searchedVersions by remember(visibleVersions, query) {
        derivedStateOf {
            if (query.isBlank()) visibleVersions
            else visibleVersions.filter { version ->
                version.getVersionName().contains(query.trim(), ignoreCase = true) ||
                        runCatching { version.getVersionInfo()?.minecraftVersion }
                            .getOrNull()
                            ?.contains(query.trim(), ignoreCase = true) == true
            }
        }
    }

    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar: the filter chips get their own row so the controls never
        // overflow on narrow landscape screens.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CsSpace.l, vertical = CsSpace.s),
            verticalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
            ) {
                CsSearchField(
                    modifier = Modifier.weight(1f),
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.cs_instances_search_hint)
                )
                CsIconButton(
                    iconRes = R.drawable.ic_refresh,
                    contentDescription = stringResource(R.string.generic_refresh),
                    onClick = onRefresh
                )
                CsTonalButton(
                    text = stringResource(R.string.cs_instances_manage_folders),
                    iconRes = R.drawable.ic_folder_outlined,
                    onClick = onManageFolders
                )
                CsPrimaryButton(
                    text = stringResource(R.string.cs_instances_create),
                    iconRes = R.drawable.ic_add,
                    onClick = onCreate
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
            ) {
                CsChip(
                    text = stringResource(R.string.cs_instances_filter_all),
                    selected = filter == InstanceFilter.ALL,
                    onClick = { filter = InstanceFilter.ALL }
                )
                CsChip(
                    text = stringResource(R.string.cs_instances_filter_modded),
                    selected = filter == InstanceFilter.MODDED,
                    onClick = { filter = InstanceFilter.MODDED }
                )
                CsChip(
                    text = stringResource(R.string.cs_instances_filter_vanilla),
                    selected = filter == InstanceFilter.VANILLA,
                    onClick = { filter = InstanceFilter.VANILLA }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (searchedVersions.isEmpty()) {
                CsEmptyState(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(360.dp),
                    iconRes = R.drawable.ic_sports_esports_outlined,
                    title = stringResource(R.string.cs_instances_empty_title),
                    message = stringResource(R.string.cs_instances_empty_message),
                    actionLabel = stringResource(R.string.cs_quick_download),
                    onAction = onCreate
                )
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
                        items = searchedVersions,
                        key = { version -> "${version.getGameHome()}/${version.getVersionName()}" },
                        contentType = { "instance" }
                    ) { version ->
                        CsInstanceRowCard(
                            version = version,
                            isCurrent = version == currentVersion,
                            onClick = { onOpenInstance(version) },
                            onPlay = { onPlay(version) },
                            onSettings = { onOpenInstance(version) },
                            onRename = { operation = VersionsOperation.Rename(version) },
                            onCopy = { operation = VersionsOperation.Copy(version) },
                            onExport = { onExport(version) },
                            onDelete = { operation = VersionsOperation.Delete(version) },
                            onSetCurrent = { onSetCurrent(version) }
                        )
                    }
                }
            }
        }
    }
}
