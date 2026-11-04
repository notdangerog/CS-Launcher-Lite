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

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField
import com.movtery.zalithlauncher.game.download.assets.platform.searchAssets
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.design.CsCard
import com.movtery.zalithlauncher.ui.design.CsEmptyState
import com.movtery.zalithlauncher.ui.design.CsIconButton
import com.movtery.zalithlauncher.ui.design.CsPrimaryButton
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsSize
import com.movtery.zalithlauncher.ui.design.CsSpace
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.SearchAssetsState
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "CsCatalog"
private const val PAGE_SIZE = 20

/**
 * The content kinds the CS catalog can browse.
 *
 * Every entry maps onto a [PlatformClasses] the launcher can actually install into
 * an instance, so nothing shown here is decoration: pressing "install" always opens
 * the existing download / version picker flow of the launcher.
 */
enum class CsCatalogType(
    val platform: Platform,
    val classes: PlatformClasses,
    @StringRes val labelRes: Int
) {
    /** Mod packs (integration packs) - the home screen catalog */
    MODPACK(Platform.MODRINTH, PlatformClasses.MOD_PACK, R.string.cs_catalog_modpacks),

    /** Mods */
    MOD(Platform.MODRINTH, PlatformClasses.MOD, R.string.cs_catalog_mods),

    /** Resource packs */
    RESOURCE_PACK(Platform.MODRINTH, PlatformClasses.RESOURCE_PACK, R.string.cs_catalog_resource_packs),

    /** Shader packs */
    SHADER(Platform.MODRINTH, PlatformClasses.SHADERS, R.string.cs_catalog_shaders),

    /** Worlds / saves - Modrinth has no world project type, so this uses CurseForge */
    WORLD(Platform.CURSEFORGE, PlatformClasses.SAVES, R.string.cs_catalog_worlds)
}

/**
 * Immutable view model of one catalog card. Everything comes from the platform API,
 * no download counts or versions are invented locally.
 */
@Immutable
data class CsCatalogEntry(
    val platform: Platform,
    val classes: PlatformClasses,
    val projectId: String,
    val title: String,
    val description: String,
    val author: String,
    val iconUrl: String?,
    val downloads: Long,
    val versions: List<String>,
    val loaders: List<String>
)

/**
 * Maps a platform search hit onto the immutable card model.
 */
fun PlatformSearchData.toCsCatalogEntry(classes: PlatformClasses): CsCatalogEntry {
    val versions = platformVersions().orEmpty()
    val loaders = platformModLoaders()
        ?.mapNotNull { label -> runCatching { label.getDisplayName() }.getOrNull() }
        .orEmpty()

    return CsCatalogEntry(
        platform = platform(),
        classes = classes,
        projectId = platformId(),
        title = platformTitle().ifBlank { platformId() },
        description = platformDescription(),
        author = platformAuthor(),
        iconUrl = platformIconUrl(),
        downloads = platformDownloadCount(),
        versions = versions,
        loaders = loaders
    )
}

/**
 * Paged catalog state shared by the home screen and the discover screen.
 *
 * Only one network request is in flight at a time; results are appended, so the
 * grid keeps its scroll position and existing cards are never re-fetched.
 */
class CsCatalogViewModel(val type: CsCatalogType) : ViewModel() {
    var entries by mutableStateOf<List<CsCatalogEntry>>(emptyList())
        private set

    /** First page is loading */
    var isLoading by mutableStateOf(true)
        private set

    /** A follow-up page is loading */
    var isLoadingMore by mutableStateOf(false)
        private set

    /** Last error, cleared by [retry] */
    var error by mutableStateOf<AndroidStringText?>(null)
        private set

    var hasMore by mutableStateOf(true)
        private set

    /** Current search text (empty = browse the most downloaded projects) */
    var query by mutableStateOf("")
        private set

    private var searchJob: Job? = null
    private var queryJob: Job? = null

    init {
        reload()
    }

