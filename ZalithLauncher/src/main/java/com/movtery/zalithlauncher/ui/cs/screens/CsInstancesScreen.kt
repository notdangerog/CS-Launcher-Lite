/*
 * CS Launcher Lite
 *
 * Derived from Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.movtery.zalithlauncher.ui.cs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionType
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsEmptyState
import com.movtery.zalithlauncher.ui.cs.components.CsSearchField
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.CsPrimaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsTabs
import com.movtery.zalithlauncher.ui.cs.components.CsTag
import com.movtery.zalithlauncher.ui.cs.theme.CsColors

/** Instance filter, mapped to the launcher's real [VersionType] data. */
private enum class InstanceFilter(val labelRes: Int) {
    ALL(R.string.cs_library_tab_all),
    MODDED(R.string.cs_library_tab_modded),
    VANILLA(R.string.cs_library_tab_vanilla)
}

/**
 * Instances — every locally installed Minecraft version, with working launch / settings / pin actions.
 */
@Composable
fun CsInstancesScreen(
    onLaunch: (Version) -> Unit,
    onOpenSettings: (Version) -> Unit,
    onCreateInstance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    var filterIndex by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val filter = InstanceFilter.entries[filterIndex]

    val filtered = remember(versions, filter, query) {
        val byType = when (filter) {
            InstanceFilter.ALL -> versions
            InstanceFilter.MODDED -> versions.filter { it.versionType == VersionType.MODLOADERS }
            InstanceFilter.VANILLA -> versions.filter { it.versionType != VersionType.MODLOADERS }
        }
        val needle = query.trim()
        if (needle.isEmpty()) byType
        else byType.filter { it.getVersionName().contains(needle, ignoreCase = true) }
    }

    // Grid scroll state drives the collapsing header (scroll-position based, so no jumps)
    val gridState = rememberLazyGridState()
    val collapsed = gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 6
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "csLibraryCollapse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        // 大标题：随滚动收起
        AnimatedVisibility(
            visible = collapseProgress < 0.5f,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(140))
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cs_nav_instances),
                            style = MaterialTheme.typography.headlineMedium,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.cs_instances_subtitle, versions.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = CsColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    CsPrimaryButton(
                        text = stringResource(R.string.cs_library_create_instance),
                        onClick = onCreateInstance,
                        iconRes = R.drawable.ic_add,
                        compact = true
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 细搜索栏：始终固定在顶部
        CsSearchField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.cs_library_search)
        )

        // 分类标签：随滚动收起
        AnimatedVisibility(
            visible = collapseProgress < 0.5f,
            enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                fadeOut(animationSpec = tween(140))
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                CsTabs(
                    labels = InstanceFilter.entries.map { stringResource(it.labelRes) },
                    selectedIndex = filterIndex,
                    onSelected = { filterIndex = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(if (collapseProgress > 0.5f) 8.dp else 2.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (versions.isNotEmpty() && query.isNotBlank()) {
                    CsEmptyState(
                        title = stringResource(R.string.cs_library_no_results),
                        description = stringResource(R.string.cs_library_search)
                    )
                } else {
                    EmptyInstances(
                        isFirstRun = versions.isEmpty(),
                        onCreateInstance = onCreateInstance
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Adaptive(minSize = 250.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(items = filtered, key = { "${it.getGameHome()}_${it.getVersionName()}" }) { version ->
                    InstanceCard(
                        version = version,
                        onLaunch = { onLaunch(version) },
                        onOpenSettings = { onOpenSettings(version) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInstances(
    isFirstRun: Boolean,
    onCreateInstance: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(38.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(CsColors.AccentSoft, CsColors.Background)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CsColors.Card)
                    .border(1.dp, CsColors.Border, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(38.dp),
                    painter = painterResource(R.drawable.ic_cs_layers),
                    contentDescription = null,
                    tint = CsColors.Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.cs_library_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            modifier = Modifier.fillMaxWidth(0.92f),
            text = if (isFirstRun) {
                stringResource(R.string.cs_library_empty_desc)
            } else {
                stringResource(R.string.cs_discover_no_results_desc)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = CsColors.TextSecondary,
            maxLines = 3,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        CsPrimaryButton(
            text = stringResource(R.string.cs_library_create_instance),
            onClick = onCreateInstance,
            iconRes = R.drawable.ic_add
        )
    }
}

@Composable
private fun InstanceCard(
    version: Version,
    onLaunch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val info = version.getVersionInfo()
    val loaderName = info?.primaryLoader?.loader?.displayName?.takeIf { it.isNotBlank() }
    val mcVersion = info?.minecraftVersion

    CsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InstanceIcon(version = version, size = 44.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = info?.getInfoString() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (version.pinnedState) {
                    Icon(
                        modifier = Modifier.size(15.dp),
                        painter = painterResource(R.drawable.ic_pinned_filled),
                        contentDescription = null,
                        tint = CsColors.Accent
                    )
                }
                Box {
                    CsIconButton(
                        iconRes = R.drawable.ic_more_vert,
                        contentDescription = stringResource(R.string.cs_library_settings),
                        onClick = { menuExpanded = true },
                        size = 28.dp,
                        iconSize = 15.dp
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(CsColors.CardElevated)
                            .width(200.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (version.pinnedState) R.string.cs_library_unpin else R.string.cs_library_pin
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CsColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                version.setPinnedAndSave(!version.pinnedState)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.cs_library_settings),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CsColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mcVersion != null) CsTag(text = mcVersion)
                if (loaderName != null) {
                    CsTag(text = loaderName, accent = true)
                } else {
                    CsTag(text = stringResource(R.string.cs_library_tab_vanilla))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CsPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cs_library_launch),
                onClick = onLaunch,
                iconRes = R.drawable.ic_play_arrow_filled
            )
        }
    }
}

/** Instance icon: the version's own icon file when present, otherwise the default glyph. */
@Composable
fun InstanceIcon(
    version: Version,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val iconFile = remember(version) {
        version.getVersionIconFile().takeIf { it.exists() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(CsColors.CardElevated)
            .border(1.dp, CsColors.Border, RoundedCornerShape(size / 4)),
        contentAlignment = Alignment.Center
    ) {
        if (iconFile != null) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = iconFile,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                modifier = Modifier.size(size * 0.5f),
                painter = painterResource(R.drawable.ic_sports_esports_filled),
                contentDescription = null,
                tint = CsColors.TextSecondary
            )
        }
    }
}
