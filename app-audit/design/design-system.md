# Observed design system

IRL Pro uses two related visual languages: Android dark preference/dialog surfaces for configuration, and a transparent high-density control overlay on the camera preview. Measurements are tied to the 450 dpi, 2316 × 1080 reference device.

## Raw color samples

| Role | Observed value | Classification | Evidence/method |
|---|---:|---|---|
| Settings background | `#303030` | CONFIRMED | dominant solid pixels, screen 120 |
| Top app bar | `#212121` | CONFIRMED | dominant solid pixels, screen 120 |
| Dialog surface | `#424242` | CONFIRMED | dominant solid pixels, screen 129 |
| Accent/active teal | `#80CBC4` | CONFIRMED | solid text, underline, and switch pixels across screens 120/129 |
| Primary text | `#FFFFFF` | CONFIRMED | solid interior text pixels |
| Secondary text | approximately `#C7C7C7` | CONFIRMED sample, anti-aliasing varies | screens 120/129 |
| Disabled control/text | approximately `#6A6A6A`–`#959595` | CONFIRMED range | screens 115/126 |
| Divider | approximately `#4A4A4A` | CONFIRMED sample | settings and dialogs |
| Live quick-settings panel | `#020202` dominant | CONFIRMED | screen 130 interior sample |
| Audio meter active | `#1F8B4D` | CONFIRMED | screen 001 meter interior |
| Audio meter muted | gray, approximately `#7F8C8D` | CONFIRMED visually | screen 139 |
| Built-in timestamp | `#66FF66` | CONFIRMED | visible HTML template and screen 095 |
| Safe-margin outline | red | CONFIRMED visually; exact antialiased value not normalized | screen 085 |

Normalized tokens in `design-tokens.json` preserve these raw values and mark estimates.

## Layout and spacing

- Reference scale: 2.8125 px/dp.
- Landscape content width: 2106 px / 748.8 dp between x=75 and x=2181.
- Settings app bar: y=74–219, 145 px / 51.56 dp; close to the platform 56 dp pattern.
- Settings content starts at y=219. Preference rows are usually 151 px / 53.69 dp for one-line rows or 204 px / 72.53 dp with summaries.
- Preference text starts at x=278, 203 px / 72.18 dp from the content edge. The large inset matches the geometry of a standard preference layout with a reserved icon column even when no icon is visible.
- Full-width form actions are usually two equal 1053 px / 374.4 dp buttons with 135 px / 48 dp height.
- Selection dialogs are commonly x=443–1812: 1369 px / 486.76 dp wide. Heights vary with list length and can fill nearly the entire safe canvas.
- List-option/dialog rows are normally 135 px / 48 dp high.
- Thin dividers are approximately 2–3 px (0.7–1.1 dp) after rasterization.

## Shape and elevation

- Preference screens use square, edge-to-edge surfaces and thin dividers; no cards were observed.
- Dialogs use the platform rectangular dark surface. Visible corner radius is minimal/indistinct at this viewport; preserve the Android dialog feel rather than inventing rounded cards.
- Live controls use circles or outlined pills on a translucent charcoal backing. The largest mute control is approximately 203 × 203 px / 72.18 dp.
- Toasts use the Android 16 system toast treatment: rounded dark gray pill, app icon at left, white text. Rebuild code should invoke a semantic in-app status surface or standard toast rather than copying OS chrome pixel-for-pixel.

## Live-console composition

1. Full-bleed camera preview inside the safe content canvas.
2. Narrow audio meter anchored at the lower left.
3. Battery/current/power/temperature telemetry in the upper right.
4. Settings gear at upper left; reload and overflow at upper right.
5. Snapshot, camera flip, and microphone rail at the right.
6. FPS, start/stop control, and front/back lens pills along the bottom.
7. Quick settings open as a black tabbed panel over the upper-right/center of the preview, without navigating away.

The live overlay is deliberately low-chrome so the preview remains dominant. Selected lens pills have a brighter border/text; unavailable lenses are dim. Audio bars turn gray when muted. Grid, safe margins, text layers, and web layers render above the preview according to their state and z-order.

## Responsive behavior

Only one phone/density was observed. Rebuild acceptance should first match the reference canvas, then use dp and insets to adapt. Do not scale the full UI as one bitmap. Allow long preference summaries to wrap, dialog lists to scroll, and tab rows to horizontally reveal later tabs (`Audio`, `Log`) as observed in screens 135–138.

## Accessibility design debt

- UI Automator marked 141 nodes as `NAF="true"` across 21 captured states, concentrated in icon-only live-console controls.
- The reference main screen exposes empty content descriptions for Settings, Reload, Quick settings, Snapshot, Flip, Start, and Mute controls (screen 001 XML).
- Many exposed child bounds are below 48 dp even where a parent row may enlarge the actual hit area. The clearest failures are Reload (~23.8 dp square), Settings/Overflow/Snapshot (~40.2 dp square), Start (~64 × 24.2 dp), and lens pills (~45.9 × 28.1 dp).
- A rebuild should preserve visual proportions while supplying at least 48 dp semantic hit targets, explicit labels, state descriptions, logical focus order, and non-color selected/muted cues.

