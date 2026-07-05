# Data Flow Diagram (DFD)

How data moves between external entities, processes, and data stores. Uses Gane–Sarson-style
elements expressed in Mermaid: external entities, processes (services), and data stores.

## Level 0 — Context

```mermaid
flowchart LR
    customer([Customer / Channel])
    llm([MCP / LLM client])
    ext([Third-party rails & Anthropic])

    system["Banking-app platform"]

    customer -->|"account & txn requests"| system
    system -->|"balances, receipts, statements"| customer
    llm -->|"tool calls"| system
    system -->|"tool results (read-only)"| llm
    system -->|"verify / route / risk prompt"| ext
    ext -->|"verification & AI responses"| system
```

## Level 1 — Processes and data stores

```mermaid
flowchart TB
    customer([Customer / Channel])
    analyst([Analyst / Ops])
    llm([MCP client])

    p_gw["P1: Route request<br/>(api-gateway)"]
    p_core["P2: Process transaction<br/>(core-banking)"]
    p_prod["P3: Serve product config<br/>(product-config)"]
    p_notif["P4: Send notification<br/>(notification)"]
    p_rep["P5: Aggregate analytics<br/>(reporting)"]
    p_audit["P6: Record audit<br/>(audit-logging)"]
    p_ai["P7: Detect anomalies<br/>(ai-monitor)"]
    p_mcp["P8: Expose tools<br/>(mcp-server)"]

    ds_ledger[("D1: Ledger<br/>accounts, transactions, ledger_entries, customers")]
    ds_prod[("D2: Products")]
    ds_notif[("D3: Notifications")]
    ds_rep[("D4: Daily aggregates")]
    ds_audit[("D5: Audit trail")]
    ds_anom[("D6: Anomalies (in-memory)")]
    bus{{"Event bus (Kafka)<br/>banking.transactions / banking.accounts"}}

    customer -->|request| p_gw
    p_gw -->|txn/account cmd| p_core
    p_gw -->|product query| p_prod
    p_core <-->|read/write| ds_ledger
    p_prod <-->|read/write| ds_prod
    p_core -->|TransactionEvent / AccountEvent| bus

    bus --> p_notif
    bus --> p_rep
    bus --> p_audit
    bus --> p_ai

    p_notif <--> ds_notif
    p_rep <--> ds_rep
    p_audit --> ds_audit
    p_ai <--> ds_anom

    analyst -->|report query| p_rep
    analyst -->|audit query| p_audit
    analyst -->|analyze request| p_ai
    llm -->|tool call| p_mcp
    p_mcp -->|read balance/txns| p_core
```

## Data classification & handling

| Data | Store / flow | Sensitivity | Handling |
|---|---|---|---|
| Balances, amounts | D1 ledger, events | Financial | `BigDecimal`; changed only via journal engine under lock |
| Account / customer PII (name, email, phone, BVN) | D1 ledger | PII | Least privilege; mask in logs; never log full BVN |
| Ledger entries (DR/CR) | D1 | Financial (immutable) | Append-only; corrections via reversing transactions |
| Audit trail | D5 | Compliance (immutable) | Append-only; no update/delete endpoints |
| Event payloads | Kafka topics | Financial + PII | Trusted-packages deserialization; do not widen consumers |
| Anomalies | D6 (in-memory) | Derived | Ephemeral; rebuilt from the stream |
| API keys / DB creds | env / secrets manager | Secret | Never in code/logs/images; placeholders only in `application.yml` |

See [`.claude/skills/security/SKILL.md`](../../.claude/skills/security/SKILL.md) for the full data
protection and threat-modeling guidance.
```
