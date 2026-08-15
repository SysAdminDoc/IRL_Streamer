# Package inventory

## Installation shape

**CONFIRMED:** the Play install consists of four APK splits; paths were recorded without opening or decompiling them:

- base APK
- `split_config.arm64_v8a.apk`
- `split_config.en.apk`
- `split_config.xxhdpi.apk`

The package manager reports 11 declared components, but `dumpsys package` does not expose a complete non-exported component list. Do not treat the observable names below as the full manifest.

## Observable components

| Component | Type | State/role | Classification | Evidence |
|---|---|---|---|---|
| `com.wmspanel.streamer.LaunchActivity` | Activity | MAIN/LAUNCHER entry and `larix:` VIEW handler | CONFIRMED | package resolver dump |
| `com.wmspanel.streamer.StreamerServiceActivity` | Activity | live camera/broadcast console | CONFIRMED | screens 001, 130–145 |
| `com.wmspanel.streamer.preference.SettingsActivity` | Activity | all observed settings and app-owned dialogs | CONFIRMED | screens 002–129 |
| `com.wmspanel.streamer.StreamerService` | Foreground service | camera/microphone pipeline; notification ID 101 | CONFIRMED | activity service and notification evidence |
| Chromium sandboxed process service | Isolated service | WebView process used while overlay/chat WebViews exist | CONFIRMED at runtime | target service evidence |
| Broadcast receivers | — | not safely enumerable from the retained black-box evidence | UNKNOWN | — |
| Content providers | — | not safely enumerable from the retained black-box evidence | UNKNOWN | — |

## Runtime facts

- **CONFIRMED:** the foreground service declares active camera and microphone service types (`0xC0`) while the preview is running.
- **CONFIRMED:** the service is `START_STICKY`-like from the observed `startCommandResult=1`; exact restart policy semantics remain a **STRONG INFERENCE** rather than an internal contract.
- **CONFIRMED:** an ongoing, private, low-importance notification uses channel `com.wmspanel.streamer.channel.foreground_service`, display name “Foreground service,” notification ID 101, timeout 72 hours, and actions “Start” and “Exit.” The shade was not opened to avoid exposing unrelated notifications.
- **CONFIRMED:** no target-package AlarmManager entries were present. JobScheduler evidence showed no concrete scheduled job for the target; only package/top-app accounting lines were present.
- **CONFIRMED:** the app opened two WebViews in the measured idle session, consistent with chat/overlay infrastructure.
- **CONFIRMED:** target-PID logs contain New Relic Android instrumentation tags and a collector connection failure. The audit did not inspect telemetry payloads or configuration.

## Orientation

**CONFIRMED observable behavior:** launcher and settings paths resolve into a landscape window even when launched from a portrait home screen. Exact manifest `screenOrientation` declarations are **UNKNOWN**.
