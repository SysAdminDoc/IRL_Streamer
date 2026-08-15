# IRL Pro observable reconstruction specification

This is a clean-room, evidence-backed product contract for a future implementation. It describes observable behavior only. It does not authorize implementation, identify the original internals, or grant rights to names, artwork, protocols, or third-party integrations.

## 1. Product overview

IRL Pro 3.5.23 is an Android landscape live-production console. On launch it starts a camera/audio preview pipeline, overlays compact runtime telemetry and controls, and exposes configuration for outgoing connections, camera/video/audio encoding, local recording/snapshots, display guides, text/picture layers, WebView layers, network bonding, import/export, and advanced behavior. The captured default has no outgoing connection and therefore blocks broadcast Start.

The reference environment is a Samsung SM-S908U1 on Android 16 at physical 1080×2316, 450 dpi, font scale 1.0, en-US, night mode, and three-button navigation. The app renders 2316×1080 landscape; usable content is approximately x=75–2181 (2106 px / 748.8 dp) with a left 26.67 dp safe inset and right 48 dp navigation inset. Match this canvas first, then adapt with real insets and dp/sp—not bitmap scaling.

All facts use `CONFIRMED`, `STRONG INFERENCE`, `POSSIBLE`, or `UNKNOWN`. Screen IDs refer to `screens/screen-catalog.csv`; every ID has a screenshot, XML, activity/window/IME metadata, and JSON spec.

## 2. Complete screen inventory

The authoritative complete inventory is the 145-row screen catalog and 145 screen-spec JSON files. It resolves to 119 logical screen names because scroll positions and meaningful states share surfaces. The following ranges cover every captured ID without gaps:

| IDs | Surface family | Captured states |
|---|---|---|
| 001 | Live console | Default rear-camera idle preview |
| 002–005 | Settings root | Top, scrolled, bottom, final scroll states |
| 006–015 | Streamer options | Chat/dashboard/alerts/custom page sections, URL and scale dialogs, temporary platform-icons state |
| 016–020 | Bonding | Description/mode/link policy/weights and reversible Wi-Fi-weight state |
| 021–031 | Connections | Empty lists; generic/RTMP/auth forms; target menu; Twitch/Kick quick setup |
| 032–064 | Video | All section scroll states and camera/resolution/FPS/exposure/bitrate/codec/profile/stabilization/adaptive menus |
| 065–075 | Audio | Source/channels/bitrate/sample-rate menus, lower processing, reversible maximum gain |
| 076–082 | Recording | Defaults/lower section, duration, format/quality dialogs, Android folder picker |
| 083–090 | Display | Default settings; preview grid/margins/meter variants; ratio and indent controls |
| 091–106 | Text/picture overlays | Root/lists, Timestamp list/preview/editor/refresh, type/form/validation/manage/selectors |
| 107–115 | Web overlays | Root, full draft form, view/position/custom controls, empty manager |
| 116–118 | Import/export | Settings surface, `larix://` dialog, scanner install prompt |
| 119 | Settings root | Lower category state |
| 120–127 | Advanced | Full scroll range, disabled capability row, Volume keys dialog |
| 128–129 | Help/About | Help actions and About modal |
| 130–138 | Live quick settings | Camera upper/lower, Network upper/lower/stats, Display, Overlays, Audio, Log |
| 139–143 | Live-console interaction states | Mute, camera selections, no-connection Start dialog, reload toast |
| 144–145 | Lifecycle | Settled cold launch and background/hot resume |

There were 15 modal dialogs, 37 selection-dialog/menu states, no bottom sheets, one system-intent surface, and nine quick-panel states. The external Barcode Scanner dependency prompt is app-owned; only the Android DocumentsUI folder picker is a captured system surface. Early filenames 022, 024, 094, 096, 110, 117, and 126 preserve capture-time hypotheses; the catalog/spec names and state descriptions are authoritative.

## 3. Navigation hierarchy

