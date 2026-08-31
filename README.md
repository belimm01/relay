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

## Stack

- **Backend** — Java 21, Spring Boot, PostgreSQL, Liquibase
- **Frontend** — Vue 3, Vite, Pinia
- **Infra** — Docker Compose, GitHub Actions

## Status

Work in progress. See the roadmap below.

### Roadmap

- [ ] Phase 1 — inbound capture, durable event log, single-destination forward
- [ ] Phase 2 — async delivery, retries with backoff, filtering, transforms, replay
- [ ] Phase 3 — live dashboard over WebSockets, Testcontainers integration tests, CI

## License

MIT — see [LICENSE](LICENSE).
