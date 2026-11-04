/*
 * CS Launcher Lite — Unified UI
 * Spec: Center features prominent "Recommended Modpacks" with large preview banner cards
 * displaying game screenshots, titles, short descriptions, download stats, neon green Install buttons.
 */

package com.movtery.zalithlauncher.ui.screens.lite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsSectionHeader
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.design.CsStatusPill
import com.movtery.zalithlauncher.ui.design.CsTonalButton
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage

@Composable
fun CsHomeScreen(
    versions: List<Version>,
    currentVersion: Version?,
    catalog: CsCatalogViewModel,
    onPlay: (Version) -> Unit,
    onOpenInstance: (Version) -> Unit,
    onOpenInstances: () -> Unit,
    onOpenModpack: (CsCatalogEntry) -> Unit,
    onInstallModpack: (CsCatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.background(CsColor.Background)) {
        val cardHeight = CsSize.catalogCardHeight
        val columns = when {
            maxWidth < 380.dp -> 1
            maxWidth < 720.dp -> 2
            else -> 2
        }

        CsCatalogGrid(
            viewModel = catalog,
            cardHeight = cardHeight,
            columns = columns,
            gridState = rememberLazyGridState(),
            onOpen = onOpenModpack,
            onInstall = onInstallModpack,
            contentPadding = PaddingValues(horizontal = CsSpace.l, vertical = CsSpace.m),
            header = {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = CsSpace.s)) {
                        Text(
                            text = "Recommended Modpacks",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CsColor.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.cs_home_popular_modpacks_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = CsColor.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun InstancesSection(
    versions: List<Version>,
    currentVersion: Version?,
    onPlay: (Version) -> Unit,
    onOpenInstance: (Version) -> Unit,
    onOpenAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CsSectionHeader(
            modifier = Modifier.padding(bottom = CsSpace.s),
            title = stringResource(R.string.cs_home_your_instances),
            subtitle = stringResource(R.string.cs_home_your_instances_subtitle),
            trailing = { CsInstancesAllButton(onClick = onOpenAll) }
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CsSpace.m),
            contentPadding = PaddingValues(end = CsSpace.s)
        ) {
            items(
                items = versions,
                key = { version -> runCatching { version.getVersionName() }.getOrDefault("") }
            ) { version ->
                HomeInstanceCard(
                    version = version,
                    isCurrent = version == currentVersion,
                    onPlay = { onPlay(version) },
                    onOpen = { onOpenInstance(version) }
                )
            }
        }
    }
}

@Composable
private fun HomeInstanceCard(
    version: Version,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = rememberCsInstanceMeta(version)
    CsCard(
        modifier = modifier.width(270.dp),
        onClick = onOpen,
        color = CsColor.Card,
        contentPadding = PaddingValues(horizontal = CsSpace.m, vertical = CsSpace.s)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CsSpace.s)
        ) {
            VersionIconImage(
                modifier = Modifier.size(CsSize.cardIconSmall),
                version = version
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CsSpace.xs)
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = meta.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = CsColor.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrent) {
                        CsStatusPill(text = stringResource(R.string.cs_instance_current))
                    }
                }
                Spacer(modifier = Modifier.height(CsSpace.xxs))
                Text(
                    text = buildString {
                        append(meta.minecraftVersion ?: stringResource(R.string.cs_instance_unknown_version))
                        append(" · ")
                        append(meta.loader ?: stringResource(R.string.cs_instance_vanilla))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = CsColor.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CsPrimaryButton(
                text = stringResource(R.string.cs_instance_play),
                iconRes = R.drawable.ic_play_arrow_filled,
                enabled = meta.isValid,
                onClick = onPlay
            )
        }
    }
}

@Composable
private fun CsInstancesAllButton(onClick: () -> Unit) {
    CsTonalButton(
        text = stringResource(R.string.cs_home_all_instances),
        onClick = onClick
    )
}
