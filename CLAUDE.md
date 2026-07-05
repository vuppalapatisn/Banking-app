# CLAUDE.md

Guidance for Claude Code (and human engineers) working in the **Banking-app** repository.
Read this file fully before making changes. It is the source of truth for how this repo is
structured, built, tested, and safely extended.

---

## Project Overview

### Purpose
Banking-app is a reference implementation of a **core banking architecture**: a highly
transactional (ACID) core surrounded by an event-driven integration layer, fronted by an API
gateway, and augmented with an MCP server and an AI-based transaction monitor. It demonstrates
how *stored-procedure-style* transactional processing and *event-driven* downstream services work
together — the pattern in `architecture-diagram` / the project README.

### Business goals
- Move money **correctly and atomically** (no lost updates, no partial postings) — correctness
  outranks throughput.
- Keep the **core small and trusted**; push non-critical work (notifications, analytics, audit,
  integrations) to **decoupled** services that react to events.
- Provide a **single source of truth** (the core ledger) while enabling real-time downstream value.
- Show **AI augmentation** (MCP tools + anomaly monitoring) layered on without touching the core.

### Architecture summary
- **Synchronous edge:** Channels → `api-gateway` → `core-banking-service` (and other services).
- **Transactional core:** `core-banking-service` performs credit / debit / transfer / NUBAN
  generation / double-entry journal posting / real-time balance update inside a single DB
  transaction using **pessimistic write locks** taken in a **deterministic order** (deadlock-safe).
- **Event bus:** after a transaction commits, the core publishes `TransactionEvent` / `AccountEvent`
  to **Kafka**. Publishing is **fire-and-forget** — a broker outage never rolls back committed money.
- **Event-driven periphery:** `notification-service`, `reporting-analytics-service`,
  `audit-logging-service`, and `ai-monitor-service` **consume** events and maintain their own state.
- **Shared contracts:** the `common` module holds the event records and topic names so producers and
  consumers can never drift.
- **AI layer:** `mcp-server` exposes read-only banking tools to LLM clients; `ai-monitor-service`
  applies anomaly rules and (optionally) uses Claude for natural-language risk summaries.

```
Channels ─▶ api-gateway ─▶ core-banking-service ─▶ Core DB (source of truth)
                                  │ publish (async, after commit)
                                  ▼
                            Kafka event bus
        ┌──────────────┬─────────┼──────────┬───────────────┐
        ▼              ▼         ▼          ▼               ▼
  notification     reporting   audit     ai-monitor   (product-config,
                                                       third-party — sync)
```

---

## Technology Stack

### Languages
- **Java 21** (records, pattern matching, virtual-thread-ready). No other JVM languages.

### Frameworks & libraries
- **Spring Boot 3.4.1** (parent BOM pins all versions — do not hard-code dependency versions).
- **Spring Cloud 2024.0.0** — `spring-cloud-starter-gateway` (reactive/WebFlux gateway).
- **Spring AI 1.0.0** — `spring-ai-starter-mcp-server-webmvc`, `spring-ai-starter-model-anthropic`.
- **Spring Kafka** (producers/consumers), **Spring Data JPA + Hibernate**, **Jakarta Validation**.
- **JUnit 5 + AssertJ + Spring Boot Test**.

### Infrastructure
- **Apache Kafka** (KRaft mode, no ZooKeeper) — the event bus.
- **PostgreSQL 16** in production/local; **H2 (in-memory)** for dev default and tests.
- **Docker** (multi-stage image per service) + **docker-compose** (Kafka, Postgres, Kafka UI).
- **GitHub Actions** CI (build/test + per-service image build).

---

## Repository Structure

