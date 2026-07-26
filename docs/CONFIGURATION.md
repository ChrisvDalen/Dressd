# Configuration reference

Every Java service reads `application.yml` with a `prod` profile override. Defaults
target a laptop with nothing else running: H2 file database, local filesystem
storage, `DEV` auth.

For the compose stack these are supplied through `.env` — copy
[`.env.example`](../.env.example) and edit.

## Environment variables (`prod` profile)

| Variable                 | Service(s) | Default                                    | Meaning                              |
| ------------------------ | ---------- | ------------------------------------------ | ------------------------------------ |
| `SPRING_PROFILES_ACTIVE` | all        | `default` (H2)                             | set `prod` for Postgres              |
| `DB_URL`                 | all        | `jdbc:postgresql://localhost:5432/dressd`  | JDBC URL                             |
| `DB_USER` / `DB_PASSWORD`| all        | `dressd` / `dressd`                        | database credentials                 |
| `AUTH_MODE`              | all        | `TOKEN` in `prod`, `DEV` otherwise         | `DEV` or `TOKEN`; see [SECURITY.md](SECURITY.md) |
| `AUTH_SECRET`            | all        | *(none)*                                   | HMAC secret, ≥32 chars; **required** in `TOKEN` mode |
| `CORS_ORIGINS`           | all        | `http://localhost:4200`                    | comma-separated allowed origins      |
| `TRACING_SAMPLE_RATE`    | all        | `0.1` in `prod`                            | fraction of requests traced          |
| `RECOGNITION_URL`        | wardrobe   | `http://recognition-service:8000`          | sidecar base URL                     |
| `STORAGE_ROOT`           | wardrobe   | `/data/media`                              | object-storage root                  |
| `STORAGE_PUBLIC_URL`     | wardrobe   | `/media`                                   | public URL prefix                    |

## Application properties

### `dressd.auth`

| Property                    | Default                                  | Meaning                                            |
| --------------------------- | ---------------------------------------- | -------------------------------------------------- |
| `mode`                      | `DEV`                                    | `DEV` trusts `X-Owner-Id`; `TOKEN` requires a bearer token |
| `secret`                    | *(empty)*                                | HMAC-SHA256 signing secret, minimum 32 characters  |
| `demo-owner`                | `00000000-0000-0000-0000-000000000001`   | owner used in `DEV` when no header is sent         |
| `token-ttl`                 | `24h`                                    | lifetime of a freshly minted token                 |
| `public-paths`              | `/actuator/**`, `/media/**`, `/error`    | request paths exempt from authentication           |

Starting in `TOKEN` mode without a usable `secret` is a startup failure, by design.

### `dressd.storage` (wardrobe-service)

| Property                    | Default          | Meaning                                                   |
| --------------------------- | ---------------- | --------------------------------------------------------- |
| `local.root`                | `./data/media`   | filesystem directory backing the store                    |
| `local.public-base-url`     | `/media`         | URL prefix the objects are served under                   |
| `pending-ttl`               | `6h`             | how long an unconfirmed scan survives before being swept  |
| `pending-cleanup-interval`  | `1h`             | how often the sweep runs                                  |
| `pending-cleanup-enabled`   | `true`           | set `false` to disable the sweep (the test profile does)   |

### `dressd.recognition` (wardrobe-service)

| Property    | Default                  | Meaning                                          |
| ----------- | ------------------------ | ------------------------------------------------ |
| `base-url`  | `http://localhost:8000`  | sidecar base URL                                 |
| `enabled`   | `true`                   | `false` skips the network call and always stubs   |

### `dressd.cors`

| Property           | Default                   | Meaning                          |
| ------------------ | ------------------------- | -------------------------------- |
| `allowed-origins`  | `http://localhost:4200`   | origins allowed to call the API  |

In the compose stack nginx serves the app and the API from one origin, so CORS is
moot there; the setting matters for `ng serve` on `:4200`.

## Recognition sidecar

| Setting            | Default   | Meaning                                                        |
| ------------------ | --------- | -------------------------------------------------------------- |
| `rembg` installed? | no        | if present, high-quality cut-out; otherwise offline colour-key  |
| `MAX_UPLOAD_BYTES` | 15MB      | enforced while streaming the upload (`app/main.py`)             |
| `MAX_PIXELS`       | 40M       | decoded-pixel ceiling, checked from the header (`app/recognition.py`) |

Enable `rembg` by uncommenting `rembg` / `onnxruntime` in
`recognition-service/requirements.txt` and reinstalling. The upload cap is kept in
step with `spring.servlet.multipart.max-file-size` in wardrobe-service and
`client_max_body_size` in `web/nginx.conf` — change all three together.

## Observability

Actuator exposes `health`, `info`, `metrics` and `prometheus` on every service.

Micrometer tracing propagates W3C trace context, so a scan request's trace id
carries from wardrobe-service into the recognition sidecar, which logs the incoming
`traceparent` on failure. Sampling is 100% in dev and `TRACING_SAMPLE_RATE` (default
10%) in prod.

The `prod` profile emits one ECS-formatted JSON line per log event; dev keeps the
human-readable console pattern with `[service,traceId,spanId]` prefixed.
