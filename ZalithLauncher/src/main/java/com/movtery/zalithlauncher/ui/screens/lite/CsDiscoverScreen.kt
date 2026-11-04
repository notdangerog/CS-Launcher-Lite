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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsSpace

private class DiscoverEntry(
    val titleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int,
    val action: () -> Unit
)

/**
 * Content discovery hub.
 *
 * Each tile opens the existing download flow that the launcher already ships
 * (game install, modpacks, mods, resource packs, shaders, worlds, ID search and
 * favorites), so the feature set is unchanged - only the entry experience is new.
 */
@Composable
fun CsDiscoverScreen(
    onDownloadGame: () -> Unit,
    onDownloadModPacks: () -> Unit,
    onDownloadMods: () -> Unit,
    onDownloadResourcePacks: () -> Unit,
    onDownloadShaders: () -> Unit,
    onDownloadSaves: () -> Unit,
    onSearchById: () -> Unit,
    onFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        DiscoverEntry(
            R.string.cs_discover_game,
            R.string.cs_discover_game_desc,
            R.drawable.ic_sports_esports_filled,
            onDownloadGame
        ),
        DiscoverEntry(
            R.string.cs_discover_modpack,
            R.string.cs_discover_modpack_desc,
            R.drawable.ic_package_2_outlined,
            onDownloadModPacks
        ),
        DiscoverEntry(
            R.string.cs_discover_mod,
            R.string.cs_discover_mod_desc,
            R.drawable.ic_extension_outlined,
            onDownloadMods
        ),
        DiscoverEntry(
            R.string.cs_discover_resource_pack,
            R.string.cs_discover_resource_pack_desc,
            R.drawable.ic_format_paint_outlined,
            onDownloadResourcePacks
        ),
        DiscoverEntry(
            R.string.cs_discover_shaders,
            R.string.cs_discover_shaders_desc,
            R.drawable.ic_lightbulb,
            onDownloadShaders
        ),
        DiscoverEntry(
            R.string.cs_discover_saves,
            R.string.cs_discover_saves_desc,
            R.drawable.ic_public,
            onDownloadSaves
        ),
        DiscoverEntry(
            R.string.cs_discover_by_id,
            R.string.cs_discover_by_id_desc,
            R.drawable.ic_card,
            onSearchById
        ),
        DiscoverEntry(
            R.string.cs_discover_favorites,
            R.string.cs_discover_favorites_desc,
            R.drawable.ic_favorite_outlined,
            onFavorites
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CsSpace.l, vertical = CsSpace.s)
        ) {
            Text(
                text = stringResource(R.string.cs_discover_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(minSize = 200.dp),
            state = rememberLazyGridState(),
            contentPadding = PaddingValues(
                start = CsSpace.l,
                end = CsSpace.l,
                top = CsSpace.xs,
                bottom = CsSpace.l
            ),
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m),
            verticalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            items(
                items = entries,
                key = { it.titleRes },
                contentType = { "discover" }
            ) { entry ->
                CsDiscoverTile(entry = entry)
            }
        }
    }
}

@Composable
private fun CsDiscoverTile(entry: DiscoverEntry) {
    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = entry.action,
        contentPadding = PaddingValues(CsSpace.l),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        painter = painterResource(entry.iconRes),
                        contentDescription = null
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(entry.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(entry.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
