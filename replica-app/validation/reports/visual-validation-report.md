# Visual validation report

Generated: 2026-08-15 05:06:10 -04:00
Replica device: `emulator-5554`
States captured: 5
Passed configured threshold: 0
Below threshold or errored: 5

| Screen | Status | SSIM | Threshold | Alignment dx,dy |
|---|---:|---:|---:|---:|
| 001_streamer_home_default | FAIL | 0.618499 | 0.985 | -1,0 |
| 132_live_console_network | FAIL | 0.675959 | 0.985 | -1,0 |
| 138_live_console_log | FAIL | 0.575241 | 0.985 | -1,1 |
| 085_streamer_home_safe_margins_enabled | FAIL | 0.584629 | 0.985 | 0,0 |
| 141_live_console_rear_ultrawide | FAIL | 0.437146 | 0.985 | -1,1 |

A failed metric is not masked or waived automatically. Inspect the matching files in `side-by-side/`, `overlays/`, and `diffs/` and record any legitimate platform-only variance before changing a threshold.
