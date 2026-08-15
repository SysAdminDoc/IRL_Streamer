# Known deviations

| ID | Scope | Deviation | Reason | Validation treatment | Status |
|---|---|---|---|---|---|
| D001 | All identity surfaces | The app is named **IRL Streamer**, uses package `com.irlstreamer.reconstruction`, and does not claim to be official IRL Pro. | Original name/package/official identity were not authorized for reuse. | Compare geometry while accepting documented copy differences. | ACCEPTED |
| D002 | Launcher icon and toast icon | A new generic streaming-camera identity replaces the observed magenta IRL Pro icon. | No authorized original asset was supplied. | Asset region is reviewed against equivalent dimensions/visual weight, not pixel identity. | ACCEPTED |
| D003 | Live console preview states 001, 084-086, 095, 130-145 | A deterministic independently generated preview replaces captured camera pixels. | Audit imagery is evidence-only and cannot ship; emulator camera output is nondeterministic. | Raw comparisons remain unmasked, so the legitimate replacement still lowers SSIM and remains visible in the report. | ACCEPTED_WITH_VISUAL_GAP |
| D004 | Third-party/service copy | Service-specific logos are omitted; locally simulated helper forms use text-only compatibility labels and never authenticate. | No brand artwork or backend authorization was provided. | Geometry and form behavior remain in scope; artwork/backend results are out of scope. | ACCEPTED |
| D005 | Streaming, bonding, recording, snapshots, remote overlays | Operations are safe deterministic simulations or explicitly disabled; no media or network transmission occurs. | Audit leaves these runtime contracts unknown and the rebuild authorization supplies no infrastructure. | Validate guards, states, copy, and local transitions only. | BLOCKED_EXTERNAL |
| D006 | Typography | Android system sans-serif/Roboto replaces any exact unknown font file. | The audit identified only a system-sans strong inference; no proprietary font was supplied. | Allow platform rasterization tolerance while matching size, weight, wrapping, and baselines. | ACCEPTED |
| D007 | DocumentsUI, permission prompts, toast chrome | System-owned surfaces are invoked rather than redrawn and may differ by emulator image. | System UI must not be copied as an app asset. | Validate the handoff/intent and separate app-owned UI from OS-rendered pixels. | ACCEPTED |
| D008 | Camera list/capabilities | The local fixture exposes the four audited lens choices regardless of emulator hardware; no physical Camera2 parity is claimed. | The reference device camera matrix is device-specific and unavailable on the AVD. | Validate deterministic selected states and labels; hardware output is blocked. | ACCEPTED |
| D009 | Telemetry/log content | Telemetry and log lines are deterministic, sanitized fixtures. | Live device diagnostics are dynamic and may contain device-specific/private details. | Values were not masked in the final sweep; positions and formatting are reviewed in combined evidence. | ACCEPTED_WITH_VISUAL_GAP |
| D010 | Accessibility | Core icon controls receive accessible names, state descriptions, and 48 dp semantic targets. | The audit confirms unlabeled/sub-48 dp source controls; reproducing those defects is not required. | Record as an intentional accessibility improvement. | ACCEPTED |
| D011 | About/help/legal text | Author, version, licensed-code, and official support destinations are replaced with reconstruction disclosures and local explanations. | Original identity, author claim, URLs, and SRTLA licensing are not authorized. | Validate modal structure and action behavior, not protected claims. | ACCEPTED |
| D012 | Lifecycle service | The replica keeps local UI/session state but does not run a camera/microphone streaming foreground service. | A real capture/transport pipeline would exceed the authorized local simulation scope. | Validate persistence, background/relaunch restoration, and no unintended transmission. | BLOCKED_EXTERNAL |
| D013 | Soft keyboard on screens 055, 057, 077 | The replica focuses the field and raises the AVD's IME; the audit screenshots show the Samsung IME's landscape fullscreen extract-edit window covering the app. | The IME is a separate system-owned application. The audit UI hierarchy proves the app dialog is present underneath, and that dialog is reproduced. | Keyboard *behaviour* is validated (field focused, IME raised). The IME window pixels are excluded from the secondary app-chrome metric and remain in the strict unmasked gate. | ACCEPTED_WITH_VISUAL_GAP |
| D014 | Third-party service names | Audited labels naming specific streaming platforms are replaced with neutral equivalents (for example "Twitch, Kick, etc." becomes "Platform A, Platform B, etc."). | Third-party marks were not authorized for reuse. | Row geometry and behaviour are validated; the substituted string differs in width and is excluded from label matching. | ACCEPTED |

## Final validation disposition

All 145 states have complete current, hierarchy, result, overlay, diff and
side-by-side artifacts, captured from a single build.

The configured SSIM threshold remains 0.985. No threshold was lowered and the
strict gate is computed on unmasked whole screens, so the masks introduced in
this pass cannot convert a failure into a pass. The result is **0 strict passes
and 145 retained failures**, with median SSIM 0.869631 (previous pass 0.836694)
and maximum 0.930838.

Masks exclude only operating-system-owned pixels — the status bar, the Samsung
navigation strip, and the IME window on screens 055, 057 and 077 — each declared
with a category and reason in `validation/masks/mask-register.csv`. They feed a
clearly labelled secondary app-chrome metric, whose median is 0.896448. The
camera preview is deliberately not masked.

Element-level accuracy is reported separately because whole-screen SSIM cannot
localise a fault: mean origin error 50.88 px, 34.9% of matched origins within
2 px, and 62 of 145 states with at least half their origins within 2 px. The
remaining error is concentrated in list content and scroll state rather than in
component geometry — the tokens themselves are locked to the audit evidence by
`AuditMetricsTest`.

Production streaming and capture behaviour remains blocked by D005 and D012.
Pixel-equivalent reconstruction remains blocked by D001-D004, D006-D009, D011,
D013 and the remaining app-owned raster differences recorded in `design-qa.md`.
