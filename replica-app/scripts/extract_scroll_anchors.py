#!/usr/bin/env python3
"""Derive each audited settings state's scroll position from the audit UI-XML.

The debug state catalog originally carried hand-guessed list indices, which put
most settings captures hundreds of pixels away from the audited scroll offset
(`scripts/geometry_diff.py` measured dtop errors of 240-600 px while dleft was
exactly 0). The audit hierarchy already records which row sits where, so the
anchor is evidence rather than a guess.

For each settings state this records the first preference row that intersects the
list viewport, its visible label, and how far that row is scrolled past the top of
the viewport. The app consumes the result at runtime to place the list exactly.

Output: app/src/main/assets/audit-scroll-anchors.json
"""
from __future__ import annotations

import csv
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
AUDIT_ROOT = PROJECT_ROOT.parent / "app-audit"
XMLDIR = AUDIT_ROOT / "evidence" / "ui-xml"
CATALOG = AUDIT_ROOT / "screens" / "screen-catalog.csv"
OUT = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "audit-scroll-anchors.json"

BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")

# Audit evidence: settings content occupies [75,219]-[2181,1080]; the app bar ends
# at y=219 and the left safe inset is 75 px (app-audit/device/display-and-insets.md).
CONTENT_TOP = 219
CONTENT_LEFT = 75
CONTENT_RIGHT = 2181
DENSITY = 2.8125


def bounds(node):
    m = BOUNDS.fullmatch(node.get("bounds", ""))
    return tuple(int(v) for v in m.groups()) if m else None


def main() -> int:
    with CATALOG.open(encoding="utf-8-sig", newline="") as fh:
        rows = list(csv.DictReader(fh))

    anchors = {}
    for row in rows:
        sid = row["screen_id"]
        surface = row["surface_type"]
        path = XMLDIR / f"{sid}.xml"
        if not path.exists():
            continue
        nodes = [n for n in ET.parse(path).getroot().iter("node") if bounds(n)]

        if surface in ("settings_screen", "validation_state"):
            # Preference rows span the full content width and sit below the app bar.
            candidates = [
                (bounds(n)[1], n)
                for n in nodes
                if bounds(n)[0] == CONTENT_LEFT
                and bounds(n)[2] == CONTENT_RIGHT
                and bounds(n)[3] > CONTENT_TOP
                and bounds(n)[3] - bounds(n)[1] >= 100
            ]
            viewport_top = CONTENT_TOP
        elif surface == "selection_dialog":
            # Choice rows sit inside a ListView/RecyclerView within the modal. The
            # audited *_menu_lower / *_menu_middle states are scrolled, so the anchor
            # must name the first visible option, not the first option in the list.
            listv = next(
                (n for n in nodes if "ListView" in (n.get("class") or "") or "RecyclerView" in (n.get("class") or "")),
                None,
            )
            if listv is None:
                continue
            list_left, list_top, list_right, _ = bounds(listv)
            viewport_top = list_top
            candidates = [
                (bounds(n)[1], n)
                for n in nodes
                if bounds(n)[0] >= list_left
                and bounds(n)[2] <= list_right
                and bounds(n)[1] >= list_top
                and (n.get("text") or "").strip()
            ]
        else:
            continue

        if not candidates:
            continue
        candidates.sort(key=lambda c: c[0])
        top, node = candidates[0]

        # The row's own label is the first text descendant.
        label = ""
        for child in node.iter("node"):
            text = (child.get("text") or "").strip()
            if text:
                label = text
                break
        if not label:
            label = (node.get("text") or "").strip()
        if not label:
            continue

        anchors[sid] = {
            "label": label,
            # How much of the anchor row has scrolled above the viewport top.
            "scroll_offset_px": max(0, viewport_top - top),
            "scroll_offset_dp": round(max(0, viewport_top - top) / DENSITY, 2),
            "row_top_px": top,
            "surface_type": surface,
        }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(anchors, indent=1, sort_keys=True), encoding="utf-8")
    print(f"wrote {len(anchors)} scroll anchors -> {OUT}")
    partial = sum(1 for a in anchors.values() if a["scroll_offset_px"] > 0)
    print(f"  anchors with a partially scrolled first row: {partial}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
