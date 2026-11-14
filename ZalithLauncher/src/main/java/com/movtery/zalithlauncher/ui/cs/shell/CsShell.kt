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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.movtery.zalithlauncher.utils.animation.staggeredEnter
import com.movtery.zalithlauncher.utils.animation.BounceEasing
import com.movtery.zalithlauncher.utils.animation.PulseEasing
import com.movtery.zalithlauncher.utils.animation.QuickSpringEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.csPressScale
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.cs.theme.CsFonts
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

/**
 * Navigation label style: Minecraft-style pixel font, sized to fit inside the slim rail
 * (Monocraft advances 2/3 em per glyph, so "Discover" is ~37 dp at 7 sp — never clipped).
 */
private val railLabelStyle = TextStyle(
    fontFamily = CsFonts.Minecraft,
    fontWeight = FontWeight.Normal,
    fontSize = 7.sp,
    lineHeight = 9.sp,
    letterSpacing = 0.sp
)

/** Brand title style for the top header (pixel font, single line, no clipping). */
private val brandTitleStyle = TextStyle(
    fontFamily = CsFonts.Minecraft,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.sp
)

/** Extra tools that are not primary destinations (engine features, kept out of the rail). */
data class CsMenuItem(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

/**
 * The single application shell: ultra-slim icon rail + slim top bar + content area.
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
 * Ultra-slim rail: brand logo at the top-left, then the navigation icons with small pixel labels.
 * The active destination is marked by a rounded green capsule that springs into place
 * plus a slim accent bar on the rail's inner edge.
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
            .background(CsColors.Sidebar),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Brand mark: the single CS Launcher Lite logo, top-left of the rail, above Home
        Image(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp)),
            painter = painterResource(R.drawable.ic_cs_brand_round),
            contentDescription = stringResource(R.string.cs_brand_name),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Main navigation: Home, Discover, Library — directly below the logo
        topDestinations.forEachIndexed { index, destination ->
            AnimatedVisibility(
                enter = fadeIn(animationSpec = staggeredEnter(index, baseDelayMillis = 30, durationMillis = 400)) + slideInVertically(
                    initialOffsetY = { 12.dp },
                    animationSpec = staggeredEnter(index, baseDelayMillis = 30, durationMillis = 400)
                ),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 12.dp })
            ) {
                CsRailItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onNavigate(destination) }
                )
            }
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
            label = stringResource(R.string.cs_nav_exit),
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
    label: String,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .height(46.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.92f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = railLabelStyle,
            color = CsColors.TextTertiary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CsRailItem(
    destination: CsDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // New selection: the capsule pops open with a quick spring, the accent bar grows with a bounce,
    // and the selected item gets a slow heartbeat pulse so it feels alive on the rail.
    val capsuleSpring = spring<Dp>(
        dampingRatio = 0.45f,
        stiffness = Spring.StiffnessMedium
    )
    val capsuleWidth by animateDpAsState(
        targetValue = if (selected) 36.dp else 22.dp,
        animationSpec = capsuleSpring,
        label = "csRailCapsuleWidth"
    )
    val capsuleHeight by animateDpAsState(
        targetValue = if (selected) 34.dp else 24.dp,
        animationSpec = capsuleSpring,
        label = "csRailCapsuleHeight"
    )
    val barHeight by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = capsuleSpring,
        label = "csRailBarHeight"
    )
    val barAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "csRailBarAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 0.9f,
        animationSpec = tween(durationMillis = 140, easing = BounceEasing),
        label = "csRailIconScale"
    )
    val capsuleColor by animateColorAsState(
        targetValue = if (selected) CsColors.AccentSoft else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = QuickSpringEasing),
        label = "csRailCapsule"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) CsColors.Accent else CsColors.TextSecondary,
        animationSpec = tween(durationMillis = 200, easing = QuickSpringEasing),
        label = "csRailTint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) CsColors.Accent else CsColors.TextTertiary,
        animationSpec = tween(durationMillis = 200, easing = QuickSpringEasing),
        label = "csRailLabel"
    )
    val pulse by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 2200, easing = PulseEasing),
        label = "csRailPulse"
    )

    Box(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .height(46.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.92f)
    ) {
        // Inner-edge accent bar: grows out of nothing when the item becomes active
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(CsColors.Accent.copy(alpha = barAlpha))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(capsuleWidth + 4.dp * pulse)
                    .height(capsuleHeight + 4.dp * pulse)
                    .clip(CircleShape)
                    .background(capsuleColor)
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, CsColors.AccentDark, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(iconScale),
                    painter = painterResource(destination.iconRes),
                    contentDescription = stringResource(destination.labelRes),
                    tint = tint
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = stringResource(destination.labelRes),
                style = railLabelStyle,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Slim top header: text only — "CS Launcher" + "Lite" in the accent colour, in a
 * Minecraft-style pixel font. The single CS Launcher Lite logo sits in the rail's
 * top-left corner. No Minecraft tag, no task/status chip.
 */
@Composable
private fun CsTopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    menuItems: List<CsMenuItem>
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(CsSpacing.topBarHeight),
        color = CsColors.Background,
        border = BorderStroke(1.dp, CsColors.HeaderOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
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
                        size = 28.dp,
                        iconSize = 15.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // Brand title — the only logo lives in the rail, so the header is text only
            Text(
                text = stringResource(R.string.cs_brand_name),
                style = brandTitleStyle,
                color = CsColors.TextPrimary,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.cs_brand_lite),
                style = brandTitleStyle,
                color = CsColors.Accent,
                maxLines = 1,
                softWrap = false
            )

            if (canGoBack) {
                // Nested screens keep their own title (secondary, never competing with the brand)
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(CsColors.HeaderOutline)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = CsColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (menuItems.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box {
                    CsIconButton(
                        iconRes = R.drawable.ic_more_vert,
                        contentDescription = stringResource(R.string.cs_more_menu),
                        onClick = { menuExpanded = true },
                        size = 28.dp,
                        iconSize = 15.dp
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
