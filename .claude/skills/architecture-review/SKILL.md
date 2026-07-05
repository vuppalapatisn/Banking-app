---
name: architecture-review
description: Review or evolve the architecture of the Banking-app core-banking system — analyze service boundaries, data flow, event contracts, and transactional integrity, run design reviews, and produce Architecture Decision Records (ADRs). Use when a change affects service boundaries, the event bus, the shared `common` contracts, the ACID core, or when an ADR is requested.
---

# Skill: architecture-review

## Description
Structured architecture analysis and design review for a microservices core-banking platform
(transactional core + Kafka event-driven periphery + API gateway + AI layer). Produces findings,
recommendations, and ADRs that keep the system correct, decoupled, and evolvable.

## Scope
- **In scope:** service boundaries & responsibilities, sync vs async flows, Kafka topic/contract
  design, the `common` shared contracts, transactional/ACID integrity of the core, coupling &
  cohesion, scalability/resilience, ADR authoring under `docs/adr/`.
- **Out of scope:** line-by-line bug hunting (use `code-review`), pipeline/infra mechanics (use
  `devops`), vulnerability analysis (use `security`).

## Inputs
- The change or question (feature, refactor, incident learning, scaling need).
- Current code: `pom.xml` module list, `common/` event records, `core-banking-service/service/*`,
  each service `application.yml`, `README.md`, existing `docs/adr/`.
- Constraints from `CLAUDE.md` (Forbidden Changes) and `.claude/rules/rules.md`.

## Outputs
- A written architecture assessment: current-state summary, options with trade-offs, a recommendation.
- Impact map: which services, contracts, and data flows are affected.
- One or more **ADRs** (`docs/adr/NNNN-title.md`) using the repo template for decisions taken.
- A migration/compatibility note when contracts or boundaries change.

## Process
1. **Map the current state.** Identify affected services, whether the path is sync (gateway→service)
   or async (Kafka), and which contracts (`TransactionEvent`, `AccountEvent`, topics) are involved.
2. **Locate the boundary.** Prefer changes at the periphery (new service / new consumer) over
   changes to the ACID core or shared contracts.
3. **Check invariants.** Double-entry balance, deterministic lock ordering, publish-after-commit,
   single-source-of-truth ledger, backwards-compatible contracts.
4. **Enumerate options** (≥2) with trade-offs: coupling, blast radius, operational cost, reversibility.
5. **Recommend** the simplest option that satisfies the requirement (KISS/YAGNI) and preserves
   functionality.
6. **Record an ADR** for any decision that changes boundaries, contracts, data stores, or the core.
7. **Define compatibility/migration** steps if a contract or boundary moves.

## Best Practices
- Keep the core small and trusted; add value at the edges via events.
- Evolve events additively (new nullable fields); version topics only when unavoidable.
- Make services independently deployable and configurable via environment variables.
- Design for graceful degradation (a down consumer must not affect the core).
- One decision per ADR; link superseding ADRs.

## Anti-Patterns
- Adding business logic to `api-gateway`.
- A downstream service reading another service's database (share events, not tables).
- Synchronous chains that make the core depend on a downstream service being up.
- Breaking event field order/types in place; "temporary" contract hacks.
- Introducing a new datastore/broker/framework for a single feature without an ADR.

## Examples
- *"Add fraud scoring on transactions."* → New consumer of `banking.transactions` (or extend
  `ai-monitor-service`); no core change; ADR only if a new datastore is introduced.
- *"Add a `channel` attribute to transactions."* → Additive nullable field on `TransactionEvent`
  + producer set + consumers tolerate null; backwards-compatible; short ADR.
- *"Reporting needs sub-second dashboards."* → Evaluate materialized read model vs streaming; ADR
  comparing options; keep core untouched.
```
