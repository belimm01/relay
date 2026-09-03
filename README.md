# Relay

A self-hosted webhook gateway and inspector.

Relay sits between the services that *send* webhooks (Stripe, GitHub, Shopify, …)
and the services that *consume* them. It captures every inbound request into a
durable log, fans events out to one or more destinations, retries failed
deliveries with exponential backoff, and lets you inspect and replay any event
from a live dashboard.

## Why

Debugging webhooks is painful: payloads arrive once, delivery failures are
invisible, and there's no easy way to replay a request against a local service.
Relay makes the whole flow observable and recoverable.

## How it works

```
 sender ──POST /in/{token}──▶ Relay ──┬─▶ destination A
                               │       └─▶ destination B
                               │
                          Postgres (durable log + delivery queue)
                               │
                          dashboard ◀── live event stream (WebSocket)
```

1. **Capture.** Every request to an endpoint's ingest URL is persisted in full —
   method, headers, body, source IP — and acknowledged with `202 Accepted`.
2. **Fan-out.** Each active destination whose filter matches the event gets its
   own delivery row. Deliveries are a durable queue, not fire-and-forget.
3. **Deliver.** A poller claims due deliveries with `FOR UPDATE SKIP LOCKED`,
   sends them on virtual threads, and records every attempt. Failures are
   retried with exponential backoff and jitter until they succeed or the attempt
   budget is exhausted.
4. **Observe.** The dashboard streams events as they arrive and shows the full
   delivery timeline for each one. Any event can be replayed with one click.

## Design notes

- **Virtual threads** (Java 21) carry outbound deliveries, so a slow destination
  blocks a cheap virtual thread rather than a pooled platform thread.
- **Durable delivery queue** in Postgres using `SKIP LOCKED` with a lease /
  visibility-timeout, so multiple instances can poll the same queue safely.
- **Backoff with equal jitter**, driven by an injectable source of randomness so
  the schedule is deterministic under test.
- **Transaction-safe broadcasting** — websocket events are published on
  `AFTER_COMMIT`, so a client never sees an event that later rolls back.
- **Safe filter DSL** — destination filters are a small, non-executable
  expression language (`body.type == "order.created" && body.amount >= 100`),
  never `eval` or SpEL.
- **API-key auth** via a request filter with SHA-256 hashed keys; no plaintext
  key is ever stored.

## Stack

- **Backend** — Java 21, Spring Boot 3.4, PostgreSQL 16, Liquibase
- **Frontend** — Vue 3, TypeScript, Vite, Pinia
- **Testing** — JUnit 5, Mockito, AssertJ, Testcontainers, Playwright
- **Infra** — Docker Compose, GitHub Actions

## Running it

### With Docker Compose

```bash
docker compose up --build
```

Then open http://localhost:5173 and sign in with the bootstrap key
`rk_local_dev_key` (override it with `RELAY_BOOTSTRAP_API_KEY`).

### Locally

Backend (needs a Postgres on `localhost:5432`):

```bash
cd backend
RELAY_DB_URL=jdbc:postgresql://localhost:5432/relay \
RELAY_DB_USER=relay RELAY_DB_PASSWORD=relay \
mvn spring-boot:run
```

On first start the backend logs a generated API key. Frontend:

```bash
cd frontend
npm install
npm run dev
```

The dev server proxies API, ingest, and websocket traffic to the backend, so the
app is reachable at http://localhost:5173.

### Sending a test webhook

```bash
curl -X POST http://localhost:8080/in/<ingest-token> \
  -H 'Content-Type: application/json' \
  -d '{"type":"order.created","amount":150}'
```

It shows up on the dashboard immediately.

## Tests

```bash
cd backend && mvn verify
```

Unit tests cover the backoff policy, filter evaluator, and delivery state
machine. The Testcontainers suite spins up a real Postgres and exercises the
whole ingest-to-delivery path, including the retry loop, against a stub HTTP
server.

The browser suite exercises endpoint creation, webhook ingestion, delivery
visibility, and replay against the complete Docker Compose stack:

```bash
docker compose up --build -d
cd frontend
npx playwright install chromium
npm run test:e2e
cd .. && docker compose down --volumes
```

## Production engineering

- [ADR: PostgreSQL durable delivery queue](docs/adr/0001-postgres-delivery-queue.md) records the consistency and scaling trade-offs.
- [Service objectives](docs/slo.md) define ingest availability, latency, delivery freshness, and the required dashboards.
- [Operations runbook](docs/runbook.md) covers backlog recovery, repeated destination failures, migrations, and key rotation.
- Prometheus metrics are available at `/actuator/prometheus`; Kubernetes probes use the dedicated Actuator liveness/readiness endpoints.
- `performance/ingest.js` is a k6 arrival-rate scenario with explicit error-rate and p99 thresholds.

## Kubernetes

The Helm chart under `deploy/helm/relay` deploys separate backend and frontend workloads with non-root security contexts, health probes, resource guarantees, and externally managed secrets. It deliberately does not create PostgreSQL or place credentials in values.

```bash
helm lint deploy/helm/relay
helm template relay deploy/helm/relay > /tmp/relay.yaml
```

The default image coordinates are examples; build and publish the two repository Dockerfiles or override the image repositories and immutable tags.

## Roadmap

- [x] Phase 1 — inbound capture, durable event log, single-destination forward
- [x] Phase 2 — async delivery, retries with backoff, filtering, replay
- [x] Phase 3 — live dashboard over WebSockets, Testcontainers integration tests, CI
- [ ] Publish signed multi-architecture images and a versioned Helm release

## License

MIT — see [LICENSE](LICENSE).
