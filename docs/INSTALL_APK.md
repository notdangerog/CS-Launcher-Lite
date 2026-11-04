# CS Launcher Lite — v1.2.0 (unified UI, arm64 debug)

## Download

**Direct link (no login needed):**
https://github.com/notdangerog/CS-Launcher-Lite/releases/download/v1.2.0/CS-Launcher-Lite-1.2.0-arm64-debug.apk

Release page: https://github.com/notdangerog/CS-Launcher-Lite/releases/tag/v1.2.0
Source: https://github.com/notdangerog/CS-Launcher-Lite

| Property | Value |
| --- | --- |
| File | `CS-Launcher-Lite-1.2.0-arm64-debug.apk` |
| Size | 220447637 bytes (≈211 MiB) |
| SHA-256 | `82781b41d5e74f3b727f6b69210b2d086e96f5a7ef2f0f88c9408dba0551e7ef` |
| Package | `com.movtery.zalithlauncher.v2.debug` |
| App label | CS Launcher Lite |
| versionName / code | `1.2.0-debug` / 300101 |
| minSdk / compileSdk | 26 (Android 8.0) / 37 |
| ABI | arm64-v8a only |
| Signature | debug keystore (`CN=MovTery`), APK Signature Scheme v2 — verified |

## What changed in 1.2.0 — Complete UI Replacement (Unified)

This is the **one unified CS Launcher Lite interface** per the FINAL DESIGN SPEC — no merged old/new, no duplicated navigation:

* **Color & Theme:** Deep charcoal `#121212` background, rounded dark grey `#1E1E1E` cards, electric green `#1DB954` accent for active states and Install buttons. White primary text, muted grey secondary, clean sans-serif throughout.
* **Left Sidebar:** Ultra-slim (60dp) vertical rail with **ONLY** 5 thin-line icons — Home, Instances, Discover, Downloads, Settings — **NO text labels**. Active tab has a sleek rounded green capsule (pill) highlight. CS moon logo at top (same as app icon), gear pinned to bottom. Thin 1dp divider to center, 2dp to right panel.
* **Center:** **Recommended Modpacks** — large preview banner cards (140dp banner with game screenshot, rounded icon, title/description, download count/MC version/loader, neon green **Install** button). Real Modrinth data sorted by downloads, 20 per page with infinite scroll. No welcome banners, no duplicated quick-actions.
* **Right Panel:** Compact widget area — profile status card (avatar, username, "Local Account / Offline" or Microsoft when signed in) + **news feed** cards (Minecraft updates, modpack news, launcher news) with article icons. "Not signed in — Sign in" horizontal row when logged out. One source of truth via `AccountsManager`.
* **Five destinations:** Home (Recommended Modpacks), Instances (Play/Edit/⋮ with create/manage), Discover (search + categories Modpacks/Mods/Resource Packs/Shaders/Worlds), Downloads (active tasks with progress + empty states), Settings (Java, RAM, resolution, renderers, storage, appearance — all real settings, no fakes).
* **App Icon:** Crescent moon + metallic **CS** + stars on black — same logo inside rail and on launcher icon (mipmap + adaptive icon).
* **Responsive:** Rail + center + right panel on tablets/landscape (≥720dp); rail + center with right panel as slide-in drawer on phones/portrait. 1 column on narrow, 2 on wide, never overlap.
* **Preserved:** Pojav launch engine, Microsoft & Local auth, instance/version management, Java runtimes, mod-loader support, download/install, multiplayer, file manager, task system — all backend untouched.

## Install

```bash
adb install -r CS-Launcher-Lite-1.2.0-arm64-debug.apk
sha256sum CS-Launcher-Lite-1.2.0-arm64-debug.apk # must match SHA above
```

## Rebuild

```bash
git clone https://github.com/notdangerog/CS-Launcher-Lite.git
cd CS-Launcher-Lite
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64
```
Requires JDK 21, Android SDK 37 / build-tools 37.0.0, NDK 25.2.9519653, Rust with `aarch64-linux-android`, cargo-ndk 4.1.2.
