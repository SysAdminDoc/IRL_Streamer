# Recommended Android architecture

This document separates observed facts from a clean-room implementation recommendation. It does not claim the original app uses these technologies.

## Observed constraints

- **CONFIRMED:** the installed app supports API 28+ and targets API 34; it runs on Android 16.
- **CONFIRMED:** a foreground camera/microphone service keeps the preview pipeline active and publishes a low-importance ongoing notification with Start and Exit actions.
- **CONFIRMED:** the product is landscape-first, has a full-bleed camera preview plus composited overlays, a dense runtime control console, a large preference hierarchy, persistent configuration, SAF folder selection, a WebView overlay family, and a `larix:` import deep link.
- **CONFIRMED:** physical camera identity/capability matters; the reference phone exposes four front/rear FoV choices.
- **UNKNOWN:** the original framework, database, networking stack, media libraries, backend, serialization format, and transport implementation.

## Proposed module boundaries

| Module | Responsibility | Rationale |
|---|---|---|
| `app-shell` | Process entry, navigation, insets/orientation, dependency wiring | Keeps activity/task behavior separate from media state |
| `live-console` | Preview UI, telemetry, controls, quick-settings panel, accessibility semantics | High-frequency ephemeral UI should not share persistence logic |
| `settings` | Category screens, forms, dialogs, validation, import/export orchestration | Most observable screens share preference components |
| `camera` | Camera discovery, selected physical lens, focus/WB/exposure/zoom/torch, capture lifecycle | Device-dependent behavior needs a capability abstraction |
| `media-core` | Video/audio capture, encoding, muxing, recording/snapshot commands | Long-running work must be testable outside UI and owned by the service |
| `streaming-api` | Transport-neutral connection/session state machine | Prevents UI from depending on a specific RTMP/SRT library |
| `transport-rtmp`, `transport-srt`, `transport-bonding` | Protocol adapters behind the streaming API | Licensing/interoperability can be decided independently |
| `overlays` | Text/picture model, renderer, selectors, z-order composition | Separates safe template/content handling from camera rendering |
| `web-overlays` | Sandboxed WebView lifecycle, CSS injection policy, cookies/geolocation controls | Security/privacy boundary with explicit process/lifecycle tests |
| `data` | Typed preferences, Room entities, secret storage, migrations | Separates persistent records from runtime telemetry/drafts |
| `platform` | Permissions, notification actions, SAF, deep links, external intents | Centralizes Android-version behavior and intent validation |
| `diagnostics` | Redacted ring-buffer log, debug export, performance counters | Prevents secrets/URLs from leaking into user-visible diagnostics |

These are conceptual Gradle boundaries; begin with fewer physical modules if build overhead outweighs isolation, while retaining the interfaces.

## UI recommendation

Use Kotlin and Jetpack Compose with Material 3 primitives for settings, dialogs, and semantics, customized to the measured dark tokens. Use a single host activity for settings/console navigation unless a foreground-service trampoline or external intent requires a dedicated entry surface. Keep the preview/rendering surface in an Android View (`SurfaceView`/`TextureView`) when required by Camera2/MediaCodec and embed it through Compose interoperability.

Do not scale screenshots or reproduce Android system surfaces. Recreate component behavior from dp/sp tokens, draw behind the app's safe landscape content only where observed, and let DocumentsUI/toasts/permission surfaces remain OS-owned. Minimum semantic targets should be 48 dp even when visual glyphs remain smaller.

## Camera and media recommendation

Prefer direct Camera2 for physical-camera selection and fine control fidelity; CameraX with `Camera2Interop` is viable only if a capability spike proves all observed physical lens, FPS-range, WB, exposure, stabilization, multi-camera, and Surface sharing behavior. Keep camera capability discovery authoritative and never hard-code the four reference-camera IDs.

