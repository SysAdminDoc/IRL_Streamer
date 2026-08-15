# IRL Pro black-box audit summary

## Outcome

The safely reachable Android surface has been mapped into a reconstruction-ready evidence set: 145 captured visual states, 119 logical screen names, 15 primary flows, 1,145 interactive/adjustable control instances, measured design tokens/components, behavior/persistence rules, an inferred user-facing data model, test matrices, 26 explicit open questions, and a phased architecture/backlog.

This is audit-only. No Android project, Gradle configuration, source code, replacement layout, or production asset was created. Real broadcasting, media creation, credentials, external sharing, destructive settings operations, and private app/system surfaces were not touched.

## Coverage snapshot

| Measure | Result |
|---|---:|
| Visual states / evidence bundles | 145 / 145 complete |
| Logical screen names | 119 |
| Dialogs | 15 |
| Selection dialogs/menus | 37 |
| Bottom sheets | 0 |
| System-intent surfaces | 1 |
| Primary flows | 15 |
| Full / partial / wholly untested primary flows | 3 / 12 / 0 |
| Discrete blocked/untested cases | 24 |
| Interactive/adjustable instances / deduplicated signatures | 1,145 / 378 |
| Unresolved questions | 26 |

Every captured ID has PNG, UI XML, target/focused activity data, privacy-filtered window/IME context, capture metadata, a manifest row, and a structured JSON screen spec. Screen 144 also has a privacy-trimmed launch recording. See `testing/coverage-report.md` for counting rules and limits.

## Most important confirmed findings

1. **The app is a service-backed landscape live-production console.** Launch enters an active camera/microphone preview even with no broadcast connection. A foreground camera/mic service and ongoing notification remain involved across task backgrounding. Evidence: 001, 144–145; app component/runtime reports.
2. **The product has two distinct interface systems.** Configuration uses Android-style dark preferences/dialogs (`#303030`, `#212121`, `#424242`, teal `#80CBC4`); operation uses a full-preview console with translucent circular controls and a black in-place quick panel. Evidence: 002–129, 130–138; design reports.
3. **Configuration breadth is substantial and capability-driven.** Streamer/chat, link bonding, connections, video encoder/camera choices, audio, recording, display guides, two overlay families, import/export, WebView and advanced controls are all observable. The reference device exposes four physical cameras and many FPS/codec/profile choices. Evidence: 006–138.
4. **Core persistence and safe guards behave predictably.** Settings survive force-stop; drafts cancel cleanly; Start with zero active connections cannot broadcast and offers Create Connection; empty required HTML is rejected; unsupported features are disabled. Evidence: 024, 101, 126, 142; lifecycle report.
5. **Measured single-device performance is acceptable but not benchmark-grade.** Cold launch 330 ms, Home resume 119 ms, warm relaunch 174 ms; idle preview ~275 MB PSS; current jank metric 14/695 frames (2.01%). Evidence: lifecycle and measurement files.

## Prioritized product and reconstruction risks

1. **[P0] Protocol, secret, and licensing contracts are unresolved.** Real RTMP/SRT/SRTLA/bonding, credential storage/export, backend/API behavior, and code/mark rights could not be inferred safely. A visually complete clone would still be functionally and legally incomplete. Resolve Q06/Q08/Q20/Q21/Q24/Q26 before transport or release work.
2. **[P1] Essential live controls lack accessible names.** UI Automator reports empty descriptions/`NAF` for Settings, Reload, Quick settings, Snapshot, Flip, Start, and Mute on screen 001. A screen-reader user cannot reliably identify the app's primary actions. Rebuild acceptance must require explicit names, roles, and changing state descriptions.
3. **[P1] Several visible live targets are too small or narrow.** Reload is about 23.8 dp square, several icon controls about 40.2 dp, Start about 64×24.2 dp, and lens pills about 45.9×28.1 dp. The broad XML scan finds 427 potential sub-48 dp instances, though compound rows inflate that count. Preserve the visual scale but wrap targets in 48 dp semantics.
4. **[P1] Runtime success/error behavior is largely unverified.** No stream, reconnect, failover, recording, snapshot, storage write, remote overlay, import, or notification action ran. The 24 blocked cases define the future fixture matrix; do not claim functional parity from forms/defaults alone.
5. **[P2] Runtime legibility varies with the camera image.** Gray telemetry, inactive lens labels, and translucent controls sit directly over unpredictable preview luminance; screenshots show low-contrast states. Add measured scrims/outlines and non-color state cues while keeping the low-chrome composition.
6. **[P2] The quick panel and settings are information-dense.** Later tabs require horizontal reveal and many option lists/sections require deep scrolling; live controls rely heavily on icons and undocumented gestures (double tap/long press). Preserve expert efficiency, but expose gesture alternatives, selected state, and searchable/helpful labels.
7. **[P2] High-impact actions resemble ordinary preferences.** Reset, Clear cookies, debug-detail sharing, Lock Screen, and notification Exit were not activated. Their scope/confirmation/recovery is unknown. A rebuild needs explicit confirmation, disclosure, and safe defaults.

## Highest-risk unknowns and missing states

The release-blocking unknowns are the streaming state machine and interoperability; connection/secret lifecycle; settings import/export schema and transactionality; backend/analytics requirements; and protocol/asset licensing. Missing visual/behavioral states include first run, populated connection and overlay managers, real live/reconnect/failure, recording/snapshot/storage errors, WebView/image loading and offline errors, malformed import, external support/share results, and supported-device USB/Horizon/alternate-camera surfaces.

These limits are exact in `planning/open-questions.md` (Q01–Q26) and `testing/untested-and-blocked-cases.md` (U01–U24).

## Recommended reconstruction order

Resolve legal/security/protocol decisions first; then build the measured shell and accessible component system, typed settings/persistence, connection/layer records and validation, service-backed preview/live console, local-fixture overlay rendering, media recording/encoding, controlled transports, and finally import/external/destructive flows plus device/accessibility/performance hardening. Detailed exit criteria are in `planning/implementation-backlog.md` and the 20-section reconstruction specification.

## Evidence integrity and device restoration

- All ADB interactions used explicit serial `R5CT139QJ5F`.
- No root, decompilation, private storage, traffic interception, authentication bypass, real messages/forms, purchases, clear-data, uninstall/reinstall, or irreversible account change occurred.
- Captured logs are target-PID scoped and sanitized; window/activity/IME evidence is target/focused-surface filtered; the retained video contains no launcher frames.
- Reversible test states were restored: rear 73° camera, microphone active, link weights 100, resolution-matched bitrate on, gain 0 dB, audio meter on, grid/margins off, 16:9 at 5%, Timestamp off, platform icons off, and no connection/web-overlay/test record.
- Camera-preview imagery is evidence only and must not be reused as a shipping asset.

## Handoff

Start with `planning/reconstruction-specification.md`, then use `screens/screen-catalog.csv` and each JSON spec as the state-level source of truth. Implementation remains gated until the operator explicitly says `BEGIN REBUILD`.
