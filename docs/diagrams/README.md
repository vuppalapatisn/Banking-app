# Architecture Diagrams

Version-controlled architecture diagrams for **Banking-app**, written in
[Mermaid](https://mermaid.js.org/) so they render directly on GitHub and evolve with the code.

| Diagram | File | Shows |
|---|---|---|
| System Context & Container | this file | Actors, external systems, and the services/containers |
| Design / Component | [design-diagram.md](design-diagram.md) | Module dependencies, layering, core internals |
| Deployment | [deployment-diagram.md](deployment-diagram.md) | Runtime topology (Docker/Kubernetes), infra, config |
| Interaction (Sequence) | [interaction-diagram.md](interaction-diagram.md) | Key end-to-end flows step by step |
| Data Flow (DFD) | [dataflow-diagram.md](dataflow-diagram.md) | How data moves between processes and stores |

> Keep these in sync with the code and with [`docs/adr/`](../adr). A structural change should update
> the relevant diagram in the same PR.

---

## 1. System Context

Who and what interacts with the platform.

```mermaid
flowchart TB
    customer["Customer / Channels<br/>(Mobile, Web, USSD, ATM, Branch, Corporate API)"]
    llm["LLM Client<br/>(Claude Desktop / MCP client)"]
    ops["Operations / Analysts"]

    system["<b>Banking-app</b><br/>Core banking platform<br/>(ACID core + event-driven services)"]

    anthropic["Anthropic Claude API<br/>(external LLM)"]
    thirdparty["Third-party rails<br/>(payment switch, BVN/NIN/KYC) — simulated"]

    customer -->|"HTTPS REST"| system
    llm -->|"MCP over SSE"| system
    ops -->|"dashboards / reports"| system
    system -.->|"optional risk summaries"| anthropic
    system -->|"verify / route"| thirdparty
```

---

## 2. Container View

The deployable units (one Spring Boot service per container) and the shared infrastructure.
Solid = synchronous request/response; dashed = asynchronous events.

```mermaid
flowchart LR
    channels["Channels"]
    llm["MCP client"]

    gw["api-gateway<br/>:8080"]

    subgraph core_group["Transactional core"]
        cb["core-banking-service<br/>:8081"]
        pg[("PostgreSQL")]
    end

    pc["product-config-service<br/>:8082"]
    tp["third-party-integration-service<br/>:8086"]
    mcp["mcp-server<br/>:8087"]

    subgraph consumers["Event-driven services"]
        no["notification-service<br/>:8083"]
        re["reporting-analytics-service<br/>:8084"]
        au["audit-logging-service<br/>:8085"]
        am["ai-monitor-service<br/>:8088"]
    end

    kafka{{"Kafka event bus<br/>banking.transactions / banking.accounts"}}
    anthropic["Anthropic Claude API"]

    channels --> gw
    gw --> cb
    gw --> pc
    gw --> no
    gw --> re
    gw --> au
    gw --> tp
    cb --> pg
    cb -. publish .-> kafka
    kafka -. consume .-> no
    kafka -. consume .-> re
    kafka -. consume .-> au
    kafka -. consume .-> am
    llm -->|SSE| mcp
    mcp -->|RestClient| cb
    am -.->|optional| anthropic
```

**Legend** — `[ ]` service/container · `[( )]` datastore · `{{ }}` message bus ·
solid arrow = synchronous REST · dashed arrow = asynchronous event / optional call.
