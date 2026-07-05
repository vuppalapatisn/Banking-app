# Design / Component Diagram

Internal design: Maven module dependencies, the layered pattern every service follows, and the
component breakdown of the transactional core.

## 1. Module dependency graph

All business services depend on the shared `common` contracts module (event records + topic names).
`api-gateway` is pure routing and has no `common` dependency.

```mermaid
flowchart TD
    common["common<br/>(event contracts + topics)"]

    cb["core-banking-service"]
    pc["product-config-service"]
    no["notification-service"]
    re["reporting-analytics-service"]
    au["audit-logging-service"]
    tp["third-party-integration-service"]
    mcp["mcp-server"]
    am["ai-monitor-service"]
    gw["api-gateway<br/>(no common dep)"]

    cb --> common
    pc --> common
    no --> common
    re --> common
    au --> common
    tp --> common
    mcp --> common
    am --> common
```

## 2. Layered architecture (per service)

Each service follows package-by-layer with strict inward dependencies and constructor injection.

```mermaid
flowchart TD
    web["web/ — REST controllers<br/>(@RestController, @Valid, ProblemDetail)"]
    messaging["messaging/ — Kafka listeners<br/>(@KafkaListener) — consumers only"]
    service["service/ — business logic<br/>(@Service, @Transactional)"]
    repository["repository/ — Spring Data JPA"]
    domain["domain/ — JPA entities + enums"]
    dto["dto/ — request/response records"]
    config["config/ — beans, Kafka factories, seeders"]

    web --> service
    messaging --> service
    web --> dto
    service --> repository
    service --> domain
    repository --> domain
    config -.-> service
```

## 3. Core-banking-service component breakdown

The heart of the system. Controllers stay thin; the "stored-procedure" processes live in services;
money only changes through the journal engine on pessimistically-locked accounts, and events are
published **after** commit.

```mermaid
flowchart TD
    subgraph web["web"]
        ac["AccountController"]
        tc["TransactionController"]
    end

    subgraph svc["service"]
        as["AccountService"]
        ts["TransactionService<br/>(credit / debit / transfer, @Transactional)"]
        nuban["NubanGenerationService<br/>(CBN check digit)"]
        je["JournalEntryEngine<br/>(double-entry DR/CR)"]
        ep["EventPublisher<br/>(fire-and-forget)"]
    end

    subgraph repo["repository"]
        ar[("AccountRepository<br/>(pessimistic write lock)")]
        tr[("TransactionRepository")]
        lr[("LedgerEntryRepository")]
        cr[("CustomerRepository")]
    end

    pg[("PostgreSQL / H2")]
    kafka{{"Kafka"}}

    ac --> as
    tc --> ts
    as --> nuban
    as --> ar
    as --> cr
    as --> ep
    ts --> ar
    ts --> je
    ts --> tr
    ts --> ep
    je --> lr
    ar --> pg
    tr --> pg
    lr --> pg
    cr --> pg
    ep -. publish .-> kafka
```

**Key invariants** (see [`CLAUDE.md`](../../CLAUDE.md) Forbidden Changes):
money is `BigDecimal`; money paths are `@Transactional`; accounts are locked in deterministic order;
every posting is a balanced DR/CR; publishing never blocks or rolls back a committed transaction.
