# api-gateway

Reactive **Spring Cloud Gateway** that serves as the single entry point in front of the
Banking-app microservices. Runs on **port 8080**.

## Responsibilities
- Path-based routing to the downstream services.
- Global request logging (method + path) via `LoggingGlobalFilter`.

## Routes
| Path predicate         | Target service (env override)               |
|------------------------|---------------------------------------------|
| `/api/accounts/**`     | `${CORE_BANKING_URI:http://localhost:8081}` |
| `/api/transactions/**` | `${CORE_BANKING_URI:http://localhost:8081}` |
| `/api/products/**`     | `${PRODUCT_URI:http://localhost:8082}`      |
| `/api/notifications/**`| `${NOTIFICATION_URI:http://localhost:8083}` |
| `/api/reports/**`      | `${REPORTING_URI:http://localhost:8084}`    |
| `/api/audit/**`        | `${AUDIT_URI:http://localhost:8085}`        |
| `/api/third-party/**`  | `${THIRDPARTY_URI:http://localhost:8086}`   |

## Actuator
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
