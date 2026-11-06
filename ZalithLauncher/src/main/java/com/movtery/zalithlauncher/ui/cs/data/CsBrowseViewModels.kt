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

package com.movtery.zalithlauncher.ui.cs.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSortField
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthModCategory
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthModpackCategory
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthResourcePackCategory
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthShadersCategory
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.modrinthModLoaderFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 首页精选整合包状态 */
sealed interface CsFeaturedState {
    data object Loading : CsFeaturedState
    data class Loaded(val items: List<CsAddonItem>) : CsFeaturedState
    data object Empty : CsFeaturedState
    data object Error : CsFeaturedState
}

/**
 * 首页数据：精选整合包列表（真实 Modrinth 数据，按下载量排序）
 */
class CsHomeViewModel : ViewModel() {
    var featured by mutableStateOf<CsFeaturedState>(CsFeaturedState.Loading)
        private set

    private var job: Job? = null

    init {
        loadFeatured()
    }

    fun loadFeatured() {
        job?.cancel()
        featured = CsFeaturedState.Loading
        job = viewModelScope.launch {
            try {
                // 首页固定展示 12 个精选整合包（2 列 × 6 行）
                val page = csBrowsePage(
                    classes = PlatformClasses.MOD_PACK,
                    filter = PlatformSearchFilter(
                        sortField = PlatformSortField.DOWNLOADS,
                        index = 0,
                        limit = FEATURED_COUNT
                    )
                )
                featured = if (page.items.isEmpty()) {
                    CsFeaturedState.Empty
                } else {
                    CsFeaturedState.Loaded(page.items.take(FEATURED_COUNT))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                featured = CsFeaturedState.Error
            }
        }
    }

    private companion object {
        const val FEATURED_COUNT = 12
    }
}

/** Discover 搜索结果状态 */
sealed interface CsDiscoverState {
    data object Loading : CsDiscoverState
    data class Loaded(val page: CsAddonPage) : CsDiscoverState
    data object Error : CsDiscoverState
    data object Unsupported : CsDiscoverState
}

/**
 * Discover 屏幕状态：分类标签、搜索、排序、过滤、分页
 */
class CsDiscoverViewModel : ViewModel() {
    var category by mutableStateOf(CsBrowseCategory.MODPACKS)
        private set
    var query by mutableStateOf("")
        private set
    var sort by mutableStateOf(PlatformSortField.RELEVANCE)
        private set
    var pageSize by mutableStateOf(DEFAULT_PAGE_SIZE)
        private set
    var loader by mutableStateOf<PlatformDisplayLabel?>(null)
        private set
    var gameVersion by mutableStateOf("")
        private set
    var selectedFilters by mutableStateOf<Set<PlatformFilterCode>>(emptySet())
        private set
    var state by mutableStateOf<CsDiscoverState>(CsDiscoverState.Loading)
        private set
    var pageNumber by mutableStateOf(1)
        private set
    var totalPages by mutableStateOf(1)
        private set

    private var job: Job? = null

    init {
        search()
    }

    fun selectCategory(target: CsBrowseCategory) {
        if (target == category) return
        category = target
        // 不同资源类型的过滤器互不通用，切换时清空
        selectedFilters = emptySet()
        loader = null
        gameVersion = ""
        pageNumber = 1
        search()
    }

    fun updateQuery(value: String) {
        query = value
    }

    fun submitQuery() {
        pageNumber = 1
        search()
    }

    fun selectSort(value: PlatformSortField) {
        if (value == sort) return
        sort = value
        pageNumber = 1
        search()
    }

    fun selectPageSize(value: Int) {
        if (value == pageSize) return
        pageSize = value
        pageNumber = 1
        search()
    }

    fun selectLoader(value: PlatformDisplayLabel?) {
        if (value == loader) return
        loader = value
        pageNumber = 1
        search()
    }

    fun selectGameVersion(value: String) {
        if (value == gameVersion) return
        gameVersion = value
        pageNumber = 1
        search()
    }

    fun toggleFilter(code: PlatformFilterCode) {
        selectedFilters = if (code in selectedFilters) {
            selectedFilters - code
        } else {
            selectedFilters + code
        }
        pageNumber = 1
        search()
    }

    fun clearFilters() {
        if (selectedFilters.isEmpty() && loader == null && gameVersion.isEmpty()) return
        selectedFilters = emptySet()
        loader = null
        gameVersion = ""
        pageNumber = 1
        search()
    }

    fun hasActiveFilters(): Boolean =
        selectedFilters.isNotEmpty() || loader != null || gameVersion.isNotEmpty()

    fun nextPage() {
        if (pageNumber >= totalPages) return
        pageNumber += 1
        search()
    }

    fun previousPage() {
        if (pageNumber <= 1) return
        pageNumber -= 1
        search()
    }

    fun retry() {
        search()
    }

    private fun search() {
        job?.cancel()
        val classes = category.platformClasses
        if (classes == null) {
            // 当前启动器版本没有数据包下载后端，如实展示，而不是伪造结果
            state = CsDiscoverState.Unsupported
            totalPages = 1
            return
        }

        state = CsDiscoverState.Loading
        val filter = PlatformSearchFilter(
            searchName = query.trim(),
            gameVersion = gameVersion,
            sortField = sort,
            categories = selectedFilters.toList(),
            modloader = loader,
            index = (pageNumber - 1) * pageSize,
            limit = pageSize
        )

        job = viewModelScope.launch {
            try {
                val page = csBrowsePage(classes = classes, filter = filter)
                totalPages = page.totalPages
                if (page.items.isEmpty() && pageNumber > 1) {
                    // 页码越界时回到第一页
                    pageNumber = 1
                    search()
                    return@launch
                }
                state = CsDiscoverState.Loaded(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = CsDiscoverState.Error
            }
        }
    }

    companion object {
        val PAGE_SIZE_OPTIONS = listOf(10, 20, 30, 50)
        const val DEFAULT_PAGE_SIZE = 20

        val SORT_OPTIONS = listOf(
            PlatformSortField.RELEVANCE,
            PlatformSortField.DOWNLOADS,
            PlatformSortField.POPULARITY,
            PlatformSortField.UPDATED,
            PlatformSortField.NEWEST
        )

        /** 当前资源分类下可用的类别过滤器 */
        fun categoriesFor(category: CsBrowseCategory): List<PlatformFilterCode> = when (category) {
            CsBrowseCategory.MODPACKS -> ModrinthModpackCategory.entries
            CsBrowseCategory.MODS -> ModrinthModCategory.entries
            CsBrowseCategory.RESOURCE_PACKS -> ModrinthResourcePackCategory.entries
            CsBrowseCategory.SHADERS -> ModrinthShadersCategory.entries
            CsBrowseCategory.DATA_PACKS -> emptyList()
        }

        /** 当前资源分类下可用的加载器过滤器 */
        fun loadersFor(category: CsBrowseCategory): List<PlatformDisplayLabel> = when (category) {
            CsBrowseCategory.MODPACKS, CsBrowseCategory.MODS -> modrinthModLoaderFilters
            else -> emptyList()
        }
    }
}
