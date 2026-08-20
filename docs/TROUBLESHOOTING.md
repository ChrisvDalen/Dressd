# Troubleshooting

| Symptom                                                       | Likely cause                          | Fix                                                                                       |
| ------------------------------------------------------------- | ------------------------------------- | ----------------------------------------------------------------------------------------- |
| `ng` fails: "requires Node ≥ 22.22.3"                         | Old Node                              | Use Node 24 (`nvm use 24`)                                                                |
| `npm install` peer-dep error on `@ngrx/signals`               | No NgRx v22 yet                       | `npm install --legacy-peer-deps`                                                           |
| Service exits at startup: "requires dressd.auth.secret"       | `TOKEN` mode with no secret           | Set `AUTH_SECRET` (≥32 chars), or `AUTH_MODE=DEV` locally. This failure is deliberate.     |
| Every API call returns `401`                                  | `TOKEN` mode, no bearer token         | Mint one with `OwnerTokenTool` ([API.md](API.md#authentication)), or use `DEV` mode         |
| `401` even though `X-Owner-Id` is set                         | `TOKEN` mode ignores that header      | By design ([SECURITY.md](SECURITY.md)); send a bearer token                                |
| `400 "Invalid X-Owner-Id header: not a UUID"`                 | Malformed header                      | Send a valid UUID, or omit it to get the demo owner                                        |
| Startup fails: `Schema-validation: missing table [...]`       | Migrations did not run, or drifted    | Check the Flyway log lines; confirm `spring.flyway.enabled` and that the schema matches ADR-007 |
| Startup fails: `Schema-validation: wrong column type`         | Entity changed without a migration    | Add a migration; never edit an applied one ([CONTRIBUTING.md](../CONTRIBUTING.md))          |
| `400 "imageUrl is not a Dressd media URL"`                    | `imageUrl` was not from `/api/scan`   | Post the `imageUrl` the scan returned, unmodified                                          |
| `400 "does not reference one of your scans"`                  | Another owner's image, or a stale scan the sweep reclaimed | Re-scan                                                              |
| Scan returns `415`                                            | Upload's content type not an image    | Send `image/jpeg`, `image/png`, `image/webp` or `image/heic`                                |
| Scan returns `413`                                            | Photo over 15MB                       | Downscale, or raise the cap in all three places ([CONFIGURATION.md](CONFIGURATION.md))     |
| Scan returns `422 "Could not process image"`                  | Unreadable, truncated, or >40MP image | Check the sidecar log; the `traceparent` there ties back to the wardrobe request           |
| Scan succeeds but the category is odd                         | Sidecar down → deterministic stub     | Expected fallback; check `wardrobe-service` logs for "Recognition sidecar unavailable"      |
| Cut-out has no transparency                                   | `rembg` absent + busy background      | The offline colour-key needs a fairly uniform backdrop; install `rembg` for quality        |
| Web loads but API calls `404` in dev                          | Proxy not active                      | Ensure `ng serve` is using `proxy.conf.json`                                               |
| Outfit card says "N items no longer in your wardrobe"         | Garments deleted after the outfit was saved | Expected ([ADR-008](ADR.md#adr-008--no-cross-service-referential-integrity))          |
| `docker compose build` fails on the Maven cache mount         | BuildKit disabled                     | `DOCKER_BUILDKIT=1`, or upgrade Docker (BuildKit is the default in current versions)        |
| Postgres refuses connection in compose                        | DB not healthy yet                    | Services wait on the healthcheck; retry, or `docker compose logs postgres`                 |
| `web` container is healthy but the app 502s on `/api/*`       | A backend is not up                   | `docker compose ps`; compose gates `web` on all three services being healthy                |
| Old H2 dev data conflicts after the Flyway change             | Pre-migration `ddl-auto: update` schema | `rm -rf backend/*/data/` and restart; dev data is disposable                              |

## Useful commands

```bash
# What did Flyway actually apply?
docker compose logs wardrobe-service | grep -i flyway

# Is a service healthy, and which build is it running?
curl -s localhost:8081/actuator/health
curl -s localhost:8081/actuator/info

# Follow a scan across services by its trace id
docker compose logs | grep "<traceId>"

# Reset everything, including volumes
docker compose down -v
```
