# core-banking-service

The highly transactional (ACID) heart of the platform — "everything that touches money lives here".
Implements the stored-procedure-style processes from the architecture diagram:

| Process | Where |
|---|---|
| Credit / Debit / Fund Transfer | `TransactionService` (single DB transaction, pessimistic write lock, deadlock-safe lock ordering) |
| NUBAN Generation | `NubanGenerationService` (CBN 10-digit check-digit algorithm) |
| Journal Entry Engine | `JournalEntryEngine` (balanced double-entry DR/CR postings) |
| Real-time Balance Update | balance mutated on the locked account inside the transaction |
| Publish Events | `EventPublisher` → Kafka topics `banking.transactions`, `banking.accounts` |

Port: **8081**. Database: H2 in-memory by default; activate the `postgres` profile for PostgreSQL.

## REST API

| Method | Path | Description |
|---|---|---|
| POST | `/api/accounts` | Open an account (generates a NUBAN). Body: `{firstName,lastName,email,phone,bvn,accountType,currency}` |
| GET | `/api/accounts/{nuban}` | Account details |
| GET | `/api/accounts/{nuban}/balance` | Current balance |
| POST | `/api/transactions/credit` | Deposit. Body: `{accountNumber,amount,narration}` |
| POST | `/api/transactions/debit` | Withdrawal. Body: `{accountNumber,amount,narration}` |
| POST | `/api/transactions/transfer` | Transfer. Body: `{sourceAccount,destinationAccount,amount,narration}` |
| GET | `/api/transactions/account/{nuban}` | Last 50 transactions |

## Run

```bash
mvn -pl core-banking-service -am spring-boot:run
# with PostgreSQL + Kafka from docker-compose:
SPRING_PROFILES_ACTIVE=postgres SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  mvn -pl core-banking-service spring-boot:run
```
