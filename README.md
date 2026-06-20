# Policy API

A REST API for managing insurance policies — quotation, issuance, lifecycle
status transitions, and premium calculation — built with Spring Boot and
PostgreSQL.

This project models the kind of backend work I did at Go Digit Insurance:
policy lifecycle management, pricing logic, and REST APIs for insurance
products.

## Features

- Full CRUD for insurance policies
- Server-side premium calculation based on policy type, sum insured, and term length
- Enforced policy lifecycle (`QUOTED → ACTIVE → RENEWED / LAPSED / CANCELLED`) — invalid transitions are rejected with a `409 Conflict`
- Request validation with detailed field-level error responses
- Centralized exception handling (`404`, `400`, `409`, `500` all return a consistent JSON error shape)
- Unit tests for business logic (premium calculation, lifecycle rules) and slice tests for the web layer
- Dockerized, with `docker-compose` for one-command local startup (app + Postgres)
- CI via GitHub Actions — tests run automatically on every push

## Tech Stack

Java 17 · Spring Boot 3 · Spring Data JPA · PostgreSQL · Maven · JUnit 5 · Mockito · Docker

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/policies` | Create a new policy (status starts as `QUOTED`) |
| GET | `/api/v1/policies/{id}` | Get a policy by ID |
| GET | `/api/v1/policies/number/{policyNumber}` | Get a policy by policy number |
| GET | `/api/v1/policies?status=ACTIVE` | List policies, optionally filtered by status |
| PATCH | `/api/v1/policies/{id}/status` | Transition a policy's status |
| DELETE | `/api/v1/policies/{id}` | Delete a policy |

### Example: create a policy

```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -d '{
    "holderName": "Asha Verma",
    "holderEmail": "asha@example.com",
    "policyType": "HEALTH",
    "sumInsured": 500000,
    "startDate": "2026-07-01",
    "endDate": "2027-07-01"
  }'
```

Response:

```json
{
  "id": 1,
  "policyNumber": "POL-482913",
  "holderName": "Asha Verma",
  "holderEmail": "asha@example.com",
  "policyType": "HEALTH",
  "status": "QUOTED",
  "sumInsured": 500000,
  "premium": 17500.00,
  "startDate": "2026-07-01",
  "endDate": "2027-07-01"
}
```

## Running locally

### Option 1: Docker Compose (easiest)

```bash
docker-compose up --build
```

This starts both PostgreSQL and the app. API will be available at `http://localhost:8080`.

### Option 2: Local Maven + your own Postgres

1. Create a database: `createdb policy_db` (or via any Postgres client)
2. Set environment variables (or edit `src/main/resources/application.yml` directly):
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   ```
3. Run:
   ```bash
   mvn spring-boot:run
   ```

## Running tests

```bash
mvn test
```

Tests use an in-memory H2 database, so no Postgres setup is needed to run them.

## Project Structure

```
src/main/java/com/sparshdarhe/policyapi/
├── controller/   REST endpoints
├── service/      Business logic (premium calc, lifecycle rules)
├── repository/   Spring Data JPA repositories
├── model/        JPA entities and enums
├── dto/          Request/response payloads (kept separate from entities)
└── exception/    Custom exceptions + global handler
```

## Design notes

- **DTOs are separate from entities** so the API contract can evolve independently of the persistence model.
- **Premium calculation and lifecycle rules live in the service layer**, not in controllers or entities, to keep business logic centralized and unit-testable without a database.
- **Status transitions are validated against an explicit allowed-transitions map** rather than scattered if/else checks, making the lifecycle rules easy to read and extend.
