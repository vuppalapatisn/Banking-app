# reporting-analytics-service

Reporting & Analytics Service for the Core Banking platform. Produces daily reports,
dashboards and regulatory reports from committed banking activity.

- **Port:** 8084
- **Package:** `com.banking.reporting`

## Event consumption

Consumes `TransactionEvent` messages from the Kafka topic `banking.transactions`
(consumer group `reporting-analytics-service`). Each event is folded into a
`DailyAggregate` keyed by `(date, account, type)` tracking the transaction count and
the running total amount. Aggregates are persisted to an in-memory H2 database
(`jdbc:h2:mem:reporting`).

The service starts cleanly without a Kafka broker present; the listener container
retries the connection in the background.

## REST API

| Method | Path                          | Description                              |
|--------|-------------------------------|------------------------------------------|
| GET    | `/api/reports/daily?date=YYYY-MM-DD` | All aggregate buckets for a date  |
| GET    | `/api/reports/summary`        | Totals grouped by transaction type       |
| GET    | `/api/reports/account/{acct}` | All aggregate buckets for an account     |

## Configuration

| Property | Env var | Default |
|----------|---------|---------|
| `spring.kafka.bootstrap-servers` | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |

Actuator endpoints `health`, `info`, `metrics` are exposed under `/actuator`.

## Build & run

```bash
mvn -B -ntp -pl reporting-analytics-service -am -DskipTests package
java -jar reporting-analytics-service/target/*.jar
```
