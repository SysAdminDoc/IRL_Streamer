# Measured layout tokens

Every value here was measured from the immutable audit evidence, not chosen from a
convention. Two sources are used and they are not interchangeable:

- **UI-hierarchy bounds** (`app-audit/evidence/ui-xml/<id>.xml`) give the *window*
  geometry that Android reports.
- **Screenshot pixels** (`app-audit/evidence/screenshots/<id>.png`) give the
  *drawn* geometry a user actually sees.

They differ for dialogs, and mixing them up was the largest single source of
visual error in the first reconstruction pass. See "AlertDialog decor inset".

Tools: `scripts/geometry_diff.py` (per-element deltas against a live replica
hierarchy dump) and `scripts/extract_scroll_anchors.py` (audited scroll state).

## Display contract

| Token | Measured | dp @ 450 dpi | Evidence |
|---|---:|---:|---|
| Viewport | 2316 x 1080 px landscape | 823.47 x 384 | `device/display-and-insets.md` |
| Left cutout safe inset | 75 px | 26.67 | all settings captures |
| Right navigation inset | 135 px | 48.00 | all captures |
| Settings status band | 74 px | 26.31 | screen 002 |
| Settings content region | `[75,219]-[2181,1080]` | | app bar ends at y=219 |

## App bar

| Token | Measured | dp | Evidence |
|---|---:|---:|---|
| App bar height | 145 px | 51.56 | y=74..219 |
| Title origin | x=277, y=125 | | 71 settings captures, min == max |
| Title line box | 53 px | 18.84 | |
| Nav icon cell | `[75,84]-[233,219]` | 56.18 x 48 | `Navigate up` ImageButton |
| Nav icon drawn glyph | 45 x 44 px | 16 | pixel bbox, screen 002 |

The title and nav icon both sit 5 px below the app-bar centre, hence the shared
1.78 dp vertical offset in `AuditedAppBar`.

## Preference rows

| Token | Measured | dp | Evidence |
|---|---:|---:|---|
| Row label origin x | 278 px | 98.84 | 400 of 421 sampled text nodes |
| Title-only row height | 151 px | 53.69 | 93 rows |
| Title + summary row height | 204 px | 72.53 | 80 rows |
| Two-line summary row height | 250 px | 88.89 | 16 rows |

Rows are contiguous: measured pitch equals measured height.

## AlertDialog decor inset

The audit hierarchy reports a dialog window of **1369 px**, but the drawn
`#424242` surface measures **1279 px** (x = 488..1767 in every screenshot). The
90 px difference is the AlertDialog's transparent decor inset, 45 px per side.

The first pass took the 1369 px window bounds as the surface width, which made
every modal 90 px too wide and pushed all of its internal padding outward. All
dialog tokens below are therefore measured **relative to the visible surface**.

| Token | Measured | dp | n | Spread |
|---|---:|---:|---:|---|
| Visible modal width | 1279 px | 454.76 | 47 | min == max |
| Modal centre x | 1127.5 px | | 47 | centre of `[75,2181]` |
| Settings vertical region | `[83,1080]` px | 29.51 top | 52 | centre y = 581.5 |
| Live-console vertical region | `[0,1080]` px | 0 top | 1 | centre y = 539.5 (screen 142) |
| Max visible height | 907 px | 322.49 | | screen 042 drew 906 px |
| Title inset (both sides) | 68 px | 24.18 | 47 | min == max |
| Title top | 51 px | 18.13 | 47 | min == max |
| Title line box | 76 px | 27.02 | 50 | |
| Field inset (both sides) | 56 px | 19.91 | 3 | min == max |
| Field top | 127 px | 45.16 | 3 | flush with title block |
| Field height | 135 px | 48.00 | 3 | |
| Choice list top | 150 px | 53.33 | 38 | |
| Choice row height | 135 px | 48.00 | | |
| Button row height | 135 px | 48.00 | 50 | min == max |
| Button end gap | 34 px | 12.09 | 47 | min == max |
| Button bottom gap | 11 px | 3.91 | 47 | median |

The vertical region was validated against variable-height modals rather than
assumed: screen 031 (h=845 -> top 159), 042 (h=996 -> top 84), 118 (h=532 ->
top 316), 129 (h=536 -> top 314). All land within 1 px of `[83,1080]` centring.

## Anchored spinner popups

Screens 027 and 112 are **not** AlertDialogs. The hierarchy shows a bare
`ListView` of `CheckedTextView` rows with no title and no button bar, placed
next to the spinner that opened it.

| Screen | Popup bounds | Rows | Row height | Panel colour |
|---|---|---:|---:|---|
| 027 | `[98,157]-[657,832]` | 5 | 135 px / 48 dp | `#303030` |
| 112 | `[278,270]-[575,1080]` | 6 | 135 px / 48 dp | `#303030` |

Row label inset is 27 px / 9.6 dp, measured from the first row's glyph bounding
box in screen 027 — notably not the 16 dp a Material popup would use.

## Scroll state

`scripts/extract_scroll_anchors.py` records, for all 108 scrollable captures, the
label of the first row inside the list viewport. Every audited row snapped flush
to the viewport top (`row_top = 219 px` on all 72 settings states), so restoring
the audited scroll position requires only that label.

This replaced hand-guessed list indices. Before the change the differ measured
240-600 px of vertical error on those states while their horizontal error was
exactly 0 px — the signature of a scroll offset rather than a layout fault.
