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

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import kotlin.math.roundToInt

/**
 * CS Launcher Lite motion system — the single source of truth for how the app moves.
 *
 * Design goals (mobile-first):
 *  - **Fast to the touch.** Nothing the user waits on runs longer than [CsDuration.Slow]; presses
 *    answer in [CsDuration.Instant] so the UI never feels like it trails the finger.
 *  - **Physics over timers.** Gesture-driven motion uses springs, so an interrupted animation
 *    resumes from its current velocity instead of restarting — that is what makes it feel native.
 *  - **Cheap on the GPU.** Effects animate `graphicsLayer` properties (scale / alpha / translation)
 *    or a draw-phase brush. Nothing here invalidates measure or layout, so it holds 60 fps on
 *    low-end arm64 devices and 120 fps on high-refresh panels.
 *  - **Respectful.** Honours the launcher's animation switch and speed dial, plus the system-wide
 *    "remove animations" accessibility setting, degrading to instant/cross-fade rather than cutting.
 *  - **Direction aware.** Horizontal travel is a fraction of the container, so it scales to any
 *    screen size and mirrors correctly under RTL.
 *
 * Nothing here changes what the UI *looks* like — only how it moves.
 */

// ---------------------------------------------------------------------------
// Global gate
// ---------------------------------------------------------------------------

/**
 * Master switch and speed dial for the whole motion system.
 *
 * Reads straight through to the launcher settings, whose `state` is an observable Compose value —
 * so the existing "Launcher animations" toggle and "Animation speed" slider in Settings keep
 * working unchanged and now govern the new system too. Reading it during composition automatically
 * subscribes the caller, and re-times every spec when the dial moves.
 */
object CsMotion {

    /**
     * System-wide animator duration scale (Settings → Developer options → Animation scale),
     * cached in a volatile so no draw pass ever touches a content resolver.
     * Updated from [CsMotionGate]; 1f until then.
     */
    @Volatile
    var systemMotionScale: Float = 1f

    /** `false` when the user — or the OS — asked for no animation at all. */
    val enabled: Boolean
        get() = AllSettings.launcherSwapAnimEnabled.state && systemMotionScale > 0.01f

    /**
     * Combined multiplier: the launcher dial (10/10 = 1.0x) times the system animator scale.
     * Clamped so a 3x dial can never yield a zero or negative duration.
     */
    fun speed(): Float {
        val dial = (AllSettings.launcherAnimateSpeedLevel.state / 10f).coerceIn(0.1f, 3f)
        return (dial * systemMotionScale.coerceIn(0f, 3f)).coerceAtLeast(0.05f)
    }

    /** A *base* duration in ms, scaled by [speed]. Zero when motion is disabled. */
    fun ms(base: Int): Int = if (!enabled) 0 else (base / speed()).roundToInt().coerceAtLeast(0)

    /** Like [ms] but never zero — ambient loops must keep looping even at 3x. */
    fun msLoop(base: Int): Int = (base / speed()).roundToInt().coerceAtLeast(1)

    /** Travel distance collapses to zero when motion is off, degrading slides into cross-fades. */
    fun fraction(base: Float): Float = if (enabled) base else 0f

    /** Travel in dp, collapsed to [Dp.Zero] when motion is off. */
    fun travel(base: Dp): Dp = if (enabled) base else 0.dp
}

// ---------------------------------------------------------------------------
// Durations
// ---------------------------------------------------------------------------

/**
 * Duration ramp, in *base* milliseconds. Always pass these through [CsMotion.ms], or use the
 * [csTween] family, which does it for you.
 *
 * Deliberately compressed versus a desktop design system: on a phone the entire screen sits in
 * the user's peripheral vision, so long animations read as latency rather than polish.
 */
object CsDuration {
    /** Immediate feedback — press states, ripples, icon swaps. */
    const val Instant = 90

    /** Micro-interactions — toggles, chips, small colour shifts. */
    const val Fast = 150

    /** The workhorse — cards, panels, most content changes. */
    const val Normal = 240

    /** Screen-level moves — page swaps, sheets, rail switches. */
    const val Slow = 340

    /** Only for things the user watches on purpose — hero banner, first-run moments. */
    const val Deliberate = 520