`LaunchActivity` routes to the live console (`StreamerServiceActivity`). The console gear opens `SettingsActivity`. Settings uses a hierarchical Back stack:

- Settings root → Streamer, Bonding, Connections, Video, Audio, Recording, Display, Overlays, Import/Export, Advanced, Help.
- Connections → New connection or Manage connections; New → protocol-dependent fields or Twitch/Kick quick-setup modals.
- Overlays → Text/Picture or Web overlays → list/manage/editor/choice surfaces.
- Help → About modal; other Help actions cross an external/share/destructive boundary and remain untested.
- Console overflow → in-place Camera, Network, Display, Overlays, Audio, and Log tabs. It overlays the preview and is not a new activity.
- Recording Save-to → Android DocumentsUI; Scan QR → external dependency prompt.
- `larix:` is an accepted deep-link/import scheme, but payload semantics are unknown.

Back dismisses a modal, returns from a child preference page to its parent, and backgrounds the app from the live console. Home/background resume rebinds to the running pipeline. See both Mermaid maps for exact observed edges.

## 4. Screen-by-screen requirements

| Surface/IDs | Observable requirements |
|---|---|
| Live console (001, 139–145) | Full-bleed preview; audio meter lower-left; telemetry upper-right; gear upper-left; reload/overflow upper-right; snapshot/flip/mute rail right; FPS, Start pill, and front/back physical-lens pills at bottom. Default rear 73°, unmuted, FPS settles near 30. Mute turns meter gray. Physical lens selection updates preview and selected pill. Reload shows the exact asynchronous toast captured in 143. Start with zero active connections shows the blocking guidance in 142. Cold start initially uses black preview shell, shows H.264/1920×1080 status, then frames arrive. |
| Settings root (002–005,119) | Dark app bar and scrollable full-row categories in the observed order. Back returns to console; revisiting must reflect persisted summaries. |
| Streamer (006–015) | Optional Twitch/Kick usernames, chat toggles, user-count/platform/bot/command/layout switches, custom chatbox/page URLs, dashboard/API-key fields, and scale editors. Preserve observed defaults: platform icons/bots/commands/smaller box/left margin/right side off; user count on; URLs/keys blank; chat 220; alert 280. Save only explicit dialog confirmation. |
| Bonding (016–020) | Explain own-server SRTLA/built-in bonding, expose modes and Cellular/Wi-Fi/Ethernet policies. Each link defaults enabled with weight 100. Slider/value updates must be reflected in live Network quick settings and persist transactionally. |
| Connections (021–031) | Empty list with New/Manage. Generic draft includes Name and URL; Save disabled with URL empty. Recognized RTMP URL reveals help, target-type choice, and optional auth fields. Target choices and Twitch/Kick helper schemas must match evidence. Cancel discards drafts. Populated/edit/delete behavior remains gated by Q03–Q08. |
| Video (032–064) | Capability-driven startup camera, resolution, FPS, orientation, focus, WB, anti-flicker, exposure, encoder bitrate/mode/keyframe/format/profiles, stabilization/noise reduction, adaptive algorithm/frame-rate. Preserve defaults in the behavior spec. Menus must scroll and mark the current selection. Unsupported choices are disabled or omitted based on capability, never assumed from the reference phone. |
| Audio (065–075) | Bluetooth preference, source, channels, bitrate, sample rate, speaker-awake, gain (-40 to +10 dB, default 0), AEC, and noise suppressor. Menus/ranges match captured choices; device provider labels are capability-dependent. |
| Recording (076–082) | Record off; split on/30 minutes; JPEG quality 90; SAF off; destination summary. Record/snapshot formats and quality options match evidence. Save-to launches Android folder picker. Show critical-battery warning. Runtime file behavior is gated by U10–U12. |
| Display (083–090) | Audio meter on; grid/margins off; ratios multi-select with 16:9 selected; indent 5% in range 0–20. Grid and red margin rectangle composite over preview immediately; disabling restores clean preview. |
| Text/Picture overlays (091–106) | Root and ordered layer list, preview visibility and standby defaults on, built-in inactive Timestamp with observed HTML/date format, 1 s refresh, scale 20%, center, z=5. New subtype selection exposes URL/file or HTML forms. Blank Text Save shows exact validation toast. Higher z renders in front. Standby/Pause selectors reference layers. |
| Web overlays (107–115) | Empty list/manage state and new draft with Name, URL, CSS, view mode, position/custom coordinates, z, width, height, scale. Preserve default transparent CSS; Preview+stream; Center; z=1; 1280×720; 100%. Warn no more than 3–4 for performance. Cancel discards. Remote and saved states are gated by U16–U17. |
| Import/export (116–118) | Import text dialog is prefilled `larix://` and has Cancel/OK. Scan QR checks an external Barcode Scanner dependency and prompts to install when missing. Transaction, format, export, and QR success are gated by U18–U19. |
| Advanced (120–127) | Expose captured runtime/network/camera/WebView switches and summaries. Always-on/unsupported rows remain disabled. Volume keys menu has Do nothing (default), Broadcast, Zoom, Flip camera. Destructive/experimental controls require confirmation and are gated by U22–U24. |
| Help/About (128–129) | Help lists Discord, camera-debug sharing, Reset, About, and USB help. About shows product/version 3.5.23 and licensed SRTLA notice. External/share/reset behavior is gated by legal/product decisions. |
| Quick panel (130–138) | Black overlay with horizontally revealable tabs and vertical bodies. Camera mirrors physical/capture controls; Network shows conditioner, target 6 Mbps, adaptive 0 bps idle, links/weights/stats; Display exposes grid/margins/Lock Screen; Overlays master on/Timestamp off; Audio gain 0; Log shows redacted pipeline diagnostics and file-logging-off copy. Preserve preview underneath and panel state while interacting. |

