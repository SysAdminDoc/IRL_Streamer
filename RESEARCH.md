# Research — IRL Streamer
Date: 2026-08-15 — replaces all prior research.

## Executive Summary

IRL Streamer is a clean-room Kotlin/Compose reconstruction of the *observable interface* of IRL Pro. It reproduces 145 audited states, but every transport, capture and integration behind that interface is a deterministic local simulation — the app requests no camera, microphone or network permission. The reconstruction is therefore visually substantial and functionally hollow, which is exactly the boundary the audit drew.

The single most useful finding of this pass is that the audit already tells us what the original is built on. Every class in the audited package is `com.wmspanel.streamer.*`, the deep-link scheme is `larix:`, and the About screen states "Includes licensed SRTLA code" (`app-audit/app/application-identity.md`, `app-audit/app/components-and-intents.md`). `com.wmspanel` is Softvelum, whose own app is `com.wmspanel.larix_broadcaster`. **IRL Pro runs on Softvelum's Larix mobile broadcaster code, with BELABOX SRTLA bonding added.** The Softvelum code is verified from the class prefix and deep-link scheme; that it arrives via Softvelum's commercial Larix Mobile Broadcaster SDK licence (rather than some other arrangement) is a strong inference from the audited "Built by WilliamH" authorship on a third-party package. Either way it resolves the audit's P0 unknown Q06/Q26 (engine provenance) from evidence rather than guesswork, and it means the build-out question is not "how do we invent this" but "which of three known paths do we take".

Independent corroboration: the `go-irl` SRTLA server README lists "IRL Pro (Android)" alongside Moblin and BELABOX as a compatible client, so the original speaks standard, interoperable SRTLA — not a proprietary variant.

Top opportunities in priority order:

1. **Decide the transport licence posture before writing transport code.** BELABOX `srtla` is AGPL-3.0. Linking it into this app makes the whole app AGPL. Moblin's SRTLA implementation is MIT. This is a fork in the road, not a detail (see Security/Licensing).
2. **Adopt StreamPack (Apache-2.0) as the capture/encode/RTMP/SRT engine.** It is the only actively-maintained Android library that covers Camera2 + MediaCodec + RTMP/RTMPS/SRT under a permissive licence, and its architecture (sources → processing → endpoints) maps cleanly onto the audited settings tree.
3. **Treat SRTLA bonding as a distinct, later milestone.** No open-source SRTLA client for Android surfaced in this pass — the references are iOS (Moblin) or Linux C (BELABOX). Treat that as the largest apparent gap in the ecosystem and the app's headline audited feature, but re-confirm before committing to build it.
4. **Ship the relay/receiver story with the client.** Reddit shows users do not understand that bonding requires a server-side SRTLA→RTMP relay. Shipping a client without that guidance reproduces a known support burden.
5. **Real chat ingest (Twitch first).** Twitch has a first-party, documented, stable surface; Kick does not.
6. **Replace the simulated network telemetry with real per-link statistics**, which is the one live-console element that is both audited and achievable without transport work.
7. **Adaptive bitrate (ABR)** — table stakes across every competitor, and the mechanism the audited "Bitrate matches resolution" and adaptive-mode settings imply.
8. **Local recording** — the audited Recording tree is fully specified and needs no network at all, making it the cheapest real feature in the app.

## Product Map

**Core workflows**
- Land in a landscape live console with an active preview; start/stop a broadcast.
- Configure a connection (RTMP/SRT/SRTLA target, credentials, target type) and select it.
- Configure video (resolution, FPS, codec, bitrate mode), audio (source, channels, bitrate, gain), and recording.
- Overlay management: text/picture layers, timestamp, and transparent web overlays.
- In-broadcast quick panel: camera, network, display, overlays, audio, log — without leaving the preview.

**User personas**
- Mobile IRL streamer on cellular, outdoors, one-handed, needing glanceable state and no dialogs mid-broadcast.
- Streamer with a bonded multi-modem rig who needs per-link visibility and graceful degradation.
- Operator configuring the device once at a desk, then never touching settings while live.

**Platforms and distribution**
- Android 9+ (minSdk 28), compileSdk/targetSdk 36, forced landscape, single activity, Compose.
- Distribution is unresolved: the app ships unsigned-by-any-production-identity with a repo-owned self-signed key. No update channel exists.

**Key integrations and data flows (all currently simulated)**
- Egress: RTMP/RTMPS → platform ingest; SRT → relay; SRTLA → bonding receiver → SRT → RTMP → platform.
- Chat ingest: Twitch, Kick, and a custom chatbox URL (audited screens 006-011).
- Alerts/dashboards: Streamlabs API key, StreamElements dashboard with manual login, Toonation API key (audited screens 008-009).
- Settings import/export via a `larix://` deep link and QR scan (audited screens 116-118).

