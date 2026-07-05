# 0001. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Repository maintainers

## Context
The system spans multiple services with shared contracts and a high-risk transactional core.
Decisions about boundaries, contracts, and the core must be captured durably so future engineers
and AI agents understand *why* the system is shaped as it is and can change it safely.

## Decision
We will record significant architectural decisions as numbered **Architecture Decision Records**
under `docs/adr/`, using `docs/adr/adr-template.md`. ADRs are immutable once Accepted; a decision is
reversed by a new ADR that supersedes the old one. The ADR process is described in
`docs/GOVERNANCE.md`.

## Consequences
- Positive: durable rationale; safer evolution; onboarding and agent context improve.
- Negative: small overhead per significant change.
- Compatibility & migration: none.

## Alternatives Considered
- Rely on commit messages / PR descriptions — too scattered and easily lost.
- A single design doc — becomes stale and mixes many decisions.

## References
- `docs/GOVERNANCE.md`, `CLAUDE.md`, Michael Nygard's ADR pattern.
```
