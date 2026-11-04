/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.TextRailItem
import com.movtery.zalithlauncher.ui.guide.sendStartGuideOnce
import com.movtery.zalithlauncher.ui.screens.BackStackNavKey
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.FirstLoginMenu
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsDiscoverScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsHomeScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsInstancesScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsLibraryScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsSection
import com.movtery.zalithlauncher.ui.screens.lite.CsSidebar
import com.movtery.zalithlauncher.ui.screens.lite.CsTopBar
import com.movtery.zalithlauncher.ui.screens.lite.csSectionOf
import com.movtery.zalithlauncher.ui.screens.lite.isCsSectionRoot
import com.movtery.zalithlauncher.ui.screens.content.LicenseScreen
import com.movtery.zalithlauncher.ui.screens.content.LogViewScreen
import com.movtery.zalithlauncher.ui.screens.content.MultiplayerScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionExportScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionsManageScreen
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreen
import com.movtery.zalithlauncher.ui.screens.content.assetinfo.AssetInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.festivals.FestivalTitleText
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.festival.LocalFestivals
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ModifyVersionViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import com.movtery.zalithlauncher.viewmodel.sendToast

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    //监控当前是否有任务正在进行
    var taskRunning by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val noTasks = tasks.isEmpty()
            eventViewModel.sendKeepScreen(!noTasks)
            taskRunning = noTasks
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain

    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val launcherBackgroundOpacity = AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f

    val backgroundColor = if (isBackgroundValid) {
        backgroundColor().copy(alpha = launcherBackgroundOpacity)
    } else backgroundColor()

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val section = csSectionOf(mainScreenKey)
    val sectionTitle = when (section) {
        CsSection.HOME -> stringResource(R.string.cs_nav_home)
        CsSection.INSTANCES -> stringResource(R.string.cs_nav_instances)
        CsSection.DISCOVER -> stringResource(R.string.cs_nav_discover)
        CsSection.LIBRARY -> stringResource(R.string.cs_nav_library)
        CsSection.SETTINGS -> stringResource(R.string.cs_nav_settings)
        null -> BuildKeys.LAUNCHER_NAME
    }

    // Sidebar navigation keeps the existing back stack semantics: navigating to a
    // section clears the transient screens (settings / downloads / multiplayer)
    // exactly like the old top bar did, so the back stack never grows sideways.
    val navigateToSection: (CsSection) -> Unit = { target ->
        val stack = screenBackStackModel.mainScreen
        val removes = screenBackStackModel.clearBeforeNavKeys
        when (target) {
            CsSection.HOME -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.LauncherMain)
            CsSection.INSTANCES -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Instances)
            CsSection.DISCOVER -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Discover)
            CsSection.LIBRARY -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Library)
            CsSection.SETTINGS -> stack.removeAndNavigateTo(
                removes = removes,
                screenKey = screenBackStackModel.settingsScreen
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = onBackgroundColor()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            CsSidebar(
                selected = section,
                onNavigate = navigateToSection
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CsTopBar(
                    title = sectionTitle,
                    detailTitle = if (!isCsSectionRoot(mainScreenKey)) {
                        {
                            val detail = mainScreenKey?.title
                            if (detail != null) {
                                AndroidStringText(
                                    text = detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    } else null,
                    taskRunning = taskRunning,
                    onBack = if (!isCsSectionRoot(mainScreenKey)) {
                        {
                            backDispatcher?.onBackPressed()
                                ?: screenBackStackModel.mainScreen.backStack.removeLastOrNull()
                        }
                    } else null,
                    onTaskClick = { changeTasksExpandedState() },
                    onMultiplayer = {
                        screenBackStackModel.mainScreen.removeAndNavigateTo(
                            removes = screenBackStackModel.clearBeforeNavKeys,
                            screenKey = NormalNavKey.Multiplayer
                        )
                    },
                    onFileManager = {
                        eventViewModel.sendEvent(
                            EventViewModel.Event.OpenFileManager(
                                rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                            )
                        )
                    },
                    onAccount = {
                        screenBackStackModel.mainScreen.navigateTo(
                            screenKey = NormalNavKey.AccountManager(FirstLoginMenu.NONE)
                        )
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                        NavigationUI(
                            modifier = Modifier.fillMaxSize(),
                            screenBackStackModel = screenBackStackModel,
                            toMainScreen = toMainScreen,
                            eventViewModel = eventViewModel,
                            modpackImportViewModel = modpackImportViewModel,
                            modifyVersionViewModel = modifyVersionViewModel,
                            submitError = submitError
                        )
                    }

                    TaskMenu(
                        isExpanded = isTaskMenuExpanded,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.3f)
                            .align(Alignment.CenterStart)
                            .padding(all = 6.dp)
                    ) {
                        changeTasksExpandedState()
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    // Single subscription point for the instance list, shared by every CS screen.
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NestedNavDisplay(
            ownerKey = screenBackStackModel.mainScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    CsHomeScreen(
                        currentVersion = currentVersion,
                        versions = versions,
                        runningTasks = tasks.size,
                        onPlay = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onOpenInstance = navigateToVersions,
                        onOpenInstances = {
                            backStack.navigateTo(NormalNavKey.Instances)
                        },
                        onOpenDiscover = {
                            backStack.navigateTo(NormalNavKey.Discover)
                        },
                        onOpenClassicHome = {
                            backStack.navigateTo(NormalNavKey.ClassicHome)
                        },
                        onOpenAccounts = {
                            backStack.navigateTo(
                                NormalNavKey.AccountManager(FirstLoginMenu.NONE)
                            )
                        },
                        onOpenSettings = {
                            screenBackStackModel.mainScreen.removeAndNavigateTo(
                                removes = screenBackStackModel.clearBeforeNavKeys,
                                screenKey = screenBackStackModel.settingsScreen
                            )
                        },
                        onOpenMultiplayer = {
                            backStack.navigateTo(NormalNavKey.Multiplayer)
                        },
                        onOpenFiles = {
                            eventViewModel.sendEvent(
                                EventViewModel.Event.OpenFileManager(
                                    rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath
                                )
                            )
                        },
                        onImportModpack = { uri ->
                            modpackImportViewModel.import(
                                context = context,
                                uri = uri
                            )
                        }
                    )
                }
                entry<NormalNavKey.Instances> {
                    CsInstancesScreen(
                        versions = versions,
                        currentVersion = currentVersion,
                        isRefreshing = isRefreshing,
                        onRefresh = { VersionsManager.refresh("CS Launcher Lite") },
                        onPlay = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onOpenInstance = navigateToVersions,
                        onSetCurrent = { version ->
                            VersionsManager.saveVersion(version)
                        },
                        onCreate = {
                            screenBackStackModel.navigateToDownload()
                        },
                        onManageFolders = {
                            backStack.navigateTo(NormalNavKey.VersionsManager)
                        },
                        onExport = navigateToExport,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Discover> {
                    CsDiscoverScreen(
                        onDownloadGame = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadGameScreen)
                        },
                        onDownloadModPacks = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadModPackScreen)
                        },
                        onDownloadMods = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadModScreen)
                        },
                        onDownloadResourcePacks = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadResourcePackScreen)
                        },
                        onDownloadShaders = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadShadersScreen)
                        },
                        onDownloadSaves = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadSavesScreen)
                        },
                        onSearchById = {
                            screenBackStackModel.navigateToDownload(NormalNavKey.SearchId)
                        },
                        onFavorites = {
                            screenBackStackModel.navigateToDownload(screenBackStackModel.downloadFavoritesScreen)
                        }
                    )
                }
                entry<NormalNavKey.Library> {
                    CsLibraryScreen(
                        versions = versions,
                        currentVersion = currentVersion,
                        onManageInstance = navigateToVersions,
                        onOpenManager = { version, managerKey ->
                            val opened = backStack.lastOrNull() as? NestedNavKey.VersionSettings
                            if (opened != null && opened.version == version) {
                                opened.backStack.navigateTo(managerKey, useClassEquality = true)
                            } else {
                                val key = NestedNavKey.VersionSettings(version)
                                key.backStack.navigateTo(managerKey, useClassEquality = true)
                                backStack.navigateTo(key)
                            }
                        },
                        onOpenInstances = {
                            backStack.navigateTo(NormalNavKey.Instances)
                        }
                    )
                }
                entry<NormalNavKey.ClassicHome> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        onLaunchGame = { version ->
                            eventViewModel.sendEvent(
                                EventViewModel.Event.Launch.Game(version)
                            )
                        },
                        onQuickPlayServer = { version, address ->
                            val target = (version ?: VersionsManager.currentVersion.value)?.takeIf { it.isValid() }
                            if (target != null) {
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.PlayServer(target, address)
                                )
                            } else {
                                eventViewModel.sendToast(androidText(R.string.game_launch_no_version))
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.VersionsManager)
                            }
                        },
                        onOpenLink = {
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it))
                        },
                        startGuideOnce = { keys ->
                            eventViewModel.sendStartGuideOnce(keys)
                        }
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        modifyViewModel = modifyVersionViewModel,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.AssetInfo> { key ->
                    AssetInfoScreen(
                        key = key,
                        mainScreenKey = screenBackStackModel.mainScreen.currentKey,
                        assetInfoScreenKey = key.currentKey,
                        eventViewModel = eventViewModel,
                        submitError = submitError,
                    )
                }
                entry<NormalNavKey.Multiplayer> {
                    MultiplayerScreen(
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.LogView> { key ->
                    LogViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor(),
                contentColor = onBackgroundColor()
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()

                        TaskItem(
                            taskProgress = taskProgress,
                            taskMessage = taskMessage,
                            rateBytesPerSec = rateBytesPerSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    taskProgress: Float,
    taskMessage: AndroidStringText?,
    rateBytesPerSec: Long?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = cardColor(false),
    contentColor: Color = onCardColor(),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                taskMessage?.let { message ->
                    AndroidStringText(
                        text = message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { taskProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}