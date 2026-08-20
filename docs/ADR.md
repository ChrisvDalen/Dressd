# Architecture Decision Records

Condensed ADRs for the decisions [`SPEC.md`](SPEC.md) left open, plus the ones made
since.

---

## ADR-001 — Client-side compositing, no server render

**Status:** Accepted

**Context:** Outfit preview must feel instant, and the server should stay cheap.

**Decision:** The backend stores garment images and layer *positions*
(z-index/offset/scale) only; the Angular SVG canvas composites layers in the
browser.

**Consequences:** No GPU on the server. Swipes need no network once the wardrobe is
cached. Photorealistic try-on remains a future, additive upgrade because the domain
model already separates garment images from rendering logic.

---

## ADR-002 — Signed-token owner identity, header mode for local work

**Status:** Accepted · supersedes the original header-only decision

**Context:** v1 identified the caller purely by an `X-Owner-Id` header with a demo
default. That is convenient locally, but as a deployed posture it means anyone can
read or delete anyone's wardrobe by guessing a UUID — and because the header was
trusted unconditionally, there was no configuration that made it safe.

**Decision:** Two modes, in `backend/common`:

- `DEV` trusts `X-Owner-Id` and falls back to a demo owner. Local default.
- `TOKEN` requires `Authorization: Bearer <token>`, an HMAC-SHA256 signed token
  carrying the owner id and an expiry, and **ignores `X-Owner-Id` entirely** so the
  header cannot be used to bypass the very thing the token proves.

The `prod` Spring profile defaults to `TOKEN` and the services refuse to start in
that mode without a secret of at least 32 characters. A deployment that forgets to
configure auth fails closed rather than serving unauthenticated traffic.

**Why a bespoke token rather than JWT:** a single fixed algorithm removes the
algorithm-confusion class of bug that generic JWT parsing invites, and it adds no
dependency. This is still a placeholder: a gateway fronting a real IdP can mint the
same token, or the services can be switched to validating the IdP's JWTs, without
controllers changing — they only ever see `@CurrentOwner UUID`.

**Consequences:** Local development is unchanged. Production is deny-by-default.
The token has no revocation list, which is acceptable only because it is a
stepping stone to a real IdP, not a destination.

---

## ADR-003 — Pluggable object storage, local FS in dev

**Status:** Accepted

**Decision:** Cut-out PNGs go through an `ObjectStorage` interface; the shipped
`LocalObjectStorage` serves them under `/media/**`.

**Consequences:** Runs with zero cloud credentials. Swap for Azure Blob / S3 in
production. Every key operation rejects path traversal, since keys are partly
derived from client input.

---

## ADR-004 — Recognition as a Python sidecar with graceful fallback

**Status:** Accepted

**Decision:** Background removal and classification live in a FastAPI service;
wardrobe-service calls it over REST and falls back to a deterministic stub when it
is unavailable.

**Consequences:** The scan flow never hard-fails. The ML surface evolves
independently of the JVM services. The fallback produces a *plausible but wrong*
category, which is acceptable because the user confirms every scan before it is
saved.

---

## ADR-005 — Java 25 instead of the requested Java 26

**Status:** Accepted

**Context:** Java 26 is not yet GA.

**Decision:** Target Java 25 with Spring Boot 3.5.16.

**Consequences:** A one-line bump (`java.version`, base images) when 26 ships.

---

## ADR-006 — Angular 22 zoneless + Vitest

**Status:** Accepted

**Decision:** Adopt Angular 22's zoneless change detection and the Vitest-based
unit-test runner.

**Consequences:** Signal-driven components get change detection for free; Karma is
retired; `@ngrx/signals` is pinned to 21.1.1 (no v22 yet) via legacy peer deps.
Component tests must settle async work themselves — zoneless Angular does not track
plain promises, so `fixture.whenStable()` is not sufficient after an async
`ngOnInit`.

---

## ADR-007 — Flyway migrations and a schema per service

**Status:** Accepted

