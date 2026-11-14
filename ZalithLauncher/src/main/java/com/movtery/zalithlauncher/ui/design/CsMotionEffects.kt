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

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Motion *effects* — the reusable building blocks that the [CsMotion] tokens get turned into.
 *
 * Every effect is a two-part API on purpose:
 *  1. a `rememberCsX()` composable that owns the animation state, and
 *  2. a `Modifier.csX(...)` factory that consumes it in the draw or graphics-layer phase.
 *
 * That split keeps the whole library free of `Modifier.composed` (which allocates a composition
 * scope per node and is deprecated), so a list of two hundred shimmering skeleton rows costs one
 * state holder each and no extra scopes — which matters on the low-end arm64 devices this launcher
 * targets.
 *
 * All effects animate only `graphicsLayer` properties or draw-phase brushes. None of them
 * invalidate measure or layout, so none of them can cause a list to re-flow mid-animation.
 *
 * And all of them degrade: with animation switched off in Settings, or the system animator scale at
 * zero, they resolve straight to their end state instead of freezing half-way through.
 */

// ---------------------------------------------------------------------------
// System gate
// ---------------------------------------------------------------------------

/**
 * Reads the system-wide animator duration scale into [CsMotion.systemMotionScale].
 *
 * Call once from the app's root composable. When the user has chosen "Animation scale: off" in
 * Developer options, the whole motion system collapses to instant state changes — the same contract
 * the rest of Android follows.
 */
@Composable
fun CsMotionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(context) { CsMotion.refreshSystemScale(context) }
    content()
}

/** Re-reads the system animator scale. Safe to call again from `Activity.onResume`. */
fun CsMotion.refreshSystemScale(context: Context) {
    systemMotionScale = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
    } catch (ignored: Throwable) {
        1f
    }
}

// ---------------------------------------------------------------------------
// Press physics
// ---------------------------------------------------------------------------

/**
 * How far things compress under a finger. Deeper for large targets than small ones, because a big
 * surface needs more travel for the press to be felt at all.
 */
object CsPress {
    /** Icon buttons, chips, small controls. */
    const val Icon = 0.90f

    /** Cards, list rows, standard buttons. */
    const val Card = 0.965f

    /** The hero Play action — deliberately the most physical thing on screen. */
    const val Hero = 0.93f
}

/**
 * Spring-driven press scale for [interactionSource].
 *
 * Pressing snaps down stiffly, so the finger is acknowledged immediately; releasing springs back
 * with a single small overshoot, which is what makes a button feel like a physical object rather
 * than a colour change. Because both directions are springs, interrupting mid-release (tapping
 * again before it settles) is seamless rather than jumpy.
 */
@Composable
fun rememberCsPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = CsPress.Card,
    enabled: Boolean = true
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val active = pressed && enabled
    val value by animateFloatAsState(
        targetValue = if (active) pressedScale else 1f,
        animationSpec = if (active) {
            CsSpring.stiff(visibilityThreshold = 0.001f)
        } else {
            CsSpring.bouncy(visibilityThreshold = 0.001f)
        },
        label = "CsPressScale"
    )
    return if (CsMotion.enabled) value else 1f
}

/**
 * Applies the press scale. Pure `graphicsLayer`, so it never re-measures the element.
 */
fun Modifier.csPressLayer(pressScale: Float): Modifier = graphicsLayer {
    scaleX = pressScale
    scaleY = pressScale
}

/**
 * Press scale plus an additive brightness lift, which sells the element being pushed *into* the
 * surface. Place this **after** a `Modifier.clip(shape)` so the highlight respects rounded corners.
 */
fun Modifier.csPressLayer(pressScale: Float, lift: Color = Color.White, maxLiftAlpha: Float = 0.10f): Modifier = this
    .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
    }
    .drawWithContent {
        drawContent()
        val amount = ((1f - pressScale) * (maxLiftAlpha / 0.10f)).coerceIn(0f, maxLiftAlpha)
        if (amount > 0.001f) {
            drawRect(color = lift.copy(alpha = amount), blendMode = BlendMode.Plus)
        }
    }

/** Scale only — for elements that already have their own pressed colour treatment. */
fun Modifier.csScale(value: Float): Modifier = graphicsLayer {
    scaleX = value
    scaleY = value
}

// ---------------------------------------------------------------------------
// Reveal on enter
// ---------------------------------------------------------------------------

/**
 * One-shot reveal progress for content that has just entered the composition: 0f on the first frame,
 * springing to 1f. Re-runs whenever [key] changes, which is what makes a refreshed list re-cascade
 * instead of popping in.
 */
@Composable
fun rememberCsReveal(key: Any? = Unit): Float {
    var shown by remember(key) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = csArriveTween(CsDuration.Slow),
        label = "CsReveal"
    )
    LaunchedEffect(key) { shown = true }
    return if (CsMotion.enabled) progress else 1f
}

/**
 * Reveal progress for item [index] of a list, with [CsStagger] applied.
 *
 * Past [CsStagger.MaxTracked] items the delay stops growing, so a long list finishes arriving in
 * roughly the same time as a short one.
 */
