# Glossary

| Term              | Definition                                                                                          |
| ----------------- | --------------------------------------------------------------------------------------------------- |
| **Garment**       | One scanned wardrobe item (cut-out PNG + metadata + anchor points)                                   |
| **Anchor points** | Normalised (0..1) vertical attach points plus a width scale, used to place a garment on the silhouette |
| **Avatar**        | A 2D silhouette defined by a body type + proportions (no ML)                                        |
| **Body type**     | One of `SLIM`, `AVERAGE`, `CURVY`, `CUSTOM`; each has a preset                                       |
| **Outfit**        | A named set of garment *layers* posed on an avatar (positions only, no rendered image)               |
| **Layer**         | `{garmentId, category, zIndex, offsetX, offsetY, scale}`                                             |
| **Colour-key**    | Offline background removal by keying out the corner-sampled colour                                   |
| **Owner**         | The tenant a resource belongs to; resolved per request and injected as `@CurrentOwner`                |
| **Pending scan**  | A cut-out written to `pending/<owner>/` that the user has not confirmed yet, and which the sweep reclaims after the TTL |
| **Promotion**     | Moving a confirmed scan from `pending/` to `garments/`; doubles as the authorisation check on `imageUrl` |
| **DEV / TOKEN**   | The two auth modes: header-trusting (local) and signed-bearer-token (deployments)                     |
| **Page envelope** | The `PageResponse` wrapper (`content`, `page`, `size`, `totalElements`, …) all paged endpoints return  |
| **Signal Store**  | NgRx signal-based state container holding the wardrobe cache                                          |
| **Zoneless**      | Angular change detection without zone.js, driven by signals                                          |
