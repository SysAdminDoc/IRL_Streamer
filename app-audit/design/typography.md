# Typography

## Font family

- **STRONG INFERENCE:** settings and dialogs use the Android system sans-serif family through AppCompat/platform widgets. Exact font files were not inspected and must not be copied.
- **CONFIRMED:** the built-in Timestamp template explicitly requests CSS `font-family:sans-serif` and `font-size:x-large`.
- **UNKNOWN:** whether the app packages any custom font; no custom brand typeface was visible.

## Estimated scale

| Role | Estimate | Weight/case | Behavior | Confidence |
|---|---:|---|---|---|
| App-bar title | 20 sp | medium, sentence/title case | one line, start aligned | STRONG INFERENCE |
| Preference title | 16 sp | regular | one or two lines; start aligned | STRONG INFERENCE |
| Preference summary/current value | 14 sp | regular | wraps; secondary gray | STRONG INFERENCE |
| Section header | 14 sp | medium, authored capitalization | teal, one line | STRONG INFERENCE |
| Dialog title | 20 sp | medium | wraps if needed | STRONG INFERENCE |
| Dialog body/options | 16 sp | regular | 48 dp option rows; long body wraps | STRONG INFERENCE |
| Dialog/form button | 14 sp | medium, uppercase | centered | STRONG INFERENCE |
| Live telemetry | 12–14 sp | regular | right aligned, tabular-looking numbers | POSSIBLE visual estimate |
| Quick-tab label | 14 sp | medium, uppercase | horizontally scrolls/clips with tab row | STRONG INFERENCE |

## Formatting rules

- Units are attached to values with observed spacing: `6000 Kbps`, `6.0Mbps`, `0.0dB`, `+10.0`, `30 fps`, `34.7 °C`, `-739 mW`.
- Resolution is `widthxheight`, followed by an optional aspect ratio: `1920x1080 (16:9)`.
- Exposure positive values include `+`; zero is `0`.
- Camera lens labels use integer degree symbols (`73°`) and detailed radio labels use `id:0 [73° FoV, rear]`.
- Timestamp default format is `MMM dd, HH:mm:ss` with locale `en_US` (example observed: `Aug 14, 21:07:39`).
- Button labels in dialogs/forms are uppercase; preference titles are sentence/title case as authored.

## Accessibility requirement

Support Android font scaling without clipping settings/dialog content. The 384 dp landscape height is especially constrained: long text must scroll rather than shrink. The live console may cap telemetry scaling only if necessary to preserve controls, but semantic labels and alternate announcements must remain available.

