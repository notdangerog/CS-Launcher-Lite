/*
 * CS Launcher Lite — Friends screen
 *
 * Presence, requests, world invites and chat against the shared CS friends backend.
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.friends.CsFriend
import com.movtery.zalithlauncher.friends.CsFriendRequest
import com.movtery.zalithlauncher.friends.CsFriendStatus
import com.movtery.zalithlauncher.friends.CsFriendsState
import com.movtery.zalithlauncher.friends.CsWorldInvite
import com.movtery.zalithlauncher.friends.FriendsClient
import com.movtery.zalithlauncher.host.JoinWorld
import com.movtery.zalithlauncher.ui.design.CsAnimatedVisibility
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsChip
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsDivider
import com.movtery.zalithlauncher.ui.design.CsEmptyState
import com.movtery.zalithlauncher.ui.design.CsMetaLabel
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSectionHeader
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsStatusPill
import com.movtery.zalithlauncher.ui.design.CsTonalButton
import com.movtery.zalithlauncher.ui.design.csReveal
import com.movtery.zalithlauncher.ui.design.rememberCsStaggeredReveal
import kotlinx.coroutines.launch

/** Which slice of the friends surface is on screen. */
private enum class CsFriendsTab { FRIENDS, REQUESTS, INVITES }

/**
 * The friends screen.
 *
 * Three states, and the screen decides between them before drawing anything: signed out (an account
 * form), signed in with nothing to show (empty states that explain what each tab is for), and signed
 * in with content.
 *
 * **A watcher is held for as long as the screen is composed.** That is what moves the backend poll
 * from the 20 s background heartbeat to the 8 s foreground rate — and releasing it on dispose is what
 * stops the radio from staying warm after the player leaves. [DisposableEffect] rather than
 * `LaunchedEffect` because the release must run even when the composition is torn down mid-frame.
 *
 * @param onJoinWorld called with a normalized `host:port` when the player taps Join
 */
@Composable
fun CsFriendsScreen(
    modifier: Modifier = Modifier,
    onJoinWorld: (address: String, hostVersion: String?) -> Unit = { _, _ -> },
    onOpenHostSettings: () -> Unit = {}
) {
    val state by FriendsClient.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val release = FriendsClient.acquireWatcher()
        onDispose(release)
    }

    // Coming back to the screen should feel immediate, so ask for a sync now rather than waiting out
    // whatever is left of the current poll interval.
    LaunchedEffect(Unit) { FriendsClient.refreshNow() }

    var tab by remember { mutableStateOf(CsFriendsTab.FRIENDS) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    // A chat open on top of the list needs the fast poll; the tab selection alone does not.
    var chatWith by remember { mutableStateOf<CsFriend?>(null) }
    LaunchedEffect(chatWith) { FriendsClient.setFastPoll(chatWith != null) }

    // Any error the client reports is surfaced once, then cleared so it does not outlive its cause.
    LaunchedEffect(state.lastError) {
        state.lastError?.let {
            notice = it
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CsColor.Background)
            .padding(horizontal = CsSpace.l, vertical = CsSpace.m),
        verticalArrangement = Arrangement.spacedBy(CsSpace.m)
    ) {
        CsFriendsHeader(state = state, notice = notice, onDismissNotice = { notice = null })

        if (state.account == null) {
            CsFriendsSignIn(
                busy = busy,
                onBusy = { busy = it },
                onNotice = { notice = it }
            )
            return@Column
        }

        CsFriendsTabs(tab = tab, state = state, onTab = { tab = it })

        when (tab) {
            CsFriendsTab.FRIENDS -> CsFriendsList(
                friends = state.friends,
                syncing = state.syncing,
                onJoin = { friend ->
                    val address = friend.address
                    if (address.isNullOrBlank()) {
                        notice = context.getString(R.string.cs_friends_not_hosting, friend.name)
                        return@CsFriendsList
                    }
                    JoinWorld.rememberHost(friend.id)
                    onJoinWorld(address, friend.version)
                },
                onChat = { chatWith = it },
                onRemove = { friend ->
                    scope.launch {
                        busy = friend.id
                        notice = FriendsClient.removeFriend(friend.id)
                        busy = null
                    }
                }
            )

            CsFriendsTab.REQUESTS -> CsRequestList(
                requests = state.requests,
                busy = busy,
                onRespond = { request, accept ->
                    scope.launch {
                        busy = request.id
                        notice = FriendsClient.respond(request.id, accept)
                        busy = null
                    }
                }
            )

            CsFriendsTab.INVITES -> CsInviteList(
                invites = state.liveInvites(),
                onJoin = { invite ->
                    JoinWorld.remember(invite.address, invite.version)
                    onJoinWorld(invite.address, invite.version)
                },
                onDecline = { FriendsClient.declineInvite(it.id) }
            )
        }

        CsChatSheet(
            friend = chatWith,
            state = state,
            onClose = { chatWith = null },
            onNotice = { notice = it },
            onOpenHostSettings = onOpenHostSettings,
            onJoinWorld = onJoinWorld
        )
    }
}

