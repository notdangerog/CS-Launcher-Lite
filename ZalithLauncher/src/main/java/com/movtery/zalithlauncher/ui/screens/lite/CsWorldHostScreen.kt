/*
 * CS Launcher Lite — World hosting screen
 *
 * The player limit, who may join, and the live address of a world that is open to friends.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.friends.FriendsClient
import com.movtery.zalithlauncher.host.CsHostPhase
import com.movtery.zalithlauncher.host.HostSettings
import com.movtery.zalithlauncher.host.WorldHostManager
import com.movtery.zalithlauncher.ui.design.CsAnimatedVisibility
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsChip
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsDivider
import com.movtery.zalithlauncher.ui.design.CsEmptyState
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsMetaLabel
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSectionHeader
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsStatusPill
import com.movtery.zalithlauncher.ui.design.CsTonalButton
import com.movtery.zalithlauncher.ui.design.rememberCsAnimatedInt
import kotlinx.coroutines.launch

/**
 * World hosting settings plus the live state of a world that is currently shared.
 *
 * The screen is settings-first rather than status-first on purpose: the limit and the access mode have
 * to be decided *before* the world is opened to LAN, because the relay enforces them from the moment
 * the tunnel exists. The live card is what appears once a port shows up in the game log.
 *
 * @param onInviteAll called to invite every friend to the running world
 * @param onOpenFriends called to pick individual friends to invite
 */
