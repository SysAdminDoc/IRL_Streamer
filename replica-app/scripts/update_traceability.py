#!/usr/bin/env python3
"""Refresh the traceability and status CSVs from the current validation artifacts.

Both documents are derived, not hand-maintained: every status, metric and pointer
is read back out of `validation/`. A state is only promoted past IMPLEMENTED on
evidence that exists on disk.

Status ladder used here:
  IMPLEMENTED             rendered and captured
  BEHAVIORALLY_VALIDATED  element origins match the audit hierarchy within 2 px
                          for at least 95% of matched elements
  VISUALLY_VALIDATED      the strict unmasked SSIM gate passed
"""
from __future__ import annotations

import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VALIDATION = ROOT / "validation"
DOCS = ROOT / "docs"


def load_results() -> dict[str, dict]:
    out = {}
    for path in (VALIDATION / "results").glob("*.json"):
        out[path.stem] = json.loads(path.read_text(encoding="utf-8"))
    return out


def load_geometry() -> dict[str, dict]:
    out = {}
    csv_path = VALIDATION / "reports" / "geometry-summary.csv"
    if not csv_path.exists():
        return out
    with csv_path.open(encoding="utf-8-sig", newline="") as fh:
        for row in csv.DictReader(fh):
            if row.get("status") == "OK":
                out[row["screen"]] = row
    return out


def classify(sid: str, results: dict, geometry: dict) -> tuple[str, str]:
    result = results.get(sid)
    geo = geometry.get(sid)
    if result is None:
        return "NOT_STARTED", "No capture on disk."

    ssim = result.get("ssim", 0.0)
    threshold = result.get("threshold", 0.985)
    chrome = result.get("app_chrome_ssim")
    parts = [f"strict SSIM {ssim:.6f} vs {threshold}"]
    if chrome is not None:
        parts.append(f"app-chrome SSIM {chrome:.6f}")
    if geo:
        parts.append(
            f"geometry {geo['origins_within_2px_pct']}% of {geo['matched']} element origins within 2 px "
            f"(mean {geo['mean_origin_err_px']} px)"
        )

    if result.get("status") == "PASS":
        return "VISUALLY_VALIDATED", "; ".join(parts) + "."
    if geo and float(geo["origins_within_2px_pct"]) >= 95.0:
        return "BEHAVIORALLY_VALIDATED", "; ".join(parts) + "."
    return "IMPLEMENTED", "; ".join(parts) + "."


def rewrite(path: Path, status_field: str, note_field: str, id_field: str,
            results: dict, geometry: dict, extra) -> dict[str, int]:
    with path.open(encoding="utf-8-sig", newline="") as fh:
        reader = csv.DictReader(fh)
        fieldnames = reader.fieldnames
        rows = list(reader)

    counts: dict[str, int] = {}
    for row in rows:
        sid = row[id_field]
        status, note = classify(sid, results, geometry)
        row[status_field] = status
        row[note_field] = note
        if extra:
            extra(row, sid, results.get(sid), geometry.get(sid))
        counts[status] = counts.get(status, 0) + 1

    with path.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames, quoting=csv.QUOTE_ALL)
        writer.writeheader()
        writer.writerows(rows)
    return counts


def main() -> int:
    results = load_results()
    geometry = load_geometry()

    def status_extra(row, sid, result, geo):
        if result is not None:
            row["visual_validation"] = (
                f"{'PASS' if result.get('status') == 'PASS' else 'FAIL'}_SSIM_{result.get('ssim', 0.0):.6f}"
            )

    counts_status = rewrite(
        DOCS / "implementation-status.csv", "status", "notes", "audit_screen_id",
        results, geometry, status_extra,
    )
    counts_matrix = rewrite(
        DOCS / "audit-traceability-matrix.csv", "current_status", "reason_for_deviation", "audit_screen_id",
        results, geometry, None,
    )

    print("implementation-status.csv:", dict(sorted(counts_status.items())))
    print("audit-traceability-matrix.csv:", dict(sorted(counts_matrix.items())))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