/** Title row: the player's own identity and friend code, plus the sync spinner. */
@Composable
private fun CsFriendsHeader(state: CsFriendsState, notice: String?, onDismissNotice: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(CsSpace.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cs_friends_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CsColor.TextPrimary
                )
                Text(
                    text = stringResource(R.string.cs_friends_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = CsColor.TextSecondary
                )
            }
            if (state.syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = CsColor.Accent
                )
            }
        }

        val account = state.account
        if (account != null) {
            // The share code is the whole onboarding story: a friend types it and the request arrives.
            // One tap to copy beats reading six characters aloud.
            CsCard(
                modifier = Modifier.fillMaxWidth(),
                color = CsColor.Card,
                onClick = { clipboard.setText(AnnotatedString(account.code)) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(R.drawable.ic_person_outlined),
                        contentDescription = null,
                        tint = CsColor.Accent
                    )
                    Spacer(Modifier.width(CsSpace.s))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CsColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CsMetaLabel(
                            text = stringResource(R.string.cs_friends_your_code, account.code),
                            iconRes = R.drawable.ic_content_copy_filled
                        )
                    }
                    if (state.onlineCount > 0) {
                        CsStatusPill(
                            text = stringResource(R.string.cs_friends_online_count, state.onlineCount),
                            color = CsColor.Accent,
                            // Breathing rather than blinking: the count is ambient information, and a
                            // blink at the top of a list competes with the content below it.
                            live = true
                        )
                    }
                }
            }
        }

        CsAnimatedVisibility(visible = notice != null) {
            CsCard(
                modifier = Modifier.fillMaxWidth(),
                color = CsColor.Card,
                onClick = onDismissNotice
            ) {
                CsMetaLabel(
                    text = notice.orEmpty(),
                    iconRes = R.drawable.ic_info_outlined,
                    tint = CsColor.Warning
                )
            }
        }
    }
}

@Composable
private fun CsFriendsTabs(tab: CsFriendsTab, state: CsFriendsState, onTab: (CsFriendsTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
        CsFriendsTabChip(CsFriendsTab.FRIENDS, stringResource(R.string.cs_friends_tab_friends), state.friends.size, tab, onTab)
        CsFriendsTabChip(CsFriendsTab.REQUESTS, stringResource(R.string.cs_friends_tab_requests), state.incomingRequests, tab, onTab)
        CsFriendsTabChip(CsFriendsTab.INVITES, stringResource(R.string.cs_friends_tab_invites), state.liveInvites().size, tab, onTab)
    }
}

@Composable
private fun CsFriendsTabChip(
    target: CsFriendsTab,
    label: String,
    count: Int,
    selected: CsFriendsTab,
    onTab: (CsFriendsTab) -> Unit
) {
    // The count rides in the label rather than in a badge of its own: a tab chip that changes width
    // when a request arrives would shove its neighbours sideways on every sync.
    CsChip(
        text = if (count > 0) "$label  $count" else label,
        selected = selected == target,
        onClick = { onTab(target) }
    )
}

/**
 * The friends list.
 *
 * Sorted hosting-first, then online, then by name: the reason to open this screen is nearly always
 * "who can I play with right now", so the joinable worlds come to the top instead of sitting wherever
 * the backend happened to return them.
 */
