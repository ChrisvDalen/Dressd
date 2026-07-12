# Dressd

Digitale garderobe-app. Scan je kleding met de camera, laat de achtergrond
automatisch verwijderen en categoriseren, en stel outfits samen door per
kledingcategorie horizontaal te swipen op een 2D poppetje.

De volledige build-specificatie is leidend en staat in
[`docs/SPEC.md`](docs/SPEC.md).

## Architectuur

```
                 Angular web (nginx :8080 / ng serve :4200)
                 WardrobeStore · ScanFlow · AvatarCanvas · CategorySwipe
                                    │  REST/JSON (same-origin)
   ┌────────────────┬──────────────┼───────────────────┐
   ▼                ▼              ▼                    ▼
wardrobe-service  avatar-service  outfit-composer   garment-recognition
  (Java :8081)     (Java :8082)   (Java :8083)      (Python FastAPI :8000)
   │  scan orchestration → calls recognition, stores PNG
   └── Postgres (prod) / H2 file (dev)          background removal · classify · colour
```

| Component | Stack | Port | Docs |
|---|---|---|---|
| Web frontend | Angular 22 (zoneless), standalone components, signals, NgRx Signal Store | 4200 (dev) / 8080 (nginx) | [`web/`](web) |
| wardrobe-service | Java 25, Spring Boot 3.5 | 8081 | [`backend/README.md`](backend/README.md) |
| avatar-service | Java 25, Spring Boot 3.5 | 8082 | [`backend/README.md`](backend/README.md) |
| outfit-composer-service | Java 25, Spring Boot 3.5 | 8083 | [`backend/README.md`](backend/README.md) |
| garment-recognition-service | Python 3.11, FastAPI, Pillow (+ optional rembg) | 8000 | [`recognition-service/README.md`](recognition-service/README.md) |
| Mobile | TBD — see [`docs/MOBILE.md`](docs/MOBILE.md) (deferred, Phase 3) | – | [`docs/MOBILE.md`](docs/MOBILE.md) |

## Run the whole stack (Docker)

```bash
docker compose up --build
# web:      http://localhost:8080
# services: 8081 / 8082 / 8083, recognition: 8000, postgres: 5432
```

The Java services run with the `prod` profile (Postgres); the web container's
nginx routes `/api/*` and `/media/*` to the right service, so there is no CORS.

## Run locally without Docker

Each backend service uses a local **H2 file database** by default — no external
infra required.

```bash
# 1. recognition sidecar
cd recognition-service && python3 -m venv .venv && . .venv/bin/activate \
  && pip install -r requirements-dev.txt && uvicorn app.main:app --port 8000

# 2. backend services (separate shells)
cd backend && mvn -pl wardrobe-service spring-boot:run
              mvn -pl avatar-service spring-boot:run
              mvn -pl outfit-composer-service spring-boot:run

# 3. web (dev server proxies /api and /media to the services)
#    Angular 22 CLI needs Node >= 22.22.3 (or 24.x).
#    NgRx has no v22 yet, so install with --legacy-peer-deps.
cd web && npm install --legacy-peer-deps && npm start   # http://localhost:4200
```

## Test

```bash
cd backend && mvn test                         # Java services (7 tests)
cd recognition-service && . .venv/bin/activate && pytest   # recognition (6 tests)
cd web && npx ng test --watch=false            # Angular 22 (Vitest)
```

## What's implemented (P0 must-haves)

- ✅ Camera-scan flow (web) with background removal + suggested category/colour,
  manual correction before saving
- ✅ Automatic categorisation (top/bottom/socks/layer/shoes/accessory)
- ✅ 2D silhouette with 3 fixed body-type variants (slim/average/curvy)
- ✅ Swipe-per-category interaction with real-time client-side layer compositing
- ✅ Save & reload outfits
- ✅ Wardrobe grid with category / colour / season filters

See [`docs/SPEC.md`](docs/SPEC.md) §6 for the full requirement list and the P1/P2
backlog.

## Key design decisions (SPEC.md §8 open questions)

- **Auth:** header-based owner id (`X-Owner-Id`) with a demo default; pluggable
  for a real IdP later.
- **Object storage:** `ObjectStorage` interface with a local-filesystem impl for
  dev; swap for Azure Blob / S3 in prod.
- **Background removal:** Python/FastAPI sidecar (`rembg` when installed, offline
  colour-key fallback otherwise).
- **Mobile platform:** intentionally undecided — deferred (see `docs/MOBILE.md`).

## Branching

Git flow: `main` (stable), `develop` (integration), `feature/*`, `release/*`,
`hotfix/*`.
