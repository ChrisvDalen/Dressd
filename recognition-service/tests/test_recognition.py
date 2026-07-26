import base64
import io

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app.main import MAX_UPLOAD_BYTES, app
from app.recognition import ImageRejected, recognize

client = TestClient(app)


def _make_photo(width: int, height: int, garment_color=(200, 40, 40)) -> bytes:
    """A garment-coloured rectangle centred on a near-white background."""
    img = Image.new("RGB", (width, height), (245, 245, 245))
    pad_x, pad_y = width // 6, height // 6
    for y in range(pad_y, height - pad_y):
        for x in range(pad_x, width - pad_x):
            img.putpixel((x, y), garment_color)
    buffer = io.BytesIO()
    img.save(buffer, format="PNG")
    return buffer.getvalue()


def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_recognize_returns_cutout_and_metadata():
    resp = client.post("/recognize", files={"file": ("shirt.png", _make_photo(300, 200), "image/png")})
    assert resp.status_code == 200
    body = resp.json()
    assert body["category"] in {"TOP", "BOTTOM", "SOCKS", "LAYER", "SHOES", "ACCESSORY"}
    assert body["colorTag"].startswith("#") and len(body["colorTag"]) == 7
    assert body["pattern"] in {"solid", "patterned"}

    # The returned cut-out is a valid PNG with an alpha channel.
    png = base64.b64decode(body["imageBase64"])
    out = Image.open(io.BytesIO(png))
    assert out.mode == "RGBA"


def test_background_becomes_transparent():
    result = recognize(_make_photo(200, 200))
    out = Image.open(io.BytesIO(result.image_png)).convert("RGBA")
    # A corner pixel (background) should be transparent.
    assert out.getpixel((2, 2))[3] == 0
    # A centre pixel (garment) should stay opaque.
    assert out.getpixel((100, 100))[3] > 0


def test_dominant_color_tracks_garment():
    # Strong blue garment -> blue channel dominant in the reported hex.
    result = recognize(_make_photo(200, 200, garment_color=(20, 40, 220)))
    hexval = result.color_tag.lstrip("#")
    r, g, b = int(hexval[0:2], 16), int(hexval[2:4], 16), int(hexval[4:6], 16)
    assert b > r and b > g


def test_tall_garment_classified_as_bottom():
    # A tall, narrow garment shape should read as BOTTOM.
    result = recognize(_make_photo(120, 400))
    assert result.category == "BOTTOM"


def test_empty_upload_rejected():
    resp = client.post("/recognize", files={"file": ("empty.png", b"", "image/png")})
    assert resp.status_code == 400


def test_non_image_content_type_rejected():
    resp = client.post(
        "/recognize", files={"file": ("payload.sh", b"#!/bin/sh\nrm -rf /", "text/x-shellscript")}
    )
    assert resp.status_code == 415
    assert "Unsupported content type" in resp.json()["detail"]


def test_oversized_upload_rejected_without_being_buffered_whole():
    # One byte over the limit is enough; the reader aborts mid-stream.
    oversized = b"\x00" * (MAX_UPLOAD_BYTES + 1)
    resp = client.post("/recognize", files={"file": ("big.png", oversized, "image/png")})
    assert resp.status_code == 413


def test_declared_image_that_is_not_an_image_is_rejected():
    resp = client.post(
        "/recognize", files={"file": ("lies.png", b"this is definitely not a png", "image/png")}
    )
    assert resp.status_code == 422


def test_decompression_bomb_rejected_before_decoding():
    # A ~50KB PNG that expands to 100M pixels — the classic decompression bomb.
    bomb = Image.new("L", (10_000, 10_000))
    buffer = io.BytesIO()
    bomb.save(buffer, format="PNG")

    with pytest.raises(ImageRejected, match="pixels"):
        recognize(buffer.getvalue())
