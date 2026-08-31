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

Rotate `RELAY_BOOTSTRAP_API_KEY`, restart all replicas, and audit access. Production deployments must source it from a Kubernetes Secret or external secret manager; never place it in Helm values committed to Git.
