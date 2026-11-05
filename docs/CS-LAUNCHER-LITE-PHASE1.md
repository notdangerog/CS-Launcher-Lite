# CS Launcher Lite — Phase 1 (UI/UX redesign)

Branch: `phase1-modrinth-ui` · Base: upstream `ZalithLauncher/ZalithLauncher2` @ `main`
Previous CS Launcher Lite UI attempts are intentionally **not** included: this branch starts
from the original source and applies only the Phase 1 redesign.

## What this branch contains

Modrinth-inspired shell and screens, implemented as a self-contained package
`ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/cs/`:

| Path | Purpose |
|---|---|
| `ui/cs/theme/CsTheme.kt` | Dark emerald design system (`#121212`, `#171717`, `#1E1E1E`, `#252525`, accent `#1FD760`) |
| `ui/cs/components/CsComponents.kt` | Cards, buttons, chips, tabs, search field, dropdowns, tags, skeletons, empty states |
| `ui/cs/shell/CsShell.kt` | Left icon sidebar (Home / Discover / Library + Settings & Accounts) and top bar |
| `ui/cs/data/*` | Mapping to the existing Modrinth search backend + view models |
| `ui/cs/screens/CsHomeScreen.kt` | Home: welcome, 2×2 featured modpack grid, account/friends/news sidebar |
| `ui/cs/screens/CsDiscoverScreen.kt` | Discover: 5 tabs, search, sort, paging, collapsible filter panel |
| `ui/cs/screens/CsLibraryScreen.kt` | Library: All/Modded/Vanilla, instance grid, empty state |
| `ui/cs/screens/CsWelcomeScreen.kt` | First-run welcome (green panel + agreements + Get Started) |

Wired into: `MainScreen.kt` (new shell, all previous entry points preserved),
`NormalNavKey.kt` (+ `Discover`, `Library`), `SplashScreen.kt` (new welcome).

## Real vs. not implemented

* Featured modpacks, Discover search/filters/paging, and Install all use the launcher's existing
  Modrinth searcher/mirror layer and the existing addon install screen. No mock data.
* “Data Packs” tab shows an explicit “not available in this build yet” state — `PlatformClasses`
  has no data-pack type in this launcher.
* Licence filter exposes Modrinth's working `open_source` facet; `license:*` facets are ignored
  by the Modrinth search API (verified).
* Friends panel is an honest placeholder; no friend backend exists.
* Library tabs are All/Modded/Vanilla (mapped to the real `VersionType`).

## Build

```bash
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64   # or all/arm/x86/x86_64
```
CI: `.github/workflows/push_ci.yml` → `build.yml` builds every ABI on push.

Upstream: Zalith Launcher 2, copyright MovTery and contributors, GPL v3. User-visible branding
replaced; internal package names kept unchanged on purpose.
