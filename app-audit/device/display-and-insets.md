# Display, orientation, and insets

## Device baseline

| Finding | Classification | Observed value | Evidence |
|---|---|---|---|
| Device | CONFIRMED | Samsung SM-S908U1 (`b0q`) | `device/device-environment.json` |
| OS | CONFIRMED | Android 16, SDK 36, build `BP2A.250605.031.A3` | `device/device-environment.json` |
| Physical display | CONFIRMED | 1080 × 2316 px portrait baseline | `device/device-environment.json` |
| Density | CONFIRMED | 450 dpi = 2.8125 px/dp | `device/device-environment.json` |
| Logical size | CONFIRMED | 384 × 823.47 dp portrait baseline | `device/device-environment.json` |
| Font scale | CONFIRMED | 1.0 | `device/device-environment.json` |
| Locale/time zone | CONFIRMED | `en-US`, `America/New_York` | `device/device-environment.json` |
| Theme | CONFIRMED | Android night mode enabled; app uses dark surfaces | screenshot set; device JSON |
| Navigation | CONFIRMED | Samsung three-button navigation | window evidence and screenshots |

## IRL Pro window contract

- **CONFIRMED:** All observed IRL Pro surfaces force landscape, producing 2316 × 1080 screenshots and an 823.47 × 384 dp logical canvas. This includes settings screens, not only the live console.
- **CONFIRMED:** Landscape navigation occupies 135 px / 48 dp on the right. Content stops at x=2181.
- **CONFIRMED:** A 75 px / 26.67 dp safe inset remains on the left in landscape. In portrait, the centered display cutout is 56 px wide and 75 px deep; rotated handling manifests as the left safe strip.
- **CONFIRMED:** Settings show the 74–75 px (~26.7 dp) black status bar and begin the app bar at y≈74. The app bar ends at y=219.
- **CONFIRMED:** The default live console hides the status bar and draws the preview/control canvas from y=0; the system navigation bar remains visible on the right. `Advanced options > Show status bar on broadcast page` was off.
- **CONFIRMED:** Settings content uses `[75,219]–[2181,1080]`; usable live-console content is `[75,0]–[2181,1080]`, or 748.8 × 384 dp.
- **STRONG INFERENCE:** The app is not generally edge-to-edge; it explicitly respects the cutout and navigation bar while selectively hiding the status bar on the broadcast surface.

## Reconstruction constraints

1. Lock the primary experience to landscape unless a future product decision deliberately changes this observable contract.
2. Consume `WindowInsets` rather than hard-coding 75/135 px. Match the observed results at 450 dpi and adapt safely elsewhere.
3. Preserve the status-bar distinction: settings visible, live console hidden by default and controlled by the advanced preference.
4. Verify the main preview/control overlay at exactly 823.47 × 384 dp as the reference viewport before testing other densities.
5. Keep critical controls out of cutout, rounded-corner, and right navigation regions.

## Unknowns

- **UNKNOWN:** behavior on gesture navigation, tablets, foldables, external displays, split screen, display zoom other than default, font scale above 1.0, and right-to-left locales.
- **UNKNOWN:** whether reverse-landscape is supported when `Reverse Orientation` or live rotation is enabled; settings were observed but rotation was not changed because the brief prohibited system-wide setting changes.

