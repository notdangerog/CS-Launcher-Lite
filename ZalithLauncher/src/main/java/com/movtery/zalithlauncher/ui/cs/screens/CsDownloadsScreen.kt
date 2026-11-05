/*
 * CS Launcher Lite
 *
 * Derived from Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.movtery.zalithlauncher.ui.cs.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.resolveAndroidString
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsEmptyState
import com.movtery.zalithlauncher.ui.cs.components.CsSecondaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsSectionHeader
import com.movtery.zalithlauncher.ui.cs.data.CsBrowseCategory
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.utils.file.formatFileSize

/**
 * Downloads — the single place where every running install/update task is visible and cancellable,
 * plus shortcuts into the content categories of Discover.
 *
 * Uses the launcher's real [TaskSystem]; nothing here is simulated.
 */
@Composable
fun CsDownloadsScreen(
    onBrowseCategory: (CsBrowseCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.cs_nav_downloads),
            style = MaterialTheme.typography.headlineMedium,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = stringResource(R.string.cs_downloads_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = CsColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        CsSectionHeader(title = stringResource(R.string.cs_downloads_active))
        Spacer(modifier = Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            CsCard(modifier = Modifier.fillMaxWidth()) {
                CsEmptyState(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 12.dp),
                    title = stringResource(R.string.cs_downloads_empty_title),
                    description = stringResource(R.string.cs_downloads_empty_desc)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                tasks.forEach { task ->
                    TaskRow(task = task)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        CsSectionHeader(title = stringResource(R.string.cs_downloads_browse))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.cs_downloads_browse_desc),
            style = MaterialTheme.typography.bodySmall,
            color = CsColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            BrowseRow(
                title = stringResource(R.string.cs_discover_tab_modpacks),
                iconRes = R.drawable.ic_box,
                onClick = { onBrowseCategory(CsBrowseCategory.MODPACKS) }
            )
            BrowseRow(
                title = stringResource(R.string.cs_discover_tab_mods),
                iconRes = R.drawable.ic_extension_outlined,
                onClick = { onBrowseCategory(CsBrowseCategory.MODS) }
            )
            BrowseRow(
                title = stringResource(R.string.cs_discover_tab_resourcepacks),
                iconRes = R.drawable.ic_image_outlined,
                onClick = { onBrowseCategory(CsBrowseCategory.RESOURCE_PACKS) }
            )
            BrowseRow(
                title = stringResource(R.string.cs_discover_tab_shaders),
                iconRes = R.drawable.ic_style_outlined,
                onClick = { onBrowseCategory(CsBrowseCategory.SHADERS) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun TaskRow(task: Task) {
    val progress by task.progress.collectAsStateWithLifecycle()
    val message by task.message.collectAsStateWithLifecycle()
    val speed by task.rateBytesPerSec.collectAsStateWithLifecycle()

    val title = remember(message) {
        message?.let { resolveAndroidString(it).text }
            ?: resolveAndroidString(androidText(R.string.cs_downloads_task_fallback)).text
    }

    CsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CsColors.AccentSoft, CsColors.CardElevated)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_download_2_outlined),
                        contentDescription = null,
                        tint = CsColors.Accent
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                CsSecondaryButton(
                    text = stringResource(R.string.cs_downloads_cancel),
                    onClick = { TaskSystem.cancelTask(task) },
                    iconRes = R.drawable.ic_close,
                    compact = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (progress >= 0f) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    progress = { progress.coerceIn(0f, 1f) },
                    color = CsColors.Accent,
                    trackColor = CsColors.CardElevated
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CsColors.Accent,
                    trackColor = CsColors.CardElevated
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (progress >= 0f) {
                        "${(progress.coerceIn(0f, 1f) * 100).toInt()}%"
                    } else {
                        stringResource(R.string.cs_downloads_preparing)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val rate = speed
                if (rate != null) {
                    Text(
                        text = "${formatFileSize(rate)}/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseRow(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = CsColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier.size(15.dp),
                painter = painterResource(R.drawable.ic_cs_chevron_right),
                contentDescription = null,
                tint = CsColors.TextTertiary
            )
        }
    }
}