```
Banking-app/
├── pom.xml                         # Parent reactor POM: Java/Spring versions, module list, BOM imports
├── docker-compose.yml              # Local infra: Kafka (KRaft), PostgreSQL, Kafka UI
├── .dockerignore                   # Keeps target/, .git/, docs out of Docker build context
├── README.md                       # High-level overview + module table
├── run.md                          # Step-by-step build & run guide
├── LICENSE                         # MIT
├── .github/workflows/ci.yml        # CI: mvn clean verify + Docker image matrix
├── CLAUDE.md                       # THIS FILE — agent & engineer guidance
├── .claude/
│   ├── rules/rules.md              # Non-negotiable rules for agents (security, testing, PR, CI/CD)
│   └── skills/<skill>/SKILL.md     # Reusable task playbooks (architecture-review, code-review, etc.)
├── docs/
│   ├── GOVERNANCE.md               # SemVer, ADR process, deps, breaking changes, deprecation, migration
│   └── adr/                        # Architecture Decision Records (+ template)
│
├── common/                         # SHARED CONTRACTS — Kafka event records + topic names. No Spring deps.
│   └── src/main/java/com/banking/common/event/
│
├── core-banking-service/           # THE ACID CORE (port 8081). Change here with extreme care.
│   └── src/main/java/com/banking/core/
│       ├── domain/                 # JPA entities (Account, Customer, LedgerEntry, TransactionRecord) + enums
│       ├── repository/             # Spring Data JPA repos (incl. pessimistic-lock finder)
│       ├── service/                # Stored-procedure-style processes: Transaction/Account/Nuban/Journal/EventPublisher
│       ├── web/                    # REST controllers (accounts, transactions)
│       ├── dto/                    # Request/response records
│       ├── exception/              # Domain exceptions + @RestControllerAdvice
│       └── config/                 # DataSeeder (GL cash account), etc.
│
├── api-gateway/                    # Spring Cloud Gateway (port 8080). Route config only — no business logic.
├── product-config-service/         # Products, rates, fees, limits (port 8082, JPA)
├── notification-service/           # SMS/Email/Push, event consumer (port 8083, JPA)
├── reporting-analytics-service/    # Daily aggregates, event consumer (port 8084, JPA)
├── audit-logging-service/          # Immutable audit trail, event consumer (port 8085, JPA)
├── third-party-integration-service/# Payment switch, BVN/NIN/KYC — simulated (port 8086)
├── mcp-server/                     # Spring AI MCP server exposing banking tools (port 8087)
└── ai-monitor-service/             # Anomaly rules + optional Claude summaries, event consumer (port 8088)
```

**Per-service layout (all services follow this):** `pom.xml`, `Dockerfile`, `README.md`,
`src/main/java/com/banking/<pkg>/…`, `src/main/resources/application.yml`,
`src/test/java/com/banking/<pkg>/…ApplicationTests.java`.

---

## Build Commands

```bash
mvn clean verify                       # Build, test and package every module (the canonical build)
mvn -DskipTests package                # Package without tests
mvn -pl core-banking-service -am package   # Build one module + its dependencies
mvn -pl common install                 # Install shared contracts to local .m2 (rarely needed; reactor handles it)
```

> **Corporate proxy note:** if dependency downloads fail with PKIX/403 errors, export
> `MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT"` and/or configure an internal Maven
> mirror in `~/.m2/settings.xml`. Never bypass TLS verification (`-k`, `<insecure>true`). The CI
> pipeline is the authoritative build when local networks block Maven Central.

## Test Commands

```bash
mvn test                               # Unit + slice tests across all modules
mvn -pl core-banking-service test      # Tests for a single module
mvn -pl core-banking-service test -Dtest=NubanGenerationServiceTest   # One test class
```

## Local Development

```bash
docker compose up -d                   # Start Kafka, PostgreSQL, Kafka UI (http://localhost:8090)
# Run a service from source (H2 default):
mvn -pl core-banking-service spring-boot:run
# Or against Postgres + Kafka:
SPRING_PROFILES_ACTIVE=postgres SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  mvn -pl core-banking-service spring-boot:run
```
See `run.md` for the full end-to-end walkthrough (open account → credit → transfer → observe events).
Config is environment-overridable; sensible `localhost` defaults are baked into each `application.yml`.

## Deployment Process

1. Merge to `main` → GitHub Actions runs `mvn clean verify` for all modules.
2. On success, CI builds a Docker image per service (`<service>/Dockerfile`, root build context).
3. Images are deployed to the target runtime (Kubernetes/cloud). Each service is independently
   deployable; configuration is injected via environment variables (`SPRING_*`, `*_URI`,
   `ANTHROPIC_API_KEY`, datasource creds) — **never** baked into images.
4. Database schema is currently Hibernate `ddl-auto=update` for dev; production should adopt a
   managed migration tool (Flyway/Liquibase) — see `docs/GOVERNANCE.md` (Migration strategy).

---

## Coding Standards

Apply these consistently. Match the surrounding code's style over personal preference.

- **SOLID**
  - *Single Responsibility:* one process per service class (e.g. `NubanGenerationService`,
    `JournalEntryEngine`). Controllers stay thin; business rules live in services.
  - *Open/Closed:* extend behavior via new services/consumers, not by editing the core's money paths.
  - *Liskov / Interface Segregation:* keep repository and provider interfaces focused.
  - *Dependency Inversion:* depend on Spring-managed abstractions; **constructor injection only**
    (no field `@Autowired`).
