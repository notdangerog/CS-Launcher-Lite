# CS Launcher Lite — implementation notes

CS Launcher Lite is a rebranded, redesigned distribution of
[Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2) (GPL-3.0).
The Minecraft runtime, download engine, renderers, controller layer and account
stack are the original, working implementations — this work replaces the shell
(navigation, home, instances, discover, library, settings chrome, branding) with
a new, Modrinth-inspired design language.

---

## 1. Identity

| Item | Value | Notes |
| --- | --- | --- |
| Display name | `CS Launcher Lite` | `launcher_app_name` in `ZalithLauncher/gradle.properties` (manifest placeholder `launcher_name`) |
| Short name | `CS Lite` | `launcher_short_name` |
| Version | `1.0.0` (`versionCode 300000`) | `launcher_version_*` |
| applicationId | `com.movtery.zalithlauncher` + `.v2` | **unchanged on purpose** — see below |
| Namespace | `com.movtery.zalithlauncher` | unchanged |
| On-disk identifier | `ZalithLauncher` (`BuildKeys.LAUNCHER_IDENTIFIER`) | unchanged on purpose |

**Why the applicationId and the on-disk identifier stay untouched:** the package
name is what Android uses for app identity, permissions, exported intents and
upgrade paths, and `BuildKeys.LAUNCHER_IDENTIFIER` is the folder name that stores
per-instance launcher data (`<version>/ZalithLauncher/…`). Renaming either would
break existing installations, save data and instance configurations while adding
zero user-visible value. Branding is therefore applied at the display layer only:
app label, launcher icon, splash, in-app word mark, window title and default
colour palette.

## 2. Architecture (as found, preserved)

```
ZalithLauncher/                     app module (Jetpack Compose, Material 3 Expressive)
├── ui/screens/…
│   ├── _Navigation.kt              Navigation 3 helpers (navigateTo / clearWith / onBack)
│   ├── NavStateHost.kt             nested NavDisplay host
│   ├── NormalNavKey.kt             top-level destinations (serializable keys)
│   ├── NestedNavKey.kt             nested stacks: Main / Settings / Download / VersionSettings
│   └── main/MainScreen.kt          shell (top bar + task menu + NavDisplay)  ← redesigned
├── game/                           Minecraft core: launch, version info, mod loaders,
│                                   download engine, accounts, renderers, controls
├── setting/AllSettings.kt          MMKV-backed settings (single source of truth)
├── ui/theme/                       Material theme, palettes, shapes
└── ui/design/                      ★ new: CS design system (tokens + components)
LWJGL/ Terracotta/ PetLib/ …        untouched sub-modules (native + Rust components)
```

* **UI framework:** Jetpack Compose + Material 3 Expressive, AndroidX Navigation 3,
  Hilt DI, KSP, MMKV settings, Coil 3 for images, Room for accounts/paths.
* **Launch path:** `MainActivity` → `EventViewModel.Event.Launch` → `GameService`
  (`:game` process) → LWJGL/Terracotta. Unmodified.
* **Downloads:** `game/download/**` (Modrinth, CurseForge, MCIM, MultiMC, MCBBS).
  Unmodified; only used from new entry points.
* **Java runtime:** `components/jre`, `game/download/jvm_server`. Unmodified.

## 3. Design system (`ui/design/`)

`CsDesign.kt` — tokens:

* **Colour** `CsColor`: charcoal ramp `#08090C … #2E353F`, accent green `#3DDC84`,
  semantic warning/danger/info, light-theme ramp. Wired into Material as
  `csLiteDark` / `csLiteLight` `ColorScheme`s and registered as the new default
  `ColorThemeType.CS_LITE` (previous palettes and dynamic colour remain selectable
  in Settings → Launcher → theme).
* **Shapes** `CsShapes`: 6/10/14/18/24 dp radius scale.
* **Typography** `CsTypography`: semibold display/headline/title, tighter
  letter-spacing, medium labels.
* **Spacing/size** `CsSpace` (4 dp grid) and `CsSize` (sidebar 196 dp, top bar
  56 dp, buttons 40/48 dp, icon sizes, tile min width).

`CsComponents.kt` — reusable primitives so nothing is duplicated per screen:
`CsCard`, `CsSectionHeader`, `CsPrimaryButton`, `CsTonalButton`, `CsIconButton`,
`CsChip`, `CsSearchField`, `CsEmptyState`, `CsMetaLabel`, `CsStatusPill`,
`CsNavItem`, `CsNavSectionLabel`, `CsDivider`.

Because every legacy screen already reads `MaterialTheme.colorScheme`, shapes and
typography, changing the palette/typography in one place reskins the *entire*
launcher (including screens that were not individually rebuilt) without touching
their layout code.

## 4. Navigation shell

`ui/screens/lite/CsShell.kt`:

* `CsSidebar` — permanent left rail: brand block, **Home / Instances / Discover /
  Library / Settings**, version footer. Selected item uses an accent tint plus
  accent text, animated with a 160 ms colour tween.
* `CsTopBar` — section title, optional detail title, back affordance for
  non-root screens, task indicator, multiplayer, file manager and the account chip.
* `csSectionOf(key)` maps the current Navigation 3 key onto the owning section so
  the rail stays correct for nested stacks (`NestedNavKey.Download`, `Settings`,
  `VersionSettings`). Covered by unit tests.

The shell replaced the previous 40 dp top bar with icon rail. Navigation semantics
are unchanged: section switches use `removeAndNavigateTo(removes = clearBeforeNavKeys)`
so the back stack cannot grow sideways, and system back is still handled by
`MainActivity`/`onBack`.

