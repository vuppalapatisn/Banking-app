# product-config-service

The **Product and Configuration Service** of the Core Banking platform. Owns
product setup, interest rates, charges and fees, and limits and rules.
Runs on **port 8082**.

## Data model
`Product { id, code (unique), name, type, interestRate, monthlyFee, dailyDebitLimit, minBalance, active }`

Four sample products are seeded on startup via a `CommandLineRunner`.

## REST API `/api/products`
| Method | Path              | Description              |
|--------|-------------------|--------------------------|
| GET    | `/api/products`   | List all products        |
| GET    | `/api/products/{code}` | Get product by code |
| POST   | `/api/products`   | Create a product         |
| PUT    | `/api/products/{code}` | Update a product    |
| DELETE | `/api/products/{code}` | Delete a product    |

## Persistence
- Default profile: **H2** in-memory (`jdbc:h2:mem:products`), H2 console enabled.
- `postgres` profile: PostgreSQL via `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` /
  `SPRING_DATASOURCE_PASSWORD` (defaults to `jdbc:postgresql://localhost:5432/banking`,
  user/pass `banking`). `spring.jpa.hibernate.ddl-auto=update`.

## Actuator
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