    /** Ambient loops — shimmer, breathing status dot, hosting pulse. */
    const val Loop = 1400

    /** Long ambient loops — slow sweeps, background drift. */
    const val LoopSlow = 2600
}

// ---------------------------------------------------------------------------
// Easings
// ---------------------------------------------------------------------------

/**
 * Curve library. Hand-tuned cubic béziers rather than the platform defaults: `FastOutSlowInEasing`
 * is symmetric and reads as mechanical, while these decelerate strongly — fast out of the gate,
 * long gentle settle — which is how physical objects move and what makes touch motion feel alive.
 */
object CsEasing {

    /** Anything entering the screen. Strong deceleration, no overshoot. */
    val Enter: Easing = CubicBezierEasing(0.05f, 0.70f, 0.10f, 1.00f)

    /** Anything leaving the screen. Accelerates away quickly and cleanly. */
    val Exit: Easing = CubicBezierEasing(0.30f, 0.00f, 0.80f, 0.15f)

    /** Direction-less value changes — colour, alpha, elevation. */
    val Standard: Easing = CubicBezierEasing(0.20f, 0.00f, 0.00f, 1.00f)

    /** Extra-pronounced deceleration for large surfaces — page swaps, panels, dialogs. */
    val Emphasized: Easing = CubicBezierEasing(0.05f, 0.85f, 0.08f, 1.00f)

    /** Decelerate-only half of [Emphasized], for interrupted or very large enter moves. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.70f, 0.10f, 1.00f)

    /** Accelerate-only half of [Emphasized], for large exit moves. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.30f, 0.00f, 1.00f, 1.00f)

    /** Single visible overshoot — reserved for the primary Play action and success moments. */
    val Expressive: Easing = CubicBezierEasing(0.34f, 1.56f, 0.34f, 1.00f)

    /** Near-linear — ambient loops, where a pronounced curve would visibly throb. */
    val Gentle: Easing = CubicBezierEasing(0.40f, 0.00f, 0.60f, 1.00f)

    /** Snap with a little grace — staggered list items, so long lists stay brisk. */
    val Snappy: Easing = CubicBezierEasing(0.12f, 0.90f, 0.20f, 1.00f)

    /**
     * Composite curve for positional entry — replaces the old two-segment `EnterTranslateEasing`.
     * Keeps its "arrive, then settle" character but drops the cosine wobble, which read as jitter
     * on 90/120 Hz panels.
     */
    val Arrive: Easing = Easing { f ->
        val t = f.coerceIn(0f, 1f)
        // 70% of the distance inside the first 35% of the time, then a long exponential settle.
        val sprint = EmphasizedDecelerate.transform((t / 0.35f).coerceAtMost(1f)) * 0.70f
        val settle = Enter.transform(t) * 0.30f
        (sprint + settle).coerceIn(0f, 1f)
    }
}

// ---------------------------------------------------------------------------
// Springs
// ---------------------------------------------------------------------------

/**
 * Spring presets. Preferred over tweens wherever the user can interrupt the motion — dragging,
 * rapid tapping, flinging — because a spring picks up the current velocity instead of restarting
 * a clock from zero.
 */
object CsSpring {

    /** Buttons, icon presses, chips — stiff, so the reply is immediate. */
    fun <T : Any> stiff(visibilityThreshold: T) = SpringSpec(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow * 1.55f,
        visibilityThreshold = visibilityThreshold
    )

    /** Cards, list items, inline expand/collapse. */
    fun <T : Any> medium(visibilityThreshold: T) = SpringSpec(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = visibilityThreshold
    )

    /** Large surfaces — panels, sheets, page-level moves. Slow to start, very smooth to land. */
    fun <T : Any> soft(visibilityThreshold: T) = SpringSpec(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = visibilityThreshold
    )

    /** One visible bounce. Play button and success moments only — never for repeated interactions. */
    fun <T : Any> bouncy(visibilityThreshold: T) = SpringSpec(
        dampingRatio = 0.48f,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    /** No bounce — for motion driven directly by a finger position (parallax, drag follow). */
    fun <T : Any> noBounce(visibilityThreshold: T) = SpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium * 2.5f,
        visibilityThreshold = visibilityThreshold
    )

