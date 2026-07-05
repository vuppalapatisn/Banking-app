---
name: troubleshooting
description: Diagnose and resolve production or local issues in the Banking-app system — failed/partial transactions, balance discrepancies, Kafka consumer lag or deserialization errors, startup/config failures, and AI-layer errors. Use when debugging an incident, tracing a root cause, or explaining unexpected behavior.
---

# Skill: troubleshooting

## Description
A systematic debugging and root-cause-analysis playbook for a Spring Boot + Kafka + JPA
microservices system. Favors evidence (logs, metrics, DB state, offsets) over guessing, and
distinguishes core (money) issues from periphery (eventing) issues.

## Scope
- **In scope:** runtime errors, transaction/balance anomalies, event flow problems (lag,
  deserialization, missing messages), config/startup failures, AI-layer (MCP/Anthropic) errors.
- **Out of scope:** designing the fix's architecture (use `architecture-review`); reviewing the
  fix diff (use `code-review`); pipeline failures (use `devops`).

## Inputs
- Symptom description, timeframe, affected service(s), and environment.
- Signals: service logs, `GET /actuator/health` & metrics, Kafka UI (http://localhost:8090) for
  topics/consumer-group lag, DB state (ledger entries, transactions, balances), recent deploys/PRs.

## Outputs
- Root-cause statement (what failed, why, trigger conditions).
- Evidence trail (logs/queries/offsets that prove it).
- Remediation: immediate mitigation + durable fix (code/config) + regression test.
- Optional ADR/postmortem note and a `troubleshooting` entry if novel.

## Process
1. **Reproduce / localize.** Which service, which endpoint or topic, since when? Correlate with the
   last deploy.
2. **Gather evidence.** Check `actuator/health`; read logs around the timestamp; inspect Kafka
   consumer-group lag and topic contents; query the DB for the affected account/transaction.
3. **Classify:**
   - *Money/core* (wrong balance, partial post, lock/deadlock, insufficient-funds bug) → inspect
     `TransactionService`/`JournalEntryEngine`, ledger entries (DR/CR must balance per txn), and
     `@Transactional`/lock behavior.
   - *Eventing* (missing downstream effect) → confirm the event was published (core log), consumed
     (consumer log/offset), and deserialized (JsonDeserializer trusted packages / type). Remember
     publishing is fire-and-forget: a broker outage drops events silently by design.
   - *Startup/config* → check profile, datasource URL/creds, env vars, port conflicts.
   - *AI layer* → MCP tool errors or Anthropic failures; recall the ChatClient is optional and falls
     back to heuristics when `ANTHROPIC_API_KEY` is unset/invalid.
4. **Form one hypothesis**, test it against evidence, iterate.
5. **Mitigate then fix**; add a regression test that fails before the fix.
6. **Document** the root cause and prevention.

## Best Practices
- Verify the double-entry invariant first for any balance discrepancy (sum DR == sum CR per txn).
- Use consumer-group lag to tell "not produced" from "not consumed".
- Change one variable at a time; keep an evidence log.
- Prefer idempotent, replay-safe fixes for event issues.

## Anti-Patterns
- "Fixing" a balance by directly editing rows instead of a corrective (reversing) transaction.
- Restarting services to clear symptoms without capturing evidence.
- Assuming Kafka delivered a message without checking offsets/logs.
- Blanket `catch (Exception)` that hides the real error.

## Examples
- *Downstream notification missing.* → Core log shows publish OK, consumer lag > 0 and rising →
  consumer deserialization error (untrusted package/type mismatch) → fix consumer config; replay.
- *Balance looks wrong after a transfer.* → Query ledger entries for the txn; if only one side
  posted, investigate a non-transactional path; add a conservation test.
- *ai-monitor 500 on /analyze.* → Anthropic call failing with a real key → confirm model/key;
  otherwise it should fall back to the heuristic summary (regression if it doesn't).
```
