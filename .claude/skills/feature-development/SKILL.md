---
name: feature-development
description: Implement a new feature in the Banking-app core-banking system end-to-end — impact analysis, design that respects the ACID core / event-driven boundaries, implementation, tests, and docs. Use when adding a new endpoint, service, event consumer, product rule, or AI tool, or when estimating the blast radius of a proposed change.
---

# Skill: feature-development

## Description
A disciplined workflow for adding features without destabilizing the transactional core or the
event contracts. Emphasizes additive, decoupled change and test-first for money logic.

## Scope
- **In scope:** new REST endpoints, new services/modules, new Kafka consumers, product/config rules,
  new MCP tools, new anomaly rules; the tests and docs that accompany them.
- **Out of scope:** infra/pipeline delivery (use `devops`); pure design/ADR debate (use
  `architecture-review`); incident fixes (use `troubleshooting`).

## Inputs
- The feature request and acceptance criteria.
- Current contracts (`common`), affected service code, `application.yml`, `CLAUDE.md`, rules.

## Outputs
- Working, tested code in the correct module following repo conventions.
- New/updated tests (unit + `contextLoads`, plus core invariants when money logic changes).
- Updated module `README.md` (endpoints), `run.md` if run steps change, and an ADR if the design
  changes boundaries/contracts.
- A short impact summary (modules touched, contracts affected, risk).

## Process
1. **Impact analysis.** Decide: does this belong at the edge (new service/consumer) or does it truly
   require a core change? Prefer the edge. List affected modules, contracts, and config.
2. **Design.** Keep the core untouched if possible; consume events for read-side/reactive features.
   For core changes, design the transaction, locking, and journal impact first.
3. **Contracts.** If events must change, make it additive (new nullable field); update `common` and
   all producers/consumers together; note compatibility.
4. **Implement** following conventions: package-by-layer, constructor injection, records for DTOs,
   `@Valid` inputs, `BigDecimal` money, `ProblemDetail` errors, env-overridable config.
5. **Test-first for money logic**; add `contextLoads`; cover failure paths.
6. **Wire config** with safe localhost defaults and env overrides; expose actuator health.
7. **Document** (README/run.md/ADR) and run `mvn -pl <module> -am verify`, then full `mvn clean verify`.

## Best Practices
- New feature → new consumer/service beats editing the core (Open/Closed).
- Reuse `common` contracts; never duplicate event records.
- Keep the change single-purpose and small; land infra/config toggles behind env vars.
- Add the feature's endpoints to the module README in the same PR.

## Anti-Patterns
- Bolting business logic onto `api-gateway` or into controllers.
- Cross-service database reads instead of consuming events.
- Breaking event/topic contracts to ship faster.
- Speculative generality (extra config/abstraction "for later") — YAGNI.
- Shipping a money-path change without concurrency and failure tests.

## Examples
- *"Email a monthly statement."* → New scheduled job in `notification-service` reading its own
  data; no core/contract change.
- *"Block debits above a per-account daily limit."* → Read the limit from `product-config-service`;
  enforce in `core-banking-service` debit path inside the existing transaction; add tests for
  limit-exceeded and boundary; document; ADR if it introduces a sync dependency.
- *"Expose recent transactions as an MCP tool."* → Add a `@Tool` method in `mcp-server/BankingTools`
  calling core via `RestClient` with simulated fallback; register via `ToolCallbackProvider`.
```
