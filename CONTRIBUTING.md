# Contributing to Dressd

## Scope first

[`docs/SPEC.md`](docs/SPEC.md) is the canonical statement of what Dressd is meant to
do. If a change alters scope, update the spec in the same pull request — code and
spec drifting apart is how the non-goals in the README quietly stop being true.

## Branching

git-flow: `main` (stable) · `develop` (integration) · `feature/*` · `release/*` ·
`hotfix/*`.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
`chore:`, `docs:`, `refactor:`, `test:`, `ci:`.

Write the body for someone reading `git log` in a year with no memory of the
conversation: say what changed and *why*, not just what.

## Prerequisites

| Tool   | Minimum                | Notes                          |
| ------ | ---------------------- | ------------------------------ |
| JDK    | 25                     | `java -version` → 25.x         |
| Maven  | 3.9                    | or use the reactor directly    |
| Node   | ≥ 22.22.3 or 24.x      | Angular 22 CLI enforces this   |
| Python | 3.11                   | recognition sidecar            |
| Docker | 24+ with BuildKit      | only for the compose path      |

## Running the tests

Everything CI runs, you can run locally:

```bash
# Backend — includes integration tests that apply the real Flyway migrations
cd backend && mvn verify

# Web
cd web && npm ci --legacy-peer-deps && npm test && npm run format:check

# Recognition sidecar
cd recognition-service
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements-dev.txt
python -m pytest
```

## Code style

- **Java** — standard Spring conventions. Constructor injection, no field
  injection. Keep cross-service concerns (auth, error contract, pagination) in
  `backend/common` rather than copying them per service.
- **TypeScript** — Prettier is enforced in CI. `npm run format` before pushing.
- **Comments** — explain *why*, not *what*. A comment restating the code is noise;
  a comment recording a constraint or a rejected alternative earns its place.

## Database changes

Hibernate runs with `ddl-auto: validate`, so schema changes go through Flyway:

1. Add `backend/<service>/src/main/resources/db/migration/V<n>__<description>.sql`.
2. Never edit an applied migration — Flyway checksums them.
3. Keep the SQL portable across Postgres and H2 (dev/test run H2 in PostgreSQL
   mode). Store enums as `varchar` with a check constraint, not a native enum type.
4. `mvn verify` will fail if the entities and the migrations disagree.

## Definition of done

- Compiles, and tests are green in every runtime you touched.
- A behavioural change is verified by exercising the real flow, not only by unit
  tests.
- New cross-cutting behaviour has a test that would fail without it.
- Docs updated when you changed the API ([`docs/API.md`](docs/API.md)),
  configuration ([`docs/CONFIGURATION.md`](docs/CONFIGURATION.md)) or made a
  decision worth recording ([`docs/ADR.md`](docs/ADR.md)).

## Security

Auth modes and the current posture are documented in
[`docs/SECURITY.md`](docs/SECURITY.md). Please do not open a public issue for a
vulnerability — contact the repository owner directly.
