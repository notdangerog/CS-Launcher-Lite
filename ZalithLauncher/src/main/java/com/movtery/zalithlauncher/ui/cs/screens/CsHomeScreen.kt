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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsDivider
import com.movtery.zalithlauncher.ui.cs.components.CsEmptyState
import com.movtery.zalithlauncher.ui.cs.components.CsLoadingBlock
import com.movtery.zalithlauncher.ui.cs.components.CsProjectIcon
import com.movtery.zalithlauncher.ui.cs.components.CsRemoteImage
import com.movtery.zalithlauncher.ui.cs.components.CsSecondaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsSectionHeader
import com.movtery.zalithlauncher.ui.cs.components.CsSkeletonBlock
import com.movtery.zalithlauncher.ui.cs.components.CsStatItem
import com.movtery.zalithlauncher.ui.cs.components.CsTag
import com.movtery.zalithlauncher.ui.cs.components.formatCount
import com.movtery.zalithlauncher.ui.cs.data.CsAddonItem
import com.movtery.zalithlauncher.ui.cs.data.CsFeaturedState
import com.movtery.zalithlauncher.ui.cs.data.CsHomeViewModel
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace

/**
 * 首页：左侧主内容（欢迎语 + 2 × 2 精选整合包网格），右侧账号/好友/新闻信息栏
 *
 * 宽屏（≥ 880dp）为左右两栏；窄屏时信息栏内容移动到主内容下方同一滚动容器里，
 * 保证任何屏幕尺寸下都不会出现重叠或被裁切的元素。
 */
