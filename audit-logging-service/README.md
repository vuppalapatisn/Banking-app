# audit-logging-service

Audit & Logging Service for the Banking-app event-driven layer. Maintains an **immutable**
audit trail, activity logs and compliance logs.

## Responsibilities
- Consumes **both** `banking.transactions` (`TransactionEvent`) and `banking.accounts`
  (`AccountEvent`).
- Serializes each event to a JSON string via an injected Jackson `ObjectMapper` and persists
  an `AuditRecord` capturing `eventType`, `entityRef` (account / transaction id), the full
  JSON payload, `source` topic and `receivedAt`.
- Records are immutable: the entity has no setters, columns are `updatable = false`, and the
  service exposes no update/delete endpoints.
- Backed by an in-memory H2 database (`jdbc:h2:mem:audit`).

## Port
- HTTP: **8085**

## REST API
| Method | Path                          | Description                                  |
|--------|-------------------------------|----------------------------------------------|
| GET    | `/api/audit?limit=100`        | Most recent audit records (limited)          |
| GET    | `/api/audit/account/{acct}`   | Records for a given account / transaction ref |
| GET    | `/api/audit/type/{eventType}` | Records of a given event type                |

Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`.

## Kafka
Two dedicated `ConcurrentKafkaListenerContainerFactory` beans bind a `JsonDeserializer`
to each concrete event type, avoiding deserialization ambiguity. Configure the broker via
`SPRING_KAFKA_BOOTSTRAP_SERVERS` (defaults to `localhost:9092`). The app starts even when
no broker is running.

## Build & run
```
mvn -B -ntp -pl audit-logging-service -am -DskipTests package
java -jar audit-logging-service/target/*.jar
```
