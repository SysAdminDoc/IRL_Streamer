#!/usr/bin/env python3
"""Compare audit UI-hierarchy bounds against replica UI-hierarchy bounds.

Screenshot SSIM tells you *that* a screen differs; it does not tell you *which
element* moved. This differ reads the immutable audit `evidence/ui-xml/<id>.xml`
and a replica hierarchy dumped from the emulator, matches nodes by visible text
(then by resource-id suffix / class + ordinal), and reports per-element pixel
deltas plus the aggregate layout error used by the audit's 2 px bounds target.

Usage:
  geometry_diff.py --screen 055_video_bitrate_editor \
      --replica validation/hierarchy/055_video_bitrate_editor.xml \
      --out validation/reports/geometry/055_video_bitrate_editor.json
  geometry_diff.py --all --replica-dir validation/hierarchy \
      --out-csv validation/reports/geometry-summary.csv
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, asdict
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
AUDIT_ROOT = PROJECT_ROOT.parent / "app-audit"
AUDIT_XML = AUDIT_ROOT / "evidence" / "ui-xml"
CATALOG = AUDIT_ROOT / "screens" / "screen-catalog.csv"

BOUNDS_RE = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")

# Identity copy differs by authorization (deviation D001); geometry of these
# nodes is not comparable because the replacement string has a different width.
IDENTITY_TEXT = {"IRL Pro", "IRL Streamer"}


@dataclass
class Node:
    text: str
    resource_id: str
    cls: str
    desc: str
    left: int
    top: int
    right: int
    bottom: int

    @property
    def width(self) -> int:
        return self.right - self.left

    @property
    def height(self) -> int:
        return self.bottom - self.top

    @property
    def key(self) -> str:
        return self.text.strip() or self.desc.strip()


def parse_nodes(path: Path) -> list[Node]:
    nodes: list[Node] = []
    root = ET.parse(path).getroot()
    for element in root.iter("node"):
        match = BOUNDS_RE.fullmatch(element.get("bounds", ""))
        if not match:
            continue
        left, top, right, bottom = (int(v) for v in match.groups())
        if right <= left or bottom <= top:
            continue
        nodes.append(
            Node(
                text=element.get("text", ""),
                resource_id=element.get("resource-id", ""),
                cls=element.get("class", ""),
                desc=element.get("content-desc", ""),
                left=left,
                top=top,
                right=right,
                bottom=bottom,
            )
        )
    return nodes


def index_by_key(nodes: list[Node]) -> dict[str, list[Node]]:
    table: dict[str, list[Node]] = {}
    for node in nodes:
        if not node.key:
            continue
        table.setdefault(node.key, []).append(node)
    return table


def compare(audit: list[Node], replica: list[Node]) -> dict:
    """Match audited nodes to replica nodes and measure their drawn origin.

    Only the *origin* (left, top) is scored. An Android `TextView` inside a
    preference row stretches to the full row width while the equivalent Compose
    `Text` wraps its glyphs, so right/bottom edges are not comparable between the
    two toolkits and are reported for information only. Candidates are paired by
    nearest origin rather than document order, because labels such as "Auto"
    recur several times on one screen and document order pairs them wrongly.
    """
    audit_index = index_by_key(audit)
    replica_index = index_by_key(replica)

    matched: list[dict] = []
    missing: list[str] = []
    for key, audit_nodes in sorted(audit_index.items()):
        if key in IDENTITY_TEXT:
            continue
        available = list(replica_index.get(key, []))
        if not available:
            missing.append(key)
            continue
        for audit_node in audit_nodes:
            if not available:
                missing.append(f"{key}#{len(matched)}")
                continue
            replica_node = min(
                available,
                key=lambda n: abs(n.left - audit_node.left) + abs(n.top - audit_node.top),
            )
            available.remove(replica_node)
            matched.append(
                {
                    "key": key,
                    "class": audit_node.cls.rsplit(".", 1)[-1],
                    "audit_bounds": [audit_node.left, audit_node.top, audit_node.right, audit_node.bottom],
                    "replica_bounds": [replica_node.left, replica_node.top, replica_node.right, replica_node.bottom],
                    "dleft": replica_node.left - audit_node.left,
                    "dtop": replica_node.top - audit_node.top,
                    "dwidth": replica_node.width - audit_node.width,
                    "dheight": replica_node.height - audit_node.height,
                }
            )

    extra = sorted(set(replica_index) - set(audit_index) - IDENTITY_TEXT)

    def worst(field: str) -> int:
        return max((abs(m[field]) for m in matched), default=0)

    origin_errors = [abs(m[f]) for m in matched for f in ("dleft", "dtop")]
    within_2 = sum(1 for e in origin_errors if e <= 2)
    within_4 = sum(1 for e in origin_errors if e <= 4)
    return {
        "matched_elements": len(matched),
        "unmatched_audit_elements": missing,
        "replica_only_elements": extra,
        "max_abs_dleft": worst("dleft"),
        "max_abs_dtop": worst("dtop"),
        "max_abs_dwidth": worst("dwidth"),
        "max_abs_dheight": worst("dheight"),
        "mean_abs_origin_error_px": round(sum(origin_errors) / len(origin_errors), 3) if origin_errors else 0.0,
        "origins_within_2px_pct": round(100.0 * within_2 / len(origin_errors), 2) if origin_errors else 0.0,
        "origins_within_4px_pct": round(100.0 * within_4 / len(origin_errors), 2) if origin_errors else 0.0,
        "elements": sorted(matched, key=lambda m: -max(abs(m["dleft"]), abs(m["dtop"]))),
    }


def screen_ids() -> list[str]:
    with CATALOG.open(encoding="utf-8-sig", newline="") as fh:
        return [row["screen_id"] for row in csv.DictReader(fh)]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--screen")
    parser.add_argument("--replica", type=Path)
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--replica-dir", type=Path, default=PROJECT_ROOT / "validation" / "hierarchy")
    parser.add_argument("--out", type=Path)
    parser.add_argument("--out-csv", type=Path)
    parser.add_argument("--out-dir", type=Path, default=PROJECT_ROOT / "validation" / "reports" / "geometry")
    args = parser.parse_args()

    targets = screen_ids() if args.all else [args.screen]
    if not targets or targets == [None]:
        parser.error("--screen or --all is required")

    summary_rows = []
    skipped_without_audit = []
    for sid in targets:
        audit_path = AUDIT_XML / f"{sid}.xml"
        replica_path = args.replica if (args.replica and not args.all) else args.replica_dir / f"{sid}.xml"
        if not audit_path.exists():
            print(f"{sid}: no audit XML", file=sys.stderr)
            skipped_without_audit.append(sid)
            continue
        if not replica_path.exists():
            summary_rows.append({"screen": sid, "status": "NO_REPLICA_DUMP"})
            continue
        result = compare(parse_nodes(audit_path), parse_nodes(replica_path))
        result["screen"] = sid
        out_path = args.out if (args.out and not args.all) else args.out_dir / f"{sid}.json"
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(result, indent=2), encoding="utf-8")
        summary_rows.append(
            {
                "screen": sid,
                "status": "OK",
                "matched": result["matched_elements"],
                "unmatched": len(result["unmatched_audit_elements"]),
                "max_dleft": result["max_abs_dleft"],
                "max_dtop": result["max_abs_dtop"],
                "max_dwidth": result["max_abs_dwidth"],
                "max_dheight": result["max_abs_dheight"],
                "mean_origin_err_px": result["mean_abs_origin_error_px"],
                "origins_within_2px_pct": result["origins_within_2px_pct"],
                "origins_within_4px_pct": result["origins_within_4px_pct"],
            }
        )
        if not args.all:
            print(json.dumps({k: v for k, v in result.items() if k != "elements"}, indent=2))
            for element in result["elements"][:15]:
                print(
                    f"  {element['key'][:40]:<40} d=({element['dleft']:+d},{element['dtop']:+d}) "
                    f"size=({element['dwidth']:+d},{element['dheight']:+d})"
                )

    if args.out_csv and summary_rows:
        fields = [
            "screen", "status", "matched", "unmatched", "max_dleft", "max_dtop",
            "max_dwidth", "max_dheight", "mean_origin_err_px", "origins_within_2px_pct", "origins_within_4px_pct",
        ]
        args.out_csv.parent.mkdir(parents=True, exist_ok=True)
        with args.out_csv.open("w", encoding="utf-8", newline="") as fh:
            writer = csv.DictWriter(fh, fieldnames=fields, extrasaction="ignore")
            writer.writeheader()
            writer.writerows(summary_rows)
        print(f"wrote {args.out_csv}")

    # A screen that could not be compared is not a screen that passed. The
    # caller gates on the CSV, but a silent skip here would shrink the
    # denominator without anything saying so.
    if skipped_without_audit:
        print(
            f"{len(skipped_without_audit)} screen(s) skipped with no audit XML: "
            + ", ".join(skipped_without_audit),
            file=sys.stderr,
        )
        return 1
    if not summary_rows:
        print("no screens compared", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
