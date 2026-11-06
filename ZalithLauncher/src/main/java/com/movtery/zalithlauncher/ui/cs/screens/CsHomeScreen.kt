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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
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
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsDivider
import com.movtery.zalithlauncher.ui.cs.components.CsEmptyState
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
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
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace

/**
 * Home.
 *
 * Layout contract:
 *  - the left navigation rail belongs to `CsShell` and is untouched here;
 *  - of the remaining space, the main column takes ~72% and the right activity panel ~28%;
 *  - the modpack grid is a compact 2-column grid of exactly 12 entries;
 *  - the main column and the right panel scroll independently;
 *  - the right panel never drops below the grid and never disappears while the grid scrolls.
 */
@Composable
fun CsHomeScreen(
    onInstall: (CsAddonItem) -> Unit,
    onOpenDiscover: () -> Unit,
    onManageAccounts: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CsHomeViewModel = viewModel(key = "cs_home")
) {
    HomeSplitLayout(
        modifier = modifier.background(CsColors.Background),
        main = {
            HomeMainContent(
                featured = viewModel.featured,
                onInstall = onInstall,
                onOpenDiscover = onOpenDiscover,
                onRetry = { viewModel.loadFeatured() }
            )
        },
        side = {
            HomeSidePanel(
                onManageAccounts = onManageAccounts,
                onOpenMultiplayer = onOpenMultiplayer
            )
        }
    )
}

/* ------------------------------------------------------------------ */
/* Split layout: 72% content / 28% right panel                         */
/* ------------------------------------------------------------------ */

/**
 * Two-pane Home layout.
 *
 * Wide (>= 600dp of usable width): fixed 72 / 28 split, each pane scrolls on its own.
 * Narrow: the content keeps the full width and the activity panel is docked to the *right edge*
 * as a slim always-visible strip that expands into an overlay — it is never placed under the grid.
 */
@Composable
private fun HomeSplitLayout(
    main: @Composable () -> Unit,
    side: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Read every constraint-derived value before entering any layout scope:
        // BoxWithConstraintsScope members cannot be used from inside Row/Box content lambdas.
        val availableWidth = maxWidth
        val isWide = availableWidth >= 600.dp
        val sidePanelWidth = availableWidth * 0.28f
        val overlayPanelWidth = (availableWidth * 0.9f).coerceAtMost(340.dp)

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    main()
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(CsColors.Border)
                )
                Box(
                    modifier = Modifier
                        .width(sidePanelWidth)
                        .fillMaxHeight()
                        .background(CsColors.BackgroundSecondary)
                ) {
                    side()
                }
            }
        } else {
            var panelOpen by remember { mutableStateOf(false) }
            val panelWidth = overlayPanelWidth

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 54.dp)
                ) {
                    main()
                }

                // Always-visible right-edge dock: the panel lives on the right, never below the grid.
                if (panelOpen) {
                    // Scrim closes the overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CsColors.Background.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { panelOpen = false }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(panelWidth + 42.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        // Collapse handle
                        Column(
                            modifier = Modifier
                                .width(42.dp)
                                .fillMaxHeight()
                                .background(CsColors.BackgroundSecondary),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CsIconButton(
                                iconRes = R.drawable.ic_cs_chevron_right,
                                contentDescription = stringResource(R.string.cs_home_side_close),
                                onClick = { panelOpen = false },
                                background = CsColors.CardElevated,
                                borderColor = CsColors.Border,
                                tint = CsColors.TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(panelWidth)
                                .fillMaxHeight()
                                .background(CsColors.BackgroundSecondary)
                        ) {
                            side()
                        }
                    }
                } else {
                    SideDock(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(54.dp)
                            .fillMaxHeight(),
                        onExpand = { panelOpen = true }
                    )
                }
            }
        }
    }
}

/**
 * Collapsed right-edge strip for narrow screens: avatar, news and an expand affordance.
 * Keeps the activity panel permanently reachable on the right side.
 */
@Composable
private fun SideDock(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .background(CsColors.BackgroundSecondary)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand
                ),
            contentAlignment = Alignment.Center
        ) {
            AccountAvatar(account = currentAccount, size = 34.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        CsIconButton(
            iconRes = R.drawable.ic_article_outlined,
            contentDescription = stringResource(R.string.cs_home_side_open),
            onClick = onExpand,
            background = CsColors.CardElevated,
            borderColor = CsColors.Border
        )

        Spacer(modifier = Modifier.height(8.dp))

        CsIconButton(
            iconRes = R.drawable.ic_keyboard_arrow_left,
            contentDescription = stringResource(R.string.cs_home_side_open),
            onClick = onExpand,
            background = CsColors.AccentSoft,
            borderColor = CsColors.AccentDark,
            tint = CsColors.Accent
        )
    }
}

