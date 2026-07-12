"""Response schema shared with the Java wardrobe-service RecognitionResult."""
from __future__ import annotations

from pydantic import BaseModel


class RecognitionResponse(BaseModel):
    # Must match com.dressd.common.domain.GarmentCategory
    category: str
    # Dominant colour as a hex string, e.g. "#3a5f8a"
    colorTag: str
    # solid / striped / ... (best-effort; "solid" when unknown)
    pattern: str
    # Background-removed PNG, base64 encoded (no data: prefix)
    imageBase64: str
