# Known deviations

| ID | Scope | Deviation | Reason | Validation treatment | Status |
|---|---|---|---|---|---|
| D001 | All identity surfaces | The app is named **IRL Streamer**, uses package `com.irlstreamer.reconstruction`, and does not claim to be official IRL Pro. | Original name/package/official identity were not authorized for reuse. | Compare geometry while accepting documented copy differences. | ACCEPTED |
| D002 | Launcher icon and toast icon | A new generic streaming-camera identity replaces the observed magenta IRL Pro icon. | No authorized original asset was supplied. | Asset region is reviewed against equivalent dimensions/visual weight, not pixel identity. | ACCEPTED |
| D003 | Live console preview states 001, 084-086, 095, 130-145 | In audited debug states a deterministic independently generated preview replaces captured camera pixels. Outside the harness the console shows the device camera through the broadcast engine (CameraX in v0.3.0, StreamPack from v0.4.0). | Audit imagery is evidence-only and cannot ship; emulator camera output is nondeterministic. | Raw comparisons remain unmasked, so the legitimate replacement still lowers SSIM and remains visible in the report. | ACCEPTED_WITH_VISUAL_GAP |
| D004 | Third-party/service copy | Service-specific logos are omitted; locally simulated helper forms use text-only compatibility labels and never authenticate. | No brand artwork or backend authorization was provided. | Geometry and form behavior remain in scope; artwork/backend results are out of scope. | ACCEPTED |
| D005 | Bonding, recording, snapshots, remote overlays | These remain safe deterministic simulations. **RTMP broadcasting is real since v0.4.0**: camera capture, H.264 encode and publish to the saved connection all run for real, verified against a local MediaMTX. Audio is not captured yet, so the stream is video only. | The audit leaves the remaining runtime contracts unknown and no authorized infrastructure was supplied for them. | Validate guards, states, copy and local transitions; the RTMP path is validated end to end against a local receiver. | PARTIALLY_RESOLVED |
| D006 | Typography | Android system sans-serif/Roboto replaces any exact unknown font file. | The audit identified only a system-sans strong inference; no proprietary font was supplied. | Allow platform rasterization tolerance while matching size, weight, wrapping, and baselines. | ACCEPTED |
| D007 | DocumentsUI, permission prompts, toast chrome | System-owned surfaces are invoked rather than redrawn and may differ by emulator image. | System UI must not be copied as an app asset. | Validate the handoff/intent and separate app-owned UI from OS-rendered pixels. | ACCEPTED |
| D008 | Camera list/capabilities | The local fixture exposes the four audited lens choices regardless of hardware; ids 1 and 3 open the front camera and 0 and 2 the back, but the FoV labels are not read from Camera2 and no per-lens parity is claimed. | The reference device camera matrix is device-specific and unavailable on the AVD. | Validate deterministic selected states and labels; hardware output is blocked. | ACCEPTED |
| D009 | Telemetry/log content | The battery/power/temperature block and the log panel are still deterministic, sanitized fixtures. **Narrowed since IS-109: the outgoing stream's own counters are real.** Uptime, written bytes, dropped-plus-lost packets and the current bitrate come from the endpoint's metrics while a broadcast is running, and the Network tab's "Current Adaptive Bitrate" shows the measured value. The audited captures run the simulated engine, which reports nothing, so they still read the fixture values. | Live device diagnostics are dynamic and may contain device-specific/private details. | Values were not masked in the final sweep; positions and formatting are reviewed in combined evidence. | ACCEPTED_WITH_VISUAL_GAP |
| D010 | Accessibility | Core icon controls receive accessible names, state descriptions, and 48 dp semantic targets. | The audit confirms unlabeled/sub-48 dp source controls; reproducing those defects is not required. | Record as an intentional accessibility improvement. | ACCEPTED |
| D011 | About/help/legal text | Author, version, licensed-code, and official support destinations are replaced with reconstruction disclosures and local explanations. | Original identity, author claim, and URLs are not authorized. The original's engine provenance is now known (see below) and its SRTLA licensing is a strong inference, but neither is reproduced here. | Validate modal structure and action behavior, not protected claims. | ACCEPTED |
| D012 | Lifecycle service | The replica keeps local UI/session state but does not run a camera/microphone streaming foreground service. | A real capture/transport pipeline would exceed the authorized local simulation scope. | Validate persistence, background/relaunch restoration, and no unintended transmission. | BLOCKED_EXTERNAL |
| D013 | Soft keyboard on screens 055, 057, 077 | The replica focuses the field and raises the AVD's IME; the audit screenshots show the Samsung IME's landscape fullscreen extract-edit window covering the app. | The IME is a separate system-owned application. The audit UI hierarchy proves the app dialog is present underneath, and that dialog is reproduced. | Keyboard *behaviour* is validated (field focused, IME raised). The IME window pixels are excluded from the secondary app-chrome metric and remain in the strict unmasked gate. | ACCEPTED_WITH_VISUAL_GAP |
| D015 | Third-party application names in copy | The QR-scanner prompt reads "Install QR Scanner?" where the audit names a specific scanner application. | The named application is a third-party product whose name was not authorized for reuse; the generic description carries the same meaning. | Modal structure, buttons and behaviour are validated; the substituted string differs in width and is excluded from label matching. | ACCEPTED |
| D014 | Third-party service names | Audited labels naming specific streaming platforms are replaced with neutral equivalents (for example "Twitch, Kick, etc." becomes "Platform A, Platform B, etc."). | Third-party marks were not authorized for reuse. | Row geometry and behaviour are validated; the substituted string differs in width and is excluded from label matching. | ACCEPTED |

