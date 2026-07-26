# API reference

All responses are JSON. Errors use the shared `ApiError` body from
`com.dressd.common.web.ApiError`:

```json
{
  "timestamp": "2026-07-26T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/garments",
  "details": ["category: must not be null"]
}
```

## Authentication

Two modes, selected by `dressd.auth.mode` — see
[SECURITY.md](SECURITY.md) for the full picture.

| Mode    | Credential                        | Notes                                              |
| ------- | --------------------------------- | -------------------------------------------------- |
| `DEV`   | `X-Owner-Id: <uuid>` (optional)   | Falls back to a fixed demo owner. Local default.   |
| `TOKEN` | `Authorization: Bearer <token>`   | `X-Owner-Id` is ignored. The `prod` profile default. |

A malformed `X-Owner-Id` is a `400`. A missing or invalid bearer token in TOKEN
mode is a `401` with `WWW-Authenticate: Bearer`.

Public paths (no credential required) are configurable per service via
`dressd.auth.public-paths`; by default `/actuator/**`, `/media/**` and `/error`,
plus `/api/avatars/body-types` on avatar-service.

Mint a token for local TOKEN-mode testing:

```bash
cd backend && mvn -q -pl common package -DskipTests
java -cp common/target/classes com.dressd.common.auth.OwnerTokenTool \
  "$AUTH_SECRET" 00000000-0000-0000-0000-000000000001
```

## Pagination

`GET /api/garments` and `GET /api/outfits` return a page envelope
(`com.dressd.common.web.PageResponse`):

```json
{
  "content": [],
  "page": 0,
  "size": 50,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

`page` defaults to `0`, `size` to `50`, and `size` is **clamped to 200** — a client
cannot ask a service to materialise an unbounded result set. Both endpoints sort
newest-updated first.

`GET /api/avatars` is deliberately *not* paginated: an owner holds at most one
avatar per body type, so the collection is bounded by the catalog.

## wardrobe-service — `:8081`

| Method   | Path                                                | Purpose                                                     |
| -------- | --------------------------------------------------- | ----------------------------------------------------------- |
| `POST`   | `/api/scan`                                         | Multipart photo → cut-out + suggested metadata (nothing persisted) |
| `GET`    | `/api/garments?category=&color=&season=&page=&size=` | List/filter the wardrobe (paged)                            |
| `GET`    | `/api/garments/{id}`                                | Fetch one garment                                           |
| `POST`   | `/api/garments`                                     | Persist a confirmed garment                                 |
| `PATCH`  | `/api/garments/{id}`                                | Partial metadata update (re-categorise, recolour)           |
| `DELETE` | `/api/garments/{id}`                                | Remove a garment **and its stored image**                   |
| `GET`    | `/media/**`                                         | Fetch a stored cut-out PNG                                  |

### `POST /api/scan`

Multipart field `file`. The part's content type must be one of `image/jpeg`,
`image/png`, `image/webp`, `image/heic` — anything else is a `400`. Uploads are
capped at 15MB.

The cut-out is written to `pending/<owner>/<uuid>.png` and returned as
`/media/pending/...`. Nothing is persisted in the database at this point, and
unconfirmed scans are swept after `dressd.storage.pending-ttl` (6h by default).

```json
{
  "imageUrl": "/media/pending/00000000-.../a1b2.png",
  "suggestedCategory": "TOP",
  "suggestedColorTag": "#3a5f8a",
  "suggestedPattern": "solid"
}
```

### `POST /api/garments`

`imageUrl` must be a `/media/pending/<your-owner-id>/...` URL from your own scan
(an already-confirmed `/media/garments/<your-owner-id>/...` URL is also accepted,
which makes retries idempotent). Confirming **moves** the image into
`garments/<owner>/`, and the response carries the new URL.

Any other value is a `400`: this is the authorisation check on the field, without
which a caller could attach an arbitrary URL — or another owner's image — to their
own garment.

```json
{
  "category": "TOP",
  "imageUrl": "/media/pending/00000000-.../a1b2.png",
  "colorTag": "#3a5f8a",
  "pattern": "solid",
  "season": "ALL",
  "anchorPoints": { "shoulderY": 0.2, "waistY": 0.45, "hemY": 0.7, "widthScale": 1.0 }
}
```

## avatar-service — `:8082`

| Method   | Path                       | Purpose                                              |
| -------- | -------------------------- | ---------------------------------------------------- |
| `GET`    | `/api/avatars/body-types`  | Static preset catalog (slim/average/curvy/custom); public |
| `GET`    | `/api/avatars`             | List the owner's avatars (unpaged, bounded)          |
| `GET`    | `/api/avatars/{id}`        | Fetch one avatar                                     |
| `POST`   | `/api/avatars`             | Create an avatar (defaults applied from body type)   |
| `PUT`    | `/api/avatars/{id}`        | Update an avatar                                     |
| `DELETE` | `/api/avatars/{id}`        | Remove an avatar                                     |

## outfit-composer-service — `:8083`

| Method   | Path                            | Purpose                                        |
| -------- | ------------------------------- | ---------------------------------------------- |
| `GET`    | `/api/outfits?page=&size=`      | List saved outfits, newest first (paged)       |
| `GET`    | `/api/outfits/{id}`             | Fetch one outfit (layers ordered by z-index)   |
| `POST`   | `/api/outfits`                  | Save an outfit (avatar + garment layers)       |
| `PUT`    | `/api/outfits/{id}`             | Update an outfit                               |
| `DELETE` | `/api/outfits/{id}`             | Remove an outfit                               |

### Layer validation

On save, the layer stack must satisfy:

| Rule                                   | Violation |
| -------------------------------------- | --------- |
| At most 32 layers                      | `400`     |
| No garment appearing twice             | `400`     |
| `scale` within `[0.1, 5.0]`            | `400`     |
| `offsetX` / `offsetY` within `[-2, 2]` | `400`     |

`zIndex` values are renumbered into a contiguous `0..n-1` range in the order given,
so stored outfits always render deterministically regardless of what the client
sent.

`garmentId` is **not** validated against the wardrobe — garments live in another
service. A saved outfit can therefore outlive one of its garments; see
[ADR-008](ADR.md#adr-008--no-cross-service-referential-integrity) for why, and how
the UI handles it.

## recognition-service — `:8000`

| Method | Path          | Purpose                                                      |
| ------ | ------------- | ------------------------------------------------------------ |
| `GET`  | `/health`     | Reports which background-removal backend is active           |
| `POST` | `/recognize`  | Multipart photo → `{category, colorTag, pattern, imageBase64}` |

`POST /recognize` rejects non-image content types with `415`, uploads over 15MB
with `413` (enforced while streaming, not after buffering), and images that are
unreadable or exceed 40 megapixels with `422`.

## Operational endpoints

Every Java service exposes, unauthenticated:

| Path                    | Purpose                              |
| ----------------------- | ------------------------------------ |
| `/actuator/health`      | Liveness/readiness, used by Docker   |
| `/actuator/info`        | Build version and timestamp          |
| `/actuator/metrics`     | Micrometer metrics                   |
| `/actuator/prometheus`  | Prometheus scrape endpoint           |
