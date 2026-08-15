# Design QA

## Target and method

- Source: `../app-audit/evidence/screenshots/*.png`
- Implementation: native Android UI in `app/src/main/java/com/irlstreamer/reconstruction/ui/`
- Comparison viewport: 2316 × 1080 captured pixels, 450 dpi, landscape, dark mode, font scale 1.0, three-button navigation
- Device: isolated headless AVD `issue-sweep-api36` (`emulator-5554`)
- Method: each audit source and replica capture was combined into labeled side-by-side, overlay, heat-map, and JSON metric evidence
- Second pass added a **geometry gate** (`scripts/geometry_diff.py`) that compares the replica UI hierarchy against the audit UI hierarchy element by element. The strict pixel gate remains unmasked; masks feed only a clearly labelled secondary app-chrome metric (`validation/masks/mask-register.csv`)

## Full-view evidence reviewed

Representative combined comparisons inspected during iteration:

| Region/state | Combined evidence | Finding |
|---|---|---|
| Settings root | `validation/side-by-side/002_settings_root.png` | Repeated row geometry is close; identity/system glyph differences remain |
| Display switches | `validation/side-by-side/083_display_settings.png` | Right navigation inset and AppCompat-like switch geometry corrected |
| Web-overlay form | `validation/side-by-side/108_web_overlay_new_form.png` | Form indentation, multiline CSS row, summary rhythm, and underline treatment corrected |
| About dialog | `validation/side-by-side/129_about_irl_pro.png` | Modal geometry retained; identity/legal copy intentionally differs |
| Camera quick panel | `validation/side-by-side/130_live_console_overflow.png` | Panel, tabs, controls, start pill, telemetry, and vertical row rhythm corrected to measured bounds |
| Network quick panel | `validation/side-by-side/132_live_console_network.png` | Stacked values, slider/hint spacing, panel bounds, and right-aligned summaries corrected |

## Iterations and issue disposition

1. **P1 — settings content began under the status bar.** Added the measured 26.67 dp status region; representative settings SSIM moved from approximately 0.826 to 0.894.
2. **P1 — controls ignored the right 48 dp navigation inset.** Corrected settings switches, live panel, telemetry, and console controls to the audited usable-content edge.
3. **P2 — Material switches/sliders were visibly oversized.** Replaced them with measured 46.93 × 27.02 dp semantic switches and thin AppCompat-like sliders.
4. **P2 — forms used Material text-field indentation and wrong row heights.** Introduced a measured basic field renderer, 53.69/72.53 dp rows, and matching title/summary line rhythm. State 108 reached SSIM 0.929665.
5. **P2 — quick-panel rows were compressed and horizontally displaced.** Rebuilt Camera/Network row timing from XML bounds and moved the 320 dp panel to x=395.38 dp. The row labels and controls now align closely in the combined evidence.
6. **P1 — hot state injection retained stale scroll position.** Keyed list state to `debugScreenId`, verified adjacent scroll captures differ, and reran all 145 states.

### Second pass — defects the geometry gate exposed that SSIM could not

7. **P0 — every modal was mispositioned and oversized.** Two independent faults: `Dialog(usePlatformDefaultWidth = false)` centred on the platform-fitted dialog window, which starts at x=131 px on the AVD and pushed all 52 modals 28 px right of the audited centre; and the audit's 1369 px hierarchy bounds were taken as the drawn width when the visible `#424242` surface is 1279 px (a 45 px decor inset per side). Re-derived every dialog token from the visible surface — see `docs/measured-tokens.md`. Modal bounds now land within 1–2 px of the audit.
8. **P0 — most settings captures were at the wrong scroll offset.** The differ showed `dleft` exactly 0 with `dtop` 240–600 px out, which is a scroll signature, not a layout fault. Replaced hand-guessed list indices with anchors extracted from the audit hierarchy for all 108 scrollable states (`scripts/extract_scroll_anchors.py`). Selection dialogs gained 0.06–0.07 SSIM.
9. **P1 — screens 027 and 112 were the wrong component.** The hierarchy shows a bare anchored `ListView` popup with no title and no button bar, not a centred AlertDialog. Implemented `AuditedSpinnerPopup` at the audited absolute bounds.
10. **P1 — the connection form used preference rows instead of text fields.** The audit shows full-width `EditText`s with floating labels and underlines at a 157 px pitch, and a *single* hint slot whose text changes with the URL. Split into `ConnectionFormField` so the layer and web-overlay forms — which the audit really does render as preference rows — keep their existing, higher-scoring treatment.
11. **P2 — copy and fixture drift.** The differ's unmatched-label report surfaced real text defects (truncated summaries, `Bluetooth` vs `bluetooth`, a missing `(SRTLA)` suffix, punctuation) and fixture values that differed from the audit (telemetry `-283 mA / -1144 mW / 31.6 °C`, sample RTMP host).
12. **P2 — quick-panel tab strip and log.** Derived the tab strip's horizontal offset per state from the audited tab bounds, and made the LOG tab anchor to its newest entry as the audit shows.
13. **P1 — keyboard states were missing.** Screens 055, 057 and 077 were captured with the field focused and the IME raised; the replica now focuses the field and raises the keyboard.

## Remaining blockers

- The audited live-console camera pixels are evidence-only and cannot ship. The original low-light scene is replaced by an independently generated neutral preview, which materially lowers whole-screen SSIM for live states.
- Product name, package, About/help text, launcher identity, author/licensing claims, and third-party marks intentionally differ because authorization to reuse them was not supplied.
- Samsung status/navigation glyphs and system-owned surfaces differ from the Android 16 emulator image.
- System font rasterization and several library icons are not pixel-identical to the source device.
- Additional app-owned dialog/menu typography and component raster details remain below the strict 0.985 threshold.

## Metric result

Current numbers are generated, not transcribed — see `validation/reports/final-coverage-report.md`
(pixel gates, per-surface medians, ten weakest states) and
`validation/reports/geometry-validation-report.md` (element-origin accuracy).

A structural correction can *lower* whole-screen SSIM: fixing screen 027's popup
exposed background-form differences the oversized centred dialog had been covering.
The geometry gate is the arbiter for that class of change.
