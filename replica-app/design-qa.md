# Design QA

## Target and method

- Source: `../app-audit/evidence/screenshots/*.png`
- Implementation: native Android UI in `app/src/main/java/com/irlstreamer/reconstruction/ui/`
- Comparison viewport: 2316 × 1080 captured pixels, 450 dpi, landscape, dark mode, font scale 1.0, three-button navigation
- Device: isolated headless AVD `issue-sweep-api36` (`emulator-5554`)
- Method: each audit source and replica capture was combined into labeled side-by-side, overlay, heat-map, and JSON metric evidence; no mask or waiver was applied

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

## Remaining blockers

- The audited live-console camera pixels are evidence-only and cannot ship. The original low-light scene is replaced by an independently generated neutral preview, which materially lowers whole-screen SSIM for live states.
- Product name, package, About/help text, launcher identity, author/licensing claims, and third-party marks intentionally differ because authorization to reuse them was not supplied.
- Samsung status/navigation glyphs and system-owned surfaces differ from the Android 16 emulator image.
- System font rasterization and several library icons are not pixel-identical to the source device.
- Additional app-owned dialog/menu typography and component raster details remain below the strict 0.985 threshold.

## Metric result

- States compared: 145
- Strict passes: 0
- Median SSIM: 0.836694
- Maximum SSIM: 0.930146
- Threshold: 0.985000

Final result: blocked