@Composable
fun CsWorldHostScreen(
    modifier: Modifier = Modifier,
    onInviteAll: () -> Unit = {},
    onOpenFriends: () -> Unit = {}
) {
    val host by WorldHostManager.state.collectAsState()
    val friends by FriendsClient.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var relayDraft by remember { mutableStateOf(HostSettings.relay) }
    var notice by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CsColor.Background)
            .padding(horizontal = CsSpace.l, vertical = CsSpace.m),
        contentPadding = PaddingValues(bottom = CsSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(CsSpace.m)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(CsSpace.xs)) {
                Text(
                    text = stringResource(R.string.cs_host_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CsColor.TextPrimary
                )
                Text(
                    text = stringResource(R.string.cs_host_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = CsColor.TextSecondary
                )
            }
        }

        // ---- Live world ----
        item {
            CsLiveWorldCard(
                phase = host.phase,
                address = host.address,
                occupancy = host.guests.size + 1,
                maxLabel = if (HostSettings.maxPlayers == HostSettings.NO_LIMIT) {
                    stringResource(R.string.cs_host_no_limit)
                } else {
                    HostSettings.javaMaxPlayers.toString()
                },
                world = host.world,
                problem = host.problem,
                supported = host.supported,
                unsupportedReason = host.unsupportedReason,
                onCopy = { address ->
                    clipboard.setText(AnnotatedString(address))
                    notice = context.getString(R.string.cs_host_address_copied)
                },
                onStop = { WorldHostManager.stop() },
                onInviteAll = onInviteAll,
                onOpenFriends = onOpenFriends,
                canInvite = friends.account != null
            )
        }

        // ---- Guests in the world ----
        if (host.guests.isNotEmpty() || host.refusals.isNotEmpty()) {
            item {
                CsGuestCard(
                    guests = host.guests,
                    refusals = host.refusals.map { it.toString() },
                    onKick = { name -> WorldHostManager.kick(name) }
                )
            }
        }

        // ---- Sharing toggle ----
        item {
            CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cs_host_sharing),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = CsColor.TextPrimary
                        )
                        CsMetaLabel(text = stringResource(R.string.cs_host_sharing_note))
                    }
                    Switch(
                        checked = HostSettings.sharingEnabled,
                        onCheckedChange = { enabled ->
                            // Through the manager, not the setter alone: turning sharing off has to tear
                            // a live tunnel down, and turning it back on has to resume a known port.
                            WorldHostManager.setEnabled(enabled)
                        }
                    )
                }
            }
        }

        // ---- Player limit ----
        item {
            CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
                Column(verticalArrangement = Arrangement.spacedBy(CsSpace.m)) {
                    CsSectionHeader(
                        title = stringResource(R.string.cs_host_max_players),
                        subtitle = stringResource(R.string.cs_host_max_players_note)
                    )
                    CsPlayerLimitStepper(
                        maxPlayers = HostSettings.maxPlayers,
                        onStep = { direction ->
                            HostSettings.maxPlayers = HostSettings.stepMaxPlayers(direction)
                        }
                    )
                }
            }
        }

        // ---- Who may join ----
        item {
            CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
                Column(verticalArrangement = Arrangement.spacedBy(CsSpace.m)) {
                    CsSectionHeader(
                        title = stringResource(R.string.cs_host_access),
                        subtitle = stringResource(R.string.cs_host_access_note)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsAccessChip(HostSettings.ACCESS_ANYONE, HostSettings.access) { HostSettings.access = it }
                        CsAccessChip(HostSettings.ACCESS_FRIENDS, HostSettings.access) { HostSettings.access = it }
                        CsAccessChip(HostSettings.ACCESS_INVITED, HostSettings.access) { HostSettings.access = it }
                    }
                    CsMetaLabel(
                        text = when (HostSettings.access) {
                            HostSettings.ACCESS_FRIENDS -> stringResource(R.string.cs_host_access_friends_note)
                            HostSettings.ACCESS_INVITED -> stringResource(
                                R.string.cs_host_access_invited_note,
                                HostSettings.knownPlayers().size
                            )
                            else -> stringResource(R.string.cs_host_access_anyone_note)
                        },
                        iconRes = R.drawable.ic_info_outlined
                    )

                    CsAnimatedVisibility(visible = HostSettings.access == HostSettings.ACCESS_INVITED) {
                        Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                            CsTonalButton(
                                text = stringResource(R.string.cs_host_forget_known),
                                iconRes = R.drawable.ic_delete_outlined,
                                onClick = { HostSettings.clearKnownPlayers() }
                            )
                        }
                    }
                }
            }
        }

        // ---- Server icon ----
        item {
            CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
                Column(verticalArrangement = Arrangement.spacedBy(CsSpace.m)) {
                    CsSectionHeader(
                        title = stringResource(R.string.cs_host_icon),
                        subtitle = stringResource(R.string.cs_host_icon_note)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsIconModeChip(HostSettings.ICON_CS, HostSettings.iconMode) { HostSettings.iconMode = it }
                        CsIconModeChip(HostSettings.ICON_PROFILE, HostSettings.iconMode) { HostSettings.iconMode = it }
                        CsIconModeChip(HostSettings.ICON_CUSTOM, HostSettings.iconMode) { HostSettings.iconMode = it }
                    }
                    CsAnimatedVisibility(visible = HostSettings.iconMode == HostSettings.ICON_CUSTOM) {
                        CsMetaLabel(
                            text = stringResource(R.string.cs_host_icon_custom_hint, HostSettings.CUSTOM_ICON_FILENAME),
                            iconRes = R.drawable.ic_image_outlined
                        )
                    }
                }
            }
        }

        // ---- Relay ----
        item {
            CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
                Column(verticalArrangement = Arrangement.spacedBy(CsSpace.m)) {
                    CsSectionHeader(
                        title = stringResource(R.string.cs_host_relay),
                        subtitle = stringResource(R.string.cs_host_relay_note)
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = relayDraft,
                        onValueChange = { relayDraft = it },
                        placeholder = { Text(stringResource(R.string.cs_host_relay_placeholder)) },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsTonalButton(
                            text = stringResource(R.string.cs_host_relay_save),
                            iconRes = R.drawable.ic_check,
                            onClick = {
                                HostSettings.relay = relayDraft
                                notice = if (relayDraft.isBlank() || HostSettings.relayOrNull != null) {
                                    context.getString(R.string.cs_host_relay_saved)
                                } else {
                                    context.getString(R.string.cs_host_relay_invalid)
                                }
                            }
                        )
                        CsTonalButton(
                            text = stringResource(R.string.cs_host_relay_reset),
                            iconRes = R.drawable.ic_refresh,
                            onClick = {
                                relayDraft = ""
                                HostSettings.relay = ""
                                notice = context.getString(R.string.cs_host_relay_saved)
                            }
                        )
                    }
                }
            }
        }

        // ---- Blocked this session ----
        item {
            val blocked = HostSettings.blockedNames()
            if (blocked.isNotEmpty()) {
                CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
                    Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsSectionHeader(
                            title = stringResource(R.string.cs_host_blocked),
                            subtitle = stringResource(R.string.cs_host_blocked_note)
                        )
                        blocked.forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CsColor.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CsTonalButton(
                                    text = stringResource(R.string.cs_host_unblock),
                                    onClick = { HostSettings.unblock(name) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            CsAnimatedVisibility(visible = notice != null) {
                CsCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = CsColor.Card,
                    onClick = { notice = null }
                ) {
                    CsMetaLabel(
                        text = notice.orEmpty(),
                        iconRes = R.drawable.ic_info_outlined,
                        tint = CsColor.Info
                    )
                }
            }
        }

        item {
            CsMetaLabel(
                text = stringResource(R.string.cs_host_how_it_works),
                iconRes = R.drawable.ic_help_outlined
            )
        }
    }
}

