import base64
import io

from fastapi.testclient import TestClient
from PIL import Image

from app.main import app
from app.recognition import recognize

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
