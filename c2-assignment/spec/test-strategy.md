# Test Strategy Specification

## 1. Purpose

Define what must be tested, at which level, and with which tools so acceptance criteria in the other specs are verifiable.

## 2. Tools

| Level | Tools |
|-------|-------|
| Unit | JUnit 5; Mockito only when isolating dependencies is useful |
| Integration | Spring Boot Test; MockMvc or TestRestTemplate/WebTestClient; Testcontainers PostgreSQL |
| Frontend | Manual verification against `ui-flow.md` for v1 |

Do not mock simple value objects or pure domain transition logic unnecessarily — prefer direct unit tests of the state-machine rules.

## 3. Principles

1. Tests verify **behavior**, not implementation details.
2. Every business rule has automated coverage.
3. State transitions require **integration** tests (HTTP + DB), not only unit tests.
4. Integration DB tests use **Testcontainers PostgreSQL**, not H2 as a substitute for persistence verification.
5. When an AI-generated bug is found: reproduce with a test, fix, keep the regression test.
6. Optimistic locking and concurrent transitions are out of scope; do not write tests that require `@Version`.

## 4. Unit Tests (backend)

### 4.1 State machine / domain service

Cover the full matrix in `spec/state-machine.md`:

**Valid**

- OPEN → IN_PROGRESS
- OPEN → CANCELLED
- IN_PROGRESS → RESOLVED
- IN_PROGRESS → CANCELLED
- RESOLVED → CLOSED

**Invalid (required)**

- CLOSED → OPEN
- RESOLVED → OPEN
- RESOLVED → IN_PROGRESS
- CANCELLED → OPEN
- OPEN → RESOLVED
- OPEN → CLOSED
- IN_PROGRESS → CLOSED
- RESOLVED → CANCELLED
- Self-transitions for each status
- Any transition from CLOSED
- Any transition from CANCELLED

### 4.2 Validation helpers (if extracted)

- Blank / whitespace title and description rejected
- Priority defaulting to MEDIUM when absent
- Length boundaries: title 200, description 5000, assignee 100, author 100, content 2000

## 5. Integration Tests (backend)

Use Spring Boot integration tests with Testcontainers PostgreSQL and Flyway migrations applied. Hibernate must run with `ddl-auto=validate`.

### 5.1 REST API — tickets

| Case | Expect |
|------|--------|
| Create valid ticket without priority | 201; status OPEN; priority MEDIUM; `comments` omitted; `Location` header present |
| Create with priority HIGH | 201; priority HIGH |
| Create with blank title | 400; `VALIDATION_ERROR`; fieldErrors.title present |
| Create with blank description | 400; `VALIDATION_ERROR` |
| Create with invalid priority | 400; `VALIDATION_ERROR` |
| Create with title length 201 | 400; `VALIDATION_ERROR` |
| Create with assignee length 101 | 400; `VALIDATION_ERROR` |
| Create with status field present | 400; `BAD_REQUEST`; no row inserted |
| Create with unknown JSON field | 400; `BAD_REQUEST`; no row inserted |
| Get existing ticket | 200; body matches; `comments` array present |
| Get malformed UUID | 400; `BAD_REQUEST` |
| Get valid UUID missing ticket | 404; `NOT_FOUND` |
| Patch title | 200; updatedAt changes; title updated; `comments` omitted |
| Patch with status field | 400; `BAD_REQUEST`; ticket unchanged in DB |
| Patch with unknown field | 400; `BAD_REQUEST`; ticket unchanged |
| Patch empty body | 400; `BAD_REQUEST`; ticket unchanged |
| Patch missing ticket | 404; `NOT_FOUND` |
| Patch malformed UUID | 400; `BAD_REQUEST` |
| List default pagination | size 20, page 0 |
| List size 101 | 400; `BAD_REQUEST` |
| List size 0 | 400; `BAD_REQUEST` |
| List negative page | 400; `BAD_REQUEST` |
| List filter status=OPEN | only OPEN |
| List invalid status query | 400; `VALIDATION_ERROR` |
| List search case-insensitive on title | match |
| List search on description | match |
| List search + status | AND semantics |
| Sort order | newest createdAt first |

### 5.2 REST API — comments

| Case | Expect |
|------|--------|
| Add comment to existing ticket | 201; appears on GET ticket |
| Add comment with blank content | 400; `VALIDATION_ERROR` |
| Add comment missing author | 400; `VALIDATION_ERROR` |
| Add comment author length 101 | 400; `VALIDATION_ERROR` |
| Add comment content length 2001 | 400; `VALIDATION_ERROR` |
| Add comment unknown field | 400; `BAD_REQUEST` |
| Add comment to missing ticket | 404; `NOT_FOUND` |
| Add comment malformed UUID | 400; `BAD_REQUEST` |
| Add comment to CLOSED ticket | 201 |
| Add comment to CANCELLED ticket | 201 |
| Parent updatedAt changes after comment | true |

### 5.3 REST API — transitions

| Case | Expect |
|------|--------|
| Each valid transition | 200; status persisted; `comments` omitted |
| Each SM acceptance invalid transition | 409; `INVALID_TRANSITION`; DB unchanged |
| Transition missing ticket | 404; `NOT_FOUND` |
| Transition malformed UUID | 400; `BAD_REQUEST` |
| Transition invalid toStatus enum | 400; `VALIDATION_ERROR` |
| Transition unknown JSON field | 400; `BAD_REQUEST` |

### 5.4 Persistence

| Case | Expect |
|------|--------|
| Flyway migrates cleanly on empty Postgres | tables `tickets` and `comments` exist |
| Hibernate validate succeeds against Flyway schema | startup/context loads |
| Ticket round-trip fields | equals written values |
| IDs are application-set UUIDs | persisted id equals generated id; no DB UUID default required |
| Enum stored as string values | not ordinals |
| FK: comment references ticket | enforced with ON DELETE RESTRICT |

### 5.5 Error contract

| Case | Expect |
|------|--------|
| Validation error body | timestamp, status, error=`VALIDATION_ERROR`, message, path, fieldErrors |
| Bad request body | timestamp, status, error=`BAD_REQUEST`, message, path; fieldErrors omitted |
| Not found body | error=`NOT_FOUND` |
| Invalid transition body | error=`INVALID_TRANSITION`; status 409 |

### 5.6 CORS

| Case | Expect |
|------|--------|
| Preflight/request from origin `http://localhost:5173` to `/api/v1/**` | Allowed |

## 6. Frontend Verification (v1)

Automated frontend tests are not required by this strategy for v1.

Manual checklist (from `ui-flow.md`):

- UI-1 through UI-10

## 7. Regression Policy

1. Reproduce the bug with an automated test (prefer integration for API bugs).
2. Fix production code.
3. Keep the test.

## 8. Out of scope for tests (v1)

- Concurrent transition race conditions
- `@Version` / optimistic lock failure cases
- Elasticsearch / full-text ranking behavior
- Authentication / authorization

## 9. Definition of Done (Testing)

A feature is not done until:

- Acceptance criteria in `requirements.md`, `state-machine.md`, and `api-contract.md` that apply to that feature have automated backend tests
- Valid and invalid state transitions covering the matrix are integration-tested
- Validation cases for blank title, blank description, invalid priority, missing required fields, unknown fields, status-on-create, status-on-PATCH, malformed UUID, and missing resource pass
- Tests run against PostgreSQL via Testcontainers for persistence/API integration suites
- CORS allowance for `http://localhost:5173` is verified
