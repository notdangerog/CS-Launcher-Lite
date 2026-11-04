/*
 * CS Launcher Lite — Right Side Panel (Spec Exact)
 * Layout Structure: Right Side Panel: Compact widget area showing user profile status
 * ("Local Account / Offline") and news feed cards.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace

data class CsNewsItem(
    val title: String,
    val summary: String,
    val date: String,
    val url: String
)

private val csNewsFeed = listOf(
    CsNewsItem(
        title = "Minecraft 1.21.8 — Hotfix Update",
        summary = "Stability fixes and performance improvements for the latest 1.21 release.",
        date = "Nov 2025",
        url = "https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-8"
    ),
    CsNewsItem(
        title = "Fabulously Optimized updated",
        summary = "The #1 performance modpack now supports 1.21.5 with new optimizations.",
        date = "Oct 2025",
        url = "https://modrinth.com/modpack/fabulously-optimized"
    ),
    CsNewsItem(
        title = "CS Launcher Lite — Unified UI",
        summary = "New Modrinth-inspired dark theme, capsule navigation and banner modpacks.",
        date = "Nov 2025",
        url = "https://github.com/notdangerog/CS-Launcher-Lite"
    )
)

@Composable
fun CsRightPanel(
    onManageAccounts: () -> Unit,
    onOpenNews: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val current by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .width(com.movtery.zalithlauncher.ui.design.CsSize.accountPanelWidth)
            .background(CsColor.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CsSpace.m, vertical = CsSpace.m),
        verticalArrangement = Arrangement.spacedBy(CsSpace.l)
    ) {
        // Profile widget — compact, dark grey card #1E1E1E
        Text(
            text = "Profile",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = CsColor.TextSecondary
        )
        CsCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onManageAccounts,
            color = CsColor.Card,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CsSpace.m)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CsColor.Neutral4),
                    contentAlignment = Alignment.Center
                ) {
                    if (current != null) {
                        PlayerFace(
                            modifier = Modifier.size(42.dp),
                            account = current!!,
                            avatarSize = 42.dp
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            painter = painterResource(R.drawable.ic_person_outlined),
                            contentDescription = null,
                            tint = CsColor.TextSecondary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    val name = current?.username ?: stringResource(R.string.cs_account_offline)
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CsColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val typeLabel = when {
                        current == null -> "Local Account / Offline"
                        current!!.isMicrosoftAccount() -> "Microsoft"
                        current!!.isLocalAccount() -> "Local Account / Offline"
                        else -> getAccountTypeName(current!!)
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColor.TextSecondary,
                        maxLines = 1
                    )
                }
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                    contentDescription = null,
                    tint = CsColor.TextSecondary
                )
            }
        }

        // Small accounts count row
        if (accounts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${accounts.size} account${if (accounts.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColor.TextSecondary
                )
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onManageAccounts)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    text = stringResource(R.string.cs_account_manage),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = CsColor.Accent
                )
            }
        } else {
            // Signed out CTA — spec: show "Not signed in" with sign-in affordance
            CsCard(
                modifier = Modifier.fillMaxWidth(),
                color = CsColor.Card,
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(CsSpace.m)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.cs_account_not_signed_in),
                        style = MaterialTheme.typography.labelMedium,
                        color = CsColor.TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CsColor.Accent)
                            .clickable(onClick = onManageAccounts)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cs_account_sign_in),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // News feed — compact widget cards #1E1E1E
        Text(
            text = stringResource(R.string.cs_news_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = CsColor.TextSecondary
        )
        Column(verticalArrangement = Arrangement.spacedBy(CsSpace.s)) {
            csNewsFeed.forEach { item ->
                CsNewsCard(item = item, onClick = { onOpenNews(item.url) })
            }
        }

        Spacer(modifier = Modifier.height(CsSpace.s))
    }
}

@Composable
private fun CsNewsCard(item: CsNewsItem, onClick: () -> Unit) {
    CsCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = CsColor.Card,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CsColor.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_article_outlined),
                        contentDescription = null,
                        tint = CsColor.Accent
                    )
                }
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColor.TextTertiary
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = CsColor.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = CsColor.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
