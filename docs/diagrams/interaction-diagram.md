# Interaction (Sequence) Diagrams

Step-by-step behavior of the key flows. Synchronous calls are solid; asynchronous event delivery is
shown crossing the Kafka boundary.

## 1. Open account (NUBAN generation)

```mermaid
sequenceDiagram
    actor Client
    participant GW as api-gateway
    participant CB as core-banking-service
    participant NUB as NubanGenerationService
    participant DB as PostgreSQL
    participant K as Kafka

    Client->>GW: POST /api/accounts {name, type}
    GW->>CB: forward
    CB->>DB: save Customer (@Transactional)
    CB->>NUB: generate()
    NUB->>DB: check NUBAN uniqueness
    NUB-->>CB: 10-digit NUBAN (CBN check digit)
    CB->>DB: save Account
    CB-->>K: publish AccountEvent(CREATED) [after commit, async]
    CB-->>GW: 201 {accountNumber, ...}
    GW-->>Client: 201 Created
    K-->>+notification-service: AccountEvent → welcome notification
    K-->>+audit-logging-service: AccountEvent → audit record
```

## 2. Credit (deposit) with downstream fan-out

```mermaid
sequenceDiagram
    actor Client
    participant GW as api-gateway
    participant CB as core-banking-service
    participant JE as JournalEntryEngine
    participant DB as PostgreSQL
    participant K as Kafka
    participant NO as notification-service
    participant RE as reporting-analytics-service
    participant AU as audit-logging-service
    participant AM as ai-monitor-service

    Client->>GW: POST /api/transactions/credit {account, amount}
    GW->>CB: forward
    Note over CB,DB: BEGIN @Transactional
    CB->>DB: lock GL cash + customer account (FOR UPDATE, sorted order)
    CB->>JE: post(DR cash, CR customer, amount)
    JE->>DB: write balanced DR/CR ledger entries + update balances
    CB->>DB: save TransactionRecord (POSTED)
    Note over CB,DB: COMMIT
    CB-->>K: publish TransactionEvent [fire-and-forget]
    CB-->>GW: 200 {transactionId, ...}
    GW-->>Client: 200 OK
    par asynchronous
        K-->>NO: send SMS + email (simulated)
    and
        K-->>RE: update daily aggregates
    and
        K-->>AU: append immutable audit record
    and
        K-->>AM: run anomaly rules (large amount / velocity)
    end
```

## 3. Fund transfer (deadlock-safe locking)

```mermaid
sequenceDiagram
    actor Client
    participant GW as api-gateway
    participant CB as core-banking-service
    participant JE as JournalEntryEngine
    participant DB as PostgreSQL
    participant K as Kafka

    Client->>GW: POST /api/transactions/transfer {source, dest, amount}
    GW->>CB: forward
    Note over CB: reject if source == dest
    Note over CB,DB: BEGIN @Transactional
    CB->>DB: lock source & dest accounts in sorted order (deadlock-safe)
    CB->>CB: validate ACTIVE + sufficient funds on source
    CB->>JE: post(DR source, CR dest, amount)
    JE->>DB: balanced DR/CR ledger entries + balance updates
    CB->>DB: save TransactionRecord (POSTED)
    Note over CB,DB: COMMIT
    CB-->>K: publish TransactionEvent [after commit]
    CB-->>GW: 200 {transactionId}
    GW-->>Client: 200 OK
```

## 4. MCP tool call (AI layer, read-only)

```mermaid
sequenceDiagram
    participant LLM as MCP Client (Claude Desktop)
    participant MCP as mcp-server
    participant CB as core-banking-service

    LLM->>MCP: SSE connect / list tools
    MCP-->>LLM: getAccountBalance, listRecentTransactions, getProductCatalog
    LLM->>MCP: call getAccountBalance(nuban)
    MCP->>CB: GET /api/accounts/{nuban}/balance (RestClient)
    alt core reachable
        CB-->>MCP: balance
    else core unavailable
        MCP-->>MCP: simulated fallback value
    end
    MCP-->>LLM: tool result (read-only; no money movement)
```

## 5. AI risk analysis (optional Claude)

```mermaid
sequenceDiagram
    actor Analyst
    participant AM as ai-monitor-service
    participant ANTH as Anthropic Claude API

    Analyst->>AM: POST /api/monitor/analyze {accountNumber}
    AM->>AM: collect detected anomalies for account
    alt ANTHROPIC_API_KEY present
        AM->>ANTH: prompt(risk summary)
        ANTH-->>AM: natural-language summary
    else no key / error
        AM->>AM: deterministic heuristic summary
    end
    AM-->>Analyst: {anomalyCount, summary}
```
