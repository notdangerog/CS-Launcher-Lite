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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace

/**
 * The right hand account panel of the three section layout.
 *
 * It is a pure view over [AccountsManager]; switching an account calls the same
 * manager entry point the account screen uses, so nothing here is simulated.
 *
 * @param onSignIn opens the existing account screen with the login chooser
 * @param onMicrosoftLogin opens the existing account screen on the Microsoft flow
 * @param onLocalLogin opens the existing account screen on the offline/local flow
 * @param onManageAccounts opens the account manager screen (add / remove / refresh)
 */
@Composable
fun CsAccountPanel(
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit,
    onMicrosoftLogin: () -> Unit,
    onLocalLogin: () -> Unit,
    onManageAccounts: () -> Unit,
    compact: Boolean = false
) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(if (compact) CsSize.accountPanelWidth - 24.dp else CsSize.accountPanelWidth),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CsSpace.m, vertical = CsSpace.m)
        ) {
            if (currentAccount == null) {
                SignedOutHeader()
                Spacer(modifier = Modifier.height(CsSpace.l))
                SignInOptions(
                    onMicrosoftLogin = onMicrosoftLogin,
                    onLocalLogin = onLocalLogin
                )
                if (accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(CsSpace.l))
                    AccountList(
                        accounts = accounts,
                        current = null,
                        onSelect = { account -> AccountsManager.setCurrentAccount(account) }
                    )
                    Spacer(modifier = Modifier.height(CsSpace.s))
                    ManageAccountsRow(onManageAccounts = onManageAccounts)
                }
                Spacer(modifier = Modifier.weight(1f))
                SignedOutStatusRow(onSignIn = onSignIn)
            } else {
                var expanded by rememberSaveable { mutableStateOf(false) }

                PlayingAsHeader()
                Spacer(modifier = Modifier.height(CsSpace.s))
                CurrentAccountCard(
                    account = currentAccount!!,
                    expanded = expanded,
                    onManage = { expanded = !expanded }
                )

                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(140)) + expandVertically(tween(180)),
                    exit = fadeOut(tween(110)) + shrinkVertically(tween(150))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(CsSpace.m))
                        AccountList(
                            accounts = accounts,
                            current = currentAccount,
                            onSelect = { account ->
                                AccountsManager.setCurrentAccount(account)
                                expanded = false
                            }
                        )
                        Spacer(modifier = Modifier.height(CsSpace.s))
                        ManageAccountsRow(onManageAccounts = onManageAccounts)
                    }
                }

                // The panel deliberately keeps the remaining space empty: no widgets
                // compete with the catalog in the center column.
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SignedOutHeader() {
    Column {
        Text(
            text = stringResource(R.string.cs_account_select_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(CsSpace.xxs))
        Text(
            text = stringResource(R.string.cs_account_select_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3
        )
    }
}

@Composable
private fun PlayingAsHeader() {
    Text(
        text = stringResource(R.string.cs_account_playing_as),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

/**
 * Microsoft + local/offline entry points while signed out.
 */
@Composable
private fun SignInOptions(
    onMicrosoftLogin: () -> Unit,
    onLocalLogin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
        SignInOptionRow(
            iconRes = R.drawable.ic_login,
            title = stringResource(R.string.cs_account_microsoft),
            subtitle = stringResource(R.string.cs_account_microsoft_desc),
            onClick = onMicrosoftLogin
        )
        SignInOptionRow(
            iconRes = R.drawable.ic_person_outlined,
            title = stringResource(R.string.cs_account_local),
            subtitle = stringResource(R.string.cs_account_local_desc),
            onClick = onLocalLogin
        )
    }
}

@Composable
private fun SignInOptionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = CsSpace.m, vertical = CsSpace.s)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_arrow_right_rounded),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The account switcher card: player head, name, account type and the dropdown arrow.
 */
@Composable
private fun CurrentAccountCard(
    account: Account,
    expanded: Boolean,
    onManage: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "CsAccountChevron"
    )

    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onManage,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = CsSpace.m, vertical = CsSpace.s)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            PlayerFace(
                account = account,
                avatarSize = 32.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getAccountTypeName(account),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
                painter = painterResource(R.drawable.ic_arrow_drop_down_rounded),
                contentDescription = stringResource(R.string.cs_account_switch),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * List of the stored accounts. The active one is marked with the accent colour.
 */
@Composable
private fun AccountList(
    accounts: List<Account>,
    current: Account?,
    onSelect: (Account) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(bottom = CsSpace.xs),
            text = stringResource(R.string.cs_account_your_accounts),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = CsSpace.xxs),
            verticalArrangement = Arrangement.spacedBy(CsSpace.xxs)
        ) {
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    selected = current != null && account.uniqueUUID == current.uniqueUUID,
                    onClick = { onSelect(account) }
                )
            }
        }
    }
}

/**
 * Entry point of the existing account manager screen.
 */
@Composable
private fun ManageAccountsRow(onManageAccounts: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onManageAccounts,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CsSpace.m, vertical = CsSpace.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_settings_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cs_account_manage),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_arrow_right_rounded),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CsSpace.s, vertical = CsSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            PlayerFace(
                account = account,
                avatarSize = 24.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getAccountTypeName(account),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (selected) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * The single horizontal status row at the bottom of the signed out panel.
 */
@Composable
private fun SignedOutStatusRow(onSignIn: () -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(CsSpace.s))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cs_account_not_signed_in),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            CsPrimaryButton(
                text = stringResource(R.string.cs_account_sign_in),
                iconRes = R.drawable.ic_login,
                onClick = onSignIn
            )
        }
    }
}

/**
 * Compact account button used when the account panel is collapsed (portrait phones).
 * Opens the account overlay of the shell.
 */
@Composable
fun CsAccountChip(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val current = account

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CsSpace.s, vertical = CsSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            if (current != null) {
                PlayerFace(account = current, avatarSize = 22.dp)
                Text(
                    text = current.username,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_person_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.cs_account_sign_in),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Returns the account type icon of an account (Microsoft / authlib / local).
 */
fun accountTypeIcon(account: Account): Int = when {
    account.isMicrosoftAccount() -> R.drawable.ic_login
    account.isLocalAccount() -> R.drawable.ic_person_outlined
    else -> R.drawable.ic_public
}
