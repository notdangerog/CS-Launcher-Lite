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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.design.CsChip
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsSearchField
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace

/**
 * Discover screen: one search field, a horizontal category row and the matching
 * catalog results underneath.
 *
 * Categories map onto the content types the launcher can actually install:
 * mod packs, mods, resource packs, shaders (Modrinth) and worlds (CurseForge).
 */
@Composable
fun CsDiscoverScreen(
    onOpenEntry: (CsCatalogEntry) -> Unit,
    onInstallEntry: (CsCatalogEntry) -> Unit,
    onFavorites: () -> Unit,
    onSearchById: () -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = ""
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var selectedName by rememberSaveable { mutableStateOf(CsCatalogType.MODPACK.name) }
    val selected = CsCatalogType.entries.firstOrNull { it.name == selectedName } ?: CsCatalogType.MODPACK

    // One view model per category: switching back and forth reuses cached pages.
    val catalog = rememberCsCatalogViewModel(key = "discover", type = selected)

    // Keep the search text in sync with the active category (debounced inside the VM).
    LaunchedEffect(selected, query) {
        catalog.onQueryChange(query)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = CsSpace.l, end = CsSpace.l, top = CsSpace.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            CsSearchField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.cs_discover_search_hint),
                onSearch = { catalog.searchNow() }
            )
            CsIconButton(
                iconRes = R.drawable.ic_favorite_outlined,
                contentDescription = stringResource(R.string.cs_discover_favorites),
                onClick = onFavorites
            )
            CsIconButton(
                iconRes = R.drawable.ic_search,
                contentDescription = stringResource(R.string.cs_discover_by_id),
                onClick = onSearchById
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CsSpace.s, bottom = CsSpace.xs),
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = CsSpace.l)
        ) {
            items(items = CsCatalogType.entries, key = { it.name }) { type ->
                CsChip(
                    text = stringResource(type.labelRes),
                    selected = type == selected,
                    onClick = { selectedName = type.name }
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val columns = when {
                maxWidth < 400.dp -> 1
                maxWidth < 700.dp -> 2
                maxWidth < 1000.dp -> 3
                else -> 4
            }
            CsCatalogGrid(
                viewModel = catalog,
                cardHeight = CsSize.catalogCardHeight
                    .takeIf { maxWidth >= 700.dp }
                    ?: 148.dp,
                columns = columns,
                gridState = rememberLazyGridState(),
                onOpen = onOpenEntry,
                onInstall = onInstallEntry
            )
        }
    }
}