@Composable
private fun CsFriendsList(
    friends: List<CsFriend>,
    syncing: Boolean,
    onJoin: (CsFriend) -> Unit,
    onChat: (CsFriend) -> Unit,
    onRemove: (CsFriend) -> Unit
) {
    if (friends.isEmpty()) {
        CsEmptyState(
            title = stringResource(R.string.cs_friends_empty_title),
            message = stringResource(R.string.cs_friends_empty_message),
            iconRes = R.drawable.ic_group_filled
        )
        return
    }

    val ordered = remember(friends) {
        friends.sortedWith(
            compareByDescending<CsFriend> { it.isJoinable }
                .thenByDescending { it.status == CsFriendStatus.ONLINE || it.status == CsFriendStatus.PLAYING }
                .thenBy { it.name.lowercase() }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = CsSpace.xl),
        verticalArrangement = Arrangement.spacedBy(CsSpace.s)
    ) {
        items(ordered) { friend ->
            // Stagger is per row: each one rises a beat after the row above, so a refresh reads as one
            // wave instead of twenty independent pops. Capped by CsStagger so a long list does not take
            // a second to settle.
            val index = ordered.indexOf(friend)
            val reveal = rememberCsStaggeredReveal(index, key = friend.id)
            CsFriendRow(
                modifier = Modifier.csReveal(reveal),
                friend = friend,
                onJoin = { onJoin(friend) },
                onChat = { onChat(friend) },
                onRemove = { onRemove(friend) }
            )
        }
        if (syncing) {
            item {
                CsMetaLabel(
                    modifier = Modifier.padding(top = CsSpace.s),
                    text = stringResource(R.string.cs_friends_syncing)
                )
            }
        }
    }
}

@Composable
private fun CsFriendRow(
    modifier: Modifier = Modifier,
    friend: CsFriend,
    onJoin: () -> Unit,
    onChat: () -> Unit,
    onRemove: () -> Unit
) {
    val statusColor = when (friend.status) {
        CsFriendStatus.HOSTING -> CsColor.Accent
        CsFriendStatus.PLAYING -> CsColor.Info
        CsFriendStatus.ONLINE -> CsColor.TextSecondary
        CsFriendStatus.OFFLINE -> CsColor.TextTertiary
    }

    CsCard(modifier = modifier.fillMaxWidth(), color = CsColor.Card) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CsFriendAvatar(friend = friend)

            Spacer(Modifier.width(CsSpace.m))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = CsColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // A hosted world is the interesting line, so it takes the subtitle slot; otherwise the
                // subtitle explains what the player is doing.
                val subtitle = friend.worldLabel
                    ?: stringResource(R.string.cs_friends_status_line, friend.status.wire)
                CsMetaLabel(text = subtitle, tint = statusColor)
                friend.version?.let {
                    CsMetaLabel(text = it, iconRes = R.drawable.ic_info_outlined)
                }
            }

            if (friend.isJoinable) {
                CsPrimaryButton(
                    text = stringResource(R.string.cs_friends_join),
                    iconRes = R.drawable.ic_play_arrow_filled,
                    onClick = onJoin
                )
            } else {
                CsTonalButton(
                    text = stringResource(R.string.cs_friends_chat),
                    iconRes = R.drawable.ic_chat_info,
                    onClick = onChat
                )
            }

            Spacer(Modifier.width(CsSpace.s))
            androidx.compose.material3.IconButton(onClick = onRemove) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.cs_friends_remove),
                    tint = CsColor.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun CsFriendAvatar(friend: CsFriend) {
    // A monogram rather than a fetched skin: the head texture lives behind a signed texture URL that
    // may not exist for every account, and an initials tile never fails to render.
    val initial = friend.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val tone = when (friend.status) {
        CsFriendStatus.HOSTING -> CsColor.Accent
        CsFriendStatus.OFFLINE -> CsColor.TextTertiary
        else -> CsColor.Info
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(CsSpace.m))
            .background(tone.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = tone
        )
    }
}

