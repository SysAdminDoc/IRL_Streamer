# ROADMAP — IRL Streamer

Single task tracker. Incomplete work only.

Item IDs use the `IS-nn` scheme. Continue numbering from the highest existing ID.

## Research-Driven Additions

Added 2026-08-15 from `RESEARCH.md`. Every item traces to a source recorded there.

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

### P1

### P2

- [ ] P2 — IS-32 Replica reworded audited non-branded copy — undocumented deviations that also break geometry matching
  Category: correctness
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/settings/SettingsCatalog.kt:303` ("Disable Camera2 only if camera resolutions or framerates are unavailable.") and `:304` ("Stream and record front camera as it appears in preview (mirrored).")
  Problem: the audited strings are "Disable Camera2 API only if you have some issues with camera like some resolutions or framerates are not available." and "Stream and record front camera as appears in preview (mirrored)." (no "it"). Neither contains third-party marks, so D014 does not cover the rewording; the rebuild contract reproduces confirmed copy verbatim. The changed strings additionally fail label matching, contributing to state 126's unmatched count.
  Evidence: `app-audit/evidence/ui-xml/126_preferred_camera_api_dialog.xml` contains both audited strings verbatim; geometry 126 lists them under `unmatched_audit_elements` with the replica variants under `replica_only_elements`.
  Fix: restore the audited strings character-for-character (including the original's grammar). Sweep the catalog against the audit XMLs for other silent rewordings of non-branded copy and either restore or add a numbered deviation for each.
  Acceptance: geometry for 126 matches both summary strings; a sweep report lists zero undocumented copy diffs on non-branded strings.
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

- [ ] P2 — IS-39 Fresh clone cannot run any visual comparison: `validation/baseline/` is gitignored and nothing populates it
  Category: reliability
  Where: `.gitignore:21`, `replica-app/scripts/compare-screen.ps1:7-9`, `replica-app/README.md:129`
  Problem: every compare requires `validation/baseline/<id>.png`; the directory is gitignored, no script copies it from `app-audit/evidence/screenshots/`, and the README claims `validation/` contains "immutable baseline copies" — false on a clone. Every documented compare command dies with "Image not found".
  Fix: add a `sync-baselines.ps1` (or a step inside `run-visual-validation.ps1`) that mirrors `app-audit/evidence/screenshots/` → `validation/baseline/` when missing, and correct the README sentence.
  Acceptance: on a clean checkout, `run-full-validation.ps1 -AllScreens` reaches the capture stage without manual baseline copying.
  Confidence: Verified
  Effort: S

### P3

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

- [ ] P3 — IS-48 Validation-state 101's error text lives in a 3.5-second system toast — capture timing can silently lose it
  Category: testing
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/ReplicaApp.kt:41-47` (validationError rendered via `Toast` then immediately consumed), `DebugStateCatalog.kt:86`
  Problem: the "Please input non-empty text/html code" state renders as a system toast that is consumed on first composition and never re-shows. If the screenshot lands after `Toast.LENGTH_LONG` expires (slow emulator, retried capture), the capture silently shows no error and — combined with IS-22 — a stale prior result could stand in. The toast window is also invisible to `uiautomator dump`, so the geometry gate cannot cover this state's defining element at all.
  Fix: render `validationError` as an in-app surface for the debug state (e.g. a transient overlay matching the audited toast bounds) or re-trigger the toast on each injection and have the capture script wait-and-verify; at minimum document that 101's defining pixel region is time-boxed.
  Acceptance: repeated captures of state 101 deterministically contain the error text (verify with 5 consecutive captures).
  Confidence: Needs-repro (mechanism verified in code; flake not yet observed)
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

