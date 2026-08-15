#!/usr/bin/env python3
"""Deterministic screenshot comparison used by compare-screen.ps1."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from skimage.metrics import structural_similarity


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--current", type=Path, required=True)
    parser.add_argument("--mask", type=Path)
    parser.add_argument("--side-by-side", type=Path, required=True)
    parser.add_argument("--overlay", type=Path, required=True)
    parser.add_argument("--diff", type=Path, required=True)
    parser.add_argument("--result", type=Path, required=True)
    parser.add_argument("--threshold", type=float, required=True)
    parser.add_argument("--max-shift", type=int, default=2)
    return parser.parse_args()


def shifted(image: np.ndarray, dx: int, dy: int) -> np.ndarray:
    output = np.zeros_like(image)
    height, width = image.shape[:2]
    src_x0, src_x1 = max(0, -dx), min(width, width - dx)
    src_y0, src_y1 = max(0, -dy), min(height, height - dy)
    dst_x0, dst_x1 = max(0, dx), min(width, width + dx)
    dst_y0, dst_y1 = max(0, dy), min(height, height + dy)
    output[dst_y0:dst_y1, dst_x0:dst_x1] = image[src_y0:src_y1, src_x0:src_x1]
    return output


def best_alignment(reference: np.ndarray, current: np.ndarray, valid: np.ndarray, maximum: int) -> tuple[int, int]:
    sample = np.s_[::4, ::4]
    best = (float("inf"), 0, 0)
    for dy in range(-maximum, maximum + 1):
        for dx in range(-maximum, maximum + 1):
            candidate = shifted(current, dx, dy)
            active = valid[sample]
            if not np.any(active):
                continue
            delta = reference[sample].astype(np.float32) - candidate[sample].astype(np.float32)
            mse = float(np.mean(np.square(delta[active])))
            if mse < best[0]:
                best = (mse, dx, dy)
    return best[1], best[2]


def save_side_by_side(reference: Image.Image, current: Image.Image, path: Path) -> None:
    gap, heading = 16, 28
    canvas = Image.new("RGB", (reference.width * 2 + gap, reference.height + heading), "#111111")
    canvas.paste(reference, (0, heading))
    canvas.paste(current, (reference.width + gap, heading))
    draw = ImageDraw.Draw(canvas)
    draw.text((8, 7), "AUDIT BASELINE", fill="white")
    draw.text((reference.width + gap + 8, 7), "REPLICA", fill="white")
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path)


def main() -> int:
    args = parse_args()
    reference_image = Image.open(args.baseline).convert("RGB")
    current_image = Image.open(args.current).convert("RGB")
    args.result.parent.mkdir(parents=True, exist_ok=True)

    if reference_image.size != current_image.size:
        result = {
            "status": "DIMENSION_MISMATCH",
            "baseline_size": list(reference_image.size),
            "current_size": list(current_image.size),
            "ssim": 0.0,
            "threshold": args.threshold,
            "alignment": {"dx": 0, "dy": 0},
        }
        args.result.write_text(json.dumps(result, indent=2), encoding="utf-8")
        save_side_by_side(reference_image, current_image, args.side_by_side)
        print(json.dumps(result))
        return 3

    reference = np.asarray(reference_image, dtype=np.uint8)
    current = np.asarray(current_image, dtype=np.uint8)
    valid = np.ones(reference.shape[:2], dtype=bool)
    if args.mask:
        mask_image = Image.open(args.mask).convert("L")
        if mask_image.size != reference_image.size:
            raise ValueError("Mask dimensions must match the screenshots")
        valid = np.asarray(mask_image) < 128
    if not np.any(valid):
        raise ValueError("Mask excludes the entire screenshot")

    dx, dy = best_alignment(reference, current, valid, max(0, args.max_shift))
    aligned = shifted(current, dx, dy)
    aligned_image = Image.fromarray(aligned)
    absolute = np.abs(reference.astype(np.int16) - aligned.astype(np.int16)).astype(np.uint8)
    active_values = absolute[valid]
    mae = float(np.mean(active_values) / 255.0)
    rmse = float(np.sqrt(np.mean(np.square(active_values.astype(np.float64)))) / 255.0)
    within_eight = float(np.mean(np.max(absolute, axis=2)[valid] <= 8))
    # The strict gate is always the unmasked whole-screen score. A mask only ever
    # produces the *secondary* app-chrome diagnostic, so excluding a region can
    # never turn a failing screen into a passing one.
    whole_score, score_map = structural_similarity(reference, aligned, channel_axis=2, data_range=255, full=True)
    score = float(whole_score)
    per_pixel = np.mean(score_map, axis=2) if score_map.ndim == 3 else score_map
    app_chrome_score = float(np.mean(per_pixel[valid])) if args.mask else None

    args.overlay.parent.mkdir(parents=True, exist_ok=True)
    Image.blend(reference_image, aligned_image, 0.5).save(args.overlay)
    heat = np.max(absolute, axis=2)
    heat_rgb = np.zeros_like(reference)
    # heat is uint8: multiplying before widening wraps modulo 256, so a diff of
    # 86 rendered as red 2 and the *worst* regions came out near-black - in the
    # exact images the report tells a reviewer to inspect.
    heat_rgb[..., 0] = np.clip(heat.astype(np.int16) * 3, 0, 255)
    heat_rgb[..., 1] = np.clip(np.maximum(heat.astype(np.int16) - 85, 0) * 3, 0, 255)
    heat_rgb[~valid] = np.array([32, 32, 32], dtype=np.uint8)
    args.diff.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(heat_rgb).save(args.diff)
    save_side_by_side(reference_image, aligned_image, args.side_by_side)

    result = {
        "status": "PASS" if score >= args.threshold else "FAIL",
        "baseline_size": list(reference_image.size),
        "current_size": list(current_image.size),
        "ssim": score,
        "threshold": args.threshold,
        "normalized_mae": mae,
        "normalized_rmse": rmse,
        "pixel_fraction_within_8": within_eight,
        "masked_pixel_fraction": float(1.0 - np.mean(valid)),
        "alignment": {"dx": dx, "dy": dy},
        # Secondary diagnostic only: SSIM over app-owned comparable pixels, with the
        # documented system/evidence-only regions excluded. Never gates the result.
        "app_chrome_ssim": app_chrome_score,
        "gate": "unmasked_whole_screen_ssim",
    }
    args.result.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps(result))
    return 0 if result["status"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