- **Clean Code** — intention-revealing names, small methods, no dead code, meaningful Javadoc on
  domain/service classes explaining *why*, not *what*.
- **DRY** — shared event/DTO contracts live in `common`; do not copy event records into services.
- **KISS** — prefer the simplest correct solution. The existing anomaly detection and simulated
  integrations are intentionally simple; don't over-engineer.
- **YAGNI** — don't add frameworks, caches, or abstraction layers speculatively. Add them when a
  concrete requirement exists (and record it as an ADR).
- **Secure by Default** — no secrets in code or config; validate all external input (`@Valid`);
  least-privilege DB users; money as `BigDecimal` (never `double`/`float`); fail closed.

---

## Agent Guidance

### How Claude agents should work here
1. **Read before writing:** open `CLAUDE.md`, `.claude/rules/rules.md`, the target module's
   `README.md`, and the relevant `application.yml` before editing.
2. **Respect the boundary between core and periphery.** The `core-banking-service` money paths
   (`TransactionService`, `JournalEntryEngine`, locking, balance math) are the highest-risk code in
   the repo. Changes there require tests and, for behavior changes, an ADR.
3. **Contracts change deliberately.** Editing `common` event records affects every producer and
   consumer — treat it as a breaking change (see Governance) and update all sides + tests.
4. **Prefer additive change.** New feature → new service or new event consumer, not a modification
   to the core transaction, when possible.
5. **Keep configuration externalized** with localhost defaults; never hard-code hostnames or secrets.

### How to perform changes safely
- Work module-by-module; build the touched module with `-am` before committing.
- Keep double-entry invariant intact: every posting has an equal DR and CR; balances only change
  through `JournalEntryEngine` under a locked account.
- Preserve deterministic lock ordering (`lockPair` sorts by account number) — never lock accounts
  in request order.
- Maintain "publish after commit, fire-and-forget" — do not make Kafka publishing block or
  participate in the DB transaction such that a broker outage rolls back money.
- Preserve environment-variable overrides and existing default ports.

### How to avoid regressions
- Run `mvn -pl <module> -am verify` for every touched module; run the full `mvn clean verify`
  before opening a PR.
- Never weaken validation, remove `@Transactional`, or change lock modes without a test proving
  concurrency safety.
- Do not change Kafka topic names, consumer `group-id`s, or event field order/types without a
  migration plan — consumers deserialize by these.

### How to generate tests
- Every service keeps a `@SpringBootTest` `contextLoads` smoke test — keep it green.
- Add focused unit tests for pure logic (e.g. `NubanGenerationServiceTest` for the check-digit).
- For the core, add tests that assert: balance conservation, insufficient-funds rejection,
  double-entry balance, and idempotent/consistent behavior under the locked path.
- Use `@DataJpaTest` for repository queries, `@WebMvcTest` for controller wiring where a full
  context is unnecessary. Prefer AssertJ assertions.

### How to review pull requests
Use the `code-review` skill. At minimum verify: correctness of money math, transaction/locking
integrity, event-contract compatibility, input validation, no secrets, tests added/passing, docs
updated. See `.claude/rules/rules.md` (PR Rules) for mandatory checks.

---

## Forbidden Changes

Do **not** introduce any of the following without an explicit, approved ADR:

- Representing money with `double`/`float` (must be `BigDecimal`), or removing scale/rounding control.
- Removing/weakening `@Transactional` on money paths, or dropping the pessimistic write lock, or
  locking accounts in non-deterministic order.
- Making event publishing synchronous/blocking inside the DB transaction, or making a broker outage
  able to roll back or block a committed transaction.
- Hard-coding secrets, API keys, passwords, or connection strings; logging secrets, full PANs, BVNs,
  or other PII/sensitive data.
- Changing Kafka topic names, consumer group IDs, or the shape of `common` event records without a
  documented migration and updates to all producers/consumers.
- Hard-pinning dependency versions that the Spring Boot/Cloud/AI BOMs already manage.
- Disabling TLS verification, adding `permitAll()`-style blanket auth, or committing `application-*.yml`
  files containing real credentials.
- Adding business logic to `api-gateway` (it is routing/cross-cutting only).
- Introducing a new database, message broker, or framework for a single feature (YAGNI) without an ADR.
- Auto-formatting/reflowing untouched files (creates noisy, risky diffs).
```