/* ------------------------------------------------------------------ */
/* Main column                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun HomeMainContent(
    featured: CsFeaturedState,
    onInstall: (CsAddonItem) -> Unit,
    onOpenDiscover: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cs_home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cs_home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = CsColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CsSectionHeader(
            title = stringResource(R.string.cs_home_recommended),
            actionText = stringResource(R.string.cs_home_view_all),
            onAction = onOpenDiscover
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeaturedGrid(
            state = featured,
            onInstall = onInstall,
            onRetry = onRetry
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Exactly twelve entries, two per row, compact cards with landscape covers.
 * Availability of two full rows in the first viewport is handled by the compact card height.
 */
@Composable
private fun FeaturedGrid(
    state: CsFeaturedState,
    onInstall: (CsAddonItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is CsFeaturedState.Loading -> FeaturedSkeleton(modifier = modifier)

        is CsFeaturedState.Error -> CsCard(modifier = modifier.fillMaxWidth()) {
            CsEmptyState(
                modifier = Modifier.padding(vertical = 22.dp, horizontal = 12.dp),
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

        is CsFeaturedState.Empty -> CsCard(modifier = modifier.fillMaxWidth()) {
            CsEmptyState(
                modifier = Modifier.padding(vertical = 22.dp, horizontal = 12.dp),
                title = stringResource(R.string.cs_home_no_features),
                description = stringResource(R.string.cs_discover_source_note)
            )
        }

        is CsFeaturedState.Loaded -> BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val spacing = 10.dp
            // Two columns as soon as each card can be at least 232dp wide; otherwise one column.
            val columns = if (maxWidth >= 476.dp) 2 else 1
            val cellWidth = if (columns == 2) (maxWidth - spacing) / 2 else maxWidth
            val coverHeight = coverHeightFor(cellWidth)

            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                state.items.chunked(columns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rowItems.forEach { item ->
                            ModpackCard(
                                modifier = Modifier.width(cellWidth),
                                item = item,
                                coverHeight = coverHeight,
                                onOpen = { onInstall(item) }
                            )
                        }
                        // Keep the last row aligned to the grid when it holds a single card
                        if (rowItems.size < columns) {
                            Spacer(modifier = Modifier.width(cellWidth))
                        }
                    }
                }
            }
        }
    }
}

/** Landscape cover ratio, clamped so cards stay compact on both small and large widths. */
private fun coverHeightFor(cellWidth: Dp): Dp =
    (cellWidth * 0.44f).coerceIn(84.dp, 122.dp)

