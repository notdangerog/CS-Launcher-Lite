/*
 * CS Launcher Lite
 *
 * Derived from Zalith Launcher 2, Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.cs.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSortField
import com.movtery.zalithlauncher.ui.cs.components.CsCard
import com.movtery.zalithlauncher.ui.cs.components.CsDropdownField
import com.movtery.zalithlauncher.ui.cs.components.CsEmptyState
import com.movtery.zalithlauncher.ui.cs.components.CsFilterChip
import com.movtery.zalithlauncher.ui.cs.components.CsIconButton
import com.movtery.zalithlauncher.ui.cs.components.CsPrimaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsProjectIcon
import com.movtery.zalithlauncher.ui.cs.components.CsSearchField
import com.movtery.zalithlauncher.ui.cs.components.CsSecondaryButton
import com.movtery.zalithlauncher.ui.cs.components.CsSkeletonBlock
import com.movtery.zalithlauncher.ui.cs.components.CsStatItem
import com.movtery.zalithlauncher.ui.cs.components.CsTabs
import com.movtery.zalithlauncher.ui.cs.components.CsTag
import com.movtery.zalithlauncher.ui.cs.components.formatCount
import com.movtery.zalithlauncher.ui.cs.data.CsAddonItem
import com.movtery.zalithlauncher.ui.cs.data.CsBrowseCategory
import com.movtery.zalithlauncher.ui.cs.data.CsDiscoverRequest
import com.movtery.zalithlauncher.ui.cs.data.CsDiscoverState
import com.movtery.zalithlauncher.ui.cs.data.CsDiscoverViewModel
import com.movtery.zalithlauncher.ui.cs.data.CsEnvironmentFacet
import com.movtery.zalithlauncher.ui.cs.data.CsLicenseFacet
import com.movtery.zalithlauncher.ui.cs.data.csGameVersionOptions
import com.movtery.zalithlauncher.ui.cs.theme.CsColors
import kotlinx.coroutines.delay

private val filterPanelWidth = 250.dp

/**
 * Discover 屏幕：分类标签 + 搜索 + 排序/分页 + 结果列表 + 右侧过滤器面板
 */
