# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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
