# Dressd — Build Spec

> Canonieke specificatie voor Dressd. Dit document is leidend voor scope,
> architectuur en requirements. Wijzigingen aan de scope lopen via een update
> van dit bestand.

## 1. Overzicht

Dressd is een digitale garderobe-app. Gebruikers scannen kledingstukken met de
camera, waarna de achtergrond automatisch wordt verwijderd en het item wordt
gecategoriseerd. Op een 2D poppetje kan de gebruiker per kledingcategorie
(top / bottom / sokken / laagje) horizontaal swipen om items te previewen en zo
outfits samen te stellen — real-time, zonder zware AI-rendering.

**Platform v1**: web (Angular) én mobiel, met gedeelde backend.

## 2. Doel & scope

**Kernprobleem**: mensen weten niet meer wat ze hebben en combineren steeds
dezelfde items, terwijl fysiek passen omslachtig is.

**V1 doel**: kleding scannen → automatisch verwerkt tot digitaal item →
outfit samenstellen via swipe-interactie op een 2D poppetje.

### Non-goals (v1)
- Geen 3D/fotorealistische try-on (bewust gekozen voor simpel 2D-overlay-model)
- Geen social features (delen, volgen, community-outfits)
- Geen automatische stijladviezen/AI-aanbevelingen
- Geen maatvoering/pasvorm-simulatie (kleding "past" altijd op het silhouet)
- Geen wasseizoen-tracking of slijtage-status

## 3. Architectuur

### High-level

```
┌─────────────────┐     ┌─────────────────┐
│  Angular (web)   │     │  Mobiel (native) │
└────────┬─────────┘     └────────┬─────────┘
         │                        │
         └──────────┬─────────────┘
                     │ REST/JSON
         ┌───────────▼────────────┐
         │   API Gateway (optioneel│
         │   in v1: direct naar    │
         │   services)             │
         └───────────┬────────────┘
                     │
   ┌─────────────────┼─────────────────┬──────────────────┐
   │                 │                 │                  │
┌──▼──────────┐ ┌────▼──────────┐ ┌────▼──────────┐ ┌─────▼─────────┐
│ wardrobe-   │ │ garment-      │ │ avatar-        │ │ outfit-        │
│ service     │ │ recognition-  │ │ service        │ │ composer-      │
│             │ │ service       │ │                │ │ service        │
└──┬──────────┘ └────┬──────────┘ └────┬───────────┘ └─────┬──────────┘
   │                 │                 │                    │
   └────────┬────────┴─────────────────┴────────────────────┘
            │
      ┌─────▼─────┐        ┌──────────────┐
      │  Postgres  │        │ Object storage│
      │ (metadata) │        │ (PNG's, blob) │
      └────────────┘        └──────────────┘
```

### Services (Spring Boot 4.1, Java 26)

**wardrobe-service**
- CRUD op `Garment` entiteiten
- Orkestreert de scan-flow (upload → recognition → opslag)
- Filters/zoeken op categorie, kleur, seizoen

**garment-recognition-service**
- Achtergrond verwijderen bij upload (bv. `rembg` als sidecar-proces, of lichte edge-detection library)
- Kledingcategorie classificeren (top/bottom/sokken/laagje/schoenen)
- Kleur-extractie (dominant color uit de uitgesneden afbeelding)
- Kan als losse Python/FastAPI-sidecar draaien, aangeroepen via REST vanuit Spring Boot

**avatar-service**
- CRUD op `Avatar`/silhouet-varianten (vaste set lichaamstypes + instelbare proporties)
- Geen ML — puur statische data + proporties

**outfit-composer-service**
- CRUD op `Outfit` (welke garment-layers, in welke volgorde, met welke offset/scale)
- Bewaart alleen posities — geen gerenderde afbeelding (rendering gebeurt client-side)

### Frontend (Angular, web)

- `WardrobeStore` (NgRx Signal Store) — kledingcollectie + filters
- `ScanFlowComponent` — camera-integratie via `getUserMedia`, upload-flow met voortgangsindicatie
- `AvatarCanvasComponent` — HTML5 canvas/SVG, rendert silhouet + kledinglagen
- `CategorySwipeComponent` — horizontale swipe/carousel per kledingcategorie, update de laag op het canvas bij swipe
- Alle laag-compositie gebeurt client-side (geen backend-rendering nodig)

### Mobiel

- Gedeelde REST API met web
- Native camera-integratie voor scan-flow
- Zelfde canvas/swipe-interactiepatroon (native equivalent, bv. Compose/SwiftUI canvas of een cross-platform canvas-library als de stack cross-platform is)
- **Open vraag**: native (Kotlin/Swift) of cross-platform (bv. Flutter/React Native)? Zie sectie 8.

## 4. Domeinmodel

