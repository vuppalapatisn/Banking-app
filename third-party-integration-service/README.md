# third-party-integration-service

Third Party Integration Service for the Core Banking platform. Fronts external
systems - payment switch, KYC/BVN/NIN verification and other-bank / inter-bank APIs.

All integrations are **simulated**: no real network calls are made. Responses are
derived deterministically from a UUID so the response shape matches a real adapter.

- **Port:** 8086
- **Package:** `com.banking.thirdparty`

## REST API

| Method | Path | Request body | Response |
|--------|------|--------------|----------|
| POST | `/api/third-party/kyc/verify` | `{ bvn, firstName, lastName }` | `{ verified, reference, matchScore }` |
| POST | `/api/third-party/payment-switch/route` | `{ cardPan, amount, currency }` | `{ approved, authCode, rrn }` |
| POST | `/api/third-party/nip/transfer` | `{ sourceAccount, destBank, destAccount, amount }` | `{ status, sessionId }` |
| GET  | `/api/third-party/banks` | - | static list of `{ code, name }` |

Request bodies are validated with `@Valid` (Jakarta Bean Validation); invalid input
returns HTTP 400.

Actuator endpoints `health`, `info`, `metrics` are exposed under `/actuator`.

## Build & run

```bash
mvn -B -ntp -pl third-party-integration-service -am -DskipTests package
java -jar third-party-integration-service/target/*.jar
```
