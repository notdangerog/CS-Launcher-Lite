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

package com.movtery.zalithlauncher.ui.cs.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CS Launcher Lite 设计系统调色板。
 *
 * 全部界面颜色都应从这里取用，禁止在界面代码里直接写死颜色值，
 * 以便后续统一调整主题。
 */
object CsColors {
    /** 主背景 */
    val Background = Color(0xFF121212)

    /** 次级背景（侧边栏、右侧信息栏） */
    val BackgroundSecondary = Color(0xFF171717)

    /** 卡片 */
    val Card = Color(0xFF1E1E1E)

    /** 悬浮/强调卡片 */
    val CardElevated = Color(0xFF252525)

    /** 主强调色：祖母绿 */
    val Accent = Color(0xFF1FD760)

    /** 强调色的深色变体（边框、渐变） */
    val AccentDark = Color(0xFF12A04A)

    /** 强调色的柔和背景（选中态、标签底色） */
    val AccentSoft = Color(0x1F1FD760)

    /** 强调色上的文字 */
    val OnAccent = Color(0xFF06130A)

    /** 主文字 */
    val TextPrimary = Color(0xFFFFFFFF)

    /** 次级文字 */
    val TextSecondary = Color(0xFFA5A5A5)

    /** 三级文字（提示、占位符） */
    val TextTertiary = Color(0xFF737373)

    /** 边框 */
    val Border = Color(0xFF2A2A2A)

    /** 强边框（悬浮态） */
    val BorderStrong = Color(0xFF3D3D3D)

    /** 危险色 */
    val Danger = Color(0xFFFF6B6B)

    /** 图片/封面占位渐变色（依据标题取模选择） */
    val coverGradients: List<Pair<Color, Color>> = listOf(
        Color(0xFF1B4D3E) to Color(0xFF0E2A22),
        Color(0xFF3A2E5C) to Color(0xFF1B1530),
        Color(0xFF4A2B1F) to Color(0xFF241410),
        Color(0xFF17384D) to Color(0xFF0A1D28),
        Color(0xFF4A3A18) to Color(0xFF241C0C),
        Color(0xFF3A1F3D) to Color(0xFF1D0F1F)
    )

    /** 依据文本稳定地取一个封面渐变 */
    fun coverGradientFor(seed: String): Pair<Color, Color> {
        if (seed.isEmpty()) return coverGradients.first()
        var hash = 7
        seed.forEach { hash = hash * 31 + it.code }
        val index = (hash % coverGradients.size + coverGradients.size) % coverGradients.size
        return coverGradients[index]
    }
}

private val CsColorScheme = darkColorScheme(
    primary = CsColors.Accent,
    onPrimary = CsColors.OnAccent,
    primaryContainer = CsColors.AccentDark,
    onPrimaryContainer = CsColors.TextPrimary,
    inversePrimary = CsColors.AccentDark,
    secondary = CsColors.Accent,
    onSecondary = CsColors.OnAccent,
    secondaryContainer = CsColors.CardElevated,
    onSecondaryContainer = CsColors.TextPrimary,
    tertiary = CsColors.Accent,
    onTertiary = CsColors.OnAccent,
    tertiaryContainer = CsColors.CardElevated,
    onTertiaryContainer = CsColors.TextPrimary,
    background = CsColors.Background,
    onBackground = CsColors.TextPrimary,
    surface = CsColors.BackgroundSecondary,
    onSurface = CsColors.TextPrimary,
    surfaceVariant = CsColors.Card,
    onSurfaceVariant = CsColors.TextSecondary,
    surfaceTint = CsColors.Accent,
    surfaceDim = CsColors.Background,
    surfaceBright = CsColors.CardElevated,
    surfaceContainerLowest = CsColors.Background,
    surfaceContainerLow = CsColors.BackgroundSecondary,
    surfaceContainer = CsColors.Card,
    surfaceContainerHigh = CsColors.Card,
    surfaceContainerHighest = CsColors.CardElevated,
    inverseSurface = CsColors.TextPrimary,
    inverseOnSurface = CsColors.Background,
    error = CsColors.Danger,
    onError = Color(0xFF1A0505),
    errorContainer = Color(0xFF3A1515),
    onErrorContainer = Color(0xFFFFD9D9),
    outline = CsColors.BorderStrong,
    outlineVariant = CsColors.Border,
    scrim = Color(0xCC000000)
)

private val CsTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = TextStyle(
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    )
)

private val CsShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

/**
 * CS Launcher Lite 主题。
 *
 * 强制使用暗色 + 祖母绿强调色，不受系统动态取色影响，
 * 保证与 Modrinth 风格参考图一致的视觉呈现。
 */
@Composable
fun CsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CsColorScheme,
        typography = CsTypography,
        shapes = CsShapes,
        content = content
    )
}

/** 常用间距，保证全局一致 */
object CsSpacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp

    /** 主内容区域左右内边距 */
    val contentHorizontal = 24.dp

    /** 侧边栏宽度 */
    val sidebarWidth = 60.dp

    /** 顶栏高度 */
    val topBarHeight = 46.dp
}
