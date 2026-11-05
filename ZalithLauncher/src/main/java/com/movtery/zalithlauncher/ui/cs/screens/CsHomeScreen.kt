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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsDivider
import com.movtery.zalithlauncher.ui.cs.components.CsEmptyState
import com.movtery.zalithlauncher.ui.cs.components.CsPrimaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsProjectIcon
import com.movtery.zalithlauncher.ui.cs.components.CsSecondaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsSectionHeader
import com.movtery.zalithlauncher.ui.cs.components.CsSkeletonBlock
import com.movtery.zalithlauncher.ui.cs.components.CsStatItem
import com.movtery.zalithlauncher.ui.cs.components.CsTag
import com.movtery.zalithlauncher.ui.cs.components.formatCount
import com.movtery.zalithlauncher.ui.cs.data.CsAddonItem
import com.movtery.zalithlauncher.ui.cs.data.CsFeaturedState
import com.movtery.zalithlauncher.ui.cs.data.CsHomeViewModel
import com.movtery.zalithlauncher.ui.cs.shell.CsDashboardLayout
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace

/**
 * Home — the dashboard.
 *
 * Centre: large "Recommended Modpacks" banner cards with real Modrinth data and working Install actions.
 * Right: compact widget panel with the player's account status and a news feed.
 */
@Composable
fun CsHomeScreen(
    onInstall: (CsAddonItem) -> Unit,
    onOpenDiscover: () -> Unit,
    onManageAccounts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CsHomeViewModel = viewModel(key = "cs_home")
) {
    CsDashboardLayout(
        modifier = modifier.background(CsColors.Background),
        main = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.cs_home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.cs_home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = CsColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                CsSectionHeader(
                    title = stringResource(R.string.cs_home_recommended),
                    actionText = stringResource(R.string.cs_home_view_all),
                    onAction = onOpenDiscover
                )

                Spacer(modifier = Modifier.height(12.dp))

                RecommendedGrid(
                    state = viewModel.featured,
                    onInstall = onInstall,
                    onRetry = { viewModel.loadFeatured() }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        },
        side = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                ProfileWidget(onManageAccounts = onManageAccounts)
                Spacer(modifier = Modifier.height(18.dp))
                NewsWidget()
            }
        }
    )
}

/* ------------------------------------------------------------------ */
/* Recommended modpacks                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun RecommendedGrid(
    state: CsFeaturedState,
    onInstall: (CsAddonItem) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is CsFeaturedState.Loading -> RecommendedSkeleton()

        is CsFeaturedState.Error -> CsCard(modifier = Modifier.fillMaxWidth()) {
            CsEmptyState(
                modifier = Modifier.padding(vertical = 26.dp),
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

        is CsFeaturedState.Empty -> CsCard(modifier = Modifier.fillMaxWidth()) {
            CsEmptyState(
                modifier = Modifier.padding(vertical = 26.dp),
                title = stringResource(R.string.cs_home_no_features),
                description = stringResource(R.string.cs_discover_source_note)
            )
        }

        is CsFeaturedState.Loaded -> BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Capture layout values before entering Row/Column scopes
            val columns = if (maxWidth >= 760.dp) 2 else 1
            val spacing = 12.dp
            val cellWidth = if (columns == 2) (maxWidth - spacing) / 2 else maxWidth
            val coverHeight = if (columns == 2) 132.dp else 158.dp

            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                state.items.chunked(columns).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        rowItems.forEach { item ->
                            ModpackBannerCard(
                                modifier = Modifier.width(cellWidth),
                                item = item,
                                coverHeight = coverHeight,
                                compact = columns == 2,
                                onInstall = { onInstall(item) }
                            )
                        }
                        if (rowItems.size < columns) {
                            Spacer(modifier = Modifier.width(cellWidth))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedSkeleton() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 760.dp) 2 else 1
        val spacing = 12.dp
        val cellWidth = if (columns == 2) (maxWidth - spacing) / 2 else maxWidth
        val coverHeight = if (columns == 2) 132.dp else 158.dp

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(columns) {
                        CsCard(modifier = Modifier.width(cellWidth)) {
                            CsSkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(coverHeight),
                                cornerRadius = 0.dp
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(14.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Large preview banner card: screenshot-style cover, title, description, stats, green Install button.
 *
 * All text is line-clamped and the card height is derived from fixed pieces, so two cards in a row
 * always have identical dimensions and nothing can overflow.
 */
