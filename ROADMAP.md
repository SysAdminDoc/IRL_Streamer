# ROADMAP — IRL Streamer

Single task tracker. Incomplete work only.

Item IDs use the `IS-nn` scheme. Continue numbering from the highest existing ID.

## Research-Driven Additions

Added 2026-08-15 from `RESEARCH.md`. Every item traces to a source recorded there.

### P1

- [ ] P1 — IS-06 Foreground service with correct Android 14+ service types
  Note (2026-09-04): StreamPack ships `streampack-services` (Apache-2.0), whose `StreamerService` is a `LifecycleService` that owns the streamer, collects `throwableFlow` and posts open/close/error notifications. Its manifest declares FOREGROUND_SERVICE, POST_NOTIFICATIONS and the mediaProjection type only, so a camera streamer must add FOREGROUND_SERVICE_CAMERA and FOREGROUND_SERVICE_MICROPHONE itself. Extend it rather than writing a service from scratch, and take the wake lock from IS-107 with it.
  Why: the audited original runs a camera/mic foreground service (notification ID 101, channel `...channel.foreground_service`, actions Start/Exit). On modern Android an undeclared or mistyped service type is a crash on start, not a warning.
  Evidence: `app-audit/app/package-inventory.md`; deviation D012
  Depends on: IS-05
  Touches: `AndroidManifest.xml`, new `.../service/BroadcastService.kt`
  Acceptance: broadcast survives backgrounding; ongoing notification matches the audited shape; verified on the API 36 emulator and on a physical device.
  Complexity: M

