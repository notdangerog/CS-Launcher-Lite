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

package com.movtery.zalithlauncher.ui.cs.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import java.util.Locale

/**
 * CS Launcher Lite 通用界面组件。
 *
 * 约定（保证不出现文字溢出、错位、尺寸不一致）：
 * 1. 所有 [Text] 必须指定 `maxLines` 与 `overflow = TextOverflow.Ellipsis`
 * 2. 卡片内部不允许使用固定宽度 + 无限长文本的组合
 * 3. 横向排列的自适应内容使用 `Modifier.weight(1f, fill = false)`
 * 4. 图标一律使用 [Modifier.size] 固定尺寸，并使用 `ContentScale.Fit`
 */

/** 把 1250000 格式化成 1.25M */
fun formatCount(value: Long): String = when {
    value >= 1_000_000_000L -> String.format(Locale.US, "%.2fB", value / 1_000_000_000.0)
    value >= 1_000_000L -> String.format(Locale.US, "%.2fM", value / 1_000_000.0)
    value >= 10_000L -> String.format(Locale.US, "%.0fK", value / 1_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

/**
 * 按下时轻微缩放的交互反馈
 */
@Composable
fun Modifier.csPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.985f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        label = "csPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 通用卡片容器：圆角、细边框、可选点击反馈
 */
@Composable
fun CsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = CsColors.Card,
    borderColor: Color = CsColors.Border,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .then(clickableModifier),
        color = Color.Transparent,
        contentColor = CsColors.TextPrimary,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * 主按钮（祖母绿实心）
 */
@Composable
fun CsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null,
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = when {
        !enabled -> CsColors.CardElevated
        else -> CsColors.Accent
    }
    val contentColor = when {
        !enabled -> CsColors.TextTertiary
        else -> CsColors.OnAccent
    }
    val interactionModifier = if (enabled) {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.97f)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 34.dp else 44.dp)
            .clip(RoundedCornerShape(if (compact) 9.dp else 12.dp))
            .background(containerColor, RoundedCornerShape(if (compact) 9.dp else 12.dp))
            .then(interactionModifier),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = if (compact) 14.dp else 20.dp,
                    vertical = if (compact) 7.dp else 11.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Icon(
                    modifier = Modifier.size(if (compact) 14.dp else 17.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = text,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 次级按钮（描边 / 弱背景）
 */
@Composable
fun CsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null,
    compact: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (enabled) CsColors.TextPrimary else CsColors.TextTertiary
    val interactionModifier = if (enabled) {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.97f)
    } else {
        Modifier
    }
    val shape = RoundedCornerShape(if (compact) 9.dp else 12.dp)

    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 34.dp else 44.dp)
            .clip(shape)
            .background(CsColors.CardElevated, shape)
            .border(1.dp, CsColors.Border, shape)
            .then(interactionModifier),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 14.dp else 20.dp,
                vertical = if (compact) 7.dp else 11.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconRes != null) {
                Icon(
                    modifier = Modifier.size(if (compact) 14.dp else 17.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = text,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 圆形图标按钮
 */
@Composable
fun CsIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 17.dp,
    tint: Color = CsColors.TextSecondary,
    background: Color = Color.Transparent,
    borderColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(size / 3)
    Surface(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background, shape)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.9f),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

/**
 * 小标签（项目类别、加载器、版本等）
 */
@Composable
fun CsTag(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Surface(
        modifier = modifier.heightIn(min = 22.dp),
        color = if (accent) CsColors.AccentSoft else CsColors.CardElevated,
        shape = RoundedCornerShape(7.dp),
        contentColor = if (accent) CsColors.Accent else CsColors.TextSecondary
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) CsColors.Accent else CsColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 可点击的过滤器标签
 */
@Composable
fun CsFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 30.dp)
            .clip(shape)
            .background(if (selected) CsColors.AccentSoft else CsColors.CardElevated, shape)
            .border(1.dp, if (selected) CsColors.AccentDark else CsColors.Border, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .csPressScale(interactionSource, pressedScale = 0.95f),
        color = Color.Transparent
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CsColors.Accent else CsColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 区块标题，可带右侧动作文字
 */
@Composable
fun CsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (onAction != null && actionText != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = CsColors.Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.ic_cs_chevron_right),
                    contentDescription = null,
                    tint = CsColors.Accent
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * 统计信息（下载量、关注数）
 */
@Composable
fun CsStatItem(
    iconRes: Int,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = CsColors.TextTertiary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(13.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = CsColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 搜索框
 */
@Composable
fun CsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(11.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(shape)
            .background(CsColors.Card, shape)
            .border(1.dp, CsColors.Border, shape),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = CsColors.TextTertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = CsColors.TextPrimary),
                    cursorBrush = SolidColor(CsColors.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSearch()
                        }
                    )
                )
            }
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                CsIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = null,
                    onClick = {
                        onValueChange("")
                        onSearch()
                    },
                    size = 24.dp,
                    iconSize = 13.dp
                )
            }
        }
    }
}