Exact visible strings, control bounds, enabled/selected flags, destinations, and per-state notes are in each JSON spec. Treat those records—not generalized prose—as the acceptance source when a detail conflicts.

## 5. Reusable component list

Implement a measured dark app bar; section header; one-line and summary preference rows; switch/disabled/choice/numeric/seek preferences; scrolling checked-choice dialog; numeric/text editor dialog; equal Cancel/Save footer; empty manager; toast/status surface; full-screen preview surface; audio meter; telemetry block; circular icon control; Start pill; lens pill matrix; grid/safe-margin renderer; text/picture/web layer renderer; and the tabbed quick-settings panel.

Every component needs stable semantic IDs, names/roles/states, disabled semantics, logical traversal, at least a 48 dp semantic target, keyboard/D-pad activation where meaningful, and explicit loading/error slots. Do not copy system toasts, DocumentsUI, permission prompts, or third-party UI as assets.

## 6. Design tokens

Use `design/design-tokens.json` as the machine-readable source. Core observed samples: settings `#303030`, app bar `#212121`, dialog `#424242`, accent `#80CBC4`, primary text `#FFFFFF`, secondary approximately `#C7C7C7`, quick panel `#020202`, active audio `#1F8B4D`, Timestamp `#66FF66`. Preserve raw and normalized values with confidence metadata.

At 450 dpi: app bar ~51.56 dp; standard preference row ~53.69 dp; summary row ~72.53 dp; choice row 48 dp; common dialog width ~486.76 dp; form action height 48 dp. Preference text begins ~72.18 dp from safe content left. Use estimated motion/dimensions only where marked; no fixed shadow/radius claim was confirmed.

Typography appears Android sans/Roboto-like but exact font files are unknown. Use roles in `design/typography.md`; preserve wrapping, numeric units, capitalization, and the `MMM dd, HH:mm:ss`/`en_US` Timestamp template where authorized.

## 7. Data entities

