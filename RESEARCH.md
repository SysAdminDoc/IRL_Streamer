# Research — IRL Streamer
Date: 2026-08-29 — replaces all prior research (the two 2026-08-15 passes). Findings from those passes that still stand are restated here with their sources rather than referenced.

## Executive Summary

IRL Streamer is a clean-room Kotlin/Compose reconstruction of IRL Pro's 145 audited screens. As of v0.3.0 (2026-08-29) the console shows a live CameraX preview; everything else that would leave the phone (encode, RTMP/SRT, bonding, recording, chat) is still a deterministic simulation behind the `BroadcastEngine` seam (`replica-app/app/src/main/java/com/irlstreamer/reconstruction/engine/`). The audited original is Softvelum Larix code plus BELABOX SRTLA (class prefix `com.wmspanel.*`, `larix:` scheme; Verified from `app-audit/app/application-identity.md`), and Softvelum has since discontinued the Larix SDK (https://softvelum.com/larix/android_sdk/), so IRL Pro's derivation cannot be repeated. It must be built.

The strongest shape for this project is the thing no open-source Android app is: a free Moblin-class broadcaster with SRTLA bonding. IRL Pro (closed, free, last shipped v3.5.23 on 2024-09-03) still has the Android SRTLA market to itself apart from IRLwhatever (proprietary open beta, https://irlwhatever.com/). Moblin's maintainer closed the Android port request as not planned (https://github.com/eerimoq/moblin/issues/149). The window found on 2026-08-15 is still open.

Three facts from this pass change the existing plan:

1. **SRTLA no longer needs an AGPL port.** irlserver/srtla_send (Rust, MIT, v3.0.0 2026-01-11, pushed 2026-08-23) is a complete SRTLA v2 sender with quality-aware link scoring, and datagutt/moblink-rust (MIT) documents the Moblink protocol. Port from those, treat BELABOX's C as protocol reference only. Retargets IS-17 and the licence posture note in `LICENSE`.
2. **libsrt 1.5.7 is one dependency override away.** srtdroid 1.10.0 (2026-08-26) bundles srt 1.5.7 + OpenSSL 3.5.1 (https://github.com/ThibaultBee/srtdroid/releases); StreamPack main still pins srtdroid 1.9.5 (srt 1.5.4, https://github.com/ThibaultBee/StreamPack/blob/main/gradle/libs.versions.toml). A Gradle resolution rule replaces most of IS-52's from-source build; only the OpenSSL bump remains a reason to build.
3. **Overlays have a platform path.** CameraX `OverlayEffect` (1.4+, multi-Preview fix in 1.7.0-alpha03) draws Canvas content into the frame pipeline, so IS-13's "GPU compositor" can start as Canvas, not GL.

Top opportunities, priority order:

1. Ship a stream: RECORD_AUDIO + foreground service + StreamPack RTMP/SRT (IS-05, IS-06, IS-04), with srtdroid 1.10.0 forced.
2. Thermal governor and charge-wattage HUD. Overheating and the S24U 5 W charging trap are the most repeated Android IRL complaint (https://start.irlstreami.ng/android-devices/irl-pro). Existing IS-18 gets the API and thresholds below.
3. ABR ported from belacoder's constants (fully readable in `belacoder.c`) with Moblin's Fight preset as the second controller (IS-12).
4. SRTLA sender from srtla_send + Moblink streamer side, so spare Android phones running the existing Moblink relay app donate links (new IS-76; IS-59 covers the donor direction).
5. Disconnect protection without a PC: in-app BRB flag plus NOALBS-compatible stats (IS-14, IS-56). IRL Pro's most-cited review gap.
6. Portrait 9:16 output. IRL Pro's top Play review request and Moblin issue #88. New IS-79.
7. Chat parity: Kick needs a webhook relay (official events are webhook-only), Twitch is EventSub WebSocket. TTS offline via sherpa-onnx. New IS-80, IS-81.
8. Trust fixes already visible in the code: stale "Version 0.2.0" literals, toast-only error feedback, an untested irreversible reset, 2 string resources against ~632 literals.

## Product Map

- **Core workflows:** landscape console with live preview; start/stop broadcast; connection CRUD (RTMP/SRT/SRTLA); video/audio/recording settings; text/picture/timestamp/web overlays; quick panel (camera/network/display/overlays/audio/log).
- **Personas:** outdoor cellular streamer (one-handed, glanceable); bonded multi-modem rig operator (per-link visibility); iOS Moblin streamer with a spare Android phone (donor role).
- **Platforms and distribution:** Android 9+ (minSdk 28), targetSdk 36, single activity, Compose BOM 2026.06.01, CameraX 1.6.2, AGP 8.13.2, Gradle 8.14.4, Kotlin 2.3.21. No releases published yet (GitHub releases API empty on 2026-08-29). Play requires target API 36 from 2026-08-31 (met). 16 KB page alignment required for API 35+ targets (https://developer.android.com/guide/practices/page-sizes).
- **Integrations (all simulated today):** RTMP/RTMPS/SRT egress, SRTLA relay, Twitch/Kick chat, Streamlabs/StreamElements/Toonation dashboards, `irlstreamer://` settings import (parsed nowhere: `ui/settings/SettingsCatalog.kt:296-301`).

## Competitive Landscape

**Moblin (iOS, MIT, https://github.com/eerimoq/moblin)** — the feature ceiling: RTMP(S)/SRT/SRTLA/RIST/WHIP, bonding with per-link stats and priorities, four ABR controllers, widgets, chat with 7TV/BTTV/FFZ emotes and moderation, TTS, OBS WebSocket, web remote, Apple Watch. Learn: its open issues are a free bug-avoidance list (#414 SRT latency creep on long streams, #426 HEVC SEI timecode size bug, #418 RIST bonding sends to a dead link, #409 low-light boost forces 60 fps). Avoid: the widget surface before transport works.

**Moblink (https://github.com/eerimoq/Moblink, protocol in https://github.com/datagutt/moblink-rust)** — Android relay app on Play; mDNS discovery, WebSocket hello/identify challenge-response, then UDP forwarding bound to the relay's interface. Learn: implementing the *streamer* side makes every existing Moblink phone a bonding leg for this app. Avoid: a private protocol.

**IRL Pro (https://irlpro.app/, https://play.google.com/store/apps/details?id=app.irlpro.android)** — free, SRTLA with hosted relay, HEVC, chat overlay. Play ~4.0/334 reviews, stagnant since 2024-09. Review complaints (Likely, via search excerpts): no portrait mode, no disconnect protection, can't use USB cameras, over-exposure, no scenes, web chat "bounces around", slow torch, no low-light boost. Its own community docs name overheating and charge-lock as the main problems. Learn: these are the acceptance criteria for a replacement. Avoid: a hosted relay as the moat; ship self-host docs.

**IRLwhatever (https://irlwhatever.com/, proprietary beta)** — SRTLA/RIST/SRT, Moblink client, UVC, incoming RTMP, PiP, emotes, TTS, browser remote. The only Android app with Moblink client today. Learn: it proves the feature set is buildable on Android. Avoid: nothing; it is the yardstick for "done".

**StreamPack (Apache-2.0, 3.2.0 2026-07-21, https://github.com/ThibaultBee/StreamPack)** — Camera2 + MediaCodec, RTMP/RTMPS/SRT, H.264/HEVC/VP9/AV1, dual output (record + stream), SRT ABR, Metrics API, physical camera ID flow, torch strength, Compose preview. No SRTLA (#76 open since 2023, maintainer lacks time), no overlays (#52 open), no USB source (#186). Learn: use it for capture/encode/transport only. Avoid: waiting on upstream for bonding or effects.

**RootEncoder (Apache-2.0, 2.8.0 2026-07-16, https://github.com/pedroSG94/RootEncoder)** — adds WHIP (beta) and an OpenGL filter pipeline. Learn: reference implementation for GL text/image overlays if `OverlayEffect` proves insufficient. Avoid: its ABR (simple stepper, #1990).

**irlserver/srtla_send (Rust, MIT, https://github.com/irlserver/srtla_send)** — SRTLA v2 sender with ~8 s exponential NAK decay, burst detection, stalled-link deselection, whole-bond re-home on DNS change, keyframe-priority hints, Prometheus metrics. Learn: this is the spec to port to Kotlin. Avoid: BELABOX/srtla C (AGPL-3.0) as code.

**BELABOX belacoder (GPL-3.0, https://raw.githubusercontent.com/BELABOX/belacoder/master/belacoder.c)** — the reference ABR: `SRTO_SNDDATA` buffer occupancy and `msRTT` EWMAs (0.99/0.01), three buffer thresholds, RTT thresholds from latency (drop to min at RTT ≥ latency/3 or buffer > th3; step down 100 kbps + bitrate/10 every 250 ms at RTT > latency/5 or buffer > th2; step up 30 kbps + bitrate/30 every 500 ms when RTT < min and delta < 0.01; round to 100 kbps; 6 s ACK silence = dead link). Learn: port the constants verbatim, cross-check with Moblin's `AdaptiveBitrateSrtBelabox.swift` and its test suite. Moblin's Fight preset (https://github.com/eerimoq/moblin/blob/main/Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtFight.swift): 200 ms tick on RTT + packets-in-flight, Fast PIF 200 / factor 0.9, Slow PIF 500 / 0.95, RTT clamp 450 ms, min 50 kbps.

**NOALBS (MIT, v2.19.1, https://github.com/NOALBS/nginx-obs-automatic-low-bitrate-switching)** — defaults low 500 kbps, offline 450 kbps, RTT 1000 ms degraded / 3500 ms offline, 5 consecutive checks; chat commands `!bitrate !fix !refresh !switch !live !privacy`. Learn: mirror the thresholds on-device and emit a privacy/BRB flag. Avoid: assuming a PC is present.

**Receivers** — go-irl v2.4.0 (AGPL, 2026-08-20, https://github.com/e04/go-irl), irl-srt-server (MIT, https://github.com/irlserver/irl-srt-server), OpenIRL/srtla-receiver (GPL-3, Docker), and **MediaMTX PR #5811 "SRTLA receiver support" open and updated 2026-08-28** (https://github.com/bluenviron/mediamtx/pull/5811). Learn: if #5811 merges, MediaMTX becomes the one-container test rig for both SRT and SRTLA.

**Larix Broadcaster (https://softvelum.com/larix/premium/)** — $9.99/mo or $119/yr removes watermark and time limit; bonding is Zixi-only on Apple; Tuner remote $10/device/mo. Play 2.88/5. Learn: everything it paywalls is free-tier scope here. Avoid: subscription resentment.

**StreamCaster-android (v0.0.3, no LICENSE, https://github.com/alxayo/StreamCaster-android)** — RootEncoder-based Compose app with thermal throttling (60 s cooldown) and QR endpoint import. Learn: the thermal cooldown pattern and QR payload. Avoid: copying unlicensed code.

**Commercial class in one line** — PRISM Plus $9.99/mo for multistream since 2025-07-28; Streamlabs Ultra $27/mo gates disconnect protection; CameraFi $9.99/mo; Twitch app added 90 s disconnect protection in 2026-03; Kick app can't reply to chat; IRLToolkit $129–$179/mo; IRLServer relay $9.99/mo; BELABOX Cloud $10/mo. Disconnect protection and multistream are the two features every closed app charges for.

## Reported Issues

The GitHub tracker is empty on 2026-08-29: no open or closed issues, no PRs, no discussions (disabled), no releases. There is no KNOWN_ISSUES.md. Defects come from code inspection instead:

- `ui/settings/SettingsCatalog.kt:81,348` and `debug/DebugStateCatalog.kt:137` hardcode "Version 0.2.0" while `versionName` is 0.3.0. Root cause: version is a literal, not `BuildConfig.VERSION_NAME`.
- `MainViewModel.kt:129-130` → `ReplicaSettingsRepository.kt:119` `edit { it.clear() }`: reset is real, irreversible, unconfirmed by any toast, and has no unit test.
- `CameraPreview.kt:118,141`: camera bind failures reach the user only as a toast; no retry surface.
- Loading/empty/error states exist only as debug-catalog seeds (`DebugStateCatalog.kt:152-155`); production feedback is toast-only.
- `res/values/strings.xml` has 2 strings; ~632 hardcoded literals; `supportsRtl="true"` with no RTL copy.
- 4 `contentDescription` sites across 20 UI files; IS-68 (unnamed checkable quick-panel switches) still open.
- `DebugStateCatalog` ships in `src/main` (IS-69).

## Security, Privacy, and Reliability

- **libsrt:** 1.5.6 (2026-07-20) fixed CVE-2026-55868 (encryption downgrade) and CVE-2026-55869 (KMREQ overflow); 1.5.7 (2026-08-28) adds ACK/DROPREQ/FEC bounds checks and a bonding use-after-free fix (https://github.com/Haivision/srt/releases). srtdroid 1.10.0 ships 1.5.7; StreamPack pins 1.9.5 (1.5.4). Force 1.10.0 via `resolutionStrategy` and verify `llvm-readelf -l` shows 0x4000 LOAD alignment.
- **OpenSSL:** srtdroid 1.10.0 bundles 3.5.1; current LTS is 3.5.8 (https://openssl-library.org/news/openssl-3.5-notes/). The 3.5.1 CMS CVEs are not reachable from SRT's AES-GCM path (Likely). Rebuild only if a TLS/RTMPS path exposes them.
- **Secrets:** `androidx.security-crypto` is deprecated (1.1.0-alpha07); replacement is DataStore 1.3.0-alpha07+ `datastore-tink` with an AndroidKeyStore-backed keyset (https://developer.android.com/jetpack/androidx/releases/datastore). Feeds IS-07. Today stream keys would land in plain Preferences DataStore.
- **Foreground service:** `camera|microphone` types have no 6 h timeout; cannot start from BOOT_COMPLETED on 15+; `microphone` cannot start from background; Play rejects unused type declarations and requires a Console usage declaration (https://developer.android.com/about/versions/15/changes/foreground-service-types, https://support.google.com/googleplay/android-developer/answer/16965181). Feeds IS-06.
- **Multi-network:** `requestNetwork(TRANSPORT_CELLULAR)` + `Network.bindSocket` per link; 100-request cap per UID; sockets die on network loss, rebind in `onAvailable` (https://developer.android.com/develop/connectivity/network-ops/reading-network-state). MPTCP is not exposed on stock Android (https://github.com/mptcp-nexus/android); SRTLA over bound UDP is the only path.
- **Supply chain:** 2026 Gradle plugin portal trojanized-plugin incident (Likely, https://www.redfoxsec.com/blog/software-supply-chain-attacks-2026-latest-incidents-analysis-and-how-to-protect-your-pipeline). The repo pins the wrapper SHA-256 but has no `gradle/verification-metadata.xml`.
- **Platform:** CVE-2026-0006 critical RCE in Media Codecs Mainline (https://source.android.com/docs/security/bulletin/2026/2026-03-01); nothing to do in-app beyond not parsing untrusted media.
- **Reset with no undo** and **toast-only errors** (see Reported Issues) are the reliability gaps in shipped code.

## Architecture Assessment

- **Engine seam holds.** `BroadcastEngine` is the right boundary; StreamPack's `StreamerPipeline`/`DualStreamer` maps onto it and gives record+stream for free (IS-08).
- **Camera stack split.** CameraX for the preview (shipped) versus StreamPack's Camera2 for the encoder surface will fight over the device. Decision needed before IS-04: either StreamPack's Compose preview replaces `CameraPreview.kt`, or CameraX `VideoCapture`/`OverlayEffect` feeds the encoder and StreamPack is used for transport only. Research leans to the latter: `OverlayEffect` (Canvas into the frame), `SessionConfig` feature groups (60 fps + PREVIEW/VIDEO_STABILIZATION + HDR with `isSessionConfigSupported`), Low Light Boost, concurrent-camera PiP, and `Recorder.setVideoMimeType(HEVC)` all arrive in CameraX 1.5–1.7 (https://developer.android.com/jetpack/androidx/releases/camera). Needs live validation: whether a CameraX `VideoCapture` surface can be redirected into StreamPack's encoder, or whether StreamPack must accept an external `Surface` source.
- **Debug harness leaks into production paths** (`debugScreenId` checks in five `src/main` files). IS-69.
- **Version string** should derive from `BuildConfig.VERSION_NAME` (IS-75).
- **Toolchain ceiling is one migration, not several:** Compose BOM 2026.08.00 requires compileSdk 37 + AGP 9.1.1+, AGP 9 requires dropping `kotlin-android` for built-in Kotlin, AGP 9.3 needs Gradle 9.5 (https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html, https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/). CameraX 1.7.0-alpha03 deprecates `Camera2Interop.Extender` for a DSL. Plan as one item (IS-96).
- **Test gaps:** no ViewModel navigation test, no permission test, no accessibility test, no reset test, no transport loopback. Emulator `-camera-back videofile:` plus MediaMTX in Docker gives a local loopback rig (https://developer.android.com/studio/run/emulator-commandline).
- **Docs:** README has no troubleshooting section and no self-host receiver guidance; `known-deviations.md` numbering skips D014 before D015.

## Rejected Ideas

- **Port BELABOX srtla C via NDK** (RESEARCH 2026-08-15, ROADMAP IS-17 as written): AGPL relicenses the app and a MIT Rust sender now exists. Retarget, don't port.
- **Build libsrt from source as the primary CVE fix** (IS-52 as written): srtdroid 1.10.0 already ships 1.5.7. Keep from-source only for an OpenSSL bump.
- **Migrate the parity harness to Roborazzi/Paparazzi** (both 2026-08-15 passes): still true, none score cross-app goldens. Roborazzi stays a self-golden layer (IS-63).
- **MPTCP / Speedify-style VPN bonding**: MPTCP needs a patched AOSP; VPN bonding doesn't speak SRTLA and community reports it still drops (https://github.com/mptcp-nexus/android, https://support.speedify.com/article/870-bonding-mode).
- **WHIP to Twitch/Kick/YouTube as a primary transport**: Kick and YouTube don't accept WHIP; Twitch's endpoint is experimental (https://www.rfc-editor.org/rfc/rfc9725.html, https://space-node.net/blog/kick-streaming-setup-guide-2026). Keep WHIP for MediaMTX/LiveKit only, later.
- **Twitch Enhanced Broadcasting from the phone as P1**: the IVS multitrack contract is public (https://docs.aws.amazon.com/ivs/latest/LowLatencyUserGuide/multitrack-video-sw-integration.html) but whether Twitch whitelists arbitrary mobile clients is unverified. P3 with that question attached.
- **Health Connect for heart-rate widgets**: historical records only, no live stream (https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data). Use BLE GATT 0x180D directly.
- **WebView-to-GL texture for browser overlays with WebGL/video**: WebView falls back to software draw off-screen (https://groups.google.com/a/chromium.org/g/chromium-dev/c/3wrULcul8lw). Accept low-fps `PixelCopy` snapshots, no WebGL.
- **Zixi/NDI, Guest Star, APV codec, hosted relay, HaishinKit.kt, Larix SDK**: all still rejected for the 2026-08-15 reasons (proprietary, platform-locked, irrelevant to uplink, discontinued).
- **Samsung `sdhms` thermal-throttle tile hacks** (https://xdaforums.com/t/thermal-throttling-quick-tile-s25-plus-ultra.4717811/): root-adjacent, not for an app.

## Sources

Engines, protocol, bonding
- https://github.com/ThibaultBee/StreamPack
- https://github.com/ThibaultBee/StreamPack/blob/main/CHANGELOG.md
- https://github.com/ThibaultBee/StreamPack/blob/main/gradle/libs.versions.toml
- https://github.com/ThibaultBee/StreamPack/issues/76
- https://github.com/ThibaultBee/StreamPack/issues/52
- https://github.com/ThibaultBee/srtdroid/releases
- https://github.com/Haivision/srt/releases
- https://github.com/BELABOX/srtla/blob/main/README.md
- https://raw.githubusercontent.com/BELABOX/belacoder/master/belacoder.c
- https://github.com/irlserver/srtla_send
- https://github.com/irlserver/irl-srt-server
- https://github.com/e04/go-irl
- https://github.com/bluenviron/mediamtx/pull/5811
- https://github.com/bluenviron/mediamtx/releases
- https://github.com/eerimoq/moblin
- https://github.com/eerimoq/moblin/issues/149
- https://github.com/eerimoq/moblin/issues/414
- https://github.com/eerimoq/moblin/issues/426
- https://github.com/eerimoq/moblin/issues/88
- https://github.com/eerimoq/moblin/blob/main/Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtFight.swift
- https://github.com/eerimoq/Moblink
- https://github.com/datagutt/moblink-rust
- https://github.com/pedroSG94/RootEncoder
- https://github.com/NOALBS/nginx-obs-automatic-low-bitrate-switching
- https://github.com/alxayo/StreamCaster-android
- https://raw.githubusercontent.com/obsproject/obs-websocket/master/docs/generated/protocol.md
- https://github.com/k2-fsa/sherpa-onnx
- https://github.com/lmarceau/heart-rate-monitor-ble

Competitors, commercial, community
- https://irlpro.app/
- https://play.google.com/store/apps/details?id=app.irlpro.android
- https://start.irlstreami.ng/android-devices/irl-pro
- https://start.irlstreami.ng/android-devices/recommended-android-phones
- https://irlwhatever.com/
- https://softvelum.com/larix/premium/
- https://softvelum.com/larix/android_sdk/
- https://softvelum.com/larix/faq/
- https://guide.prismlive.com/mobile/announcement/general/upcoming-subscription-model-for-prism-live-studio
- https://streamlabs.com/mobile-app
- https://help.kick.com/en/articles/15159836-getting-started-with-the-kick-go-live-app
- https://irltoolkit.com/
- https://irlserver.com/
- https://belabox.net/
- https://support.speedify.com/article/870-bonding-mode
- https://github.com/irlhost/awesome-irl-streaming
- https://github.com/banj-oe/IRLStreamingforFreeorCheap

Platform, dependencies, policy
- https://developer.android.com/jetpack/androidx/releases/camera
- https://developer.android.com/jetpack/androidx/releases/datastore
- https://developer.android.com/about/versions/15/changes/foreground-service-types
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/16/features
- https://developer.android.com/about/versions/17/release-notes
- https://developer.android.com/games/optimize/adpf/thermal
- https://developer.android.com/media/camera/lowlight/low-light-boost-ae
- https://developer.android.com/develop/connectivity/network-ops/reading-network-state
- https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-recording
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/google/play/requirements/target-sdk
- https://support.google.com/googleplay/android-developer/answer/16965181
- https://developer.android.com/studio/run/emulator-commandline
- https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- https://blog.jetbrains.com/kotlin/2026/01/update-your-projects-for-agp9/
- https://developer.android.com/build/releases/agp-9-3-0-release-notes
- https://openssl-library.org/news/openssl-3.5-notes/
- https://source.android.com/docs/security/bulletin/2026/2026-03-01
- https://dev.twitch.tv/docs/eventsub/handling-websocket-events
- https://github.com/KickEngineering/KickDevDocs
- https://docs.aws.amazon.com/ivs/latest/LowLatencyUserGuide/multitrack-video-sw-integration.html
- https://www.rfc-editor.org/rfc/rfc9725.html
- https://github.com/mptcp-nexus/android

Project evidence
- `app-audit/app/permissions-and-appops.md`, `app-audit/app/package-inventory.md`, `app-audit/testing/untested-and-blocked-cases.md`
- `replica-app/docs/known-deviations.md`, `replica-app/app/build.gradle.kts`

## Open Questions

1. **Capture topology:** can StreamPack accept an external `Surface` (CameraX `VideoCapture`/`OverlayEffect` output) as its video source, or must the preview move to StreamPack's Camera2 pipeline? Decides whether `CameraPreview.kt` survives IS-04 and whether IS-13 can use `OverlayEffect`. Needs a spike against StreamPack 3.2.0.
2. **Twitch mobile whitelisting for Enhanced Broadcasting:** the IVS multitrack contract is public, Twitch's own docs 403 to automation. Blocks IS-91 prioritisation.
3. **Receiver for end-to-end bonding tests:** MediaMTX PR #5811 (SRTLA) is unmerged; until it lands the LAN rig is go-irl or irl-srt-server, and Android 16's local-network permission may bite LAN tests.
4. **Distribution channel** (unchanged from 2026-08-15): Play vs F-Droid vs GitHub decides signing and the FGS Console declaration before IS-06 ships.

Confidence: library versions, CVEs, CameraX/Android release notes, Moblin/StreamPack/srtla_send facts and pricing pages are Verified against primary sources on 2026-08-29. IRL Pro Play-review complaints and Larix version dates are Likely (search excerpts; Play blocks automated fetch). Reddit thread frequencies were not fetchable this pass; community ranking rests on vendor community docs, GitHub issues and Play excerpts.
