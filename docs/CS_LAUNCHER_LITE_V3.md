# CS Launcher Lite v1.2.0 — Unified UI (Complete Replacement)

## 1. Critical Task Recap
Previous implementation incorrectly merged ZalithLauncher2 UI with CS Lite UI. User ordered **complete removal** of both and replacement with ONE consistent brand-new UI per exact spec. All obsolete layouts, duplicated nav, overlapping components must be removed — preserve only backend launcher engine.

## 2. What Was Removed
- Old Zalith top-bar navigation, dashboard Welcome/Quick-Actions/Recent-Activity, HomeGrid/ActionMenu, Settings TabMenu overlap
- Previous merged 3-section with ClassicHome entry point (LauncherScreen old complex layout no longer exposed)
- Old filled icons (ic_home_filled etc.) replaced by thin-line outlines
- Old color ramp (#0D0F13 / #3DDC84) replaced by spec exact #121212 / #1E1E1E / #1DB954
- Duplicated account panel vs account chip logic unified into CsRightPanel

## 3. What Was Built (Spec-Exact)

### App Icon & Rail Logo
Same CS moon logo (crescent + metallic CS + stars on black) on mipmap-hdpi...xxxhdpi webp + adaptive icon (cs_ic_launcher_foreground via bitmap inset) + rail header `ic_cs_logo.xml` vector.

### Design System `CsDesign.kt`
Background #121212, Card #1E1E1E, Accent #1DB954, TextPrimary #FFF, TextSecondary #9AA0A6. Typography clean sans-serif. CsSize railWidth 60dp ultra-slim, accountPanel 272dp compact, divider 1dp/2dp, banner 140dp, catalogCard 268dp.

### Left Sidebar `CsShell.kt`
Enum CsSection{HOME,INSTANCES,DISCOVER,DOWNLOADS,SETTINGS}. CsRail: 60dp wide, logo 42dp top, then Home->Instances->Discover->Downloads (all thin-line new vectors ic_nav_*.xml) + Settings pinned bottom. Active = 48x32 green capsule pill #1DB954 with white icon; inactive = transparent with muted grey. No text names.

### Center — Recommended Modpacks `CsHomeScreen.kt` + `CsCatalog.kt`
Header exact string "Recommended Modpacks" + subtitle. CsCatalogCard large banner: 268dp total, 140dp banner (iconUrl cropped as screenshot with gradient scrim, 44dp rounded icon bottom-left), white title, muted grey 2-line desc, stats row (download icon + count + version + loader in #1DB954), neon green Install button (8dp rounded, white bold text, clickable). Real Modrinth via searchAssets DOWNLOADS sort, 20/page, infinite scroll, LaunchEffect+snapshotFlow, cancellation safe.

### Right Panel `CsRightPanel.kt`
Profile widget: 42dp avatar (PlayerFace when signed in, person_outlined when offline), name + typeLabel ("Local Account / Offline" / Microsoft). Accounts count + Manage link. Signed-out CTA: Card #1E1E1E with Not signed in — Sign in green button. News feed: 3 cards #1E1E1E, article icon #1DB954 chip, date tertiary, bold title white, summary grey, url on click -> WebScreen.

### Other Sections
- Instances `CsInstancesScreen.kt`: search + chips + list, Play/Edit/⋮ menu, create/manage, delete confirmation via VersionsOperation — all dark cards.
- Discover `CsDiscoverScreen.kt`: search + favorites + search-by-id + 5 category chips -> same banner grid.
- Downloads `CsDownloadsScreen.kt`: observes TaskSystem.tasksFlow, empty state with inbox icon, active tasks list with LinearProgressIndicator #1DB954 on #252525, stage + message.
- Settings `SettingsScreen.kt`: kept but now dark via new color scheme, no fake settings.

### Navigation `MainScreen.kt` + `NormalNavKey.kt`
Removed ClassicHome, added Downloads data object. csSectionOf/isCsSectionRoot updated for 5 sections. MainScreen: Surface CsColor.Background, BoxWithConstraints wide>=720dp => Row[rail|divider|center|divider(thick)|rightPanel]; narrow => Row[rail|divider|center] + rightPanel as AnimatedVisibility fade + slideIn drawer with scrim. CsTopBar no onClassicHome, taskRunning fixed to hasTasks, colors CsColor.Card. NavigationUI: LauncherMain->CsHomeScreen, Instances->CsInstancesScreen, Discover->CsDiscoverScreen, Downloads->CsDownloadsScreen, Settings->SettingsScreen, plus all sub-screens (License, AccountManager, WebScreen, VersionsManager, FileSelector, VersionSettings/Export, Download, AssetInfo, Multiplayer, LogView).

## 4. Verification
- compileDebugKotlin: BUILD SUCCESSFUL (warnings only for deprecated TextFieldLabelPosition/Slider, not blocking)
- assembleDebug: BUILD SUCCESSFUL 6m6s, APK 221, ... bytes, label CS Launcher Lite, versionName 1.2.0-debug/300101, arm64-v8a, v2 signature verified CN=MovTery
- Unit test CsNavigationTest updated for 5 sections, Downloads mapping, roots
- apksigner verify + aapt2 dump badging ok

## 5. Responsive & Polish
Portrait phones: rail stays, right panel hidden to drawer opened via account chip; 1 column banner grid. Landscape/tablet: full 3 sections, 2 columns. Capsule spring animation? Simplified to tween 180ms for stability. No overlapping, no duplicated buttons, consistent rounded corners 12-16dp, 4dp spacing grid.

## 6. Backend Preserved
Pojav JNI launch, AccountsManager Microsoft/Local, VersionsManager create/manage/edit/delete/launch, Java runtime, mod-loaders, download/install via swapToCategoryAssets -> DownloadScreen, file manager, multiplayer, TaskSystem, coil caching — all untouched, only presentation replaced.