/**
 * The state of the world right now: dialling, published with an address to copy, or failed with a
 * reason.
 *
 * The address is the single most important thing on this screen, so when it exists it gets the largest
 * type on the page and a copy button beside it — a player reading an address aloud to a friend on
 * voice chat is the common path, and transcribing `abc123.e4mc.link` from small grey text is where
 * that path breaks.
 */
@Composable
private fun CsLiveWorldCard(
    phase: CsHostPhase,
    address: String?,
    occupancy: Int,
    maxLabel: String,
    world: String?,
    problem: String?,
    supported: Boolean,
    unsupportedReason: String?,
    onCopy: (String) -> Unit,
    onStop: () -> Unit,
    onInviteAll: () -> Unit,
    onOpenFriends: () -> Unit,
    canInvite: Boolean
) {
    CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.PanelCard) {
        Column(verticalArrangement = Arrangement.spacedBy(CsSpace.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.cs_host_live_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CsColor.TextPrimary
                )
                when (phase) {
                    CsHostPhase.PUBLISHED -> CsStatusPill(
                        text = stringResource(R.string.cs_host_phase_published),
                        color = CsColor.Accent,
                        // Breathing: this is the "your world is reachable" signal, and it should read as
                        // alive at the edge of vision rather than demand attention every second.
                        live = true
                    )
                    CsHostPhase.STARTING -> CsStatusPill(
                        text = stringResource(R.string.cs_host_phase_starting),
                        color = CsColor.Warning
                    )
                    CsHostPhase.FAILED -> CsStatusPill(
                        text = stringResource(R.string.cs_host_phase_failed),
                        color = CsColor.Danger
                    )
                    CsHostPhase.IDLE -> CsStatusPill(
                        text = stringResource(R.string.cs_host_phase_idle),
                        color = CsColor.TextTertiary
                    )
                }
            }

            when {
                !supported -> CsEmptyState(
                    title = stringResource(R.string.cs_host_unsupported_title),
                    message = unsupportedReason ?: stringResource(R.string.cs_host_unsupported_message),
                    iconRes = R.drawable.ic_public
                )

                phase == CsHostPhase.IDLE -> CsMetaLabel(
                    text = stringResource(R.string.cs_host_idle_hint),
                    iconRes = R.drawable.ic_info_outlined
                )

                else -> {
                    world?.let {
                        CsMetaLabel(text = it, iconRes = R.drawable.ic_nav_instances)
                    }

                    if (!address.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(CsSpace.m),
                            color = CsColor.Accent.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(CsSpace.m),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = address,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CsColor.AccentGlow,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CsIconButton(
                                    iconRes = R.drawable.ic_content_copy_filled,
                                    contentDescription = stringResource(R.string.cs_host_copy_address),
                                    onClick = { onCopy(address) },
                                    containerColor = CsColor.Accent,
                                    contentColor = CsColor.Background
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                            CsPrimaryButton(
                                text = stringResource(R.string.cs_host_invite_all),
                                iconRes = R.drawable.ic_group_filled,
                                enabled = canInvite,
                                onClick = onInviteAll
                            )
                            CsTonalButton(
                                text = stringResource(R.string.cs_host_invite_pick),
                                iconRes = R.drawable.ic_person_outlined,
                                enabled = canInvite,
                                onClick = onOpenFriends
                            )
                            CsTonalButton(
                                text = stringResource(R.string.cs_host_stop),
                                iconRes = R.drawable.ic_close,
                                onClick = onStop
                            )
                        }
                        if (!canInvite) {
                            CsMetaLabel(
                                text = stringResource(R.string.cs_host_invite_needs_account),
                                iconRes = R.drawable.ic_info_outlined,
                                tint = CsColor.Warning
                            )
                        }
                    } else {
                        CsMetaLabel(text = stringResource(R.string.cs_host_dialling))
                    }

                    problem?.let {
                        CsMetaLabel(text = it, iconRes = R.drawable.ic_info_outlined, tint = CsColor.Danger)
                    }
                }
            }

            // Occupancy counts the host, matching the limit the player set: "3 / 8" is what they expect
            // after choosing 8, not "2 / 7".
            CsDivider()
            CsOccupancyRow(occupancy = occupancy, maxLabel = maxLabel)
        }
    }
}

