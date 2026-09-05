# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- A dropped broadcast reconnects on its own. The console shows which retry is
  in flight, waits a little longer between each one, and goes back to live as
  soon as the destination accepts again. Pressing Stop ends it for good.

### Added

- The app checks GitHub once a day for a newer release and says so under Help &
  support, with a link to the releases page. It fails silently offline and can be
  turned off. There is no store to push updates, so it has to look.
- Several outgoing connections can be saved. Tap one to make it the destination
  the console broadcasts to, and delete one from Manage outgoing connections,
  which asks first. Saving a name that already exists edits it instead of adding
  a duplicate.

### Fixed

- Saving a second connection no longer silently replaces the first.
- Changing the resolution or frame rate while the preview is open now reaches
  the encoder when you press Start, instead of sending the size the preview
  happened to open with.
- Stopping a broadcast clears the bytes-sent total, so an idle console no longer
  shows a figure from the session that just ended.
- A destination URL ending in a slash no longer shows its stream key in full,
  and a key quoted inside a connection error no longer reaches the screen or the
  log. Passwords embedded in a URL are hidden the same way.
- A stream that drops again in the moment a reconnect succeeds is now noticed
  rather than ignored.
- Broadcast statistics are measured instead of invented. Uptime, bytes sent,
  dropped packets and the current bitrate come from the outgoing stream, and the
  Network tab shows the real rate while a broadcast is running.
- The Resolution and FPS settings now reach the encoder. Choosing 1280x720 sends
  1280x720 instead of 1080p regardless of the setting. A variable frame rate uses
  the top of its range, and System default is 30 fps.
- A broadcast that drops now leaves the live state instead of showing LIVE
  forever. The console watches the outgoing stream and says why it stopped.
- Stream keys no longer leave the device in a cloud backup or a phone-to-phone
  transfer. The backup rules named a file the app never writes, so the settings
  store holding the connection URL was being copied off the device.
- The saved destination no longer shows its stream key on screen. Outgoing
  connections list the server and application path with the key hidden, and a
  Show stream key row reveals it for that visit only. Publish failures name the
  destination the same way instead of quoting the whole URL.

## [0.4.0], 2026-08-30

### Added

- Broadcasts can include microphone audio. Camera and microphone permissions are
  requested together, while a refused microphone falls back to silent video.
- RTMPS destinations are accepted by the connection form.

### Changed

- The camera is released when the app leaves the foreground and opened again on
  return. Retry now replaces a failed capture session instead of reusing it.
- Automated screen captures use the simulated engine, keeping the camera and
  network inactive during deterministic validation.
- Release signing now reads an external local key from environment variables.
  No signing key is stored in the repository.

### Fixed

- Quick repeated taps can no longer start two broadcasts at once.
- Publish failures log only the destination host, so stream keys stay out of logs.
- Dependency verification regeneration now covers debug, release, test, lint,
  and device-test configurations.

## [0.3.1], 2026-08-29

### Changed

- New app icon: adaptive, themed (monochrome) and legacy variants regenerated from the 2026-08 icon set.

### Added

- The console broadcasts for real. Camera capture and H.264 encoding run through
  StreamPack, and the saved connection is published over RTMP. Verified against a
  local MediaMTX, which reports the stream online with one H264 track. The one
  control starts and stops it.
- Connections you enter are saved. The form used to say "saved to the local
  fixture" and keep nothing, so Start could only ever refuse.
- Resetting the app settings now says so, and offers Undo for ten seconds. The
  reset used to clear everything silently with no way back. Resetting twice
  inside that window still restores what the first reset cleared.
- When the camera will not start, the console says why and offers Retry instead
  of flashing a toast and leaving a black rectangle.

- Every dependency is pinned by checksum. A build now fails if an artifact's
  bytes change under it, and regenerating the pins takes a deliberate flag.

### Changed

- The camera now runs through the broadcast engine rather than CameraX, so the
  preview and the outgoing stream share one capture session. CameraX is no
  longer a dependency.

### Fixed

- The capture catalog the screenshot test rig uses no longer ships in release
  builds, and neither does the audit scroll-anchor data it reads.

- The settings header and the About dialog show the version the build actually
  carries. Both were literals that still said 0.2.0 after the 0.3.0 release. A
  test now fails if a version literal reappears in production source.

## [0.3.0]: 2026-08-29

The console shows a real camera. Until now the live view was a static JPEG
(`preview_fixture`), so the app looked frozen the moment it opened.

### Added

- CameraX preview (1.6.2) bound to the console's lifecycle. The lens pills,
  the quick-panel camera list and the flip button switch facing live: ids 1
  and 3 open the front camera, 0 and 2 the back. If the wanted facing is
  missing the other one is used and a toast says so.
