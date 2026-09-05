# Research — IRL Streamer
Date: 2026-09-04 — replaces all prior research (passes of 2026-08-15 and 2026-08-29).

## Executive Summary

IRL Streamer is a clean-room Android reconstruction of a 145-state IRL-broadcasting console audit, now shipping v0.4.0 with a real capture path: StreamPack 3.2.0 owns the camera, encodes H.264, captures the microphone when granted, and publishes to a saved RTMP or RTMPS destination. The UI is the strongest part of the project — a data-driven Compose settings tree, measured audit tokens locked by tests, and a validation harness whose gates were deliberately made able to fail. The transport is the weakest: the engine starts a stream and then stops observing it.

The strategic picture changed since the last pass. The 2026-08-15 and 2026-08-29 conclusions that "no open-source SRTLA client for Android exists" and that "the market window is open" are **both now false**. Between 2025-08 and 2026-09 four independent Android efforts appeared — LifeStreamer (GPL-3.0, built on the same StreamPack SDK, v1.45.0 on 2026-09-03), Bond Bunny (AGPL-3.0 SRTLA bonding sidecar), PocketSRT/PocketBond, and Vivid (MIT, an explicit Android port of Moblin, nightly releases) — plus a first-party Android Moblink relay from Moblin's own author. What none of them have is a finished console: LifeStreamer explicitly tells users to add overlays in OBS and run chat elsewhere, and Vivid has the UI but has not shipped bonding. That gap — audited-quality console UI on top of real transport — is where this project's existing asset actually is.

Top opportunities in priority order:

1. **A dead stream is invisible.** `StreamPackBroadcastEngine` never collects StreamPack's `throwableFlow`, `isStreamingFlow` or `isOpenFlow`, so the console shows LIVE after the connection drops.
2. **The stream key leaves the device.** The backup rules exclude a file the app never writes; the real store is backed up.
3. **The stream key is displayed in clear** on the connections page and inside a publish-failure toast.
4. **No gate can be run on this machine.** The harness AVD and android-36 image are gone, so the three validation gates and every audited-screen change are blocked.
5. **No reconnect.** Competitors advertise "infinite reconnect"; this app has none, and the state machine already declares `RECONNECTING`.
6. **The screen sleeps mid-broadcast** — no wake lock, no `FLAG_KEEP_SCREEN_ON`, no foreground service.
7. **Resolution and FPS settings are decoys** — the encoder is hardcoded to 1920x1080 at 30 fps.
8. **Statistics are fabricated** even though StreamPack 3.2.0 ships a Metrics API with exactly the fields the console fakes.
9. **The one risky class has no tests**, and the instrumented console test now opens the real camera and microphone on a device.
10. **Toolchain is a generation behind** and the next targetSdk bump adds a mandatory runtime permission that this app's LAN use cases need.

## Product Map

- **Core workflows.** Go live from the landscape console to a saved RTMP/RTMPS destination; configure video, audio, recording, bonding, overlays and display through a 22-page settings tree; drive the live quick panel (camera, network, display, overlays, audio, log tabs); reproduce any of 145 audited states through the debug harness for visual and geometry validation.
- **Personas.** The IRL streamer walking with a phone as the whole rig; the operator running a phone as a camera into a home OBS or a relay; the maintainer running the parity harness.
- **Platforms and distribution.** Android 9 (API 28) to Android 16 (API 36), forced landscape, single `:app` Gradle module, `com.irlstreamer.reconstruction`. Distribution is signed APKs on GitHub Releases only (latest `v0.4.0`, 2026-08-30, matching HEAD); there is no in-app update path.
- **Integrations and data flows.** Camera2 and AudioRecord through StreamPack into MediaCodec H.264, out over RTMP/RTMPS. Settings persist in a DataStore Preferences file (`files/datastore/irl_streamer_settings.preferences_pb`) including `connection_url`, which carries the stream key. Bonding, recording, snapshots, chat, overlays and telemetry are deterministic local simulations (`docs/known-deviations.md` D005, D008, D009).

## Competitive Landscape

