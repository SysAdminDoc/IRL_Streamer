# ROADMAP — IRL Streamer

Single task tracker. Incomplete work only.

Item IDs use the `IS-nn` scheme. Continue numbering from the highest existing ID.

## Research-Driven Additions

Added 2026-08-15 from `RESEARCH.md`. Every item traces to a source recorded there.

### P0

- [ ] P0 — IS-02 Introduce a `BroadcastEngine` abstraction with the current simulation behind it
  Why: `MainViewModel` mutates UI state directly and `DebugStateCatalog` fabricates every value, so no real pipeline can be added without disturbing the 145-state debug harness that all validation depends on.
  Evidence: RESEARCH.md "Architecture Assessment"; `app/src/main/java/com/irlstreamer/reconstruction/MainViewModel.kt`, `.../debug/DebugStateCatalog.kt`
  Touches: new `.../engine/BroadcastEngine.kt`, `.../engine/SimulatedBroadcastEngine.kt`, `MainViewModel.kt`
  Acceptance: all 145 debug states render identically (geometry + visual gates unchanged) with the simulation reached only through the interface; unit tests cover start/stop/state transitions against a fake.
  Complexity: M

- [ ] P0 — IS-03 Record the confirmed engine provenance of the audited original
  Why: the audit's P0 unknown Q06/Q26 is now answered from evidence — the original runs Softvelum Larix broadcaster code with BELABOX SRTLA. Leaving it documented as "unknown" makes the next reader re-research it.
  Evidence: `app-audit/app/application-identity.md` (`com.wmspanel.streamer.*`, `larix:` scheme, "Includes licensed SRTLA code"); go-irl lists IRL Pro as a compatible SRTLA client.
  Touches: `replica-app/docs/known-deviations.md`, `replica-app/README.md`, `CLAUDE.md` `## Learned`
  Acceptance: provenance is stated with its evidence, and the reconstruction's non-affiliation is restated alongside it.
  Complexity: S

### P1

- [ ] P1 — IS-04 Adopt StreamPack as the capture/encode engine behind `BroadcastEngine`
  Why: it is the only actively-maintained Android library covering Camera2 + MediaCodec + RTMP/RTMPS/SRT under a permissive licence (Apache-2.0), and its source→processing→endpoint model maps onto the audited settings split.
  Evidence: StreamPack README (v3.2.0, Apache-2.0) — RESEARCH.md "Competitive Landscape".
  Depends on: IS-02
  Touches: `app/build.gradle.kts`, new `.../engine/StreamPackBroadcastEngine.kt`
  Acceptance: a real camera preview renders in the live console and an RTMP publish to a local MediaMTX instance succeeds; the simulation remains selectable for the debug-state harness.
  Complexity: L

- [ ] P1 — IS-05 Runtime permission flow for camera and microphone
  Why: no permission request exists today, and the audited original declares `CAMERA`, `RECORD_AUDIO`, location and network-state permissions. Real capture cannot start without this, and the audit has no evidence for the denial screens.
  Evidence: `app-audit/app/permissions-and-appops.md`; `testing/untested-and-blocked-cases.md`
  Touches: `MainActivity.kt`, new permission composables, `AndroidManifest.xml`
  Acceptance: grant, deny, and permanently-denied paths each reach a defined state; denial never crashes or blanks the console; the net-new screens are documented as additions beyond the audit.
  Complexity: M

- [ ] P1 — IS-06 Foreground service with correct Android 14+ service types
  Why: the audited original runs a camera/mic foreground service (notification ID 101, channel `...channel.foreground_service`, actions Start/Exit). On modern Android an undeclared or mistyped service type is a crash on start, not a warning.
  Evidence: `app-audit/app/package-inventory.md`; deviation D012
  Depends on: IS-05
  Touches: `AndroidManifest.xml`, new `.../service/BroadcastService.kt`
  Acceptance: broadcast survives backgrounding; ongoing notification matches the audited shape; verified on the API 36 emulator and on a physical device.
  Complexity: M

- [ ] P1 — IS-07 Encrypted storage for stream keys and API tokens
  Why: the audited settings accept stream keys plus Streamlabs and Toonation API keys, and the export/QR-import flow round-trips settings. Plain `SharedPreferences` for these would be a real vulnerability.
  Evidence: audited screens 025-031, 008-009, 116-118; RESEARCH.md "Security, Privacy, and Reliability"
  Touches: `.../data/ReplicaSettingsRepository.kt`, new secret storage
  Acceptance: secrets are stored via Keystore-backed encryption; export omits or explicitly re-encrypts them; a test asserts no secret appears in cleartext prefs or exported payloads.
  Complexity: M

