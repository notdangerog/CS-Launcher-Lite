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

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.addon.platform.Platform
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.addon.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.addon.platform.searchAddons
import com.movtery.zalithlauncher.game.download.addon.platform.modrinth.models.ModrinthFacet
import com.movtery.zalithlauncher.game.versioninfo.popularVersions
import com.movtery.zalithlauncher.ui.screens.content.download.addon.elements.AddonPage

/**
 * CS Launcher Lite 的浏览层。
 *
 * 这里没有任何"假数据"：所有内容都通过启动器已有的资源搜索后端
 * （Modrinth 官方源 / 镜像源，由现有设置决定）实时获取，
 * 点击项目后进入现有的资源详情/安装流程。
 */
private const val CS_BROWSE_PLATFORM = Platform.MODRINTH

/** 界面使用的项目数据模型 */
data class CsAddonItem(
    val platform: Platform,
    val classes: PlatformClasses,
    val projectId: String,
    val title: String,
    val description: String,
    val author: String,
    val iconUrl: String?,
    val downloadCount: Long,
    val follows: Long?,
    val categories: List<PlatformFilterCode>,
    val loaders: List<String>
)

/** 一页搜索结果 */
data class CsAddonPage(
    val items: List<CsAddonItem>,
    val pageNumber: Int,
    val totalPages: Int,
    val pageSize: Int
)

private class CsSearchException(message: String) : Exception(message)

/**
 * 通过现有搜索后端获取一页数据
 */
suspend fun csSearchAddons(
    classes: PlatformClasses,
    filter: PlatformSearchFilter
): AddonPage {
    var page: AddonPage? = null
    var failed = false

    searchAddons(
        searchPlatform = CS_BROWSE_PLATFORM,
        searchFilter = filter,
        platformClasses = classes,
        onSuccess = { result ->
            page = result.getAddonPage(classes)
        },
        onError = {
            failed = true
        }
    )

    return page ?: throw CsSearchException(
        if (failed) "The search backend returned an error." else "Empty search response."
    )
}

/**
 * 获取一页结果并转换成界面模型
 */
suspend fun csBrowsePage(
    classes: PlatformClasses,
    filter: PlatformSearchFilter
): CsAddonPage {
    val raw = csSearchAddons(classes, filter)
    return CsAddonPage(
        items = raw.data.map { (data, _) -> data.toCsAddonItem(classes) },
        pageNumber = raw.pageNumber,
        totalPages = raw.totalPage.coerceAtLeast(1),
        pageSize = raw.data.size
    )
}

private fun PlatformSearchData.toCsAddonItem(classes: PlatformClasses): CsAddonItem {
    val loads = platformModLoaders()?.map { it.getDisplayName() }.orEmpty()
    return CsAddonItem(
        platform = platform(),
        classes = classes,
        projectId = platformId(),
        title = platformTitle().ifBlank { platformId() },
        description = platformDescription(),
        author = platformAuthors()?.joinToString(", ").orEmpty(),
        iconUrl = platformIconUrl(),
        downloadCount = platformDownloadCount(),
        follows = platformFollows(),
        categories = platformCategories(classes).orEmpty(),
        loaders = loads
    )
}

/**
 * 浏览界面的资源分类（对应 Discover 顶部的标签页）
 */
enum class CsBrowseCategory(
    val labelRes: Int,
    val placeholderRes: Int,
    val platformClasses: PlatformClasses?
) {
    MODPACKS(R.string.cs_discover_tab_modpacks, R.string.cs_discover_search_modpacks, PlatformClasses.MOD_PACK),
    MODS(R.string.cs_discover_tab_mods, R.string.cs_discover_search_mods, PlatformClasses.MOD),
    RESOURCE_PACKS(
        R.string.cs_discover_tab_resourcepacks,
        R.string.cs_discover_search_resourcepacks,
        PlatformClasses.RESOURCE_PACK
    ),
    DATA_PACKS(R.string.cs_discover_tab_datapacks, R.string.cs_discover_search_resourcepacks, null),
    SHADERS(R.string.cs_discover_tab_shaders, R.string.cs_discover_search_shaders, PlatformClasses.SHADERS)
}

/**
 * 过滤器里可用的 Minecraft 游戏版本
 */
val csGameVersionOptions: List<String> = listOf("") + popularVersions

/**
 * 环境过滤器（Modrinth 的 client_side / server_side facet）
 */
enum class CsEnvironmentFacet(private val facetNameValue: String, private val facetValueValue: String) : ModrinthFacet,
    PlatformFilterCode {
    CLIENT("client_side", "required") {
        override fun getDisplayName(): Int = R.string.cs_discover_environment_client
    },
    SERVER("server_side", "required") {
        override fun getDisplayName(): Int = R.string.cs_discover_environment_server
    };

    override fun facetName(): String = facetNameValue
    override fun facetValue(): String = facetValueValue
    override fun index(): Int = ordinal
}

/**
 * 许可过滤器：Modrinth 搜索接口只支持 open_source facet
 */
enum class CsLicenseFacet(private val facetValueValue: String) : ModrinthFacet, PlatformFilterCode {
    OPEN_SOURCE("true") {
        override fun getDisplayName(): Int = R.string.cs_discover_license_open_source
    };

    override fun facetName(): String = "open_source"
    override fun facetValue(): String = facetValueValue
    override fun index(): Int = 0
}