## Competitive Landscape

**Softvelum Larix Broadcaster / Larix Mobile Broadcaster SDK** (commercial) — the engine the audited original actually licenses. Covers SRT, RTMP, RTSP, NDI, WebRTC, RIST, plus multi-output, HEVC, ABR and UVC camera support; the consumer app monetises at $9.99/month for Premium. *Learn:* the settings taxonomy this reconstruction already mirrors is Larix's, which is why the audited tree is so deep — it is an SDK surface, not a hand-designed product. *Avoid:* assuming it is a drop-in for this project; it is a paid commercial licence and adopting it would make the reconstruction a re-skin of the very product it was cleanly reconstructed from.

**StreamPack** (Apache-2.0, v3.2.0) — modular Android live-streaming library: Camera2/microphone/screen sources, MediaCodec encoding (H.264/HEVC/VP9/AV1, AAC/Opus), RTMP/RTMPS/SRT endpoints, file recording (TS/FLV/MP4/WebM), simultaneous record-and-stream, multiple independent outputs, orientation handling, and camera controls (focus/exposure/WB/zoom/flash). *Learn:* its source→processing→endpoint separation is the right internal boundary for this app and matches the audited settings split almost one-to-one. *Avoid:* nothing material; this is the recommended engine.

**RootEncoder** (Apache-2.0, v2.8.0, minSdk 16) — RTMP/RTSP/SRT/UDP plus WHIP beta, Camera1 and Camera2, real-time OpenGL filters, background streaming service, wide codec coverage including AV1/VP8/VP9/G711, runtime bitrate adjustment. *Learn:* the OpenGL filter pipeline is the cleanest OSS answer to the audited overlay/layer system, and its background-service pattern matches the audited foreground service. *Avoid:* depending on a single-maintainer project for the whole stack without a vendoring plan — the README actively solicits sponsorship for device testing.

**HaishinKit.kt** — Android camera/mic streaming with multi-camera, multi-streaming and video mixing. *Learn:* its video-mixing layer is a useful reference for watermark/timestamp overlays. *Avoid:* selecting it as the primary engine — the Android/Kotlin port is RTMP-only, while the Swift version carries SRT. SRT is non-negotiable for this app.

**Moblin** (MIT, iOS/Swift) — the closest functional analogue to IRL Pro that is open source: RTMP/RTMPS/SRT/SRTLA/RIST/WHIP, bonding across cellular + WiFi + multiple Ethernet, Twitch/Kick/YouTube chat with moderation, alert/image/text/browser/map/QR/scoreboard widgets, OBS WebSocket remote control, RTMP multi-streaming, and a watchOS companion. *Learn:* this is the feature ceiling to aim at, and — critically — it is **MIT**, so its SRTLA logic can be studied and ported without copyleft contamination. *Avoid:* trying to port the app; it is Swift/iOS with no Android version. Port the protocol logic, not the product.

**BELABOX** (`srtla`, AGPL-3.0) — the reference SRTLA sender/receiver in C, plus the BELABOX hardware/cloud ecosystem ($10/month cloud SRTLA relay). *Learn:* it defines the wire protocol every relay expects. *Avoid:* linking the AGPL code into this app, and depending on the in-repo receiver — the project itself states `srtla_rec` is "unsupported, no longer under development and not suitable for production deployment", and it is Linux-only.

**go-irl** (AGPL-3.0, Go) — a maintained cross-platform SRTLA server for Windows/macOS/Linux with native binaries, packet-loss-driven scene switching, and explicit compatibility with IRL Pro, Moblin and BELABOX. *Learn:* it fills the exact hole BELABOX's own receiver leaves, and is the right thing to point users at for self-hosting. *Avoid:* bundling it; AGPL applies to network use, and it is a server, not app code.

**MediaMTX** (MIT, v1.19.2) — zero-dependency media server: SRT/RTMP/RTSP/WebRTC/HLS/MoQ publish and read, proxying and recording. *Learn:* the permissively-licensed, self-hostable ingest endpoint to recommend for testing and for users who want their own relay without AGPL obligations. *Avoid:* expecting it to do SRTLA bonding — it terminates SRT, not SRTLA.

**NOALBS** — automatic OBS scene switching driven by ingest bitrate, the de-facto community answer to "my stream degraded and my viewers saw a frozen frame". *Learn:* the low-bitrate/offline state machine is a UX pattern this app should expose on-device rather than deferring entirely to a PC. *Avoid:* re-implementing its OBS integration; interoperate instead.