@Composable
private fun CsOccupancyRow(occupancy: Int, maxLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.ic_group_filled),
            contentDescription = null,
            tint = CsColor.TextSecondary
        )
        Spacer(Modifier.width(CsSpace.s))
        Text(
            text = stringResource(R.string.cs_host_occupancy, occupancy, maxLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = CsColor.TextSecondary
        )
    }
}

/**
 * Players in the world, with a kick beside each, plus whoever the relay turned away.
 *
 * Refusals are shown rather than swallowed: a friend who cannot get in will otherwise conclude the
 * host is ignoring them, and the host has no other way to learn that the access list is wrong.
 */
@Composable
private fun CsGuestCard(guests: List<String>, refusals: List<String>, onKick: (String) -> Unit) {
    CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
        Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
            CsSectionHeader(
                title = stringResource(R.string.cs_host_guests),
                subtitle = stringResource(R.string.cs_host_guests_note)
            )
            if (guests.isEmpty()) {
                CsMetaLabel(text = stringResource(R.string.cs_host_no_guests))
            } else {
                guests.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CsColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CsTonalButton(
                            text = stringResource(R.string.cs_host_kick),
                            iconRes = R.drawable.ic_block_outlined,
                            onClick = { onKick(name) }
                        )
                    }
                }
            }
            if (refusals.isNotEmpty()) {
                CsDivider()
                CsMetaLabel(
                    text = stringResource(R.string.cs_host_refused),
                    iconRes = R.drawable.ic_info_outlined,
                    tint = CsColor.Warning
                )
                refusals.forEach { line ->
                    CsMetaLabel(text = line, tint = CsColor.Warning)
                }
            }
        }
    }
}

/**
 * The limit stepper.
 *
 * Walks [HostSettings.STEPS] rather than counting by one: 8 → 10 → 12 is three taps, whereas 8 → 64
 * would be fifty-six, and nobody sets a limit by holding a button down. The number animates because a
 * value that changes silently is easy to misread after a double tap.
 */
@Composable
private fun CsPlayerLimitStepper(maxPlayers: Int, onStep: (Int) -> Unit) {
    val shown = rememberCsAnimatedInt(if (maxPlayers == HostSettings.NO_LIMIT) 0 else maxPlayers)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
    ) {
        CsIconButton(
            iconRes = R.drawable.ic_keyboard_arrow_left,
            contentDescription = stringResource(R.string.cs_host_limit_lower),
            onClick = { onStep(-1) },
            size = 44
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .background(CsColor.Background, RoundedCornerShape(CsSpace.m))
                .padding(vertical = CsSpace.m),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (maxPlayers == HostSettings.NO_LIMIT) {
                    stringResource(R.string.cs_host_no_limit)
                } else {
                    shown.toString()
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = CsColor.Accent
            )
        }

        CsIconButton(
            iconRes = R.drawable.ic_keyboard_arrow_right,
            contentDescription = stringResource(R.string.cs_host_limit_raise),
            onClick = { onStep(1) },
            size = 44
        )
    }
}

@Composable
private fun CsAccessChip(mode: Int, current: Int, onSelect: (Int) -> Unit) {
    CsChip(
        text = stringResource(
            when (mode) {
                HostSettings.ACCESS_FRIENDS -> R.string.cs_host_access_friends
                HostSettings.ACCESS_INVITED -> R.string.cs_host_access_invited
                else -> R.string.cs_host_access_anyone
            }
        ),
        selected = current == mode,
        onClick = { onSelect(mode) }
    )
}

@Composable
private fun CsIconModeChip(mode: Int, current: Int, onSelect: (Int) -> Unit) {
    CsChip(
        text = stringResource(
            when (mode) {
                HostSettings.ICON_PROFILE -> R.string.cs_host_icon_profile
                HostSettings.ICON_CUSTOM -> R.string.cs_host_icon_custom
                else -> R.string.cs_host_icon_cs
            }
        ),
        selected = current == mode,
        onClick = { onSelect(mode) }
    )
}
