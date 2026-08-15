# Validation masks

White pixels are excluded from the masked metric; black pixels remain measurable. A mask must have exactly the same dimensions as its baseline and current screenshots (2316 x 1080).

## What a mask can and cannot do

**A mask can never turn a failure into a pass.** The strict gate is the *unmasked* whole-screen SSIM computed over every pixel (`gate: unmasked_whole_screen_ssim` in each `validation/results/<screen>.json`). Masks feed only the secondary `app_chrome_ssim` diagnostic, which is reported beside the strict score and never substituted for it.

Masks exist to answer one question: how much of a screen's difference is app-owned rather than operating-system-owned.

## Currently masked regions

Every region is declared in `mask-register.csv` with a category and a reason, and `scripts/build_masks.py` generates the per-screen PNGs from that register. Three regions are masked, all `SYSTEM_OWNED`:

| Scope | Region | Bounds (x, y, w, h) | Why |
|---|---|---|---|
| `settings_surface` | System status bar | 0, 0, 2316, 74 | Drawn by Android; carries the emulator's own clock and icons. |
| `all_surfaces` | System navigation bar | 2181, 0, 135, 1080 | The Samsung three-button strip; the AVD renders its own. |
| `ime_surface` | IME extract window | 0, 74, 1884, 1006 | Screens 055, 057 and 077 were captured with the Samsung keyboard covering the app. The audit hierarchy proves the app dialog is present underneath, and that dialog is reproduced (deviation D013). |

## The camera preview is deliberately not masked

Excluding it would remove about 97% of a live-console screen and leave a number that means nothing. Those states are validated by the geometry gate instead, and their pixel gap is recorded as deviation D003.

## Rules for adding one

- Only genuinely operating-system-owned or externally-variable content qualifies.
- Never mask a region because the reconstruction is inaccurate there. That is what the strict gate is for.
- Every region needs a row in `mask-register.csv` with a category and a reason, and a line in the table above.
