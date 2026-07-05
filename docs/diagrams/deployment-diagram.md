# Deployment Diagram

Runtime topology. Each service ships as its own container image (multi-stage `Dockerfile`, JRE 21
runtime). Locally they are orchestrated with `docker-compose`; in production the same images run on
a container platform (e.g. Kubernetes). Configuration and secrets are injected via environment
variables — never baked into images.

## 1. Container topology

```mermaid
flowchart TB
    subgraph clients["Clients"]
        user["User / Channel<br/>(HTTPS)"]
        mcpc["MCP client<br/>(SSE)"]
        analyst["Analyst / Ops"]
    end

    subgraph platform["Container platform — Docker Compose / Kubernetes"]
        subgraph edge["Edge"]
            gw["api-gateway<br/>:8080"]
        end

        subgraph apps["Application services (stateless, independently deployable)"]
            cb["core-banking<br/>:8081"]
            pc["product-config<br/>:8082"]
            no["notification<br/>:8083"]
            re["reporting<br/>:8084"]
            au["audit<br/>:8085"]
            tp["third-party<br/>:8086"]
            mcp["mcp-server<br/>:8087"]
            am["ai-monitor<br/>:8088"]
        end

        subgraph infra["Stateful infrastructure"]
            kafka[("Kafka KRaft<br/>:9092")]
            pg[("PostgreSQL<br/>:5432")]
            kui["Kafka UI<br/>:8090"]
        end
    end

    ext["Anthropic Claude API<br/>(external, TLS)"]

    user --> gw
    analyst --> gw
    mcpc --> mcp
    gw --> cb & pc & no & re & au & tp
    cb --> pg
    cb --> kafka
    kafka --> no & re & au & am
    mcp --> cb
    am -.->|"ANTHROPIC_API_KEY set"| ext
    kui --- kafka
```

## 2. Health, config & scaling

```mermaid
flowchart LR
    subgraph pod["Service container (representative)"]
        app["Spring Boot app"]
        health["/actuator/health"]
    end
    cfgmap["ConfigMap / env<br/>(SPRING_*, *_URI, ports)"]
    secret["Secret<br/>(DB creds, ANTHROPIC_API_KEY)"]
    probe["Readiness / Liveness probe"]

    cfgmap --> app
    secret --> app
    probe --> health
```

## 3. Deployment notes

- **Images:** built from the repo root context with each `<service>/Dockerfile`
  (`mvn -pl <service> -am -DskipTests package` → `eclipse-temurin:21-jre`). CI builds one image per
  service via a matrix.
- **Stateless services** (`gw`, `cb`, `pc`, `no`, `re`, `au`, `tp`, `mcp`, `am`) scale horizontally.
  The consumers scale within their Kafka consumer group; the core scales behind the gateway (DB
  locking preserves correctness under concurrency).
- **Stateful infra** (Kafka, PostgreSQL) is external/managed; connection details come from env
  (`SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_DATASOURCE_*`).
- **Configuration:** non-secret via ConfigMap/env; secrets via a secrets manager / Kubernetes
  Secret. See [`.claude/rules/rules.md`](../../.claude/rules/rules.md) (Security Rules).
- **Probes:** readiness/liveness on `/actuator/health`; readiness should also reflect dependency
  reachability where appropriate.
- **Rollout:** deploy per service with immutable image tags (git SHA) and a rollback to the prior tag;
  verify health and Kafka consumer-group lag after rollout.
```