- [ ] P1 — IS-07 Encrypted storage for stream keys and API tokens
  Note (2026-08-29): androidx.security-crypto is deprecated. Use DataStore 1.3.0-alpha07+ `datastore-tink` with an AndroidKeyStore-backed keyset (https://developer.android.com/jetpack/androidx/releases/datastore).
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

### P2

- [ ] P2 — IS-11 Twitch chat ingest
  Why: the audited chat surface is presently empty. Twitch is the only audited platform with a first-party, documented, stable API, so it is the correct first integration.
  Evidence: audited screens 006-011; Twitch4J covers Chat/Helix/EventSub — RESEARCH.md "Sources".
  Touches: new `.../data/chat/`, streamer settings pages
  Acceptance: authenticated connection renders live messages in the audited chat layout, honouring the audited bot/command/platform-icon toggles; failures surface as the audited error treatment rather than a blank panel.
  Complexity: L
  Note (2026-08-15 pass 2): Moblin's ceiling is Twitch/Kick/YouTube/SOOP with 7TV/BTTV/FFZ emotes, TTS and phone-side moderation; sequence Kick (KickLib) after Twitch.

- [ ] P2 — IS-12 Adaptive bitrate (ABR)
  Note (2026-08-29): Constants are readable in belacoder.c: buffer/RTT EWMA 0.99/0.01, drop to min at RTT >= latency/3 or buffer > th3, step down (100 kbps + bitrate/10) every 250 ms at RTT > latency/5 or buffer > th2, step up (30 kbps + bitrate/30) every 500 ms when RTT < min and delta < 0.01, round to 100 kbps, 6 s ACK silence = dead. Second controller: Moblin Fight (200 ms tick, Fast PIF 200/0.9, Slow PIF 500/0.95, RTT clamp 450 ms, min 50 kbps).
  Why: table stakes across every competitor (Larix Premium, StreamPack, RootEncoder, Moblin), and the audited "Bitrate matches resolution" and adaptive-mode settings already imply it.
  Evidence: audited screens 053-056, 064; RESEARCH.md "Competitive Landscape"
  Depends on: IS-04
  Touches: `.../engine/`, video settings page
  Acceptance: encoder bitrate responds to congestion within the audited mode options; behaviour is observable in the log panel.
  Complexity: M
  Note (2026-08-15 pass 2): implement Moblin's four named algorithms — `belabox` (default), `fastIrl`, `slowIrl`, `customIrl` with their exposed tunables (packetsInFlight, pifDiffIncreaseFactor, minimumBitrate) — porting `Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtBelabox.swift` semantics and using Moblin's `AdaptiveBitrateSuite` tests as the cross-check corpus. No Android competitor ships named tunable IRL profiles.

- [ ] P2 — IS-13 Overlay compositor that burns layers into the encoded frame
  Note (2026-09-04): StreamPack still has no GL filter API (issue #52, open since 2022-10-20). RootEncoder 2.8.1 (2026-09-01) rebuilt its filter pipeline in Kotlin (`ViewFilterRender`, `ImageFilterRender`, `TextFilterRender`) and is the reference to read. StreamPack's own seam is `ISurfaceSourceInternal` plus a `VideoSourceFactory`, documented in `docs/use_cases/CustomSources.md`.
  Note (2026-08-29): Start with CameraX `OverlayEffect` (Canvas into the frame pipeline, multi-Preview fix in 1.7.0-alpha03) before any GL layer; browser widgets via low-fps PixelCopy of an offscreen WebView (no WebGL). RootEncoder's GL filter pipeline is the fallback reference.
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

- [ ] P2 — IS-16 Accessibility pass over net-new surfaces
  Why: D010 records that the reconstruction deliberately improved on the original's unlabeled, sub-48 dp controls. Permission screens, chat and any engine error states are new surfaces with no audit evidence and no accessibility guarantee.
  Evidence: `app-audit/audit-summary.md` P1 accessibility findings; deviation D010
  Depends on: IS-05, IS-11
  Touches: new permission/chat/error composables
  Acceptance: every net-new interactive element has a content description, role and state description, and a 48 dp semantic target; verified with Accessibility Scanner or an equivalent automated check.
  Complexity: S

### P3

- [ ] P3 — IS-17 SRTLA bonding client, ported from BELABOX `srtla_send` via the NDK
  Note (2026-09-04): two open-source Android SRTLA senders now exist and can be read for behaviour, though neither may be copied: Bond Bunny (AGPL-3.0, https://github.com/dimadesu/bond-bunny, v1.16.0 2026-07-30) and PocketBond (https://github.com/romestylez/pocketBond). Both take SRT on a local port and re-spray to an SRTLA receiver, which is a shipping architecture worth considering over an in-process port. The MIT reference to port from is still `irlserver/srtla_send`.
  Note (2026-08-29): Do not port BELABOX srtla (AGPL-3.0). irlserver/srtla_send (Rust, MIT, v3.0.0) is a full SRTLA v2 sender with NAK-decay link scoring, stalled-link deselection and DNS re-home; port that to Kotlin. Depends on IS-77 for per-link sockets.
  Why: the headline audited capability, and no open-source Android SRTLA client surfaced. Build on the reference implementation rather than reimplementing the protocol: `srtla_send` is C, and Android is Linux, so the congestion/window/scheduling core compiles largely as-is.
  Evidence: BELABOX/srtla README — `srtla_send [local_port] [receiver_ip] [receiver_port] [ips_file]`, "keeps track of the number of packets in flight ... together with a dynamic window size that tracks the capacity of each link", traffic "balanced through each link proportionally to its capacity". Moblin (iOS) remains a behavioural cross-check.
  Depends on: IS-04
  Touches: new `app/src/main/cpp/` (srtla core + JNI), `AndroidManifest.xml` (`CHANGE_NETWORK_STATE`), connection settings, bonding page (audited screens 016-020)
  Acceptance: two simultaneous links aggregate to a single stream at a standard SRTLA receiver (go-irl or BELABOX Cloud), per-link weights honour the audited controls, and dropping one link degrades rather than ends the broadcast.
  Complexity: L
  Note — the one part that does NOT port: `srtla_send` selects links with `bind()` to a source IP and requires OS source routing, which Android cannot configure unrooted. Replace that layer with Android's own multi-network API — `ConnectivityManager.requestNetwork()` to hold a cellular link up while WiFi is default, then `android_setsocknetwork()` (`<android/multinetwork.h>`) to pin each UDP socket to its `Network`. Budget the effort here, not in the protocol.
  Note (2026-08-15 pass 2): platform constraints now verified — `CHANGE_NETWORK_STATE` is install-time; hard cap 100 outstanding network requests per UID (register once, reuse); the platform may swap the satisfying network, so rebind sockets in `onAvailable` rather than caching `Network` handles; keep requests alive inside the foreground service. The `.so` must be 16 KB-page-aligned (NDK r28+, Play deadline 2027-02-01) and must link the IS-52 libsrt build, not srtdroid's bundled copy.

- [ ] P3 — IS-18 Thermal and sustained-load behaviour
  Note (2026-09-04): the signal to act on is `PowerManager.getCurrentThermalStatus()` plus `addThermalStatusListener`, which is the only supported thermal source on Android; pair the backoff with IS-108 so a throttle can drop resolution as well as bitrate.
  Note (2026-08-29): API: PowerManager.getThermalHeadroom(30) polled every 10 s (more often returns NaN), act at > 0.85 (fps 60->30, dim screen), hard cut at > 0.95 (720p, drop bonding legs); getThermalHeadroomThresholds() on API 35+. Show skin-temp and charge wattage on the HUD: S24U drops to 5 W charging when hot (https://start.irlstreami.ng/android-devices/irl-pro). StreamCaster-android uses a 60 s cooldown before stepping back up.
  Why: sustained outdoor encoding is a top IRL failure mode, and the audit's ~275 MB PSS / 2.01% jank baseline was measured with no real encode running.
  Evidence: `app-audit/audit-summary.md` performance findings; Reddit IRL setup threads
  Depends on: IS-04
  Touches: `.../engine/`, new thermal listener
  Acceptance: `PowerManager` thermal status is observed and drives a documented degradation ladder rather than an abrupt stop.
  Complexity: M

- [ ] P3 — IS-19 Settings import/export over the audited `larix://` deep link and QR path
  Note (2026-08-29): Today the import dialog is prefilled `irlstreamer://` and parsed nowhere (SettingsCatalog.kt:296-301); export is a toast. StreamCaster-android's QR endpoint payload is a versioned-JSON reference.
  Why: audited screens 116-118 define the flow, and it is how the original moves configuration between devices — but it must not exfiltrate secrets.
  Evidence: audited screens 116-118; `app-audit/app/components-and-intents.md` (payload grammar UNKNOWN beyond the scheme prefix)
  Depends on: IS-07
  Touches: `AndroidManifest.xml` intent filter, import/export settings page, secret storage
  Acceptance: a settings payload round-trips between two installs; secrets are excluded or re-encrypted; the deep-link grammar is this project's own and documented as a deviation, since the original's is unknown.
  Complexity: M

- [ ] P3 — IS-20 Distribution and update channel decision
  Note (2026-09-04): the channel question is settled and this item should close as written. Distribution is signed APKs on GitHub Releases only; there is no store decision to make. The remaining work is the in-app update check, now tracked as IS-113.
  Why: the app ships with a repo-owned self-signed key and no update path, so no user can receive a fix.
  Evidence: `replica-app/README.md` signing note; RESEARCH.md "Product Map"
  Touches: release scripts, `README.md`
  Acceptance: a decision is recorded (Play, GitHub Releases with an in-app update check, or explicitly none) and the release process matches it.
  Complexity: S
  Note (2026-08-15 pass 2): AGPL-3.0 on Play is verified viable (Signal-Android precedent; Google's AGPL ban is internal-use policy, not Play policy). F-Droid accepts AGPL; take the F-Droid signing key initially — reproducible builds with NDK code is the documented hard path. New Play apps must target API 36 from 2026-08-31 (already satisfied).

- [ ] P3 — IS-21 Localisation and RTL readiness
  Note (2026-08-29): Measured 2026-08-29: strings.xml has 2 entries, ~632 hardcoded literals across ui/, 0 stringResource calls.
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

### P3

- [ ] P3 — IS-48 Validation-state 101's error text lives in a 3.5-second system toast — capture timing can silently lose it
  Category: testing
  Where: `replica-app/app/src/main/java/com/irlstreamer/reconstruction/ui/ReplicaApp.kt:41-47` (validationError rendered via `Toast` then immediately consumed), `DebugStateCatalog.kt:86`
  Problem: the "Please input non-empty text/html code" state renders as a system toast that is consumed on first composition and never re-shows. If the screenshot lands after `Toast.LENGTH_LONG` expires (slow emulator, retried capture), the capture silently shows no error and — combined with IS-22 — a stale prior result could stand in. The toast window is also invisible to `uiautomator dump`, so the geometry gate cannot cover this state's defining element at all.
  Fix: render `validationError` as an in-app surface for the debug state (e.g. a transient overlay matching the audited toast bounds) or re-trigger the toast on each injection and have the capture script wait-and-verify; at minimum document that 101's defining pixel region is time-boxed.
  Acceptance: repeated captures of state 101 deterministically contain the error text (verify with 5 consecutive captures).
  Confidence: Needs-repro (mechanism verified in code; flake not yet observed)
  Effort: S

## Findings from the 2026-08-15 drain

- [ ] P3 — IS-68 Quick-panel switches are checkable but unnamed in the accessibility tree
  Why: the rows are now `toggleable` with `Role.Switch`, so the tree exposes a checkable node spanning the whole row (verified), but that node carries no name — the label stays a sibling `Text`. A screen reader announces a switch with no indication of what it switches.
  Evidence: `adb shell uiautomator dump` of `135_live_console_display` shows three `checkable="true"` nodes at `[1135,202][1989,278]` and below, each with empty `text` and `content-desc`, while `Grid`, `Safe margins` and `Lock Screen` appear as separate `TextView` nodes. Tried and rejected during the 2026-08-15 drain: `Modifier.semantics(mergeDescendants = true) {}` after `toggleable`, `Modifier.semantics { contentDescription = title }` after `toggleable`, and the same before it — none changed the dumped node.
  Touches: `.../ui/live/QuickSettingsPanel.kt` (`QuickToggle`)
  Acceptance: the checkable node for each quick toggle reports its label (as `text` or `content-desc`) in a `uiautomator` dump, and toggling it still works.
  Complexity: S
  Note: verify with a Compose `SemanticsNodeInteraction` assertion rather than the dump alone — the dump is the accessibility tree as exported, which may differ from Compose's merged tree.

- [ ] P2 — IS-67 Quick settings panel covers the console telemetry block
  Why: on every panel-open state (130-138) the replica's telemetry readings disappear behind the quick panel, while the audit hierarchy for the same states lists all three readings as present. The panel is therefore too wide, too far left, or drawn at the wrong z-order relative to the telemetry.
  Evidence: replica hierarchy for `132_live_console_network` contains `-512 mA` but not `-2106 mW` or `33.3 °C`; the audit dump for the same state contains all three. Panel is placed at `maxWidth - 428.1.dp` with width 320 dp (`LiveConsoleScreen.kt`), spanning to `maxWidth - 108 dp`; the telemetry block sits at `maxWidth - 214.14.dp` with width 100 dp, entirely inside that span. Non-panel states (001, 141) render all three readings correctly.
  Touches: `.../ui/live/LiveConsoleScreen.kt` (`TelemetryBlock`, `QuickSettingsPanel` placement)
  Acceptance: geometry for 132 and 138 matches the audited mW and temperature readings; the panel and telemetry do not overlap in the captured hierarchy.
  Complexity: S

## Research-Driven Additions — 2026-08-15 (pass 2)

From `RESEARCH.md` second pass (screenshot-testing ecosystem, competitor matrices, platform/CVE sweep, community signal). Every item traces to a source recorded there.

### P1

- [ ] P1 — IS-52 Build libsrt 1.5.6+ and OpenSSL 3.5.x from source; do not ship srtdroid's bundled SRT stack
  Note (2026-09-04): srtdroid 1.10.0 (2026-08-26) carries srt 1.5.7 but bundles OpenSSL 3.5.1, five patch releases behind 3.5.7 (2026-06-09), which fixed 15 CVEs including CVE-2026-45447 (use-after-free in `PKCS7_verify`). StreamPack itself still pins srtdroid 1.9.5 (srt 1.5.4, vulnerable to CVE-2026-55868/55869), so adding `streampack-srt` without a resolution rule ships both problems. Whether libsrt reaches the PKCS#7 path decides if an owned OpenSSL build is needed or only a srtdroid bump.
  Note (2026-08-29): not yet reachable. v0.4.0 publishes over RTMP only and pulls no srtdroid, so no libsrt ships today. This item becomes live the moment SRT or SRTLA is added; srtdroid 1.10.0 (2026-08-26) bundles srt 1.5.7 and is the version to depend on.
  Note (2026-08-29): Downgraded: srtdroid 1.10.0 already ships srt 1.5.7 + OpenSSL 3.5.1. Override the dependency in IS-04 and verify 16 KB alignment with llvm-readelf. Build from source only if a TLS path needs OpenSSL 3.5.8/4.0.2.
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

### P2

- [ ] P2 — IS-55 OBS WebSocket 5.x client (Kotlin) for remote scene/stream control
  Why: parity-plus-leapfrog at low cost — Moblin ships it, IRL Pro's site still lists it as TODO after 23 stagnant months, and it is the integration path for receiver-side BRB scene switching (pairs with IS-14/IS-56). Protocol 5.7.4 is 9 opcodes over JSON with SHA256-challenge auth; the only JVM client (obs-websocket-java 2.0.0) is low-activity and not coroutine-native, so a ~500-line Kotlin/OkHttp + kotlinx.serialization client is the better long-term asset.
  Evidence: obsproject/obs-websocket protocol.md (RPC 1); irlpro.app TODO list; Moblin README — RESEARCH.md "Competitive Landscape".
  Depends on: IS-02 (surfaces as an engine-adjacent service), IS-07 (server password storage)
  Touches: new `.../data/obsws/`, advanced settings page, quick-panel action
  Acceptance: connects to OBS 28+ (auth + RPC negotiation), switches scenes and reads stream status from the quick panel; disconnect/reconnect is visible and non-fatal; unit tests run against a fake server over the same opcodes.
  Complexity: M

- [ ] P2 — IS-56 On-device BRB: standby slate pushed to viewers on degraded links
  Note (2026-09-04): StreamPack has the mechanism already. `BitmapSource` implements `AbstractPreviewableSource` and main carries an unreleased commit from 2026-08-29 adding a public method to set a Bitmap as the video source, so the slate is a source swap rather than a new pipeline. Scope note: Twitch shipped native Disconnect Protection on 2026-03-09 holding a dropped stream for up to 90 seconds, so the value here is now for Kick, YouTube and self-hosted destinations.
  Note (2026-08-29): NOALBS defaults to match: low 500 kbps, offline 450 kbps, RTT 1000 ms degraded / 3500 ms offline, 5 consecutive checks; emit a `!privacy`-equivalent flag so servers switch cleanly.
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
  Note (2026-09-04): reconsider or close. A first-party Android Moblink relay already exists (https://github.com/eerimoq/Moblink, MIT, Kotlin, pushed 2026-09-05), with a Rust reimplementation for non-phone donors (https://github.com/datagutt/moblink-rust). Building a second donor app duplicates maintained work; the streamer side (IS-76) is the half that still has no Android implementation here.
  Note (2026-08-29): Pair with IS-76 (streamer side). Moblink protocol: mDNS discovery, WebSocket hello/identify challenge-response, startTunnel, UDP forwarding bound to the relay interface (https://github.com/datagutt/moblink-rust).
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

- [ ] P3 — IS-62 Night streaming: Low Light Boost and Android 16 camera keys
  Note (2026-08-29): CameraX 1.5 exposes isLowLightBoostSupported/enableLowLightBoostAsync; 1.7.0-alpha02 adds getNightModeIndicator for auto-toggle. Moblin #409: LLB forces 60 fps on some devices, guard the frame-rate range.
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


## Product Assessment — 2026-08-29

Added after v0.3.0 shipped the CameraX preview. The app is a pixel-faithful replica with a live viewfinder; nothing leaves the phone yet. The items below are what that assessment turned up that the existing entries do not already cover, plus a suggested order through the backlog.

### Suggested order

1. IS-06 next. Camera, microphone, and RTMP publishing are live, but a stream still needs a foreground service to survive screen-off.
2. IS-07 before any connection form can save a key.
3. IS-09, IS-12, IS-14/IS-56, IS-08: what makes a cellular stream survivable.
4. IS-58: the ultrawide-for-free pitch the research found. Small job once IS-04 is in.
5. IS-17 + IS-52: bonding only after single-link streaming is solid.
6. IS-53: demote SSIM to advisory now; real camera frames make most live states incomparable anyway.

### P1

- [ ] P1 — IS-70 Retire fixture decoys as each real subsystem lands
  Note (2026-09-04): the resolution and FPS decoys are now split out as IS-108 with their own acceptance; close that case here when it lands.
  Why: fixture telemetry (mA/mW/°C), the hardcoded "30 fps" pill, the "Snapshot simulation complete" toast, "local camera fixture opened" in the LOG tab, and the SIMULATED capture states are misleading now that the console shows a real camera. Left in place they become permanent lies in a shipping app.
  Evidence: `TelemetryBlock` and `FpsPill` in `LiveConsoleScreen.kt`; `QuickSettingsPanel.kt` LOG tab text; `SimulatedBroadcastEngine.IdleStatistics`.
  Depends on: IS-04 (engine), IS-09 (telemetry), IS-74 (fps)
  Touches: `LiveConsoleScreen.kt`, `QuickSettingsPanel.kt`, `SimulatedBroadcastEngine.kt`, `DebugStateCatalog.kt`
  Acceptance: outside the harness no console reading is a constant; each fixture value is either measured or the control is hidden with a stated reason.
  Complexity: S per subsystem, tracked here so none is forgotten

### P2

- [ ] P2 — IS-71 Snapshot button captures a still through CameraX `ImageCapture`
  Why: the audited snapshot control exists and does nothing. With a bound preview this is one more use case and a MediaStore write.
  Touches: `CameraPreview.kt` (bind `ImageCapture` beside `Preview`), `LiveConsoleScreen.kt` snapshot handler, `WRITE`-free MediaStore insert
  Acceptance: tapping the snapshot control saves a JPEG to Pictures/IRL Streamer and toasts the filename; failure toasts the reason; the debug harness state is unchanged.
  Complexity: S

- [ ] P2 — IS-72 Torch, zoom and exposure compensation act on the camera
  Why: the quick panel's Torch switch and Exposure/Zoom sliders are `remember` state only. `CameraControl.enableTorch`, `setZoomRatio` and `setExposureCompensationIndex` make all three real, and `CameraInfo` supplies the real ranges instead of the hardcoded 1..8x and -2..2.
  Touches: `CameraPreview.kt` (expose the bound `Camera`), `QuickSettingsPanel.kt` `CameraQuickSettings`, a small camera-controls holder on the view model
  Acceptance: torch lights the LED, zoom and exposure visibly change the preview, slider bounds come from `CameraInfo`, controls are disabled with a reason when the lens lacks the capability (front torch).
  Complexity: S

- [ ] P2 — IS-73 Lens pills open the physical lens they name
  Why: v0.3.0 maps ids 1/3 to front and 0/2 to back facing only; 73° and 103° both open the default back camera. Stepping stone to IS-58 that needs no engine work.
  Evidence: `CameraPreview.kt` selector logic; `LensSelector` labels in `LiveConsoleScreen.kt`
  Touches: `CameraPreview.kt` (enumerate `cameraProvider.availableCameraInfos`, match by facing and FoV from `SENSOR_INFO_PHYSICAL_SIZE` / focal length), lens pill labels
  Acceptance: on the S25 Ultra the 103° pill shows the ultrawide and 73° the main; on a two-camera device the unavailable pills are dimmed.
  Complexity: M

### P3

- [ ] P3 — IS-74 FPS pill reads the measured preview frame rate
  Why: "30 fps" is a string constant. A frame-timestamp counter on the preview surface (or `Preview.setTargetFrameRate` plus an `ImageAnalysis` tick) gives an honest number and will later reflect the encoder.
  Touches: `CameraPreview.kt`, `FpsPill` in `LiveConsoleScreen.kt`
  Acceptance: covering the lens or switching to a 60 fps-capable lens changes the reading; the harness states still render the audited "30 fps".
  Complexity: S

## Research-Driven Additions — 2026-08-29

From `RESEARCH.md` (2026-08-29). Every item traces to a source recorded there. Existing items that this pass modifies carry an inline `Note (2026-08-29)`.

### P0

### P1

- [ ] P1 — IS-77 Multi-network hold and per-link socket binding layer
  Why: SRTLA (IS-17), Moblink (IS-59/IS-76) and per-link telemetry (IS-09) all need cellular, WiFi and USB Ethernet held simultaneously with UDP sockets bound per link; nothing in the app does this and the platform gives no per-link RTT.
  Evidence: `ConnectivityManager.requestNetwork` per transport + `Network.bindSocket`, 100-request cap per UID, sockets die on loss (https://developer.android.com/develop/connectivity/network-ops/reading-network-state); MPTCP unavailable on stock Android (https://github.com/mptcp-nexus/android).
  Touches: new `.../net/LinkManager.kt` (one request per transport, `onAvailable`/`onLost` rebind, `getLinkUpstreamBandwidthKbps` hints), `AndroidManifest.xml` (`CHANGE_NETWORK_STATE`, `ACCESS_NETWORK_STATE`), `engine/BroadcastEngine.kt` `LinkStatistics`
  Acceptance: with WiFi and cellular both up, the Network quick tab lists both links with a live bound-socket RTT measured by an app keepalive; pulling one link marks it lost within 3 s and re-binds when it returns; unit test covers the request-cap guard.
  Complexity: M

- [ ] P1 — IS-76 Moblink streamer side: accept links donated by phones running the Moblink relay app
  Why: existing Android Moblink relays (on Play) donate SRTLA legs to Moblin today; speaking the streamer side makes every spare Android phone a bonding modem for this app, which IRLwhatever is the only Android app to do.
  Evidence: protocol in https://github.com/datagutt/moblink-rust (mDNS, WebSocket hello/identify challenge-response, startTunnel, UDP forwarding); relay app https://github.com/eerimoq/Moblink; https://irlwhatever.com/.
  Depends on: IS-77, IS-17
  Touches: new `.../bonding/MoblinkStreamer.kt` (mDNS via `NsdManager`, OkHttp WebSocket, per-relay UDP tunnel registered as an SRTLA link), Bonding settings page (relay password, discovered relays list)
  Acceptance: a phone running the stock Moblink app on the same LAN appears in Bonding settings, is accepted with the shared password, and its link carries packets visible in the per-link stats; disconnecting it removes the link without dropping the stream.
  Complexity: L

### P2

- [ ] P2 — IS-79 Portrait 9:16 output with rotated overlays
  Why: IRL Pro's most-requested feature in Play reviews (rotation greyed out) and Moblin #88 (mobile viewers get black bars); Kick Clips and TikTok are vertical.
  Evidence: https://play.google.com/store/apps/details?id=app.irlpro.android; https://github.com/eerimoq/moblin/issues/88.
  Depends on: IS-04, IS-13
  Touches: Video settings (orientation choice), encoder configuration (1080x1920), overlay layout coordinates, the forced-landscape console (keep the console landscape, rotate the encoded frame)
  Acceptance: a 9:16 stream reaches MediaMTX with correct orientation metadata and overlays positioned for portrait; the console UI stays landscape; audited 16:9 behaviour is unchanged.
  Complexity: M

- [ ] P2 — IS-80 Kick chat ingest
  Note (2026-09-04): a phone can read Kick chat directly after all. Kick's official API is webhook-based and unusable from a handset, but live chat is delivered over a public Pusher WebSocket (`chatroom.{id}`, event `App\Events\ChatMessageEvent`) that needs no authentication, which is what KickLib and the other community clients use. The friction is a Cloudflare-protected channel-id lookup before the handshake, not the socket itself.
  Why: IS-11 covers Twitch only; Kick is the second audited platform and Kick's own app cannot reply to chat.
  Evidence: Kick official events are webhook-only with a public URL and `webhook` scope (https://github.com/KickEngineering/KickDevDocs, issue #20 for websocket); public Pusher `App\Events\ChatMessageEvent` for read-only (Likely, https://lib.rs/crates/kick-api); https://help.kick.com/en/articles/15159836-getting-started-with-the-kick-go-live-app.
  Depends on: IS-11 (shared chat model)
  Touches: new `.../chat/KickChatSource.kt` (Pusher WebSocket read path, OAuth 2.1 PKCE for send), receiver-side webhook relay documented in IS-88
  Acceptance: Kick messages appear in the chat overlay within 2 s of posting; sending a reply from the quick panel posts to the channel; loss of the socket reconnects with backoff and shows a degraded indicator.
  Complexity: M

- [ ] P2 — IS-81 Offline text-to-speech for chat
  Why: streamers monitor chat by ear while walking; IRL+ Chat and IRLwhatever exist largely to fill this gap on Android.
  Evidence: https://www.irlplus.uk/chat/; https://irlwhatever.com/; offline engine https://github.com/k2-fsa/sherpa-onnx (Piper/Kokoro voices, Android TextToSpeechService wrapper on F-Droid).
  Depends on: IS-11
  Touches: new `.../chat/ChatTts.kt` (platform `TextToSpeech` first, sherpa-onnx as an optional download), Audio quick tab (per-user mute, skip, rate), audio mixer so TTS is local-only not on-stream
  Acceptance: new messages are read aloud with username prefix; Skip stops the current utterance; per-user mute persists; TTS audio is absent from the encoded stream.
  Complexity: M

- [ ] P2 — IS-83 Audio input picker with Bluetooth, USB-C and wired sources
  Note (2026-09-04): StreamPack cannot select an input device yet — issue #190 (`Set AudioRecord setPreferredDevice`) has been open since 2025-03-18. Either contribute it upstream or reach the `AudioRecord` through a custom audio source; do not plan on a library API arriving.
  Why: mic source is currently implicit; Kick app reviews report Bluetooth mics failing, guides push wired TRRS, and StreamPack #190 asks for `setPreferredDevice`.
  Evidence: https://github.com/ThibaultBee/StreamPack/issues/190; https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-recording; https://play.google.com/store/apps/details?id=com.kick.streaming.
  Depends on: IS-04
  Touches: Audio settings (device list from `AudioManager.getDevices(GET_DEVICES_INPUTS)`), engine audio source (`AudioRecord.setPreferredDevice`, `setCommunicationDevice` for BT SCO/LE Audio), Audio quick tab
  Acceptance: plugging a USB-C or pairing a BT mic lists it; selecting it routes the stream audio within 1 s; hot-unplug falls back to the built-in mic with a toast; selection persists.
  Complexity: M

- [ ] P2 — IS-86 CameraX `SessionConfig` feature groups and Camera2Interop DSL migration
  Note (2026-08-29): reconsider before starting. CameraX was removed in v0.4.0 when StreamPack took over capture, so `SessionConfig` feature groups are no longer reachable. The equivalent work is now Camera2Interop through StreamPack's camera source, or it should be dropped.
  Why: 60 fps, preview/video stabilization and HDR are now a declarative feature group with a support pre-check; the legacy `Camera2Interop.Extender` is deprecated in 1.7.0-alpha03 and IS-58/IS-62 will need interop.
  Evidence: CameraX 1.5.0/1.6.0/1.7.0-alpha03 release notes (https://developer.android.com/jetpack/androidx/releases/camera).
  Touches: `ui/live/CameraPreview.kt` (bind via `SessionConfig` with `setPreferredFeatureGroup`), Video settings (stabilization and fps rows backed by `isSessionConfigSupported`)
  Acceptance: on the S25 Ultra the Video page offers only combinations the device reports supported; enabling stabilization visibly changes the preview; unsupported combinations are greyed with the reason.
  Complexity: M

- [ ] P2 — IS-87 Multistream: second output via StreamPack dual pipeline
  Why: every closed app paywalls multistream (PRISM Plus, Streamlabs Ultra, Larix 3+ connections); StreamPack's `DualStreamer`/`StreamerPipeline` supports multiple outputs from one encode.
  Evidence: https://guide.prismlive.com/mobile/announcement/general/upcoming-subscription-model-for-prism-live-studio; https://softvelum.com/larix/premium/; https://github.com/ThibaultBee/StreamPack.
  Depends on: IS-04
  Touches: Connections settings (enable more than one active), `engine/` (multi-output pipeline), Network quick tab (per-output status)
  Acceptance: two RTMP destinations receive the same encode simultaneously; one failing does not stop the other; per-output state is visible.
  Complexity: M

- [ ] P2 — IS-88 Self-host receiver guide and README troubleshooting section
  Why: IRL Pro's moat is a hosted relay; the README has no troubleshooting or receiver guidance, and users repeatedly do not know SRTLA needs a server-side relay.
  Evidence: https://github.com/e04/go-irl; https://github.com/irlserver/irl-srt-server; MediaMTX SRTLA receiver PR https://github.com/bluenviron/mediamtx/pull/5811; https://github.com/irlhost/awesome-irl-streaming.
  Touches: `README.md` (Troubleshooting), new `replica-app/docs/receivers.md` with docker recipes for MediaMTX (SRT/RTMP) and go-irl or irl-srt-server (SRTLA), Kick webhook relay note for IS-80
  Acceptance: a reader can bring up an SRT receiver in one docker command and see the app's stream in OBS; troubleshooting covers permissions, black preview, no connection, and thermal.
  Complexity: S

- [ ] P2 — IS-89 Local transport loopback rig: MediaMTX in Docker plus emulator video-file camera
  Why: no test exercises a real encode or publish; the pixel harness cannot. A local loopback proves IS-04 and every transport item without a live platform key.
  Evidence: emulator `-camera-back videofile:clip.mp4` (https://developer.android.com/studio/run/emulator-commandline); MediaMTX REST `/v3/paths/list` (https://github.com/bluenviron/mediamtx/releases).
  Depends on: IS-04
  Touches: `scripts/run-loopback.ps1` (docker compose up, emulator with videofile camera, start broadcast via debug intent, assert path present and bitrate > 0), `test-data/fixtures/` clip
  Acceptance: the script exits non-zero when the stream does not reach MediaMTX within 20 s and zero when it does; runs on the headless AVD only.
  Complexity: M

### P3

- [ ] P3 — IS-85 USB (UVC) camera and incoming RTMP as video sources
  Note (2026-09-04): StreamPack issue #186 (USB/OTG capture input) is still open, and it is the highest-engagement request on that tracker. LifeStreamer already ships UVC ingest on StreamPack (https://github.com/dimadesu/LifeStreamer), so it is achievable outside the library.
  Why: IRL Pro reviews: "cannot detect third-party cameras"; Moblin does DJI/GoPro ingest; IRLwhatever accepts UVC and RTMP in.
  Evidence: https://play.google.com/store/apps/details?id=app.irlpro.android; https://github.com/eerimoq/moblin; https://github.com/ThibaultBee/StreamPack/issues/186.
  Depends on: IS-04, IS-13
  Touches: engine video source abstraction, new UVC source (libuvc or `UsbManager` + MediaCodec decode), RTMP listener source, Video settings source picker
  Acceptance: a UVC webcam appears as a lens option and streams; a DJI/GoPro pushing RTMP to the phone's IP appears as a source and auto-resumes after the camera restarts.
  Complexity: XL

- [ ] P3 — IS-90 Instant replay clip trigger
  Why: Moblin ships replay from the watch; CameraFi paywalls it; IRL Pro lacks it.
  Evidence: https://github.com/eerimoq/moblin; https://start.irlstreami.ng/android-devices/irl-pro.
  Depends on: IS-08, IS-13
  Touches: rolling encoded-buffer (last 30 s) in the recording path, console button, overlay playback of the clip into the stream
  Acceptance: tapping Replay plays the last 15 s into the outgoing stream with a "REPLAY" overlay and returns to live; local clip saved to Movies/IRL Streamer.
  Complexity: L

- [ ] P3 — IS-91 Twitch Enhanced Broadcasting via Enhanced RTMP multitrack
  Note (2026-09-04): Twitch Dual Format (multi-codec in one Enhanced RTMP connection) went generally available in June 2026 and 1440p HEVC is now GA for Partners and Affiliates; AV1 and native 4K remain inside the beta community. The open question is unchanged — every documented multitrack example is a desktop encoder, and whether Twitch accepts it from a third-party mobile client is unverified.
  Why: no mobile app implements it natively; 1440p HEVC for Partners/Affiliates since 2026-06.
  Evidence: IVS multitrack contract (https://docs.aws.amazon.com/ivs/latest/LowLatencyUserGuide/multitrack-video-sw-integration.html); https://streamrun.com/docs/twitch-enhanced-broadcasting. Whether Twitch whitelists third-party mobile clients is unverified (RESEARCH.md Open Question 2).
  Depends on: IS-04, IS-87
  Touches: RTMP output (E-RTMP v2 multitrack packets, `?clientConfigId=`), `GetClientConfiguration` call, aligned IDRs across tracks
  Acceptance: Twitch dashboard shows multiple qualities from the phone; falls back to single-track when the config call is refused.
  Complexity: XL

- [ ] P3 — IS-93 BLE heart-rate text variable for overlays
  Why: heart rate is a standard Moblin text-widget variable; Health Connect cannot stream live values.
  Evidence: https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data; GATT Heart Rate Service 0x180D reference https://github.com/lmarceau/heart-rate-monitor-ble.
  Depends on: IS-13
  Touches: new `.../sensors/BleHeartRate.kt` (`BLUETOOTH_CONNECT`/`SCAN`, 0x2A37 notifications), text overlay variable `{hr}`
  Acceptance: a paired chest strap updates `{hr}` in a text overlay within 2 s; disconnect shows "--" and reconnects automatically.
  Complexity: S

- [ ] P3 — IS-94 Keyframe duplication across bonding links under loss
  Why: Speedify's streaming mode duplicates priority packets across links when loss appears; srtla_send exposes keyframe-priority hints to the bonding layer.
  Evidence: https://support.speedify.com/article/870-bonding-mode; https://github.com/irlserver/srtla_send.
  Depends on: IS-17
  Touches: SRTLA sender (IDR-tagged packets sent on the two best links when loss > threshold), Bonding settings toggle
  Acceptance: with 5% induced loss on one link, keyframe delivery time to the receiver improves measurably and total bandwidth rises by no more than the keyframe share.
  Complexity: M

- [ ] P3 — IS-95 Long-stream SRT latency creep guard and HEVC SEI timecode correctness
  Why: two open Moblin defects the same pipeline will hit: G2G latency grows over hours (#414) and HEVC SEI timecode payload size mis-accounts emulation-prevention bytes (#426).
  Evidence: https://github.com/eerimoq/moblin/issues/414; https://github.com/eerimoq/moblin/issues/426.
  Depends on: IS-04
  Touches: engine SRT stats loop (periodic latency re-measure, drop-and-resync when send buffer stays above target), SEI writer (when SEI timing lands via StreamPack, verify payload_size after emulation-prevention insertion)
  Acceptance: a 4-hour MediaMTX loopback shows end-to-end latency within 10% of the configured SRT latency at hour 4; SEI payloads parse in ffprobe without warnings.
  Complexity: M

- [ ] P3 — IS-96 Toolchain migration as one unit: compileSdk 37, AGP 9.x built-in Kotlin, Gradle 9.5, Compose BOM 2026.08
  Note (2026-09-04): the target moved. Compose 1.12.0 (BOM 2026.08.00, 2026-08-14) requires compileSdk 37 and AGP 9, not AGP 9.1.1 specifically. Current stable is AGP 9.4.0 (September 2026), which needs Gradle 9.6.0 and JDK 17 and tops out at API level 37; Android 17 / API 37 is itself stable. Vivid, a comparable Android streaming app, already builds at targetSdk 37 on Gradle 9.3.1, so the migration is routine rather than exploratory. Bundle IS-117 with it: at targetSdk 37 the ACCESS_LOCAL_NETWORK runtime permission becomes mandatory for all LAN traffic. Consider introducing `gradle/libs.versions.toml` first — versions are inline literals in `app/build.gradle.kts` today, which makes staging this harder than it needs to be.
  Why: the next Compose BOM forces compileSdk 37 and AGP 9.1.1+, AGP 9 requires dropping `kotlin-android`, and AGP 9.3 needs Gradle 9.5; doing these piecemeal breaks the build at every step.
  Evidence: https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html; https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/; https://developer.android.com/build/releases/agp-9-3-0-release-notes.
  Touches: `replica-app/build.gradle.kts`, `app/build.gradle.kts` (remove `org.jetbrains.kotlin.android`, keep the compose plugin), `gradle/wrapper/gradle-wrapper.properties`, working-note pins, `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` review (API 37 removes the opt-out)
  Acceptance: release build, unit tests and the 145-state capture pass on the new toolchain; working-note pins updated; no `kotlin-android` plugin remains.
  Complexity: L

- [ ] P3 — IS-97 Chat moderation actions from the quick panel
  Why: IRL Pro chat is read-only; a reviewer monitored chat from a second phone for five hours; Moblin ships timeout/ban.
  Evidence: https://start.irlstreami.ng/android-devices/irl-pro; https://github.com/eerimoq/moblin.
  Depends on: IS-11, IS-80
  Touches: chat model (message actions), Twitch Helix moderation endpoints, Kick moderation API, quick panel long-press menu
  Acceptance: long-press on a message offers Delete, Timeout 10 min, Ban; the action takes effect on the platform and the message is marked in the overlay.
  Complexity: M

## Findings from the 2026-08-29 drain

- [ ] P2 — IS-100 The undo offer can be unreachable or expire unseen
  Why: the snackbar is hosted in the activity window, so any dialog opened inside the ten seconds covers it and the offer lapses untappable; the countdown also keeps running while the app is backgrounded, so returning after ten seconds finds the offer gone without it ever having been seen.
  Evidence: adversarial review 2026-08-29 of `ui/ReplicaApp.kt` snackbar host vs `ui/components/AuditedDialogs.kt` (Compose `Dialog` is a separate window with a `fillMaxSize()` scrim); `LaunchedEffect` is composition-scoped, not lifecycle-scoped.
  Touches: `ui/ReplicaApp.kt`, `ui/components/AuditedDialogs.kt`
  Acceptance: opening a dialog while the offer is live still leaves Undo tappable; backgrounding the app pauses the countdown so the offer survives to be seen on return; a test drives both paths.
  Complexity: M

- [ ] P2 — IS-101 Quick-panel toggles never reflect their own writes
  Why: `QuickSettingsPanel` reads `runtime.transientBooleans[...]` while `MainViewModel.toggleBoolean` writes through `repository.setExtraToggle`, which lands in `settings.extraToggles`. Nothing copies one to the other, so Torch, Focus mode, White balance and Anti-flicker snap back the moment they are tapped.
  Evidence: adversarial review 2026-08-29; `ui/live/QuickSettingsPanel.kt:177-189` vs `MainViewModel.toggleBoolean`.
  Touches: `ui/live/QuickSettingsPanel.kt`, `MainViewModel.kt`
  Acceptance: tapping each quick-panel toggle leaves it in the new position, and the position survives closing and reopening the panel; a unit test covers one toggle end to end.
  Complexity: S

## Research-Driven Additions

Added 2026-09-04 from `RESEARCH.md`. Every item traces to a source recorded there.

### P0

- [ ] P0 — IS-104 Recreate the validation AVD so the three gates can run again
  Why: the geometry, strict-SSIM and app-chrome gates are the only defence against regressions across 145 audited screens, and none of them can be executed on this machine. Every audited-screen change is blocked behind this, including IS-99.
  Evidence: `Roadmap_Blocked.md`; repo `CLAUDE.md` entry 2026-08-29 "the harness AVD no longer exists on this machine"; the baseline in `replica-app/validation/geometry-baseline.json` was captured at 2316x1080 / 450 dpi on API 36, so no other API level is comparable.
  Touches: `replica-app/scripts/start-headless-emulator.ps1`, `scripts/check-environment.ps1`, `docs/testing-guide.md`
  Acceptance: `sdkmanager` installs the android-36 system image, `issue-sweep-api36` is recreated at 2316x1080 / 450 dpi, and `run-full-validation.ps1` completes with the geometry gate reporting against the existing baseline rather than erroring; `validation/reports/release-verification.txt` is regenerated for the current versionCode.
  Complexity: M

### P1

- [ ] P1 — IS-105 The stream key is displayed in clear on two screens
  Why: the connections page renders the whole RTMP URL as a row summary and a publish failure raises a toast containing the same URL. A stream key visible on the phone screen is exactly the channel-hijack path that key rotation exists to undo, and this screen is what an IRL streamer shares.
  Evidence: `ui/settings/SettingsCatalog.kt:152` (`summary = settings.connectionUrl`); `engine/StreamPackBroadcastEngine.kt:179` builds `"Could not publish to $url"`, surfaced by `MainViewModel.kt:329-330` as `toastMessage`. The `Log.e` on line 174 is already redacted, so the sanitisation exists but does not cover the user-visible paths.
  Depends on: IS-07
  Touches: `ui/settings/SettingsCatalog.kt`, `engine/StreamPackBroadcastEngine.kt`, `MainViewModel.kt`
  Acceptance: the row summary and every failure message show scheme, host and path with the key segment masked, with a deliberate reveal action; a unit test feeds a URL with a known key and asserts it appears in no user-facing string.
  Complexity: S

- [ ] P1 — IS-106 Reconnect automatically after a drop, with bounded backoff
  Why: cellular drops are the defining failure of IRL streaming and every competitor answers them. LifeStreamer advertises infinite reconnect; this app stops and stays stopped, while `BroadcastState.RECONNECTING` is already declared and never used.
  Evidence: `engine/BroadcastEngine.kt:17-18` (`DEGRADED`, `RECONNECTING` emitted only by the test fake at `app/src/test/.../BroadcastEngineTest.kt:141-146`); https://github.com/dimadesu/LifeStreamer feature list.
  Depends on: IS-102
  Touches: `engine/StreamPackBroadcastEngine.kt`, `ui/live/LiveConsoleScreen.kt`
  Acceptance: dropping the receiver moves the console to RECONNECTING and it returns to LIVE without user action once the receiver comes back; retries use bounded exponential backoff, stop on an explicit Stop, and the attempt count is visible; a test covers drop, retry and recovery.
  Complexity: M

- [ ] P1 — IS-107 The screen sleeps and the process is killable mid-broadcast
  Why: the app holds no wake lock and sets no keep-screen-on flag, so a broadcast ends when the display times out or the system reclaims the process. This is the difference between a demo and a tool someone walks around with.
  Evidence: zero occurrences of `keepScreenOn`, `FLAG_KEEP_SCREEN_ON` or `WakeLock` under `replica-app/app/src`; no `<service>` element in `AndroidManifest.xml`. `FLAG_KEEP_SCREEN_ON` needs no permission and covers the visible console; a `PARTIAL_WAKE_LOCK` held by the foreground service covers screen-off.
  Depends on: IS-06
  Touches: `MainActivity.kt`, `AndroidManifest.xml`, the broadcast service added by IS-06
  Acceptance: a broadcast survives the display timeout and a manual screen-off; the flag and the wake lock are released the moment the broadcast stops, verified with `dumpsys power`.
  Complexity: S

- [ ] P1 — IS-108 Resolution and FPS settings never reach the encoder
  Why: the Video parameters page presents nine resolutions and eleven frame rates as configurable and persists the choice, but the encoder is hardcoded. A user who selects 720p to survive a weak link gets 1080p anyway.
  Evidence: `engine/StreamPackBroadcastEngine.kt:204-208, 216-219` (`VIDEO_WIDTH = 1920`, `VIDEO_HEIGHT = 1080`, `VIDEO_FPS = 30`); `model/AppModels.kt` carries `h264BitrateKbps` and no resolution or fps field; `ui/settings/SettingsCatalog.kt` video page rows `resolution` and `fps`.
  Cross-reference: this is the specific case IS-70 covers in general; close it there when this lands.
  Touches: `model/AppModels.kt`, `data/ReplicaSettingsRepository.kt`, `engine/StreamPackBroadcastEngine.kt`
  Acceptance: selecting 1280x720 at 30 fps produces a stream a receiver reports as 1280x720; the "System default" fps option resolves to a documented value; a unit test asserts the `VideoCodecConfig` built from settings matches the selection.
  Complexity: M

- [ ] P1 — IS-109 Broadcast statistics are fabricated while a real stream is running
  Why: uptime and dropped frames are only ever reset to zero and the current bitrate is echoed back from settings, so the numbers on the console cannot tell a healthy stream from a failing one. StreamPack already measures all of it.
  Evidence: `engine/StreamPackBroadcastEngine.kt:165-170, 195-197`; StreamPack 3.2.0 `BasicEndpointMetrics` exposes `uptime`, `packetsWritten`, `packetsWriteDropped`, `packetsWriteLost`, `bytesWritten` and the `writtenBitrateInBps` extension (`core/src/main/java/io/github/thibaultbee/streampack/core/elements/metrics/EndpointMetrics.kt`), with an RTMP implementation in `extensions/rtmp/.../RtmpEndpointMetrics.kt`.
  Cross-reference: IS-09 covers per-link network telemetry from `ConnectivityManager`; this covers the endpoint's own counters.
  Touches: `engine/BroadcastEngine.kt`, `engine/StreamPackBroadcastEngine.kt`, `ui/live/LiveConsoleScreen.kt`
  Acceptance: uptime advances in real time, bytes and dropped-packet counts move under induced loss, and the D009 deviation note is narrowed to only what is still simulated.
  Complexity: M

- [ ] P1 — IS-110 RTMP authorization login and password are collected and thrown away
  Why: selecting "RTMP authorization" reveals Login and Password fields, but Save persists only the name and URL, so the credentials vanish and any authenticated destination fails with no explanation.
  Evidence: `ui/settings/Forms.kt:135-136` hold `login`/`password` in local `remember` state; the Save handler at `Forms.kt:143` calls `viewModel.saveConnection(name, url)` only; `data/ReplicaSettingsRepository.kt:129-132` stores two keys.
  Depends on: IS-07
  Touches: `ui/settings/Forms.kt`, `MainViewModel.kt`, `data/ReplicaSettingsRepository.kt`, `engine/StreamPackBroadcastEngine.kt`
  Acceptance: credentials entered on the form are persisted through the secret store, applied to the RTMP connect, and survive an app restart; a test round-trips a credentialled connection.
  Complexity: M

- [ ] P1 — IS-111 Saving a connection silently destroys the previous one
  Why: the repository keeps a single name/URL pair, so a second saved connection overwrites the first with no warning, while the page copy promises editing and a disabled "Delete multiple" row implies a list.
  Evidence: `data/ReplicaSettingsRepository.kt:129-132`; `ui/settings/SettingsCatalog.kt:161` "Long hold to edit a connection, only one active at a time" and `:167-173` `manage_connections` with `delete_multiple` disabled.
  Touches: `data/ReplicaSettingsRepository.kt`, `model/AppModels.kt`, `ui/settings/SettingsCatalog.kt`, `ui/settings/Forms.kt`
  Acceptance: several connections persist with one marked active, editing an existing one does not create a duplicate, deleting asks first, and the audited connections page still matches its captured geometry.
  Complexity: M

- [ ] P1 — IS-112 Nothing survives a crash: no handler, no log file, no diagnostics export
  Why: the app has six log calls, an in-app Log tab that is a hardcoded string, and a "send debug details" row that is a toast. A user whose stream failed in the field has nothing to send and the developer has nothing to read.
  Evidence: zero occurrences of `UncaughtExceptionHandler` or `Thread.setDefault*` under `replica-app/app/src`; `ui/live/QuickSettingsPanel.kt:258-287` renders a fixed string ending `state=SIMULATED`; `ui/settings/SettingsCatalog.kt:356` is a toast-only row.
  Touches: new `diagnostics/` package, `ui/live/QuickSettingsPanel.kt`, `ui/settings/SettingsCatalog.kt`
  Acceptance: an uncaught exception writes a timestamped crash file; the Log tab renders the real ring buffer; the export action produces a share intent whose payload contains no stream key or credential, asserted by a test.
  Complexity: M

- [ ] P1 — IS-113 No in-app update check on a sideload-only channel
  Why: distribution is signed APKs on GitHub Releases, so a user on an old build has no signal that a fix shipped. Nothing in the app queries the releases API.
  Evidence: zero occurrences of `api.github.com` or `releases/latest` under `replica-app/app/src`; latest release `v0.4.0` (2026-08-30) matches HEAD today, so the gap is preventable rather than already open.
  Cross-reference: this replaces the open half of IS-20, whose channel question is settled.
  Touches: new update-check source, `ui/settings/SettingsCatalog.kt` (Help & support), `AndroidManifest.xml`
  Acceptance: the app compares `BuildConfig.VERSION_NAME` against the latest release tag on a throttled schedule and offers the release page when behind; the check fails silently offline and can be turned off.
  Complexity: S

- [ ] P1 — IS-114 The only risky class in the repo has no tests
  Why: `StreamPackBroadcastEngine` owns the camera, the microphone, the encoder and the network and is referenced by no test source set, while `BroadcastEngineTest` asserts on a `FakeEngine` that emits DEGRADED and RECONNECTING states no production code can produce.
  Evidence: `app/src/test/java/com/irlstreamer/reconstruction/BroadcastEngineTest.kt:141-146`; no occurrence of `StreamPackBroadcastEngine` in `app/src/test` or `app/src/androidTest`.
  Depends on: IS-102
  Touches: `engine/StreamPackBroadcastEngine.kt` (extract a streamer factory seam), `app/src/test/.../`
  Acceptance: the engine takes its streamer through an injectable factory so a fake `IStreamer` can drive open, start, throw, reopen and release; tests cover the audio-permission fork, the failure-release path and the state transitions, and mutating the engine turns them red.
  Complexity: M

- [ ] P1 — IS-115 The instrumented console test opens the real camera and microphone
  Why: `LiveConsoleTest` launches without a capture extra, and any non-capture launch selects the real engine, so the device suite grabs hardware and may reach the network. That makes results device-dependent and can collide with whatever else holds the camera.
  Evidence: `app/src/androidTest/java/com/irlstreamer/reconstruction/LiveConsoleTest.kt:11`; `MainActivity.kt:33-37` selects `SimulatedBroadcastEngine` only when `isCaptureLaunch(intent)`.
  Touches: `app/src/androidTest/.../LiveConsoleTest.kt`, `MainActivity.kt`
  Acceptance: the instrumented suite runs with the simulated engine by construction, and a test asserts that a launch without the capture extra in a test context never constructs the StreamPack engine.
  Complexity: S

### P2

- [ ] P2 — IS-116 Offer HEVC as an encoder choice
  Why: HEVC halves the bitrate for the same quality on a constrained uplink, StreamPack already supports it, and Twitch made 1440p HEVC generally available to Partners and Affiliates in June 2026. LifeStreamer ships SRT HEVC as its headline.
  Evidence: StreamPack `CHANGELOG.md` and README list HEVC/H.265, AVC, VP9 and AV1 video codecs; `engine/StreamPackBroadcastEngine.kt:204-208` builds a `VideoCodecConfig` with the default AVC profile; https://github.com/dimadesu/LifeStreamer.
  Depends on: IS-108
  Cross-reference: IS-95 covers HEVC SEI timecode correctness on long streams; take it together with this if HEVC ships.
  Touches: `engine/StreamPackBroadcastEngine.kt`, `ui/settings/SettingsCatalog.kt` (encoder parameters)
  Acceptance: an HEVC option appears only when `MediaCodecList` reports a hardware HEVC encoder, a stream published with it is reported by the receiver as H265, and falling back to AVC on an unsupported device is automatic and stated in the log.
  Complexity: M

- [ ] P2 — IS-117 Declare `ACCESS_LOCAL_NETWORK` before the targetSdk 37 bump
  Why: at targetSdk 37 Android blocks all traffic to local network addresses until the user grants a new runtime permission. Every LAN use case this project has — a home OBS or MediaMTX receiver, Moblink mDNS discovery, the planned local web remote, and the loopback test rig — stops working silently on the bump.
  Evidence: https://developer.android.com/privacy-and-security/local-network-permission and https://developer.android.com/about/versions/17/behavior-changes-17; the app currently declares only CAMERA, RECORD_AUDIO, INTERNET and ACCESS_NETWORK_STATE.
  Depends on: IS-96
  Touches: `AndroidManifest.xml`, connection form, new permission rationale surface
  Acceptance: on a targetSdk 37 build, publishing to a LAN address prompts for the permission with an explanation, a refusal produces a named error rather than a silent failure, and publishing to a public address never prompts.
  Complexity: M

- [ ] P2 — IS-118 No static analysis runs before a release
  Why: there is no lint configuration, no baseline, and no formatter or detekt, so `lintVital` on a release assembly is the only check and nothing fails a build for a lint error introduced today.
  Evidence: no `lint {}` block in `replica-app/app/build.gradle.kts`, no `lint-baseline.xml`, no detekt, ktlint or `.editorconfig` anywhere in `replica-app`.
  Touches: `replica-app/app/build.gradle.kts`, new baseline file, `scripts/run-full-validation.ps1`
  Acceptance: `lint` runs on debug and release with `abortOnError` true against a checked-in baseline, a formatter runs over `app/src`, both are wired into `run-full-validation.ps1`, and a deliberately introduced violation fails the run.
  Complexity: S

- [ ] P2 — IS-119 Documentation contradicts the shipped engine
  Why: three separate places still describe v0.2.0 behaviour, and this is the third release in a row where docs drifted. Anyone reading them plans against an app that no longer exists.
  Evidence: `replica-app/docs/known-deviations.md` D005 "Audio is not captured yet, so the stream is video only", false since `d045b6e`; `engine/BroadcastEngine.kt:70-72` "SimulatedBroadcastEngine is the only implementation today"; `MainViewModel.kt:282-285` "No connection is configurable yet"; `validation/reports/release-verification.txt` reports versionCode 1 / versionName 0.1.0.
  Depends on: IS-104 (for the regenerated verification report)
  Touches: `docs/known-deviations.md`, `engine/BroadcastEngine.kt`, `MainViewModel.kt`, `validation/reports/release-verification.txt`
  Acceptance: each statement matches the code at HEAD, and a test in the spirit of `VersionStringTest` fails if the D005 row still claims audio is absent while `MicrophoneSourceFactory` is referenced in `src/main`.
  Complexity: S

- [ ] P2 — IS-120 No session data counter or metered-link warning
  Why: 1080p at 6 Mbps is roughly 2.7 GB per hour, and the app gives no indication of what a session has cost on a metered SIM. The bytes are already measured once IS-109 lands.
  Evidence: `BasicEndpointMetrics.bytesWritten` in StreamPack 3.2.0; `ConnectivityManager.isActiveNetworkMetered` and `NetworkCapabilities.NET_CAPABILITY_NOT_METERED` for the link classification; the app declares `ACCESS_NETWORK_STATE` and uses it for nothing.
  Depends on: IS-109
  Touches: `engine/BroadcastEngine.kt`, `ui/live/QuickSettingsPanel.kt` (Network tab), `ui/settings/SettingsCatalog.kt`
  Acceptance: the network tab shows bytes sent this session and an estimated rate per hour; starting a broadcast on a metered link warns once, and the warning can be suppressed per connection.
  Complexity: S

### P3

- [ ] P3 — IS-121 Settings snapshots carry no schema version
  Why: `reset()` and `restore()` copy an untyped `Preferences` map, and the planned import/export path will accept a payload written by a different build. Without a version key an older app will happily restore keys it cannot interpret, and a renamed key silently becomes a lost setting.
  Evidence: `data/ReplicaSettingsRepository.kt:140-162`; the generic `toggle.` and `choice.` key namespaces mean any string key is accepted.
  Depends on: IS-19
  Touches: `data/ReplicaSettingsRepository.kt`, import/export path
  Acceptance: the store writes a schema version, restore and import refuse a payload from a newer schema with a named error and migrate an older one, and a test round-trips across a simulated version bump.
  Complexity: S

- [ ] P3 — IS-122 Console telemetry text fails contrast at readable sizes
  Why: the live console draws its telemetry and lens labels in `#767676`, `#777777`, `#555555` and `#494949` on a near-black surface at 5 to 13 sp, which is below any usable contrast ratio outdoors — the exact condition this app is used in. D010 already establishes accessibility improvements as an accepted deviation from the audited original.
  Evidence: `ui/live/LiveConsoleScreen.kt:256-258, 290, 329-332, 369-370`; `docs/known-deviations.md` D010.
  Depends on: IS-104
  Touches: `ui/live/LiveConsoleScreen.kt`, `ui/theme/AuditMetrics.kt`, `docs/known-deviations.md`
  Acceptance: every console text token meets 4.5:1 against its drawn background at its rendered size, the change is recorded as a new D-number under the D010 precedent, and the geometry gate still passes on the affected audited states. Blocked until IS-104 restores the harness AVD.
  Complexity: M
