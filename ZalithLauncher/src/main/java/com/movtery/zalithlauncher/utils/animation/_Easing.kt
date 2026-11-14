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
import com.movtery.zalithlauncher.ui.design.CsEasing
import kotlin.math.cos
import kotlin.math.pow

/**
 * Easing curves for the launcher, now backed by the [CsEasing] library.
 *
 * These types are kept — 30-odd call sites across the app construct them — but the curves behind
 * them have been replaced. The old versions were pure power functions, which start with infinite
 * acceleration and read as "mechanical" on a touchscreen; the replacements are shaped béziers that
 * leave the gate fast and settle gently, matching how a physical object actually stops.
 *
 * [power] is still honoured, but as a *character* dial rather than a raw exponent: higher values
 * lean harder into the deceleration instead of simply raising it to a bigger power.
 */

/**
 * Smooth-deceleration ease-out.
 *
 * Interpolates between [CsEasing.Standard] (power 1) and [CsEasing.Emphasized] (power 5+), so
 * existing `EaseOutFluent(3)` call sites keep their relative weighting while gaining a shaped curve.
 */
class EaseOutFluent(power: Int) : Easing {

    private val weight: Float = ((power - 1).coerceIn(0, 4)) / 4f

    override fun transform(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        val soft = CsEasing.Standard.transform(t)
        val strong = CsEasing.Emphasized.transform(t)
        return soft + (strong - soft) * weight
    }
}

/**
 * Overshooting ease-out — arrives past the target and settles back.
 *
 * The previous implementation multiplied a power curve by `cos(1.5π·t)`, which crosses zero and
 * makes the value *reverse* through the start point before overshooting. On a 90/120 Hz panel that
 * reads as a visible jitter. This version uses a single damped cosine so it overshoots exactly once
 * and then decays monotonically — a bounce, not a wobble.
 */
class EaseOutBack(power: Int) : Easing {

    /** Overshoot amplitude: power 1 barely overshoots, power 4 is a clear bounce. */
    private val amplitude: Float = (0.06f + power.coerceIn(1, 6) * 0.035f).coerceAtMost(0.24f)

    /** How many oscillations fit in the window; kept below one full cycle so it never reverses twice. */
    private val frequency: Float = 1.5f

    override fun transform(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        if (t == 0f) return 0f
        if (t == 1f) return 1f
        // Decelerating base curve, plus a single damped cosine bump that has fully decayed by t = 1.
        val base = CsEasing.EmphasizedDecelerate.transform(t)
        val decay = (1f - t).pow(2)
        val bump = cos((1f - t) * frequency * Math.PI.toFloat()) * decay
        return (base + amplitude * bump).coerceIn(-0.05f, 1f + amplitude)
    }
}

/**
 * The launcher's positional entry curve — delegates to [CsEasing.Arrive].
 *
 * Same "arrive, then settle" character as before, but built from two shaped segments instead of a
 * weighted sum of a power curve and a cosine, which removes the overshoot past 1.0 and the jitter
 * that came with it.
 */
val EnterTranslateEasing: Easing = CsEasing.Arrive
