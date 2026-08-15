# Reusable component catalog

## Configuration surfaces

| Component | Observable contract | Key states | Evidence |
|---|---|---|---|
| Dark top app bar | back arrow, one-line title, black status bar above | normal; external system surface differs | screens 002–129 |
| Preference section header | teal all/initial-cap label on `#303030`, non-interactive | normal | screens 006, 016, 120 |
| Preference row | 54–73 dp height, title, optional summary/current value, full-row tap | enabled, disabled, selected summary | most settings screens |
| Switch preference | title/summary at left, switch at far right; whole row taps | on, off, disabled | screens 006–010, 083, 120–125 |
| Numeric preference | title + formatted summary opens editor/list | default, adjusted, reset | screens 013–014, 055, 074, 077, 090 |
| Seek preference | title/value and full-width slider | minimum, default, maximum | screens 018–020, 074, 090 |
| Choice dialog | dark modal, title, 48 dp `CheckedTextView` rows, Cancel | checked, unchecked, scroll | screens 041–073 |
| Numeric editor dialog | dark modal, title, single-line field, Cancel/OK | focused keyboard state, validation | screens 013, 014, 055, 057, 077 |
| Two-action form footer | equal-width gray Cancel/Save buttons | enabled/disabled Save | screens 023–028, 098–110 |
| Toast feedback | system rounded dark pill with app icon and white text | validation, asynchronous status | screens 101, 143 |
| Destructive/edge control | ordinary preference styling; no extra warning until activation is known | disabled, UNTESTED | Reset, Clear cookies, Delete multiple |

## Live-console components

| Component | Observable contract | Evidence |
|---|---|---|
| Camera preview surface | fills safe landscape canvas; initializes black before frames | screens 001, 144; cold-launch video |
| Audio level meter | stacked green horizontal bars at lower left; gray when muted | screens 001, 139 |
| Battery telemetry | battery bar plus mA, mW, °C values; values update live | screens 001, 130–145 |
| Circular icon control | translucent charcoal circle with gray/white vector glyph | Settings, Snapshot, Flip, Mute |
| Reload control | small upper-right circular arrow; reloads chat/web overlays and shows toast | screen 143 |
| Start/stop pill | centered outlined pill with play glyph; long-clickable in XML | screens 001, 142 |
| Lens pill matrix | `FRONT`/`BACK` labels with per-physical-camera FoV pills; selection via border/text | screens 001, 140, 141 |
| FPS pill | live numeric frame-rate readout near bottom | screens 001, 144 |
| Grid overlay | 3×3 guide above preview | screen 084 |
| Safe-margin overlay | red ratio rectangle with configurable indent | screens 085, 087–090 |
| Text/picture layer | HTML or URL-backed layer, active/periodic/scale/position/z-order | screens 093–106 |
| Web overlay layer | transparent WebView, preview/stream mode, position/custom coordinates, 1080p canvas width/height/scale | screens 107–115 |
| Quick-settings panel | black ~320 dp wide overlay, horizontally scrolling tabs, vertically scrolling tab body | screens 130–138 |

## Quick-settings tabs

- Camera: physical-camera radio selection, torch, exposure slider, zoom, focus/white-balance/anti-flicker choices, tap-focus/exposure/white-balance toggles.
- Network: conditioner, bitrate mode, current adaptive bitrate, target bitrate slider/reset gesture, link toggles/weights, idle Stats section.
- Display: grid, safe margins, Lock Screen touch blocker.
- Overlays: master switch, Timestamp checkbox, long-press edit instruction.
- Audio: input gain slider and double-tap reset.
- Log: scrollable diagnostic text for active camera/audio/encoder events; no file logging unless enabled in Recording settings.

## Component acceptance notes

Every rebuilt component needs a stable semantic ID independent of displayed copy, a minimum 48 dp hit target, content/state descriptions, disabled semantics, keyboard/D-pad behavior where applicable, and tests for state restoration. Visual-only child bounds must not become the accessibility hit target.

