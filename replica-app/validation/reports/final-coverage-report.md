# Final coverage report

Generated: 2026-08-15 (America/New_York)

## Outcome

IRL Streamer is implemented as a native, independent Kotlin/Compose application and ships a minified, signed local release APK. All 145 audit catalog IDs resolve to deterministic debug states and were rendered on the isolated `issue-sweep-api36` emulator at 2316 × 1080 captured pixels / 450 dpi.

The rebuild is **partially complete** against the strict audit contract. Functional and build gates pass, but no screen reaches the configured SSIM threshold of 0.985. No threshold was lowered, no comparison was waived, and no dynamic region was masked.

## Counted coverage

| Measure | Result |
|---|---:|
| Audit catalog states mapped | 145 / 145 |
| Current screenshots captured | 145 / 145 |
| JSON metric results | 145 / 145 |
| Side-by-side comparisons | 145 / 145 |
| 50% overlays | 145 / 145 |
| Difference heat maps | 145 / 145 |
| Strict visual passes (SSIM ≥ 0.985) | 0 / 145 |
| Visual failures retained as failures | 145 / 145 |
| Primary flow families represented | 15 / 15 |
| JVM unit tests | 6 passed / 0 failed |
| Device Compose tests | 4 passed / 0 failed |
| Android lint | passed |
| Minified release build | passed |
| Release install/cold launch | passed |

## Visual metrics

| Metric | Result |
|---|---:|
| Minimum SSIM | 0.436994 |
| Median SSIM | 0.836694 |
| Mean SSIM | 0.812113 |
| Maximum SSIM | 0.930146 |
| Configured threshold | 0.985000 |

The strongest app-owned settings/form comparisons are `091_overlays_list` (0.930146), `108_web_overlay_new_form` (0.929665), and `113_web_overlay_custom_position_fields` (0.929454). Live-console states score lower because the original camera pixels cannot ship and the clean-room preview is intentionally different. Platform status/navigation glyphs, distinct identity copy, system font rasterization, and remaining component differences also affect raw whole-screen SSIM.

## Behavioral coverage

The deterministic state catalog covers F01–F15 and the four device tests directly exercise safe core paths for launch/start guard, live quick-panel tab switching, settings hierarchy/Back, and blank connection-form validation. Remaining safe states were smoke-rendered through explicit ADB state injection. Real broadcast transport, camera/audio capture, recording, remote chat/dashboard/WebView content, credentials, service authentication, proprietary bonding, and destructive/system-changing paths remain outside the authorized local-simulation boundary.

## Release evidence

- APK: `app/build/outputs/apk/release/app-release.apk` (3,992,134 bytes)
- Package: `com.irlstreamer.reconstruction`
- Version: `0.1.0` (`versionCode=1`)
- Foreground activity: `com.irlstreamer.reconstruction/.MainActivity`
- Release screenshot: `validation/current/release-launch.png`
- Signing certificate SHA-256: `b831eb19f068a8eb688deb65c12af6cf4160b802ee1a0ff9bddc2ef38419bac2`
- Certificate: repository-owned local self-signed QA identity; not a production key

## Evidence index

- Per-screen status: `docs/implementation-status.csv`
- Requirement mapping: `docs/audit-traceability-matrix.csv`
- Per-screen metrics: `validation/reports/visual-validation-results.csv`
- Full metric table: `validation/reports/visual-validation-report.md`
- Design review: `design-qa.md`
- Behavioral evidence: `validation/reports/behavioral-validation-report.md`
- Accepted/blocked differences: `docs/known-deviations.md`

## Final disposition

The implementation, test harness, complete state capture set, and release artifact are delivered. Audit-equivalent pixel validation and unavailable production integrations remain open, so the repository must not be represented as a fully validated clone.