```java
Garment
- id: UUID
- ownerId: UUID
- category: enum (TOP, BOTTOM, SOCKS, LAYER, SHOES, ACCESSORY)
- imageUrl: String          // uitgesneden PNG, transparante achtergrond
- colorTag: String          // dominante kleur, bv. hex of naam
- pattern: String?          // optioneel: solid/striped/etc
- season: enum?             // optioneel: SUMMER/WINTER/ALL
- anchorPoints: {
    shoulderY: float,
    waistY: float,
    hemY: float,
    widthScale: float
  }
- createdAt, updatedAt

Avatar
- id: UUID
- ownerId: UUID
- bodyType: enum (vaste set silhouetten) | CUSTOM
- proportions: {
    height: float,
    shoulderWidth: float,
    hipWidth: float
  }

Outfit
- id: UUID
- ownerId: UUID
- avatarId: UUID
- name: String?
- garmentLayers: [
    { garmentId: UUID, category: enum, zIndex: int, offsetX: float, offsetY: float, scale: float }
  ]
- createdAt, updatedAt
```

## 5. Kernflows

### Flow A — Kledingstuk scannen
1. Gebruiker maakt foto via camera (web: `getUserMedia`, mobiel: native camera)
2. Foto naar `garment-recognition-service`
3. Achtergrond verwijderen → transparante PNG
4. Categorie + dominante kleur bepalen
5. Resultaat (PNG + metadata) opslaan via `wardrobe-service`
6. Gebruiker kan categorie/kleur handmatig corrigeren vóór definitief opslaan

### Flow B — Outfit samenstellen via swipe
1. Gebruiker opent outfit-builder met 2D poppetje in beeld
2. Per categorie-rij (top/bottom/sokken/laagje) wordt het huidige geselecteerde item getoond
3. Horizontaal swipen binnen een categorie-rij wisselt naar het volgende/vorige item uit de garderobe in die categorie
4. Bij elke swipe wordt de laag op het canvas direct bijgewerkt (client-side, geen netwerk-call nodig zodra garderobe-data lokaal gecached is)
5. Gebruiker kan de samengestelde outfit opslaan → `outfit-composer-service`

### Flow C — Garderobe beheren
1. Grid-overzicht van alle gescande items, gefilterd op categorie/kleur/seizoen
2. Item verwijderen of metadata aanpassen
3. Zoekfunctie op kleur/categorie

## 6. Requirements

### Must-have (P0)
- [ ] Camera-scan flow (web + mobiel) met achtergrond-verwijdering
- [ ] Automatische categorisatie (top/bottom/sokken/laagje/schoenen), met handmatige correctie-optie
- [ ] 2D silhouet/avatar met minimaal 2-3 vaste bodyType-varianten
- [ ] Swipe-per-categorie interactie op het canvas, real-time laag-update
- [ ] Outfit opslaan en later terug opvragen
- [ ] Garderobe-overzicht (grid) met basisfilters

### Nice-to-have (P1)
- [ ] Seizoen-tag en filter
- [ ] Patroon-detectie (solid/striped/etc)
- [ ] Meerdere avatars per gebruiker (bv. instelbare proporties)
- [ ] Outfit-geschiedenis ("laatst gedragen")

### Future considerations (P2)
- Fotorealistische try-on (diffusion-based model) als upgrade-pad — domeinmodel houdt hier al rekening mee door garment-images los te bewaren van rendering-logica
- Stijladvies/aanbevelingen op basis van garderobe-samenstelling
- Delen van outfits

## 7. Acceptatiecriteria (voorbeelden)

**Scannen**
- Given een gebruiker maakt een foto van een kledingstuk
- When de foto wordt geüpload
- Then toont de app binnen een paar seconden een uitgesneden preview met voorgestelde categorie en kleur, die de gebruiker kan bevestigen of aanpassen

**Swipen**
- Given een gebruiker heeft minimaal 2 items in de categorie "top"
- When de gebruiker horizontaal swipet op de top-rij
- Then wisselt het getoonde item op het poppetje direct, zonder zichtbare laadvertraging

**Lege staat**
- Given een gebruiker heeft nog geen items in een categorie
- When die categorie-rij in beeld is
- Then toont de app een duidelijke lege-staat met call-to-action om iets te scannen

## 8. Open vragen

- Mobiel: native (Kotlin/Swift, apart per platform) of cross-platform? Bepaalt of canvas-rendering herbruikbaar is met de Angular-implementatie
- Waar draait de achtergrond-verwijdering: losse Python/FastAPI-sidecar of een Java-native library binnen `garment-recognition-service`?
- Object storage: Azure Blob (sluit aan bij bestaande AKS-ervaring) of S3-compatible?
- Auth: eigen user-service of een bestaande identity-provider (bv. Auth0/Azure AD B2C)?

## 9. Tech stack samenvatting

| Laag | Keuze |
|---|---|
| Backend | Java 26, Spring Boot 4.1, microservices |
| Web frontend | Angular 22 (zoneless, signals, Signal Store, standalone components) |
| Mobiel | TBD (zie open vragen) |
| Database | Postgres (metadata) |
| Storage | Object storage voor PNG's (Azure Blob / S3) |
| Image processing | rembg of vergelijkbare lichte segmentatie-library |
| Rendering | Client-side canvas/SVG, geen server-side GPU-rendering |

## 10. Fasering (suggestie)

**Fase 1**: wardrobe-service + scan-flow (web) + garderobe-grid
**Fase 2**: avatar-service + outfit-composer-service + swipe-canvas (web)
**Fase 3**: mobiele client (gedeelde API)
**Fase 4**: P1-features (seizoen, patroon, meerdere avatars)
