# Untested and blocked cases

The following 24 discrete cases were not executed. They are not implied failures; each is a deliberate evidence boundary. All remain **UNKNOWN** unless copy or a disabled state is separately marked confirmed.

| ID | Case | Exact reason | What a future authorized test needs |
|---|---|---|---|
| U01 | First-run onboarding and permission prompts | App data could not be cleared, app could not be reinstalled, and permissions were not revoked | Disposable device/profile or explicit authorization to reset app state |
| U02 | Logged-in/logged-out/account states | No account surface was observed and authentication bypass/account probing is prohibited | Product-owner test account and documented entry path |
| U03 | Saved connection populated/edit/reorder/delete states | Saving could retain endpoint/credential-like data or change broadcast readiness | Isolated test endpoint, sanitized record policy, and deletion authorization |
| U04 | Real RTMP broadcast | Would transmit camera/audio to a remote destination | Owner-controlled ingest server and explicit approval |
| U05 | Real SRT/SRTLA/bonded broadcast | Requires external server(s), live network use, and licensed/proprietary behavior may apply | Authorized infrastructure, protocol requirements, and license decision |
| U06 | Link failure/failover/adaptive bitrate under load | Requires an active broadcast and network manipulation | Controlled lab network and disposable stream |
| U07 | Codec/profile/FPS receiver compatibility matrix | Requires sustained encoding/decoding against receivers and unsupported combinations may fail | Hardware matrix and owner-controlled receiver |
| U08 | Bluetooth audio route | Pairing external hardware changes device state | Approved headset/mic and restoration checklist |
| U09 | Acoustic quality of AEC/noise suppression | Would capture environmental audio and needs calibrated playback | Controlled acoustic fixture and consent |
| U10 | Recording output and low-battery stop | Creates user media; low-battery test is disruptive | Empty approved destination and power-controlled test phone |
| U11 | Snapshot output, naming, metadata, and errors | Creates a camera image | Empty approved destination and explicit media authorization |
| U12 | SAF grant persistence/write failures | Granting a folder changes persistent OS permission state | Disposable folder/tree and permission-revocation plan |
| U13 | Picture overlay file/URL load and refresh failure | Reads local/remote content and could expose private material | Sanitized fixture image and local test server |
| U14 | Arbitrary Text/HTML layer rendering | Unsafe HTML/content was intentionally not injected | Sanitized rendering fixture and security test scope |
| U15 | Standby/pause overlay trigger behavior | Requires entering runtime/broadcast states not safely activated | Disposable stream and explicit trigger procedure |
| U16 | Saved Web overlay populated/edit/delete states | Saving/loading remote content changes persistent WebView state | Local fixture server and disposable overlay record |
| U17 | WebView cookies, geolocation, debugging, and load errors | Cookie clearing is destructive; geolocation/debugging can expose data | Disposable profile, local fixture site, and explicit permission |
| U18 | Settings import/export and malformed payloads | Import may overwrite broad configuration; export may reveal secrets | Disposable app profile and redacted fixture payloads |
| U19 | QR scanning | External Barcode Scanner package was absent; installation prohibited | Approved scanner install or built-in test scanner |
| U20 | External Help/Discord/USB links | Opening browser/Discord may expose signed-in state or leave app scope | Clean browser profile or supplied destination URLs |
| U21 | Camera debug-detail sharing | Share sheet could disclose device/camera diagnostics to third parties | Redaction policy and controlled share target |
| U22 | Reset settings and Clear WebView cookies | Both are destructive to persisted user state | Disposable app profile and explicit authorization |
| U23 | USB camera, Horizon demo, and alternate Camera API | Controls were disabled and compatible hardware/software was unavailable | Supported device/UVC camera and documented capability conditions |
| U24 | Lock Screen, notification Start/Exit, torch, long-press Start, and tap-focus/exposure/WB | Could block input, stop service/start transmission, alter camera hardware, or has unclear side effect | Recovery path, disposable connection state, and explicit approval |

Additional environmental gaps—not counted as separate cases above—are offline behavior, locale/font-scale variants, TalkBack traversal, multi-window, device rotation, process death under pressure, reboot persistence, and large populated lists. These need a controlled device matrix; they were not reachable without changing system/app state.