- `AppSettings`: versioned aggregate of singleton Streamer, Bonding, Video, Audio, Recording, Display, and Advanced preferences.
- `LinkPolicy`: link kind, enabled, integer relative weight.
- `OutgoingConnection`: stable ID, optional/display Name, URL, derived protocol, active flag, ordered position, target type, encrypted optional credentials/service fields.
- `TextPictureLayer`: stable ID/name, subtype, URL/file or HTML, active, refresh flag/interval, scale, position, z-order, ordered position.
- `WebOverlay`: stable ID/name, URL, CSS, view mode, position/custom x/y, z-order, width/height, scale, ordered position.
- `LayerSelection`: standby/pause mode to referenced layer IDs.
- `RuntimeSessionState`: nonpersistent camera/audio/encoder/transport/telemetry/overlay state.
- `StorageGrant`: SAF URI and persisted-permission status; actual media remains external.

This is a user-facing model, not an asserted backend/database schema. Requiredness, validation, formatting, relationships, and evidence by field are in `data/entity-field-matrix.csv`.

## 8. State-management requirements

Separate persisted configuration, unsaved form drafts, capability state, and runtime session state. A single serialized service command state machine must prevent duplicate Start/Stop/record commands. UI screens observe state; they do not own camera/encoder lifetime. Cancel/Back drops a draft; Save validates then commits atomically. Runtime quick-setting changes update the matching persisted policy only where observed (link weights and display/audio controls); ephemeral FPS/current bitrate/log lines never persist.

The UI must reconcile after activity recreation or service rebinding, preserve safe selected settings, and never auto-start a broadcast due solely to process recreation. Disabled-state dependencies must match: empty URL disables Save; automatic bitrate matching governs manual bitrate; capability limits govern camera controls; empty managers disable delete; unavailable APIs/hardware remain disabled.

## 9. Persistence requirements

Settings survive navigation, Home/background, and force-stop/relaunch. Unsaved drafts do not. Store singleton settings transactionally, ordered records with stable IDs, encrypted secrets separately, and SAF grants with validity checks. Define versioned migrations and rollback-safe import. Do not persist transient telemetry, raw diagnostics, unredacted URLs with secrets, or an “actively live” state that could trigger unintended transmission.

Scroll-position persistence beyond the same activity session is unknown and not required for first parity. Background inactivity timeout, reboot behavior, and process-death restoration await Q10 and device-matrix tests.

## 10. Permission requirements

Observed requested capabilities include camera, microphone, notifications, network/Bluetooth-related access, foreground service, wake/network behavior, and optional location; camera/microphone/notification were granted and location denied. The foreground service must declare camera/microphone types and comply with current Android start restrictions. Default preview must operate without location. Request permissions just in time with denial/permanent-denial recovery and no loop.

SAF folder access uses a system picker and persisted URI grants only after deliberate user selection. Web-overlay geolocation must be off by default and require both app/runtime permission and per-origin user consent. Exact manifest declarations and exported status are documented in `app/permissions-and-appops.md` and `app/components-and-intents.md`.

## 11. External integrations

Observed or named surfaces include Twitch, Kick, Streamlabs, Toonation, Streamelements, Discord, RTMP, SRT/SRTLA/bonding, Android DocumentsUI, external Barcode Scanner, WebView, Bluetooth audio, USB/UVC camera, `larix:` deep links, and New Relic Android instrumentation in PID-scoped logs. Names/tags do not confirm API contracts, SDK configuration, payloads, endpoints beyond the logged collector host, or reuse rights.

Each integration must sit behind a validated adapter, have missing-app/offline/error behavior, avoid credentials in logs, and use explicit intents. Transport implementations require independent interoperability/security/license review. External links and backends remain UNKNOWN unless supplied by the product owner.

## 12. Error and loading behavior

Confirmed states are: black-to-preview camera initialization with codec/resolution toast; reload-in-progress toast without blocking the console; empty list/manager/stats states; disabled Save for empty connection URL; blank Text Save toast; no-active-connection Start modal; scanner dependency prompt; and disabled unsupported capability rows. Preserve exact confirmed messages listed in `behavior/validation-rules.csv`.