@Composable
private fun FeaturedSkeleton(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 10.dp
        val columns = if (maxWidth >= 476.dp) 2 else 1
        val cellWidth = if (columns == 2) (maxWidth - spacing) / 2 else maxWidth
        val coverHeight = coverHeightFor(cellWidth)

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
                            Column(modifier = Modifier.padding(10.dp)) {
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(12.dp)
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                CsSkeletonBlock(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
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
 * Compact modpack card: landscape cover, icon + title, two-line description, stats and Install.
 *
 * Every text is line-clamped and the description block has a fixed minimum height, so all cards in
 * a row share the same height and nothing can overflow or misalign.
 */
@Composable
private fun ModpackCard(
    item: CsAddonItem,
    coverHeight: Dp,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    CsCard(
        modifier = modifier,
        onClick = onOpen
    ) {
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
                        .alpha(0.34f)
                        .blur(14.dp),
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            // Soft vignette so the cover blends into the card and the badge stays readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CsColors.Card.copy(alpha = 0.35f))
                        )
                    )
            )

            val badge = item.loaders.firstOrNull()
                ?: item.categories.firstOrNull()?.let { stringResource(it.getDisplayName()) }
            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CsColors.Background.copy(alpha = 0.74f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
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
        }

        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CsProjectIcon(
                    modifier = Modifier.size(26.dp),
                    iconUrl = item.iconUrl,
                    name = item.title,
                    cornerRadius = 7.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                modifier = Modifier.heightIn(min = 30.dp),
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = CsColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CsStatItem(
                    iconRes = R.drawable.ic_download,
                    text = formatCount(item.downloadCount)
                )
                val follows = item.follows ?: 0L
                if (follows > 0L) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CsStatItem(
                        iconRes = R.drawable.ic_favorite_outlined,
                        text = formatCount(follows)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                CsPrimaryButton(
                    text = stringResource(R.string.cs_discover_install),
                    onClick = onOpen,
                    iconRes = R.drawable.ic_download,
                    compact = true
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Right panel                                                         */
/* ------------------------------------------------------------------ */

/**
 * The activity panel: account on top, friends below it, news in the middle and at the bottom.
 * It owns its own scroll state so it never moves with the modpack grid.
 */
@Composable
private fun HomeSidePanel(
    onManageAccounts: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        // ---------------- Account (always first, never scrolled away by the grid) ----------------
        Text(
            text = stringResource(R.string.cs_home_playing_as),
            style = MaterialTheme.typography.titleMedium,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        AccountCard(onManageAccounts = onManageAccounts)

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------- Friends ----------------
        Text(
            text = stringResource(R.string.cs_home_friends),
            style = MaterialTheme.typography.titleMedium,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        FriendsCard(
            onManageAccounts = onManageAccounts,
            onOpenMultiplayer = onOpenMultiplayer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---------------- News ----------------
        Text(
            text = stringResource(R.string.cs_home_news),
            style = MaterialTheme.typography.titleMedium,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        NewsList()
    }
}

/**
 * Real account state: avatar, username, account type and an account-selection dropdown.
 */
@Composable
private fun AccountCard(onManageAccounts: () -> Unit) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

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
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAccount?.username
                            ?: stringResource(R.string.cs_home_select_account),
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentAccount != null) CsColors.Accent else CsColors.TextTertiary
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentAccount?.let { getAccountTypeName(it) }
                                ?: stringResource(R.string.cs_profile_offline),
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
                    contentDescription = stringResource(R.string.cs_home_account_select),
                    tint = CsColors.TextSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(CsColors.CardElevated)
                .widthIn(min = 220.dp, max = 280.dp)
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
}

/**
 * Friends section. No friend graph exists in this launcher, so this reports the real state instead
 * of inventing one: a sign-in prompt for local/offline accounts, or a multiplayer entry point once
 * a Microsoft account is active.
 */
@Composable
private fun FriendsCard(
    onManageAccounts: () -> Unit,
    onOpenMultiplayer: () -> Unit
) {
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val signedIn = currentAccount?.isMicrosoftAccount() == true

    CsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (signedIn) CsColors.AccentSoft else CsColors.CardElevated)
                        .border(
                            1.dp,
                            if (signedIn) CsColors.AccentDark else CsColors.Border,
                            RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(
                            if (signedIn) R.drawable.ic_group_filled else R.drawable.ic_login
                        ),
                        contentDescription = null,
                        tint = if (signedIn) CsColors.Accent else CsColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        if (signedIn) R.string.cs_home_friends_online_title
                        else R.string.cs_home_friends_signin_title
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = stringResource(
                    if (signedIn) R.string.cs_home_friends_online_desc
                    else R.string.cs_home_friends_signin_desc
                ),
                style = MaterialTheme.typography.bodySmall,
                color = CsColors.TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(9.dp))

            CsSecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    if (signedIn) R.string.cs_home_friends_online_action
                    else R.string.cs_home_friends_signin_action
                ),
                onClick = if (signedIn) onOpenMultiplayer else onManageAccounts,
                iconRes = if (signedIn) R.drawable.ic_group_filled else R.drawable.ic_person_outlined,
                compact = true
            )
        }
    }
}

/**
 * News list built from the launcher's real Minecraft version manifest (release channel),
 * newest first, each entry with a generated thumbnail, title and description.
 */
@Composable
private fun NewsList() {
    val versions by MinecraftVersions.allVersions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        runCatching { MinecraftVersions.refreshVersions(force = false) }
    }

    if (versions.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                CsCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CsSkeletonBlock(
                            modifier = Modifier.size(width = 48.dp, height = 48.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            CsSkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(11.dp)
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            CsSkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val releases = remember(versions) {
        versions.filter { it.type == MinecraftVersion.Type.Release }.take(6)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        releases.forEach { release ->
            val id = release.version.id
            val gradient = remember(id) { CsColors.coverGradientFor(id) }

            CsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail: stable per-release gradient cover with the version badge
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 48.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(listOf(gradient.first, gradient.second)))
                            .border(1.dp, CsColors.Border, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = id,
                            style = MaterialTheme.typography.labelMedium,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cs_home_news_release, id),
                            style = MaterialTheme.typography.titleSmall,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                R.string.cs_home_news_desc,
                                release.version.releaseTime.take(10)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    CsTag(
                        text = stringResource(R.string.cs_home_news_release_tag),
                        accent = true
                    )
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
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(10.dp))
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
