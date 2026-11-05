/*
 * CS Launcher Lite
 *
 * Derived from Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.cs.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionType
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.CsPrimaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsTabs
import com.movtery.zalithlauncher.ui.cs.components.CsTag
import com.movtery.zalithlauncher.ui.cs.theme.CsColors

/** 库的筛选标签：按版本类型过滤，与本地实例数据模型一致 */
private enum class LibraryFilter(val labelRes: Int) {
    ALL(R.string.cs_library_tab_all),
    MODDED(R.string.cs_library_tab_modded),
    VANILLA(R.string.cs_library_tab_vanilla)
}

/**
 * 库屏幕：所有本地实例；没有实例时居中显示说明与"创建新实例"按钮
 */
@Composable
fun CsLibraryScreen(
    onLaunch: (Version) -> Unit,
    onOpenSettings: (Version) -> Unit,
    onCreateInstance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    var filterIndex by remember { mutableIntStateOf(0) }
    val filter = LibraryFilter.entries[filterIndex]

    val filtered = remember(versions, filter) {
        when (filter) {
            LibraryFilter.ALL -> versions
            LibraryFilter.MODDED -> versions.filter { it.versionType == VersionType.MODLOADERS }
            LibraryFilter.VANILLA -> versions.filter { it.versionType != VersionType.MODLOADERS }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = stringResource(R.string.cs_nav_library),
                style = MaterialTheme.typography.headlineMedium,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (versions.isNotEmpty()) {
                CsTag(text = stringResource(R.string.cs_library_instances_count, versions.size))
            }
            Spacer(modifier = Modifier.weight(1f))
            CsPrimaryButton(
                text = stringResource(R.string.cs_library_create_instance),
                onClick = onCreateInstance,
                iconRes = R.drawable.ic_add,
                compact = true
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CsTabs(
            labels = LibraryFilter.entries.map { stringResource(it.labelRes) },
            selectedIndex = filterIndex,
            onSelected = { filterIndex = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                LibraryEmptyState(
                    isFirstRun = versions.isEmpty(),
                    onCreateInstance = onCreateInstance
                )
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(minSize = 240.dp),
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
private fun LibraryEmptyState(
    isFirstRun: Boolean,
    onCreateInstance: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 空状态插画：柔和的祖母绿光晕 + 层叠图形，完全由 Compose 绘制
        Box(
            modifier = Modifier.size(148.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                CsColors.AccentSoft,
                                CsColors.Background
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(CsColors.Card)
                    .border(1.dp, CsColors.Border, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(42.dp),
                    painter = painterResource(R.drawable.ic_cs_layers),
                    contentDescription = null,
                    tint = CsColors.Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.cs_library_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.fillMaxWidth(0.9f),
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

        Spacer(modifier = Modifier.height(22.dp))

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

    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenSettings
    ) {
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
                        contentDescription = null,
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
                                        if (version.pinnedState) {
                                            R.string.cs_library_unpin
                                        } else {
                                            R.string.cs_library_pin
                                        }
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
                if (mcVersion != null) {
                    CsTag(text = mcVersion)
                }
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