    /**
     * Called on each keystroke; debounced so typing does not hammer the API.
     */
    fun onQueryChange(value: String) {
        if (value == query) return
        query = value
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(450)
            reload()
        }
    }

    /** Search immediately (keyboard action) */
    fun searchNow() {
        queryJob?.cancel()
        reload()
    }

    /**
     * Reloads the first page. Safe to call repeatedly: the previous request is cancelled.
     */
    fun reload() {
        searchJob?.cancel()
        entries = emptyList()
        hasMore = true
        error = null
        isLoading = true
        isLoadingMore = false
        searchJob = viewModelScope.launch {
            fetch(offset = 0, firstPage = true)
        }
    }

    /**
     * Appends the next page. Ignored while another request is running or when the
     * platform reported the last page.
     */
    fun loadMore() {
        if (isLoading || isLoadingMore || !hasMore || error != null) return
        isLoadingMore = true
        searchJob = viewModelScope.launch {
            fetch(offset = entries.size, firstPage = false)
        }
    }

    private suspend fun fetch(offset: Int, firstPage: Boolean) {
        try {
            searchAssets(
                searchPlatform = type.platform,
                searchFilter = PlatformSearchFilter(
                    searchName = query,
                    sortField = if (query.isBlank()) PlatformSortField.DOWNLOADS
                    else PlatformSortField.RELEVANCE,
                    index = offset,
                    limit = PAGE_SIZE
                ),
                platformClasses = type.classes,
                onSuccess = { result ->
                    val page = result.getAssetsPage(type.classes)
                    val mapped = page.data.map { (data, _) -> data.toCsCatalogEntry(type.classes) }
                    entries = if (firstPage) mapped else entries + mapped
                    // Stop paging when the platform says so or when a page came back empty
                    hasMore = !page.isLastPage && mapped.isNotEmpty()
                },
                onError = { state ->
                    error = state.message
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Logger.error(TAG, "Failed to load the catalog for " + type.name, e)
            error = androidText(R.string.cs_catalog_error_title)
        } finally {
            isLoading = false
            isLoadingMore = false
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
        queryJob?.cancel()
    }
}

/**
 * Remembers a catalog view model keyed by the content type, so switching category in
 * the discover screen keeps one instance per category instead of refetching.
 */
@Composable
fun rememberCsCatalogViewModel(
    key: String,
    type: CsCatalogType
): CsCatalogViewModel {
    return viewModel(key = "cs_catalog_${key}_${type.name}") {
        CsCatalogViewModel(type)
    }
}

/**
 * Formats a download count the way the store front does (1.2M, 840K, ...).
 */
fun formatDownloadCount(value: Long): String {
    fun oneDecimal(v: Double): String {
        val text = String.format(Locale.US, "%.1f", v)
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }
    return when {
        value >= 1_000_000_000L -> "${oneDecimal(value / 1_000_000_000.0)}B"
        value >= 1_000_000L -> "${oneDecimal(value / 1_000_000.0)}M"
        value >= 1_000L -> "${oneDecimal(value / 1_000.0)}K"
        else -> value.toString()
    }
}

/**
 * One catalog card: cover artwork, name, short description, download count, supported
 * Minecraft versions, mod loaders and an install action.
 *
 * The card has a fixed [height] so a grid of them lines up perfectly and the caller can
 * size it so exactly four cards are visible on a landscape phone.
 */
@Composable
fun CsCatalogCard(
    entry: CsCatalogEntry,
    height: Dp,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Spec: large preview banner cards — game screenshots, titles, descriptions,
    // download stats, neon green Install button. Single unified dark theme.
    CsCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        onClick = onOpen,
        contentPadding = PaddingValues(0.dp),
        color = CsColor.Card,
        shape = RoundedCornerShape(16.dp)
    ) {
        // ---- Banner artwork — game screenshot simulation ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CsSize.bannerHeight)
                .background(CsColor.Neutral2)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            if (entry.iconUrl != null) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = entry.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.92f
                )
                // Darken slightly for text readability — spec dark theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Black.copy(alpha = 0.18f),
                                1f to Color.Black.copy(alpha = 0.55f)
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(CsColor.Neutral3, CsColor.Neutral4)
                            )
                        )
                )
            }
            // Small project icon overlapping banner bottom-left — rounded, bordered
            if (entry.iconUrl != null) {
                AsyncImage(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = CsSpace.m, bottom = 10.dp)
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CsColor.Card),
                    model = entry.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
        }

        // ---- Text + metadata — white title, muted grey description ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = CsSpace.m, vertical = CsSpace.s)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = CsColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = entry.description.ifBlank { stringResource(R.string.cs_catalog_empty_message) },
                style = MaterialTheme.typography.bodySmall,
                color = CsColor.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CsSpace.xs)
                ) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        tint = CsColor.TextSecondary
                    )
                    Text(
                        text = formatDownloadCount(entry.downloads),
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColor.TextSecondary,
                        maxLines = 1
                    )
                    CatalogMetaDivider()
                    Text(
                        text = catalogVersionLabel(entry),
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColor.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.loaders.isNotEmpty()) {
                        CatalogMetaDivider()
                        Text(
                            text = entry.loaders.first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = CsColor.Accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Neon green Install button — vibrant electric green #1DB954
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CsColor.Accent)
                        .clickable(onClick = onInstall)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cs_catalog_install),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogMetaDivider() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
    )
}