- [ ] P1 — IS-08 Local recording to file
  Why: the audited Recording tree is fully specified and needs no network, making it the cheapest genuinely-real feature in the app.
  Evidence: audited screens 076-082; StreamPack supports simultaneous record-and-stream to TS/FLV/MP4/WebM.
  Depends on: IS-04
  Touches: `.../engine/`, recording settings page, storage-access handling
  Acceptance: a recording is produced at the configured resolution/bitrate, split into sections per the audited setting, and plays back in a standard player.
  Complexity: M

- [ ] P1 — IS-09 Real per-link network telemetry in the live console and quick panel
  Why: the console's network readouts are fixtures pinned to audited values. They are the one live element that can become real without any transport work, and they are what a bonded-rig user watches continuously.
  Evidence: audited screens 132-134; deviation D009
  Touches: `.../ui/live/LiveConsoleScreen.kt`, `.../ui/live/QuickSettingsPanel.kt`, new connectivity source
  Acceptance: values derive from `ConnectivityManager`/`NetworkCapabilities` per active link; the D009 fixture note is narrowed to only what remains simulated.
  Complexity: M

- [ ] P1 — IS-10 Behavioural test fixtures for the engine seam
  Why: the three validation gates all rest on screenshots and hierarchy dumps; none can observe a running pipeline, and real streaming cannot run in CI.
  Evidence: RESEARCH.md "Architecture Assessment"; `app/src/test/.../AuditMetricsTest.kt` guards layout only.
  Depends on: IS-02
  Touches: `app/src/test/`, new fake transport
  Acceptance: start/stop, reconnect, degraded and error transitions are asserted against a fake engine with no device or network.
  Complexity: M

### P2

- [ ] P2 — IS-11 Twitch chat ingest
  Why: the audited chat surface is presently empty. Twitch is the only audited platform with a first-party, documented, stable API, so it is the correct first integration.
  Evidence: audited screens 006-011; Twitch4J covers Chat/Helix/EventSub — RESEARCH.md "Sources".
  Touches: new `.../data/chat/`, streamer settings pages
  Acceptance: authenticated connection renders live messages in the audited chat layout, honouring the audited bot/command/platform-icon toggles; failures surface as the audited error treatment rather than a blank panel.
  Complexity: L

- [ ] P2 — IS-12 Adaptive bitrate (ABR)
  Why: table stakes across every competitor (Larix Premium, StreamPack, RootEncoder, Moblin), and the audited "Bitrate matches resolution" and adaptive-mode settings already imply it.
  Evidence: audited screens 053-056, 064; RESEARCH.md "Competitive Landscape"
  Depends on: IS-04
  Touches: `.../engine/`, video settings page
  Acceptance: encoder bitrate responds to congestion within the audited mode options; behaviour is observable in the log panel.
  Complexity: M

- [ ] P2 — IS-13 Overlay compositor that burns layers into the encoded frame
  Why: audited text/picture/timestamp/web overlays must reach viewers, not merely the local preview. This is a GPU concern Compose cannot serve.
  Evidence: audited screens 091-115; RootEncoder's OpenGL filter pipeline and StreamPack's processing stage — RESEARCH.md "Architecture Assessment".
  Depends on: IS-04
  Touches: new `.../engine/overlay/`, existing overlay settings pages
  Acceptance: a recorded file contains the configured overlays at the audited positions and scales.
  Complexity: L

- [ ] P2 — IS-14 On-device low-bitrate/degraded state, interoperable with NOALBS conventions
  Why: viewers seeing a frozen frame is the most-reported IRL failure, and the community answer (NOALBS) lives on a PC. Surfacing the state on-device is a genuine client-side gap the cloud services currently monetise.
  Evidence: NOALBS project; Reddit r/Twitch threads on keeping a home OBS reliable — RESEARCH.md "Sources".
  Depends on: IS-04
  Touches: `.../engine/`, live console status treatment
  Acceptance: sustained low bitrate raises a defined degraded state with a visible indicator and a recovery transition; thresholds are configurable.
  Complexity: M

- [ ] P2 — IS-15 Ship relay/receiver guidance with the app
  Why: Reddit shows users do not know bonding requires a server-side SRTLA→RTMP relay, so shipping without it reproduces a known support burden.
  Evidence: r/Twitch comment thread on SRTLA→RTMP conversion; go-irl and MediaMTX as self-host options — RESEARCH.md "Sources".
  Touches: `replica-app/README.md`, new `docs/relay-setup.md`, connection form help text
  Acceptance: documentation explains the SRTLA→SRT→RTMP chain and names at least one self-hosted (go-irl/MediaMTX) and one hosted option, without endorsing a paid service in-app.
  Complexity: S

- [ ] P2 — IS-16 Accessibility pass over net-new surfaces
  Why: D010 records that the reconstruction deliberately improved on the original's unlabeled, sub-48 dp controls. Permission screens, chat and any engine error states are new surfaces with no audit evidence and no accessibility guarantee.
  Evidence: `app-audit/audit-summary.md` P1 accessibility findings; deviation D010
  Depends on: IS-05, IS-11
  Touches: new permission/chat/error composables
  Acceptance: every net-new interactive element has a content description, role and state description, and a 48 dp semantic target; verified with Accessibility Scanner or an equivalent automated check.
  Complexity: S

