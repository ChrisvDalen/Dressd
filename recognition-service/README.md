# garment-recognition-service

Python/FastAPI sidecar for the Dressd scan flow (SPEC.md section 3). Given a
photo it returns a background-removed PNG plus suggested metadata.

## Endpoint

`POST /recognize` (multipart, field `file`) →

```json
{
  "category": "TOP",
  "colorTag": "#3a5f8a",
  "pattern": "solid",
  "imageBase64": "<png bytes, base64>"
}
```

`GET /health` reports which background-removal backend is active.

## Input limits

The sidecar decodes attacker-supplied bytes, so uploads are vetted before they reach
the decoder:

| Rule                                                   | Response |
| ------------------------------------------------------ | -------- |
| Content type not `image/{jpeg,png,webp,heic}`           | `415`    |
| Larger than 15MB — enforced *while streaming*, so the read aborts rather than buffering the whole body first | `413`    |
| Empty body                                             | `400`    |
| Unreadable/corrupt, or over 40 megapixels once decoded  | `422`    |

The pixel ceiling is checked from the image header before any decode, which is what
stops a small file that describes an enormous bitmap. `Image.MAX_IMAGE_PIXELS` is
pinned to the same limit as a second line of defence.

Failures log the incoming `traceparent`, so a rejected scan can be tied back to the
wardrobe-service request that caused it.

## Pipeline

1. **Background removal** — uses [`rembg`](https://github.com/danielgatis/rembg)
   when installed, otherwise a fast offline corner-sampled colour-key.
2. **Category classification** — aspect-ratio heuristic over the garment's
   bounding box. This is a deliberate placeholder (`_classify` in
   `app/recognition.py`); swap in a trained classifier there without changing
   the API.
3. **Dominant colour** — most-frequent quantised foreground colour, as hex.

## Run locally

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn app.main:app --reload --port 8000
pytest
```

To enable the high-quality `rembg` cut-out, uncomment `rembg` / `onnxruntime`
in `requirements.txt` and reinstall.
