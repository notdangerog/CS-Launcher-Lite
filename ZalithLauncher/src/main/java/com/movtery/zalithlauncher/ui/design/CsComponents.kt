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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.screens.content.elements.DisabledAlpha

/**
 * The base card surface used across CS Launcher Lite.
 *
 * @param onClick when not null the card becomes clickable (ripple + role).
 * @param border draws a 1dp hairline, used for secondary/outline cards.
 */
@Composable
fun CsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = MaterialTheme.shapes.large,
    border: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(CsSpace.l),
    content: @Composable ColumnScope.() -> Unit
) {
    val borderStroke = if (border) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else null

    if (onClick != null) {
        // Press physics: a stiff snap down under the finger, one springy overshoot on release.
        // Applied through graphicsLayer so a tapped card never re-measures its contents.
        val interactionSource = remember { MutableInteractionSource() }
        val press = rememberCsPressScale(interactionSource, CsPress.Card)
        Surface(
            modifier = modifier.csPressLayer(press),
            onClick = onClick,
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = borderStroke,
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            border = borderStroke
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Section title with an optional subtitle and trailing actions.
 */
@Composable
fun CsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing
        )
    }
}

/**
 * Primary (accent) action button.
 */
@Composable
fun CsPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null,
    large: Boolean = false,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = csTween(easing = CsEasing.Standard, durationMillis = CsDuration.Fast),
        label = "CsPrimaryButtonContainer"
    )
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    // The large variant is the launcher's hero action, so it gets deeper, bouncier travel.
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberCsPressScale(
        interactionSource = interactionSource,
        pressedScale = if (large) CsPress.Hero else CsPress.Card,
        enabled = enabled
    )

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = if (large) CsSize.buttonHeightLarge else CsSize.buttonHeight)
            .csPressLayer(press),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(if (large) 14.dp else 12.dp),
        color = container,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (large) CsSpace.xl else CsSpace.l),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    modifier = Modifier.size(if (large) 20.dp else 18.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(CsSpace.s))
            }
            Text(
                text = text,
                style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

/**
 * Secondary action button: a surface card with a hairline border.
 */
@Composable
fun CsTonalButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberCsPressScale(interactionSource, CsPress.Card, enabled = enabled)

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = CsSize.buttonHeight)
            .csPressLayer(press),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CsSpace.l),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(CsSpace.s))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

/**
 * A compact icon-only button on a rounded surface.
 */
@Composable
fun CsIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Int = 36,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Small targets need the deepest relative travel to be felt at all.
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberCsPressScale(interactionSource, CsPress.Icon, enabled = enabled)

    Surface(
        modifier = modifier
            .size(size.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .csPressLayer(press),
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                modifier = Modifier.size((size * 0.5f).dp),
                painter = painterResource(iconRes),
                contentDescription = contentDescription
            )
        }
    }
}

/**
 * Pill/chip used for filters and quick categories.
 */
@Composable
fun CsChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = csTween(durationMillis = CsDuration.Fast, easing = CsEasing.Standard),
        label = "CsChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = csTween(durationMillis = CsDuration.Fast, easing = CsEasing.Standard),
        label = "CsChipContent"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberCsPressScale(interactionSource, CsPress.Icon)

    Surface(
        modifier = modifier.csPressLayer(press),
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CsSpace.m, vertical = CsSpace.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.xs)
        ) {
            if (iconRes != null) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/**
 * Rounded search input, tuned for the launcher's landscape layout.
 */
@Composable
fun CsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {}
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = CsSize.buttonHeight),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CsSpace.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(com.movtery.zalithlauncher.R.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(CsSpace.s))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
            }
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(CsSpace.xs))
                CsIconButton(
                    iconRes = com.movtery.zalithlauncher.R.drawable.ic_close,
                    contentDescription = null,
                    size = 24,
                    containerColor = Color.Transparent,
                    onClick = { onValueChange("") }
                )
            }
        }
    }
}

/**
 * Consistent empty / error placeholder.
 */
@Composable
fun CsEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(CsSpace.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconRes != null) {
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(CsSpace.m))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(CsSpace.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(CsSpace.l))
            CsPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Small metadata label (version, loader, mod count...).
 */
@Composable
fun CsMetaLabel(
    text: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CsSpace.xs)
    ) {
        if (iconRes != null) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A tiny accent "dot + text" status pill, e.g. current instance indicator.
 *
 * @param live when true the dot breathes on a slow sine, marking a state that is actively running —
 *   a hosted world, a live download, a connected friend. Off by default, so existing pills are
 *   pixel-identical to before; the motion is opt-in per call site.
 */
@Composable
fun CsStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    live: Boolean = false
) {
    // A breathing dot rather than a blinking one: the sine has no acceleration kink at the turns,
    // so it reads as "alive" at the edge of your vision instead of demanding attention.
    val breath = rememberCsBreathPhase(CsDuration.LoopSlow)

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CsSpace.s, vertical = CsSpace.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
                    .then(if (live) Modifier.csBreathLayer(breath, minAlpha = 0.45f, minScale = 0.80f) else Modifier)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Sidebar navigation item of the CS Launcher Lite shell.
 */
@Composable
fun CsNavItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        else Color.Transparent,
        animationSpec = csTween(durationMillis = CsDuration.Fast, easing = CsEasing.Standard),
        label = "CsNavItemContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = csTween(durationMillis = CsDuration.Fast, easing = CsEasing.Standard),
        label = "CsNavItemContent"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberCsPressScale(interactionSource, CsPress.Card)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .csPressLayer(press),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (collapsed) CsSpace.s else CsSpace.m,
                vertical = CsSpace.m
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(iconRes),
                contentDescription = null
            )
            if (!collapsed) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Sidebar group caption.
 */
@Composable
fun CsNavSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(start = CsSpace.m, top = CsSpace.s, bottom = CsSpace.xs),
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

/**
 * Divider that matches the token palette instead of the theme default.
 */
@Composable
fun CsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun CsGradientTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(color = MaterialTheme.colorScheme.onSurface)
            ) { append(text) }
        },
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 1
    )
}