@Composable
fun CsDiscoverScreen(
    onOpenProject: (CsAddonItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CsDiscoverViewModel = viewModel(key = "cs_discover")
) {
    var filterDrawerOpen by remember { mutableStateOf(false) }

    // 其它页面（例如 Downloads）请求打开指定分类
    LaunchedEffect(Unit) {
        CsDiscoverRequest.consume()?.let { viewModel.selectCategory(it) }
    }

    // 搜索文本输入后自动搜索（550ms 防抖）
    var lastSubmittedQuery by remember { mutableStateOf(viewModel.query) }
    LaunchedEffect(viewModel.query) {
        if (viewModel.query == lastSubmittedQuery) return@LaunchedEffect
        delay(550)
        lastSubmittedQuery = viewModel.query
        viewModel.submitQuery()
    }

    // Results list state drives the collapsing header (scroll-position based, so no jumps)
    val listState = rememberLazyListState()
    val collapsed = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 6
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "csDiscoverCollapse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CsColors.Background)
    ) {
        val isWide = maxWidth >= 900.dp

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 24.dp, end = if (isWide) 16.dp else 24.dp, top = 14.dp)
            ) {
                // 分类标签：随滚动收起，平滑动画
                AnimatedVisibility(
                    visible = collapseProgress < 0.5f,
                    enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(140))
                ) {
                    Column {
                        CsTabs(
                            labels = CsBrowseCategory.entries.map { stringResource(it.labelRes) },
                            selectedIndex = CsBrowseCategory.entries.indexOf(viewModel.category),
                            onSelected = { index ->
                                viewModel.selectCategory(CsBrowseCategory.entries[index])
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CsSearchField(
                        modifier = Modifier.weight(1f),
                        value = viewModel.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = stringResource(viewModel.category.placeholderRes),
                        onSearch = { viewModel.submitQuery() }
                    )
                    if (!isWide) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val activeFilters = viewModel.hasActiveFilters()
                        CsIconButton(
                            iconRes = R.drawable.ic_filter_alt_outlined,
                            contentDescription = stringResource(R.string.cs_discover_filters),
                            onClick = { filterDrawerOpen = true },
                            size = 42.dp,
                            iconSize = 18.dp,
                            tint = if (activeFilters) CsColors.Accent else CsColors.TextSecondary,
                            background = CsColors.Card,
                            borderColor = if (activeFilters) CsColors.AccentDark else CsColors.Border
                        )
                    }
                }

                // 滚动后仅保留这条细搜索栏
                AnimatedVisibility(
                    visible = collapseProgress < 0.5f,
                    enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                        fadeOut(animationSpec = tween(140))
                ) {
                    Column {
                        // 排序 / 每页数量 / 分页：窄屏自动换行，控件不会被挤压或溢出
                        DiscoverControlRow(
                            sortLabel = stringResource(R.string.cs_discover_sort_by),
                            sortOptions = CsDiscoverViewModel.SORT_OPTIONS.map {
                                stringResource(it.getDisplayName()) to it
                            },
                            sort = viewModel.sort,
                            onSortSelected = { viewModel.selectSort(it) },
                            pageSizeLabel = stringResource(R.string.cs_discover_per_page),
                            pageSizeOptions = CsDiscoverViewModel.PAGE_SIZE_OPTIONS.map { it.toString() to it },
                            pageSize = viewModel.pageSize,
                            onPageSizeSelected = { viewModel.selectPageSize(it) },
                            pageNumber = viewModel.pageNumber,
                            totalPages = viewModel.totalPages,
                            onPrevious = { viewModel.previousPage() },
                            onNext = { viewModel.nextPage() }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(if (collapseProgress > 0.5f) 4.dp else 12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = viewModel.state) {
                        is CsDiscoverState.Loading -> ResultSkeleton()
                        is CsDiscoverState.Error -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CsEmptyState(
                                title = stringResource(R.string.cs_discover_error),
                                description = stringResource(R.string.cs_discover_source_note),
                                action = {
                                    CsSecondaryButton(
                                        text = stringResource(R.string.cs_home_retry),
                                        onClick = { viewModel.retry() },
                                        iconRes = R.drawable.ic_refresh,
                                        compact = true
                                    )
                                }
                            )
                        }

                        is CsDiscoverState.Unsupported -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CsEmptyState(
                                title = stringResource(R.string.cs_discover_tab_datapacks),
                                description = stringResource(R.string.cs_discover_datapack_unsupported)
                            )
                        }

                        is CsDiscoverState.Loaded -> {
                            if (state.page.items.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CsEmptyState(
                                        title = stringResource(R.string.cs_discover_no_results),
                                        description = stringResource(R.string.cs_discover_no_results_desc),
                                        action = if (viewModel.hasActiveFilters() || viewModel.query.isNotEmpty()) {
                                            {
                                                CsSecondaryButton(
                                                    text = stringResource(R.string.cs_discover_filter_clear),
                                                    onClick = {
                                                        viewModel.updateQuery("")
                                                        viewModel.clearFilters()
                                                        viewModel.submitQuery()
                                                    },
                                                    iconRes = R.drawable.ic_close,
                                                    compact = true
                                                )
                                            }
                                        } else null
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
                                ) {
                                    items(
                                        items = state.page.items,
                                        key = { item -> "${item.platform}_${item.classes}_${item.projectId}" }
                                    ) { item ->
                                        DiscoverResultCard(
                                            item = item,
                                            onOpen = { onOpenProject(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isWide) {
                Box(
                    modifier = Modifier
                        .width(filterPanelWidth)
                        .fillMaxHeight()
                        .background(CsColors.BackgroundSecondary)
                ) {
                    FilterPanel(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                    )
                }
            }
        }

        // 窄屏：抽屉式过滤器
        if (!isWide) {
            AnimatedVisibility(
                visible = filterDrawerOpen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CsColors.Background.copy(alpha = 0.7f))
                        .clickable { filterDrawerOpen = false }
                )
            }
            val drawerWidth = minOf(320.dp, maxWidth * 0.86f)
            AnimatedVisibility(
                visible = filterDrawerOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
                        .background(CsColors.BackgroundSecondary)
                        .border(1.dp, CsColors.Border)
                ) {
                    FilterPanel(
                        viewModel = viewModel,
                        showClose = true,
                        onClose = { filterDrawerOpen = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sort / page-size / paging controls.
 *
 * Wide: one row — sort, page size, flexible space, paging on the right.
 * Narrow: sort and page size share one row (equal weight, ellipsized labels) and the paging controls
 * move to their own row, so nothing overlaps, clips or escapes its container.
 */
@Composable
private fun <T, P> DiscoverControlRow(
    sortLabel: String,
    sortOptions: List<Pair<String, T>>,
    sort: T,
    onSortSelected: (T) -> Unit,
    pageSizeLabel: String,
    pageSizeOptions: List<Pair<String, P>>,
    pageSize: P,
    onPageSizeSelected: (P) -> Unit,
    pageNumber: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 560.dp

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CsDropdownField(
                        modifier = Modifier.weight(1f),
                        value = sort,
                        options = sortOptions,
                        onSelected = onSortSelected,
                        label = sortLabel,
                        fillWidth = true
                    )
                    CsDropdownField(
                        modifier = Modifier.weight(1f),
                        value = pageSize,
                        options = pageSizeOptions,
                        onSelected = onPageSizeSelected,
                        label = pageSizeLabel,
                        fillWidth = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaginationControls(
                        pageNumber = pageNumber,
                        totalPages = totalPages,
                        onPrevious = onPrevious,
                        onNext = onNext
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CsDropdownField(
                    value = sort,
                    options = sortOptions,
                    onSelected = onSortSelected,
                    label = sortLabel
                )
                Spacer(modifier = Modifier.width(8.dp))
                CsDropdownField(
                    value = pageSize,
                    options = pageSizeOptions,
                    onSelected = onPageSizeSelected,
                    label = pageSizeLabel
                )

                Spacer(modifier = Modifier.weight(1f))

                PaginationControls(
                    pageNumber = pageNumber,
                    totalPages = totalPages,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
            }
        }
    }
}

@Composable
private fun PaginationControls(
    pageNumber: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CsIconButton(
            iconRes = R.drawable.ic_arrow_back,
            contentDescription = stringResource(R.string.cs_discover_previous_page),
            onClick = onPrevious,
            size = 30.dp,
            iconSize = 15.dp,
            tint = if (pageNumber > 1) CsColors.TextPrimary else CsColors.TextTertiary,
            background = CsColors.Card,
            borderColor = CsColors.Border
        )
        Text(
            modifier = Modifier
                .widthIn(min = 92.dp)
                .padding(horizontal = 8.dp),
            text = stringResource(R.string.cs_discover_page_of, pageNumber, totalPages),
            style = MaterialTheme.typography.labelMedium,
            color = CsColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        CsIconButton(
            iconRes = R.drawable.ic_cs_chevron_right,
            contentDescription = stringResource(R.string.cs_discover_next_page),
            onClick = onNext,
            size = 30.dp,
            iconSize = 15.dp,
            tint = if (pageNumber < totalPages) CsColors.TextPrimary else CsColors.TextTertiary,
            background = CsColors.Card,
            borderColor = CsColors.Border
        )
    }
}

/**
 * 单条搜索结果
 *
 * 左侧图标、中间名称/作者/描述/标签、右侧下载量/关注量与安装按钮。
 * 文本全部限制行数并省略，右侧区域固定宽度，因此不会出现文字挤压按钮的情况。
 */
@Composable
private fun DiscoverResultCard(
    item: CsAddonItem,
    onOpen: () -> Unit
) {
    CsCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            CsProjectIcon(
                iconUrl = item.iconUrl,
                name = item.title,
                modifier = Modifier.size(56.dp),
                cornerRadius = 12.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.author.isNotEmpty()) {
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = CsColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CsColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tags = buildList {
                        item.loaders.firstOrNull()?.let { add(it to true) }
                        item.categories.take(2).forEach { add(stringResource(it.getDisplayName()) to false) }
                    }
                    tags.forEach { (label, accent) ->
                        CsTag(text = label, accent = accent)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.width(96.dp),
                horizontalAlignment = Alignment.End
            ) {
                CsStatItem(
                    iconRes = R.drawable.ic_download,
                    text = formatCount(item.downloadCount)
                )
                val follows = item.follows ?: 0L
                if (follows > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    CsStatItem(
                        iconRes = R.drawable.ic_favorite_outlined,
                        text = formatCount(follows)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CsPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.cs_discover_install),
                    onClick = onOpen,
                    iconRes = R.drawable.ic_download,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun ResultSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            CsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CsSkeletonBlock(modifier = Modifier.size(56.dp), cornerRadius = 12.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        CsSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(13.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CsSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CsSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    CsSkeletonBlock(modifier = Modifier.size(width = 96.dp, height = 46.dp))
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 过滤器面板                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun FilterPanel(
    viewModel: CsDiscoverViewModel,
    modifier: Modifier = Modifier,
    showClose: Boolean = false,
    onClose: () -> Unit = {}
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cs_discover_filters),
                style = MaterialTheme.typography.titleMedium,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showClose) {
                CsIconButton(
                    iconRes = R.drawable.ic_close,
                    contentDescription = null,
                    onClick = onClose,
                    size = 28.dp,
                    iconSize = 14.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (viewModel.hasActiveFilters()) {
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.clearFilters() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    text = stringResource(R.string.cs_discover_filter_clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = CsColors.Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        FilterSection(title = stringResource(R.string.cs_discover_filter_categories)) {
            val codes = CsDiscoverViewModel.categoriesFor(viewModel.category)
            if (codes.isEmpty()) {
                EmptyFilterHint()
            } else {
                FilterChipFlow(
                    labels = codes.map { stringResource(it.getDisplayName()) },
                    isSelected = { index -> codes[index] in viewModel.selectedFilters },
                    onToggle = { index -> viewModel.toggleFilter(codes[index]) }
                )
            }
        }

        FilterSection(title = stringResource(R.string.cs_discover_filter_environment)) {
            val facets = CsEnvironmentFacet.entries
            FilterChipFlow(
                labels = facets.map { stringResource(it.getDisplayName()) },
                isSelected = { index -> facets[index] in viewModel.selectedFilters },
                onToggle = { index -> viewModel.toggleFilter(facets[index]) }
            )
        }

        FilterSection(title = stringResource(R.string.cs_discover_filter_game_version)) {
            CsDropdownField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.gameVersion,
                options = csGameVersionOptions.map { version ->
                    (version.ifEmpty { stringResource(R.string.cs_discover_all) }) to version
                },
                onSelected = { viewModel.selectGameVersion(it) }
            )
        }

        val loaders: List<PlatformDisplayLabel> = CsDiscoverViewModel.loadersFor(viewModel.category)
        FilterSection(title = stringResource(R.string.cs_discover_filter_loader)) {
            if (loaders.isEmpty()) {
                EmptyFilterHint()
            } else {
                FilterChipFlow(
                    labels = loaders.map { it.getDisplayName() },
                    isSelected = { index -> loaders[index] == viewModel.loader },
                    onToggle = { index ->
                        val target = loaders[index]
                        viewModel.selectLoader(if (viewModel.loader == target) null else target)
                    }
                )
            }
        }

        val licenses: List<PlatformFilterCode> = CsLicenseFacet.entries
        FilterSection(title = stringResource(R.string.cs_discover_filter_license)) {
            FilterChipFlow(
                labels = licenses.map { stringResource(it.getDisplayName()) },
                isSelected = { index -> licenses[index] in viewModel.selectedFilters },
                onToggle = { index -> viewModel.toggleFilter(licenses[index]) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.cs_discover_source_note),
            style = MaterialTheme.typography.labelSmall,
            color = CsColors.TextTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyFilterHint() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.cs_discover_datapack_unsupported),
        style = MaterialTheme.typography.labelSmall,
        color = CsColors.TextTertiary,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * 可折叠的过滤器分组
 */
@Composable
private fun FilterSection(
    title: String,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "csFilterChevron"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = CsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation),
                painter = painterResource(R.drawable.ic_keyboard_arrow_up),
                contentDescription = null,
                tint = CsColors.TextSecondary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                content()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CsColors.Border)
        )
    }
}

@Composable
private fun FilterChipFlow(
    labels: List<String>,
    isSelected: (Int) -> Boolean,
    onToggle: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            CsFilterChip(
                text = label,
                selected = isSelected(index),
                onClick = { onToggle(index) }
            )
        }
    }
}
