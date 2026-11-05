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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.CsSecondaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsSectionHeader
import com.movtery.zalithlauncher.ui.cs.components.CsTag
import com.movtery.zalithlauncher.ui.cs.theme.CsColors

/**
 * Settings — part of the unified CS Launcher Lite design.
 *
 * Only real, persisted preferences are exposed here (they write through to the launcher's own
 * settings store). Engine-level tools are grouped separately and clearly labelled.
 */
@Composable
fun CsSettingsScreen(
    onOpenAccounts: () -> Unit,
    onOpenVersionsManager: () -> Unit,
    onOpenEngineSettings: () -> Unit,
    onOpenFileManager: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    val animateSpeed by AllSettings.launcherAnimateSpeedLevel.state
    val backgroundOpacity by AllSettings.launcherBackgroundOpacity.state
    val taskMenuExpanded by AllSettings.launcherTaskMenuExpanded.state

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.cs_nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            color = CsColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = stringResource(R.string.cs_settings_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = CsColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        /* ---------------- Account ---------------- */
        CsSectionHeader(title = stringResource(R.string.cs_settings_accounts))
        Spacer(modifier = Modifier.height(9.dp))
        CsCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenAccounts
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CsColors.CardElevated)
                        .border(1.dp, CsColors.Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_person_filled),
                        contentDescription = null,
                        tint = if (currentAccount != null) CsColors.Accent else CsColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAccount?.username ?: stringResource(R.string.cs_home_select_account),
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentAccount?.let { getAccountTypeName(it) }
                            ?: stringResource(R.string.cs_profile_offline),
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (accounts.isNotEmpty()) {
                    CsTag(text = accounts.size.toString())
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    modifier = Modifier.size(15.dp),
                    painter = painterResource(R.drawable.ic_cs_chevron_right),
                    contentDescription = null,
                    tint = CsColors.TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        /* ---------------- Appearance (real preferences) ---------------- */
        CsSectionHeader(title = stringResource(R.string.cs_settings_appearance))
        Spacer(modifier = Modifier.height(9.dp))

        CsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Interface animation speed: 1..30, persisted by the launcher settings store
                SettingStepper(
                    title = stringResource(R.string.cs_settings_anim_speed),
                    description = stringResource(R.string.cs_settings_anim_speed_desc),
                    value = animateSpeed,
                    valueLabel = "$animateSpeed",
                    onDecrease = {
                        if (animateSpeed > 1) AllSettings.launcherAnimateSpeedLevel.save(animateSpeed - 1)
                    },
                    onIncrease = {
                        if (animateSpeed < 30) AllSettings.launcherAnimateSpeedLevel.save(animateSpeed + 1)
                    }
                )

                SettingDivider()

                SettingStepper(
                    title = stringResource(R.string.cs_settings_bg_opacity),
                    description = stringResource(R.string.cs_settings_bg_opacity_desc),
                    value = backgroundOpacity,
                    valueLabel = "$backgroundOpacity%",
                    onDecrease = {
                        if (backgroundOpacity > 20) {
                            AllSettings.launcherBackgroundOpacity.save(backgroundOpacity - 5)
                        }
                    },
                    onIncrease = {
                        if (backgroundOpacity < 100) {
                            AllSettings.launcherBackgroundOpacity.save(backgroundOpacity + 5)
                        }
                    }
                )

                SettingDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cs_settings_task_panel),
                            style = MaterialTheme.typography.titleSmall,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.cs_settings_task_panel_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.TextTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = taskMenuExpanded,
                        onCheckedChange = { AllSettings.launcherTaskMenuExpanded.save(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CsColors.OnAccent,
                            checkedTrackColor = CsColors.Accent,
                            uncheckedThumbColor = CsColors.TextSecondary,
                            uncheckedTrackColor = CsColors.CardElevated
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        /* ---------------- Engine tools ---------------- */
        CsSectionHeader(title = stringResource(R.string.cs_settings_engine))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.cs_settings_engine_desc),
            style = MaterialTheme.typography.bodySmall,
            color = CsColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(9.dp))

        CsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ToolRow(
                    title = stringResource(R.string.cs_settings_versions),
                    description = stringResource(R.string.cs_settings_versions_desc),
                    iconRes = R.drawable.ic_box,
                    onClick = onOpenVersionsManager
                )
                SettingDivider()
                ToolRow(
                    title = stringResource(R.string.cs_settings_advanced),
                    description = stringResource(R.string.cs_settings_advanced_desc),
                    iconRes = R.drawable.ic_settings_filled,
                    onClick = onOpenEngineSettings
                )
                SettingDivider()
                ToolRow(
                    title = stringResource(R.string.cs_menu_file_manager),
                    description = stringResource(R.string.cs_settings_files_desc),
                    iconRes = R.drawable.ic_folder_filled,
                    onClick = onOpenFileManager
                )
                SettingDivider()
                ToolRow(
                    title = stringResource(R.string.cs_menu_multiplayer),
                    description = stringResource(R.string.cs_settings_multiplayer_desc),
                    iconRes = R.drawable.ic_group_filled,
                    onClick = onOpenMultiplayer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        /* ---------------- About ---------------- */
        CsSectionHeader(title = stringResource(R.string.cs_settings_about))
        Spacer(modifier = Modifier.height(9.dp))
        CsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CsColors.AccentSoft)
                            .border(1.dp, CsColors.AccentDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            painter = painterResource(R.drawable.ic_cs_logo),
                            contentDescription = null,
                            tint = CsColors.Accent
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = BuildKeys.LAUNCHER_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            color = CsColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.cs_settings_version, BuildKeys.LAUNCHER_VERSION_NAME),
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CsColors.Accent)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.cs_settings_unified_ui),
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColors.Accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CsSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cs_settings_about_open),
                    onClick = onOpenAbout,
                    iconRes = R.drawable.ic_info_outlined,
                    compact = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CsColors.Border)
    )
}

@Composable
private fun SettingStepper(
    title: String,
    description: String,
    value: Int,
    valueLabel: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = CsColors.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CsIconButton(
                iconRes = R.drawable.ic_keyboard_arrow_left,
                contentDescription = null,
                onClick = onDecrease,
                size = 28.dp,
                iconSize = 16.dp,
                background = CsColors.CardElevated,
                borderColor = CsColors.Border
            )
            Text(
                modifier = Modifier.width(46.dp),
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CsIconButton(
                iconRes = R.drawable.ic_keyboard_arrow_right,
                contentDescription = null,
                onClick = onIncrease,
                size = 28.dp,
                iconSize = 16.dp,
                background = CsColors.CardElevated,
                borderColor = CsColors.Border
            )
        }
    }
}

@Composable
private fun ToolRow(
    title: String,
    description: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = CsColors.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(17.dp),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = CsColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                modifier = Modifier.size(15.dp),
                painter = painterResource(R.drawable.ic_cs_chevron_right),
                contentDescription = null,
                tint = CsColors.TextTertiary
            )
        }
    }
}
