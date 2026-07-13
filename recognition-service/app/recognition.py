"""Garment recognition pipeline (SPEC.md section 3, garment-recognition-service).

Three steps run on every uploaded photo:

1. Background removal -> transparent PNG.
2. Category classification (top / bottom / socks / layer / shoes / accessory).
3. Dominant colour extraction.

Background removal uses `rembg` (a light U^2-Net segmentation model) when it is
installed; otherwise it falls back to a fast corner-sampled colour-key that works
fully offline. The category classifier here is an intentionally simple
aspect-ratio heuristic — a clearly marked seam where a trained model can be
dropped in later without changing the API.
"""
from __future__ import annotations

import base64
import io
import math
from dataclasses import dataclass

import numpy as np
from PIL import Image

# Keep processing fast and memory-bounded regardless of upload size.
MAX_DIM = 512
# Categories must match com.dressd.common.domain.GarmentCategory.
CATEGORIES = ("TOP", "BOTTOM", "SOCKS", "LAYER", "SHOES", "ACCESSORY")

try:  # rembg is optional and heavy; guarded so the service always starts.
    from rembg import remove as _rembg_remove  # type: ignore

    _REMBG_AVAILABLE = True
except Exception:  # pragma: no cover - exercised only when rembg is absent
    _rembg_remove = None
    _REMBG_AVAILABLE = False


@dataclass
class Recognition:
    category: str
    color_tag: str
    pattern: str
    image_png: bytes

    def image_base64(self) -> str:
        return base64.b64encode(self.image_png).decode("ascii")


def recognize(image_bytes: bytes) -> Recognition:
    image = Image.open(io.BytesIO(image_bytes)).convert("RGBA")
    image = _downscale(image)

    cutout = _remove_background(image)
    mask = np.asarray(cutout)[:, :, 3] > 16  # foreground alpha mask

    category = _classify(mask)
    color_tag = _dominant_color(np.asarray(cutout), mask)
    pattern = _detect_pattern(np.asarray(cutout), mask)

    buffer = io.BytesIO()
    cutout.save(buffer, format="PNG")
    return Recognition(category, color_tag, pattern, buffer.getvalue())


def _downscale(image: Image.Image) -> Image.Image:
    w, h = image.size
    scale = min(1.0, MAX_DIM / max(w, h))
    if scale < 1.0:
        image = image.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.LANCZOS)
    return image


def _remove_background(image: Image.Image) -> Image.Image:
    if _REMBG_AVAILABLE:
        try:
            return _rembg_remove(image).convert("RGBA")
        except Exception:
            pass  # fall through to the offline key
    return _color_key_background(image)


def _color_key_background(image: Image.Image) -> Image.Image:
    """Offline fallback: estimate the background colour from the four corners
    and make matching pixels transparent. Works well for photos shot against a
    fairly uniform backdrop, which is the common wardrobe-scan case."""
    arr = np.asarray(image).astype(np.int16)
    h, w = arr.shape[:2]
    patch = max(1, min(h, w) // 20)
    corners = np.concatenate([
        arr[:patch, :patch].reshape(-1, 4),
        arr[:patch, -patch:].reshape(-1, 4),
        arr[-patch:, :patch].reshape(-1, 4),
        arr[-patch:, -patch:].reshape(-1, 4),
    ])
    bg = corners[:, :3].mean(axis=0)

    dist = np.sqrt(((arr[:, :, :3] - bg) ** 2).sum(axis=2))
    threshold = 45.0
    alpha = np.where(dist < threshold, 0, 255).astype(np.uint8)

    out = arr.astype(np.uint8)
    out[:, :, 3] = alpha
    return Image.fromarray(out, mode="RGBA")


def _classify(mask: np.ndarray) -> str:
    """Aspect-ratio heuristic over the garment's bounding box. Placeholder for a
    trained classifier — see module docstring."""
    ys, xs = np.where(mask)
    if len(xs) == 0:
        return "ACCESSORY"

    box_w = xs.max() - xs.min() + 1
    box_h = ys.max() - ys.min() + 1
    coverage = len(xs) / mask.size
    ratio = box_h / box_w if box_w else 1.0

    if coverage < 0.06:
        return "ACCESSORY"
    if ratio >= 1.6:
        return "BOTTOM"
    if ratio <= 0.7:
        # short and wide: socks/shoes tend to sit low and wide, tops are broader
        return "SHOES" if coverage < 0.2 else "TOP"
    if box_h < mask.shape[0] * 0.3:
        return "SOCKS"
    return "TOP"


def _dominant_color(arr: np.ndarray, mask: np.ndarray) -> str:
    fg = arr[mask][:, :3]
    if len(fg) == 0:
        return "#888888"
    # Coarse quantisation then most-frequent bucket -> robust dominant colour.
    quantised = (fg // 24) * 24 + 12
    colors, counts = np.unique(quantised, axis=0, return_counts=True)
    r, g, b = colors[counts.argmax()]
    return f"#{int(r):02x}{int(g):02x}{int(b):02x}"


def _detect_pattern(arr: np.ndarray, mask: np.ndarray) -> str:
    """Very rough solid-vs-patterned signal from foreground colour variance
    (SPEC.md P1: pattern detection). Conservative: only flags clearly busy
    garments as non-solid."""
    fg = arr[mask][:, :3].astype(np.float32)
    if len(fg) < 50:
        return "solid"
    std = fg.std(axis=0).mean()
    return "patterned" if std > 55 else "solid"
