# Research — IRL Streamer
Date: 2026-08-15 (second pass, supersedes the 2026-08-15 morning pass) — replaces all prior research.

Two passes ran on 2026-08-15. The morning pass established engine provenance and the build-out path; a deep audit between the passes logged 30 harness/app defects (`ROADMAP.md` IS-22..IS-51). This second pass adds: the screenshot-testing ecosystem verdict, concrete competitor feature matrices, a platform/CVE sweep, and the community-signal coverage the morning pass lost to rate limiting. Morning-pass findings that still stand are retained below rather than re-derived.

## Executive Summary

IRL Streamer is a clean-room Kotlin/Compose reconstruction of the observable interface of IRL Pro: 145 audited states with every transport simulated. The audited original runs Softvelum Larix broadcaster code (`com.wmspanel.streamer.*`, `larix:` scheme, "Includes licensed SRTLA code") with BELABOX SRTLA bonding — **Verified** from audit evidence, corroborated by go-irl's compatibility list. The build-out path is settled (operator decision 2026-08-15): StreamPack (Apache-2.0) as engine, BELABOX `srtla` ported via NDK for bonding, licences not a gate.

**The market window is open.** IRL Pro — the only Android SRTLA app — has not shipped since 2024-09-03 (v3.5.23, ~23 months), and its own site still lists OBS WebSocket control as TODO. Meanwhile Moblin (iOS, MIT) ships weekly (v33.1337.0 on 2026-08-14) and is the reflexive recommendation iOS streamers give while Android askers get redirected to closed apps; an explicit "Moblin alternative for Android??" thread exists. IRL Pro's moat is its free hosted bonding relay; its stagnation makes transport parity attackable now.

Top opportunities, priority order:

