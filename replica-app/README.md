# IRL Streamer

IRL Streamer is an independent, authorized clean-room reconstruction of the observable Android interface documented in `../app-audit`. It is not the official IRL Pro application and contains no decompiled code, extracted private data, original signing material, or unauthorized artwork.

The implementation recreates the audited landscape live console, settings hierarchy, dialogs, forms, overlays, quick settings, validation states, and lifecycle-facing navigation with deterministic local fixtures. Broadcasting, bonding transport, remote chat, remote dashboards, camera capture, recording, and arbitrary WebView execution are deliberately safe simulations because no authorized backend or transport specification was supplied.

## Requirements

- Windows 10/11 with PowerShell 5.1 or newer
- Android SDK with platform/build tools 36 and `adb`
- Android Studio JBR / Java 17
- Python 3.12 with Pillow, NumPy, and scikit-image for visual comparison
- An isolated emulator; never use the original audit phone for replica QA

Run the preflight:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-environment.ps1 -Serial emulator-5554
```

## Build, install, and launch

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-debug.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-debug.ps1 -Serial emulator-5554
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\launch-replica.ps1 -Serial emulator-5554
```

- Debug package: `com.irlstreamer.reconstruction.debug`
- Release package: `com.irlstreamer.reconstruction`
- Supported Android versions: Android 9 (API 28) and newer
- Compile/target SDK: 36
- Orientation: forced landscape
- Reference test display: 2316 × 1080 captured pixels, 450 dpi, font scale 1.0, dark mode, three-button navigation

The repository-owned `irl-streamer-signing.jks` is a disposable local self-signed identity used for deterministic debug/release upgrade testing. It is not a production or Play signing key.

## Delivered validation status

The local release builds, installs, and cold-launches successfully. Six JVM tests, four device Compose tests, Android lint, and release signature verification pass. All 145 audit states have complete current/diff/overlay/side-by-side/JSON evidence.

The strict pixel gate does **not** pass: 0 of 145 full-screen comparisons meet SSIM 0.985 (median 0.836694; maximum 0.930146). The report intentionally retains clean-room preview, distinct identity, system UI, and remaining rendering differences rather than masking or waiving them. See `validation/reports/final-coverage-report.md` and `design-qa.md` before treating the app as audit-equivalent.

## Tests

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-unit-tests.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-ui-tests.ps1 -Serial emulator-5554
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-visual-validation.ps1 -Serial emulator-5554 -All
```

The full runner builds, tests, installs, launches, captures, compares, and reports:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-full-validation.ps1 -Serial emulator-5554 -AllScreens
```

Visual validation writes a current screenshot, labeled side-by-side image, 50% overlay, difference heat map, and JSON metrics for each state. The default SSIM threshold is 0.985 and is explicitly recorded for every screen in `validation/thresholds.csv`. Failed comparisons remain failures until the UI is corrected or a legitimate platform variance is documented.

## Reproducing debug states

Every audit catalog state is addressable only in the debug build:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\launch-replica.ps1 -Serial emulator-5554 -ScreenId 032_video_settings
```

The accepted `ScreenId` values are the 145 IDs in `../app-audit/screens/screen-catalog.csv`. Direct ADB is also supported:

```powershell
adb -s emulator-5554 shell am start -W -n com.irlstreamer.reconstruction.debug/com.irlstreamer.reconstruction.MainActivity --es screen_id 032_video_settings
```

Deterministic non-default state aliases are `loading`, `empty`, `network_error`, and `validation_error`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\launch-replica.ps1 -Serial emulator-5554 -State network_error
```

The selector is compiled out of release builds. Release launch extras do not change UI state.

## Project map

- `app/src/main/`: native Kotlin/Compose application
- `app/src/test/`: deterministic state and catalog unit tests
- `app/src/androidTest/`: launch, navigation, dialog, and form UI tests
- `docs/architecture.md`: state, navigation, persistence, and integration boundaries
- `docs/testing-guide.md`: emulator and evidence workflow
- `docs/audit-traceability-matrix.csv`: screen-level evidence mapping
- `validation/`: immutable baseline copies and generated comparison evidence
- `scripts/`: PowerShell 5.1 entry points and the pixel comparator

## Authorization and known limits

No authorized original-assets directory was supplied. The UI therefore uses Android library icons, a newly generated neutral preview fixture, independent geometry, system typography, and the distinct IRL Streamer identity. The original screenshots remain validation evidence only and are never rendered by the app. See `docs/known-deviations.md` and `docs/asset-rights-register.csv` for the complete register.

No live backend is connected. All telemetry, logs, network weights, connection forms, alerts, reloads, snapshots, recording controls, and overlay content remain on-device deterministic simulations. The code requests no camera, microphone, or network permission.

The final installed release smoke capture is `validation/current/release-launch.png`. The signed release APK is `app/build/outputs/apk/release/app-release.apk`.
