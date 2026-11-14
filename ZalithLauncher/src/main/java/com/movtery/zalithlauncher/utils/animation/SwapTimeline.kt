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

import com.movtery.zalithlauncher.ui.design.CsDuration
import com.movtery.zalithlauncher.ui.design.CsMotion
import com.movtery.zalithlauncher.ui.design.CsStagger

/**
 * The serial timeline for a page swap, re-tuned for touch.
 *
 * The swap is deliberately *serial*: the outgoing screen clears, there is a short gap, then the
 * incoming screen arrives. On a phone the two never overlap, because a partially-visible outgoing
 * page competes with the incoming one for the same small area and reads as a glitch rather than a
 * transition.
 *
 * All values are **base milliseconds** — put them through [time] / [swapTime] before use so the
 * launcher's animation switch, its speed dial and the system animator scale all apply.
 *
 * What changed versus the previous tuning:
 *  - Exits got longer (70 → 130 ms). A 70 ms exit is under five frames at 60 Hz, which reads as a
 *    hard cut and makes rapid navigation feel flickery rather than fast.
 *  - Entries got marginally shorter (350 → 340 ms) and now share one duration with [CsDuration.Slow],
 *    so a page swap and a card arrival move at the same tempo.
 *  - The stagger cap dropped from 250 → 300 ms but now shares [CsStagger]'s curve, so long lists
 *    stop cascading item-by-item and arrive as a group past the tenth row.
 *  - The gap widened slightly (30 → 40 ms) to keep the two phases legible at 3x speed.
 */
object SwapTimeline {

    /** How long one item on the outgoing page takes to travel away. */
    const val EXIT_ITEM = 130

    /** How long one item on the outgoing page takes to fade. */
    const val EXIT_ALPHA = 90

    /** Delay between neighbouring items leaving the outgoing page. */
    const val EXIT_STAGGER = 18

    /** Ceiling on any single outgoing item's stagger delay. */
    const val EXIT_STAGGER_CAP = 90

    /** The whole window the outgoing page owns before the incoming one may start. */
    const val EXIT_WINDOW = EXIT_ITEM + EXIT_STAGGER_CAP

    /** Pause between the outgoing page finishing and the incoming page starting. */
    const val GAP = 40

    /** How long one item on the incoming page takes to travel into place. */
    const val ENTER_ITEM = CsDuration.Slow

    /** How long one item on the incoming page takes to fade in. */
    const val ENTER_ALPHA = 180

    /** Delay between neighbouring items arriving on the incoming page. */
    const val ENTER_STAGGER = CsStagger.Step

    /** Ceiling on any single incoming item's stagger delay. */
    const val ENTER_STAGGER_CAP = CsStagger.Cap

    /** The base duration for general content animation. */
    const val CONTENT_BASE = CsDuration.Slow

    /**
     * How far the outgoing page travels relative to the incoming one.
     *
     * Under a third: the page that is leaving attention should not compete with the page that is
     * claiming it. Matches the ratio baked into `CsTransitions.fadeThrough`.
     */
    const val EXIT_DISTANCE_SCALE = 0.34f

    /**
     * @return whether page-swap animation is switched off (user setting, or system animator scale 0)
     */
    fun isSwapClosed(): Boolean = !CsMotion.enabled

    /**
     * @return the combined speed multiplier — launcher dial × system animator scale
     */
    fun speedScale(): Float = CsMotion.speed()

    /**
     * @return [ms] scaled by the speed multiplier; 0 when animation is off
     */
    fun time(ms: Int): Int = CsMotion.ms(ms)

    /**
     * @return [ms] scaled by the speed multiplier, forced to 0 when page-swap animation is off
     */
    fun swapTime(ms: Int): Int = if (isSwapClosed()) 0 else time(ms)

    /**
     * @return the stagger delay for incoming item [index]
     */
    fun enterStagger(index: Int): Int = CsStagger.delayFor(index)

    /**
     * @return the stagger delay for outgoing item [index]
     */
    fun exitStagger(index: Int): Int = swapTime(minOf(index * EXIT_STAGGER, EXIT_STAGGER_CAP))

    /**
     * @return the stagger delay for outgoing item [index] of [count], reversed so the list rolls away
     */
    fun exitStaggerReversed(index: Int, count: Int): Int = CsStagger.exitDelayFor(index, count)
}

/**
 * The delay window for one page swap.
 * @param exitDelayMillis how long the outgoing scene layer waits before it starts to fade
 * @param enterDelayMillis how long the incoming scene layer waits before it starts to fade in
 */
class SwapWindow(
    val exitDelayMillis: Int,
    val enterDelayMillis: Int
) {
    /** The full window this swap occupies, for callers that need to defer work until it settles. */
    val totalMillis: Int get() = exitDelayMillis + enterDelayMillis

    override fun toString(): String = "SwapWindow(exit=$exitDelayMillis, enter=$enterDelayMillis)"
}

/**
 * Keeps rapid navigation coherent.
 *
 * Tapping through several screens quickly would otherwise restart a full exit-then-enter sequence
 * for each hop, so the animations pile up and the UI feels like it is always catching up. Once a
 * swap is under way, a follow-up hop that continues it reuses the same window instead of opening a
 * new one — the pages keep flowing in the direction the user is already going.
 */
object SwapClock {

    /** The scene the current swap segment started from; null when no swap is running. */
    @Volatile
    private var swapInitial: Any? = null

    /** The scene the current swap segment is heading to; null when no swap is running. */
    @Volatile
    private var swapTarget: Any? = null

    /** When the outgoing content finishes leaving, in ms. */
    @Volatile
    private var exitEndAtMillis: Long = 0

    /** When the incoming content is allowed to arrive, in ms. */
    @Volatile
    var enterAtMillis: Long = 0
        private set

    /**
     * Records a change of scene segment and works out this swap's exit and enter delays.
     *
     * @param initial the segment's starting scene
     * @param target the segment's destination scene
     */
    @Synchronized
    fun record(initial: Any?, target: Any?): SwapWindow {
        val now = System.nanoTime() / 1_000_000
        if (initial != target) {
            // Only a real scene change counts as a swap. Otherwise this is just a segment settling
            // back down — first composition, or the swap animation finishing.
            val continuing = now < enterAtMillis &&
                    (initial == swapInitial || initial == swapTarget ||
                            target == swapInitial || target == swapTarget)
            if (!continuing) {
                swapInitial = initial
                swapTarget = target
                exitEndAtMillis = now + SwapTimeline.swapTime(SwapTimeline.EXIT_WINDOW)
                enterAtMillis = exitEndAtMillis + SwapTimeline.swapTime(SwapTimeline.GAP)
            }
        }
        return SwapWindow(
            exitDelayMillis = (exitEndAtMillis - now).coerceAtLeast(0).toInt(),
            enterDelayMillis = (enterAtMillis - now).coerceAtLeast(0).toInt()
        )
    }

    /** Forgets any swap in progress, so the next navigation opens a fresh window. */
    @Synchronized
    fun reset() {
        swapInitial = null
        swapTarget = null
        exitEndAtMillis = 0
        enterAtMillis = 0
    }
}
