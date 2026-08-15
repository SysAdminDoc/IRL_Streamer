# Open questions

These 26 questions are unresolved by safe black-box evidence. None should be silently answered during reconstruction.

| ID | Question | Current classification | Evidence needed |
|---|---|---|---|
| Q01 | Is there any first-run onboarding beyond Android permission prompts? | UNKNOWN | Fresh disposable app profile without touching the audited user state |
| Q02 | Are there account, entitlement, subscription, or logged-out surfaces? | UNKNOWN | Product requirements or authorized test account |
| Q03 | Is connection Name required, auto-generated, or only a display label? | UNKNOWN | Safe save attempt in a disposable profile |
| Q04 | How are duplicate connection names/URLs handled? | UNKNOWN | Two sanitized disposable connection records |
| Q05 | Can connections be reordered, and what controls active/inactive state? | UNKNOWN | Populated Manage connections list |
| Q06 | How are stream keys/passwords masked, stored, exported, and redacted? | UNKNOWN | Security requirements and disposable secret fixture; no real credentials |
| Q07 | Which URL schemes beyond RTMP are accepted, and what fields does each reveal? | UNKNOWN | Public protocol specification or sanitized fixtures for SRT/SRTLA/other schemes |
| Q08 | What is the precise Start/Stop state machine, including reconnect and duplicate taps? | UNKNOWN | Owner-controlled ingest server and runtime trace |
| Q09 | What do notification Start and Exit do in each task/service state? | UNKNOWN | Disposable connection state and explicit authorization |
| Q10 | What is the background inactivity timeout and its cancellation/restart behavior? | UNKNOWN | Timed background test with no private launcher/notification capture |
| Q11 | Which camera/resolution/FPS/codec combinations are rejected, downgraded, or hidden per device? | UNKNOWN | Multi-device capability matrix and receiver tests |
| Q12 | What exactly does “Preferred camera API” choose on supported devices? | UNKNOWN | Supported device where the row is enabled |
| Q13 | How are multi-camera capture and Horizon integration presented when available? | UNKNOWN | Compatible device/software and product documentation |
| Q14 | What are recording/snapshot filenames, containers, metadata, collision rules, and storage errors? | UNKNOWN | Authorized empty storage destination and media-creation test |
| Q15 | Does critical-battery recording stop at a fixed threshold or Android low-battery signal? | UNKNOWN | Controlled battery test or owner specification |
| Q16 | Which text-template functions besides `date` are supported, and how are malformed templates escaped? | UNKNOWN | Template-language specification and security fixtures |
| Q17 | How are picture-layer load, cache, refresh, timeout, and broken-image states represented? | UNKNOWN | Local fixture server/file and controlled failure cases |
| Q18 | What runtime events activate Standby and Pause layer sets? | UNKNOWN | Product definition plus disposable stream/session |
| Q19 | How do web overlays report HTTP, TLS, JavaScript, geolocation, render-process, and offline failures? | UNKNOWN | Local fixture pages and controlled network matrix |
| Q20 | What does settings export contain, especially secrets and SAF URIs, and is it versioned/encrypted? | UNKNOWN | Disposable profile, redacted export, and format ownership decision |
| Q21 | Is `larix://` import transactional; how are malformed, partial, older, or newer payloads handled? | UNKNOWN | Sanitized payload corpus and disposable profile |
| Q22 | What exact destinations/actions do Discord, USB help, and camera-debug sharing invoke? | UNKNOWN | Clean browser/share target or owner-supplied URLs/intents |
| Q23 | Does Reset show confirmation, what scope does it clear, and does it preserve connections/media grants? | UNKNOWN | Disposable profile and explicit destructive-test approval |
| Q24 | What analytics, crash reporting, remote configuration, licensing, or backend APIs are product requirements? | UNKNOWN | Owner-provided requirements; black-box traffic interception was prohibited |
| Q25 | What are required behavior and layouts for TalkBack, large fonts, alternate locales, gestures, tablets, foldables, and multi-window? | UNKNOWN | Accessibility/localization/device matrix and product targets |
| Q26 | Which names, icons, third-party service marks, SRT/SRTLA code, and other assets are licensed for reuse? | UNKNOWN | Legal review, asset provenance, and protocol/library license inventory |

Questions Q06, Q08, Q20, Q21, Q24, and Q26 are release blockers because a guess could create a security, interoperability, privacy, or licensing defect. The rest can be isolated behind interfaces or deferred feature flags, but acceptance criteria must continue to label them unverified.
