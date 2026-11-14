/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.utils.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * 获取通用动画的持续时长
 */
private fun getAnimateSpeed(): Int = SwapTimeline.time(SwapTimeline.CONTENT_BASE)

fun <E> getAnimateTween(
    delayMillis: Int = 0
): FiniteAnimationSpec<E> = getAnimateTween(
    durationMillis = getAnimateSpeed(),
    delayMillis = delayMillis
)

fun <E> getAnimateTween(
    durationMillis: Int,
    delayMillis: Int = 0
): FiniteAnimationSpec<E> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = QuickSpringEasing
)

fun <E> getEnterTween(
    delayMillis: Int = 0
): FiniteAnimationSpec<E> = tween(
    durationMillis = getAnimateSpeed(),
    delayMillis = delayMillis,
    easing = EnterTranslateEasing
)

/**
 * 新：条目进入时的错峰延迟（第 [index] 项），用于首页卡片/好友列表的逐条弹入。
 */
fun <E> staggeredEnter(
    index: Int,
    baseDelayMillis: Int = 60,
    durationMillis: Int = getAnimateSpeed()
): FiniteAnimationSpec<E> = tween(
    durationMillis = durationMillis,
    delayMillis = baseDelayMillis + index * 45,
    easing = BounceEasing
)

/**
 * 新：按钮按下时的快速缩放，比原来的 csPressScale 更脆。
 */
fun <E> pressScaleSpec(
    durationMillis: Int = 110
): FiniteAnimationSpec<E> = tween(
    durationMillis = durationMillis,
    easing = FastEaseOutEasing
)
