# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `BroadcastEngine` seam with the local simulation behind it
  (`SimulatedBroadcastEngine`), so a capture/transport implementation can land
  without touching Compose or the 145-state debug harness. Starting a broadcast
  is now the engine's decision: it refuses with `NoActiveConnection` and the UI
  raises the audited guard dialog.