Add non-fabricated, product-approved states for permission denial, camera/mic unavailable, encoder error, connection/reconnect failure, storage full/grant lost, import parse/version failure, image/WebView timeout/TLS/offline failure, and foreground-service restriction. Their copy and retry contracts are not evidenced and must be accepted separately before claiming parity.

## 13. Accessibility requirements

The reference has confirmed semantic debt: 141 `NAF` instances across 21 states, including empty descriptions on core console controls; 427 potential sub-48 dp node instances; and color-heavy selected/muted cues. The rebuild must improve rather than clone these defects.

Give every action an accessible name, role, checked/selected/muted/disabled state, and deterministic focus order. Announce validation and asynchronous reload/session transitions; merge compound preference-row semantics; provide 48 dp hit targets without enlarging glyphs; expose slider value/range/actions; make tabs and choice selection explicit; and provide non-color state cues. Support large text without clipping, contrast-test sampled colors, and test TalkBack/keyboard/switch access before release. Preserve privacy by excluding camera-preview content and secret values from accessibility announcements.

## 14. Recommended Android architecture

Use a Kotlin, coroutine/StateFlow, Compose-first shell with Android View interop for the preview; a camera/microphone foreground service owning Camera2/AudioRecord/MediaCodec; typed DataStore for singleton settings; Room for ordered connection/layer records; Keystore-backed secret encryption; SAF/MediaStore for media; and isolated protocol/WebView adapters. Prefer direct Camera2 unless a capability spike proves CameraX+Interop parity. Use minSdk 28 to match observed support unless product requirements change it.

This is a recommendation, not an observation. Complete module boundaries, state machine, security model, and test architecture are in `recommended-architecture.md`.

## 15. Suggested implementation order

1. Resolve product/legal/security gates and controlled fixture infrastructure.
2. Build the measured shell/design components and screenshot/accessibility harness.
3. Implement typed settings/defaults/persistence and full hierarchy.
4. Add connection/layer data models, drafts, validation, encryption, and populated fixtures.
5. Implement preview service and accessible live console without network streaming.
6. Implement text/picture and isolated WebView composition using local fixtures.
7. Add encoding, recording, snapshots, storage, and long-run resource testing.
8. Add RTMP, then SRT/SRTLA/bonding only after license/interoperability gates.
9. Add import/export/deep links/help, destructive confirmations, and full hardening matrix.

Use the phase exits in `implementation-backlog.md`; unknown/high-side-effect features stay behind disabled flags.

## 16. Testable acceptance criteria

- At the reference viewport, golden screenshots for every approved state match measured structure, color, spacing, text hierarchy, selected/disabled state, and system insets within agreed tolerances; no camera evidence pixels are used as assets.
- The implemented catalog accounts for every approved ID 001–145 or records an accepted divergence; core navigation edges and Back/Home/resume behavior match the maps.
- Default values/options/ranges/summaries match the behavior specification and screen specs; capability-dependent choices derive from the actual device.
- All 15 primary flows have automated safe-path coverage; no-connection Start and blank Text validation reproduce confirmed guards; Cancel never commits a draft.
- Settings persist after navigation/background/force-stop; transient session state and unsaved drafts do not; process recreation cannot start a broadcast.
- Camera/microphone service, notification, activity rebinding, mute, lens selection, grid/margins, quick tabs, and idle pipeline work under instrumentation without a network destination.
- Media and transport phases pass only against sanitized local/owner-controlled fixtures, with no production credentials/endpoints in code or CI.
- Secrets are Keystore-backed and absent from logs, analytics, accessibility text, exports unless policy explicitly encrypts them, saved state, screenshots, and crash reports.
- Every action has name/role/state; semantic targets are at least 48 dp; TalkBack order, large fonts, contrast, slider actions, selected state, and error/status announcements pass tests.
- Permission denial, missing external app, lost storage grant, offline/timeout, camera/encoder failure, and reconnect have product-approved recovery paths before their feature is marked complete.
- Import is size/schema/version validated and transactional; malformed payloads cannot partially mutate settings; Reset/cookie clear/Exit have explicit scope and confirmation.
- Release documentation distinguishes verified parity from unresolved or licensed behavior and retains evidence traceability.