@Composable
private fun ModpackBannerCard(
    item: CsAddonItem,
    coverHeight: Dp,
    compact: Boolean,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    CsCard(modifier = modifier) {
        // Cover: gradient base + blurred icon as the "screenshot" backdrop + vignette into the card colour
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
                        .alpha(0.38f)
                        .blur(16.dp),
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                CsColors.Card.copy(alpha = 0.45f),
                                CsColors.Card
                            )
                        )
                    )
            )
            // Loader / category chip on the cover
            val badge = item.loaders.firstOrNull()
                ?: item.categories.firstOrNull()?.let { stringResource(it.getDisplayName()) }
            if (badge != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(CsColors.Background.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.Accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Project icon bottom-left, overlapping the cover/content boundary
            CsProjectIcon(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp)
                    .size(if (compact) 40.dp else 44.dp),
                iconUrl = item.iconUrl,
                name = item.title,
                cornerRadius = 11.dp
            )
        }

        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Text(
                modifier = Modifier
                    .padding(start = if (compact) 0.dp else 56.dp),
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                modifier = Modifier.heightIn(min = 32.dp),
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
                    Spacer(modifier = Modifier.width(9.dp))
                    CsStatItem(
                        iconRes = R.drawable.ic_favorite_outlined,
                        text = formatCount(follows)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                CsPrimaryButton(
                    text = stringResource(R.string.cs_discover_install),
                    onClick = onInstall,
                    iconRes = R.drawable.ic_download,
                    compact = true
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Right panel widgets                                                 */
/* ------------------------------------------------------------------ */

/**
 * Account widget — shows the real signed-in account or the offline/local state.
 */
@Composable
private fun ProfileWidget(onManageAccounts: () -> Unit) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.cs_profile_title),
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
                AccountAvatar(account = currentAccount, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAccount?.username ?: stringResource(R.string.cs_home_select_account),
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Status row: account type + online/offline marker
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (currentAccount != null) CsColors.Accent else CsColors.TextTertiary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentAccount != null) {
                                getAccountTypeName(currentAccount!!)
                            } else {
                                stringResource(R.string.cs_profile_offline)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentAccount != null) CsColors.Accent else CsColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                .width(250.dp)
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
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.cs_home_manage_accounts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CsColors.Accent,
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
    }

    Spacer(modifier = Modifier.height(10.dp))

    CsSecondaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.cs_home_manage_accounts),
        onClick = onManageAccounts,
        iconRes = R.drawable.ic_person_outlined,
        compact = true
    )
}

@Composable
private fun AccountAvatar(account: Account?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(CsColors.CardElevated)
            .border(1.dp, CsColors.Border, RoundedCornerShape(size / 4)),
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

/**
 * News widget — real Minecraft release entries from the launcher's version manifest.
 */
@Composable
private fun NewsWidget() {
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
        CsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                CsSkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                )
            }
        }
        return
    }

    val releases = remember(versions) {
        versions.filter { it.type == MinecraftVersion.Type.Release }.take(4)
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        releases.forEach { release ->
            CsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 44.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CsColors.AccentDark, CsColors.Background)
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
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cs_home_news_release, release.version.id),
                            style = MaterialTheme.typography.titleSmall,
                            color = CsColors.TextPrimary,
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
                    CsTag(text = stringResource(R.string.cs_home_news_release_tag), accent = true)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
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
