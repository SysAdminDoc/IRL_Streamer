# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Security

- API-key and password dialogs actually mask their input. The check tested the
  per-field label, which is blank for every single-field catalog dialog, so it
  only ever fired for two multi-field fixtures and left "Dashboard A API Key" and
  similar in cleartext. Secret values are also never printed into a settings row
  summary.
- `Assert-ReplicaDevice` now refuses any target that does not report itself as an
  emulator. Requiring a serial was not a guard while the operator's phone and the
  emulator are both attached and the phone's serial was printed in the testing
  guide: one paste slip would install, launch and clear data on the phone.
  `-AllowPhysicalDevice` is the explicit escape hatch, and the physical serial is
  no longer written down in the docs.

### Fixed

- The final coverage report derives its evidence instead of asserting it. Test
  counts now come from the Gradle JUnit XML of the last run, and the signing
  certificate and cold-launch rows from the dated release verification file; a
  missing input reads "not recorded" rather than reporting a stale pass. The
  report also states its own coverage and refuses to run on an empty result set.
- Debug state 054 rendered an adaptive-bitrate dialog where the audit shows the
  Video settings page with manual bitrate enabled. It matched zero elements
  against the audit hierarchy and now matches ten.
- Dialog choice lists reused the previous dialog's scroll position. The list
  state was remembered without a key, so during a warm capture sweep the audited
  scroll anchor only ever applied to the first dialog of the run, corrupting
  every `*_menu_middle` / `*_menu_lower` state.
- Reopening a settings dialog showed the catalog default instead of the value it
  was editing, so confirming it silently reverted the saved setting - and a
  multi-select dropped every selection the default did not contain.
- The geometry gate can now fail. It enforces coverage (every catalog state with
  audit evidence must be compared), non-vacuity (a state that matched zero
  elements measured nothing and is not a pass), and no regression against a
  recorded ratchet baseline in `validation/geometry-baseline.json`. Previously it
  computed statistics and always exited 0, so the states validated by geometry
  rather than SSIM had no failing gate at all.
- Settings now persist. Only fifteen toggle keys were durable; roughly twenty
  more switches and every single-choice and text value lived in memory and reset
  on relaunch. Toggles without a named field and all choice/text values are
  stored generically, and "Reset app settings" clears in-session state instead of
  leaving pre-reset values on screen.
- Confirming a settings dialog no longer discards the input. Unhandled text and
  number dialogs keep their value so the row they were opened from updates, a
  non-numeric entry in a number dialog reports itself rather than closing
  silently, and choosing a recording folder acknowledges the choice.
- Settings pages no longer inherit each other's scroll position: every catalog
  page renders through one composable, so the list state needed the page in its
  key. Opening Root after scrolling Video showed Video's offset.
- Flipping the camera switches facing rather than toggling against id 1, so
  flipping from the second front lens no longer lands on another front lens, and
  the console control lights up for both.
- PowerShell harness robustness: the emulator boot poll no longer dies when adb
  writes routine "device offline" noise to stderr under the script-wide `Stop`
  preference, a wrong AVD name is reported immediately instead of after the full
  timeout, and `wm size` / `wm density` are read back so a silent failure cannot
  produce 145 dimension mismatches with no stated cause. `Stop-GradleDaemons` can
  no longer fail a green build (or mask a real error) from a `finally` block, and
  a missing executable reports itself instead of surfacing as an unset variable.
- `update_traceability.py` refuses to rewrite the committed status CSVs from a
  partial or empty results directory, which previously marked every uncovered
  state `NOT_STARTED` and destroyed the recorded status. `--allow-partial`
  updates only the states that have results.
- Seven scroll anchors recorded a slider value or the page title instead of a row
  title and could never resolve, silently falling back to the hand-guessed
  indices the anchors were built to replace. Anchors now come from the row's own
  `android:id/title`, and an unresolved anchor is logged rather than swallowed.

- Validation artifacts are invalidated before every capture. A failed capture or
  a crashed compare previously left the previous run's screenshot, hierarchy dump
  and result JSON in place, and the sweep reported that stale result - `PASS`
  included - as though it belonged to the current run. Capture and compare
  failures now surface as `CAPTURE_FAILED` / `COMPARE_FAILED` rows and a non-zero
  exit, and a failed hierarchy dump leaves no file so the geometry gate reports
  `NO_REPLICA_DUMP`.

### Added

- `BroadcastEngine` seam with the local simulation behind it
  (`SimulatedBroadcastEngine`), so a capture/transport implementation can land
  without touching Compose or the 145-state debug harness. Starting a broadcast
  is now the engine's decision: it refuses with `NoActiveConnection` and the UI
  raises the audited guard dialog.
