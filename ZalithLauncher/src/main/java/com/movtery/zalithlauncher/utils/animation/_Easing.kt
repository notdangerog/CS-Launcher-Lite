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

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * 平滑结束缓动（保留，用于旧页面退场）
 */
class EaseOutFluent(private val power: Int) : Easing {
    override fun transform(fraction: Float): Float = 1f - (1f - fraction).coerceIn(0f, 1f).pow(power)
}

/**
 * 回弹结束缓动（保留，用于旧页面退场）
 */
class EaseOutBack(power: Int) : Easing {
    private val exponent = 3f - power * 0.5f

    override fun transform(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        return 1f - (1f - t).pow(exponent) * cos(1.5f * Math.PI * t).toFloat()
    }
}

/**
 * 新：弹性回弹缓动——条目弹入时的“咔”感，适合首页卡片与好友列表逐条出现。
 */
val BounceEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    val a = 0.05f
    val b = 0.3f
    if (t == 0f || t == 1f) t
    else -(a * kotlin.math.pow(2f, 10f * (t - 1f)) * sin((t - 1f - b / (4f * Math.PI)) * 2f * Math.PI / b)).toFloat()
}

/**
 * 新：极快启动、慢速收尾——按钮按下与弹出层的快速响应。
 */
val FastEaseOutEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    1f - (1f - t) * (1f - t) * (1f - t)
}

/**
 * 新：快速启动、轻微超调再回位——列表项选中时的弹簧胶囊。
 */
val QuickSpringEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    if (t < 0.5f) {
        2f * t * t
    } else {
        val x = 1f - t
        1f - 2f * x * x
    }
}

/**
 * 新：柔和的慢速淡出——对话框与遮罩层的退场。
 */
val SlowEaseOutEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    1f - kotlin.math.pow(1f - t, 3f)
}

/**
 * 进场位移合成曲线（保留，供 SwapTimeline 使用）
 */
val EnterTranslateEasing = Easing { fraction ->
    // 进场位移第一段曲线
    val first = 5f * EaseOutFluent(3).transform((fraction * 350f / 250f).coerceAtMost(1f))
    // 进场位移第二段曲线
    val second = 11f * EaseOutBack(3).transform(fraction)
    (first + second) / 16f
}

/**
 * 新：页面切换时的快速弹性位移，比原来的 EnterTranslateEasing 更脆。
 */
val PageSwapEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    if (t < 0.35f) {
        2.857f * t * t
    } else if (t < 0.7f) {
        val x = t - 0.35f
        0.35f + 1.2f * x - 0.8f * x * x
    } else {
        val x = t - 0.7f
        0.83f + 0.5f * x
    }.coerceAtMost(1f)
}

/**
 * 新：爱心/好友气泡的跳动缓动（1.2 倍超调再收回）。
 */
val PulseEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    if (t < 0.5f) {
        4f * t * t * (t + 1f)
    } else {
        val x = t - 0.5f
        1f - 4f * x * x * (2f - t)
    }
}

/**
 * 新：边缘滑动进入时的快速回弹，侧边栏展开/收起使用。
 */
val EdgeSlideEasing = FastOutSlowInEasing