    /** Float spring with the card preset. */
    fun float(stiffness: Float = Spring.StiffnessMediumLow, dampingRatio: Float = 0.80f): SpringSpec<Float> =
        SpringSpec(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = 0.001f)

    /** Dp spring with the card preset. */
    fun dp(stiffness: Float = Spring.StiffnessMediumLow, dampingRatio: Float = 0.80f): SpringSpec<Dp> =
        SpringSpec(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = Dp.VisibilityThreshold)

    /** IntOffset spring with the card preset. */
    fun offset(
        stiffness: Float = Spring.StiffnessMediumLow,
        dampingRatio: Float = 0.80f
    ): SpringSpec<IntOffset> = SpringSpec(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )
}

// ---------------------------------------------------------------------------
// Spec factories
// ---------------------------------------------------------------------------

/**
 * The tween factory every animation in the app should use.
 *
 * `durationMillis` and `delayMillis` are *base* values: they are run through [CsMotion.ms], so the
 * launcher's animation switch and speed dial apply automatically. When motion is disabled this
 * returns [snap] — a true instant value, never a 1 ms tween that can still tear a frame.
 *
 * Because [CsMotion.ms] reads observable settings state, calling this inside composition subscribes
 * the caller and re-times it live when the user moves the dial.
 */
fun <T> csTween(
    durationMillis: Int = CsDuration.Normal,
    delayMillis: Int = 0,
    easing: Easing = CsEasing.Standard
): FiniteAnimationSpec<T> = if (!CsMotion.enabled) snap() else tween(
    durationMillis = CsMotion.ms(durationMillis),
    delayMillis = CsMotion.ms(delayMillis),
    easing = easing
)