**LifeStreamer** (https://github.com/dimadesu/LifeStreamer, GPL-3.0, 38 stars, created 2025-09-11, v1.45.0 on 2026-09-03, pushed 2026-09-05) — the closest competitor by construction: an Android IRL app built on the same StreamPack SDK, releasing every few days. Ships SRT and RTMP, HEVC and H.264, a BELABOX/Moblin-derived adaptive bitrate, SRTLA via its Bond Bunny sidecar, USB/UVC and RTMP/SRT sources, USB and Bluetooth audio, Moblink, and a foreground service for screen-off streaming. **Learn:** infinite reconnect, background service, and external sources are what an Android IRL app is now expected to have. **Avoid:** its deliberate absence of overlays, chat and UI — the author points users at OBS instead, which is precisely the ground this project already holds.

**Vivid** (https://github.com/thoser666/Vivid, MIT, 24 stars, Kotlin/Compose, nightly builds through 2026-09-04) — an explicit Android port of Moblin, 34 of 67 features done, `v0.5.8-beta.1`. Has RTMP/RTMPS/SRT on RootEncoder 2.7.5, Twitch EventSub chat with moderation, 11 OpenGL filters, OBS WebSocket, widgets, minSdk 24 / targetSdk 37 on Gradle 9.3.1 and JDK 25. **Learn:** it proves the toolchain migration this project has deferred (IS-96) is routine now. **Avoid:** its position — it has the UI and no bonding, which is the mirror of LifeStreamer and leaves both incomplete.

**Moblin** (https://github.com/eerimoq/moblin, MIT, 730 stars, iOS, pushed 2026-09-04) — still the feature ceiling: chat across Twitch/Kick/YouTube/SOOP, widget system, four named ABR controllers, Moblink, DJI Bluetooth control, replay, TTS, web remote. Its 13 open issues are a free defect list for anyone reimplementing the same features (#426 HEVC SEI timecode emulation-prevention bytes, #418 RIST bonding feeding a stale-RTT dead link, #414 SRT latency growth on long streams, #409 Low Light Boost forcing 60 fps). **Learn:** the remote-control API is the feature users ask to extend. **Avoid:** building the widget surface before the transport is trustworthy.

**Moblink** (https://github.com/eerimoq/Moblink, MIT, Kotlin, 24 stars, pushed 2026-09-05) — a first-party Android relay app already exists that turns a spare phone into an extra SRTLA bonding leg, with a Rust reimplementation for non-phone donors (https://github.com/datagutt/moblink-rust). **Learn:** the streamer side of Moblink is the valuable half for this project; the donor role is already served. **Avoid:** rebuilding the donor app.

**Bond Bunny** (https://github.com/dimadesu/bond-bunny, AGPL-3.0, Java, 31 stars, v1.16.0 on 2026-07-30) and **PocketSRT/PocketBond** (https://github.com/romestylez/pocketSRT, https://github.com/romestylez/pocketBond) — two independent Android SRTLA implementations, both structured as a local SRT listener that re-sprays packets across links. **Learn:** the sidecar architecture is a legitimate shipping strategy that decouples bonding from capture. **Avoid:** the AGPL source as a copy target; port from the MIT reference instead.

**irlserver/srtla_send** (https://github.com/irlserver/srtla_send, MIT, v3.0.0 2026-01-11, pushed 2026-08-23) — the best-documented SRTLA v2 sender in public: `window / (in_flight + 1)` scoring with ~8 s exponential NAK decay, a 0.7x penalty above five NAKs, a ≤3% RTT bonus, 10% hysteresis against link flapping, stalled-link deselection and whole-bond DNS re-home. **Learn:** port these semantics and emit the same per-link metric names (`rtt_ms`, `window`, `in_flight`, `nak_total`, `quality_multiplier`). **Avoid:** bundling it — it is a standalone Unix binary with no FFI or Android target and needs source routing that Android does not give you outside `Network.bindSocket`.

**BELABOX** (https://github.com/BELABOX/srtla, AGPL-3.0) — the protocol of record (REG1/REG2/REG3 handshake, connection groups, `REG_ERR`/`REG_NAK`/`REG_NGP`), but the sender has had one commit since 2025-04-05 and the newest OS image is 2025-09-15. **Learn:** the wire format, from the README. **Avoid:** treating the C code as a live upstream, and avoid linking it (AGPL).

**MediaMTX** (https://github.com/bluenviron/mediamtx, v1.20.1 2026-08-18) — healthy release cadence and the natural loopback rig for RTMP and SRT. Its SRTLA receiver PR #5811 has been **open since 2026-05-29** with a sceptical maintainer. **Learn:** use it as the RTMP/SRT test receiver. **Avoid:** documenting it as an SRTLA ingest — point users at `irlserver/irl-srt-server` (MIT, pushed 2026-09-03) instead.

**Cloud relays** — the paywall map is stable and cheap: IRLServer $9.99/mo, PerHost SRTLA $7/mo, BELABOX Cloud from $10/mo (gated behind GitHub Sponsors or a voucher), IRLHost Plus €11.99/mo; cloud-OBS is the step change at $30–$60/mo, with IRLToolkit at $129–$179 the legacy tier everyone undercuts. Restream only accepts SRT from $239/mo and has no SRTLA at any tier. **Learn:** endpoint presets for the $7–$12 relays are the fastest onboarding path. **Avoid:** a hosted relay as this project's moat.

**Larix Broadcaster** (https://softvelum.com/larix/premium/) — the audited original's engine lineage; Android build updated 2026-07-06, Premium $9.99/mo removes the watermark and time limit and unlocks multi-output, HEVC and ABR. Bonding is RIST-based, not SRTLA. **Learn:** everything it paywalls is free-tier scope here. **Avoid:** its watermark-and-timer free tier.

**IRL Pro** (`app.irlpro.android`) — last updated 2024-09-03, roughly two years stale, ~120K installs at 4.0/5. The audited original, now effectively abandoned. **Learn:** its unaddressed review complaints are acceptance criteria. **Avoid:** assuming its behaviour is current.

**IRLwhatever** (https://irlwhatever.com/, proprietary beta) — SRTLA, RIST, SRT, RTMP, UVC, Moblink client, dual-camera PiP, Twitch/Kick chat overlay with 7TV/BTTV/FFZ emotes, OBS control. The yardstick for a finished Android product.

**Twitch, Kick, Prism, Streamlabs mobile** — Twitch's native app added **Disconnect Protection on 2026-03-09** (holds the stream up to 90 s on a drop, on by default on Android and iOS), which weakens the case for an on-device BRB slate on Twitch specifically but not on Kick or YouTube. Kick's "Go Live" app is RTMP-class with no SRT. Prism Live has RTMP/HLS/WHIP/SRT/RIST and 6-way multistream but no bonding. Streamlabs Mobile is RTMP only.

## Reported Issues

The GitHub tracker at `SysAdminDoc/IRL_Streamer` is **empty on 2026-09-04**: no open or closed issues, no pull requests, discussions disabled. There is no `KNOWN_ISSUES.md` and the README has no troubleshooting section. Every defect below came from reading the code on 2026-09-04.

- **A dead stream reads as LIVE.** `engine/StreamPackBroadcastEngine.kt:162-186` sets `BroadcastState.LIVE` on a successful `startStream` and never observes the streamer again. StreamPack's `IStreamer` exposes `throwableFlow: StateFlow<Throwable?>` and `isStreamingFlow: StateFlow<Boolean>`, and `ICloseableStreamer` exposes `isOpenFlow`; none is collected anywhere in the app.
- **`BroadcastState.DEGRADED` and `RECONNECTING` are unreachable.** Only the `FakeEngine` at `app/src/test/.../BroadcastEngineTest.kt:141-146` emits them, so the suite asserts on a contract no production code implements.
- **The stream key is backed up off the device.** `res/xml/backup_rules.xml:3` and `res/xml/data_extraction_rules.xml` exclude `sharedpref/secrets.xml`, a file the app never writes. The real store is `preferencesDataStore(name = "irl_streamer_settings")` (`data/ReplicaSettingsRepository.kt:19`), which lands under `getFilesDir()` and is in Auto Backup's default set (https://developer.android.com/identity/data/autobackup), with `allowBackup="true"` in the manifest.
- **The stream key is rendered in clear.** `ui/settings/SettingsCatalog.kt:152` uses the full connection URL as a settings-row summary, and `engine/StreamPackBroadcastEngine.kt:179` builds a failure message containing `$url` that `MainViewModel.kt:329-330` shows as a toast. The `Log.e` on line 174 is correctly redacted, so the sanitisation exists but does not cover the two user-visible paths.
- **RTMP authorization credentials are discarded.** `ui/settings/Forms.kt:135-136` collects Login and Password into local `remember` state, and the Save handler at `:143` calls `saveConnection(name, url)` only. The fields are a dead form.
- **Only one connection can exist.** `ReplicaSettingsRepository.setConnection` writes two fixed keys, so saving a second connection silently replaces the first, while the page copy at `SettingsCatalog.kt:161` promises "Long hold to edit a connection" and `manage_connections` offers a disabled "Delete multiple".
- **Resolution and FPS are decoys.** `StreamPackBroadcastEngine.kt:204-208, 216-219` hardcode 1920x1080 at 30 fps; `model/AppModels.kt` carries no resolution or fps field at all, while `SettingsCatalog.kt` presents nine resolutions and eleven frame rates as configurable.
- **Statistics are fabricated.** `uptimeSeconds` and `droppedFrames` are only ever reset to zero, `currentBitrateKbps` is set once from settings, and all three `LinkStatistics` report 0 kbps. StreamPack 3.2.0 ships `BasicEndpointMetrics` (`uptime`, `packetsWritten`, `packetsWriteDropped`, `packetsWriteLost`, `bytesWritten`, `writtenBitrateInBps`) which covers most of it.
- **No screen or CPU hold.** Zero occurrences of `keepScreenOn`, `FLAG_KEEP_SCREEN_ON` or `WakeLock`, and no `<service>` in the manifest, so the display sleeps and the process is killable mid-broadcast.
- **No crash handler, no log file, no diagnostics export.** Six `Log.` calls in the whole app; the quick panel's Log tab (`ui/live/QuickSettingsPanel.kt:258-287`) is a hardcoded string ending `state=SIMULATED`; the "Send cameras debug details" row is a toast.
- **The instrumented console test opens real hardware.** `app/src/androidTest/.../LiveConsoleTest.kt:11` launches without a `screen_id` extra, and `MainActivity.kt:33-37` selects `StreamPackBroadcastEngine` for any non-capture launch.
- **Documentation contradicts the code.** `docs/known-deviations.md` D005 still says "Audio is not captured yet, so the stream is video only" — untrue since `d045b6e`; the KDoc at `engine/BroadcastEngine.kt:70-72` still calls the simulation the only implementation; `MainViewModel.kt:282-285` still says no connection is configurable; `validation/reports/release-verification.txt` still reports versionCode 1 / versionName 0.1.0 from 2026-08-15.
- **Every gate is un-runnable.** `Roadmap_Blocked.md` and the repo `CLAUDE.md` record that the `issue-sweep-api36` AVD and the android-36 system image are absent, and the geometry baseline was captured at 2316x1080 / 450 dpi on API 36, so no other API level is comparable. The strict visual gate stands at 0/145 with median SSIM 0.869 and mean geometry error 48.78 px.
- **No static analysis.** No `lint {}` block, no lint baseline, no detekt, ktlint or `.editorconfig`; lint only runs via `lintVital` on a release assembly.

Judged not worth acting on: the `TODO`/`FIXME` scan returned zero hits across the whole tree, so there is no hidden debt in comments; the D014/D015 out-of-order numbering in `known-deviations.md` is cosmetic; `stopBroadcast()` lacking the in-flight guard that `toggleBroadcast()` has is unreachable from the current UI, which only exposes the toggle.

## Security, Privacy, and Reliability

- **Stream-key exposure has three independent paths**, all live in v0.4.0: cloud backup and device transfer (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`), the settings row summary (`ui/settings/SettingsCatalog.kt:152`), and the publish-failure toast (`engine/StreamPackBroadcastEngine.kt:179` → `MainViewModel.kt:330`). A stream key on screen is a documented channel-hijack vector, and this app's own screen is what a phone streamer shares.
- **Secrets are stored unencrypted.** DataStore Preferences is plaintext protobuf; IS-07 covers the fix and the deprecation of `androidx.security-crypto`.
- **Supply chain, ahead of SRT adoption.** libsrt 1.5.6 fixed CVE-2026-55868 (encryption state-machine downgrade) and CVE-2026-55869 (heap overflow in KMREQ handling); StreamPack still pins srtdroid 1.9.5, which bundles srt 1.5.4 and is therefore vulnerable. srtdroid 1.10.0 (2026-08-26) carries srt 1.5.7 but bundles **OpenSSL 3.5.1**, five patch releases behind 3.5.7 (2026-06-09), which fixed 15 CVEs including CVE-2026-45447, a use-after-free in `PKCS7_verify`. Adding `streampack-srt` without a resolution rule ships both problems.
- **Missing guardrails.** No reconnect, no thermal backoff, no degraded state, no data-usage guard on a metered link, no rate limit or confirmation on a destructive settings reset beyond the ten-second undo (which IS-100 shows can be covered by a dialog).
- **Recovery and rollback.** The settings reset/undo snapshot is the only recovery mechanism in the app; there is no crash log to recover from a failure, no exported diagnostics, and no way for a user on an old APK to learn a fix shipped.
- **Positive findings.** Dependency verification is on with sha256 per artifact across debug, release, test, lint and device-test configurations (`gradle/verification-metadata.xml`, 4,461 lines); release signing reads four external environment variables and no key is in the repo; the browsable `irlstreamer://` deep link is confined to the debug source set; `Assert-ReplicaDevice` refuses any non-emulator serial.

## Architecture Assessment

- **The engine seam held, and the prior pass's open question is settled.** StreamPack owns the camera and drives both preview and encode through one session; CameraX was removed entirely in v0.4.0 and `ui/live/CameraPreview.kt` now wraps StreamPack's `SourcePreview`. The camera-stack fork the last pass agonised over is closed.
- **The engine is write-only.** It commands StreamPack but never listens to it. Adding a single collector over `throwableFlow`, `isStreamingFlow` and `isOpenFlow` in `StreamPackBroadcastEngine` unlocks dead-stream detection, reconnect, the degraded state and honest statistics without touching Compose.
- **StreamPack 3.2.0 already provides four things this app hand-rolls or fakes.** `streampack-services` ships an Apache-2.0 `StreamerService` (a `LifecycleService` that owns the streamer, posts open/close/error notifications and collects `throwableFlow` for you) — its manifest declares `FOREGROUND_SERVICE` and `POST_NOTIFICATIONS` but only the `mediaProjection` type, so a camera streamer must add `FOREGROUND_SERVICE_CAMERA` and `FOREGROUND_SERVICE_MICROPHONE` itself. The Metrics API covers uptime, bytes, dropped and lost packets. `ISurfaceSourceInternal` plus a `VideoSourceFactory` is the documented custom-source seam (`docs/use_cases/CustomSources.md`), and main has an unreleased "set Bitmap as video source" commit from 2026-08-29 — that is the BRB slate. `DualStreamer` and `CombineEndpoint` give record-and-stream and multistream.
- **Version-catalog gap.** Dependency versions are inline literals in `app/build.gradle.kts` with no `gradle/libs.versions.toml`, which makes the coming toolchain migration harder to stage.
- **Toolchain is one generation behind.** The project pins Gradle 8.14.4 / AGP 8.13.2 (the tip of the 8.x line) / Kotlin 2.3.21 / Compose BOM 2026.06.01. Current is AGP 9.4.0 (September 2026, requiring Gradle 9.6.0 and JDK 17, max API level 37) and Compose BOM 2026.08.00 (2026-08-14). Compose 1.12.0 "requires compileSdk 37 and Android Gradle Plugin (AGP) 9". Android 17 / API 37 is stable, and at targetSdk 37 the `ACCESS_LOCAL_NETWORK` runtime permission becomes mandatory for **all** traffic to local addresses — which this app needs for a LAN MediaMTX, a home OBS, Moblink mDNS discovery, the planned local web remote and the loopback test rig.
- **Test gaps.** 40 test functions across 12 files, none touching `StreamPackBroadcastEngine`, networking, real-disk persistence, permission flows or the (absent) service. The `InMemoryPreferencesDataStore` fake is a documented workaround for a Windows rename failure, so repository semantics are covered but disk persistence is not.
- **Documentation and harness.** README claims and `known-deviations.md` have drifted from the shipped engine three times in three releases (`edb1a29`, `6f91265`, and the D005 line still wrong today); the commit history shows five separate commits fixing gates that could not fail, which is exactly why the un-runnable AVD is a priority rather than a chore.

## Rejected Ideas

- **Rebuild a Moblink donor/relay app** — `eerimoq/Moblink` (MIT, Kotlin, pushed 2026-09-05) already does it first-party, and `moblink-rust` covers non-phone donors. Only the streamer side (IS-76) is worth building. This supersedes IS-59 as written.
- **Wear OS companion (IS-92 as written)** — removed from the roadmap: Wear OS is out of bounds for this machine's Android work by standing rule, and no research or implementation effort should be spent on it.
- **Play / F-Droid distribution decision (IS-20 as written)** — settled, not open: distribution is signed APKs on GitHub Releases, so the remaining work is an in-app update check, not a channel decision.
- **Bundle `srtla_send` or fork BELABOX `srtla`** — unchanged from 2026-08-29 and now better evidenced: `srtla_send` is a standalone Unix binary with no FFI or Android target and relies on source routing that Android replaces with `Network.bindSocket`; BELABOX's C is AGPL and has had one commit since 2025-04-05.
- **Document MediaMTX as the SRTLA receiver** — PR #5811 has been open since 2026-05-29 with the maintainer arguing SRTLA is unstandardised and OS-level bonding is preferable. Use `irlserver/irl-srt-server` for SRTLA and MediaMTX for RTMP/SRT.
- **WHIP as a primary uplink** — RFC 9725 since March 2025, and Cloudflare, AWS IVS, Ant Media and Red5 accept it, but Twitch, YouTube and Kick do not. Keep it for self-hosted relays only.
- **AV1 encode for live** — YouTube is the only major live ingest accepting AV1 as of mid-2026; Twitch keeps it inside its beta community. HEVC is the codec worth adding now.
- **On-device BRB as a Twitch feature** — Twitch shipped native Disconnect Protection on 2026-03-09 with a 90-second hold, on by default. IS-56 keeps its value for Kick, YouTube and self-hosted targets; it is no longer a Twitch differentiator.
- **Copy Bond Bunny or LifeStreamer source** — AGPL-3.0 and GPL-3.0 respectively. Read them for behaviour, port from the MIT `srtla_send` semantics.
- **Migrate the parity harness to Roborazzi/Paparazzi** — still true across three passes: none of them score cross-app goldens. Roborazzi stays a self-golden layer (IS-63).
- **A plugin or extension surface, and any multi-user or account model** — considered and excluded rather than overlooked: this is a single-purpose console owned by one person on one handset, the audit describes no extension point, and every integration it needs (chat, OBS, relays) is a first-class feature rather than a third-party slot.
- **Health Connect for live heart rate, WebView-to-GL with WebGL, MPTCP/Speedify VPN bonding, Zixi/NDI, Larix SDK, HaishinKit.kt** — all still rejected for the reasons recorded on 2026-08-15 and 2026-08-29.

## Sources

Competitor clients
- https://github.com/dimadesu/LifeStreamer
- https://github.com/dimadesu/bond-bunny
- https://github.com/dimadesu/MediaSrvr
- https://github.com/thoser666/Vivid
- https://github.com/romestylez/pocketSRT
- https://github.com/romestylez/pocketBond
- https://github.com/eerimoq/moblin
- https://github.com/eerimoq/Moblink
- https://github.com/datagutt/moblink-rust
- https://irlwhatever.com/
- https://softvelum.com/larix/premium/
- https://play.google.com/store/apps/details?id=app.irlpro.android
- https://prismlive.com/en_us/mobile.html

Transport, relays and receivers
- https://github.com/BELABOX/srtla
- https://github.com/irlserver/srtla_send
- https://github.com/irlserver/irl-srt-server
- https://github.com/bluenviron/mediamtx/pull/5811
- https://github.com/NOALBS/nginx-obs-automatic-low-bitrate-switching
- https://belabox.net/
- https://irlserver.com/
- https://www.perhost.app/en
- https://irltoolkit.com/
- https://restream.io/pricing

Engine and dependencies
- https://github.com/ThibaultBee/StreamPack
- https://github.com/ThibaultBee/StreamPack/blob/main/CHANGELOG.md
- https://github.com/ThibaultBee/StreamPack/issues
- https://repo1.maven.org/maven2/io/github/thibaultbee/streampack/streampack-core/maven-metadata.xml
- https://github.com/ThibaultBee/srtdroid
- https://github.com/Haivision/srt
- https://github.com/pedroSG94/RootEncoder

Platform and toolchain
- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/privacy-and-security/local-network-permission
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/build/releases/gradle-plugin
- https://developer.android.com/develop/ui/compose/bom
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- https://developer.android.com/media/camera/camera2/camera-enumeration
- https://developer.android.com/reference/android/net/Network#bindSocket(java.net.Socket)

Protocols and platforms
- https://github.com/veovera/enhanced-rtmp
- https://dev.twitch.tv/docs/eventsub/eventsub-subscription-types/
- https://github.com/Bukk94/KickLib
- https://developers.google.com/youtube/v3/live/guides/ingestion-protocol-comparison
- https://www.rfc-editor.org/rfc/rfc9725.html
- https://github.com/Haivision/srt/blob/master/docs/API/statistics.md

## Open Questions

1. **Can StreamPack's video config be changed mid-stream?** `IConfigurableVideoStreamer.setVideoConfig()` has no guard against being called while `isStreamingFlow` is true, and `SingleStreamerImpl` just forwards to the pipeline output. Bitrate is likely safe; a resolution or fps change reconfigures the encoder and is unvalidated. This decides whether IS-12 (ABR) can move resolution as well as bitrate, and whether IS-108 needs a stream restart. Needs a spike against 3.2.0.
2. **Is srtdroid's OpenSSL actually exploitable here?** srtdroid 1.10.0 bundles OpenSSL 3.5.1, and 3.5.7 fixed a `PKCS7_verify` use-after-free. Whether libsrt's key exchange reaches that code path determines whether IS-52 needs an owned OpenSSL build or only a srtdroid version bump. Needs a reading of libsrt's crypto usage, not a version comparison.
3. **Does `Network.bindSocket` still hold several simultaneous networks on Android 16/17 without a foreground service?** No 2026 behaviour-change document narrowing this surfaced, but absence of a hit is not proof. This gates IS-77 and therefore all bonding work. Needs live validation on a dual-SIM device.
4. **Will Twitch accept Enhanced RTMP multitrack from a third-party mobile encoder?** Dual Format went GA in June 2026 and the contract is public, but every documented example is a desktop encoder. Still blocks IS-91's priority.
5. **Which physical lenses does the target device expose to Camera2?** OEMs commonly hide ultrawide and telephoto from Camera2 even when the stock app uses them. IS-58 and IS-73 cannot be scoped until this is measured on the S25 Ultra.

Confidence: library versions, Maven metadata, CVE fix versions, GitHub repository metadata and release dates, Android platform documentation, and every file path and line number cited here are **Verified** against primary sources on 2026-09-04. Cloud-relay pricing tiers are **Verified** from vendor pages except IRLHosting and Streamrun, which are **Likely**. Community pain rankings are **Likely** at best — Reddit was not reachable from this session's tooling, so those conclusions rest on GitHub issue trackers, vendor community docs and trade write-ups. Anything marked as needing a spike above is **Needs live validation**.
