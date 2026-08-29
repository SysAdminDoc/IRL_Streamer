<p align="center">
  <img src="branding/irl-streamer-app-icon-v3.png" width="180" alt="IRL Streamer logo">
</p>

# IRL_Streamer

[![Version](https://img.shields.io/badge/version-0.3.1-80CBC4)](CHANGELOG.md)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%209%2B%20(API%2028--36)-3DDC84)](replica-app/)

IRL_Streamer is an independent Android live-streaming console reconstructed from an authorized, observable-behavior audit. The project preserves the audited workflow map while replacing the original app identity, artwork, and implementation with clean-room Kotlin and Jetpack Compose code.

The working Android project is in [`replica-app`](replica-app/). The immutable 145-state behavior and screen audit is in [`app-audit`](app-audit/).

## Current status

- Native Android application targeting API 36, with API 28 minimum support
- Forced landscape live console, settings, dialogs, forms, overlays, and debug-addressable audit states
- Live camera preview and RTMP broadcasting through StreamPack, with a runtime permission flow
- Deterministic local simulations for bonding, recording, chat, and remote services
- JVM tests, device Compose tests, lint, release build, signature verification, and visual-comparison tooling
- Original IRL_Streamer logo and adaptive launcher icon generated specifically for this project

See [`replica-app/README.md`](replica-app/README.md) for setup, build, test, and validation instructions.

## Clean-room notice

This is not the official IRL Pro application. It contains no decompiled source, production signing material, private user data, or original artwork. Audit screenshots are retained only as reconstruction evidence and are never rendered by the app.