**Cloud IRL services — IRLToolkit ($129-$179/month cloud OBS), BELABOX Cloud ($10/month SRTLA relay), IRLServer/IRLHosting/IRLKIT/IRL Uplink/StreamableRun ($13+)** — the commercial layer that exists precisely because the client-side story is incomplete. *Learn:* what they charge for — SRTLA relay, disconnect protection with BRB scenes, cloud OBS — is the list of things users cannot do from the phone alone, and therefore the highest-value client-side gaps. *Avoid:* building a cloud service; the differentiator here is the on-device console.

## Security, Privacy, and Reliability

**Licensing is the dominant risk, ahead of any code defect.**

- `BELABOX/srtla` is **AGPL-3.0** (verified at the repository). Statically or dynamically linking it into this Android app makes the entire app AGPL-3.0, including the Compose UI. Distributing it on Play under any other terms would be a licence violation. The audited original sidesteps this with a "licensed SRTLA code" arrangement it declares on screen 129 — a route that is not available to this project by default.
- **Moblin is MIT.** An MIT-licensed SRTLA implementation is a viable clean basis for a Kotlin/native port with attribution and no copyleft reach. This is the recommended path and it should be an explicit, recorded decision, not an accident.
- **libsrt (Haivision) is MPL-2.0** — file-level copyleft. Safe to link from a permissive or proprietary app provided modifications to MPL files are published. StreamPack already depends on it, so this obligation arrives regardless of the bonding decision.
- `docs/known-deviations.md` D011 already records that SRTLA licensing is unresolved. This research converts that from "unknown" to "known and constrained"; the deviation text should be updated to match.

**Current guardrails that are missing because nothing is real yet**

- No permission request flow exists for `CAMERA` or `RECORD_AUDIO`. The audited original declares both plus location and network-state permissions (`app-audit/app/permissions-and-appops.md`). Any real capture work must land a rationale + denial + permanently-denied path, and the audit has no evidence for those screens (`testing/untested-and-blocked-cases.md`).
- No credential storage. The audited app accepts stream keys, a Streamlabs API key and a Toonation API key. Storing those in plain `SharedPreferences` would be the obvious wrong move; `EncryptedSharedPreferences`/Keystore is the floor, and export/QR-import (audited screens 116-118) must never round-trip secrets in cleartext.
- No network-failure state machine. `ReplicaSettingsRepository` and `MainViewModel` model UI state only; there is no reconnect, backoff, or degraded-quality path.

**Reliability gaps with direct audit evidence**

- The audited foreground service (`com.wmspanel.streamer.StreamerService`, notification ID 101, channel `...channel.foreground_service`, actions Start/Exit) is not reproduced — deviation D012. Android 14+ requires a declared `camera`/`microphone` foreground-service type; getting this wrong is a crash-on-start, not a warning.
- Thermal and battery behaviour is unmeasured. The audit recorded ~275 MB PSS idle preview and 2.01% jank, but a real encode pipeline changes both, and sustained outdoor encoding is the most-reported IRL failure mode.

## Architecture Assessment

The reconstruction's boundaries are currently drawn for *rendering audited states*, not for running a pipeline. Three changes prepare it without disturbing the validated UI:

- **Introduce a transport abstraction now, implement it later.** Today `MainViewModel.kt` mutates `RuntimeUiState` directly and `DebugStateCatalog.kt` fabricates every value. A `BroadcastEngine` interface (start/stop/state/statistics) with the current behaviour as `SimulatedBroadcastEngine` lets a StreamPack-backed implementation land behind it without touching Compose. This also preserves the 145-state debug harness, which is the project's most valuable asset and must keep working.
- **Separate settings *schema* from settings *storage*.** `SettingsCatalog.kt` (343 lines) currently encodes labels, structure, defaults and actions together. Real transport needs typed, validated, persisted values; DataStore-backed typed settings behind the existing catalog keeps the audited tree intact while making values usable by an engine.
- **The overlay system needs a compositor, not Compose.** Audited text/picture/timestamp/web overlays must be burned into the encoded frame, not merely drawn on screen. That is a GPU/OpenGL concern (RootEncoder's filter pipeline or StreamPack's processing stage), and it is architecturally distinct from the current Compose-only rendering.

**Test and documentation gaps**

- `AuditMetricsTest.kt` locks layout tokens to evidence; there is no equivalent guard for *behaviour*. Any engine work needs fake-transport unit tests, since real streaming cannot run in CI.
- The three-gate validation harness (geometry/visual/behaviour) has no notion of a running pipeline. Engine work will need its own fixtures rather than screenshot comparison.
- `docs/architecture.md` describes the simulation boundary; it should gain the transport-abstraction seam once introduced.

## Rejected Ideas

