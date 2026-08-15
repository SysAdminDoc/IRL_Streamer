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

Pinned toolchain (Gradle and AGP are different versions and are easy to confuse):

| Component | Version |
|---|---|
| Gradle wrapper | 8.14.4 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin / Compose compiler plugin | 2.3.21 |
| Compose BOM | 2026.06.01 |

Do not bump the Compose BOM to 2026.08.00 or later without planning the rest: it requires compileSdk 37 and AGP 9.1.1 as a minimum, which is a toolchain migration rather than a dependency bump.

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

Measured 2026-08-15 on v0.2.0 (all 145 states, one build, `emulator-5554`). These
numbers drift with every change; `validation/reports/final-coverage-report.md` is
generated from the run itself and is authoritative.

| Gate | Result |
|---|---|
| Geometry | 1103 element origins matched against the audit hierarchy, 174 unmatched; mean origin error 48.78 px; 33.6% of origins within 2 px |
| Visual (strict, unmasked) | 0 of 145 at SSIM 0.985; median 0.869499 |
| Visual (app-chrome, secondary) | median 0.899487 |
| JVM unit tests | 25 passed |
| On-device Compose tests | 4 passed |
| Minified release | built, installed, cold-launched and screenshotted on device |

The geometry gate is enforced: it fails on missing coverage, on any state that
matched zero elements, and on a drop below the ratchet baseline in
`validation/geometry-baseline.json`. Lowering that baseline requires a stated
reason and evidence that coverage went up.

The strict pixel gate is **not** met and no threshold was lowered to make it pass.
Full numbers, per-surface medians, the ten weakest states, and build/release
evidence are in `validation/reports/final-coverage-report.md`; element-level
accuracy is in `validation/reports/geometry-validation-report.md`. Read
`docs/measured-tokens.md` for where each layout constant came from, and
`docs/known-deviations.md` before treating the app as audit-equivalent.

## Bonding needs a receiver

This is the single most common misunderstanding about SRTLA bonding, so it is
stated here rather than left to be discovered: **bonding does not improve a
connection on its own.** An SRTLA sender splits one stream across several links,
and something on the far side has to reassemble them. The chain is:

```
phone (SRTLA, multiple links) --> SRTLA receiver --> SRT --> RTMP --> platform ingest
```

Without that receiver there is nothing to bond *to*. Streaming platforms accept
RTMP and sometimes SRT; none of them accept SRTLA.

Self-hosted receivers, all open source:

| Project | Licence | Notes |
|---|---|---|
| [`irlserver/irl-srt-server`](https://github.com/irlserver/irl-srt-server) | AGPL-3.0 | SLS fork with SRTLA built in, per-stream limits and an HTTP stats API. |
| [`e04/go-irl`](https://github.com/e04/go-irl) | AGPL-3.0 | Cross-platform native binaries; lists IRL Pro, Moblin and BELABOX as compatible clients. |
| [`bluenviron/mediamtx`](https://github.com/bluenviron/mediamtx) | MIT | Terminates SRT and republishes to RTMP. **Does not speak SRTLA**, so it is the right target for a single-link SRT test, not for bonding. |

BELABOX's own `srtla_rec` is described by its authors as unsupported and not
suitable for production, so it is not recommended here. Hosted relays exist and
are what the commercial services charge for; this project does not endorse or
bundle one.

Nothing in this reconstruction transmits media (deviation D005). The chain above
is what a real transport implementation would have to target.

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

The release verification run writes its smoke capture to `validation/current/release-launch.png` (generated evidence, not committed) and records the result in `validation/reports/release-verification.txt`. The signed release APK is built to `app/build/outputs/apk/release/app-release.apk`.
