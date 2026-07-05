# 0002. Core banking architecture baseline

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Repository maintainers

## Context
We need to process money correctly and atomically while enabling real-time downstream value
(notifications, analytics, audit, monitoring) and keeping services independently deployable. This
ADR captures the founding structural decisions of the repository so later changes are measured
against them.

## Decision
We will structure the system as a **transactional (ACID) core plus an event-driven periphery**:

1. **Single trusted core.** `core-banking-service` owns all money movement (credit, debit,
   transfer), NUBAN generation, the double-entry journal, and balances. It is the single source of
   truth (the ledger).
2. **ACID with pessimistic locking.** Each money operation runs in one DB transaction, loads
   affected accounts with a pessimistic write lock in a **deterministic order** (sorted by account
   number) to prevent lost updates and deadlocks. Money is `BigDecimal`.
3. **Publish after commit, fire-and-forget.** After commit, the core publishes `TransactionEvent` /
   `AccountEvent` to **Kafka**. A broker outage must never block or roll back committed money.
4. **Shared contracts in `common`.** Event records and topic names live in one module so producers
   and consumers cannot drift; events evolve additively.
5. **Decoupled periphery.** Notification, reporting, audit, and AI-monitor services consume events
   and own their state; they never read another service's database.
6. **Gateway is routing-only.** `api-gateway` provides the single entry point and cross-cutting
   concerns; it contains no business logic.
7. **AI layer is additive and read-only.** `mcp-server` exposes read-only tools; `ai-monitor-service`
   observes the stream and optionally uses Claude (degrading to heuristics without a key).
8. **PostgreSQL for the core; H2 for dev/test.** Config is environment-overridable via profiles.

## Consequences
- Positive: correctness is centralized and testable; downstream services scale and fail
  independently; new value is added at the edge without touching the core.
- Negative / trade-offs: eventual consistency downstream; event contracts must be governed carefully;
  the core is a critical path requiring rigorous change control.
- Compatibility & migration: establishes the baseline; deviations require a superseding ADR. Schema
  management will migrate from `ddl-auto=update` to Flyway/Liquibase before production (future ADR).

## Alternatives Considered
- Monolith — simpler ops but couples everything to the core's release cycle and scaling.
- Orchestrated/synchronous downstream calls — makes the core depend on downstream availability.
- Shared database across services — tight coupling, contract erosion, hard to evolve.

## References
- `CLAUDE.md` (Architecture summary, Forbidden Changes), `README.md`, `common/` event contracts,
  `core-banking-service/service/*`.
```
