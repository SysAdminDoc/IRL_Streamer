# IRL Pro clean-room audit

This directory is an audit-only reconstruction blueprint for IRL Pro 3.5.23 (`app.irlpro.android`). It was produced from authorized, observable behavior on a connected Android phone. No APK decompilation, root access, private-storage access, traffic interception, credential capture, account changes, real broadcasts, recordings, messages, or purchases were used.

No replacement app or Android source project is included. Implementation is intentionally deferred until the operator says `BEGIN REBUILD`.

## Audit snapshot

- **CONFIRMED:** 145 full-resolution visual states, each paired with UI Automator XML, focused-activity evidence, window/keyboard summaries, capture metadata, and a JSON screen specification.
- **CONFIRMED:** 119 logical screen names, including 15 dialogs, 37 selection dialogs/menus, 9 live quick-settings states, and 1 system-intent surface.
- **CONFIRMED:** 1,145 interactive/adjustable control instances and 378 deduplicated resource/label/type signatures were cataloged from the captured trees.
- **CONFIRMED:** one privacy-trimmed cold-launch recording, target-scoped package/runtime evidence, and device-specific launch/memory/frame metrics.
- **UNKNOWN/UNTESTED:** real streaming, recording/snapshot output, populated connection management, settings import/export payloads, external support links, debug-detail sharing, reset/cookie clearing, USB-camera behavior, lock-screen touch blocking, offline failures, and first-run permission/onboarding states.

## Where to start

1. Read [`audit-summary.md`](audit-summary.md).
2. Use [`screens/screen-catalog.csv`](screens/screen-catalog.csv) to locate a state.
3. Open its PNG under `evidence/screenshots/`, then its JSON under `screens/screen-specs/` for bounds, controls, states, accessibility, and source links.
4. Read [`planning/reconstruction-specification.md`](planning/reconstruction-specification.md) for the rebuild contract and acceptance criteria.
5. Use the Mermaid files under `flows/` for navigation and state transitions.

## Evidence conventions

- `CONFIRMED` means directly observed on-device or reported by ADB.
- `STRONG INFERENCE` means supported by more than one observation but not directly exposed as an internal fact.
- `POSSIBLE` means plausible and implementation-relevant, but unverified.
- `UNKNOWN` means evidence is insufficient.
- Every screen ID is stable across the screenshot, XML, metadata, activity summary, catalog, and screen specification.
- A few early filenames reflect the hypothesis at capture time. The authoritative state name is in the screen catalog/specification. In particular, `117_import_settings_file_picker` is a `larix://` text dialog, and `126_preferred_camera_api_dialog` is a disabled preference row, not an opened dialog.

## Privacy and legal posture

- Launcher footage was removed from the retained launch video.
- PID-scoped logs were sanitized for URLs, email addresses, and credential-like values.
- Device-wide activity/window/IME evidence was filtered to the target or the focused system surface.
- Camera-preview pixels are evidence only and must never be reused as product assets.
- The observed app icon, name, third-party names, and licensed SRTLA behavior require authorization, independent replacement, or appropriate licensing in any rebuild.

## Re-running evidence scripts

The PowerShell scripts are compatible with Windows PowerShell 5.1. Pass the explicit device serial to every script. They validate ADB/device selection, create required directories, check exit codes, and refuse accidental evidence overwrite unless deliberately overridden.

```powershell
.\scripts\capture-screen.ps1 -Serial R5CT139QJ5F -ScreenId 146_example
.\scripts\capture-flow.ps1 -Serial R5CT139QJ5F -FlowId flow_003_example
.\scripts\collect-package-info.ps1 -Serial R5CT139QJ5F -PackageName app.irlpro.android -AllowOverwrite
.\scripts\collect-runtime-info.ps1 -Serial R5CT139QJ5F -PackageName app.irlpro.android -AllowOverwrite
```

The two collectors use stable baseline filenames and therefore require the explicit `-AllowOverwrite` switch once those files exist. Omit it for a safety check; the script will stop before running collection.
