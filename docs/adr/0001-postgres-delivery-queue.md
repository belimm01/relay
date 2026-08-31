# ADR 0001: Use PostgreSQL as the durable delivery queue

- Status: accepted
- Date: 2026-08-31

## Context

Relay must acknowledge inbound webhooks quickly, retain their original payload, fan them out, and retry destinations independently. Adding a separate broker would improve very high-throughput scaling but would introduce another consistency boundary and operational dependency for a self-hosted product.

## Decision

Persist events, deliveries, and delivery attempts in PostgreSQL. Workers claim due rows with `FOR UPDATE SKIP LOCKED` and a visibility lease. Event capture and delivery creation share a transaction; outbound HTTP never runs while a database transaction is held.

## Consequences

- The durable log and queue have one backup and recovery model.
- Claiming is safe across multiple worker instances and abandoned leases can be recovered.
- Queue throughput is bounded by the primary database and hot-index contention.
- A future broker migration must preserve delivery identity, attempt history, and at-least-once semantics; destination idempotency remains required.