/**
 * 下拉选择器
 * @param options 选项列表（显示文本 -> 值）
 */
@Composable
fun <T> CsDropdownField(
    value: T,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.second == value }?.first ?: options.firstOrNull()?.first ?: ""
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(9.dp)

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = 34.dp)
                .clip(shape)
                .background(CsColors.CardElevated, shape)
                .border(1.dp, CsColors.Border, shape)
                .then(
                    if (enabled) {
                        Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { expanded = true }
                            )
                            .csPressScale(interactionSource, pressedScale = 0.97f)
                    } else Modifier
                ),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    modifier = if (fillWidth) {
                        Modifier.weight(1f, fill = false)
                    } else {
                        Modifier.widthIn(max = 150.dp)
                    },
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) CsColors.TextPrimary else CsColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.size(14.dp),
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
                .widthIn(min = 160.dp, max = 260.dp)
        ) {
            options.forEach { (text, optionValue) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (optionValue == value) CsColors.Accent else CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(optionValue)
                    }
                )
            }
        }
    }
}

/**
 * 标签页（带下划线动画效果）
 *
 * @param outlined 为 true 时改为浅灰描边的圆角胶囊样式（当前选中项仍用强调色区分）
 */
@Composable
fun CsTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .then(if (outlined) Modifier.padding(vertical = 2.dp) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (outlined) Arrangement.spacedBy(8.dp) else Arrangement.Start
    ) {
        if (outlined) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) CsColors.Accent else CsColors.TextSecondary,
                    label = "csOutlinedTabColor"
                )
                Surface(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (selected) CsColors.AccentSoft else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) CsColors.AccentDark else CsColors.Border,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelected(index) },
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            return@Row
        }

        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (selected) CsColors.TextPrimary else CsColors.TextSecondary,
                label = "csTabColor"
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .clickable { onSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(if (selected) CsColors.Accent else Color.Transparent)
                )
            }
        }
    }
}

/**
 * 图片加载（失败时保留底层渐变，不出现空白/拉伸）
 */
@Composable
fun CsRemoteImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackSeed: String = ""
) {
    val gradient = remember(fallbackSeed) { CsColors.coverGradientFor(fallbackSeed) }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(gradient.first, gradient.second))
        )
    ) {
        if (model != null) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = model,
                contentDescription = null,
                contentScale = contentScale
            )
        }
    }
}

/**
 * 项目图标（图片缺失时用名称首字母代替）
 */
@Composable
fun CsProjectIcon(
    iconUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(CsColors.CardElevated, shape)
            .border(1.dp, CsColors.Border, shape),
        contentAlignment = Alignment.Center
    ) {
        CsRemoteImage(
            model = iconUrl,
            modifier = Modifier.fillMaxSize(),
            fallbackSeed = name
        )
        if (iconUrl.isNullOrEmpty()) {
            Text(
                text = name.take(2).uppercase(Locale.US),
                style = MaterialTheme.typography.titleMedium,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

/**
 * 加载指示器
 */
@Composable
fun CsLoadingBlock(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            color = CsColors.Accent,
            strokeWidth = 2.dp
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = CsColors.TextSecondary,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 骨架屏占位块（加载动画）
 */
@Composable
fun CsSkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(CsColors.CardElevated)
    )
}

/**
 * 空状态 / 错误状态
 */
@Composable
fun CsEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    illustration: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        illustration?.invoke()
        if (illustration != null) Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = CsColors.TextPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.widthIn(max = 420.dp),
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = CsColors.TextSecondary,
            maxLines = 4,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(22.dp))
            action()
        }
    }
}

/**
 * 分隔线
 */
@Composable
fun CsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CsColors.Border)
    )
}

/**
 * 现代对话框容器
 */
@Composable
fun CsDialogCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .widthIn(max = 460.dp)
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(CsColors.CardElevated)
            .border(1.dp, CsColors.Border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        content = content
    )
}
