/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.utils.animation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import com.movtery.zalithlauncher.ui.design.CsDuration
import com.movtery.zalithlauncher.ui.design.CsEasing
import com.movtery.zalithlauncher.ui.design.CsMotion
import com.movtery.zalithlauncher.ui.design.csArriveTween
import com.movtery.zalithlauncher.ui.design.csEnterTween
import com.movtery.zalithlauncher.ui.design.csExitTween
import com.movtery.zalithlauncher.ui.design.csTween

/**
 * Animation spec helpers used across the launcher's screens.
 *
 * Every function here now routes through the [CsMotion] system in `ui/design`, so the whole app
 * shares one duration ramp, one curve library and one speed dial. Nothing about the UI changes —
 * only how it moves.
 *
 * The durations these return are already scaled by the launcher's animation switch and speed
 * setting, and collapse to an instant `snap()` when the user asks for no animation.
 */

/**
 * The *base* duration for general content animation.
 *
 * Deliberately unscaled: the [csTween] factories below already apply the launcher's speed dial, and
 * scaling here as well would apply it twice — a 2x dial would then run at 4x.
 */
private fun contentBaseDuration(): Int = SwapTimeline.CONTENT_BASE

/**
 * A general-purpose tween at the launcher's content speed, with an optional start delay.
 *
 * Re-timed through [CsMotion] and curved with [CsEasing.Standard] instead of the platform's
 * symmetric `FastOutSlowInEasing`.
 */
fun <E> getAnimateTween(
    delayMillis: Int = 0
): FiniteAnimationSpec<E> = getAnimateTween(
    durationMillis = contentBaseDuration(),
    delayMillis = delayMillis
)

/** A general-purpose tween at an explicit duration, with an optional start delay. */
fun <E> getAnimateTween(
    durationMillis: Int,
    delayMillis: Int = 0
): FiniteAnimationSpec<E> = csTween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = CsEasing.Standard
)

/** A general-purpose tween with an explicit curve. */
fun <E> getAnimateTween(
    durationMillis: Int,
    delayMillis: Int,
    easing: Easing
): FiniteAnimationSpec<E> = csTween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = easing
)

/**
 * The entry tween: content arriving on screen, using the two-phase [CsEasing.Arrive] curve so it
 * covers most of the distance quickly and then settles, rather than gliding in at a constant rate.
 */
fun <E> getEnterTween(
    delayMillis: Int = 0
): FiniteAnimationSpec<E> = csArriveTween(
    durationMillis = contentBaseDuration(),
    delayMillis = delayMillis
)

// ---------------------------------------------------------------------------
// New entry points — prefer these in new code
// ---------------------------------------------------------------------------

/** Something entering the screen. Decelerating, [CsDuration.Slow] by default. */
fun <E> csEnterSpec(durationMillis: Int = CsDuration.Slow, delayMillis: Int = 0): FiniteAnimationSpec<E> =
    csEnterTween(durationMillis, delayMillis)

/** Something leaving the screen. Accelerating and deliberately quicker than the entry. */
fun <E> csExitSpec(durationMillis: Int = CsDuration.Fast, delayMillis: Int = 0): FiniteAnimationSpec<E> =
    csExitTween(durationMillis, delayMillis)

/**
 * A large positional move: content travelling more than about 24 dp, where a single tween reads as
 * either sluggish or abrupt.
 */
fun <E> csArriveSpec(durationMillis: Int = CsDuration.Slow, delayMillis: Int = 0): FiniteAnimationSpec<E> =
    csArriveTween(durationMillis, delayMillis)
