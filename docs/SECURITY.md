# Security posture

## Reporting

Please do not open a public issue for a vulnerability. Contact the repository owner
directly.

## Authentication

Owner identity is resolved once per request in `backend/common`
(`OwnerIdentityFilter` → `OwnerResolver`) and reaches controllers as
`@CurrentOwner UUID`. Controllers never read a raw header or parse a token.

| Mode    | Credential                       | Header trusted?     | Intended for            |
| ------- | -------------------------------- | ------------------- | ----------------------- |
| `DEV`   | `X-Owner-Id: <uuid>`, optional   | Yes                 | A laptop. Nothing else. |
| `TOKEN` | `Authorization: Bearer <token>`  | **No — ignored**    | Deployments             |

Three properties of the design are worth stating plainly:

1. **`TOKEN` mode ignores `X-Owner-Id`.** If it merely preferred the token, the
   header would remain a one-line bypass of the thing the token proves.
2. **`prod` fails closed.** The `prod` profile defaults `dressd.auth.mode` to
   `TOKEN`, and `TOKEN` mode refuses to start without a secret of at least 32
   characters. A deployment that forgets to configure auth does not boot; it does
   not quietly serve unauthenticated traffic.
3. **`DEV` mode is unauthenticated by design.** It is not weak authentication, it is
   none. The docker-compose stack runs in `DEV` because it is a local dev
   orchestration.

Tokens are HMAC-SHA256 over `<ownerId>:<expiry>` with a single fixed algorithm —
chosen over generic JWT parsing to eliminate the algorithm-confusion class of bug —
and compared in constant time. There is **no revocation list**: this is a stepping
stone to a real identity provider, not a destination. See
[ADR-002](ADR.md#adr-002--signed-token-owner-identity-header-mode-for-local-work).

## Authorisation

Every query is owner-scoped in the repository layer
(`findByIdAndOwnerId`, `Specification.ownedBy`), so a resource belonging to another
owner reads as `404` rather than `403` — the existence of the id is not disclosed.

Client-supplied `imageUrl` on `POST /api/garments` is validated to be one of the
caller's own scans. Without that check a caller could attach an arbitrary URL, or
another owner's image, to their own garment
([ADR-009](ADR.md#adr-009--confirm-on-write-media-lifecycle)).

## Uploads

| Control                   | Where                                                |
| ------------------------- | ---------------------------------------------------- |
| 15MB size cap             | nginx, Spring multipart, and the FastAPI reader      |
| Enforced while streaming  | the sidecar aborts mid-read rather than buffering first, so a caller cannot decide how much memory to consume before the check |
| Content-type allow-list   | `image/jpeg`, `image/png`, `image/webp`, `image/heic` |
| Decompression-bomb cap    | 40 megapixels, checked from the image header before any decode |
| Path-traversal rejection  | every storage key operation                          |

## Error responses

A catch-all handler logs the detail and returns a generic `500` message, so
stack traces and internal paths do not reach clients. Client errors keep their
specific message because it is actionable.

## Containers

All three images run as a non-root user. The media volume inherits that ownership so
the application can still write to it. The web image uses `nginx-unprivileged` on
port 8080, since a non-root process cannot bind 80.

## Secrets

Externalised via environment variables; none are committed. `.env` is gitignored and
[`.env.example`](../.env.example) documents what is needed. Generate a signing
secret with:

```bash
openssl rand -base64 48
```

## Known gaps

Honest list of what is *not* addressed:

- **No token revocation.** Compromised tokens stay valid until they expire.
- **No rate limiting.** The scan endpoint runs image processing on request, which is
  the obvious thing to abuse.
- **No audit log** of who read or deleted what.
- **Transport security is deployment-level.** Nothing in the stack terminates TLS;
  put it in front.
- **Actuator is unauthenticated.** `health`, `info`, `metrics` and `prometheus` are
  public. Restrict them at the network layer, or move them to a management port,
  before exposing a service to the internet.
