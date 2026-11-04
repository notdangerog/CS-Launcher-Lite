# CS Launcher Lite v2 — three-section redesign

## 1. Repository inspection (what had to be preserved)

| Area | Finding |
| --- | --- |
| UI framework | Jetpack Compose + Material 3, Navigation 3 (`NavDisplay`, `NavBackStack`, `TitledNavKey`) |
| Shell | `ui/screens/main/MainScreen.kt` -> `NavigationUI` (nav graph), `ScreenBackStackViewModel` |
| Launch core | PojavLauncher-derived JNI, untouched |
| Instances | `game/version/installed/Version`, `VersionsManager`, `VersionsOperation` (rename/copy/delete dialogs) |
| Accounts | `game/account/AccountsManager` (`accountsFlow`, `currentAccountFlow`, `setCurrentAccount`), `AccountManageScreen` + `FirstLoginMenu` (NONE / MICROSOFT / NORMAL) |
| Content APIs | `game/download/assets/platform/*`: mirror-aware `searchAssets()`, Modrinth + CurseForge searchers, `PlatformClasses` (MOD, MOD_PACK, RESOURCE_PACK, SAVES, SHADERS), `PlatformSearchData` |
| Install flow | `ScreenBackStackViewModel.swapToCategoryAssets()` -> download screen -> `NormalNavKey.DownloadAssets` detail -> real version picker + installer |
| Images | Coil 3, 20 MB memory + 512 MB disk cache (`ZLApplication.newImageLoader`) |
| Settings | `SettingsScreen` with the upstream category rail, all backed by real settings |

Nothing in the launch / install / auth path was rewritten. The redesign is a presentation
layer plus a read-only catalog view model on top of the existing search API.

## 2. What was implemented

### Shell — exactly three sections
```
Row {
    CsRail()                  // 72dp, icons only
    CsSectionDivider()        // 1dp hairline
    Column { CsTopBar(); screens }   // center
    CsSectionDivider(thick)   // 2dp
    CsAccountPanel()          // 288dp, always visible at width >= 720dp
}
```
* `>= 720dp` (all landscape phones, tablets): full three-section layout.
* Narrower portrait: rail + center, account panel becomes a slide-in overlay opened from an
  account chip in the center header (spec allows a drawer for small screens).
* `CsSection` = HOME, DISCOVER, INSTANCES, SETTINGS. The previous "Library" section is gone.

### Rail (`ui/screens/lite/CsShell.kt`)
Logo on top, then Home / Discover / Instances as **icons only**, settings gear pinned to the
bottom. Selected entry: translucent green container plus an animated 3dp green indicator on
the left edge (spring, no bounce).

### Home = the catalog (`CsHomeScreen.kt`)
* Two-column `LazyVerticalGrid`; card height derived from available height
  (`(maxHeight - header) / 2`, clamped 124..196dp) so ~four cards (2x2) are visible on a
  landscape phone and the grid breathes on tablets.
* With instances: a "Recent instances" strip (current first) above the catalog. Without:
  the catalog starts at the top. Only real `Version` objects are shown.
* Infinite scroll: next page requested within 4 items of the end (20 per page, so the first
  page already exceeds the required 12 recommendations).

### Catalog layer (`CsCatalog.kt`)
* `CsCatalogViewModel` calls the existing `searchAssets()`; empty query -> `DOWNLOADS`
  (real popularity), typed query -> `RELEVANCE`. 450 ms debounce, one request in flight,
  pages appended, `CancellationException` rethrown.
* `CsCatalogEntry` is `@Immutable` and carries platform id, title, description, author, icon,
  downloads, supported versions, mod loaders — all from the API response.
* `PlatformSearchData` gained `platformVersions()` (default null) so cards can show the
  supported Minecraft versions (Modrinth `versions`, CurseForge `latestFilesIndexes`).

### Cards
Cover artwork (project icon as blurred cropped banner + crisp rounded icon + gradient scrim),
title, two-line description, then one metadata row: compact download count, Minecraft
version, first mod loader and an accent install button. Card body opens the existing download
detail screen; the install button does the same — no fake installs.

### Account panel (`CsAccountPanel.kt`)
* Signed in: "Playing as", avatar, username, account type and dropdown arrow; tapping expands
  (animated) the account switcher with the active account accent-marked plus "Manage accounts"
  which opens `AccountManageScreen`.
* Signed out: "Select account or username", two compact rows (Microsoft account, Local /
  Offline account) wired to the real login flows, and one horizontal bottom row:
  *Not signed in* …… **[Sign in]**. Remaining space stays empty.

### Discover (`CsDiscoverScreen.kt`)
Large debounced search field, favourites + search-by-ID shortcuts, horizontal category row
(Modpacks / Mods / Resource Packs / Worlds / Shaders) and the same card grid, 1-4 columns by
width, each result with icon, name, description, downloads, versions, loaders, install action.

### Instances / Settings
Instances keeps the working CS manager (icon, name, MC version, loader, mod count, Play,
edit, three-dot menu: set current / rename / copy / export / delete with confirmation,
create, search, filter). Settings keeps the existing real categories (Java, RAM, resolution,
renderer/performance, storage, appearance, launcher behaviour).

## 3. Performance

* One catalog view model per category for the session: navigating away and back never
  refetches; category switches reuse cached pages.
* Paged, append-only state, stable keys (`platform_projectId`), immutable models: typing and
  scrolling never rebuild the whole grid.
* Debounced search with job cancellation; skeletons instead of layout jumps; error card with
  retry instead of silent failure.
* Coil 3 caching unchanged, so artwork is decoded once.

## 4. Verification

| Check | Result |
| --- | --- |
| `:ZalithLauncher:compileDebugKotlin` | BUILD SUCCESSFUL |
| `:ZalithLauncher:testDebugUnitTest --tests "...ui.screens.lite.*"` | `CsNavigationTest` 6/6 passed |
| `:ZalithLauncher:assembleDebug -Darch=arm64` | BUILD SUCCESSFUL, APK 220,391,806 bytes |
| `aapt2 dump badging` | label *CS Launcher Lite*, 1.1.0-debug / 300100, arm64-v8a |
| `apksigner verify --print-certs` | v2 signature valid (`CN=MovTery`) |
| New strings present in `resources.arsc` | yes ("Popular Modpacks", account panel strings) |

**Not verified:** on-device rendering, touch behaviour and a real Minecraft launch - this
environment has no emulator or device. All code paths those screens call are the ones the
launcher already shipped.

## 5. Deliberate deviations (and why)

1. **No Data Packs category.** The launcher has no data-pack install target: `VersionFolders`
   has no `datapacks` entry and `PlatformClasses` has no data-pack class (CurseForge exposes
   no such class id here). The category would be a dead button, which the spec forbids.
   Adding it properly needs `VersionFolders.DATAPACK`, `PlatformClasses.DATA_PACK` and the
   `world/datapacks` install path - contained, can be done as a follow-up.
2. **Worlds uses CurseForge** (Modrinth has no world project type). Without a CurseForge API
   key the category shows the error card with retry instead of fake content.
3. **Portrait phones** use rail + center with a slide-in account panel, as the spec permits.
4. **Application ID unchanged** (`com.movtery.zalithlauncher.v2.debug`) as previously agreed;
   only the display layer is renamed.
5. The **classic card-grid home** (upstream `LauncherScreen`) stays reachable through the
   dashboard icon in the center header, so no existing functionality became unreachable.
