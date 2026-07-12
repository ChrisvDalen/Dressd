# Dressd Backend

Java 21 / Spring Boot microservices (SPEC.md section 3). A Maven multi-module
reactor with one deployable per bounded context plus a small shared `common`
module.

| Module | Port | Responsibility |
|---|---|---|
| `wardrobe-service` | 8081 | Garment CRUD, scan orchestration, wardrobe filters |
| `avatar-service` | 8082 | Avatar/silhouette CRUD, fixed body-type presets |
| `outfit-composer-service` | 8083 | Outfit CRUD (garment layers, z-index, offset, scale) |
| `common` | – | Shared domain enums (`GarmentCategory`, `Season`, `BodyType`) and the `ApiError` body |

The Python `garment-recognition-service` (background removal + classification)
lives one level up in `../recognition-service` and is called over REST by the
wardrobe-service.

## Run

Each service uses a local **H2 file database** by default, so you can run them
with no external infrastructure:

```bash
mvn -pl wardrobe-service spring-boot:run
mvn -pl avatar-service spring-boot:run
mvn -pl outfit-composer-service spring-boot:run
```

With the `prod` profile they use **Postgres** instead (see `docker-compose.yml`
at the repo root, which wires everything together).

## Test

```bash
mvn test          # all modules
mvn -pl wardrobe-service test
```

Tests run against in-memory H2 with the recognition sidecar stubbed, so they
need no network or database.

## Design notes

- **Auth (SPEC.md open question):** v1 identifies the owner via the
  `X-Owner-Id` header, defaulting to a fixed demo owner when absent. A real IdP
  can populate this header at a gateway without touching the services.
- **Storage (SPEC.md open question):** the wardrobe-service writes cut-out PNGs
  through an `ObjectStorage` interface. The shipped `LocalObjectStorage`
  implementation serves them under `/media/**`; swap in an Azure Blob / S3
  implementation for production.
- **Recognition:** the wardrobe-service degrades gracefully — if the sidecar is
  down or disabled it falls back to a deterministic local stub so the scan flow
  never hard-fails.