@Composable
private fun CsRequestList(
    requests: List<CsFriendRequest>,
    busy: String?,
    onRespond: (CsFriendRequest, Boolean) -> Unit
) {
    if (requests.isEmpty()) {
        CsEmptyState(
            title = stringResource(R.string.cs_friends_requests_empty_title),
            message = stringResource(R.string.cs_friends_requests_empty_message),
            iconRes = R.drawable.ic_person_outlined
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = CsSpace.xl),
        verticalArrangement = Arrangement.spacedBy(CsSpace.s)
    ) {
        items(requests) { request ->
            val index = requests.indexOf(request)
            val reveal = rememberCsStaggeredReveal(index, key = request.id)
            CsCard(
                modifier = Modifier.csReveal(reveal).fillMaxWidth(),
                color = CsColor.Card
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = CsColor.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CsMetaLabel(
                            text = if (request.incoming) {
                                stringResource(R.string.cs_friends_request_incoming)
                            } else {
                                stringResource(R.string.cs_friends_request_outgoing)
                            },
                            iconRes = R.drawable.ic_info_outlined
                        )
                    }
                    if (request.incoming) {
                        val isBusy = busy == request.id
                        CsPrimaryButton(
                            text = stringResource(R.string.cs_friends_accept),
                            iconRes = R.drawable.ic_check,
                            enabled = !isBusy,
                            onClick = { onRespond(request, true) }
                        )
                        Spacer(Modifier.width(CsSpace.s))
                        CsTonalButton(
                            text = stringResource(R.string.cs_friends_decline),
                            iconRes = R.drawable.ic_close,
                            enabled = !isBusy,
                            onClick = { onRespond(request, false) }
                        )
                    } else if (busy == request.id) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CsColor.Accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CsInviteList(
    invites: List<CsWorldInvite>,
    onJoin: (CsWorldInvite) -> Unit,
    onDecline: (CsWorldInvite) -> Unit
) {
    if (invites.isEmpty()) {
        CsEmptyState(
            title = stringResource(R.string.cs_friends_invites_empty_title),
            message = stringResource(R.string.cs_friends_invites_empty_message),
            iconRes = R.drawable.ic_public
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = CsSpace.xl),
        verticalArrangement = Arrangement.spacedBy(CsSpace.s)
    ) {
        items(invites) { invite ->
            val index = invites.indexOf(invite)
            val reveal = rememberCsStaggeredReveal(index, key = invite.id)
            CsCard(
                modifier = Modifier.csReveal(reveal).fillMaxWidth(),
                color = CsColor.Card
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.cs_friends_invite_from, invite.from),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = CsColor.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CsMetaLabel(text = invite.world ?: invite.address, iconRes = R.drawable.ic_public)
                        }
                        // Live invites breathe; the ten-minute window means the pill is also a countdown
                        // the player can feel without reading a clock.
                        CsStatusPill(
                            text = stringResource(R.string.cs_friends_invite_live),
                            color = CsColor.Accent,
                            live = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                        CsPrimaryButton(
                            text = stringResource(R.string.cs_friends_join),
                            iconRes = R.drawable.ic_play_arrow_filled,
                            onClick = { onJoin(invite) }
                        )
                        CsTonalButton(
                            text = stringResource(R.string.cs_friends_decline),
                            iconRes = R.drawable.ic_close,
                            onClick = { onDecline(invite) }
                        )
                    }
                }
            }
        }
    }
}

/** Sign-in / registration form, shown when no session is stored. */
@Composable
private fun CsFriendsSignIn(busy: String?, onBusy: (String?) -> Unit, onNotice: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }

    CsCard(modifier = Modifier.fillMaxWidth(), color = CsColor.Card) {
        Column(verticalArrangement = Arrangement.spacedBy(CsSpace.m)) {
            CsSectionHeader(
                title = stringResource(R.string.cs_friends_signin_title),
                subtitle = stringResource(R.string.cs_friends_signin_subtitle)
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.cs_friends_field_username)) },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.cs_friends_field_password)) },
                singleLine = true
            )
            CsAnimatedVisibility(visible = registering) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = display,
                    onValueChange = { display = it },
                    label = { Text(stringResource(R.string.cs_friends_field_display)) },
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                CsPrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(if (registering) R.string.cs_friends_register else R.string.cs_friends_signin),
                    enabled = busy == null && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        scope.launch {
                            onBusy("auth")
                            onNotice(
                                FriendsClient.signIn(
                                    username = username,
                                    password = password,
                                    register = registering,
                                    display = display.takeIf { it.isNotBlank() }
                                )
                            )
                            onBusy(null)
                        }
                    }
                )
                CsTonalButton(
                    text = stringResource(if (registering) R.string.cs_friends_have_account else R.string.cs_friends_need_account),
                    enabled = busy == null,
                    onClick = { registering = !registering }
                )
            }

            if (busy == "auth") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CsColor.Accent
                    )
                    Spacer(Modifier.width(CsSpace.s))
                    CsMetaLabel(text = stringResource(R.string.cs_friends_working))
                }
            }

            CsDivider()
            CsMetaLabel(text = stringResource(R.string.cs_friends_signin_note), iconRes = R.drawable.ic_info_outlined)
        }
    }
}

