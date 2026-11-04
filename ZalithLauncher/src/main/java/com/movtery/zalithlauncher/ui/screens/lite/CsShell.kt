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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsNavItem
import com.movtery.zalithlauncher.ui.design.CsNavSectionLabel
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.ui.theme.backgroundColor

/**
 * Top level destinations of the CS Launcher Lite shell.
 */
enum class CsSection {
    HOME,
    INSTANCES,
    DISCOVER,
    LIBRARY,
    SETTINGS
}

/**
 * The permanent left navigation rail of CS Launcher Lite.
 */
@Composable
fun CsSidebar(
    selected: CsSection?,
    onNavigate: (CsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(CsSize.sidebarWidth)
            .padding(horizontal = CsSpace.m, vertical = CsSpace.m),
        verticalArrangement = Arrangement.spacedBy(CsSpace.xxs)
    ) {
        CsBrand()

        Spacer(modifier = Modifier.height(CsSpace.l))

        CsNavItem(
            label = stringResource(R.string.cs_nav_home),
            iconRes = R.drawable.ic_home_filled,
            selected = selected == CsSection.HOME,
            onClick = { onNavigate(CsSection.HOME) }
        )
        CsNavItem(
            label = stringResource(R.string.cs_nav_instances),
            iconRes = R.drawable.ic_sports_esports_outlined,
            selected = selected == CsSection.INSTANCES,
            onClick = { onNavigate(CsSection.INSTANCES) }
        )
        CsNavItem(
            label = stringResource(R.string.cs_nav_discover),
            iconRes = R.drawable.ic_public,
            selected = selected == CsSection.DISCOVER,
            onClick = { onNavigate(CsSection.DISCOVER) }
        )
        CsNavItem(
            label = stringResource(R.string.cs_nav_library),
            iconRes = R.drawable.ic_folder_zip_outlined,
            selected = selected == CsSection.LIBRARY,
            onClick = { onNavigate(CsSection.LIBRARY) }
        )
        CsNavItem(
            label = stringResource(R.string.cs_nav_settings),
            iconRes = R.drawable.ic_settings_filled,
            selected = selected == CsSection.SETTINGS,
            onClick = { onNavigate(CsSection.SETTINGS) }
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier = Modifier.padding(start = CsSpace.m, bottom = CsSpace.xs),
            text = stringResource(R.string.cs_brand_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            maxLines = 1
        )
        Text(
            modifier = Modifier.padding(start = CsSpace.m, bottom = CsSpace.s),
            text = stringResource(R.string.cs_brand_powered_by),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Launcher word mark + logo block.
 */
@Composable
private fun CsBrand(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = CsSpace.xs, top = CsSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
    ) {
        Image(
            modifier = Modifier.size(28.dp),
            painter = painterResource(R.drawable.ic_cs_logo),
            contentDescription = null
        )
        Column {
            Text(
                text = stringResource(R.string.cs_brand_name),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.cs_brand_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = CsColor.AccentMuted,
                maxLines = 1
            )
        }
    }
}

/**
 * Top bar of the shell: section title on the start side, global actions on the end side.
 */
@Composable
fun CsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detailTitle: (@Composable () -> Unit)? = null,
    taskRunning: Boolean = false,
    onBack: (() -> Unit)? = null,
    onTaskClick: () -> Unit = {},
    onMultiplayer: () -> Unit = {},
    onFileManager: () -> Unit = {},
    onAccount: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CsSize.topBarHeight)
            .padding(start = CsSpace.l, end = CsSpace.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CsIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.generic_back),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(CsSpace.m))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detailTitle != null) {
                detailTitle()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (taskRunning) {
                CsIconButton(
                    iconRes = R.drawable.ic_assignment_filled,
                    contentDescription = stringResource(R.string.main_task_menu),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = onTaskClick
                )
            }
            CsIconButton(
                iconRes = R.drawable.ic_group_filled,
                contentDescription = stringResource(R.string.terracotta),
                onClick = onMultiplayer
            )
            CsIconButton(
                iconRes = R.drawable.ic_folder_filled,
                contentDescription = stringResource(R.string.cs_topbar_files),
                onClick = onFileManager
            )
            AccountChip(onClick = onAccount)
        }
    }
}

/**
 * Compact account button: avatar, user name and account type.
 */
@Composable
private fun AccountChip(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val current = account

    CsCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = CsSpace.m,
            vertical = CsSpace.s
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            if (current != null) {
                PlayerFace(
                    account = current,
                    avatarSize = 24.dp
                )
                Column {
                    Text(
                        text = current.username,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = getAccountTypeName(current),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            } else {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_person_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.cs_account_sign_in),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Vertical scrolling container used by the CS screens so headers stay reachable
 * on short landscape screens.
 */
@Composable
fun CsScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CsSpace.l, vertical = CsSpace.s),
        verticalArrangement = Arrangement.spacedBy(CsSpace.m),
        content = content
    )
}

/**
 * Fills the remaining space below a header inside a screen.
 */
@Composable
fun CsContentContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxWidth()) {
        content()
    }
}

/**
 * Small helper returning the display name of an account or a placeholder.
 */
fun Account?.displayNameOrNull(): String? = this?.username

/**
 * Maps the current navigation key onto the sidebar section that owns it, so the
 * rail highlights the right entry for both top level and nested screens.
 */
fun csSectionOf(key: TitledNavKey?): CsSection? = when (key) {
    null,
    NormalNavKey.LauncherMain,
    NormalNavKey.ClassicHome -> CsSection.HOME

    NormalNavKey.Instances,
    NormalNavKey.VersionsManager,
    is NestedNavKey.VersionSettings,
    is NestedNavKey.VersionExport -> CsSection.INSTANCES

    NormalNavKey.Discover,
    NormalNavKey.SearchMod,
    NormalNavKey.SearchModPack,
    NormalNavKey.SearchResourcePack,
    NormalNavKey.SearchSaves,
    NormalNavKey.SearchShaders,
    NormalNavKey.SearchId,
    NormalNavKey.Favorites,
    is NormalNavKey.DownloadAssets,
    is NestedNavKey.Download -> CsSection.DISCOVER

    NormalNavKey.Library -> CsSection.LIBRARY

    is NormalNavKey.Settings,
    is NestedNavKey.Settings -> CsSection.SETTINGS

    else -> null
}

/**
 * Returns true when the given key is one of the section roots, i.e. the screen the
 * sidebar itself navigates to. Detail screens show a back affordance instead.
 */
fun isCsSectionRoot(key: TitledNavKey?): Boolean = when (key) {
    null,
    NormalNavKey.LauncherMain,
    NormalNavKey.Instances,
    NormalNavKey.Discover,
    NormalNavKey.Library -> true
    is NestedNavKey.Settings -> true
    else -> false
}
