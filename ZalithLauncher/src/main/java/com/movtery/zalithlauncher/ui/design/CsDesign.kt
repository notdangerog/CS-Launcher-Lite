/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CS Launcher Lite design tokens.
 *
 * The whole launcher derives its look from MaterialTheme (colorScheme, shapes,
 * typography, motion), so keeping the tokens in one place reskins every screen
 * consistently without touching each of them.
 */
object CsColor {
    /** Brand accent - the single signature green of CS Launcher Lite */
    val Accent = Color(0xFF3DDC84)
    val AccentDark = Color(0xFF1B9A57)
    val AccentMuted = Color(0xFF2BA968)

    /** Neutral charcoal ramp, darkest to lightest */
    val Neutral0 = Color(0xFF08090C)
    val Neutral1 = Color(0xFF0D0F13)
    val Neutral2 = Color(0xFF12151A)
    val Neutral3 = Color(0xFF171B21)
    val Neutral4 = Color(0xFF1D222A)
    val Neutral5 = Color(0xFF242A33)
    val Neutral6 = Color(0xFF2E353F)

    val TextPrimary = Color(0xFFECF0F6)
    val TextSecondary = Color(0xFFA6AFBD)
    val TextDisabled = Color(0xFF6C7686)

    val Divider = Color(0xFF262C35)
    val Warning = Color(0xFFF0B429)
    val Danger = Color(0xFFEF4B5A)
    val Info = Color(0xFF58A6FF)

    // Light theme ramp (secondary, the launcher ships dark-first)
    val LightBg = Color(0xFFF6F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHigh = Color(0xFFEFF2F6)
    val LightOutline = Color(0xFFD7DDE5)
    val LightTextPrimary = Color(0xFF11151A)
    val LightTextSecondary = Color(0xFF5A6472)
    val LightAccent = Color(0xFF12874F)
}

/**
 * Dark scheme - the primary CS Launcher Lite theme.
 */
val csLiteDark = darkColorScheme(
    primary = CsColor.Accent,
    onPrimary = Color(0xFF04180D),
    primaryContainer = Color(0xFF14402A),
    onPrimaryContainer = Color(0xFFB6F5D2),
    inversePrimary = CsColor.AccentDark,

    secondary = Color(0xFF8FB7A2),
    onSecondary = Color(0xFF0B1A12),
    secondaryContainer = CsColor.Neutral5,
    onSecondaryContainer = CsColor.TextPrimary,

    tertiary = CsColor.Info,
    onTertiary = Color(0xFF04121F),
    tertiaryContainer = Color(0xFF17303F),
    onTertiaryContainer = Color(0xFFCDE6FF),

    background = CsColor.Neutral1,
    onBackground = CsColor.TextPrimary,

    surface = CsColor.Neutral1,
    onSurface = CsColor.TextPrimary,
    surfaceVariant = CsColor.Neutral4,
    onSurfaceVariant = CsColor.TextSecondary,

    surfaceTint = CsColor.Accent,
    inverseSurface = CsColor.TextPrimary,
    inverseOnSurface = CsColor.Neutral1,

    error = CsColor.Danger,
    onError = Color(0xFF2B0308),
    errorContainer = Color(0xFF4A1016),
    onErrorContainer = Color(0xFFFFDADD),

    outline = CsColor.Neutral6,
    outlineVariant = CsColor.Divider,
    scrim = Color(0xFF000000),

    surfaceBright = CsColor.Neutral3,
    surfaceDim = CsColor.Neutral1,
    surfaceContainerLowest = CsColor.Neutral0,
    surfaceContainerLow = CsColor.Neutral2,
    surfaceContainer = CsColor.Neutral2,
    surfaceContainerHigh = CsColor.Neutral3,
    surfaceContainerHighest = CsColor.Neutral4
)

/**
 * Light scheme - kept complete so the existing theme switch keeps working.
 */
val csLiteLight = lightColorScheme(
    primary = CsColor.LightAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7F2DA),
    onPrimaryContainer = Color(0xFF00281A),
    inversePrimary = CsColor.Accent,

    secondary = Color(0xFF4B6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CsColor.LightSurfaceHigh,
    onSecondaryContainer = CsColor.LightTextPrimary,

    tertiary = Color(0xFF2B6C99),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E8FA),
    onTertiaryContainer = Color(0xFF001E31),

    background = CsColor.LightBg,
    onBackground = CsColor.LightTextPrimary,

    surface = CsColor.LightBg,
    onSurface = CsColor.LightTextPrimary,
    surfaceVariant = CsColor.LightSurfaceHigh,
    onSurfaceVariant = CsColor.LightTextSecondary,

    surfaceTint = CsColor.LightAccent,
    inverseSurface = CsColor.LightTextPrimary,
    inverseOnSurface = CsColor.LightBg,

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = CsColor.LightOutline,
    outlineVariant = Color(0xFFE6EAF0),
    scrim = Color(0xFF000000),

    surfaceBright = CsColor.LightSurface,
    surfaceDim = Color(0xFFE4E8ED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF6F8FA),
    surfaceContainerHigh = CsColor.LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE7EBF0)
)

/**
 * Rounded, consistent corner radii for every surface in the launcher.
 */
val CsShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Spacing scale (4dp grid).
 */
object CsSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/**
 * Fixed component sizing so cards feel like one system.
 */
object CsSize {
    val sidebarWidth = 196.dp
    val sidebarCollapsedWidth = 76.dp
    val topBarHeight = 56.dp
    val cardIcon = 56.dp
    val cardIconSmall = 40.dp
    val cardIconLarge = 72.dp
    val buttonHeight = 40.dp
    val buttonHeightLarge = 48.dp
    val tileMinWidth = 240.dp
}

private fun TextStyle.tighter(tracking: Int = 0): TextStyle = copy(
    letterSpacing = tracking.sp,
    fontFeatureSettings = "ss01"
)

/**
 * Modern, slightly tighter typography than the Material defaults.
 */
val CsTypography = Typography(
    displaySmall = Typography().displaySmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(fontWeight = FontWeight.Medium),
    bodyMedium = Typography().bodyMedium.copy(letterSpacing = 0.1.sp)
)
