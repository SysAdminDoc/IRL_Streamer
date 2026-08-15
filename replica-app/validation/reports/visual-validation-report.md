# Visual validation report

Generated: 2026-08-15 05:33:22 -04:00
Replica device: `emulator-5554`
States captured: 2
Passed configured threshold: 0
Below threshold or errored: 2

| Screen | Status | SSIM | Threshold | Alignment dx,dy |
|---|---:|---:|---:|---:|
| 002_settings_root | FAIL | 0.897824 | 0.985 | -1,-2 |
| 129_about_irl_pro | FAIL | 0.88827 | 0.985 | -2,1 |

A failed metric is not masked or waived automatically. Inspect the matching files in `side-by-side/`, `overlays/`, and `diffs/` and record any legitimate platform-only variance before changing a threshold.
