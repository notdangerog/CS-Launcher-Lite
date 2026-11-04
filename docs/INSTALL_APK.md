# CS Launcher Lite — APK

| Field | Value |
| --- | --- |
| File | `CS-Launcher-Lite-1.0.0-arm64-debug.apk` |
| Size | 220,346,618 bytes (≈ 210 MiB) |
| Package | `com.movtery.zalithlauncher.v2.debug` |
| App label | **CS Launcher Lite** |
| Version | 1.0.0 (versionCode 300000, `-debug` suffix) |
| ABI | **arm64-v8a only** (all modern phones; no x86/32-bit) |
| minSdk / targetSdk | 26 / 34 (built against SDK 37.2) |
| Build type | debug (not minified) |
| Signature | v2, debug keystore bundled with the project (`CN=MovTery`) |
| SHA-256 | `b4390a903f619a64da2b7d24eb24e65a0b0fe189c9ebe895652db85a44d6479c` |

## Install

1. Copy the `.apk` to the device (USB, `adb push`, or download link).
2. Allow installation from unknown sources for the file manager/browser you use.
3. Install and launch. First start unpacks the bundled Java runtimes, so expect a
   longer first launch than subsequent ones.

With adb:

```bash
adb install -r CS-Launcher-Lite-1.0.0-arm64-debug.apk
```

## Important notes

* **Debug build.** It is signed with the project's *debug* keystore, not a release
  key. It can be installed side-by-side with a Zalith Launcher 2 install because
  the applicationId keeps the `.v2.debug` suffix — but because the package name is
  `…zalithlauncher.v2.debug`, Android treats it as a **different app**: it will not
  see data of a release Zalith install. To try it against existing instances, point
  CS Launcher Lite at the same game folder from *Instances → Game folders*.
* **Why 210 MiB:** the APK bundles four Java runtimes (JRE 8/17/21/25, ~70 MiB of
  xz-compressed runtime archives), the OSMesa/GL translation libraries (~48 MiB),
  LWJGL natives and the whole Compose/Kotlin dependency graph in dex form. This is
  the same footprint as upstream Zalith Launcher 2 — a release build with R8 and
  per-ABI splits is noticeably smaller.
* **arm64 only.** Building all four ABIs needs
  `./gradlew :ZalithLauncher:assembleDebug` without `-Darch=arm64`.

## Verified on this build

```
aapt2 dump badging   → application-label:'CS Launcher Lite', versionName 1.0.0-debug
apksigner verify     → Verifies (v2 scheme)
gradle assembleDebug → BUILD SUCCESSFUL
unit tests           → CsNavigationTest 6/6 pass
```

**Not verified:** runtime behaviour on a device (no emulator/device in this
sandbox). The checklist for a device pass is in `CS_LAUNCHER_LITE.md` §7.

## Rebuild it yourself

```bash
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64
# output: ZalithLauncher/build/outputs/apk/debug/
```

or use the repository's GitHub Actions workflow **“Build APK”**
(`.github/workflows/build.yml`) with `variant: Debug` — it produces per-ABI
artifacts. The full change set is also available as `CS-Launcher-Lite.patch`
(applies onto upstream commit `9546e84`).
