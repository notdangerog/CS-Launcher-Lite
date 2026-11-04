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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey

/**
 * Top level destinations of the CS Launcher Lite shell.
 *
 * The navigation rail only ever shows these; everything else is a detail screen
 * pushed on top of one of them.
 */
enum class CsSection {
    HOME,
    DISCOVER,
    INSTANCES,
    SETTINGS
}

/**
 * Icon-only navigation rail.
 *
 * The logo sits at the top, Home / Discover / Instances directly below it and the
 * settings gear is pinned to the bottom. Labels are intentionally omitted: the rail
 * stays narrow so the catalog keeps the width it needs on a landscape phone.
 */
@Composable
fun CsRail(
    selected: CsSection?,
    onNavigate: (CsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(CsSize.railWidth)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(vertical = CsSpace.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CsSpace.xs)
    ) {
        CsRailLogo()

        Spacer(modifier = Modifier.height(CsSpace.l))

        CsRailItem(
            iconRes = R.drawable.ic_home_filled,
            contentDescription = stringResource(R.string.cs_nav_home),
            selected = selected == CsSection.HOME,
            onClick = { onNavigate(CsSection.HOME) }
        )
        CsRailItem(
            iconRes = R.drawable.ic_search,
            contentDescription = stringResource(R.string.cs_nav_discover),
            selected = selected == CsSection.DISCOVER,
            onClick = { onNavigate(CsSection.DISCOVER) }
        )
        CsRailItem(
            iconRes = R.drawable.ic_box,
            contentDescription = stringResource(R.string.cs_nav_instances),
            selected = selected == CsSection.INSTANCES,
            onClick = { onNavigate(CsSection.INSTANCES) }
        )

        Spacer(modifier = Modifier.weight(1f))

        CsRailItem(
            iconRes = R.drawable.ic_settings_filled,
            contentDescription = stringResource(R.string.cs_nav_settings),
            selected = selected == CsSection.SETTINGS,
            onClick = { onNavigate(CsSection.SETTINGS) }
        )
    }
}

/**
 * Launcher mark shown at the very top of the rail.
 */
@Composable
private fun CsRailLogo() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(26.dp),
            painter = painterResource(R.drawable.ic_cs_logo),
            contentDescription = stringResource(R.string.cs_brand_name)
        )
    }
}

/**
 * One icon button of the rail with the animated selection indicator.
 */
@Composable
private fun CsRailItem(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = tween(durationMillis = 160),
        label = "CsRailContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 160),
        label = "CsRailContent"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val indicatorHeight by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CsRailIndicator"
    )

    Box(
        modifier = Modifier
            .size(CsSize.railItem)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // selection indicator on the left edge of the item
        if (indicatorHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .width(3.dp)
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = content
        )
    }
}

/**
 * Divider drawn between the three sections of the shell. The one in front of the
 * account panel is slightly thicker, the one after the rail is a hairline.
 */
@Composable
fun CsSectionDivider(thick: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(if (thick) CsSize.dividerStrong else CsSize.dividerThin)
            .background(if (thick) CsColor.DividerStrong else MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * Header of the center column: section title, optional back affordance and the
 * launcher wide actions (tasks, multiplayer, files, and the account chip when the
 * account panel is collapsed).
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
    showAccountChip: Boolean = false,
    onAccount: () -> Unit = {},
    onClassicHome: (() -> Unit)? = null
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
            if (onClassicHome != null) {
                CsIconButton(
                    iconRes = R.drawable.ic_dashboard_outlined,
                    contentDescription = stringResource(R.string.cs_classic_home),
                    onClick = onClassicHome
                )
            }
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
            if (showAccountChip) {
                CsAccountChip(onClick = onAccount)
            }
        }
    }
}

/**
 * Maps the current navigation key onto the rail entry that owns it, so nested
 * screens keep the right icon highlighted.
 */
fun csSectionOf(key: TitledNavKey?): CsSection? = when (key) {
    null,
    NormalNavKey.LauncherMain,
    NormalNavKey.ClassicHome -> CsSection.HOME

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

    NormalNavKey.Instances,
    NormalNavKey.VersionsManager,
    is NestedNavKey.VersionSettings,
    is NestedNavKey.VersionExport -> CsSection.INSTANCES

    is NormalNavKey.Settings,
    is NestedNavKey.Settings -> CsSection.SETTINGS

    else -> null
}

/**
 * Returns true when the given key is one of the section roots, i.e. a screen the rail
 * navigates to directly. Detail screens show a back affordance instead.
 */
fun isCsSectionRoot(key: TitledNavKey?): Boolean = when (key) {
    null,
    NormalNavKey.LauncherMain,
    NormalNavKey.Instances,
    NormalNavKey.Discover -> true
    is NestedNavKey.Settings -> true
    else -> false
}

/**
 * Small helper used by the top bar to render the title of a detail screen.
 */
@Composable
fun CsDetailTitle(text: AndroidStringText, modifier: Modifier = Modifier) {
    AndroidStringText(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1
    )
}

/**
 * Container used by screens that need a plain, padded column.
 */
@Composable
fun CsContentContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxWidth()) {
        content()
    }
}

/**
 * Width of the rail as a Dp value, exposed for tests and layout helpers.
 */
val csRailWidth: Dp = CsSize.railWidth

/**
 * Card used for the CS brand block on wide layouts.
 */
@Composable
fun CsBrandCard(modifier: Modifier = Modifier) {
    CsCard(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_cs_logo),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.cs_brand_name),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
