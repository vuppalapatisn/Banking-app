# ai-monitor-service

An AI-based observability / anomaly monitor for the core-banking transaction stream
(an "observability agent"). It consumes `TransactionEvent`s from Kafka, applies
rule-based anomaly detection, and can produce natural-language risk summaries with
Anthropic Claude.

- Package: `com.banking.aimonitor`
- Port: `8088`
- Kafka consumer group: `ai-monitor-service`
- Topic consumed: `banking.transactions`

## Anomaly rules

Every incoming transaction is inspected against two rules; matches are stored in an
in-memory concurrent list (no database):

| Rule | Condition |
|------|-----------|
| `LARGE_AMOUNT` | A single transaction with `amount > 1,000,000`. |
| `VELOCITY` | More than 5 transactions for the same account within a 60-second sliding window. |

Each stored `Anomaly` has `{ id, accountNumber, rule, amount, detectedAt, explanation }`.

The Kafka listener uses a dedicated `ConcurrentKafkaListenerContainerFactory` bound to
`TransactionEvent` (trusted package `com.banking.common.event`). The service starts
**without a broker present** — the listener container retries the connection in the
background.

## Claude-generated risk summaries

By default the service runs with a `dummy-key` and uses a deterministic heuristic
summary. Setting a real key enables Claude:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
# optional: export ANTHROPIC_MODEL=claude-sonnet-4-20250514
```

The `AnthropicChatModel` is injected via `ObjectProvider` (optional) and a `ChatClient`
is built lazily only when a real key is present. Any LLM error falls back to the
heuristic summary.

## REST API

| Method & path | Description |
|---------------|-------------|
| `GET /api/monitor/anomalies` | List all detected anomalies (most recent first). |
| `POST /api/monitor/analyze` | Body `{ "accountNumber": "1234567890" }`. Returns a natural-language risk summary — Claude when a real key is set, otherwise a heuristic. |
| `GET /api/monitor/health-summary` | Counts of anomalies grouped by rule. |

Actuator `health`, `info`, and `metrics` are exposed under `/actuator`.

## Running

```bash
mvn -B -ntp -pl ai-monitor-service -am -DskipTests package
java -jar ai-monitor-service/target/*.jar
# or
docker build -t banking-ai-monitor -f ai-monitor-service/Dockerfile .
docker run -p 8088:8088 -e ANTHROPIC_API_KEY=sk-ant-... banking-ai-monitor
```
