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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSectionHeader
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsStatusPill
import com.movtery.zalithlauncher.ui.design.CsTonalButton
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage

/**
 * Home screen of CS Launcher Lite.
 *
 * The center column is the catalog: "Popular Modpacks" as a 2-column grid that shows
 * roughly four cards at a time and keeps loading more while the user scrolls.
 *
 * When at least one instance exists, the recently played instances are placed above
 * the catalog (the catalog itself never disappears, it just moves down).
 */
@Composable
fun CsHomeScreen(
    versions: List<Version>,
    currentVersion: Version?,
    catalog: CsCatalogViewModel,
    onPlay: (Version) -> Unit,
    onOpenInstance: (Version) -> Unit,
    onOpenInstances: () -> Unit,
    onOpenModpack: (CsCatalogEntry) -> Unit,
    onInstallModpack: (CsCatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    // Most recent first: the current instance, then the rest in alphabetical order,
    // exactly like the instances screen sorts them.
    val recentInstances = remember(versions, currentVersion) {
        versions.sortedWith(
            compareByDescending<Version> { it == currentVersion }
                .thenBy { runCatching { it.getVersionName() }.getOrDefault("").lowercase() }
        ).take(8)
    }

    BoxWithConstraints(modifier = modifier) {
        val hasInstances = recentInstances.isNotEmpty()

        // Two rows of cards must fit the content area on a landscape phone, so the
        // card height is derived from the available height instead of being fixed.
        val reservedForInstances = if (hasInstances) 132.dp else 0.dp
        val headerHeight = 52.dp
        val cardHeight = ((maxHeight - reservedForInstances - headerHeight - CsSpace.l) / 2)
            .coerceIn(124.dp, CsSize.catalogCardHeightLarge)

        // A single column on very narrow portrait screens, two everywhere else.
        val columns = if (maxWidth < 400.dp) 1 else 2

        CsCatalogGrid(
            viewModel = catalog,
            cardHeight = cardHeight,
            columns = columns,
            gridState = rememberLazyGridState(),
            onOpen = onOpenModpack,
            onInstall = onInstallModpack,
            header = {
                if (hasInstances) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        InstancesSection(
                            versions = recentInstances,
                            currentVersion = currentVersion,
                            onPlay = onPlay,
                            onOpenInstance = onOpenInstance,
                            onOpenAll = onOpenInstances
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CsSectionHeader(
                        modifier = Modifier.padding(top = CsSpace.xs, bottom = CsSpace.xxs),
                        title = stringResource(R.string.cs_home_popular_modpacks),
                        subtitle = stringResource(R.string.cs_home_popular_modpacks_subtitle)
                    )
                }
            }
        )
    }
}

/**
 * "Your instances" strip shown above the catalog when the user already has instances.
 */
@Composable
private fun InstancesSection(
    versions: List<Version>,
    currentVersion: Version?,
    onPlay: (Version) -> Unit,
    onOpenInstance: (Version) -> Unit,
    onOpenAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CsSectionHeader(
            modifier = Modifier.padding(top = CsSpace.xs, bottom = CsSpace.s),
            title = stringResource(R.string.cs_home_your_instances),
            subtitle = stringResource(R.string.cs_home_your_instances_subtitle),
            trailing = {
                CsInstancesAllButton(onClick = onOpenAll)
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m),
            contentPadding = PaddingValues(end = CsSpace.s)
        ) {
            items(
                items = versions,
                key = { version -> runCatching { version.getVersionName() }.getOrDefault("") }
            ) { version ->
                HomeInstanceCard(
                    version = version,
                    isCurrent = version == currentVersion,
                    onPlay = { onPlay(version) },
                    onOpen = { onOpenInstance(version) }
                )
            }
        }
    }
}

/**
 * Compact instance card used on the home screen. Instance management (rename, copy,
 * delete, export) stays on the instances screen, this card only launches or opens.
 */
@Composable
private fun HomeInstanceCard(
    version: Version,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = rememberCsInstanceMeta(version)

    CsCard(
        modifier = modifier.width(268.dp),
        onClick = onOpen,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = CsSpace.m, vertical = CsSpace.s)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            VersionIconImage(
                modifier = Modifier.size(CsSize.cardIconSmall),
                version = version
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CsSpace.xs)
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
                Text(
                    text = buildString {
                        append(meta.minecraftVersion ?: stringResource(R.string.cs_instance_unknown_version))
                        append(" · ")
                        append(meta.loader ?: stringResource(R.string.cs_instance_vanilla))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CsPrimaryButton(
                text = stringResource(R.string.cs_instance_play),
                iconRes = R.drawable.ic_play_arrow_filled,
                enabled = meta.isValid,
                onClick = onPlay
            )
        }
    }
}

/**
 * Small text button that switches to the instances section.
 */
@Composable
private fun CsInstancesAllButton(onClick: () -> Unit) {
    CsTonalButton(
        text = stringResource(R.string.cs_home_all_instances),
        onClick = onClick
    )
}