1. **Fix the validation harness first (IS-22..IS-51, already filed).** The three gates can currently report stale PASSes, never fail on geometry, and print hardcoded evidence. Everything else builds on trusting these gates.
2. **Own the libsrt build.** StreamPack 3.2.0 ships srt 1.5.4 via srtdroid 1.9.5; SRT ≤1.5.5 carries two Critical CVEs (CVE-2026-55868 encryption-downgrade, CVE-2026-55869 KMREQ stack overflow) fixed in 1.5.6 (2026-07-20). Building libsrt 1.5.6+ + OpenSSL 3.5.x from source — one 16 KB-aligned copy shared by StreamPack and the srtla port — kills both CVEs and the 16 KB page-size risk (Play deadline now 2027-02-01) in one move.
3. **Keep the custom parity harness; steal framework practices.** No 2026 screenshot-testing framework can score cross-app parity — all assume self-recorded same-renderer goldens with pixel-family comparators. The only precedents for our geometry-first architecture (Applitools "Layout" match level, the GVT research line) validate the design. Adopt: per-screen calibrated SSIM thresholds, run-scoped artifacts with freshness manifests, an emulator determinism kit, and a Roborazzi self-golden layer for day-to-day regression.
4. **Transport core before widget surface**: SRTLA + named ABR profiles + OBS-WS remote — small code, big moat. Moblin's four named tunable ABR algorithms (`belabox` default, `fastIrl`, `slowIrl`, `customIrl` — verified from its source) are the concrete spec IS-12 needed.
5. **Community-ranked pain, in order:** connection death in crowds (bonding), relay complexity/cost, battery drain (including while charging), disconnect protection without a home PC, Android lens gaps (ultrawide unusable outside paid Larix). These map to IS-17, IS-15, new battery/BRB/lens items below.
6. **Moblink as the Android leapfrog**: use spare phones as extra bonding modems (protocol public, Rust reference exists, Moblin's own Android Moblink app has 23 stars). An app that plays the donor role is useful even to iOS Moblin streamers — a distribution wedge no Android competitor has.

## Product Map

**Core workflows** — land in a landscape live console with preview; start/stop broadcast; configure connections (RTMP/SRT/SRTLA, credentials, target type); video/audio/recording parameters; text/picture/timestamp/web overlays; in-broadcast quick panel (camera/network/display/overlays/audio/log).

**Personas** — outdoor IRL streamer on cellular, one-handed, glanceable state; bonded multi-modem rig operator needing per-link visibility; desk-configured device never touched while live.

**Platforms/distribution** — Android 9+ (minSdk 28), targetSdk 36, forced landscape, single activity, Compose. Distribution unresolved but constrained: AGPL-3.0 (once srtla links) is fine on Play (Signal-Android is AGPL on Play; Google's AGPL ban is its internal-use policy, not a Play policy) and explicitly acceptable on F-Droid (take F-Droid's signing key initially; reproducible builds with NDK code is the documented hard path). New Play apps/updates must target API 36 from 2026-08-31 — already satisfied.

**Integrations (currently simulated)** — RTMP/RTMPS/SRT egress; SRTLA → relay → SRT/RTMP; Twitch/Kick chat; Streamlabs/StreamElements/Toonation dashboards; `larix://`-style settings import/QR.

## Competitive Landscape

**Moblin (iOS, MIT, eerimoq/moblin)** — the feature ceiling and a public dated spec of IRL demand (in-app version history, weekly releases; v33.1337.0 2026-08-14). Verified from source: 4 named ABR algorithms (`belabox`/`fastIrl`/`slowIrl`/`customIrl` with exposed tunables: packetsInFlight, pifDiffIncreaseFactor, minimumBitrate; implementation `Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtBelabox.swift` + test suite). RTMP(S)/SRT/SRTLA/RIST/WHIP; SRT(LA)/RTMP/RTSP/WHEP *ingest server* (DJI/GoPro/phones as sources); Twitch/Kick/YouTube/SOOP chat with 7TV/BTTV/FFZ emotes, TTS, moderation, chat bot; map/scoreboard/text-variable widgets; OBS WebSocket client; browser remote-control assistant; watchOS companion; game-controller/keyboard input. *Learn:* treat its version history as the roadmap oracle; port ABR semantics with its test suite as cross-check. *Avoid:* chasing the full widget surface before the transport core.

**Moblink (eerimoq/moblink, Android app + moblink-rust relay)** — spare phones as extra SRTLA bonding connections for a Moblin streamer. *Learn:* implementing the protocol in Kotlin gives this app both roles — donor (useful to iOS streamers today) and consumer (bond via spare Androids). *Avoid:* inventing a private protocol for the same job.

**Softvelum Larix Broadcaster (Android 1.5.11, 2026-07-11)** — the settings-tree baseline this replica already mirrors. SRT caller/listener/rendezvous, RIST, Enhanced RTMP HEVC, NDI|HX2 + Zixi (Premium), talkback, SEI/NTP timing, Grove config links, Tuner fleet management. Slow cadence (one notable feature — NDI tally — in 12 months). No bonding: their biggest hole and this app's wedge. *Learn:* SRT listener/rendezvous modes and per-connection overlay sets are audited-tree-shaped features worth real implementations. *Avoid:* subscription resentment — community explicitly shops for a free Larix alternative (lens thread, 2025).

**IRL Pro (app.irlpro.android, v3.5.23, 2024-09-03 — stagnant)** — advertised: SRTLA + free hosted bonding relay, auto bitrate, Twitch/Kick chat overlay, dashboards, web overlays with audio, per-lens buttons. Site TODO still lists OBS-WS control and realtime stats. *Learn:* the free relay is the moat — answer with first-class self-host docs (go-irl, MediaMTX, irl-srt-server) rather than a hosted service. *Avoid:* its silence — 23 months without updates is the reason the window exists.

**BELABOX** (belabox.net) — dynamic bitrate under marginal signal, H.265, Orange Pi/Radxa hardware, Cloud relays from $10/mo, browser cloud remotes from $5/mo. Receiver expectation: srtla_rec UDP :5000 → SRT :5001, streamid routing. Ecosystem latency guidance: ~2000-3500 ms SRT latency depending on conditions (third-party guides; Likely). *Learn:* scenario-based SRT latency presets beat a single default. *Avoid:* depending on srtla_rec server-side (upstream calls it unsupported).

**irlserver/irl-srt-server** — SLS fork with SRTLA built in, player auth, per-stream bitrate limits, audio gap filling, HTTP stats/control API. The best self-host receiver to test against alongside go-irl/MediaMTX.

**One-paragraph class** — PRISM Live Studio: free multistream + AR effects, no bonding. CameraFi Live: UVC/DSLR sources, instant replay, SRT, no bonding. Streamlabs Mobile: alerts/themes ecosystem, Ultra multistream, disconnect protection, RTMP-only. Twitch native Go Live: zero-setup + Guest Star, no bitrate depth — and community calls it "an absolute abysmal disaster" for IRL. *Learn (cross-cutting):* disconnect protection is now table stakes in closed apps; UVC input and replay are differentiators nobody in the SRT class ships on Android.

**OBS WebSocket 5.x** (protocol 5.7.4, RPC 1, bundled since OBS 28) — 9 opcodes, SHA256-challenge auth, JSON. Only JVM client is obs-websocket-java 2.0.0 (MIT, modest activity, not coroutine-native). *Learn:* a Kotlin/OkHttp + kotlinx.serialization client is ~500 lines and itself a community asset.

**Screenshot-testing ecosystem (harness-relevant)** — Roborazzi 1.72.0 (JVM/Robolectric Native Graphics, Dropbox Differ pixel comparator, accessibility checks, record/compare/verify with JSON results); Paparazzi 1.3.5/2.0-alpha (layoutlib, no dialogs); official Compose Preview Screenshot Testing still 0.0.1-alpha with deprecation signals; device-based Dropshots/android-testify (pluggable validators, exclusion regions). **None supports cross-app baselines; all assume self-recorded goldens.** *Learn:* per-screen thresholds, artifact freshness discipline, `_compare` triptychs, emulator determinism (SwiftShader, animations off, demo-mode status bar, pinned AVD density). *Avoid:* migrating the parity gate to any of them — median SSIM 0.87 against real-Samsung captures would force meaningless tolerances.

## Security, Privacy, and Reliability

- **libsrt Critical CVEs (Verified):** CVE-2026-55868 (encryption state-machine downgrade) and CVE-2026-55869 (KMREQ/KMRSP stack buffer overflow), both ≤1.5.5, fixed in v1.5.6 (2026-07-20). StreamPack 3.2.0 → srtdroid 1.9.5 → **srt 1.5.4 + OpenSSL 3.5.1** (vulnerable). Do not ship the stock stack; build 1.5.6+ from source. OpenSSL 3.5.1's CMS CVEs (CVE-2025-9230..9232) are likely unreachable from SRT paths but bundle current 3.5.x when rebuilding.
- **16 KB page size (Verified):** Play deadline now 2027-02-01 for apps targeting 15+; applies to every packaged `.so` including the future srtla port. NDK r28+ aligns by default; verify with `llvm-objdump -p | grep LOAD` (align 2**14). srtdroid ≥1.9.2 is compliant.
- **Foreground service semantics (Verified):** camera|microphone types + normal FGS permissions + runtime CAMERA/RECORD_AUDIO; missing type → `MissingForegroundServiceTypeException`; ungranted runtime perm → `SecurityException`; background start → `ForegroundServiceStartNotAllowedException`. Android 15 removed BOOT_COMPLETED camera-FGS starts. No timeout for camera/mic types.
- **Multi-network constraints for bonding (Verified from AOSP docs):** `requestNetwork` needs only `CHANGE_NETWORK_STATE` (install-time); hard cap 100 outstanding requests/UID (`TooManyRequestsException` — register once, reuse); the platform may swap the satisfying network (rebind sockets in `onAvailable`, never cache `Network` handles); cellular hold is documented to cost battery; requests drop when the process dies — keep them in the FGS. Android 16's local-network permission (rolling out through 2026) will affect LAN receivers, not internet uplinks.
- **targetSdk 36 sleeper (Verified):** orientation locks are ignored on ≥600 dp displays — the forced-landscape console must either handle portrait on tablets/foldables or declare the per-activity compat property.
- **Harness integrity:** the deep audit (same day) found the validation gates can report stale PASSes (IS-22), never fail on geometry (IS-24), and print fabricated evidence rows (IS-23). These are P0/P1 in ROADMAP and precede any engine work in trust order.
- Missing guardrails unchanged from pass 1: no permission flow (IS-05), no encrypted secret storage (IS-07), no reconnect/backoff state machine (IS-14 territory).

## Architecture Assessment

Unchanged from pass 1 and still correct: (a) `BroadcastEngine` seam with the simulation behind it before any real pipeline (IS-02); (b) settings schema/storage separation — `SettingsCatalog.kt` encodes labels+structure+defaults+actions together, and the audit showed the interactive consequences (IS-27/IS-31); (c) overlays need a GPU compositor, not Compose (IS-13).

New this pass:
- **Camera stack: stay Camera2 via StreamPack** (it uses Camera2+MediaCodec directly; CameraX's VideoCapture targets file recording, not encoder surfaces). Revisit only if dual-camera PiP becomes a headline feature — CameraX 1.5+/1.6 is materially easier there.
- **Harness two-tier gating:** geometry differ (structure) as primary gate, per-screen calibrated SSIM (`median − k·MAD` over N clean runs, stored in the existing `validation/thresholds.csv`) as secondary — replacing the single global 0.985 a 0.87-median corpus can never meet. Roborazzi-style run-scoped outputs + content-hash manifest close the stale-artifact class.
- **Test gap:** behavioural fixtures for the engine seam (IS-10) should adopt Moblin's `AdaptiveBitrateSuite` cases as the ABR cross-check corpus.
- Version pins verified 2026-08-15: Gradle 8.14.4 + **AGP 8.13.2** (repo CLAUDE.md's "AGP 8.14.4" conflates the two), Kotlin 2.3.21 (2.4.10 current, routine), Compose BOM 2026.06.01 — **do not bump to BOM 2026.08.00**, it requires compileSdk 37 + AGP 9.1.1.

## Rejected Ideas

- **Migrate the parity harness to Roborazzi/Paparazzi/CPST/testify** — every framework assumes self-recorded same-renderer goldens with pixel-family comparators; cross-app parity at median SSIM 0.87 would need tolerances so loose they pass anything. Keep custom, adopt practices. (Sources: tool READMEs/docs, 2026-08-15.)
- **CameraX as the capture layer** — StreamPack is Camera2-native; CameraX VideoCapture is not an encoder-surface pipeline. (StreamPack README + libs.versions.toml.)
- **Speedify-style OS-level VPN bonding** — community distrusts it for live use ("stream still cut out like 5 times"); a VPN service is a different product and does not speak SRTLA.
- **Depend on obs-websocket-java** — MIT but low-activity and not coroutine-native; the protocol is small enough to own (~500 lines Kotlin).
- **Zixi/NDI outputs** — proprietary SDK licensing (Larix ships them as certified Premium); no audited evidence; conflicts with an open build.
- **Guest Star-style co-streaming** — platform-locked Twitch feature with no public API surface for third-party broadcasters.
- **APV codec (Android 16)** — 2 Gbps capture/archive class, irrelevant to cellular uplink.
- **Hosted relay service** (still rejected from pass 1) — crowded $10-$179/mo field; answer IRL Pro's free relay with self-host docs instead. Also still rejected: Larix SDK licensing (cost/provenance), HaishinKit.kt (RTMP-only on Android), wholesale Moblin port (Swift), srtla_rec as recommended receiver (upstream-declared unsupported), WHIP-first transport (no audited evidence), plugin ecosystem/multi-user (absent from all 145 audited states).

## Sources

Engines, protocol, bonding
- https://github.com/ThibaultBee/StreamPack
- https://github.com/ThibaultBee/StreamPack/blob/3.2.0/gradle/libs.versions.toml
- https://github.com/ThibaultBee/srtdroid/releases
- https://github.com/Haivision/srt/releases/tag/v1.5.6
- https://github.com/Haivision/srt/security/advisories/GHSA-4mc6-qmpp-g7gw
- https://github.com/Haivision/srt/security/advisories/GHSA-6xg9-784j-24rm
- https://github.com/BELABOX/srtla
- https://github.com/eerimoq/moblin
- https://github.com/eerimoq/moblink
- https://github.com/e04/go-irl
- https://github.com/irlserver/irl-srt-server
- https://github.com/bluenviron/mediamtx
- https://github.com/pedroSG94/RootEncoder

Competitors / commercial
- https://softvelum.com/larix/android/
- https://irlpro.app/
- https://belabox.net/
- https://prismlive.com/
- https://www.camerafi.com/
- https://streamlabs.com/mobile-app
- https://github.com/irlhost/awesome-irl-streaming
- https://github.com/obsproject/obs-websocket
- https://github.com/obs-websocket-community-projects/obs-websocket-java

Platform / policy
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/16/features
- https://developer.android.com/media/camera/lowlight/low-light-boost-ae
- https://developer.android.com/ndk/reference/group/networking
- https://opensource.google/documentation/reference/using/agpl-policy
- https://github.com/signalapp/Signal-Android/blob/main/LICENSE
- https://f-droid.org/en/docs/Inclusion_Policy/
- https://f-droid.org/en/docs/Reproducible_Builds/
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html

Screenshot testing
- https://github.com/takahirom/roborazzi
- https://github.com/cashapp/paparazzi
- https://developer.android.com/studio/preview/compose-screenshot-testing-release-notes
- https://github.com/dropbox/dropshots
- https://github.com/ndtp/android-testify
- https://robolectric.org/device-configuration/
- https://applitools.com/docs/eyes/concepts/best-practices/match-levels
- https://android-ui-testing.github.io/Cookbook/basics/screenshot_testing/

Community signal (Arctic Shift archive; permalinks for citation only)
- reddit.com/r/Twitch/comments/1vhoy3c/irl_streaming_newbie/
- reddit.com/r/Twitch/comments/1rlyf97/switch_from_streamlabs_obs_to_moblin_on_twitch/
- reddit.com/r/Twitch/comments/1pbuad7/latest_ios_shreds_my_battery_bank_how_many_mah_do/
- reddit.com/r/Twitch/comments/1pfj1xv/brb_screen_using_irl_pro/
- reddit.com/r/Twitch/comments/1kv15h8/android_streaming_apps_that_support_ultrawide_lens/
- reddit.com/r/Twitch/comments/1myr0wz/ ("Moblin alternative fürs Android??")
- reddit.com/r/Twitch/comments/1oxb099/irlmobile_streamers_whats_your_favorite/
- reddit.com/r/obs/comments/1n12jb3/ (SRTLA receiver confusion)
- reddit.com/r/streaming/comments/1saavbg/ ("obs studio equivalent for android")

Project evidence
- `app-audit/app/application-identity.md`, `app-audit/app/components-and-intents.md`, `app-audit/app/permissions-and-appops.md`
- `ROADMAP.md` IS-22..IS-51 (deep audit, 2026-08-15)

## Open Questions

1. **Is there an authorised SRTLA receiver to test against end-to-end?** Bonding acceptance (IS-17) needs a reachable relay; candidates are self-hosted go-irl or irl-srt-server on the LAN — but Android 16's local-network permission rollout may bite LAN tests specifically.
2. **Play, F-Droid, or GitHub-first?** All three are viable with AGPL (verified above); the choice changes signing strategy (Play App Signing enrollment vs F-Droid keys vs the repo key) and should be made before IS-06/IS-20 land, not after.

Confidence: library versions, CVEs, platform deadlines, and Moblin/Larix feature claims are **Verified** against primary sources on 2026-08-15. Larix 1.5.11 date and SRT latency guidance are **Likely** (secondary sources). Reddit findings are sentiment evidence; r/Twitch comment-body search older than ~1 month was rate-limited, so comment-level coverage is partial (post-level coverage is all-time).
