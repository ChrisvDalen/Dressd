"""FastAPI entrypoint for the garment-recognition sidecar."""
from __future__ import annotations

import logging

from fastapi import FastAPI, File, Header, HTTPException, UploadFile

from .models import RecognitionResponse
from .recognition import _REMBG_AVAILABLE, ImageRejected, recognize

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Dressd Garment Recognition Service",
    version="0.1.0",
    description="Background removal, category classification and colour extraction.",
)

# Kept in step with spring.servlet.multipart.max-file-size in wardrobe-service.
MAX_UPLOAD_BYTES = 15 * 1024 * 1024
READ_CHUNK_BYTES = 256 * 1024

# The sidecar only ever receives photos, so anything else is rejected before it
# reaches the decoder.
ACCEPTED_CONTENT_TYPES = frozenset(
    {"image/jpeg", "image/png", "image/webp", "image/heic"}
)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "backgroundRemoval": "rembg" if _REMBG_AVAILABLE else "color-key"}


@app.post("/recognize", response_model=RecognitionResponse)
async def recognize_endpoint(
    file: UploadFile = File(...),
    traceparent: str | None = Header(default=None),
) -> RecognitionResponse:
    if file.content_type not in ACCEPTED_CONTENT_TYPES:
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported content type: {file.content_type or 'unknown'}",
        )

    content = await _read_limited(file)
    if not content:
        raise HTTPException(status_code=400, detail="Empty upload")

    try:
        result = recognize(content)
    except ImageRejected as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001 - surface a clean 422 to the caller
        # traceparent ties this line back to the wardrobe-service request that
        # triggered it.
        logger.warning("Recognition failed (traceparent=%s): %s", traceparent, exc)
        raise HTTPException(status_code=422, detail="Could not process image") from exc

    return RecognitionResponse(
        category=result.category,
        colorTag=result.color_tag,
        pattern=result.pattern,
        imageBase64=result.image_base64(),
    )


async def _read_limited(upload: UploadFile) -> bytes:
    """Read the upload, refusing it as soon as it passes the size limit.

    Reading straight into memory would let a caller decide how much of ours to
    use, so the limit is enforced while streaming rather than afterwards.
    """
    chunks: list[bytes] = []
    total = 0
    while chunk := await upload.read(READ_CHUNK_BYTES):
        total += len(chunk)
        if total > MAX_UPLOAD_BYTES:
            raise HTTPException(
                status_code=413,
                detail=f"File exceeds the {MAX_UPLOAD_BYTES // (1024 * 1024)}MB limit",
            )
        chunks.append(chunk)
    return b"".join(chunks)