/**
 * The conversation with one friend, drawn over the list while [friend] is non-null.
 *
 * Held in the same composition as the list rather than pushed as a nav destination: a chat is a
 * transient surface the player drops out of constantly, and a back-stack entry would make returning to
 * the list cost a navigation instead of a tap.
 */
@Composable
private fun CsChatSheet(
    friend: CsFriend?,
    state: CsFriendsState,
    onClose: () -> Unit,
    onNotice: (String?) -> Unit,
    onOpenHostSettings: () -> Unit,
    onJoinWorld: (address: String, hostVersion: String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember(friend?.id) { mutableStateOf("") }

    val messages = remember(state.messages, friend?.id) {
        friend?.let { state.threadWith(it.id) } ?: emptyList()
    }

    // New messages should be visible without the player scrolling for them.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    CsAnimatedVisibility(visible = friend != null) {
        val target = friend ?: return@CsAnimatedVisibility
        CsCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            color = CsColor.PanelCard
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CsColor.TextPrimary
                        )
                        CsMetaLabel(text = target.worldLabel ?: target.address ?: target.status.wire)
                    }
                    if (target.isJoinable) {
                        CsTonalButton(
                            text = stringResource(R.string.cs_friends_join),
                            iconRes = R.drawable.ic_play_arrow_filled,
                            onClick = {
                                // Joining leaves the chat: the launch takes over the screen, and a chat
                                // sheet floating above a starting game is just something to dismiss.
                                JoinWorld.rememberHost(target.id)
                                val address = target.address
                                val version = target.version
                                onClose()
                                if (!address.isNullOrBlank()) onJoinWorld(address, version)
                            }
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cs_friends_close_chat),
                            tint = CsColor.TextSecondary
                        )
                    }
                }

                CsDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp, max = 260.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(CsSpace.xs)
                ) {
                    items(messages, key = { it.id }) { message ->
                        CsChatBubble(message = message, mine = message.mine)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(stringResource(R.string.cs_friends_chat_placeholder)) },
                        singleLine = true
                    )
                    Spacer(Modifier.width(CsSpace.s))
                    CsPrimaryButton(
                        text = stringResource(R.string.cs_friends_send),
                        iconRes = R.drawable.ic_share_filled,
                        enabled = draft.isNotBlank(),
                        onClick = {
                            val text = draft.trim()
                            draft = ""
                            scope.launch { onNotice(FriendsClient.send(target.id, text)) }
                        }
                    )
                }

                CsTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cs_friends_invite_to_world),
                    iconRes = R.drawable.ic_public,
                    onClick = onOpenHostSettings
                )
            }
        }
    }
}

/**
 * One message.
 *
 * Bubbles are aligned to the sender's side and tinted rather than shaped differently: at chat sizes a
 * colour and an alignment read instantly, while a mirrored shape has to be re-parsed every time.
 */
@Composable
private fun CsChatBubble(message: com.movtery.zalithlauncher.friends.CsChatMessage, mine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(CsSpace.m),
            color = if (mine) CsColor.AccentMuted else CsColor.Card
        ) {
            Column(modifier = Modifier.padding(CsSpace.s)) {
                if (message.body.isNotEmpty()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CsColor.TextPrimary
                    )
                }
                // An invite inside a message is joinable from the message itself; the address alone
                // would leave the player copying it out by hand.
                message.inviteAddress?.let { address ->
                    CsMetaLabel(text = address, iconRes = R.drawable.ic_public, tint = CsColor.AccentGlow)
                }
                message.replyBody?.let { reply ->
                    CsMetaLabel(
                        text = stringResource(R.string.cs_friends_replying_to, message.replyFrom.orEmpty(), reply),
                        iconRes = R.drawable.ic_info_outlined
                    )
                }
            }
        }
    }
}
