# Mobile client — status: deferred (Phase 3)

The build spec lists mobile as an **open question** (SPEC.md section 8) with no
decided stack:

> Mobiel: native (Kotlin/Swift, apart per platform) of cross-platform? Bepaalt
> of canvas-rendering herbruikbaar is met de Angular-implementatie.

Because the platform choice is explicitly undecided — and it is the decision
that determines whether the canvas/swipe rendering can be shared with the web
implementation — the mobile client is intentionally **not** built in this pass.
Guessing a stack here would bake in an architectural decision the spec reserves.

What *is* in place to make Phase 3 a thin client effort:

- The backend exposes a **shared REST API** (the same endpoints the web app
  uses). See `backend/README.md` and the OpenAPI-shaped controllers.
- The domain model keeps garment images **separate from rendering**, so any
  client (web canvas, Compose canvas, SwiftUI, Flutter `CustomPainter`) can
  reproduce the same layer-composition logic from `Outfit.garmentLayers`
  (z-index, offset, scale) and `Garment.anchorPoints`.
- The scan flow is a single `POST /api/scan` multipart call, so native camera
  integration only has to hand raw photo bytes to the same endpoint.

## Recommendation

If canvas rendering reuse with Angular is a priority, a cross-platform stack
(Flutter or React Native) lets the swipe/compose interaction be written once.
If native feel and camera performance dominate, Kotlin/Compose + Swift/SwiftUI
share only the API. This choice should be made before Phase 3 starts and
recorded back in `SPEC.md` section 8.
