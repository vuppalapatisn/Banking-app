# Repository Governance

How the Banking-app repository is versioned, evolved, and kept maintainable over the long term
(3+ years). Read alongside [`CLAUDE.md`](../CLAUDE.md) and [`.claude/rules/rules.md`](../.claude/rules/rules.md).

---

## Semantic Versioning

The project version (in the parent `pom.xml`) and any externally published API follow
**SemVer 2.0.0**: `MAJOR.MINOR.PATCH`.

- **MAJOR** — incompatible/breaking changes to a public contract: REST request/response shape,
  Kafka topic names, `common` event record shape, consumer group semantics, or removed endpoints.
- **MINOR** — backwards-compatible functionality: new endpoints, new services/consumers, new
  **nullable** event fields, new optional config.
- **PATCH** — backwards-compatible bug fixes and internal improvements.

Rules:
- Breaking changes require an **ADR** and an explicit call-out in the PR and changelog.
- Prefer additive, backwards-compatible change; a MAJOR bump is a last resort with a migration plan.
- Tag releases (`vMAJOR.MINOR.PATCH`) and keep a changelog (Conventional Commits make this derivable).

---

## ADR Process (Architecture Decision Records)

Significant decisions are recorded as immutable, numbered ADRs under `docs/adr/`.

**When to write an ADR:** anything that changes service boundaries, data flow, event/API contracts,
the datastore or broker, the ACID core's behavior, a cross-cutting framework/library choice, or any
Forbidden Change in `CLAUDE.md`.

**How:**
1. Copy `docs/adr/adr-template.md` to `docs/adr/NNNN-short-title.md` (next number, zero-padded).
2. Fill in Context, Decision, Consequences, Alternatives. One decision per ADR.
3. Set status: `Proposed` → `Accepted` (on merge). To reverse a past decision, add a **new** ADR
   with status `Accepted` and mark the old one `Superseded by NNNN` — never edit accepted history.
4. Link the ADR from the PR.

ADRs are the durable memory of *why* the system is the way it is — future agents should read them
before proposing structural change.

---

## Dependency Upgrades

- Versions are governed by the **Spring Boot / Spring Cloud / Spring AI BOMs**. Upgrade by bumping
  the BOM/parent version, not individual artifacts. Do not hard-code a version the BOM manages.
- Upgrade cadence: patch/minor dependency updates routinely (e.g. monthly or via automated PRs);
  validate with the full `mvn clean verify` and the CI matrix.
- Security-driven upgrades take priority — see the `security` skill for CVE triage.
- Major framework upgrades (e.g. Spring Boot 3.4 → next major, Spring Cloud train, Spring AI) are
  **ADR-worthy**: check the compatibility matrix (Boot ↔ Cloud ↔ AI versions must align), read
  release notes, and migrate in a dedicated PR with tests. Note: the gateway starter and Spring AI
  artifact names have changed across trains — verify coordinates against the target train's BOM.

---

## Breaking Change Management

1. **Avoid if possible** — model the change additively first.
2. If unavoidable: write an ADR, bump MAJOR, and provide a **migration path** (below).
3. For event/API contracts, prefer **parallel-change**: introduce the new field/topic/version,
   migrate producers then consumers (or vice-versa), and remove the old only after all sides move.
4. Communicate: PR description, changelog, and a deprecation window where feasible.
5. Provide tests proving both old and new behavior during the transition.

---

## Deprecation Strategy

- Mark deprecated Java APIs with `@Deprecated` (+ `@deprecated` Javadoc naming the replacement).
- Deprecate config keys/endpoints in docs with the removal target version and the alternative.
- Keep deprecated behavior working for at least one MINOR release before removal in a MAJOR.
- Track deprecations in the changelog and an ADR when they affect contracts.
- Never silently remove a public contract; deprecate → announce → remove.

---

## Migration Strategy

- **Database schema:** dev uses Hibernate `ddl-auto=update`. Before production, adopt a versioned
  migration tool (**Flyway** or **Liquibase**): check in ordered, immutable migration scripts;
  never hand-edit applied migrations; forward-only in production. Record the adoption as an ADR.
- **Event/contract migration:** use parallel-change and additive fields; version a topic
  (`banking.transactions.v2`) only when a truly incompatible change is required, running consumers on
  both during transition.
- **Data backfills:** run idempotent, resumable jobs; validate with reconciliation (e.g. ledger
  DR==CR conservation) before cutover.
- **Service/config migration:** change env-var names additively (support old + new for one release),
  update `docker-compose.yml`, `README.md`, and `run.md` in the same PR.

---

## Ownership & Change Control

- The `core-banking-service` money paths and the `common` contracts are the highest-risk assets;
  changes there require tests, review, and (for behavior/contract changes) an ADR.
- All changes flow through PRs with the mandatory checks in `.claude/rules/rules.md`.
- Governance documents themselves evolve via PR; material changes to rules/governance should be
  noted in an ADR.
```
