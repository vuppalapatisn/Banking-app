# Banking-app — Core Banking Architecture (Spring Boot)

A reference implementation of the **"Core Banking – Stored Procedures & Events Work Together"** architecture:
a highly transactional (ACID) core surrounded by an event-driven integration layer, fronted by an API
gateway, with an MCP server and an AI-based transaction monitor.

```
 Channels ──▶ API Gateway ──▶ Core Banking (ACID, stored-procedure style) ──▶ Core DB (source of truth)
                                        │ publishes events
                                        ▼
                             Event Bus (Kafka)
                                        │
     ┌───────────────┬────────────────┼────────────────┬───────────────┐
     ▼               ▼                ▼                ▼               ▼
 Notification     Reporting &        Audit &        AI Monitor      (Product & Config,
   Service         Analytics        Logging         (anomaly +        Third-party
                                                    Claude summary)   integrations)
```

## Modules

| Module | Port | Role in the diagram |
|---|---|---|
| [`api-gateway`](api-gateway) | 8080 | API Gateway / Load Balancer — routes channel traffic to services |
| [`core-banking-service`](core-banking-service) | 8081 | Transactional (ACID) core: credit, debit, transfer, NUBAN, journal engine, real-time balance |
| [`product-config-service`](product-config-service) | 8082 | Product & Configuration: interest rates, charges, limits & rules |
| [`notification-service`](notification-service) | 8083 | Notification: SMS / Email / Push (consumes events) |
| [`reporting-analytics-service`](reporting-analytics-service) | 8084 | Reporting & Analytics: daily aggregates, dashboards (consumes events) |
| [`audit-logging-service`](audit-logging-service) | 8085 | Audit & Logging: immutable audit trail, compliance logs (consumes events) |
| [`third-party-integration-service`](third-party-integration-service) | 8086 | Third-party: payment switch, BVN/NIN/KYC (simulated) |
| [`mcp-server`](mcp-server) | 8087 | Spring AI **MCP server** exposing banking tools to LLM clients |
| [`ai-monitor-service`](ai-monitor-service) | 8088 | **AI-based monitoring**: rule anomaly detection + Claude summaries |
| [`common`](common) | — | Shared event contracts (`TransactionEvent`, `AccountEvent`), topic names |

Synchronous request/response is drawn as solid arrows (channels → gateway → core); asynchronous
event-driven flows are dashed (core → Kafka → downstream services), exactly as in the architecture diagram.

## Tech stack

- Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0 (Gateway), Spring AI 1.0.0 (MCP + Anthropic)
- Apache Kafka (KRaft) as the event bus, PostgreSQL as the core database (H2 for local/dev & tests)
- Maven multi-module build, Docker images per service, GitHub Actions CI

## Quick start

### 1. Infrastructure (Kafka + PostgreSQL + Kafka UI)

```bash
docker compose up -d
```

Kafka UI: http://localhost:8090 · PostgreSQL: `localhost:5432` (`banking`/`banking`).

### 2. Build

```bash
mvn -B clean verify
```

> Corporate networks that block Maven Central `.jar` downloads (e.g. TLS-inspection proxies) cannot
> build locally — the GitHub Actions CI pipeline builds and tests every module on each push.

### 3. Run a service

```bash
# core banking against Postgres + Kafka
SPRING_PROFILES_ACTIVE=postgres SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  mvn -pl core-banking-service spring-boot:run
```

### 4. Try it end-to-end

```bash
# open an account (through the gateway)
curl -X POST http://localhost:8080/api/accounts -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","accountType":"SAVINGS"}'

# credit it, then check the balance
curl -X POST http://localhost:8080/api/transactions/credit -H 'Content-Type: application/json' \
  -d '{"accountNumber":"<NUBAN>","amount":50000,"narration":"opening deposit"}'
curl http://localhost:8080/api/accounts/<NUBAN>/balance
```

The credit produces a double-entry journal posting, updates the balance in one ACID transaction, and
publishes a `TransactionEvent` that the notification, reporting, audit and AI-monitor services consume.

## AI components

- **`mcp-server`** — a Model Context Protocol server (Spring AI, WebMVC SSE) exposing tools such as
  `getAccountBalance`, `listRecentTransactions` and `getProductCatalog` to LLM clients (e.g. Claude Desktop).
- **`ai-monitor-service`** — consumes the transaction stream, flags anomalies (large amount / velocity),
  and — when `ANTHROPIC_API_KEY` is set — uses Claude to generate natural-language risk summaries.

## CI/CD

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) builds & tests all modules on JDK 21 and builds a
Docker image per service on push.
