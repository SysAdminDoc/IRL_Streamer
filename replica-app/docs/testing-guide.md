# Testing guide

## Isolation rule

All replica commands require a serial, and `Assert-ReplicaDevice` in `scripts/Common.ps1` refuses any target that does not report itself as an emulator (serial prefixed `emulator-`, or `ro.kernel.qemu=1`). Use an isolated emulator serial such as `emulator-5554`. The original audit phone is never a valid target: the guard rejects it, and the physical serial is deliberately not written down here so it cannot be copied out of the docs by mistake. `-AllowPhysicalDevice` exists as an explicit, authorized escape hatch only. The scripts do not inspect, launch, clear, or modify the original package.

Start the configured emulator without a visible window:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-headless-emulator.ps1 -Avd issue-sweep-api36
```

The script configures 1080 × 2316 natural resolution, 450 dpi, landscape rotation, font scale 1.0, dark mode, and three-button navigation. Confirm the returned `HEADLESS_EMULATOR_SERIAL` and use it explicitly for every later command.

## Build and static gates

```powershell
.\scripts\check-environment.ps1 -Serial emulator-5554
.\scripts\run-unit-tests.ps1
.\scripts\build-debug.ps1
```

Unit tests verify all 145 mappings, named loading/empty/error states, representative dialogs/quick panels, override precedence, and settings-catalog coverage. Gradle is always run with the configured Java 17 runtime, `--no-daemon`, and `--no-configuration-cache`, then stopped.

## UI tests

```powershell
.\scripts\install-debug.ps1 -Serial emulator-5554
.\scripts\run-ui-tests.ps1 -Serial emulator-5554
```

The runner sets `ANDROID_SERIAL` to the explicit emulator before invoking Android Gradle Plugin. Compose tests use semantic tags instead of screen coordinates and cover launch, the broadcast guard, quick-panel tabs, hierarchical settings navigation, Back, and disabled blank-form Save behavior.

## Visual validation

One state:

```powershell
.\scripts\capture-replica-screen.ps1 -Serial emulator-5554 -ScreenId 002_settings_root
.\scripts\compare-screen.ps1 -ScreenId 002_settings_root
```

Representative cross-surface pass:

```powershell
.\scripts\run-visual-validation.ps1 -Serial emulator-5554
```

All captured states:

```powershell
.\scripts\run-visual-validation.ps1 -Serial emulator-5554 -All
```

For each state, inspect:

- `validation/current/SCREEN_ID.png`
- `validation/side-by-side/SCREEN_ID.png`
- `validation/overlays/SCREEN_ID.png`
- `validation/diffs/SCREEN_ID.png`
- `validation/results/SCREEN_ID.json`

The comparator first rejects dimension mismatches, searches only the configured ±2 px alignment window, applies a screen mask only if one exists, calculates SSIM/MAE/RMSE/pixels-within-tolerance, and fails below the screen threshold. Mask semantics and review rules are in `validation/masks/README.md`.

## Release gate

Debug success does not prove minified release correctness:

```powershell
.\scripts\build-release.ps1
.\scripts\install-release.ps1 -Serial emulator-5554
.\scripts\launch-replica.ps1 -Serial emulator-5554 -Release
```

After launch, verify the foreground component and capture a release smoke screenshot with explicit `adb -s emulator-5554` commands. The release package ignores debug-state extras.

## Result policy

A screen moves to `VISUALLY_VALIDATED` only when it has all comparison artifacts and passes its configured threshold, or when a visible platform-only variance is reviewed and explicitly documented. A behavior moves to `BEHAVIORALLY_VALIDATED` only after its relevant unit/UI/ADB test passes. Capturing a screenshot alone is not validation.

## Latest verified run

The 2026-08-15 isolated-emulator run produced 145/145 complete comparison bundles. The JVM and device Compose suites passed, lint passed, and the minified release was installed and resumed as `com.irlstreamer.reconstruction/.MainActivity`. All 145 strict visual comparisons remained below their thresholds, and no threshold was lowered or waived. Test counts and metrics are read from the run itself by `scripts/build_final_report.py`; see `validation/reports/final-coverage-report.md` and `design-qa.md` for reviewed visual blockers.