@Composable
fun rememberCsStaggeredReveal(index: Int, key: Any? = Unit): Float {
    var shown by remember(key) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = csTween(
            durationMillis = CsDuration.Slow,
            delayMillis = CsStagger.delayFor(index),
            easing = CsEasing.Arrive
        ),
        label = "CsStaggerReveal$index"
    )
    LaunchedEffect(key) { shown = true }
    return if (CsMotion.enabled) progress else 1f
}

/**
 * Applies a reveal: fades in while rising [rise] from below and easing up from [startScale].
 *
 * This is the shared entry gesture for cards, rows and panels across the launcher. One consistent
 * arrival is a large part of what makes an app feel designed rather than assembled.
 */
@Composable
fun Modifier.csReveal(progress: Float, rise: Dp = 14.dp, startScale: Float = 0.965f): Modifier {
    val risePx = with(LocalDensity.current) { rise.toPx() }
    val p = progress.coerceIn(0f, 1f)
    return this.graphicsLayer {
        alpha = p
        val scale = startScale + (1f - startScale) * p
        scaleX = scale
        scaleY = scale
        translationY = risePx * (1f - p)
    }
}

// ---------------------------------------------------------------------------
// Ambient loops
// ---------------------------------------------------------------------------

/**
 * Shimmer phase, 0f → 1f on a seamless loop, for loading placeholders.
 *
 * A single linear restart rather than a reversing tween: the turn-around of `RepeatMode.Reverse` is
 * an acceleration discontinuity that reads as a hitch when several skeleton rows share it.
 */
@Composable
fun rememberCsShimmerPhase(periodMillis: Int = CsDuration.Loop): Float {
    if (!CsMotion.enabled) return 1f
    val transition = rememberInfiniteTransition(label = "CsShimmer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CsMotion.msLoop(periodMillis), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CsShimmerPhase"
    ).value
}

/**
 * Draws a diagonal light sweep across the content — the loading shimmer.
 *
 * The band is wider than the element and travels from fully off one edge to fully off the other, so
 * the loop has no visible seam where it restarts.
 */
fun Modifier.csShimmer(
    phase: Float,
    highlight: Color = CsColor.CardHover,
    angleDegrees: Float = 20f,
    bandRatio: Float = 0.55f
): Modifier = drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@drawWithContent

    val band = w * bandRatio
    val x = -band + (w + band * 2f) * phase.coerceIn(0f, 1f)
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()

    val brush = Brush.linearGradient(
        colors = listOf(
            highlight.copy(alpha = 0f),
            highlight.copy(alpha = 0.55f),
            highlight.copy(alpha = 0f)
        ),
        start = Offset(x, 0f),
        end = Offset(x + band * dx + h * dy, h)
    )
    drawRect(brush = brush, alpha = 0.9f)
}

/**
 * Breathing phase for status indicators — a slow sine mapped onto 0f → 1f → 0f.
 *
 * Sinusoidal instead of a reversing tween so there is no acceleration kink at the turns. That is
 * the difference between a "world hosting" dot that feels alive and one that looks like it is
 * blinking at you.
 */
@Composable
fun rememberCsBreathPhase(periodMillis: Int = CsDuration.LoopSlow): Float {
    if (!CsMotion.enabled) return 1f
    val transition = rememberInfiniteTransition(label = "CsBreath")
    val raw = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(CsMotion.msLoop(periodMillis), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CsBreathPhase"
    ).value
    return (1f - cos(raw)) * 0.5f
}

/** Applies a breath: gentle alpha and scale oscillation for live-status elements. */
fun Modifier.csBreathLayer(
    phase: Float,
    minAlpha: Float = 0.55f,
    minScale: Float = 0.94f
): Modifier = graphicsLayer {
    val p = phase.coerceIn(0f, 1f)
    alpha = minAlpha + (1f - minAlpha) * p
    val s = minScale + (1f - minScale) * p
    scaleX = s
    scaleY = s
}

/** Rotating sweep in degrees, 0 → 360 — the indeterminate "hosting a world" ring. */
@Composable
fun rememberCsSweepPhase(periodMillis: Int = CsDuration.Loop): Float {
    if (!CsMotion.enabled) return 0f
    val transition = rememberInfiniteTransition(label = "CsSweep")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(CsMotion.msLoop(periodMillis), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CsSweepPhase"
    ).value
}

/**
 * A one-shot attention pulse keyed on [trigger] — for notifications and "a friend joined your
 * world" moments. Unlike a breath, this is meant to be noticed, so it overshoots once and settles.
 */
@Composable
fun rememberCsPulse(trigger: Any?): Float {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        if (!CsMotion.enabled) {
            animatable.snapTo(1f)
            return@LaunchedEffect
        }
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = csKeyframes(CsDuration.Deliberate) {
                0f at 0f
                1f at 0.28f with CsEasing.Expressive
                0.86f at 0.55f
                1f at 1f
            }
        )
    }
    return animatable.value
}

