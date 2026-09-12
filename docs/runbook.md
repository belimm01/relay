# Operations runbook

## Delivery backlog grows

1. Compare ingest rate with completed-attempt rate.
2. Check database saturation and the count of expired leases.
3. Confirm destination timeouts are bounded and workers are still polling.
4. Scale backend replicas only while the database has connection and I/O headroom.
5. Do not delete queue rows to recover capacity; pause ingest or isolate a failing destination.

## Repeated delivery failures

Inspect the latest attempt without logging authorization headers or payload secrets. Confirm DNS, TLS, timeout, and destination response status. Replay only after fixing the destination; Relay provides at-least-once delivery, so consumers must tolerate duplicates.

## Database migration fails

Stop the rollout and keep the previous application version serving. Inspect the Liquibase lock and failed change set. Never clear a lock while another migration process is active. Restore from a tested backup if a non-transactional schema change partially applied.

## Bootstrap key exposure

### Scope and prerequisites

`RELAY_BOOTSTRAP_API_KEY` updates the stored key hash for `admin@relay.local` when a backend starts. It is not a live configuration reload and does not rotate other users or endpoint ingest tokens. An empty value leaves an existing key unchanged. Each API request checks the database, so the old key stops authenticating after the new hash commits; there is no overlap period for client migration.

Use a fresh secret from a secret manager. Do not put either key in Git, PRs, terminal command arguments, screenshots, or incident logs. Production deployments must source the key from a Kubernetes Secret or external secret manager, not Helm values committed to Git.

### Rotation procedure

1. Restrict external access while containing the incident, including `/ws` and `/in/` if necessary. API-key rotation alone does not revoke existing WebSocket connections or endpoint ingest URLs; the API-key filter protects `/api/`, not those routes.
2. Pause backend rollouts and automatic restarts under the old configuration. Record the intended replica count, stop all backend instances, and wait for them to exit. Leave PostgreSQL and its volume intact. This introduces downtime but prevents an old-config instance from starting later and writing the compromised key hash back into the shared database.
3. Replace the bootstrap secret in every backend startup configuration, including rollback and disaster-recovery configurations. In Kubernetes, update the referenced Secret while the backend Deployment is scaled to zero. In Compose, update the protected environment source and recreate the backend; `docker compose restart` alone does not apply changed environment variables. Never use `docker compose down --volumes` for rotation.
4. Start one backend with the new configuration. Once ready, verify `GET /api/endpoints` returns `200` with the new `X-Api-Key`, `401` with the old key, and `401` without a key. Supply secrets through a protected client without recording request headers or response bodies. Check status codes only; do not use a write endpoint as a credential probe.
5. Restore the remaining replicas using the same new secret. Repeat the credential checks against each backend and through the public API entry point before restoring access. Update dashboard sessions and API clients with the new key through a secure channel.
6. Audit access during the exposure window, including endpoint and destination changes. Treat exposed ingest URLs separately; changing the bootstrap key does not invalidate them.

### Failure and rollback

If the new key fails, keep external access restricted and verify the configured secret reference and the existence of `admin@relay.local`. Bootstrap does not create that account when other users already exist. Do not delete user records or database volumes to recover access.

If the old key becomes valid again, stop all backends and find the stale startup configuration before retrying. Roll back application images only with the new secret retained; never restore the compromised secret. A database restore may also restore the old key hash, so repeat rotation and credential checks before reopening access.