## Provenance of the audited original

Recorded 2026-08-15 from evidence already in the audit, so it is not re-derived each pass.

Every audited class carries the `com.wmspanel.streamer.*` prefix, the deep-link scheme is
`larix:`, and the About screen declares "Includes licensed SRTLA code"
(`app-audit/app/application-identity.md`, `app-audit/app/components-and-intents.md`).
`com.wmspanel` is Softvelum, whose own application is `com.wmspanel.larix_broadcaster`, and
the `go-irl` SRTLA server lists IRL Pro (Android) as a compatible client. The original runs
**Softvelum Larix broadcaster code with BELABOX SRTLA bonding** — verified for the Softvelum
code, a strong inference for the commercial licensing route.

This changes nothing in the reconstruction: no Softvelum or BELABOX code is present, and the
non-affiliation stated in `README.md` and D001 stands. It matters only because it makes the
audit's engine question answered rather than open.

## Final validation disposition

All 145 states have complete current, hierarchy, result, overlay, diff and
side-by-side artifacts, captured from a single build.

The configured SSIM threshold remains 0.985. No threshold was lowered and the
strict gate is computed on unmasked whole screens, so masks cannot convert a
failure into a pass. As of v0.2.0 (2026-08-15) the result is **0 strict passes
and 145 retained failures**, with median SSIM 0.869499. Exact current figures are
generated into `validation/reports/final-coverage-report.md` rather than restated
here, because a hand-copied number goes stale silently.

Masks exclude only operating-system-owned pixels — the status bar, the Samsung
navigation strip, and the IME window on screens 055, 057 and 077 — each declared
with a category and reason in `validation/masks/mask-register.csv`. They feed a
clearly labelled secondary app-chrome metric, whose median is 0.899487. The
camera preview is deliberately not masked.

Element-level accuracy is reported separately because whole-screen SSIM cannot
localise a fault: 1103 element origins matched against the audit hierarchy with
174 unmatched, mean origin error 48.78 px, and 33.6% of matched origins within
2 px. The remaining error is concentrated in list content and scroll state rather
than in component geometry - the tokens themselves are locked to the audit
evidence by `AuditMetricsTest`.

Production streaming and capture behaviour remains blocked by D005 and D012.
Pixel-equivalent reconstruction remains blocked by D001-D004, D006-D009, D011,
D013 and the remaining app-owned raster differences recorded in `design-qa.md`.
