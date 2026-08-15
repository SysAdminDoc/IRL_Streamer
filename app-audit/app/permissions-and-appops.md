# Permissions and app operations

## Declared permissions

The package declared the following permissions (**CONFIRMED**, `dumpsys package`):

- `CAMERA`
- `RECORD_AUDIO`
- `POST_NOTIFICATIONS`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `CHANGE_NETWORK_STATE`
- `CHANGE_WIFI_STATE`
- `MODIFY_AUDIO_SETTINGS`
- `BLUETOOTH`
- `BROADCAST_STICKY`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_CAMERA`
- `FOREGROUND_SERVICE_MICROPHONE`

## Runtime grants on the test phone

| Permission | State | Observable purpose | Classification |
|---|---|---|---|
| Camera | granted / foreground app-op | camera preview and capture pipeline | CONFIRMED |
| Record audio | granted / foreground app-op | audio capture and meter | CONFIRMED |
| Notifications | granted | ongoing foreground-service notification | CONFIRMED |
| Fine location | denied | optional Web overlay geolocation | CONFIRMED state; purpose supported by screen 124 |
| Coarse location | denied | optional Web overlay geolocation | CONFIRMED state; purpose supported by screen 124 |

No permissions were revoked or newly granted during this audit. First-run permission prompts and denied-camera/microphone behavior are **UNKNOWN** because clearing app data and changing permission state were prohibited.

## Storage behavior

- **CONFIRMED:** the default destination summary is `DCIM/IRLPro, Podcasts/IRLPro`.
- **CONFIRMED:** “Use Storage Access Framework” is off by default; enabling it is described as necessary for external SD cards.
- **CONFIRMED:** tapping `Save to` opens Android DocumentsUI’s folder picker. No folder was selected.
- **STRONG INFERENCE:** a rebuild should use MediaStore for normal shared-media output and persistable SAF tree-URI grants for the optional custom/external destination.

## Rebuild permission rules

Request camera and microphone only when entering the capture experience, notification permission when required by the target SDK, and location only after the user explicitly enables web-overlay geolocation. Never request broad media-read permissions solely to write new captures. Keep the service types camera/microphone and provide a visible stop/exit path.

