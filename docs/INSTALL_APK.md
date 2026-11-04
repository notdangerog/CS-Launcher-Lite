# CS Launcher Lite — v1.1.0 (three-section redesign, arm64 debug)

## Download

**Direct link (no login needed):**
https://github.com/notdangerog/CS-Launcher-Lite/releases/download/v1.1.0/CS-Launcher-Lite-1.1.0-arm64-debug.apk

Release page: https://github.com/notdangerog/CS-Launcher-Lite/releases/tag/v1.1.0
Source: https://github.com/notdangerog/CS-Launcher-Lite (commit `a3734ea`)

| Property | Value |
| --- | --- |
| File | `CS-Launcher-Lite-1.1.0-arm64-debug.apk` |
| Size | 220,391,806 bytes (≈210 MiB) |
| SHA-256 | `80d286bcff6c2e532aaba79b8210343c70d96c4cb5eadba1c3b092614bfee5a9` |
| Package | `com.movtery.zalithlauncher.v2.debug` |
| App label | CS Launcher Lite |
| versionName / code | `1.1.0-debug` / 300100 |
| minSdk / compileSdk | 26 (Android 8.0) / 37 |
| ABI | arm64-v8a only |
| Signature | debug keystore (`CN=MovTery`), APK Signature Scheme v2 — verified |

## What changed in 1.1.0

* **Three-section layout** — icon-only rail (logo, Home / Discover / Instances, settings at
  the bottom), catalog center, permanent account panel behind a slightly thicker divider.
* **Home is the catalog** — Popular Modpacks, 2-column grid, ~4 cards visible, real Modrinth
  data sorted by downloads, 20 per page with infinite scroll, recent instances above it.
* **Account panel** — Playing as / avatar / username / animated switcher; Microsoft and
  Local-Offline sign-in; a single "Not signed in … Sign in" row when logged out.
* **Discover** — debounced search + Modpacks / Mods / Resource Packs / Worlds / Shaders.
* Instances and Settings keep every existing feature.

Full report: [`docs/CS_LAUNCHER_LITE_V2.md`](https://github.com/notdangerog/CS-Launcher-Lite/blob/main/docs/CS_LAUNCHER_LITE_V2.md)

## Install

```bash
adb install -r CS-Launcher-Lite-1.1.0-arm64-debug.apk
```

Or copy the APK to the device, allow "install unknown apps" and open it.

Verify the download first:

```bash
sha256sum CS-Launcher-Lite-1.1.0-arm64-debug.apk   # must match the hash above
```

## Caveats

- **Debug build**, signed with the debug keystore; not store-ready.
- **arm64 only.**
- Because the applicationId ends in `.v2.debug`, it does not share data with a release
  Zalith Launcher 2 install — point it at the same game folder in Settings → Storage.
- Runtime behaviour (actual Minecraft launch, downloads, Microsoft login) was **not**
  verified on a device: no emulator or phone exists in the build environment. Compilation,
  unit tests, packaging and signature verification were performed.

## Rebuild

```bash
git clone https://github.com/notdangerog/CS-Launcher-Lite.git
cd CS-Launcher-Lite
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64
```
Requires JDK 17+, Android SDK 37 / build-tools 37.0.0, NDK 25.2.9519653, Rust with
`aarch64-linux-android`, and `cargo-ndk`.
