# Dressd — Production Readiness Assessment & Improvements

Branch: `openhands/production-readiness` (base `main` @ `9a8cf4d`)
Date: 2026-09-27

## Executive summary

The repository was already in unusually good shape: clean microservice layout,
working CI for all three stacks (Java 26 / Angular 22 / Python 3.14), Flyway
migrations applied in tests, non-root containers, a documented security model
(`docs/SECURITY.md`), fail-closed production authentication, and a mature
`docker-compose` orchestration. No critical or high-severity defects were found.

This mission left it demonstrably better in four ways:

1. **LCM** — all three stacks moved to current supported releases with the
   Angular peer-dependency workaround (`--legacy-peer-deps`) removed entirely.
2. **Tests** — 34 new meaningful tests (backend 50→74, web 28→38), focused on
   tenancy isolation, filter logic, and documented API boundaries. JaCoCo
   coverage reporting added to the backend build.
3. **Security** — HTTP security headers added to the nginx front door (verified
   live), graceful shutdown enabled on all services.
4. **Observability of quality** — CI now reports and archives per-module test
   coverage.

All changes are committed on a dedicated branch, unpushed/unmerged, ready for
review. Every validation command listed in *Validation* passes.

---

## 1. Production-readiness assessment (initial)

| # | Severity | Problem | Impact | Resolution |
|---|----------|---------|--------|------------|
| 1 | High | No HTTP security headers on the web tier | Clickjacking / MIME sniffing / referrer leakage on the public surface | **Fixed** — headers added to `web/nginx.conf` (item below), verified with live nginx |
| 2 | Medium | Services stop in-flight requests on SIGTERM (default abrupt shutdown) | Dropped requests during deploys / `compose down` | **Fixed** — `server.shutdown: graceful` on all three services |
| 3 | Medium | `--legacy-peer-deps` required for `npm ci` | Fragile, non-standard install; masks future peer conflicts | **Fixed** — @ngrx/signals moved to the Angular-22 line; flag removed from 6 files |
| 4 | Medium | No test-coverage visibility for the backend | Cannot tell whether risky code is actually exercised | **Fixed** — JaCoCo report + CI summary |
| 5 | Medium | Owner-isolation (tenancy) and boundary behaviour untested on avatar/outfit mutations | The security property the whole service exists for had no regression protection | **Fixed** — new integration tests |
| 6 | Low | Spring Boot patch version stale (4.1.0), minor Python bumps available | Missed patch security/maintenance fixes | **Fixed** — 4.1.1; uvicorn 0.54.0, numpy 2.5.3 |
| 7 | Low | Dependabot cannot see `backend/service.Dockerfile` (non-standard name) | Base-image patches for the Java images need a manual bump | Documented; acceptable (already noted in `dependabot.yml`) |
| 8 | Low | `/media/**` images are public URLs (no auth on stored photos) | Anyone with a URL can fetch a garment photo | Documented — needs a product decision (signed URLs vs per-user token scope); see *Remaining risks* |
| 9 | Low | No rate limiting on upload/scan endpoints | Brute-force / resource abuse possible | Documented — belongs at the gateway; see *Remaining risks* |
| 10 | Low | Per-module coverage can't attribute `common` web-infra classes to the service tests that exercise them | Coverage numbers look worse than reality for the shared module | Documented — `report-aggregate` candidate, not worth the build machinery yet |

No critical findings. The auth implementation (`OwnerTokenService`) was reviewed
in depth and is sound: fixed HMAC-SHA256 algorithm (no algorithm-confusion),
MAC over the version+payload prefix, constant-time comparison, minimum 32-char
secret, expiry enforced, TOKEN mode refuses to start without a secret
(fail-closed). `OwnerIdentityFilter` scopes every non-public path and returns
404 (not 403) for foreign owner resources so existence is not disclosed.
Filesystem access is normalised and confined to the storage root
(`LocalObjectStorage.normalizeKey`/`resolve`). The recognition sidecar streams
uploads with an enforced 15 MB cap and allowlists content types before
decoding.

---

## 2. Changes implemented

