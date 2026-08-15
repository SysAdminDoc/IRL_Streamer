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
  Note (2026-08-15 pass 2): Moblin's ceiling is Twitch/Kick/YouTube/SOOP with 7TV/BTTV/FFZ emotes, TTS and phone-side moderation; sequence Kick (KickLib) after Twitch.

- [ ] P2 — IS-12 Adaptive bitrate (ABR)
  Why: table stakes across every competitor (Larix Premium, StreamPack, RootEncoder, Moblin), and the audited "Bitrate matches resolution" and adaptive-mode settings already imply it.
  Evidence: audited screens 053-056, 064; RESEARCH.md "Competitive Landscape"
  Depends on: IS-04
  Touches: `.../engine/`, video settings page
  Acceptance: encoder bitrate responds to congestion within the audited mode options; behaviour is observable in the log panel.
  Complexity: M
  Note (2026-08-15 pass 2): implement Moblin's four named algorithms — `belabox` (default), `fastIrl`, `slowIrl`, `customIrl` with their exposed tunables (packetsInFlight, pifDiffIncreaseFactor, minimumBitrate) — porting `Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtBelabox.swift` semantics and using Moblin's `AdaptiveBitrateSuite` tests as the cross-check corpus. No Android competitor ships named tunable IRL profiles.

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
  Note (2026-08-15 pass 2): add `irlserver/irl-srt-server` (SLS fork with SRTLA built in, HTTP stats API) as a recommended self-host receiver; the confusion is confirmed at scale ("I just thought your connection automatically improved when you bonded") and IRL Pro's free hosted relay is its moat — these docs are the open answer.

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
  Note (2026-08-15 pass 2): platform constraints now verified — `CHANGE_NETWORK_STATE` is install-time; hard cap 100 outstanding network requests per UID (register once, reuse); the platform may swap the satisfying network, so rebind sockets in `onAvailable` rather than caching `Network` handles; keep requests alive inside the foreground service. The `.so` must be 16 KB-page-aligned (NDK r28+, Play deadline 2027-02-01) and must link the IS-52 libsrt build, not srtdroid's bundled copy.

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
  Note (2026-08-15 pass 2): AGPL-3.0 on Play is verified viable (Signal-Android precedent; Google's AGPL ban is internal-use policy, not Play policy). F-Droid accepts AGPL; take the F-Droid signing key initially — reproducible builds with NDK code is the documented hard path. New Play apps must target API 36 from 2026-08-31 (already satisfied).

- [ ] P3 — IS-21 Localisation and RTL readiness
  Why: all copy is hardcoded English in `SettingsCatalog.kt` and the composables. The audit explicitly lists RTL locales and font scales above 1.0 as UNKNOWN, so this is unmeasured rather than known-good.
  Evidence: `app-audit/device/display-and-insets.md` "Unknowns"; `.../ui/settings/SettingsCatalog.kt`
  Touches: `res/values/strings.xml`, settings catalog, composables
  Acceptance: user-visible strings move to resources and the console renders without clipping at font scale 1.3 and under a forced-RTL locale; audited-geometry validation continues to run against the default locale.
  Complexity: M

## Audit Findings — 2026-08-15

Deep audit pass over `replica-app/` (app source, validation harness, scripts, docs).
Baseline at audit time: debug build green, 11/11 JVM unit tests green, no pre-existing
build/test failures. IDs continue the `IS-nn` scheme from IS-21.

### P0

- [ ] P0 — IS-22 Validation artifacts are never invalidated between runs — a state that fails to capture keeps its previous result, including PASS
  Category: correctness
  Where: `replica-app/scripts/run-visual-validation.ps1:30-45`, `replica-app/scripts/capture-replica-screen.ps1:45-54`, `replica-app/scripts/run-geometry-validation.ps1:16-30`
  Problem: three compounding freshness holes. (a) `run-visual-validation.ps1` assigns `$compareExit` but never uses it; each row's status is read solely from `validation\results\$id.json`, which is never deleted before capture — if capture fails (`$captureExit -ne 0`) or `compare-screen.ps1` throws before `visual_compare.py` writes the JSON, the *previous run's* result (including PASS) is reported and the script can exit 0. (b) `capture-replica-screen.ps1` swallows `uiautomator dump` failure with only a `Write-Warning` while `validation/hierarchy/` retains all 145 dumps between runs, so the geometry pass silently scores a stale hierarchy as current (`NO_REPLICA_DUMP` can never fire — a file always exists). (c) Nothing compares artifact mtimes against capture start. A screen that can no longer even be captured stays green indefinitely, and `run-full-validation.ps1:38` then prints "Full validation passed."
  Evidence: code paths traced; `validation/results/` and `validation/hierarchy/` verified on disk to hold all 145 files from the prior run; the comment at capture-replica-screen.ps1:50 ("the screenshot is the gating artifact") predates the geometry gate.
  Fix: delete `results\$id.json`, `current\$id.png` and `hierarchy\$id.xml` at the start of each state's capture; on `$captureExit -ne 0` or compare failure emit an explicit `ERROR` row that increments `$failed`; optionally embed capture timestamps in result JSONs and have `run-geometry-validation.ps1` reject dumps older than the matching current PNG.
  Acceptance: killing adb mid-sweep (or renaming one baseline) produces ERROR/NO_REPLICA_DUMP rows and a non-zero exit, never a PASS carried over from the previous run.
  Confidence: Verified
  Effort: M

### P1

- [ ] P1 — IS-23 Final report presents hardcoded constants as file-derived gate evidence
  Category: correctness
  Where: `replica-app/scripts/build_final_report.py:151-155` (also 96, 110-114)
  Problem: the generated report states "Every number below is read from a file in that tree", but the JVM test count ("11 passed / 0 failed"), on-device test count, release signing SHA-256, and "Release install + cold launch … no FATAL EXCEPTION" rows are string literals. After a test regression, re-signed APK, or crashing release build, re-running the report still prints identical "verified" evidence — behaviour gate 3 can never fail through this script.
  Evidence: literals at the cited lines; contrast with the APK size row which is genuinely read from disk.
  Fix: parse Gradle test-result XML under `app/build/test-results/` and `app/build/outputs/androidTest-results/`, compute the cert hash via `apksigner verify --print-certs`; any row that cannot be derived must be labelled "manually recorded YYYY-MM-DD", not presented as read-from-file.
  Acceptance: deleting a test or breaking the build changes the report's numbers (or the report refuses to claim them); no literal test-count/hash strings remain in the script.
  Confidence: Verified
  Effort: M

- [ ] P1 — IS-24 The geometry gate cannot fail: no threshold is enforced, exit code is always 0, and shrunken coverage passes silently
  Category: testing
  Where: `replica-app/scripts/run-geometry-validation.ps1:4,29-36` (param `-WithinPxTarget` used only in report prose), `replica-app/scripts/geometry_diff.py:234` (`main()` returns 0 unconditionally), `replica-app/scripts/run-full-validation.ps1:35-36` (only "failed to run" is an error)
  Problem: README/CLAUDE.md describe a three-gate validation, but geometry is reporting-only. Every element could drift 30 px and every exit code stays 0. Rows with `status -ne 'OK'` are filtered out of the stats, so if only 5 of 145 states have hierarchy dumps the report says "States compared: 5" and still exits 0. Per-state, `matched_elements: 0` yields `mean_abs_origin_error_px: 0.0` and status `OK` — a vacuous pass that reads as perfect (state 054 currently reports exactly this: 0 matched, 11 unmatched, mean 0.0). Critically, the masking policy relies on geometry *instead of* SSIM for camera-preview live-console states, so those states currently have no failing gate at all.
  Evidence: `validation/reports/geometry-summary.csv` row `054_video_manual_bitrate_enabled,OK,0,11,0,0,0,0,0.0,0.0,0.0`; script traced end to end.
  Fix: in `run-geometry-validation.ps1`, exit non-zero when compared-state count < catalog states with audit XML, when any state has `matched_elements == 0`, or when the within-2px aggregate is below a declared target derived from `-WithinPxTarget`; report unmatched counts in the summary stats instead of dropping them.
  Acceptance: renaming one hierarchy dump or zeroing one state's matches makes `run-geometry-validation.ps1` (and therefore `run-full-validation.ps1`) exit non-zero.
  Confidence: Verified
  Effort: M

- [ ] P1 — IS-25 Debug state 054 renders the wrong surface entirely — an adaptive-mode dialog instead of the manual-bitrate settings screen
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/debug/DebugStateCatalog.kt:53` (`in 41..64` branch) and `:216-234` (`videoDialog` has no case for 54, so it falls to the `else` adaptive-mode dialog)
  Problem: audit `054_video_manual_bitrate_enabled` is type `settings_screen` — the Video page with "Bitrate matches resolution" toggled off so the H264 bitrate row is enabled. The catalog blankets 41..64 with `.withDialog(videoDialog(number))`, so 054 injects the adaptive-bitrate-mode choice dialog over the page instead. The state can never validate.
  Evidence: `app-audit/screens/screen-catalog.csv` row 054 (`settings_screen`, path "Settings > Video > Bitrate matches resolution"); geometry result 0 matched / 11 unmatched; SSIM 0.7359.
  Fix: special-case 54 before the 41..64 branch: `base.settings(SettingsPage.VIDEO, 12).copy(overrides = ScreenOverrides(bitrateMatchesResolution = false))` — the override field already exists and `SettingsScreen.kt:89` already derives the bitrate row's enabled state from it. Verify the audited scroll anchor for 054 resolves.
  Acceptance: geometry for 054 matches > 0 elements with the H264 bitrate row enabled and no dialog present; SSIM rises materially from 0.7359.
  Confidence: Verified
  Effort: S

- [ ] P1 — IS-26 Dialog choice lists reuse the previous dialog's scroll position — every `*_menu_middle/_lower` validation state captures the wrong list window
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/components/AuditedDialogs.kt:141-143`
  Problem: `rememberLazyListState(initialFirstVisibleItemIndex = request.listAnchorIndex)` is remembered positionally with no key. During the warm sequential ADB sweep (042→043, 044→045→046, 050→051→052→053, 068→069→070, 072→073) the dialog host stays in composition, so the LazyListState — and its scroll position — carries over from the previous state; `listAnchorIndex` only applies on first creation. The `input`/`selected` states directly above are correctly keyed on `request.id`; the list state was missed. This is the same warm-injection trap CLAUDE.md records for settings lists (fixed there via `key(debugScreenId, …)`).
  Evidence: geometry results show the exact signature — 050 expects options around "0…+0.4" (audit anchor label "0", index 20) but the replica dump shows "-2…-1.6" (list top, i.e. the prior dialog's scroll); same pattern on 043, 045, 046, 051-053, 069, 070, 073.
  Fix: wrap in `key(request.id, request.listAnchorIndex) { rememberLazyListState(...) }` inside the choice branch of `AuditedDialogHost`.
  Acceptance: a warm sweep over 044→045→046 produces three different first-visible options matching each state's anchor; the ~10 affected states' geometry matched counts rise.
  Confidence: Verified
  Effort: S

- [ ] P1 — IS-27 Reopening a dialog shows catalog defaults, not current values — pressing OK silently reverts saved settings
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/settings/SettingsCatalog.kt:337-342` (static `DialogRequest` factories), `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/components/AuditedDialogs.kt:65-68` (seeds `input`/`selected` from the request), `MainViewModel.kt:84-104`
  Problem: every catalog action embeds a static `DialogRequest` with hardcoded `initialValue`/`selectedOptions`. Concrete data-loss path: set chat font scale to 300 (row correctly shows 300 via DataStore) → reopen the dialog → field shows "220" → OK persists 220. Same for `alert_dashboard_scale`, `h264_bitrate_kbps`. Worse for `safe_margin_ratios` (CHOICE_MULTIPLE): user persists {16:9, 21:9} → reopen shows only 16:9 checked → OK persists {16:9}, silently dropping 21:9. Single-choice dialogs (resolution, FPS, etc.) re-check the catalog default instead of the current transient selection.
  Evidence: `handleAction` (SettingsScreen.kt:131-138) passes `action.request` unmodified to `showDialog`; the dialog host seeds local state from `request.initialValue`/`request.selectedOptions` only.
  Fix: resolve current values at show time — in `handleAction` (or a `MainViewModel.showDialog` overload), copy the request with `initialValue` from the same source `resolveSummary` uses (settings + transientValues) and `selectedOptions` from `transientValues[id] ?: persisted value ?: catalog default`. Add a MainViewModel unit test for the reopen-then-OK path.
  Acceptance: set font scale 300, reopen → field pre-fills 300; check 21:9 in safe margins, reopen, OK → 21:9 still persisted (asserted by a new unit test).
  Confidence: Verified
  Effort: M

- [ ] P1 — IS-28 Seven states' scroll anchors are structurally unresolvable and fall back silently to hand-guessed indices
  Category: correctness
  Where: `replica-app/scripts/extract_scroll_anchors.py` (records the first text node in the viewport, which can be a slider value or the app-bar title), `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/settings/SettingsScreen.kt:57-62` (label must equal `itemTitle`, else silent fallback), `app/src/main/assets/audit-scroll-anchors.json`
  Problem: anchors for 018/019/020 record label "100" (a slider value), 038/039/040 record "On" (a choice value), 126 records "Advanced options" (the app-bar title). None can ever match a catalog item title, so `indexOfFirst` returns -1 and the state silently scrolls to the old hand-guessed `settingsScrollIndex` — exactly the error class the anchor system was built to eliminate. All seven states currently show more unmatched than matched elements in geometry (e.g. 018 misses "WiFi Weight"/"Ethernet Weight" while showing rows above them).
  Evidence: anchors JSON inspected (20 of 108 anchors carry value-like labels; the 13 on selection dialogs resolve against option lists and are fine — the 7 settings-screen ones cannot resolve); `validation/reports/geometry/018….json` missing/extra lists confirm the wrong window.
  Fix: two sides. Extractor: when the first viewport node's text matches no known row-title pattern (pure number / On / Off / the page title), walk forward to the first node that is a plausible title, or record an ordered candidate list. App: match anchor labels against summaries and slider value labels too, and log (debug build) whenever a non-null anchor fails to resolve so the fallback is never silent. Re-run `extract_scroll_anchors.py` and re-commit the asset.
  Acceptance: geometry for 018-020, 038-040, 126 shows the audited rows matched (unmatched < matched); a debug log line fires on any unresolved anchor.
  Confidence: Verified
  Effort: M

- [ ] P1 — IS-29 Emulator boot script crashes intermittently: `2>$null` under `$ErrorActionPreference='Stop'` turns adb stderr into a terminating error
  Category: reliability
  Where: `replica-app/scripts/start-headless-emulator.ps1:28,39` (dot-sources `Common.ps1`, which sets EAP Stop at line 1-2)
  Problem: in PS 5.1 with EAP Stop, `& adb … 2>$null` throws on the first stderr line. The boot poll at line 28 runs immediately after the serial appears, exactly when adb emits `error: closed`/`error: device offline` — killing the script mid-wait. When adb errors, stdout is `$null` and the subsequent `.Trim()` is a second terminating error. Line 39 has the same trap after boot, which can orphan a running headless emulator after the wait succeeded but before the serial is printed. This is the entry point of the whole validation pipeline.
  Evidence: mechanics empirically verified on this host's PS 5.1 (stderr redirect under EAP Stop throws; `$null.Trim()` throws).
  Fix: wrap the poll body in try/catch (treat any throw as "not booted yet"), null-guard with `"$booted".Trim()`, and lower EAP around the post-boot `cmd overlay`/`wm` calls the way `Invoke-Checked` (Common.ps1:59-61) already does.
  Acceptance: the script survives an `adb: device offline` window during boot and always either prints `HEADLESS_EMULATOR_SERIAL` or kills the emulator process it started.
  Confidence: Verified
  Effort: S

### P2

- [ ] P2 — IS-30 Settings scroll position leaks between pages — opening a second settings page inherits the previous page's scroll
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/settings/SettingsScreen.kt:63-65`
  Problem: the LazyListState is created under `key(state.runtime.debugScreenId, initialIndex)` — the page is not in the key. In interactive use both values are (null, 0) for every page, so ROOT→VIDEO→AUDIO all reuse one LazyListState from the same call site. Scroll Video parameters to the bottom (~30 items), navigate back to Root (12 items): Root renders scrolled/clamped to its end instead of the top.
  Evidence: code trace — `GenericSettingsScreen` is the shared `else` branch for all catalog pages, so positional memoization retains the state across page changes; item keys change wholesale so LazyColumn keeps the raw index.
  Fix: include the page in the key: `key(page, state.runtime.debugScreenId, initialIndex) { rememberLazyListState(...) }`.
  Acceptance: scroll Video to the bottom, back, open Root → Root starts at the top (Compose UI test or manual on emulator).
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-31 Persistence is silently partial: ~20 toggles and every choice selection are lost on process death, and "Reset app settings" does not clear the in-memory ones
  Category: ux
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/MainViewModel.kt:162-170,193-211` (`persistedBooleanKeys` whitelist; others go to `transientBooleans`), `:84-104` (CHOICE_SINGLE results go only to `transientValues`), `:101` (`reset_settings` clears DataStore but not `transientBooleans`/`transientValues`)
  Problem: only 15 toggle keys persist; the other ~20 catalog toggles (prefer_bluetooth, aec, noise_suppressor, mirror_front, web_debug, …) flip convincingly but reset on restart. Every single-choice selection (resolution, FPS, codec, audio source, …) is transient too — the row summary updates, implying a saved setting, then reverts. And after "Reset app settings" the UI keeps showing the transient values the reset claimed to clear, because only the DataStore is wiped.
  Evidence: whitelist vs `SettingsCatalog` toggle inventory compared exhaustively; `confirmDialog` stores CHOICE_SINGLE only into `transientValues`; `reset()` path touches only the repository. No deviation in `docs/known-deviations.md` documents partial persistence (D005/D012 cover simulated operations, not settings retention — D012's own validation column says "Validate persistence").
  Fix: either persist everything (extend DataStore keys to all audited toggles and add a string map for choice values — mechanical, catalog-driven) or document the boundary as a numbered deviation; in both cases make `confirmDialog("reset_settings")` also clear `transientBooleans`/`transientValues` in the same state update.
  Acceptance: toggle "Prefer Bluetooth Mic", pick "1280x720", kill and relaunch → both retained (or a deviation row explicitly says they are not); after Reset, no row shows a pre-reset transient value.
  Confidence: Verified
  Effort: M

- [ ] P2 — IS-32 Replica reworded audited non-branded copy — undocumented deviations that also break geometry matching
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/settings/SettingsCatalog.kt:303` ("Disable Camera2 only if camera resolutions or framerates are unavailable.") and `:304` ("Stream and record front camera as it appears in preview (mirrored).")
  Problem: the audited strings are "Disable Camera2 API only if you have some issues with camera like some resolutions or framerates are not available." and "Stream and record front camera as appears in preview (mirrored)." (no "it"). Neither contains third-party marks, so D014 does not cover the rewording; the rebuild contract reproduces confirmed copy verbatim. The changed strings additionally fail label matching, contributing to state 126's unmatched count.
  Evidence: `app-audit/evidence/ui-xml/126_preferred_camera_api_dialog.xml` contains both audited strings verbatim; geometry 126 lists them under `unmatched_audit_elements` with the replica variants under `replica_only_elements`.
  Fix: restore the audited strings character-for-character (including the original's grammar). Sweep the catalog against the audit XMLs for other silent rewordings of non-branded copy and either restore or add a numbered deviation for each.
  Acceptance: geometry for 126 matches both summary strings; a sweep report lists zero undocumented copy diffs on non-branded strings.
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-33 Password masking for API-key dialogs is dead code — secrets render in cleartext
  Category: security
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/components/AuditedDialogs.kt:212-216`
  Problem: masking triggers on `label.contains("key", true)`, but `label` is the per-field label from `request.options`, which is empty for every single-field catalog dialog — so "Dashboard A API Key", "Dashboard C API Key" and any future stream-key dialog show typed secrets unmasked. The condition only ever fires for the debug fixtures (states 030/031) whose field labels literally contain "Stream key". The check plainly intends to cover the catalog dialogs and misses them because it tests the wrong string.
  Evidence: all catalog `text(...)` dialogs construct `DialogRequest` with `options = emptyList()` → `fields = listOf("")` → label blank; the dialog *titles* contain "API Key".
  Fix: extend the condition to `request.title` as well (`label.contains(...) || request.title.contains("key", true) || …password…`), or add an explicit `sensitive: Boolean` to `DialogRequest` set by the catalog factories.
  Acceptance: typing into "Dashboard A API Key" shows dots; states 030/031 still mask their key fields.
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-34 Safe-margin overlay ignores the persisted ratios setting — hardcoded to a single 16:9 rectangle
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/live/LiveConsoleScreen.kt:358-384` (`SafeMarginGuide`, `val ratio = 16f / 9f`)
  Problem: "Safe margins ratios" is a persisted multi-select (DataStore `safe_margin_ratios`, nine options), but the console overlay draws exactly one 16:9 rectangle regardless. Selecting 21:9 — or several ratios — changes nothing on the live console, making a persisted, validated setting a no-op.
  Evidence: `SafeMarginGuide(indentPercent)` takes no ratios parameter; no other reader of `settings.safeMarginRatios` exists outside the settings row/dialog.
  Fix: pass `state.settings.safeMarginRatios` in, parse each label's parenthesised ratio (e.g. "21:9 (2.33)" → 2.33), and draw one centred rectangle per selected ratio, keeping the existing indent logic. Audit evidence (state 085) only shows the default single 16:9 — keep that as the default so validation is unchanged.
  Acceptance: selecting two ratios draws two rectangles; state 085 still validates with the default selection.
  Confidence: Verified
  Effort: M

- [ ] P2 — IS-35 Silent no-op inputs across the app: text/number dialog entries are discarded with zero feedback
  Category: ux
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/MainViewModel.kt:84-104` (`confirmDialog` handles only 7 ids), `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/ReplicaApp.kt:32` (folder picker result discarded)
  Problem: OK on any unhandled dialog id silently discards the input: all TEXT dialogs (platform usernames, custom chatbox URL, dashboard API keys, custom page, `import_settings` payload) and the NUMBER dialogs `keyframe`, `section_minutes`, plus the form dialogs `refresh_interval`, `layer_z`, `web_z`, `web_width`, `web_height`, `web_scale` (typed values vanish; the row summary never changes). Entering a non-numeric value in a *handled* NUMBER dialog (`toIntOrNull()` → null) also closes silently. The Save-to folder picker launches DocumentsUI and drops the chosen tree. The QR alert asks "Would you like to open the app store?" and YES does nothing. Individually small; together the settings tree teaches the user that editing does nothing.
  Evidence: `confirmDialog`'s `when` exhaustively traced against every `DialogRequest` id in `SettingsCatalog`/`Forms`; `rememberLauncherForActivityResult(OpenDocumentTree) { }` has an empty callback.
  Fix: route unhandled TEXT/NUMBER confirmations into `transientValues[id]` so summaries update in-session (mirroring CHOICE_SINGLE), show the existing toast pattern for fixture-only actions ("Stored locally in this simulation"), reject non-numeric NUMBER input by keeping the dialog open or toasting, and acknowledge the folder pick (toast the chosen path or persist it as the Save-to summary). The QR alert's YES should either open the Play listing intent or the copy should stop asking.
  Acceptance: every OK path visibly does something (summary update, toast, or validation message); no dialog discards typed input without feedback.
  Confidence: Verified
  Effort: M

- [ ] P2 — IS-36 Geometry matcher: phantom "missing" elements from text/content-desc duplicates and unscored authorized renames drown real regressions
  Category: testing
  Where: `replica-app/scripts/geometry_diff.py:37` (`IDENTITY_TEXT` covers only 2 strings), `:60-61` (`key = text or desc`), `:5-7` (docstring promises resource-id/class fallback matching that does not exist)
  Problem: three noise sources make `unmatched_audit_elements` unreliable as a regression signal. (a) Audit dumps carry both a TextView (`text="DISPLAY"`) and a tab container (`content-desc="Display"`) for the same widget, so every quick-panel state reports phantom missing Title-case tabs even when the uppercase text matched (state 138 "misses" Audio/Display/Log/Network/Overlays). (b) D014-renamed labels (TWITCH.TV→PLATFORM A, Streamlabs→Dashboard A, AuditTestz→Local fixture) are not in `IDENTITY_TEXT`, so states 009-011, 025, 030-031, 129 permanently report high unmatched counts, hiding any genuinely missing element among authorized renames. (c) ~44% of nodes have neither text nor desc and are invisible to the matcher, while the docstring claims a fallback that was never implemented.
  Evidence: fresh run of `geometry_diff.py --screen 138…` reproduces the Title-case phantoms; `app-audit/evidence/ui-xml/138….xml` contains only uppercase text plus desc duplicates; unmatched lists for 009/025/030 are exactly the D014 rename pairs.
  Fix: dedupe audit nodes whose text and desc keys refer to the same bounds; match case-insensitively; move the D014 rename pairs into a versioned alias table (audit label → replica label) consulted before declaring an element missing; report the unscored-node count per screen and fix the docstring.
  Acceptance: state 138 reports no tab-label phantoms; states 009-011/025/030-031 report unmatched ≈ 0 with aliases applied; the summary CSV gains an `unscored` column.
  Confidence: Verified
  Effort: M

- [ ] P2 — IS-37 Diff heatmap: uint8 overflow renders the *worst* pixel errors near-black
  Category: correctness
  Where: `replica-app/scripts/visual_compare.py:119`
  Problem: `heat_rgb[..., 0] = np.clip(heat * 3, 0, 255)` multiplies a uint8 array, which wraps modulo 256 before the clip (diff 86 → red 2, diff 200 → red 88). Severe-difference regions render dark in `validation/diffs/` — the exact images the report tells a reviewer to inspect. Line 120 already does it correctly with `astype(np.int16)`.
  Evidence: numerically verified (uint8 86*3 = 2).
  Fix: `np.clip(heat.astype(np.int16) * 3, 0, 255)`.
  Acceptance: a synthetic 100%-different image pair produces a saturated-red diff map.
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-38 Device guard accepts the operator's physical phone; the docs print its serial next to the commands
  Category: reliability
  Where: `replica-app/scripts/Common.ps1:80-92` (`Assert-ReplicaDevice` checks only connected + non-empty), `replica-app/docs/testing-guide.md:5` (prints the physical device serial verbatim)
  Problem: the repo's standing rule is that validation never touches the attached Samsung phone, but nothing enforces it — a single paste slip of the phone serial (conveniently displayed in the testing guide) passes the guard and installs/launches/`pm clear`s on the phone. All adb calls are `-s`-targeted (verified), so the only hole is serial choice — which is exactly the hole.
  Fix: in `Assert-ReplicaDevice`, require `$Serial -like 'emulator-*'` or `adb -s $Serial shell getprop ro.kernel.qemu` returning 1, with an explicit `-AllowPhysicalDevice` switch as the escape hatch; remove the real serial from testing-guide.md.
  Acceptance: passing the phone's serial to any validation script aborts with a clear message unless the escape hatch is given.
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-39 Fresh clone cannot run any visual comparison: `validation/baseline/` is gitignored and nothing populates it
  Category: reliability
  Where: `.gitignore:21`, `replica-app/scripts/compare-screen.ps1:7-9`, `replica-app/README.md:129`
  Problem: every compare requires `validation/baseline/<id>.png`; the directory is gitignored, no script copies it from `app-audit/evidence/screenshots/`, and the README claims `validation/` contains "immutable baseline copies" — false on a clone. Every documented compare command dies with "Image not found".
  Fix: add a `sync-baselines.ps1` (or a step inside `run-visual-validation.ps1`) that mirrors `app-audit/evidence/screenshots/` → `validation/baseline/` when missing, and correct the README sentence.
  Acceptance: on a clean checkout, `run-full-validation.ps1 -AllScreens` reaches the capture stage without manual baseline copying.
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-40 `validation/masks/README.md` states "No regions are currently masked" — the opposite of the shipped policy
  Category: docs
  Where: `replica-app/validation/masks/README.md:5`; designated as the mask-policy source by `replica-app/docs/testing-guide.md:63`
  Problem: the file the docs point to for mask semantics still carries the first-pass text, while `mask-register.csv` defines 3 SYSTEM_OWNED regions and every current report quotes the secondary app-chrome metric. An auditor of the masking policy is told the opposite of what the pipeline does.
  Fix: rewrite to the current policy — strict gate unmasked, register-driven secondary metric, the three OS-owned regions with their reasons, camera preview deliberately unmasked.
  Acceptance: the README matches `mask-register.csv` and the policy paragraph in `docs/known-deviations.md`.
  Confidence: Verified
  Effort: S

- [ ] P2 — IS-41 `Stop-GradleDaemons` in `finally` blocks can convert a green build into a failure (or mask the real error)
  Category: reliability
  Where: `replica-app/scripts/Common.ps1:96`
  Problem: `& gradle --stop … 2>&1 | Out-Null` runs under script-wide EAP Stop; any JVM stderr noise (`Picked up _JAVA_OPTIONS`, deprecation warnings) becomes a terminating error inside the `finally` of every build/test wrapper — a successful build exits non-zero, and if the `try` had already thrown, the finally's exception replaces the real error. `Invoke-Checked` (lines 59-61) already lowers EAP around its own redirect for exactly this reason.
  Evidence: `2>&1 | Out-Null` under EAP Stop empirically verified to throw on stderr output on this host's PS 5.1.
  Fix: set `$ErrorActionPreference = 'Continue'` locally inside `Stop-GradleDaemons` (it is intentionally best-effort).
  Acceptance: a build run with `_JAVA_OPTIONS` set to emit a warning still exits 0.
  Confidence: Verified
  Effort: S

### P3

- [ ] P3 — IS-42 Stale claims from the previous pass persist in three docs
  Category: docs
  Where: `replica-app/docs/rebuild-plan.md:38,58`; `replica-app/docs/testing-guide.md:83`; `replica-app/README.md:138`
  Problem: rebuild-plan says "six JVM tests" (actual 11), quotes the previous pass's median/max (0.836694/0.930146 vs current 0.869631/0.930838) and claims "the final sweep deliberately used no masks" (a mask register now exists); testing-guide also says "Six JVM tests"; README presents gitignored `validation/current/release-launch.png` as an existing deliverable.
  Fix: update the three passages to the current numbers/policy, or point them at the generated reports the way design-qa.md does; rephrase the README pointer as "is written to".
  Acceptance: `grep -ri "six JVM" docs/ README.md` is empty; no doc contradicts the mask register; no committed doc claims a gitignored file exists.
  Confidence: Verified
  Effort: S

- [ ] P3 — IS-43 `update_traceability.py` run on an empty results dir wipes both committed status CSVs
  Category: reliability
  Where: `replica-app/scripts/update_traceability.py:27-29,44-48,62-63`
  Problem: the script treats whatever is in `validation/results/` as the truth. On a fresh clone (results gitignored/empty) it rewrites all 145 rows of `docs/audit-traceability-matrix.csv` and `docs/implementation-status.csv` to `NOT_STARTED "No capture on disk."`, destroying committed status; with a partial (default 16-screen) sweep it mixes passes into the "current" columns.
  Fix: refuse to run (with a clear message) when results count < catalog count unless `--allow-partial` is passed; on partial runs touch only the refreshed rows.
  Acceptance: running the script on a clean clone exits non-zero without modifying the CSVs.
  Confidence: Verified
  Effort: S

- [ ] P3 — IS-44 Accessibility gaps on the surfaces D010 claims were improved
  Category: a11y
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/live/LiveConsoleScreen.kt:196-199` (every console button, toggle or not, announces "Selected"/"Not selected"), `QuickSettingsPanel.kt:305-320` (quick-toggle rows are 27 dp tall; `AuditedSwitch` state semantics live on the switch while the click action lives on the row — TalkBack reads a stateless clickable row), `PreferenceComponents.kt:124` (`indication = null` removes the ripple from every preference row while toggle rows keep theirs)
  Problem: non-toggle buttons (Settings, Reload, Snapshot) announce a selection state they don't have; quick-panel rows are far below the 48 dp target D010 cites; the switch's On/Off state is not merged into the actionable row semantics; and tap feedback is inconsistent across row types (ripple suppression buys nothing — static captures never show ripples).
  Fix: set `stateDescription` only when the control is a toggle; add `Modifier.toggleable(value = checked, role = Role.Switch)` on the quick rows (semantic 48 dp via `minimumInteractiveComponentSize` without changing visuals) and clear the child switch's duplicate semantics; drop `indication = null`.
  Acceptance: TalkBack announces no selection state on plain buttons, announces On/Off on toggle rows, and Accessibility Scanner reports no sub-48 dp targets on the quick panel; geometry/SSIM results unchanged.
  Confidence: Verified
  Effort: M

- [ ] P3 — IS-45 Live-console telemetry fixture pins state 001's values on all nine live states
  Category: visual
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/live/LiveConsoleScreen.kt:222-231` (`TelemetryBlock` hardcodes -283 mA / -1144 mW / 31.6 °C)
  Problem: each audited live state carries its own telemetry (132: -512 mA / -2106 mW / 33.3 °C; 138: -482 mA / -1947 mW / 34.7 °C). The fixture always renders 001's values, so eight states mismatch both text and glyph geometry. D009 accepts sanitized values, but the repo already pins per-state values elsewhere via `ScreenOverrides` (e.g. wifiWeight=50 for 020), so per-state fidelity is the established pattern.
  Evidence: audit XMLs for 132/138 vs the hardcoded strings; geometry lists the audited readings as unmatched.
  Fix: add telemetry fields to `ScreenOverrides` (or a small per-state map in `DebugStateCatalog`) populated from the audited values for 130-145; default stays the 001 fixture.
  Acceptance: geometry for 132/134/138 matches the audited telemetry strings.
  Confidence: Verified
  Effort: S

- [ ] P3 — IS-46 Release build ships a browsable `irlstreamer://` intent filter whose handler is debug-only
  Category: maintainability
  Where: `replica-app/app/src/main/AndroidManifest.xml` (VIEW/BROWSABLE filter, scheme `irlstreamer`), `MainActivity.kt:65-66` (`handleDebugIntent` returns immediately unless `ENABLE_DEBUG_STATE_SELECTOR`, which is false in release)
  Problem: any `irlstreamer://` link launches the release app to no effect — a dead deep-link surface that also squats on the scheme IS-19 will later define. In debug it is intentional (state injection).
  Fix: move the VIEW intent filter to `src/debug/AndroidManifest.xml` so release manifests omit it until IS-19 defines the real import grammar.
  Acceptance: `adb shell am start -a android.intent.action.VIEW -d "irlstreamer://x"` resolves to the app only in debug builds.
  Confidence: Verified
  Effort: S

- [ ] P3 — IS-47 Flip-camera treats camera 3 (front) as a flip target to camera 1 (also front), and the flip button highlights only camera 1
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/MainViewModel.kt:130-132` (`flipCamera`: `if (id == 1) 0 else 1`), `LiveConsoleScreen.kt:143` (`selected = currentCameraId == 1`)
  Problem: the lens fixture defines 0/2 as rear and 1/3 as front (D008). Flipping from camera 3 lands on camera 1 — front to front — and from camera 2 also lands on 1 rather than the last-used front lens; the flip button's selected tint ignores camera 3. Audit evidence covers only cameras 0/1/2, so the intended mapping for 3 is unevidenced, but front→front is wrong under any reading.
  Fix: flip between facings: if current ∈ {1,3} go to the last-used rear (default 0), else to the last-used front (default 1); highlight when current ∈ {1,3}.
  Acceptance: from camera 3, flip lands on a rear lens; flip highlight active for both front cameras; states 139-141 still validate.
  Confidence: Likely (mechanism verified; audited intent for camera 3 unknown)
  Effort: S

- [ ] P3 — IS-48 Validation-state 101's error text lives in a 3.5-second system toast — capture timing can silently lose it
  Category: testing
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/ReplicaApp.kt:41-47` (validationError rendered via `Toast` then immediately consumed), `DebugStateCatalog.kt:86`
  Problem: the "Please input non-empty text/html code" state renders as a system toast that is consumed on first composition and never re-shows. If the screenshot lands after `Toast.LENGTH_LONG` expires (slow emulator, retried capture), the capture silently shows no error and — combined with IS-22 — a stale prior result could stand in. The toast window is also invisible to `uiautomator dump`, so the geometry gate cannot cover this state's defining element at all.
  Fix: render `validationError` as an in-app surface for the debug state (e.g. a transient overlay matching the audited toast bounds) or re-trigger the toast on each injection and have the capture script wait-and-verify; at minimum document that 101's defining pixel region is time-boxed.
  Acceptance: repeated captures of state 101 deterministically contain the error text (verify with 5 consecutive captures).
  Confidence: Needs-repro (mechanism verified in code; flake not yet observed)
  Effort: S

- [ ] P3 — IS-49 PowerShell harness robustness: StrictMode `.Count`, unset `$output`, dead-emulator wait, unverified `wm size`
  Category: reliability
  Where: `replica-app/scripts/run-geometry-validation.ps1:29-30` (`$rows.Count` throws under StrictMode 2.0 when Where-Object yields 0 or 1 rows — the "no rows" guard can never print its own message); `Common.ps1:62-64` (missing `py.exe` leaves `$output` unassigned → misleading "variable not set" error); `start-headless-emulator.ps1:16-38` (wait loop never checks `$process.HasExited`, so a bad `-Avd` name blocks the full 240 s; `wm size`/`wm density` results are piped to Out-Null unchecked — a failure yields 145 DIMENSION_MISMATCH results with no root-cause hint)
  Problem: all four fail in degraded scenarios with wrong or absent messages — exactly when diagnostics matter.
  Fix: `$rows = @(Import-Csv … | Where-Object …)`; initialize `$output = @()` or `Get-Command $FilePath` upfront; poll `HasExited` in the wait loop and surface `.err.log`; read back `wm size`/`wm density` after setting them and abort on mismatch.
  Acceptance: each degraded scenario (0 OK rows; py.exe absent; typo'd AVD; failed wm size) produces its intended, specific error message.
  Confidence: Verified (first two empirically; latter two by code path)
  Effort: M

- [ ] P3 — IS-50 Python harness robustness: crash on empty results, threshold text hardcoded, anchor extractor's latent filter and silent skip
  Category: maintainability
  Where: `replica-app/scripts/build_final_report.py:65,96,110-114` (`stats()` returns None on empty input → `TypeError` instead of "no results found"; report text hardcodes "0.985" and the `PREVIOUS` pass constants rather than deriving them from `thresholds.csv`/results); `replica-app/scripts/extract_scroll_anchors.py:74-88` (selection-dialog branch requires `top >= list_top`, so a row straddling the viewport top can never anchor and `scroll_offset_px` is structurally 0 — latent today, wrong on any re-capture with a mid-row offset; screen 057, whose dump has no ListView because the IME covers it, is dropped by a bare `continue` with no message)
  Fix: early-exit with a clear message when results are empty; derive threshold text from `thresholds.csv` and assert uniformity; mirror the settings-branch filter (`bottom > list_top`); log every skipped screen.
  Acceptance: empty-dir run prints a one-line explanation and exits non-zero; extractor logs 057 as skipped-with-reason; a synthetic straddling-row XML produces a non-zero offset.
  Confidence: Verified
  Effort: S

- [ ] P3 — IS-51 Dead code and dead parameters
  Category: maintainability
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/components/AuditedDialogs.kt:263-270` (the `else if` button branch is unreachable — its guard can only be reached when type is CHOICE_SINGLE with dismissOnChoice, and its own condition then excludes every case); `PreferenceComponents.kt:245-256` (`InfoRow.enabled` — no caller passes true; the enabled branch is dead and its color choice inverts the disabled convention); `run-geometry-validation.ps1:3,13` (`-Serial` param and `$android` assignment unused); `replica-app/.gitignore:10-14` (`!validation/*/.gitkeep` negations can never take effect — the files don't exist and the root .gitignore ignores the dirs wholesale)
  Problem: each misleads the next reader about behavior that cannot occur.
  Fix: delete the unreachable branch; drop `InfoRow.enabled` or wire a real caller; remove the dead param/assignment; delete the `.gitkeep` negations or ship the scaffold they imply.
  Acceptance: `grep` finds none of the four; build and tests stay green.
  Confidence: Verified
  Effort: S

## Research-Driven Additions — 2026-08-15 (pass 2)

From `RESEARCH.md` second pass (screenshot-testing ecosystem, competitor matrices, platform/CVE sweep, community signal). Every item traces to a source recorded there.

### P1

- [ ] P1 — IS-52 Build libsrt 1.5.6+ and OpenSSL 3.5.x from source; do not ship srtdroid's bundled SRT stack
  Why: StreamPack 3.2.0 → srtdroid 1.9.5 bundles srt 1.5.4 + OpenSSL 3.5.1; SRT ≤1.5.5 carries two Critical CVEs — CVE-2026-55868 (encryption state-machine downgrade) and CVE-2026-55869 (KMREQ/KMRSP stack buffer overflow) — fixed in v1.5.6 (2026-07-20). The srtla port (IS-17) needs libsrt anyway, so one owned, 16 KB-aligned, NDK r28-built copy serves both and closes the CVEs and the Play 2027-02-01 page-size deadline in a single move.
  Evidence: GHSA-4mc6-qmpp-g7gw, GHSA-6xg9-784j-24rm, Haivision/srt v1.5.6 release; StreamPack `gradle/libs.versions.toml@3.2.0`; srtdroid 1.9.5 release notes — RESEARCH.md "Security, Privacy, and Reliability".
  Depends on: IS-04 (lands together with engine adoption)
  Touches: `app/build.gradle.kts`, new native build scripts (CMake/NDK), CI artifact caching
  Acceptance: `llvm-objdump -p libsrt.so | grep LOAD` shows `align 2**14`; the packaged APK contains srt ≥1.5.6 (verify via `strings`/version symbol); a passphrase-protected SRT publish negotiates without downgrade against a 1.5.6 receiver. Tradeoff accepted: owning NDK toolchain maintenance that srtdroid was absorbing.
  Complexity: M

- [ ] P1 — IS-53 Two-tier gating: geometry primary, per-screen calibrated SSIM secondary
  Why: the single global 0.985 SSIM threshold can never pass a corpus whose honest median is 0.87 (cross-app parity against real-Samsung captures), so the strict gate carries no signal per screen. Industry precedent for cross-renderer comparison (Applitools "Layout" match level; the GVT research line) gates on element structure first with a perceptual score second — exactly this harness's geometry differ, promoted.
  Evidence: RESEARCH.md "Competitive Landscape → Screenshot-testing ecosystem"; Applitools match-levels doc; `validation/thresholds.csv` already supports per-screen values (all currently 0.985).
  Depends on: IS-24 (gate must be able to fail at all), IS-22 (artifacts must be fresh)
  Touches: `scripts/run-geometry-validation.ps1`, `scripts/visual_compare.py`, `validation/thresholds.csv`, `scripts/run-visual-validation.ps1`
  Acceptance: geometry pass/fail is the primary per-state verdict; each state's SSIM threshold is derived from N≥5 clean-run captures as `median − k·MAD` and recorded with its derivation date in `thresholds.csv`; a state that regresses structurally fails geometry even when its calibrated SSIM passes.
  Complexity: M

- [ ] P1 — IS-54 Emulator determinism kit for the validation AVD
  Why: run-to-run pixel noise (animations, GPU raster differences, live status-bar clock/battery, warm app state) consumes threshold budget and is the enemy of IS-53's calibration; every practice below is standard in 2026 screenshot-testing CI and absent here.
  Evidence: android-ui-testing cookbook; ReactiveCircus emulator-runner configs; Roborazzi/testify docs — RESEARCH.md "Screenshot-testing ecosystem".
  Depends on: none
  Touches: `scripts/start-headless-emulator.ps1`, `scripts/capture-replica-screen.ps1`, AVD `config.ini`
  Acceptance: AVD pins `hw.lcd.density=450`/`hw.lcd.width=2316`/`hw.lcd.height=1080`; emulator launches with `-gpu swiftshader_indirect -no-boot-anim -noaudio`; the three animation scales are set to 0; `adb shell cmd statusbar` demo mode freezes clock/battery during capture; a post-boot configured snapshot is restored with `-snapshot <name> -no-snapshot-save` per run; `am force-stop` runs between states (closing the warm-state class alongside IS-26); two consecutive full sweeps of an unchanged build produce per-state SSIM deltas < 0.005.
  Complexity: M

### P2

- [ ] P2 — IS-55 OBS WebSocket 5.x client (Kotlin) for remote scene/stream control
  Why: parity-plus-leapfrog at low cost — Moblin ships it, IRL Pro's site still lists it as TODO after 23 stagnant months, and it is the integration path for receiver-side BRB scene switching (pairs with IS-14/IS-56). Protocol 5.7.4 is 9 opcodes over JSON with SHA256-challenge auth; the only JVM client (obs-websocket-java 2.0.0) is low-activity and not coroutine-native, so a ~500-line Kotlin/OkHttp + kotlinx.serialization client is the better long-term asset.
  Evidence: obsproject/obs-websocket protocol.md (RPC 1); irlpro.app TODO list; Moblin README — RESEARCH.md "Competitive Landscape".
  Depends on: IS-02 (surfaces as an engine-adjacent service), IS-07 (server password storage)
  Touches: new `.../data/obsws/`, advanced settings page, quick-panel action
  Acceptance: connects to OBS 28+ (auth + RPC negotiation), switches scenes and reads stream status from the quick panel; disconnect/reconnect is visible and non-fatal; unit tests run against a fake server over the same opcodes.
  Complexity: M

- [ ] P2 — IS-56 On-device BRB: standby slate pushed to viewers on degraded links
  Why: ranked #4 community pain — "automatically switch… without having to have a PC running at home"; NOALBS is OBS-only, closed apps ship "disconnect protection" as a headline. The audited tree already has standby/pause overlay settings (screens 092-094 region), so the UI surface exists; what's missing is a real slate injected into the encoded output when IS-14's degraded state trips.
  Evidence: reddit.com/r/Twitch/comments/1pfj1xv (BRB without a PC); Streamlabs/Twitch "disconnect protection" feature pages — RESEARCH.md "Community signal".
  Depends on: IS-04, IS-13 (slate must be burned into the frame), IS-14 (trigger)
  Touches: `.../engine/overlay/`, standby settings wiring, live-console status treatment
  Acceptance: with the encoder running and sustained low bitrate, the outgoing stream switches to the configured standby overlay and recovers automatically; a recorded receiver-side file shows the slate; behaviour is covered by an IS-10 fake-engine test.
  Complexity: M

- [ ] P2 — IS-57 Low-energy mode and honest battery telemetry
  Why: ranked #3 community pain — phones drain while charging mid-stream ("incredible how fast you can lose battery while charging"), and users choose apps on battery behaviour. Moblin ships low-energy modes; the audited console already shows battery/current/power readouts (IS-45 makes them per-state fixtures; this makes them real).
  Evidence: reddit.com/r/Twitch/comments/1pbuad7 and 1oxb099; Moblin README (battery/low-energy) — RESEARCH.md "Community signal".
  Depends on: IS-04 (real load to measure), IS-09 (telemetry plumbing)
  Touches: `.../ui/live/LiveConsoleScreen.kt` telemetry source, new screen-dim/black-overlay mode, `BatteryManager` readouts
  Acceptance: telemetry shows measured `BATTERY_PROPERTY_CURRENT_NOW`/computed watts (matching the audited readout format); a low-energy toggle dims/blacks the preview while the encode continues; net charge rate visibly improves in low-energy mode on a physical device (documented measurement).
  Complexity: M

- [ ] P2 — IS-58 Expose every physical lens, ultrawide included, at no charge
  Why: ranked #5 community pain — "the only app that works [ultrawide] is Larix, but it requires a $10/month subscription… hoping to find a free alternative". The audited console already renders four lens pills (61°/67°/73°/103°, D008 fixture); backing them with real `CameraManager` physical-camera enumeration converts a fixture into the exact feature the community shops for.
  Evidence: reddit.com/r/Twitch/comments/1kv15h8; audited screens 001/140/141; D008.
  Depends on: IS-04
  Touches: `.../engine/` camera source, `LensSelector` in `LiveConsoleScreen.kt`, video settings camera list
  Acceptance: on a multi-lens physical device, all physical back/front cameras (including ultrawide) are listed with real FoV labels and switch live without stopping the stream (or with the documented reconnect the hardware requires); the AVD fixture remains for validation states.
  Complexity: M

- [ ] P2 — IS-59 Moblink donor role: serve as a bonding modem for Moblin streamers
  Why: the single biggest Android leapfrog surfaced this pass — Moblin streamers use spare phones as extra SRTLA connections via the public Moblink protocol; shipping the donor role makes this app worth installing even for iOS streamers (distribution wedge), and the protocol work is reusable when the app later consumes donors for its own bonding.
  Evidence: github.com/eerimoq/moblink (Android donor app, 23 stars); moblink-rust relay as protocol reference — RESEARCH.md "Competitive Landscape".
  Depends on: IS-06 (runs under a foreground service); independent of IS-17
  Touches: new `.../moblink/` (protocol client + relay loop), settings entry, notification
  Acceptance: a Moblin (iOS) streamer discovers this device as a Moblink relay and streams through its cellular link; per-link traffic is visible in this app; battery/network use is surfaced in the ongoing notification.
  Complexity: L

### P3

- [ ] P3 — IS-60 Scenario-based SRT latency presets
  Why: ecosystem guidance is 2000-3500 ms depending on conditions, and too-low latency silently costs sustainable bitrate; a single default number reproduces the #2 support confusion class. Larix exposes raw latency; nobody ships scenario presets ("stable WiFi" / "bonded cellular" / "marginal signal") with receiver-side implications explained.
  Evidence: BELABOX-ecosystem latency guides (Likely-confidence, RESEARCH.md "Competitive Landscape → BELABOX"); audited connection form fields.
  Depends on: IS-04
  Touches: connection form (`Forms.kt`), engine SRT socket options
  Acceptance: presets set latency + peer-latency with a one-line explanation of what to configure on the receiver; a custom value remains available; the chosen value is applied to the SRT socket (verified in the receiver's stats).
  Complexity: S

- [ ] P3 — IS-61 Large-screen orientation compliance (targetSdk 36)
  Why: Android 16 ignores `android:screenOrientation="landscape"` on displays ≥600 dp, so the forced-landscape console will render portrait on tablets/foldables with no handling today — clipped or broken layout on a real device class, silently.
  Evidence: developer.android.com/about/versions/16/behavior-changes-16 (orientation/resizability restrictions ignored on large screens); `AndroidManifest.xml` `screenOrientation="landscape"`.
  Depends on: none
  Touches: `AndroidManifest.xml`, `MainActivity.kt`, possibly a minimal portrait/letterboxed layout
  Acceptance: on a ≥600 dp device or resizable-emulator profile, the app either letterboxes gracefully or presents a usable layout in portrait; no audited-state validation is affected (AVD is phone-profile).
  Complexity: M

- [ ] P3 — IS-62 Night streaming: Low Light Boost and Android 16 camera keys
  Why: night IRL is a differentiator the audited "Vendor-specific video enhancements" section is shaped for; Android 15 adds Low Light Boost AE (brightens the live preview/stream, not stills), Android 16 adds hybrid AE priority and precise CCT white balance — all Camera2 keys StreamPack can reach and CameraX lags on.
  Evidence: developer.android.com/media/camera/lowlight/low-light-boost-ae; Android 16 features page — RESEARCH.md "Security/platform".
  Depends on: IS-04
  Touches: `.../engine/` camera source, video settings vendor-enhancements section
  Acceptance: on supporting hardware, Low Light Boost is offered and its active state indicated (per `CaptureResult`); on the AVD the rows degrade to the audited disabled treatment.
  Complexity: M

- [ ] P3 — IS-63 Roborazzi self-golden regression layer (JVM) beside the parity harness
  Why: the parity harness answers "does the replica match the audit" but is emulator-bound and slow for day-to-day changes; Roborazzi 1.72.0 (Robolectric Native Graphics, activities/dialogs/landscape supported) can regression-lock this app's own renders on the JVM in seconds, with accessibility checks free. Parity vs the Samsung baselines stays the periodic audit; this layer catches regressions per commit.
  Evidence: github.com/takahirom/roborazzi (v1.72.0, 2026-08-13); RESEARCH.md ecosystem verdict. Caveat recorded there: 2316 px is not representable in whole dp at 450 dpi (823.47 dp), so JVM goldens use the nearest representable canvas — they are self-goldens, never compared to audit baselines.
  Depends on: none
  Touches: `app/build.gradle.kts` (roborazzi plugin), new `app/src/test/.../snapshot/`, CI task
  Acceptance: `verifyRoborazziDebug` covers the live console, one settings page, one dialog and the quick panel; an intentional 2 dp layout change fails it; runtime under 2 minutes on this host.
  Complexity: M

- [ ] P3 — IS-64 Local web remote-control assistant
  Why: solves "the phone is on a gimbal" — Moblin ships a browser assistant (scene/mic/bitrate/zoom + stats push) and BELABOX sells cloud remotes from $5/mo; an on-LAN web page served by the app covers the same need without a cloud.
  Evidence: Moblin README (remote control assistant, v33.1337.0 saved-URL list); belabox.net cloud remotes — RESEARCH.md "Competitive Landscape".
  Depends on: IS-02, IS-06; benefits from IS-55 patterns
  Touches: new `.../remote/` (embedded HTTP/WebSocket server), settings page, QR pairing
  Acceptance: a browser on the same LAN opens the assistant via QR, can start/stop, mute, adjust bitrate and see live stats; access requires a pairing token; feature is off by default.
  Complexity: L

- [ ] P3 — IS-65 Per-connection bitrate presets in the quick panel
  Why: a loved Moblin feature quoted verbatim in community threads ("as many pre-sets as you want") that maps directly onto the existing quick-panel Network tab; cheap once IS-04/IS-12 land.
  Evidence: reddit.com/r/Twitch/comments/1rlyf97 (preset praise); Moblin README — RESEARCH.md "Community signal".
  Depends on: IS-04, IS-12
  Touches: `QuickSettingsPanel.kt` Network tab, settings storage for presets
  Acceptance: user-defined bitrate presets appear as one-tap chips in the Network quick tab and apply live; presets persist (IS-31 rules).
  Complexity: S

- [ ] P3 — IS-66 Toolchain currency notes and pin corrections
  Why: two documented traps for the next builder: Compose BOM 2026.08.00 requires compileSdk 37 + AGP 9.1.1 (do not bump casually from 2026.06.01), and repo docs say "AGP + Gradle 8.14.4" where the truth is Gradle 8.14.4 + AGP 8.13.2 — a future session chasing a nonexistent AGP 8.14.4 wastes time.
  Evidence: Jetpack Compose August 2026 release blog; `replica-app/build.gradle.kts:2` (AGP 8.13.2), `gradle-wrapper.properties` (Gradle 8.14.4).
  Depends on: none
  Touches: `CLAUDE.md`, `replica-app/README.md` toolchain section
  Acceptance: docs state the exact split (Gradle 8.14.4 / AGP 8.13.2 / Kotlin 2.3.21) and carry the BOM-2026.08 upgrade precondition; no doc claims an AGP 8.14.x.
  Complexity: S
