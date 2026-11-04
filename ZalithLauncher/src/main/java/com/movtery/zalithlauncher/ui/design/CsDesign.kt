/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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
 * CS Launcher Lite design tokens — EXACT spec implementation.
 *
 * Color Palette & Theme per FINAL UI/UX Design Specification:
 * - Background: Deep charcoal/matte black #121212
 * - Cards: Rounded dark grey #1E1E1E
 * - Accent: Vibrant electric green #1DB954 (neon green)
 * - Typography: Clean modern sans-serif, white primary, muted grey secondary
 */
object CsColor {
    /** Brand accent — vibrant electric green #1DB954 — sole signature color */
    val Accent = Color(0xFF1DB954)
    val AccentDark = Color(0xFF179443)
    val AccentMuted = Color(0xFF14833B)
    val AccentGlow = Color(0xFF1ED760)

    /** Spec background #121212 — deep charcoal/matte black */
    val Background = Color(0xFF121212)
    /** Spec card #1E1E1E — rounded dark grey */
    val Card = Color(0xFF1E1E1E)
    val CardHover = Color(0xFF252525)
    val CardPressed = Color(0xFF2A2A2A)

    /** Neutral ramp aligned to spec — darkest to lightest */
    val Neutral0 = Color(0xFF0A0A0A)
    val Neutral1 = Color(0xFF121212) // <- spec background
    val Neutral2 = Color(0xFF1A1A1A)
    val Neutral3 = Color(0xFF1E1E1E) // <- spec card
    val Neutral4 = Color(0xFF252525)
    val Neutral5 = Color(0xFF2A2A2A)
    val Neutral6 = Color(0xFF2E2E2E)
    val Neutral7 = Color(0xFF363636)

    /** Typography: white primary, muted grey secondary */
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9AA0A6)
    val TextTertiary = Color(0xFF6B7280)
    val TextDisabled = Color(0xFF4B5563)

    /** Dividers — thin vertical lines */
    val Divider = Color(0xFF2A2A2A)
    /** Slightly thicker divider in front of right panel */
    val DividerStrong = Color(0xFF333333)
    /** Panel background — same as app background for seamless look */
    val Panel = Color(0xFF121212)
    val PanelCard = Color(0xFF1A1A1A)

    val Warning = Color(0xFFF0B429)
    val Danger = Color(0xFFEF4B5A)
    val Info = Color(0xFF58A6FF)

    // Light theme (secondary, ships dark-first)
    val LightBg = Color(0xFFF6F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHigh = Color(0xFFEFF2F6)
    val LightOutline = Color(0xFFD7DDE5)
    val LightTextPrimary = Color(0xFF11151A)
    val LightTextSecondary = Color(0xFF5A6472)
    val LightAccent = Color(0xFF1DB954)
}

/**
 * Dark scheme — THE primary CS Launcher Lite theme per spec.
 * Background #121212, cards #1E1E1E, accent #1DB954.
 */
val csLiteDark = darkColorScheme(
    primary = CsColor.Accent,
    onPrimary = Color(0xFF05210F),
    primaryContainer = Color(0xFF0F3D1E),
    onPrimaryContainer = Color(0xFFB7F5CC),
    inversePrimary = CsColor.AccentDark,

    secondary = Color(0xFF9AA0A6),
    onSecondary = Color(0xFF121212),
    secondaryContainer = CsColor.Neutral4,
    onSecondaryContainer = CsColor.TextPrimary,

    tertiary = CsColor.Info,
    onTertiary = Color(0xFF04121F),
    tertiaryContainer = Color(0xFF17303F),
    onTertiaryContainer = Color(0xFFCDE6FF),

    background = CsColor.Background,
    onBackground = CsColor.TextPrimary,

    surface = CsColor.Background,
    onSurface = CsColor.TextPrimary,
    surfaceVariant = CsColor.Card,
    onSurfaceVariant = CsColor.TextSecondary,

    surfaceTint = CsColor.Accent,
    inverseSurface = CsColor.TextPrimary,
    inverseOnSurface = CsColor.Background,

    error = CsColor.Danger,
    onError = Color(0xFF2B0308),
    errorContainer = Color(0xFF4A1016),
    onErrorContainer = Color(0xFFFFDADD),

    outline = CsColor.Neutral6,
    outlineVariant = CsColor.Divider,
    scrim = Color(0xFF000000),

    surfaceBright = CsColor.Card,
    surfaceDim = CsColor.Background,
    surfaceContainerLowest = CsColor.Neutral0,
    surfaceContainerLow = CsColor.Background,
    surfaceContainer = CsColor.Neutral2,
    surfaceContainerHigh = CsColor.Card,
    surfaceContainerHighest = CsColor.Neutral4
)

/**
 * Light scheme — kept complete so theme switch keeps working.
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
 * Rounded, consistent corner radii — per spec, modern dark cards.
 */
val CsShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
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
 * Fixed component sizing — ultra-slim rail, compact right panel, large banner cards.
 */
object CsSize {
    // Legacy name kept for compat
    val sidebarWidth = 196.dp
    val sidebarCollapsedWidth = 64.dp
    /** Ultra-slim icon-only rail per spec */
    val railWidth = 60.dp
    /** Compact right widget panel */
    val accountPanelWidth = 272.dp
    /** Hairline between rail and content */
    val dividerThin = 1.dp
    /** Slightly thicker divider in front of right panel */
    val dividerStrong = 2.dp
    /** Rail icon hit target — pill capsule */
    val railItem = 42.dp
    val railIconSize = 22.dp
    val railCapsuleWidth = 48.dp
    val railCapsuleHeight = 32.dp
    /** Large preview banner card — prominent modpack display */
    val catalogCardHeight = 268.dp
    val catalogCardHeightLarge = 300.dp
    val bannerHeight = 140.dp
    val bannerHeightLarge = 160.dp
    val topBarHeight = 56.dp
    val cardIcon = 48.dp
    val cardIconSmall = 40.dp
    val cardIconLarge = 72.dp
    val buttonHeight = 36.dp
    val buttonHeightLarge = 44.dp
    val tileMinWidth = 280.dp
}

private fun TextStyle.tighter(tracking: Int = 0): TextStyle = copy(
    letterSpacing = tracking.sp,
    fontFeatureSettings = "ss01"
)

/**
 * Typography — clean, modern sans-serif, high readability.
 * White primary, muted grey secondary per spec.
 */
val CsTypography = Typography(
    displaySmall = Typography().displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontWeight = FontWeight.Bold,
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
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(fontWeight = FontWeight.Medium),
    bodyLarge = Typography().bodyLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.15.sp),
    bodyMedium = Typography().bodyMedium.copy(letterSpacing = 0.15.sp),
    bodySmall = Typography().bodySmall.copy(letterSpacing = 0.1.sp)
)
