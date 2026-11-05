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

/** 主导航的三个目的地 */
enum class CsDestination(val labelRes: Int, val iconRes: Int) {
    HOME(R.string.cs_nav_home, R.drawable.ic_home_filled),
    DISCOVER(R.string.cs_nav_discover, R.drawable.ic_cs_explore),
    LIBRARY(R.string.cs_nav_library, R.drawable.ic_cs_layers)
}

/** 顶栏溢出菜单项 */
data class CsMenuItem(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

/**
 * CS Launcher Lite 主框架：最左侧的细长导航栏 + 顶栏 + 内容区域
 */
@Composable
fun CsShell(
    selected: CsDestination?,
    onNavigate: (CsDestination) -> Unit,
    onSettings: () -> Unit,
    onAccounts: () -> Unit,
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
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
        CsSidebar(
            selected = selected,
            onNavigate = onNavigate,
            onSettings = onSettings,
            onAccounts = onAccounts
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
 * 最左侧细长导航栏：仅图标，无文字标签
 */
@Composable
private fun CsSidebar(
    selected: CsDestination?,
    onNavigate: (CsDestination) -> Unit,
    onSettings: () -> Unit,
    onAccounts: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .fillMaxHeight()
            .background(CsColors.BackgroundSecondary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 品牌标记
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(CsColors.AccentSoft)
                .border(1.dp, CsColors.AccentDark, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(21.dp),
                painter = painterResource(R.drawable.ic_cs_logo),
                contentDescription = stringResource(R.string.cs_app_name),
                tint = CsColors.Accent
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        CsDestination.entries.forEach { destination ->
            CsSidebarItem(
                destination = destination,
                selected = destination == selected,
                onClick = { onNavigate(destination) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        CsSidebarIcon(
            iconRes = R.drawable.ic_settings_filled,
            contentDescription = stringResource(R.string.cs_nav_settings),
            selected = false,
            onClick = onSettings
        )
        Spacer(modifier = Modifier.height(6.dp))
        CsSidebarIcon(
            iconRes = R.drawable.ic_person_filled,
            contentDescription = stringResource(R.string.cs_nav_account),
            selected = false,
            onClick = onAccounts
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CsSidebarItem(
    destination: CsDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) CsColors.AccentSoft else Color.Transparent,
        label = "csSidebarItemBackground"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) CsColors.Accent else CsColors.TextSecondary,
        label = "csSidebarItemTint"
    )

    Box(
        modifier = Modifier
            .width(CsSpacing.sidebarWidth)
            .height(46.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左侧选中指示条
        val indicatorWidth by animateDpAsState(
            targetValue = if (selected) 3.dp else 0.dp,
            label = "csSidebarIndicator"
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(indicatorWidth)
                .height(22.dp)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(CsColors.Accent)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .csPressScale(interactionSource, pressedScale = 0.92f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(destination.iconRes),
                contentDescription = stringResource(destination.labelRes),
                tint = tint
            )
        }
    }
}

@Composable
private fun CsSidebarIcon(
    iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) CsColors.AccentSoft else Color.Transparent,
        label = "csSidebarFooterBackground"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) CsColors.Accent else CsColors.TextSecondary,
        label = "csSidebarFooterTint"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.92f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(19.dp),
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

/**
 * 顶栏：返回按钮、页面标题、运行状态、任务入口、溢出菜单
 */
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

            // 运行状态
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