/** Entering the screen — decelerating curve by default. */
fun <T> csEnterTween(durationMillis: Int = CsDuration.Slow, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    csTween(durationMillis, delayMillis, CsEasing.Enter)

/** Leaving the screen — accelerating curve by default. */
fun <T> csExitTween(durationMillis: Int = CsDuration.Fast, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    csTween(durationMillis, delayMillis, CsEasing.Exit)

/** Large positional entry — the two-phase "arrive then settle" curve. */
fun <T> csArriveTween(durationMillis: Int = CsDuration.Slow, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    csTween(durationMillis, delayMillis, CsEasing.Arrive)

/** Ambient loop timing — never collapses to zero, but still respects the speed dial. */
fun <T> csLoopTween(durationMillis: Int = CsDuration.Loop, easing: Easing = CsEasing.Gentle): FiniteAnimationSpec<T> =
    tween(durationMillis = CsMotion.msLoop(durationMillis), easing = easing)

/** Spring that collapses to [snap] when motion is disabled. */
fun <T : Any> csSpring(
    dampingRatio: Float = 0.80f,
    stiffness: Float = Spring.StiffnessMediumLow,
    visibilityThreshold: T
): FiniteAnimationSpec<T> = if (!CsMotion.enabled) snap() else SpringSpec(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold
)

/** Float spring, card preset, motion-aware. */
fun csFloatSpring(
    dampingRatio: Float = 0.80f,
    stiffness: Float = Spring.StiffnessMediumLow
): FiniteAnimationSpec<Float> = csSpring(dampingRatio, stiffness, visibilityThreshold = 0.001f)

/** Dp spring, card preset, motion-aware. */
fun csDpSpring(
    dampingRatio: Float = 0.80f,
    stiffness: Float = Spring.StiffnessMediumLow
): FiniteAnimationSpec<Dp> = csSpring(dampingRatio, stiffness, visibilityThreshold = Dp.VisibilityThreshold)

/**
 * Keyframe spec whose checkpoint positions are expressed as *fractions* of the total duration,
 * so the shape of the curve survives re-timing by the speed dial.
 */
fun <T> csKeyframes(
    durationMillis: Int = CsDuration.Slow,
    delayMillis: Int = 0,
    block: CsKeyframeScope<T>.() -> Unit
): FiniteAnimationSpec<T> {
    if (!CsMotion.enabled) return snap()
    val total = CsMotion.ms(durationMillis)
    val scope = CsKeyframeScope<T>()
    scope.block()
    return keyframes {
        this.durationMillis = total
        this.delayMillis = CsMotion.ms(delayMillis)
        scope.frames.forEach { frame ->
            val timeStamp = (total * frame.fraction).roundToInt()
            val easing = frame.easing
            if (easing != null) frame.value at timeStamp with easing else frame.value at timeStamp
        }
    }
}

/**
 * Receiver for [csKeyframes].
 *
 * ```
 * csKeyframes<Float>(CsDuration.Slow) {
 *     0f at 0f
 *     0.6f at 1.06f with CsEasing.Snappy
 *     1f at 1f
 * }
 * ```
 */
class CsKeyframeScope<T> {
    class Frame<T>(val fraction: Float, val value: T, var easing: Easing?)

    val frames: MutableList<Frame<T>> = mutableListOf()

    /** Place this value at [fraction] of the total duration. */
    @Suppress("UNCHECKED_CAST")
    infix fun T.at(fraction: Float): Frame<T> =
        Frame(fraction.coerceIn(0f, 1f), this, null).also { frames += it }

    /** Attach the outgoing easing for the segment starting at this checkpoint. */
    infix fun Frame<T>.with(easing: Easing): Frame<T> = apply { this.easing = easing }
}

// ---------------------------------------------------------------------------
// Stagger
// ---------------------------------------------------------------------------

/**
 * Staggering for lists and grids.
 *
 * The per-item delay is capped, and items past [MaxTracked] arrive together, so a 200-row instance
 * list never spends seconds cascading in. Uncapped stagger is the single most common reason a
 * mobile UI feels slow, so it gets its own type instead of being inlined at each call site.
 */
object CsStagger {

    /** Delay between neighbouring items, base ms. */
    const val Step = 34

    /** Hard ceiling on any single item's delay, base ms. */
    const val Cap = 300

    /** Items beyond this stop adding delay and arrive together. */
    const val MaxTracked = 10

    /** Base (unscaled) delay for item [index]. */
    fun base(index: Int): Int {
        if (index <= 0) return 0
        return minOf(index.coerceAtMost(MaxTracked) * Step, Cap)
    }

    /** Scaled enter delay for item [index]. */
    fun delayFor(index: Int): Int = CsMotion.ms(base(index))

    /**
     * Scaled exit delay for item [index] of [count].
     *
     * Exits run at 45% of the enter tempo and in reverse order — the last item leaves first, so the
     * content reads as rolling away rather than collapsing in place, and the screen clears fast
     * enough that the next screen is never waiting on it.
     */
    fun exitDelayFor(index: Int, count: Int = 0): Int {
        val reversed = if (count > 0) (count - 1 - index).coerceAtLeast(0) else index
        return CsMotion.ms((base(reversed) * 0.45f).roundToInt())
    }
}

// ---------------------------------------------------------------------------
// Transition factories
// ---------------------------------------------------------------------------

/**
 * Ready-made enter/exit pairs. Every screen, panel and card transition should pick one of these
 * rather than hand-rolling `fadeIn(tween(...))`, so the whole launcher moves as a single object.
 *
 * Horizontal and vertical travel is a *fraction* of the container, which keeps the motion
 * proportional on a 5" phone and a 13" tablet alike, and mirrors correctly under RTL because
 * Compose resolves `slideIn` against the layout direction.
 */
object CsTransitions {

    /** Standard content swap — the incoming surface nudges in from the direction of travel. */
    fun fadeThrough(
        fraction: Float = 0.06f,
        forward: Boolean = true
    ): Pair<EnterTransition, ExitTransition> {
        val f = CsMotion.fraction(fraction)
        val dir = if (forward) 1 else -1
        val enter = fadeIn(csEnterTween(CsDuration.Slow)) +
                slideIn(
                        animationSpec = csArriveTween(CsDuration.Slow),
                        initialOffset = { size -> offset(size, f * dir, 0f) }
                    )
        // The outgoing surface travels a third as far: it is leaving attention, not competing for it.
        val exit = fadeOut(csExitTween(CsDuration.Fast)) +
                slideOut(
                        animationSpec = csExitTween(CsDuration.Fast),
                        targetOffset = { size -> offset(size, -f * 0.34f * dir, 0f) }
                    )
        return enter to exit
    }

    /** Sheet or panel rising from the bottom edge. */
    fun riseUp(fraction: Float = 0.22f): Pair<EnterTransition, ExitTransition> {
        val f = CsMotion.fraction(fraction)
        val enter = fadeIn(csEnterTween(CsDuration.Slow)) +
                slideIn(
                        animationSpec = csArriveTween(CsDuration.Slow),
                        initialOffset = { size -> offset(size, 0f, f) }
                    )
        val exit = fadeOut(csExitTween(CsDuration.Fast)) +
                slideOut(
                        animationSpec = csExitTween(CsDuration.Fast),
                        targetOffset = { size -> offset(size, 0f, f * 0.5f) }
                    )
        return enter to exit
    }

    /** Content dropping from the top edge — account chip, notifications, banners. */
    fun dropDown(fraction: Float = 0.16f): Pair<EnterTransition, ExitTransition> {
        val f = CsMotion.fraction(fraction)
        val enter = fadeIn(csEnterTween(CsDuration.Normal)) +
                slideIn(
                        animationSpec = csArriveTween(CsDuration.Normal),
                        initialOffset = { size -> offset(size, 0f, -f) }
                    )
        val exit = fadeOut(csExitTween(CsDuration.Fast)) +
                slideOut(
                        animationSpec = csExitTween(CsDuration.Fast),
                        targetOffset = { size -> offset(size, 0f, -f * 0.5f) }
                    )
        return enter to exit
    }

    /** Card or dialog materialising from its own centre — a light container-transform feel. */
    fun scaleFade(initialScale: Float = 0.94f): Pair<EnterTransition, ExitTransition> {
        val enter = fadeIn(csEnterTween(CsDuration.Normal)) +
                scaleIn(csFloatSpring(0.62f, Spring.StiffnessMediumLow), initialScale = initialScale)
        val exit = fadeOut(csExitTween(CsDuration.Fast)) +
                scaleOut(csExitTween(CsDuration.Fast), targetScale = initialScale)
        return enter to exit
    }

    /** Inline content growing open — filters, advanced sections, chat composer. */
    fun expand(vertical: Boolean = true): Pair<EnterTransition, ExitTransition> {
        val enterSpec = csArriveTween<IntSize>(CsDuration.Normal)
        val exitSpec = csExitTween<IntSize>(CsDuration.Fast)
        val enter = fadeIn(csEnterTween(CsDuration.Normal)) +
                if (vertical) expandVertically(enterSpec) else expandHorizontally(enterSpec)
        val exit = fadeOut(csExitTween(CsDuration.Fast)) +
                if (vertical) shrinkVertically(exitSpec) else shrinkHorizontally(exitSpec)
        return enter to exit
    }

    /**
     * Rail / tab switch. Tabs change constantly, so this is deliberately subtle — a short
     * directional nudge rather than a full slide, which would make rapid tab-hopping feel drunk.
     */
    fun tabSwitch(fromIndex: Int, toIndex: Int): Pair<EnterTransition, ExitTransition> =
        fadeThrough(fraction = 0.04f, forward = toIndex >= fromIndex)

    /** Pure cross-fade. The universal fallback, and all that is left when motion is disabled. */
    fun crossFade(): Pair<EnterTransition, ExitTransition> =
        fadeIn(csEnterTween(CsDuration.Normal)) to fadeOut(csExitTween(CsDuration.Fast))

    private fun offset(size: IntSize, xFraction: Float, yFraction: Float) = IntOffset(
        x = (size.width * xFraction).roundToInt(),
        y = (size.height * yFraction).roundToInt()
    )
}

/**
 * Turn an enter/exit pair from [CsTransitions] into a [ContentTransform] for `AnimatedContent`.
 *
 * ```
 * AnimatedContent(
 *     targetState = section,
 *     transitionSpec = { CsTransitions.tabSwitch(0, 2).toContentTransform() }
 * ) { ... }
 * ```
 */
fun Pair<EnterTransition, ExitTransition>.toContentTransform(): ContentTransform =
    first togetherWith second
