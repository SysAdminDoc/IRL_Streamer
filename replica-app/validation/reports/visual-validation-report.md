# Visual validation report

Generated: 2026-08-15 04:29:29 -04:00
Replica device: `emulator-5554`
States captured: 10
Passed configured threshold: 0
Below threshold or errored: 10

| Screen | Status | SSIM | Threshold | Alignment dx,dy |
|---|---:|---:|---:|---:|
| 054_video_manual_bitrate_enabled | FAIL | 0.863997 | 0.985 | -2,2 |
| 018_bonding_link_weights | FAIL | 0.847057 | 0.985 | 1,2 |
| 038_video_vendor_enhancements | FAIL | 0.862365 | 0.985 | 0,0 |
| 126_preferred_camera_api_dialog | FAIL | 0.842638 | 0.985 | -2,2 |
| 044_video_fps_menu | FAIL | 0.880277 | 0.985 | -2,2 |
| 045_video_fps_menu_middle | FAIL | 0.873415 | 0.985 | -1,1 |
| 046_video_fps_menu_lower | FAIL | 0.878284 | 0.985 | -1,1 |
| 050_video_exposure_compensation_menu | FAIL | 0.887039 | 0.985 | 0,2 |
| 051_video_exposure_compensation_menu_scrolled | FAIL | 0.881423 | 0.985 | -2,2 |
| 052_video_exposure_compensation_menu_lower | FAIL | 0.885288 | 0.985 | -2,2 |

A failed metric is not masked or waived automatically. Inspect the matching files in `side-by-side/`, `overlays/`, and `diffs/` and record any legitimate platform-only variance before changing a threshold.
