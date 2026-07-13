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