- `CAMERA` runtime permission. Requested on first launch; a refusal leaves the
  console usable on a black surface with an "ALLOW CAMERA" retry, and once
  Android stops showing its prompt the retry opens the app's settings page.
- `android.hardware.camera.any` declared as optional so camera-less devices
  can still install.

### Changed

- The debug-state test rig keeps the deterministic fixture (any resolved
  `debugScreenId`) so the 145 audited captures still compare pixel for pixel.

Verified on a Galaxy S25 Ultra (Android 16): cold launch shows the back
camera, flip shows the front camera, revoke plus relaunch raises the system
prompt and the preview resumes on allow.

## [0.2.0]: 2026-08-15

First pass over the audit findings (IS-22..IS-51) and the earlier research
roadmap. The theme is gate integrity: the three validation gates could report a
stale pass, could not fail on geometry at all, and printed evidence that was
hardcoded rather than measured. Fixing them exposed four real reconstruction
defects that the blunted gates had been hiding.

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
  was editing, so confirming it silently reverted the saved setting: and a
  multi-select dropped every selection the default did not contain.
- Accessibility on the live console: plain buttons (Settings, Reload, Snapshot)
  no longer announce a "Not selected" state they do not have, quick-panel toggles
  are real switch-role controls spanning the whole row rather than a stateless
  clickable row beside a switch, and preference rows regained their press ripple.
- The safe-margin overlay honours the persisted ratios setting. "Safe margins
  ratios" is a nine-option multi-select, but the console drew one hardcoded 16:9
  rectangle, so selecting 21:9 or several ratios changed nothing on screen.
- Live-console telemetry is per state rather than pinned to screen 001's reading.
  Each audited live capture recorded its own battery values, so one fixture
  mismatched nineteen states in both text and glyph geometry.
- The browsable `irlstreamer://` intent filter is debug-only. The release build
  has no handler for it, so shipping it advertised a scheme that did nothing and
  squatted on the one the settings import flow will define. Verified against the
  built APKs: absent from release, present in debug.
- The forced-landscape console declares
  `PROPERTY_COMPAT_ALLOW_RESTRICTED_ORIENTATION`. Android 16 ignores
  `screenOrientation` on displays 600dp and wider, so the console would have
  rendered portrait on tablets and unfolded foldables with no layout for it.
- The difference heat maps rendered the worst pixel errors as near-black. The red
  channel was multiplied as `uint8`, so it wrapped modulo 256 and any difference
  at or above 86 came out darker than a small one: in exactly the images the
  report tells a reviewer to inspect before waiving a failure.
- The geometry matcher no longer reports phantom missing elements. Labels are
  matched case-insensitively (the audit dumps a tab's text in caps and its
  container's description in title case), authorized renames are paired through
  an alias table, and a label duplicated on its own container is indexed once.
  Coverage rose from 1041 to 1060 matched elements with unmatched down from 238
  to 217; the differ also reports how many nodes it cannot score at all.
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
- The PowerShell test rig no longer aborts its emulator boot poll when adb
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
  and result JSON in place, and the sweep reported that stale result: `PASS`
  included: as though it belonged to the current run. Capture and compare
  failures now surface as `CAPTURE_FAILED` / `COMPARE_FAILED` rows and a non-zero
  exit, and a failed hierarchy dump leaves no file so the geometry gate reports
  `NO_REPLICA_DUMP`.

### Added

- `LICENSE` (MIT), matching what the application actually links today, with the
  licence consequences of the two planned copyleft dependencies recorded rather
  than left to be rediscovered.
- The README explains that SRTLA bonding requires a server-side receiver and
  names three open-source options. Users repeatedly expect bonding to improve a
  connection by itself, which is the most common support burden in this space.
- Behavioural fixtures for the engine seam: degraded, reconnecting and error
  transitions are asserted against a scripted fake with no device or network,
  which the screenshot and hierarchy gates structurally cannot observe.
- `scripts/sync-baselines.ps1` restores the comparison baselines from the audit
  evidence, and the visual sweep calls it automatically when none are present. A
  fresh clone previously failed every documented compare command with "Image not
  found", because the baselines are generated evidence and therefore gitignored.
- The emulator provisioning script zeroes all three animation scales and reads
  the display size and density back, so a silently failed `wm size` is reported
  instead of producing 145 dimension mismatches with no stated cause.
- `BroadcastEngine` seam with the local simulation behind it
  (`SimulatedBroadcastEngine`), so a capture/transport implementation can land
  without touching Compose or the 145-state debug test rig. Starting a broadcast
  is now the engine's decision: it refuses with `NoActiveConnection` and the UI
  raises the audited guard dialog.