### Screens

| Section | Composable | Behaviour |
| --- | --- | --- |
| Home | `CsHomeScreen` | Greeting with account name, hero card of the current instance (icon, MC version, loader, mod count, large **Play**), recent instances, quick actions (install, import modpack, multiplayer, accounts, files, settings, card-grid home), recent-activity card fed by `TaskSystem`. |
| Instances | `CsInstancesScreen` | Search, All/Modded/Vanilla filters, refresh, create, game-folder management, per-row play / settings / pin / rename / copy / export / delete / set-current. Reuses `VersionsOperation` dialogs. |
| Discover | `CsDiscoverScreen` | Content hub (game, modpacks, mods, resource packs, shaders, worlds, ID search, favourites) routing into the existing download flows. |
| Library | `CsLibraryScreen` | Per-instance content inventory (mods, resource packs, shaders, worlds, screenshots, servers) with direct jumps to the matching manager screens. Stats are scanned off the main thread. |
| Settings | `SettingsScreen` (restyled) | Same nested settings screens, new CS rail. |
| Classic home | `NormalNavKey.ClassicHome` | The original card-grid home is still reachable from Home → *Card grid home* — no feature was removed. |

New navigation keys: `NormalNavKey.Instances`, `.Discover`, `.Library`,
`.ClassicHome`. `LauncherMain` now hosts the new dashboard.

## 5. Branding assets

* `drawable/ic_cs_logo.xml` — sidebar/splash mark (charcoal tile + green cube).
* `drawable/cs_ic_launcher_foreground|background|monochrome.xml` — adaptive icon
  layers; `mipmap-anydpi-v26/ic_launcher(.round).xml` point at them (minSdk 26, so
  every device uses the new artwork).
* `drawable/splash_launcher.xml` — splash icon.
* Startup splash screens (`SplashScreen.kt`, `WelcomeScreen.kt`) use the new mark
  and `BuildKeys.LAUNCHER_NAME` (= “CS Launcher Lite”).

## 6. Performance notes

* One `collectAsStateWithLifecycle` subscription per data source in `MainScreen`;
  screens receive plain values — no duplicate flows per screen.
* Instance filtering/sorting use `derivedStateOf` + `remember`, so typing in the
  search box does not rebuild the list and scrolling allocates nothing.
* `LazyColumn`/`LazyVerticalGrid` use stable keys and `contentType`.
* Filesystem scans (mod/resource-pack/shader/world/screenshot counts) run on
  `Dispatchers.IO` inside `produceState`, keyed by version — never on the main
  thread, never per frame, and only for visible rows.
* Images still go through the existing Coil singleton (20 MB memory cache, 512 MB
  disk cache, crossfade) — unchanged because it is already sized correctly.
* Animations are limited to colour/elevation tweens (150–180 ms) on interaction;
  no infinite or heavy motion was added. `Mode`/`StateFlow` reading follows the
  existing lifecycle-aware pattern.
* No new dependencies were added.

## 7. Build & verification

Toolchain used (sandbox): JDK 21 (Temurin), Gradle 9.5.0 (wrapper), AGP 9.3.0,
Android SDK Platform 37.2 / Build-Tools 37.0.0, NDK 25.2.9519653, Rust + cargo-ndk
4.1.2 for `PetLib`.

```bash
# full debug APK (one ABI)
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64

# all architectures
./gradlew :ZalithLauncher:assembleDebug
```

Verified in this workspace:

* `assembleDebug -Darch=arm64` → **BUILD SUCCESSFUL**,
  `ZalithLauncher/build/outputs/apk/debug/ZalithLauncher-Debug-1.0.0-arm64-v8a.apk`
  (220 MB, as expected for a bundled JRE + LWJGL + renderer APK).
* `aapt2 dump badging` → `application-label: 'CS Launcher Lite'`,
  `versionName 1.0.0`, `minSdk 26`, new `res/drawable/cs_ic_launcher_*` present.
* `:ZalithLauncher:testDebugUnitTest --tests "…ui.screens.lite.*"` → 6/6 pass
  (`CsNavigationTest`: sidebar mapping + back-affordance rules).
* Full `:ZalithLauncher:testDebugUnitTest` suite → **123 tests, 115 pass, 8 fail**.
  All 8 failures are in untouched, non-UI code and are environment/fixture
  dependent (`VersionCompareTest` needs an Android-backed initializer,
  `FileTest.testCalculateFileSha1` requires a fixture file that is not present,
  `MurmurHash2IncrementalTest` needs a missing asset file). `git diff` confirms
  none of their sources were modified by this change set.

### What is *not* verified here

This sandbox has no Android device or accelerating emulator (2 vCPU / 2 GB RAM),
so the following were **not** executed: on-device launch, touch scrolling,
game download, and the actual Minecraft launch. Those paths are untouched by this
change set, but they must be validated on a real device before shipping:

1. cold start → splash → home (no crash, theme applied),
2. sidebar navigate through all five sections and back,
3. instance list: select, rename, copy, delete, export, play (with a real
   Minecraft install),
4. Discover → each download category,
5. Library → each content manager,
6. Settings → every nested page, including the theme picker (`CS Lite Green`).

## 8. Follow-ups (deliberately out of scope)

* Migrating the remaining detail screens (version settings, download lists) to the
  CS component set — they already inherit the palette/typography/shape tokens.
* Tablet/foldable adaptive layout (rail → expanded drawer).
* Localisation of the new strings for the 20+ existing locales (English source is
  in `values/strings.xml`, other locales fall back to English).
