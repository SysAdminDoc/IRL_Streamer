# Blocked roadmap items

Items that need external input or a rig this machine does not have. Each keeps its original text plus the blocker.

- [ ] P2 — IS-99 Inline form validation replaces the audited validation toast
  Why: audit state 101 records the original app showing a 3.5 second system toast for a blank-save validation error, which capture timing can miss entirely (IS-48) and which leaves no error next to the field. Inline validation is the improvement, but it changes a captured screen.
  Evidence: `app-audit/screens/screen-specs/101_overlay_blank_save_validation.json` state_name "Blank HTML Save validation toast; draft discarded"; RESEARCH.md 2026-08-29.
  Touches: `ui/settings/Forms.kt`, `ui/components/FormTextField`, `docs/known-deviations.md` (needs a new D-number, precedent D010)
  Acceptance: a blank required field shows its error under the field with no toast; the geometry gate still passes on state 101 and the deviation is recorded.
  Complexity: M
  BLOCKED: changing an audited screen needs the 145-state geometry gate to re-run.
  Updated 2026-09-05: the rig blocker is gone. The android-36 `google_apis;x86_64` image is installed, `issue-sweep-api36` has been recreated, and `scripts\start-headless-emulator.ps1` verifies the display reads back 1080x2316 / 450. The gates also no longer die on a missing `py -3.12` - `Common.ps1` resolves an interpreter. What remains is running the 145-state capture, which needs enough free RAM to hold an emulator and a Gradle daemon at the same time; see the IS-104 entry below for the measured conditions.

- [ ] P0 — IS-104 Recreate the validation AVD so the three gates can run again
  Why: the geometry, strict-SSIM and app-chrome gates are the only defence against regressions across 145 audited screens, and none of them can be executed on this machine. Every audited-screen change is blocked behind this, including IS-99.
  Evidence: `Roadmap_Blocked.md`; repo `CLAUDE.md` entry 2026-08-29 "the harness AVD no longer exists on this machine"; the baseline in `replica-app/validation/geometry-baseline.json` was captured at 2316x1080 / 450 dpi on API 36, so no other API level is comparable.
  Touches: `replica-app/scripts/start-headless-emulator.ps1`, `scripts/check-environment.ps1`, `docs/testing-guide.md`
  Acceptance: `sdkmanager` installs the android-36 system image, `issue-sweep-api36` is recreated at 2316x1080 / 450 dpi, and `run-full-validation.ps1` completes with the geometry gate reporting against the existing baseline rather than erroring; `validation/reports/release-verification.txt` is regenerated for the current versionCode.
  Complexity: M
  DONE 2026-09-05, and verified: the android-36 `google_apis;x86_64` system image was already installed - the 2026-08-29 note claiming otherwise was wrong. `issue-sweep-api36` was recreated with `avdmanager create avd -n issue-sweep-api36 -k "system-images;android-36;google_apis;x86_64" -d pixel_6`, booted headless, and `adb shell wm size` / `wm density` read back `Override size: 1080x2316` / `Override density: 450`. The gates' Python was also broken independently of the AVD (they hardcoded `py.exe -3.12`, which does not exist here); `Common.ps1` now resolves an interpreter and `check-environment.ps1` passes against emulator-5554 on API 36, reporting Pillow 12.3.0 / NumPy 2.5.2 / scikit-image 0.26.0. `:app:assembleDebug` produces the debug APK.
  BLOCKED on the rest, two separate reasons:
  1. `validation/reports/release-verification.txt` cannot be regenerated without release signing. `build-release.ps1` hard-fails unless `IRL_STREAMER_KEYSTORE_FILE`, `IRL_STREAMER_KEYSTORE_PASSWORD`, `IRL_STREAMER_KEY_ALIAS` and `IRL_STREAMER_KEY_PASSWORD` are set; all four are unset here and the key is deliberately kept outside the repository. This needs the operator's keystore.
  2. The 145-state capture needs a quieter machine than this one. Measured on 2026-09-05: free RAM swung between 0.4 GB and 5.5 GB of 31.7 GB with 15-17 other `java.exe` processes from parallel sessions, the machine-wide `~/.gradle/gradle.properties` forces `-Xmx4g` so daemons fail to start under that pressure, and `~/.claude/scripts/reap-build-daemons.ps1` kills `java.exe` by an idle-CPU heuristic, which killed four builds mid-flight. Run the capture when the box is otherwise idle, with `-Dorg.gradle.jvmargs=-Xmx2g -Dorg.gradle.priority=normal -Dorg.gradle.daemon.registry.base=<scratch>` (see repo `CLAUDE.md` 2026-09-05).
