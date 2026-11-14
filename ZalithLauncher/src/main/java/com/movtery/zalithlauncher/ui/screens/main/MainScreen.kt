/*
 * Zalith Launcher 2 — Unified CS Launcher Lite Shell
 * Critical Task: ONE consistent UI — spec-exact, no merged old/new, no duplicated components.
 */

package com.movtery.zalithlauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.FirstLoginMenu
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
import com.movtery.zalithlauncher.ui.screens.content.swapToCategoryAssets
import com.movtery.zalithlauncher.ui.screens.lite.CsCatalogType
import com.movtery.zalithlauncher.ui.screens.lite.CsCatalogViewModel
import com.movtery.zalithlauncher.ui.screens.lite.CsDetailTitle
import com.movtery.zalithlauncher.ui.screens.lite.CsDiscoverScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsDownloadsScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsFriendsScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsWorldHostScreen
import com.movtery.zalithlauncher.host.JoinWorld
import com.movtery.zalithlauncher.host.WorldHostManager
import com.movtery.zalithlauncher.ui.screens.lite.CsHomeScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsInstancesScreen
import com.movtery.zalithlauncher.ui.screens.lite.CsRail
import com.movtery.zalithlauncher.ui.screens.lite.CsRightPanel
import com.movtery.zalithlauncher.ui.screens.lite.CsSection
import com.movtery.zalithlauncher.ui.screens.lite.CsSectionDivider
import com.movtery.zalithlauncher.ui.screens.lite.CsTopBar
import com.movtery.zalithlauncher.ui.screens.lite.csSectionOf
import com.movtery.zalithlauncher.ui.screens.lite.isCsSectionRoot
import com.movtery.zalithlauncher.ui.screens.lite.rememberCsCatalogViewModel
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.ui.design.CsColor
import com.movtery.zalithlauncher.ui.design.CsMotion
import com.movtery.zalithlauncher.ui.design.refreshSystemScale
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast
import com.movtery.zalithlauncher.viewmodel.ModifyVersionViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import com.movtery.zalithlauncher.viewmodel.sendToast
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    modifyVersionViewModel: ModifyVersionViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    var taskRunning by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        TaskSystem.tasksFlow.collect { tasks ->
            val hasTasks = tasks.isNotEmpty()
            eventViewModel.sendKeepScreen(hasTasks)
            taskRunning = hasTasks
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state
    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val section = csSectionOf(mainScreenKey)
    val sectionTitle = when (section) {
        CsSection.HOME -> stringResource(R.string.cs_nav_home)
        CsSection.INSTANCES -> stringResource(R.string.cs_nav_instances)
        CsSection.DISCOVER -> stringResource(R.string.cs_nav_discover)
        CsSection.DOWNLOADS -> stringResource(R.string.cs_nav_downloads)
        CsSection.FRIENDS -> stringResource(R.string.cs_friends_title)
        CsSection.HOST -> stringResource(R.string.cs_host_title)
        CsSection.SETTINGS -> stringResource(R.string.cs_nav_settings)
        null -> "CS Launcher Lite"
    }

    val navigateToSection: (CsSection) -> Unit = { target ->
        val stack = screenBackStackModel.mainScreen
        val removes = screenBackStackModel.clearBeforeNavKeys
        when (target) {
            CsSection.HOME -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.LauncherMain)
            CsSection.INSTANCES -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Instances)
            CsSection.DISCOVER -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Discover)
            CsSection.DOWNLOADS -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Downloads)
            CsSection.FRIENDS -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.Friends)
            CsSection.HOST -> stack.removeAndNavigateTo(removes = removes, screenKey = NormalNavKey.WorldHost)
            CsSection.SETTINGS -> stack.removeAndNavigateTo(
                removes = removes,
                screenKey = screenBackStackModel.settingsScreen
            )
        }
    }

    val homeCatalog = rememberCsCatalogViewModel(key = "home", type = CsCatalogType.MODPACK)

    val openAccountScreen: (FirstLoginMenu) -> Unit = { menu ->
        screenBackStackModel.mainScreen.navigateTo(screenKey = NormalNavKey.AccountManager(menu))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CsColor.Background,
        contentColor = CsColor.TextPrimary
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Sync the OS animator scale into the motion system once per composition of the shell,
            // so a user who turns animations off in Developer options gets instant state changes
            // everywhere instead of animations that ignore the system setting.
            val motionContext = LocalContext.current
            LaunchedEffect(motionContext) { CsMotion.refreshSystemScale(motionContext) }

            val wideLayout = maxWidth >= 720.dp
            var rightPanelOpen by remember { mutableStateOf(false) }
            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            Row(modifier = Modifier.fillMaxSize()) {
                // ---- Left: ultra-slim icon-only rail with capsule highlight ----
                CsRail(
                    selected = section,
                    onNavigate = navigateToSection
                )
                CsSectionDivider()

                // ---- Center: content ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(CsColor.Background)
                ) {
                    CsTopBar(
                        title = sectionTitle,
                        detailTitle = if (!isCsSectionRoot(mainScreenKey)) {
                            {
                                val detail = mainScreenKey?.title
                                if (detail != null) CsDetailTitle(text = detail)
                            }
                        } else null,
                        taskRunning = taskRunning,
                        onBack = if (!isCsSectionRoot(mainScreenKey)) {
                            { backDispatcher?.onBackPressed() ?: screenBackStackModel.mainScreen.backStack.removeLastOrNull() }
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
                                EventViewModel.Event.OpenFileManager(rootPath = PathManager.DIR_FILES_EXTERNAL.absolutePath)
                            )
                        },
                        showAccountChip = !wideLayout,
                        onAccount = { rightPanelOpen = true }
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).background(CsColor.Background)
                    ) {
                        NavStateHost(rootKey = screenBackStackModel.mainScreen) {
                            NavigationUI(
                                modifier = Modifier.fillMaxSize(),
                                screenBackStackModel = screenBackStackModel,
                                toMainScreen = toMainScreen,
                                eventViewModel = eventViewModel,
                                modpackImportViewModel = modpackImportViewModel,
                                modifyVersionViewModel = modifyVersionViewModel,
                                submitError = submitError,
                                homeCatalog = homeCatalog
                            )
                        }
                        TaskMenu(
                            isExpanded = isTaskMenuExpanded,
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.34f).align(Alignment.CenterStart).padding(6.dp)
                        ) { changeTasksExpandedState() }
                    }
                }

                // ---- Right: compact widget panel (profile + news) ----
                if (wideLayout) {
                    CsSectionDivider(thick = true)
                    CsRightPanel(
                        onManageAccounts = { openAccountScreen(FirstLoginMenu.NONE) },
                        onOpenNews = { url ->
                            screenBackStackModel.mainScreen.navigateTo(NormalNavKey.WebScreen(url))
                        }
                    )
                }
            }

            // ---- Right panel overlay on narrow (phones) ----
            if (!wideLayout) {
                AnimatedVisibility(
                    visible = rightPanelOpen,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(120))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { rightPanelOpen = false }
                    )
                }
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    visible = rightPanelOpen,
                    enter = slideInHorizontally(tween(180)) { it },
                    exit = slideOutHorizontally(tween(160)) { it }
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().width(280.dp).background(CsColor.Background)
                    ) {
                        CsRightPanel(
                            onManageAccounts = {
                                rightPanelOpen = false
                                openAccountScreen(FirstLoginMenu.NONE)
                            },
                            onOpenNews = { url ->
                                rightPanelOpen = false
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.WebScreen(url))
                            }
                        )
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
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    homeCatalog: CsCatalogViewModel
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    // Inviting friends is a one-shot call from a click handler; the request itself hops to IO inside
    // the friends API, so the composition scope is enough and no extra dispatcher is needed here.
    val hostScope = rememberCoroutineScope()

    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
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
            modifier = modifier.background(CsColor.Background),
            onBack = { onBack(backStack) },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    CsHomeScreen(
                        versions = versions,
                        currentVersion = currentVersion,
                        catalog = homeCatalog,
                        onPlay = { version -> eventViewModel.sendEvent(EventViewModel.Event.Launch.Game(version)) },
                        onOpenInstance = navigateToVersions,
                        onOpenInstances = { backStack.navigateTo(NormalNavKey.Instances) },
                        onOpenModpack = { entry ->
                            screenBackStackModel.swapToCategoryAssets(entry.platform, entry.classes, entry.projectId, entry.iconUrl)
                        },
                        onInstallModpack = { entry ->
                            screenBackStackModel.swapToCategoryAssets(entry.platform, entry.classes, entry.projectId, entry.iconUrl)
                        }
                    )
                }
                entry<NormalNavKey.Instances> {
                    CsInstancesScreen(
                        versions = versions,
                        currentVersion = currentVersion,
                        isRefreshing = isRefreshing,
                        onRefresh = { VersionsManager.refresh("CS Launcher Lite") },
                        onPlay = { version -> eventViewModel.sendEvent(EventViewModel.Event.Launch.Game(version)) },
                        onOpenInstance = navigateToVersions,
                        onSetCurrent = { version -> VersionsManager.saveVersion(version) },
                        onCreate = { screenBackStackModel.navigateToDownload() },
                        onManageFolders = { backStack.navigateTo(NormalNavKey.VersionsManager) },
                        onExport = navigateToExport,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Discover> {
                    CsDiscoverScreen(
                        onOpenEntry = { entry -> screenBackStackModel.swapToCategoryAssets(entry.platform, entry.classes, entry.projectId, entry.iconUrl) },
                        onInstallEntry = { entry -> screenBackStackModel.swapToCategoryAssets(entry.platform, entry.classes, entry.projectId, entry.iconUrl) },
                        onFavorites = { screenBackStackModel.navigateToDownload(screenBackStackModel.downloadFavoritesScreen) },
                        onSearchById = { screenBackStackModel.navigateToDownload(NormalNavKey.SearchId) }
                    )
                }
                entry<NormalNavKey.Downloads> {
                    CsDownloadsScreen()
                }
                entry<NormalNavKey.Friends> {
                    CsFriendsScreen(
                        // Joining goes through the same event the server list uses, so the launch
                        // pipeline keeps the only copy of the quick-play argument logic and the pending
                        // address survives the version check that follows.
                        onJoinWorld = { address, hostVersion ->
                            val version = VersionsManager.currentVersion.value
                            if (version == null) {
                                eventViewModel.sendToast(androidText(R.string.cs_friends_no_version))
                            } else {
                                JoinWorld.remember(address, hostVersion)
                                eventViewModel.sendEvent(
                                    EventViewModel.Event.Launch.PlayServer(version, address)
                                )
                            }
                        },
                        onOpenHostSettings = { backStack.navigateTo(NormalNavKey.WorldHost) }
                    )
                }
                entry<NormalNavKey.WorldHost> {
                    CsWorldHostScreen(
                        // The manager owns the address and the fan-out; the screen only asks.
                        onInviteAll = { hostScope.launch { WorldHostManager.invite() } },
                        onOpenFriends = { backStack.navigateTo(NormalNavKey.Friends) }
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw -> backStack.navigateTo(NormalNavKey.License(raw)) },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key -> LicenseScreen(key, screenBackStackModel) }
                entry<NormalNavKey.AccountManager> { key ->
                    AccountManageScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url -> eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url)) },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key -> WebViewScreen(key, screenBackStackModel, eventViewModel) }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(screenBackStackModel, navigateToVersions, navigateToExport, eventViewModel, submitError)
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(key, screenBackStackModel) { backStack.removeLastOrNull() }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(key, modifyVersionViewModel, screenBackStackModel, toMainScreen, { navigateToExport(key.version) }, eventViewModel, submitError)
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(key, screenBackStackModel, eventViewModel, toMainScreen)
                }
                entry<NestedNavKey.Download> { key ->
                    com.movtery.zalithlauncher.ui.screens.content.DownloadScreen(key, screenBackStackModel, modpackImportViewModel, eventViewModel, submitError)
                }
                entry<NestedNavKey.AssetInfo> { key ->
                    AssetInfoScreen(key, screenBackStackModel.mainScreen.currentKey, key.currentKey, eventViewModel, submitError)
                }
                entry<NormalNavKey.Multiplayer> { MultiplayerScreen(screenBackStackModel, eventViewModel) }
                entry<NormalNavKey.LogView> { key -> LogViewScreen(key, screenBackStackModel) }
            }
        )
    } else {
        Box(modifier.background(CsColor.Background))
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
        enter = androidx.compose.animation.slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = androidx.compose.animation.slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = CsColor.Card, contentColor = CsColor.TextPrimary),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout(blur = 0) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 4.dp)) {
                        IconButton(modifier = Modifier.size(28.dp).align(Alignment.CenterStart), onClick = changeExpandedState) {
                            Icon(modifier = Modifier.size(28.dp), painter = painterResource(R.drawable.ic_arrow_left_rounded), contentDescription = stringResource(R.string.generic_collapse))
                        }
                        Text(modifier = Modifier.align(Alignment.Center), text = stringResource(R.string.main_task_menu), color = CsColor.TextPrimary)
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxHeight().weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        val taskProgress by task.progress.collectAsStateWithLifecycle()
                        val taskMessage by task.message.collectAsStateWithLifecycle()
                        val rateBytesPerSec by task.rateBytesPerSec.collectAsStateWithLifecycle()
                        TaskItem(taskProgress, taskMessage, rateBytesPerSec, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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
    color: Color = CsColor.Card,
    contentColor: Color = CsColor.TextPrimary,
    onCancelClick: () -> Unit = {}
) {
    Surface(modifier = modifier, shape = shape, color = color, contentColor = contentColor) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(modifier = Modifier.size(24.dp).align(Alignment.CenterVertically), onClick = onCancelClick) {
                Icon(modifier = Modifier.size(20.dp), painter = painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.generic_cancel), tint = CsColor.TextSecondary)
            }
            Column(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                taskMessage?.let { message ->
                    com.movtery.zalithlauncher.ui.AndroidStringText(text = message, style = MaterialTheme.typography.labelMedium.copy(color = CsColor.TextPrimary))
                }
                if (taskProgress < 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CsColor.Accent, trackColor = CsColor.Neutral4)
                } else {
                    LinearProgressIndicator(progress = { taskProgress }, modifier = Modifier.fillMaxWidth(), color = CsColor.Accent, trackColor = CsColor.Neutral4)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    taskProgress.takeIf { it >= 0f }?.let { progress ->
                        Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = CsColor.TextSecondary)
                    }
                    rateBytesPerSec?.let { bytes ->
                        val text = remember(bytes) { "${formatFileSize(bytes)}/s" }
                        Text(text = text, style = MaterialTheme.typography.labelMedium, color = CsColor.TextSecondary)
                    }
                }
            }
        }
    }
}
