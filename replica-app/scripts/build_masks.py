#!/usr/bin/env python3
"""Generate per-screen comparison masks from the documented mask register.

A mask excludes a region from the *secondary* app-chrome metric only. The primary
gate in `visual_compare.py` stays unmasked, so nothing a mask touches can turn a
failing screen into a passing one. Every region must carry a category and a
reason in `validation/masks/mask-register.csv`; regions exist because the pixels
are owned by the operating system or are evidence that must not ship, never
because the reconstruction is inaccurate.

Scopes resolve against `app-audit/screens/screen-catalog.csv` surface types:
  all_surfaces     every captured state
  settings_surface settings_screen / dialog / selection_dialog / validation_state
  ime_surface      the three states whose screenshot is covered by the Samsung IME
"""
from __future__ import annotations

import csv
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

PROJECT_ROOT = Path(__file__).resolve().parent.parent
AUDIT_ROOT = PROJECT_ROOT.parent / "app-audit"
CATALOG = AUDIT_ROOT / "screens" / "screen-catalog.csv"
REGISTER = PROJECT_ROOT / "validation" / "masks" / "mask-register.csv"
OUT_DIR = PROJECT_ROOT / "validation" / "masks"

WIDTH, HEIGHT = 2316, 1080

SETTINGS_SURFACES = {"settings_screen", "dialog", "selection_dialog", "validation_state", "system_intent"}
# The audit screenshots for these three states are covered by the Samsung IME's
# landscape fullscreen extract-edit window; the app dialog underneath is proven by
# the matching UI hierarchy dumps.
IME_SCREENS = {
    "055_video_bitrate_editor",
    "057_video_keyframe_frequency_menu",
    "077_recording_section_duration_editor",
}


def scope_matches(scope: str, screen_id: str, surface: str) -> bool:
    if scope == "all_surfaces":
        return True
    if scope == "settings_surface":
        return surface in SETTINGS_SURFACES
    if scope == "ime_surface":
        return screen_id in IME_SCREENS
    raise ValueError(f"Unknown mask scope: {scope}")


def main() -> int:
    with CATALOG.open(encoding="utf-8-sig", newline="") as fh:
        catalog = {row["screen_id"]: row["surface_type"] for row in csv.DictReader(fh)}
    with REGISTER.open(encoding="utf-8-sig", newline="") as fh:
        regions = list(csv.DictReader(fh))

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    applied_rows = []
    for screen_id, surface in catalog.items():
        image = Image.new("L", (WIDTH, HEIGHT), 0)
        draw = ImageDraw.Draw(image)
        applied = []
        for region in regions:
            if not scope_matches(region["scope"], screen_id, surface):
                continue
            x, y = int(region["x"]), int(region["y"])
            w, h = int(region["width"]), int(region["height"])
            draw.rectangle([x, y, x + w - 1, y + h - 1], fill=255)
            applied.append(region["region_id"])
        image.save(OUT_DIR / f"{screen_id}.png")
        excluded = float((np.asarray(image) >= 128).mean())
        applied_rows.append(
            {
                "screen_id": screen_id,
                "surface_type": surface,
                "regions": " ".join(applied),
                "excluded_fraction": round(excluded, 4),
            }
        )

    out = PROJECT_ROOT / "validation" / "masks" / "applied-masks.csv"
    with out.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["screen_id", "surface_type", "regions", "excluded_fraction"])
        writer.writeheader()
        writer.writerows(applied_rows)
    print(f"generated {len(applied_rows)} masks; index: {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
