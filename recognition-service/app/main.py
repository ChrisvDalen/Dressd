"""FastAPI entrypoint for the garment-recognition sidecar."""
from __future__ import annotations

from fastapi import FastAPI, File, HTTPException, UploadFile

from .models import RecognitionResponse
from .recognition import _REMBG_AVAILABLE, recognize

app = FastAPI(
    title="Dressd Garment Recognition Service",
    version="0.1.0",
    description="Background removal, category classification and colour extraction.",
)

MAX_UPLOAD_BYTES = 15 * 1024 * 1024


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "backgroundRemoval": "rembg" if _REMBG_AVAILABLE else "color-key"}


@app.post("/recognize", response_model=RecognitionResponse)
async def recognize_endpoint(file: UploadFile = File(...)) -> RecognitionResponse:
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty upload")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="File too large")

    try:
        result = recognize(content)
    except Exception as exc:  # noqa: BLE001 - surface a clean 422 to the caller
        raise HTTPException(status_code=422, detail=f"Could not process image: {exc}") from exc

    return RecognitionResponse(
        category=result.category,
        colorTag=result.color_tag,
        pattern=result.pattern,
        imageBase64=result.image_base64(),
    )
