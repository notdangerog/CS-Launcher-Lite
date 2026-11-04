# CS Launcher Lite — v1.0.0 (arm64 debug)

## Download

**Direct link (no login needed):**
https://github.com/notdangerog/CS-Launcher-Lite/releases/download/v1.0.0/CS-Launcher-Lite-1.0.0-arm64-debug.apk

Release page: https://github.com/notdangerog/CS-Launcher-Lite/releases/tag/v1.0.0
Source repository: https://github.com/notdangerog/CS-Launcher-Lite

| Property | Value |
| --- | --- |
| File | `CS-Launcher-Lite-1.0.0-arm64-debug.apk` |
| Size | 220,346,666 bytes (≈210 MiB) |
| SHA-256 | `ebd4546c3e787e55eba5599ffa39994ae64b96f546bb64d65c2dc5bb78ab6027` |
| Package | `com.movtery.zalithlauncher.v2.debug` |
| App label | CS Launcher Lite |
| versionName / code | `1.0.0-debug` / 300000 |
| minSdk / compileSdk | 26 (Android 8.0) / 37 |
| ABI | arm64-v8a only |
| Signature | debug keystore (`CN=MovTery`), APK Signature Scheme v2 — **verified** |

Verified with `aapt2 dump badging` (label, version, native-code) and `apksigner verify --print-certs`.

## Install

1. Copy the APK to the device (or use `adb`).
2. Enable *Install unknown apps* for your file manager / browser.
3. Open the file and install.

```bash
adb install -r CS-Launcher-Lite-1.0.0-arm64-debug.apk
```

Verify the download first:

```bash
sha256sum CS-Launcher-Lite-1.0.0-arm64-debug.apk   # must match the hash above
```

## Why is it ~220 MB?

The APK bundles everything a Java-Edition launch needs offline, same as upstream:

- four Java runtimes (JRE 8 / 17 / 21 / 25 archives) — ~80 MB compressed
- OSMesa / OpenGL compatibility libraries for several renderers — ~48 MB
- application code + Compose resources (`classes*.dex` ≈ 19 MB)
- native libs (Pojav core, LWJGL, Rust-built components) for arm64-v8a

A release build with R8 + per-ABI splits and runtime-JRE downloading would be far smaller;
this is a debug build for testing.

## Caveats

- **Debug build** — signed with the debug keystore; do not ship to a store.
- **arm64 only** — no armeabi-v7a / x86_64 build in this release.
- Because the applicationId is `…v2.debug`, it does **not** share data with a release
  Zalith Launcher 2 install. Point CS Launcher Lite at the same `.minecraft` folder in
  Settings → Storage if you want to reuse existing instances.
- Runtime behaviour (launching a real game, downloads, logins) was **not** verified on a
  device in this environment — no emulator or phone was available. The device checklist is
  in `docs/CS_LAUNCHER_LITE.md` §7.

## Rebuild from source

```bash
git clone https://github.com/notdangerog/CS-Launcher-Lite.git
cd CS-Launcher-Lite
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64
```

Requires JDK 17+, Android SDK 37 / build-tools 37.0.0, NDK 25.2.9519653, Rust targets
`aarch64-linux-android` + `x86_64-linux-android`, and `cargo-ndk`.
Measured on 2 vCPU / 2 GB RAM with warm caches: **BUILD SUCCESSFUL in 18m 13s**.