- **License the Softvelum Larix SDK** — would make the project a re-skin of the audited original and re-introduce exactly the provenance question the clean-room process was run to avoid. (Source: softvelum.com/larix.)
- **Link BELABOX `srtla` directly** — AGPL-3.0 forces the whole app copyleft; incompatible with any Play distribution the operator has not explicitly chosen. (Source: BELABOX/srtla LICENSE.)
- **Adopt HaishinKit.kt as the primary engine** — Android port is RTMP-only; SRT is required. (Source: HaishinKit.kt README.)
- **Port Moblin wholesale** — Swift/iOS with no Android version; only the protocol logic transfers. (Source: eerimoq/moblin README.)
- **Plugin ecosystem and multi-user/collaboration features** — consciously excluded: this is a single-operator device console reconstructed from a fixed audit, and neither appears anywhere in the 145 audited states.
- **Build a cloud relay service** — a crowded commercial field ($10-$179/month) and orthogonal to an on-device console. Point users at go-irl/MediaMTX self-hosting instead. (Source: irltoolkit.com, belabox.net, streamable.run comparison.)
- **Ship `srtla_rec` as the recommended receiver** — the upstream project declares it unsupported and unsuitable for production. (Source: BELABOX/srtla README.)
- **WebRTC/WHIP as the first transport** — RootEncoder marks WHIP beta and no audited screen references it; RTMP and SRT carry all audited evidence.

## Sources

Engine and protocol libraries
- https://github.com/ThibaultBee/StreamPack
- https://github.com/ThibaultBee/StreamPack-boilerplate
- https://github.com/pedroSG94/RootEncoder
- https://github.com/pedroSG94/RootEncoder/wiki
- https://github.com/HaishinKit/HaishinKit.kt
- https://github.com/Haivision/srt
- https://github.com/Haivision/srt/blob/master/docs/build/build-options.md

Bonding and IRL clients/servers
- https://github.com/BELABOX/srtla
- https://github.com/BELABOX/srtla/blob/main/LICENSE
- https://github.com/eerimoq/moblin
- https://github.com/e04/go-irl
- https://github.com/NOALBS/nginx-obs-automatic-low-bitrate-switching
- https://github.com/bluenviron/mediamtx
- https://belabox.net/

Commercial landscape
- https://softvelum.com/larix/
- https://softvelum.com/larix/premium/
- https://play.google.com/store/apps/details?id=com.wmspanel.larix_broadcaster
- https://irltoolkit.com/
- https://shop.belabox.net/product/belabox-bee
- https://streamable.run/blog/irltoolkit-irlserver-belabox-streamablerun-comparison

Chat/platform integration
- https://github.com/twitch4j
- https://github.com/Bukk94/KickLib
- https://pkg.go.dev/github.com/johanvandegriff/kick-chat-wrapper

Community signal
- https://www.reddit.com/r/Twitch/comments/1vhoy3c/_/p27c5vw
- https://www.reddit.com/r/Twitch/comments/1s9sxbz/home_obs_setup_running_reliably_while_away_for/
- https://www.reddit.com/r/Twitch/comments/1njh2ot/irl_upgrade_advice_needed/
- https://www.reddit.com/r/Twitch/comments/1rc39fc/belabox_for_irl_question/

Project evidence
- `app-audit/app/application-identity.md`
- `app-audit/app/package-inventory.md`
- `app-audit/app/components-and-intents.md`
- `app-audit/app/permissions-and-appops.md`

## Open Questions

1. **What licence will this project ship under?** Everything downstream of bonding depends on it. AGPL-3.0 makes BELABOX `srtla` usable directly; anything else forces the MIT-derived port. This is an operator decision, not a technical one.
2. **Is real broadcasting in scope at all, or is the deliverable a faithful interface?** The audit authorised reconstruction of *observable behaviour*; transport was explicitly out of scope and remains blocked (D005/D012). Roadmap items below are written so the answer can be "no" without wasting the earlier work.
3. **Is there an authorised relay to test against?** Without a reachable SRTLA receiver, bonding cannot be validated end-to-end on this machine.
4. **Does the operator intend Play distribution?** That decides foreground-service type declarations, the data-safety form, and whether AGPL is even an option.

Confidence labels: library licences and capabilities above are **Verified** against primary repository sources on 2026-08-15. That the original contains Softvelum code is **Verified** (`com.wmspanel.*` classes, `larix:` scheme, on-screen SRTLA notice, corroborated by go-irl's compatibility list); the specific commercial-SDK licensing arrangement is a **Strong inference**. The absence of an Android SRTLA client is **Needs live validation** — it is an unsuccessful search, not a proven negative. Reddit findings are **Likely** sentiment signal only; the Arctic Shift backend rate-limited several queries during this pass, so community coverage is partial and should be re-run before being treated as exhaustive.