**Context:** All three services ran `ddl-auto: update` against one shared `dressd`
database. Hibernate was therefore free to mutate the production schema on every
boot, nothing could be renamed or dropped safely, and the README's claim that each
service owned its own schema was not actually true.

**Decision:**

- Flyway owns the schema. Hibernate runs `ddl-auto: validate`, so a mapping that
  drifts from the migrations fails at startup instead of silently altering tables.
- Each service migrates into its own Postgres schema (`wardrobe`, `avatar`,
  `outfit`), so a shared instance does not make the services jointly migratable.
- Integration tests run the real migrations, so a broken one fails the build.

**Portability:** one migration set has to work on Postgres (prod) and H2 in
PostgreSQL mode (dev/test). That means:

- Enum columns are `varchar` with a check constraint. Hibernate would otherwise
  emit a *native* `enum` type on H2, so the entities carry
  `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- Embedded fields have explicit `@Column(name = ...)`, so the schema reads
  `hem_y` rather than Hibernate's default `hemy`.
- H2 URLs set `DATABASE_TO_LOWER=TRUE`. Flyway quotes identifiers, so it creates a
  lowercase `wardrobe` schema; without this H2 would upper-case Hibernate's
  unquoted lookups and validation would not find the tables.

**Consequences:** Schema changes are now a reviewable artefact. Migrations may never
be edited once applied.

---

## ADR-008 — No cross-service referential integrity

**Status:** Accepted

**Context:** An `OUTFIT_LAYER.garmentId` points at a garment owned by a different
service. Deleting a garment leaves that reference dangling.

**Decision:** Keep the reference soft. No foreign key, and no synchronous call from
outfit-composer to wardrobe-service to validate garment ids on save.

**Rationale:** A cross-service read on the write path would couple outfit saves to
wardrobe-service's availability, in exchange for a guarantee it still could not
provide — the garment can be deleted a millisecond later regardless.

**How it is handled instead:** the client resolves layers against its cached
wardrobe and reports how many no longer resolve ("2 items no longer in your
wardrobe") rather than silently rendering a gap. Layer *shape* is still validated
server-side (bounded count, no duplicate garments, scale and offset ranges) because
that needs no cross-service knowledge.

**Consequences:** Dangling layer rows accumulate. Closing that properly means an
outbox and a `garment.deleted` event that outfit-composer consumes to prune — worth
doing when there is an event bus, not worth inventing one for.

---

## ADR-009 — Confirm-on-write media lifecycle

**Status:** Accepted

**Context:** `POST /api/scan` wrote the cut-out to `garments/<owner>/` immediately,
before the user confirmed anything, and `imageUrl` was then accepted from the client
verbatim on create. Two problems: every abandoned scan leaked a file forever, and a
caller could attach any URL — including another owner's image — to their garment.
Deleting a garment also left its image behind.

**Decision:** Scans land in `pending/<owner>/` and are *promoted* to
`garments/<owner>/` when the user confirms. Promotion validates that the URL is one
of the caller's own scans, which makes the authorisation check and the lifecycle
transition the same operation — they cannot drift apart. Deleting a garment deletes
its image; a scheduled sweep reclaims pending files older than the TTL.

**Consequences:** One more move on the confirm path. `imageUrl` is no longer a
free-text field. Storage growth is bounded by confirmed garments plus at most one
TTL window of abandoned scans.

---

## ADR-010 — Paginated collections behind a shared envelope

**Status:** Accepted

**Context:** `GET /api/garments` and `GET /api/outfits` returned unbounded arrays.

**Decision:** Both return a `PageResponse` envelope with a page size clamped to 200.
Retrofitting pagination later would have been a breaking change for every client, so
it goes in now while there is one.

**Consequences:** Clients read `.content`. The outfit builder's cache walks pages to
stay warm, capped so a paging bug cannot become an infinite request loop, and tells
the user when it stopped early. `GET /api/avatars` stays unpaged because the
collection is bounded by the body-type catalog.
