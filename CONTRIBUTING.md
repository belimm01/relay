# Contributing

Open an issue before changing API contracts, delivery semantics, persistence, or the filter language. Keep pull requests focused and add a regression test for behavioral changes.

Before opening a pull request:

```bash
cd backend && mvn verify
cd ../frontend && npm ci && npm run build
cd .. && helm lint deploy/helm/relay
```

Never commit API keys, webhook payloads from real systems, database dumps, or employer-owned code and configuration.