## 17. Evidence references

- `screens/screen-catalog.csv` and `screens/screen-specs/*.json`: authoritative screen/state/control records.
- `evidence/evidence-manifest.csv`: one-row cross-reference from every screen ID to artifacts.
- `evidence/screenshots/`, `ui-xml/`, `activity/`: visual, semantic, and target/focused runtime evidence.
- `flows/flow-catalog.csv`, `navigation-map.mmd`, `state-transition-map.mmd`: coverage and transitions.
- `design/`: raw colors, tokens, measurements, typography, reusable components, asset risks.
- `behavior/`: default values, interactions, validation, lifecycle, errors/empty/loading.
- `data/`: inferred entities and per-field evidence.
- `app/`, `device/`: identity, components, permissions/app-ops, hardware/display/insets.
- `evidence/measurements/` and `evidence/logs/`: target-scoped performance/service/log evidence.
- `evidence/recordings/flow_002_cold_launch_actual.mp4` plus JSON: privacy-trimmed launch sequence.

All evidence paths are relative to `app-audit/`. Runtime metrics are single-device samples. UI Automator gaps on rendering surfaces are documented rather than guessed.

## 18. Known unknowns

There are exactly 26 unresolved questions, Q01–Q26 in `open-questions.md`. Highest-risk unknowns are credentials/secret export, real Start/Stop/reconnect behavior, import transaction/version rules, backend/analytics requirements, protocol/library interoperability, and asset/licensing rights. Other unknowns include first run, populated lists, device capability variants, media files, template/WebView failures, external intent destinations, accessibility/localization/device matrices, and destructive-action scope.

Do not label these implemented or compatible based on visible copy. Resolve them with owner requirements or isolated authorized fixtures.

## 19. Features that could not be safely tested

Exactly 24 cases U01–U24 are enumerated in `testing/untested-and-blocked-cases.md`: first-run/account states; persisted connections; real RTMP/SRT/SRTLA/bonding and failover; codec receiver matrix; Bluetooth/acoustic processing; recording/snapshot/SAF writes; remote/file/unsafe overlay content; standby/pause triggers; WebView persistence/security/failures; import/export/QR; external Help/share; reset/cookie clear; unavailable camera/USB/Horizon capabilities; and lock-screen/notification/torch/unknown live gestures.

The boundary protects user configuration, credentials, media, signed-in external apps, privacy, device accessibility/network state, and remote systems. It is intentional—not missing effort.

## 20. Assets requiring original replacements or authorization

| Asset/category | Evidence | Reconstruction rule |
|---|---|---|
| “IRL Pro” name and app icon | launcher/package/About | Obtain authorization or create a distinct product identity/icon |
| Third-party names/logos: Twitch, Kick, Streamlabs, Toonation, Streamelements, Discord, Larix | settings/help/import copy | Confirm trademark/API terms; use approved marks only |
| SRT/SRTLA/bonding implementation and notice | Bonding/About | Perform code/protocol/license review; do not copy proprietary behavior/code |
| Live-console and settings icons | screenshots/XML | Use licensed platform/open iconography or create originals; do not extract APK assets |
| Camera-preview imagery and recorded device pixels | screenshot/video evidence | Evidence only; never ship or use as training/marketing/product assets |
| Timestamp template/copy and all UI strings | captured UI | Product/legal review; rewrite where branding/copyright requires while preserving behavior |
| Android system UI (toasts, DocumentsUI, install/permission prompts) | screens 082,101,118,143 | Invoke system components or native semantics; never raster-copy OS chrome |
| Typeface | visual estimate only | Use a properly licensed Android system font; exact original file is UNKNOWN |

The asset inventory CSV assigns risk and replacement guidance per observed category. No proprietary APK asset was extracted.