@Composable
fun CsHomeScreen(
    onOpenDiscover: () -> Unit,
    onOpenProject: (CsAddonItem) -> Unit,
    onManageAccounts: () -> Unit,
    onOpenInstance: (Version) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: CsHomeViewModel = viewModel(key = "cs_home")
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
    ) {
        val isWide = maxWidth >= 880.dp
        // 右侧信息栏宽度：可用宽度的 29%，并限制在合理区间内
        val sideWidth = (maxWidth * 0.29f).coerceIn(280.dp, 360.dp)

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    HomeMainContent(
                        homeViewModel = homeViewModel,
                        onOpenDiscover = onOpenDiscover,
                        onOpenProject = onOpenProject,
                        onOpenInstance = onOpenInstance
                    )
                }

                Box(
                    modifier = Modifier
                        .width(sideWidth)
                        .fillMaxHeight()
                        .background(CsColors.BackgroundSecondary)
                ) {
                    HomeSidePanel(
                        onManageAccounts = onManageAccounts,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    HomeMainContent(
                        homeViewModel = homeViewModel,
                        onOpenDiscover = onOpenDiscover,
                        onOpenProject = onOpenProject,
                        onOpenInstance = onOpenInstance
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CsColors.BackgroundSecondary)
                ) {
                    HomeSidePanel(
                        onManageAccounts = onManageAccounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 主内容                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun ColumnScope.HomeMainContent(
    homeViewModel: CsHomeViewModel,
    onOpenDiscover: () -> Unit,
    onOpenProject: (CsAddonItem) -> Unit,
    onOpenInstance: (Version) -> Unit
) {
    Text(
        text = stringResource(R.string.cs_home_welcome_back),
        style = MaterialTheme.typography.headlineMedium,
        color = CsColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(18.dp))

    CsSectionHeader(
        title = stringResource(R.string.cs_home_featured_modpacks),
        actionText = stringResource(R.string.cs_nav_discover),
        onAction = onOpenDiscover
    )

    Spacer(modifier = Modifier.height(12.dp))

    FeaturedGrid(
        state = homeViewModel.featured,
        onOpenProject = onOpenProject,
        onRetry = { homeViewModel.loadFeatured() }
    )

    Spacer(modifier = Modifier.height(26.dp))

    InstancesStrip(onOpenInstance = onOpenInstance)
}

/**
 * 严格的 2 列 × 2 行网格。
 *
 * 卡片高度完全由固定尺寸构成（封面高度 = 卡片宽度 × 0.5，文字区域固定行数），
 * 因此同一行的两张卡片尺寸必然一致，也不会出现文字把卡片撑开的情况。
 */
@Composable
private fun FeaturedGrid(
    state: CsFeaturedState,
    onOpenProject: (CsAddonItem) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is CsFeaturedState.Loading -> FeaturedSkeleton()
        is CsFeaturedState.Error -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            CsEmptyState(
                title = stringResource(R.string.cs_home_modpacks_error),
                description = stringResource(R.string.cs_discover_source_note),
                action = {
                    CsSecondaryButton(
                        text = stringResource(R.string.cs_home_retry),
                        onClick = onRetry,
                        iconRes = R.drawable.ic_refresh,
                        compact = true
                    )
                }
            )
        }

        is CsFeaturedState.Empty -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            CsEmptyState(
                title = stringResource(R.string.cs_home_no_features),
                description = stringResource(R.string.cs_discover_source_note)
            )
        }

        is CsFeaturedState.Loaded -> BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 14.dp
            val cardWidth = (maxWidth - spacing) / 2
            // 封面高度跟随卡片宽度，保证不同屏幕尺寸下比例一致
            val coverHeight = (cardWidth * 0.5f).coerceIn(96.dp, 168.dp)
            val rows = state.items.chunked(2)

            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                rows.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        rowItems.forEach { item ->
                            FeaturedModpackCard(
                                modifier = Modifier.width(cardWidth),
                                item = item,
                                coverHeight = coverHeight,
                                onClick = { onOpenProject(item) }
                            )
                        }
                        // 不足两个时补齐空位，保证左对齐
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.width(cardWidth))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedSkeleton() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 14.dp
        val cardWidth = (maxWidth - spacing) / 2
        val coverHeight = (cardWidth * 0.5f).coerceIn(96.dp, 168.dp)
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(2) {
                        CsCard(modifier = Modifier.width(cardWidth)) {
                            CsSkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(coverHeight),
                                cornerRadius = 0.dp
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CsSkeletonBlock(
                                        modifier = Modifier.size(38.dp),
                                        cornerRadius = 10.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    CsSkeletonBlock(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedModpackCard(
    item: CsAddonItem,
    coverHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CsCard(modifier = modifier, onClick = onClick) {
        // 封面：渐变底 + 图标虚化放大 + 居中清晰图标，任何素材都不会被拉伸
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight)
        ) {
            val gradient = remember(item.title) { CsColors.coverGradientFor(item.title) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(gradient.first, gradient.second)))
            )
            if (!item.iconUrl.isNullOrEmpty()) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.32f)
                        .blur(12.dp),
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                AsyncImage(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }
            // 底部渐变过渡到卡片色
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                CsColors.Card.copy(alpha = 0.55f),
                                CsColors.Card
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CsProjectIcon(
                    iconUrl = item.iconUrl,
                    name = item.title,
                    modifier = Modifier.size(38.dp),
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = CsColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CsStatItem(
                    iconRes = R.drawable.ic_download,
                    text = formatCount(item.downloadCount)
                )
                val follows = item.follows ?: 0L
                if (follows > 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    CsStatItem(
                        iconRes = R.drawable.ic_favorite_outlined,
                        text = formatCount(follows)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val tag = item.loaders.firstOrNull() ?: item.categories.firstOrNull()?.let { code ->
                    stringResource(code.getDisplayName())
                }
                if (tag != null) {
                    CsTag(text = tag, accent = true)
                }
            }
        }
    }
}

/**
 * 已有实例的快速入口（真实数据，点击进入实例设置）
 */
@Composable
private fun InstancesStrip(onOpenInstance: (Version) -> Unit) {
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    if (versions.isEmpty()) return

    CsSectionHeader(title = stringResource(R.string.cs_home_instances_heading))
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        versions.take(8).forEach { version ->
            CsCard(
                modifier = Modifier.width(180.dp),
                onClick = { onOpenInstance(version) }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InstanceIcon(version = version, size = 32.dp)
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = version.getVersionName(),
                            style = MaterialTheme.typography.titleSmall,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = version.getVersionInfo()?.getInfoString() ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 实例图标（使用版本自带的图标文件，没有则显示默认图标）
 */
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

/* ------------------------------------------------------------------ */
/* 右侧信息栏                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun HomeSidePanel(
    onManageAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AccountPanel(onManageAccounts = onManageAccounts)

        Spacer(modifier = Modifier.height(20.dp))

        FriendsPanel(onManageAccounts = onManageAccounts)

        Spacer(modifier = Modifier.height(20.dp))

        NewsPanel()
    }
}

@Composable
private fun AccountPanel(onManageAccounts: () -> Unit) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.cs_home_playing_as),
        style = MaterialTheme.typography.titleMedium,
        color = CsColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(10.dp))

    Box {
        CsCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountAvatar(account = currentAccount, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAccount?.username ?: stringResource(R.string.cs_home_select_account),
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentAccount?.let { getAccountTypeName(it) }
                            ?: stringResource(R.string.cs_home_no_account),
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                    contentDescription = null,
                    tint = CsColors.TextSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(CsColors.CardElevated)
                .width(260.dp)
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountAvatar(account = account, size = 22.dp)
                            Spacer(modifier = Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (account.uniqueUUID == currentAccount?.uniqueUUID) {
                                        CsColors.Accent
                                    } else {
                                        CsColors.TextPrimary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = getAccountTypeName(account),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CsColors.TextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        AccountsManager.setCurrentAccount(account)
                    }
                )
            }
            if (accounts.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.cs_home_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CsColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onManageAccounts()
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(15.dp),
                            painter = painterResource(R.drawable.ic_person_outlined),
                            contentDescription = null,
                            tint = CsColors.Accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cs_home_manage_accounts),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CsColors.Accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onManageAccounts()
                }
            )
        }
    }
}

@Composable
private fun AccountAvatar(account: Account?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(CsColors.CardElevated),
        contentAlignment = Alignment.Center
    ) {
        if (account != null) {
            PlayerFace(
                modifier = Modifier.fillMaxSize(),
                account = account,
                avatarSize = size
            )
        } else {
            Icon(
                modifier = Modifier.size(size * 0.55f),
                painter = painterResource(R.drawable.ic_person_filled),
                contentDescription = null,
                tint = CsColors.TextSecondary
            )
        }
    }
}

@Composable
private fun FriendsPanel(onManageAccounts: () -> Unit) {
    Text(
        text = stringResource(R.string.cs_home_friends),
        style = MaterialTheme.typography.titleMedium,
        color = CsColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.cs_home_friends_empty),
        style = MaterialTheme.typography.bodySmall,
        color = CsColors.TextSecondary,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(10.dp))
    CsSecondaryButton(
        text = stringResource(R.string.cs_home_manage_accounts),
        onClick = onManageAccounts,
        iconRes = R.drawable.ic_person_outlined,
        compact = true
    )
}

/**
 * 新闻卡片：使用 Minecraft 官方版本清单中的真实发布数据
 */
@Composable
private fun NewsPanel() {
    val versions by MinecraftVersions.allVersions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        runCatching { MinecraftVersions.refreshVersions(force = false) }
    }

    Text(
        text = stringResource(R.string.cs_home_news),
        style = MaterialTheme.typography.titleMedium,
        color = CsColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (versions.isEmpty()) {
        CsLoadingBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
        return
    }

    val releases = remember(versions) {
        versions
            .filter { it.type == MinecraftVersion.Type.Release }
            .take(4)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        releases.forEach { release ->
            CsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 缩略图：渐变底 + 版本号，无需额外网络资源
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 46.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CsColors.AccentDark,
                                        CsColors.Background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = release.version.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Minecraft ${release.version.id}",
                            style = MaterialTheme.typography.titleSmall,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = release.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.Accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = release.version.releaseTime.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    CsDivider()
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.cs_discover_source_note),
        style = MaterialTheme.typography.labelSmall,
        color = CsColors.TextTertiary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
