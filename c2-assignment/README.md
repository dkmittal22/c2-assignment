# c2-assignment

Full-stack support ticket system: a Spring Boot REST API backed by PostgreSQL with a React (Vite) single-page application.

- **Backend:** Java 21, Spring Boot 3.4, Spring Data JPA, Flyway, PostgreSQL, testcontainers (port `8080`, API base `/api/v1`)
- **Frontend:** React 18 + Vite 5 + React Router (port `5173`)
- **Testing:** JUnit 5, MockMvc ITs, Testcontainers Postgres ITs, unit tests — see [spec/test-strategy.md](spec/test-strategy.md)

## Prerequisites

- JDK 21, Maven 3.8+, Node 18+, Docker (with a working Unix socket at `/var/run/docker.sock`)
- PostgreSQL runs via Docker Compose; the API itself does not run without it.

## Quick start

### 1. Database (Docker)

```bash
cp .env.example .env      # edit credentials as needed; defaults target host port 5433
export DOCKER_HOST=unix:///var/run/docker.sock   # if Docker Desktop socket path is in use
docker compose up -d
docker compose ps          # expect c2-assignment-postgres healthy on 0.0.0.0:5433
```

`.env` is gitignored — never commit secrets. `POSTGRES_PORT` defaults to `5433` to avoid clashes with a local Postgres on `5432`.

### 2. Backend API

```bash
cd backend
set -a && . ../.env && set +a      # export SPRING_DATASOURCE_URL / USERNAME / PASSWORD
mvn spring-boot:run
# Started ... Tomcat started on port 8080
```

Schema is created by Flyway on startup (`ddl-auto=validate` verifies the entity mapping).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
# vite ready → http://localhost:5173
```

Open <http://localhost:5173> — `/` redirects to `/tickets`. The frontend calls `http://localhost:8080/api/v1` (override with `VITE_API_BASE_URL` in `frontend/.env`).

## Running the tests

```bash
cd backend
DOCKER_HOST=unix:///var/run/docker.sock mvn test
# 121 tests: unit + Testcontainers/MockMvc integration (needs Docker)
```

Note: `pom.xml` pins `testcontainers.version=1.21.4` — the Spring Boot-managed 1.20.x bundles a docker-java that cannot talk to Docker Engine 29 (min API 1.44).

## API overview

All endpoints under `/api/v1` (details in [spec/api-contract.md](spec/api-contract.md)):

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/tickets?search=&status=&page=&size=` | Paginated list (default `size=20`) |
| POST | `/tickets` | Create (status always `OPEN`) |
| GET | `/tickets/{id}` | Detail incl. comments |
| PATCH | `/tickets/{id}` | Update title/description/priority/assignee (partial) |
| POST | `/tickets/{id}/transitions` | Status change `{ "toStatus": "..." }` |
| POST | `/tickets/{id}/comments` | Add comment |

State machine (spec/state-machine.md): `OPEN → IN_PROGRESS | CANCELLED`, `IN_PROGRESS → RESOLVED | CANCELLED`, `RESOLVED → CLOSED`; `CLOSED`/`CANCELLED` are terminal. Status changes go through the transitions endpoint only.

Errors use a uniform body: `{ timestamp, status, error, message, path, fieldErrors? }` with codes `VALIDATION_ERROR`, `BAD_REQUEST`, `NOT_FOUND`, `INVALID_TRANSITION`, `INTERNAL_ERROR`.

## Project layout

```
backend/                      Spring Boot API (com.c2assignment)
  src/main/resources/db/migration/   Flyway schema
frontend/                     React SPA (port 5173)
spec/                         Requirement, architecture, data-model, API contract,
                              state machine, UI flow, test strategy
docs/                         Implementation plan, decisions, prompt history, AI review
rules/ commands/ skills/      Project steering files
```

## Documentation maintained per session

- `docs/implementation-plan.md` — task plan (T-01…T-36) the implementation follows
- `docs/prompt-history.md` — every major prompt and its outcome
- `.specstory/history/` — saved development-session records (summaries of major prompts and outcomes, not guaranteed verbatim chat transcripts)
- `docs/ai-review.md` — spec consistency review + human/AI implementation reviews