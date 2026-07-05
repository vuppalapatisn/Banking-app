---
name: security
description: Perform security work on the Banking-app system — SAST/static review, DAST/dynamic testing guidance, dependency/CVE review, secrets and PII handling, and threat modeling of the money paths, event bus, and AI layer. Use when reviewing security, hardening a service, triaging a vulnerability, or modeling threats for a change.
---

# Skill: security

## Description
Security review and threat-modeling playbook for a financial system handling money, account data,
and PII (BVN/NIN, contact details). Applies "secure by default" and least privilege, with emphasis
on the transactional core, Kafka contracts, and the AI layer.

## Scope
- **In scope:** static analysis of Java/Spring code, dependency/CVE review, secrets & PII handling,
  authn/authz posture, input validation, transport/at-rest encryption, dynamic-test guidance,
  threat modeling.
- **Out of scope:** functional correctness bugs (use `code-review`); pipeline mechanics (use
  `devops`); architecture trade-offs (use `architecture-review`).

## Inputs
- The change or target service; its endpoints, data handled, and trust boundaries.
- Dependency tree (`mvn dependency:tree`), config (`application.yml`), CI security-scan results.

## Outputs
- Findings by severity (critical/high/medium/low) with concrete exploit scenario and remediation.
- Dependency risk report (vulnerable/abandoned libs; upgrade path).
- Threat model for significant changes (assets, entry points, threats, mitigations).
- Hardening recommendations and required follow-ups before production.

## Process
1. **Define trust boundaries.** Channels → gateway → services; service → Kafka; service → DB;
   service → third parties; MCP/LLM boundary. Note which handle money or PII.
2. **SAST / static review:** injection (JPQL/SQL built by concatenation), missing `@Valid`,
   secrets in code/config, sensitive data in logs (PAN/BVN/tokens), unsafe deserialization
   (Kafka JsonDeserializer trusted packages), SSRF in outbound calls, error responses leaking internals.
3. **Dependency review:** `mvn dependency:tree`; flag known critical CVEs and abandoned libs; ensure
   versions come from managed BOMs; prefer patched upgrades.
4. **Secrets & PII:** no hard-coded secrets (`${VAR:default}` placeholders only); PII minimized and
   masked in logs; secrets sourced from a manager in prod.
5. **AuthN/AuthZ:** confirm money/account endpoints require auth+authz before prod; least-privilege
   DB users and service tokens; fail closed.
6. **Crypto/transport:** TLS everywhere in prod; no cert-verification bypass; sensitive data at rest
   encrypted.
7. **DAST guidance:** exercise running services (via gateway) for authz bypass, input abuse
   (negative/huge amounts, self-transfer, overflow), rate limits, and error leakage.
8. **Threat model** the change; record mitigations; open follow-ups for accepted risks.

## Best Practices
- Money and PII are crown jewels — validate, authorize, and log-mask around them by default.
- Keep the Kafka `JsonDeserializer` trusted-packages list tight (`com.banking.common.event`).
- Validate amounts (positive, scale, upper bounds) at the edge and in the core.
- Treat LLM/MCP output as untrusted; MCP tools are read-only and must not perform money movement.
- Upgrade vulnerable deps promptly; document accepted risk with an owner and expiry.

## Anti-Patterns
- Hard-coded credentials/keys; committing real `application-*.yml`; logging secrets or full PANs/BVNs.
- Disabling TLS verification; trust-all managers; `permitAll()` on sensitive endpoints.
- Building SQL/JPQL via string concatenation; echoing stack traces to clients.
- Widening Kafka trusted packages to `*`; deserializing arbitrary types.
- Giving MCP/AI components write access to money paths or broad DB privileges.

## Examples
- *New endpoint accepts `amount`.* → Enforce `@DecimalMin`, positive, max bound, scale; authorize
  caller; add abuse tests (negative/huge/self-transfer).
- *Dependency flagged with a critical CVE.* → Confirm reachability, upgrade to patched version
  (BOM-aligned), re-run tests; if unavoidable, document accepted risk + mitigation.
- *Third-party integration added.* → Validate/allowlist outbound URLs (SSRF), use TLS, keep creds in
  a secrets manager, never log responses containing PII.
```
