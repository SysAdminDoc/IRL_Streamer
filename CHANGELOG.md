# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed

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
