# Service objectives and telemetry

## Proposed objectives

These objectives define how Relay should be operated; they are not claims about a public production deployment.

| Signal | Objective | Measurement |
| --- | --- | --- |
| Ingest availability | 99.9% successful acknowledgements per 30 days | non-5xx responses at `/in/*`, excluding invalid tokens |
| Ingest latency | 99% below 250 ms over 5 minutes | server request histogram |
| Delivery freshness | 99% of first attempts start within 30 seconds | `first_attempt_at - captured_at` |
| Queue recovery | no delivery remains leased beyond visibility timeout plus one poll interval | overdue leased-delivery gauge |

Destination failures are not Relay availability failures, but exhausted deliveries must remain visible and replayable.

## Required production dashboards

- ingest rate, latency, and 5xx ratio;
- pending/due/leased/exhausted delivery counts;
- attempt outcome and latency by destination host (with bounded cardinality);
- database connection saturation and claim duration;
- JVM heap, pause time, and virtual-thread pinning.

Prometheus metrics are exposed at `/actuator/prometheus`; health probes use `/actuator/health/liveness` and `/actuator/health/readiness`.
