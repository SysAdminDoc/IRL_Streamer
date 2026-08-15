# Observable behavior specification

## Launch and live pipeline

- **CONFIRMED:** a true force-stop launch routes through `LaunchActivity` into `StreamerServiceActivity`; `am start -W` reported `LaunchState: COLD`, `TotalTime: 330 ms`, `WaitTime: 347 ms` on the test phone.
- **CONFIRMED:** there is no observed branded splash. The retained launch recording starts at a black live-console surface, shows the overlay controls, displays an `H.264, 1920x1080` toast, then fills the preview as camera frames arrive. FPS briefly shows 28 before settling at 30.
- **CONFIRMED:** the idle pipeline opens camera 0, configures 1920×1080 H.264 at 6,000,000 bps, 30 fps, two-second keyframes, AAC stereo at 44.1 kHz and 96 kbps, then reports audio/video capture `STARTED` in the app-visible Log tab.
- **CONFIRMED:** camera/audio capture begins for preview even when no broadcast connection is active. “Current Adaptive Bitrate” remains `0bps` while idle.

## Main console controls

| Control | Default/observed effect | Classification | Evidence |
|---|---|---|---|
| Settings gear | opens Settings root | CONFIRMED | 001→002 |
| Reload | toast says chat and all web overlays are reloading | CONFIRMED | 143 |
| Overflow | opens persistent tabbed quick-settings overlay | CONFIRMED | 130–138 |
| Snapshot/aperture | intended to take a picture | CONFIRMED label by resource/effect context; output UNTESTED | 001 XML; Recording settings |
| Flip camera | 73° rear → 67° front; second tap returns rear | CONFIRMED | 140 |
| Microphone | active meter/green → muted meter/gray and selected white mute button; second tap restores | CONFIRMED | 139 |
| Start pill | with zero active connections opens blocking `Start Broadcast?` dialog | CONFIRMED | 142 |
| Lens pills | select physical camera directly; 103° rear and 67° front states observed | CONFIRMED | 140–141 |
| Back | backgrounds the task to launcher; foreground-service process remains | CONFIRMED | lifecycle evidence |

The screenshot control is intentionally **UNTESTED** because it creates user media. Start/stop with a real destination is **UNTESTED** because it would contact a remote service and transmit camera/audio data.

## Quick settings

### Camera

- Physical camera list: id 0 rear 73° (selected by default), id 1 front 67°, id 2 rear 103°, id 3 front 61°.
- Active camera and Torch switches are exposed. Torch was not changed.
- Exposure compensation defaults to 0; Zoom defaults to 1.00×.
- Focus mode, White balance, and Anti-flicker rows use green check affordances and share the same choice contracts as Video settings.
- Tap behavior toggles Focus, Exposure, and White balance independently; all three were off. Their actual on-preview gesture effects were not tested.

### Network

- Conditioner displays `Off` while idle.
- Target Video Bitrate defaults to 6.0 Mbps; double-tapping the label resets to the start value.
- Current Adaptive Bitrate is 0 bps with no active stream.
- Cellular, Wi-Fi, and Ethernet/USB links are enabled; each weight is 100 in the restored state.
- `Stats` is present but visually empty while idle.

### Display, overlays, audio, and log

- Grid, Safe margins, and Lock Screen are off in the quick panel. Lock Screen says it blocks all screen touches for rain/water use and was not enabled.
- Overlay master is on; the built-in Timestamp checkbox is off. Long-pressing text overlays is documented as the edit gesture.
- Audio exposes Input gain at 0.0 dB and documents double-tap-to-reset.
- Log is a single scrollable diagnostic text area. It says file logging is off and directs the user to Recording settings to enable it.

## Settings hierarchy and defaults

### Streamer options

- Twitch Username and Kick Username are blank. Blank disables the associated chat/view-count/title use.
- Platform icons off; bots off; commands off; smaller chat box off; increased left margin off; right-side chat off.
- Show user count on.
- Custom chatbox URL blank; setting it overrides username-driven chat.
- Streamlabs/Toonation API key fields blank; Streamelements dashboard off; custom page URL blank.
- Chat font scale 220; alert dashboard scale 280.

### Bonding

- The screen explains own-server SRTLA and built-in connection bonding.
- Cellular, Wi-Fi, and Ethernet/USB are enabled with weight 100 each.
- Weights are relative and can be changed live in Network quick settings.
- A temporary Wi-Fi weight of 50 updated the UI and was restored to 100.

### Connections

- Empty list exposes New connection and Manage connections; Manage is also empty.
- Generic form has Name and URL plus Twitch/Kick quick-setup buttons. Protocol-specific fields appear after entering a recognized URL.
- RTMP help specifies `rtmp://server/application/streamkey` and a YouTube-shaped example.
- Target types: Default (no authorization), RTMP authorization, Akamai/Dacast, Limelight Networks, Periscope Producer.
- RTMP authorization adds Login and Password.
- Twitch quick setup asks for username and stream key. Kick asks for username, stream URL, and stream key.
- Save stays disabled with required URL empty. Every draft was canceled; no connection record exists.

### Video

