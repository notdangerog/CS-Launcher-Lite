/*
 * CS Launcher Lite
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * Based on Zalith Launcher 2 (GPL-3.0). This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsEmptyState
import com.movtery.zalithlauncher.ui.design.CsSectionHeader
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsStatusPill

/**
 * CS Launcher Lite dashboard.
 *
 * Everything here is driven by state that already exists in the launcher:
 * VersionsManager provides the instances, the play action is routed through the
 * existing launch pipeline and the quick actions reuse the existing screens.
 */
@Composable
fun CsHomeScreen(
    currentVersion: Version?,
    versions: List<Version>,
    runningTasks: Int,
    modifier: Modifier = Modifier,
    onPlay: (Version) -> Unit,
    onOpenInstance: (Version) -> Unit,
    onOpenInstances: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenClassicHome: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenFiles: () -> Unit,
    onImportModpack: (Uri) -> Unit
) {
    // Modpack import reuses the existing ModpackImporter through the activity picker.
    val modpackPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onImportModpack(uri) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CsSpace.l, vertical = CsSpace.s),
        horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
    ) {
        // Main column: greeting, hero instance and recent instances
        Column(
            modifier = Modifier
                .weight(1.65f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            GreetingHeader()

            val hero = currentVersion ?: versions.firstOrNull()
            if (hero != null) {
                CsInstanceHeroCard(
                    modifier = Modifier.fillMaxWidth(),
                    version = hero,
                    isCurrent = true,
                    onPlay = { onPlay(hero) },
                    onSettings = { onOpenInstance(hero) }
                )
            } else {
                CsCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    CsEmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        iconRes = R.drawable.ic_sports_esports_outlined,
                        title = stringResource(R.string.cs_home_no_instance_title),
                        message = stringResource(R.string.cs_home_no_instance_message),
                        actionLabel = stringResource(R.string.cs_quick_download),
                        onAction = onOpenDiscover
                    )
                }
            }

            val recent = remember(versions, hero) {
                versions.filter { it != hero }.take(3)
            }
            if (recent.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CsSectionHeader(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.cs_home_recent),
                        subtitle = stringResource(R.string.cs_home_recent_desc)
                    ) {
                        CsLinkButton(
                            text = stringResource(R.string.cs_home_view_all),
                            onClick = onOpenInstances
                        )
                    }
                }

                recent.forEach { version ->
                    CsInstanceRowCard(
                        version = version,
                        isCurrent = false,
                        onPlay = { onPlay(version) },
                        onSettings = { onOpenInstance(version) },
                        onSetCurrent = { onOpenInstance(version) }
                    )
                }
            }
        }

        // Side column: quick actions and activity
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            CsSectionHeader(title = stringResource(R.string.cs_home_quick_actions))

            CsCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(CsSpace.s),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_download,
                            label = stringResource(R.string.cs_quick_download),
                            onClick = onOpenDiscover
                        )
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_folder_zip_filled,
                            label = stringResource(R.string.cs_quick_import_modpack),
                            onClick = { modpackPicker.launch("application/*") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_group_filled,
                            label = stringResource(R.string.cs_quick_multiplayer),
                            onClick = onOpenMultiplayer
                        )
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_person_outlined,
                            label = stringResource(R.string.cs_quick_account),
                            onClick = onOpenAccounts
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_folder_filled,
                            label = stringResource(R.string.cs_quick_files),
                            onClick = onOpenFiles
                        )
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_settings_filled,
                            label = stringResource(R.string.cs_quick_settings),
                            onClick = onOpenSettings
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsQuickAction(
                            modifier = Modifier.weight(1f),
                            iconRes = R.drawable.ic_dashboard_outlined,
                            label = stringResource(R.string.cs_home_classic),
                            onClick = onOpenClassicHome
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            CsSectionHeader(title = stringResource(R.string.cs_home_activity))
            CsCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(CsSpace.l),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = true
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(
                            if (runningTasks > 0) R.drawable.ic_assignment_filled
                            else R.drawable.ic_check
                        ),
                        contentDescription = null,
                        tint = if (runningTasks > 0) MaterialTheme.colorScheme.primary
                        else CsColor.TextSecondary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (runningTasks > 0) {
                                stringResource(R.string.cs_home_tasks_running, runningTasks)
                            } else {
                                stringResource(R.string.cs_home_tasks_none)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.cs_home_activity_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (runningTasks > 0) {
                        CsStatusPill(text = stringResource(R.string.cs_home_tasks_active))
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader() {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val name = account?.username

    Column {
        Text(
            text = if (name != null) {
                stringResource(R.string.cs_home_greeting_named, name)
            } else {
                stringResource(R.string.cs_home_greeting)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(CsSpace.xxs))
        Text(
            text = stringResource(R.string.cs_home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CsQuickAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CsCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(CsSpace.m),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CsSpace.xs)
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CsLinkButton(text: String, onClick: () -> Unit) {
    CsCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = CsSpace.m, vertical = CsSpace.xs),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}
