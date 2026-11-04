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
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Top-level destinations — EXACTLY five, NO text labels, icon-only.
 * Spec: Home, Instances, Discover, Downloads, Settings
 */
enum class CsSection {
    HOME,
    INSTANCES,
    DISCOVER,
    DOWNLOADS,
    SETTINGS
}

/**
 * Ultra-slim vertical navigation bar — spec exact:
 * - ONLY minimalist thin-line icons, NO text names
 * - Active tab highlighted with sleek rounded green capsule shape
 * - Logo at top (same CS moon logo as app icon), Settings at bottom
 * - Thin vertical line divider after rail (handled by CsSectionDivider)
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
            .background(CsColor.Background)
            .padding(vertical = CsSpace.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CsRailLogo()

        Spacer(modifier = Modifier.height(CsSpace.l))

        CsRailItem(
            iconRes = R.drawable.ic_nav_home,
            contentDescription = stringResource(R.string.cs_nav_home),
            selected = selected == CsSection.HOME,
            onClick = { onNavigate(CsSection.HOME) }
        )
        CsRailItem(
            iconRes = R.drawable.ic_nav_instances,
            contentDescription = stringResource(R.string.cs_nav_instances),
            selected = selected == CsSection.INSTANCES,
            onClick = { onNavigate(CsSection.INSTANCES) }
        )
        CsRailItem(
            iconRes = R.drawable.ic_nav_discover,
            contentDescription = stringResource(R.string.cs_nav_discover),
            selected = selected == CsSection.DISCOVER,
            onClick = { onNavigate(CsSection.DISCOVER) }
        )
        CsRailItem(
            iconRes = R.drawable.ic_nav_downloads,
            contentDescription = stringResource(R.string.cs_nav_downloads),
            selected = selected == CsSection.DOWNLOADS,
            onClick = { onNavigate(CsSection.DOWNLOADS) }
        )

        Spacer(modifier = Modifier.weight(1f))

        CsRailItem(
            iconRes = R.drawable.ic_nav_settings,
            contentDescription = stringResource(R.string.cs_nav_settings),
            selected = selected == CsSection.SETTINGS,
            onClick = { onNavigate(CsSection.SETTINGS) }
        )

        Spacer(modifier = Modifier.height(CsSpace.xs))
    }
}

/**
 * Launcher mark at top of rail — exact same logo as app icon (crescent + CS + stars)
 * No text, no extra chrome, just the moon logo on charcoal.
 */
@Composable
private fun CsRailLogo() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CsColor.Background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(34.dp),
            painter = painterResource(R.drawable.ic_cs_logo),
            contentDescription = stringResource(R.string.cs_brand_name)
        )
    }
}

/**
 * One icon button with the sleek rounded green capsule for active state.
 * Spec: active tab highlighted with a rounded green capsule shape (pill).
 */
@Composable
private fun CsRailItem(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) CsColor.Accent else CsColor.Background,
        animationSpec = tween(180),
        label = "CsRailContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) Color.White else CsColor.TextSecondary,
        animationSpec = tween(180),
        label = "CsRailContent"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(CsSize.railCapsuleWidth)
            .height(CsSize.railCapsuleHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(CsSize.railIconSize),
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = content
        )
    }
}

/**
 * Divider between sections — thin vertical line, right divider slightly thicker.
 * Spec: thin vertical line separating rail from center, right divider slightly thicker.
 */
@Composable
fun CsSectionDivider(thick: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(if (thick) CsSize.dividerStrong else CsSize.dividerThin)
            .background(if (thick) CsColor.DividerStrong else CsColor.Divider)
    )
}

/**
 * Header of center column: section title + actions.
 * Clean, minimal — white primary, muted grey secondary.
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
    onAccount: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CsSize.topBarHeight)
            .background(CsColor.Background)
            .padding(start = CsSpace.l, end = CsSpace.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CsIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.generic_back),
                containerColor = CsColor.Card,
                contentColor = CsColor.TextPrimary,
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(CsSpace.m))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CsColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (detailTitle != null) {
                detailTitle()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColor.TextSecondary,
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
                    containerColor = CsColor.Accent.copy(alpha = 0.15f),
                    contentColor = CsColor.Accent,
                    onClick = onTaskClick
                )
            }
            CsIconButton(
                iconRes = R.drawable.ic_group_filled,
                contentDescription = stringResource(R.string.terracotta),
                containerColor = CsColor.Card,
                contentColor = CsColor.TextSecondary,
                onClick = onMultiplayer
            )
            CsIconButton(
                iconRes = R.drawable.ic_folder_filled,
                contentDescription = stringResource(R.string.cs_topbar_files),
                containerColor = CsColor.Card,
                contentColor = CsColor.TextSecondary,
                onClick = onFileManager
            )
            if (showAccountChip) {
                CsAccountChip(onClick = onAccount)
            }
        }
    }
}

/**
 * Maps current navigation key to its rail section.
 */
fun csSectionOf(key: TitledNavKey?): CsSection? = when (key) {
    null,
    NormalNavKey.LauncherMain -> CsSection.HOME

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

    NormalNavKey.Downloads -> CsSection.DOWNLOADS

    is NormalNavKey.Settings,
    is NestedNavKey.Settings -> CsSection.SETTINGS

    else -> null
}

/**
 * True when given key is a section root (rail destination).
 */
fun isCsSectionRoot(key: TitledNavKey?): Boolean = when (key) {
    null,
    NormalNavKey.LauncherMain,
    NormalNavKey.Instances,
    NormalNavKey.Discover,
    NormalNavKey.Downloads -> true
    is NestedNavKey.Settings -> true
    else -> false
}

@Composable
fun CsDetailTitle(text: AndroidStringText, modifier: Modifier = Modifier) {
    AndroidStringText(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(color = CsColor.TextSecondary),
        maxLines = 1
    )
}

@Composable
fun CsContentContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxWidth()) {
        content()
    }
}

val csRailWidth: Dp = CsSize.railWidth

@Composable
fun CsBrandCard(modifier: Modifier = Modifier) {
    CsCard(modifier = modifier, color = CsColor.Card) {
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
                color = CsColor.TextPrimary,
                maxLines = 1
            )
        }
    }
}