| Commit | Description |
|--------|-------------|
| `61bf799` | LCM: Angular → 22.2.0 (all packages) + @ngrx/signals 21.1.1 → 22.0.1, regenerated lockfile, removed `--legacy-peer-deps` from all 6 references; Spring Boot 4.1.0 → 4.1.1; uvicorn 0.52.4 → 0.54.0, numpy 2.5.2 → 2.5.3 |
| `fd884da` | 24 new backend tests: owner isolation (avatar read/update/delete, list; outfit read/delete), avatar default proportions & explicit-override, outfit 32-layer cap / update / delete, `GarmentSpecifications` real-SQL filter tests, `PageRequests` clamp, `BodyTypeCatalog` |
| `f66e70c` | 10 new web specs: `AvatarService` and `OutfitService` HTTP contracts (URL, method, body, query-parameter mapping) |
| `460ecff` | nginx security headers (nosniff, DENY, referrer policy, minimal CSP) verified live; `server.shutdown: graceful` on all three services |
| `4994953` | JaCoCo coverage reporting (report-only) + CI coverage summary and artifact upload |

---

## 3. Lifecycle management (LCM)

| Stack | Component | Before | After | Notes |
|-------|-----------|--------|-------|-------|
| Web | @angular/* (all 22 packages) | 22.1.x line | 22.2.0 | Minor; lockfile regenerated |
| Web | @ngrx/signals | 21.1.1 (peer conflict → `--legacy-peer-deps`) | 22.0.1 | Resolves the conflict properly; flag removed everywhere |
| Web | TypeScript / vitest / esbuild | ~6.0.3 / 4.1.11 / 0.28.2 | unchanged | Current |
| Backend | Spring Boot | 4.1.0 | 4.1.1 | Patch; parent pom BOMs the Spring framework & plugins |
| Backend | Java / Maven | 26 / 3.9.11 | unchanged | Current; Temurin 26 base images |
| Python | fastapi / pillow / python-multipart | 0.141.1 / 12.3.0 / 0.0.32 | unchanged | Current |
| Python | uvicorn[standard] | 0.52.4 | 0.54.0 | Minor |
| Python | numpy | 2.5.2 | 2.5.3 | Patch |
| Docker | node:24-alpine, nginxinc/nginx-unprivileged:1.29-alpine, python:3.14-slim, maven/eclipse-temurin:26 | — | unchanged | Current; Dependabot covers the two standard Dockerfiles |
| CI | actions/* | checkout/setup-java/setup-node/setup-python/buildx v7/v5/v6/v3 | unchanged | Current |

**Migrations performed:** none required beyond the version moves — no
deprecated APIs surfaced in either build, no configuration keys changed.

**Remaining lifecycle risks:**
- `backend/service.Dockerfile` base images are not visible to Dependabot
  (non-standard file name); bump manually alongside Temurin releases (already
  documented in `dependabot.yml`).
- The web stack's `npm ci` must run on Node ≥ 24 (Angular 22 requirement); CI
  is pinned to Node 24. Local developers on older Node should use an nvm/.nvmrc
  discipline — worth adding a `.nvmrc` (small, not done to keep the diff tight).

---

## 4. Testing

**Before:** backend 50 tests, web 28, recognition 10 — all green at baseline.
Backend tests were happy-path-flow oriented; tenancy isolation on avatar/outfit
mutations and the documented boundary behaviour had no coverage. Web covered
`garment.service` and the store well, but the two other HTTP services were
untested. No coverage tooling existed anywhere.

**Added (34 tests):**

- *Backend — security/tenancy (integration):* another owner gets 404 on avatar
  read/update/delete and an empty list; outfit read/delete are owner-scoped.
  These pin the exact behaviour that makes multi-tenancy safe (404-not-403, no
  existence leakage).
- *Backend — business rules:* avatar creation applies body-type default
  proportions; explicit proportions override the preset; outfit creation is
  rejected at the 32-layer cap (400 + message); outfit update replaces the
  whole layer set, renumbered by z-index.
- *Backend — persistence logic (real SQL via H2):* `GarmentSpecifications`
  `ownedBy` isolation; the "null means unfiltered" contract for category and
  season; case-insensitive partial colour matching (including the blank-string
  sentinel); AND composition of predicates.
- *Backend — pure unit:* `PageRequests` page-size clamp (the bound that stops
  unbounded result sets); `BodyTypeCatalog` presets (every body type has one,
  they are distinct, returned values are by-value).
- *Web — HTTP contracts:* `AvatarService` (body-types, list, create, update)
  and `OutfitService` (list default vs page/size mapping, get, create, update,
  remove) using the same `HttpTestingController` style as the existing specs.

**Coverage achieved (JaCoCo, instruction level):**

| Module | Instruction | Branch |
|--------|-------------|--------|
| avatar-service | **95.7%** | 66.7% (6 branches) |
| outfit-composer-service | **92.6%** | 75.0% (16 branches) |
| wardrobe-service | **79.8%** | 63.9% (72 branches) |
| common | 35.3% (per-module) | 52.5% |

The `common` figure is a per-module attribution artifact, not real exposure:
`OwnerIdentityFilter`, `GlobalExceptionHandler` and the auto-configuration are
exercised heavily by the service integration tests (auth tests, error-contract
tests, flow tests), but JaCoCo counts them in the module that declares the
class. The remaining genuinely-uncovered `common` code is the DEV-mode
fallbacks and the `OwnerTokenTool` CLI. A cross-module number would require
`jacoco:report-aggregate` (an extra aggregator module); the per-module numbers
above plus the service coverage are a faithful picture. Web has no coverage
tooling (vitest can report, but the 28→38 tests target the previously untested
services; the store and component were already covered). Recognition keeps its
10 pytest cases covering the reject path, size-limit streaming, and the
classification heuristics.

**Important scenarios now protected:** tenant isolation on both avatar and
outfit APIs; the documented 32-layer cap; the "null = unfiltered" query
contract; page-size clamping; body-type preset integrity; the full avatar and
outfit HTTP surface.

---

## 5. Security

**Findings (initial review):**

- **High — no HTTP security headers** (clickjacking/MIME-sniffing/referrer) →
  **resolved**: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
  `Referrer-Policy: no-referrer`, `Content-Security-Policy: frame-ancestors
  'none'` on all nginx responses (including the two `add_header`-defining
  locations, which don't inherit server-level headers). Verified against a live
  nginx 1.26: headers present on `index.html`, the SPA fallback, and asset
  responses.
- **Medium — abrupt shutdown** → **resolved**: graceful shutdown on all three
  services.
- **Medium — no coverage visibility** → **resolved**: JaCoCo (report-only).
- **Low — public `/media/**` URLs** → **documented, not fixed**: garment photos
  are fetchable by anyone holding the URL (owner id + random UUID name).
  Fixing needs a product decision: signed/temporary URLs, or scoping media
  delivery through the owner token. Deliberately not changed because it alters
  API behaviour.
- **Low — no rate limiting** → **documented**: belongs at the API-gateway/edge
  layer; there is no in-repo place for it without adding infrastructure.

**Verified clean (no changes needed):**

- Auth: constant-time MAC comparison, single fixed HMAC algorithm, version
  prefix covered by the MAC, expiry + min-secret enforcement, fail-closed
  TOKEN mode, 404 (not 403) on foreign-owner access so existence isn't
  leaked, CORS preflight correctly exempt.
- CORS: origin-allowlist only (no `*`), no `allowCredentials`+wildcard, default
  is localhost.
- Filesystem: `normalizeKey`/`resolve` confine writes under the storage root
  (path-traversal resistant); uploads streamed with hard 15 MB cap (Java and
  Python sides both enforce).
- Error handling: shared `GlobalExceptionHandler` — 500s never leak stack
  details or internal messages; validation errors structured.
- Secrets: none committed; `.env` gitignored with a well-documented
  `.env.example`; compose passes through defaults that are explicitly labelled
  dev-only.
- Containers: all run as non-root dedicated UIDs (10001/10002), minimal base
  images, healthchecks everywhere, `--no-cache-dir` pip, `npm ci`.
- CI: `permissions: contents: read` (least privilege), pinned action versions,
  concurrency + cancellation.
- Logging: no secrets logged; structured (ECS) logging in the prod profile;
  correlation/trace ids in log pattern.

---

## 6. Architecture & maintainability

Reviewed all modules; the codebase is well-factored and no refactor was
justified (per mission guidance: preserve behaviour, incremental changes only).

- **Good:** clean three-service split with a `common` module delivering
  auth/error-contract/CORS via Spring Boot auto-configuration (add the
  dependency, inherit the behaviour); domain logic in services, controllers
  thin; owner identity resolved once per request in a filter and injected via
  `@CurrentOwner` argument resolver — no ownership checks scattered in
  controllers; Flyway-owned migrations run in tests; ADRs documented
  (`docs/ADR.md`).
- **Observed, not changed:** `outfit-builder.component.ts` (272 lines) is the
  largest frontend unit but has a clear single responsibility (composer state
  + gesture handling) and a tested store; the recognition service is ~400
  lines total. Nothing approaches "oversized".
- **Observed, not changed:** `common`'s per-module coverage number looks poor
  for the reason described in *Testing*; a `report-aggregate` module would
  fix the optics if a single number is ever wanted.

No circular dependencies, no persistence leakage into controllers, no dead
code found, no deprecated patterns (the codebase is on the current Spring Boot
4.x API surface).

---

## 7. CI/CD & repository quality

**Already strong (unchanged):** matrix backend build on Java 26; web
`npm ci` + format + test + build; recognition pytest; **container image build
validation in CI** (catchs Dockerfile drift, with GHA layer caching); compose
validation; maven/npm/pip caches; dependabot for maven, npm, pip,
github-actions and the two standard Dockerfiles; test-report artifacts;
`contents: read` token.

**Added:**

- **Coverage:** `Print coverage summary` step and JaCoCo reports in the
  backend artifact.
- **Nothing else** — the mission guidance says not to add tooling without
  clear value, and this repo's CI is already beyond most production repos.

**Documentation:** README, CONTRIBUTING, TROUBLESHOOTING, docs/ (SPEC, API,
SECURITY, CONFIGURATION, ADR, GLOSSARY, MOBILE) are accurate and current; the
`--legacy-peer-deps` removal required and received updates in all six files
that mentioned it. `.gitignore` is complete. No README drift was found.

**Not added, on purpose:** SAST (e.g. Semgrep) — would need tuning to avoid
noise on a small codebase; SBOM generation — `mvn dependency:go-offline` +
lockfiles already give a dependency manifest and no registry is configured to
consume SBOMs; branch protection / PR checks — these are GitHub-side settings,
not repo files.

---

## 8. Validation (final state)

All commands run on this branch after all commits:

| Command | Result |
|---------|--------|
| `cd backend && mvn -B --no-transfer-progress verify` (JDK 26.0.2.1, Maven 3.9.11) | **BUILD SUCCESS** — 74 tests, 0 failures (common 19, wardrobe 35, avatar 12, outfit 8); JaCoCo reports generated |
| `cd web && npm ci && npm test` (Node 24 / npm 11.16) | **38 passed (6 files)** |
| `cd web && npm run format:check` | **All matched files use Prettier code style** |
| `cd web && npm run build` | **Production build OK** (dist/web/browser) |
| `cd recognition-service && .venv/bin/python -m pytest -q` | **10 passed** |
| `docker compose config -q` | Valid (CI also builds all four images) |
| nginx 1.26 live check of `web/nginx.conf` | Syntax OK; security headers verified on index/SPA/asset responses |

Total automated tests: **122** (74 + 38 + 10), all green. No pre-existing
failures were found at baseline; no test was deleted or weakened anywhere in
this mission.

---

## 9. Remaining risks (need a human decision)

1. **Public `/media/**` URLs** — stored garment photos are publicly fetchable
   if the URL is known (owner UUID + random file name). Decide: signed URLs,
   or require the owner token for media. *Behaviour-changing, so left for the
   owner.*
2. **Rate limiting / abuse control** — no in-app limiting on upload/scan.
   Decide where it lives (edge/gateway vs in-app).
3. **Compose stack runs `AUTH_MODE=DEV`** by default (documented, dev-only).
   A real deployment must leave it unset + set `AUTH_SECRET` — currently
   enforced by fail-closed startup, but worth a deploy checklist.
4. **`.nvmrc` for the web stack** (Node 24) — trivial DX win, not added to
   keep the diff focused.
5. **JaCoCo `report-aggregate`** if a single cross-module coverage number is
   ever wanted.
6. **Full CSP with hashed asset sources** — the current `frame-ancestors 'none'`
   CSP is the safe minimum; a strict CSP needs build-time hashing.

## 10. Recommended next actions (prioritized)

1. Decide and implement the media-URL auth model (risk #1).
2. Add a `.nvmrc` (24) + `engines` field for the web stack.
3. Add Dependabot coverage for `service.Dockerfile` base images (rename it, or
   accept manual bumps as documented).
4. Consider `jacoco:check` with a floor (e.g. 80% instruction on the three
   service modules) now that the baseline is measured.
5. Add a Semgrep (or similar) SAST job once the codebase has a couple more
   contributors, to keep findings in the PR review flow.
6. Deploy-checklist doc: `AUTH_MODE`/`AUTH_SECRET`/`CORS_ORIGINS`/`TRACING_SAMPLE_RATE`
   for the prod profile.