- Multi-camera capture off; startup camera rear (0); resolution 1920×1080 (16:9); FPS System default; orientation Landscape.
- Live rotation disabled and unavailable in the captured state; Reverse Orientation off; double-tap flip off.
- Background streaming is always on and disabled as a control.
- Focus continuous autofocus; white balance Auto; anti-flicker Auto; exposure compensation 0.
- Resolution choices captured from 1920×1080 down through 640×360, including square and 4:3 options.
- FPS choices include System default, fixed 10/15/24/30/60/120 rates and multiple variable ranges where supported; exact list is preserved in screens 044–046.
- Bitrate follows resolution on; H.264 bitrate 6000 Kbps; HEVC auto-adjust described as 66%; bitrate mode System default; keyframe 2 sec; format Auto.
- Bitrate-mode choices: System default, constant quality, variable, constant, and constant with frame drops (warned as often unsupported).
- H.264 profiles: System default, Baseline, Constrained Baseline, Main, High. HEVC profiles: System default, Main, Main 10, HDR10, HDR10+.
- Auto-adjust bitrate by format on. Electronic stabilization displays On; optical stabilization uses a choice row; noise reduction Fast.
- Adaptive mode summary recommends automatic selection. Alternatives include IRL Pro SRT/SRTLA/Bonding and three legacy algorithms. Adaptive frame rate off.

### Audio

- Prefer Bluetooth mic/headset off; source Camcorder; Stereo; bitrate Auto; sample rate 44100 Hz.
- Bitrate choices: Auto, 16, 24, 32, 64, 96, 128, 160, 256, 320 Kbps.
- Sample-rate choices: 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000 Hz.
- Keep Speakers Awake off. Input gain range is -40 to +10 dB, default 0.0 dB; maximum was tested and reset.
- Acoustic Echo Canceler and Noise Suppressor are off; the device reports Qualcomm Fluence implementations.

### Recording

- Record stream off. Warning: recording stops around critically low battery (~7%).
- Split into sections on, duration 30 minutes.
- Snapshot JPEG, quality 90; choices JPEG/PNG/WebP and 100/95/90/85 quality.
- SAF off; destination summary `DCIM/IRLPro, Podcasts/IRLPro`.

### Display

- Audio meter on; 3×3 grid off; safe margins off.
- Margin ratios are multi-select: 1:1, 5:4, 4:3, 3:2, 14:9, 16:9 (selected), 2:1, 19.5:9, 21:9.
- Indent range 0–20%, default 5%.

### Text/picture overlays

- Show layers on preview on; standby mode on.
- Built-in Timestamp is inactive by default, periodic refresh on at 1 second, scale 20%, center position, z-order 5.
- Template: `<p style='font-family:sans-serif;font-size:x-large; color: #66ff66;'><b><%date('MMM dd, HH:mm:ss', 'en_US') %></b>`.
- New layers can be Picture or Text. Picture accepts URL or file and periodic refresh. Text accepts HTML, periodic refresh, active, optional scale, position, and z-order.
- Positions use preset corners/center in the editor family; layers with higher z-order render in front.
- Standby and Pause overlay selectors are multi-select lists; Timestamp was unchecked in both.

### Web overlays

- Screen warns that no more than 3–4 web overlays are recommended for performance.
- New overlay fields: Name, URL, Custom CSS, View mode, Position, z-order, Width, Height, Scale.
- Default CSS makes body transparent with zero margin and hidden overflow.
- Defaults: Preview + stream, Center, z-order 1, 1280×720 on a 1080p canvas, scale 100%.
- View modes: Preview + stream, Preview only, Stream only.
- Positions: top-left, top-right, center, bottom-left, bottom-right, custom. Custom exposes horizontal and vertical position sliders/percentages.
- Manage web overlays is empty and Delete multiple disabled.

### Import/export, Advanced, Help

- Import uses a `larix://` text dialog. QR scanning depends on an external Barcode Scanner package and asks to install it.
- Advanced defaults: status bar on broadcast off, SRTLA debug off, RTMP HEVC off, experimental SRTLA tweaks off, mirror front off, network-check bypass off, custom encoder buffer off, WebView debugging off, web-overlay geolocation off.
- Keep streaming when not in focus is always on/disabled. Preferred Camera API, Horizon demo, and USB Camera are disabled on this phone. Volume keys default to Do nothing; choices also include broadcast, Zoom, and Flip camera.
- Help exposes Discord, camera debug-detail sharing, Reset settings, and About. About confirms version and licensed SRTLA notice.

## Gestures and feedback

- Vertical swipes scroll preference screens, dialog lists, and quick-panel bodies.
- Horizontal tab movement is implicit in quick-panel tab positions; later tabs become visible as the selection advances.
- Double tap is explicitly documented for target bitrate reset, input-gain reset, and optional camera flip.
- Long press is explicitly documented for editing text overlays; the Start control is also long-clickable in UI Automator, but its long-press effect is **UNKNOWN**.
- No haptic feedback was measured; treat it as **UNKNOWN**.
- Navigation/dialog animations were visually platform-like but not frame-timed; normalized motion tokens are estimates, not confirmed facts.

