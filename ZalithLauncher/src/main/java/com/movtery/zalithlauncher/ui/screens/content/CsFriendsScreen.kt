/*
 * CS Launcher Lite — CS Friends client
 * Ported from JoooaoooDVD/CSL2 (app_cs/src/main/java/com/craftstudio/cslauncher/friends/ui/FriendsFragment.java)
 */

package com.movtery.zalithlauncher.ui.screens.content

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.CsPrimaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsSecondaryButton
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import com.movtery.zalithlauncher.ui.cs.friends.*

@Composable
fun CsFriendsScreen(
    onBack: () -> Unit,
    onToast: ((AndroidStringText) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val client = FriendsClient
    val account = CsAccount

    val snapshot by remember { mutableStateOf(client.snapshot) }
    val inbox by remember { mutableStateOf(client.inbox()) }

    var tab by remember { mutableStateOf(0) } // 0 friends, 1 requests, 2 invites, 3 messages
    var addCode by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        client.addListener(object : FriendsClient.Listener {
            override fun onFriendsChanged(s: Snapshot) {
                snapshot.value = s
            }
            override fun onNote(n: Note) {
                inbox.value = client.inbox()
            }
            override fun onFriendsError(message: String) {
                onToast?.invoke(androidText(message))
            }
        })
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CsColors.Background,
        contentColor = CsColors.TextPrimary
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.cs_friends_title), style = MaterialTheme.typography.titleLarge, color = CsColors.TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(modifier = Modifier.size(24.dp), painter = painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.generic_back), tint = CsColors.TextPrimary)
                    }
                },
                colors = MaterialTheme.colorScheme.copy(backgroundColor = Color.Transparent),
                containerColor = Color.Transparent
            ) {}

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair(0, stringResource(R.string.cs_friends_title)),
                    Pair(1, stringResource(R.string.cs_friends_requests)),
                    Pair(2, stringResource(R.string.cs_friends_invites)),
                    Pair(3, stringResource(R.string.cs_friends_messages))
                ).forEach { (id, label) ->
                    val selected = tab == id
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { tab.value = id }
                            )
                            .background(if (selected) CsColors.AccentSoft else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.labelLarge, color = if (selected) CsColors.Accent else CsColors.TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.width(1f))
                CsIconButton(
                    iconRes = R.drawable.ic_add_box_filled,
                    contentDescription = stringResource(R.string.cs_friends_add),
                    onClick = { showAddDialog = true },
                    size = 32.dp,
                    iconSize = 20.dp,
                    background = CsColors.CardElevated,
                    borderColor = CsColors.Border,
                    tint = CsColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (tab) {
                0 -> FriendsList(snapshot.friends, client, onToast)
                1 -> RequestsList(snapshot.requests, client, onToast)
                2 -> InvitesList(snapshot.invites, client, onToast)
                3 -> MessagesList(snapshot.messages, client, onToast)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showAddDialog) {
            AddFriendDialog(
                onDismiss = { showAddDialog = false },
                code = addCode,
                onCodeChange = { addCode.value = it },
                onAdd = { code ->
                    client.addFriend(code) { error ->
                        if (error == null) {
                            onToast?.invoke(androidText(R.string.cs_friends_request_sent))
                            showAddDialog = false
                            addCode.value = ""
                        } else {
                            onToast?.invoke(androidText(error))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FriendsList(friends: List<Friend>, client: FriendsClient, onToast: ((String) -> Unit)?) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(modifier = Modifier.size(48.dp), painter = painterResource(R.drawable.ic_group_filled), contentDescription = null, tint = CsColors.TextTertiary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(R.string.cs_friends_empty), style = MaterialTheme.typography.bodyMedium, color = CsColors.TextSecondary)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(friends, key = { it.id }) { friend ->
            CsCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                onClick = { /* open conversation */ }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(CsColors.AccentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(modifier = Modifier.size(20.dp), painter = painterResource(R.drawable.ic_person_filled), contentDescription = null, tint = CsColors.Accent)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = friend.name, style = MaterialTheme.typography.titleMedium, color = CsColors.TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (friend.online()) CsColors.Accent else CsColors.TextTertiary))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (friend.online()) "Online" else "Offline", style = MaterialTheme.typography.labelSmall, color = if (friend.online()) CsColors.Accent else CsColors.TextTertiary)
                            friend.address?.let { address ->
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "• Hosting $address", style = MaterialTheme.typography.labelSmall, color = CsColors.TextTertiary)
                            }
                        }
                    }
                    CsSecondaryButton(
                        text = stringResource(if (friend.online()) R.string.cs_friends_join else R.string.generic_send),
                        onClick = { friend.address?.let { addr -> onToast?.invoke(androidText("Join $addr")) } ?: onToast?.invoke(androidText("Open chat")) },
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestsList(requests: List<Request>, client: FriendsClient, onToast: ((String) -> Unit)?) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No pending requests", style = MaterialTheme.typography.bodyMedium, color = CsColors.TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(requests, key = { it.id }) { req ->
            CsCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = req.name, style = MaterialTheme.typography.titleMedium, color = CsColors.TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Code: ${req.code}", style = MaterialTheme.typography.labelSmall, color = CsColors.TextTertiary)
                    }
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CsSecondaryButton(text = stringResource(R.string.cs_friends_accept), onClick = { client.respond(req.id, true) { onToast?.invoke(androidText("Accepted")) } }, compact = true)
                        CsSecondaryButton(text = stringResource(R.string.cs_friends_decline), onClick = { client.respond(req.id, false) { onToast?.invoke(androidText("Declined")) } }, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitesList(invites: List<Invite>, client: FriendsClient, onToast: ((String) -> Unit)?) {
    val openInvites = client.openInvites()
    if (openInvites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No invites", style = MaterialTheme.typography.bodyMedium, color = CsColors.TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(openInvites, key = { it.id }) { inv ->
            CsCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "From ${inv.from}", style = MaterialTheme.typography.titleMedium, color = CsColors.TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = inv.world.takeIf { it.isNotEmpty() } ?: "World", style = MaterialTheme.typography.labelSmall, color = CsColors.TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = inv.address, style = MaterialTheme.typography.bodySmall, color = CsColors.TextTertiary, maxLines = 2)
                    }
                    CsSecondaryButton(text = stringResource(R.string.cs_friends_join), onClick = {
                        client.declineInvite(inv)
                        onToast?.invoke(androidText("Joining ${inv.address}"))
                    }, compact = true)
                }
            }
        }
    }
}

@Composable
private fun MessagesList(messages: List<Message>, client: FriendsClient, onToast: ((String) -> Unit)?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.cs_friends_select_conversation), style = MaterialTheme.typography.bodyMedium, color = CsColors.TextSecondary)
    }
}

@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    onAdd: (String) -> Unit
) {
    var localCode by remember { mutableStateOf(code) }
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = androidx.compose.foundation.layout.widthIn(min = 260.dp, max = 340.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.cs_friends_add), style = MaterialTheme.typography.titleLarge, color = CsColors.TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = localCode,
                onValueChange = {
                    localCode = it
                    onCodeChange(it)
                },
                label = { Text(text = stringResource(R.string.cs_friends_code_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CsSecondaryButton(text = stringResource(R.string.generic_cancel), onClick = onDismiss, compact = true)
                Spacer(modifier = Modifier.width(8.dp))
                CsPrimaryButton(text = stringResource(R.string.cs_friends_add), onClick = { onAdd(localCode) }, compact = true)
            }
        }
    }
}