// ---------------------------------------------------------------------------
// Value animation
// ---------------------------------------------------------------------------

/**
 * Rolls a number to its new value instead of jumping — download percentages, player counts, file
 * sizes. Spring-driven with no bounce, so a value that changes every frame (a live progress readout)
 * never fights itself and never falls further and further behind the real number.
 */
@Composable
fun rememberCsAnimatedNumber(target: Float): Float {
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = CsSpring.noBounce(visibilityThreshold = 0.01f),
        label = "CsNumber"
    )
    return if (CsMotion.enabled) value else target
}

/** Integer variant of [rememberCsAnimatedNumber]. */
@Composable
fun rememberCsAnimatedInt(target: Int): Int = rememberCsAnimatedNumber(target.toFloat()).roundToInt()

/**
 * Counts from zero to [target] the first time it is composed — the "stats rolling up" effect for the
 * home dashboard's instance count, friends online and worlds hosted.
 */
@Composable
fun rememberCsCountUp(target: Int, durationMillis: Int = CsDuration.Deliberate): Int {
    var displayed by remember(target) { mutableIntStateOf(if (CsMotion.enabled) 0 else target) }
    LaunchedEffect(target) {
        if (!CsMotion.enabled) {
            displayed = target
            return@LaunchedEffect
        }
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = csTween(durationMillis, easing = CsEasing.Emphasized)
        ) { displayed = value.roundToInt() }
    }
    return displayed
}

/**
 * Reveals [text] a few characters at a time — for the news ticker and host status lines, where text
 * arriving all at once is easy to miss on a small landscape screen.
 */
@Composable
fun rememberCsTypewriter(text: String, charsPerSecond: Float = 42f): String {
    var shown by remember(text) { mutableIntStateOf(if (CsMotion.enabled) 0 else text.length) }
    LaunchedEffect(text) {
        if (!CsMotion.enabled) {
            shown = text.length
            return@LaunchedEffect
        }
        shown = 0
        val step = (1000f / charsPerSecond.coerceAtLeast(1f)).toLong().coerceAtLeast(1L)
        while (shown < text.length) {
            delay(step)
            shown += 1
        }
    }
    return if (shown >= text.length) text else text.take(shown)
}

// ---------------------------------------------------------------------------
// Scroll-linked motion
// ---------------------------------------------------------------------------

/**
 * Parallax offset in px for a banner driven by [scrollOffset] px of content scroll.
 *
 * The banner moves at a fraction of the content's speed, which gives the hero area depth without a
 * second scrolling surface. Returns 0 when motion is off.
 */
fun csParallaxOffset(scrollOffset: Float, strength: Float = 0.35f): Float {
    if (!CsMotion.enabled) return 0f
    return -scrollOffset * strength.coerceIn(0f, 0.8f)
}

/**
 * Progress-driven fade and scale for a collapsing header: 0f fully expanded, 1f fully collapsed.
 */
fun Modifier.csCollapseLayer(progress: Float, minScale: Float = 0.92f): Modifier = graphicsLayer {
    val p = progress.coerceIn(0f, 1f)
    val s = 1f - (1f - minScale) * p
    scaleX = s
    scaleY = s
    alpha = 1f - p * 0.85f
}

// ---------------------------------------------------------------------------
// Composable wrappers
// ---------------------------------------------------------------------------

/**
 * [AnimatedVisibility] pre-wired with the CS transitions, so call sites stop hand-rolling
 * `fadeIn(tween(140)) + expandVertically(tween(180))` and every disclosure in the app moves alike.
 */
@Composable
fun CsAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    fromTop: Boolean = false,
    expand: Boolean = true,
    label: String = "CsAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val (enter, exit) = when {
        expand && fromTop -> CsTransitions.dropDown()
        expand -> CsTransitions.expand(vertical = true)
        else -> CsTransitions.crossFade()
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content
    )
}

/** Box-scoped variant of [CsAnimatedVisibility] using the scale-fade pair. */
@Composable
fun CsAnimatedBox(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "CsAnimatedBox",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val (enter, exit) = CsTransitions.scaleFade()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = label,
        content = content
    )
}

/**
 * Keeps a "working" indicator alive for a beat after the last change, so a spinner driven by chunked
 * download updates does not strobe off and on between chunks.
 */
@Composable
fun rememberCsSettling(changing: Boolean, settleMillis: Int = CsDuration.Normal): Boolean {
    var settling by remember { mutableStateOf(false) }
    LaunchedEffect(changing) {
        if (changing) {
            settling = true
        } else if (settling) {
            delay(CsMotion.ms(settleMillis).toLong())
            settling = false
        }
    }
    return changing || settling
}

/** True while [value] has changed within the last [CsDuration.Normal] — for a just-changed highlight. */
@Composable
fun rememberCsIsAnimating(value: Float): Boolean {
    var previous by remember { mutableFloatStateOf(value) }
    var moving by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        moving = abs(value - previous) > 0.0001f
        previous = value
        if (moving) {
            delay(CsMotion.ms(CsDuration.Normal).toLong())
            moving = false
        }
    }
    return moving
}
