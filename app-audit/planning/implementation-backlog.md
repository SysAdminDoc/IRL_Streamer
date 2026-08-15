# Implementation backlog

This is a sequencing plan only. No replacement project or source code has been created.

## Phase 0 — product, legal, and safety gates

- Resolve Q06/Q08/Q20/Q21/Q24/Q26: secret handling, session state machine, import/export, backend scope, and asset/protocol licensing.
- Define parity target: reference-device pixel fidelity versus generalized Android support.
- Supply original replacement brand assets and decide whether “IRL Pro,” Twitch, Kick, Streamlabs, Toonation, Streamelements, Discord, Larix, and SRTLA marks/copy may appear.
- Establish owner-controlled RTMP/SRT/SRTLA and WebView fixture infrastructure.
- Freeze a privacy-safe test-data policy and destructive-test environment.

Exit: signed-off product/security/legal decisions and no unknown release-blocking contract.

## Phase 1 — shell and measured design system

- Create the landscape app shell, system-inset handling, theme/tokens, typography, app bar, preference rows, dialogs, switches, sliders, form footers, and semantic wrappers.
- Add deterministic screenshot harness at 2316×1080 px / 450 dpi and responsive dp fixtures.
- Implement navigation hierarchy with placeholder destinations, Back behavior, and stable test tags.
- Match settings-root and About/dialog surfaces first because they provide low-risk visual baselines.

Exit: screenshot and accessibility tests pass for shell components; every visible action has a name/role and at least a 48 dp semantic target.

## Phase 2 — typed settings and persistence

- Define versioned singleton configuration schemas and defaults from the evidence tables.
- Build reusable choice/numeric/seek preferences with enabled/disabled dependencies.
- Implement Streamer, Bonding, Video, Audio, Recording, Display, and Advanced screens without media side effects.
- Add transactional DataStore migrations and process-death tests.

Exit: all measured default summaries/options and reversible setting transitions match screens 002–090 and 119–127.

## Phase 3 — connection and overlay records

- Implement Room models/repositories for ordered connections, text/picture layers, and web overlays.
- Implement draft/Cancel/Save semantics, required-field validation, manage/empty/populated states, active selection, ordering, and secret encryption.
- Implement Timestamp seed layer and standby/pause references.
- Keep remote transports and WebViews stubbed behind local fakes.

Exit: empty/default/validation/selected/populated states are covered with sanitized fixtures; no secret appears in logs, exports, screenshots, or saved-state bundles.

## Phase 4 — preview service and live console

- Implement camera capability discovery, physical-lens selection, Surface lifecycle, microphone meter/mute, and foreground-service notification.
- Recreate the console controls, telemetry layout, lens pills, grid/margins, quick-settings tabs, and activity/service rebinding.
- Add idempotent command processing and safe no-connection Start guard before any network transport exists.

Exit: reference-device preview lifecycle, Home/Back/resume, camera flip, mute, overlays, and service notification pass instrumentation tests without streaming.

## Phase 5 — overlay rendering

- Implement deterministic logical-canvas composition for text/picture layers across preview and encoder outputs.
- Add template parser/sanitizer, URL/file image loader, periodic refresh, cache/error states, scale/position/z-order, and standby/pause state hooks.
- Build isolated WebView overlays with explicit security policy, view modes, custom position, process/lifecycle recovery, and local fixture pages.

Exit: local fixtures cover success/loading/timeout/error/reload and preview-only/stream-only parity; malformed content cannot escape the defined policy.

## Phase 6 — media encoding, recording, and snapshots

- Implement AudioRecord/MediaCodec pipeline with capability negotiation, parameter validation, and observable failure states.
- Implement MediaMuxer recording/splitting, SAF/MediaStore destinations, snapshot formats/quality, battery/storage checks, and collision-safe naming.
- Add long-run resource/thermal/backpressure tests and privacy-safe output verification.

Exit: owner-approved fixture media is produced and cleaned up; failures are recoverable and actionable.

## Phase 7 — transports and adaptive networking

- Implement RTMP first against a local/owner-controlled ingest endpoint.
- Add SRT/SRTLA/bonding only after licensing and protocol acceptance criteria are resolved.
- Implement per-link policy/weights, adaptive bitrate interface, reconnect/backoff, background rules, notification actions, duplicate-tap prevention, and diagnostics redaction.

Exit: each advertised protocol passes interoperability, network-loss, background, process/service, and security tests; no real user endpoint is used in CI.

## Phase 8 — import/export, deep links, help, and hardening

- Implement versioned transactional import/export with preview/confirmation, limits, redaction/encryption policy, QR path, and `larix:` validation.
- Add external help/share intents with explicit disclosure and resolvability guards.
- Implement Reset/cookie-clear confirmations and scoped effects.
- Complete TalkBack, large-font, localization, device matrix, offline, rotation/multi-window policy, performance, battery, privacy, and threat-model testing.

Exit: all agreed acceptance criteria in the reconstruction specification pass and every residual difference is documented.

## Prioritization rule

Build evidence-rich, low-risk surfaces first; keep unknown protocols, external integrations, destructive actions, and licensed assets behind disabled feature flags until their contracts are resolved. Do not infer parity from visual similarity alone.
