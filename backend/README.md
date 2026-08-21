# Dressd Backend

Java 26 / Spring Boot 4.1 microservices (SPEC.md section 3). A Maven multi-module
reactor with one deployable per bounded context plus a shared `common` module.

| Module                    | Port | Responsibility                                              |
| ------------------------- | ---- | ----------------------------------------------------------- |
| `wardrobe-service`        | 8081 | Garment CRUD, scan orchestration, wardrobe filters, storage |
| `avatar-service`          | 8082 | Avatar/silhouette CRUD, fixed body-type presets             |
| `outfit-composer-service` | 8083 | Outfit CRUD (garment layers, z-index, offset, scale)        |
| `common`                  | –    | Cross-service web concerns, wired in by auto-configuration  |

The Python `garment-recognition-service` (background removal + classification) lives
one level up in `../recognition-service` and is called over REST by wardrobe-service.

## The `common` module

`common` is a Spring Boot **auto-configuration**, not a bag of classes to
component-scan. Adding it as a dependency is all a service does; the behaviour
arrives with it:

- **Owner authentication** — `OwnerIdentityFilter` resolves the caller once per
  request and controllers receive `@CurrentOwner UUID`. Two modes (`DEV` header,
  `TOKEN` signed bearer); see [`../docs/SECURITY.md`](../docs/SECURITY.md).
- **Error contract** — `GlobalExceptionHandler` maps exceptions to the shared
  `ApiError` body, including a catch-all that logs the detail and returns a generic
  message rather than leaking internals.
- **Pagination** — `PageResponse` / `PageRequests`, with the page size clamped.
- **CORS** — configured from `dressd.cors.allowed-origins`.
- **Domain enums** — `GarmentCategory`, `Season`, `BodyType`.

These used to be copy-pasted into all three services, so a fix had to be made three
times. If you find yourself adding a class to more than one service, it belongs here.

## Run

Each service uses a local **H2 file database** by default, so you can run them with
no external infrastructure:

```bash
mvn -pl wardrobe-service spring-boot:run
mvn -pl avatar-service spring-boot:run
mvn -pl outfit-composer-service spring-boot:run
```

With the `prod` profile they use **Postgres** instead (see `docker-compose.yml` at
the repo root). Note that `prod` also switches auth to `TOKEN` mode and will refuse
to start without `AUTH_SECRET` — that is deliberate.

## Test

```bash
mvn verify        # all modules, including integration tests
mvn -pl wardrobe-service test
```

Tests run against in-memory H2 with the recognition sidecar stubbed, so they need no
network or database. They apply the **real Flyway migrations**, so a broken migration
fails the build rather than a deployment.

## Schema

Flyway owns the schema; Hibernate runs `ddl-auto: validate`. Each service migrates
into its own Postgres schema. Migrations live in
`<service>/src/main/resources/db/migration/` and must stay portable across Postgres
and H2 — see [ADR-007](../docs/ADR.md#adr-007--flyway-migrations-and-a-schema-per-service)
for the specific constraints that implies, and
[`../CONTRIBUTING.md`](../CONTRIBUTING.md) for the workflow.

## Design notes

- **Auth** — an interim HMAC-signed bearer token, deny-by-default in `prod`. A real
  IdP replaces it without controllers changing
  ([ADR-002](../docs/ADR.md#adr-002--signed-token-owner-identity-header-mode-for-local-work)).
- **Storage** — cut-out PNGs go through an `ObjectStorage` interface; the shipped
  `LocalObjectStorage` serves them under `/media/**`. Scans land in a pending area
  and are promoted on confirm, which is also the authorisation check on the
  client-supplied `imageUrl`
  ([ADR-009](../docs/ADR.md#adr-009--confirm-on-write-media-lifecycle)).
- **Recognition** — wardrobe-service degrades gracefully: if the sidecar is down or
  disabled it falls back to a deterministic local stub, so the scan flow never
  hard-fails. The user confirms every scan, which is what makes a wrong guess safe.
- **Observability** — Prometheus metrics on `/actuator/prometheus`, and Micrometer
  tracing so a scan's trace id carries into the recognition sidecar.