/**
 * Builds the "1.21.1" / "1.21.1 +3" label out of the platform version list.
 */
@Composable
private fun catalogVersionLabel(entry: CsCatalogEntry): String {
    if (entry.versions.isEmpty()) return stringResource(R.string.cs_catalog_versions_unknown)
    val first = entry.versions.first()
    val others = entry.versions.size - 1
    return if (others > 0) "$first +$others" else first
}

/**
 * The reusable 2-column card grid used by Home (popular mod packs) and Discover.
 *
 * @param header extra full width rows placed before the cards (used by the home
 * screen to show the installed instances above the catalog).
 */
@Composable
fun CsCatalogGrid(
    viewModel: CsCatalogViewModel,
    cardHeight: Dp,
    onOpen: (CsCatalogEntry) -> Unit,
    onInstall: (CsCatalogEntry) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(horizontal = CsSpace.l, vertical = CsSpace.s),
    header: (LazyGridScope.() -> Unit)? = null
) {
    val entries = viewModel.entries
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    // Ask for the next page a little before the user reaches the bottom.
    LaunchedEffect(gridState, entries.size) {
        snapshotFlow {
            val layout = gridState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to layout.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4) {
                viewModel.loadMore()
            }
        }
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(CsSpace.m),
        verticalArrangement = Arrangement.spacedBy(CsSpace.m)
    ) {
        if (header != null) {
            header()
        }

        when {
            // First load: skeleton cards keep the layout stable
            isLoading -> {
                items(count = 4) {
                    CsCatalogSkeleton(height = cardHeight)
                }
            }

            error != null -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CsCatalogError(onRetry = { viewModel.reload() })
                }
            }

            entries.isEmpty() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CsEmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        iconRes = R.drawable.ic_document_search_filled,
                        title = stringResource(R.string.cs_catalog_empty_title),
                        message = stringResource(R.string.cs_catalog_empty_message)
                    )
                }
            }

            else -> {
                items(
                    items = entries,
                    key = { entry -> "${entry.platform.name}_${entry.projectId}" }
                ) { entry ->
                    CsCatalogCard(
                        entry = entry,
                        height = cardHeight,
                        onOpen = { onOpen(entry) },
                        onInstall = { onInstall(entry) }
                    )
                }
            }
        }

        if (viewModel.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CsCatalogSkeleton(height = cardHeight / 2)
            }
        }
    }
}

/**
 * Error state of the catalog, with an actionable retry.
 */
@Composable
fun CsCatalogError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CsCard(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = stringResource(R.string.cs_catalog_error_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(CsSpace.xs))
        Text(
            text = stringResource(R.string.cs_catalog_error_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(CsSpace.m))
        Row(horizontalArrangement = Arrangement.spacedBy(CsSpace.s)) {
            CsPrimaryButton(
                text = stringResource(R.string.cs_catalog_retry),
                iconRes = R.drawable.ic_refresh,
                onClick = onRetry
            )
        }
    }
}

/**
 * Pulsing placeholder used while a page is loading.
 */
@Composable
fun CsCatalogSkeleton(height: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "CsCatalogSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CsCatalogSkeletonAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}
