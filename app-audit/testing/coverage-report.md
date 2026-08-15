# Coverage report

## Counted coverage

| Measure | Count | Counting rule |
|---|---:|---|
| Unique logical screens/states | 119 | Distinct authoritative `screen_name` values after correcting capture-time filenames and case-normalizing repeated surfaces |
| Visual states captured | 145 | One screenshot/XML/spec bundle per stable screen ID |
| Dialogs | 15 | Modal editor/confirmation/prompt states classified by the screen generator |
| Selection dialogs or menus | 37 | Choice-list/menu states; this category is separate from general dialogs |
| Bottom sheets | 0 | No bottom sheet was observed |
| System-intent surfaces | 1 | Android DocumentsUI folder picker; the Barcode Scanner dependency prompt is app-owned |
| Quick-settings overlay states | 9 | Live-console panel tabs plus meaningful control states |
| Primary flows | 15 | F01–F15 in `flows/flow-catalog.csv` |
| Flows fully tested | 3 | F01 launch/lifecycle, F02 settings hierarchy, F09 display guides/meter |
| Flows partially tested | 12 | F03–F08 and F10–F15 |
| Primary flows wholly untested | 0 | Every primary flow family has at least one safe observed path |
| Discrete blocked/untested cases | 24 | U01–U24 in `untested-and-blocked-cases.md` |
| Interactive/adjustable control instances | 1,145 | Repeated controls across captured UI trees count once per state |
| Deduplicated control signatures | 378 | Resource-ID/label/type signatures; not guaranteed to equal implementation widgets |
| Interaction tests/cases | 56 | T001–T056 in `interaction-test-matrix.csv` |
| Unresolved questions | 26 | Q01–Q26 in `planning/open-questions.md` |

Counts are **CONFIRMED** from the generated catalog and UI Automator evidence. “Unique logical screens/states” is not a claim about internal fragments or composables. The 145 captures include scroll positions, selected/disabled/validation states, menus, system surfaces, and reversible temporary states.

## State-family coverage

| State family | Coverage | Evidence and limit |
|---|---|---|
| Default | Strong | Default state captured across every reachable primary settings family and live console |
| Empty | Strong | Connections, Manage connections, Text/Picture overlays, Manage web overlays, and idle Network Stats |
| Populated | Partial | Built-in Timestamp and settings summaries; user-created connection/overlay lists intentionally remain empty |
| Loading | Partial | Cold camera initialization and reload-status toast; remote content loading not tested |
| Error/guard | Partial | No-connection broadcast guard, blank Text validation, external scanner dependency prompt; offline/server errors untested |
| Disabled | Strong | Save buttons, Delete multiple, preferred Camera API, Horizon, USB camera, live rotation/background controls |
| Selected | Strong | Choice dialogs, switches, camera pills, mute state, grid/margins, Timestamp active state |
| Validation | Partial | Empty URL disabled Save and blank HTML toast; malformed imports, server URLs, secrets, ranges, duplicates unknown |

## Evidence completeness

All 145 IDs have a full-resolution PNG, UI Automator XML, target/focused activity summary, privacy-filtered window/IME summary, capture metadata, catalog entry, evidence-manifest entry, and JSON screen specification. Screen 144 additionally links the privacy-trimmed cold-launch recording and its metadata.

UI Automator exposes many preference rows well, but the camera preview is a rendering surface and several live-console icons lack semantic labels. For that area, screenshot comparison and controlled interaction observations carry more weight than the XML alone.

## Exact reasons for missing coverage

Coverage is missing only where testing would require one or more of: clearing/reinstalling the app; entering credentials; saving endpoint-like records; transmitting camera/audio; creating user media; installing an external scanner; opening private signed-in apps; sharing diagnostics; deleting settings/cookies; granting persistent storage access; changing system/network/accessibility state; unavailable hardware; or an unknown control with a lockout/streaming/service side effect. Each mapping is enumerated in U01–U24.

## Accessibility measurements

- 141 explicit UI Automator `NAF="true"` instances occur across 21 captured states.
- 542 directly unlabeled actionable-node instances were counted. This is an XML-node count and overstates unique user-facing failures because compound rows expose clickable descendants.
- 427 interactive-node instances have at least one exposed dimension under 48 dp. Compound parent hit areas may mitigate some, so this is a potential-risk count, not 427 proven failures.
- The strongest confirmed defect is the live console: Settings, Reload, Quick settings, Snapshot, Flip, Start, and Mute expose empty descriptions in screen 001.
- TalkBack, switch announcement wording, keyboard traversal, text scaling, and error announcements were not enabled/tested because doing so would change device-wide accessibility state.

## Performance sample

On one Samsung SM-S908U1 running Android 16: cold launch was 330 ms total/347 ms wait, Home resume 119/121 ms, and relaunch after Back 174/182 ms. Idle camera/audio preview used about 275,057 KB total PSS and 435,908 KB RSS. `gfxinfo` reported 14/695 janky frames (2.01%) using the current metric and 80/695 (11.51%) under the legacy threshold. These are single-sample, device-specific observations—not product benchmarks.
