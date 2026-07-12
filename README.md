# Dressd

Digitale garderobe-app. Scan je kleding met de camera, laat de achtergrond
automatisch verwijderen en categoriseren, en stel outfits samen door per
kledingcategorie horizontaal te swipen op een 2D poppetje.

## Documentatie

De volledige build-specificatie is leidend en staat in
[`docs/SPEC.md`](docs/SPEC.md). Scope, architectuur, domeinmodel en
requirements worden daar bijgehouden.

## Branching

Dit project volgt een [git flow](https://nvie.com/posts/a-successful-git-branching-model/)
branching-model:

- `main` — productie-releases, altijd stabiel
- `develop` — integratiebranch voor lopende ontwikkeling
- `feature/*` — nieuwe features, vertakt vanaf en gemerged naar `develop`
- `release/*` — voorbereiding van een release
- `hotfix/*` — dringende fixes vanaf `main`

## Tech stack (kort)

| Laag | Keuze |
|---|---|
| Backend | Java 21, Spring Boot, microservices |
| Web | Angular (signals, Signal Store, standalone components) |
| Mobiel | TBD |
| Database | Postgres |
| Storage | Object storage (Azure Blob / S3) |

Zie [`docs/SPEC.md`](docs/SPEC.md) voor details.