### P3

- [ ] P3 — IS-17 SRTLA bonding client, ported from BELABOX `srtla_send` via the NDK
  Why: the headline audited capability, and no open-source Android SRTLA client surfaced. Build on the reference implementation rather than reimplementing the protocol: `srtla_send` is C, and Android is Linux, so the congestion/window/scheduling core compiles largely as-is.
  Evidence: BELABOX/srtla README — `srtla_send [local_port] [receiver_ip] [receiver_port] [ips_file]`, "keeps track of the number of packets in flight ... together with a dynamic window size that tracks the capacity of each link", traffic "balanced through each link proportionally to its capacity". Moblin (iOS) remains a behavioural cross-check.
  Depends on: IS-04
  Touches: new `app/src/main/cpp/` (srtla core + JNI), `AndroidManifest.xml` (`CHANGE_NETWORK_STATE`), connection settings, bonding page (audited screens 016-020)
  Acceptance: two simultaneous links aggregate to a single stream at a standard SRTLA receiver (go-irl or BELABOX Cloud), per-link weights honour the audited controls, and dropping one link degrades rather than ends the broadcast.
  Complexity: L
  Note — the one part that does NOT port: `srtla_send` selects links with `bind()` to a source IP and requires OS source routing, which Android cannot configure unrooted. Replace that layer with Android's own multi-network API — `ConnectivityManager.requestNetwork()` to hold a cellular link up while WiFi is default, then `android_setsocknetwork()` (`<android/multinetwork.h>`) to pin each UDP socket to its `Network`. Budget the effort here, not in the protocol.

- [ ] P3 — IS-18 Thermal and sustained-load behaviour
  Why: sustained outdoor encoding is a top IRL failure mode, and the audit's ~275 MB PSS / 2.01% jank baseline was measured with no real encode running.
  Evidence: `app-audit/audit-summary.md` performance findings; Reddit IRL setup threads
  Depends on: IS-04
  Touches: `.../engine/`, new thermal listener
  Acceptance: `PowerManager` thermal status is observed and drives a documented degradation ladder rather than an abrupt stop.
  Complexity: M

- [ ] P3 — IS-19 Settings import/export over the audited `larix://` deep link and QR path
  Why: audited screens 116-118 define the flow, and it is how the original moves configuration between devices — but it must not exfiltrate secrets.
  Evidence: audited screens 116-118; `app-audit/app/components-and-intents.md` (payload grammar UNKNOWN beyond the scheme prefix)
  Depends on: IS-07
  Touches: `AndroidManifest.xml` intent filter, import/export settings page, secret storage
  Acceptance: a settings payload round-trips between two installs; secrets are excluded or re-encrypted; the deep-link grammar is this project's own and documented as a deviation, since the original's is unknown.
  Complexity: M

- [ ] P3 — IS-01 Record the licence posture that follows from the dependency choices
  Why: operator decision 2026-08-15 — pick the best tool and prefer open licences; do not gate work on licensing. That is a valid call, but it has one mechanical consequence worth writing down once: linking BELABOX `srtla` (AGPL-3.0) means the app is AGPL-3.0 when distributed, so its source must be offered to anyone who receives it. The repo is already public, so this costs nothing — it just needs stating.
  Evidence: BELABOX/srtla LICENSE (AGPL-3.0); libsrt is MPL-2.0 (file-level copyleft, arrives via StreamPack regardless) — RESEARCH.md "Security, Privacy, and Reliability".
  Touches: `LICENSE`, `README.md`, `replica-app/docs/known-deviations.md` (D011)
  Acceptance: a LICENSE file exists matching the strongest obligation actually linked, and D011 records which dependency imposed it.
  Complexity: S

- [ ] P3 — IS-20 Distribution and update channel decision
  Why: the app ships with a repo-owned self-signed key and no update path, so no user can receive a fix.
  Evidence: `replica-app/README.md` signing note; RESEARCH.md "Product Map"
  Touches: release scripts, `README.md`
  Acceptance: a decision is recorded (Play, GitHub Releases with an in-app update check, or explicitly none) and the release process matches it.
  Complexity: S

- [ ] P3 — IS-21 Localisation and RTL readiness
  Why: all copy is hardcoded English in `SettingsCatalog.kt` and the composables. The audit explicitly lists RTL locales and font scales above 1.0 as UNKNOWN, so this is unmeasured rather than known-good.
  Evidence: `app-audit/device/display-and-insets.md` "Unknowns"; `.../ui/settings/SettingsCatalog.kt`
  Touches: `res/values/strings.xml`, settings catalog, composables
  Acceptance: user-visible strings move to resources and the console renders without clipping at font scale 1.3 and under a forced-RTL locale; audited-geometry validation continues to run against the default locale.
  Complexity: M
