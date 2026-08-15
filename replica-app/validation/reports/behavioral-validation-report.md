# Behavioral validation report

Generated: 2026-08-15 (America/New_York)

## Automated gates

| Gate | Result | Evidence |
|---|---:|---|
| Debug assembly | PASS | `app/build/outputs/apk/debug/app-debug.apk` |
| JVM unit tests | PASS — 6/6 | `validation/logs/unit-tests-20260815-000512.log` |
| Device Compose tests | PASS — 4/4 | `validation/logs/ui-tests-20260815-000607.log` |
| Android lint | PASS | `app/build/reports/lint-results-debug.html` |
| Minified/resource-shrunk release | PASS | `app/build/outputs/apk/release/app-release.apk` |
| Release install and cold launch | PASS | foreground `com.irlstreamer.reconstruction/.MainActivity` |
| Release signature verification | PASS | V2 signer SHA-256 `b831eb19f068a8eb688deb65c12af6cf4160b802ee1a0ff9bddc2ef38419bac2` |

## What the tests prove

The six JVM tests prove that every numbered audit ID from 001 through 145 maps to a deterministic runtime state, the four named loading/empty/error aliases resolve, representative dialogs and live tabs are configured, runtime overrides do not mutate persisted settings, and every catalog-backed settings page has content including the 12 audited root destinations.

The four device tests prove:

1. the live console launches and the Start action opens the no-active-connection guard;
2. quick settings opens and switches to the Network tab;
3. Settings → Streamer → Back follows the audited hierarchy;
4. the outgoing-connection form exposes its fields and prevents a silent blank Save.

The final ADB sweep successfully launched and captured all 145 deterministic states without a missing screenshot or runtime-rendering error. A hot-state list caching defect discovered during validation was fixed by keying scroll state to the audit screen ID, then the complete sweep was rerun.

## Flow disposition

| Flow | Local implementation | Direct interaction test | Remaining limitation |
|---|---|---|---|
| F01 Launch and lifecycle | Implemented | Device launch + release cold launch | No foreground capture service |
| F02 Settings hierarchy | Implemented | Device navigation/Back test | None for local hierarchy |
| F03 Streamer/chat/dashboard settings | Implemented fixture | Catalog + state smoke | No remote chat/dashboard backend |
| F04 Bonding configuration | Implemented fixture | Catalog + state smoke | No proprietary transport |
| F05 Outgoing connection creation | Implemented fixture | Device blank-validation test | No endpoint authentication/transmission |
| F06 Video configuration | Implemented fixture | Catalog + state smoke | No physical camera pipeline |
| F07 Audio configuration | Implemented fixture | Catalog + state smoke | No microphone capture |
| F08 Recording and storage | Implemented fixture | Catalog + state smoke | No media recording/write |
| F09 Display guides and meter | Implemented | Catalog + state smoke | Preview pixels intentionally differ |
| F10 Text/picture overlays | Implemented fixture | Catalog + state smoke | No arbitrary HTML/media execution |
| F11 Web overlays | Implemented fixture | Catalog + state smoke | No network/WebView execution |
| F12 Import/export and QR | Safe prompt simulation | Catalog + state smoke | No external scanner/install action |
| F13 Advanced options | Implemented fixture | Catalog + state smoke | Unsupported hardware/system controls remain disabled |
| F14 Help and About | Implemented with disclosures | Catalog + state smoke | Official identity/support text intentionally omitted |
| F15 Live-console operation | Implemented fixture | Start guard + quick-tab device tests | No real broadcast/snapshot/capture |

## Safety/isolation

Every replica device command used explicit serial `emulator-5554`. The attached physical audit phone and original package were not used for replica QA. The manifest requests no Internet, camera, microphone, or storage permission, so the delivered app cannot silently perform the blocked production operations.
