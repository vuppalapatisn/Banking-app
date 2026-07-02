# Build & Run Guide — Banking-app

Step-by-step instructions to build, run and exercise the Core Banking platform locally.

---

## 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker + Compose | recent | `docker version` / `docker compose version` |
| curl | any | `curl --version` |

> The repo also ships per-service `Dockerfile`s, so Docker alone is enough if you prefer not to install JDK/Maven.

---

## 2. Get the code

```bash
git clone https://github.com/vuppalapatisn/Banking-app.git
cd Banking-app
```

---

## 3. Build

The project is a single Maven reactor (parent `pom.xml` + 10 modules).

```bash
# compile, run unit tests and package every module
mvn clean verify
```

Artifacts are produced at `<module>/target/<module>-1.0.0.jar`.

Useful variants:

```bash
mvn -DskipTests package                 # skip tests
mvn -pl core-banking-service -am package # build one module + its dependencies
```

### Behind a corporate proxy (Zscaler / TLS inspection)

If dependency downloads fail with **PKIX / certificate** errors or **HTTP 403** from
`repo.maven.apache.org`, use the OS trust store and (if needed) point Maven at your
internal mirror:

```bash
# make the JVM trust the corporate root CA from the Windows store
export MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT"   # PowerShell: $env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT"
mvn clean verify
```

If Maven Central `.jar` downloads are blocked outright, configure an internal
Nexus/Artifactory mirror in `~/.m2/settings.xml`:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>corp-nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>https://your-nexus/repository/maven-public/</url>
    </mirror>
  </mirrors>
</settings>
```

> The GitHub Actions CI pipeline (`.github/workflows/ci.yml`) builds and tests every
> module on each push using unrestricted Maven Central access — use it as the source
> of truth if your local network blocks downloads.

---

## 4. Start infrastructure (Kafka + PostgreSQL)

```bash
docker compose up -d
```

This starts:

| Service | Address | Notes |
|---|---|---|
| PostgreSQL | `localhost:5432` | db `banking`, user/pass `banking`/`banking` |
| Kafka (KRaft) | `localhost:9092` | topics auto-created |
| Kafka UI | http://localhost:8090 | browse topics & messages |

Check status: `docker compose ps` · Stop later: `docker compose down` (add `-v` to wipe data).

---

## 5. Run the services

Each service is an independent Spring Boot app. The **core-banking-service** is the
producer; the consumers (notification, reporting, audit, ai-monitor) can start in any
order. The **api-gateway** should start after the services it routes to.

### Option A — run from source (dev)

Open a terminal per service (or use `&`). Example for the core:

```bash
# H2 in-memory (no Postgres needed):
mvn -pl core-banking-service spring-boot:run

# OR against Postgres + Kafka from docker-compose:
SPRING_PROFILES_ACTIVE=postgres SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  mvn -pl core-banking-service spring-boot:run
```

Repeat for the other modules:

```bash
mvn -pl product-config-service        spring-boot:run
mvn -pl notification-service          spring-boot:run
mvn -pl reporting-analytics-service   spring-boot:run
mvn -pl audit-logging-service         spring-boot:run
mvn -pl third-party-integration-service spring-boot:run
mvn -pl mcp-server                    spring-boot:run
mvn -pl ai-monitor-service            spring-boot:run
mvn -pl api-gateway                   spring-boot:run
```

### Option B — run the packaged jars

```bash
mvn -DskipTests package
java -jar core-banking-service/target/core-banking-service-1.0.0.jar
# ...one java -jar per service
```

### Option C — run as Docker containers

```bash
# build an image (context is the repo root)
docker build -f core-banking-service/Dockerfile -t banking/core-banking-service .
docker run --rm -p 8081:8081 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  banking/core-banking-service
```

### Ports

| Service | Port | Health |
|---|---|---|
| api-gateway | 8080 | http://localhost:8080/actuator/health |
| core-banking-service | 8081 | http://localhost:8081/actuator/health |
| product-config-service | 8082 | http://localhost:8082/actuator/health |
| notification-service | 8083 | http://localhost:8083/actuator/health |
| reporting-analytics-service | 8084 | http://localhost:8084/actuator/health |
| audit-logging-service | 8085 | http://localhost:8085/actuator/health |
| third-party-integration-service | 8086 | http://localhost:8086/actuator/health |
| mcp-server | 8087 | http://localhost:8087/actuator/health |
| ai-monitor-service | 8088 | http://localhost:8088/actuator/health |

---

## 6. End-to-end smoke test

All calls go through the gateway on port **8080** (or hit the core directly on 8081).

```bash
# 1) Open an account — returns a generated NUBAN
curl -s -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","accountType":"SAVINGS"}'
#   note the "accountNumber" (NUBAN) in the response

# 2) Credit it (deposit)
curl -s -X POST http://localhost:8080/api/transactions/credit \
  -H 'Content-Type: application/json' \
  -d '{"accountNumber":"<NUBAN>","amount":50000,"narration":"opening deposit"}'

# 3) Check balance
curl -s http://localhost:8080/api/accounts/<NUBAN>/balance

# 4) Debit (withdrawal)
curl -s -X POST http://localhost:8080/api/transactions/debit \
  -H 'Content-Type: application/json' \
  -d '{"accountNumber":"<NUBAN>","amount":2000,"narration":"atm"}'

# 5) Transaction history
curl -s http://localhost:8080/api/transactions/account/<NUBAN>
```

Each transaction posts a balanced double-entry journal, updates the balance in one ACID
transaction, and publishes a `TransactionEvent`. Verify the downstream (event-driven) side:

```bash
curl -s http://localhost:8080/api/notifications          # notifications "sent"
curl -s http://localhost:8080/api/reports/summary        # aggregated totals
curl -s http://localhost:8080/api/audit                  # immutable audit trail
curl -s http://localhost:8088/api/monitor/anomalies      # AI-monitor detections
```

Third-party (simulated) integrations:

```bash
curl -s -X POST http://localhost:8080/api/third-party/kyc/verify \
  -H 'Content-Type: application/json' \
  -d '{"bvn":"22212345678","firstName":"Ada","lastName":"Lovelace"}'
```

---

## 7. AI components

### MCP server (port 8087)

Exposes banking tools (`getAccountBalance`, `listRecentTransactions`, `getProductCatalog`)
to MCP clients over WebMVC SSE at `http://localhost:8087/sse`. Example Claude Desktop
client config:

```json
{
  "mcpServers": {
    "banking": { "url": "http://localhost:8087/sse" }
  }
}
```

### AI monitor (port 8088)

Rule-based anomaly detection runs automatically on the transaction stream. To enable
Claude-generated risk summaries, set an API key before starting the service:

```bash
export ANTHROPIC_API_KEY=sk-ant-...          # PowerShell: $env:ANTHROPIC_API_KEY="sk-ant-..."
mvn -pl ai-monitor-service spring-boot:run

curl -s -X POST http://localhost:8088/api/monitor/analyze \
  -H 'Content-Type: application/json' -d '{"accountNumber":"<NUBAN>"}'
```

Without a key it returns a deterministic heuristic summary.

---

## 8. Handy extras

- **H2 console** (core service): http://localhost:8081/h2-console — JDBC URL `jdbc:h2:mem:corebanking`, user `sa`, empty password.
- **Kafka UI**: http://localhost:8090
- **Actuator health** for any service: `GET /actuator/health`

---

## 9. Stop everything

```bash
# stop each spring-boot:run / java -jar with Ctrl+C, then:
docker compose down          # add -v to also delete Postgres/Kafka volumes
```
