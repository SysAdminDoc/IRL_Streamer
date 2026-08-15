# IRL Streamer rebuild plan

## Scope and source of truth

This repository is an authorized clean-room reconstruction of the observable IRL Pro 3.5.23 Android experience. The immutable evidence under `../app-audit/` is the source of truth. No APK decompilation, private app storage, credentials, proprietary source, signing identity, or screenshot-backed interface is used.

The captured target comprises 145 visual states, 119 logical screen names, 15 primary flows, 15 dialogs, 37 selection dialogs/menus, nine live quick-panel states, one system-intent surface, and 1,145 interactive/adjustable instances. All 145 JSON screen specifications parse successfully and reference present PNG and UI-hierarchy evidence.

## Product identity and authorization decisions

- Repository: `C:\Users\--\repos\IRL_Streamer`
- Android project: `replica-app`
- Display name: `IRL Streamer`
- Original package (evidence only): `app.irlpro.android`
- Reconstruction package: `com.irlstreamer.reconstruction`
- Minimum SDK: 28
- Compile/target SDK: 36
- Implementation: Kotlin, Jetpack Compose, single activity, deterministic local repositories
- Original branding/assets: not authorized because `authorized-assets/` was absent at intake
- Live backend/transport integration: not authorized or documented; locally simulated behavior only
- Validation device: an isolated headless Android emulator, never the original Samsung phone

## Architecture and delivery phases

1. Establish measured theme tokens, landscape window/insets, navigation, reusable preference rows, dialogs, controls, and deterministic test-state routing.
2. Implement the complete settings hierarchy as data-driven destinations with audited labels, defaults, enabled states, choice menus, editors, scrolling states, validation, and local persistence.
3. Implement connection and overlay drafts, cancel/save behavior, empty managers, guarded local fixtures, and deterministic populated/error/loading triggers without contacting a backend.
4. Implement the live console, deterministic preview fixture, telemetry, audio meter, lens selection, mute, grid, safe margins, timestamp layer, quick-panel tabs, reload feedback, and no-connection Start guard.
5. Add unit, Compose UI, ADB, accessibility, persistence, lifecycle, and state-selector coverage.
6. Install and exercise debug and minified signed release builds on the isolated emulator. Capture replica states and generate side-by-side, overlay, heat-map, and metric reports against the audit baselines.
7. Update traceability and coverage honestly. A state is complete only after its behavior and visual evidence are validated.

## Fidelity constraints

- Reference viewport: 2316 x 1080 px, 450 dpi, 823.47 x 384 dp landscape.
- App settings retain the observed system status bar; the live console hides it by default.
- Core observed colors, dimensions, typography roles, and interaction wording come from `app-audit/design/`.
- Dynamic camera imagery and telemetry are legitimate mask candidates, but the final sweep deliberately used no masks so every replacement remains visible in raw metrics.
- Android/Material library icons are used where the audit classifies icons as replaceable.
- Accessibility debt in the source is intentionally corrected with named controls and 48 dp semantic hit targets; these improvements remain documented deviations.

## Validation gates

- Environment and audit-integrity checks pass.
- Debug unit tests, lint, assembly, install, launch, UI tests, and ADB smoke tests pass.
- Repository-owned signing identity is used; the ambient Android debug keystore is never used.
- The minified release APK installs and relaunches successfully on the emulator.
- Every captured state has an implementation status and traceability row.
- Every visually validated state has a current screenshot, side-by-side image, overlay, diff heat map, similarity metrics, and documented masks.
- Gradle is invoked serially from PowerShell with explicit JDK/SDK paths, `--no-daemon`, and `--no-configuration-cache`; daemons are stopped after each batch.

## Explicit non-goals until separately authorized

Real broadcasting, proprietary SRTLA/bonding behavior, live third-party chat/dashboard services, credential storage/export, real media creation, arbitrary remote WebView content, destructive reset/cookie clearing, external app installs, and actions that could affect the original phone are outside this locally functional reconstruction.

## Execution result

Phases 1–7 were executed. All 145 states are implemented and captured, six JVM tests and four device UI tests pass, lint passes, and the minified release installs and launches. The strict visual gate remains open: 0/145 screens reach SSIM 0.985 (median 0.836694; maximum 0.930146). The project is therefore delivered as a partially validated clean-room reconstruction; see `validation/reports/final-coverage-report.md` and `design-qa.md`.
