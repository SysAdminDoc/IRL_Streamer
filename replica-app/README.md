# IRL Streamer

IRL Streamer is an independent, authorized clean-room reconstruction of the observable Android interface documented in `../app-audit`. It is not the official IRL Pro application and contains no decompiled code, extracted private data, original signing material, or unauthorized artwork.

The implementation recreates the audited landscape live console, settings hierarchy, dialogs, forms, overlays, quick settings, validation states, and lifecycle-facing navigation with deterministic local fixtures. Broadcasting, bonding transport, remote chat, remote dashboards, camera capture, recording, and arbitrary WebView execution are deliberately safe simulations because no authorized backend or transport specification was supplied.

## Provenance of the audited original

The audit left the original's streaming engine recorded as unknown. It is identifiable from evidence the audit already collected, so it is stated here rather than left for the next reader to re-derive:

- Every class in the audited package carries the `com.wmspanel.streamer.*` prefix, the deep-link scheme is `larix:`, and the About screen declares "Includes licensed SRTLA code" (`../app-audit/app/application-identity.md`, `../app-audit/app/components-and-intents.md`).
- `com.wmspanel` is Softvelum, whose own published application is `com.wmspanel.larix_broadcaster`.
- The `go-irl` SRTLA server lists IRL Pro (Android) among its compatible clients, so the original speaks standard, interoperable SRTLA rather than a private variant.

The audited original therefore runs Softvelum Larix broadcaster code with BELABOX SRTLA bonding added. The Softvelum code is **verified** from the class prefix and deep-link scheme; the specific commercial licensing arrangement behind it is a **strong inference**, not a verified fact.

None of that code is present here. This reconstruction shares no source, no assets, and no signing identity with the original, is not affiliated with or endorsed by Softvelum, BELABOX, or the authors of IRL Pro, and does not present itself as the official application.

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

The reconstruction is validated by three gates, in the order they constrain it.

**1. Geometry — replica UI hierarchy vs audit UI hierarchy.** SSIM tells you *that*
a screen differs; only the hierarchy tells you *which element* moved, which is the
target the audit states in pixels. `scripts/geometry_diff.py` pairs nodes by visible
label and measures each element's drawn origin against
`../app-audit/evidence/ui-xml/<id>.xml`. Current numbers are in
`validation/reports/geometry-validation-report.md`.

**2. Visual — unmasked whole-screen SSIM at 0.985.** Strict and deliberately not
met by masking. Masks exist (`validation/masks/mask-register.csv`) but feed only a
*secondary* app-chrome metric that excludes system-owned pixels: the Android status
bar, the Samsung navigation strip, and the IME window on the three states where the
keyboard covers the app. The camera preview is **not** masked — excluding it would
remove about 97% of a live-console screen and leave a meaningless number.

**3. Behaviour — JVM and on-device Compose tests.**

Current measured outcome (all 145 states, one build, `emulator-5554`):

| Gate | Result |
|---|---|
| Geometry | mean element-origin error 50.88 px; 34.9% of origins within 2 px; 62 of 145 states have at least half their origins within 2 px |
| Visual (strict, unmasked) | 0 of 145 at SSIM 0.985; median 0.869631 (previous pass 0.836694) |
| Visual (app-chrome, secondary) | median 0.896448 |
| JVM unit tests | 11 passed |
| On-device Compose tests | 4 passed |
| Minified release | built, installed, cold-launched and screenshotted on device |

The strict pixel gate is **not** met and no threshold was lowered to make it pass.
Full numbers, per-surface medians, the ten weakest states, and build/release
evidence are in `validation/reports/final-coverage-report.md`; element-level
accuracy is in `validation/reports/geometry-validation-report.md`. Read
`docs/measured-tokens.md` for where each layout constant came from, and
`docs/known-deviations.md` before treating the app as audit-equivalent.

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

Visual validation writes a current screenshot, a UI-hierarchy dump, a labeled side-by-side image, a 50% overlay, a difference heat map, and JSON metrics for each state. The hierarchy dump feeds the geometry gate:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-geometry-validation.ps1
```

The default SSIM threshold is 0.985 and is explicitly recorded for every screen in `validation/thresholds.csv`. Failed comparisons remain failures until the UI is corrected or a legitimate platform variance is documented.

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
- `docs/audit-traceability-matrix.csv`: screen-level evidence mapping (generated by `scripts/update_traceability.py`)
- `docs/measured-tokens.md`: every layout constant and the audit measurement behind it
- `validation/`: immutable baseline copies and generated comparison evidence
- `scripts/`: PowerShell 5.1 entry points, the pixel comparator, the hierarchy geometry differ, the mask generator, and the scroll-anchor extractor

## Authorization and known limits

No authorized original-assets directory was supplied. The UI therefore uses Android library icons, a newly generated neutral preview fixture, independent geometry, system typography, and the distinct IRL Streamer identity. The original screenshots remain validation evidence only and are never rendered by the app. See `docs/known-deviations.md` and `docs/asset-rights-register.csv` for the complete register.

No live backend is connected. All telemetry, logs, network weights, connection forms, alerts, reloads, snapshots, recording controls, and overlay content remain on-device deterministic simulations. The code requests no camera, microphone, or network permission.

The final installed release smoke capture is `validation/current/release-launch.png`. The signed release APK is `app/build/outputs/apk/release/app-release.apk`.
