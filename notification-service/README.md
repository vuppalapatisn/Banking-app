# notification-service

Notification Service for the Banking-app event-driven layer. Handles simulated SMS /
Email / Push notifications and statements.

## Responsibilities
- Consumes `TransactionEvent` from `banking.transactions` and `AccountEvent` from
  `banking.accounts`.
- On a transaction event: creates an **SMS** and an **EMAIL** notification (dispatch simulated via logging).
- On an account `CREATED` event: sends a welcome notification.
- Persists every notification to an in-memory H2 database (`jdbc:h2:mem:notifications`).

## Port
- HTTP: **8083**

## REST API
| Method | Path                              | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/api/notifications`              | All notifications (newest first)   |
| GET    | `/api/notifications/account/{acct}` | Notifications for a given account |

Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`.

## Kafka
Two dedicated `ConcurrentKafkaListenerContainerFactory` beans bind a `JsonDeserializer`
to each concrete event type, avoiding deserialization ambiguity. Configure the broker via
`SPRING_KAFKA_BOOTSTRAP_SERVERS` (defaults to `localhost:9092`). The app starts even when
no broker is running.

## Build & run
```
mvn -B -ntp -pl notification-service -am -DskipTests package
java -jar notification-service/target/*.jar
```