Use `MediaCodec`/`MediaMuxer` and `AudioRecord` behind interfaces with explicit state machines. The foreground service—not the activity—owns camera/audio/encoder/transport lifetimes. Model commands (`Prepare`, `Preview`, `StartBroadcast`, `StopBroadcast`, `StartRecording`, `Snapshot`, `Exit`) and observable states (`Idle`, `Preparing`, `Previewing`, `Connecting`, `Live`, `Recovering`, `Stopping`, `Failed`). Make Start idempotent and require an active validated connection before transition to `Connecting`.

## State and concurrency

Use coroutines and `StateFlow` for unidirectional state:

- persisted configuration is read-only state plus transactional commands;
- form drafts live in screen-scoped state and commit only on Save;
- service/session telemetry is ephemeral and rate-limited for UI collection;
- capability state is recomputed when the selected camera/audio route changes;
- one reducer/state machine serializes media commands to prevent duplicate Start/Stop races.

Compose navigation state and service state must be independent: returning to the console rebinds to the existing service instead of recreating media. Persist only stable user intent, never transient encoder/session status.

## Persistence and secrets

- Use typed DataStore for singleton settings with explicit schema versions/migrations.
- Use Room for ordered connections and overlay-layer collections, including stable IDs, active flags, subtype fields, order, and transactions.
- Encrypt stream keys, passwords, dashboard API keys, and any exportable secrets with Android Keystore-backed encryption. Avoid logging or exposing them through Compose state snapshots.
- Store SAF tree URIs plus persisted URI permissions; validate access before every write and surface recoverable errors.
- Version import/export, validate fully before commit, support dry-run summaries, and define whether secrets are omitted, separately encrypted, or never exportable.

## Permissions and platform behavior

At minimum, design for camera, microphone, notifications on Android 13+, foreground-service camera/microphone types, internet/network state, Bluetooth audio on relevant releases, wake/Wi-Fi locks only while justified, SAF, and optional coarse/fine location only when a user enables WebView geolocation. The observed location denial must not break the default app.

Use explicit immutable `PendingIntent`s, validate `larix:` input size/schema/version before parsing, constrain exported components, and require deliberate confirmation for external intents, diagnostic sharing, cookie clearing, Reset, and Exit.

## Web and overlay security

Treat HTML/CSS/URLs as untrusted. Define allowed URL schemes, mixed-content policy, file/content access, JavaScript bridges (prefer none), navigation behavior, TLS errors, renderer crashes, and data clearing. Use a separate WebView data-directory suffix or process only if the product needs stronger isolation and the Android support cost is accepted. Sanitize text templates; do not evaluate arbitrary code.

Render overlays in deterministic z-order on both preview and encoded output. Share the same normalized 1920×1080 logical canvas transform, then adapt to actual encoder/preview aspect ratios. Keep preview-only and stream-only visibility explicit.

## Transport and licensing boundary

RTMP, SRT, SRTLA, and bonding are not interchangeable. Select maintained libraries only after protocol interoperability, mobile lifecycle, ABI, security, and license review. SRTLA behavior and the displayed third-party notice are observed, but code provenance and reuse rights are not. Keep proprietary/uncertain behavior behind an interface and do not advertise parity until tested against owner-controlled infrastructure.

## Testing recommendation

- Pure state-machine tests for Start/Stop/reconnect/background/notification races.
- Repository migration/import validation and secret-redaction tests.
- Compose screenshot tests at the 823.47×384 dp reference plus small/large-font and inset variants.
- Accessibility tests for names, roles, selected/muted state, 48 dp semantics, traversal, and live-region error/status announcements.
- Camera/media instrumentation across a capability matrix, with fake surfaces/transports for deterministic CI.
- Local fixture servers for RTMP/SRT and WebView success/failure; no production endpoints in tests.
- Long-run thermal, memory, dropped-frame, reconnect, and process-death tests on physical devices.

## Recommended baseline

Use minSdk 28 to match the observed product unless product requirements deliberately broaden it; compile/target the current stable Android SDK at implementation time. Select library versions only when rebuild starts because versions are time-sensitive. Hilt (or a small manual graph), Room, DataStore, coroutines, and Compose are recommendations—not observed internals.
