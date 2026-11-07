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

package com.movtery.zalithlauncher.ui.cs.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.csPressScale
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.cs.theme.CsSpacing

/**
 * The sidebar destinations. Home, Discover and Library form the top group;
 * Settings is pinned to the bottom (Exit sits below it as an action, not a destination).
 */
enum class CsDestination(val labelRes: Int, val iconRes: Int) {
    HOME(R.string.cs_nav_home, R.drawable.ic_home_filled),
    DISCOVER(R.string.cs_nav_discover, R.drawable.ic_cs_explore),
    LIBRARY(R.string.cs_nav_library, R.drawable.ic_photo_library_outlined),
    SETTINGS(R.string.cs_nav_settings, R.drawable.ic_settings_filled)
}

/** The three destinations grouped together at the top of the rail. */
private val topDestinations = listOf(
    CsDestination.HOME,
    CsDestination.DISCOVER,
    CsDestination.LIBRARY
)

/** Extra tools that are not primary destinations (engine features, kept out of the rail). */
data class CsMenuItem(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

/**
 * The single application shell: ultra-slim icon-only rail + slim top bar + content area.
 *
 * This is the only navigation surface in the app — no second rail, no tab bar, no duplicated buttons.
 */
@Composable
fun CsShell(
    selected: CsDestination?,
    onNavigate: (CsDestination) -> Unit,
    onExit: () -> Unit,
    onBack: () -> Unit,
    title: String,
    canGoBack: Boolean,
    runningTaskCount: Int,
    onOpenTasks: () -> Unit,
    menuItems: List<CsMenuItem>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
    ) {
        CsNavRail(
            selected = selected,
            onNavigate = onNavigate,
            onExit = onExit
        )

        Column(modifier = Modifier.fillMaxSize()) {
            CsTopBar(
                title = title,
                canGoBack = canGoBack,
                onBack = onBack,
                runningTaskCount = runningTaskCount,
                onOpenTasks = onOpenTasks,
                menuItems = menuItems
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                content()
            }
        }
    }
}

/**
 * Ultra-slim rail: thin-line icons only, no text labels.
 * The active destination is highlighted with a rounded green capsule behind the icon.
 */
@Composable
private fun CsNavRail(
    selected: CsDestination?,
    onNavigate: (CsDestination) -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .fillMaxHeight()
            .background(CsColors.BackgroundSecondary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Brand mark (identity, not navigation)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(CsColors.AccentSoft)
                .border(1.dp, CsColors.AccentDark, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(19.dp),
                painter = painterResource(R.drawable.ic_cs_logo),
                contentDescription = stringResource(R.string.cs_app_name),
                tint = CsColors.Accent
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main navigation: Home, Discover, Library — grouped, compact spacing
        topDestinations.forEach { destination ->
            CsRailItem(
                destination = destination,
                selected = destination == selected,
                onClick = { onNavigate(destination) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom: Settings, then Exit
        CsRailItem(
            destination = CsDestination.SETTINGS,
            selected = selected == CsDestination.SETTINGS,
            onClick = { onNavigate(CsDestination.SETTINGS) }
        )
        CsRailActionItem(
            iconRes = R.drawable.ic_exit_to_app,
            contentDescription = stringResource(R.string.cs_nav_exit),
            tint = CsColors.TextSecondary,
            onClick = onExit
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Rail entry that performs an action (Exit) instead of navigating. */
@Composable
private fun CsRailActionItem(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(30.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .csPressScale(interactionSource, pressedScale = 0.9f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Composable
private fun CsRailItem(
    destination: CsDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // "Sleek rounded green capsule" for the active tab
    val capsuleColor by animateColorAsState(
        targetValue = if (selected) CsColors.AccentSoft else Color.Transparent,
        label = "csRailCapsule"
    )
    val capsuleWidth by animateDpAsState(
        targetValue = if (selected) 30.dp else 22.dp,
        label = "csRailCapsuleWidth"
    )
    val capsuleHeight by animateDpAsState(
        targetValue = if (selected) 40.dp else 30.dp,
        label = "csRailCapsuleHeight"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) CsColors.Accent else CsColors.TextSecondary,
        label = "csRailTint"
    )

    Box(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(capsuleWidth)
                .height(capsuleHeight)
                .clip(CircleShape)
                .background(capsuleColor)
                .then(
                    if (selected) {
                        Modifier.border(1.dp, CsColors.AccentDark, CircleShape)
                    } else Modifier
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .csPressScale(interactionSource, pressedScale = 0.9f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(19.dp),
                painter = painterResource(destination.iconRes),
                contentDescription = stringResource(destination.labelRes),
                tint = tint
            )
        }
    }
}

@Composable
private fun CsTopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    runningTaskCount: Int,
    onOpenTasks: () -> Unit,
    menuItems: List<CsMenuItem>
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(CsSpacing.topBarHeight),
        color = CsColors.Background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = canGoBack,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CsIconButton(
                        iconRes = R.drawable.ic_arrow_back,
                        contentDescription = stringResource(R.string.generic_back),
                        onClick = onBack,
                        size = 30.dp,
                        iconSize = 16.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CsColors.BackgroundSecondary)
                    .border(1.dp, CsColors.Border, RoundedCornerShape(8.dp))
                    .clickable(enabled = runningTaskCount > 0, onClick = onOpenTasks)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (runningTaskCount > 0) CsColors.Accent else CsColors.TextTertiary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (runningTaskCount > 0) {
                        stringResource(R.string.cs_status_tasks, runningTaskCount)
                    } else {
                        stringResource(R.string.cs_status_idle)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (runningTaskCount > 0) CsColors.TextPrimary else CsColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (menuItems.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box {
                    CsIconButton(
                        iconRes = R.drawable.ic_more_vert,
                        contentDescription = stringResource(R.string.cs_more_menu),
                        onClick = { menuExpanded = true },
                        size = 30.dp,
                        iconSize = 16.dp
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(CsColors.CardElevated)
                            .widthIn(min = 190.dp, max = 260.dp)
                    ) {
                        menuItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            modifier = Modifier.size(15.dp),
                                            painter = painterResource(item.iconRes),
                                            contentDescription = null,
                                            tint = CsColors.TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CsColors.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    item.onClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
