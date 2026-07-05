# Repository Rules

Non-negotiable rules for any agent or engineer changing this repository. These complement
[`CLAUDE.md`](../../CLAUDE.md). "MUST" = enforced; a PR violating a MUST should be rejected.

---

## Development Rules

### Backwards compatibility
- MUST keep public REST contracts (paths, request/response fields, status codes) backwards
  compatible within a major version. Add fields; never repurpose or remove them without a version bump.
- MUST keep Kafka contracts stable: topic names (`banking.transactions`, `banking.accounts`),
  consumer `group-id`s, and `common` event record shape. Evolve events **additively** (new nullable
  fields). Removing/renaming/reordering fields is a **breaking change**.
- MUST NOT change default ports or environment-variable names without updating every dependent
  service, `docker-compose.yml`, `README.md`, and `run.md`.

### Versioning
- MUST follow **Semantic Versioning** for the artifact version and any published API. See
  `docs/GOVERNANCE.md`.
- Dependency versions MUST come from the managed BOMs (Spring Boot/Cloud/AI). MUST NOT hard-code a
  version the BOM already provides.
- Breaking changes MUST be recorded in an ADR and called out in the PR description and changelog.

### Code review requirements
- Every change MUST be reviewed (human or the `code-review` skill) before merge to `main`.
- Reviews MUST verify: correctness (esp. money math & transactions), tests, security, and docs.
- No direct commits to `main` for feature work — use a branch + PR.

### Documentation updates
- MUST update docs in the same PR as the change: module `README.md` for behavior, `run.md` for
  run/build steps, `CLAUDE.md`/rules for conventions, and an ADR for architectural decisions.
- New endpoints MUST be documented (method, path, body, response) in the module README.

---

## Security Rules

### Secrets handling
- MUST NOT commit secrets, API keys, passwords, tokens, or real connection strings. Use environment
  variables / a secrets manager. `application.yml` may only contain **placeholders with safe
  local defaults** (e.g. `${ANTHROPIC_API_KEY:dummy-key}`).
- MUST NOT log secrets, full card PANs, full BVN/NIN, passwords, or auth tokens. Mask sensitive data.
- `.env`, `application-*.yml` with real values, and keystores MUST remain git-ignored.

### Encryption
- All external network traffic MUST use TLS in production. MUST NOT disable certificate verification
  (`curl -k`, `insecure`, custom trust-all managers) in code or committed config.
- Sensitive data at rest (PII, credentials) SHOULD be encrypted; secrets MUST be stored in a manager,
  not plaintext.

### Authentication & authorization standards
- Money-moving and account-data endpoints MUST be authenticated and authorized before production
  exposure (currently a demo without auth — do not present it as production-ready without this).
- MUST apply least privilege: DB users limited to needed schemas/ops; service tokens scoped narrowly.
- MUST validate and sanitize all external input (`@Valid` on request bodies, bounds on amounts).
- MUST fail closed: on auth/validation error, deny the operation.

---

## Testing Rules

### Unit testing
- Pure logic (calculations, rules, mappers) MUST have unit tests (JUnit 5 + AssertJ). Example:
  NUBAN check-digit, anomaly thresholds, balance math.
- New/changed money logic in `core-banking-service` MUST include tests for: balance conservation,
  insufficient-funds rejection, double-entry (DR total == CR total), and self-transfer rejection.

### Integration testing
- Every service MUST keep its `@SpringBootTest` `contextLoads` test green (wiring/config guard).
- Repository queries SHOULD be covered with `@DataJpaTest`; controllers with `@WebMvcTest` or
  `@SpringBootTest` + MockMvc.
- Kafka consumer/producer changes SHOULD be verified with an embedded broker
  (`spring-kafka-test` / `@EmbeddedKafka`) or Testcontainers where feasible.

### Coverage expectations
- New code SHOULD reach **≥ 80% line coverage**; the `core-banking-service` money paths SHOULD target
  **≥ 90%** and cover failure/edge cases, not just the happy path.
- Coverage MUST NOT be gamed with assertion-free tests. A PR MUST NOT decrease coverage of touched
  modules.

---

## CI/CD Rules

### Pipeline validation
- CI (`.github/workflows/ci.yml`) MUST pass before merge. A red build blocks merge.
- MUST NOT disable, skip, or `continue-on-error` a failing check to force a merge. Fix the cause.
- Changes to the pipeline MUST be reviewed and MUST keep the `build` (mvn clean verify) job intact.

### Security scanning
- Dependency and static-analysis scanning SHOULD run in CI (e.g. dependency review, CodeQL/SAST).
  New high/critical findings MUST be triaged before merge (fix, or documented accepted-risk).
- MUST NOT introduce dependencies with known critical CVEs or unvetted/abandoned libraries.

### Build verification
- The full reactor MUST build with `mvn clean verify` on JDK 21. MUST NOT commit code that only
  builds locally due to cached artifacts.
- Docker images MUST build from the repo root context using the per-service `Dockerfile`.

---

## PR Rules

### Mandatory checks (all MUST pass before merge)
1. CI `build` job green (`mvn clean verify`, all modules).
2. Docker image build green for any service whose Dockerfile/deps changed.
3. Tests added/updated for the change; coverage not decreased.
4. No secrets, PII, or credentials in the diff.
5. Docs updated (README/run.md/CLAUDE.md/ADR as applicable).
6. Backwards-compatibility and event-contract rules honored (or breaking change + ADR present).

### Review process
- PRs SHOULD be small and single-purpose. Large diffs MUST be split where practical.
- PR description MUST state: what changed, why, risk/impact (esp. to core & contracts), and how it
  was tested.
- Use the `code-review` skill for correctness/security/performance; use `architecture-review` when
  the change affects service boundaries, data flow, or contracts.
- At least one approving review required. The author MUST NOT approve their own PR.
- Merge only after all mandatory checks pass; prefer squash merge with a Conventional-Commit title.
```
