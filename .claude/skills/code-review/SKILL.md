---
name: code-review
description: Review a pull request or working diff in the Banking-app repo for correctness, security, and performance — with special attention to money math, transactional/locking integrity, and Kafka event-contract compatibility. Use when reviewing a PR, validating a change before merge, or performing a focused security/performance pass on Java/Spring code.
---

# Skill: code-review

## Description
A rigorous review playbook for this Spring Boot core-banking codebase. Prioritizes financial
correctness and safety over style. Covers correctness, security, and performance dimensions.

## Scope
- **In scope:** Java/Spring changes across all modules; REST controllers, JPA repos/entities,
  transactional services, Kafka producers/consumers, config, tests, Dockerfiles.
- **Out of scope:** high-level boundary/contract design rationale (use `architecture-review`);
  deep threat modeling (use `security`); pipeline tuning (use `devops`).

## Inputs
- The diff / PR (`git diff main...HEAD` or the PR link) and its description.
- Context: touched modules' code, `application.yml`, tests, `CLAUDE.md` Forbidden Changes,
  `.claude/rules/rules.md`.

## Outputs
- Findings ranked by severity (blocker / major / minor / nit), each with file:line, the concrete
  failure scenario, and a suggested fix.
- A merge recommendation (approve / request changes) and the list of unmet mandatory checks.

## Process
1. **Understand intent** from the PR description; confirm the diff matches it and is single-purpose.
2. **Correctness (highest priority):**
   - Money is `BigDecimal` with correct scale/rounding; no `double`/`float`.
   - Money paths are `@Transactional`; accounts loaded via the pessimistic-lock finder; lock order
     deterministic (sorted); balances mutated only through `JournalEntryEngine`.
   - Double-entry preserved (equal DR/CR); insufficient-funds and self-transfer handled.
   - Event publish stays after-commit and fire-and-forget.
3. **Contract compatibility:** `common` event records, topic names, and consumer `group-id`s
   unchanged — or a documented breaking change + ADR + all-sides update.
4. **Security:** input validated (`@Valid`, bounds); no secrets/PII in code, config, or logs; no
   TLS-verification bypass; least privilege.
5. **Performance:** no N+1 JPA queries; correct fetch/pagination; no unbounded in-memory growth;
   locks held minimally; no blocking calls on the transaction/hot path.
6. **Tests:** added/updated for the change; failure cases covered; `contextLoads` still present;
   coverage not decreased.
7. **Docs:** README/run.md/CLAUDE.md/ADR updated as applicable.
8. **Summarize** with ranked findings and a clear verdict.

## Best Practices
- Reproduce the bug in words (inputs → wrong output) before asserting it's a bug; skip stylistic noise.
- Verify concurrency claims by reasoning about interleavings, not just reading the happy path.
- Confirm new dependencies are BOM-managed and free of critical CVEs.
- Prefer requesting a test over prose when correctness is in doubt.

## Anti-Patterns (flag these)
- Field injection, business logic in controllers/gateway, catching `Exception` and swallowing it.
- Removing `@Transactional`/locks; locking in request order; blocking Kafka sends in the txn.
- `float`/`double` for money; string concatenation into JPQL/SQL; missing validation.
- Logging PANs/BVNs/tokens; hard-coded credentials; disabling cert checks.
- Reflowing/auto-formatting untouched code (noisy diffs).

## Examples
- *Debit with no funds check* → **blocker**: allows negative balance; require funds check + test.
- *`amount` typed `double` in a new DTO* → **blocker**: switch to `BigDecimal`.
- *New consumer changes `TransactionEvent` field order* → **blocker**: breaks other consumers;
  make additive + ADR.
- *`findAll()` then filter in memory for one account* → **major**: add a derived query; risks OOM.
```
